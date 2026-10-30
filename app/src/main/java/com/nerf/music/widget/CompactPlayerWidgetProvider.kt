package com.nerf.music.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class CompactPlayerWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WidgetRenderer.ACTION_REFRESH) {
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) ?: intArrayOf()
            if (ids.isNotEmpty()) {
                val pending = goAsync()
                WidgetRenderer.scope.launch {
                    try {
                        withTimeoutOrNull(8000) { WidgetRenderer.update(context.applicationContext, ids, WidgetRenderer.Size.TWO, partial = true) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        android.util.Log.w("NERFWidget", "Falha ao atualizar widget 2×1", error)
                    } finally {
                        pending.finish()
                    }
                }
            }
            return
        }
        if (intent.action?.startsWith(CompactPlayerWidgetProvider.ACTION_PREFIX) == true) {
            CompactPlayerWidgetProvider.handlePlayerControlAction(context, intent.action!!)
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        WidgetRenderer.scope.launch {
            try {
                withTimeoutOrNull(8000) { WidgetRenderer.update(context.applicationContext, ids, WidgetRenderer.Size.TWO) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.w("NERFWidget", "Falha ao atualizar widget", error)
            } finally {
                pending.finish()
            }
        }
    }
    companion object {
        const val ACTION_PREFIX = "com.nerf.music.widget.ACTION_"
        const val ACTION_PLAY_PAUSE = "${ACTION_PREFIX}PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "${ACTION_PREFIX}SKIP_NEXT"
        const val ACTION_SKIP_PREV = "${ACTION_PREFIX}SKIP_PREV"
        const val ACTION_FAVORITE = "${ACTION_PREFIX}FAVORITE"

        fun updateAllWidgets(context: Context) = WidgetRenderer.requestUpdate(context)
        fun updateProgress(context: Context) = WidgetRenderer.updateProgress(context)

        fun handlePlayerControlAction(context: Context, action: String) {
            val service = com.nerf.music.player.AudioPlayerService.getInstance()
            if (service == null && action == ACTION_PLAY_PAUSE) {
                try {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, com.nerf.music.player.AudioPlayerService::class.java)
                            .setAction(com.nerf.music.player.AudioPlayerService.ACTION_WIDGET_PLAY_MIX)
                    )
                } catch (error: IllegalStateException) {
                    android.util.Log.w("NERFWidget", "O sistema recusou iniciar a reprodução em segundo plano", error)
                }
                return
            }
            service ?: return
            when (action) {
                ACTION_PLAY_PAUSE -> service.togglePlayPause()
                ACTION_SKIP_NEXT -> service.skipToNextTrack()
                ACTION_SKIP_PREV -> service.skipToPreviousTrack()
                ACTION_FAVORITE -> service.toggleWidgetFavorite()
            }
            updateAllWidgets(context)
        }
    }
}
