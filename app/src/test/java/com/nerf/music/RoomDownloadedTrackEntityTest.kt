package com.nerf.music

import com.nerf.music.data.db.DownloadedTrackEntity
import com.nerf.music.data.model.DownloadedTrack
import com.nerf.music.data.model.Track
import org.junit.Assert.*
import org.junit.Test

class RoomDownloadedTrackEntityTest {

    @Test
    fun testBidirectionalMappingBetweenEntityAndModel() {
        val originalTrack = Track(
            id = "test_vid_123",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            album = "A Night at the Opera",
            thumbnailUrl = "https://img.youtube.com/vi/test_vid_123/hqdefault.jpg",
            durationText = "05:55",
            durationSeconds = 355L
        )

        val downloadedTrack = DownloadedTrack(
            track = originalTrack,
            localFilePath = "/data/user/0/com.nerf.music/files/offline_audio/test_vid_123.opus",
            downloadedAtEpochMs = 1700000000000L,
            fileSizeBytes = 4500000L,
            isSmartDownload = true,
            localArtworkPath = "/data/user/0/com.nerf.music/files/offline_audio/test_vid_123.jpg"
        )

        val entity = DownloadedTrackEntity.fromDownloadedTrack(downloadedTrack)
        assertEquals("test_vid_123", entity.id)
        assertEquals("Bohemian Rhapsody", entity.title)
        assertEquals("Queen", entity.artist)
        assertEquals("A Night at the Opera", entity.album)
        assertEquals(4500000L, entity.fileSizeBytes)
        assertTrue(entity.isSmartDownload)
        assertEquals("/data/user/0/com.nerf.music/files/offline_audio/test_vid_123.jpg", entity.localArtworkPath)

        val convertedBack = entity.toDownloadedTrack()
        assertEquals(downloadedTrack.track.id, convertedBack.track.id)
        assertEquals(downloadedTrack.track.title, convertedBack.track.title)
        assertEquals(downloadedTrack.track.artist, convertedBack.track.artist)
        assertEquals(downloadedTrack.localFilePath, convertedBack.localFilePath)
        assertEquals(downloadedTrack.downloadedAtEpochMs, convertedBack.downloadedAtEpochMs)
        assertEquals(downloadedTrack.fileSizeBytes, convertedBack.fileSizeBytes)
        assertEquals(downloadedTrack.isSmartDownload, convertedBack.isSmartDownload)
        assertEquals(downloadedTrack.localArtworkPath, convertedBack.localArtworkPath)
        // Thumbnail should be resolved to local artwork file URI
        assertEquals("file:///data/user/0/com.nerf.music/files/offline_audio/test_vid_123.jpg", convertedBack.track.thumbnailUrl)
    }

    @Test
    fun testEntityToDownloadedTrackFallbackWithoutLocalArtwork() {
        val entity = DownloadedTrackEntity(
            id = "vid_no_art",
            title = "No Art Track",
            artist = "Solo Artist",
            album = null,
            thumbnailUrl = "https://example.com/art.jpg",
            durationText = "03:10",
            durationSeconds = 190L,
            localFilePath = "/offline/vid_no_art.m4a",
            downloadedAtEpochMs = 1690000000000L,
            fileSizeBytes = 2000000L,
            isSmartDownload = false,
            localArtworkPath = null
        )

        val model = entity.toDownloadedTrack()
        assertEquals("https://example.com/art.jpg", model.track.thumbnailUrl)
        assertNull(model.localArtworkPath)
    }
}
