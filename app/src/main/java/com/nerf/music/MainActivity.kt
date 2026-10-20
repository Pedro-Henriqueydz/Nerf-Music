package com.nerf.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.nerf.music.ui.screens.SearchScreen
import com.nerf.music.ui.theme.NERFMusicTheme
import com.nerf.music.ui.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializa a conexão com o MediaSessionService em segundo plano
        viewModel.initializePlayerController(this)

        setContent {
            NERFMusicTheme {
                SearchScreen(viewModel = viewModel)
            }
        }
    }
}
