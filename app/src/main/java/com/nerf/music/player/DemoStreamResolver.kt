package com.nerf.music.player

/**
 * Resolvedor de stream demonstrativo para teste e validação do motor Media3 ExoPlayer.
 *
 * Utiliza faixas de áudio devidamente licenciadas (domínio público / Creative Commons)
 * fornecidas pelo repositório oficial de testes do AndroidX Media3 / Google ExoPlayer.
 */
class DemoStreamResolver : StreamResolver {

    companion object {
        const val AUTHORIZED_TEST_AUDIO_URL =
            "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"
    }

    override suspend fun resolveStreamUrl(videoId: String): Result<String> {
        // Para testes e validações offline/demonstrativas, entrega URL de teste oficial
        return Result.success(AUTHORIZED_TEST_AUDIO_URL)
    }
}
