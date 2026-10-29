package com.nerf.music.widget

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.nerf.music.util.ArtworkColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.security.MessageDigest

/** Stores the final crop, not just the downloaded source. Survives process death and offline use. */
internal object WidgetArtworkCache {
    data class Cover(val bitmap: Bitmap, val color: Int)

    suspend fun load(context: Context, url: String, sizeDp: Int, radiusDp: Int): Cover? = withContext(Dispatchers.IO) {
        val density = context.resources.displayMetrics.density
        val size = (sizeDp * density).toInt().coerceAtLeast(1)
        val key = MessageDigest.getInstance("SHA-256").digest("$url:$size:$radiusDp".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val directory = File(context.filesDir, "widget_covers").apply { mkdirs() }
        val image = File(directory, "$key.png")
        val colorFile = File(directory, "$key.color")
        if (image.exists() && colorFile.exists()) {
            val cached = runCatching {
                BitmapFactory.decodeFile(image.path)?.let { Cover(it, colorFile.readText().toInt()) }
            }.getOrNull()
            if (cached != null) {
                image.setLastModified(System.currentTimeMillis())
                return@withContext cached
            }
        }
        val result = try {
            withTimeoutOrNull(4500) {
                context.imageLoader.execute(ImageRequest.Builder(context).data(url).size(384)
                    .allowHardware(false).build())
            } as? SuccessResult
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) { null } ?: return@withContext null
        val source = (result.drawable as? BitmapDrawable)?.bitmap ?: return@withContext null
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, radiusDp * density, radiusDp * density, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val edge = minOf(source.width, source.height)
        val crop = Rect((source.width - edge) / 2, (source.height - edge) / 2,
            (source.width + edge) / 2, (source.height + edge) / 2)
        canvas.drawBitmap(source, crop, rect, paint)
        val cover = Cover(output, ArtworkColors.extract(source).top)
        // A bounded disk cache holds the five recommendations plus recent/current artwork sizes.
        synchronized(this@WidgetArtworkCache) {
            runCatching {
                val temporary = File(directory, "$key.tmp")
                temporary.outputStream().use { output.compress(Bitmap.CompressFormat.PNG, 100, it) }
                if (temporary.renameTo(image)) colorFile.writeText(cover.color.toString())
                directory.listFiles { file -> file.extension == "png" }?.sortedByDescending { it.lastModified() }
                    ?.drop(12)?.forEach { old ->
                        old.delete()
                        File(directory, "${old.nameWithoutExtension}.color").delete()
                    }
            }
        }
        cover
    }
}
