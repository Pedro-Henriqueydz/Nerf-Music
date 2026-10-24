package com.nerf.music.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.nerf.music.MainActivity

/**
 * Utilitário para emissão e atualização das notificações de progresso
 * dos Downloads Inteligentes (Smart Downloads) do NERF Music.
 */
object SmartDownloadNotificationHelper {

    private const val TAG = "SmartDownloadNotif"
    const val CHANNEL_ID = "nerf_smart_downloads"
    const val NOTIFICATION_ID = 1004

    /**
     * Cria o canal dedicado de notificações para Downloads Inteligentes no Android 8.0+ (API 26+).
     * O nível de importância é BAIXO (IMPORTANCE_LOW) para que atualizações contínuas de progresso
     * não emitam bips ou vibrações repetitivas na barra de status.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Downloads Inteligentes"
            val descriptionText = "Progresso e status da sincronização de faixas offline"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(false)
                setShowBadge(false)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Exibe ou atualiza a notificação de progresso dos Downloads Inteligentes.
     * O contador sobe gradualmente de [currentCount] até o limite [targetQuota].
     */
    fun showProgressNotification(
        context: Context,
        currentCount: Int,
        targetQuota: Int,
        trackTitle: String? = null
    ) {
        val appContext = context.applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                return
            }
        }

        createNotificationChannel(appContext)

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val boundedCurrent = currentCount.coerceIn(0, targetQuota)
        val contentText = if (!trackTitle.isNullOrBlank()) {
            "($boundedCurrent de $targetQuota) • $trackTitle"
        } else {
            "$boundedCurrent de $targetQuota músicas salvas offline"
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloads Inteligentes")
            .setContentText(contentText)
            .setSubText("Offline")
            .setProgress(targetQuota, boundedCurrent, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Atualiza a notificação informando que a sincronização dos Downloads Inteligentes foi concluída.
     */
    fun showCompletedNotification(context: Context, totalDownloaded: Int) {
        val appContext = context.applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        createNotificationChannel(appContext)

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Downloads Inteligentes concluídos")
            .setContentText("$totalDownloaded músicas salvas para reprodução offline.")
            .setSubText("Offline")
            .setProgress(0, 0, false)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
        Log.i(TAG, "Notificação de conclusão emitida para $totalDownloaded músicas.")
    }

    /**
     * Atualiza a notificação indicando que os downloads foram temporariamente pausados.
     */
    fun showPausedNotification(context: Context, reason: String) {
        val appContext = context.applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        createNotificationChannel(appContext)

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloads Inteligentes pausados")
            .setContentText(reason)
            .setSubText("Offline")
            .setProgress(0, 0, false)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Remove a notificação da barra de status.
     */
    fun cancelNotification(context: Context) {
        val notificationManager =
            context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)
    }
}
