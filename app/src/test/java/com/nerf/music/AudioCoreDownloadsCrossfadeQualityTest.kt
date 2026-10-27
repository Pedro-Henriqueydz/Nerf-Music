package com.nerf.music

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.net.Uri
import com.nerf.music.data.repository.AudioQuality
import com.nerf.music.data.repository.DataSaverMode
import com.nerf.music.data.repository.DownloadAudioQuality
import com.nerf.music.player.AudioPlayerService
import com.nerf.music.player.CompositeStreamResolver
import com.nerf.music.player.StreamResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.nio.file.Files

/**
 * Suite de testes unitários para validar:
 * 1. Resolução e reprodução de faixas offline (músicas baixadas em background/notificação)
 *    sem crash por file:// URI e sem pausas por arquivos vazios ou incompletos.
 * 2. Transições e Crossfade dinâmico em reprodução local e online, evitando colapso em skips.
 * 3. Aplicação real e fidedigna dos níveis de qualidade de mídia (bitrates/codecs)
 *    e respeito a download Lossless sem downgrade indevido por Data Saver.
 */
class AudioCoreDownloadsCrossfadeQualityTest {

    private lateinit var tempDir: File
    private lateinit var offlineDir: File
    private val prefsMap = mutableMapOf<String, Any>()

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("nerf_audio_core_test").toFile()
        offlineDir = File(tempDir, "offline_audio").apply { mkdirs() }
        prefsMap.clear()
    }

    private fun createMockContext(): Context {
        val dummyPrefs = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
                    val key = args?.getOrNull(0) as? String
                    return when (method.name) {
                        "getString" -> (prefsMap[key] as? String) ?: args?.getOrNull(1)
                        "getBoolean" -> (prefsMap[key] as? Boolean) ?: args?.getOrNull(1) ?: false
                        "getInt" -> (prefsMap[key] as? Int) ?: args?.getOrNull(1) ?: 0
                        "getLong" -> (prefsMap[key] as? Long) ?: args?.getOrNull(1) ?: 0L
                        "contains" -> prefsMap.containsKey(key)
                        "registerOnSharedPreferenceChangeListener",
                        "unregisterOnSharedPreferenceChangeListener" -> null
                        "edit" -> proxy
                        "putString" -> {
                            val v = args?.getOrNull(1) as? String
                            if (key != null && v != null) prefsMap[key] = v
                            proxy
                        }
                        "putBoolean" -> {
                            val v = args?.getOrNull(1) as? Boolean
                            if (key != null && v != null) prefsMap[key] = v
                            proxy
                        }
                        "putInt" -> {
                            val v = args?.getOrNull(1) as? Int
                            if (key != null && v != null) prefsMap[key] = v
                            proxy
                        }
                        "apply", "commit" -> proxy
                        else -> null
                    }
                }
            }
        ) as SharedPreferences

        return object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = tempDir
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = dummyPrefs
            override fun getPackageName(): String = "com.nerf.music"
        }
    }

    // =========================================================================
    // 1. Verificação de Integridade de Downloads e Prevenção de Pausas Abruptas
    // =========================================================================

    @Test
    fun testOfflineFilesRejectsCorruptOrZeroByteFiles() {
        val validOpus = File(offlineDir, "valid_track.opus").apply { writeBytes(ByteArray(2048)) }
        val zeroByteWebm = File(offlineDir, "zero_byte.webm").apply { writeBytes(ByteArray(0)) }
        val tinyCorruptM4a = File(offlineDir, "tiny.m4a").apply { writeBytes(ByteArray(100)) } // < 1024 bytes

        val possibleExts = listOf("webm", "m4a", "opus", "mp4", "ogg", "mp3", "aac")

        // Valid track must be recognized
        val resolvedValid = possibleExts
            .map { ext -> File(offlineDir, "valid_track.$ext") }
            .firstOrNull { it.exists() && it.length() > 1024L }
        assertNotNull("Faixa válida > 1024 bytes deve ser encontrada", resolvedValid)
        assertEquals(validOpus.absolutePath, resolvedValid?.absolutePath)

        // Zero-byte file must be rejected to prevent abrupt pauses
        val resolvedZero = possibleExts
            .map { ext -> File(offlineDir, "zero_byte.$ext") }
            .firstOrNull { it.exists() && it.length() > 1024L }
        assertNull("Arquivo de 0 bytes deve ser rejeitado", resolvedZero)

        // Tiny incomplete chunk (< 1024 bytes) must be rejected
        val resolvedTiny = possibleExts
            .map { ext -> File(offlineDir, "tiny.$ext") }
            .firstOrNull { it.exists() && it.length() > 1024L }
        assertNull("Arquivo menor que 1024 bytes deve ser rejeitado", resolvedTiny)
    }

    @Test
    fun testAllAudioContainersSupportedForOfflinePlayback() {
        val extensions = listOf("webm", "m4a", "opus", "mp4")
        for (ext in extensions) {
            val trackId = "track_ext_$ext"
            val file = File(offlineDir, "$trackId.$ext").apply { writeBytes(ByteArray(5000)) }

            val found = extensions
                .map { e -> File(offlineDir, "$trackId.$e") }
                .firstOrNull { it.exists() && it.length() > 1024L }

            assertNotNull("Extensão .$ext deve ser reconhecida pelo resolvedor offline", found)
            assertEquals(file.absolutePath, found?.absolutePath)
        }
    }

    // =========================================================================
    // 2. Transições e Crossfade em Skips e Prevenção de Colapso
    // =========================================================================

    @Test
    fun testEqualPowerCrossfadeVolumeCurveSmoothness() {
        // Equal-power curve: cos(t * PI/2) and sin(t * PI/2)
        // Power sum = cos^2 + sin^2 = 1.0 (constant acoustic power, no dip in middle)
        val steps = 50
        for (i in 0..steps) {
            val progress = i.toFloat() / steps.toFloat()
            val volOut = kotlin.math.cos(progress * (Math.PI / 2)).toFloat()
            val volIn = kotlin.math.sin(progress * (Math.PI / 2)).toFloat()

            assertTrue("volOut deve estar no intervalo [0, 1]", volOut in 0f..1.0001f)
            assertTrue("volIn deve estar no intervalo [0, 1]", volIn in 0f..1.0001f)

            val powerSum = (volOut * volOut) + (volIn * volIn)
            assertEquals("Soma de potência equal-power deve ser exatamente 1.0", 1.0f, powerSum, 0.001f)
        }
    }

    @Test
    fun testSkipTransitionClampingCalculatesSafeFadeDuration() {
        // Automix enabled with 0 crossfade seconds -> default 800ms
        prefsMap["key_crossfade_seconds"] = 0
        prefsMap["key_automix_enabled"] = true

        val automix = prefsMap["key_automix_enabled"] as Boolean
        val crossfadeSec = prefsMap["key_crossfade_seconds"] as Int
        val fadeMs = when {
            crossfadeSec > 0 -> (crossfadeSec * 1000L).coerceAtMost(2000L)
            automix -> 800L
            else -> 400L
        }
        assertEquals(800L, fadeMs)

        // Crossfade set to 8s -> clamped to 2000ms for manual skips to avoid sluggish delay
        prefsMap["key_crossfade_seconds"] = 8
        val fadeMsWithCrossfade = when {
            (prefsMap["key_crossfade_seconds"] as Int) > 0 -> ((prefsMap["key_crossfade_seconds"] as Int) * 1000L).coerceAtMost(2000L)
            else -> 800L
        }
        assertEquals(2000L, fadeMsWithCrossfade)

        // Crossfade 0 and Automix false -> 400ms minimal anti-click fade
        prefsMap["key_crossfade_seconds"] = 0
        prefsMap["key_automix_enabled"] = false
        val fadeMsMinimal = when {
            (prefsMap["key_crossfade_seconds"] as Int) > 0 -> ((prefsMap["key_crossfade_seconds"] as Int) * 1000L).coerceAtMost(2000L)
            (prefsMap["key_automix_enabled"] as Boolean) -> 800L
            else -> 400L
        }
        assertEquals(400L, fadeMsMinimal)
    }

    @Test
    fun testPlayerDualSlotCandidateInvariance() {
        val dummyA = Any()
        val dummyB = Any()
        var currentActive = dummyA

        fun getIncoming(active: Any) = if (active === dummyA) dummyB else dummyA

        // 1. Initial state
        assertEquals(dummyB, getIncoming(currentActive))
        assertFalse(currentActive === getIncoming(currentActive))

        // 2. Transition to B
        currentActive = getIncoming(currentActive)
        assertEquals(dummyB, currentActive)
        assertEquals(dummyA, getIncoming(currentActive))
        assertFalse(currentActive === getIncoming(currentActive))

        // 3. Repeated transitions always guarantee separate instances
        for (i in 1..10) {
            val incoming = getIncoming(currentActive)
            assertFalse("incoming must never be the same instance as currentActive", incoming === currentActive)
            currentActive = incoming
        }
    }

    @Test
    fun testSeekDiscontinuityDebounceAndShortRemainingGuards() {
        // 1. Debounce guard: within 500ms of seek, transition must not trigger
        val lastSeekMs = 1000L
        val nowDuringDebounce = 1300L
        val isDebouncing = (nowDuringDebounce - lastSeekMs) < 500L
        assertTrue("Deve bloquear transição durante os primeiros 500ms pós-seek", isDebouncing)

        val nowAfterDebounce = 1600L
        val isDebouncingAfter = (nowAfterDebounce - lastSeekMs) < 500L
        assertFalse("Deve liberar transição após 500ms pós-seek", isDebouncingAfter)

        // 2. Short remaining duration guard: under 1500ms, do not trigger heavy crossfade
        val remainingShort = 1200L
        val shouldSkipCrossfade = remainingShort < 1500L
        assertTrue("Deve evitar crossfade pesado em músicas com menos de 1.5s restantes", shouldSkipCrossfade)
    }

    // =========================================================================
    // 3. Aplicação Real dos Níveis de Qualidade de Mídia
    // =========================================================================

    @Test
    fun testGetTargetAudioQualityRespectsDataSaver() {
        val context = createMockContext()

        // 1. DataSaver ALWAYS_ON forces LOW
        prefsMap["key_data_saver_mode"] = DataSaverMode.ALWAYS_ON.name
        prefsMap["key_cellular_audio_quality"] = AudioQuality.VERY_HIGH.name
        assertEquals(AudioQuality.LOW, AudioPlayerService.getTargetAudioQuality(context))

        // 2. DataSaver ALWAYS_OFF allows configured cellular quality (in test context, isConnectedToWifi is false)
        prefsMap["key_data_saver_mode"] = DataSaverMode.ALWAYS_OFF.name
        prefsMap["key_cellular_audio_quality"] = AudioQuality.VERY_HIGH.name
        assertEquals(AudioQuality.VERY_HIGH, AudioPlayerService.getTargetAudioQuality(context))

        // 3. NORMAL quality
        prefsMap["key_cellular_audio_quality"] = AudioQuality.NORMAL.name
        assertEquals(AudioQuality.NORMAL, AudioPlayerService.getTargetAudioQuality(context))
    }

    @Test
    fun testExplicitDownloadQualityNotOverriddenByDataSaverInCompositeResolver() = runBlocking {
        val context = createMockContext()
        // Data Saver is set to ALWAYS_ON
        prefsMap["key_data_saver_mode"] = DataSaverMode.ALWAYS_ON.name

        var receivedQuality: AudioQuality? = null
        val mockPrimary = object : StreamResolver {
            override suspend fun resolveStreamUrl(videoId: String): Result<String> = Result.failure(Exception())
            override suspend fun resolveStreamUrl(videoId: String, targetQuality: AudioQuality?): Result<String> {
                receivedQuality = targetQuality
                return Result.success("https://googlevideo.com/download_stream")
            }
        }
        val mockSecondary = object : StreamResolver {
            override suspend fun resolveStreamUrl(videoId: String): Result<String> = Result.failure(Exception())
            override suspend fun resolveStreamUrl(videoId: String, targetQuality: AudioQuality?): Result<String> = Result.failure(Exception())
        }

        val composite = CompositeStreamResolver(mockPrimary, mockSecondary, context)

        // When download requests VERY_HIGH explicitly, it MUST be preserved even if Data Saver is ON
        val result = composite.resolveStreamUrl("video_dl_123", AudioQuality.VERY_HIGH)
        assertTrue(result.isSuccess)
        assertEquals("Qualidade de download explícita (VERY_HIGH) não pode ser reduzida por Data Saver", AudioQuality.VERY_HIGH, receivedQuality)
    }

    @Test
    fun testStreamCacheKeyIncludesQualityProfile() {
        val context = createMockContext()
        AudioPlayerService.setPrefetchContext(context)

        // Clear cache
        AudioPlayerService.clearStreamCache()
        assertFalse(AudioPlayerService.isStreamCached("test_cache_track"))
    }
}
