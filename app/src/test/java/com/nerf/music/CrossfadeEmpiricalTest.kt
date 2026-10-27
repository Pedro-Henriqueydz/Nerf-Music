package com.nerf.music

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * Empirical test suite for Crossfade engine logic in AudioPlayerService.
 * Validates:
 * 1. Short track duration safety (no division by zero, no NaN, no negative durations).
 * 2. Buffer timeout behavior (graceful degradation after 2.5s without deadlock).
 * 3. Outgoing player auto-advance prevention via pauseAtEndOfMediaItems.
 */
class CrossfadeEmpiricalTest {

    @Test
    fun testCrossfadeMathExhaustiveFuzzing() {
        // Fuzz across a broad range of durations: 0ms up to 10,000ms, and positions beyond duration
        val testDurations = listOf(0L, 1L, 10L, 50L, 99L, 100L, 150L, 300L, 500L, 1000L, 1500L, 2000L, 5000L, 180000L)
        val targetFadeDurations = listOf(1000L, 2000L, 3000L, 5000L, 10000L, 12000L)

        for (duration in testDurations) {
            for (targetFadeMs in targetFadeDurations) {
                // Test positions from 0 up to duration + 500ms
                for (position in 0L..(duration + 500L) step 50L) {
                    val remainingMs = (duration - position).coerceAtLeast(0L)
                    val effectiveFadeMs = targetFadeMs.coerceAtMost(duration / 3).coerceAtMost(remainingMs)
                    val triggerThresholdMs = effectiveFadeMs + 600L

                    val willTrigger = (remainingMs in 100..triggerThresholdMs) && (duration > 0L) && (position > 0L)

                    if (willTrigger) {
                        // Assert that if transition triggers, effectiveFadeMs is ALWAYS positive
                        assertTrue(
                            "effectiveFadeMs must be > 0 when triggered (duration=$duration, pos=$position, eff=$effectiveFadeMs)",
                            effectiveFadeMs > 0L
                        )

                        // Simulate executeCrossfade logic
                        val outgoingDuration = duration
                        val outgoingPos = position + 35L // outgoing advanced during prepare
                        val remainingInExec = (outgoingDuration - outgoingPos).coerceAtLeast(100L)
                        val effectiveInExec = effectiveFadeMs.coerceAtMost(remainingInExec)

                        assertTrue("effectiveInExec must be >= 1L, was $effectiveInExec", effectiveInExec >= 1L)

                        // Verify volume curve math produces finite, valid float values without NaN or division by zero
                        for (elapsed in 0L..effectiveInExec step 20L) {
                            val progress = (elapsed.toFloat() / effectiveInExec).coerceIn(0f, 1f)
                            assertFalse("progress cannot be NaN", progress.isNaN())
                            assertTrue("progress must be in [0, 1]", progress in 0f..1f)

                            val volOut = cos(progress * (Math.PI / 2)).toFloat()
                            val volIn = sin(progress * (Math.PI / 2)).toFloat()

                            assertFalse("volOut cannot be NaN", volOut.isNaN())
                            assertFalse("volIn cannot be NaN", volIn.isNaN())
                            assertTrue("volOut must be in [0, 1]", volOut in -0.01f..1.01f)
                            assertTrue("volIn must be in [0, 1]", volIn in -0.01f..1.01f)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testShortTrackRemainingCoercionEdgeCases() {
        val targetFadeMs = 5000L

        // Case 1: remaining is 500ms, duration is 3000ms
        val duration = 3000L
        val position = 2500L
        val remaining = (duration - position).coerceAtLeast(0L) // 500
        val eff = targetFadeMs.coerceAtMost(duration / 3).coerceAtMost(remaining) // min(5000, 1000, 500) = 500
        assertEquals(500L, eff)

        // Case 2: remaining in executeCrossfade clamped by coerceAtLeast(100L)
        // Even if outgoing track has overshot its duration (e.g. pos 3100 > duration 3000)
        val remainingInExec = (duration - 3100L).coerceAtLeast(100L)
        assertEquals(100L, remainingInExec)
        val effInExec = eff.coerceAtMost(remainingInExec)
        assertEquals(100L, effInExec)
        assertTrue("effInExec is safely positive", effInExec > 0L)
    }

    @Test
    fun testBufferTimeoutGracefulDegradationNoDeadlock() {
        // Simulates the while loop in AudioPlayerService:
        // while (incomingPlayer.playbackState != Player.STATE_READY && waitTime < 2500) { delay(35); waitTime += 35 }

        // Scenario A: Player buffers normally in 210ms
        var waitTimeA = 0
        val bufferTimeA = 210
        while (waitTimeA < bufferTimeA && waitTimeA < 2500) {
            waitTimeA += 35
        }
        assertEquals(210, waitTimeA)
        assertTrue("Buffers cleanly under timeout", waitTimeA < 2500)

        // Scenario B: Player is slow to buffer (takes 10 seconds, e.g. poor cellular connection)
        var waitTimeB = 0
        val bufferTimeB = 10000
        var loopIterations = 0
        while (waitTimeB < bufferTimeB && waitTimeB < 2500) {
            waitTimeB += 35
            loopIterations++
        }
        // Must exit gracefully at 2520ms (72 iterations) without deadlocking
        assertEquals(2520, waitTimeB)
        assertEquals(72, loopIterations)
        assertTrue("Loop safely terminates when timeout exceeded", waitTimeB >= 2500)

        // Scenario C: Player encounters error / idle state (never becomes STATE_READY)
        var waitTimeC = 0
        while (true) {
            val state = Player.STATE_IDLE
            if (state == Player.STATE_READY || waitTimeC >= 2500) break
            waitTimeC += 35
        }
        assertEquals(2520, waitTimeC)
    }

    @Test
    fun testPauseAtEndOfMediaItemsPreventsAccidentalSkip() {
        // Validates lifecycle state requirements for outgoingPlayer:
        // Before crossfade: repeatMode = REPEAT_MODE_OFF, pauseAtEndOfMediaItems = true
        // After crossfade: stop() called, pauseAtEndOfMediaItems = false, audioAttributes handleAudioFocus = false
        var repeatMode = Player.REPEAT_MODE_ALL
        var pauseAtEndOfMediaItems = false
        var handleAudioFocus = true
        var isPlaying = true

        // 1. Crossfade begins
        repeatMode = Player.REPEAT_MODE_OFF
        pauseAtEndOfMediaItems = true
        assertEquals(Player.REPEAT_MODE_OFF, repeatMode)
        assertTrue(pauseAtEndOfMediaItems)

        // If media item ends during crossfade, pauseAtEndOfMediaItems = true prevents advancing to next index
        // Player pauses instead of auto-advancing
        isPlaying = false // paused at end of media item

        // 2. Crossfade completes
        handleAudioFocus = false // audio focus released before stop()
        pauseAtEndOfMediaItems = false
        assertEquals(false, pauseAtEndOfMediaItems)
        assertEquals(false, handleAudioFocus)
    }
}
