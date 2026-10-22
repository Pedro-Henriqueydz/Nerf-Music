package com.nerf.music.player

/**
 * Interface responsável por resolver URLs de streaming de áudio e vídeo direto a partir de um videoId.
 */
interface StreamResolver {
    /**
     * Resolve a URL de áudio reproduzível para o [videoId] fornecido.
     * Retorna a URL direta (ex: googlevideo) pronta para reprodução no ExoPlayer.
     */
    suspend fun resolveStreamUrl(videoId: String): Result<String>

    /**
     * Resolve uma URL de vídeo econômica (ex: 360p / 480p) para exibição de fundo estilo Canvas.
     */
    suspend fun resolveVideoStreamUrl(videoId: String): Result<String?> = Result.success(null)
}
