package com.nerf.music.updater

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
import com.nerf.music.R

/**
 * Utilitário para emissão de notificações do sistema quando novas versões
 * do aplicativo NERF Music estiverem disponíveis no GitHub Releases.
 */
object UpdateNotificationHelper {

    private const val TAG = "UpdateNotification"
    const val CHANNEL_ID = "nerf_app_updates"
    const val NOTIFICATION_ID_UPDATE = 1002

    const val EXTRA_OPEN_UPDATE_DIALOG = "EXTRA_OPEN_UPDATE_DIALOG"
    const val EXTRA_UPDATE_TAG = "EXTRA_UPDATE_TAG"
    const val EXTRA_UPDATE_URL = "EXTRA_UPDATE_URL"

    /**
     * Cria o canal de notificações dedicado a atualizações no Android 8.0+ (API 26+).
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Atualizações do Aplicativo"
            val descriptionText = "Avisos de novas versões e melhorias disponíveis para o NERF Music"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Dispara uma notificação do sistema informando que uma nova versão está pronta.
     * Ao tocar, abre o app e aciona o diálogo de atualização.
     */
    fun showUpdateNotification(context: Context, updateInfo: AppUpdateInfo) {
        val appContext = context.applicationContext

        // Verifica permissão no Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w(TAG, "Permissão POST_NOTIFICATIONS não concedida. Notificação de atualização suprimida.")
                return
            }
        }

        createNotificationChannel(appContext)

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_UPDATE_DIALOG, true)
            putExtra(EXTRA_UPDATE_TAG, updateInfo.tagName)
            putExtra(EXTRA_UPDATE_URL, updateInfo.downloadUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanNotes = updateInfo.releaseNotes
            ?.replace(Regex("(?m)^#+\\s*"), "")
            ?.trim()
            ?.take(400)

        val bigText = if (!cleanNotes.isNullOrBlank()) {
            "Uma nova versão está disponível com novidades:\n\n$cleanNotes\n\nToque para instalar."
        } else {
            "Uma nova versão (${updateInfo.tagName}) do NERF Music está pronta para instalação. Toque para atualizar agora."
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nova versão disponível: ${updateInfo.tagName}")
            .setContentText("Uma nova versão do NERF Music está pronta. Toque para atualizar.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID_UPDATE, notification)
        Log.i(TAG, "Notificação de atualização disparada com sucesso para ${updateInfo.tagName}")
    }

    /**
     * Remove a notificação da barra de status quando a atualização for baixada/concluída.
     */
    fun cancelUpdateNotification(context: Context) {
        val notificationManager =
            context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID_UPDATE)
    }
}
