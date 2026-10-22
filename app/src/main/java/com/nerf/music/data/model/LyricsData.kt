package com.nerf.music.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncedLyricsLine(
    val timeMs: Long,
    val text: String
)

@Serializable
data class LyricsData(
    val trackId: String,
    val isSynced: Boolean,
    val plainLyrics: String? = null,
    val syncedLines: List<SyncedLyricsLine> = emptyList()
)
