package com.nerf.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.nerf.music.MainActivity
import com.nerf.music.R
import com.nerf.music.data.model.Track
import com.nerf.music.data.repository.UserHistoryRepositoryImpl
import com.nerf.music.player.AudioPlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AppWidgetProvider para o Widget Expandido:
 * - Topo: Mini Player da música atual com capa bem arredondada e botões Play/Pause, Next, Prev.
 * - Barra de progresso contínua.
 * - Linha de Capas Recomendadas / Ouvidas Recentemente (Estilo Spotify & YouTube Music).
 * - Ao clicar em qualquer capa recomendada, abre o app e inicia imediatamente a Mix daquela música.
 * - Fundo com cor dinâmica adaptativa extraída da capa (Palette API).
 */
class ExpandedPlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action.startsWith(CompactPlayerWidgetProvider.ACTION_PREFIX)) {
            CompactPlayerWidgetProvider.handlePlayerControlAction(context, action)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_expanded_player)
            val service = AudioPlayerService.getInstance()
            val trackInfo = service?.getCurrentPlayingTrackInfo()
            val isPlaying = service?.isCurrentPlayerPlaying() == true

            // Clique na raiz para abrir o reprodutor completo
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action_open_player", true)
            }
            val appPendingIntent = PendingIntent.getActivity(
                context, 10, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_expanded_root, appPendingIntent)

            // Controles de Play/Pause, Next, Prev
            val playPauseIntent = Intent(context, ExpandedPlayerWidgetProvider::class.java).apply {
                action = CompactPlayerWidgetProvider.ACTION_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context, 11, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_expanded_play_pause, playPausePendingIntent)
            views.setImageViewResource(
                R.id.btn_widget_expanded_play_pause,
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )

            val skipNextIntent = Intent(context, ExpandedPlayerWidgetProvider::class.java).apply {
                action = CompactPlayerWidgetProvider.ACTION_SKIP_NEXT
            }
            val skipNextPendingIntent = PendingIntent.getBroadcast(
                context, 12, skipNextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_expanded_skip_next, skipNextPendingIntent)

            val skipPrevIntent = Intent(context, ExpandedPlayerWidgetProvider::class.java).apply {
                action = CompactPlayerWidgetProvider.ACTION_SKIP_PREV
            }
            val skipPrevPendingIntent = PendingIntent.getBroadcast(
                context, 13, skipPrevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_expanded_skip_prev, skipPrevPendingIntent)

            // Carrega faixas recomendadas / recentes do histórico
            val historyRepo = UserHistoryRepositoryImpl(context)
            val recentTracks = try {
                historyRepo.getRecentlyPlayed().take(8)
            } catch (_: Exception) {
                emptyList()
            }

            val filteredRecs = recentTracks.filter { it.id != trackInfo?.trackId }.ifEmpty { recentTracks }
            val coverViewIds = listOf(
                R.id.iv_rec_cover_1,
                R.id.iv_rec_cover_2,
                R.id.iv_rec_cover_3,
                R.id.iv_rec_cover_4,
                R.id.iv_rec_cover_5
            )

            for (i in coverViewIds.indices) {
                val coverId = coverViewIds[i]
                val track = filteredRecs.getOrNull(i)
                bindRecommendationCover(context, views, coverId, track, 200 + i)
            }

            // Metadados do Player Superior
            if (trackInfo != null && trackInfo.title.isNotBlank()) {
                views.setTextViewText(R.id.tv_widget_title, trackInfo.title)
                views.setTextViewText(R.id.tv_widget_artist, trackInfo.artist)

                val duration = trackInfo.durationMs.coerceAtLeast(1L)
                val pos = trackInfo.positionMs.coerceIn(0L, duration)
                val progressPercent = ((pos.toFloat() / duration.toFloat()) * 100).toInt().coerceIn(0, 100)
                views.setProgressBar(R.id.pb_widget_progress, 100, progressPercent, false)

                val candidate = CompactPlayerWidgetProvider.resolveTrackArtworkUrl(context, trackInfo.trackId, trackInfo.artworkUrl)
                if (!candidate.isNullOrBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        CompactPlayerWidgetProvider.loadCoverAndApplyAdaptiveColor(context, candidate, targetSizeDp = 64f, cornerRadiusDp = 17f) { roundedBitmap, bgColor ->
                            if (roundedBitmap != null) {
                                views.setImageViewBitmap(R.id.iv_widget_cover, roundedBitmap)
                            }
                            if (bgColor != null) {
                                views.setInt(R.id.widget_expanded_root, "setBackgroundColor", bgColor)
                            }
                            withContext(Dispatchers.Main) {
                                appWidgetManager.updateAppWidget(widgetId, views)
                            }
                        }
                    }
                } else {
                    views.setImageViewResource(R.id.iv_widget_cover, R.drawable.ic_widget_placeholder)
                }
            } else if (filteredRecs.isNotEmpty()) {
                val firstRec = filteredRecs.first()
                views.setTextViewText(R.id.tv_widget_title, firstRec.title)
                views.setTextViewText(R.id.tv_widget_artist, "${firstRec.artist} • Toque para tocar")
                views.setProgressBar(R.id.pb_widget_progress, 100, 0, false)

                val candidate = CompactPlayerWidgetProvider.resolveTrackArtworkUrl(context, firstRec.id, firstRec.thumbnailUrl)
                if (!candidate.isNullOrBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        CompactPlayerWidgetProvider.loadCoverAndApplyAdaptiveColor(context, candidate, targetSizeDp = 64f, cornerRadiusDp = 17f) { roundedBitmap, bgColor ->
                            if (roundedBitmap != null) {
                                views.setImageViewBitmap(R.id.iv_widget_cover, roundedBitmap)
                            }
                            if (bgColor != null) {
                                views.setInt(R.id.widget_expanded_root, "setBackgroundColor", bgColor)
                            }
                            withContext(Dispatchers.Main) {
                                appWidgetManager.updateAppWidget(widgetId, views)
                            }
                        }
                    }
                } else {
                    views.setImageViewResource(R.id.iv_widget_cover, R.drawable.ic_widget_placeholder)
                }
            } else {
                views.setTextViewText(R.id.tv_widget_title, "NERF Music")
                views.setTextViewText(R.id.tv_widget_artist, "Abra para ouvir suas músicas")
                views.setProgressBar(R.id.pb_widget_progress, 100, 0, false)
                views.setImageViewResource(R.id.iv_widget_cover, R.drawable.ic_widget_placeholder)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun bindRecommendationCover(
            context: Context,
            views: RemoteViews,
            coverId: Int,
            track: Track?,
            requestCode: Int
        ) {
            if (track != null && track.id.isNotBlank()) {
                views.setViewVisibility(coverId, View.VISIBLE)

                val playTrackIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("action_play_track_id", track.id)
                }
                val pi = PendingIntent.getActivity(
                    context, requestCode, playTrackIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(coverId, pi)

                val candidate = CompactPlayerWidgetProvider.resolveTrackArtworkUrl(context, track.id, track.thumbnailUrl)
                if (!candidate.isNullOrBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        CompactPlayerWidgetProvider.loadCoverAndApplyAdaptiveColor(context, candidate, targetSizeDp = 96f, cornerRadiusDp = 26f) { roundedBitmap, _ ->
                            if (roundedBitmap != null) {
                                views.setImageViewBitmap(coverId, roundedBitmap)
                            }
                        }
                    }
                } else {
                    views.setImageViewResource(coverId, R.drawable.ic_widget_placeholder)
                }
            } else {
                views.setViewVisibility(coverId, View.GONE)
            }
        }
    }
}

