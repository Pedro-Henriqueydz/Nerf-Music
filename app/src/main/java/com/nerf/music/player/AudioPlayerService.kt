package com.nerf.music.player

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Serviço em segundo plano gerenciador da reprodução de áudio.
 *
 * Implementa [MediaSessionService] do AndroidX Media3, fornecendo:
 * - Reprodução em segundo plano garantida via Foreground Service do sistema.
 * - Resolução sob demanda (Just-In-Time) de URIs `nerfmusic://track/{videoId}` via [ResolvingDataSource]
 *   e [NewPipeStreamResolver], permitindo que toda a fila resida nativamente no `currentTimeline` do ExoPlayer.
 * - Notificação de mídia nativa no Android com controles (Play, Pause, Anterior, Próxima, Barra de Progresso).
 */
class AudioPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val streamResolver: StreamResolver = NewPipeStreamResolver()

    private data class CachedStreamUrl(val url: String, val timestampMs: Long)
    private val streamUrlCache = ConcurrentHashMap<String, CachedStreamUrl>()

    companion object {
        private const val TAG = "AudioPlayerService"
        const val NERF_URI_SCHEME = "nerfmusic"
        private const val CACHE_TTL_MS = 25 * 60 * 1000L // 25 minutos de validade em cache
        private const val STREAM_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

        fun buildTrackUri(videoId: String): Uri = Uri.parse("$NERF_URI_SCHEME://track/$videoId")
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

        // ResolvingDataSource permite que a playlist inteira fique no currentTimeline do ExoPlayer
        // resolvendo a URL real do googlevideo sob demanda quando cada faixa começa a carregar.
        val resolvingDataSourceFactory = ResolvingDataSource.Factory(
            httpDataSourceFactory,
            ResolvingDataSource.Resolver { dataSpec: DataSpec ->
                val uri = dataSpec.uri
                if (uri.scheme == NERF_URI_SCHEME) {
                    val videoId = uri.lastPathSegment?.takeIf { it.isNotBlank() }
                        ?: uri.host?.takeIf { it.isNotBlank() }
                        ?: throw IOException("VideoId ausente na URI: $uri")

                    val now = System.currentTimeMillis()
                    val cached = streamUrlCache[videoId]
                    val resolvedUrl = if (cached != null && (now - cached.timestampMs) < CACHE_TTL_MS) {
                        cached.url
                    } else {
                        Log.i(TAG, "Resolvendo URL de stream JIT para videoId=$videoId")
                        val url = runBlocking {
                            streamResolver.resolveStreamUrl(videoId).getOrThrow()
                        }
                        streamUrlCache[videoId] = CachedStreamUrl(url, now)
                        url
                    }
                    dataSpec.buildUpon().setUri(Uri.parse(resolvedUrl)).build()
                } else {
                    dataSpec
                }
            }
        )

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this).setDataSourceFactory(resolvingDataSourceFactory)
            )
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
                // Caso ocorra erro 403/expiração, limpa o cache da faixa atual
                player.currentMediaItem?.mediaId?.let { streamUrlCache.remove(it) }
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
