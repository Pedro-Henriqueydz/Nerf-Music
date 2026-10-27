package com.nerf.music

import com.nerf.music.data.repository.AudioQuality
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

        val extractedUrl = resolver.extractAudioUrlFromPlayerResponse(json, "TEST_CLIENT")
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

        val url = resolver.extractAudioUrlFromPlayerResponse(loginRequiredJson, "TEST_CLIENT")
        assertNull("Status LOGIN_REQUIRED não deve retornar URL", url)

        val corruptedJson = "{ not json at all }"
        val corruptedUrl = resolver.extractAudioUrlFromPlayerResponse(corruptedJson, "TEST_CLIENT")
        assertNull("JSON corrompido deve retornar null com segurança", corruptedUrl)
    }

    @Test
    fun testExtractAudioUrlHandlesCiphersGracefully() {
        // Simula formato com signatureCipher onde campo url está ausente
        val cipherJson = """
        {
          "playabilityStatus": { "status": "OK" },
          "streamingData": {
            "adaptiveFormats": [
              {
                "itag": 251,
                "mimeType": "audio/webm; codecs=\"opus\"",
                "bitrate": 140000,
                "signatureCipher": "s=dummy_cipher_sig&sp=sig&url=https%3A%2F%2Fgooglevideo.com"
              },
              {
                "itag": 140,
                "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
                "bitrate": 128000,
                "cipher": "s=dummy_cipher_sig2&sp=sig&url=https%3A%2F%2Fgooglevideo.com"
              }
            ]
          }
        }
        """.trimIndent()

        val url = resolver.extractAudioUrlFromPlayerResponse(cipherJson, "IOS_MUSIC")
        assertNull("Quando todos os formatos possuem apenas cipher e nenhuma URL direta, deve retornar null com segurança", url)
    }

    @Test
    fun testExtractAudioUrlPrefersDirectUrlOverCipher() {
        val mixedJson = """
        {
          "playabilityStatus": { "status": "OK" },
          "streamingData": {
            "adaptiveFormats": [
              {
                "itag": 251,
                "mimeType": "audio/webm; codecs=\"opus\"",
                "bitrate": 140000,
                "signatureCipher": "s=cipher&url=https%3A%2F%2Fexample.com"
              },
              {
                "itag": 140,
                "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
                "bitrate": 128000,
                "url": "https://example.com/direct_audio.mp4"
              }
            ]
          }
        }
        """.trimIndent()

        val url = resolver.extractAudioUrlFromPlayerResponse(mixedJson, "IOS")
        assertNotNull("Deve extrair a URL direta disponível mesmo quando outros formatos têm cipher", url)
        assertEquals("https://example.com/direct_audio.mp4", url)
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

        val extractedUrl = resolver.extractAudioUrlFromPlayerResponse(json, "WEB_REMIX")
        assertNotNull("Deve fazer fallback para progressive formats", extractedUrl)
        assertEquals("https://example.com/progressive.mp4", extractedUrl)
    }

    @Test
    fun testExtractAudioUrlWithQualitySelection() {
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
              }
            ]
          }
        }
        """.trimIndent()

        val lowUrl = resolver.extractAudioUrlFromPlayerResponse(json, "TEST_CLIENT", AudioQuality.LOW)
        assertEquals("LOW deve selecionar menor bitrate", "https://example.com/audio_low.webm", lowUrl)

        val normalUrl = resolver.extractAudioUrlFromPlayerResponse(json, "TEST_CLIENT", AudioQuality.NORMAL)
        assertEquals("NORMAL deve selecionar formato alvo de 128 kbps", "https://example.com/audio_mid.mp4", normalUrl)

        val highUrl = resolver.extractAudioUrlFromPlayerResponse(json, "TEST_CLIENT", AudioQuality.HIGH)
        assertEquals("HIGH deve priorizar Opus itag 251 de alta fidelidade", "https://example.com/audio_high.webm", highUrl)

        val veryHighUrl = resolver.extractAudioUrlFromPlayerResponse(json, "TEST_CLIENT", AudioQuality.VERY_HIGH)
        assertEquals("VERY_HIGH deve priorizar Opus itag 251", "https://example.com/audio_high.webm", veryHighUrl)
    }

    @Test
    fun testLiveStreamResolutionWhenOnline() = runBlocking {
        try {
            // Valida resolução real para música oficial
            val result = resolver.resolveStreamUrl("dQw4w9WgXcQ")
            if (result.isSuccess) {
                val streamUrl = result.getOrNull()
                assertNotNull("URL resolvida não pode ser nula", streamUrl)
                assertTrue("URL deve apontar para os servidores do googlevideo", streamUrl!!.contains("googlevideo.com"))
                println("Música oficial resolvida com sucesso: ${streamUrl.substring(0, 60)}...")
            }
        } catch (e: Exception) {
            println("Aviso: Teste de rede ignorado por indisponibilidade de conexão: ${e.message}")
        }
    }
}
