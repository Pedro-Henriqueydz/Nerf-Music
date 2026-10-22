package com.nerf.music.updater

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Worker do Android WorkManager para verificação periódica de atualizações em segundo plano
 * mesmo quando o aplicativo estiver fechado ou o usuário fora do app.
 */
class UpdateCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Iniciando verificação periódica de atualizações em segundo plano...")
        return try {
            val updateManager = AppUpdateManager()
            val updateInfo = updateManager.checkForUpdate()

            if (updateInfo != null) {
                Log.i(TAG, "Nova versão detectada pelo Worker: ${updateInfo.tagName}")
                UpdateNotificationHelper.showUpdateNotification(applicationContext, updateInfo)
            } else {
                Log.d(TAG, "Nenhuma atualização encontrada durante a verificação em segundo plano.")
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Falha na verificação de atualização em segundo plano: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UpdateCheckWorker"
        const val PERIODIC_WORK_NAME = "nerf_periodic_update_check"

        /**
         * Agenda o Worker periódico para executar a cada 4 horas
         * com a restrição de conexão de rede ativa.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Intervalo de 4 horas (mínimo suportado pelo Android é 15 minutos)
            val periodicRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(4, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Log.i(TAG, "Verificação de atualizações agendada via WorkManager (intervalo: 4h).")
        }
    }
}
