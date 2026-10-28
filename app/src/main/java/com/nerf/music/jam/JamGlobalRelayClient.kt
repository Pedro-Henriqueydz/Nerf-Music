package com.nerf.music.jam

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Cliente WebSocket Global (Internet / 4G / 5G / Wi-Fi em redes distintas) para o NERF Jam.
 *
 * Utiliza WebSockets seguros (`wss://`) com framing binário MQTT v3.1.1 ultraleve sobre clusters
 * públicos globais de alta disponibilidade (com failover automático entre EMQX, HiveMQ e Eclipse),
 * permitindo que salas `NERF-XXXX` funcionem de qualquer lugar do mundo 100% sem conta (No-Account)
 * e sem necessidade de redirecionamento de portas ou mesma rede Wi-Fi.
 */
class JamGlobalRelayClient(
    private val okHttpClient: OkHttpClient,
    private val scope: CoroutineScope,
    private val sessionId: String,
    private val localPeerId: String,
    private val onConnected: (endpointUrl: String) -> Unit,
    private val onMessageReceived: (jsonPayload: String) -> Unit,
    private val onReconnecting: (reason: String) -> Unit
) {
    companion object {
        private const val TAG = "JamGlobalRelayClient"

        val GLOBAL_WSS_RELAYS = listOf(
            "wss://broker.emqx.io:8084/mqtt",
            "wss://broker.hivemq.com:8884/mqtt",
            "wss://mqtt.eclipseprojects.io:443/mqtt"
        )

        const val DEFAULT_GLOBAL_WSS_URL = "wss://broker.emqx.io:8084/mqtt"

        fun isGlobalRelayUrl(wsUrl: String): Boolean {
            val lower = wsUrl.trim().lowercase()
            return lower.contains("/mqtt") ||
                lower.contains("broker.emqx.io") ||
                lower.contains("broker.hivemq.com") ||
                lower.contains("eclipseprojects.io") ||
                lower.startsWith("global://")
        }

        /**
         * Extrai um código de sala padronizado `NERF-XXXX` caso o texto digitado pelo usuário
         * seja um código de 4 dígitos (`4821`), `NERF-4821` ou `nerf4821`.
         * Retorna `null` se for uma URL `ws://` / `wss://` ou `IP:PORT` explícito.
         */
        fun extractRoomCodeOrNull(rawInput: String): String? {
            val trimmed = rawInput.trim()
            if (trimmed.isEmpty()) return null
            if (trimmed.contains("://") || trimmed.contains(".") || trimmed.contains(":")) {
                return null
            }
            val cleaned = trimmed.uppercase().replace(Regex("[^A-Z0-9]"), "")
            val digitsMatch = Regex("""^(?:NERF)?(\d{4,6})$""").find(cleaned)
            if (digitsMatch != null) {
                return "NERF-${digitsMatch.groupValues[1]}"
            }
            val alphaNumMatch = Regex("""^NERF([A-Z0-9]{4,8})$""").find(cleaned)
            if (alphaNumMatch != null) {
                return "NERF-${alphaNumMatch.groupValues[1]}"
            }
            return null
        }

        fun topicForSession(sessionId: String): String {
            val normalizedSession = sessionId.trim().uppercase().ifBlank { "NERF-JAM" }
            return "nerfmusic/jam/v1/$normalizedSession"
        }

        // =====================================================================
        // Codificação / Decodificação Binária MQTT v3.1.1 over WebSocket
        // =====================================================================

        fun encodeRemainingLength(length: Int): ByteArray {
            var x = length
            val out = ByteArrayOutputStream(4)
            do {
                var encodedByte = x % 128
                x /= 128
                if (x > 0) {
                    encodedByte = encodedByte or 0x80
                }
                out.write(encodedByte)
            } while (x > 0)
            return out.toByteArray()
        }

        fun encodeUtf8Field(text: String): ByteArray {
            val bytes = text.toByteArray(StandardCharsets.UTF_8)
            val out = ByteArrayOutputStream(bytes.size + 2)
            out.write((bytes.size ushr 8) and 0xFF)
            out.write(bytes.size and 0xFF)
            out.write(bytes)
            return out.toByteArray()
        }

        fun buildConnectPacket(clientId: String, keepAliveSec: Int = 45): ByteString {
            val variableHeader = byteArrayOf(
                0x00, 0x04,
                'M'.code.toByte(), 'Q'.code.toByte(), 'T'.code.toByte(), 'T'.code.toByte(),
                0x04, // Protocol Level 4 (MQTT 3.1.1)
                0x02, // Connect Flags: Clean Session
                ((keepAliveSec ushr 8) and 0xFF).toByte(),
                (keepAliveSec and 0xFF).toByte()
            )
            val payload = encodeUtf8Field(clientId)
            val remaining = variableHeader.size + payload.size
            val out = ByteArrayOutputStream(2 + remaining)
            out.write(0x10) // CONNECT
            out.write(encodeRemainingLength(remaining))
            out.write(variableHeader)
            out.write(payload)
            return out.toByteArray().toByteString()
        }

        fun buildSubscribePacket(packetId: Int, topic: String): ByteString {
            val idBytes = byteArrayOf(
                ((packetId ushr 8) and 0xFF).toByte(),
                (packetId and 0xFF).toByte()
            )
            val topicBytes = encodeUtf8Field(topic)
            val qosByte = byteArrayOf(0x00) // QoS 0 para latência mínima em tempo real
            val remaining = idBytes.size + topicBytes.size + qosByte.size
            val out = ByteArrayOutputStream(2 + remaining)
            out.write(0x82) // SUBSCRIBE
            out.write(encodeRemainingLength(remaining))
            out.write(idBytes)
            out.write(topicBytes)
            out.write(qosByte)
            return out.toByteArray().toByteString()
        }

        fun buildPublishPacket(topic: String, jsonPayload: String, retain: Boolean = false): ByteString {
            val topicBytes = encodeUtf8Field(topic)
            val payloadBytes = jsonPayload.toByteArray(StandardCharsets.UTF_8)
            val remaining = topicBytes.size + payloadBytes.size
            val out = ByteArrayOutputStream(5 + remaining)
            val headerByte = if (retain) 0x31 else 0x30 // PUBLISH QoS 0 (+ Retain opcional)
            out.write(headerByte)
            out.write(encodeRemainingLength(remaining))
            out.write(topicBytes)
            out.write(payloadBytes)
            return out.toByteArray().toByteString()
        }

        fun buildPingReqPacket(): ByteString {
            return byteArrayOf(0xC0.toByte(), 0x00.toByte()).toByteString()
        }

        /**
         * Decodifica um ou mais pacotes MQTT contidos em um frame binário WebSocket
         * e retorna a lista de payloads JSON `PUBLISH` recebidos.
         */
        fun extractPublishedPayloads(data: ByteArray): List<String> {
            val results = mutableListOf<String>()
            var offset = 0
            while (offset < data.size) {
                val header = data[offset].toInt() and 0xFF
                val packetType = header and 0xF0
                val qos = (header ushr 1) and 0x03
                offset++

                var multiplier = 1
                var remainingLength = 0
                var encodedByte: Int
                var bytesRead = 0
                do {
                    if (offset >= data.size) return results
                    encodedByte = data[offset].toInt() and 0xFF
                    offset++
                    remainingLength += (encodedByte and 127) * multiplier
                    multiplier *= 128
                    bytesRead++
                } while ((encodedByte and 128) != 0 && bytesRead < 4)

                val packetEnd = (offset + remainingLength).coerceAtMost(data.size)
                if (packetType == 0x30 && offset + 2 <= packetEnd) {
                    val topicLen = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
                    var payloadStart = offset + 2 + topicLen
                    if (qos > 0) {
                        payloadStart += 2 // Packet Identifier presente em QoS 1/2
                    }
                    if (payloadStart <= packetEnd) {
                        val payloadStr = String(
                            data,
                            payloadStart,
                            packetEnd - payloadStart,
                            StandardCharsets.UTF_8
                        )
                        if (payloadStr.isNotBlank()) {
                            results.add(payloadStr)
                        }
                    }
                }
                offset = packetEnd
            }
            return results
        }
    }

    private var webSocket: WebSocket? = null
    private var keepAliveJob: Job? = null
    private var reconnectJob: Job? = null
    private var currentRelayIndex = 0
    @Volatile
    private var isStopped = false
    @Volatile
    private var isMqttReady = false

    private val roomTopic = topicForSession(sessionId)

    fun connect(preferredUrl: String? = null) {
        isStopped = false
        isMqttReady = false
        reconnectJob?.cancel()

        val relayUrl = if (!preferredUrl.isNullOrBlank() &&
            (preferredUrl.startsWith("wss://", ignoreCase = true) || preferredUrl.startsWith("ws://", ignoreCase = true)) &&
            !preferredUrl.startsWith("global://", ignoreCase = true)
        ) {
            preferredUrl
        } else {
            GLOBAL_WSS_RELAYS[currentRelayIndex % GLOBAL_WSS_RELAYS.size]
        }

        val request = try {
            Request.Builder()
                .url(relayUrl)
                .header("Sec-WebSocket-Protocol", "mqtt")
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "URL de relay global inválida: $relayUrl", e)
            scheduleFailoverReconnect("URL inválida")
            return
        }

        val clientId = "nerf_${sessionId.replace("[^A-Za-z0-9]".toRegex(), "")}_${localPeerId.takeLast(6)}_${System.currentTimeMillis() % 10000}"

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (isStopped) {
                    webSocket.close(1000, "Stopped")
                    return
                }
                Log.i(TAG, "WebSocket Global aberto em $relayUrl (protocol=${response.header("Sec-WebSocket-Protocol")}). Enviando CONNECT + SUBSCRIBE para $roomTopic")
                webSocket.send(buildConnectPacket(clientId))
                webSocket.send(buildSubscribePacket(1, roomTopic))
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (isStopped) return
                val raw = bytes.toByteArray()
                if (raw.isEmpty()) return

                val firstPacketType = raw[0].toInt() and 0xF0
                // 0x20 = CONNACK, 0x90 = SUBACK
                if ((firstPacketType == 0x20 || firstPacketType == 0x90) && !isMqttReady) {
                    isMqttReady = true
                    startMqttKeepAlive(webSocket)
                    scope.launch {
                        onConnected(relayUrl)
                    }
                }

                val payloads = extractPublishedPayloads(raw)
                if (payloads.isNotEmpty()) {
                    scope.launch {
                        for (payload in payloads) {
                            onMessageReceived(payload)
                        }
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (isStopped || text.isBlank()) return
                scope.launch {
                    onMessageReceived(text)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isMqttReady = false
                keepAliveJob?.cancel()
                if (!isStopped) {
                    scheduleFailoverReconnect("Conexão encerrada ($code)")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isMqttReady = false
                keepAliveJob?.cancel()
                if (!isStopped) {
                    Log.w(TAG, "Falha no relay global $relayUrl: ${t.message}. Alternando para próximo cluster...")
                    currentRelayIndex = (currentRelayIndex + 1) % GLOBAL_WSS_RELAYS.size
                    scheduleFailoverReconnect(t.localizedMessage ?: "Falha de rede")
                }
            }
        })
    }

    fun send(jsonPayload: String, retain: Boolean = false) {
        val ws = webSocket ?: return
        if (isStopped) return
        ws.send(buildPublishPacket(roomTopic, jsonPayload, retain = retain))
    }

    private fun startMqttKeepAlive(ws: WebSocket) {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive && !isStopped) {
                delay(20_000L)
                try {
                    ws.send(buildPingReqPacket())
                } catch (_: Exception) {}
            }
        }
    }

    private fun scheduleFailoverReconnect(reason: String) {
        if (isStopped) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            onReconnecting(reason)
            delay(1_800L)
            if (!isStopped) {
                connect(null)
            }
        }
    }

    fun close() {
        isStopped = true
        isMqttReady = false
        keepAliveJob?.cancel()
        reconnectJob?.cancel()
        try {
            // MQTT DISCONNECT packet (0xE0 0x00)
            webSocket?.send(byteArrayOf(0xE0.toByte(), 0x00.toByte()).toByteString())
            webSocket?.close(1000, "JAM ended")
        } catch (_: Exception) {}
        webSocket = null
    }
}
