package com.nerf.music

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.nerf.music.data.model.DownloadedTrack
import com.nerf.music.data.model.Track
import com.nerf.music.data.repository.AudioQuality
import com.nerf.music.data.repository.LibraryRepositoryImpl
import com.nerf.music.player.StreamResolver
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Empirical stress tests for HTTP 206 (Partial Content) vs HTTP 200 (OK) chunk range handling and fallback.
 * Validates:
 * 1. Parallel chunk download completes successfully when server honors Range and returns HTTP 206.
 * 2. Parallel chunk download safely aborts when server ignores Range and returns HTTP 200, cleanly
 *    falling back to sequential single-stream download without disk corruption.
 * 3. HTTP 500 error during chunk download is safely handled and does not pollute the library.
 * 4. Progress callbacks are properly dispatched.
 */
class DownloadChunkRangeStressTest {

    private lateinit var tempDir: File
    private lateinit var downloadsDir: File
    private lateinit var testTrack: Track

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("nerf_download_test").toFile()
        downloadsDir = File(tempDir, "offline_audio").apply { mkdirs() }
        testTrack = Track(
            id = "test_vid_123",
            title = "Test Track",
            artist = "Test Artist",
            thumbnailUrl = null,
            durationSeconds = 240
        )
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createTestRepository(interceptor: Interceptor): LibraryRepositoryImpl {
        val dummyPrefs = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
                    return when (method.name) {
                        "getString" -> args?.getOrNull(1)
                        "getBoolean" -> true // Allow cellular downloads in unit test environment
                        "getInt" -> args?.getOrNull(1) ?: 0
                        "getLong" -> args?.getOrNull(1) ?: 0L
                        "contains" -> false
                        "registerOnSharedPreferenceChangeListener",
                        "unregisterOnSharedPreferenceChangeListener" -> null
                        "edit" -> proxy
                        "putString", "putBoolean", "putInt", "putLong", "apply", "commit" -> proxy
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
            override suspend fun resolveStreamUrl(videoId: String): Result<String> =
                Result.success("https://googlevideo.com/stream/$videoId")

            override suspend fun resolveStreamUrl(videoId: String, targetQuality: AudioQuality?): Result<String> =
                Result.success("https://googlevideo.com/stream/$videoId")
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        return LibraryRepositoryImpl(
            context = testContext,
            httpClient = httpClient,
            streamResolver = dummyResolver
        )
    }

    @Test
    fun testSuccessfulParallelChunkDownloadWithHttp206() = runBlocking {
        val totalSize = 250_000 // 250 KB
        val payload = ByteArray(totalSize) { (it % 256).toByte() }

        val chunkCount = AtomicInteger(0)

        val interceptor = Interceptor { chain ->
            val req = chain.request()
            val rangeHeader = req.header("Range")

            if (req.method == "HEAD") {
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Length", totalSize.toString())
                    .header("Content-Type", "audio/webm; codecs=\"opus\"")
                    .body("".toResponseBody("audio/webm".toMediaType()))
                    .build()
            } else if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                chunkCount.incrementAndGet()
                // Parse Range e.g. "bytes=0-1048575"
                val rangePart = rangeHeader.removePrefix("bytes=")
                val parts = rangePart.split("-")
                val start = parts[0].toInt()
                val end = parts[1].toInt().coerceAtMost(totalSize - 1)
                val chunkSize = (end - start + 1).coerceAtLeast(0)

                val chunkBytes = ByteArray(chunkSize)
                System.arraycopy(payload, start, chunkBytes, 0, chunkSize)

                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(206)
                    .message("Partial Content")
                    .header("Content-Range", "bytes $start-$end/$totalSize")
                    .header("Content-Length", chunkSize.toString())
                    .header("Content-Type", "audio/webm; codecs=\"opus\"")
                    .body(chunkBytes.toResponseBody("audio/webm".toMediaType()))
                    .build()
            } else {
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Length", totalSize.toString())
                    .body(payload.toResponseBody("audio/webm".toMediaType()))
                    .build()
            }
        }

        val repo = createTestRepository(interceptor)
        var lastReportedProgress = 0f

        val result = repo.downloadTrack(testTrack) { p ->
            lastReportedProgress = p
        }

        assertTrue("Download deve ser bem-sucedido via HTTP 206", result.isSuccess)
        val downloaded = result.getOrThrow()

        val expectedFile = File(downloadsDir, "${testTrack.id}.opus")
        assertTrue("Arquivo .opus final deve existir", expectedFile.exists())
        assertEquals("Tamanho do arquivo baixado deve bater exatamente", totalSize.toLong(), expectedFile.length())
        assertEquals("Progresso final deve ser 1.0", 1.0f, lastReportedProgress, 0.01f)
        assertEquals(expectedFile.absolutePath, downloaded.localFilePath)

        // Verify exact byte integrity
        val savedBytes = expectedFile.readBytes()
        assertTrue("Conteúdo gravado deve coincidir byte a byte", payload.contentEquals(savedBytes))
    }

    @Test
    fun testFallbackToSequentialWhenServerReturnsHttp200ForRangeRequest() = runBlocking {
        val totalSize = 150_000 // 150 KB
        val payload = ByteArray(totalSize) { ((it * 3) % 256).toByte() }

        val sequentialInvoked = AtomicBoolean(false)
        val rangeAttempts = AtomicInteger(0)

        val interceptor = Interceptor { chain ->
            val req = chain.request()
            val rangeHeader = req.header("Range")

            if (req.method == "HEAD") {
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Length", totalSize.toString())
                    .header("Content-Type", "audio/mp4; codecs=\"mp4a.40.2\"")
                    .body("".toResponseBody("audio/mp4".toMediaType()))
                    .build()
            } else if (rangeHeader != null) {
                // Server DOES NOT honor Range, returning 200 with full body instead of 206
                rangeAttempts.incrementAndGet()
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK (Range Ignored)")
                    .header("Content-Type", "audio/mp4; codecs=\"mp4a.40.2\"")
                    .body(payload.toResponseBody("audio/mp4".toMediaType()))
                    .build()
            } else {
                // Sequential fallback GET request (no Range header)
                sequentialInvoked.set(true)
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Length", totalSize.toString())
                    .header("Content-Type", "audio/mp4; codecs=\"mp4a.40.2\"")
                    .body(payload.toResponseBody("audio/mp4".toMediaType()))
                    .build()
            }
        }

        val repo = createTestRepository(interceptor)
        var reportedProgress = 0f

        val result = repo.downloadTrack(testTrack) { p ->
            reportedProgress = p
        }

        assertTrue("Download deve completar com sucesso via fallback sequencial", result.isSuccess)
        assertTrue("Fallback sequencial deve ter sido acionado", sequentialInvoked.get())

        val destFile = File(downloadsDir, "${testTrack.id}.m4a")
        assertTrue("Arquivo .m4a deve existir após fallback", destFile.exists())
        assertEquals("Tamanho do arquivo deve coincidir", totalSize.toLong(), destFile.length())

        val savedBytes = destFile.readBytes()
        assertTrue("Bytes após fallback sequencial devem estar íntegros e sem corrupção", payload.contentEquals(savedBytes))

        // Ensure temp file is cleaned up
        val tempFile = File(downloadsDir, "${testTrack.id}.tmp")
        assertFalse("Arquivo temporário .tmp deve ter sido deletado", tempFile.exists())
    }

    @Test
    fun testChunkDownloadHttp500FailureHandling() = runBlocking {
        val totalSize = 200_000

        val interceptor = Interceptor { chain ->
            val req = chain.request()
            if (req.method == "HEAD") {
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Length", totalSize.toString())
                    .body("".toResponseBody("audio/webm".toMediaType()))
                    .build()
            } else {
                // Chunk request fails with 500 Internal Server Error
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(500)
                    .message("Internal Server Error")
                    .body("Error".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
        }

        val repo = createTestRepository(interceptor)
        val result = repo.downloadTrack(testTrack)

        assertTrue("Falha no chunk deve retornar Result.failure", result.isFailure)

        // Neither .opus nor .m4a should exist
        assertFalse(File(downloadsDir, "${testTrack.id}.opus").exists())
        assertFalse(File(downloadsDir, "${testTrack.id}.m4a").exists())
    }
}
