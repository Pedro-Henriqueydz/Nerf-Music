package com.nerf.music

import com.nerf.music.data.model.Track
import com.nerf.music.ui.screens.QueueExpansionState
import com.nerf.music.ui.screens.formatDurationMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerUiUtilsTest {

    @Test
    fun testFormatDurationMs() {
        assertEquals("00:00", formatDurationMs(0L))
        assertEquals("00:00", formatDurationMs(-500L))
        assertEquals("01:20", formatDurationMs(80_000L))
        assertEquals("03:45", formatDurationMs(225_000L))
        assertEquals("10:05", formatDurationMs(605_000L))
    }

    @Test
    fun testHighResThumbnailUrlUpgrade() {
        val track = Track(
            id = "abc123",
            title = "Como Nossos Pais",
            artist = "Elis Regina",
            thumbnailUrl = "https://lh3.googleusercontent.com/somepath=w120-h120-l90-rj"
        )
        assertEquals(
            "https://lh3.googleusercontent.com/somepath=w544-h544-l90-rj",
            track.highResThumbnailUrl
        )
    }

    @Test
    fun testQueueExpansionStatesExist() {
        val states = QueueExpansionState.entries
        assertEquals(3, states.size)
        assertTrue(states.contains(QueueExpansionState.COLLAPSED))
        assertTrue(states.contains(QueueExpansionState.HALF_EXPANDED))
        assertTrue(states.contains(QueueExpansionState.EXPANDED))
    }
}
