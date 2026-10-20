package com.nerf.music.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicViewModel(
    private val repository: MusicRepository = MusicRepositoryImpl()
) : ViewModel() {

    companion object {
        private const val EXTRA_DURATION_TEXT = "extra_duration_text"
        private const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        private const val EXTRA_THUMBNAIL_URL = "extra_thumbnail_url"
    }

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

    // Cache de metadados das faixas conhecidas para reconstrução rápida a partir do Timeline
    private val trackMetadataMap = mutableMapOf<String, Track>()

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var progressUpdateJob: Job? = null

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
            syncTimelineState(controller)
        }, MoreExecutors.directExecutor())
    }

    private fun resolveEffectiveDuration(player: Player): Long {
        val playerDuration = player.duration
        if (playerDuration > 0L) return playerDuration
        val fallbackSeconds = _playbackState.value.currentTrack?.durationSeconds ?: 0L
        return (fallbackSeconds * 1000L).coerceAtLeast(0L)
    }

    private fun trackToMediaItem(track: Track): MediaItem {
        trackMetadataMap[track.id] = track
        val artworkUrl = track.highResThumbnailUrl ?: track.thumbnailUrl
        val extras = Bundle().apply {
            putString(EXTRA_DURATION_TEXT, track.durationText)
            putLong(EXTRA_DURATION_SECONDS, track.durationSeconds)
            putString(EXTRA_THUMBNAIL_URL, track.thumbnailUrl)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(artworkUrl?.let { Uri.parse(it) })
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(AudioPlayerService.buildTrackUri(track.id))
            .setMediaMetadata(metadata)
            .build()
    }

    private fun mediaItemToTrack(mediaItem: MediaItem): Track {
        val id = mediaItem.mediaId
        trackMetadataMap[id]?.let { return it }

        val meta = mediaItem.mediaMetadata
        val extras = meta.extras
        return Track(
            id = id,
            title = meta.title?.toString() ?: "Faixa desconhecida",
            artist = meta.artist?.toString() ?: "Artista desconhecido",
            album = meta.albumTitle?.toString(),
            thumbnailUrl = extras?.getString(EXTRA_THUMBNAIL_URL) ?: meta.artworkUri?.toString(),
            durationText = extras?.getString(EXTRA_DURATION_TEXT) ?: "--:--",
            durationSeconds = extras?.getLong(EXTRA_DURATION_SECONDS, 0L) ?: 0L
        ).also { trackMetadataMap[id] = it }
    }

    /**
     * Sincroniza a fila da UI diretamente com o `currentTimeline` real do MediaController.
     */
    private fun syncTimelineState(player: Player) {
        val timeline = player.currentTimeline
        val itemCount = player.mediaItemCount
        if (timeline.isEmpty || itemCount == 0) {
            _playbackState.value = _playbackState.value.copy(
                currentTrack = null,
                queue = emptyList(),
                currentQueueIndex = -1,
                isPlaying = false,
                isBuffering = false,
                hasNext = false,
                hasPrevious = false
            )
            _isFullPlayerVisible.value = false
            return
        }

        val queueTracks = (0 until itemCount).map { idx ->
            mediaItemToTrack(player.getMediaItemAt(idx))
        }
        val currentIdx = player.currentMediaItemIndex.coerceIn(0, queueTracks.lastIndex)
        val activeTrack = queueTracks.getOrNull(currentIdx)
        _selectedTrack.value = activeTrack

        _playbackState.value = _playbackState.value.copy(
            currentTrack = activeTrack,
            queue = queueTracks,
            currentQueueIndex = currentIdx,
            durationMs = resolveEffectiveDuration(player),
            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
            hasNext = queueTracks.size > 1,
            hasPrevious = queueTracks.size > 1
        )
    }

    private fun setupPlayerListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                syncTimelineState(player)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    _loadingTrackId.value = mediaItem.mediaId
                }
                syncTimelineState(player)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    _loadingTrackId.value = null
                }
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
                val isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                    _loadingTrackId.value = null
                }
                _playbackState.value = _playbackState.value.copy(
                    isBuffering = isBuffering,
                    durationMs = resolveEffectiveDuration(player),
                    currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("MusicViewModel", "Erro no ExoPlayer: ${error.errorCodeName} - ${error.message}", error)
                _loadingTrackId.value = null
                val errorMsg = "Falha ao carregar a faixa. O servidor recusou a conexão."
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
                res.songs.forEach { trackMetadataMap[it.id] = it }
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
        val controller = mediaController ?: return

        // Se a faixa já for a atual e estiver tocando, apenas pausa
        if (_playbackState.value.currentTrack?.id == track.id && _playbackState.value.isPlaying && _loadingTrackId.value == null) {
            togglePlayPause()
            return
        }

        // Se a faixa já for a atual e estiver pausada, retoma reprodução
        if (
            _playbackState.value.currentTrack?.id == track.id &&
            !_playbackState.value.isPlaying &&
            _loadingTrackId.value == null &&
            controller.mediaItemCount > 0
        ) {
            controller.play()
            return
        }

        val currentSongs = _searchResult.value.songs
        val indexInSearch = currentSongs.indexOfFirst { it.id == track.id }
        val newQueue = if (indexInSearch >= 0) {
            currentSongs
        } else {
            val existingQueue = _playbackState.value.queue
            if (existingQueue.any { it.id == track.id }) {
                existingQueue
            } else {
                listOf(track)
            }
        }
        val startIndex = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        _loadingTrackId.value = track.id
        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            queue = newQueue,
            currentQueueIndex = startIndex,
            isPlaying = false,
            currentPositionMs = 0L,
            durationMs = (track.durationSeconds * 1000L).coerceAtLeast(0L),
            isBuffering = true,
            hasNext = newQueue.size > 1,
            hasPrevious = newQueue.size > 1,
            errorMessage = null
        )

        val mediaItems = newQueue.map { trackToMediaItem(it) }
        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun playQueueItemAt(index: Int) {
        val controller = mediaController ?: return
        if (index !in 0 until controller.mediaItemCount) return

        if (index == controller.currentMediaItemIndex) {
            togglePlayPause()
        } else {
            val targetTrack = _playbackState.value.queue.getOrNull(index)
            _loadingTrackId.value = targetTrack?.id
            _playbackState.value = _playbackState.value.copy(
                currentTrack = targetTrack ?: _playbackState.value.currentTrack,
                currentQueueIndex = index,
                currentPositionMs = 0L,
                isBuffering = true
            )
            controller.seekToDefaultPosition(index)
            controller.play()
        }
    }

    /**
     * Reordena um item na fila do ExoPlayer disparando `moveMediaItem(fromIndex, toIndex)`.
     */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentQueue = _playbackState.value.queue
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices || fromIndex == toIndex) {
            return
        }

        // Atualização otimista na UI para fluidez durante o Drag-and-Drop
        val mutableQueue = currentQueue.toMutableList()
        val movedItem = mutableQueue.removeAt(fromIndex)
        mutableQueue.add(toIndex, movedItem)

        val oldActiveIdx = _playbackState.value.currentQueueIndex
        val newActiveIdx = when {
            oldActiveIdx == fromIndex -> toIndex
            fromIndex < oldActiveIdx && toIndex >= oldActiveIdx -> oldActiveIdx - 1
            fromIndex > oldActiveIdx && toIndex <= oldActiveIdx -> oldActiveIdx + 1
            else -> oldActiveIdx
        }

        _playbackState.value = _playbackState.value.copy(
            queue = mutableQueue,
            currentQueueIndex = newActiveIdx
        )

        mediaController?.moveMediaItem(fromIndex, toIndex)
    }

    /**
     * Remove um item da fila do ExoPlayer disparando `removeMediaItem(index)`.
     */
    fun removeQueueItem(index: Int) {
        val controller = mediaController ?: return
        val currentQueue = _playbackState.value.queue
        if (index !in currentQueue.indices) return

        val removedTrack = currentQueue[index]
        controller.removeMediaItem(index)
        _userMessage.value = "Removida da fila: ${removedTrack.title}"
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.mediaItemCount == 0) return

        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0L)
            }
            controller.play()
        }
    }

    fun skipNext() {
        val controller = mediaController ?: return
        val count = controller.mediaItemCount
        if (count <= 0) return

        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
            controller.play()
        } else if (count > 1) {
            controller.seekToDefaultPosition(0)
            controller.play()
        } else {
            seekTo(0L)
            controller.play()
        }
    }

    fun skipPrevious() {
        val controller = mediaController ?: return
        val count = controller.mediaItemCount
        if (count <= 0) return

        if (controller.currentPosition > 3_000L) {
            seekTo(0L)
            return
        }

        if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
            controller.play()
        } else if (count > 1) {
            controller.seekToDefaultPosition(count - 1)
            controller.play()
        } else {
            seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val targetPos = positionMs.coerceAtLeast(0L)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = targetPos)
        mediaController?.seekTo(targetPos)
    }

    fun toggleFavorite(trackId: String) {
        val currentFavs = _playbackState.value.favoriteTrackIds.toMutableSet()
        val added = if (currentFavs.contains(trackId)) {
            currentFavs.remove(trackId)
            false
        } else {
            currentFavs.add(trackId)
            true
        }
        _playbackState.value = _playbackState.value.copy(favoriteTrackIds = currentFavs)
        _userMessage.value = if (added) "Adicionada aos favoritos" else "Removida dos favoritos"
    }

    fun toggleSaveToPlaylist(trackId: String) {
        val currentSaved = _playbackState.value.savedTrackIds.toMutableSet()
        val added = if (currentSaved.contains(trackId)) {
            currentSaved.remove(trackId)
            false
        } else {
            currentSaved.add(trackId)
            true
        }
        _playbackState.value = _playbackState.value.copy(savedTrackIds = currentSaved)
        _userMessage.value = if (added) "Salva na sua biblioteca" else "Removida da biblioteca"
    }

    fun toggleDownload(trackId: String) {
        val currentDownloads = _playbackState.value.downloadedTrackIds.toMutableSet()
        val added = if (currentDownloads.contains(trackId)) {
            currentDownloads.remove(trackId)
            false
        } else {
            currentDownloads.add(trackId)
            true
        }
        _playbackState.value = _playbackState.value.copy(downloadedTrackIds = currentDownloads)
        _userMessage.value = if (added) "Download agendado para modo offline" else "Download removido"
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
