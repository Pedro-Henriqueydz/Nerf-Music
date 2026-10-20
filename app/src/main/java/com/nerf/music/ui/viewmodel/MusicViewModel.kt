package com.nerf.music.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var progressUpdateJob: Job? = null

    val authorizedDemoTrack = Track(
        id = "demo_audioplayer_test",
        title = "Áudio de Teste Autorizado (Sinfonia CC-BY)",
        artist = "AndroidX Media3 / Google ExoPlayer Test Media",
        album = "Creative Commons",
        thumbnailUrl = "https://storage.googleapis.com/exoplayer-test-media-0/sample_artwork.png",
        durationText = "00:03",
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

    private fun setupPlayerListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = isPlaying,
                    durationMs = player.duration.coerceAtLeast(0L),
                    currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                )
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                _playbackState.value = _playbackState.value.copy(
                    isBuffering = isBuffering,
                    durationMs = player.duration.coerceAtLeast(0L)
                )
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
                            durationMs = controller.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
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
        if (_playbackState.value.currentTrack?.id == track.id && _playbackState.value.isPlaying) {
            togglePlayPause()
            return
        }

        // Se a faixa já for a atual e estiver pausada, retoma reprodução
        if (_playbackState.value.currentTrack?.id == track.id && !_playbackState.value.isPlaying && mediaController != null) {
            mediaController?.play()
            return
        }

        viewModelScope.launch {
            _loadingTrackId.value = track.id
            _playbackState.value = _playbackState.value.copy(
                currentTrack = track,
                isBuffering = true,
                errorMessage = null
            )

            val streamResult = streamResolver.resolveStreamUrl(track.id)
            _loadingTrackId.value = null

            streamResult.onSuccess { streamUrl ->
                val metadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(track.thumbnailUrl?.let { Uri.parse(it) })
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
            }.onFailure { _ ->
                val errorMsg = "Falha ao carregar a faixa. O servidor recusou a conexão."
                _playbackState.value = _playbackState.value.copy(
                    isBuffering = false,
                    errorMessage = errorMsg
                )
                _userMessage.value = errorMsg
            }
        }
    }

    fun playAuthorizedTestAudio() {
        _selectedTrack.value = authorizedDemoTrack
        viewModelScope.launch {
            _loadingTrackId.value = authorizedDemoTrack.id
            _playbackState.value = _playbackState.value.copy(
                currentTrack = authorizedDemoTrack,
                isBuffering = true,
                errorMessage = null
            )

            val streamResult = demoStreamResolver.resolveStreamUrl(authorizedDemoTrack.id)
            _loadingTrackId.value = null

            streamResult.onSuccess { streamUrl ->
                val metadata = MediaMetadata.Builder()
                    .setTitle(authorizedDemoTrack.title)
                    .setArtist(authorizedDemoTrack.artist)
                    .setAlbumTitle(authorizedDemoTrack.album)
                    .setArtworkUri(authorizedDemoTrack.thumbnailUrl?.let { Uri.parse(it) })
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setMediaId(authorizedDemoTrack.id)
                    .setUri(streamUrl)
                    .setMediaMetadata(metadata)
                    .build()

                mediaController?.run {
                    setMediaItem(mediaItem)
                    prepare()
                    play()
                }
            }.onFailure { error ->
                val errorMsg = "Falha ao carregar o áudio de teste: ${error.localizedMessage}"
                _playbackState.value = _playbackState.value.copy(
                    isBuffering = false,
                    errorMessage = errorMsg
                )
                _userMessage.value = errorMsg
            }
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        stopProgressTracker()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
