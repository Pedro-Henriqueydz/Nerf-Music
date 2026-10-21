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
import com.nerf.music.data.model.HomeSection
import com.nerf.music.data.model.PlaybackState
import com.nerf.music.data.model.QueueFilterChip
import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.SearchResult
import com.nerf.music.data.model.Track
import com.nerf.music.data.repository.MusicRepository
import com.nerf.music.data.repository.MusicRepositoryImpl
import com.nerf.music.player.AudioPlayerService
import com.nerf.music.updater.AppUpdateInfo
import com.nerf.music.updater.AppUpdateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicViewModel(
    private val repository: MusicRepository = MusicRepositoryImpl(),
    private val appUpdateManager: AppUpdateManager = AppUpdateManager()
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

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _homeSections = MutableStateFlow<List<HomeSection>>(emptyList())
    val homeSections: StateFlow<List<HomeSection>> = _homeSections.asStateFlow()

    private val _isLoadingHome = MutableStateFlow(false)
    val isLoadingHome: StateFlow<Boolean> = _isLoadingHome.asStateFlow()

    private val _homeError = MutableStateFlow<String?>(null)
    val homeError: StateFlow<String?> = _homeError.asStateFlow()

    // Histórico da sessão para o Speed Dial ("Ouvidas Recentemente")
    private val _recentlyPlayed = MutableStateFlow<List<Track>>(emptyList())
    val recentlyPlayed: StateFlow<List<Track>> = _recentlyPlayed.asStateFlow()

    // Cache de metadados das faixas conhecidas para reconstrução rápida a partir do Timeline
    private val trackMetadataMap = mutableMapOf<String, Track>()

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var progressUpdateJob: Job? = null
    private var radioFetchJob: Job? = null
    private var seedVideoId: String? = null
    private var hasCheckedForUpdates = false

    fun initializePlayerController(context: Context) {
        if (!hasCheckedForUpdates) {
            hasCheckedForUpdates = true
            checkForAppUpdates()
            loadHomeFeed()
        }

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

    fun loadHomeFeed() {
        viewModelScope.launch {
            _isLoadingHome.value = true
            _homeError.value = null
            repository.getHomeFeed().onSuccess { sections ->
                _homeSections.value = sections
                _isLoadingHome.value = false
            }.onFailure { error ->
                _homeError.value = "Não foi possível carregar o feed inicial."
                _isLoadingHome.value = false
            }
        }
    }

    fun checkForAppUpdates() {
        viewModelScope.launch {
            val update = appUpdateManager.checkForUpdate()
            if (update != null) {
                _availableUpdate.value = update
            }
        }
    }

    fun dismissUpdateDialog() {
        _availableUpdate.value = null
    }

    fun confirmAndDownloadUpdate(context: Context) {
        val updateInfo = _availableUpdate.value ?: return
        _availableUpdate.value = null
        appUpdateManager.downloadAndInstallApk(
            context = context,
            updateInfo = updateInfo,
            onStatusMessage = { msg -> _userMessage.value = msg }
        )
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
                hasPrevious = false,
                shuffleModeEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode
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
            hasPrevious = queueTracks.size > 1,
            shuffleModeEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            queueSourceTitle = _playbackState.value.queueSourceTitle ?: activeTrack?.title
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

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playbackState.value = _playbackState.value.copy(
                    shuffleModeEnabled = shuffleModeEnabled
                )
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _playbackState.value = _playbackState.value.copy(
                    repeatMode = repeatMode
                )
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

    /**
     * Inicia a reprodução imediata da música selecionada na SearchScreen e, em background,
     * consulta `/youtubei/v1/next` (`getUpNextRadio`) para popular automaticamente a fila
     * dinâmica no `MediaController` via `addMediaItems`, além de carregar os chips de filtro.
     */
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

        radioFetchJob?.cancel()
        seedVideoId = track.id

        // Atualiza histórico do Speed Dial com no máximo 5 faixas
        val updatedRecent = (listOf(track) + _recentlyPlayed.value.filterNot { it.id == track.id }).take(5)
        _recentlyPlayed.value = updatedRecent

        val initialQueue = listOf(track)
        _loadingTrackId.value = track.id
        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            queue = initialQueue,
            currentQueueIndex = 0,
            isPlaying = false,
            currentPositionMs = 0L,
            durationMs = (track.durationSeconds * 1000L).coerceAtLeast(0L),
            isBuffering = true,
            hasNext = false,
            hasPrevious = false,
            queueSourceTitle = track.title,
            queueFilterChips = emptyList(),
            selectedQueueChipParams = null,
            isLoadingQueueRadio = true,
            errorMessage = null
        )

        // Inicia reprodução imediata da faixa clicada
        controller.setMediaItem(trackToMediaItem(track))
        controller.prepare()
        controller.play()

        // Busca a Rádio Dinâmica ("A seguir") e os Chips em background
        radioFetchJob = viewModelScope.launch {
            val radioResult = repository.getUpNextRadio(videoId = track.id, params = null)
            radioResult.onSuccess { upNext ->
                upNext.tracks.forEach { trackMetadataMap[it.id] = it }

                val seenIds = mutableSetOf(track.id)
                val radioTracks = upNext.tracks.filter { seenIds.add(it.id) }

                val activeController = mediaController
                if (activeController != null && radioTracks.isNotEmpty()) {
                    val radioMediaItems = radioTracks.map { trackToMediaItem(it) }
                    activeController.addMediaItems(radioMediaItems)
                }

                val selectedParams = upNext.chips.firstOrNull { it.isSelected }?.params
                _playbackState.value = _playbackState.value.copy(
                    queueSourceTitle = track.title,
                    queueFilterChips = upNext.chips,
                    selectedQueueChipParams = selectedParams,
                    isLoadingQueueRadio = false
                )
            }.onFailure { error ->
                Log.w("MusicViewModel", "Falha ao carregar rádio dinâmica para ${track.id}: ${error.message}")
                val fallbackSongs = _searchResult.value.songs
                val seenIds = mutableSetOf(track.id)
                val extraTracks = fallbackSongs.filter { seenIds.add(it.id) }
                val activeController = mediaController
                if (activeController != null && extraTracks.isNotEmpty()) {
                    activeController.addMediaItems(extraTracks.map { trackToMediaItem(it) })
                }
                _playbackState.value = _playbackState.value.copy(
                    isLoadingQueueRadio = false
                )
            }
        }
    }

    /**
     * Atualiza a fila dinâmica quando o usuário seleciona um Chip de filtro
     * (ex: "Tudo", "Familiar", "Descobrir", "Popular", "Em alta") na Gaveta da Fila.
     */
    fun selectQueueFilterChip(chip: QueueFilterChip) {
        val currentTrack = _playbackState.value.currentTrack ?: return
        val baseVideoId = seedVideoId ?: currentTrack.id

        val updatedChipsOptimistic = _playbackState.value.queueFilterChips.map {
            it.copy(isSelected = it.params == chip.params)
        }
        _playbackState.value = _playbackState.value.copy(
            queueFilterChips = updatedChipsOptimistic,
            selectedQueueChipParams = chip.params,
            isLoadingQueueRadio = true
        )

        radioFetchJob?.cancel()
        radioFetchJob = viewModelScope.launch {
            val result = repository.getUpNextRadio(videoId = baseVideoId, params = chip.params)
            result.onSuccess { upNext ->
                upNext.tracks.forEach { trackMetadataMap[it.id] = it }

                val controller = mediaController
                if (controller != null && controller.mediaItemCount > 0) {
                    val currentIdx = controller.currentMediaItemIndex.coerceAtLeast(0)
                    if (controller.mediaItemCount > currentIdx + 1) {
                        controller.removeMediaItems(currentIdx + 1, controller.mediaItemCount)
                    }

                    val existingIds = (0..currentIdx).mapNotNull { idx ->
                        if (idx < controller.mediaItemCount) controller.getMediaItemAt(idx).mediaId else null
                    }.toMutableSet()

                    val newUpcomingTracks = upNext.tracks.filter { existingIds.add(it.id) }
                    if (newUpcomingTracks.isNotEmpty()) {
                        controller.addMediaItems(newUpcomingTracks.map { trackToMediaItem(it) })
                    }
                    syncTimelineState(controller)
                }

                val finalChips = if (upNext.chips.isNotEmpty()) {
                    upNext.chips.map { it.copy(isSelected = it.params == chip.params) }
                } else {
                    _playbackState.value.queueFilterChips.map {
                        it.copy(isSelected = it.params == chip.params)
                    }
                }

                _playbackState.value = _playbackState.value.copy(
                    queueFilterChips = finalChips,
                    selectedQueueChipParams = chip.params,
                    isLoadingQueueRadio = false
                )
            }.onFailure { error ->
                Log.w("MusicViewModel", "Falha ao filtrar fila com chip ${chip.title}: ${error.message}")
                _playbackState.value = _playbackState.value.copy(
                    isLoadingQueueRadio = false
                )
                _userMessage.value = "Não foi possível atualizar o filtro \"${chip.title}\""
            }
        }
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

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newShuffle = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = newShuffle
        _playbackState.value = _playbackState.value.copy(shuffleModeEnabled = newShuffle)
    }

    fun toggleRepeat() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _playbackState.value = _playbackState.value.copy(repeatMode = nextMode)
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
        radioFetchJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
