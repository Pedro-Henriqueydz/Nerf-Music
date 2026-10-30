package com.nerf.music.data.model

data class RecapTrackStats(
    val track: Track,
    val plays: Int,
    val listenedMs: Long
)

data class RecapArtistStats(
    val name: String,
    val plays: Int,
    val listenedMs: Long,
    val representativeTrack: Track?
)

data class RecapSummary(
    val year: Int = java.time.Year.now().value,
    val totalMinutes: Long = 0,
    val totalPlays: Int = 0,
    val topTracks: List<RecapTrackStats> = emptyList(),
    val topArtists: List<RecapArtistStats> = emptyList(),
    val mixTracks: List<Track> = emptyList()
) {
    val hasEnoughData: Boolean get() = totalPlays > 0 || topTracks.isNotEmpty()
    val annualEligible: Boolean get() = totalMinutes >= 600 && topTracks.size >= 5
    val topArtist: RecapArtistStats? get() = topArtists.firstOrNull()
}
