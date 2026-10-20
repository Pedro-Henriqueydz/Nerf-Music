package com.nerf.music.data.model

import kotlinx.serialization.Serializable

/**
 * Resultado estruturado de uma pesquisa no catálogo.
 * Separa estritamente músicas de artistas e destaques.
 */
@Serializable
data class SearchResult(
    val songs: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val topArtist: Artist? = null
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && artists.isEmpty() && topArtist == null

    val totalItemsCount: Int
        get() = songs.size + artists.size + (if (topArtist != null) 1 else 0)
}
