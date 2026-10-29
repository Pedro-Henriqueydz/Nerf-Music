package com.nerf.music

import com.nerf.music.util.ShareRules
import org.junit.Assert.*
import org.junit.Test

class ShareRulesTest {
    @Test fun dragSelectionIsContinuousInBothDirectionsAndLimitedToFour() {
        assertEquals(listOf(3, 4, 5, 6), ShareRules.lyricRange(3, 9, 12))
        assertEquals(listOf(3, 4, 5, 6), ShareRules.lyricRange(6, 0, 12))
        assertEquals(listOf(5), ShareRules.lyricRange(5, 5, 12))
        assertEquals(listOf(0, 1), ShareRules.lyricRange(1, -50, 12))
        assertEquals(listOf(10, 11), ShareRules.lyricRange(10, 99, 12))
    }

    @Test fun invalidOrEmptyLyricsCannotCreateAnInvalidSelection() {
        assertTrue(ShareRules.lyricRange(0, 4, 0).isEmpty())
        assertTrue(ShareRules.lyricRange(-1, 4, 10).isEmpty())
        assertTrue(ShareRules.lyricRange(10, 4, 10).isEmpty())
        assertTrue(ShareRules.cleanLines(listOf("", "  ", "null", " NULL ")).isEmpty())
    }

    @Test fun selectionKeepsSourceOrderAndCanBeDeselectedAtLimit() {
        var selected = listOf(1, 3, 5)
        selected = ShareRules.toggleLine(selected, 2)
        assertEquals(listOf(1, 2, 3, 5), selected)
        assertEquals(selected, ShareRules.toggleLine(selected, 6))
        assertEquals(listOf(1, 2, 5), ShareRules.toggleLine(selected, 3))
        assertEquals(emptyList<Int>(), ShareRules.toggleLine(listOf(1), 1))
    }

    @Test fun rendererCleansLinesAndLimitsAfterRemovingEmptyValues() {
        assertEquals(listOf("a", "b", "c", "d"), ShareRules.cleanLines(listOf(" a ", "", "null", "b", "c", "d", "e")))
    }

    @Test fun backgroundPreservesHueAndBoundsBrightnessWithoutChannelClipping() {
        assertArrayEquals(floatArrayOf(260f, 0.85f, 0.68f), ShareRules.backgroundHsv(260f, 1f, 1f), 0.0001f)
        assertArrayEquals(floatArrayOf(25f, 0.3f, 0.38f), ShareRules.backgroundHsv(25f, 0.3f, 0.02f), 0.0001f)
        assertArrayEquals(floatArrayOf(120f, 0.6f, 0.5f), ShareRules.backgroundHsv(120f, 0.6f, 0.5f), 0.0001f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0.68f), ShareRules.backgroundHsv(0f, 0f, 1f), 0.0001f)
    }
}
