package com.nerf.music.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nerf.music.data.repository.UserSettingsRepository
import com.nerf.music.data.repository.UserSettingsRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Serviço em segundo plano gerenciador da reprodução de áudio.
 *
 * Implementa [MediaSessionService] do AndroidX Media3, fornecendo:
 * - Reprodução em segundo plano garantida via Foreground Service do sistema.
 * - Resolução sob demanda (Just-In-Time) de URIs `nerfmusic://track/{videoId}` via [ResolvingDataSource]
 *   e [NewPipeStreamResolver], permitindo que toda a fila resida nativamente no `currentTimeline` do ExoPlayer.
 * - Buffer de início ultrarrápido (250ms) via [DefaultLoadControl] independente para cada instância do ExoPlayer.
 * - Pre-fetching cooperativo em background com cache e deduplicação de requisições.
 * - Transições inteligentes de faixa em tempo real estilo Spotify (Gapless, Automix e Crossfade de 0s a 12s com curva equal-power).
 * - Notificação de mídia nativa no Android com controles (Play, Pause, Anterior, Próxima, Barra de Progresso).
 */
class AudioPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var activePlayer: ExoPlayer
    private var auxiliaryPlayer: ExoPlayer? = null
    private var fadingPlayer: ExoPlayer? = null

    private lateinit var resolvingDataSourceFactory: ResolvingDataSource.Factory
    private lateinit var audioAttributes: AudioAttributes
    private lateinit var settingsRepository: UserSettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isCrossfading: Boolean = false
    private var crossfadeJob: Job? = null
    private var transitionMonitorJob: Job? = null

    private fun getCrossfadeSeconds(): Int {
        val prefs = applicationContext.getSharedPreferences("nerf_user_settings_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("key_crossfade_seconds", 0)
    }

    private fun isAutomixEnabled(): Boolean {
        val prefs = applicationContext.getSharedPreferences("nerf_user_settings_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("key_automix_enabled", true)
    }

    private fun isGaplessEnabled(): Boolean {
        val prefs = applicationContext.getSharedPreferences("nerf_user_settings_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("key_gapless_enabled", true)
    }

    private fun triggerPrefetchNextTrack() {
        val nextIndex = activePlayer.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET || nextIndex >= activePlayer.mediaItemCount) return
        val nextItem = activePlayer.getMediaItemAt(nextIndex)
        val videoId = nextItem.mediaId.takeIf { it.isNotBlank() }
            ?: nextItem.localConfiguration?.uri?.lastPathSegment?.takeIf { it.isNotBlank() }
            ?: return

        prefetchTrack(videoId, filesDir)
    }

    private fun startTransitionMonitor() {
        transitionMonitorJob?.cancel()
        transitionMonitorJob = serviceScope.launch {
            while (isActive) {
                delay(120)
                checkAndTriggerTransition()
            }
        }
    }

    private fun checkAndTriggerTransition() {
        if (isCrossfading) return
        if (!activePlayer.isPlaying) return

        val duration = activePlayer.duration
        val position = activePlayer.currentPosition
        if (duration <= 0L || position <= 0L) return

        val nextIndex = activePlayer.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET || nextIndex >= activePlayer.mediaItemCount) return

        val crossfadeSec = getCrossfadeSeconds()
        val automix = isAutomixEnabled()

        val targetFadeMs = when {
            crossfadeSec > 0 -> crossfadeSec * 1000L
            automix -> 2000L // 2s de transição suave inteligente do Automix
            else -> 0L
        }

        if (targetFadeMs <= 0L) return

        // Limite de segurança: nunca cruzar mais de um terço da música
        val effectiveFadeMs = targetFadeMs.coerceAtMost(duration / 3)
        val remainingMs = duration - position
        val triggerThresholdMs = effectiveFadeMs + 600L

        if (remainingMs in 100..triggerThresholdMs) {
            executeCrossfade(nextIndex, effectiveFadeMs)
        }
    }

    @OptIn(UnstableApi::class)
    private fun executeCrossfade(nextIndex: Int, fadeDurationMs: Long) {
        if (isCrossfading) return
        isCrossfading = true
        Log.i(TAG, "Iniciando Crossfade dinâmico em tempo real: ${fadeDurationMs}ms para o índice $nextIndex")

        try {
            val outgoingPlayer = activePlayer
            val incomingPlayer = auxiliaryPlayer ?: createExoPlayer(handleAudioFocus = false).also { auxiliaryPlayer = it }

            // Garante que o player secundário NÃO dispute foco de áudio
            incomingPlayer.setAudioAttributes(audioAttributes, false)

            val mediaItems = mutableListOf<MediaItem>()
            for (i in 0 until outgoingPlayer.mediaItemCount) {
                mediaItems.add(outgoingPlayer.getMediaItemAt(i))
            }

            incomingPlayer.volume = 0f
            incomingPlayer.setMediaItems(mediaItems, nextIndex, 0L)
            incomingPlayer.prepare()
            incomingPlayer.play()

            crossfadeJob?.cancel()
            crossfadeJob = serviceScope.launch {
                // Aguarda o incomingPlayer atingir STATE_READY para que o áudio de ambos toque junto sem corte
                var waitTime = 0
                while (incomingPlayer.playbackState != Player.STATE_READY && waitTime < 2500) {
                    delay(35)
                    waitTime += 35
                }

                // Agora que ambos os streams estão ativos e decodificando áudio, atualiza a MediaSession
                fadingPlayer = outgoingPlayer
                activePlayer = incomingPlayer
                mediaSession?.setPlayer(activePlayer)

                val startTime = System.currentTimeMillis()
                while (isActive) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val progress = (elapsed.toFloat() / fadeDurationMs).coerceIn(0f, 1f)

                    // Curva equal-power do Spotify: cos(t * PI/2) e sin(t * PI/2)
                    val volOut = kotlin.math.cos(progress * (Math.PI / 2)).toFloat()
                    val volIn = kotlin.math.sin(progress * (Math.PI / 2)).toFloat()

                    outgoingPlayer.volume = volOut
                    incomingPlayer.volume = volIn

                    if (progress >= 1f) break
                    delay(20)
                }

                outgoingPlayer.stop()
                outgoingPlayer.volume = 1f
                incomingPlayer.volume = 1f

                // Assume o foco oficial do sistema para o player ativo
                incomingPlayer.setAudioAttributes(audioAttributes, true)

                auxiliaryPlayer = outgoingPlayer
                fadingPlayer = null
                isCrossfading = false

                triggerPrefetchNextTrack()
                Log.i(TAG, "Crossfade de ${fadeDurationMs}ms concluído com sucesso. Transição suave finalizada.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao iniciar crossfade: ${e.message}", e)
            isCrossfading = false
            activePlayer.volume = 1f
            fadingPlayer?.stop()
            fadingPlayer = null
            mediaSession?.setPlayer(activePlayer)
        }
    }

    companion object {
        private const val TAG = "AudioPlayerService"
        const val NERF_URI_SCHEME = "nerfmusic"
        private const val CACHE_TTL_MS = 25 * 60 * 1000L // 25 minutos de validade em cache
        private const val STREAM_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

        fun buildTrackUri(videoId: String): Uri = Uri.parse("$NERF_URI_SCHEME://track/$videoId")

        private data class CachedStreamUrl(val url: String, val timestampMs: Long)
        private val streamUrlCache = ConcurrentHashMap<String, CachedStreamUrl>()
        private val inFlightResolutions = ConcurrentHashMap<String, Deferred<String>>()
        private val globalStreamResolver: StreamResolver by lazy { NewPipeStreamResolver() }
        private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun isStreamCached(videoId: String): Boolean {
            val cached = streamUrlCache[videoId] ?: return false
            return (System.currentTimeMillis() - cached.timestampMs) < CACHE_TTL_MS
        }

        /**
         * Pre-busca a URL do fluxo de áudio de forma assíncrona sem interromper operações em andamento.
         */
        fun prefetchTrack(videoId: String, offlineCheckDir: File? = null) {
            if (videoId.isBlank()) return
            if (offlineCheckDir != null) {
                val offlineFile = File(offlineCheckDir, "offline_audio/$videoId.opus")
                if (offlineFile.exists() && offlineFile.length() > 0) return
            }

            val now = System.currentTimeMillis()
            val cached = streamUrlCache[videoId]
            if (cached != null && (now - cached.timestampMs) < CACHE_TTL_MS) return

            val existing = inFlightResolutions[videoId]
            if (existing != null && existing.isActive) return

            val deferred = prefetchScope.async {
                try {
                    Log.i(TAG, "Pre-fetching stream URL em background para: $videoId")
                    val result = globalStreamResolver.resolveStreamUrl(videoId)
                    val url = result.getOrNull()
                    if (!url.isNullOrBlank()) {
                        streamUrlCache[videoId] = CachedStreamUrl(url, System.currentTimeMillis())
                        Log.i(TAG, "Pre-fetch de áudio concluído com sucesso para: $videoId")
                        url
                    } else {
                        throw IOException("URL vazia retornada para $videoId")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Falha no pre-fetch de stream para $videoId: ${e.message}")
                    throw e
                } finally {
                    inFlightResolutions.remove(videoId)
                }
            }
            inFlightResolutions[videoId] = deferred
        }

        /**
         * Resolve a URL de streaming ou aguarda uma pre-busca já em andamento, garantindo 0 requisições duplicadas.
         */
        fun resolveOrAwaitStreamUrl(videoId: String): String {
            val now = System.currentTimeMillis()
            val cached = streamUrlCache[videoId]
            if (cached != null && (now - cached.timestampMs) < CACHE_TTL_MS) {
                Log.i(TAG, "Stream URL obtida diretamente do cache instantâneo para: $videoId")
                return cached.url
            }

            val existing = inFlightResolutions[videoId]
            if (existing != null && existing.isActive) {
                Log.i(TAG, "Aguardando pre-fetch em voo para: $videoId")
                return runBlocking {
                    try {
                        existing.await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Pre-fetch em voo falhou, re-tentando JIT: ${e.message}")
                        val retryUrl = globalStreamResolver.resolveStreamUrl(videoId).getOrThrow()
                        streamUrlCache[videoId] = CachedStreamUrl(retryUrl, System.currentTimeMillis())
                        retryUrl
                    }
                }
            }

            val deferred = prefetchScope.async {
                Log.i(TAG, "Resolvendo URL de stream JIT para: $videoId")
                val result = globalStreamResolver.resolveStreamUrl(videoId).getOrThrow()
                streamUrlCache[videoId] = CachedStreamUrl(result, System.currentTimeMillis())
                result
            }
            inFlightResolutions[videoId] = deferred
            return runBlocking {
                try {
                    deferred.await()
                } finally {
                    inFlightResolutions.remove(videoId)
                }
            }
        }

        fun evictCache(videoId: String) {
            streamUrlCache.remove(videoId)
            inFlightResolutions.remove(videoId)?.cancel()
        }
    }

    @OptIn(UnstableApi::class)
    private fun buildLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 250,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    @OptIn(UnstableApi::class)
    private fun createExoPlayer(handleAudioFocus: Boolean = true): ExoPlayer {
        val playerInstance = ExoPlayer.Builder(this)
            .setLoadControl(buildLoadControl())
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this).setDataSourceFactory(resolvingDataSourceFactory)
            )
            .setAudioAttributes(audioAttributes, handleAudioFocus) // Controla Audio Focus
            .setHandleAudioBecomingNoisy(handleAudioFocus)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        playerInstance.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playerInstance != activePlayer) return
                val stateName = when (playbackState) {
                    Player.STATE_IDLE -> "STATE_IDLE"
                    Player.STATE_BUFFERING -> "STATE_BUFFERING"
                    Player.STATE_READY -> "STATE_READY"
                    Player.STATE_ENDED -> "STATE_ENDED"
                    else -> "UNKNOWN($playbackState)"
                }
                Log.i(TAG, "ExoPlayer estado: $stateName (isPlaying=${playerInstance.isPlaying})")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (playerInstance != activePlayer) return
                Log.i(TAG, "ExoPlayer onIsPlayingChanged: isPlaying=$isPlaying, duration=${playerInstance.duration}ms")
                if (isPlaying) {
                    startTransitionMonitor()
                    triggerPrefetchNextTrack()
                } else if (!isCrossfading) {
                    transitionMonitorJob?.cancel()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (playerInstance != activePlayer) return
                Log.i(TAG, "ExoPlayer onMediaItemTransition: ${mediaItem?.mediaId}, reason=$reason")
                // Trata opção 'Sem pausas' (Gapless): se desativada, insere pausa entre faixas
                if (!isGaplessEnabled() &&
                    !isCrossfading &&
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                ) {
                    activePlayer.pause()
                    serviceScope.launch {
                        delay(1000)
                        if (playerInstance == activePlayer && !activePlayer.isPlaying) {
                            activePlayer.play()
                        }
                    }
                }
                triggerPrefetchNextTrack()
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (playerInstance != activePlayer) return
                triggerPrefetchNextTrack()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK && isCrossfading) {
                    // Se o usuário buscar na timeline durante um crossfade, cancela a transição e restaura volume
                    crossfadeJob?.cancel()
                    fadingPlayer?.stop()
                    fadingPlayer = null
                    activePlayer.volume = 1f
                    isCrossfading = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (playerInstance != activePlayer) {
                    Log.w(TAG, "Erro no player auxiliar ignorado: ${error.message}")
                    return
                }
                playerInstance.currentMediaItem?.mediaId?.let { evictCache(it) }
                Log.e(
                    TAG,
                    "ExoPlayer erro de reprodução [${error.errorCodeName}]: ${error.message}",
                    error
                )
            }
        })

        return playerInstance
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        settingsRepository = UserSettingsRepositoryImpl(this)

        // Configuração de atributos de áudio para música
        audioAttributes = AudioAttributes.Builder()
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
        resolvingDataSourceFactory = ResolvingDataSource.Factory(
            DefaultDataSource.Factory(this, httpDataSourceFactory),
            ResolvingDataSource.Resolver { dataSpec: DataSpec ->
                val uri = dataSpec.uri
                if (uri.scheme == NERF_URI_SCHEME) {
                    val videoId = uri.lastPathSegment?.takeIf { it.isNotBlank() }
                        ?: uri.host?.takeIf { it.isNotBlank() }
                        ?: throw IOException("VideoId ausente na URI: $uri")

                    // 1. Verifica se a faixa já foi baixada offline no disco
                    val offlineFile = File(filesDir, "offline_audio/$videoId.opus")
                    if (offlineFile.exists() && offlineFile.length() > 0) {
                        Log.i(TAG, "Reproduzindo faixa offline local: ${offlineFile.absolutePath}")
                        return@Resolver dataSpec.buildUpon().setUri(Uri.fromFile(offlineFile)).build()
                    }

                    val resolvedUrl = resolveOrAwaitStreamUrl(videoId)
                    dataSpec.buildUpon().setUri(Uri.parse(resolvedUrl)).build()
                } else {
                    dataSpec
                }
            }
        )

        activePlayer = createExoPlayer(handleAudioFocus = true)

        val sessionActivityPendingIntent = packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        mediaSession = MediaSession.Builder(this, activePlayer).apply {
            sessionActivityPendingIntent?.let { setSessionActivity(it) }
        }.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null) {
            player.pause()
            player.stop()
            player.clearMediaItems()
        }
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        transitionMonitorJob?.cancel()
        crossfadeJob?.cancel()
        fadingPlayer?.release()
        fadingPlayer = null
        auxiliaryPlayer?.release()
        auxiliaryPlayer = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
