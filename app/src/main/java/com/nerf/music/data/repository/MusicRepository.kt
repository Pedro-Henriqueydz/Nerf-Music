package com.nerf.music.data.repository

import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.SearchResult
import com.nerf.music.data.model.UpNextResult

/**
 * Interface do repositório para acesso ao catálogo musical e à rádio dinâmica.
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

    /**
     * Obtém a fila de próximas músicas (Rádio Dinâmica / Mix) e os chips de filtro
     * via endpoint `/youtubei/v1/next`.
     */
    suspend fun getUpNextRadio(
        videoId: String,
        params: String? = null
    ): Result<UpNextResult>
}
