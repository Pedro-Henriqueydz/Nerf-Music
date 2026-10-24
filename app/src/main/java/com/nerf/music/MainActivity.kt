package com.nerf.music

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.nerf.music.ui.screens.MainScreen
import com.nerf.music.ui.theme.NERFMusicTheme
import com.nerf.music.ui.viewmodel.MusicViewModel
import com.nerf.music.updater.UpdateCheckWorker
import com.nerf.music.updater.UpdateNotificationHelper
import com.nerf.music.work.SmartDownloadWorker

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializa canal de notificação e agenda verificação em segundo plano via WorkManager
        UpdateNotificationHelper.createNotificationChannel(this)
        com.nerf.music.util.SmartDownloadNotificationHelper.createNotificationChannel(this)
        UpdateCheckWorker.schedule(this)
        SmartDownloadWorker.schedule(this)

        // Inicializa a conexão com o MediaSessionService em segundo plano
        viewModel.initializePlayerController(this)

        handleUpdateIntent(intent)

        setContent {
            NERFMusicTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
    }

    private fun handleUpdateIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(UpdateNotificationHelper.EXTRA_OPEN_UPDATE_DIALOG, false) == true) {
            viewModel.checkForAppUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reconecta e sincroniza a UI com o estado real do serviço de áudio ao voltar para o app
        viewModel.syncWithPlayer()
    }
}
