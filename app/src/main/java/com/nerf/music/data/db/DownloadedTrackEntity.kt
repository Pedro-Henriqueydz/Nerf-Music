package com.nerf.music.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nerf.music.data.model.DownloadedTrack
import com.nerf.music.data.model.Track

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrackEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val thumbnailUrl: String?,
    val durationText: String = "--:--",
    val durationSeconds: Long = 0L,
    val sourceIdentifier: String = "INNERTUBE_WEB_REMIX",
    val playlistId: String? = null,
    val browseId: String? = null,
    val artistBrowseId: String? = null,
    val localFilePath: String,
    val downloadedAtEpochMs: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val isSmartDownload: Boolean = false,
    val localArtworkPath: String? = null
) {
    fun toDownloadedTrack(): DownloadedTrack {
        val effectiveThumbnail = if (!localArtworkPath.isNullOrBlank()) {
            "file://$localArtworkPath"
        } else {
            thumbnailUrl
        }
        return DownloadedTrack(
            track = Track(
                id = id,
                title = title,
                artist = artist,
                album = album,
                thumbnailUrl = effectiveThumbnail,
                durationText = durationText,
                durationSeconds = durationSeconds,
                sourceIdentifier = sourceIdentifier,
                playlistId = playlistId,
                browseId = browseId,
                artistBrowseId = artistBrowseId
            ),
            localFilePath = localFilePath,
            downloadedAtEpochMs = downloadedAtEpochMs,
            fileSizeBytes = fileSizeBytes,
            isSmartDownload = isSmartDownload,
            localArtworkPath = localArtworkPath
        )
    }

    companion object {
        fun fromDownloadedTrack(item: DownloadedTrack): DownloadedTrackEntity {
            return DownloadedTrackEntity(
                id = item.track.id,
                title = item.track.title,
                artist = item.track.artist,
                album = item.track.album,
                thumbnailUrl = item.track.thumbnailUrl,
                durationText = item.track.durationText,
                durationSeconds = item.track.durationSeconds,
                sourceIdentifier = item.track.sourceIdentifier,
                playlistId = item.track.playlistId,
                browseId = item.track.browseId,
                artistBrowseId = item.track.artistBrowseId,
                localFilePath = item.localFilePath,
                downloadedAtEpochMs = item.downloadedAtEpochMs,
                fileSizeBytes = item.fileSizeBytes,
                isSmartDownload = item.isSmartDownload,
                localArtworkPath = item.localArtworkPath
            )
        }
    }
}
