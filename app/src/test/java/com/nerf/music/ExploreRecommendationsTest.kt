package com.nerf.music

import com.nerf.music.data.model.ExploreRecommendations
import com.nerf.music.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExploreRecommendationsTest {
    @Test
    fun discoveriesComeFromInterleavedRadiosAndExcludeKnownTracks() {
        val firstRadio = listOf(
            track("seed", "Seed artist"),
            track("a1", "Artist A"),
            track("a2", "Artist A"),
            track("a3", "Artist A")
        )
        val secondRadio = listOf(
            track("liked", "Liked artist"),
            track("b1", "Artist B"),
            track("blocked-track", "Artist C"),
            track("b2", "Blocked artist")
        )

        val result = ExploreRecommendations.selectDiscoveryTracks(
            radioGroups = listOf(firstRadio, secondRadio),
            excludedTrackIds = setOf("seed", "liked"),
            penalizedArtists = setOf("Blocked artist"),
            isTrackPenalized = { it == "blocked-track" }
        )

        assertEquals(listOf("a1", "b1", "a2"), result.map { it.id })
        assertFalse(result.any { it.id == "a3" })
    }

    @Test
    fun discoveriesIgnoreAlbumsDuplicatesAndRespectLimit() {
        val duplicate = track("same", "Artist A")
        val album = track("MPREb_album", "Artist B", browseId = "MPREb_album")

        val result = ExploreRecommendations.selectDiscoveryTracks(
            radioGroups = listOf(listOf(duplicate, album), listOf(duplicate, track("new", "Artist C"))),
            excludedTrackIds = emptySet(),
            penalizedArtists = emptySet(),
            limit = 1
        )

        assertEquals(listOf("same"), result.map { it.id })
    }

    private fun track(id: String, artist: String, browseId: String? = null) = Track(
        id = id,
        title = "Track $id",
        artist = artist,
        browseId = browseId
    )
}
