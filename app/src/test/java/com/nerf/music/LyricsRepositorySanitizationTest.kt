package com.nerf.music

import com.nerf.music.data.repository.optCleanString
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LyricsRepositorySanitizationTest {

    @Test
    fun testOptCleanStringHandlesNullAndLiteralNull() {
        val json = JSONObject()
        json.put("validKey", " Hello World ")
        json.put("emptyKey", "   ")
        json.put("nullLiteralLower", "null")
        json.put("nullLiteralUpper", "NULL")
        json.put("nullLiteralMixed", "Null")
        json.put("jsonNull", JSONObject.NULL)

        assertEquals("Hello World", json.optCleanString("validKey"))
        assertNull(json.optCleanString("emptyKey"))
        assertNull(json.optCleanString("nullLiteralLower"))
        assertNull(json.optCleanString("nullLiteralUpper"))
        assertNull(json.optCleanString("nullLiteralMixed"))
        assertNull(json.optCleanString("jsonNull"))
        assertNull(json.optCleanString("missingKey"))
    }

    @Test
    fun testParseLrclibJsonWithNullFieldsDoesNotYieldLiteralNull() {
        val json = JSONObject()
        json.put("syncedLyrics", JSONObject.NULL)
        json.put("plainLyrics", JSONObject.NULL)

        val synced = json.optCleanString("syncedLyrics")
        val plain = json.optCleanString("plainLyrics")

        assertNull("syncedLyrics nulo deve ser resolvido para null", synced)
        assertNull("plainLyrics nulo deve ser resolvido para null", plain)
    }

    @Test
    fun testParseLrclibJsonWithStringLiteralNullDoesNotYieldLyrics() {
        val json = JSONObject()
        json.put("syncedLyrics", "null")
        json.put("plainLyrics", "null")

        val hasSynced = json.optCleanString("syncedLyrics") != null
        val hasPlain = json.optCleanString("plainLyrics") != null

        assertFalse("Não deve detectar syncedLyrics quando o valor é a string literal 'null'", hasSynced)
        assertFalse("Não deve detectar plainLyrics quando o valor é a string literal 'null'", hasPlain)
    }

    @Test
    fun testLrcLinesOmitLiteralNullVerses() {
        val lrc = """
            [00:01.00]Primeira linha real
            [00:04.50]null
            [00:08.00]Null
            [00:12.00]Terceira linha real
        """.trimIndent()

        val pattern = java.util.regex.Pattern.compile("""^\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)$""")
        val lines = mutableListOf<String>()
        lrc.lines().forEach { line ->
            val matcher = pattern.matcher(line.trim())
            if (matcher.matches()) {
                val text = matcher.group(4)?.trim().orEmpty()
                if (text.isNotBlank() && !text.equals("null", ignoreCase = true)) {
                    lines.add(text)
                }
            }
        }

        assertEquals(2, lines.size)
        assertEquals("Primeira linha real", lines[0])
        assertEquals("Terceira linha real", lines[1])
    }
}
