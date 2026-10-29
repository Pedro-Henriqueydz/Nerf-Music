package com.nerf.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.widget.RemoteViews
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.nerf.music.MainActivity
import com.nerf.music.R
import com.nerf.music.player.AudioPlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * AppWidgetProvider para o Widget Compacto:
 * Exibe Capa com cantos arredondados de 24dp, Título, Artista, Controles Play/Pause, Next e Prev,
 * com fundo adaptativo de cor extraída da capa (Palette API).
 */
class CompactPlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action.startsWith(ACTION_PREFIX)) {
            handlePlayerControlAction(context, action)
        }
    }

    companion object {
        const val ACTION_PREFIX = "com.nerf.music.widget.ACTION_"
        const val ACTION_PLAY_PAUSE = "com.nerf.music.widget.ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "com.nerf.music.widget.ACTION_SKIP_NEXT"
        const val ACTION_SKIP_PREV = "com.nerf.music.widget.ACTION_SKIP_PREV"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val compactComponent = ComponentName(context, CompactPlayerWidgetProvider::class.java)
            val compactIds = appWidgetManager.getAppWidgetIds(compactComponent)
            if (compactIds.isNotEmpty()) {
                val intent = Intent(context, CompactPlayerWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, compactIds)
                }
                context.sendBroadcast(intent)
            }

            val expandedComponent = ComponentName(context, ExpandedPlayerWidgetProvider::class.java)
            val expandedIds = appWidgetManager.getAppWidgetIds(expandedComponent)
            if (expandedIds.isNotEmpty()) {
                val intent = Intent(context, ExpandedPlayerWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, expandedIds)
                }
                context.sendBroadcast(intent)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_compact_player)
            val service = AudioPlayerService.getInstance()
            val trackInfo = service?.getCurrentPlayingTrackInfo()
            val isPlaying = service?.isCurrentPlayerPlaying() == true

            // Clique na raiz para abrir o reprodutor completo
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action_open_player", true)
            }
            val appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_compact_root, appPendingIntent)

            // Play/Pause
            val playPauseIntent = Intent(context, CompactPlayerWidgetProvider::class.java).apply {
                action = ACTION_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context, 1, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_play_pause, playPausePendingIntent)
            views.setImageViewResource(
                R.id.btn_widget_play_pause,
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )

            // Skip Next
            val skipNextIntent = Intent(context, CompactPlayerWidgetProvider::class.java).apply {
                action = ACTION_SKIP_NEXT
            }
            val skipNextPendingIntent = PendingIntent.getBroadcast(
                context, 2, skipNextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_skip_next, skipNextPendingIntent)

            // Skip Prev
            val skipPrevIntent = Intent(context, CompactPlayerWidgetProvider::class.java).apply {
                action = ACTION_SKIP_PREV
            }
            val skipPrevPendingIntent = PendingIntent.getBroadcast(
                context, 3, skipPrevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_skip_prev, skipPrevPendingIntent)

            // Dados da Música e Capa
            if (trackInfo != null && trackInfo.title.isNotBlank()) {
                views.setTextViewText(R.id.tv_compact_title, trackInfo.title)
                views.setTextViewText(R.id.tv_compact_artist, trackInfo.artist)

                val candidate = resolveTrackArtworkUrl(context, trackInfo.trackId, trackInfo.artworkUrl)
                if (!candidate.isNullOrBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        loadCoverAndApplyAdaptiveColor(context, candidate, targetSizeDp = 50f, cornerRadiusDp = 14f) { roundedBitmap, bgColor ->
                            if (roundedBitmap != null) {
                                views.setImageViewBitmap(R.id.iv_compact_cover, roundedBitmap)
                            }
                            if (bgColor != null) {
                                views.setInt(R.id.widget_compact_root, "setBackgroundColor", bgColor)
                            }
                            withContext(Dispatchers.Main) {
                                appWidgetManager.updateAppWidget(widgetId, views)
                            }
                        }
                    }
                } else {
                    views.setImageViewResource(R.id.iv_compact_cover, R.drawable.ic_widget_placeholder)
                }
            } else {
                views.setTextViewText(R.id.tv_compact_title, "NERF Music")
                views.setTextViewText(R.id.tv_compact_artist, "Toque para ouvir")
                views.setImageViewResource(R.id.iv_compact_cover, R.drawable.ic_widget_placeholder)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        fun handlePlayerControlAction(context: Context, action: String) {
            val service = AudioPlayerService.getInstance() ?: return
            when (action) {
                ACTION_PLAY_PAUSE -> service.togglePlayPause()
                ACTION_SKIP_NEXT -> service.skipToNextTrack()
                ACTION_SKIP_PREV -> service.skipToPreviousTrack()
            }
            updateAllWidgets(context)
        }

        fun resolveTrackArtworkUrl(context: Context, trackId: String, candidateArtwork: String?): String? {
            val offlineFile = File(context.filesDir, "offline_audio/$trackId.jpg")
            return if (offlineFile.exists()) "file://${offlineFile.absolutePath}" else candidateArtwork
        }

        fun roundBitmap(bitmap: Bitmap, cornerRadiusPx: Float, targetSizePx: Int = 0): Bitmap {
            val srcSize = Math.min(bitmap.width, bitmap.height)
            val destSize = if (targetSizePx > 0) targetSizePx else srcSize
            val output = Bitmap.createBitmap(destSize, destSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rectF = RectF(0f, 0f, destSize.toFloat(), destSize.toFloat())
            canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

            val srcRect = Rect(
                (bitmap.width - srcSize) / 2,
                (bitmap.height - srcSize) / 2,
                (bitmap.width + srcSize) / 2,
                (bitmap.height + srcSize) / 2
            )
            canvas.drawBitmap(bitmap, srcRect, rectF, paint)
            return output
        }

        suspend fun loadCoverAndApplyAdaptiveColor(
            context: Context,
            artUrl: String,
            targetSizeDp: Float = 50f,
            cornerRadiusDp: Float = 14f,
            onResult: suspend (Bitmap?, Int?) -> Unit
        ) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(artUrl)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bmp = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bmp != null) {
                        val density = context.resources.displayMetrics.density
                        val targetSizePx = (targetSizeDp * density).toInt().coerceAtLeast(1)
                        val cornerRadiusPx = cornerRadiusDp * density
                        val roundedBmp = roundBitmap(bmp, cornerRadiusPx, targetSizePx)

                        val palette = Palette.from(bmp).generate()
                        val rawColor = palette.dominantSwatch?.rgb ?: palette.darkMutedSwatch?.rgb
                            ?: palette.darkVibrantSwatch?.rgb
                            ?: palette.dominantSwatch?.rgb
                            ?: 0xFF1E1E22.toInt()

                        val r = (Color.red(rawColor) * 0.7f).toInt().coerceIn(18, 120)
                        val g = (Color.green(rawColor) * 0.7f).toInt().coerceIn(18, 120)
                        val b = (Color.blue(rawColor) * 0.7f).toInt().coerceIn(22, 130)
                        val adaptiveColor = Color.argb(0xEE, r, g, b)

                        onResult(roundedBmp, adaptiveColor)
                        return
                    }
                }
            } catch (_: Exception) {}
            onResult(null, null)
        }
    }
}


