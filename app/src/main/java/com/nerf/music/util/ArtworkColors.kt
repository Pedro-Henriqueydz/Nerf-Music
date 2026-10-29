package com.nerf.music.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.palette.graphics.Palette

object ArtworkColors {
    data class Colors(val top: Int, val bottom: Int, val card: Int)

    fun extract(bitmap: Bitmap?): Colors {
        val palette = bitmap?.let { Palette.from(it).generate() }
        val chosen = palette?.dominantSwatch?.rgb ?: palette?.vibrantSwatch?.rgb
            ?: palette?.mutedSwatch?.rgb ?: 0xFF261815.toInt()
        val hsv = FloatArray(3)
        Color.colorToHSV(chosen, hsv)
        val top = ShareRules.backgroundHsv(hsv[0], hsv[1], hsv[2])
        val accent = palette?.lightVibrantSwatch?.rgb ?: palette?.vibrantSwatch?.rgb
            ?: palette?.lightMutedSwatch?.rgb ?: palette?.mutedSwatch?.rgb ?: chosen
        val accentHsv = FloatArray(3)
        Color.colorToHSV(accent, accentHsv)
        return Colors(
            Color.HSVToColor(top),
            Color.HSVToColor(floatArrayOf(top[0], top[1], (top[2] * 0.3f).coerceIn(0.10f, 0.2f))),
            Color.HSVToColor(0x66, floatArrayOf(accentHsv[0], accentHsv[1].coerceIn(0.25f, 0.8f), accentHsv[2].coerceIn(0.75f, 1f)))
        )
    }
}
