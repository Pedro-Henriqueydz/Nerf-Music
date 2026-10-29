package com.nerf.music

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nerf.music.util.ArtworkColors
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArtworkColorsInstrumentedTest {
    @Test fun dominantAreaWinsOverSmallVibrantAccent() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(55, 80, 135))
        Canvas(bitmap).drawRect(0f, 0f, 10f, 100f, Paint().apply { color = Color.RED })
        val colors = ArtworkColors.extract(bitmap)
        val hsv = FloatArray(3)
        Color.colorToHSV(colors.top, hsv)
        assertTrue("dominant blue hue, got ${hsv[0]}", hsv[0] in 215f..235f)
        assertTrue(hsv[2] in 0.38f..0.69f)
        assertEquals(255, Color.alpha(colors.top))
        bitmap.recycle()
    }

    @Test fun missingBlackAndWhiteArtworkAllProduceReadableOpaqueBackgrounds() {
        assertEquals(255, Color.alpha(ArtworkColors.extract(null).top))
        for (color in listOf(Color.BLACK, Color.WHITE)) {
            val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(color)
            val hsv = FloatArray(3)
            val colors = ArtworkColors.extract(bitmap)
            Color.colorToHSV(colors.top, hsv)
            assertEquals(255, Color.alpha(colors.top))
            assertTrue(hsv[2] in 0.37f..0.69f)
            bitmap.recycle()
        }
    }
}
