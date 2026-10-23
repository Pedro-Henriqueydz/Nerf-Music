package com.nerf.music.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPlaylist(
    val id: String,
    val title: String,
    val description: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val tracks: List<Track> = emptyList(),
    val customThumbnailUrl: String? = null
) {
    val trackCount: Int get() = tracks.size

    val thumbnailUrl: String?
        get() = customThumbnailUrl ?: tracks.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl
}
