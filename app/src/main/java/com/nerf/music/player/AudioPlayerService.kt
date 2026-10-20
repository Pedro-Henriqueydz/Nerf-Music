package com.nerf.music.player

import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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

    companion object {
        private const val TAG = "AudioPlayerService"
        private const val STREAM_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Configuração de atributos de áudio para música
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // DataSource HTTP customizado com suporte a redirects e User-Agent compatível com googlevideo
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(STREAM_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(20_000)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(httpDataSourceFactory))
            .setAudioAttributes(audioAttributes, true) // Tratamento automático de Audio Focus
            .setHandleAudioBecomingNoisy(true)        // Pausa automática ao desconectar fone
            .setWakeMode(C.WAKE_MODE_NETWORK)         // Mantém Wi-Fi acordado durante stream
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateName = when (playbackState) {
                    Player.STATE_IDLE -> "STATE_IDLE"
                    Player.STATE_BUFFERING -> "STATE_BUFFERING"
                    Player.STATE_READY -> "STATE_READY"
                    Player.STATE_ENDED -> "STATE_ENDED"
                    else -> "UNKNOWN($playbackState)"
                }
                Log.i(TAG, "ExoPlayer estado alterado: $stateName (isPlaying=${player.isPlaying})")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.i(TAG, "ExoPlayer onIsPlayingChanged: isPlaying=$isPlaying, duration=${player.duration}ms")
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(
                    TAG,
                    "ExoPlayer erro de reprodução [${error.errorCodeName}]: ${error.message}",
                    error
                )
            }
        })

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
