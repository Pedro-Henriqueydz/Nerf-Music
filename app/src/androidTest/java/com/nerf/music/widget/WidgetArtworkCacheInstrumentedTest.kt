package com.nerf.music.widget

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WidgetArtworkCacheInstrumentedTest {
    @Test fun processedCoverSurvivesSourceRemovalAndPreservesSquareCropAndCorners() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("widget_source", ".png", context.cacheDir)
        val source = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.BLUE)
        file.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
        try {
            val first = WidgetArtworkCache.load(context, file.toURI().toString(), 96, 26)
            assertNotNull(first)
            assertEquals(first!!.bitmap.width, first.bitmap.height)
            assertEquals(0, Color.alpha(first.bitmap.getPixel(0, 0)))
            val center = first.bitmap.width / 2
            assertEquals(Color.BLUE, first.bitmap.getPixel(center, center))
            file.delete()
            val offline = WidgetArtworkCache.load(context, file.toURI().toString(), 96, 26)
            assertNotNull(offline)
            assertEquals(first.color, offline!!.color)
            assertTrue(first.bitmap.sameAs(offline.bitmap))
        } finally { file.delete(); source.recycle() }
    }
}
