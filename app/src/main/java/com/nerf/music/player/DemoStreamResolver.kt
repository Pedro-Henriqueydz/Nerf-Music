package com.nerf.music.player

import com.nerf.music.data.model.Track

/**
 * Resolvedor de stream para teste e validação do motor Media3 ExoPlayer.
 *
 * Utiliza faixas de áudio devidamente licenciadas (domínio público / Creative Commons)
 * fornecidas pelo repositório oficial de testes do AndroidX Media3 / Google ExoPlayer.
 */
class DemoStreamResolver : StreamResolver {

    companion object {
        // Áudio de teste oficial mantido pela equipe do ExoPlayer (Google) para testes de reprodução
        const val AUTHORIZED_TEST_AUDIO_URL =
            "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"
    }

    override suspend fun resolveStreamUrl(track: Track): Result<String> {
        // Para a Prova de Conceito (PoC), encaminha para a fonte de teste autorizada
        return Result.success(AUTHORIZED_TEST_AUDIO_URL)
    }
}
