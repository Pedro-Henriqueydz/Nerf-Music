package com.nerf.music.data.model

/**
 * Estado reativo da reprodução e da fila sincronizado com o MediaController / ExoPlayer.
 */
data class PlaybackState(
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentQueueIndex: Int = -1,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val favoriteTrackIds: Set<String> = emptySet(),
    val savedTrackIds: Set<String> = emptySet(),
    val downloadedTrackIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)
