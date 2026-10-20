package com.nerf.music.data.model

/**
 * Estado reativo da reprodução para consumo na interface Jetpack Compose.
 */
data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null
)
