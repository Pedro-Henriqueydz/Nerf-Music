package com.nerf.music

import com.nerf.music.data.repository.AudioQuality
import com.nerf.music.player.InnerTubeStreamResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Empirical stress tests for stream quality selector sorting logic.
 * Specifically validates:
 * 1. Prioritization of itag 251 (Opus) over itag 140 (AAC) when both are present at comparable bitrates.
 * 2. Order invariance (itag 140 appearing before itag 251 in the response array).
 * 3. Itag 251 prioritized even when itag 140 has a higher nominal bitrate.
 * 4. Fallback to other Opus formats when itag 251 is absent.
 * 5. Fallback to highest bitrate AAC when no Opus formats exist.
 * 6. LOW quality selecting the lowest bitrate stream.
 * 7. NORMAL quality selecting the stream closest to 128 kbps.
 * 8. Filtering out cipher-locked formats.
 */
class StreamQualitySelectorStressTest {

    private lateinit var resolver: InnerTubeStreamResolver

    @Before
    fun setUp() {
        resolver = InnerTubeStreamResolver(context = null)
    }

    private fun buildPlayerResponse(adaptiveFormatsJson: String, formatsJson: String = "[]"): String {
        return """
        {
          "playabilityStatus": {
            "status": "OK"
          },
          "streamingData": {
            "adaptiveFormats": $adaptiveFormatsJson,
            "formats": $formatsJson
          }
        }
        """.trimIndent()
    }

    @Test
    fun testItag251PrioritizedOverItag140AtComparableBitrates() {
        val formats = """
        [
          {
            "itag": 140,
            "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
            "bitrate": 130677,
            "url": "https://example.com/aac_itag140.m4a"
          },
          {
            "itag": 251,
            "mimeType": "audio/webm; codecs=\"opus\"",
            "bitrate": 136544,
            "url": "https://example.com/opus_itag251.webm"
          }
        ]
        """.trimIndent()

        val json = buildPlayerResponse(formats)

        // HIGH should pick itag 251
        val highResult = resolver.extractAudioUrlFromPlayerResponse(json, "TEST", AudioQuality.HIGH)
        assertEquals("HIGH deve priorizar Opus itag 251", "https://example.com/opus_itag251.webm", highResult)

        // VERY_HIGH should pick itag 251
        val veryHighResult = resolver.extractAudioUrlFromPlayerResponse(json, "TEST", AudioQuality.VERY_HIGH)
        assertEquals("VERY_HIGH deve priorizar Opus itag 251", "https://example.com/opus_itag251.webm", veryHighResult)
    }

    @Test
    fun testItag251PrioritizedEvenWhenItag140HasHigherNominalBitrate() {
        // Here AAC has 160 kbps, while Opus has 136 kbps.
        // Opus at 136 kbps delivers superior acoustic fidelity than AAC at 160 kbps.
        val formats = """
        [
          {
            "itag": 140,
            "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
            "bitrate": 160000,
            "url": "https://example.com/aac_160k.m4a"
          },
          {
            "itag": 251,
            "mimeType": "audio/webm; codecs=\"opus\"",
            "bitrate": 136000,
            "url": "https://example.com/opus_itag251_136k.webm"
          }
        ]
        """.trimIndent()

        val json = buildPlayerResponse(formats)

        val highResult = resolver.extractAudioUrlFromPlayerResponse(json, "TEST", AudioQuality.HIGH)
        assertEquals(
            "HIGH deve selecionar itag 251 mesmo que AAC possua bitrate nominal numericamente superior",
            "https://example.com/opus_itag251_136k.webm",
            highResult
        )

        val veryHighResult = resolver.extractAudioUrlFromPlayerResponse(json, "TEST", AudioQuality.VERY_HIGH)
        assertEquals(
            "VERY_HIGH deve selecionar itag 251 mesmo que AAC possua bitrate nominal superior",
            "https://example.com/opus_itag251_136k.webm",
            veryHighResult
        )
    }

    @Test
    fun testOrderInvarianceWhenItag251AppearsFirstOrSecond() {
        val formatsFirst = """
        [
          {
            "itag": 251,
            "mimeType": "audio/webm; codecs=\"opus\"",
            "bitrate": 136000,
            "url": "https://example.com/opus_first.webm"
          },
          {
            "itag": 140,
            "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
            "bitrate": 130000,
            "url": "https://example.com/aac_second.m4a"
          }
        ]
        """.trimIndent()

        val formatsSecond = """
        [
          {
            "itag": 140,
            "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
            "bitrate": 130000,
            "url": "https://example.com/aac_first.m4a"
          },
          {
            "itag": 251,
            "mimeType": "audio/webm; codecs=\"opus\"",
            "bitrate": 136000,
            "url": "https://example.com/opus_second.webm"
          }
        ]
        """.trimIndent()

        val res1 = resolver.extractAudioUrlFromPlayerResponse(buildPlayerResponse(formatsFirst), "TEST", AudioQuality.HIGH)
        val res2 = resolver.extractAudioUrlFromPlayerResponse(buildPlayerResponse(formatsSecond), "TEST", AudioQuality.HIGH)

        assertEquals("https://example.com/opus_first.webm", res1)
        assertEquals("https://example.com/opus_second.webm", res2)
    }

    @Test
    fun testFallbackToOtherOpusFormatsWhenItag251Absent() {
        // itag 251 absent; itag 250 (Opus ~70k), itag 249 (Opus ~49k), and itag 140 (AAC ~130k)
        val formats = """
        [
          {
            "itag": 140,
            "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
            "bitrate": 130000,
            "url": "https://example.com/aac_140.m4a"
          },
          {
            "itag": 249,
            "mimeType": "audio/webm; codecs=\"opus\"",
            "bitrate": 49000,
            "url": "https://example.com/opus_249.webm"
          },
          {
            "itag": 250,
            "mimeType": "audio/webm; codecs=\"opus\"",
            "bitrate": 70000,
            "url": "https://example.com/opus_250.webm"
          }
        ]
        """.trimIndent()

        val json = buildPlayerResponse(formats)
        val result = resolver.extractAudioUrlFromPlayerResponse(json, "TEST", AudioQuality.HIGH)

        // When itag 251 is missing, it should pick the highest bitrate Opus stream (itag 250) over AAC
        assertEquals("Deve selecionar o melhor stream Opus disponível quando itag 251 ausente", "https://example.com/opus_250.webm", result)
    }

    @Test
    fun testFallbackToHighestAacWhenNoOpusFormatsAvailable() {
        val formats = """
        [
          {
            "itag": 139,
            "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
            "bitrate": 48000,
            "url": "https://example.com/aac_low.m4a"
          },
          {
            "itag": 140,
            "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
            "bitrate": 130000,
            "url": "https://example.com/aac_mid.m4a"
          }
        ]
        """.trimIndent()

        val json = buildPlayerResponse(formats)
        val result = resolver.extractAudioUrlFromPlayerResponse(json, "TEST", AudioQuality.HIGH)

        assertEquals("Quando não houver Opus, deve selecionar o formato com maior bitrate disponível", "https://example.com/aac_mid.m4a", result)
    }

    @Test
    fun testLowQualitySelectsLowestBitrate() {
        val formats = """
        [
          {
            "itag": 251,
            "mimeType": "audio/webm; codecs=\"opus\"",
            "bitrate": 136000,
            "url": "https://example.com/high.webm"
          },
          {
            "itag": 140,
            "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
            "bitrate": 130000,
            "url": "https://example.com/mid.m4a"
          },
          {
            "itag": 249,
            "mimeType": "audio/webm; codecs=\"opus\"",
            "bitrate": 49000,
            "url": "https://example.com/low.webm"
          }
        ]
        """.trimIndent()

        val json = buildPlayerResponse(formats)
        val result = resolver.extractAudioUrlFromPlayerResponse(json, "TEST", AudioQuality.LOW)

        assertEquals("LOW deve escolher o menor bitrate", "https://example.com/low.webm", result)
    }

    @Test
    fun testNormalQualitySelectsFormatClosestTo128kbps() {
        val formats = """
        [
          {
            "itag": 249,
            "mimeType": "audio/webm; codecs=\"opus\"",
            "bitrate": 49000,
            "url": "https://example.com/low.webm"
          },
          {
            "itag": 140,
            "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
            "bitrate": 129000,
            "url": "https://example.com/aac_129k.m4a"
          },
          {
            "itag": 251,
            "mimeType": "audio/webm; codecs=\"opus\"",
            "bitrate": 160000,
            "url": "https://example.com/high_160k.webm"
          }
        ]
        """.trimIndent()

        val json = buildPlayerResponse(formats)
        val result = resolver.extractAudioUrlFromPlayerResponse(json, "TEST", AudioQuality.NORMAL)

        // 129000 is 1000 away from 128000; 160000 is 32000 away; 49000 is 79000 away
        assertEquals("NORMAL deve selecionar o formato mais próximo de 128 kbps", "https://example.com/aac_129k.m4a", result)
    }

    @Test
    fun testCipherFormatsAreFilteredOutEvenForItag251() {
        val formats = """
        [
          {
            "itag": 251,
            "mimeType": "audio/webm; codecs=\"opus\"",
            "bitrate": 136000,
            "signatureCipher": "s=abc&sp=sig&url=https%3A%2F%2Fcipher.com"
          },
          {
            "itag": 140,
            "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
            "bitrate": 130000,
            "url": "https://example.com/direct_aac.m4a"
          }
        ]
        """.trimIndent()

        val json = buildPlayerResponse(formats)
        val result = resolver.extractAudioUrlFromPlayerResponse(json, "TEST", AudioQuality.HIGH)

        assertEquals(
            "Formatos protegidos por Cipher não devem ser selecionados; deve haver fallback para URL direta",
            "https://example.com/direct_aac.m4a",
            result
        )
    }

    @Test
    fun testProgressiveFormatsFallbackWhenAdaptiveEmpty() {
        val adaptive = "[]"
        val progressive = """
        [
          {
            "itag": 18,
            "mimeType": "video/mp4; codecs=\"avc1.42001E, mp4a.40.2\"",
            "bitrate": 500000,
            "url": "https://example.com/progressive_video.mp4"
          }
        ]
        """.trimIndent()

        val json = buildPlayerResponse(adaptive, progressive)
        val result = resolver.extractAudioUrlFromPlayerResponse(json, "TEST", AudioQuality.HIGH)

        assertEquals("https://example.com/progressive_video.mp4", result)
    }

    @Test
    fun testUnplayableStatusReturnsNull() {
        val json = """
        {
          "playabilityStatus": {
            "status": "LOGIN_REQUIRED",
            "reason": "This video is private"
          }
        }
        """.trimIndent()

        val result = resolver.extractAudioUrlFromPlayerResponse(json, "TEST", AudioQuality.HIGH)
        assertNull("Status não OK deve retornar nulo", result)
    }
}
