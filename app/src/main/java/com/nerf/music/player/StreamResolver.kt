package com.nerf.music.player

/**
 * Interface responsável por resolver a URL de streaming de áudio direto a partir de um videoId.
 */
interface StreamResolver {
    /**
     * Resolve a URL de áudio reproduzível para o [videoId] fornecido.
     * Retorna a URL direta (ex: googlevideo) pronta para reprodução no ExoPlayer.
     */
    suspend fun resolveStreamUrl(videoId: String): Result<String>
}
