package com.nerf.music

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nerf.music.data.model.Track
import com.nerf.music.util.SocialShareHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ShareCardInstrumentedTest {
    @Test fun artworkCardsRenderAndProduceVisualReviewArtifacts() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.rgb(55, 80, 135))
        Canvas(source).apply {
            drawCircle(300f, 300f, 200f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(230, 120, 80) })
            drawCircle(300f, 300f, 50f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(55, 80, 135) })
        }
        val file = File.createTempFile("card_source", ".png", context.cacheDir)
        file.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val track = Track("visual_test", "Um novo jeito de ouvir", "NERF Music", thumbnailUrl = file.toURI().toString())
        val output = File(context.getExternalFilesDir(null), "share-validation").apply { mkdirs() }
        try {
            for (style in SocialShareHelper.Style.entries) {
                for (lines in listOf(emptyList(), listOf("Tudo muda com a música", "Cada verso conta uma história", "Um instante para guardar", "E compartilhar com você"))) {
                    val bitmap = SocialShareHelper.renderCard(context, track, lines, style)
                    assertEquals(1080, bitmap.width)
                    File(output, "${style.name}_${if (lines.isEmpty()) "music" else "lyrics"}.png").outputStream()
                        .use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
            }
        } finally { file.delete(); source.recycle() }
    }

    @Test fun allStylesRenderWithoutArtworkAndExportTheExactPreview() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val track = Track("test", "Uma música com título longo ".repeat(8), "Artista 日本語 🎵")
        val pixels = mutableSetOf<Int>()
        for (style in SocialShareHelper.Style.entries) {
            for (lines in listOf(emptyList(), List(4) { "Uma linha de letra longa para testar a quebra de texto ".repeat(3) })) {
                val bitmap = SocialShareHelper.renderCard(context, track, lines, style)
                assertEquals(1080, bitmap.width)
                assertEquals(1920, bitmap.height)
                pixels += bitmap.getPixel(540, 960)
                val uri = SocialShareHelper.writeShareImage(context, bitmap)
                assertEquals("content", uri.scheme)
                val exported = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
                assertNotNull(exported)
                assertTrue(bitmap.sameAs(exported))
                exported.recycle()
                bitmap.recycle()
            }
        }
        assertTrue("Styles should produce distinct backgrounds", pixels.size >= 2)
    }
}
