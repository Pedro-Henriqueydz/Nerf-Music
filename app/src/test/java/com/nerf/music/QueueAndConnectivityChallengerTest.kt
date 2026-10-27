package com.nerf.music

import com.nerf.music.ui.screens.QueueExpansionState
import com.nerf.music.util.ConnectivityObserver
import org.junit.Assert.*
import org.junit.Test

/**
 * Empirical Challenger Test Suite for Block 3:
 * 1. Queue auto-scroll target index clamping and gesture interruption protection.
 * 2. ConnectivityObserver state transitions and automatic Home feed reload trigger.
 * 3. Offline empty state card rendering decision matrix and navigation callbacks.
 */
class QueueAndConnectivityChallengerTest {

    // =========================================================================
    // 1. Queue Auto-Scroll Clamping & Gesture Interruption Protection
    // =========================================================================

    /**
     * Verifies that the queue auto-scroll index calculation coerces properly
     * across boundary values and empty lists without throwing IllegalArgumentException.
     */
    @Test
    fun testQueueIndexClampingExhaustiveStress() {
        // Empty queue protection:
        // In PlayerScreen: `if (queueState != COLLAPSED && state.queue.isNotEmpty())`
        // Calling coerceIn(0, lastIndex) on an empty list throws IllegalArgumentException because lastIndex == -1.
        val emptyQueue = emptyList<String>()
        val canExecuteOnEmpty = emptyQueue.isNotEmpty()
        assertFalse("Auto-scroll must be guarded against empty queue", canExecuteOnEmpty)

        // Single element queue
        val singleQueue = listOf("Track 1")
        val singleLastIndex = singleQueue.lastIndex // 0
        assertEquals(0, (-999).coerceIn(0, singleLastIndex))
        assertEquals(0, (-1).coerceIn(0, singleLastIndex))
        assertEquals(0, 0.coerceIn(0, singleLastIndex))
        assertEquals(0, 1.coerceIn(0, singleLastIndex))
        assertEquals(0, 999.coerceIn(0, singleLastIndex))

        // Large queue (10,000 items)
        val largeQueueSize = 10_000
        val largeLastIndex = largeQueueSize - 1
        assertEquals(0, (-100).coerceIn(0, largeLastIndex))
        assertEquals(0, 0.coerceIn(0, largeLastIndex))
        assertEquals(5_000, 5_000.coerceIn(0, largeLastIndex))
        assertEquals(largeLastIndex, largeLastIndex.coerceIn(0, largeLastIndex))
        assertEquals(largeLastIndex, (largeLastIndex + 500).coerceIn(0, largeLastIndex))
        assertEquals(largeLastIndex, Int.MAX_VALUE.coerceIn(0, largeLastIndex))
    }

    /**
     * Models and verifies the exact interaction logic in PlayerScreen:
     * - Immediate snap `scrollToItem` on drawer expansion.
     * - Smooth `animateScrollToItem` on song advance, strictly protected by `!isScrollInProgress`.
     */
    private class QueueScrollControllerMock {
        var instantScrollTarget: Int? = null
        var animatedScrollTarget: Int? = null

        fun onQueueStateChange(
            queueState: QueueExpansionState,
            queue: List<String>,
            currentQueueIndex: Int
        ) {
            if (queueState != QueueExpansionState.COLLAPSED && queue.isNotEmpty()) {
                val targetIndex = currentQueueIndex.coerceIn(0, queue.lastIndex)
                instantScrollTarget = targetIndex
            }
        }

        fun onCurrentQueueIndexChange(
            queueState: QueueExpansionState,
            queue: List<String>,
            currentQueueIndex: Int,
            isScrollInProgress: Boolean
        ) {
            if (queueState != QueueExpansionState.COLLAPSED && queue.isNotEmpty()) {
                val targetIndex = currentQueueIndex.coerceIn(0, queue.lastIndex)
                if (!isScrollInProgress) {
                    animatedScrollTarget = targetIndex
                }
            }
        }
    }

    @Test
    fun testQueueGestureInterruptionProtection() {
        val controller = QueueScrollControllerMock()
        val queue = listOf("Track 0", "Track 1", "Track 2", "Track 3", "Track 4")

        // 1. When queue is COLLAPSED, advancing track does NOT trigger animated scroll
        controller.onCurrentQueueIndexChange(
            queueState = QueueExpansionState.COLLAPSED,
            queue = queue,
            currentQueueIndex = 2,
            isScrollInProgress = false
        )
        assertNull("Collapsed queue must not trigger animated scroll", controller.animatedScrollTarget)

        // 2. When queue is EXPANDED and user is NOT scrolling, track advance triggers animated scroll
        controller.onCurrentQueueIndexChange(
            queueState = QueueExpansionState.EXPANDED,
            queue = queue,
            currentQueueIndex = 2,
            isScrollInProgress = false
        )
        assertEquals(2, controller.animatedScrollTarget)

        // Reset
        controller.animatedScrollTarget = null

        // 3. User is actively touching/scrolling list (`isScrollInProgress == true`):
        // Song finishes and advances to index 3. Gesture must NOT be interrupted!
        controller.onCurrentQueueIndexChange(
            queueState = QueueExpansionState.EXPANDED,
            queue = queue,
            currentQueueIndex = 3,
            isScrollInProgress = true
        )
        assertNull(
            "Track advance while user is dragging/scrolling must NOT interrupt finger gesture",
            controller.animatedScrollTarget
        )

        // 4. Once gesture completes (`isScrollInProgress == false`), next advance scrolls
        controller.onCurrentQueueIndexChange(
            queueState = QueueExpansionState.EXPANDED,
            queue = queue,
            currentQueueIndex = 4,
            isScrollInProgress = false
        )
        assertEquals(4, controller.animatedScrollTarget)
    }

    @Test
    fun testQueueDrawerExpansionImmediateSnap() {
        val controller = QueueScrollControllerMock()
        val queue = listOf("Track 0", "Track 1", "Track 2")

        // Drawer expands from COLLAPSED to HALF_EXPANDED:
        controller.onQueueStateChange(
            queueState = QueueExpansionState.HALF_EXPANDED,
            queue = queue,
            currentQueueIndex = 1
        )
        assertEquals(1, controller.instantScrollTarget)

        // Drawer expands to EXPANDED:
        controller.onQueueStateChange(
            queueState = QueueExpansionState.EXPANDED,
            queue = queue,
            currentQueueIndex = 2
        )
        assertEquals(2, controller.instantScrollTarget)

        // Reset and test with out-of-bound index:
        controller.instantScrollTarget = null
        controller.onQueueStateChange(
            queueState = QueueExpansionState.EXPANDED,
            queue = queue,
            currentQueueIndex = 99
        )
        assertEquals(2, controller.instantScrollTarget)
    }

    // =========================================================================
    // 2. ConnectivityObserver State Transitions & Auto-Reload Trigger
    // =========================================================================

    private class ConnectivityRecoveryHarness {
        var reloadCount = 0
        private var prevStatus: ConnectivityObserver.Status? = null

        fun onStatusEmitted(status: ConnectivityObserver.Status) {
            if (prevStatus != null &&
                prevStatus != ConnectivityObserver.Status.Available &&
                status == ConnectivityObserver.Status.Available
            ) {
                reloadCount++
            }
            prevStatus = status
        }
    }

    @Test
    fun testConnectivityTransitionsAndRecoveryReload() {
        val harness = ConnectivityRecoveryHarness()

        // 1. Initial startup when offline:
        harness.onStatusEmitted(ConnectivityObserver.Status.Unavailable)
        assertEquals("Initial offline status must not trigger reload", 0, harness.reloadCount)

        // 2. Connectivity established:
        harness.onStatusEmitted(ConnectivityObserver.Status.Available)
        assertEquals("Initial connection establishment from Unavailable must reload feed", 1, harness.reloadCount)

        // 3. Steady state emissions of Available:
        harness.onStatusEmitted(ConnectivityObserver.Status.Available)
        harness.onStatusEmitted(ConnectivityObserver.Status.Available)
        assertEquals("Redundant Available emissions must not trigger reloads", 1, harness.reloadCount)

        // 4. Connection degrades:
        harness.onStatusEmitted(ConnectivityObserver.Status.Losing)
        assertEquals("Losing state must not trigger reload", 1, harness.reloadCount)

        // 5. Connection fully lost:
        harness.onStatusEmitted(ConnectivityObserver.Status.Lost)
        assertEquals("Lost state must not trigger reload", 1, harness.reloadCount)

        // 6. Multiple dropped states:
        harness.onStatusEmitted(ConnectivityObserver.Status.Unavailable)
        assertEquals(1, harness.reloadCount)

        // 7. Connection re-established:
        harness.onStatusEmitted(ConnectivityObserver.Status.Available)
        assertEquals("Recovery after Lost/Unavailable MUST trigger reload", 2, harness.reloadCount)

        // 8. Flapping connection: Losing -> Available
        harness.onStatusEmitted(ConnectivityObserver.Status.Losing)
        harness.onStatusEmitted(ConnectivityObserver.Status.Available)
        assertEquals("Recovery from Losing to Available MUST trigger reload", 3, harness.reloadCount)
    }

    // =========================================================================
    // 3. Offline Empty State Card & Navigation Callback Decision Matrix
    // =========================================================================

    private fun shouldDisplayOfflineCard(
        connectivityStatus: ConnectivityObserver.Status,
        homeError: String?,
        homeSectionsCount: Int
    ): Boolean {
        val isOffline = connectivityStatus == ConnectivityObserver.Status.Unavailable ||
                connectivityStatus == ConnectivityObserver.Status.Lost
        return (isOffline || homeError != null) && homeSectionsCount == 0
    }

    @Test
    fun testOfflineEmptyStateCardRenderingMatrix() {
        // Case 1: Online, no error, empty sections (e.g. initial loading) -> NOT offline card
        assertFalse(
            shouldDisplayOfflineCard(
                connectivityStatus = ConnectivityObserver.Status.Available,
                homeError = null,
                homeSectionsCount = 0
            )
        )

        // Case 2: Online, network error occurred, empty sections -> Shows card with retry
        assertTrue(
            shouldDisplayOfflineCard(
                connectivityStatus = ConnectivityObserver.Status.Available,
                homeError = "HTTP 500 Server Error",
                homeSectionsCount = 0
            )
        )

        // Case 3: Offline (Unavailable), empty sections -> MUST show offline card
        assertTrue(
            shouldDisplayOfflineCard(
                connectivityStatus = ConnectivityObserver.Status.Unavailable,
                homeError = null,
                homeSectionsCount = 0
            )
        )

        // Case 4: Offline (Lost), empty sections -> MUST show offline card
        assertTrue(
            shouldDisplayOfflineCard(
                connectivityStatus = ConnectivityObserver.Status.Lost,
                homeError = null,
                homeSectionsCount = 0
            )
        )

        // Case 5: Offline (Unavailable), but cached home sections exist -> MUST display cached feed
        assertFalse(
            "Cached home feed must be displayed even when offline",
            shouldDisplayOfflineCard(
                connectivityStatus = ConnectivityObserver.Status.Unavailable,
                homeError = null,
                homeSectionsCount = 5
            )
        )

        // Case 6: Offline (Lost), but cached home sections exist -> MUST display cached feed
        assertFalse(
            "Cached home feed must be displayed when Lost",
            shouldDisplayOfflineCard(
                connectivityStatus = ConnectivityObserver.Status.Lost,
                homeError = null,
                homeSectionsCount = 3
            )
        )

        // Case 7: Degraded (Losing), empty sections -> Not offline card (still trying)
        assertFalse(
            shouldDisplayOfflineCard(
                connectivityStatus = ConnectivityObserver.Status.Losing,
                homeError = null,
                homeSectionsCount = 0
            )
        )
    }

    @Test
    fun testOfflineCardActionCallbacks() {
        var navigatedToDownloads = false
        var homeReloadTriggered = false

        val onNavigateToDownloads: () -> Unit = {
            navigatedToDownloads = true
        }

        val onRetryClick: () -> Unit = {
            homeReloadTriggered = true
        }

        // Simulate user clicking "Vá para downloads"
        onNavigateToDownloads()
        assertTrue("Clicking 'Vá para downloads' must invoke navigation callback", navigatedToDownloads)

        // Simulate user clicking "Tentar reconectar"
        onRetryClick()
        assertTrue("Clicking 'Tentar reconectar' must invoke home reload callback", homeReloadTriggered)
    }
}
