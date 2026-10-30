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
import com.nerf.music.data.model.Artist
import com.nerf.music.data.model.ArtistDetail
import com.nerf.music.data.model.HomeChip
import com.nerf.music.data.model.HomeSection
import com.nerf.music.data.model.HomeSectionStyle
import com.nerf.music.data.model.PlaybackState
import com.nerf.music.data.model.QueueFilterChip
import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.SearchResult
import com.nerf.music.data.model.Track
import com.nerf.music.data.model.ExploreRecommendations
import com.nerf.music.data.model.RecapSummary
import com.nerf.music.data.model.RecapTrackStats
import com.nerf.music.data.model.RecapArtistStats
import com.nerf.music.data.db.AppDatabase
import com.nerf.music.data.repository.MusicRepository
import com.nerf.music.data.repository.MusicRepositoryImpl
import com.nerf.music.data.repository.UserHistoryRepository
import com.nerf.music.data.repository.UserHistoryRepositoryImpl
import com.nerf.music.data.repository.BackupRepository
import com.nerf.music.player.AudioPlayerService
import com.nerf.music.data.repository.UpdateRepository
import com.nerf.music.data.repository.UpdateRepositoryImpl
import com.nerf.music.updater.AppUpdateInfo
import com.nerf.music.updater.UpdateNotificationHelper
import com.nerf.music.data.model.TrackRelatedData
import com.nerf.music.data.model.RelatedSection
import com.nerf.music.data.model.LyricsData
import com.nerf.music.data.model.SyncedLyricsLine
import com.nerf.music.data.repository.LyricsRepository
import com.nerf.music.data.repository.LyricsRepositoryImpl
import com.nerf.music.data.repository.UserSettingsRepository
import com.nerf.music.data.repository.UserSettingsRepositoryImpl
import com.nerf.music.data.repository.DataSaverMode
import com.nerf.music.data.repository.AudioQuality
import com.nerf.music.data.repository.LoudnessStandard
import com.nerf.music.data.repository.UserAffinityRepository
import com.nerf.music.data.repository.UserAffinityRepositoryImpl
import com.nerf.music.player.SmartRadioEngine
import com.nerf.music.player.RadioTuningMode
import com.nerf.music.player.NewPipeStreamResolver
import com.nerf.music.player.KaraokeLyricsEngine
import com.nerf.music.player.KaraokeSyncSnapshot
import com.nerf.music.jam.JamCommandType
import com.nerf.music.jam.JamPlaybackSnapshot
import com.nerf.music.jam.JamSessionManager
import com.nerf.music.jam.JamSessionState
import com.nerf.music.jam.JamWireMessage
import com.nerf.music.util.NetworkUtils
import com.nerf.music.util.ConnectivityObserver
import com.nerf.music.util.NetworkConnectivityObserver
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import com.nerf.music.util.StorageBreakdown
import com.nerf.music.util.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.nerf.music.data.repository.SpotifyCanvasRepository
import com.nerf.music.data.repository.SpotifyCanvasRepositoryImpl
import com.nerf.music.data.repository.LibraryRepository
import com.nerf.music.data.repository.LibraryRepositoryImpl
import com.nerf.music.data.repository.DownloadAudioQuality
import com.nerf.music.data.repository.SmartDownloadManager
import com.nerf.music.data.repository.MigrationManager
import com.nerf.music.data.repository.SmartDownloadSyncState
import com.nerf.music.work.SmartDownloadWorker
import com.nerf.music.data.model.UserPlaylist
import com.nerf.music.data.model.DownloadedTrack
import com.nerf.music.data.repository.HomeFeedSynthesizer
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class MigrationProgressState(
    val isImporting: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val currentTrackTitle: String = "",
    val message: String = ""
)

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

    private val _loudnessStandard = MutableStateFlow(LoudnessStandard.SPOTIFY)
    val loudnessStandard: StateFlow<LoudnessStandard> = _loudnessStandard.asStateFlow()

    private val _dataSaverMode = MutableStateFlow(DataSaverMode.ALWAYS_OFF)
    val dataSaverMode: StateFlow<DataSaverMode> = _dataSaverMode.asStateFlow()

    private val _isDownloadOverCellular = MutableStateFlow(false)
    val isDownloadOverCellular: StateFlow<Boolean> = _isDownloadOverCellular.asStateFlow()

    private val _wifiAudioQuality = MutableStateFlow(AudioQuality.HIGH)
    val wifiAudioQuality: StateFlow<AudioQuality> = _wifiAudioQuality.asStateFlow()

    private val _cellularAudioQuality = MutableStateFlow(AudioQuality.AUTOMATIC)
    val cellularAudioQuality: StateFlow<AudioQuality> = _cellularAudioQuality.asStateFlow()
    private var smartDownloadManager: SmartDownloadManager? = null
    private var migrationManager: MigrationManager? = null

    private val _isYouTubeMusicConnected = MutableStateFlow(false)
    val isYouTubeMusicConnected: StateFlow<Boolean> = _isYouTubeMusicConnected.asStateFlow()

    private val _isSpotifyConnected = MutableStateFlow(false)
    val isSpotifyConnected: StateFlow<Boolean> = _isSpotifyConnected.asStateFlow()

    private val _migrationProgress = MutableStateFlow<MigrationProgressState?>(null)
    val migrationProgress: StateFlow<MigrationProgressState?> = _migrationProgress.asStateFlow()

    private val _downloadAudioQuality = MutableStateFlow(DownloadAudioQuality.LOSSLESS)
    val downloadAudioQuality: StateFlow<DownloadAudioQuality> = _downloadAudioQuality.asStateFlow()

    private val _isDownloadCanvasEnabled = MutableStateFlow(false)
    val isDownloadCanvasEnabled: StateFlow<Boolean> = _isDownloadCanvasEnabled.asStateFlow()

    private val _isDownloadLyricsEnabled = MutableStateFlow(true)
    val isDownloadLyricsEnabled: StateFlow<Boolean> = _isDownloadLyricsEnabled.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private var connectivityObserver: ConnectivityObserver? = null
    private val _connectivityStatus = MutableStateFlow(ConnectivityObserver.Status.Available)
    val connectivityStatus: StateFlow<ConnectivityObserver.Status> = _connectivityStatus.asStateFlow()
    private var connectivityObserverJob: Job? = null

    private val _isSmartDownloadsEnabled = MutableStateFlow(false)
    val isSmartDownloadsEnabled: StateFlow<Boolean> = _isSmartDownloadsEnabled.asStateFlow()

    private val _smartDownloadsLimit = MutableStateFlow(100)
    val smartDownloadsLimit: StateFlow<Int> = _smartDownloadsLimit.asStateFlow()

    private val _isSmartDownloadsWifiOnly = MutableStateFlow(true)
    val isSmartDownloadsWifiOnly: StateFlow<Boolean> = _isSmartDownloadsWifiOnly.asStateFlow()

    private val _smartDownloadSyncState = MutableStateFlow(SmartDownloadSyncState())
    val smartDownloadSyncState: StateFlow<SmartDownloadSyncState> = _smartDownloadSyncState.asStateFlow()

    val sleepTimerRemainingSeconds: StateFlow<Long?> = AudioPlayerService.sleepTimerRemainingSeconds
    val sleepTimerAtEndOfTrack: StateFlow<Boolean> = AudioPlayerService.sleepTimerAtEndOfTrack

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

    // =========================================================================
    // KARAOKÊ (LRC Real-Time Sync + Vocal Isolation DSP + Translation & Pronunciation)
    // =========================================================================
    private val _karaokeSyncSnapshot = MutableStateFlow(KaraokeSyncSnapshot())
    val karaokeSyncSnapshot: StateFlow<KaraokeSyncSnapshot> = _karaokeSyncSnapshot.asStateFlow()

    private val _isKaraokeVocalIsolationEnabled = MutableStateFlow(AudioPlayerService.isKaraokeEnabled)
    val isKaraokeVocalIsolationEnabled: StateFlow<Boolean> = _isKaraokeVocalIsolationEnabled.asStateFlow()

    private val _karaokeVocalLevel = MutableStateFlow(AudioPlayerService.karaokeVocalLevel)
    val karaokeVocalLevel: StateFlow<Float> = _karaokeVocalLevel.asStateFlow()

    private val _lyricsOffsetMs = MutableStateFlow(0L)
    val lyricsOffsetMs: StateFlow<Long> = _lyricsOffsetMs.asStateFlow()

    private val _showLyricsTranslation = MutableStateFlow(false)
    val showLyricsTranslation: StateFlow<Boolean> = _showLyricsTranslation.asStateFlow()

    private val _showLyricsPronunciation = MutableStateFlow(false)
    val showLyricsPronunciation: StateFlow<Boolean> = _showLyricsPronunciation.asStateFlow()

    private val _hideOriginalLyrics = MutableStateFlow(false)
    val hideOriginalLyrics: StateFlow<Boolean> = _hideOriginalLyrics.asStateFlow()

    private val _isTranslatingLyrics = MutableStateFlow(false)
    val isTranslatingLyrics: StateFlow<Boolean> = _isTranslatingLyrics.asStateFlow()

    private val _translationTargetLanguage = MutableStateFlow("auto")
    val translationTargetLanguage: StateFlow<String> = _translationTargetLanguage.asStateFlow()

    private val _isInstrumentalTrackReady = MutableStateFlow(false)
    val isInstrumentalTrackReady: StateFlow<Boolean> = _isInstrumentalTrackReady.asStateFlow()

    private val _isUsingRealInstrumental = MutableStateFlow(AudioPlayerService.isUsingRealInstrumental)
    val isUsingRealInstrumental: StateFlow<Boolean> = _isUsingRealInstrumental.asStateFlow()

    private var lyricsTranslationJob: Job? = null
    private var instrumentalFetchJob: Job? = null

    // =========================================================================
    // JAM (Escuta Sincronizada em Grupo via WebSockets + DeepLinks, No-Account)
    // =========================================================================
    private var isApplyingRemoteJamEvent: Boolean = false
    private var pendingDeepLinkUri: Uri? = null

    private val _isJamSheetVisible = MutableStateFlow(false)
    val isJamSheetVisible: StateFlow<Boolean> = _isJamSheetVisible.asStateFlow()

    private val jamSessionManager = JamSessionManager(
        scope = viewModelScope,
        providePlaybackSnapshot = {
            val st = _playbackState.value
            val pos = mediaController?.currentPosition?.coerceAtLeast(0L) ?: st.currentPositionMs
            val playing = mediaController?.isPlaying ?: st.isPlaying
            val repeatStr = when (mediaController?.repeatMode ?: st.repeatMode) {
                Player.REPEAT_MODE_ALL -> "ALL"
                Player.REPEAT_MODE_ONE -> "ONE"
                else -> "OFF"
            }
            JamPlaybackSnapshot(
                currentTrack = st.currentTrack,
                queue = st.queue,
                currentQueueIndex = st.currentQueueIndex.coerceAtLeast(0),
                isPlaying = playing,
                positionMs = pos,
                repeatModeName = repeatStr,
                hostCrossfadeSeconds = _crossfadeSeconds.value,
                hostAutomixEnabled = _isAutomixEnabled.value,
                hostGaplessEnabled = _isGaplessEnabled.value,
                isKaraokeActive = _isKaraokeVocalIsolationEnabled.value,
                karaokeVocalLevel = _karaokeVocalLevel.value
            )
        },
        onApplyRemoteSnapshot = { snapshot, compensatedPosMs ->
            applyRemoteJamSnapshot(snapshot, compensatedPosMs)
        },
        onExecuteGuestCommandOnHost = { cmdMsg ->
            executeGuestJamCommandOnHost(cmdMsg)
        },
        onUserFeedback = { msg ->
            _userMessage.value = msg
        }
    )
    val jamSessionState: StateFlow<JamSessionState> = jamSessionManager.sessionState

    private var canvasFetchJob: Job? = null
    private var homeCanvasPrefetchJob: Job? = null
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
    private val _exploreSections = MutableStateFlow<List<HomeSection>>(emptyList())
    val exploreSections: StateFlow<List<HomeSection>> = _exploreSections.asStateFlow()
    private val _exploreLoading = MutableStateFlow(false)
    val exploreLoading: StateFlow<Boolean> = _exploreLoading.asStateFlow()
    private val _moodCategories = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val moodCategories = _moodCategories.asStateFlow()
    private val _moodError = MutableStateFlow<String?>(null)
    val moodError = _moodError.asStateFlow()
    private val _moodSections = MutableStateFlow<List<HomeSection>>(emptyList())
    val moodSections = _moodSections.asStateFlow()
    private val _moodLoading = MutableStateFlow(false)
    val moodLoading = _moodLoading.asStateFlow()
    private val _chartsSections = MutableStateFlow<List<HomeSection>>(emptyList())
    val chartsSections = _chartsSections.asStateFlow()
    fun loadCharts() {
        if (_chartsSections.value.isNotEmpty()) return
        viewModelScope.launch { repository.getChartsFeed().onSuccess { _chartsSections.value = it } }
    }

    fun loadMoodCategories() {
        if (_moodLoading.value) return
        viewModelScope.launch {
            _moodLoading.value = true
            _moodError.value = null
            repository.getMoodCategories()
                .onSuccess { _moodCategories.value = it; if (it.isEmpty()) _moodError.value = "Nenhum gênero foi retornado pelo catálogo." }
                .onFailure { _moodError.value = "Falha ao carregar gêneros. Toque para tentar novamente." }
            _moodLoading.value = false
        }
    }

    fun loadMoodFeed(id: String) {
        viewModelScope.launch {
            _moodSections.value = emptyList()
            _moodLoading.value = true
            repository.getMoodFeed(id).onSuccess { _moodSections.value = it }
            _moodLoading.value = false
        }
    }

    fun loadExplore(force: Boolean = false) {
        if (_exploreLoading.value || (!force && _exploreSections.value.isNotEmpty())) return
        viewModelScope.launch {
            _exploreLoading.value = true
            try {
                val cachedExplore: List<HomeSection> = _exploreSections.value.ifEmpty {
                    userHistoryRepository?.getCachedExploreFeed().orEmpty()
                }
                if (_exploreSections.value.isEmpty() && cachedExplore.isNotEmpty()) {
                    _exploreSections.value = cachedExplore
                }
                val penalizedArtists = userAffinityRepository?.getPenalizedArtists().orEmpty()
                val rawArtists = (userAffinityRepository?.getTopAffinitiveArtists(15).orEmpty() +
                    userHistoryRepository?.getTopArtists(15).orEmpty()).distinct()
                val artists = com.nerf.music.data.model.ReleaseRecommendations.cleanPreferredArtists(
                    rawArtists,
                    penalizedArtists
                )
                val likedIds = _likedTracks.value.map { it.id }.toSet()
                val recentlyPlayedIds = userHistoryRepository?.getRecentlyPlayed()
                    .orEmpty()
                    .map { it.id }
                    .toSet()
                val seedIds = buildList {
                    userHistoryRepository?.getFavoriteSeedVideoId()?.let { add(it) }
                    _likedTracks.value.firstOrNull { !it.isAlbumOrPlaylist && it.id.isNotBlank() }?.id?.let { add(it) }
                    userHistoryRepository?.getRecentlyPlayed()
                        ?.firstOrNull { !it.isAlbumOrPlaylist && it.id.isNotBlank() && userAffinityRepository?.isTrackPenalized(it.id) != true }
                        ?.id?.let { add(it) }
                }.distinct()

                coroutineScope {
                    // O feed-base deve aparecer assim que chegar. O enriquecimento mais caro
                    // (lançamentos personalizados e validação de datas) continua em paralelo.
                    val exploreDeferred = async { repository.getExploreFeed() }
                    val newReleasesDeferred = async { repository.getNewReleases().getOrNull().orEmpty() }
                    val radiosDeferred = async {
                        coroutineScope {
                            seedIds.take(2).map { seedId ->
                                async { repository.getUpNextRadio(seedId).getOrNull()?.tracks.orEmpty() }
                            }.map { it.await() }
                        }
                    }
                    val personalizedReleasesDeferred = async {
                        if (artists.isNotEmpty() || seedIds.isNotEmpty()) {
                            val radioTracks = radiosDeferred.await().flatten()
                            repository.discoverPersonalizedRecentReleases(
                                preferredArtists = artists,
                                seedVideoIds = seedIds,
                                penalizedArtists = penalizedArtists,
                                seedRadioTracks = radioTracks
                            )
                        } else {
                            emptyList()
                        }
                    }

                    val sections = exploreDeferred.await().getOrNull() ?: return@coroutineScope
                    val musicSections = sections.filterNot { section ->
                        section.title.contains("vídeo", ignoreCase = true) || section.title.contains("video", ignoreCase = true)
                    }

                    // Publicação progressiva: o usuário já pode navegar enquanto as recomendações
                    // personalizadas terminam de carregar.
                    val visibleBaseSections = musicSections.withCachedExplorePersonalization(cachedExplore)
                    _exploreSections.value = visibleBaseSections

                    val radioGroups = radiosDeferred.await()
                    val discoveryTracks = ExploreRecommendations.selectDiscoveryTracks(
                        radioGroups = radioGroups,
                        excludedTrackIds = likedIds + recentlyPlayedIds + seedIds,
                        penalizedArtists = penalizedArtists,
                        isTrackPenalized = { id -> userAffinityRepository?.isTrackPenalized(id) == true }
                    )
                    val discoverySection = discoveryTracks.takeIf { it.isNotEmpty() }?.let {
                        HomeSection(
                            title = "Descobertas para você",
                            strapline = "FAIXAS NOVAS A PARTIR DO QUE VOCÊ OUVE",
                            items = it,
                            style = HomeSectionStyle.QUICK_PICKS_GRID
                        )
                    }
                    _exploreSections.value = visibleBaseSections.withDiscoverySection(discoverySection)

                    val personalizedReleases = personalizedReleasesDeferred.await()
                    val newReleases = newReleasesDeferred.await()
                    val radioArtists = radioGroups.flatten().map { it.artist }.filter { it.isNotBlank() }
                    val discoveredClusterArtists = personalizedReleases
                        .filter { it.sourceIdentifier == "INNERTUBE_CLUSTER_RELEASE" }
                        .flatMap { com.nerf.music.data.model.ReleaseRecommendations.extractContributors(it.artist) }
                    val relatedArtists = com.nerf.music.data.model.ReleaseRecommendations.cleanPreferredArtists(
                        (radioArtists + discoveredClusterArtists).distinct(),
                        penalizedArtists
                    )

                    val tailored = musicSections.map { section ->
                        if (section.title.contains("lançamento", true) || section.title.contains("álbu", true)) {
                            val candidates = (personalizedReleases + newReleases + section.items).distinctBy { it.browseId ?: it.id }
                            val selected = com.nerf.music.data.model.ReleaseRecommendations.rank(
                                releases = repository.verifyReleaseDates(candidates, artists),
                                preferredArtists = artists,
                                relatedArtists = relatedArtists,
                                penalizedArtists = penalizedArtists
                            )
                            Log.d(
                                "ExploreReleases",
                                "candidates=${candidates.size}, personalized=${personalizedReleases.size}, preferredArtists=${artists.size}, relatedArtists=${relatedArtists.size}, selected=${selected.size}, items=${selected.take(8).joinToString(" | ") { "${it.title} (${it.artist})" }}"
                            )
                            section.copy(
                                items = selected,
                                strapline = if (selected.isEmpty()) "Nenhum lançamento recente compatível encontrado agora." else if (artists.isEmpty() && seedIds.isEmpty()) "Lançamentos gerais" else "Lançamentos selecionados pelo que você ouve",
                                style = HomeSectionStyle.TWO_ROW_CAROUSEL
                            )
                        } else section
                    }
                    val finalSections = tailored.withDiscoverySection(discoverySection)
                    _exploreSections.value = finalSections
                    userHistoryRepository?.saveCachedExploreFeed(finalSections)
                }
            } finally {
                _exploreLoading.value = false
            }
        }
    }

    private fun List<HomeSection>.withDiscoverySection(discovery: HomeSection?): List<HomeSection> {
        val withoutOldDiscovery = filterNot { it.title.equals("Descobertas para você", ignoreCase = true) }
        return if (discovery == null) withoutOldDiscovery else withoutOldDiscovery + discovery
    }

    private fun List<HomeSection>.withCachedExplorePersonalization(cached: List<HomeSection>): List<HomeSection> {
        val cachedRelease = cached.firstOrNull { it.isReleaseSection() }
        val cachedDiscovery = cached.firstOrNull {
            it.title.equals("Descobertas para você", ignoreCase = true)
        }
        var releaseInserted = false
        return buildList {
            this@withCachedExplorePersonalization.forEach { section ->
                when {
                    section.isReleaseSection() -> {
                        if (!releaseInserted && cachedRelease != null) add(cachedRelease)
                        releaseInserted = true
                    }
                    section.title.equals("Descobertas para você", ignoreCase = true) -> Unit
                    else -> add(section)
                }
            }
            cachedDiscovery?.let(::add)
        }
    }

    private fun HomeSection.isReleaseSection(): Boolean =
        title.contains("lançamento", ignoreCase = true) ||
            title.contains("álbu", ignoreCase = true) ||
            title.contains("release", ignoreCase = true)

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

    private val _mostPlayedTracks = MutableStateFlow<List<Track>>(emptyList())
    val mostPlayedTracks: StateFlow<List<Track>> = _mostPlayedTracks.asStateFlow()
    private val _recentlyAddedTracks = MutableStateFlow<List<Track>>(emptyList())
    val recentlyAddedTracks: StateFlow<List<Track>> = _recentlyAddedTracks.asStateFlow()
    private val _forgottenFavoriteTracks = MutableStateFlow<List<Track>>(emptyList())
    val forgottenFavoriteTracks: StateFlow<List<Track>> = _forgottenFavoriteTracks.asStateFlow()
    private val _recapSummary = MutableStateFlow(RecapSummary())
    val recapSummary: StateFlow<RecapSummary> = _recapSummary.asStateFlow()
    private val recapArtistImageCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private var libraryAnalyticsInitialized = false

    private val _downloadProgressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Float>> = _downloadProgressMap.asStateFlow()
    private var applicationContext: Context? = null

    fun initializePlayerController(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
        }
        AudioPlayerService.setPrefetchContext(context.applicationContext)
        if (!libraryAnalyticsInitialized) {
            libraryAnalyticsInitialized = true
            val analyticsDao = AppDatabase.getInstance(context.applicationContext).libraryTrackDao()
            viewModelScope.launch {
                analyticsDao.allTracksFlow().collect { rows ->
                    val recapYear = java.time.Year.now().value
                    val played = rows.filter { it.statsYear == recapYear && it.playCount > 0 }
                    val rankedTracks = played
                        .sortedWith(
                            compareByDescending<com.nerf.music.data.db.LibraryTrackEntity> { it.playCount }
                                .thenByDescending { it.totalListenMs }
                                .thenByDescending { it.lastPlayedAtEpochMs ?: 0L }
                        )
                    val parsedCredits = played.associateWith { splitRecapArtistCredit(it.artist) }
                    // Featured appearances resolve to an artist profile when that artist is a primary credit elsewhere.
                    val profileByArtistName = parsedCredits.flatMap { (row, credit) ->
                        val browseId = row.artistBrowseId?.trim()?.takeIf(String::isNotBlank)
                        if (browseId == null) emptyList() else credit.primary.map { normalizeRecapArtistName(it) to browseId }
                    }.groupBy({ it.first }, { it.second })
                        .mapValues { (_, ids) -> ids.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key }
                    val artistContributions = mutableMapOf<String, MutableList<RecapArtistContribution>>()
                    parsedCredits.forEach { (row, credit) ->
                        val allNames = (credit.primary + credit.featured).distinctBy(::normalizeRecapArtistName)
                        if (allNames.isEmpty()) return@forEach
                        val totalWeight = credit.primary.size * 2.0 + credit.featured.size
                        allNames.forEach { name ->
                            val normalizedName = normalizeRecapArtistName(name)
                            val isPrimary = credit.primary.any { normalizeRecapArtistName(it) == normalizedName }
                            val weight = (if (isPrimary) 2.0 else 1.0) / totalWeight
                            val browseId = profileByArtistName[normalizedName]
                                ?: if (isPrimary) row.artistBrowseId?.trim()?.takeIf(String::isNotBlank) else null
                            val key = browseId?.let { "browse:${it.lowercase(Locale.ROOT)}" } ?: "name:$normalizedName"
                            artistContributions.getOrPut(key) { mutableListOf() }.add(RecapArtistContribution(name, row, weight))
                        }
                    }
                    val rankedArtists = artistContributions.values.map { contributions ->
                        val listenMs = contributions.sumOf { contribution ->
                            contribution.track.totalListenMs * contribution.weight
                        }
                        val weightedPlays = contributions.sumOf { it.track.playCount * it.weight }
                        val name = contributions.groupBy { it.name }
                            .maxByOrNull { (_, items) -> items.sumOf { it.weight } }!!.key
                        val representative = contributions.maxByOrNull {
                            it.track.totalListenMs * it.weight
                        }!!.track
                        RecapArtistStats(
                            name = name,
                            plays = weightedPlays.toInt(),
                            listenedMs = listenMs.toLong(),
                            representativeTrack = representative.toTrack()
                        )
                    }.sortedWith(compareByDescending<RecapArtistStats> { it.listenedMs }.thenByDescending { it.plays })
                    val trackStats = rankedTracks.map {
                        RecapTrackStats(it.toTrack(), it.playCount, it.totalListenMs)
                    }
                    _recapSummary.value = RecapSummary(
                        year = recapYear,
                        totalMinutes = played.sumOf { it.totalListenMs } / 60_000L,
                        totalPlays = played.sumOf { it.playCount },
                        topTracks = trackStats.take(5),
                        // Keep the full artist set so Recap can calculate the listening-share remainder.
                        topArtists = rankedArtists,
                        mixTracks = trackStats.take(50).map { it.track }
                    )
                }
            }
            viewModelScope.launch {
                analyticsDao.mostPlayedFlow(minimumPlays = 3).collect { rows ->
                    _mostPlayedTracks.value = rows.map { it.toTrack() }
                }
            }
            viewModelScope.launch {
                analyticsDao.recentlyAddedFlow().collect { rows ->
                    _recentlyAddedTracks.value = rows.map { it.toTrack() }
                }
            }
            viewModelScope.launch {
                val cutoff = System.currentTimeMillis() - 45L * 24L * 60L * 60L * 1000L
                analyticsDao.forgottenFavoritesFlow(cutoff).collect { rows ->
                    _forgottenFavoriteTracks.value = rows.map { it.toTrack() }
                    if (_homeSections.value.isNotEmpty()) {
                        _homeSections.value = curateHomeSections(_homeSections.value)
                    }
                }
            }
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
            _loudnessStandard.value = settingsRepo.loudnessStandard.value
            _dataSaverMode.value = settingsRepo.dataSaverMode.value
            _isDownloadOverCellular.value = settingsRepo.isDownloadOverCellular.value
            _wifiAudioQuality.value = settingsRepo.wifiAudioQuality.value
            _cellularAudioQuality.value = settingsRepo.cellularAudioQuality.value
            _downloadAudioQuality.value = settingsRepo.downloadAudioQuality.value
            _isDownloadCanvasEnabled.value = settingsRepo.isDownloadCanvasEnabled.value
            _isDownloadLyricsEnabled.value = settingsRepo.isDownloadLyricsEnabled.value
            _isSmartDownloadsEnabled.value = settingsRepo.isSmartDownloadsEnabled.value
            _smartDownloadsLimit.value = settingsRepo.smartDownloadsLimit.value
            _isSmartDownloadsWifiOnly.value = settingsRepo.isSmartDownloadsWifiOnly.value
            _isOnboardingCompleted.value = settingsRepo.isOnboardingCompleted.value
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
                _homeSections.value = curateHomeSections(cachedHome)
                prefetchHomeMedia(cachedHome)
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
            viewModelScope.launch(Dispatchers.IO) {
                libRepo.syncDownloadsWithDisk()
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

        if (migrationManager == null && activeLibRepo != null && activeHistoryRepo != null) {
            val migManager = MigrationManager(
                context = context.applicationContext,
                libraryRepository = activeLibRepo,
                userHistoryRepository = activeHistoryRepo,
                userAffinityRepository = activeAffinityRepo
            )
            migrationManager = migManager
            _isYouTubeMusicConnected.value = migManager.isYouTubeMusicConnected()
            _isSpotifyConnected.value = migManager.isSpotifyConnected()
        }

        if (connectivityObserver == null) {
            val observer = NetworkConnectivityObserver(context.applicationContext)
            connectivityObserver = observer
            var prevStatus: ConnectivityObserver.Status? = null
            connectivityObserverJob?.cancel()
            connectivityObserverJob = viewModelScope.launch {
                observer.observe().collect { status ->
                    _connectivityStatus.value = status
                    if (prevStatus != null &&
                        prevStatus != ConnectivityObserver.Status.Available &&
                        status == ConnectivityObserver.Status.Available
                    ) {
                        Log.i("MusicViewModel", "Conectividade restabelecida ($prevStatus -> $status). Recarregando Home automaticamente...")
                        loadHomeFeed(force = true)
                    }
                    prevStatus = status
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
                pendingDeepLinkUri?.let { pendingUri ->
                    pendingDeepLinkUri = null
                    handleDeepLink(pendingUri)
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Erro ao obter MediaController conectado", e)
            }
        }, MoreExecutors.directExecutor())
    }

    fun completeOnboarding(selectedArtists: List<Artist>) {
        val names = selectedArtists.map { it.name.trim() }.filter { it.isNotBlank() }
        userAffinityRepository?.recordInitialArtistPreferences(names)
        userHistoryRepository?.recordInitialArtists(names)
        userSettingsRepository?.setOnboardingCompleted(true)
        _isOnboardingCompleted.value = true
        loadHomeFeed(force = true)
    }

    suspend fun searchArtistsForOnboarding(query: String): List<Artist> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        return try {
            val result = repository.search(q, SearchFilter.ALL).getOrNull()
            val list = mutableListOf<Artist>()
            result?.topArtist?.let { list.add(it) }
            result?.artists?.let { list.addAll(it) }
            list.filter { it.name.isNotBlank() }.distinctBy { it.name.trim().lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun curateHomeSections(sections: List<HomeSection>): List<HomeSection> {
        fun normalized(value: String): String = java.text.Normalizer
            .normalize(value, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)

        val filtered = sections.filterNot { section ->
            val title = normalized(section.title)
            title.contains("em alta") || title.contains("trending") ||
                title.contains("novos videos") || title.contains("new videos") ||
                title.contains("novos albuns e singles") || title.contains("new albums and singles") ||
                title.contains("new albums & singles")
        }.filterNot { it.title == "Forgotten Favorites" }

        val forgotten = _forgottenFavoriteTracks.value
            .filterNot { userAffinityRepository?.isTrackPenalized(it.id) == true }
            .distinctBy { it.id }
            .take(16)
        if (forgotten.isEmpty() || _selectedHomeChip.value != null) return filtered

        val section = HomeSection(
            title = "Forgotten Favorites",
            strapline = "FAVORITAS QUE MERECEM VOLTAR",
            items = forgotten,
            style = HomeSectionStyle.QUICK_PICKS_GRID
        )
        val speedDialIndex = filtered.indexOfFirst { it.style == HomeSectionStyle.SPEED_DIAL }
        return if (speedDialIndex >= 0) {
            filtered.toMutableList().apply { add(speedDialIndex + 1, section) }
        } else listOf(section) + filtered
    }

    fun loadHomeFeed(params: String? = _selectedHomeChip.value?.params, force: Boolean = false) {
        viewModelScope.launch {
            _isLoadingHome.value = true
            _homeError.value = null
            val historyRepo = userHistoryRepository
            val selectedChip = _selectedHomeChip.value

            // Make locally available listening data visible without waiting for the network.
            val localFeed = homeFeedSynthesizer.synthesizeHomeFeed(
                baseFeedData = com.nerf.music.data.model.HomeFeedData(),
                historyRepo = historyRepo,
                libraryRepo = libraryRepository,
                affinityRepo = userAffinityRepository,
                selectedChip = selectedChip,
                includeRemoteContent = false
            )
            if (_homeSections.value.isEmpty() && localFeed.sections.isNotEmpty()) {
                _homeSections.value = curateHomeSections(localFeed.sections)
                prefetchHomeMedia(localFeed.sections)
            }

            repository.getHomeFeedData(params).onSuccess { feedData ->
                // Publish the catalog response first. Personalization continues in the background.
                val baseFeed = homeFeedSynthesizer.synthesizeHomeFeed(
                    baseFeedData = feedData,
                    historyRepo = historyRepo,
                    libraryRepo = libraryRepository,
                    affinityRepo = userAffinityRepository,
                    selectedChip = selectedChip,
                    includeRemoteContent = false
                )
                if (baseFeed.chips.isNotEmpty() && _homeChips.value.isEmpty()) {
                    _homeChips.value = baseFeed.chips
                }
                if (_homeSections.value.isEmpty() && baseFeed.sections.isNotEmpty()) {
                    _homeSections.value = curateHomeSections(baseFeed.sections)
                    prefetchHomeMedia(baseFeed.sections)
                }
                _isLoadingHome.value = false

                val synthesizedFeed = homeFeedSynthesizer.synthesizeHomeFeed(
                    baseFeedData = feedData,
                    historyRepo = historyRepo,
                    libraryRepo = libraryRepository,
                    affinityRepo = userAffinityRepository,
                    selectedChip = selectedChip
                )

                if (synthesizedFeed.chips.isNotEmpty() && _homeChips.value.isEmpty()) {
                    _homeChips.value = synthesizedFeed.chips
                }
                _homeSections.value = curateHomeSections(synthesizedFeed.sections)
                historyRepo?.saveCachedHomeFeed(synthesizedFeed.sections)

                prefetchHomeMedia(synthesizedFeed.sections)
            }.onFailure {
                if (_homeSections.value.isEmpty() && localFeed.sections.isNotEmpty()) {
                    _homeSections.value = curateHomeSections(localFeed.sections)
                    prefetchHomeMedia(localFeed.sections)
                    _isLoadingHome.value = false
                } else if (_homeSections.value.isEmpty()) {
                    _homeError.value = "Não foi possível carregar o feed inicial."
                    _isLoadingHome.value = false
                }
            }
        }
    }

    /** Resolves streams and Canvas for the first home picks before a track is tapped. */
    private fun prefetchHomeMedia(sections: List<HomeSection>) {
        homeCanvasPrefetchJob?.cancel()
        homeCanvasPrefetchJob = null

        val speedDialTracks = sections
            .firstOrNull { it.style == HomeSectionStyle.SPEED_DIAL }
            ?.items
            .orEmpty()
        val homeTracks = (speedDialTracks + sections.flatMap { it.items })
            .filter { it.id.isNotBlank() && !it.id.startsWith("shortcut_") && !it.id.startsWith("downloads_") }
            .distinctBy { it.id }
            .take(9)

        homeTracks.forEach { AudioPlayerService.prefetchTrack(it.id) }

        val settings = userSettingsRepository
        val canvasEnabled = settings?.isCanvasEnabled?.value ?: _isCanvasEnabled.value
        if (!canvasEnabled) return

        val wifiOnly = settings?.isCanvasWifiOnly?.value ?: _isCanvasWifiOnly.value
        val context = applicationContext
        if (wifiOnly && context != null && !NetworkUtils.isConnectedToWifi(context)) return

        homeCanvasPrefetchJob = viewModelScope.launch(Dispatchers.IO) {
            homeTracks.take(4).forEach { track ->
                launch canvasLookup@{
                    if (!isActive) return@canvasLookup
                    try {
                        spotifyCanvasRepository.getCanvasUrl(track.title, track.artist)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.d("MusicViewModel", "Pré-carregamento de Canvas ignorado para ${track.title}: ${e.message}")
                    }
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
            Novidades do NERF Music 1.4.7

            A Home e o Explorar carregam com mais agilidade e mant?m suas recomenda??es est?veis. O Explorar traz descobertas musicais, e a rolagem e as anima??es ficaram mais fluidas.

            O indicador de reprodu??o ganhou ondas animadas, e o player recebeu melhorias visuais e de acessibilidade.
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
        val downloaded = libraryRepository?.downloadedTracksFlow?.value?.firstOrNull { it.track.id == track.id }
        val ctx = applicationContext
        val localArtFile = when {
            downloaded?.localArtworkPath != null && java.io.File(downloaded.localArtworkPath).exists() ->
                java.io.File(downloaded.localArtworkPath)
            ctx != null && java.io.File(ctx.filesDir, "offline_audio/${track.id}.jpg").exists() ->
                java.io.File(ctx.filesDir, "offline_audio/${track.id}.jpg")
            else -> null
        }

        // Lê bytes de arte local de forma segura para notificação direta e lockscreen sem falha IPC de permissão
        val artworkBytes: ByteArray? = localArtFile?.let { f ->
            try {
                if (f.length() in 1..500_000) f.readBytes() else null
            } catch (_: Exception) { null }
        }

        val artworkUri = if (localArtFile != null && ctx != null) {
            try {
                androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", localArtFile)
            } catch (_: Exception) {
                null
            }
        } else {
            val isDataSaver = isDataSaverActive()
            val artworkUrl = if (isDataSaver) track.thumbnailUrl else (track.highResThumbnailUrl ?: track.thumbnailUrl)
            artworkUrl?.let { Uri.parse(it) }
        }

        val extras = Bundle().apply {
            putString(EXTRA_DURATION_TEXT, track.durationText)
            putLong(EXTRA_DURATION_SECONDS, track.durationSeconds)
            putString(EXTRA_THUMBNAIL_URL, localArtFile?.absolutePath?.let { "file://$it" } ?: artworkUri?.toString() ?: track.thumbnailUrl)
        }

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setExtras(extras)

        if (artworkBytes != null) {
            metadataBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }
        if (artworkUri != null) {
            metadataBuilder.setArtworkUri(artworkUri)
        }

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(AudioPlayerService.buildTrackUri(track.id))
            .setMediaMetadata(metadataBuilder.build())
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
            loadTrackRelatedForTrack(resolvedTrack)
        } else {
            prefetchNextCanvas()
        }
        _selectedTrack.value = resolvedTrack

        // Pre-fetch proativo de stream de áudio e artwork para próxima e faixa anterior
        finalQueue.getOrNull(currentIdx + 1)?.let { nextItem ->
            AudioPlayerService.prefetchTrack(nextItem.id, applicationContext?.filesDir)
            prefetchArtwork(nextItem)
        }
        finalQueue.getOrNull(currentIdx - 1)?.let { prevItem ->
            AudioPlayerService.prefetchTrack(prevItem.id, applicationContext?.filesDir)
            prefetchArtwork(prevItem)
        }

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
                        userHistoryRepository?.recordTrackListening(prevTrack, elapsed.coerceAtMost(dur.takeIf { it > 0 } ?: elapsed))
                    }
                }
                activeTrackId = mediaItem?.mediaId
                currentTrackPlayStartTime = System.currentTimeMillis()

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
                delay(90)
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
        if (!isApplyingRemoteJamEvent && !jamSessionManager.checkPermissionOrNotify(JamCommandType.PLAY_TRACK)) {
            return
        }
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
                    loadTrackRelatedForTrack(firstReal ?: track, upNext.relatedBrowseId)
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

        if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam) {
            jamSessionManager.dispatchOrBroadcastPlayerAction(
                command = JamCommandType.PLAY_TRACK,
                targetTrack = track
            )
        }

        // Se for participante em uma JAM, a fila é ditada pelo Host; não buscamos rádio dinâmica para não sobrescrever a fila da sala
        if (jamSessionState.value.isInJam && !jamSessionState.value.isHost) {
            _playbackState.value = _playbackState.value.copy(
                isLoadingQueueRadio = false
            )
            return
        }

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
                    if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam && jamSessionState.value.isHost) {
                        jamSessionManager.broadcastHostStateSnapshot()
                    }
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
                loadTrackRelatedForTrack(track, upNext.relatedBrowseId)
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
        if (!isApplyingRemoteJamEvent && !jamSessionManager.checkPermissionOrNotify(JamCommandType.SELECT_QUEUE_INDEX)) {
            return
        }
        val isParticipantInJam = jamSessionState.value.isInJam && !jamSessionState.value.isHost
        if (isParticipantInJam) {
            val targetTrack = _playbackState.value.queue.getOrNull(index) ?: return
            _loadingTrackId.value = targetTrack.id
            loadLyrics(targetTrack)
            loadCanvasVideo(targetTrack)
            _playbackState.value = _playbackState.value.copy(
                currentTrack = targetTrack,
                currentQueueIndex = index,
                currentPositionMs = 0L,
                isBuffering = true
            )
            jamSessionManager.dispatchOrBroadcastPlayerAction(
                command = JamCommandType.SELECT_QUEUE_INDEX,
                targetQueueIndex = index
            )
            return
        }

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
            val service = AudioPlayerService.getInstance()
            if (service != null && controller.isPlaying) {
                service.smoothTransitionTo(index)
            } else {
                controller.seekToDefaultPosition(index)
                controller.play()
            }
            if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam) {
                jamSessionManager.dispatchOrBroadcastPlayerAction(
                    command = JamCommandType.SELECT_QUEUE_INDEX,
                    targetQueueIndex = index
                )
            }
        }
    }

    /**
     * Reordena um item na fila do ExoPlayer disparando `moveMediaItem(fromIndex, toIndex)`.
     */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (!isApplyingRemoteJamEvent && !jamSessionManager.checkPermissionOrNotify(JamCommandType.MOVE_QUEUE_ITEM)) {
            return
        }
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

        val isParticipantInJam = jamSessionState.value.isInJam && !jamSessionState.value.isHost
        if (!isParticipantInJam) {
            val controller = mediaController
            if (controller != null && fromIndex in 0 until controller.mediaItemCount && toIndex in 0 until controller.mediaItemCount) {
                try {
                    controller.moveMediaItem(fromIndex, toIndex)
                } catch (e: Exception) {
                    Log.e("MusicViewModel", "Erro ao mover item na timeline do player: ${e.message}")
                }
            }
        }

        if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam) {
            jamSessionManager.dispatchOrBroadcastPlayerAction(
                command = JamCommandType.MOVE_QUEUE_ITEM,
                fromQueueIndex = fromIndex,
                toQueueIndex = toIndex
            )
        }
    }

    /**
     * Remove um item da fila do ExoPlayer disparando `removeMediaItem(index)`.
     */
    fun removeQueueItem(index: Int) {
        if (!isApplyingRemoteJamEvent && !jamSessionManager.checkPermissionOrNotify(JamCommandType.REMOVE_QUEUE_ITEM)) {
            return
        }
        val currentQueue = _playbackState.value.queue
        if (index !in currentQueue.indices) return

        val removedTrack = currentQueue[index]
        val mutableQueue = currentQueue.toMutableList()
        mutableQueue.removeAt(index)

        val oldActiveIdx = _playbackState.value.currentQueueIndex
        val newActiveIdx = when {
            index < oldActiveIdx -> oldActiveIdx - 1
            index == oldActiveIdx -> oldActiveIdx.coerceAtMost((mutableQueue.size - 1).coerceAtLeast(0))
            else -> oldActiveIdx
        }

        _playbackState.value = _playbackState.value.copy(
            queue = mutableQueue,
            currentQueueIndex = newActiveIdx
        )
        _userMessage.value = "Removida da fila: ${removedTrack.title}"

        val isParticipantInJam = jamSessionState.value.isInJam && !jamSessionState.value.isHost
        if (!isParticipantInJam) {
            val controller = mediaController
            if (controller != null && index in 0 until controller.mediaItemCount) {
                try {
                    controller.removeMediaItem(index)
                } catch (e: Exception) {
                    Log.e("MusicViewModel", "Erro ao remover item do player: ${e.message}")
                }
            }
        }

        if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam) {
            jamSessionManager.dispatchOrBroadcastPlayerAction(
                command = JamCommandType.REMOVE_QUEUE_ITEM,
                targetQueueIndex = index
            )
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newShuffle = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = newShuffle
        _playbackState.value = _playbackState.value.copy(shuffleModeEnabled = newShuffle)
    }

    fun toggleRepeat() {
        if (!isApplyingRemoteJamEvent && !jamSessionManager.checkPermissionOrNotify(JamCommandType.SET_REPEAT_MODE)) {
            return
        }
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _playbackState.value = _playbackState.value.copy(repeatMode = nextMode)
        if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam) {
            val modeName = when (nextMode) {
                Player.REPEAT_MODE_ALL -> "ALL"
                Player.REPEAT_MODE_ONE -> "ONE"
                else -> "OFF"
            }
            jamSessionManager.dispatchOrBroadcastPlayerAction(
                command = JamCommandType.SET_REPEAT_MODE,
                targetRepeatModeName = modeName
            )
        }
    }

    fun setRepeatModeExplicit(mode: Int) {
        if (!isApplyingRemoteJamEvent && !jamSessionManager.checkPermissionOrNotify(JamCommandType.SET_REPEAT_MODE)) {
            return
        }
        val controller = mediaController
        controller?.repeatMode = mode
        _playbackState.value = _playbackState.value.copy(repeatMode = mode)
        if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam) {
            val modeName = when (mode) {
                Player.REPEAT_MODE_ALL -> "ALL"
                Player.REPEAT_MODE_ONE -> "ONE"
                else -> "OFF"
            }
            jamSessionManager.dispatchOrBroadcastPlayerAction(
                command = JamCommandType.SET_REPEAT_MODE,
                targetRepeatModeName = modeName
            )
        }
    }

    fun togglePlayPause() {
        val targetCmd = if (mediaController?.isPlaying == true) JamCommandType.PAUSE else JamCommandType.PLAY
        if (!isApplyingRemoteJamEvent && !jamSessionManager.checkPermissionOrNotify(targetCmd)) {
            return
        }
        val controller = mediaController ?: return
        if (controller.mediaItemCount == 0) return

        if (controller.isPlaying) {
            controller.pause()
            if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam) {
                jamSessionManager.dispatchOrBroadcastPlayerAction(command = JamCommandType.PAUSE)
            }
        } else {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0L)
            }
            controller.play()
            if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam) {
                jamSessionManager.dispatchOrBroadcastPlayerAction(command = JamCommandType.PLAY)
            }
        }
    }

    fun skipNext() {
        if (!isApplyingRemoteJamEvent && !jamSessionManager.checkPermissionOrNotify(JamCommandType.SKIP_NEXT)) {
            return
        }
        val controller = mediaController ?: return
        val count = controller.mediaItemCount
        if (count <= 0) return

        val targetIndex = if (controller.hasNextMediaItem()) {
            controller.nextMediaItemIndex
        } else if (count > 1) {
            0
        } else {
            seekTo(0L)
            return
        }

        // Atualização Otimista Imediata da UI para eliminar delay perceptível
        val targetTrack = _playbackState.value.queue.getOrNull(targetIndex)
        if (targetTrack != null) {
            _loadingTrackId.value = targetTrack.id
            _selectedTrack.value = targetTrack
            _playbackState.value = _playbackState.value.copy(
                currentTrack = targetTrack,
                currentQueueIndex = targetIndex,
                isBuffering = true,
                currentPositionMs = 0L,
                durationMs = (targetTrack.durationSeconds * 1000L).coerceAtLeast(0L),
                hasNext = targetIndex < count - 1,
                hasPrevious = targetIndex > 0
            )
            loadLyrics(targetTrack)
            loadCanvasVideo(targetTrack)
            loadTrackRelatedForTrack(targetTrack)
        }

        val service = AudioPlayerService.getInstance()
        if (service != null && controller.isPlaying) {
            service.smoothTransitionTo(targetIndex)
        } else {
            controller.seekToDefaultPosition(targetIndex)
            controller.play()
        }
        if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam) {
            jamSessionManager.dispatchOrBroadcastPlayerAction(
                command = JamCommandType.SKIP_NEXT,
                targetQueueIndex = targetIndex
            )
        }
    }

    fun skipPrevious() {
        if (!isApplyingRemoteJamEvent && !jamSessionManager.checkPermissionOrNotify(JamCommandType.SKIP_PREV)) {
            return
        }
        val controller = mediaController ?: return
        val count = controller.mediaItemCount
        if (count <= 0) return

        if (controller.currentPosition > 3_000L) {
            seekTo(0L)
            return
        }

        val targetIndex = if (controller.hasPreviousMediaItem()) {
            controller.previousMediaItemIndex
        } else if (count > 1) {
            count - 1
        } else {
            seekTo(0L)
            return
        }

        // Atualização Otimista Imediata da UI
        val targetTrack = _playbackState.value.queue.getOrNull(targetIndex)
        if (targetTrack != null) {
            _loadingTrackId.value = targetTrack.id
            _selectedTrack.value = targetTrack
            _playbackState.value = _playbackState.value.copy(
                currentTrack = targetTrack,
                currentQueueIndex = targetIndex,
                isBuffering = true,
                currentPositionMs = 0L,
                durationMs = (targetTrack.durationSeconds * 1000L).coerceAtLeast(0L),
                hasNext = targetIndex < count - 1,
                hasPrevious = targetIndex > 0
            )
            loadLyrics(targetTrack)
            loadCanvasVideo(targetTrack)
            loadTrackRelatedForTrack(targetTrack)
        }

        val service = AudioPlayerService.getInstance()
        if (service != null && controller.isPlaying) {
            service.smoothTransitionTo(targetIndex)
        } else {
            controller.seekToDefaultPosition(targetIndex)
            controller.play()
        }
        if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam) {
            jamSessionManager.dispatchOrBroadcastPlayerAction(
                command = JamCommandType.SKIP_PREV,
                targetQueueIndex = targetIndex
            )
        }
    }

    /**
     * Pula imediatamente para a faixa anterior (usado em gestos de swipe),
     * sem reiniciar a música atual mesmo se ela estiver após 3 segundos.
     */
    fun forceSkipPrevious() {
        if (!isApplyingRemoteJamEvent && !jamSessionManager.checkPermissionOrNotify(JamCommandType.SKIP_PREV)) {
            return
        }
        val controller = mediaController ?: return
        val count = controller.mediaItemCount
        if (count <= 0) return

        val targetIndex = if (controller.hasPreviousMediaItem()) {
            controller.previousMediaItemIndex
        } else if (count > 1) {
            count - 1
        } else {
            seekTo(0L)
            return
        }

        // Atualização Otimista Imediata da UI
        val targetTrack = _playbackState.value.queue.getOrNull(targetIndex)
        if (targetTrack != null) {
            _loadingTrackId.value = targetTrack.id
            _selectedTrack.value = targetTrack
            _playbackState.value = _playbackState.value.copy(
                currentTrack = targetTrack,
                currentQueueIndex = targetIndex,
                isBuffering = true,
                currentPositionMs = 0L,
                durationMs = (targetTrack.durationSeconds * 1000L).coerceAtLeast(0L),
                hasNext = targetIndex < count - 1,
                hasPrevious = targetIndex > 0
            )
            loadLyrics(targetTrack)
            loadCanvasVideo(targetTrack)
            loadTrackRelatedForTrack(targetTrack)
        }

        val service = AudioPlayerService.getInstance()
        if (service != null && controller.isPlaying) {
            service.smoothTransitionTo(targetIndex)
        } else {
            controller.seekToDefaultPosition(targetIndex)
            controller.play()
        }
        if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam) {
            jamSessionManager.dispatchOrBroadcastPlayerAction(
                command = JamCommandType.SKIP_PREV,
                targetQueueIndex = targetIndex
            )
        }
    }

    fun seekTo(positionMs: Long) {
        if (!isApplyingRemoteJamEvent && !jamSessionManager.checkPermissionOrNotify(JamCommandType.SEEK)) {
            return
        }
        val targetPos = positionMs.coerceAtLeast(0L)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = targetPos)
        mediaController?.seekTo(targetPos)
        updateCurrentLyricsLine(targetPos)
        if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam) {
            jamSessionManager.dispatchOrBroadcastPlayerAction(
                command = JamCommandType.SEEK,
                targetPositionMs = targetPos
            )
        }
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
            // Validação de Economia de Dados
            val mode = userSettingsRepository?.dataSaverMode?.value ?: _dataSaverMode.value
            val allowCellular = userSettingsRepository?.isDownloadOverCellular?.value ?: _isDownloadOverCellular.value
            val context = applicationContext
            val isWifi = context?.let { NetworkUtils.isConnectedToWifi(it) } ?: false
            val isNetworkAvailable = context?.let { NetworkUtils.isNetworkAvailable(it) } ?: false
            val isCellular = isNetworkAvailable && !isWifi

            if (mode == DataSaverMode.ALWAYS_ON) {
                _userMessage.value = "Downloads bloqueados: modo Economia de Dados (Sempre ativado) está ativo."
                return
            }
            if (!isWifi && (isCellular || !isNetworkAvailable)) {
                if (!allowCellular) {
                    _userMessage.value = "Download pausado: 'Downloads via dados móveis' está desativado nas configurações."
                    return
                }
                if (mode == DataSaverMode.AUTOMATIC) {
                    _userMessage.value = "Downloads bloqueados em rede móvel pela Economia de Dados."
                    return
                }
            }

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

    fun playRecapMix(summary: RecapSummary) {
        val tracks = summary.mixTracks.distinctBy { it.id }
        if (tracks.isNotEmpty()) {
            playTrackList(
                title = "NERF Recap ${summary.year}",
                tracks = tracks,
                startIndex = 0,
                isShuffle = false
            )
        }
    }

    suspend fun getRecapArtistImageUrl(name: String): String? {
        val key = name.trim().lowercase(Locale.ROOT)
        if (key.isBlank()) return null
        recapArtistImageCache[key]?.let { return it }
        val result = repository.search(name, SearchFilter.ARTISTS).getOrNull() ?: return null
        val artists = listOfNotNull(result.topArtist) + result.artists
        val imageUrl = artists.firstOrNull { it.name.equals(name, ignoreCase = true) }?.thumbnailUrl
            ?: result.topArtist?.thumbnailUrl
            ?: result.artists.firstOrNull()?.thumbnailUrl
        return imageUrl?.takeIf(String::isNotBlank)?.also { recapArtistImageCache[key] = it }
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
            val appCtx = applicationContext
            val isDownloaded = libraryRepository?.isDownloaded(nextTrack.id) == true
            if (!isDownloaded && (appCtx == null || NetworkUtils.isNetworkAvailable(appCtx))) {
                AudioPlayerService.prefetchTrack(nextTrack.id, appCtx?.filesDir)
            }
        }
    }

    fun downloadAll(tracks: List<Track>) {
        val libRepo = libraryRepository ?: return
        val toDownload = tracks.filterNot { libRepo.isDownloaded(it.id) }
        if (toDownload.isEmpty()) {
            _userMessage.value = "Todas as músicas já foram baixadas"
            return
        }

        // Validação de Economia de Dados
        val mode = userSettingsRepository?.dataSaverMode?.value ?: _dataSaverMode.value
        val allowCellular = userSettingsRepository?.isDownloadOverCellular?.value ?: _isDownloadOverCellular.value
        val context = applicationContext
        val isWifi = context?.let { NetworkUtils.isConnectedToWifi(it) } ?: false
        val isNetworkAvailable = context?.let { NetworkUtils.isNetworkAvailable(it) } ?: false
        val isCellular = isNetworkAvailable && !isWifi

        if (mode == DataSaverMode.ALWAYS_ON) {
            _userMessage.value = "Downloads bloqueados: modo Economia de Dados (Sempre ativado) está ativo."
            return
        }
        if (!isWifi && (isCellular || !isNetworkAvailable)) {
            if (!allowCellular) {
                _userMessage.value = "Download pausado: 'Downloads via dados móveis' está desativado nas configurações."
                return
            }
            if (mode == DataSaverMode.AUTOMATIC) {
                _userMessage.value = "Downloads bloqueados em rede móvel pela Economia de Dados."
                return
            }
        }

        _userMessage.value = "Baixando músicas..."
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
        if (!isApplyingRemoteJamEvent && !jamSessionManager.checkPermissionOrNotify(JamCommandType.ADD_TO_QUEUE)) {
            return
        }
        if (tracks.isEmpty()) return
        tracks.forEach { trackMetadataMap[it.id] = it }

        val isParticipantInJam = jamSessionState.value.isInJam && !jamSessionState.value.isHost
        if (isParticipantInJam) {
            val cur = _playbackState.value
            _playbackState.value = cur.copy(
                queue = cur.queue + tracks,
                hasNext = true
            )
            _userMessage.value = if (tracks.size == 1) "Música adicionada à fila" else "${tracks.size} músicas adicionadas à fila"
            jamSessionManager.dispatchOrBroadcastPlayerAction(
                command = JamCommandType.ADD_TO_QUEUE,
                targetTracks = tracks
            )
            return
        }

        val controller = mediaController ?: return
        val mediaItems = tracks.map { trackToMediaItem(it) }
        controller.addMediaItems(mediaItems)
        val cur = _playbackState.value
        _playbackState.value = cur.copy(
            queue = cur.queue + tracks,
            hasNext = true
        )
        _userMessage.value = if (tracks.size == 1) "Música adicionada à fila" else "${tracks.size} músicas adicionadas à fila"
        if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam) {
            jamSessionManager.dispatchOrBroadcastPlayerAction(
                command = JamCommandType.ADD_TO_QUEUE,
                targetTracks = tracks
            )
        }
    }

    fun addTracksToPlayNext(tracks: List<Track>) {
        if (!isApplyingRemoteJamEvent && !jamSessionManager.checkPermissionOrNotify(JamCommandType.PLAY_NEXT_IN_QUEUE)) {
            return
        }
        if (tracks.isEmpty()) return
        tracks.forEach { trackMetadataMap[it.id] = it }

        val isParticipantInJam = jamSessionState.value.isInJam && !jamSessionState.value.isHost
        if (isParticipantInJam) {
            val cur = _playbackState.value
            val insertIdx = (cur.currentQueueIndex + 1).coerceIn(0, cur.queue.size)
            val newQueue = cur.queue.toMutableList().apply { addAll(insertIdx, tracks) }
            _playbackState.value = cur.copy(
                queue = newQueue,
                hasNext = true
            )
            _userMessage.value = if (tracks.size == 1) "Música adicionada para tocar a seguir" else "${tracks.size} músicas adicionadas para tocar a seguir"
            jamSessionManager.dispatchOrBroadcastPlayerAction(
                command = JamCommandType.PLAY_NEXT_IN_QUEUE,
                targetTracks = tracks
            )
            return
        }

        val controller = mediaController ?: return
        val nextIndex = (controller.currentMediaItemIndex + 1).coerceAtLeast(0)
        val mediaItems = tracks.map { trackToMediaItem(it) }
        controller.addMediaItems(nextIndex, mediaItems)
        val cur = _playbackState.value
        val insertIdx = (cur.currentQueueIndex + 1).coerceIn(0, cur.queue.size)
        val newQueue = cur.queue.toMutableList().apply { addAll(insertIdx, tracks) }
        _playbackState.value = cur.copy(
            queue = newQueue,
            hasNext = true
        )
        _userMessage.value = if (tracks.size == 1) "Música adicionada para tocar a seguir" else "${tracks.size} músicas adicionadas para tocar a seguir"
        if (!isApplyingRemoteJamEvent && jamSessionState.value.isInJam) {
            jamSessionManager.dispatchOrBroadcastPlayerAction(
                command = JamCommandType.PLAY_NEXT_IN_QUEUE,
                targetTracks = tracks
            )
        }
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

    private var lastRelatedTrackId: String? = null

    /**
     * Resolve e enriquece a aba "Relacionados" com arquitetura multi-tier à prova de falhas:
     * 1. Endpoint oficial InnerTube /browse (via [browseId] retornado pelo /next).
     * 2. Rádio algorítmica UpNext para o ID da faixa (ou fila dinâmica atual) garantindo "Músicas semelhantes".
     * 3. Busca de catálogo por músicas do artista principal limpo ("Mais de <Artista>").
     * 4. Busca por termos contextuais ("$título $artista") como último recurso.
     */
    fun loadTrackRelatedForTrack(track: Track, browseId: String? = null, forceReload: Boolean = false) {
        if (!forceReload && lastRelatedTrackId == track.id && _trackRelatedData.value?.sections?.isNotEmpty() == true) {
            return
        }
        lastRelatedTrackId = track.id
        relatedFetchJob?.cancel()
        relatedFetchJob = viewModelScope.launch(Dispatchers.IO) {
            _isLoadingRelated.value = true
            _trackRelatedData.value = null
            try {
                val sections = mutableListOf<RelatedSection>()
                var aboutHeader: String? = null
                var aboutDesc: String? = null

                // 1. Tenta buscar endpoint oficial do YouTube Music se houver browseId
                val effectiveBrowseId = browseId ?: _playbackState.value.relatedBrowseId
                if (!effectiveBrowseId.isNullOrBlank()) {
                    try {
                        repository.getTrackRelated(effectiveBrowseId).onSuccess { data ->
                            val nonEmptySections = data.sections.filter { it.items.isNotEmpty() }
                            sections.addAll(nonEmptySections)
                            aboutHeader = data.aboutArtistHeader
                            aboutDesc = data.aboutArtistDescription
                            nonEmptySections.forEach { sec ->
                                sec.items.forEach { trackMetadataMap[it.id] = it }
                            }
                        }.onFailure { err ->
                            Log.w("MusicViewModel", "InnerTube getTrackRelated falhou para $effectiveBrowseId: ${err.message}")
                        }
                    } catch (e: Exception) {
                        Log.w("MusicViewModel", "Erro ao invocar getTrackRelated: ${e.message}")
                    }
                }

                // 2. Se a aba veio sem seções (ou sem browseId), busca recomendações similares via Rádio / UpNext
                if (sections.isEmpty()) {
                    // Primeiro tenta a fila existente caso ela tenha faixas relacionadas
                    val currentQueueSimilar = _playbackState.value.queue
                        .filter { it.id != track.id && it.id.isNotBlank() }
                        .distinctBy { it.id }
                    if (currentQueueSimilar.isNotEmpty()) {
                        val similarItems = currentQueueSimilar.take(15)
                        sections.add(
                            RelatedSection(
                                title = "Músicas semelhantes",
                                items = similarItems,
                                style = HomeSectionStyle.QUICK_PICKS_GRID
                            )
                        )
                        similarItems.forEach { trackMetadataMap[it.id] = it }
                    } else {
                        // Busca Rádio Algorítmica diretamente da InnerTube para esta faixa
                        try {
                            val radioResult = repository.getUpNextRadio(videoId = track.id, params = null).getOrNull()
                            val radioTracks = radioResult?.tracks
                                ?.filter { it.id != track.id && it.id.isNotBlank() }
                                ?.distinctBy { it.id }
                                ?.take(15)
                                .orEmpty()
                            if (radioTracks.isNotEmpty()) {
                                sections.add(
                                    RelatedSection(
                                        title = "Músicas semelhantes",
                                        items = radioTracks,
                                        style = HomeSectionStyle.QUICK_PICKS_GRID
                                    )
                                )
                                radioTracks.forEach { trackMetadataMap[it.id] = it }
                            }
                        } catch (e: Exception) {
                            Log.w("MusicViewModel", "Erro ao obter UpNextRadio fallback: ${e.message}")
                        }
                    }
                }

                // 3. Adiciona seção "Mais de <Artista>" pesquisando no catálogo por músicas do artista principal
                val cleanArtist = extractPrimaryArtist(track.artist)
                if (cleanArtist.isNotBlank() && !cleanArtist.equals("Desconhecido", ignoreCase = true) && !cleanArtist.equals("Artista Desconhecido", ignoreCase = true)) {
                    try {
                        val searchResult = repository.search(cleanArtist, SearchFilter.SONGS).getOrNull()
                        val rawArtistTracks = searchResult?.songs.orEmpty()
                        val existingIds = sections.flatMap { it.items }.map { it.id }.toSet()
                        val artistTracks = rawArtistTracks
                            .filter { it.id != track.id && it.id !in existingIds && it.id.isNotBlank() }
                            .distinctBy { it.id }
                            .take(10)
                        if (artistTracks.isNotEmpty()) {
                            sections.add(
                                RelatedSection(
                                    title = "Mais de $cleanArtist",
                                    items = artistTracks,
                                    style = HomeSectionStyle.QUICK_PICKS_GRID
                                )
                            )
                            artistTracks.forEach { trackMetadataMap[it.id] = it }
                        }
                    } catch (e: Exception) {
                        Log.w("MusicViewModel", "Fallback artist search falhou para $cleanArtist: ${e.message}")
                    }
                }

                // 4. Último fallback de segurança para nunca ficar vazio: busca por query do título limpo
                if (sections.isEmpty()) {
                    val cleanTitle = track.title.replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), "").trim()
                    val query = if (cleanArtist.isNotBlank()) "$cleanTitle $cleanArtist" else cleanTitle
                    if (query.isNotBlank()) {
                        try {
                            val searchResult = repository.search(query, SearchFilter.SONGS).getOrNull()
                            val queryTracks = searchResult?.songs
                                ?.filter { it.id != track.id && it.id.isNotBlank() }
                                ?.distinctBy { it.id }
                                ?.take(10)
                                .orEmpty()
                            if (queryTracks.isNotEmpty()) {
                                sections.add(
                                    RelatedSection(
                                        title = "Você também pode gostar",
                                        items = queryTracks,
                                        style = HomeSectionStyle.QUICK_PICKS_GRID
                                    )
                                )
                                queryTracks.forEach { trackMetadataMap[it.id] = it }
                            }
                        } catch (e: Exception) {
                            Log.w("MusicViewModel", "Query fallback search falhou: ${e.message}")
                        }
                    }
                }

                _trackRelatedData.value = TrackRelatedData(
                    sections = sections,
                    aboutArtistHeader = aboutHeader,
                    aboutArtistDescription = aboutDesc
                )
            } finally {
                _isLoadingRelated.value = false
            }
        }
    }

    private fun extractPrimaryArtist(rawArtist: String): String {
        if (rawArtist.isBlank()) return ""
        return rawArtist
            .split(Regex("(?i)[,;&/]|\\b(feat|ft|part)\\b|\\(part\\."))
            .firstOrNull()
            ?.trim()
            .orEmpty()
            .ifEmpty { rawArtist.trim() }
    }

    fun loadTrackRelated(relatedBrowseId: String? = null) {
        val current = _playbackState.value.currentTrack ?: return
        loadTrackRelatedForTrack(current, relatedBrowseId, forceReload = true)
    }

    fun loadTrackRelatedFallback() {
        val current = _playbackState.value.currentTrack ?: return
        loadTrackRelatedForTrack(current, null, forceReload = true)
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
        if (jamSessionState.value.isInJam && jamSessionState.value.isHost) {
            jamSessionManager.broadcastHostStateSnapshot()
        }
    }

    fun setAutomixEnabled(enabled: Boolean) {
        _isAutomixEnabled.value = enabled
        userSettingsRepository?.setAutomixEnabled(enabled)
        if (jamSessionState.value.isInJam && jamSessionState.value.isHost) {
            jamSessionManager.broadcastHostStateSnapshot()
        }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(0, 12)
        _crossfadeSeconds.value = clamped
        userSettingsRepository?.setCrossfadeSeconds(clamped)
        if (jamSessionState.value.isInJam && jamSessionState.value.isHost) {
            jamSessionManager.broadcastHostStateSnapshot()
        }
    }

    fun setLoudnessStandard(standard: LoudnessStandard) {
        _loudnessStandard.value = standard
        userSettingsRepository?.setLoudnessStandard(standard)
        AudioPlayerService.setLoudnessStandard(standard)
    }

    fun setDataSaverMode(mode: DataSaverMode) {
        _dataSaverMode.value = mode
        userSettingsRepository?.setDataSaverMode(mode)
        AudioPlayerService.clearStreamCache()
    }

    fun isDataSaverActive(): Boolean {
        val mode = userSettingsRepository?.dataSaverMode?.value ?: _dataSaverMode.value
        val context = applicationContext ?: return mode == DataSaverMode.ALWAYS_ON
        val isWifi = NetworkUtils.isConnectedToWifi(context)
        return when (mode) {
            DataSaverMode.ALWAYS_ON -> true
            DataSaverMode.AUTOMATIC -> !isWifi
            DataSaverMode.ALWAYS_OFF -> false
        }
    }

    fun setDownloadOverCellular(enabled: Boolean) {
        _isDownloadOverCellular.value = enabled
        userSettingsRepository?.setDownloadOverCellular(enabled)
    }

    fun setWifiAudioQuality(quality: AudioQuality) {
        _wifiAudioQuality.value = quality
        userSettingsRepository?.setWifiAudioQuality(quality)
        AudioPlayerService.clearStreamCache()
    }

    fun setCellularAudioQuality(quality: AudioQuality) {
        _cellularAudioQuality.value = quality
        userSettingsRepository?.setCellularAudioQuality(quality)
        AudioPlayerService.clearStreamCache()
    }

    fun setDownloadAudioQuality(quality: DownloadAudioQuality) {
        _downloadAudioQuality.value = quality
        userSettingsRepository?.setDownloadAudioQuality(quality)
        AudioPlayerService.clearStreamCache()
    }

    fun setDownloadCanvasEnabled(enabled: Boolean) {
        _isDownloadCanvasEnabled.value = enabled
        userSettingsRepository?.setDownloadCanvasEnabled(enabled)
    }

    fun setDownloadLyricsEnabled(enabled: Boolean) {
        _isDownloadLyricsEnabled.value = enabled
        userSettingsRepository?.setDownloadLyricsEnabled(enabled)
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

    fun importFromSpotifyUrl(url: String) {
        val manager = migrationManager ?: return
        viewModelScope.launch {
            _migrationProgress.value = MigrationProgressState(isImporting = true, message = "Acessando playlist pública do Spotify...")
            val result = manager.importFromSpotifyUrl(url) { current, total, trackTitle ->
                _migrationProgress.value = MigrationProgressState(
                    isImporting = true,
                    current = current,
                    total = total,
                    currentTrackTitle = trackTitle
                )
            }
            _migrationProgress.value = null
            result.onSuccess { playlist ->
                _userMessage.value = "Playlist '${playlist.title}' importada com sucesso (${playlist.tracks.size} músicas)!"
                loadHomeFeed()
            }.onFailure { e ->
                _userMessage.value = "Falha ao importar do Spotify: ${e.message}"
            }
        }
    }

    fun importFromYouTubeMusicUrl(url: String) {
        val manager = migrationManager ?: return
        viewModelScope.launch {
            _migrationProgress.value = MigrationProgressState(isImporting = true, message = "Carregando playlist do YouTube Music...")
            val result = manager.importFromYouTubeMusicUrl(url) { current, total, trackTitle ->
                _migrationProgress.value = MigrationProgressState(
                    isImporting = true,
                    current = current,
                    total = total,
                    currentTrackTitle = trackTitle
                )
            }
            _migrationProgress.value = null
            result.onSuccess { playlist ->
                _userMessage.value = "Playlist '${playlist.title}' importada com sucesso (${playlist.tracks.size} músicas)!"
                loadHomeFeed()
            }.onFailure { e ->
                _userMessage.value = "Falha ao importar do YouTube Music: ${e.message}"
            }
        }
    }

    fun importFromTextOrCsv(content: String, title: String) {
        val manager = migrationManager ?: return
        viewModelScope.launch {
            _migrationProgress.value = MigrationProgressState(isImporting = true, message = "Processando arquivo de backup...")
            val result = manager.importFromTextOrCsv(content, title) { current, total, trackTitle ->
                _migrationProgress.value = MigrationProgressState(
                    isImporting = true,
                    current = current,
                    total = total,
                    currentTrackTitle = trackTitle
                )
            }
            _migrationProgress.value = null
            result.onSuccess { playlist ->
                _userMessage.value = "Arquivo importado com sucesso! (${playlist.tracks.size} músicas na playlist '${playlist.title}')"
                loadHomeFeed()
            }.onFailure { e ->
                _userMessage.value = "Falha ao importar arquivo: ${e.message}"
            }
        }
    }

    fun connectYouTubeMusicAccount(cookies: String, accountName: String): Boolean {
        val saved = migrationManager?.saveYouTubeMusicSession(cookies, accountName) == true
        if (!saved) {
            _userMessage.value = "Não foi possível proteger e salvar a sessão do YouTube Music."
            return false
        }
        _isYouTubeMusicConnected.value = true
        _userMessage.value = "Conta do YouTube Music conectada com sucesso!"
        syncYouTubeMusicAccount()
        return true
    }

    fun disconnectYouTubeMusicAccount() {
        migrationManager?.disconnectYouTubeMusic()
        _isYouTubeMusicConnected.value = false
        _userMessage.value = "Conta do YouTube Music desconectada."
    }

    fun syncYouTubeMusicAccount() {
        viewModelScope.launch {
            _migrationProgress.value = MigrationProgressState(
                isImporting = true,
                current = 0,
                total = 1,
                currentTrackTitle = "Iniciando sincronização com YouTube Music..."
            )
            val result = migrationManager?.syncYouTubeMusicAccount { current, total, trackTitle ->
                _migrationProgress.value = MigrationProgressState(
                    isImporting = true,
                    current = current,
                    total = total,
                    currentTrackTitle = trackTitle
                )
            }
            _migrationProgress.value = MigrationProgressState(isImporting = false)
            result?.onSuccess { count ->
                _userMessage.value = "YouTube Music sincronizado com sucesso! $count faixas atualizadas."
                loadHomeFeed()
            }?.onFailure { error ->
                _userMessage.value = "Erro ao sincronizar YouTube Music: ${error.message}"
            }
        }
    }

    fun connectSpotifyAccount(tokenOrCookies: String, accountName: String): Boolean {
        val saved = migrationManager?.saveSpotifySession(tokenOrCookies, accountName) == true
        if (!saved) {
            _userMessage.value = "Não foi possível proteger e salvar a sessão do Spotify."
            return false
        }
        _isSpotifyConnected.value = true
        _userMessage.value = "Conta do Spotify conectada com sucesso!"
        syncSpotifyAccount()
        return true
    }

    fun disconnectSpotifyAccount() {
        migrationManager?.disconnectSpotify()
        _isSpotifyConnected.value = false
        _userMessage.value = "Conta do Spotify desconectada."
    }

    fun syncSpotifyAccount() {
        viewModelScope.launch {
            _migrationProgress.value = MigrationProgressState(
                isImporting = true,
                current = 0,
                total = 1,
                currentTrackTitle = "Iniciando sincronização com Spotify..."
            )
            val result = migrationManager?.syncSpotifyAccount { current, total, trackTitle ->
                _migrationProgress.value = MigrationProgressState(
                    isImporting = true,
                    current = current,
                    total = total,
                    currentTrackTitle = trackTitle
                )
            }
            _migrationProgress.value = MigrationProgressState(isImporting = false)
            result?.onSuccess { count ->
                _userMessage.value = "Spotify sincronizado com sucesso! $count faixas atualizadas."
                loadHomeFeed()
            }?.onFailure { error ->
                _userMessage.value = "Erro ao sincronizar Spotify: ${error.message}"
            }
        }
    }

    fun triggerSmartDownloadSyncNow() {
        viewModelScope.launch {
            smartDownloadManager?.syncNow(force = true)
        }
    }

    fun exportBackup(uri: Uri) {
        val context = applicationContext ?: return
        viewModelScope.launch {
            runCatching { BackupRepository(context).exportTo(uri) }
                .onSuccess { summary ->
                    _userMessage.value = "Backup criado: ${summary.preferenceCount} preferências e ${summary.trackCount} faixas."
                }
                .onFailure { error -> _userMessage.value = "Falha ao criar backup: ${error.message}" }
        }
    }

    fun restoreBackup(uri: Uri) {
        val context = applicationContext ?: return
        viewModelScope.launch {
            runCatching { BackupRepository(context).restoreFrom(uri) }
                .onSuccess { summary ->
                    _userMessage.value = "Backup restaurado: ${summary.preferenceCount} preferências e ${summary.trackCount} faixas."
                    libraryRepository = null
                    userHistoryRepository = null
                    userAffinityRepository = null
                    initializePlayerController(context)
                    loadHomeFeed(force = true)
                }
                .onFailure { error -> _userMessage.value = "Falha ao restaurar backup: ${error.message}" }
        }
    }

    fun setSleepTimerMinutes(minutes: Int) {
        AudioPlayerService.setSleepTimerMinutes(minutes)
    }

    fun setSleepTimerEndOfTrack() {
        AudioPlayerService.setSleepTimerEndOfTrack()
        _userMessage.value = "Temporizador: a reprodução será pausada ao final desta música."
    }

    fun cancelSleepTimer() {
        AudioPlayerService.cancelSleepTimer()
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

        // Validação de Economia de Dados: suprime Canvas em ALWAYS_ON ou AUTOMATIC em redes não-Wi-Fi
        val currentMode = settings?.dataSaverMode?.value ?: _dataSaverMode.value
        val context = applicationContext
        val isWifi = context?.let { NetworkUtils.isConnectedToWifi(it) } ?: false
        if (currentMode == DataSaverMode.ALWAYS_ON || (currentMode == DataSaverMode.AUTOMATIC && !isWifi)) {
            Log.i("MusicViewModel", "Canvas suprimido: Economia de Dados ativa.")
            _isLoadingCanvasVideo.value = false
            return
        }

        val isWifiOnly = settings?.isCanvasWifiOnly?.value ?: _isCanvasWifiOnly.value
        if (isWifiOnly && !isWifi) {
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

        val currentMode = settings?.dataSaverMode?.value ?: _dataSaverMode.value
        val context = applicationContext
        val isWifi = context?.let { NetworkUtils.isConnectedToWifi(it) } ?: false
        if (currentMode == DataSaverMode.ALWAYS_ON || (currentMode == DataSaverMode.AUTOMATIC && !isWifi)) return

        val isWifiOnly = settings?.isCanvasWifiOnly?.value ?: _isCanvasWifiOnly.value
        if (isWifiOnly && !isWifi) return

        val currentIdx = _playbackState.value.currentQueueIndex
        val nextTrack = _playbackState.value.queue.getOrNull(currentIdx + 1) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                spotifyCanvasRepository.getCanvasUrl(nextTrack.title, nextTrack.artist)
            } catch (_: Exception) {}
        }
    }

    private fun prefetchArtwork(track: Track) {
        val context = applicationContext ?: return
        val artFile = java.io.File(context.filesDir, "offline_audio/${track.id}.jpg")
        val url = if (artFile.exists()) {
            "file://${artFile.absolutePath}"
        } else {
            track.highResThumbnailUrl ?: track.thumbnailUrl
        } ?: return

        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            ImageLoader(context).enqueue(request)
        } catch (e: Exception) {
            Log.w("MusicViewModel", "Falha no pre-fetch de imagem para ${track.id}: ${e.message}")
        }
    }

    fun loadLyrics(track: Track) {
        lyricsFetchJob?.cancel()
        lyricsTranslationJob?.cancel()
        instrumentalFetchJob?.cancel()
        _currentLyrics.value = null
        _currentLyricsLine.value = null
        _karaokeSyncSnapshot.value = KaraokeSyncSnapshot()
        _isTranslatingLyrics.value = false
        _isInstrumentalTrackReady.value = false
        _isUsingRealInstrumental.value = false

        AudioPlayerService.onInstrumentalStatusChanged = { enabled, isReal ->
            _isUsingRealInstrumental.value = enabled && isReal
        }

        // Pré-busca em background a versão Instrumental / Karaokê Oficial para troca instantânea ao tocar no microfone
        prefetchStandbyInstrumentalForTrack(track)

        lyricsFetchJob = viewModelScope.launch {
            val res = lyricsRepository.getLyrics(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                durationSeconds = track.durationSeconds.toInt()
            )
            val data = res.getOrNull()
            val cleanData = data?.let { d ->
                val cleanPlain = if (d.plainLyrics.isNullOrBlank() || d.plainLyrics.trim().equals("null", ignoreCase = true)) null else d.plainLyrics.trim()
                val cleanSynced = d.syncedLines.filter { it.text.isNotBlank() && !it.text.trim().equals("null", ignoreCase = true) }
                val structuredPlainLines = if (cleanSynced.isEmpty() && cleanPlain != null) {
                    KaraokeLyricsEngine.buildPlainLinesWithPronunciation(cleanPlain)
                } else {
                    d.plainLines
                }
                if (cleanPlain == null && cleanSynced.isEmpty()) null else d.copy(
                    plainLyrics = cleanPlain,
                    syncedLines = cleanSynced,
                    plainLines = structuredPlainLines,
                    hasPronunciation = cleanSynced.any { !it.pronunciationText.isNullOrBlank() } ||
                        structuredPlainLines.any { !it.pronunciationText.isNullOrBlank() } ||
                        d.hasPronunciation
                )
            }
            _currentLyrics.value = cleanData
            updateCurrentLyricsLine(_playbackState.value.currentPositionMs)

            if (cleanData != null && (_showLyricsTranslation.value || _showLyricsPronunciation.value)) {
                ensureLyricsTranslationAndPronunciation(forceRetranslate = false)
            }
        }
    }

    private fun prefetchStandbyInstrumentalForTrack(track: Track) {
        if (track.id.isBlank() || track.id.startsWith("playlist_")) return
        val lowerTitle = track.title.lowercase()
        // Se a própria faixa já for instrumental, não precisa buscar outra
        if (lowerTitle.contains("instrumental") || lowerTitle.contains("karaoke") || lowerTitle.contains("playback")) return

        instrumentalFetchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val queries = KaraokeLyricsEngine.buildInstrumentalSearchQueries(track)
                val collectedCandidates = mutableListOf<Track>()

                for (query in queries) {
                    val songsRes = repository.search(query, SearchFilter.SONGS).getOrNull()?.songs.orEmpty()
                    val allRes = if (songsRes.isEmpty()) {
                        repository.search(query, SearchFilter.ALL).getOrNull()?.songs.orEmpty()
                    } else {
                        emptyList()
                    }
                    collectedCandidates.addAll(songsRes)
                    collectedCandidates.addAll(allRes)

                    val earlyMatch = KaraokeLyricsEngine.selectBestInstrumentalCandidate(track, collectedCandidates)
                    if (earlyMatch != null) {
                        break
                    }
                }

                val bestMatch = KaraokeLyricsEngine.selectBestInstrumentalCandidate(
                    originalTrack = track,
                    candidates = collectedCandidates.distinctBy { it.id }
                )

                if (bestMatch != null && _playbackState.value.currentTrack?.id == track.id) {
                    Log.i(
                        "MusicViewModel",
                        "Faixa Instrumental validada com alta precisão para '${track.title}' (${track.artist}): '${bestMatch.title}' por '${bestMatch.artist}' (${bestMatch.id})"
                    )
                    withContext(Dispatchers.Main) {
                        _isInstrumentalTrackReady.value = true
                        AudioPlayerService.prepareStandbyInstrumental(
                            originalTrackId = track.id,
                            instrumentalVideoId = bestMatch.id
                        )
                    }
                } else {
                    Log.i(
                        "MusicViewModel",
                        "Nenhuma faixa instrumental confiável encontrada para '${track.title}' (${track.artist}). Usando isolamento vocal DSP de fase preservada."
                    )
                }
            } catch (e: Exception) {
                Log.w("MusicViewModel", "Busca de instrumental em standby falhou: ${e.message}")
            }
        }
    }

    private fun updateCurrentLyricsLine(positionMs: Long) {
        val lyrics = _currentLyrics.value ?: run {
            _currentLyricsLine.value = null
            _karaokeSyncSnapshot.value = KaraokeSyncSnapshot()
            return
        }
        if (!lyrics.isSynced || lyrics.syncedLines.isEmpty()) {
            _currentLyricsLine.value = null
            _karaokeSyncSnapshot.value = KaraokeSyncSnapshot()
            return
        }
        val snapshot = KaraokeLyricsEngine.resolveSyncSnapshot(
            lyrics = lyrics,
            rawPositionMs = positionMs,
            offsetMs = _lyricsOffsetMs.value
        )
        _karaokeSyncSnapshot.value = snapshot

        val lines = lyrics.syncedLines
        val activeLine = snapshot.activeLineIndex.takeIf { it in lines.indices }?.let { lines[it] }
        val rawText = when {
            activeLine == null -> null
            _hideOriginalLyrics.value && _showLyricsTranslation.value && !activeLine.translatedText.isNullOrBlank() -> activeLine.translatedText
            _hideOriginalLyrics.value && _showLyricsPronunciation.value && !activeLine.pronunciationText.isNullOrBlank() -> activeLine.pronunciationText
            else -> activeLine.text
        }
        val cleanText = if (rawText.isNullOrBlank() || rawText.equals("null", ignoreCase = true)) null else rawText
        _currentLyricsLine.value = cleanText
    }

    // =========================================================================
    // CONTROLES DE KARAOKÊ, TRADUÇÃO E PRONÚNCIA
    // =========================================================================

    fun toggleKaraokeVocalIsolation() {
        val next = !_isKaraokeVocalIsolationEnabled.value
        _isKaraokeVocalIsolationEnabled.value = next
        AudioPlayerService.setKaraokeMode(next, _karaokeVocalLevel.value)
        _userMessage.value = when {
            next && _isInstrumentalTrackReady.value -> "Modo Karaokê ativado (Instrumental Oficial sincronizado)"
            next -> "Modo Karaokê ativado"
            else -> "Modo Karaokê desativado (voz original restaurada)"
        }
        if (jamSessionState.value.isInJam && jamSessionState.value.isHost && jamSessionState.value.roomSettings.syncKaraokeWithRoom) {
            jamSessionManager.broadcastHostStateSnapshot()
        }
    }

    fun setKaraokeVocalLevel(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        _karaokeVocalLevel.value = clamped
        if (!_isKaraokeVocalIsolationEnabled.value && clamped < 0.99f) {
            _isKaraokeVocalIsolationEnabled.value = true
        }
        AudioPlayerService.setKaraokeMode(_isKaraokeVocalIsolationEnabled.value, clamped)
        if (jamSessionState.value.isInJam && jamSessionState.value.isHost && jamSessionState.value.roomSettings.syncKaraokeWithRoom) {
            jamSessionManager.broadcastHostStateSnapshot()
        }
    }

    fun adjustLyricsOffset(deltaMs: Long) {
        val next = (_lyricsOffsetMs.value + deltaMs).coerceIn(-10_000L, 10_000L)
        _lyricsOffsetMs.value = next
        updateCurrentLyricsLine(_playbackState.value.currentPositionMs)
    }

    fun resetLyricsOffset() {
        _lyricsOffsetMs.value = 0L
        updateCurrentLyricsLine(_playbackState.value.currentPositionMs)
    }

    fun toggleLyricsTranslation() {
        val next = !_showLyricsTranslation.value
        _showLyricsTranslation.value = next
        if (!next && !_showLyricsPronunciation.value) {
            _hideOriginalLyrics.value = false
        }
        if (next) {
            ensureLyricsTranslationAndPronunciation(forceRetranslate = false)
        } else {
            updateCurrentLyricsLine(_playbackState.value.currentPositionMs)
        }
    }

    fun selectTranslationTargetLanguage(langCode: String) {
        val cleanCode = langCode.trim().ifEmpty { "auto" }
        val changed = _translationTargetLanguage.value != cleanCode
        _translationTargetLanguage.value = cleanCode
        if (!_showLyricsTranslation.value) {
            _showLyricsTranslation.value = true
        }
        ensureLyricsTranslationAndPronunciation(forceRetranslate = changed)
    }

    fun toggleLyricsPronunciation() {
        val next = !_showLyricsPronunciation.value
        _showLyricsPronunciation.value = next
        if (!next && !_showLyricsTranslation.value) {
            _hideOriginalLyrics.value = false
        }
        if (next) {
            ensureLyricsTranslationAndPronunciation(forceRetranslate = false)
        } else {
            updateCurrentLyricsLine(_playbackState.value.currentPositionMs)
        }
    }

    fun toggleHideOriginalLyrics() {
        val next = !_hideOriginalLyrics.value
        _hideOriginalLyrics.value = next
        updateCurrentLyricsLine(_playbackState.value.currentPositionMs)
    }

    private fun ensureLyricsTranslationAndPronunciation(forceRetranslate: Boolean = false) {
        val current = _currentLyrics.value ?: return
        val hasTranslatedSynced = current.syncedLines.isNotEmpty() &&
            current.syncedLines.any { !it.translatedText.isNullOrBlank() }
        val hasTranslatedPlain = current.plainLines.isNotEmpty() &&
            current.plainLines.any { !it.translatedText.isNullOrBlank() }
        if (!forceRetranslate && (hasTranslatedSynced || hasTranslatedPlain)) {
            updateCurrentLyricsLine(_playbackState.value.currentPositionMs)
            return
        }

        lyricsTranslationJob?.cancel()
        lyricsTranslationJob = viewModelScope.launch {
            _isTranslatingLyrics.value = true
            try {
                val targetLang = _translationTargetLanguage.value
                val enriched = KaraokeLyricsEngine.enrichLyricsWithTranslationAndPronunciation(
                    lyrics = current,
                    preferredTargetLang = targetLang
                )
                if (_currentLyrics.value?.trackId == current.trackId) {
                    _currentLyrics.value = enriched
                    updateCurrentLyricsLine(_playbackState.value.currentPositionMs)
                }
            } catch (e: Exception) {
                Log.w("MusicViewModel", "Falha ao traduzir letras: ${e.message}")
            } finally {
                _isTranslatingLyrics.value = false
            }
        }
    }

    // =========================================================================
    // CONTROLES DA JAM (ESCUTA SINCRONIZADA VIA WEBSOCKETS + DEEPLINKS)
    // =========================================================================

    fun openJamSheet() {
        _isJamSheetVisible.value = true
    }

    fun closeJamSheet() {
        _isJamSheetVisible.value = false
    }

    fun startHostJam(customRelayWsUrl: String? = null) {
        jamSessionManager.startHostSession(customRelayWsUrl)
    }

    fun joinJamFromInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            _userMessage.value = "Informe um link ou endereço WebSocket da JAM."
            return
        }
        jamSessionManager.joinFromUserInput(trimmed)
    }

    fun leaveJam() {
        jamSessionManager.leaveSession(silent = false)
    }

    fun updateJamDisplayName(name: String) {
        jamSessionManager.updateDisplayName(name)
    }

    fun setJamAllowGuestsToQueue(allowed: Boolean) {
        jamSessionManager.updateHostRoomSettings { it.copy(allowGuestsToQueue = allowed) }
    }

    fun setJamAllowGuestsToControlPlayback(allowed: Boolean) {
        jamSessionManager.updateHostRoomSettings { it.copy(allowGuestsToControlPlayback = allowed) }
    }

    fun setJamSyncKaraokeWithRoom(sync: Boolean) {
        jamSessionManager.updateHostRoomSettings {
            it.copy(
                syncKaraokeWithRoom = sync,
                isKaraokeActiveInRoom = _isKaraokeVocalIsolationEnabled.value,
                karaokeVocalLevelInRoom = _karaokeVocalLevel.value
            )
        }
    }

    fun kickJamParticipant(peerId: String) {
        jamSessionManager.kickParticipant(peerId)
    }

    fun requestJamAudioResync() {
        jamSessionManager.requestImmediateSyncFromHost()
    }

    fun handleDeepLink(uri: Uri) {
        val scheme = uri.scheme?.lowercase() ?: ""
        val host = uri.host?.lowercase() ?: ""
        val path = uri.path?.lowercase() ?: ""
        val isJamLink = (scheme == "nerfmusic" && host == "jam") ||
            ((scheme == "https" || scheme == "http") && (host.contains("nerfmusic") || path.startsWith("/jam")))

        if (isJamLink) {
            _isJamSheetVisible.value = true
            if (mediaController == null) {
                pendingDeepLinkUri = uri
                return
            }
            jamSessionManager.handleDeepLink(uri)
            return
        }

        // DeepLink para faixas musicais compartilhadas:
        // nerfmusic://track/{videoId}, https://nerfmusic.app/track/{videoId} ou https://music.youtube.com/watch?v={videoId}
        val isTrackLink = (scheme == "nerfmusic" && host == "track") ||
            ((scheme == "https" || scheme == "http") && (path.startsWith("/track") || host.contains("nerfmusic.app") || host.contains("music.youtube.com") || host.contains("youtube.com")))
        if (isTrackLink) {
            val videoId = uri.getQueryParameter("v")?.trim()
                ?: uri.lastPathSegment?.trim()?.takeIf { it != "track" && it != "watch" }
                ?: ""
            if (videoId.isNotBlank()) {
                openTrackById(videoId)
            }
        }
    }

    fun openTrackById(videoId: String) {
        viewModelScope.launch {
            val cachedTrack = trackMetadataMap[videoId]
            if (cachedTrack != null) {
                playTrack(cachedTrack)
                openFullPlayer()
                return@launch
            }

            _isLoading.value = true
            try {
                val radioRes = repository.getUpNextRadio(videoId = videoId, params = null)
                radioRes.onSuccess { upNext ->
                    val seed = upNext.tracks.firstOrNull { it.id == videoId }
                        ?: upNext.tracks.firstOrNull()
                        ?: Track(id = videoId, title = "Música Compartilhada", artist = "NERF Music")
                    trackMetadataMap[seed.id] = seed
                    playTrack(seed)
                    openFullPlayer()
                }.onFailure {
                    val fallback = Track(id = videoId, title = "Música Compartilhada", artist = "NERF Music")
                    trackMetadataMap[fallback.id] = fallback
                    playTrack(fallback)
                    openFullPlayer()
                }
            } catch (_: Exception) {
                val fallback = Track(id = videoId, title = "Música Compartilhada", artist = "NERF Music")
                trackMetadataMap[fallback.id] = fallback
                playTrack(fallback)
                openFullPlayer()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun applyRemoteJamSnapshot(snapshot: JamWireMessage, compensatedPosMs: Long) {
        if (isApplyingRemoteJamEvent) return
        isApplyingRemoteJamEvent = true
        try {
            val rs = snapshot.roomSettings
            // 0. Aplica configurações de áudio do Host (Crossfade, Automix, Gapless, RepeatMode e Karaokê compartilhado)
            AudioPlayerService.setJamAudioSettingsOverride(
                crossfadeSeconds = rs.hostCrossfadeSeconds,
                automixEnabled = rs.hostAutomixEnabled,
                gaplessEnabled = rs.hostGaplessEnabled
            )

            val targetRepeatMode = when (rs.repeatModeName.uppercase()) {
                "ALL" -> Player.REPEAT_MODE_ALL
                "ONE" -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            if (mediaController?.repeatMode != targetRepeatMode) {
                mediaController?.repeatMode = targetRepeatMode
            }
            if (_playbackState.value.repeatMode != targetRepeatMode) {
                _playbackState.value = _playbackState.value.copy(repeatMode = targetRepeatMode)
            }

            if (rs.syncKaraokeWithRoom) {
                if (_isKaraokeVocalIsolationEnabled.value != rs.isKaraokeActiveInRoom ||
                    kotlin.math.abs(_karaokeVocalLevel.value - rs.karaokeVocalLevelInRoom) > 0.02f
                ) {
                    _isKaraokeVocalIsolationEnabled.value = rs.isKaraokeActiveInRoom
                    _karaokeVocalLevel.value = rs.karaokeVocalLevelInRoom
                    AudioPlayerService.setKaraokeMode(rs.isKaraokeActiveInRoom, rs.karaokeVocalLevelInRoom)
                }
            }

            val remoteQueue = snapshot.queue
            val remoteIdx = snapshot.currentQueueIndex.coerceAtLeast(0)
            val remoteTrack = snapshot.currentTrack ?: remoteQueue.getOrNull(remoteIdx)
            val remotePlaying = snapshot.isPlaying

            val localState = _playbackState.value
            val localTrackId = localState.currentTrack?.id
            val localQueueIds = localState.queue.map { it.id }
            val remoteQueueIds = remoteQueue.map { it.id }

            // 1. Se a faixa atual mudou ou a fila mudou estruturalmente
            if (remoteTrack != null && remoteTrack.id != localTrackId) {
                playTrack(remoteTrack)
                viewModelScope.launch {
                    delay(450L)
                    isApplyingRemoteJamEvent = true
                    try {
                        if (compensatedPosMs > 1000L) {
                            mediaController?.seekTo(compensatedPosMs)
                        }
                        if (remotePlaying && mediaController?.isPlaying == false) {
                            mediaController?.play()
                        } else if (!remotePlaying && mediaController?.isPlaying == true) {
                            mediaController?.pause()
                        }
                        if (remoteQueue.isNotEmpty()) {
                            _playbackState.value = _playbackState.value.copy(
                                queue = remoteQueue,
                                currentQueueIndex = remoteIdx.coerceIn(0, (remoteQueue.size - 1).coerceAtLeast(0)),
                                repeatMode = targetRepeatMode
                            )
                        }
                    } finally {
                        isApplyingRemoteJamEvent = false
                    }
                }
                return
            }

            // 2. Sincroniza fila se apenas os próximos itens mudaram (e mantém timeline do ExoPlayer pronta para o Crossfade do Host)
            if (remoteQueue.isNotEmpty() && remoteQueueIds != localQueueIds) {
                remoteQueue.forEach { trackMetadataMap[it.id] = it }
                val safeIdx = remoteIdx.coerceIn(0, (remoteQueue.size - 1).coerceAtLeast(0))
                _playbackState.value = _playbackState.value.copy(
                    queue = remoteQueue,
                    currentQueueIndex = safeIdx,
                    hasNext = safeIdx < remoteQueue.size - 1,
                    hasPrevious = safeIdx > 0
                )
                val controller = mediaController
                if (controller != null && controller.mediaItemCount > 0) {
                    val curMediaIdx = controller.currentMediaItemIndex.coerceAtLeast(0)
                    val upcomingRemote = remoteQueue.drop(safeIdx + 1)
                    if (upcomingRemote.isNotEmpty()) {
                        if (controller.mediaItemCount > curMediaIdx + 1) {
                            controller.removeMediaItems(curMediaIdx + 1, controller.mediaItemCount)
                        }
                        controller.addMediaItems(upcomingRemote.map { trackToMediaItem(it) })
                    }
                }
            }

            // 3. Sincroniza estado Play / Pause
            val controller = mediaController
            if (controller != null) {
                if (remotePlaying && !controller.isPlaying) {
                    controller.play()
                    _playbackState.value = _playbackState.value.copy(isPlaying = true)
                } else if (!remotePlaying && controller.isPlaying) {
                    controller.pause()
                    _playbackState.value = _playbackState.value.copy(isPlaying = false)
                }

                // 4. Sincroniza posição se o desvio ultrapassar 350ms (compensado por RTT)
                val localPos = controller.currentPosition.coerceAtLeast(0L)
                val driftMs = kotlin.math.abs(localPos - compensatedPosMs)
                if (driftMs > 350L && compensatedPosMs >= 0L) {
                    controller.seekTo(compensatedPosMs)
                    _playbackState.value = _playbackState.value.copy(currentPositionMs = compensatedPosMs)
                    updateCurrentLyricsLine(compensatedPosMs)
                }
            }
        } finally {
            isApplyingRemoteJamEvent = false
        }
    }

    private fun executeGuestJamCommandOnHost(cmdMsg: JamWireMessage) {
        val cmd = cmdMsg.command ?: return
        when (cmd) {
            JamCommandType.PLAY -> {
                if (mediaController?.isPlaying == false) {
                    togglePlayPause()
                }
            }
            JamCommandType.PAUSE -> {
                if (mediaController?.isPlaying == true) {
                    togglePlayPause()
                }
            }
            JamCommandType.SEEK -> {
                val target = cmdMsg.targetPositionMs ?: return
                seekTo(target)
            }
            JamCommandType.SKIP_NEXT -> skipNext()
            JamCommandType.SKIP_PREV -> skipPrevious()
            JamCommandType.PLAY_TRACK -> {
                val track = cmdMsg.targetTrack ?: return
                playTrack(track)
            }
            JamCommandType.SELECT_QUEUE_INDEX -> {
                val idx = cmdMsg.targetQueueIndex ?: return
                playQueueItemAt(idx)
            }
            JamCommandType.ADD_TO_QUEUE -> {
                val tracksToAdd = if (cmdMsg.targetTracks.isNotEmpty()) {
                    cmdMsg.targetTracks
                } else {
                    listOfNotNull(cmdMsg.targetTrack)
                }
                if (tracksToAdd.isNotEmpty()) {
                    addTracksToQueue(tracksToAdd)
                }
            }
            JamCommandType.PLAY_NEXT_IN_QUEUE -> {
                val tracksToNext = if (cmdMsg.targetTracks.isNotEmpty()) {
                    cmdMsg.targetTracks
                } else {
                    listOfNotNull(cmdMsg.targetTrack)
                }
                if (tracksToNext.isNotEmpty()) {
                    addTracksToPlayNext(tracksToNext)
                }
            }
            JamCommandType.REMOVE_QUEUE_ITEM -> {
                val idx = cmdMsg.targetQueueIndex ?: return
                removeQueueItem(idx)
            }
            JamCommandType.MOVE_QUEUE_ITEM -> {
                val from = cmdMsg.fromQueueIndex ?: return
                val to = cmdMsg.toQueueIndex ?: return
                moveQueueItem(from, to)
            }
            JamCommandType.SET_REPEAT_MODE -> {
                val modeName = cmdMsg.targetRepeatModeName?.uppercase() ?: return
                val mode = when (modeName) {
                    "ALL" -> Player.REPEAT_MODE_ALL
                    "ONE" -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                setRepeatModeExplicit(mode)
            }
            JamCommandType.KICK_PARTICIPANT,
            JamCommandType.REQUEST_SYNC -> {}
        }
    }

    fun syncDownloadsWithDisk(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = libraryRepository?.syncDownloadsWithDisk() ?: 0
            withContext(Dispatchers.Main) {
                onComplete?.invoke(count)
            }
        }
    }

    override fun onCleared() {
        stopProgressTracker()
        radioFetchJob?.cancel()
        canvasFetchJob?.cancel()
        lyricsFetchJob?.cancel()
        lyricsTranslationJob?.cancel()
        jamSessionManager.leaveSession(silent = true)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
