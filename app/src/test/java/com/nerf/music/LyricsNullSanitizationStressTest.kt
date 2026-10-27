package com.nerf.music

import com.nerf.music.data.model.LyricsData
import com.nerf.music.data.model.SyncedLyricsLine
import com.nerf.music.data.repository.LyricsRepositoryImpl
import com.nerf.music.data.repository.optCleanString
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

/**
 * Empirical stress test suite for lyrics null sanitization across [optCleanString]
 * and [LyricsRepositoryImpl] internal parsers.
 *
 * Verifies all permutations of null representation:
 * - JSONObject.NULL
 * - "null" (lowercase)
 * - "Null" (capitalized)
 * - "NULL" (uppercase)
 * - "nUlL" (mixed case)
 * - " NULL " (padded with whitespace)
 * - "  null  " (padded lowercase)
 * - "\t\r\n  Null  \n\t" (tabs and newlines)
 * - "" (empty string)
 * - "   " (whitespace only)
 * - Missing keys
 * - Legit lyrics containing the substring "null" (e.g. "Nullify", "null and void")
 */
class LyricsNullSanitizationStressTest {

    private lateinit var repository: LyricsRepositoryImpl
    private lateinit var parseLrclibJsonMethod: Method
    private lateinit var parseLrcMethod: Method

    @Before
    fun setUp() {
        repository = LyricsRepositoryImpl()

        // Access internal private parser methods via reflection to empirically verify actual production parsing
        parseLrclibJsonMethod = LyricsRepositoryImpl::class.java.getDeclaredMethod(
            "parseLrclibJson",
            JSONObject::class.java,
            String::class.java
        ).apply { isAccessible = true }

        parseLrcMethod = LyricsRepositoryImpl::class.java.getDeclaredMethod(
            "parseLrc",
            String::class.java
        ).apply { isAccessible = true }
    }

    private fun invokeParseLrclibJson(json: JSONObject, trackId: String = "track_stress_test"): LyricsData? {
        return parseLrclibJsonMethod.invoke(repository, json, trackId) as? LyricsData
    }

    @Suppress("UNCHECKED_CAST")
    private fun invokeParseLrc(lrcText: String): List<SyncedLyricsLine> {
        return parseLrcMethod.invoke(repository, lrcText) as List<SyncedLyricsLine>
    }

    // =========================================================================
    // 1. EXTENSION FUNCTION optCleanString COMPREHENSIVE NULL VARIANT TESTS
    // =========================================================================

    @Test
    fun testOptCleanStringWithAllNullRepresentations() {
        val json = JSONObject()
        json.put("jsonNull", JSONObject.NULL)
        json.put("literalLower", "null")
        json.put("literalCapitalized", "Null")
        json.put("literalUpper", "NULL")
        json.put("literalMixed", "nUlL")
        json.put("paddedUpper", "   NULL   ")
        json.put("paddedLower", "  null  ")
        json.put("paddedWhitespaceTabNewline", "\t\r\n  Null  \n\t")
        json.put("emptyString", "")
        json.put("spacesOnly", "     ")
        json.put("tabsAndNewlinesOnly", "\t\r\n  \t")

        assertNull("JSONObject.NULL must return null", json.optCleanString("jsonNull"))
        assertNull("'null' must return null", json.optCleanString("literalLower"))
        assertNull("'Null' must return null", json.optCleanString("literalCapitalized"))
        assertNull("'NULL' must return null", json.optCleanString("literalUpper"))
        assertNull("'nUlL' must return null", json.optCleanString("literalMixed"))
        assertNull("'   NULL   ' must return null", json.optCleanString("paddedUpper"))
        assertNull("'  null  ' must return null", json.optCleanString("paddedLower"))
        assertNull("Whitespace+tab+newline 'Null' must return null", json.optCleanString("paddedWhitespaceTabNewline"))
        assertNull("Empty string must return null", json.optCleanString("emptyString"))
        assertNull("Spaces only must return null", json.optCleanString("spacesOnly"))
        assertNull("Tabs and newlines must return null", json.optCleanString("tabsAndNewlinesOnly"))
        assertNull("Missing key must return null", json.optCleanString("non_existent_key"))
    }

    @Test
    fun testOptCleanStringPreservesLegitimateLyricsContainingNullSubstring() {
        val json = JSONObject()
        json.put("wordNullify", "Nullify all the sadness")
        json.put("phraseNullAndVoid", "Agreement is null and void")
        json.put("wordAnnulment", "Seeking an annulment")
        json.put("capsAnnul", "ANNUL")

        assertEquals("Nullify all the sadness", json.optCleanString("wordNullify"))
        assertEquals("Agreement is null and void", json.optCleanString("phraseNullAndVoid"))
        assertEquals("Seeking an annulment", json.optCleanString("wordAnnulment"))
        assertEquals("ANNUL", json.optCleanString("capsAnnul"))
    }

    // =========================================================================
    // 2. parseLrclibJson EMPIRICAL TESTING WITH ALL NULL REPRESENTATIONS
    // =========================================================================

    @Test
    fun testParseLrclibJsonReturnsNullWhenBothFieldsAreVariousNullRepresentations() {
        val nullVariants = listOf(
            JSONObject.NULL,
            "null",
            "Null",
            "NULL",
            "  null  ",
            "   NULL   ",
            "\t\r\nNull\n",
            "",
            "   "
        )

        for (variant in nullVariants) {
            val json = JSONObject()
            json.put("syncedLyrics", variant)
            json.put("plainLyrics", variant)

            val result = invokeParseLrclibJson(json)
            assertNull(
                "parseLrclibJson must return null when both fields are '$variant'",
                result
            )
        }
    }

    @Test
    fun testParseLrclibJsonWhenKeysAreCompletelyMissing() {
        val emptyJson = JSONObject()
        val result = invokeParseLrclibJson(emptyJson)
        assertNull("Missing syncedLyrics and plainLyrics must return null", result)
    }

    @Test
    fun testParseLrclibJsonFallbackToPlainLyricsWhenSyncedLyricsIsAnyNullRepresentation() {
        val nullVariants = listOf(
            JSONObject.NULL,
            "null",
            "Null",
            "NULL",
            "   NULL   ",
            ""
        )

        for (nullVariant in nullVariants) {
            val json = JSONObject()
            json.put("syncedLyrics", nullVariant)
            json.put("plainLyrics", "Legitimate plain lyrics line 1\nLine 2")

            val result = invokeParseLrclibJson(json)
            assertNotNull("Must return LyricsData when plainLyrics is valid", result)
            assertFalse("isSynced must be false", result!!.isSynced)
            assertEquals("Legitimate plain lyrics line 1\nLine 2", result.plainLyrics)
            assertTrue("syncedLines must be empty", result.syncedLines.isEmpty())
        }
    }

    @Test
    fun testParseLrclibJsonWhenSyncedLyricsIsValidAndPlainLyricsIsAnyNullRepresentation() {
        val nullVariants = listOf(
            JSONObject.NULL,
            "null",
            "Null",
            "NULL",
            "   NULL   ",
            ""
        )

        val validLrc = """
            [00:01.00]Valid first synced verse
            [00:05.50]Valid second synced verse
        """.trimIndent()

        for (nullVariant in nullVariants) {
            val json = JSONObject()
            json.put("syncedLyrics", validLrc)
            json.put("plainLyrics", nullVariant)

            val result = invokeParseLrclibJson(json)
            assertNotNull("Must return LyricsData when syncedLyrics is valid", result)
            assertTrue("isSynced must be true", result!!.isSynced)
            assertNull("plainLyrics must be null (sanitized from literal '$nullVariant')", result.plainLyrics)
            assertEquals(2, result.syncedLines.size)
            assertEquals("Valid first synced verse", result.syncedLines[0].text)
            assertEquals("Valid second synced verse", result.syncedLines[1].text)
        }
    }

    // =========================================================================
    // 3. parseLrc STRESS SANITIZATION (OMITTING LITERAL NULL AND EMPTY VERSES)
    // =========================================================================

    @Test
    fun testParseLrcOmitsAllLiteralNullPermutationsAndPreservesValidLines() {
        val lrc = """
            [00:01.00] Primeira estrofe real
            [00:02.00] null
            [00:03.00] Null
            [00:04.00] NULL
            [00:05.00]   null   
            [00:06.00]   NULL   
            [00:07.00] 
            [00:08.00]             
            [00:09.00] Segunda estrofe real
            [00:10.00] Nullify the sorrow
            [00:11.00] The contract is null and void
        """.trimIndent()

        val parsed = invokeParseLrc(lrc)

        assertEquals("Must only contain the 4 legitimate verses", 4, parsed.size)
        assertEquals("Primeira estrofe real", parsed[0].text)
        assertEquals(1000L, parsed[0].timeMs)

        assertEquals("Segunda estrofe real", parsed[1].text)
        assertEquals(9000L, parsed[1].timeMs)

        assertEquals("Nullify the sorrow", parsed[2].text)
        assertEquals(10000L, parsed[2].timeMs)

        assertEquals("The contract is null and void", parsed[3].text)
        assertEquals(11000L, parsed[3].timeMs)
    }

    @Test
    fun testParseLrcWhenAllVersesAreNullPermutationsFallsBackToPlainLyrics() {
        val allNullLrc = """
            [00:01.00] null
            [00:02.00] Null
            [00:03.00] NULL
            [00:04.00]   null  
            [00:05.00]
        """.trimIndent()

        val lines = invokeParseLrc(allNullLrc)
        assertTrue("When all LRC verses are null variations, parsed lines must be empty", lines.isEmpty())

        // Now test through parseLrclibJson
        // Scenario A: plainLyrics is also null -> whole result is null
        val jsonA = JSONObject()
        jsonA.put("syncedLyrics", allNullLrc)
        jsonA.put("plainLyrics", "null")
        val resultA = invokeParseLrclibJson(jsonA)
        assertNull("Result must be null when both synced and plain parse to empty/null", resultA)

        // Scenario B: plainLyrics has valid fallback lyrics -> returns unsynced plain lyrics
        val jsonB = JSONObject()
        jsonB.put("syncedLyrics", allNullLrc)
        jsonB.put("plainLyrics", "Letra simples de fallback")
        val resultB = invokeParseLrclibJson(jsonB)
        assertNotNull(resultB)
        assertFalse(resultB!!.isSynced)
        assertEquals("Letra simples de fallback", resultB.plainLyrics)
        assertTrue(resultB.syncedLines.isEmpty())
    }

    // =========================================================================
    // 4. SEARCH LRCLIB JSON CANDIDATE SANITIZATION CHECK
    // =========================================================================

    @Test
    fun testCandidateEvaluationWithNullRepresentations() {
        // Simulates items in the JSONArray returned by /api/search?q=...
        // Items where syncedLyrics is "null" and plainLyrics is JSONObject.NULL
        // should evaluate hasSynced=false and hasPlain=false, properly skipping the corrupt item.

        val corruptItem = JSONObject()
        corruptItem.put("trackName", "Test Song")
        corruptItem.put("artistName", "Test Artist")
        corruptItem.put("duration", 210.0)
        corruptItem.put("syncedLyrics", "null")
        corruptItem.put("plainLyrics", JSONObject.NULL)

        val hasSynced = corruptItem.optCleanString("syncedLyrics") != null
        val hasPlain = corruptItem.optCleanString("plainLyrics") != null

        assertFalse("hasSynced must be false for literal 'null'", hasSynced)
        assertFalse("hasPlain must be false for JSONObject.NULL", hasPlain)
        assertFalse("Candidate must not qualify as having lyrics", hasSynced || hasPlain)
    }
}
