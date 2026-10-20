package com.nerf.music.player

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Serviço em segundo plano gerenciador da reprodução de áudio.
 *
 * Implementa [MediaSessionService] do AndroidX Media3, fornecendo:
 * - Reprodução em segundo plano garantida via Foreground Service do sistema.
 * - Notificação de mídia nativa no Android com controles (Play, Pause, Barra de Progresso).
 * - Suporte a fones de ouvido (Bluetooth, botões físicos de mídia).
 * - Gerenciamento de foco de áudio (pausa ao receber chamadas, ducking em notificações).
 */
class AudioPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Configuração de atributos de áudio para música
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // Tratamento automático de Audio Focus
            .setHandleAudioBecomingNoisy(true)        // Pausa automática ao desconectar fone
            .setWakeMode(C.WAKE_MODE_NETWORK)         // Mantém Wi-Fi acordado durante stream
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
