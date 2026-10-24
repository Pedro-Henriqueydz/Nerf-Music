package com.nerf.music.ui.viewmodel

import java.util.Locale

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
import com.nerf.music.data.model.AlbumDetail
import com.nerf.music.data.model.ArtistDetail
import com.nerf.music.data.model.HomeChip
import com.nerf.music.data.model.HomeSection
import com.nerf.music.data.model.PlaybackState
import com.nerf.music.data.model.QueueFilterChip
import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.SearchResult
import com.nerf.music.data.model.Track
import com.nerf.music.data.repository.MusicRepository
import com.nerf.music.data.repository.MusicRepositoryImpl
import com.nerf.music.data.repository.UserHistoryRepository
import com.nerf.music.data.repository.UserHistoryRepositoryImpl
import com.nerf.music.player.AudioPlayerService
import com.nerf.music.data.repository.UpdateRepository
import com.nerf.music.data.repository.UpdateRepositoryImpl
import com.nerf.music.updater.AppUpdateInfo
import com.nerf.music.updater.UpdateNotificationHelper
import com.nerf.music.data.model.TrackRelatedData
import com.nerf.music.data.model.LyricsData
import com.nerf.music.data.model.SyncedLyricsLine
import com.nerf.music.data.repository.LyricsRepository
import com.nerf.music.data.repository.LyricsRepositoryImpl
import com.nerf.music.data.repository.UserSettingsRepository
import com.nerf.music.data.repository.UserSettingsRepositoryImpl
import com.nerf.music.data.repository.DataSaverMode
import com.nerf.music.data.repository.AudioQuality
import com.nerf.music.data.repository.UserAffinityRepository
import com.nerf.music.data.repository.UserAffinityRepositoryImpl
import com.nerf.music.player.SmartRadioEngine
import com.nerf.music.player.RadioTuningMode
import com.nerf.music.player.NewPipeStreamResolver
import com.nerf.music.util.NetworkUtils
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import com.nerf.music.util.StorageBreakdown
import com.nerf.music.util.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import com.nerf.music.data.repository.SpotifyCanvasRepository
import com.nerf.music.data.repository.SpotifyCanvasRepositoryImpl
import com.nerf.music.data.repository.LibraryRepository
import com.nerf.music.data.repository.LibraryRepositoryImpl
import com.nerf.music.data.repository.DownloadAudioQuality
import com.nerf.music.data.repository.SmartDownloadManager
import com.nerf.music.data.repository.SmartDownloadSyncState
import com.nerf.music.work.SmartDownloadWorker
import com.nerf.music.data.model.UserPlaylist
import com.nerf.music.data.model.DownloadedTrack
import com.nerf.music.data.repository.HomeFeedSynthesizer
import kotlinx.coroutines.launch

class MusicViewModel(
    private val repository: MusicRepository = MusicRepositoryImpl(),
    private val updateRepository: UpdateRepository = UpdateRepositoryImpl(),
    private val lyricsRepository: LyricsRepository = LyricsRepositoryImpl(),
    private val spotifyCanvasRepository: SpotifyCanvasRepository = SpotifyCanvasRepositoryImpl(),
    private val homeFeedSynthesizer: HomeFeedSynthesizer = HomeFeedSynthesizer()
) : ViewModel() {

    companion object {
        private const val EXTRA_DURATION_TEXT = "extra_duration_text"
        private const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        private const val EXTRA_THUMBNAIL_URL = "extra_thumbnail_url"
    }

    private var userSettingsRepository: UserSettingsRepository? = null

    private val _isCanvasEnabled = MutableStateFlow(true)
    val isCanvasEnabled: StateFlow<Boolean> = _isCanvasEnabled.asStateFlow()

    private val _isCanvasWifiOnly = MutableStateFlow(true)
    val isCanvasWifiOnly: StateFlow<Boolean> = _isCanvasWifiOnly.asStateFlow()

    private val _isLyricsAboveTitleEnabled = MutableStateFlow(true)
    val isLyricsAboveTitleEnabled: StateFlow<Boolean> = _isLyricsAboveTitleEnabled.asStateFlow()

    private val _isLibraryGridView = MutableStateFlow(true)
    val isLibraryGridView: StateFlow<Boolean> = _isLibraryGridView.asStateFlow()

    private val _isGaplessEnabled = MutableStateFlow(true)
    val isGaplessEnabled: StateFlow<Boolean> = _isGaplessEnabled.asStateFlow()

    private val _isAutomixEnabled = MutableStateFlow(true)
    val isAutomixEnabled: StateFlow<Boolean> = _isAutomixEnabled.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(0)
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    private val _dataSaverMode = MutableStateFlow(DataSaverMode.ALWAYS_OFF)
    val dataSaverMode: StateFlow<DataSaverMode> = _dataSaverMode.asStateFlow()

    private val _isDownloadOverCellular = MutableStateFlow(false)
    val isDownloadOverCellular: StateFlow<Boolean> = _isDownloadOverCellular.asStateFlow()

    private val _wifiAudioQuality = MutableStateFlow(AudioQuality.HIGH)
    val wifiAudioQuality: StateFlow<AudioQuality> = _wifiAudioQuality.asStateFlow()

    private val _cellularAudioQuality = MutableStateFlow(AudioQuality.AUTOMATIC)
    val cellularAudioQuality: StateFlow<AudioQuality> = _cellularAudioQuality.asStateFlow()
    private var smartDownloadManager: SmartDownloadManager? = null

    private val _downloadAudioQuality = MutableStateFlow(DownloadAudioQuality.LOSSLESS)
    val downloadAudioQuality: StateFlow<DownloadAudioQuality> = _downloadAudioQuality.asStateFlow()

    private val _isSmartDownloadsEnabled = MutableStateFlow(true)
    val isSmartDownloadsEnabled: StateFlow<Boolean> = _isSmartDownloadsEnabled.asStateFlow()

    private val _smartDownloadsLimit = MutableStateFlow(100)
    val smartDownloadsLimit: StateFlow<Int> = _smartDownloadsLimit.asStateFlow()

    private val _isSmartDownloadsWifiOnly = MutableStateFlow(true)
    val isSmartDownloadsWifiOnly: StateFlow<Boolean> = _isSmartDownloadsWifiOnly.asStateFlow()

    private val _smartDownloadSyncState = MutableStateFlow(SmartDownloadSyncState())
    val smartDownloadSyncState: StateFlow<SmartDownloadSyncState> = _smartDownloadSyncState.asStateFlow()

    private var sleepTimerJob: Job? = null
    private val _sleepTimerRemainingSeconds = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Long?> = _sleepTimerRemainingSeconds.asStateFlow()

    private val _sleepTimerAtEndOfTrack = MutableStateFlow(false)
    val sleepTimerAtEndOfTrack: StateFlow<Boolean> = _sleepTimerAtEndOfTrack.asStateFlow()

    private val _storageBreakdown = MutableStateFlow<StorageBreakdown?>(null)
    val storageBreakdown: StateFlow<StorageBreakdown?> = _storageBreakdown.asStateFlow()

    // URL do vídeo para o background do Player (Canvas)
    private val _canvasVideoUrl = MutableStateFlow<String?>(null)
    val canvasVideoUrl: StateFlow<String?> = _canvasVideoUrl.asStateFlow()

    private val _isLoadingCanvasVideo = MutableStateFlow(false)
    val isLoadingCanvasVideo: StateFlow<Boolean> = _isLoadingCanvasVideo.asStateFlow()

    // Letras da faixa atual
    private val _currentLyrics = MutableStateFlow<LyricsData?>(null)
    val currentLyrics: StateFlow<LyricsData?> = _currentLyrics.asStateFlow()

    private val _trackRelatedData = MutableStateFlow<TrackRelatedData?>(null)
    val trackRelatedData: StateFlow<TrackRelatedData?> = _trackRelatedData.asStateFlow()

    private val _isLoadingRelated = MutableStateFlow(false)
    val isLoadingRelated: StateFlow<Boolean> = _isLoadingRelated.asStateFlow()

    private var relatedFetchJob: kotlinx.coroutines.Job? = null

    // Frase atual da letra sincronizada com a reprodução
    private val _currentLyricsLine = MutableStateFlow<String?>(null)
    val currentLyricsLine: StateFlow<String?> = _currentLyricsLine.asStateFlow()

    private var canvasFetchJob: Job? = null
    private var lyricsFetchJob: Job? = null
    private val streamResolver = NewPipeStreamResolver()
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

    // Exibe o Change Log da versão após atualização bem-sucedida do app
    private val _postUpdateChangelog = MutableStateFlow<String?>(null)
    val postUpdateChangelog: StateFlow<String?> = _postUpdateChangelog.asStateFlow()

    private val defaultChips = listOf(
        HomeChip("Energia", "ggNCSgQIDhABSgQIDBADSgQICRABSgQIDRABSgQIAxABSgQIBxABSgQICBABSgQIBBABSgQIChABSgQIBhABSgQIBRAB"),
        HomeChip("Relax", "ggNCSgQIBxABSgQIDBADSgQIDhABSgQICRABSgQIDRABSgQIAxABSgQICBABSgQIBBABSgQIChABSgQIBhABSgQIBRAB"),
        HomeChip("Para treinar", "ggNCSgQIBBABSgQIDBADSgQIDhABSgQICRABSgQIDRABSgQIAxABSgQIBxABSgQICBABSgQIChABSgQIBhABSgQIBRAB"),
        HomeChip("Foco", "ggNCSgQIChABSgQIDBADSgQIDhABSgQICRABSgQIDRABSgQIAxABSgQIBxABSgQICBABSgQIBBABSgQIBhABSgQIBRAB"),
        HomeChip("Festa", "ggNCSgQICRABSgQIDBADSgQIDhABSgQIDRABSgQIAxABSgQIBxABSgQICBABSgQIBBABSgQIChABSgQIBhABSgQIBRAB"),
        HomeChip("Romance", "ggNCSgQIAxABSgQIDBADSgQIDhABSgQICRABSgQIDRABSgQIBxABSgQICBABSgQIBBABSgQIChABSgQIBhABSgQIBRAB")
    )
    private val _homeChips = MutableStateFlow<List<HomeChip>>(defaultChips)
    val homeChips: StateFlow<List<HomeChip>> = _homeChips.asStateFlow()

    private val _selectedHomeChip = MutableStateFlow<HomeChip?>(null)
    val selectedHomeChip: StateFlow<HomeChip?> = _selectedHomeChip.asStateFlow()

    private val _homeSections = MutableStateFlow<List<HomeSection>>(emptyList())
    val homeSections: StateFlow<List<HomeSection>> = _homeSections.asStateFlow()

    private val _isLoadingHome = MutableStateFlow(false)
    val isLoadingHome: StateFlow<Boolean> = _isLoadingHome.asStateFlow()

    private val _homeError = MutableStateFlow<String?>(null)
    val homeError: StateFlow<String?> = _homeError.asStateFlow()

    private val _albumDetail = MutableStateFlow<AlbumDetail?>(null)
    val albumDetail: StateFlow<AlbumDetail?> = _albumDetail.asStateFlow()

    private val _isLoadingAlbum = MutableStateFlow(false)
    val isLoadingAlbum: StateFlow<Boolean> = _isLoadingAlbum.asStateFlow()

    private val _albumError = MutableStateFlow<String?>(null)
    val albumError: StateFlow<String?> = _albumError.asStateFlow()

    private val _artistDetail = MutableStateFlow<ArtistDetail?>(null)
    val artistDetail: StateFlow<ArtistDetail?> = _artistDetail.asStateFlow()

    private val _isLoadingArtist = MutableStateFlow(false)
    val isLoadingArtist: StateFlow<Boolean> = _isLoadingArtist.asStateFlow()

    private val _artistError = MutableStateFlow<String?>(null)
    val artistError: StateFlow<String?> = _artistError.asStateFlow()

    private val _libraryTracksForArtist = MutableStateFlow<List<Track>>(emptyList())
    val libraryTracksForArtist: StateFlow<List<Track>> = _libraryTracksForArtist.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

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
    private var userHistoryRepository: UserHistoryRepository? = null
    private var libraryRepository: LibraryRepository? = null
    private var userAffinityRepository: UserAffinityRepository? = null
    private var currentTrackPlayStartTime = 0L
    private var activeTrackId: String? = null
    private var isLoadingMoreQueue = false
    private val _currentRadioTuningMode = MutableStateFlow(RadioTuningMode.BALANCED)
    val currentRadioTuningMode: StateFlow<RadioTuningMode> = _currentRadioTuningMode.asStateFlow()

    private val _likedTracks = MutableStateFlow<List<Track>>(emptyList())
    val likedTracks: StateFlow<List<Track>> = _likedTracks.asStateFlow()

    private val _userPlaylists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val userPlaylists: StateFlow<List<UserPlaylist>> = _userPlaylists.asStateFlow()

    private val _downloadedTracks = MutableStateFlow<List<DownloadedTrack>>(emptyList())
    val downloadedTracks: StateFlow<List<DownloadedTrack>> = _downloadedTracks.asStateFlow()

    private val _downloadProgressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Float>> = _downloadProgressMap.asStateFlow()
    private var applicationContext: Context? = null

    fun initializePlayerController(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
        }
        if (userSettingsRepository == null) {
            val settingsRepo = UserSettingsRepositoryImpl(context.applicationContext)
            userSettingsRepository = settingsRepo
            _isCanvasEnabled.value = settingsRepo.isCanvasEnabled.value
            _isCanvasWifiOnly.value = settingsRepo.isCanvasWifiOnly.value
            _isLyricsAboveTitleEnabled.value = settingsRepo.isLyricsAboveTitleEnabled.value
            _isLibraryGridView.value = settingsRepo.isLibraryGridView.value
            _isGaplessEnabled.value = settingsRepo.isGaplessEnabled.value
            _isAutomixEnabled.value = settingsRepo.isAutomixEnabled.value
            _crossfadeSeconds.value = settingsRepo.crossfadeSeconds.value
            _dataSaverMode.value = settingsRepo.dataSaverMode.value
            _isDownloadOverCellular.value = settingsRepo.isDownloadOverCellular.value
            _wifiAudioQuality.value = settingsRepo.wifiAudioQuality.value
            _cellularAudioQuality.value = settingsRepo.cellularAudioQuality.value
            _downloadAudioQuality.value = settingsRepo.downloadAudioQuality.value
            _isSmartDownloadsEnabled.value = settingsRepo.isSmartDownloadsEnabled.value
            _smartDownloadsLimit.value = settingsRepo.smartDownloadsLimit.value
            _isSmartDownloadsWifiOnly.value = settingsRepo.isSmartDownloadsWifiOnly.value
        }
        if (userHistoryRepository == null) {
            val historyRepo = UserHistoryRepositoryImpl(context.applicationContext)
            userHistoryRepository = historyRepo
            val recents = historyRepo.getRecentlyPlayed()
            if (recents.isNotEmpty()) {
                _recentlyPlayed.value = recents.take(15)
            }
            val cachedHome = historyRepo.getCachedHomeFeed()
            if (cachedHome.isNotEmpty() && _homeSections.value.isEmpty()) {
                _homeSections.value = cachedHome
            }
            _searchHistory.value = historyRepo.getSearchHistory()
            checkPostUpdateChangelog(historyRepo)
        }
        if (userAffinityRepository == null) {
            userAffinityRepository = UserAffinityRepositoryImpl(context.applicationContext)
        }

        if (libraryRepository == null) {
            val libRepo = LibraryRepositoryImpl(context.applicationContext)
            libraryRepository = libRepo
            viewModelScope.launch {
                libRepo.likedTracksFlow.collect { list ->
                    _likedTracks.value = list
                    val ids = list.map { it.id }.toSet()
                    _playbackState.value = _playbackState.value.copy(favoriteTrackIds = ids)
                }
            }
            viewModelScope.launch {
                libRepo.playlistsFlow.collect { list ->
                    _userPlaylists.value = list
                    val ids = list.flatMap { it.tracks }.map { it.id }.toSet()
                    _playbackState.value = _playbackState.value.copy(savedTrackIds = ids)
                }
            }
            viewModelScope.launch {
                libRepo.downloadedTracksFlow.collect { list ->
                    _downloadedTracks.value = list
                    val ids = list.map { it.track.id }.toSet()
                    _playbackState.value = _playbackState.value.copy(downloadedTrackIds = ids)
                }
            }
        }

        val activeLibRepo = libraryRepository
        val activeSettingsRepo = userSettingsRepository
        val activeHistoryRepo = userHistoryRepository
        val activeAffinityRepo = userAffinityRepository ?: UserAffinityRepositoryImpl(context.applicationContext).also { userAffinityRepository = it }

        if (smartDownloadManager == null && activeLibRepo != null && activeSettingsRepo != null && activeHistoryRepo != null) {
            val sdManager = SmartDownloadManager(
                context = context.applicationContext,
                libraryRepository = activeLibRepo,
                userSettingsRepository = activeSettingsRepo,
                userHistoryRepository = activeHistoryRepo,
                userAffinityRepository = activeAffinityRepo
            )
            smartDownloadManager = sdManager
            viewModelScope.launch {
                sdManager.syncState.collect { state ->
                    _smartDownloadSyncState.value = state
                }
            }
        }

        if (!hasCheckedForUpdates) {
            hasCheckedForUpdates = true
            checkForAppUpdates()
            loadHomeFeed()
        }

        if (mediaController != null) {
            syncWithPlayer()
            return
        }

        val appContext = context.applicationContext
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, AudioPlayerService::class.java)
        )
        controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get() ?: return@addListener
                mediaController = controller
                setupPlayerListener(controller)
                syncTimelineState(controller)
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Erro ao obter MediaController conectado", e)
            }
        }, MoreExecutors.directExecutor())
    }

    fun loadHomeFeed(params: String? = _selectedHomeChip.value?.params) {
        viewModelScope.launch {
            _isLoadingHome.value = true
            _homeError.value = null
            val historyRepo = userHistoryRepository
            val seedId = historyRepo?.getFavoriteSeedVideoId()
            val topArtists = historyRepo?.getTopArtists(5) ?: emptyList()

            repository.getPersonalizedHomeFeedData(seedId, topArtists, params).onSuccess { feedData ->
                val synthesizedFeed = homeFeedSynthesizer.synthesizeHomeFeed(
                    baseFeedData = feedData,
                    historyRepo = userHistoryRepository,
                    libraryRepo = libraryRepository,
                    affinityRepo = userAffinityRepository,
                    selectedChip = _selectedHomeChip.value
                )

                if (synthesizedFeed.chips.isNotEmpty() && _homeChips.value.isEmpty()) {
                    _homeChips.value = synthesizedFeed.chips
                }
                _homeSections.value = synthesizedFeed.sections
                _isLoadingHome.value = false
                historyRepo?.saveCachedHomeFeed(synthesizedFeed.sections)

                // Pre-aquece a resolucao da primeira faixa da tela inicial para inicio instantaneo
                synthesizedFeed.sections.firstOrNull { it.items.isNotEmpty() }?.items?.firstOrNull()?.let { firstTrack ->
                    AudioPlayerService.prefetchTrack(firstTrack.id)
                }
            }.onFailure {
                // Fallback com sintese local caso nao haja conexao com o catalogo
                val fallbackSynthesized = homeFeedSynthesizer.synthesizeHomeFeed(
                    baseFeedData = com.nerf.music.data.model.HomeFeedData(),
                    historyRepo = userHistoryRepository,
                    libraryRepo = libraryRepository,
                    affinityRepo = userAffinityRepository,
                    selectedChip = _selectedHomeChip.value
                )
                if (fallbackSynthesized.sections.isNotEmpty()) {
                    _homeSections.value = fallbackSynthesized.sections
                    _isLoadingHome.value = false
                } else {
                    _homeError.value = "Não foi possível carregar o feed inicial."
                    _isLoadingHome.value = false
                }
            }
        }
    }

    fun playLikedTracks(startIndex: Int = 0, isShuffle: Boolean = false) {
        val likedTracks = libraryRepository?.likedTracksFlow?.value.orEmpty()
        if (likedTracks.isEmpty()) return
        playTrackList(
            title = "Músicas Curtidas",
            tracks = likedTracks,
            startIndex = startIndex,
            isShuffle = isShuffle
        )
    }

    fun playDownloadedTracks(startIndex: Int = 0, isShuffle: Boolean = false) {
        val downloaded = libraryRepository?.downloadedTracksFlow?.value.orEmpty().map { it.track }
        val tracksToPlay = downloaded.ifEmpty { libraryRepository?.likedTracksFlow?.value.orEmpty() }
        if (tracksToPlay.isEmpty()) return
        playTrackList(
            title = "Mix Offline",
            tracks = tracksToPlay,
            startIndex = startIndex,
            isShuffle = isShuffle
        )
    }

    fun handleHomeTrackClick(track: com.nerf.music.data.model.Track, parentSection: com.nerf.music.data.model.HomeSection? = null) {
        when {
            track.sourceIdentifier == "SHORTCUT_LIKED" || track.id == "downloads_liked_music" -> {
                playLikedTracks()
            }
            track.sourceIdentifier == "DOWNLOADS_OFFLINE_MIX" || track.id == "downloads_offline_mix" -> {
                playDownloadedTracks()
            }
            track.sourceIdentifier == "USER_PLAYLIST" -> {
                val playlist = libraryRepository?.playlistsFlow?.value?.find { it.id == track.id }
                if (playlist != null) {
                    playUserPlaylist(playlist)
                } else {
                    playTrack(track)
                }
            }
            parentSection != null && parentSection.style == com.nerf.music.data.model.HomeSectionStyle.SPEED_DIAL -> {
                playTrack(track)
            }
            parentSection != null && parentSection.style == com.nerf.music.data.model.HomeSectionStyle.QUICK_PICKS_GRID -> {
                val idx = parentSection.items.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                playSection(parentSection, startIndex = idx)
            }
            else -> {
                playTrack(track)
            }
        }
    }

    fun selectHomeChip(chip: HomeChip?) {
        val nextChip = if (chip == null || _selectedHomeChip.value?.params == chip.params) {
            null
        } else {
            chip
        }
        _selectedHomeChip.value = nextChip
        loadHomeFeed(params = nextChip?.params)
    }

    fun playSection(section: HomeSection, startIndex: Int = 0) {
        val tracks = section.items
        if (tracks.isEmpty()) return
        val title = section.title.ifBlank { "Escolha a dedo" }
        playTrackList(title = title, tracks = tracks, startIndex = startIndex, isShuffle = false)
    }

    fun checkForAppUpdates() {
        viewModelScope.launch {
            try {
                val update = updateRepository.checkForUpdate()
                if (update != null) {
                    _availableUpdate.value = update
                    applicationContext?.let { ctx ->
                        UpdateNotificationHelper.showUpdateNotification(ctx, update)
                    }
                }
            } catch (e: Exception) {
                Log.w("MusicViewModel", "Erro ao verificar atualizacoes: ${e.message}")
            }
        }
    }

    fun dismissUpdateDialog() {
        _availableUpdate.value = null
    }

    fun dismissChangelogDialog() {
        _postUpdateChangelog.value = null
    }

    private fun checkPostUpdateChangelog(historyRepo: UserHistoryRepository) {
        val currentVersion = com.nerf.music.BuildConfig.VERSION_NAME
        val lastSeenVersion = historyRepo.getLastSeenVersion()
        if (lastSeenVersion == null) {
            // Primeira instalação do app com essa funcionalidade: salva a versão atual sem forçar modal
            historyRepo.setLastSeenVersion(currentVersion)
        } else if (lastSeenVersion != currentVersion) {
            // O app foi atualizado! Atualiza a versão salva e carrega as novidades para exibir o Change Log imediatamente
            historyRepo.setLastSeenVersion(currentVersion)
            _postUpdateChangelog.value = getDefaultChangeLog()
        }
    }

    private fun getDefaultChangeLog(): String {
        return """
            🔧 NOVIDADES DA VERSÃO 1.2.1:
            • Notificação de Progresso em Tempo Real: Agora você pode acompanhar o download das músicas inteligentes direto na barra de status do celular! Uma notificação dinâmica mostra o progresso aumentando faixa a faixa (ex: 23 de 100) com o nome de cada música sendo baixada.
            • Cota Completa Garantida: Corrigido o comportamento que baixava menos faixas do que o selecionado (ex: apenas 23 músicas para quem tinha poucas curtidas). O app agora busca recomendações exclusivas baseadas nos seus artistas favoritos para preencher 100% da sua cota escolhida!
            • Conclusão Silenciosa e Limpa: Notificações sem alertas sonoros repetitivos a cada música baixada, finalizando com um aviso discreto de que suas músicas estão prontas para ouvir sem internet.
            • Mais Estabilidade: Correção de concorrência que impedia o download manual de travar quando o sistema em segundo plano já estava ativo.

            ⚡ SMART DOWNLOADS (DOWNLOADS INTELIGENTES):
            • Músicas Offline Automáticas: O Nerf Music agora cuida dos seus downloads por você! Suas músicas favoritas e novidades semanais são salvas automaticamente no aparelho.
            • Cota Flexível (50 a 500 Músicas): Escolha manter 50, 100, 250 ou até 500 faixas offline com estimativa em tempo real de espaço em disco.
            • Mix Offline Sempre Atualizado: Sincronização periódica em segundo plano via WorkManager — renova seu repertório apenas quando conectado ao Wi-Fi e com bateria segura.
            • Rotação Inteligente: Faixas antigas não ouvidas dão lugar a novos gostos e lançamentos automaticamente. Músicas baixadas manualmente NUNCA são apagadas!
            • Sincronização em Tempo Real: Acompanhe o progresso faixa a faixa e toque no botão de sincronizar a qualquer momento nas configurações de Armazenamento.

            💎 QUALIDADE LOSSLESS / MÁXIMA FIDELIDADE (PADRÃO):
            • Áudio Puro de Estúdio: Todos os downloads passam a ser extraídos na integridade máxima do catálogo original (Opus a 48kHz e AAC de alta resolução sem recompressão).
            • Seletor de Qualidade Dedicado: Escolha entre Lossless (Padrão), Muito Alta (~320 kbps eq.), Alta (256 kbps) ou Normal (128 kbps para máxima economia de memória).

            ✨ TRANSIÇÕES DE MÚSICA & EXPERIÊNCIA DE SOM:
            • Crossfade Suave (0s a 12s): Chega de cortes bruscos entre as músicas! O final de uma faixa se funde suavemente com o início da próxima no tempo que você escolher.
            • Reprodução Sem Pausas (Gapless): Música ininterrupta, ideal para curtir shows ao vivo e álbuns contínuos sem nenhum segundo de silêncio.
            • Automix Inteligente: O app cria mixagens naturais entre faixas para dar ritmo de festa às suas playlists.
            • Equalizador do Sistema: Acesse o painel de equalização direto das configurações para turbinar os graves ou ajustar o som ao seu gosto.
            • Temporizador de Sono (Sleep Timer): Programe para desligar a música em 15, 30, 45, 60 minutos ou ao terminar a música atual, com diminuição suave do volume.

            🚀 NOVA TELA INICIAL (ESTILO YOUTUBE MUSIC 9.41):
            • Início 3x3 com Swipe: Suas músicas recentes, playlists e atalho de Músicas Curtidas em uma grade dinâmica de 9 cards com navegação por gestos laterais.
            • Capas Vivas no Início: Acompanhe a música atual com borda em destaque, equalizador animado ao vivo e barra de progresso vermelha na própria capa.
            • Músicas para o Seu Momento: O início se adapta ao seu dia — sugerindo faixas para Churrasco no fim de semana, energia pela manhã e relax à noite.
            • Covers e Remixes Exclusivos: Uma prateleira feita só com versões alternativas (slowed, sped up, remixes e VIP edits) do seu estilo.
            • Seus Downloads Rápidos: Seus downloads e músicas curtidas em cartões com gradiente vibrante para tocar com um só toque.
            • Descoberta do Dia: Um card especial diário com músicas parecidas com o que você mais ouve.

            🧠 RÁDIO INTELIGENTE & SEM REPETIÇÕES:
            • Rádio com Proporção Áurea: 60% novidades do mesmo gênero e 40% músicas queridinhas do seu histórico, sem repetir o mesmo artista em sequência.
            • Respeito ao seu Gosto (Skip Penalty): Músicas que você pula com frequência deixam de tocar; faixas que você escuta até o fim ganham preferência.
            • Foco no Estilo: Chega de estilos nada a ver invadindo a sua rádio.
            • Durações Reais: Todas as músicas agora mostram o tempo de duração exato em minutos e segundos na fila e no player.

            ⚙️ NOVA CENTRAL DE CONFIGURAÇÕES & ARMAZENAMENTO:
            • Visual Estilo Spotify: Configurações elegantes, organizadas por categorias e com barra de busca rápida para achar qualquer opção.
            • Gerenciador de Armazenamento: Veja com precisão quanto espaço é ocupado por downloads e cache, com botão de limpeza rápida para liberar memória do celular.
        """.trimIndent()
    }

    fun confirmAndDownloadUpdate(context: Context) {
        val updateInfo = _availableUpdate.value ?: return
        _availableUpdate.value = null
        UpdateNotificationHelper.cancelUpdateNotification(context)
        updateRepository.downloadAndInstallApk(
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
    /**
     * Força a sincronização imediata da interface com o estado real do player
     * ao reconectar ou ao trazer o app para primeiro plano (onResume).
     */
    fun syncWithPlayer() {
        val controller = mediaController ?: return
        syncTimelineState(controller)
    }

    private fun syncTimelineState(player: Player) {
        val timeline = player.currentTimeline
        val itemCount = player.mediaItemCount
        val isCurrentlyPlaying = player.isPlaying
        val isBuffering = player.playbackState == Player.STATE_BUFFERING
        val currentPos = player.currentPosition.coerceAtLeast(0L)
        val effectiveDuration = resolveEffectiveDuration(player)

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
            stopProgressTracker()
            return
        }

        val queueTracks = (0 until itemCount).map { idx ->
            mediaItemToTrack(player.getMediaItemAt(idx))
        }
        val currentIdx = player.currentMediaItemIndex.coerceIn(0, queueTracks.lastIndex)
        val activeTrack = queueTracks.getOrNull(currentIdx)

        val resolvedTrack = if (activeTrack != null && effectiveDuration > 0L && (activeTrack.durationText == "--:--" || activeTrack.durationSeconds == 0L)) {
            val totalSeconds = effectiveDuration / 1000L
            val mins = totalSeconds / 60
            val secs = totalSeconds % 60
            val formatted = String.format(Locale.US, "%02d:%02d", mins, secs)
            val updated = activeTrack.copy(durationText = formatted, durationSeconds = totalSeconds)
            trackMetadataMap[activeTrack.id] = updated
            updated
        } else {
            activeTrack
        }

        val finalQueue = if (resolvedTrack != null && resolvedTrack !== activeTrack) {
            queueTracks.map { if (it.id == resolvedTrack.id) resolvedTrack else it }
        } else {
            queueTracks
        }

        if (resolvedTrack != null && _selectedTrack.value?.id != resolvedTrack.id) {
            loadLyrics(resolvedTrack)
            loadCanvasVideo(resolvedTrack)
        } else {
            prefetchNextCanvas()
        }
        _selectedTrack.value = resolvedTrack

        _playbackState.value = _playbackState.value.copy(
            currentTrack = resolvedTrack,
            queue = finalQueue,
            currentQueueIndex = currentIdx,
            isPlaying = isCurrentlyPlaying,
            isBuffering = isBuffering,
            durationMs = effectiveDuration,
            currentPositionMs = currentPos,
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
            shuffleModeEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            queueSourceTitle = _playbackState.value.queueSourceTitle ?: activeTrack?.title
        )
        updateCurrentLyricsLine(currentPos)

        if (isCurrentlyPlaying) {
            startProgressTracker()
        } else {
            stopProgressTracker()
        }
    }

    private fun setupPlayerListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_PLAY_WHEN_READY_CHANGED,
                        Player.EVENT_PLAYBACK_STATE_CHANGED,
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_POSITION_DISCONTINUITY,
                        Player.EVENT_TIMELINE_CHANGED
                    )
                ) {
                    val isPlaying = player.isPlaying
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = isPlaying,
                        isBuffering = player.playbackState == Player.STATE_BUFFERING,
                        durationMs = resolveEffectiveDuration(player),
                        currentPositionMs = pos
                    )
                    updateCurrentLyricsLine(pos)
                    if (isPlaying) {
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                }
            }
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                syncTimelineState(player)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    _loadingTrackId.value = mediaItem.mediaId
                }
                val prevId = activeTrackId
                if (!prevId.isNullOrBlank()) {
                    val prevTrack = trackMetadataMap[prevId]
                    if (prevTrack != null) {
                        val elapsed = System.currentTimeMillis() - currentTrackPlayStartTime
                        val dur = _playbackState.value.durationMs
                        val wasSkipped = reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK || 
                                         (elapsed < 30_000L && reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
                        userAffinityRepository?.recordTrackEnded(prevTrack, elapsed, dur, wasSkipped)
                    }
                }
                activeTrackId = mediaItem?.mediaId
                currentTrackPlayStartTime = System.currentTimeMillis()

                if (_sleepTimerAtEndOfTrack.value && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    _sleepTimerAtEndOfTrack.value = false
                    player.pause()
                    _userMessage.value = "Temporizador de sono: reprodução pausada ao final da música."
                }
                syncTimelineState(player)
                checkAndLoadMoreQueue(player)
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
                        val pos = controller.currentPosition.coerceAtLeast(0L)
                        val dur = resolveEffectiveDuration(controller)
                        val curTrack = _playbackState.value.currentTrack
                        val updatedTrack = if (curTrack != null && dur > 0L && (curTrack.durationText == "--:--" || curTrack.durationSeconds == 0L)) {
                            val totalSec = dur / 1000L
                            val m = totalSec / 60
                            val s = totalSec % 60
                            val fDur = String.format(Locale.US, "%02d:%02d", m, s)
                            val t = curTrack.copy(durationText = fDur, durationSeconds = totalSec)
                            trackMetadataMap[curTrack.id] = t
                            t
                        } else null

                        _playbackState.value = _playbackState.value.copy(
                            currentPositionMs = pos,
                            durationMs = dur,
                            currentTrack = updatedTrack ?: curTrack,
                            queue = if (updatedTrack != null) _playbackState.value.queue.map { if (it.id == updatedTrack.id) updatedTrack else it } else _playbackState.value.queue
                        )
                        updateCurrentLyricsLine(pos)
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
        addSearchHistory(query)

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

        // Registra no histórico persistente do usuário (sobrevive a fechamento do app)
        userHistoryRepository?.recordTrackPlayed(track)
        val updatedRecent = userHistoryRepository?.getRecentlyPlayed()?.take(15)
            ?: (listOf(track) + _recentlyPlayed.value.filterNot { it.id == track.id }).take(15)
        _recentlyPlayed.value = updatedRecent

        val initialQueue = listOf(track)
        _loadingTrackId.value = track.id
        loadLyrics(track)
        loadCanvasVideo(track)

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

        if (track.id.startsWith("playlist_")) {
            radioFetchJob = viewModelScope.launch {
                val radioResult = repository.getUpNextRadio(videoId = track.id, params = null)
                radioResult.onSuccess { upNext ->
                    upNext.tracks.forEach { trackMetadataMap[it.id] = it }
                    val firstReal = upNext.tracks.firstOrNull()
                    val activeController = mediaController
                    if (activeController != null && firstReal != null) {
                        seedVideoId = firstReal.id
                        _selectedTrack.value = firstReal
                        _loadingTrackId.value = firstReal.id
                        val mediaItems = upNext.tracks.map { trackToMediaItem(it) }
                        activeController.setMediaItems(mediaItems)
                        activeController.prepare()
                        activeController.play()
                    }
                    val selectedParams = upNext.chips.firstOrNull { it.isSelected }?.params
                    _playbackState.value = _playbackState.value.copy(
                        currentTrack = firstReal ?: track,
                        queue = upNext.tracks.ifEmpty { listOf(track) },
                        queueSourceTitle = track.title,
                        queueFilterChips = upNext.chips,
                        selectedQueueChipParams = selectedParams,
                        isLoadingQueueRadio = false,
                        relatedBrowseId = upNext.relatedBrowseId
                    )
                    if (upNext.relatedBrowseId != null) {
                        loadTrackRelated(upNext.relatedBrowseId)
                    }
                }.onFailure { error ->
                    Log.w("MusicViewModel", "Falha ao abrir playlist ${track.id}: ${error.message}")
                    _loadingTrackId.value = null
                    _playbackState.value = _playbackState.value.copy(
                        isBuffering = false,
                        isLoadingQueueRadio = false
                    )
                }
            }
            return
        }

        // Inicia reprodução imediata da faixa clicada
        controller.setMediaItem(trackToMediaItem(track))
        controller.prepare()
        controller.play()

        // Busca a Rádio Dinâmica ("A seguir") e os Chips em background
        radioFetchJob = viewModelScope.launch {
            val radioResult = repository.getUpNextRadio(videoId = track.id, params = null)
            radioResult.onSuccess { upNext ->
                upNext.tracks.forEach { freshTrack ->
                    val existing = trackMetadataMap[freshTrack.id]
                    if (existing == null || (existing.durationText == "--:--" && freshTrack.durationText != "--:--")) {
                        trackMetadataMap[freshTrack.id] = freshTrack
                    }
                    if (freshTrack.durationText != "--:--") {
                        userHistoryRepository?.updateTrackDuration(freshTrack.id, freshTrack.durationText, freshTrack.durationSeconds)
                    }
                }

                val affinityRepo = userAffinityRepository 
                    ?: applicationContext?.let { UserAffinityRepositoryImpl(it) }
                val smartQueue = if (affinityRepo != null) {
                    SmartRadioEngine.buildSmartQueue(
                        seedTrack = track,
                        rawCandidates = upNext.tracks,
                        userLikedTracks = _likedTracks.value,
                        userRecentTracks = _recentlyPlayed.value,
                        affinityRepo = affinityRepo,
                        tuningMode = _currentRadioTuningMode.value
                    )
                } else {
                    listOf(track) + upNext.tracks
                }
                val radioTracks = smartQueue.drop(1)
                val fullQueue = smartQueue

                val activeController = mediaController
                if (activeController != null && radioTracks.isNotEmpty()) {
                    val radioMediaItems = radioTracks.map { trackToMediaItem(it) }
                    activeController.addMediaItems(radioMediaItems)
                    syncTimelineState(activeController)

                    AudioPlayerService.prefetchTrack(radioTracks.first().id)
                }

                val selectedParams = upNext.chips.firstOrNull { it.isSelected }?.params
                _playbackState.value = _playbackState.value.copy(
                    queue = fullQueue,
                    hasNext = fullQueue.size > 1,
                    queueSourceTitle = track.title,
                    queueFilterChips = upNext.chips,
                    selectedQueueChipParams = selectedParams,
                    isLoadingQueueRadio = false,
                    relatedBrowseId = upNext.relatedBrowseId
                )
                if (upNext.relatedBrowseId != null) {
                    loadTrackRelated(upNext.relatedBrowseId)
                }
            }.onFailure { error ->
                Log.w("MusicViewModel", "Falha ao carregar rádio dinâmica para ${track.id}: ${error.message}")
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
            if (targetTrack != null) {
                loadLyrics(targetTrack)
                loadCanvasVideo(targetTrack)
            }
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

    /**
     * Pula imediatamente para a faixa anterior (usado em gestos de swipe),
     * sem reiniciar a música atual mesmo se ela estiver após 3 segundos.
     */
    fun forceSkipPrevious() {
        val controller = mediaController ?: return
        val count = controller.mediaItemCount
        if (count <= 0) return

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
        updateCurrentLyricsLine(targetPos)
    }

    fun toggleFavorite(trackId: String) {
        val targetTrack = trackMetadataMap[trackId] ?: _playbackState.value.currentTrack?.takeIf { it.id == trackId }
        val libRepo = libraryRepository
        if (targetTrack != null && libRepo != null) {
            val added = libRepo.toggleFavorite(targetTrack)
            if (added) {
                userAffinityRepository?.recordTrackLiked(targetTrack)
            } else {
                userAffinityRepository?.recordTrackUnliked(targetTrack)
            }
            _userMessage.value = if (added) "Adicionada às Músicas Curtidas" else "Removida das Músicas Curtidas"
        } else {
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
    }

    fun toggleSaveToPlaylist(trackId: String) {
        val targetTrack = trackMetadataMap[trackId] ?: _playbackState.value.currentTrack?.takeIf { it.id == trackId }
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
        val targetTrack = trackMetadataMap[trackId] ?: _playbackState.value.currentTrack?.takeIf { it.id == trackId }
        val libRepo = libraryRepository
        if (targetTrack == null || libRepo == null) {
            _userMessage.value = "Não foi possível baixar esta faixa"
            return
        }

        if (libRepo.isDownloaded(trackId)) {
            libRepo.removeDownload(trackId)
            _userMessage.value = "Download removido"
        } else {
            _userMessage.value = "Iniciando download para reprodução offline..."
            viewModelScope.launch {
                val currentProg = _downloadProgressMap.value.toMutableMap()
                currentProg[trackId] = 0.05f
                _downloadProgressMap.value = currentProg

                val result = libRepo.downloadTrack(targetTrack) { prog ->
                    val pMap = _downloadProgressMap.value.toMutableMap()
                    pMap[trackId] = prog
                    _downloadProgressMap.value = pMap
                }

                val pMap = _downloadProgressMap.value.toMutableMap()
                pMap.remove(trackId)
                _downloadProgressMap.value = pMap

                result.onSuccess {
                    _userMessage.value = "Download concluído com sucesso!"
                }.onFailure { err ->
                    _userMessage.value = "Falha no download: ${err.message}"
                }
            }
        }
    }

    fun createPlaylist(title: String, description: String = ""): UserPlaylist? {
        val created = libraryRepository?.createPlaylist(title, description)
        if (created != null) {
            _userMessage.value = "Playlist '${created.title}' criada!"
        }
        return created
    }

    fun deletePlaylist(playlistId: String) {
        val ok = libraryRepository?.deletePlaylist(playlistId) == true
        if (ok) {
            _userMessage.value = "Playlist removida"
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        val ok = libraryRepository?.addTrackToPlaylist(playlistId, track) == true
        _userMessage.value = if (ok) "Adicionada à playlist" else "Faixa já estava na playlist"
    }

        fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        val ok = libraryRepository?.removeTrackFromPlaylist(playlistId, trackId) == true
        if (ok) {
            _userMessage.value = "Faixa removida da playlist"
        }
    }

    fun updatePlaylistTitle(playlistId: String, newTitle: String) {
        val ok = libraryRepository?.updatePlaylistTitle(playlistId, newTitle) == true
        if (ok) {
            _userMessage.value = "Playlist renomeada"
        }
    }

    fun playUserPlaylist(playlist: UserPlaylist, startIndex: Int = 0, isShuffle: Boolean = false) {
        if (playlist.tracks.isEmpty()) return
        val effectiveTracks = if (isShuffle) playlist.tracks.shuffled() else playlist.tracks
        val targetIndex = if (isShuffle) 0 else startIndex.coerceIn(0, effectiveTracks.size - 1)
        val targetTrack = effectiveTracks[targetIndex]

        _selectedTrack.value = targetTrack
        val controller = mediaController ?: return

        radioFetchJob?.cancel()
        seedVideoId = targetTrack.id

        userHistoryRepository?.recordTrackPlayed(targetTrack)
        val updatedRecent = userHistoryRepository?.getRecentlyPlayed()?.take(15)
            ?: (listOf(targetTrack) + _recentlyPlayed.value.filterNot { it.id == targetTrack.id }).take(15)
        _recentlyPlayed.value = updatedRecent

        effectiveTracks.forEach { trackMetadataMap[it.id] = it }

        _loadingTrackId.value = targetTrack.id
        loadLyrics(targetTrack)
        loadCanvasVideo(targetTrack)
        _playbackState.value = _playbackState.value.copy(
            currentTrack = targetTrack,
            queue = effectiveTracks,
            currentQueueIndex = targetIndex,
            isPlaying = false,
            currentPositionMs = 0L,
            durationMs = (targetTrack.durationSeconds * 1000L).coerceAtLeast(0L),
            isBuffering = true,
            hasNext = targetIndex < effectiveTracks.size - 1,
            hasPrevious = targetIndex > 0,
            queueSourceTitle = playlist.title,
            queueFilterChips = emptyList(),
            selectedQueueChipParams = null,
            isLoadingQueueRadio = false,
            errorMessage = null
        )

        val mediaItems = effectiveTracks.map { trackToMediaItem(it) }
        controller.setMediaItems(mediaItems, targetIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun playTrackList(title: String, tracks: List<Track>, startIndex: Int = 0, isShuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        val effectiveTracks = if (isShuffle) tracks.shuffled() else tracks
        val targetIndex = if (isShuffle) 0 else startIndex.coerceIn(0, effectiveTracks.size - 1)
        val targetTrack = effectiveTracks[targetIndex]

        _selectedTrack.value = targetTrack
        val controller = mediaController ?: return

        radioFetchJob?.cancel()
        seedVideoId = targetTrack.id

        userHistoryRepository?.recordTrackPlayed(targetTrack)
        val updatedRecent = userHistoryRepository?.getRecentlyPlayed()?.take(15)
            ?: (listOf(targetTrack) + _recentlyPlayed.value.filterNot { it.id == targetTrack.id }).take(15)
        _recentlyPlayed.value = updatedRecent

        effectiveTracks.forEach { trackMetadataMap[it.id] = it }

        _loadingTrackId.value = targetTrack.id
        loadLyrics(targetTrack)
        loadCanvasVideo(targetTrack)
        _playbackState.value = _playbackState.value.copy(
            currentTrack = targetTrack,
            queue = effectiveTracks,
            currentQueueIndex = targetIndex,
            isPlaying = false,
            currentPositionMs = 0L,
            durationMs = (targetTrack.durationSeconds * 1000L).coerceAtLeast(0L),
            isBuffering = true,
            hasNext = targetIndex < effectiveTracks.size - 1,
            hasPrevious = targetIndex > 0,
            queueSourceTitle = title,
            queueFilterChips = emptyList(),
            selectedQueueChipParams = null,
            isLoadingQueueRadio = false,
            errorMessage = null
        )

        val mediaItems = effectiveTracks.map { trackToMediaItem(it) }
        controller.setMediaItems(mediaItems, targetIndex, 0L)
        controller.prepare()
        controller.play()
        val nextTrack = effectiveTracks.getOrNull(targetIndex + 1)
        if (nextTrack != null) {
            AudioPlayerService.prefetchTrack(nextTrack.id)
        }
    }

    fun downloadAll(tracks: List<Track>) {
        val libRepo = libraryRepository ?: return
        val toDownload = tracks.filterNot { libRepo.isDownloaded(it.id) }
        if (toDownload.isEmpty()) {
            _userMessage.value = "Todas as músicas já foram baixadas"
            return
        }
        _userMessage.value = "Baixando  músicas..."
        viewModelScope.launch {
            for (track in toDownload) {
                trackMetadataMap[track.id] = track
                val currentProg = _downloadProgressMap.value.toMutableMap()
                currentProg[track.id] = 0.05f
                _downloadProgressMap.value = currentProg

                libRepo.downloadTrack(track) { prog ->
                    val pMap = _downloadProgressMap.value.toMutableMap()
                    pMap[track.id] = prog
                    _downloadProgressMap.value = pMap
                }
                val pMap = _downloadProgressMap.value.toMutableMap()
                pMap.remove(track.id)
                _downloadProgressMap.value = pMap
            }
            _userMessage.value = "Download da playlist concluído!"
        }
    }

    fun addTracksToQueue(tracks: List<Track>) {
        val controller = mediaController ?: return
        if (tracks.isEmpty()) return
        val mediaItems = tracks.map { trackToMediaItem(it) }
        controller.addMediaItems(mediaItems)
        _userMessage.value = "${tracks.size} músicas adicionadas à fila"
    }

    fun addTracksToPlayNext(tracks: List<Track>) {
        val controller = mediaController ?: return
        if (tracks.isEmpty()) return
        val nextIndex = (controller.currentMediaItemIndex + 1).coerceAtLeast(0)
        val mediaItems = tracks.map { trackToMediaItem(it) }
        controller.addMediaItems(nextIndex, mediaItems)
        _userMessage.value = "${tracks.size} músicas adicionadas para tocar a seguir"
    }
        fun clearUserMessage() {
        _userMessage.value = null
    }

    /**
     * Carrega detalhes completos do álbum ou playlist a partir do browseId.
     */
    fun loadAlbum(browseId: String) {
        viewModelScope.launch {
            _isLoadingAlbum.value = true
            _albumError.value = null
            _albumDetail.value = null
            repository.getAlbumOrPlaylist(browseId).onSuccess { album ->
                _albumDetail.value = album
                album.tracks.forEach { trackMetadataMap[it.id] = it }
                _isLoadingAlbum.value = false
            }.onFailure { error ->
                Log.e("MusicViewModel", "Erro ao carregar álbum/playlist $browseId", error)
                _albumError.value = "Não foi possível carregar as faixas deste álbum."
                _isLoadingAlbum.value = false
            }
        }
    }

    /**
     * Enfileira todas as faixas do álbum no player e inicia a reprodução imediatamente no índice solicitado.
     */
    fun playAlbum(album: AlbumDetail, startIndex: Int = 0) {
        if (album.tracks.isEmpty()) return
        val targetIndex = startIndex.coerceIn(0, album.tracks.size - 1)
        val targetTrack = album.tracks[targetIndex]

        _selectedTrack.value = targetTrack
        val controller = mediaController ?: return

        radioFetchJob?.cancel()
        seedVideoId = targetTrack.id

        userHistoryRepository?.recordTrackPlayed(targetTrack)
        val updatedRecent = userHistoryRepository?.getRecentlyPlayed()?.take(15)
            ?: (listOf(targetTrack) + _recentlyPlayed.value.filterNot { it.id == targetTrack.id }).take(15)
        _recentlyPlayed.value = updatedRecent

        album.tracks.forEach { trackMetadataMap[it.id] = it }

        _loadingTrackId.value = targetTrack.id
        loadLyrics(targetTrack)
        loadCanvasVideo(targetTrack)
        _playbackState.value = _playbackState.value.copy(
            currentTrack = targetTrack,
            queue = album.tracks,
            currentQueueIndex = targetIndex,
            isPlaying = false,
            currentPositionMs = 0L,
            durationMs = (targetTrack.durationSeconds * 1000L).coerceAtLeast(0L),
            isBuffering = true,
            hasNext = targetIndex < album.tracks.size - 1,
            hasPrevious = targetIndex > 0,
            queueSourceTitle = album.title,
            queueFilterChips = emptyList(),
            selectedQueueChipParams = null,
            isLoadingQueueRadio = false,
            errorMessage = null
        )

        val mediaItems = album.tracks.map { trackToMediaItem(it) }
        controller.setMediaItems(mediaItems, targetIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun loadSearchHistory() {
        _searchHistory.value = userHistoryRepository?.getSearchHistory() ?: emptyList()
    }

    fun addSearchHistory(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        userHistoryRepository?.addSearchQuery(clean)
        _searchHistory.value = userHistoryRepository?.getSearchHistory() ?: emptyList()
    }

    fun removeSearchHistory(query: String) {
        userHistoryRepository?.removeSearchQuery(query)
        _searchHistory.value = userHistoryRepository?.getSearchHistory() ?: emptyList()
    }

    fun clearSearchHistory() {
        userHistoryRepository?.clearSearchHistory()
        _searchHistory.value = emptyList()
    }

    fun loadTrackRelated(relatedBrowseId: String) {
        relatedFetchJob?.cancel()
        relatedFetchJob = viewModelScope.launch {
            _isLoadingRelated.value = true
            _trackRelatedData.value = null
            repository.getTrackRelated(relatedBrowseId).onSuccess { data ->
                _trackRelatedData.value = data
                data.sections.forEach { section ->
                    section.items.forEach { trackMetadataMap[it.id] = it }
                }
                _isLoadingRelated.value = false
            }.onFailure { error ->
                Log.w("MusicViewModel", "Erro ao buscar conteudo relacionado: " + error.message)
                _isLoadingRelated.value = false
            }
        }
    }

    suspend fun resolveArtistBrowseId(artistName: String): String? {
        if (artistName.isBlank() || artistName.equals("Desconhecido", ignoreCase = true) || artistName.equals("Artista Desconhecido", ignoreCase = true)) {
            return null
        }
        return try {
            val res = repository.search(artistName, SearchFilter.ALL).getOrNull()
            res?.topArtist?.id ?: res?.artists?.firstOrNull()?.id
        } catch (_: Exception) {
            null
        }
    }

    fun loadArtist(browseId: String) {
        viewModelScope.launch {
            _isLoadingArtist.value = true
            _artistError.value = null
            _artistDetail.value = null
            repository.getArtist(browseId).onSuccess { artist ->
                _artistDetail.value = artist
                artist.topSongs.forEach { trackMetadataMap[it.id] = it }
                artist.videos.forEach { trackMetadataMap[it.id] = it }
                _libraryTracksForArtist.value = userHistoryRepository?.getTracksByArtist(artist.name) ?: emptyList()
                _isLoadingArtist.value = false
            }.onFailure { error ->
                Log.e("MusicViewModel", "Erro ao carregar perfil do artista $browseId", error)
                _artistError.value = "Não foi possível carregar as informações do artista."
                _isLoadingArtist.value = false
            }
        }
    }

    fun setCanvasEnabled(enabled: Boolean) {
        _isCanvasEnabled.value = enabled
        userSettingsRepository?.setCanvasEnabled(enabled)
        val track = _playbackState.value.currentTrack
        if (!enabled) {
            _canvasVideoUrl.value = null
        } else if (track != null) {
            loadCanvasVideo(track)
        }
    }

    fun setCanvasWifiOnly(wifiOnly: Boolean) {
        _isCanvasWifiOnly.value = wifiOnly
        userSettingsRepository?.setCanvasWifiOnly(wifiOnly)
        val track = _playbackState.value.currentTrack
        if (track != null) {
            loadCanvasVideo(track)
        }
    }

    fun setLyricsAboveTitleEnabled(enabled: Boolean) {
        _isLyricsAboveTitleEnabled.value = enabled
        userSettingsRepository?.setLyricsAboveTitleEnabled(enabled)
    }

    fun setLibraryGridView(isGrid: Boolean) {
        _isLibraryGridView.value = isGrid
        userSettingsRepository?.setLibraryGridView(isGrid)
    }

    fun setGaplessEnabled(enabled: Boolean) {
        _isGaplessEnabled.value = enabled
        userSettingsRepository?.setGaplessEnabled(enabled)
    }

    fun setAutomixEnabled(enabled: Boolean) {
        _isAutomixEnabled.value = enabled
        userSettingsRepository?.setAutomixEnabled(enabled)
    }

    fun setCrossfadeSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(0, 12)
        _crossfadeSeconds.value = clamped
        userSettingsRepository?.setCrossfadeSeconds(clamped)
    }

    fun setDataSaverMode(mode: DataSaverMode) {
        _dataSaverMode.value = mode
        userSettingsRepository?.setDataSaverMode(mode)
    }

    fun setDownloadOverCellular(enabled: Boolean) {
        _isDownloadOverCellular.value = enabled
        userSettingsRepository?.setDownloadOverCellular(enabled)
    }

    fun setWifiAudioQuality(quality: AudioQuality) {
        _wifiAudioQuality.value = quality
        userSettingsRepository?.setWifiAudioQuality(quality)
    }

    fun setCellularAudioQuality(quality: AudioQuality) {
        _cellularAudioQuality.value = quality
        userSettingsRepository?.setCellularAudioQuality(quality)
    }

    fun setDownloadAudioQuality(quality: DownloadAudioQuality) {
        _downloadAudioQuality.value = quality
        userSettingsRepository?.setDownloadAudioQuality(quality)
    }

    fun setSmartDownloadsEnabled(enabled: Boolean) {
        _isSmartDownloadsEnabled.value = enabled
        userSettingsRepository?.setSmartDownloadsEnabled(enabled)
        applicationContext?.let { ctx ->
            if (enabled) {
                SmartDownloadWorker.schedule(ctx)
                SmartDownloadWorker.triggerImmediate(ctx)
            } else {
                SmartDownloadWorker.cancel(ctx)
            }
        }
    }

    fun setSmartDownloadsLimit(limit: Int) {
        _smartDownloadsLimit.value = limit
        _smartDownloadSyncState.value = _smartDownloadSyncState.value.copy(targetQuota = limit)
        userSettingsRepository?.setSmartDownloadsLimit(limit)
        applicationContext?.let { ctx ->
            SmartDownloadWorker.triggerImmediate(ctx)
        }
    }

    fun setSmartDownloadsWifiOnly(wifiOnly: Boolean) {
        _isSmartDownloadsWifiOnly.value = wifiOnly
        userSettingsRepository?.setSmartDownloadsWifiOnly(wifiOnly)
        applicationContext?.let { ctx ->
            SmartDownloadWorker.schedule(ctx)
        }
    }

    fun triggerSmartDownloadSyncNow() {
        viewModelScope.launch {
            smartDownloadManager?.syncNow(force = true)
        }
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerAtEndOfTrack.value = false
        if (minutes <= 0) {
            _sleepTimerRemainingSeconds.value = null
            return
        }

        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes * 60L
            while (remaining > 0) {
                _sleepTimerRemainingSeconds.value = remaining
                delay(1000L)
                remaining--
            }
            _sleepTimerRemainingSeconds.value = 0L
            mediaController?.pause()
            _userMessage.value = "Temporizador de sono: reprodução finalizada."
            _sleepTimerRemainingSeconds.value = null
        }
    }

    fun setSleepTimerEndOfTrack() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingSeconds.value = null
        _sleepTimerAtEndOfTrack.value = true
        _userMessage.value = "Temporizador: a reprodução será pausada ao final desta música."
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSeconds.value = null
        _sleepTimerAtEndOfTrack.value = false
        _userMessage.value = "Temporizador de sono cancelado."
    }

    fun openEqualizer(context: Context) {
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            _userMessage.value = "Nenhum aplicativo de equalização nativo encontrado no dispositivo."
        }
    }

    fun refreshStorageBreakdown(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val breakdown = StorageUtils.getStorageBreakdown(context)
            _storageBreakdown.value = breakdown
        }
    }

    fun setRadioTuningMode(mode: RadioTuningMode) {
        _currentRadioTuningMode.value = mode
        val current = _playbackState.value.currentTrack
        if (current != null) {
            refreshRadioWithMode(current, mode)
        }
    }

    private fun refreshRadioWithMode(seedTrack: Track, mode: RadioTuningMode) {
        radioFetchJob?.cancel()
        radioFetchJob = viewModelScope.launch {
            _playbackState.value = _playbackState.value.copy(isLoadingQueueRadio = true)
            val result = repository.getUpNextRadio(videoId = seedTrack.id, params = null)
            result.onSuccess { upNext ->
                upNext.tracks.forEach { trackMetadataMap[it.id] = it }
                val affinityRepo = userAffinityRepository ?: applicationContext?.let { UserAffinityRepositoryImpl(it) }
                val smartQueue = if (affinityRepo != null) {
                    SmartRadioEngine.buildSmartQueue(
                        seedTrack = seedTrack,
                        rawCandidates = upNext.tracks,
                        userLikedTracks = _likedTracks.value,
                        userRecentTracks = _recentlyPlayed.value,
                        affinityRepo = affinityRepo,
                        tuningMode = mode
                    )
                } else {
                    listOf(seedTrack) + upNext.tracks
                }

                val controller = mediaController
                if (controller != null && controller.mediaItemCount > 0) {
                    val currentIdx = controller.currentMediaItemIndex.coerceAtLeast(0)
                    if (controller.mediaItemCount > currentIdx + 1) {
                        controller.removeMediaItems(currentIdx + 1, controller.mediaItemCount)
                    }
                    val upcoming = smartQueue.drop(1)
                    if (upcoming.isNotEmpty()) {
                        controller.addMediaItems(upcoming.map { trackToMediaItem(it) })
                    }
                    syncTimelineState(controller)
                }
                _playbackState.value = _playbackState.value.copy(
                    queue = smartQueue,
                    isLoadingQueueRadio = false
                )
            }.onFailure {
                _playbackState.value = _playbackState.value.copy(isLoadingQueueRadio = false)
            }
        }
    }

    private fun checkAndLoadMoreQueue(player: Player) {
        if (isLoadingMoreQueue) return
        val currentIdx = player.currentMediaItemIndex
        val totalItems = player.mediaItemCount
        if (totalItems > 0 && currentIdx >= totalItems - 4) {
            val lastMediaId = player.getMediaItemAt(totalItems - 1).mediaId
            if (lastMediaId.isNotBlank()) {
                isLoadingMoreQueue = true
                viewModelScope.launch {
                    try {
                        val result = repository.getUpNextRadio(videoId = lastMediaId, params = null)
                        result.onSuccess { moreUpNext ->
                            val existingIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }.toSet()
                            val affinityRepo = userAffinityRepository ?: applicationContext?.let { UserAffinityRepositoryImpl(it) }
                            val newTracks = moreUpNext.tracks.filter {
                                !existingIds.contains(it.id) && affinityRepo?.isTrackPenalized(it.id) != true
                            }
                            if (newTracks.isNotEmpty()) {
                                newTracks.forEach { trackMetadataMap[it.id] = it }
                                player.addMediaItems(newTracks.map { trackToMediaItem(it) })
                                syncTimelineState(player)
                                Log.i("MusicViewModel", "Continuation da rádio carregada: +${newTracks.size} faixas.")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("MusicViewModel", "Falha ao carregar continuação da rádio: ${e.message}")
                    } finally {
                        isLoadingMoreQueue = false
                    }
                }
            }
        }
    }

    fun clearAppCache(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = StorageUtils.clearCache(context)
            val breakdown = StorageUtils.getStorageBreakdown(context)
            _storageBreakdown.value = breakdown
            if (success) {
                _userMessage.value = "Cache temporário limpo com sucesso!"
            }
        }
    }

    fun showChangeLogManually() {
        _postUpdateChangelog.value = getDefaultChangeLog()
    }

    fun checkForAppUpdates(manual: Boolean) {
        viewModelScope.launch {
            val update = updateRepository.checkForUpdate()
            if (update != null) {
                _availableUpdate.value = update
            } else if (manual) {
                _userMessage.value = "Você já está na versão mais recente (v${com.nerf.music.BuildConfig.VERSION_NAME})"
            }
        }
    }

    private var lastCanvasTrackId: String? = null

    fun loadCanvasVideo(track: Track) {
        if (lastCanvasTrackId == track.id && _canvasVideoUrl.value != null) {
            return
        }
        lastCanvasTrackId = track.id
        canvasFetchJob?.cancel()

        val cachedUrl = spotifyCanvasRepository.getCachedCanvasUrl(track.title, track.artist)
        if (cachedUrl != null) {
            _canvasVideoUrl.value = cachedUrl
        } else {
            _canvasVideoUrl.value = null
        }

        val settings = userSettingsRepository
        val isEnabled = settings?.isCanvasEnabled?.value ?: _isCanvasEnabled.value
        if (!isEnabled) {
            _isLoadingCanvasVideo.value = false
            return
        }

        val isWifiOnly = settings?.isCanvasWifiOnly?.value ?: _isCanvasWifiOnly.value
        val context = applicationContext
        if (isWifiOnly && context != null && !NetworkUtils.isConnectedToWifi(context)) {
            Log.i("MusicViewModel", "Canvas ignorado: Wi-Fi Only ativado e não conectado ao Wi-Fi.")
            _isLoadingCanvasVideo.value = false
            return
        }

        canvasFetchJob = viewModelScope.launch {
            _isLoadingCanvasVideo.value = true
            try {
                val res = spotifyCanvasRepository.getCanvasUrl(track.title, track.artist)
                val canvasUrl = res.getOrNull()
                _canvasVideoUrl.value = canvasUrl
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Erro ao carregar Canvas para ${track.title}: ${e.message}", e)
                _canvasVideoUrl.value = null
            } finally {
                _isLoadingCanvasVideo.value = false
            }
            prefetchNextCanvas()
        }
    }

    private fun prefetchNextCanvas() {
        val settings = userSettingsRepository
        val isEnabled = settings?.isCanvasEnabled?.value ?: _isCanvasEnabled.value
        if (!isEnabled) return

        val isWifiOnly = settings?.isCanvasWifiOnly?.value ?: _isCanvasWifiOnly.value
        val context = applicationContext
        if (isWifiOnly && context != null && !NetworkUtils.isConnectedToWifi(context)) return

        val currentIdx = _playbackState.value.currentQueueIndex
        val nextTrack = _playbackState.value.queue.getOrNull(currentIdx + 1) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                spotifyCanvasRepository.getCanvasUrl(nextTrack.title, nextTrack.artist)
            } catch (_: Exception) {}
        }
    }

    fun loadLyrics(track: Track) {
        lyricsFetchJob?.cancel()
        _currentLyrics.value = null
        _currentLyricsLine.value = null

        lyricsFetchJob = viewModelScope.launch {
            val res = lyricsRepository.getLyrics(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                durationSeconds = track.durationSeconds.toInt()
            )
            val data = res.getOrNull()
            _currentLyrics.value = data
            updateCurrentLyricsLine(_playbackState.value.currentPositionMs)
        }
    }

    private fun updateCurrentLyricsLine(positionMs: Long) {
        val lyrics = _currentLyrics.value ?: return
        if (!lyrics.isSynced || lyrics.syncedLines.isEmpty()) {
            _currentLyricsLine.value = null
            return
        }
        val lines = lyrics.syncedLines
        val activeLine = lines.lastOrNull { positionMs >= it.timeMs }
        _currentLyricsLine.value = activeLine?.text
    }

    override fun onCleared() {
        stopProgressTracker()
        radioFetchJob?.cancel()
        canvasFetchJob?.cancel()
        lyricsFetchJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
