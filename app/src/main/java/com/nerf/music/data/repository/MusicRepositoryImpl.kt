package com.nerf.music.data.repository

import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.SearchResult
import com.nerf.music.data.network.InnerTubeCatalogClient

/**
 * Implementação padrão do repositório musical usando o cliente InnerTube anônimo.
 */
class MusicRepositoryImpl(
    private val catalogClient: InnerTubeCatalogClient = InnerTubeCatalogClient()
) : MusicRepository {

    override suspend fun search(
        query: String,
        filter: SearchFilter
    ): Result<SearchResult> {
        return catalogClient.search(query, filter)
    }
}
