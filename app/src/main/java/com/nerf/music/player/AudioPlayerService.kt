package com.nerf.music.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.audiofx.AudioEffect
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nerf.music.data.repository.AudioQuality
import com.nerf.music.data.repository.LibraryRepositoryImpl
import com.nerf.music.data.repository.LoudnessStandard
import com.nerf.music.data.repository.UserSettingsRepository
import com.nerf.music.data.repository.UserSettingsRepositoryImpl
import com.nerf.music.util.NetworkUtils
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
import kotlinx.coroutines.withContext
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
    private lateinit var playerA: ExoPlayer
    private lateinit var playerB: ExoPlayer
    private lateinit var activePlayer: ExoPlayer
    private lateinit var instrumentalPlayer: ExoPlayer
    private var fadingPlayer: ExoPlayer? = null
    private val vocalProcessors = mutableListOf<VocalIsolationAudioProcessor>()
    private val loudnessProcessors = mutableListOf<LoudnessNormalizationAudioProcessor>()
    private val playerProcessorMap = HashMap<ExoPlayer, VocalIsolationAudioProcessor>()
    private val instrumentalEnvelopeProcessor = VocalIsolationAudioProcessor().apply {
        isKaraokeEnabled = false
    }

    private var currentInstrumentalOriginalTrackId: String? = null
    private var currentInstrumentalVideoId: String? = null
    private var isInstrumentalStreamReady: Boolean = false
    private var instrumentalOffsetMs: Long = 0L
    private var hasAutoAlignedCurrentInstrumental: Boolean = false
    private var karaokeCrossfadeJob: Job? = null
    private var instrumentalSyncMonitorJob: Job? = null
    private var beatAlignmentJob: Job? = null

    fun prepareInstrumentalForTrack(
        originalTrackId: String,
        instrumentalVideoId: String,
        initialOffsetMs: Long = 0L
    ) {
        if (originalTrackId.isBlank() || instrumentalVideoId.isBlank()) return
        prefetchTrack(instrumentalVideoId, filesDir)
        serviceScope.launch {
            if (!::instrumentalPlayer.isInitialized) return@launch
            if (currentInstrumentalOriginalTrackId == originalTrackId &&
                currentInstrumentalVideoId == instrumentalVideoId &&
                isInstrumentalStreamReady
            ) {
                return@launch
            }

            currentInstrumentalOriginalTrackId = originalTrackId
            currentInstrumentalVideoId = instrumentalVideoId
            isInstrumentalStreamReady = false
            isUsingRealInstrumental = false
            instrumentalOffsetMs = initialOffsetMs
            hasAutoAlignedCurrentInstrumental = false
            instrumentalSyncMonitorJob?.cancel()
            beatAlignmentJob?.cancel()

            Log.i(TAG, "Pre-carregando faixa Instrumental em standby ($instrumentalVideoId) para $originalTrackId (offsetInicial=${initialOffsetMs}ms)")
            instrumentalPlayer.stop()
            instrumentalPlayer.clearMediaItems()
            instrumentalPlayer.volume = 0f
            instrumentalPlayer.setPlaybackParameters(PlaybackParameters.DEFAULT)
            instrumentalPlayer.playWhenReady = false
            instrumentalEnvelopeProcessor.clearEnvelopeHistory()
            val item = MediaItem.Builder()
                .setMediaId(instrumentalVideoId)
                .setUri(buildTrackUri(instrumentalVideoId))
                .build()
            instrumentalPlayer.setMediaItem(item)
            instrumentalPlayer.prepare()
        }
    }

    fun clearInstrumentalStandby() {
        serviceScope.launch {
            currentInstrumentalOriginalTrackId = null
            currentInstrumentalVideoId = null
            isInstrumentalStreamReady = false
            isUsingRealInstrumental = false
            instrumentalOffsetMs = 0L
            hasAutoAlignedCurrentInstrumental = false
            karaokeCrossfadeJob?.cancel()
            instrumentalSyncMonitorJob?.cancel()
            beatAlignmentJob?.cancel()
            instrumentalEnvelopeProcessor.isEnvelopeCaptureEnabled = false
            vocalProcessors.forEach { it.isEnvelopeCaptureEnabled = false }
            if (::instrumentalPlayer.isInitialized) {
                instrumentalPlayer.pause()
                instrumentalPlayer.stop()
                instrumentalPlayer.clearMediaItems()
                instrumentalPlayer.volume = 0f
                instrumentalPlayer.setPlaybackParameters(PlaybackParameters.DEFAULT)
            }
            if (::activePlayer.isInitialized && !isCrossfading) {
                activePlayer.volume = 1f
            }
        }
    }

    /**
     * Mantém o `instrumentalPlayer` em sincronia contínua (Lock-Step) com o `activePlayer`,
     * tanto durante o standby silencioso (`volume = 0f`) quanto durante o Karaokê ativo (`volume = 1f`).
     * Usa micro-ajuste de velocidade (`0.97x` / `1.03x`) para desvios pequenos (22ms..180ms) sem clique audível,
     * e `seekTo` compensado apenas para desvios maiores (> 180ms).
     */
    private fun startInstrumentalLockStepMonitor() {
        instrumentalSyncMonitorJob?.cancel()
        instrumentalSyncMonitorJob = serviceScope.launch {
            while (isActive) {
                if (!::activePlayer.isInitialized || !::instrumentalPlayer.isInitialized) break
                val currentMediaId = activePlayer.currentMediaItem?.mediaId
                val matchesTrack = currentMediaId != null && currentMediaId == currentInstrumentalOriginalTrackId
                if (!matchesTrack || !isInstrumentalStreamReady) {
                    delay(250)
                    continue
                }

                if (activePlayer.isPlaying) {
                    if (!instrumentalPlayer.isPlaying && instrumentalPlayer.playbackState == Player.STATE_READY) {
                        val compensated = (activePlayer.currentPosition + instrumentalOffsetMs + 35L).coerceAtLeast(0L)
                        instrumentalPlayer.seekTo(compensated)
                        instrumentalPlayer.play()
                    }
                    val targetPos = (activePlayer.currentPosition + instrumentalOffsetMs).coerceAtLeast(0L)
                    val instPos = instrumentalPlayer.currentPosition
                    val driftMs = instPos - targetPos // > 0 = instrumental adiantado; < 0 = instrumental atrasado

                    when {
                        kotlin.math.abs(driftMs) > 180L -> {
                            instrumentalPlayer.setPlaybackParameters(PlaybackParameters.DEFAULT)
                            instrumentalPlayer.seekTo((targetPos + 35L).coerceAtLeast(0L))
                        }
                        driftMs > 22L -> {
                            if (instrumentalPlayer.playbackParameters.speed != 0.97f) {
                                instrumentalPlayer.setPlaybackParameters(PlaybackParameters(0.97f, 1.0f))
                            }
                        }
                        driftMs < -22L -> {
                            if (instrumentalPlayer.playbackParameters.speed != 1.03f) {
                                instrumentalPlayer.setPlaybackParameters(PlaybackParameters(1.03f, 1.0f))
                            }
                        }
                        else -> {
                            if (instrumentalPlayer.playbackParameters.speed != 1.0f) {
                                instrumentalPlayer.setPlaybackParameters(PlaybackParameters.DEFAULT)
                            }
                        }
                    }
                } else {
                    if (instrumentalPlayer.isPlaying) {
                        instrumentalPlayer.pause()
                    }
                    if (instrumentalPlayer.playbackParameters.speed != 1.0f) {
                        instrumentalPlayer.setPlaybackParameters(PlaybackParameters.DEFAULT)
                    }
                }
                delay(180)
            }
        }
    }

    /**
     * Captura simultaneamente os envelopes de sub-grave (< 115 Hz) do `activePlayer` e do `instrumentalPlayer`
     * a partir do mesmo instante de tempo real e calcula a Correlação Cruzada de Onsets para descobrir
     * automaticamente qualquer diferença de introdução/tempo entre a música original e o instrumental.
     */
    private fun triggerAutoBeatAlignmentIfNeeded() {
        if (hasAutoAlignedCurrentInstrumental) return
        if (!::activePlayer.isInitialized || !::instrumentalPlayer.isInitialized) return
        val activeProc = playerProcessorMap[activePlayer] ?: return

        beatAlignmentJob?.cancel()
        beatAlignmentJob = serviceScope.launch(Dispatchers.Default) {
            var waitAttempts = 0
            while (isActive && waitAttempts < 30) {
                val bothPlaying = withContext(Dispatchers.Main) {
                    activePlayer.isPlaying && instrumentalPlayer.isPlaying &&
                        kotlin.math.abs(
                            instrumentalPlayer.currentPosition - (activePlayer.currentPosition + instrumentalOffsetMs).coerceAtLeast(0L)
                        ) < 120L
                }
                if (bothPlaying) break
                waitAttempts++
                delay(200)
            }

            activeProc.isEnvelopeCaptureEnabled = true
            instrumentalEnvelopeProcessor.isEnvelopeCaptureEnabled = true
            activeProc.clearEnvelopeHistory()
            instrumentalEnvelopeProcessor.clearEnvelopeHistory()

            delay(4800)
            if (!isActive) {
                activeProc.isEnvelopeCaptureEnabled = false
                instrumentalEnvelopeProcessor.isEnvelopeCaptureEnabled = false
                return@launch
            }

            val mainEnv = activeProc.getCapturedBassEnvelope()
            val instEnv = instrumentalEnvelopeProcessor.getCapturedBassEnvelope()
            activeProc.isEnvelopeCaptureEnabled = false
            instrumentalEnvelopeProcessor.isEnvelopeCaptureEnabled = false
            val detectedLagMs = VocalIsolationAudioProcessor.computeBestBeatOffsetMs(
                mainEnvelope = mainEnv,
                instEnvelope = instEnv,
                maxLagMs = 2400
            )
            hasAutoAlignedCurrentInstrumental = true
            if (detectedLagMs != null && kotlin.math.abs(detectedLagMs) in 25L..2400L) {
                withContext(Dispatchers.Main) {
                    instrumentalOffsetMs += detectedLagMs
                    Log.i(
                        TAG,
                        "Auto-Beat Alignment concluiu ajuste de sincronia: delta=${detectedLagMs}ms (offsetTotal=${instrumentalOffsetMs}ms)"
                    )
                }
            } else {
                Log.i(TAG, "Auto-Beat Alignment: faixas já estão alinhadas (delta=${detectedLagMs ?: 0L}ms)")
            }
        }
    }

    fun applyKaraokeState(enabled: Boolean, vocalLevel: Float) {
        val clamped = vocalLevel.coerceIn(0f, 1f)
        serviceScope.launch {
            if (!::activePlayer.isInitialized || !::instrumentalPlayer.isInitialized) {
                vocalProcessors.forEach { processor ->
                    processor.isKaraokeEnabled = enabled
                    processor.vocalLevel = clamped
                }
                return@launch
            }

            val currentMediaId = activePlayer.currentMediaItem?.mediaId
            val hasMatchingInstrumental = enabled &&
                currentMediaId != null &&
                currentMediaId == currentInstrumentalOriginalTrackId &&
                (isInstrumentalStreamReady || instrumentalPlayer.playbackState == Player.STATE_READY)

            if (hasMatchingInstrumental) {
                isUsingRealInstrumental = true
                vocalProcessors.forEach { processor ->
                    processor.isKaraokeEnabled = false
                }

                val targetPos = (activePlayer.currentPosition + instrumentalOffsetMs).coerceAtLeast(0L)
                if (kotlin.math.abs(instrumentalPlayer.currentPosition - targetPos) > 65L) {
                    instrumentalPlayer.seekTo((targetPos + 35L).coerceAtLeast(0L))
                }
                if (activePlayer.isPlaying && !instrumentalPlayer.isPlaying) {
                    instrumentalPlayer.play()
                } else if (!activePlayer.isPlaying && instrumentalPlayer.isPlaying) {
                    instrumentalPlayer.pause()
                }
                startInstrumentalLockStepMonitor()
                triggerAutoBeatAlignmentIfNeeded()

                karaokeCrossfadeJob?.cancel()
                karaokeCrossfadeJob = serviceScope.launch {
                    val startMainVol = activePlayer.volume
                    val startInstVol = instrumentalPlayer.volume
                    val targetMainVol = clamped * clamped
                    val targetInstVol = 1.0f
                    for (step in 1..4) {
                        val frac = step / 4f
                        activePlayer.volume = (startMainVol + (targetMainVol - startMainVol) * frac).coerceIn(0f, 1f)
                        instrumentalPlayer.volume = (startInstVol + (targetInstVol - startInstVol) * frac).coerceIn(0f, 1f)
                        delay(15)
                    }
                    activePlayer.volume = targetMainVol
                    instrumentalPlayer.volume = targetInstVol
                }
                onInstrumentalStatusChanged?.invoke(true, true)
            } else if (enabled) {
                isUsingRealInstrumental = false
                karaokeCrossfadeJob?.cancel()
                instrumentalPlayer.volume = 0f
                if (!isCrossfading) {
                    activePlayer.volume = 1f
                }
                vocalProcessors.forEach { processor ->
                    processor.isKaraokeEnabled = true
                    processor.vocalLevel = clamped
                }
                onInstrumentalStatusChanged?.invoke(true, false)
            } else {
                val wasUsingInst = isUsingRealInstrumental
                isUsingRealInstrumental = false
                vocalProcessors.forEach { processor ->
                    processor.isKaraokeEnabled = false
                    processor.vocalLevel = clamped
                }
                karaokeCrossfadeJob?.cancel()
                if (wasUsingInst || instrumentalPlayer.volume > 0.01f || activePlayer.volume < 0.99f) {
                    karaokeCrossfadeJob = serviceScope.launch {
                        val startMainVol = activePlayer.volume
                        val startInstVol = instrumentalPlayer.volume
                        for (step in 1..4) {
                            val frac = step / 4f
                            activePlayer.volume = (startMainVol + (1f - startMainVol) * frac).coerceIn(0f, 1f)
                            instrumentalPlayer.volume = (startInstVol * (1f - frac)).coerceIn(0f, 1f)
                            delay(15)
                        }
                        activePlayer.volume = 1f
                        instrumentalPlayer.volume = 0f
                    }
                }
                onInstrumentalStatusChanged?.invoke(false, false)
            }
        }
    }

    private lateinit var resolvingDataSourceFactory: ResolvingDataSource.Factory
    private lateinit var audioAttributes: AudioAttributes
    private lateinit var settingsRepository: UserSettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isCrossfading: Boolean = false
    private var crossfadeJob: Job? = null
    private var transitionMonitorJob: Job? = null
    private var lastSeekTimestampMs: Long = 0L

    private val incomingPlayerCandidate: ExoPlayer
        get() = if (::activePlayer.isInitialized && activePlayer === playerA) playerB else playerA

    private fun getCrossfadeSeconds(): Int {
        jamOverrideCrossfadeSeconds?.let { return it.coerceIn(0, 12) }
        val prefs = applicationContext.getSharedPreferences("nerf_user_settings_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("key_crossfade_seconds", 0)
    }

    private fun isAutomixEnabled(): Boolean {
        jamOverrideAutomixEnabled?.let { return it }
        val prefs = applicationContext.getSharedPreferences("nerf_user_settings_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("key_automix_enabled", true)
    }

    private fun isGaplessEnabled(): Boolean {
        jamOverrideGaplessEnabled?.let { return it }
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

    fun smoothTransitionTo(targetIndex: Int, customFadeMs: Long? = null) {
        if (!::activePlayer.isInitialized) return
        if (targetIndex !in 0 until activePlayer.mediaItemCount) return
        val crossfadeSec = getCrossfadeSeconds()
        val automix = isAutomixEnabled()
        val fadeMs = customFadeMs ?: when {
            crossfadeSec > 0 -> (crossfadeSec * 1000L).coerceAtMost(2000L)
            automix -> 800L
            else -> 400L
        }
        executeCrossfade(targetIndex, fadeMs, isSkip = true)
    }

    private fun checkAndTriggerTransition() {
        if (isCrossfading) return
        if (!::activePlayer.isInitialized || !activePlayer.isPlaying) return

        // Janela de estabilização pós-seek: impede que o arraste do slider dispare crossfades múltiplos ou prematuros
        if (System.currentTimeMillis() - lastSeekTimestampMs < 500L) return

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

        val remainingMs = (duration - position).coerceAtLeast(0L)
        // Se restar menos de 1500ms, não força um crossfade artificial; deixa transicionar naturalmente (gapless)
        if (remainingMs < 1500L) return

        val effectiveFadeMs = targetFadeMs.coerceAtMost(duration / 3).coerceAtMost(remainingMs)
        val triggerThresholdMs = effectiveFadeMs + 600L

        if (remainingMs in 100..triggerThresholdMs) {
            executeCrossfade(nextIndex, effectiveFadeMs, isSkip = false)
        }
    }

    @OptIn(UnstableApi::class)
    private fun executeCrossfade(nextIndex: Int, fadeDurationMs: Long, isSkip: Boolean = false) {
        if (!::activePlayer.isInitialized) return
        if (nextIndex !in 0 until activePlayer.mediaItemCount) return

        val outgoingPlayer = activePlayer
        val incomingPlayer = if (activePlayer === playerA) playerB else playerA

        if (isCrossfading) {
            // Se já houver um crossfade ocorrendo e um novo comando de skip for recebido, finaliza o anterior com segurança
            crossfadeJob?.cancel()
            fadingPlayer = null
            incomingPlayer.stop()
            incomingPlayer.volume = 1f
            incomingPlayer.pauseAtEndOfMediaItems = false
            outgoingPlayer.volume = 1f
            outgoingPlayer.pauseAtEndOfMediaItems = false
            isCrossfading = false
        }
        isCrossfading = true
        Log.i(TAG, "Iniciando Crossfade dinâmico em tempo real: ${fadeDurationMs}ms para o índice $nextIndex (isSkip=$isSkip)")

        try {
            if (isSkip) {
                // SKIP MANUAL: a experiência do usuário requer resposta IMEDIATA.
                // 1. Atualiza activePlayer e mediaSession instantaneamente para o incomingPlayer,
                //    fazendo com que sistema operacional, notificações, MediaController e UI
                //    atualizem a faixa no instante zero, eliminando o atraso de 2.5s.
                // 2. Faz fade-out rápido (~150ms) no outgoingPlayer sem travar a interface.
                val mediaItems = mutableListOf<MediaItem>()
                for (i in 0 until outgoingPlayer.mediaItemCount) {
                    mediaItems.add(outgoingPlayer.getMediaItemAt(i))
                }

                outgoingPlayer.pauseAtEndOfMediaItems = true
                fadingPlayer = outgoingPlayer
                activePlayer = incomingPlayer
                mediaSession?.setPlayer(activePlayer)

                incomingPlayer.setAudioAttributes(audioAttributes, true)
                incomingPlayer.volume = 1f
                incomingPlayer.setMediaItems(mediaItems, nextIndex, 0L)
                incomingPlayer.prepare()
                incomingPlayer.play()

                crossfadeJob?.cancel()
                crossfadeJob = serviceScope.launch {
                    val fadeOutDuration = 150L
                    val startTime = System.currentTimeMillis()
                    val startVol = outgoingPlayer.volume
                    while (isActive) {
                        val elapsed = System.currentTimeMillis() - startTime
                        val progress = (elapsed.toFloat() / fadeOutDuration).coerceIn(0f, 1f)
                        outgoingPlayer.volume = ((1f - progress) * startVol).coerceIn(0f, 1f)
                        if (progress >= 1f) break
                        delay(20)
                    }
                    outgoingPlayer.stop()
                    outgoingPlayer.pauseAtEndOfMediaItems = false
                    outgoingPlayer.volume = 1f

                    fadingPlayer = null
                    isCrossfading = false
                    triggerPrefetchNextTrack()
                    Log.i(TAG, "Skip manual concluído com resposta instantânea para índice $nextIndex.")
                }
            } else {
                // Transição natural (fim de faixa / crossfade / automix)
                // Mantém o incomingPlayer sem AudioFocus durante o crossfade para não interromper outgoingPlayer
                incomingPlayer.setAudioAttributes(audioAttributes, false)
                outgoingPlayer.repeatMode = Player.REPEAT_MODE_OFF
                outgoingPlayer.pauseAtEndOfMediaItems = true

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
                    // Pre-aquece o incomingPlayer até STATE_READY antes de iniciar a rampa de volume
                    var waitTime = 0
                    while (incomingPlayer.playbackState != Player.STATE_READY && waitTime < 2500) {
                        delay(30)
                        waitTime += 30
                    }

                    if (incomingPlayer.playbackState != Player.STATE_READY) {
                        Log.w(TAG, "incomingPlayer não atingiu STATE_READY a tempo (${incomingPlayer.playbackState}). Abortando crossfade.")
                        incomingPlayer.stop()
                        incomingPlayer.volume = 1f
                        incomingPlayer.pauseAtEndOfMediaItems = false
                        outgoingPlayer.volume = 1f
                        outgoingPlayer.pauseAtEndOfMediaItems = false
                        isCrossfading = false
                        return@launch
                    }

                    val remainingMs = (outgoingPlayer.duration - outgoingPlayer.currentPosition).coerceAtLeast(100L)
                    val effectiveFadeMs = fadeDurationMs.coerceAtMost(remainingMs).coerceAtLeast(200L)

                    // Atualiza a MediaSession para o player recebedor
                    fadingPlayer = outgoingPlayer
                    activePlayer = incomingPlayer
                    mediaSession?.setPlayer(activePlayer)

                    val startTime = System.currentTimeMillis()
                    while (isActive) {
                        val elapsed = System.currentTimeMillis() - startTime
                        val progress = (elapsed.toFloat() / effectiveFadeMs).coerceIn(0f, 1f)

                        // Curva equal-power: cos(t * PI/2) e sin(t * PI/2)
                        val volOut = kotlin.math.cos(progress * (Math.PI / 2)).toFloat()
                        val volIn = kotlin.math.sin(progress * (Math.PI / 2)).toFloat()

                        outgoingPlayer.volume = volOut.coerceIn(0f, 1f)
                        incomingPlayer.volume = volIn.coerceIn(0f, 1f)

                        if (progress >= 1f) break
                        delay(20)
                    }

                    // Transfere o foco de áudio de forma segura: outgoing é parado e incoming assume o foco exclusivo
                    outgoingPlayer.stop()
                    outgoingPlayer.pauseAtEndOfMediaItems = false
                    outgoingPlayer.volume = 1f
                    incomingPlayer.volume = 1f
                    incomingPlayer.setAudioAttributes(audioAttributes, true)

                    fadingPlayer = null
                    isCrossfading = false

                    triggerPrefetchNextTrack()
                    Log.i(TAG, "Crossfade de ${effectiveFadeMs}ms concluído com sucesso. Transição suave finalizada.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao iniciar crossfade: ${e.message}", e)
            isCrossfading = false
            outgoingPlayer.volume = 1f
            outgoingPlayer.pauseAtEndOfMediaItems = false
            incomingPlayer.stop()
            incomingPlayer.volume = 1f
            incomingPlayer.pauseAtEndOfMediaItems = false
            activePlayer = outgoingPlayer
            mediaSession?.setPlayer(activePlayer)
        }
    }

    fun isCurrentPlayerPlaying(): Boolean {
        return ::activePlayer.isInitialized && activePlayer.isPlaying
    }

    fun getCurrentPlayingTrackInfo(): Companion.PlayingTrackInfo? {
        if (!::activePlayer.isInitialized) return null
        val mediaItem = activePlayer.currentMediaItem ?: return null
        val meta = mediaItem.mediaMetadata
        val trackId = mediaItem.mediaId.takeIf { it.isNotBlank() }
            ?: mediaItem.localConfiguration?.uri?.lastPathSegment.orEmpty()
        val title = meta.title?.toString() ?: "NERF Music"
        val artist = meta.artist?.toString() ?: ""
        val artwork = meta.artworkUri?.toString()
        val pos = activePlayer.currentPosition.coerceAtLeast(0L)
        val dur = activePlayer.duration.coerceAtLeast(0L)
        return Companion.PlayingTrackInfo(
            trackId = trackId,
            title = title,
            artist = artist,
            artworkUrl = artwork,
            positionMs = pos,
            durationMs = dur
        )
    }

    fun togglePlayPause() {
        if (!::activePlayer.isInitialized) return
        if (activePlayer.isPlaying) {
            activePlayer.pause()
        } else {
            activePlayer.play()
        }
    }

    fun skipToNextTrack() {
        if (!::activePlayer.isInitialized) return
        if (activePlayer.hasNextMediaItem()) {
            activePlayer.seekToNextMediaItem()
        }
    }

    fun skipToPreviousTrack() {
        if (!::activePlayer.isInitialized) return
        if (activePlayer.currentPosition > 3000L || !activePlayer.hasPreviousMediaItem()) {
            activePlayer.seekTo(0L)
        } else {
            activePlayer.seekToPreviousMediaItem()
        }
    }

    companion object {
        private const val TAG = "AudioPlayerService"
        const val NERF_URI_SCHEME = "nerfmusic"
        private const val CACHE_TTL_MS = 25 * 60 * 1000L // 25 minutos de validade em cache
        private const val STREAM_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

        private var instance: AudioPlayerService? = null
        fun getInstance(): AudioPlayerService? = instance

        data class PlayingTrackInfo(
            val trackId: String,
            val title: String,
            val artist: String,
            val artworkUrl: String?,
            val positionMs: Long,
            val durationMs: Long
        )

        @Volatile
        var isKaraokeEnabled: Boolean = false
            private set

        @Volatile
        var karaokeVocalLevel: Float = 0.0f
            private set

        @Volatile
        var isUsingRealInstrumental: Boolean = false

        @Volatile
        var jamOverrideCrossfadeSeconds: Int? = null
            private set

        @Volatile
        var jamOverrideAutomixEnabled: Boolean? = null
            private set

        @Volatile
        var jamOverrideGaplessEnabled: Boolean? = null
            private set

        fun setJamAudioSettingsOverride(
            crossfadeSeconds: Int?,
            automixEnabled: Boolean?,
            gaplessEnabled: Boolean?
        ) {
            jamOverrideCrossfadeSeconds = crossfadeSeconds?.coerceIn(0, 12)
            jamOverrideAutomixEnabled = automixEnabled
            jamOverrideGaplessEnabled = gaplessEnabled
        }

        var onInstrumentalStatusChanged: ((enabled: Boolean, isRealInstrumental: Boolean) -> Unit)? = null

        fun setKaraokeMode(enabled: Boolean, vocalLevel: Float = karaokeVocalLevel) {
            isKaraokeEnabled = enabled
            karaokeVocalLevel = vocalLevel.coerceIn(0f, 1f)
            instance?.applyKaraokeState(isKaraokeEnabled, karaokeVocalLevel)
        }

        fun setLoudnessStandard(standard: LoudnessStandard) {
            instance?.settingsRepository?.setLoudnessStandard(standard)
            instance?.loudnessProcessors?.forEach { it.standard = standard }
        }

        fun prepareStandbyInstrumental(
            originalTrackId: String,
            instrumentalVideoId: String,
            initialOffsetMs: Long = 0L
        ) {
            instance?.prepareInstrumentalForTrack(originalTrackId, instrumentalVideoId, initialOffsetMs)
                ?: prefetchTrack(instrumentalVideoId)
        }

        fun clearStandbyInstrumental() {
            instance?.clearInstrumentalStandby()
        }

        fun buildTrackUri(videoId: String): Uri = Uri.parse("$NERF_URI_SCHEME://track/$videoId")

        private data class CachedStreamUrl(val url: String, val timestampMs: Long)
        private val streamUrlCache = ConcurrentHashMap<String, CachedStreamUrl>()
        private val inFlightResolutions = ConcurrentHashMap<String, Deferred<String>>()
        private var serviceAppContext: Context? = null
        private var streamResolverInstance: StreamResolver? = null

        private fun getStreamResolver(): StreamResolver {
            return streamResolverInstance ?: synchronized(this) {
                streamResolverInstance ?: createDefaultStreamResolver(serviceAppContext).also {
                    streamResolverInstance = it
                }
            }
        }

        private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun setPrefetchContext(context: Context) {
            val appCtx = context.applicationContext
            serviceAppContext = appCtx
            if (streamResolverInstance == null) {
                streamResolverInstance = createDefaultStreamResolver(appCtx)
            }
        }

        fun getTargetAudioQuality(context: Context): AudioQuality {
            val prefs = context.getSharedPreferences("nerf_user_settings_prefs", Context.MODE_PRIVATE)
            val dataSaverStr = prefs.getString("key_data_saver_mode", "ALWAYS_OFF") ?: "ALWAYS_OFF"
            val isWifi = NetworkUtils.isConnectedToWifi(context)

            if (dataSaverStr == "ALWAYS_ON" || (dataSaverStr == "AUTOMATIC" && !isWifi)) {
                return AudioQuality.LOW
            }

            val qualityKey = if (isWifi) "key_wifi_audio_quality" else "key_cellular_audio_quality"
            val defaultQuality = if (isWifi) AudioQuality.HIGH.name else AudioQuality.AUTOMATIC.name
            val qualityStr = prefs.getString(qualityKey, defaultQuality) ?: defaultQuality

            val quality = try {
                AudioQuality.valueOf(qualityStr)
            } catch (_: Exception) {
                if (isWifi) AudioQuality.HIGH else AudioQuality.AUTOMATIC
            }

            return if (quality == AudioQuality.AUTOMATIC) {
                if (isWifi) AudioQuality.HIGH else AudioQuality.NORMAL
            } else {
                quality
            }
        }

        // The resolved URL depends on the active network and quality preferences. Keep separate
        // entries so changing those settings cannot reuse a stream selected for the old profile.
        private fun streamCacheKey(videoId: String): String {
            val context = serviceAppContext
            val targetQuality = context?.let { getTargetAudioQuality(it) } ?: AudioQuality.HIGH
            return "$videoId|${targetQuality.name}"
        }

        fun isStreamCached(videoId: String): Boolean {
            val cached = streamUrlCache[streamCacheKey(videoId)] ?: return false
            return (System.currentTimeMillis() - cached.timestampMs) < CACHE_TTL_MS
        }

        /**
         * Pre-busca a URL do fluxo de áudio de forma assíncrona sem interromper operações em andamento.
         */
        fun prefetchTrack(videoId: String, offlineCheckDir: File? = null) {
            if (videoId.isBlank()) return
            val checkDir = offlineCheckDir ?: serviceAppContext?.filesDir
            if (checkDir != null) {
                val possibleExtensions = listOf("webm", "m4a", "opus", "mp4", "ogg", "mp3", "aac")
                val existsOffline = possibleExtensions.any { ext ->
                    val f = File(checkDir, "offline_audio/$videoId.$ext")
                    f.exists() && f.length() > 1024L
                }
                if (existsOffline) {
                    Log.i(TAG, "Faixa $videoId já existe offline no disco. Ignorando pre-fetch de rede.")
                    return
                }
            }

            val context = serviceAppContext
            if (context != null && !NetworkUtils.isNetworkAvailable(context)) {
                Log.i(TAG, "Dispositivo offline. Ignorando pre-fetch de rede para $videoId.")
                return
            }

            val targetQuality = context?.let { getTargetAudioQuality(it) } ?: AudioQuality.HIGH
            val cacheKey = streamCacheKey(videoId)
            val now = System.currentTimeMillis()
            val cached = streamUrlCache[cacheKey]
            if (cached != null && (now - cached.timestampMs) < CACHE_TTL_MS) return

            val existing = inFlightResolutions[cacheKey]
            if (existing != null && existing.isActive) return

            val deferred = prefetchScope.async {
                try {
                    Log.i(TAG, "Pre-fetching stream URL em background para: $videoId (qualidade=$targetQuality)")
                    val result = getStreamResolver().resolveStreamUrl(videoId, targetQuality)
                    val url = result.getOrNull()
                    if (!url.isNullOrBlank()) {
                        streamUrlCache[cacheKey] = CachedStreamUrl(url, System.currentTimeMillis())
                        Log.i(TAG, "Pre-fetch de áudio concluído com sucesso para: $videoId (qualidade=$targetQuality)")
                        url
                    } else {
                        throw IOException("URL vazia retornada para $videoId")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Falha no pre-fetch de stream para $videoId: ${e.message}")
                    throw e
                } finally {
                    inFlightResolutions.remove(cacheKey)
                }
            }
            inFlightResolutions[cacheKey] = deferred
        }

        /**
         * Resolve a URL de streaming ou aguarda uma pre-busca já em andamento, garantindo 0 requisições duplicadas.
         */
        fun resolveOrAwaitStreamUrl(videoId: String): String {
            val context = serviceAppContext
            val targetQuality = context?.let { getTargetAudioQuality(it) } ?: AudioQuality.HIGH
            val cacheKey = streamCacheKey(videoId)
            val now = System.currentTimeMillis()
            val cached = streamUrlCache[cacheKey]
            if (cached != null && (now - cached.timestampMs) < CACHE_TTL_MS) {
                Log.i(TAG, "Stream URL obtida diretamente do cache instantâneo para: $videoId (qualidade=$targetQuality)")
                return cached.url
            }

            val existing = inFlightResolutions[cacheKey]
            if (existing != null && existing.isActive) {
                Log.i(TAG, "Aguardando pre-fetch em voo para: $videoId")
                return runBlocking {
                    try {
                        existing.await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Pre-fetch em voo falhou, re-tentando JIT: ${e.message}")
                        val retryUrl = getStreamResolver().resolveStreamUrl(videoId, targetQuality).getOrThrow()
                        streamUrlCache[cacheKey] = CachedStreamUrl(retryUrl, System.currentTimeMillis())
                        retryUrl
                    }
                }
            }

            val deferred = prefetchScope.async {
                Log.i(TAG, "Resolvendo URL de stream JIT para: $videoId (qualidade=$targetQuality)")
                val result = getStreamResolver().resolveStreamUrl(videoId, targetQuality).getOrThrow()
                streamUrlCache[cacheKey] = CachedStreamUrl(result, System.currentTimeMillis())
                result
            }
            inFlightResolutions[cacheKey] = deferred
            return runBlocking {
                try {
                    deferred.await()
                } finally {
                    inFlightResolutions.remove(cacheKey)
                }
            }
        }

        fun clearStreamCache() {
            streamUrlCache.clear()
            inFlightResolutions.values.forEach { it.cancel() }
            inFlightResolutions.clear()
            Log.i(TAG, "Cache de streams limpo com sucesso.")
        }

        fun evictCache(videoId: String) {
            val prefix = "$videoId|"
            streamUrlCache.keys.removeAll { it.startsWith(prefix) }
            inFlightResolutions.entries
                .filter { it.key.startsWith(prefix) }
                .forEach { (key, deferred) ->
                    deferred.cancel()
                    inFlightResolutions.remove(key, deferred)
                }
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
        val vocalProcessor = VocalIsolationAudioProcessor().apply {
            isKaraokeEnabled = Companion.isKaraokeEnabled
            vocalLevel = Companion.karaokeVocalLevel
        }
        vocalProcessors.add(vocalProcessor)

        val loudnessProcessor = LoudnessNormalizationAudioProcessor().apply {
            standard = if (::settingsRepository.isInitialized) {
                settingsRepository.loudnessStandard.value
            } else {
                LoudnessStandard.SPOTIFY
            }
        }
        loudnessProcessors.add(loudnessProcessor)

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(vocalProcessor, loudnessProcessor))
                    .build()
            }
        }

        val playerInstance = ExoPlayer.Builder(this, renderersFactory)
            .setLoadControl(buildLoadControl())
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this).setDataSourceFactory(resolvingDataSourceFactory)
            )
            .setAudioAttributes(audioAttributes, handleAudioFocus) // Controla Audio Focus
            .setHandleAudioBecomingNoisy(handleAudioFocus)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        playerProcessorMap[playerInstance] = vocalProcessor

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
                com.nerf.music.widget.CompactPlayerWidgetProvider.updateAllWidgets(applicationContext)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (playerInstance != activePlayer) return
                Log.i(TAG, "ExoPlayer onIsPlayingChanged: isPlaying=$isPlaying, duration=${playerInstance.duration}ms")
                com.nerf.music.widget.CompactPlayerWidgetProvider.updateAllWidgets(applicationContext)
                if (isPlaying) {
                    startTransitionMonitor()
                    triggerPrefetchNextTrack()
                    val currentMediaId = activePlayer.currentMediaItem?.mediaId
                    if (::instrumentalPlayer.isInitialized &&
                        isInstrumentalStreamReady &&
                        currentMediaId != null &&
                        currentMediaId == currentInstrumentalOriginalTrackId
                    ) {
                        val targetPos = (activePlayer.currentPosition + instrumentalOffsetMs).coerceAtLeast(0L)
                        if (kotlin.math.abs(instrumentalPlayer.currentPosition - targetPos) > 70L) {
                            instrumentalPlayer.seekTo((targetPos + 35L).coerceAtLeast(0L))
                        }
                        if (!instrumentalPlayer.isPlaying) {
                            instrumentalPlayer.play()
                        }
                        startInstrumentalLockStepMonitor()
                    }
                } else if (!isCrossfading) {
                    transitionMonitorJob?.cancel()
                    if (::instrumentalPlayer.isInitialized && instrumentalPlayer.isPlaying) {
                        instrumentalPlayer.pause()
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (playerInstance != activePlayer) return
                Log.i(TAG, "ExoPlayer onMediaItemTransition: ${mediaItem?.mediaId}, reason=$reason")
                com.nerf.music.widget.CompactPlayerWidgetProvider.updateAllWidgets(applicationContext)
                if (mediaItem?.mediaId != currentInstrumentalOriginalTrackId) {
                    isUsingRealInstrumental = false
                    hasAutoAlignedCurrentInstrumental = false
                    instrumentalOffsetMs = 0L
                    instrumentalSyncMonitorJob?.cancel()
                    beatAlignmentJob?.cancel()
                    if (::instrumentalPlayer.isInitialized) {
                        instrumentalPlayer.pause()
                        instrumentalPlayer.volume = 0f
                        instrumentalPlayer.playbackParameters = PlaybackParameters.DEFAULT
                    }
                    if (!isCrossfading) {
                        activePlayer.volume = 1f
                    }
                    if (isKaraokeEnabled) {
                        vocalProcessors.forEach {
                            it.isKaraokeEnabled = true
                            it.vocalLevel = karaokeVocalLevel
                        }
                        onInstrumentalStatusChanged?.invoke(true, false)
                    }
                }
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
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                    lastSeekTimestampMs = System.currentTimeMillis()
                    val currentMediaId = activePlayer.currentMediaItem?.mediaId
                    if (::instrumentalPlayer.isInitialized &&
                        isInstrumentalStreamReady &&
                        currentMediaId != null &&
                        currentMediaId == currentInstrumentalOriginalTrackId
                    ) {
                        val compensatedPos = (newPosition.positionMs + instrumentalOffsetMs + 35L).coerceAtLeast(0L)
                        instrumentalPlayer.seekTo(compensatedPos)
                    }
                    if (isCrossfading) {
                        // Se o usuário buscar na timeline durante um crossfade, encerra a transição sem colapso abrupto
                        crossfadeJob?.cancel()
                        val otherPlayer = incomingPlayerCandidate
                        val oldFading = fadingPlayer
                        fadingPlayer = null
                        isCrossfading = false

                        serviceScope.launch {
                            oldFading?.let { fp ->
                                val startVol = fp.volume
                                for (step in 1..4) {
                                    delay(12)
                                    fp.volume = (startVol * (1f - step / 4f)).coerceIn(0f, 1f)
                                }
                                fp.stop()
                                fp.volume = 1f
                                fp.pauseAtEndOfMediaItems = false
                            }
                            if (otherPlayer != oldFading) {
                                otherPlayer.stop()
                                otherPlayer.volume = 1f
                                otherPlayer.pauseAtEndOfMediaItems = false
                            }
                        }
                        serviceScope.launch {
                            val startVol = activePlayer.volume
                            for (step in 1..5) {
                                delay(16)
                                activePlayer.volume = (startVol + (1f - startVol) * (step / 5f)).coerceIn(0f, 1f)
                            }
                            activePlayer.volume = 1f
                            activePlayer.pauseAtEndOfMediaItems = false
                        }
                    }
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
        instance = this
        serviceAppContext = applicationContext

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

                    // 1. Procura em todas as localizações e extensões possíveis (.webm, .m4a, .opus, .mp4, .ogg, .mp3, .aac)
                    val possibleExtensions = listOf("webm", "m4a", "opus", "mp4", "ogg", "mp3", "aac")
                    var offlineFile: File? = possibleExtensions
                        .map { ext -> File(filesDir, "offline_audio/$videoId.$ext") }
                        .firstOrNull { it.exists() && it.length() > 1024L }

                    if (offlineFile == null && serviceAppContext != null) {
                        offlineFile = possibleExtensions
                            .map { ext -> File(serviceAppContext!!.filesDir, "offline_audio/$videoId.$ext") }
                            .firstOrNull { it.exists() && it.length() > 1024L }
                    }

                    if (offlineFile == null && serviceAppContext != null) {
                        offlineFile = possibleExtensions
                            .map { ext -> File(serviceAppContext!!.getExternalFilesDir(null), "offline_audio/$videoId.$ext") }
                            .firstOrNull { it.exists() && it.length() > 1024L }
                    }

                    if (offlineFile == null) {
                        val dt = LibraryRepositoryImpl.sharedDownloadedTracksFlow.value.firstOrNull { it.track.id == videoId }
                        if (dt != null) {
                            val f = File(dt.localFilePath)
                            if (f.exists() && f.length() > 1024L) {
                                offlineFile = f
                            }
                        }
                    }

                    if (offlineFile != null) {
                        Log.i(TAG, "Reproduzindo faixa offline local: ${offlineFile.absolutePath} (${offlineFile.length()} bytes)")
                        return@Resolver dataSpec.buildUpon().setUri(Uri.fromFile(offlineFile)).build()
                    }

                    // Se não for arquivo offline, valida conexão antes de tentar resolver
                    val context = serviceAppContext ?: applicationContext
                    if (!NetworkUtils.isNetworkAvailable(context)) {
                        Log.w(TAG, "Dispositivo offline e faixa $videoId não encontrada em offline_audio.")
                        throw IOException("Faixa não disponível offline no dispositivo.")
                    }

                    val resolvedUrl = resolveOrAwaitStreamUrl(videoId)
                    dataSpec.buildUpon().setUri(Uri.parse(resolvedUrl)).build()
                } else {
                    dataSpec
                }
            }
        )

        playerA = createExoPlayer(handleAudioFocus = true)
        playerB = createExoPlayer(handleAudioFocus = false)
        activePlayer = playerA
        fadingPlayer = null

        val instRenderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(instrumentalEnvelopeProcessor))
                    .build()
            }
        }
        instrumentalPlayer = ExoPlayer.Builder(this, instRenderersFactory)
            .setLoadControl(buildLoadControl())
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this).setDataSourceFactory(resolvingDataSourceFactory)
            )
            .setAudioAttributes(audioAttributes, false)
            .setHandleAudioBecomingNoisy(false)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .apply {
                volume = 0f
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            isInstrumentalStreamReady = true
                            Log.i(TAG, "Instrumental standby PRONTO para troca instantânea ($currentInstrumentalVideoId)")
                            val currentMediaId = activePlayer.currentMediaItem?.mediaId
                            if (currentMediaId != null && currentMediaId == currentInstrumentalOriginalTrackId) {
                                val targetPos = (activePlayer.currentPosition + instrumentalOffsetMs + 35L).coerceAtLeast(0L)
                                if (kotlin.math.abs(currentPosition - targetPos) > 70L) {
                                    seekTo(targetPos)
                                }
                                if (activePlayer.isPlaying && !isPlaying) {
                                    play()
                                }
                                startInstrumentalLockStepMonitor()
                                triggerAutoBeatAlignmentIfNeeded()
                                if (isKaraokeEnabled) {
                                    applyKaraokeState(true, karaokeVocalLevel)
                                }
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.w(TAG, "Erro ao preparar faixa instrumental standby: ${error.message}")
                        isInstrumentalStreamReady = false
                    }
                })
            }
        try {
            val openIntent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, activePlayer.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            sendBroadcast(openIntent)
        } catch (_: Exception) {}

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

        serviceScope.launch {
            settingsRepository.loudnessStandard.collect { std ->
                loudnessProcessors.forEach { it.standard = std }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "onTaskRemoved disparado: usuário encerrou o app na lista de recentes. Parando reprodução e serviço.")
        val player = mediaSession?.player
        player?.pause()
        player?.stop()
        player?.clearMediaItems()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (instance == this) {
            instance = null
        }
        serviceScope.cancel()
        transitionMonitorJob?.cancel()
        crossfadeJob?.cancel()
        fadingPlayer = null
        vocalProcessors.clear()
        loudnessProcessors.clear()
        if (::playerA.isInitialized) {
            playerA.release()
        }
        if (::playerB.isInitialized) {
            playerB.release()
        }
        if (::instrumentalPlayer.isInitialized) {
            instrumentalPlayer.release()
        }
        mediaSession?.run {
            release()
            mediaSession = null
        }
        try {
            val closeIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, activePlayer.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
            sendBroadcast(closeIntent)
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
