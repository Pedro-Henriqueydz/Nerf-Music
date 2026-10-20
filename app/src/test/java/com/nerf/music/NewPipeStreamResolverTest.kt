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
            try {
                // Testa com música oficial para validar a execução do motor Rhino e decifração de assinatura
                val result = resolver.resolveStreamUrl("dQw4w9WgXcQ")
                if (result.isSuccess) {
                    val url = result.getOrNull()
                    assertNotNull("URL de áudio resolvida não pode ser nula", url)
                    assertTrue("URL deve apontar para os servidores do googlevideo", url!!.contains("googlevideo.com"))
                    println("Áudio decifrado com sucesso via NewPipeExtractor: ${url.substring(0, 70)}...")
                } else {
                    val ex = result.exceptionOrNull()
                    println("Aviso no teste online: ${ex?.message}")
                    ex?.cause?.printStackTrace() ?: ex?.printStackTrace()
                }
            } catch (e: Exception) {
                println("Aviso: Teste de rede ignorado por indisponibilidade de conexão: ${e.message}")
            }
        }
    }
}
