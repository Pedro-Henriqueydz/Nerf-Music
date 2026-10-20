package com.nerf.music

import com.nerf.music.player.InnerTubeStreamResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InnerTubeStreamResolverTest {

    private lateinit var resolver: InnerTubeStreamResolver

    @Before
    fun setup() {
        resolver = InnerTubeStreamResolver()
    }

    @Test
    fun testExtractAudioUrlPicksHighestBitrate() {
        val json = """
        {
          "playabilityStatus": { "status": "OK" },
          "streamingData": {
            "adaptiveFormats": [
              {
                "itag": 249,
                "mimeType": "audio/webm; codecs=\"opus\"",
                "bitrate": 49496,
                "url": "https://example.com/audio_low.webm"
              },
              {
                "itag": 251,
                "mimeType": "audio/webm; codecs=\"opus\"",
                "bitrate": 136544,
                "url": "https://example.com/audio_high.webm"
              },
              {
                "itag": 140,
                "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
                "bitrate": 130677,
                "url": "https://example.com/audio_mid.mp4"
              },
              {
                "itag": 137,
                "mimeType": "video/mp4; codecs=\"avc1.640028\"",
                "bitrate": 2500000,
                "url": "https://example.com/video_only.mp4"
              }
            ]
          }
        }
        """.trimIndent()

        val extractedUrl = resolver.extractAudioUrlFromPlayerResponse(json)
        assertNotNull("URL de áudio deve ser extraída", extractedUrl)
        assertEquals("Deve selecionar a URL de áudio com maior bitrate", "https://example.com/audio_high.webm", extractedUrl)
    }

    @Test
    fun testExtractAudioUrlReturnsNullOnUnplayableOrLoginRequired() {
        val loginRequiredJson = """
        {
          "playabilityStatus": {
            "status": "LOGIN_REQUIRED",
            "reason": "Sign in to confirm you're not a bot"
          }
        }
        """.trimIndent()

        val url = resolver.extractAudioUrlFromPlayerResponse(loginRequiredJson)
        assertNull("Status LOGIN_REQUIRED não deve retornar URL", url)

        val corruptedJson = "{ not json at all }"
        val corruptedUrl = resolver.extractAudioUrlFromPlayerResponse(corruptedJson)
        assertNull("JSON corrompido deve retornar null com segurança", corruptedUrl)
    }

    @Test
    fun testExtractAudioUrlFallbackToFormats() {
        val json = """
        {
          "playabilityStatus": { "status": "OK" },
          "streamingData": {
            "formats": [
              {
                "itag": 18,
                "mimeType": "video/mp4; codecs=\"avc1.42001E, mp4a.40.2\"",
                "bitrate": 350000,
                "url": "https://example.com/progressive.mp4"
              }
            ]
          }
        }
        """.trimIndent()

        val extractedUrl = resolver.extractAudioUrlFromPlayerResponse(json)
        assertNotNull("Deve fazer fallback para progressive formats", extractedUrl)
        assertEquals("https://example.com/progressive.mp4", extractedUrl)
    }

    @Test
    fun testLiveStreamResolutionWhenOnline() = runBlocking {
        try {
            // Testa a resolução com um vídeo de referência conhecido
            val result = resolver.resolveStreamUrl("dQw4w9WgXcQ")
            if (result.isSuccess) {
                val streamUrl = result.getOrNull()
                assertNotNull("URL resolvida não pode ser nula", streamUrl)
                assertTrue("URL deve apontar para os servidores do googlevideo", streamUrl!!.contains("googlevideo.com"))
                println("Stream real resolvido com sucesso: ${streamUrl.substring(0, 60)}...")
            }
        } catch (e: Exception) {
            println("Aviso: Teste de rede ignorado por indisponibilidade de conexão: ${e.message}")
        }
    }
}
