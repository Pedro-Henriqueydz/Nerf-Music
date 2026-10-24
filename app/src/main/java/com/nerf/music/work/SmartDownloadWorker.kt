package com.nerf.music.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nerf.music.data.repository.LibraryRepositoryImpl
import com.nerf.music.data.repository.SmartDownloadManager
import com.nerf.music.data.repository.UserHistoryRepositoryImpl
import com.nerf.music.data.repository.UserSettingsRepositoryImpl
import java.util.concurrent.TimeUnit

/**
 * Worker do Android WorkManager para sincronização contínua dos Smart Downloads
 * em segundo plano, respeitando restrições de bateria e conectividade Wi-Fi.
 */
class SmartDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Iniciando tarefa em segundo plano de Smart Downloads via WorkManager...")

        return try {
            val userSettingsRepo = UserSettingsRepositoryImpl(applicationContext)
            if (!userSettingsRepo.isSmartDownloadsEnabled.value) {
                Log.d(TAG, "Smart Downloads desativado. Finalizando worker sem ações.")
                return Result.success()
            }

            val libraryRepo = LibraryRepositoryImpl(applicationContext)
            val historyRepo = UserHistoryRepositoryImpl(applicationContext)

            val smartDownloadManager = SmartDownloadManager(
                context = applicationContext,
                libraryRepository = libraryRepo,
                userSettingsRepository = userSettingsRepo,
                userHistoryRepository = historyRepo
            )

            val result = smartDownloadManager.syncNow()
            if (result.isSuccess) {
                Log.i(TAG, "Smart Downloads sincronizado com sucesso. Novas faixas baixadas: ${result.getOrNull()}")
                Result.success()
            } else {
                Log.w(TAG, "Sincronização de Smart Downloads retornou falha: ${result.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro fatal no SmartDownloadWorker: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SmartDownloadWorker"
        const val PERIODIC_WORK_NAME = "nerf_smart_downloads_periodic"
        const val IMMEDIATE_WORK_NAME = "nerf_smart_downloads_immediate"

        /**
         * Agenda a rotina periódica a cada 24 horas (ou conforme restrições de rede e bateria).
         */
        fun schedule(context: Context) {
            val settings = UserSettingsRepositoryImpl(context.applicationContext)
            if (!settings.isSmartDownloadsEnabled.value) {
                cancel(context)
                return
            }

            val requiredNetwork = if (settings.isSmartDownloadsWifiOnly.value) {
                NetworkType.UNMETERED
            } else {
                NetworkType.CONNECTED
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(requiredNetwork)
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<SmartDownloadWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
            Log.i(TAG, "SmartDownloadWorker periódico agendado (intervalo: 24h, Wi-Fi=${settings.isSmartDownloadsWifiOnly.value}).")
        }

        /**
         * Dispara uma sincronização imediata em segundo plano com as restrições atuais.
         */
        fun triggerImmediate(context: Context) {
            val settings = UserSettingsRepositoryImpl(context.applicationContext)
            val requiredNetwork = if (settings.isSmartDownloadsWifiOnly.value) {
                NetworkType.UNMETERED
            } else {
                NetworkType.CONNECTED
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(requiredNetwork)
                .setRequiresBatteryNotLow(true)
                .build()

            val immediateRequest = OneTimeWorkRequestBuilder<SmartDownloadWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateRequest
            )
            Log.i(TAG, "Disparo imediato de SmartDownloadWorker enviado ao WorkManager.")
        }

        /**
         * Cancela as tarefas agendadas de Smart Downloads.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(PERIODIC_WORK_NAME)
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(IMMEDIATE_WORK_NAME)
            Log.i(TAG, "Tarefas de Smart Downloads canceladas no WorkManager.")
        }
    }
}
