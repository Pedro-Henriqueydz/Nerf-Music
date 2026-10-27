package com.nerf.music.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncedLyricsWord(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

@Serializable
data class SyncedLyricsLine(
    val timeMs: Long,
    val text: String,
    val endTimeMs: Long = 0L,
    val words: List<SyncedLyricsWord> = emptyList(),
    val translatedText: String? = null,
    val pronunciationText: String? = null
)

@Serializable
data class LyricsData(
    val trackId: String,
    val isSynced: Boolean,
    val plainLyrics: String? = null,
    val syncedLines: List<SyncedLyricsLine> = emptyList(),
    val plainLines: List<SyncedLyricsLine> = emptyList(),
    val source: String = "LRCLIB",
    val detectedLanguage: String? = null,
    val translatedLanguage: String? = null,
    val hasPronunciation: Boolean = false
) {
    val hasValidLyrics: Boolean
        get() {
            val hasValidSynced = isSynced && syncedLines.any { line ->
                line.text.isNotBlank() && !line.text.trim().equals("null", ignoreCase = true)
            }
            val hasValidPlain = !plainLyrics.isNullOrBlank() && !plainLyrics.trim().equals("null", ignoreCase = true)
            return hasValidSynced || hasValidPlain
        }
}
