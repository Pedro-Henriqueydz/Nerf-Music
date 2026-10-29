package com.nerf.music

import com.nerf.music.data.model.ReleaseRecommendations
import com.nerf.music.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReleaseRecommendationsTest {

    private fun release(
        id: String,
        artist: String,
        durationText: String = "--:--",
        sourceIdentifier: String = "INNERTUBE_WEB_REMIX"
    ) = Track(
        id = id,
        title = id,
        artist = artist,
        durationText = durationText,
        sourceIdentifier = sourceIdentifier,
        browseId = "MPREb_$id",
        verifiedReleaseDate = when {
            id.contains("2019") -> "2019-01-01"
            id.contains("2021") -> "2021-01-01"
            id.contains("2025") -> "2025-12-01"
            id.contains("undated") -> null
            else -> "2026-01-01"
        }
    )

    @Test
    fun knownTasteNeverFallsBackToUnrelatedReleases() {
        assertEquals(
            emptyList<Track>(),
            ReleaseRecommendations.rank(
                listOf(release("unrelated", "Other")),
                listOf("Geoxor"),
                emptyList(), today = LocalDate.of(2026, 1, 20)
            )
        )
    }

    @Test
    fun recommendationsSelectOnlyRelevantRecordsFromReleaseCatalog() {
        val releases = listOf(
            release("unrelated", "Single • Other"),
            release("similar", "Single • Discovery"),
            release("favorite", "Álbum • Favorite")
        )
        val result = ReleaseRecommendations.rank(
            releases,
            listOf("Favorite", "Old library artist"),
            listOf("Discovery"), today = LocalDate.of(2026, 1, 20)
        )
        assertEquals(listOf("favorite", "similar"), result.map { it.id })
    }

    @Test
    fun changingTasteChangesReleaseSelection() {
        val releases = listOf(release("a", "Single • A"), release("b", "Álbum • B"))
        assertEquals(listOf("a"), ReleaseRecommendations.rank(releases, listOf("A"), emptyList(), today = LocalDate.of(2026, 1, 20)).map { it.id })
        assertEquals(listOf("b"), ReleaseRecommendations.rank(releases, listOf("B"), emptyList(), today = LocalDate.of(2026, 1, 20)).map { it.id })
    }

    @Test
    fun coldStartPreservesCatalogOrderAndNeverAddsLibraryTracks() {
        val releases = listOf(release("new", "New artist"))
        assertEquals(releases, ReleaseRecommendations.rank(releases, emptyList(), emptyList(), today = LocalDate.of(2026, 1, 20)))
    }

    @Test
    fun artistDiscographyRejectsOldReleasesAndKeepsOnlyRecentYears() {
        val candidates = listOf(
            release("old_2019", "Álbum • Geoxor • 2019", "Álbum • Geoxor • 2019", "INNERTUBE_ARTIST_RELEASE"),
            release("old_2021", "Single • Altare • 2021", "Single • Altare • 2021", "INNERTUBE_ARTIST_RELEASE"),
            release("recent_2025", "Álbum • Geoxor • 2025", "Álbum • Geoxor • 2025", "INNERTUBE_ARTIST_RELEASE"),
            release("recent_2026", "Single • Altare • 2026", "Single • Altare • 2026", "INNERTUBE_ARTIST_RELEASE"),
            release("undated_profile", "Single • Altare", "Single", "INNERTUBE_ARTIST_RELEASE")
        )
        val result = ReleaseRecommendations.rank(
            releases = candidates,
            preferredArtists = listOf("Geoxor", "Altare"),
            relatedArtists = emptyList(),
            today = LocalDate.of(2026, 1, 20)
        )
        assertEquals(listOf("recent_2026", "recent_2025"), result.map { it.id })
    }

    @Test
    fun clusterReleasesIncludeUnknownArtistsOfSameStyleAndEnforceDiversity() {
        val candidates = listOf(
            release("fav1", "Single • Altare • 2026", "Single • Altare • 2026", "INNERTUBE_ARTIST_RELEASE"),
            release("fav2", "Single • Altare • 2026", "Single • Altare • 2026", "INNERTUBE_ARTIST_RELEASE"),
            release("fav3", "Single • Altare • 2025", "Single • Altare • 2025", "INNERTUBE_ARTIST_RELEASE"),
            release("cluster_new_1", "Single • Tokyo Tears • 2026", "Single • Tokyo Tears • 2026", "INNERTUBE_CLUSTER_RELEASE"),
            release("cluster_new_2", "EP • NAOMI • 2026", "EP • NAOMI • 2026", "INNERTUBE_CLUSTER_RELEASE")
        )
        val result = ReleaseRecommendations.rank(
            releases = candidates,
            preferredArtists = listOf("Altare"),
            relatedArtists = listOf("Tokyo Tears", "NAOMI"),
            maxPerArtist = 2,
            maxResults = 4,
            today = LocalDate.of(2026, 1, 20)
        )
        // Com maxPerArtist = 2, Altare ocupa 2 vagas iniciais e abre espaço para Tokyo Tears e NAOMI
        assertEquals(listOf("fav1", "fav2", "cluster_new_1", "cluster_new_2"), result.map { it.id })
    }

    @Test
    fun penalizedArtistsAndGenericPlaylistLabelsAreExcluded() {
        val cleaned = ReleaseRecommendations.cleanPreferredArtists(
            rawArtists = listOf("Playlist • YouTube Music", "Playlist automática", "Chase Atlantic", "Altare"),
            penalizedArtists = setOf("Chase Atlantic", "Altare.alt e Eclipse")
        )
        assertEquals(listOf("Altare"), cleaned)

        val candidates = listOf(
            release("penalized_collab", "Single • Altare.alt e Eclipse • 2026", "Single • 2026", "INNERTUBE_ARTIST_RELEASE"),
            release("penalized_solo", "Álbum • Chase Atlantic • 2026", "Álbum • 2026", "INNERTUBE_ARTIST_RELEASE"),
            release("valid_solo", "Single • Altare • 2026", "Single • 2026", "INNERTUBE_ARTIST_RELEASE")
        )
        val ranked = ReleaseRecommendations.rank(
            releases = candidates,
            preferredArtists = listOf("Altare", "Altare.alt"),
            relatedArtists = emptyList(),
            penalizedArtists = setOf("Chase Atlantic", "Altare.alt e Eclipse"),
            today = LocalDate.of(2026, 1, 20)
        )
        assertEquals(listOf("valid_solo"), ranked.map { it.id })
    }
    @Test
    fun datesAndDiversityAreStrictEvenWhenResultsAreFew() {
        val today = LocalDate.of(2026, 6, 1)
        val candidates = listOf(
            release("a", "Favorite").copy(verifiedReleaseDate = "2026-05-01"),
            release("b", "Favorite").copy(verifiedReleaseDate = "2026-05-02"),
            release("future", "Favorite").copy(verifiedReleaseDate = "2026-06-02"),
            release("old", "Favorite").copy(verifiedReleaseDate = "2025-01-01"),
            release("missing", "Favorite").copy(verifiedReleaseDate = null)
        )
        assertEquals(listOf("b"), ReleaseRecommendations.rank(candidates, listOf("Favorite"), emptyList(), maxPerArtist = 1, today = today).map { it.id })
    }

    @Test
    fun negativeContributorIsNotOverriddenByFavorite() {
        val track = release("duet", "Favorite & Avoid")
        assertTrue(ReleaseRecommendations.rank(listOf(track), listOf("Favorite"), emptyList(), setOf("Avoid"), today = LocalDate.of(2026, 1, 20)).isEmpty())
    }
    @Test
    fun verifiedGenreCandidateCanBeDiscoveredAcrossDifferentProfiles() {
        val edm = release("edm", "Discovery").copy(sourceIdentifier = "ITUNES_GENRE_MATCH", releaseGenre = "Electronic")
        val other = release("other", "Unrelated")
        val result = ReleaseRecommendations.rank(listOf(edm, other), listOf("Favorite"), emptyList(), today = LocalDate.of(2026, 1, 20))
        assertEquals(listOf("edm"), result.map { it.id })
    }
    @Test
    fun genreMatchingUsesOnlyTheCurrentProfilesEvidence() {
        val profiles = listOf(setOf("Electronic"), setOf("Sertanejo"), setOf("Jazz"))
        val candidates = listOf("Electronic", "Sertanejo", "Jazz")
        profiles.forEachIndexed { index, profile ->
            assertEquals(listOf(candidates[index]), candidates.filter { ReleaseRecommendations.matchesProfileGenre(it, profile) })
        }
        assertFalse(ReleaseRecommendations.matchesProfileGenre(null, profiles.first()))
        assertFalse(ReleaseRecommendations.matchesProfileGenre("Jazz", emptySet()))
    }
}
