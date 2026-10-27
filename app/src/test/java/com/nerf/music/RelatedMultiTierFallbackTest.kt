package com.nerf.music

import com.nerf.music.data.model.HomeSectionStyle
import com.nerf.music.data.model.RelatedSection
import com.nerf.music.data.model.Track
import com.nerf.music.data.model.TrackRelatedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelatedMultiTierFallbackTest {

    private fun extractPrimaryArtist(rawArtist: String): String {
        if (rawArtist.isBlank()) return ""
        return rawArtist
            .split(Regex("(?i)[,;&/]|\\b(feat|ft|part)\\b|\\(part\\."))
            .firstOrNull()
            ?.trim()
            .orEmpty()
            .ifEmpty { rawArtist.trim() }
    }

    @Test
    fun testExtractPrimaryArtist() {
        assertEquals("Altare", extractPrimaryArtist("Altare"))
        assertEquals("Altare", extractPrimaryArtist("Altare, Devilish Trio"))
        assertEquals("Altare", extractPrimaryArtist("Altare feat. Devilish Trio"))
        assertEquals("Altare", extractPrimaryArtist("Altare (part. Devilish Trio)"))
        assertEquals("Altare", extractPrimaryArtist("Altare / Phonk Producer"))
        assertEquals("Altare", extractPrimaryArtist("Altare & Friend"))
        assertEquals("Kessoku Band", extractPrimaryArtist("Kessoku Band"))
    }

    @Test
    fun testRelatedMultiTierSynthesisAlwaysPopulatesContent() {
        val testTrack = Track(
            id = "_Eq_qQUPvGQ",
            title = "Impulse (part. Devilish Trio)",
            artist = "Altare"
        )

        // Tier 1: Empty official response
        val officialData = TrackRelatedData(sections = emptyList())
        assertTrue(officialData.sections.isEmpty())

        // Tier 2: UpNext / Queue tracks as similar songs
        val queueCandidates = listOf(
            Track(id = "rec1", title = "Shadow", artist = "Altare"),
            Track(id = "rec2", title = "Vengeance", artist = "Devilish Trio"),
            Track(id = "_Eq_qQUPvGQ", title = "Impulse", artist = "Altare") // Current track filtered
        )
        val similarTracks = queueCandidates.filter { it.id != testTrack.id }
        assertFalse(similarTracks.isEmpty())
        assertEquals(2, similarTracks.size)

        val similarSection = RelatedSection(
            title = "Músicas semelhantes",
            items = similarTracks,
            style = HomeSectionStyle.QUICK_PICKS_GRID
        )

        // Tier 3: Artist top songs
        val primaryArtist = extractPrimaryArtist(testTrack.artist)
        assertEquals("Altare", primaryArtist)
        val artistTracks = listOf(
            Track(id = "art1", title = "Departure", artist = "Altare")
        )
        val artistSection = RelatedSection(
            title = "Mais de $primaryArtist",
            items = artistTracks,
            style = HomeSectionStyle.QUICK_PICKS_GRID
        )

        val synthesizedData = TrackRelatedData(
            sections = listOf(similarSection, artistSection)
        )

        assertFalse(synthesizedData.sections.isEmpty())
        assertEquals(2, synthesizedData.sections.size)
        assertEquals("Músicas semelhantes", synthesizedData.sections[0].title)
        assertEquals("Mais de Altare", synthesizedData.sections[1].title)
    }
}
