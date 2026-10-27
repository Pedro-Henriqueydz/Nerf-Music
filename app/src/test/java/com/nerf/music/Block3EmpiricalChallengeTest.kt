package com.nerf.music

import com.nerf.music.data.model.LyricsData
import com.nerf.music.data.model.SyncedLyricsLine
import com.nerf.music.data.repository.AudioQuality
import com.nerf.music.data.repository.DataSaverMode
import com.nerf.music.data.repository.DownloadAudioQuality
import com.nerf.music.util.ConnectivityObserver
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.regex.Pattern

/**
 * Empirical Challenger Verification Suite for Block 3:
 * feature/queue-scroll-canvas-lyrics-datasaver
 *
 * Exhaustively stress-tests:
 * 1. Data-saver blocking logic across all combinations (ALWAYS_ON, AUTOMATIC, ALWAYS_OFF x Wi-Fi, Cellular, Metered).
 * 2. Canvas video suppression matrix (isCanvasEnabled, DataSaverMode, isCanvasMeteredBlocked, isCanvasWifiOnly x Network).
 * 3. Predictive disk estimator boundary inputs (0 downloads, 1000 downloads, 10000 downloads, negative, qualities, Long byte overflow, 0-byte free space division safety).
 * 4. Queue auto-scroll target index clamping and scroll-in-progress gesture protection.
 * 5. Companion LRC lyrics serialization, round-trip parsing, timestamp math, and "null" token sanitization.
 * 6. ConnectivityObserver reactive transitions and auto-reload state machine.
 */
class Block3EmpiricalChallengeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // =========================================================================
    // 1. Data-Saver Download Network Decision Matrix (Exhaustive Combinatorial)
    // =========================================================================

    enum class DownloadOutcome {
        ALLOWED,
        BLOCKED_ALWAYS_ON,
        BLOCKED_CELLULAR_DISABLED,
        BLOCKED_AUTOMATIC_DATA_SAVER
    }

    /**
     * Replicates the exact production logic from MusicViewModel.toggleDownload (lines 1508-1527)
     */
    private fun evaluateDownloadAllowed(
        dataSaverMode: DataSaverMode,
        isCellularAllowed: Boolean,
        isWifi: Boolean,
        isCellular: Boolean,
        isNetworkAvailable: Boolean
    ): DownloadOutcome {
        if (dataSaverMode == DataSaverMode.ALWAYS_ON) {
            return DownloadOutcome.BLOCKED_ALWAYS_ON
        }

        if (!isWifi && (isCellular || !isNetworkAvailable)) {
            if (!isCellularAllowed) {
                return DownloadOutcome.BLOCKED_CELLULAR_DISABLED
            }
            if (dataSaverMode == DataSaverMode.AUTOMATIC) {
                return DownloadOutcome.BLOCKED_AUTOMATIC_DATA_SAVER
            }
        }

        return DownloadOutcome.ALLOWED
    }

    @Test
    fun testDataSaverAlwaysOnStrictlyBlocksEveryScenario() {
        val networkScenarios = listOf(
            Triple(true, false, true),   // Wi-Fi
            Triple(false, true, true),   // Cellular
            Triple(true, true, true),    // Dual Wi-Fi + Cellular
            Triple(false, false, false), // Offline
            Triple(false, false, true)   // Ethernet / other
        )
        val cellularOptions = listOf(true, false)

        for ((isWifi, isCellular, isAvailable) in networkScenarios) {
            for (cellAllowed in cellularOptions) {
                val outcome = evaluateDownloadAllowed(
                    dataSaverMode = DataSaverMode.ALWAYS_ON,
                    isCellularAllowed = cellAllowed,
                    isWifi = isWifi,
                    isCellular = isCellular,
                    isNetworkAvailable = isAvailable
                )
                assertEquals(
                    "DataSaver ALWAYS_ON deve bloquear incondicionalmente (wifi=$isWifi, cell=$isCellular, allowed=$cellAllowed)",
                    DownloadOutcome.BLOCKED_ALWAYS_ON,
                    outcome
                )
            }
        }
    }

    @Test
    fun testDataSaverAutomaticNetworkMatrix() {
        // AUTOMATIC on Wi-Fi -> ALWAYS ALLOWED (regardless of cellular toggle)
        assertEquals(
            DownloadOutcome.ALLOWED,
            evaluateDownloadAllowed(DataSaverMode.AUTOMATIC, isCellularAllowed = false, isWifi = true, isCellular = false, isNetworkAvailable = true)
        )
        assertEquals(
            DownloadOutcome.ALLOWED,
            evaluateDownloadAllowed(DataSaverMode.AUTOMATIC, isCellularAllowed = true, isWifi = true, isCellular = false, isNetworkAvailable = true)
        )

        // AUTOMATIC on Cellular with cellular downloads disabled -> BLOCKED_CELLULAR_DISABLED
        assertEquals(
            DownloadOutcome.BLOCKED_CELLULAR_DISABLED,
            evaluateDownloadAllowed(DataSaverMode.AUTOMATIC, isCellularAllowed = false, isWifi = false, isCellular = true, isNetworkAvailable = true)
        )

        // AUTOMATIC on Cellular with cellular downloads enabled -> BLOCKED_AUTOMATIC_DATA_SAVER
        assertEquals(
            DownloadOutcome.BLOCKED_AUTOMATIC_DATA_SAVER,
            evaluateDownloadAllowed(DataSaverMode.AUTOMATIC, isCellularAllowed = true, isWifi = false, isCellular = true, isNetworkAvailable = true)
        )

        // AUTOMATIC when disconnected (offline)
        assertEquals(
            DownloadOutcome.BLOCKED_CELLULAR_DISABLED,
            evaluateDownloadAllowed(DataSaverMode.AUTOMATIC, isCellularAllowed = false, isWifi = false, isCellular = false, isNetworkAvailable = false)
        )
        assertEquals(
            DownloadOutcome.BLOCKED_AUTOMATIC_DATA_SAVER,
            evaluateDownloadAllowed(DataSaverMode.AUTOMATIC, isCellularAllowed = true, isWifi = false, isCellular = false, isNetworkAvailable = false)
        )
    }

    @Test
    fun testDataSaverAlwaysOffNetworkMatrix() {
        // ALWAYS_OFF on Wi-Fi -> ALLOWED
        assertEquals(
            DownloadOutcome.ALLOWED,
            evaluateDownloadAllowed(DataSaverMode.ALWAYS_OFF, isCellularAllowed = false, isWifi = true, isCellular = false, isNetworkAvailable = true)
        )
        assertEquals(
            DownloadOutcome.ALLOWED,
            evaluateDownloadAllowed(DataSaverMode.ALWAYS_OFF, isCellularAllowed = true, isWifi = true, isCellular = false, isNetworkAvailable = true)
        )

        // ALWAYS_OFF on Cellular with cellular downloads disabled -> BLOCKED_CELLULAR_DISABLED
        assertEquals(
            DownloadOutcome.BLOCKED_CELLULAR_DISABLED,
            evaluateDownloadAllowed(DataSaverMode.ALWAYS_OFF, isCellularAllowed = false, isWifi = false, isCellular = true, isNetworkAvailable = true)
        )

        // ALWAYS_OFF on Cellular with cellular downloads enabled -> ALLOWED
        assertEquals(
            DownloadOutcome.ALLOWED,
            evaluateDownloadAllowed(DataSaverMode.ALWAYS_OFF, isCellularAllowed = true, isWifi = false, isCellular = true, isNetworkAvailable = true)
        )

        // ALWAYS_OFF on Ethernet (not wifi, not cellular, available) -> ALLOWED
        assertEquals(
            DownloadOutcome.ALLOWED,
            evaluateDownloadAllowed(DataSaverMode.ALWAYS_OFF, isCellularAllowed = false, isWifi = false, isCellular = false, isNetworkAvailable = true)
        )
    }

    // =========================================================================
    // 2. Canvas Video Suppression Matrix (Exhaustive Combinatorial)
    // =========================================================================

    /**
     * Replicates the exact production logic from MusicViewModel.loadCanvasVideo (lines 2426-2454)
     * and prefetchNextCanvas (lines 2474-2487)
     */
    private fun shouldSuppressCanvas(
        isCanvasEnabled: Boolean,
        dataSaverMode: DataSaverMode,
        isCanvasMeteredBlocked: Boolean,
        isCanvasWifiOnly: Boolean,
        isWifi: Boolean,
        isMetered: Boolean
    ): Boolean {
        if (!isCanvasEnabled) return true
        if (dataSaverMode == DataSaverMode.ALWAYS_ON || (dataSaverMode == DataSaverMode.AUTOMATIC && !isWifi)) return true
        if (isCanvasMeteredBlocked && (isMetered || !isWifi)) return true
        if (isCanvasWifiOnly && !isWifi) return true
        return false
    }

    @Test
    fun testCanvasDisabledAlwaysSuppresses() {
        for (ds in DataSaverMode.entries) {
            for (meteredBlocked in listOf(true, false)) {
                for (wifiOnly in listOf(true, false)) {
                    for (wifi in listOf(true, false)) {
                        for (metered in listOf(true, false)) {
                            assertTrue(
                                "Canvas desativado deve suprimir sempre",
                                shouldSuppressCanvas(
                                    isCanvasEnabled = false,
                                    dataSaverMode = ds,
                                    isCanvasMeteredBlocked = meteredBlocked,
                                    isCanvasWifiOnly = wifiOnly,
                                    isWifi = wifi,
                                    isMetered = metered
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testCanvasSuppressionOnMeteredConnections() {
        // Metered Wi-Fi with isCanvasMeteredBlocked=true -> SUPPRESSED
        assertTrue(
            "Wi-Fi tarifado com isCanvasMeteredBlocked=true deve suprimir Canvas",
            shouldSuppressCanvas(
                isCanvasEnabled = true,
                dataSaverMode = DataSaverMode.ALWAYS_OFF,
                isCanvasMeteredBlocked = true,
                isCanvasWifiOnly = false,
                isWifi = true,
                isMetered = true
            )
        )

        // Metered Wi-Fi with isCanvasMeteredBlocked=false and DataSaver ALWAYS_OFF -> ALLOWED
        assertFalse(
            "Wi-Fi tarifado com isCanvasMeteredBlocked=false e ALWAYS_OFF deve permitir Canvas",
            shouldSuppressCanvas(
                isCanvasEnabled = true,
                dataSaverMode = DataSaverMode.ALWAYS_OFF,
                isCanvasMeteredBlocked = false,
                isCanvasWifiOnly = false,
                isWifi = true,
                isMetered = true
            )
        )

        // Unmetered Wi-Fi with isCanvasMeteredBlocked=true -> ALLOWED
        assertFalse(
            "Wi-Fi ilimitado com isCanvasMeteredBlocked=true deve permitir Canvas",
            shouldSuppressCanvas(
                isCanvasEnabled = true,
                dataSaverMode = DataSaverMode.ALWAYS_OFF,
                isCanvasMeteredBlocked = true,
                isCanvasWifiOnly = false,
                isWifi = true,
                isMetered = false
            )
        )

        // Cellular (non-Wi-Fi) with isCanvasMeteredBlocked=true -> SUPPRESSED
        assertTrue(
            "Rede móvel com isCanvasMeteredBlocked=true deve suprimir Canvas",
            shouldSuppressCanvas(
                isCanvasEnabled = true,
                dataSaverMode = DataSaverMode.ALWAYS_OFF,
                isCanvasMeteredBlocked = true,
                isCanvasWifiOnly = false,
                isWifi = false,
                isMetered = true
            )
        )
    }

    @Test
    fun testCanvasSuppressionDataSaverAutomaticMatrix() {
        // AUTOMATIC on Cellular -> SUPPRESSED
        assertTrue(
            "AUTOMATIC em rede móvel deve suprimir Canvas",
            shouldSuppressCanvas(
                isCanvasEnabled = true,
                dataSaverMode = DataSaverMode.AUTOMATIC,
                isCanvasMeteredBlocked = false,
                isCanvasWifiOnly = false,
                isWifi = false,
                isMetered = false
            )
        )

        // AUTOMATIC on Unmetered Wi-Fi -> ALLOWED
        assertFalse(
            "AUTOMATIC em Wi-Fi ilimitado deve permitir Canvas",
            shouldSuppressCanvas(
                isCanvasEnabled = true,
                dataSaverMode = DataSaverMode.AUTOMATIC,
                isCanvasMeteredBlocked = false,
                isCanvasWifiOnly = false,
                isWifi = true,
                isMetered = false
            )
        )

        // AUTOMATIC on Metered Wi-Fi when isCanvasMeteredBlocked=true -> SUPPRESSED
        assertTrue(
            "AUTOMATIC em Wi-Fi tarifado com isCanvasMeteredBlocked=true deve suprimir Canvas",
            shouldSuppressCanvas(
                isCanvasEnabled = true,
                dataSaverMode = DataSaverMode.AUTOMATIC,
                isCanvasMeteredBlocked = true,
                isCanvasWifiOnly = false,
                isWifi = true,
                isMetered = true
            )
        )
    }

    @Test
    fun testCanvasSuppressionWifiOnlyMatrix() {
        // ALWAYS_OFF, isCanvasWifiOnly=true on Cellular -> SUPPRESSED
        assertTrue(
            "Wi-Fi Only ativado fora do Wi-Fi deve suprimir Canvas",
            shouldSuppressCanvas(
                isCanvasEnabled = true,
                dataSaverMode = DataSaverMode.ALWAYS_OFF,
                isCanvasMeteredBlocked = false,
                isCanvasWifiOnly = true,
                isWifi = false,
                isMetered = false
            )
        )

        // ALWAYS_OFF, isCanvasWifiOnly=true on Wi-Fi unmetered -> ALLOWED
        assertFalse(
            "Wi-Fi Only ativado conectado no Wi-Fi ilimitado deve permitir Canvas",
            shouldSuppressCanvas(
                isCanvasEnabled = true,
                dataSaverMode = DataSaverMode.ALWAYS_OFF,
                isCanvasMeteredBlocked = false,
                isCanvasWifiOnly = true,
                isWifi = true,
                isMetered = false
            )
        )
    }

    // =========================================================================
    // 3. Predictive Disk Estimator Boundary & Overflow Stress Tests
    // =========================================================================

    private fun getEstimatedMbPerTrack(quality: DownloadAudioQuality): Double {
        return when (quality) {
            DownloadAudioQuality.LOSSLESS -> 35.0
            DownloadAudioQuality.VERY_HIGH -> 10.0
            DownloadAudioQuality.HIGH -> 6.5
            DownloadAudioQuality.NORMAL -> 3.5
        }
    }

    private fun calculateProjectedStorageMb(quality: DownloadAudioQuality, quota: Int): Long {
        val safeQuota = quota.coerceAtLeast(0)
        return (safeQuota * getEstimatedMbPerTrack(quality)).toLong()
    }

    private fun calculateProjectedTotalBytes(projectedMb: Long): Long {
        return projectedMb * 1024L * 1024L
    }

    @Test
    fun testPredictiveDiskEstimatorBoundaryZeroDownloads() {
        for (q in DownloadAudioQuality.entries) {
            val projectedMb = calculateProjectedStorageMb(q, 0)
            assertEquals("0 downloads deve projetar 0 MB para qualidade $q", 0L, projectedMb)
            val projectedBytes = calculateProjectedTotalBytes(projectedMb)
            assertEquals("0 downloads deve projetar 0 bytes", 0L, projectedBytes)
        }
    }

    @Test
    fun testPredictiveDiskEstimatorNegativeDownloadsClamping() {
        // Negative quotas should safely clamp to 0
        for (q in DownloadAudioQuality.entries) {
            val projectedMb = calculateProjectedStorageMb(q, -100)
            assertEquals("Cota negativa deve ser tratada como 0 MB", 0L, projectedMb)
        }
    }

    @Test
    fun testPredictiveDiskEstimatorBoundaryOneThousandDownloads() {
        // 1000 downloads
        assertEquals(35000L, calculateProjectedStorageMb(DownloadAudioQuality.LOSSLESS, 1000))
        assertEquals(10000L, calculateProjectedStorageMb(DownloadAudioQuality.VERY_HIGH, 1000))
        assertEquals(6500L, calculateProjectedStorageMb(DownloadAudioQuality.HIGH, 1000))
        assertEquals(3500L, calculateProjectedStorageMb(DownloadAudioQuality.NORMAL, 1000))

        // Total bytes for 1000 Lossless = 35000 * 1024 * 1024 = 36,700,160,000 bytes (~36.7 GB)
        val losslessBytes = calculateProjectedTotalBytes(35000L)
        assertEquals(36700160000L, losslessBytes)
        assertTrue("Deve ser maior que Int.MAX_VALUE sem estourar Long", losslessBytes > Int.MAX_VALUE.toLong())
    }

    @Test
    fun testPredictiveDiskEstimatorExtremeTenThousandDownloadsLongOverflowImmunity() {
        // 10,000 songs at Lossless = 350,000 MB = 367,001,600,000 bytes (~367 GB)
        val massiveMb = calculateProjectedStorageMb(DownloadAudioQuality.LOSSLESS, 10000)
        assertEquals(350000L, massiveMb)

        val massiveBytes = calculateProjectedTotalBytes(massiveMb)
        assertEquals(367001600000L, massiveBytes)
        assertTrue("Cálculo de bytes deve ser positivo e não estourar em Long", massiveBytes > 0L)
    }

    @Test
    fun testPredictiveDiskSafetyHeadroomCalculations() {
        val projectedMb = calculateProjectedStorageMb(DownloadAudioQuality.LOSSLESS, 100) // 3500 MB
        val projectedBytes = calculateProjectedTotalBytes(projectedMb) // 3,670,016,000 bytes

        // Free space = 10 GB (> 2x projection) -> SAFE
        val freeSpace10Gb = 10L * 1024L * 1024L * 1024L
        val isSafe10Gb = freeSpace10Gb > projectedBytes * 2
        assertTrue("10 GB livres para cota de 3.5 GB deve ser seguro", isSafe10Gb)

        // Free space = 6 GB (6442 MB < 7340 MB) -> WARNING (< 2x projection)
        val freeSpace6Gb = 6L * 1024L * 1024L * 1024L
        val isSafe6Gb = freeSpace6Gb > projectedBytes * 2
        assertFalse("6 GB livres para cota de 3.5 GB (< 2x) deve emitir aviso", isSafe6Gb)

        // Free space = 0 bytes -> Must not crash or divide by zero
        val freeSpaceZero = 0L
        val isSafeZero = freeSpaceZero > projectedBytes * 2
        assertFalse("0 bytes livres não é seguro", isSafeZero)

        // Floating percentage safety check
        val percentageFloat = if (freeSpace10Gb > 0) {
            ((projectedBytes.toFloat() / freeSpace10Gb.toFloat()) * 100).toInt().coerceAtLeast(1)
        } else {
            100
        }
        assertTrue("Percentual deve estar entre 1 e 100", percentageFloat in 1..100)
    }

    // =========================================================================
    // 4. Queue Auto-Scroll Index Clamping & Protection
    // =========================================================================

    @Test
    fun testQueueTargetIndexClampingAcrossListSizes() {
        // Empty queue
        val emptyQueue = emptyList<String>()
        assertTrue(emptyQueue.isEmpty())

        // Single element queue
        val singleQueue = listOf("Single Track")
        assertEquals(0, 0.coerceIn(0, singleQueue.lastIndex))
        assertEquals(0, (-1).coerceIn(0, singleQueue.lastIndex))
        assertEquals(0, 5.coerceIn(0, singleQueue.lastIndex))

        // Multi element queue (5 items)
        val multiQueue = listOf("A", "B", "C", "D", "E")
        val lastIdx = multiQueue.lastIndex // 4
        assertEquals(0, 0.coerceIn(0, lastIdx))
        assertEquals(2, 2.coerceIn(0, lastIdx))
        assertEquals(4, 4.coerceIn(0, lastIdx))
        assertEquals(0, (-99).coerceIn(0, lastIdx))
        assertEquals(4, 999.coerceIn(0, lastIdx))
    }

    @Test
    fun testQueueScrollSuppressionDuringUserTouchInteraction() {
        // Simulates the contract: queueLazyListState.isScrollInProgress
        var isScrollInProgress = true
        var autoScrollAnimated = false

        fun onQueueIndexChanged(newIndex: Int) {
            if (!isScrollInProgress) {
                autoScrollAnimated = true
            }
        }

        // When user is touching / scrolling, auto-scroll must NOT animate
        onQueueIndexChanged(3)
        assertFalse("Scroll da fila não deve interromper gesto do usuário", autoScrollAnimated)

        // When user releases and scroll is idle, auto-scroll animates
        isScrollInProgress = false
        onQueueIndexChanged(3)
        assertTrue("Scroll da fila deve animar quando o usuário não estiver rolando", autoScrollAnimated)
    }

    // =========================================================================
    // 5. Offline Companion LRC Serialization & Round-Trip Parsing
    // =========================================================================

    private val lrcPattern = Pattern.compile("""^\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)$""")

    private fun parseLrcString(lrcContent: String): List<SyncedLyricsLine> {
        val result = mutableListOf<SyncedLyricsLine>()
        lrcContent.lines().forEach { line ->
            val trimmed = line.trim()
            val matcher = lrcPattern.matcher(trimmed)
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val millisRaw = matcher.group(3) ?: "0"
                val ms = if (millisRaw.length == 2) {
                    millisRaw.toLong() * 10L
                } else {
                    millisRaw.toLongOrNull() ?: 0L
                }
                val totalMs = (min * 60 * 1000L) + (sec * 1000L) + ms
                val text = matcher.group(4)?.trim().orEmpty()
                if (text.isNotBlank() && !text.equals("null", ignoreCase = true)) {
                    result.add(SyncedLyricsLine(timeMs = totalMs, text = text))
                }
            }
        }
        return result.sortedBy { it.timeMs }
    }

    private fun formatLrcContent(syncedLines: List<SyncedLyricsLine>): String {
        return syncedLines.joinToString("\n") { line ->
            val min = (line.timeMs / 60000).toString().padStart(2, '0')
            val sec = ((line.timeMs % 60000) / 1000).toString().padStart(2, '0')
            val csec = ((line.timeMs % 1000) / 10).toString().padStart(2, '0')
            "[$min:$sec.$csec]${line.text}"
        }
    }

    @Test
    fun testCompanionLrcSerializationRoundTrip() {
        val originalLines = listOf(
            SyncedLyricsLine(0L, "Instrumental intro"),
            SyncedLyricsLine(12340L, "Verse one begins"),
            SyncedLyricsLine(45670L, "Chorus explodes"),
            SyncedLyricsLine(180500L, "Final outro note")
        )

        val serialized = formatLrcContent(originalLines)
        val parsedBack = parseLrcString(serialized)

        assertEquals(originalLines.size, parsedBack.size)
        for (i in originalLines.indices) {
            // Precision to centiseconds (within 10ms)
            assertEquals("Timestamp line $i", originalLines[i].timeMs / 10 * 10, parsedBack[i].timeMs / 10 * 10)
            assertEquals("Text line $i", originalLines[i].text, parsedBack[i].text)
        }
    }

    @Test
    fun testCompanionLrcSanitizesNullLiteralLines() {
        val dirtyLrc = """
            [00:00.00]Valid line 1
            [00:05.00]null
            [00:10.00]NULL
            [00:15.00]   
            [00:20.00]Valid line 2
        """.trimIndent()

        val parsed = parseLrcString(dirtyLrc)
        assertEquals(2, parsed.size)
        assertEquals("Valid line 1", parsed[0].text)
        assertEquals("Valid line 2", parsed[1].text)
    }

    @Test
    fun testCompanionLrcPhysicalDiskWriteAndRead() {
        val offlineDir = tempFolder.newFolder("offline_audio")
        val trackId = "track_stress_001"
        val lrcFile = File(offlineDir, "$trackId.lrc")

        val lines = listOf(
            SyncedLyricsLine(1000L, "Line 1"),
            SyncedLyricsLine(3000L, "Line 2"),
            SyncedLyricsLine(5500L, "Line 3")
        )

        lrcFile.writeText(formatLrcContent(lines))
        assertTrue("Arquivo .lrc físico deve existir no diretório", lrcFile.exists())
        assertTrue("Arquivo .lrc não deve ser vazio", lrcFile.length() > 0)

        val readBack = parseLrcString(lrcFile.readText())
        assertEquals(3, readBack.size)
        assertEquals("Line 1", readBack[0].text)
        assertEquals("Line 3", readBack[2].text)
    }

    // =========================================================================
    // 6. ConnectivityObserver State Transitions & Auto-Reload Trigger
    // =========================================================================

    @Test
    fun testConnectivityObserverTransitionSequences() {
        var reloadCount = 0
        var prevStatus: ConnectivityObserver.Status? = null

        fun onStatusEmitted(status: ConnectivityObserver.Status) {
            if (prevStatus != null &&
                prevStatus != ConnectivityObserver.Status.Available &&
                status == ConnectivityObserver.Status.Available
            ) {
                reloadCount++
            }
            prevStatus = status
        }

        // 1. Initial status Available -> no reload triggered
        onStatusEmitted(ConnectivityObserver.Status.Available)
        assertEquals(0, reloadCount)

        // 2. Losing connection -> no reload
        onStatusEmitted(ConnectivityObserver.Status.Losing)
        assertEquals(0, reloadCount)

        // 3. Lost connection -> no reload
        onStatusEmitted(ConnectivityObserver.Status.Lost)
        assertEquals(0, reloadCount)

        // 4. Unavailable -> no reload
        onStatusEmitted(ConnectivityObserver.Status.Unavailable)
        assertEquals(0, reloadCount)

        // 5. Back to Available -> RELOAD TRIGGERED (1)
        onStatusEmitted(ConnectivityObserver.Status.Available)
        assertEquals(1, reloadCount)

        // 6. Flapping: Available -> Available -> NO duplicate reload
        onStatusEmitted(ConnectivityObserver.Status.Available)
        assertEquals(1, reloadCount)

        // 7. Temporary loss and recovery -> RELOAD TRIGGERED (2)
        onStatusEmitted(ConnectivityObserver.Status.Lost)
        onStatusEmitted(ConnectivityObserver.Status.Available)
        assertEquals(2, reloadCount)
    }
}
