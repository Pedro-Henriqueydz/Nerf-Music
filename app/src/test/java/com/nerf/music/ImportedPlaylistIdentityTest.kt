package com.nerf.music

import com.nerf.music.data.repository.ImportedPlaylistIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ImportedPlaylistIdentityTest {
    @Test
    fun sameSourceProducesSameLocalId() {
        assertEquals(
            ImportedPlaylistIdentity.idFor("spotify:playlist-123"),
            ImportedPlaylistIdentity.idFor("spotify:playlist-123")
        )
    }

    @Test
    fun sourceAndProviderArePartOfIdentity() {
        val spotifyId = ImportedPlaylistIdentity.idFor("spotify:playlist-123")
        assertNotEquals(spotifyId, ImportedPlaylistIdentity.idFor("youtube_music:playlist-123"))
        assertNotEquals(spotifyId, ImportedPlaylistIdentity.idFor("spotify:playlist-456"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankSourceIsRejected() {
        ImportedPlaylistIdentity.idFor("  ")
    }
}
