package com.nerf.music.data.model

import kotlinx.serialization.Serializable

/**
 * Detalhes completos de um álbum ou playlist retornados pelo InnerTube:
 * - Metadados de cabeçalho (título, artista, capa, ano, tipo)
 * - Lista ordenada de faixas com durações, contagens e endpoints
 */
@Serializable
data class AlbumDetail(
    val browseId: String,
    val title: String,
    val artistName: String,
    val artistThumbnailUrl: String? = null,
    val albumType: String = "Álbum", // "Álbum", "Single", "EP", "Playlist"
    val year: String? = null,
    val thumbnailUrl: String? = null,
    val tracks: List<Track> = emptyList(),
    val trackCount: Int = tracks.size
) {
    /**
     * Retorna a URL da capa do álbum em alta resolução (1024x1024).
     */
    val highResThumbnailUrl: String?
        get() = thumbnailUrl?.replace(Regex("=w\\d+-h\\d+"), "=w1024-h1024")
}
