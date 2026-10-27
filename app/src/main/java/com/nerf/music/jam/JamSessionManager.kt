package com.nerf.music.jam

import android.net.Uri
import android.os.Build
import android.util.Log
import com.nerf.music.data.model.Track
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
    val positionMs: Long
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
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    // Mapeia clientId do socket do servidor -> JamParticipant
    private val connectedGuestsBySocketId = ConcurrentHashMap<String, JamParticipant>()

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
        if (cleanRelay.isNotBlank()) {
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

        try {
            val server = JamWebSocketServer(
                onClientConnected = { socketId ->
                    Log.i(TAG, "Cliente WebSocket conectado ao Host: $socketId")
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
            val wsUrl = "ws://$localIp:$boundPort/jam/$sessionId"
            val inviteLink = buildInviteDeepLink(sessionId, wsUrl, localDisplayName)

            _sessionState.value = JamSessionState(
                isInJam = true,
                isHost = true,
                sessionId = sessionId,
                localPeerId = localPeerId,
                localDisplayName = localDisplayName,
                hostName = localDisplayName,
                wsEndpointUrl = wsUrl,
                inviteDeepLink = inviteLink,
                participants = listOf(hostParticipant),
                connectionStatus = JamConnectionStatus.CONNECTED,
                errorMessage = null
            )

            startHostHeartbeatLoop()
            onUserFeedback("JAM iniciada! Compartilhe o link para ouvirem juntos.")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar servidor WebSocket da JAM", e)
            _sessionState.value = _sessionState.value.copy(
                connectionStatus = JamConnectionStatus.ERROR,
                errorMessage = "Erro ao iniciar JAM: ${e.message}"
            )
            onUserFeedback("Não foi possível iniciar o servidor da JAM.")
        }
    }

    /**
     * Processa um DeepLink `nerfmusic://jam/join?...` ou `https://nerfmusic.app/jam/join?...`
     * e conecta automaticamente à sessão JAM sem necessidade de conta.
     */
    fun handleDeepLink(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path?.lowercase().orEmpty()

        val isJamUri = (scheme == DEEP_LINK_SCHEME && host == DEEP_LINK_HOST) ||
                ((scheme == "http" || scheme == "https") && (host.contains("nerfmusic") || path.startsWith("/jam")))

        if (!isJamUri) return false

        val rawWs = uri.getQueryParameter("ws")?.let {
            try { URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it }
        } ?: return false

        val sessionId = uri.getQueryParameter("session")?.takeIf { it.isNotBlank() } ?: "NERF-JAM"
        val hostName = uri.getQueryParameter("host")?.takeIf { it.isNotBlank() } ?: "Host"

        joinSession(wsUrlInput = rawWs, sessionId = sessionId, hostName = hostName)
        return true
    }

    /**
     * Permite entrar em uma JAM colando tanto um DeepLink (`nerfmusic://jam/...`) quanto um endereço
     * WebSocket (`ws://192.168.x.x:8765`) ou apenas o `IP:PORT` do Host.
     */
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

        val normalizedWs = normalizeWsUrl(trimmed)
        val extractedSession = trimmed.substringAfterLast("/jam/", "").takeIf { it.isNotBlank() } ?: "NERF-JAM"
        joinSession(wsUrlInput = normalizedWs, sessionId = extractedSession, hostName = "Host")
    }

    fun joinSession(wsUrlInput: String, sessionId: String = "NERF-JAM", hostName: String = "Host") {
        leaveSession(silent = true)

        val normalizedWs = normalizeWsUrl(wsUrlInput)
        val inviteLink = buildInviteDeepLink(sessionId, normalizedWs, hostName)
        val selfParticipant = JamParticipant(
            peerId = localPeerId,
            displayName = localDisplayName,
            isHost = false
        )

        _sessionState.value = JamSessionState(
            isInJam = true,
            isHost = false,
            sessionId = sessionId,
            localPeerId = localPeerId,
            localDisplayName = localDisplayName,
            hostName = hostName,
            wsEndpointUrl = normalizedWs,
            inviteDeepLink = inviteLink,
            participants = listOf(selfParticipant),
            connectionStatus = JamConnectionStatus.CONNECTING,
            errorMessage = null
        )

        connectClientWebSocket(normalizedWs, sessionId, isHostOnRelay = false)
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
                    // Executa comando do convidado no Host e propaga o novo estado para toda a sala
                    onExecuteGuestCommandOnHost(msg)
                    delay(60L)
                    broadcastHostStateSnapshot()
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
                    onExecuteGuestCommandOnHost(msg)
                    scope.launch {
                        delay(60L)
                        broadcastHostStateSnapshot()
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
                    connectionStatus = JamConnectionStatus.CONNECTED
                )

                onApplyRemoteSnapshot(msg, compensatedPos)
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
     * Chamado pelo Host sempre que o estado do player muda (play, pause, seek, faixa, fila)
     * ou periodicamente pelo heartbeat.
     */
    fun broadcastHostStateSnapshot() {
        val cur = _sessionState.value
        if (!cur.isInJam || !cur.isHost) return

        val snap = providePlaybackSnapshot()
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
            positionMs = snap.positionMs
        )
        val encoded = json.encodeToString(msg)
        embeddedServer?.broadcast(encoded)
        clientWebSocket?.send(encoded)
    }

    /**
     * Envia um comando de reprodução/fila caso o usuário seja um Convidado (Guest),
     * ou propaga o estado imediatamente caso o usuário seja o Host.
     */
    fun dispatchOrBroadcastPlayerAction(
        command: JamCommandType,
        targetPositionMs: Long? = null,
        targetQueueIndex: Int? = null,
        fromQueueIndex: Int? = null,
        toQueueIndex: Int? = null,
        targetTrack: Track? = null,
        targetTracks: List<Track> = emptyList()
    ) {
        val cur = _sessionState.value
        if (!cur.isInJam) return

        if (cur.isHost) {
            scope.launch {
                delay(40L)
                broadcastHostStateSnapshot()
            }
        } else {
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
                targetTracks = targetTracks
            )
            clientWebSocket?.send(json.encodeToString(cmdMsg))
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
            } catch (_: Exception) {}
        }

        try {
            clientWebSocket?.close(1000, "User left JAM")
        } catch (_: Exception) {}
        clientWebSocket = null

        embeddedServer?.stop()
        embeddedServer = null
        connectedGuestsBySocketId.clear()

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
