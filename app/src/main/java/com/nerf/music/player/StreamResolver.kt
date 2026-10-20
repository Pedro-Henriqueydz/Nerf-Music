package com.nerf.music.player

import com.nerf.music.data.model.Track

/**
 * Interface responsável por resolver uma URL de áudio reproduzível para uma faixa.
 *
 * Separa estritamente a camada de catálogo/metadados da camada de reprodução.
 * Não implementa contorno de DRM, quebra de controles de acesso ou proteções anti-bot.
 */
interface StreamResolver {
    /**
     * Resolve a URL de stream direta para a faixa especificada.
     */
    suspend fun resolveStreamUrl(track: Track): Result<String>
}
