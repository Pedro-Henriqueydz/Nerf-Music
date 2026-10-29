package com.nerf.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.view.View
import android.widget.RemoteViews
import com.nerf.music.MainActivity
import com.nerf.music.R
import com.nerf.music.data.model.Track
import com.nerf.music.data.repository.LibraryRepositoryImpl
import com.nerf.music.data.repository.UserHistoryRepositoryImpl
import com.nerf.music.player.AudioPlayerService
import kotlinx.coroutines.*
import java.io.File

internal object WidgetRenderer {
    enum class Size { FOUR, THREE, TWO }
    const val ACTION_REFRESH = "com.nerf.music.widget.ACTION_REFRESH"

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val generations = mutableMapOf<Int, Int>()
    private val recommendations = intArrayOf(
        R.id.iv_rec_cover_1, R.id.iv_rec_cover_2, R.id.iv_rec_cover_3,
        R.id.iv_rec_cover_4, R.id.iv_rec_cover_5
    )
    fun requestUpdate(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        listOf(CompactPlayerWidgetProvider::class.java, ExpandedPlayerWidgetProvider::class.java, ThreeByOneWidgetProvider::class.java).forEach { type ->
            val ids = manager.getAppWidgetIds(ComponentName(context, type))
            if (ids.isNotEmpty()) context.sendBroadcast(Intent(context, type).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            })
        }
    }

    fun updateProgress(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val info = AudioPlayerService.getInstance()?.getCurrentPlayingTrackInfo()
        val duration = info?.durationMs?.coerceAtLeast(1) ?: 1
        val progress = ((info?.positionMs ?: 0).coerceIn(0, duration) * 100 / duration).toInt()
        val ids = manager.getAppWidgetIds(ComponentName(context, ThreeByOneWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            val views = RemoteViews(context.packageName, R.layout.widget_three_player)
            views.setProgressBar(R.id.pb_three_progress, 100, progress, false)
            manager.partiallyUpdateAppWidget(ids, views)
        }
        val fourIds = manager.getAppWidgetIds(ComponentName(context, ExpandedPlayerWidgetProvider::class.java))
        fourIds.forEach { id ->
            val views = RemoteViews(context.packageName, expandedLayout(manager, id))
            views.setProgressBar(R.id.pb_widget_progress, 100, progress, false)
            manager.partiallyUpdateAppWidget(id, views)
        }
    }

    suspend fun update(context: Context, ids: IntArray, size: Size, partial: Boolean = false): Unit = coroutineScope {
        val manager = AppWidgetManager.getInstance(context)
        if (size == Size.FOUR) {
            val groups = ids.toList().groupBy { expandedLayout(manager, it) }
            if (groups.size > 1) {
                groups.forEach { (_, widgetIds) ->
                    update(context, widgetIds.toIntArray(), size, partial)
                }
                return@coroutineScope
            }
        }
        val service = AudioPlayerService.getInstance()
        val info = service?.getCurrentPlayingTrackInfo()
        val playing = service?.isCurrentPlayerPlaying() == true
        val recent = withContext(Dispatchers.IO) { UserHistoryRepositoryImpl(context).getRecentlyPlayed() }
        val likedTracks = LibraryRepositoryImpl(context).likedTracksFlow.value
        // Keep this row populated for people who have favorites but little listening history.
        val recs = (recent + likedTracks).filter { it.id != info?.trackId }
            .distinctBy { it.id }.take(5)
        val display = info?.let { Track(it.trackId, it.title, it.artist, thumbnailUrl = it.artworkUrl) }
            ?: recs.firstOrNull()
        val layout = when (size) {
            Size.FOUR -> expandedLayout(manager, ids.first())
            Size.THREE -> R.layout.widget_three_player
            Size.TWO -> R.layout.widget_compact_player
        }
        val root = when (size) {
            Size.FOUR -> R.id.widget_expanded_root
            Size.THREE -> R.id.widget_three_root
            Size.TWO -> R.id.widget_compact_root
        }
        val coverId = when (size) {
            Size.FOUR -> R.id.iv_widget_cover
            Size.THREE -> R.id.iv_three_cover
            Size.TWO -> R.id.iv_compact_cover
        }
        val versions = ids.associateWith { id -> (generations.getOrDefault(id, 0) + 1).also { generations[id] = it } }
        val views = RemoteViews(context.packageName, layout)
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action_open_player", true)
        }
        views.setOnClickPendingIntent(root, PendingIntent.getActivity(context, 10, open, flags))
        val titleId = when (size) {
            Size.FOUR -> R.id.tv_widget_title
            Size.THREE -> R.id.tv_three_title
            Size.TWO -> R.id.tv_compact_title
        }
        val artistId = when (size) {
            Size.FOUR -> R.id.tv_widget_artist
            Size.THREE -> R.id.tv_three_artist
            Size.TWO -> R.id.tv_compact_artist
        }
        views.setTextViewText(titleId, display?.title ?: "NERF Music")
        views.setTextViewText(artistId, display?.artist ?: "Toque para ouvir")
        if (!partial) views.setImageViewResource(coverId, R.drawable.ic_widget_placeholder)
        val playId = when (size) {
            Size.FOUR -> R.id.btn_widget_expanded_play_pause
            Size.THREE -> R.id.btn_three_play_pause
            Size.TWO -> R.id.btn_widget_play_pause
        }
        control(context, views, playId, CompactPlayerWidgetProvider.ACTION_PLAY_PAUSE)
        views.setImageViewResource(playId, if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
        views.setContentDescription(playId, if (playing) "Pausar" else "Reproduzir")
        if (size == Size.FOUR) {
            val liked = LibraryRepositoryImpl.sharedLikedTracksFlow.value.any { it.id == info?.trackId }
            views.setImageViewResource(
                R.id.btn_widget_favorite,
                if (liked) R.drawable.ic_widget_heart_filled else R.drawable.ic_widget_heart
            )
            views.setContentDescription(R.id.btn_widget_favorite, if (liked) "Descurtir" else "Curtir")
            control(context, views, R.id.btn_widget_favorite, CompactPlayerWidgetProvider.ACTION_FAVORITE)
            control(context, views, R.id.btn_widget_expanded_skip_prev, CompactPlayerWidgetProvider.ACTION_SKIP_PREV)
            control(context, views, R.id.btn_widget_expanded_skip_next, CompactPlayerWidgetProvider.ACTION_SKIP_NEXT)
            val duration = info?.durationMs?.coerceAtLeast(1) ?: 1
            val progress = ((info?.positionMs ?: 0).coerceIn(0, duration) * 100 / duration).toInt()
            views.setProgressBar(R.id.pb_widget_progress, 100, progress, false)
            recommendations.forEachIndexed { index, viewId ->
                val track = recs.getOrNull(index)
                views.setViewVisibility(viewId, if (track == null) View.INVISIBLE else View.VISIBLE)
                if (!partial) views.setImageViewResource(viewId, R.drawable.ic_widget_placeholder)
                if (track != null) {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("action_play_track_id", track.id)
                    }
                    views.setOnClickPendingIntent(viewId, PendingIntent.getActivity(context, 200 + index, intent, flags))
                    views.setContentDescription(viewId, "${track.title} • ${track.artist}")
                }
            }
        }
        if (size == Size.THREE) {
            control(context, views, R.id.btn_three_skip, CompactPlayerWidgetProvider.ACTION_SKIP_NEXT)
            val duration = info?.durationMs?.coerceAtLeast(1) ?: 1
            val progress = ((info?.positionMs ?: 0).coerceIn(0, duration) * 100 / duration).toInt()
            views.setProgressBar(R.id.pb_three_progress, 100, progress, false)
        }
        if (partial) manager.partiallyUpdateAppWidget(ids, views)
        else manager.updateAppWidget(ids, views)
        suspend fun artwork(track: Track, viewId: Int, size: Int, radius: Int) {
            val offline = File(context.filesDir, "offline_audio/${track.id}.jpg")
            val url = if (offline.exists()) offline.toURI().toString() else track.thumbnailUrl ?: return
            val cover = WidgetArtworkCache.load(context, url, size, radius) ?: return
            val partial = RemoteViews(context.packageName, layout)
            partial.setImageViewBitmap(viewId, cover.bitmap)
            if (viewId == coverId) {
                partial.setColorStateList(root, "setBackgroundTintList", ColorStateList.valueOf(cover.color))
            }
            ids.filter { generations[it] == versions[it] }.forEach { manager.partiallyUpdateAppWidget(it, partial) }
        }
        display?.let {
            val coverSize = if (size == Size.TWO) 60 else 64
            val radius = if (size == Size.TWO) 12 else 14
            launch { artwork(it, coverId, coverSize, radius) }
        }
        if (size == Size.FOUR) {
            recs.take(5).forEachIndexed { index, track ->
                launch { artwork(track, recommendations[index], 96, 14) }
            }
        }
    }

    private val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun expandedLayout(manager: AppWidgetManager, widgetId: Int): Int {
        val height = manager.getAppWidgetOptions(widgetId)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        return if (height >= 240) R.layout.widget_expanded_player
        else R.layout.widget_expanded_player_compact
    }

    private fun control(context: Context, views: RemoteViews, id: Int, action: String) {
        val intent = Intent(context, CompactPlayerWidgetProvider::class.java).setAction(action)
        views.setOnClickPendingIntent(id, PendingIntent.getBroadcast(context, id, intent, flags))
    }
}
