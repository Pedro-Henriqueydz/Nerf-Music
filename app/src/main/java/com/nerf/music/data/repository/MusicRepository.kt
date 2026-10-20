package com.nerf.music.data.repository

import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.SearchResult

/**
 * Interface do repositório para acesso ao catálogo musical.
 */
interface MusicRepository {
    /**
     * Realiza uma busca no catálogo sem exigir conta de usuário.
     * Retorna [SearchResult] contendo músicas, artistas e destaques mapeados de forma independente.
     */
    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter.ALL
    ): Result<SearchResult>
}
