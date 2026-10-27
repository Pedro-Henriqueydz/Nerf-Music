package com.nerf.music

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.nerf.music.data.repository.AudioQuality
import com.nerf.music.data.repository.LibraryRepositoryImpl
import com.nerf.music.player.StreamResolver
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.nio.file.Files

/**
 * Empirical stress tests for MIME and container format determination logic in LibraryRepositoryImpl.
 * Verifies boundaries: varied Content-Type headers, URL encoded parameters, mixed-casing, and null/empty fallbacks.
 */
class MimeAndContainerDetectionStressTest {

    private lateinit var tempDir: File
    private lateinit var repoInstance: LibraryRepositoryImpl
    private lateinit var determineExtensionMethod: Method

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("nerf_mime_test").toFile()

        val dummyPrefs = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
                    return when (method.name) {
                        "getString" -> args?.getOrNull(1) // return default value
                        "contains" -> false
                        "registerOnSharedPreferenceChangeListener",
                        "unregisterOnSharedPreferenceChangeListener" -> null
                        else -> null
                    }
                }
            }
        ) as SharedPreferences

        val testContext = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = tempDir
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = dummyPrefs
        }

        val dummyResolver = object : StreamResolver {
            override suspend fun resolveStreamUrl(videoId: String): Result<String> = Result.success("http://dummy")
            override suspend fun resolveStreamUrl(videoId: String, targetQuality: AudioQuality?): Result<String> = Result.success("http://dummy")
        }

        repoInstance = LibraryRepositoryImpl(
            context = testContext,
            httpClient = OkHttpClient(),
            streamResolver = dummyResolver
        )

        determineExtensionMethod = LibraryRepositoryImpl::class.java.getDeclaredMethod(
            "determineExtension",
            String::class.java,
            String::class.java
        ).apply { isAccessible = true }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun callDetermineExtension(contentType: String?, url: String): String {
        return determineExtensionMethod.invoke(repoInstance, contentType, url) as String
    }

    @Test
    fun testStandardMp4AndAacContentTypes() {
        assertEquals("m4a", callDetermineExtension("audio/mp4", "https://example.com/stream"))
        assertEquals("m4a", callDetermineExtension("audio/mp4; codecs=\"mp4a.40.2\"", "https://example.com/stream"))
        assertEquals("m4a", callDetermineExtension("audio/x-m4a", "https://example.com/stream"))
        assertEquals("m4a", callDetermineExtension("audio/aac", "https://example.com/stream"))
        assertEquals("m4a", callDetermineExtension("audio/aac; bitrate=128", "https://example.com/stream"))
    }

    @Test
    fun testStandardWebmAndOpusContentTypes() {
        assertEquals("opus", callDetermineExtension("audio/webm", "https://example.com/stream"))
        assertEquals("opus", callDetermineExtension("audio/webm; codecs=\"opus\"", "https://example.com/stream"))
        assertEquals("opus", callDetermineExtension("audio/opus", "https://example.com/stream"))
        assertEquals("opus", callDetermineExtension("audio/ogg", "https://example.com/stream"))
        assertEquals("opus", callDetermineExtension("audio/ogg; codecs=opus", "https://example.com/stream"))
    }

    @Test
    fun testCaseInsensitiveContentTypeMatching() {
        assertEquals("m4a", callDetermineExtension("AUDIO/MP4; CODECS=\"MP4A.40.2\"", "https://example.com/stream"))
        assertEquals("m4a", callDetermineExtension("Audio/X-M4A", "https://example.com/stream"))
        assertEquals("m4a", callDetermineExtension("Audio/AAC", "https://example.com/stream"))
        assertEquals("opus", callDetermineExtension("AUDIO/WEBM; CODECS=\"OPUS\"", "https://example.com/stream"))
        assertEquals("opus", callDetermineExtension("Audio/Opus", "https://example.com/stream"))
        assertEquals("opus", callDetermineExtension("AUDIO/OGG", "https://example.com/stream"))
    }

    @Test
    fun testNullContentTypeFallbackToUrlParameters() {
        // MP4 / AAC URL markers
        assertEquals("m4a", callDetermineExtension(null, "https://googlevideo.com/playback?mime=audio%2fmp4&itag=140"))
        assertEquals("m4a", callDetermineExtension(null, "https://googlevideo.com/playback?mime=audio/mp4"))
        assertEquals("m4a", callDetermineExtension(null, "https://googlevideo.com/playback?itag=140&expire=123"))
        assertEquals("m4a", callDetermineExtension(null, "https://googlevideo.com/playback?itag=139&expire=123"))
        assertEquals("m4a", callDetermineExtension(null, "https://googlevideo.com/playback?MIME=AUDIO%2FMP4"))
        assertEquals("m4a", callDetermineExtension(null, "https://googlevideo.com/playback?ITAG=140"))

        // WebM / Opus URL markers
        assertEquals("opus", callDetermineExtension(null, "https://googlevideo.com/playback?mime=audio%2fwebm&itag=251"))
        assertEquals("opus", callDetermineExtension(null, "https://googlevideo.com/playback?mime=audio/webm"))
        assertEquals("opus", callDetermineExtension(null, "https://googlevideo.com/playback?itag=251&expire=123"))
        assertEquals("opus", callDetermineExtension(null, "https://googlevideo.com/playback?itag=250&expire=123"))
        assertEquals("opus", callDetermineExtension(null, "https://googlevideo.com/playback?itag=249&expire=123"))
        assertEquals("opus", callDetermineExtension(null, "https://googlevideo.com/playback?MIME=AUDIO%2FWEBM"))
        assertEquals("opus", callDetermineExtension(null, "https://googlevideo.com/playback?ITAG=251"))
    }

    @Test
    fun testEmptyOrUnknownContentTypeWithUrlFallback() {
        assertEquals("m4a", callDetermineExtension("", "https://googlevideo.com/playback?mime=audio%2fmp4"))
        assertEquals("m4a", callDetermineExtension("   ", "https://googlevideo.com/playback?itag=140"))
        assertEquals("m4a", callDetermineExtension("application/octet-stream", "https://googlevideo.com/playback?mime=audio%2fmp4"))
        assertEquals("m4a", callDetermineExtension("text/html; charset=UTF-8", "https://googlevideo.com/playback?itag=139"))

        assertEquals("opus", callDetermineExtension("", "https://googlevideo.com/playback?mime=audio%2fwebm"))
        assertEquals("opus", callDetermineExtension("   ", "https://googlevideo.com/playback?itag=251"))
        assertEquals("opus", callDetermineExtension("binary/octet-stream", "https://googlevideo.com/playback?itag=250"))
    }

    @Test
    fun testUnknownContentTypeAndGenericUrlDefaultsToOpus() {
        assertEquals("opus", callDetermineExtension(null, "https://example.com/audio/raw_stream"))
        assertEquals("opus", callDetermineExtension("", "https://example.com/stream.bin"))
        assertEquals("opus", callDetermineExtension("application/octet-stream", "https://example.com/generic"))
        assertEquals("opus", callDetermineExtension("unknown/format", "https://example.com/audio"))
    }

    @Test
    fun testComplexRealisticGoogleVideoUrls() {
        val gvMp4Url = "https://rr2---sn.googlevideo.com/videoplayback?expire=1730000000&ei=xyz&ip=1.2.3.4" +
                "&id=o-ABC&itag=140&source=youtube&requiressl=yes&xpc=egvocndu...&vprv=1&mime=audio%2fmp4&ns=dummy"
        assertEquals("m4a", callDetermineExtension("audio/mp4; codecs=\"mp4a.40.2\"", gvMp4Url))
        assertEquals("m4a", callDetermineExtension(null, gvMp4Url))

        val gvWebmUrl = "https://rr2---sn.googlevideo.com/videoplayback?expire=1730000000&ei=xyz&ip=1.2.3.4" +
                "&id=o-ABC&itag=251&source=youtube&requiressl=yes&xpc=egvocndu...&vprv=1&mime=audio%2fwebm&ns=dummy"
        assertEquals("opus", callDetermineExtension("audio/webm; codecs=\"opus\"", gvWebmUrl))
        assertEquals("opus", callDetermineExtension(null, gvWebmUrl))
    }
}
