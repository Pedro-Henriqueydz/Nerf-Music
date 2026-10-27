package com.nerf.music

import com.nerf.music.data.model.LyricsData
import com.nerf.music.jam.JamCommandType
import com.nerf.music.jam.JamMessageType
import com.nerf.music.jam.JamWebSocketServer
import com.nerf.music.jam.JamWireMessage
import com.nerf.music.player.KaraokeLyricsEngine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class KaraokeAndJamTest {

    @Test
    fun `parseLrcWithKaraokeTiming parses enhanced LRC and synthesizes word timings`() {
        val rawLrc = """
            [offset:100]
            [00:05.00]Hello world from Nerf Music
            [00:09.50]<00:09.50>Sing <00:10.20>along <00:11.00>now
        """.trimIndent()

        val lines = KaraokeLyricsEngine.parseLrcWithKaraokeTiming(rawLrc)
        assertEquals(2, lines.size)

        // Offset +100ms applied to 5000ms -> 4900ms (standard LRC offset subtraction)
        assertEquals(4900L, lines[0].timeMs)
        assertEquals(5, lines[0].words.size)
        assertEquals("Hello", lines[0].words[0].text)
        assertEquals("Music", lines[0].words[4].text)
        assertTrue(lines[0].words[0].endTimeMs > lines[0].words[0].startTimeMs)

        // Second line has explicit enhanced LRC word tags
        assertEquals(3, lines[1].words.size)
        assertEquals("Sing", lines[1].words[0].text)
        assertEquals("along", lines[1].words[1].text)
        assertEquals("now", lines[1].words[2].text)
    }

    @Test
    fun `resolveSyncSnapshot computes active line, active word, and instrumental countdown`() {
        val lines = KaraokeLyricsEngine.parseLrcWithKaraokeTiming(
            """
            [00:06.00]First karaoke line here
            [00:10.00]Second karaoke line here
            """.trimIndent()
        )
        val lyrics = LyricsData(
            trackId = "test-id",
            plainLyrics = "First karaoke line here\nSecond karaoke line here",
            syncedLines = lines,
            isSynced = true
        )

        // Before first line (at 4.0s, 2.0s before 6.0s): instrumental countdown should be active
        val introSnap = KaraokeLyricsEngine.resolveSyncSnapshot(lyrics, rawPositionMs = 4000L)
        assertEquals(-1, introSnap.activeLineIndex)
        assertTrue(introSnap.isInstrumentalCountdown)
        assertTrue(introSnap.countdownDots in 1..3)

        // Mid-way through first line (at 7.5s)
        val midSnap = KaraokeLyricsEngine.resolveSyncSnapshot(lyrics, rawPositionMs = 7500L)
        assertEquals(0, midSnap.activeLineIndex)
        assertTrue(midSnap.activeWordIndex in 0..3)
        assertTrue(midSnap.lineProgress > 0.2f && midSnap.lineProgress < 0.9f)
    }

    @Test
    fun `generateLocalPronunciation creates phonetic sing-along guide`() {
        val pron = KaraokeLyricsEngine.generateLocalPronunciation("Tonight through the light")
        assertNotNull(pron)
        assertTrue(pron!!.contains("trú"))
        assertTrue(pron.contains("lait"))
    }

    @Test
    fun `JamWebSocketServer performs RFC6455 handshake and bidirectional message exchange`() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val serverReceivedLatch = CountDownLatch(1)
        val clientReceivedLatch = CountDownLatch(1)
        var serverReceivedText: String? = null
        var clientReceivedText: String? = null

        val server = JamWebSocketServer(
            onClientConnected = {},
            onClientMessage = { _, text ->
                serverReceivedText = text
                serverReceivedLatch.countDown()
            },
            onClientDisconnected = {}
        )

        val port = server.start(0)
        assertTrue("Server port should be > 0", port > 0)

        val client = OkHttpClient.Builder()
            .pingInterval(0, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("ws://127.0.0.1:$port/jam")
            .build()

        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val joinMsg = JamWireMessage(
                    type = JamMessageType.JOIN,
                    sessionId = "TEST01",
                    senderId = "guest-1",
                    senderName = "Convidado Teste"
                )
                webSocket.send(json.encodeToString(JamWireMessage.serializer(), joinMsg))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                clientReceivedText = text
                clientReceivedLatch.countDown()
            }
        })

        try {
            assertTrue("Server should receive JOIN from Guest", serverReceivedLatch.await(5, TimeUnit.SECONDS))
            val parsedFromGuest = json.decodeFromString(JamWireMessage.serializer(), serverReceivedText ?: "")
            assertNotNull(parsedFromGuest)
            assertEquals(JamMessageType.JOIN, parsedFromGuest.type)
            assertEquals("Convidado Teste", parsedFromGuest.senderName)

            // Broadcast command from Host to Guest
            val hostBroadcast = JamWireMessage(
                type = JamMessageType.COMMAND,
                sessionId = "TEST01",
                senderId = "host-1",
                command = JamCommandType.PLAY
            )
            server.broadcast(json.encodeToString(JamWireMessage.serializer(), hostBroadcast))

            assertTrue("Client should receive broadcast from Host", clientReceivedLatch.await(5, TimeUnit.SECONDS))
            val parsedFromHost = json.decodeFromString(JamWireMessage.serializer(), clientReceivedText ?: "")
            assertNotNull(parsedFromHost)
            assertEquals(JamMessageType.COMMAND, parsedFromHost.type)
            assertEquals(JamCommandType.PLAY, parsedFromHost.command)
        } finally {
            ws.close(1000, "test done")
            server.stop()
        }
    }

    @Test
    fun `test fallback plain lyrics structured lines with pronunciation and translation languages`() {
        val rawPlain = "Tonight the music seems so loud\n사랑해요\nありがとう"
        val structured = KaraokeLyricsEngine.buildPlainLinesWithPronunciation(rawPlain)
        assertEquals(3, structured.size)
        assertEquals("Tonight the music seems so loud", structured[0].text)
        assertNotNull(structured[0].pronunciationText)
        assertNotNull(structured[1].pronunciationText)
        assertNotNull(structured[2].pronunciationText)

        assertTrue(KaraokeLyricsEngine.SUPPORTED_TRANSLATION_LANGUAGES.any { it.code == "auto" })
        assertTrue(KaraokeLyricsEngine.SUPPORTED_TRANSLATION_LANGUAGES.any { it.code == "pt" })
        assertTrue(KaraokeLyricsEngine.SUPPORTED_TRANSLATION_LANGUAGES.any { it.code == "en" })
    }
}
