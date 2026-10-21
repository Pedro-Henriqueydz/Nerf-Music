package com.nerf.music.data.model

import kotlinx.serialization.Serializable

/**
 * Representa os detalhes completos de um perfil de artista extraído do InnerTube (/browse com channelId UC...).
 */
@Serializable
data class ArtistDetail(
    val browseId: String,
    val name: String,
    val bannerUrl: String? = null,
    val subscriberCount: String? = null,
    val description: String? = null,
    val radioVideoId: String? = null,
    val topSongs: List<Track> = emptyList(),
    val albums: List<Track> = emptyList(),
    val singles: List<Track> = emptyList(),
    val videos: List<Track> = emptyList(),
    val fansMightAlsoLike: List<Artist> = emptyList()
)
