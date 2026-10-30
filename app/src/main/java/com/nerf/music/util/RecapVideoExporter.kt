package com.nerf.music.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.Surface
import android.view.Window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Records the visible Recap story so the exported video preserves its real typography and motion. */
object RecapVideoExporter {
    suspend fun export(
        context: Context,
        window: Window,
        width: Int,
        height: Int,
        displayName: String,
        playStory: suspend () -> Unit
    ): Uri = withContext(Dispatchers.Main.immediate) {
        require(width > 0 && height > 0) { "A tela ainda nÃ£o estÃ¡ pronta para exportar." }

        val outputWidth = (width.coerceAtMost(720) / 2) * 2
        val outputHeight = ((height.toFloat() * outputWidth / width).toInt() / 2) * 2
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/NERF Music/Recap")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("NÃ£o foi possÃ­vel criar o vÃ­deo na biblioteca.")

        var recorder: MediaRecorder? = null
        var inputSurface: Surface? = null
        var descriptor: ParcelFileDescriptor? = null
        var bitmap: Bitmap? = null
        var succeeded = false
        try {
            descriptor = resolver.openFileDescriptor(uri, "w")
                ?: error("NÃ£o foi possÃ­vel abrir o ficheiro de vÃ­deo.")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder = mediaRecorder
            mediaRecorder.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(outputWidth, outputHeight)
                setVideoFrameRate(24)
                setVideoEncodingBitRate(4_000_000)
                setOutputFile(descriptor.fileDescriptor)
                prepare()
            }
            val surface = mediaRecorder.surface
            inputSurface = surface
            mediaRecorder.start()
            val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap = frameBitmap
            val destination = Rect(0, 0, outputWidth, outputHeight)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)

            coroutineScope {
                val captureJob = launch {
                    while (currentCoroutineContext().isActive) {
                        val frameStartedAt = System.nanoTime()
                        copyWindowFrame(window, frameBitmap)
                        val canvas = surface.lockCanvas(null)
                        try {
                            canvas.drawBitmap(frameBitmap, null, destination, paint)
                        } finally {
                            surface.unlockCanvasAndPost(canvas)
                        }
                        val frameDuration = 1_000L / 24L
                        val elapsed = (System.nanoTime() - frameStartedAt) / 1_000_000L
                        delay((frameDuration - elapsed).coerceAtLeast(1L))
                    }
                }
                try {
                    playStory()
                } finally {
                    captureJob.cancelAndJoin()
                }
            }
            succeeded = true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } finally {
            bitmap?.recycle()
            runCatching { recorder?.stop() }
            recorder?.release()
            inputSurface?.release()
            descriptor?.let { runCatching { it.close() } }
            if (succeeded) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
            } else {
                resolver.delete(uri, null, null)
            }
        }
        uri
    }

    private suspend fun copyWindowFrame(window: Window, bitmap: Bitmap) = suspendCancellableCoroutine<Unit> { continuation ->
        PixelCopy.request(window, bitmap, { result ->
            if (continuation.isActive) {
                if (result == PixelCopy.SUCCESS) continuation.resume(Unit)
                else continuation.resumeWith(Result.failure(IllegalStateException("NÃ£o foi possÃ­vel capturar um quadro do Recap ($result).")))
            }
        }, Handler(Looper.getMainLooper()))
    }
}
