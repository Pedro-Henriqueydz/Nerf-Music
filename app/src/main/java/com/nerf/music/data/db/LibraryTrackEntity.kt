package com.nerf.music.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nerf.music.data.model.Track
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "library_tracks")
data class LibraryTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val thumbnailUrl: String?,
    val durationText: String,
    val durationSeconds: Long,
    val sourceIdentifier: String,
    val playlistId: String?,
    val browseId: String?,
    val artistBrowseId: String?,
    val verifiedReleaseDate: String?,
    val releaseGenre: String?,
    val statsYear: Int = java.time.Year.now().value,
    val playCount: Int = 0,
    val totalListenMs: Long = 0L,
    val lastPlayedAtEpochMs: Long? = null,
    val libraryAddedAtEpochMs: Long? = null,
    val likedAtEpochMs: Long? = null
) {
    fun toTrack() = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        thumbnailUrl = thumbnailUrl,
        durationText = durationText,
        durationSeconds = durationSeconds,
        sourceIdentifier = sourceIdentifier,
        playlistId = playlistId,
        browseId = browseId,
        artistBrowseId = artistBrowseId,
        verifiedReleaseDate = verifiedReleaseDate,
        releaseGenre = releaseGenre
    )

    companion object {
        fun fromTrack(track: Track) = LibraryTrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            thumbnailUrl = track.thumbnailUrl,
            durationText = track.durationText,
            durationSeconds = track.durationSeconds,
            sourceIdentifier = track.sourceIdentifier,
            playlistId = track.playlistId,
            browseId = track.browseId,
            artistBrowseId = track.artistBrowseId,
            verifiedReleaseDate = track.verifiedReleaseDate,
            releaseGenre = track.releaseGenre
        )
    }
}
