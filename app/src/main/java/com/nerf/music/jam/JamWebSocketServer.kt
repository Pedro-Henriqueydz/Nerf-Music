package com.nerf.music.jam

import android.util.Log
import java.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Servidor WebSocket RFC 6455 embarcado leve para sessões JAM No-Account P2P / LAN / Hotspot.
 * Permite que o dispositivo Host hospede a sala JAM diretamente sem depender de servidores externos.
 */
class JamWebSocketServer(
    private val onClientConnected: (clientId: String) -> Unit,
    private val onClientMessage: (clientId: String, text: String) -> Unit,
    private val onClientDisconnected: (clientId: String) -> Unit
) {
    companion object {
        private const val TAG = "JamWebSocketServer"
        private const val WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        const val DEFAULT_PORT = 8765

        /**
         * Detecta o endereço IPv4 local (Wi-Fi / Hotspot / LAN) do dispositivo.
         */
        fun resolveLocalIpAddress(): String {
            return try {
                val candidates = mutableListOf<String>()
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces != null && interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (!iface.isUp || iface.isLoopback) continue
                    val name = iface.name.lowercase()
                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val ip = addr.hostAddress ?: continue
                            // Prioriza interfaces Wi-Fi / Tether (wlan, ap, rndis, swlan)
                            if (name.startsWith("wlan") || name.startsWith("ap") || name.startsWith("swlan")) {
                                candidates.add(0, ip)
                            } else {
                                candidates.add(ip)
                            }
                        }
                    }
                }
                candidates.firstOrNull() ?: "127.0.0.1"
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao detectar IP local: ${e.message}")
                "127.0.0.1"
            }
        }
    }

    private var serverSocket: ServerSocket? = null
    private var serverScope: CoroutineScope? = null
    private var acceptJob: Job? = null
    private val clients = ConcurrentHashMap<String, ClientConnection>()
    private val clientCounter = AtomicInteger(0)

    var boundPort: Int = DEFAULT_PORT
        private set

    val isRunning: Boolean
        get() = serverSocket != null && serverSocket?.isClosed == false

    fun start(preferredPort: Int = DEFAULT_PORT): Int {
        stop()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        serverScope = scope

        val socket = try {
            ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress("0.0.0.0", preferredPort))
            }
        } catch (_: Exception) {
            ServerSocket(0).apply { reuseAddress = true }
        }

        serverSocket = socket
        boundPort = socket.localPort
        try {
            Log.i(TAG, "Servidor WebSocket JAM iniciado na porta $boundPort (IP=${resolveLocalIpAddress()})")
        } catch (_: Throwable) {}

        acceptJob = scope.launch {
            while (isActive && !socket.isClosed) {
                try {
                    val clientSocket = socket.accept()
                    clientSocket.tcpNoDelay = true
                    val clientId = "client_${clientCounter.incrementAndGet()}"
                    launch {
                        handleClientSocket(clientId, clientSocket)
                    }
                } catch (e: Exception) {
                    if (isActive && !socket.isClosed) {
                        try {
                            Log.w(TAG, "Erro ao aceitar conexão no servidor JAM: ${e.message}")
                        } catch (_: Throwable) {}
                    }
                }
            }
        }
        return boundPort
    }

    fun broadcast(text: String, excludeClientId: String? = null) {
        val payload = text.toByteArray(Charsets.UTF_8)
        val frame = buildServerFrame(opcode = 0x1, payload = payload)
        clients.forEach { (id, conn) ->
            if (id != excludeClientId) {
                conn.sendRawFrame(frame)
            }
        }
    }

    fun sendToClient(clientId: String, text: String) {
        val conn = clients[clientId] ?: return
        val payload = text.toByteArray(Charsets.UTF_8)
        val frame = buildServerFrame(opcode = 0x1, payload = payload)
        conn.sendRawFrame(frame)
    }

    fun stop() {
        try {
            clients.values.forEach { it.closeQuietly() }
            clients.clear()
        } catch (_: Exception) {}
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        acceptJob?.cancel()
        serverScope?.cancel()
        serverScope = null
    }

    private fun handleClientSocket(clientId: String, socket: Socket) {
        try {
            val input = BufferedInputStream(socket.getInputStream())
            val output = BufferedOutputStream(socket.getOutputStream())

            if (!performHandshake(input, output)) {
                socket.close()
                return
            }

            val connection = ClientConnection(clientId, socket, output)
            clients[clientId] = connection
            onClientConnected(clientId)

            while (!socket.isClosed) {
                val frame = readClientFrame(input) ?: break
                when (frame.opcode) {
                    0x1 -> { // Text Frame
                        val text = String(frame.payload, Charsets.UTF_8)
                        onClientMessage(clientId, text)
                    }
                    0x8 -> { // Close Frame
                        break
                    }
                    0x9 -> { // Ping -> responde Pong
                        val pong = buildServerFrame(opcode = 0xA, payload = frame.payload)
                        connection.sendRawFrame(pong)
                    }
                    0xA -> { // Pong
                        // No-op
                    }
                }
            }
        } catch (e: Exception) {
            try {
                Log.d(TAG, "Conexão encerrada para $clientId: ${e.message}")
            } catch (_: Throwable) {}
        } finally {
            clients.remove(clientId)?.closeQuietly()
            onClientDisconnected(clientId)
        }
    }

    private fun performHandshake(input: InputStream, output: OutputStream): Boolean {
        val headerBuilder = StringBuilder()
        var prev3 = 0
        var prev2 = 0
        var prev1 = 0
        while (true) {
            val b = input.read()
            if (b == -1) return false
            headerBuilder.append(b.toChar())
            if (prev3 == '\r'.code && prev2 == '\n'.code && prev1 == '\r'.code && b == '\n'.code) {
                break
            }
            if (headerBuilder.length > 8192) return false
            prev3 = prev2
            prev2 = prev1
            prev1 = b
        }

        val headersText = headerBuilder.toString()
        var secWebSocketKey: String? = null
        headersText.lines().forEach { line ->
            if (line.startsWith("Sec-WebSocket-Key:", ignoreCase = true)) {
                secWebSocketKey = line.substringAfter(":").trim()
            }
        }
        val key = secWebSocketKey ?: return false
        val acceptRaw = MessageDigest.getInstance("SHA-1")
            .digest((key + WS_GUID).toByteArray(Charsets.US_ASCII))
        val acceptKey = Base64.getEncoder().encodeToString(acceptRaw)

        val response = buildString {
            append("HTTP/1.1 101 Switching Protocols\r\n")
            append("Upgrade: websocket\r\n")
            append("Connection: Upgrade\r\n")
            append("Sec-WebSocket-Accept: $acceptKey\r\n")
            append("\r\n")
        }
        output.write(response.toByteArray(Charsets.US_ASCII))
        output.flush()
        return true
    }

    private data class WsFrame(val opcode: Int, val payload: ByteArray)

    private fun readClientFrame(input: InputStream): WsFrame? {
        val b0 = input.read()
        if (b0 == -1) return null
        val b1 = input.read()
        if (b1 == -1) return null

        val opcode = b0 and 0x0F
        val isMasked = (b1 and 0x80) != 0
        var payloadLen = (b1 and 0x7F).toLong()

        if (payloadLen == 126L) {
            val hi = input.read()
            val lo = input.read()
            if (hi == -1 || lo == -1) return null
            payloadLen = ((hi and 0xFF) shl 8 or (lo and 0xFF)).toLong()
        } else if (payloadLen == 127L) {
            var len = 0L
            for (i in 0 until 8) {
                val b = input.read()
                if (b == -1) return null
                len = (len shl 8) or (b and 0xFF).toLong()
            }
            payloadLen = len
        }

        if (payloadLen < 0L || payloadLen > 2_000_000L) return null

        val maskKey = ByteArray(4)
        if (isMasked) {
            readFully(input, maskKey)
        }

        val payload = ByteArray(payloadLen.toInt())
        readFully(input, payload)

        if (isMasked) {
            for (i in payload.indices) {
                payload[i] = (payload[i].toInt() xor maskKey[i % 4].toInt()).toByte()
            }
        }

        return WsFrame(opcode = opcode, payload = payload)
    }

    private fun readFully(input: InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read == -1) throw EOFException("Fim inesperado do stream WebSocket")
            offset += read
        }
    }

    private fun buildServerFrame(opcode: Int, payload: ByteArray): ByteArray {
        val len = payload.size
        val headerSize = when {
            len <= 125 -> 2
            len <= 65535 -> 4
            else -> 10
        }
        val frame = ByteArray(headerSize + len)
        frame[0] = (0x80 or (opcode and 0x0F)).toByte() // FIN = 1
        when {
            len <= 125 -> {
                frame[1] = len.toByte()
            }
            len <= 65535 -> {
                frame[1] = 126.toByte()
                frame[2] = ((len ushr 8) and 0xFF).toByte()
                frame[3] = (len and 0xFF).toByte()
            }
            else -> {
                frame[1] = 127.toByte()
                val longLen = len.toLong()
                for (i in 0 until 8) {
                    frame[2 + i] = ((longLen ushr ((7 - i) * 8)) and 0xFF).toByte()
                }
            }
        }
        System.arraycopy(payload, 0, frame, headerSize, len)
        return frame
    }

    private class ClientConnection(
        val clientId: String,
        private val socket: Socket,
        private val output: OutputStream
    ) {
        private val writeLock = Any()

        fun sendRawFrame(frame: ByteArray) {
            synchronized(writeLock) {
                try {
                    if (!socket.isClosed) {
                        output.write(frame)
                        output.flush()
                    }
                } catch (_: Exception) {}
            }
        }

        fun closeQuietly() {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }
}
