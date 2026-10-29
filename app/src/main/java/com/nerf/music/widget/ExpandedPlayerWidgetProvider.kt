package com.nerf.music.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*

class ExpandedPlayerWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WidgetRenderer.ACTION_REFRESH) {
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) ?: intArrayOf()
            if (ids.isNotEmpty()) {
                val pending = goAsync()
                WidgetRenderer.scope.launch {
                    try { withTimeoutOrNull(8000) { WidgetRenderer.update(context.applicationContext, ids, WidgetRenderer.Size.FOUR, partial = true) } }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { android.util.Log.w("NERFWidget", "Falha ao atualizar widget", error) }
                    finally { pending.finish() }
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
            try { withTimeoutOrNull(8000) { WidgetRenderer.update(context.applicationContext, ids, WidgetRenderer.Size.FOUR) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { android.util.Log.w("NERFWidget", "Falha ao atualizar widget", error) }
            finally { pending.finish() }
        }
    }

}
