package com.nerf.music.util

/** Pure rules shared by the renderer, widgets and lyric selection UI. */
object ShareRules {
    const val MAX_LYRIC_LINES = 4

    fun lyricRange(anchor: Int, end: Int, count: Int): List<Int> {
        if (count <= 0 || anchor !in 0 until count) return emptyList()
        val target = end.coerceIn(0, count - 1)
            .coerceIn(anchor - MAX_LYRIC_LINES + 1, anchor + MAX_LYRIC_LINES - 1)
        return (minOf(anchor, target)..maxOf(anchor, target)).toList()
    }

    fun toggleLine(selected: List<Int>, index: Int): List<Int> = when {
        index < 0 -> selected
        index in selected -> selected - index
        selected.size < MAX_LYRIC_LINES -> (selected + index).sorted()
        else -> selected
    }

    fun cleanLines(lines: List<String>): List<String> = lines.map(String::trim)
        .filter { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
        .take(MAX_LYRIC_LINES)

    fun backgroundHsv(hue: Float, saturation: Float, value: Float): FloatArray =
        floatArrayOf(hue, saturation.coerceIn(0f, 0.85f), value.coerceIn(0.38f, 0.68f))
}
