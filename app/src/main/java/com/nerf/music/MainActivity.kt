package com.nerf.music

import android.content.Intent
import android.os.Build
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

        // Garante a taxa de atualização máxima de 120Hz (LTPO) em telas como Samsung Galaxy S24+
        preferMaximumRefreshRate()

        // Inicializa canal de notificação e agenda verificação em segundo plano via WorkManager
        UpdateNotificationHelper.createNotificationChannel(this)
        com.nerf.music.util.SmartDownloadNotificationHelper.createNotificationChannel(this)
        UpdateCheckWorker.schedule(this)
        SmartDownloadWorker.schedule(this)

        // Inicializa a conexão com o MediaSessionService em segundo plano
        viewModel.initializePlayerController(this)

        handleIncomingIntent(intent)

        setContent {
            NERFMusicTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(UpdateNotificationHelper.EXTRA_OPEN_UPDATE_DIALOG, false) == true) {
            viewModel.checkForAppUpdates()
        }
        val playTrackId = intent?.getStringExtra("action_play_track_id")
        if (!playTrackId.isNullOrBlank()) {
            viewModel.openTrackById(playTrackId)
        } else if (intent?.getBooleanExtra("action_open_player", false) == true) {
            viewModel.openFullPlayer()
        }
        intent?.data?.let { deepLinkUri ->
            viewModel.handleDeepLink(deepLinkUri)
        }
    }

    override fun onResume() {
        super.onResume()
        preferMaximumRefreshRate()
        // Reconecta e sincroniza a UI com o estado real do serviço de áudio ao voltar para o app
        viewModel.syncWithPlayer()
        // A tela inicial do launcher pode ter voltado ao initialLayout após uma atualização do APK.
        com.nerf.music.widget.CompactPlayerWidgetProvider.updateAllWidgets(applicationContext)
    }

    private fun preferMaximumRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val currentDisplay = display ?: return
        val currentMode = currentDisplay.mode
        val maxMode = currentDisplay.supportedModes
            .asSequence()
            .filter {
                it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight
            }
            .maxByOrNull { it.refreshRate }
            ?: return

        if (maxMode.refreshRate < 90f) return
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = maxMode.modeId
            preferredRefreshRate = maxMode.refreshRate
        }
    }
}
