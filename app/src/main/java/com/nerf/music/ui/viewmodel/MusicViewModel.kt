package com.nerf.music.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.nerf.music.data.model.PlaybackState
import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.SearchResult
import com.nerf.music.data.model.Track
import com.nerf.music.data.repository.MusicRepository
import com.nerf.music.data.repository.MusicRepositoryImpl
import com.nerf.music.player.AudioPlayerService
import com.nerf.music.player.DemoStreamResolver
import com.nerf.music.player.NewPipeStreamResolver
import com.nerf.music.player.StreamResolver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicViewModel(
    private val repository: MusicRepository = MusicRepositoryImpl(),
    private val streamResolver: StreamResolver = NewPipeStreamResolver(),
    private val demoStreamResolver: StreamResolver = DemoStreamResolver()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SearchFilter.ALL)
    val selectedFilter: StateFlow<SearchFilter> = _selectedFilter.asStateFlow()

    private val _searchResult = MutableStateFlow(SearchResult())
    val searchResult: StateFlow<SearchResult> = _searchResult.asStateFlow()

    private val _selectedTrack = MutableStateFlow<Track?>(null)
    val selectedTrack: StateFlow<Track?> = _selectedTrack.asStateFlow()

    private val _loadingTrackId = MutableStateFlow<String?>(null)
    val loadingTrackId: StateFlow<String?> = _loadingTrackId.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _isFullPlayerVisible = MutableStateFlow(false)
    val isFullPlayerVisible: StateFlow<Boolean> = _isFullPlayerVisible.asStateFlow()

    private var playbackQueue: List<Track> = emptyList()
    private var currentQueueIndex: Int = -1

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var progressUpdateJob: Job? = null
    private var streamResolutionJob: Job? = null

    val authorizedDemoTrack = Track(
        id = "demo_audioplayer_test",
        title = "Áudio de Teste Autorizado (Sinfonia CC-BY)",
        artist = "AndroidX Media3 / Google ExoPlayer Test Media",
        album = "Creative Commons",
        thumbnailUrl = "https://storage.googleapis.com/exoplayer-test-media-0/sample_artwork.png",
        durationText = "00:03",
        durationSeconds = 3,
        sourceIdentifier = "MEDIA3_DEMO_SOURCE"
    )

    fun initializePlayerController(context: Context) {
        if (mediaController != null) return

        val sessionToken = SessionToken(
            context,
            ComponentName(context, AudioPlayerService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val controller = controllerFuture?.get() ?: return@addListener
            mediaController = controller
            setupPlayerListener(controller)
        }, MoreExecutors.directExecutor())
    }

    private fun resolveEffectiveDuration(player: Player): Long {
        val playerDuration = player.duration
        if (playerDuration > 0L) return playerDuration
        val fallbackSeconds = _playbackState.value.currentTrack?.durationSeconds ?: 0L
        return (fallbackSeconds * 1000L).coerceAtLeast(0L)
    }

    private fun setupPlayerListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = isPlaying,
                    isBuffering = if (isPlaying) false else _playbackState.value.isBuffering,
                    durationMs = resolveEffectiveDuration(player),
                    currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                )
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val isResolving = _loadingTrackId.value != null
                val isBuffering = isResolving || playbackState == Player.STATE_BUFFERING
                _playbackState.value = _playbackState.value.copy(
                    isBuffering = isBuffering,
                    durationMs = resolveEffectiveDuration(player),
                    currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                )

                // Avança automaticamente para a próxima música ao terminar a faixa atual
                if (playbackState == Player.STATE_ENDED && playbackQueue.size > 1) {
                    val nextIdx = currentQueueIndex + 1
                    if (nextIdx in playbackQueue.indices) {
                        playTrackFromQueue(nextIdx)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("MusicViewModel", "Erro no ExoPlayer: ${error.errorCodeName} - ${error.message}", error)
                val errorMsg = "Falha na reprodução do fluxo de áudio (${error.errorCodeName})."
                _playbackState.value = _playbackState.value.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = errorMsg
                )
                _userMessage.value = errorMsg
            }
        })
    }

    private fun startProgressTracker() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _playbackState.value = _playbackState.value.copy(
                            currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                            durationMs = resolveEffectiveDuration(controller)
                        )
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    fun openFullPlayer() {
        if (_playbackState.value.currentTrack != null) {
            _isFullPlayerVisible.value = true
        }
    }

    fun closeFullPlayer() {
        _isFullPlayerVisible.value = false
    }

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelected(filter: SearchFilter) {
        if (_selectedFilter.value != filter) {
            _selectedFilter.value = filter
            if (_searchQuery.value.isNotBlank()) {
                performSearch()
            }
        }
    }

    fun performSearch() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) {
            _searchResult.value = SearchResult()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.search(query, _selectedFilter.value)
            result.onSuccess { res ->
                _searchResult.value = res
                _isLoading.value = false
                if (res.isEmpty) {
                    _errorMessage.value = "Nenhum resultado encontrado para \"$query\""
                }
            }.onFailure { error ->
                _isLoading.value = false
                _errorMessage.value = "Erro na busca: ${error.localizedMessage ?: "Falha de rede"}"
            }
        }
    }

    fun selectTrack(track: Track) {
        _selectedTrack.value = track
        playTrack(track)
    }

    fun playTrack(track: Track) {
        _selectedTrack.value = track

        // Se a faixa já for a atual e estiver tocando, apenas pausa
        if (_playbackState.value.currentTrack?.id == track.id && _playbackState.value.isPlaying && _loadingTrackId.value == null) {
            togglePlayPause()
            return
        }

        // Se a faixa já for a atual e estiver pausada (e já carregada), retoma reprodução
        if (
            _playbackState.value.currentTrack?.id == track.id &&
            !_playbackState.value.isPlaying &&
            _loadingTrackId.value == null &&
            mediaController != null &&
            mediaController?.mediaItemCount ?: 0 > 0
        ) {
            mediaController?.play()
            return
        }

        // Atualiza a fila de reprodução com as músicas da busca atual se a faixa pertencer a ela
        val currentSongs = _searchResult.value.songs
        val indexInSearch = currentSongs.indexOfFirst { it.id == track.id }
        if (indexInSearch >= 0) {
            playbackQueue = currentSongs
            currentQueueIndex = indexInSearch
        } else {
            val indexInQueue = playbackQueue.indexOfFirst { it.id == track.id }
            if (indexInQueue >= 0) {
                currentQueueIndex = indexInQueue
            } else {
                playbackQueue = listOf(track)
                currentQueueIndex = 0
            }
        }

        loadAndPlayTrack(track, useDemoResolver = false)
    }

    private fun playTrackFromQueue(index: Int) {
        if (index !in playbackQueue.indices) return
        currentQueueIndex = index
        val track = playbackQueue[index]
        _selectedTrack.value = track
        loadAndPlayTrack(
            track = track,
            useDemoResolver = track.id == authorizedDemoTrack.id
        )
    }

    private fun loadAndPlayTrack(track: Track, useDemoResolver: Boolean) {
        streamResolutionJob?.cancel()
        streamResolutionJob = viewModelScope.launch {
            _loadingTrackId.value = track.id
            _playbackState.value = _playbackState.value.copy(
                currentTrack = track,
                isPlaying = false,
                currentPositionMs = 0L,
                durationMs = (track.durationSeconds * 1000L).coerceAtLeast(0L),
                isBuffering = true,
                hasNext = playbackQueue.size > 1,
                hasPrevious = playbackQueue.size > 1,
                errorMessage = null
            )

            val resolver = if (useDemoResolver) demoStreamResolver else streamResolver
            val streamResult = resolver.resolveStreamUrl(track.id)
            _loadingTrackId.value = null

            streamResult.onSuccess { streamUrl ->
                val artworkUrl = track.highResThumbnailUrl ?: track.thumbnailUrl
                val metadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(artworkUrl?.let { Uri.parse(it) })
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(streamUrl)
                    .setMediaMetadata(metadata)
                    .build()

                mediaController?.run {
                    setMediaItem(mediaItem)
                    prepare()
                    play()
                }
            }.onFailure { error ->
                val errorMsg = if (useDemoResolver) {
                    "Falha ao carregar o áudio de teste: ${error.localizedMessage}"
                } else {
                    "Falha ao carregar a faixa. O servidor recusou a conexão."
                }
                _playbackState.value = _playbackState.value.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = errorMsg
                )
                _userMessage.value = errorMsg
            }
        }
    }

    fun playAuthorizedTestAudio() {
        _selectedTrack.value = authorizedDemoTrack
        playbackQueue = listOf(authorizedDemoTrack)
        currentQueueIndex = 0
        loadAndPlayTrack(authorizedDemoTrack, useDemoResolver = true)
    }

    fun togglePlayPause() {
        val current = _playbackState.value.currentTrack ?: return
        if (_loadingTrackId.value != null) return

        val controller = mediaController
        if (controller != null && controller.mediaItemCount > 0) {
            if (controller.isPlaying) {
                controller.pause()
            } else {
                if (controller.playbackState == Player.STATE_ENDED) {
                    controller.seekTo(0L)
                }
                controller.play()
            }
        } else {
            // Caso o player tenha sido reiniciado ou perdido o item, recarrega a faixa atual
            loadAndPlayTrack(current, useDemoResolver = current.id == authorizedDemoTrack.id)
        }
    }

    fun skipNext() {
        if (playbackQueue.size > 1) {
            val nextIndex = (currentQueueIndex + 1) % playbackQueue.size
            playTrackFromQueue(nextIndex)
        } else {
            val controller = mediaController
            if (controller != null && controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
            } else {
                seekTo(0L)
                controller?.play()
            }
        }
    }

    fun skipPrevious() {
        val controller = mediaController
        // Se já passou de 3 segundos de música, volta para o início da faixa atual
        if (controller != null && controller.currentPosition > 3_000L) {
            seekTo(0L)
            return
        }

        if (playbackQueue.size > 1) {
            val prevIndex = if (currentQueueIndex - 1 >= 0) {
                currentQueueIndex - 1
            } else {
                playbackQueue.lastIndex
            }
            playTrackFromQueue(prevIndex)
        } else {
            seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val targetPos = positionMs.coerceAtLeast(0L)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = targetPos)
        mediaController?.seekTo(targetPos)
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        stopProgressTracker()
        streamResolutionJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
