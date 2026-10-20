package com.nerf.music

import com.nerf.music.player.NewPipeStreamResolver
import com.nerf.music.player.OkHttpDownloader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.schabi.newpipe.extractor.NewPipe

class NewPipeStreamResolverTest {

    private lateinit var resolver: NewPipeStreamResolver

    @Before
    fun setup() {
        if (NewPipe.getDownloader() == null) {
            NewPipe.init(OkHttpDownloader())
        }
        resolver = NewPipeStreamResolver()
    }

    @Test
    fun testResolveEmptyVideoIdReturnsFailure() {
        runBlocking {
            val result = resolver.resolveStreamUrl("   ")
            assertTrue("VideoId vazio deve retornar falha", result.isFailure)
        }
    }

    @Test
    fun testRealMusicStreamResolutionViaNewPipe() {
        runBlocking {
            val testIds = listOf("n2tT6Y_rNkE", "agneRtEe-t8", "DYwQMAPf0-g", "V8ktXCb5ns4")
            val httpClient = okhttp3.OkHttpClient()

            for (videoId in testIds) {
                println("Testando resolução para videoId=$videoId...")
                val result = resolver.resolveStreamUrl(videoId)
                if (result.isFailure) {
                    val ex = result.exceptionOrNull()
                    ex?.cause?.printStackTrace() ?: ex?.printStackTrace()
                    fail("Falha ao resolver $videoId: ${ex?.cause?.message ?: ex?.message}")
                }
                val url = result.getOrThrow()
                assertTrue("URL deve apontar para googlevideo.com", url.contains("googlevideo.com"))
                println("URL resolvida ($videoId): ${url.take(100)}...")

                // Verifica se o googlevideo.com realmente aceita a conexão (HTTP 200 ou 206) e entrega bytes de áudio
                val req = okhttp3.Request.Builder()
                    .url(url)
                    .header("Range", "bytes=0-65535")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    println("Status HTTP googlevideo para $videoId: ${resp.code} (Content-Type: ${resp.header("Content-Type")}, bytes: ${resp.body?.bytes()?.size})")
                    assertTrue(
                        "googlevideo retornou HTTP ${resp.code} para $videoId",
                        resp.code == 200 || resp.code == 206
                    )
                }
            }
        }
    }
}
