package com.nerf.music.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DownloadedTrack(
    val track: Track,
    val localFilePath: String,
    val downloadedAtEpochMs: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val isSmartDownload: Boolean = false,
    val localArtworkPath: String? = null
)
