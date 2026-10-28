package com.nerf.music.jam

import android.net.Uri
import android.os.Build
import android.util.Log
import com.nerf.music.data.model.Track
import com.nerf.music.player.AudioPlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class JamPlaybackSnapshot(
    val currentTrack: Track?,
    val queue: List<Track>,
    val currentQueueIndex: Int,
    val isPlaying: Boolean,
    val positionMs: Long,
    val repeatModeName: String = "OFF",
    val hostCrossfadeSeconds: Int = 0,
    val hostAutomixEnabled: Boolean = true,
    val hostGaplessEnabled: Boolean = true,
    val isKaraokeActive: Boolean = false,
    val karaokeVocalLevel: Float = 0.0f
)

/**
 * Gerenciador central da Feature JAM (Escuta Sincronizada em Grupo No-Account).
 * Opera exclusivamente via WebSockets para sincronização em tempo real e DeepLinks para convites.
 */
class JamSessionManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val providePlaybackSnapshot: () -> JamPlaybackSnapshot,
    private val onApplyRemoteSnapshot: (snapshot: JamWireMessage, compensatedPositionMs: Long) -> Unit,
    private val onExecuteGuestCommandOnHost: (commandMsg: JamWireMessage) -> Unit,
    private val onUserFeedback: (message: String) -> Unit
) {
    companion object {
        private const val TAG = "JamSessionManager"
        const val DEEP_LINK_SCHEME = "nerfmusic"
        const val DEEP_LINK_HOST = "jam"

        fun buildInviteDeepLink(sessionId: String, wsUrl: String, hostName: String): String {
            val encodedWs = URLEncoder.encode(wsUrl, "UTF-8")
            val encodedHost = URLEncoder.encode(hostName, "UTF-8")
            val encodedSession = URLEncoder.encode(sessionId, "UTF-8")
            return "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/join?session=$encodedSession&ws=$encodedWs&host=$encodedHost"
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket mantém stream aberto indefinidamente
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private val localPeerId: String = "peer_" + UUID.randomUUID().toString().take(8)
    private var localDisplayName: String = Build.MODEL?.takeIf { it.isNotBlank() } ?: "NERF Listener"

    private val _sessionState = MutableStateFlow(
        JamSessionState(
            localPeerId = localPeerId,
            localDisplayName = localDisplayName
        )
    )
    val sessionState: StateFlow<JamSessionState> = _sessionState.asStateFlow()

    private var embeddedServer: JamWebSocketServer? = null
    private var clientWebSocket: WebSocket? = null
    private var globalRelayClient: JamGlobalRelayClient? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    // Mapeia clientId do socket do servidor (ou peerId no relay global) -> JamParticipant
    private val connectedGuestsBySocketId = ConcurrentHashMap<String, JamParticipant>()

    fun updateHostRoomSettings(transform: (JamRoomSettings) -> JamRoomSettings) {
        val cur = _sessionState.value
        val updatedSettings = transform(cur.roomSettings)
        _sessionState.value = cur.copy(roomSettings = updatedSettings)
        if (cur.isInJam && cur.isHost) {
            broadcastHostStateSnapshot()
        }
    }

    fun kickParticipant(targetPeerId: String) {
        val cur = _sessionState.value
        if (!cur.isInJam || !cur.isHost || targetPeerId == localPeerId) return

        val keysToRemove = connectedGuestsBySocketId.entries
            .filter { it.value.peerId == targetPeerId }
            .map { it.key }
        var kickedName = "Convidado"
        for (k in keysToRemove) {
            connectedGuestsBySocketId.remove(k)?.let { kickedName = it.displayName }
        }
        refreshHostParticipantsList()

        val kickMsg = JamWireMessage(
            type = JamMessageType.COMMAND,
            sessionId = cur.sessionId,
            senderId = localPeerId,
            senderName = localDisplayName,
            hostId = localPeerId,
            command = JamCommandType.KICK_PARTICIPANT,
            targetPeerId = targetPeerId
        )
        val encoded = json.encodeToString(kickMsg)
        embeddedServer?.broadcast(encoded)
        clientWebSocket?.send(encoded)
        globalRelayClient?.send(encoded, retain = false)

        onUserFeedback("$kickedName foi removido da JAM.")
        broadcastHostStateSnapshot()
    }

    fun requestImmediateSyncFromHost() {
        val cur = _sessionState.value
        if (!cur.isInJam) return
        if (cur.isHost) {
            broadcastHostStateSnapshot()
            onUserFeedback("Sincronização enviada para todos na sala.")
        } else {
            val req = JamWireMessage(
                type = JamMessageType.COMMAND,
                sessionId = cur.sessionId,
                senderId = localPeerId,
                senderName = localDisplayName,
                command = JamCommandType.REQUEST_SYNC
            )
            val encoded = json.encodeToString(req)
            clientWebSocket?.send(encoded)
            globalRelayClient?.send(encoded, retain = false)
            onUserFeedback("Ressincronizando áudio com o Host...")
        }
    }

    private fun isGuestCommandAllowed(cmd: JamCommandType, settings: JamRoomSettings): Boolean {
        return when (cmd) {
            JamCommandType.REQUEST_SYNC -> true
            JamCommandType.KICK_PARTICIPANT -> false
            JamCommandType.ADD_TO_QUEUE,
            JamCommandType.PLAY_NEXT_IN_QUEUE,
            JamCommandType.REMOVE_QUEUE_ITEM,
            JamCommandType.MOVE_QUEUE_ITEM -> settings.allowGuestsToQueue
            JamCommandType.PLAY,
            JamCommandType.PAUSE,
            JamCommandType.SEEK,
            JamCommandType.SKIP_NEXT,
            JamCommandType.SKIP_PREV,
            JamCommandType.PLAY_TRACK,
            JamCommandType.SELECT_QUEUE_INDEX,
            JamCommandType.SET_REPEAT_MODE -> settings.allowGuestsToControlPlayback
        }
    }

    private fun buildGuestCommandAnnouncement(msg: JamWireMessage): String? {
        val guestName = msg.senderName.ifBlank { "Convidado" }
        return when (msg.command) {
            JamCommandType.ADD_TO_QUEUE, JamCommandType.PLAY_NEXT_IN_QUEUE -> {
                val trackTitle = msg.targetTrack?.title ?: msg.targetTracks.firstOrNull()?.title
                if (!trackTitle.isNullOrBlank()) "$guestName adicionou '$trackTitle' à fila" else null
            }
            JamCommandType.SKIP_NEXT -> "$guestName pulou para a próxima faixa"
            JamCommandType.PLAY_TRACK -> {
                val trackTitle = msg.targetTrack?.title
                if (!trackTitle.isNullOrBlank()) "$guestName colocou '$trackTitle' para tocar" else null
            }
            else -> null
        }
    }

    fun updateDisplayName(newName: String) {
        val clean = newName.trim().ifEmpty { Build.MODEL ?: "NERF Listener" }
        localDisplayName = clean
        val current = _sessionState.value
        val updatedParticipants = current.participants.map {
            if (it.peerId == localPeerId) it.copy(displayName = clean) else it
        }
        val newDeepLink = if (current.isInJam && current.isHost && current.wsEndpointUrl.isNotBlank()) {
            buildInviteDeepLink(current.sessionId, current.wsEndpointUrl, clean)
        } else {
            current.inviteDeepLink
        }
        _sessionState.value = current.copy(
            localDisplayName = clean,
            hostName = if (current.isHost) clean else current.hostName,
            inviteDeepLink = newDeepLink,
            participants = updatedParticipants
        )
        if (current.isInJam && current.isHost) {
            broadcastHostStateSnapshot()
        }
    }

    /**
     * Inicia uma nova sessão JAM atuando como Host.
     * Se [customRelayWsUrl] for vazio, sobe o servidor WebSocket nativo no próprio aparelho (P2P/LAN/Hotspot).
     * Se [customRelayWsUrl] for informado, conecta ao relay WebSocket especificado.
     */
    fun startHostSession(customRelayWsUrl: String? = null) {
        leaveSession(silent = true)

        val sessionId = "NERF-${Random.nextInt(1000, 9999)}"
        val hostParticipant = JamParticipant(
            peerId = localPeerId,
            displayName = localDisplayName,
            isHost = true,
            rttMs = 0L
        )
        connectedGuestsBySocketId.clear()

        val cleanRelay = customRelayWsUrl?.trim().orEmpty()
        val forceLocalOnly = cleanRelay.equals("local", ignoreCase = true)

        var localWsUrl: String? = null
        try {
            val server = JamWebSocketServer(
                onClientConnected = { socketId ->
                    Log.i(TAG, "Cliente WebSocket local conectado ao Host: $socketId")
                },
                onClientMessage = { socketId, rawText ->
                    handleServerIncomingMessage(socketId, rawText)
                },
                onClientDisconnected = { socketId ->
                    val removed = connectedGuestsBySocketId.remove(socketId)
                    if (removed != null) {
                        scope.launch {
                            refreshHostParticipantsList()
                            onUserFeedback("${removed.displayName} saiu da JAM")
                            broadcastHostStateSnapshot()
                        }
                    }
                }
            )
            val boundPort = server.start(JamWebSocketServer.DEFAULT_PORT)
            embeddedServer = server
            val localIp = JamWebSocketServer.resolveLocalIpAddress()
            localWsUrl = "ws://$localIp:$boundPort/jam/$sessionId"
        } catch (e: Exception) {
            Log.w(TAG, "Servidor WebSocket local indisponível: ${e.message}")
        }

        if (forceLocalOnly && localWsUrl != null) {
            val inviteLink = buildInviteDeepLink(sessionId, localWsUrl, localDisplayName)
            _sessionState.value = JamSessionState(
                isInJam = true,
                isHost = true,
                sessionId = sessionId,
                localPeerId = localPeerId,
                localDisplayName = localDisplayName,
                hostName = localDisplayName,
                wsEndpointUrl = localWsUrl,
                inviteDeepLink = inviteLink,
                participants = listOf(hostParticipant),
                connectionStatus = JamConnectionStatus.CONNECTED,
                errorMessage = null
            )
            startHostHeartbeatLoop()
            onUserFeedback("JAM Local iniciada! Compartilhe o link na mesma rede Wi-Fi.")
            return
        }

        if (cleanRelay.isNotBlank() && !forceLocalOnly && !JamGlobalRelayClient.isGlobalRelayUrl(cleanRelay)) {
            val normalizedWs = normalizeWsUrl(cleanRelay)
            val inviteLink = buildInviteDeepLink(sessionId, normalizedWs, localDisplayName)
            _sessionState.value = JamSessionState(
                isInJam = true,
                isHost = true,
                sessionId = sessionId,
                localPeerId = localPeerId,
                localDisplayName = localDisplayName,
                hostName = localDisplayName,
                wsEndpointUrl = normalizedWs,
                inviteDeepLink = inviteLink,
                participants = listOf(hostParticipant),
                connectionStatus = JamConnectionStatus.CONNECTING
            )
            connectClientWebSocket(normalizedWs, sessionId, isHostOnRelay = true)
            return
        }

        val targetGlobalWss = if (cleanRelay.isNotBlank() && !forceLocalOnly) {
            normalizeWsUrl(cleanRelay)
        } else {
            JamGlobalRelayClient.DEFAULT_GLOBAL_WSS_URL
        }
        val inviteLink = buildInviteDeepLink(sessionId, targetGlobalWss, localDisplayName)
        _sessionState.value = JamSessionState(
            isInJam = true,
            isHost = true,
            sessionId = sessionId,
            localPeerId = localPeerId,
            localDisplayName = localDisplayName,
            hostName = localDisplayName,
            wsEndpointUrl = targetGlobalWss,
            inviteDeepLink = inviteLink,
            participants = listOf(hostParticipant),
            connectionStatus = JamConnectionStatus.CONNECTING,
            errorMessage = null
        )

        connectGlobalRelay(
            preferredWssUrl = targetGlobalWss,
            sessionId = sessionId,
            isHost = true
        )
    }

    fun handleDeepLink(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path?.lowercase().orEmpty()

        val isJamUri = (scheme == DEEP_LINK_SCHEME && host == DEEP_LINK_HOST) ||
                ((scheme == "http" || scheme == "https") && (host.contains("nerfmusic") || path.startsWith("/jam")))

        if (!isJamUri) return false

        val sessionId = uri.getQueryParameter("session")
            ?.let { try { URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it } }
            ?.trim()
            ?.uppercase()
            ?.takeIf { it.isNotBlank() }
            ?: "NERF-JAM"

        val rawWs = uri.getQueryParameter("ws")?.let {
            try { URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it }
        }?.takeIf { it.isNotBlank() } ?: JamGlobalRelayClient.DEFAULT_GLOBAL_WSS_URL

        val hostName = uri.getQueryParameter("host")
            ?.let { try { URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it } }
            ?.takeIf { it.isNotBlank() }
            ?: "Host"

        joinSession(wsUrlInput = rawWs, sessionId = sessionId, hostName = hostName)
        return true
    }

    fun joinFromUserInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        if (trimmed.startsWith("nerfmusic://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            try {
                val uri = Uri.parse(trimmed)
                if (handleDeepLink(uri)) return
            } catch (_: Exception) {}
        }

        val roomCode = JamGlobalRelayClient.extractRoomCodeOrNull(trimmed)
        if (roomCode != null) {
            joinSession(
                wsUrlInput = JamGlobalRelayClient.DEFAULT_GLOBAL_WSS_URL,
                sessionId = roomCode,
                hostName = "Host"
            )
            return
        }

        val normalizedWs = normalizeWsUrl(trimmed)
        val extractedSession = trimmed.substringAfterLast("/jam/", "")
            .takeIf { it.isNotBlank() }
            ?.uppercase()
            ?: "NERF-JAM"
        joinSession(wsUrlInput = normalizedWs, sessionId = extractedSession, hostName = "Host")
    }

    fun joinSession(wsUrlInput: String, sessionId: String = "NERF-JAM", hostName: String = "Host") {
        leaveSession(silent = true)

        val cleanSessionId = sessionId.trim().uppercase().ifBlank { "NERF-JAM" }
        val normalizedWs = normalizeWsUrl(wsUrlInput)
        val inviteLink = buildInviteDeepLink(cleanSessionId, normalizedWs, hostName)
        val selfParticipant = JamParticipant(
            peerId = localPeerId,
            displayName = localDisplayName,
            isHost = false
        )

        _sessionState.value = JamSessionState(
            isInJam = true,
            isHost = false,
            sessionId = cleanSessionId,
            localPeerId = localPeerId,
            localDisplayName = localDisplayName,
            hostName = hostName,
            wsEndpointUrl = normalizedWs,
            inviteDeepLink = inviteLink,
            participants = listOf(selfParticipant),
            connectionStatus = JamConnectionStatus.CONNECTING,
            errorMessage = null
        )

        if (JamGlobalRelayClient.isGlobalRelayUrl(normalizedWs)) {
            connectGlobalRelay(
                preferredWssUrl = normalizedWs,
                sessionId = cleanSessionId,
                isHost = false
            )
        } else {
            connectClientWebSocket(normalizedWs, cleanSessionId, isHostOnRelay = false)
        }
    }

    private fun connectGlobalRelay(
        preferredWssUrl: String,
        sessionId: String,
        isHost: Boolean
    ) {
        globalRelayClient?.close()
        val relay = JamGlobalRelayClient(
            okHttpClient = okHttpClient,
            scope = scope,
            sessionId = sessionId,
            localPeerId = localPeerId,
            onConnected = { connectedEndpoint ->
                val updatedInvite = buildInviteDeepLink(
                    sessionId = sessionId,
                    wsUrl = connectedEndpoint,
                    hostName = _sessionState.value.hostName
                )
                _sessionState.value = _sessionState.value.copy(
                    wsEndpointUrl = connectedEndpoint,
                    inviteDeepLink = updatedInvite,
                    connectionStatus = JamConnectionStatus.CONNECTED,
                    errorMessage = null
                )
                if (isHost) {
                    startHostHeartbeatLoop()
                    broadcastHostStateSnapshot()
                    onUserFeedback("JAM Global ativa ($sessionId)! Compartilhe o código ou link com qualquer pessoa.")
                } else {
                    val joinMsg = JamWireMessage(
                        type = JamMessageType.JOIN,
                        sessionId = sessionId,
                        senderId = localPeerId,
                        senderName = localDisplayName
                    )
                    globalRelayClient?.send(json.encodeToString(joinMsg), retain = false)
                    startGlobalGuestPingLoop(sessionId)
                    onUserFeedback("Conectado à sala $sessionId!")
                }
            },
            onMessageReceived = { rawJson ->
                handleGlobalRelayIncomingMessage(rawJson, isHost = isHost)
            },
            onReconnecting = { reason ->
                if (_sessionState.value.isInJam) {
                    _sessionState.value = _sessionState.value.copy(
                        connectionStatus = JamConnectionStatus.RECONNECTING,
                        errorMessage = "Reconectando ao Relay Global... ($reason)"
                    )
                }
            }
        )
        globalRelayClient = relay
        relay.connect(preferredWssUrl)
    }

    private fun handleGlobalRelayIncomingMessage(rawText: String, isHost: Boolean) {
        if (rawText.isBlank()) return
        val msg = try {
            json.decodeFromString<JamWireMessage>(rawText)
        } catch (e: Exception) {
            return
        }
        if (msg.senderId == localPeerId) return

        if (isHost) {
            when (msg.type) {
                JamMessageType.JOIN -> {
                    val guest = JamParticipant(
                        peerId = msg.senderId,
                        displayName = msg.senderName.ifBlank { "Convidado" },
                        isHost = false,
                        rttMs = 0L
                    )
                    connectedGuestsBySocketId["global_${msg.senderId}"] = guest
                    refreshHostParticipantsList()
                    onUserFeedback("${guest.displayName} entrou na JAM!")
                    broadcastHostStateSnapshot()
                }
                JamMessageType.COMMAND -> {
                    val cmd = msg.command ?: return
                    if (cmd == JamCommandType.REQUEST_SYNC) {
                        broadcastHostStateSnapshot()
                        return
                    }
                    if (!isGuestCommandAllowed(cmd, _sessionState.value.roomSettings)) {
                        Log.i(TAG, "Comando $cmd de ${msg.senderName} ignorado (bloqueado pelas permissões do Host)")
                        return
                    }
                    val announcement = buildGuestCommandAnnouncement(msg)
                    if (announcement != null) {
                        onUserFeedback(announcement)
                    }
                    onExecuteGuestCommandOnHost(msg)
                    scope.launch {
                        delay(60L)
                        broadcastHostStateSnapshot(actionAnnouncement = announcement)
                    }
                }
                JamMessageType.PING -> {
                    val pong = JamWireMessage(
                        type = JamMessageType.PONG,
                        sessionId = _sessionState.value.sessionId,
                        senderId = localPeerId,
                        senderName = localDisplayName,
                        pingTimestampMs = msg.pingTimestampMs
                    )
                    globalRelayClient?.send(json.encodeToString(pong), retain = false)
                }
                JamMessageType.LEAVE -> {
                    val removed = connectedGuestsBySocketId.remove("global_${msg.senderId}")
                    refreshHostParticipantsList()
                    if (removed != null) {
                        onUserFeedback("${removed.displayName} saiu da JAM")
                    }
                    broadcastHostStateSnapshot()
                }
                else -> {}
            }
            return
        }

        when (msg.type) {
            JamMessageType.WELCOME, JamMessageType.STATE_SNAPSHOT -> {
                val rtt = _sessionState.value.rttMs
                val oneWayLatency = (rtt / 2L).coerceIn(0L, 1_500L)
                val clockDelta = (System.currentTimeMillis() - msg.timestampMs).coerceIn(0L, 4_000L)
                val compensatedPos = if (msg.isPlaying) {
                    msg.positionMs + maxOf(oneWayLatency, clockDelta)
                } else {
                    msg.positionMs
                }

                val hostTitle = msg.hostName?.takeIf { it.isNotBlank() } ?: _sessionState.value.hostName
                val updatedInvite = buildInviteDeepLink(
                    sessionId = msg.sessionId.ifBlank { _sessionState.value.sessionId },
                    wsUrl = _sessionState.value.wsEndpointUrl,
                    hostName = hostTitle
                )

                val mergedParticipants = if (msg.participants.any { it.peerId == localPeerId }) {
                    msg.participants
                } else {
                    msg.participants + JamParticipant(
                        peerId = localPeerId,
                        displayName = localDisplayName,
                        isHost = false,
                        rttMs = _sessionState.value.rttMs
                    )
                }

                _sessionState.value = _sessionState.value.copy(
                    sessionId = msg.sessionId.ifBlank { _sessionState.value.sessionId },
                    hostName = hostTitle,
                    inviteDeepLink = updatedInvite,
                    participants = mergedParticipants,
                    roomSettings = msg.roomSettings,
                    connectionStatus = JamConnectionStatus.CONNECTED,
                    errorMessage = null
                )

                if (!msg.actionAnnouncement.isNullOrBlank()) {
                    onUserFeedback(msg.actionAnnouncement)
                }

                onApplyRemoteSnapshot(msg, compensatedPos)
            }

            JamMessageType.COMMAND -> {
                if (msg.command == JamCommandType.KICK_PARTICIPANT && msg.targetPeerId == localPeerId) {
                    leaveSession(silent = true)
                    onUserFeedback("Você foi removido da sessão JAM pelo Host.")
                }
            }

            JamMessageType.PONG -> {
                val sentAt = msg.pingTimestampMs ?: return
                val rtt = (System.currentTimeMillis() - sentAt).coerceAtLeast(1L)
                val updatedParts = _sessionState.value.participants.map {
                    if (it.peerId == localPeerId) it.copy(rttMs = rtt) else it
                }
                _sessionState.value = _sessionState.value.copy(
                    rttMs = rtt,
                    participants = updatedParts
                )
            }

            JamMessageType.LEAVE -> {
                if (msg.senderId == msg.hostId) {
                    leaveSession(silent = true)
                    onUserFeedback("O Host encerrou a sessão JAM.")
                }
            }

            else -> {}
        }
    }

    private fun startGlobalGuestPingLoop(sessionId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _sessionState.value.isInJam && !_sessionState.value.isHost) {
                val ping = JamWireMessage(
                    type = JamMessageType.PING,
                    sessionId = sessionId,
                    senderId = localPeerId,
                    senderName = localDisplayName,
                    pingTimestampMs = System.currentTimeMillis()
                )
                try {
                    globalRelayClient?.send(json.encodeToString(ping), retain = false)
                } catch (_: Exception) {}
                delay(3000L)
            }
        }
    }

    private fun connectClientWebSocket(wsUrl: String, sessionId: String, isHostOnRelay: Boolean) {
        reconnectJob?.cancel()
        val request = try {
            Request.Builder().url(wsUrl).build()
        } catch (e: Exception) {
            _sessionState.value = _sessionState.value.copy(
                connectionStatus = JamConnectionStatus.ERROR,
                errorMessage = "URL WebSocket inválida: $wsUrl"
            )
            onUserFeedback("Endereço WebSocket inválido.")
            return
        }

        clientWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Conectado ao WebSocket da JAM: $wsUrl")
                scope.launch {
                    _sessionState.value = _sessionState.value.copy(
                        connectionStatus = JamConnectionStatus.CONNECTED,
                        errorMessage = null
                    )
                    if (isHostOnRelay) {
                        startHostHeartbeatLoop()
                        broadcastHostStateSnapshot()
                        onUserFeedback("JAM conectada ao Relay com sucesso!")
                    } else {
                        val joinMsg = JamWireMessage(
                            type = JamMessageType.JOIN,
                            sessionId = sessionId,
                            senderId = localPeerId,
                            senderName = localDisplayName
                        )
                        webSocket.send(json.encodeToString(joinMsg))
                        startGuestPingLoop(webSocket, sessionId)
                        onUserFeedback("Conectado à JAM!")
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    handleClientIncomingMessage(webSocket, text, isHostOnRelay)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scope.launch {
                    if (_sessionState.value.isInJam) {
                        _sessionState.value = _sessionState.value.copy(
                            isInJam = false,
                            connectionStatus = JamConnectionStatus.DISCONNECTED
                        )
                        onUserFeedback("A sessão JAM foi encerrada.")
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Falha na conexão WebSocket da JAM: ${t.message}")
                scope.launch {
                    val cur = _sessionState.value
                    if (!cur.isInJam) return@launch
                    _sessionState.value = cur.copy(
                        connectionStatus = JamConnectionStatus.RECONNECTING,
                        errorMessage = "Reconectando ao WebSocket... (${t.localizedMessage ?: "Sem resposta"})"
                    )
                    scheduleReconnect(wsUrl, sessionId, isHostOnRelay)
                }
            }
        })
    }

    private fun scheduleReconnect(wsUrl: String, sessionId: String, isHostOnRelay: Boolean) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(2500L)
            if (_sessionState.value.isInJam && _sessionState.value.connectionStatus == JamConnectionStatus.RECONNECTING) {
                Log.i(TAG, "Tentando reconexão automática ao WebSocket da JAM: $wsUrl")
                connectClientWebSocket(wsUrl, sessionId, isHostOnRelay)
            }
        }
    }

    private fun handleServerIncomingMessage(socketId: String, rawText: String) {
        val msg = try {
            json.decodeFromString<JamWireMessage>(rawText)
        } catch (e: Exception) {
            Log.w(TAG, "Mensagem JSON inválida recebida de $socketId: ${e.message}")
            return
        }

        scope.launch {
            when (msg.type) {
                JamMessageType.JOIN -> {
                    val guest = JamParticipant(
                        peerId = msg.senderId,
                        displayName = msg.senderName.ifBlank { "Convidado" },
                        isHost = false,
                        rttMs = 0L
                    )
                    connectedGuestsBySocketId[socketId] = guest
                    refreshHostParticipantsList()
                    onUserFeedback("${guest.displayName} entrou na JAM!")

                    // Envia snapshot imediato para todos os participantes
                    broadcastHostStateSnapshot()
                }

                JamMessageType.PING -> {
                    val pong = JamWireMessage(
                        type = JamMessageType.PONG,
                        sessionId = _sessionState.value.sessionId,
                        senderId = localPeerId,
                        senderName = localDisplayName,
                        pingTimestampMs = msg.pingTimestampMs
                    )
                    embeddedServer?.sendToClient(socketId, json.encodeToString(pong))
                }

                JamMessageType.COMMAND -> {
                    val cmd = msg.command ?: return@launch
                    if (cmd == JamCommandType.REQUEST_SYNC) {
                        broadcastHostStateSnapshot()
                        return@launch
                    }
                    if (!isGuestCommandAllowed(cmd, _sessionState.value.roomSettings)) {
                        return@launch
                    }
                    val announcement = buildGuestCommandAnnouncement(msg)
                    if (announcement != null) {
                        onUserFeedback(announcement)
                    }
                    onExecuteGuestCommandOnHost(msg)
                    delay(60L)
                    broadcastHostStateSnapshot(actionAnnouncement = announcement)
                }

                JamMessageType.LEAVE -> {
                    val removed = connectedGuestsBySocketId.remove(socketId)
                    refreshHostParticipantsList()
                    if (removed != null) {
                        onUserFeedback("${removed.displayName} saiu da JAM")
                    }
                    broadcastHostStateSnapshot()
                }

                else -> {}
            }
        }
    }

    private fun handleClientIncomingMessage(
        webSocket: WebSocket,
        rawText: String,
        isHostOnRelay: Boolean
    ) {
        val msg = try {
            json.decodeFromString<JamWireMessage>(rawText)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao decodificar mensagem JAM no cliente: ${e.message}")
            return
        }

        if (msg.senderId == localPeerId) return

        if (isHostOnRelay) {
            when (msg.type) {
                JamMessageType.JOIN -> {
                    val existing = _sessionState.value.participants.filterNot { it.peerId == msg.senderId }
                    val newGuest = JamParticipant(peerId = msg.senderId, displayName = msg.senderName.ifBlank { "Convidado" }, isHost = false)
                    _sessionState.value = _sessionState.value.copy(participants = existing + newGuest)
                    onUserFeedback("${newGuest.displayName} entrou na JAM!")
                    broadcastHostStateSnapshot()
                }
                JamMessageType.COMMAND -> {
                    val cmd = msg.command ?: return
                    if (cmd == JamCommandType.REQUEST_SYNC) {
                        broadcastHostStateSnapshot()
                        return
                    }
                    if (!isGuestCommandAllowed(cmd, _sessionState.value.roomSettings)) {
                        Log.i(TAG, "Comando $cmd de ${msg.senderName} ignorado (bloqueado pelas permissões do Host)")
                        return
                    }
                    val announcement = buildGuestCommandAnnouncement(msg)
                    if (announcement != null) {
                        onUserFeedback(announcement)
                    }
                    onExecuteGuestCommandOnHost(msg)
                    scope.launch {
                        delay(60L)
                        broadcastHostStateSnapshot(actionAnnouncement = announcement)
                    }
                }
                JamMessageType.PING -> {
                    val pong = JamWireMessage(
                        type = JamMessageType.PONG,
                        sessionId = _sessionState.value.sessionId,
                        senderId = localPeerId,
                        senderName = localDisplayName,
                        pingTimestampMs = msg.pingTimestampMs
                    )
                    webSocket.send(json.encodeToString(pong))
                }
                else -> {}
            }
            return
        }

        // Modo Guest
        when (msg.type) {
            JamMessageType.WELCOME, JamMessageType.STATE_SNAPSHOT -> {
                val rtt = _sessionState.value.rttMs
                val oneWayLatency = (rtt / 2L).coerceIn(0L, 1_500L)
                val compensatedPos = if (msg.isPlaying) {
                    msg.positionMs + oneWayLatency
                } else {
                    msg.positionMs
                }

                val hostTitle = msg.hostName?.takeIf { it.isNotBlank() } ?: _sessionState.value.hostName
                val updatedInvite = buildInviteDeepLink(
                    sessionId = msg.sessionId.ifBlank { _sessionState.value.sessionId },
                    wsUrl = _sessionState.value.wsEndpointUrl,
                    hostName = hostTitle
                )

                _sessionState.value = _sessionState.value.copy(
                    sessionId = msg.sessionId.ifBlank { _sessionState.value.sessionId },
                    hostName = hostTitle,
                    inviteDeepLink = updatedInvite,
                    participants = msg.participants.ifEmpty { _sessionState.value.participants },
                    roomSettings = msg.roomSettings,
                    connectionStatus = JamConnectionStatus.CONNECTED
                )

                if (!msg.actionAnnouncement.isNullOrBlank()) {
                    onUserFeedback(msg.actionAnnouncement)
                }

                onApplyRemoteSnapshot(msg, compensatedPos)
            }

            JamMessageType.COMMAND -> {
                if (msg.command == JamCommandType.KICK_PARTICIPANT && msg.targetPeerId == localPeerId) {
                    leaveSession(silent = true)
                    onUserFeedback("Você foi removido da sessão JAM pelo Host.")
                }
            }

            JamMessageType.PONG -> {
                val sentAt = msg.pingTimestampMs ?: return
                val rtt = (System.currentTimeMillis() - sentAt).coerceAtLeast(1L)
                val updatedParts = _sessionState.value.participants.map {
                    if (it.peerId == localPeerId) it.copy(rttMs = rtt) else it
                }
                _sessionState.value = _sessionState.value.copy(
                    rttMs = rtt,
                    participants = updatedParts
                )
            }

            JamMessageType.LEAVE -> {
                if (msg.senderId == msg.hostId) {
                    leaveSession(silent = true)
                    onUserFeedback("O Host encerrou a sessão JAM.")
                }
            }

            else -> {}
        }
    }

    private fun refreshHostParticipantsList() {
        val hostPart = JamParticipant(
            peerId = localPeerId,
            displayName = localDisplayName,
            isHost = true,
            rttMs = 0L
        )
        val guests = connectedGuestsBySocketId.values.distinctBy { it.peerId }
        _sessionState.value = _sessionState.value.copy(
            participants = listOf(hostPart) + guests
        )
    }

    private fun startHostHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _sessionState.value.isInJam && _sessionState.value.isHost) {
                broadcastHostStateSnapshot()
                delay(2500L)
            }
        }
    }

    private fun startGuestPingLoop(webSocket: WebSocket, sessionId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _sessionState.value.isInJam && !_sessionState.value.isHost) {
                val ping = JamWireMessage(
                    type = JamMessageType.PING,
                    sessionId = sessionId,
                    senderId = localPeerId,
                    senderName = localDisplayName,
                    pingTimestampMs = System.currentTimeMillis()
                )
                try {
                    webSocket.send(json.encodeToString(ping))
                } catch (_: Exception) {}
                delay(3000L)
            }
        }
    }

    /**
     * Chamado pelo Host sempre que o estado do player muda (play, pause, seek, faixa, fila, repeat, crossfade)
     * ou periodicamente pelo heartbeat.
     */
    fun broadcastHostStateSnapshot(actionAnnouncement: String? = null) {
        val cur = _sessionState.value
        if (!cur.isInJam || !cur.isHost) return

        val snap = providePlaybackSnapshot()
        val syncedRoomSettings = cur.roomSettings.copy(
            repeatModeName = snap.repeatModeName,
            hostCrossfadeSeconds = snap.hostCrossfadeSeconds,
            hostAutomixEnabled = snap.hostAutomixEnabled,
            hostGaplessEnabled = snap.hostGaplessEnabled,
            isKaraokeActiveInRoom = snap.isKaraokeActive,
            karaokeVocalLevelInRoom = snap.karaokeVocalLevel
        )
        if (syncedRoomSettings != cur.roomSettings) {
            _sessionState.value = cur.copy(roomSettings = syncedRoomSettings)
        }

        val msg = JamWireMessage(
            type = JamMessageType.STATE_SNAPSHOT,
            sessionId = cur.sessionId,
            senderId = localPeerId,
            senderName = localDisplayName,
            timestampMs = System.currentTimeMillis(),
            hostId = localPeerId,
            hostName = localDisplayName,
            participants = cur.participants,
            currentTrack = snap.currentTrack,
            queue = snap.queue,
            currentQueueIndex = snap.currentQueueIndex,
            isPlaying = snap.isPlaying,
            positionMs = snap.positionMs,
            roomSettings = syncedRoomSettings,
            actionAnnouncement = actionAnnouncement
        )
        val encoded = json.encodeToString(msg)
        embeddedServer?.broadcast(encoded)
        clientWebSocket?.send(encoded)
        globalRelayClient?.send(encoded, retain = true)
    }

    /**
     * Verifica se o usuário atual pode executar o comando [command] na JAM.
     * Retorna `true` se não estiver em JAM, se for o Host, ou se a permissão do convidado estiver ativa.
     */
    fun checkPermissionOrNotify(command: JamCommandType): Boolean {
        val cur = _sessionState.value
        if (!cur.isInJam || cur.isHost) return true
        val allowed = isGuestCommandAllowed(command, cur.roomSettings)
        if (!allowed) {
            val reason = when (command) {
                JamCommandType.ADD_TO_QUEUE,
                JamCommandType.PLAY_NEXT_IN_QUEUE,
                JamCommandType.REMOVE_QUEUE_ITEM,
                JamCommandType.MOVE_QUEUE_ITEM ->
                    "Apenas o Host (${cur.hostName}) pode alterar a fila nesta Jam."
                else ->
                    "Apenas o Host (${cur.hostName}) pode pular faixas ou controlar o player nesta Jam."
            }
            onUserFeedback(reason)
        }
        return allowed
    }

    fun dispatchOrBroadcastPlayerAction(
        command: JamCommandType,
        targetPositionMs: Long? = null,
        targetQueueIndex: Int? = null,
        fromQueueIndex: Int? = null,
        toQueueIndex: Int? = null,
        targetTrack: Track? = null,
        targetTracks: List<Track> = emptyList(),
        targetRepeatModeName: String? = null
    ) {
        val cur = _sessionState.value
        if (!cur.isInJam) return

        if (cur.isHost) {
            scope.launch {
                delay(40L)
                broadcastHostStateSnapshot()
            }
        } else {
            if (!checkPermissionOrNotify(command)) return
            val cmdMsg = JamWireMessage(
                type = JamMessageType.COMMAND,
                sessionId = cur.sessionId,
                senderId = localPeerId,
                senderName = localDisplayName,
                command = command,
                targetPositionMs = targetPositionMs,
                targetQueueIndex = targetQueueIndex,
                fromQueueIndex = fromQueueIndex,
                toQueueIndex = toQueueIndex,
                targetTrack = targetTrack,
                targetTracks = targetTracks,
                targetRepeatModeName = targetRepeatModeName
            )
            val encodedCmd = json.encodeToString(cmdMsg)
            clientWebSocket?.send(encodedCmd)
            globalRelayClient?.send(encodedCmd, retain = false)
        }
    }

    fun leaveSession(silent: Boolean = false) {
        val cur = _sessionState.value
        heartbeatJob?.cancel()
        reconnectJob?.cancel()

        if (cur.isInJam) {
            try {
                val leaveMsg = JamWireMessage(
                    type = JamMessageType.LEAVE,
                    sessionId = cur.sessionId,
                    senderId = localPeerId,
                    senderName = localDisplayName,
                    hostId = if (cur.isHost) localPeerId else null
                )
                val encoded = json.encodeToString(leaveMsg)
                embeddedServer?.broadcast(encoded)
                clientWebSocket?.send(encoded)
                globalRelayClient?.send(encoded, retain = false)
                if (cur.isHost) {
                    globalRelayClient?.send("", retain = true)
                }
            } catch (_: Exception) {}
        }

        try {
            clientWebSocket?.close(1000, "User left JAM")
        } catch (_: Exception) {}
        clientWebSocket = null

        globalRelayClient?.close()
        globalRelayClient = null

        embeddedServer?.stop()
        embeddedServer = null
        connectedGuestsBySocketId.clear()
        AudioPlayerService.setJamAudioSettingsOverride(null, null, null)

        _sessionState.value = JamSessionState(
            isInJam = false,
            isHost = false,
            localPeerId = localPeerId,
            localDisplayName = localDisplayName,
            connectionStatus = JamConnectionStatus.DISCONNECTED
        )

        if (!silent && cur.isInJam) {
            onUserFeedback("Você saiu da sessão JAM.")
        }
    }

    private fun normalizeWsUrl(raw: String): String {
        val clean = raw.trim()
        return when {
            clean.startsWith("ws://", ignoreCase = true) || clean.startsWith("wss://", ignoreCase = true) -> clean
            clean.contains("://") -> "ws://" + clean.substringAfter("://")
            clean.contains(":") -> "ws://$clean"
            else -> "ws://$clean:${JamWebSocketServer.DEFAULT_PORT}/jam"
        }
    }
}
