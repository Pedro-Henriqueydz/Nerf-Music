# Graph Report - Nerf Music  (2026-10-30)

## Corpus Check
- 140 files · ~160,015 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 59 file(s) not represented in the graph (top: .xml 44, .otf 5, (none) 2)

## Summary
- 2591 nodes · 6549 edges · 115 communities (56 shown, 59 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 71 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1cd70e3c`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DownloadChunkRangeStressTest.kt
- PlayerScreen.kt
- RecapScreen.kt
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- InnerTubeCatalogClientTest
- JamSessionManager
- SocialShareHelper.kt
- steps
- VocalIsolationAudioProcessor
- InnerTubeCatalogClient
- HomeScreen
- .syncTimelineState
- Block3EmpiricalChallengeTest
- KaraokeLyricsEngine
- DownloadedTrackDao
- LibraryScreen
- LibraryRepository
- SpotifyCanvasRepositoryImpl
- DownloadedTrackEntity
- UserSettingsRepository
- .rank
- AudioMetadataTagger
- LibraryRepositoryImpl
- .setUp
- MainScreen
- LoudnessNormalizationAudioProcessor
- OnboardingScreen.kt
- .isConnectedToWifi
- RecapVideoExporter.kt
- MigrationScreen.kt
- JamWireMessage
- SmartDownloadWorker.kt
- UserHistoryRepositoryImpl
- MusicRepository
- LyricsRepositoryImpl
- HomeSectionStyle
- FullPlayerSheet
- UserHistoryRepositoryImpl
- LyricsNullSanitizationStressTest
- .update
- UserSettingsRepositoryImpl
- LyricsData
- JamCommandType
- AudioPlayerService
- SearchFilter
- .loadHomeFeed
- .updateCurrentLyricsLine
- InnerTubeParserAdversarialStressTest
- SearchResult
- LibraryRepository.kt
- MusicViewModel.kt
- NERF Music
- HomeSection
- NewPipeStreamResolver.kt
- ShareCardInstrumentedTest.kt
- SettingsSubScreen
- MimeAndContainerDetectionStressTest
- CrossfadeEmpiricalTest
- optCleanString
- AudioCoreDownloadsCrossfadeQualityTest.kt
- DataSaverInterceptor.kt
- .syncNow
- DownloadAudioQuality
- DownloadChunkRangeStressTest.kt
- InnerTubeCatalogClient
- JamWebSocketServer.kt
- .applyKaraokeState
- log
- JamGlobalRelayClient
- ShareRules
- SocialShareHelper
- .extract
- InnerTubeStreamResolver
- LoudnessStandard
- AudioQuality
- SearchScreen
- KaraokeAndJamTest.kt
- RadioTuningMode
- SharePreviewSheet
- Context
- .onCreate
- .updateMediaNotificationLayout
- Listener
- Artist
- TrackRelatedData
- NestedScrollConnection
- Track
- ContextWrapper
- LibraryFilter
- ContextWrapper
- NewPipeStreamResolverTest.kt
- ExploreScreen
- Screen
- NERF Music — widget e prévia de compartilhamento
- TrackOptionsBottomSheet
- NERF Music 1.5.0
- SmoothFlingBehavior.kt
- AlbumScreen
- KaraokeAndJamTest
- LibrarySortOption

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 225 edges
2. `Track` - 191 edges
3. `AudioPlayerService` - 80 edges
4. `InnerTubeCatalogClient` - 62 edges
5. `LibraryRepositoryImpl` - 51 edges
6. `AudioQuality` - 48 edges
7. `MigrationManager` - 46 edges
8. `HomeSection` - 43 edges
9. `UserSettingsRepositoryImpl` - 42 edges
10. `UserHistoryRepositoryImpl` - 36 edges

## Surprising Connections (you probably didn't know these)
- `AudioPlayerService` --calls--> `VocalIsolationAudioProcessor`  [INFERRED]
  app/src/main/java/com/nerf/music/player/AudioPlayerService.kt → app/src/main/java/com/nerf/music/player/VocalIsolationAudioProcessor.kt
- `createDefaultStreamResolver()` --calls--> `InnerTubeStreamResolver`  [INFERRED]
  app/src/main/java/com/nerf/music/player/CompositeStreamResolver.kt → app/src/main/java/com/nerf/music/player/InnerTubeStreamResolver.kt
- `createDefaultStreamResolver()` --calls--> `NewPipeStreamResolver`  [INFERRED]
  app/src/main/java/com/nerf/music/player/CompositeStreamResolver.kt → app/src/main/java/com/nerf/music/player/NewPipeStreamResolver.kt
- `AnimatedEqualizerBars()` --calls--> `Size`  [INFERRED]
  app/src/main/java/com/nerf/music/ui/components/AnimatedEqualizerBars.kt → app/src/main/java/com/nerf/music/widget/WidgetRenderer.kt
- `JamPersonWavesIcon()` --calls--> `Size`  [INFERRED]
  app/src/main/java/com/nerf/music/ui/components/JamPersonWavesIcon.kt → app/src/main/java/com/nerf/music/widget/WidgetRenderer.kt

## Import Cycles
- None detected.

## Communities (115 total, 59 thin omitted)

### Community 0 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.13
Nodes (3): DownloadChunkRangeStressTest, InvocationHandler, StreamResolver

### Community 2 - "RecapScreen.kt"
Cohesion: 0.14
Nodes (19): RecapArtistStats, RecapSummary, RecapTrackStats, ArtistRow(), EmptyRecap(), formatRecapPlayCount(), formattedRecapDuration(), LegacyRecapStoryPage() (+11 more)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (4): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.20
Nodes (23): AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), LoudnessNormalizationBottomSheet(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting (+15 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.07
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 10 - "steps"
Cohesion: 0.05
Nodes (39): if, runs-on, steps, GH_REPO, GH_TOKEN, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64 (+31 more)

### Community 13 - "HomeScreen"
Cohesion: 0.32
Nodes (14): HomeScreen(), HomeSongCard(), HomeSpeedDialPlaybackSection(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel() (+6 more)

### Community 15 - "Block3EmpiricalChallengeTest"
Cohesion: 0.07
Nodes (19): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+11 more)

### Community 16 - "KaraokeLyricsEngine"
Cohesion: 0.21
Nodes (3): BatchTranslationResult, KaraokeLyricsEngine, TranslationLanguageOption

### Community 18 - "LibraryScreen"
Cohesion: 0.23
Nodes (9): LibraryGridCard(), LibraryListItem(), LibraryScreen(), NewBottomSheetOption(), PlaylistCollageCover(), TrackListDetailView(), UserPlaylistDetailView(), PlaylistOptionItem() (+1 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 21 - "DownloadedTrackEntity"
Cohesion: 0.18
Nodes (4): DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 22 - "UserSettingsRepository"
Cohesion: 0.10
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepository

### Community 23 - ".rank"
Cohesion: 0.10
Nodes (6): ExploreRecommendations, RecapCalendar, RecapCalendarState, ReleaseRecommendations, ExploreRecommendationsTest, ReleaseRecommendationsTest

### Community 24 - "AudioMetadataTagger"
Cohesion: 0.13
Nodes (3): AudioMetadataTagger, ExtractedMetadata, AudioMetadataTaggerTest

### Community 26 - ".setUp"
Cohesion: 0.13
Nodes (3): ContextWrapper, InvocationHandler, StreamResolver

### Community 27 - "MainScreen"
Cohesion: 0.23
Nodes (10): ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow(), ArtistVideoCard(), MainScreen(), YtmPivotBar() (+2 more)

### Community 28 - "LoudnessNormalizationAudioProcessor"
Cohesion: 0.08
Nodes (3): Biquad, LoudnessNormalizationAudioProcessor, LoudnessNormalizationAudioProcessorTest

### Community 29 - "OnboardingScreen.kt"
Cohesion: 0.04
Nodes (13): AnimatedEqualizerBars(), applePressFeedback(), AppleArtistSelectionStep(), AppleArtistSphereCard(), AppleGlassFeatureCard(), AppleWelcomeStep(), OnboardingScreen(), artistListeningShares() (+5 more)

### Community 32 - "MigrationScreen.kt"
Cohesion: 0.12
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 33 - "JamWireMessage"
Cohesion: 0.15
Nodes (18): JamConnectionStatus, CONNECTED, CONNECTING, DISCONNECTED, ERROR, RECONNECTING, JamMessageType, COMMAND (+10 more)

### Community 34 - "SmartDownloadWorker.kt"
Cohesion: 0.07
Nodes (5): MainActivity, NERFMusicTheme(), UpdateCheckWorker, UpdateNotificationHelper, SmartDownloadWorker

### Community 38 - "HomeSectionStyle"
Cohesion: 0.13
Nodes (11): HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL (+3 more)

### Community 39 - "FullPlayerSheet"
Cohesion: 0.06
Nodes (28): PlaybackState, JamPersonWavesIcon(), JamSessionSheet(), JamSettingToggleRow(), JamVisualMatrixBadge(), AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar() (+20 more)

### Community 41 - "UserHistoryRepositoryImpl"
Cohesion: 0.09
Nodes (4): UserAffinityRepositoryImpl, OnboardingFlowTest, ContextWrapper, InvocationHandler

### Community 43 - ".update"
Cohesion: 0.07
Nodes (8): CompactPlayerWidgetProvider, ExpandedPlayerWidgetProvider, ThreeByOneWidgetProvider, Size, FOUR, THREE, TWO, WidgetRenderer

### Community 44 - "UserSettingsRepositoryImpl"
Cohesion: 0.08
Nodes (7): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, UserSettingsRepositoryImpl

### Community 45 - "LyricsData"
Cohesion: 0.21
Nodes (3): LyricsData, SyncedLyricsLine, SyncedLyricsWord

### Community 46 - "JamCommandType"
Cohesion: 0.12
Nodes (15): JamCommandType, ADD_TO_QUEUE, KICK_PARTICIPANT, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK (+7 more)

### Community 48 - "SearchFilter"
Cohesion: 0.33
Nodes (5): SearchFilter, ALBUMS, ALL, ARTISTS, SONGS

### Community 56 - "NERF Music"
Cohesion: 0.22
Nodes (8): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão

### Community 58 - "NewPipeStreamResolver.kt"
Cohesion: 0.11
Nodes (3): NerfMusicApp, NewPipeStreamResolver, OkHttpDownloader

### Community 60 - "SettingsSubScreen"
Cohesion: 0.22
Nodes (9): SettingsSubScreen, ABOUT, CONTENT_DISPLAY, DATA_SAVER, MAIN, MEDIA_QUALITY, MIGRATION, PLAYBACK (+1 more)

### Community 68 - "AudioCoreDownloadsCrossfadeQualityTest.kt"
Cohesion: 0.18
Nodes (3): AudioCoreDownloadsCrossfadeQualityTest, StreamResolver, StreamResolver

### Community 74 - "DownloadAudioQuality"
Cohesion: 0.12
Nodes (7): SmartDownloadManager, SmartDownloadSyncState, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 80 - "log"
Cohesion: 0.06
Nodes (14): AnnualTrackStatsEntity, AppDatabase, AnnualTrackStatsWithTrack, LibraryTrackDao, LibraryTrackEntity, BackupPreferenceEntry, BackupRepository, BackupRestoreSummary (+6 more)

### Community 81 - "JamGlobalRelayClient"
Cohesion: 0.08
Nodes (4): JamGlobalRelayClient, WebSocketListener, NetworkConnectivityObserver, NetworkCallback

### Community 83 - "SocialShareHelper"
Cohesion: 0.26
Nodes (6): PaletteColors, SocialShareHelper, Style, FULL_COVER, GLASS, MINIMAL

### Community 84 - ".extract"
Cohesion: 0.23
Nodes (3): ArtworkColorsInstrumentedTest, ArtworkColors, Colors

### Community 85 - "InnerTubeStreamResolver"
Cohesion: 0.11
Nodes (5): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest, StreamQualitySelectorStressTest

### Community 86 - "LoudnessStandard"
Cohesion: 0.17
Nodes (7): LoudnessStandard, APPLE_MUSIC, BROADCAST_EBU, CD_LOUD, CD_STANDARD, OFF, SPOTIFY

### Community 87 - "AudioQuality"
Cohesion: 0.15
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 88 - "SearchScreen"
Cohesion: 0.67
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 90 - "RadioTuningMode"
Cohesion: 0.33
Nodes (5): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR

### Community 91 - "SharePreviewSheet"
Cohesion: 0.33
Nodes (7): CircleAction(), DividerLine(), SectionLabel(), ShareAction(), ShareActionModern(), SharePreviewSheet(), StyleOption()

### Community 96 - "Artist"
Cohesion: 0.16
Nodes (5): AlbumDetail, ArtistDetail, SearchResult, QueueFilterChip, UpNextResult

### Community 97 - "TrackRelatedData"
Cohesion: 0.35
Nodes (3): RelatedSection, TrackRelatedData, RelatedMultiTierFallbackTest

### Community 99 - "Track"
Cohesion: 0.13
Nodes (3): Track, UserAffinityRepository, SmartRadioEngine

### Community 103 - "LibraryFilter"
Cohesion: 0.29
Nodes (7): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS

### Community 106 - "ExploreScreen"
Cohesion: 0.53
Nodes (3): ExploreMotionItem(), ExploreScreen(), YtmExploreQuickButton()

### Community 107 - "Screen"
Cohesion: 0.33
Nodes (5): Explore, Home, Library, Screen, Search

### Community 108 - "NERF Music — widget e prévia de compartilhamento"
Cohesion: 0.40
Nodes (4): NERF Music — widget e prévia de compartilhamento, Prévia de compartilhamento, Revisão visual, Widget expandido

### Community 110 - "NERF Music 1.5.0"
Cohesion: 0.33
Nodes (5): Adicionado, Alterado e corrigido, NERF Music 1.5.0, Notas técnicas da versão, Seu ano com música está aqui 🎧

### Community 112 - "AlbumScreen"
Cohesion: 0.80
Nodes (3): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow()

### Community 114 - "LibrarySortOption"
Cohesion: 0.50
Nodes (4): LibrarySortOption, A_TO_Z, RECENT, RECENTLY_ADDED

## Knowledge Gaps
- **94 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+89 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 574 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **59 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Track` connect `Track` to `DownloadChunkRangeStressTest.kt`, `PlayerScreen.kt`, `RecapScreen.kt`, `MigrationManager`, `MusicViewModel`, `InnerTubeCatalogClientTest`, `JamSessionManager`, `SocialShareHelper.kt`, `InnerTubeCatalogClient`, `HomeScreen`, `.syncTimelineState`, `KaraokeLyricsEngine`, `LibraryScreen`, `LibraryRepository`, `DownloadedTrackEntity`, `.rank`, `AudioMetadataTagger`, `LibraryRepositoryImpl`, `MainScreen`, `.isConnectedToWifi`, `JamWireMessage`, `UserHistoryRepositoryImpl`, `HomeSectionStyle`, `FullPlayerSheet`, `SharePreviewActivity.kt`, `UserHistoryRepositoryImpl`, `.update`, `LyricsData`, `AudioPlayerService`, `.loadHomeFeed`, `HomeScreen.kt`, `SearchResult`, `LibraryRepository.kt`, `MusicViewModel.kt`, `HomeSection`, `ShareCardInstrumentedTest.kt`, `.loadLyrics`, `DownloadAudioQuality`, `DownloadChunkRangeStressTest.kt`, `InnerTubeCatalogClient`, `AudioPlayerService.kt`, `log`, `SocialShareHelper`, `SearchScreen`, `KaraokeAndJamTest.kt`, `RadioTuningMode`, `SharePreviewSheet`, `.updateMediaNotificationLayout`, `Artist`, `TrackRelatedData`, `.playTrackList`, `TrackOptionsBottomSheet`, `AlbumScreen`, `KaraokeAndJamTest`?**
  _High betweenness centrality (0.225) - this node is a cross-community bridge._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _94 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DownloadChunkRangeStressTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.13450292397660818 - nodes in this community are weakly interconnected._
- **Why does `MusicViewModel` connect `MusicViewModel` to `RecapScreen.kt`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `JamSessionManager`, `InnerTubeCatalogClient`, `HomeScreen`, `.syncTimelineState`, `Block3EmpiricalChallengeTest`, `LibraryScreen`, `LibraryRepository`, `SpotifyCanvasRepositoryImpl`, `DownloadedTrackEntity`, `UserSettingsRepository`, `.rank`, `MainScreen`, `OnboardingScreen.kt`, `.isConnectedToWifi`, `MigrationScreen.kt`, `JamWireMessage`, `SmartDownloadWorker.kt`, `MusicRepository`, `LyricsRepositoryImpl`, `HomeSectionStyle`, `FullPlayerSheet`, `SharePreviewActivity.kt`, `UserSettingsRepositoryImpl`, `LyricsData`, `AudioPlayerService`, `SearchFilter`, `.loadHomeFeed`, `.updateCurrentLyricsLine`, `HomeScreen.kt`, `LibraryRepository.kt`, `MusicViewModel.kt`, `HomeSection`, `NewPipeStreamResolver.kt`, `.clearStreamCache`, `.loadLyrics`, `DownloadAudioQuality`, `AudioPlayerService.kt`, `.applyKaraokeState`, `log`, `LoudnessStandard`, `SearchScreen`, `RadioTuningMode`, `Artist`, `TrackRelatedData`, `Track`, `.playTrackList`, `ExploreScreen`, `AlbumScreen`, `KaraokeAndJamTest`?**
  _High betweenness centrality (0.204) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.024269759450171822 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `UserSettingsRepositoryImpl` to `DownloadChunkRangeStressTest.kt`, `.clearStreamCache`, `.setUp`, `SettingsScreen.kt`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `MusicViewModel`, `DownloadAudioQuality`, `DownloadChunkRangeStressTest.kt`, `LyricsData`, `AudioPlayerService.kt`, `AudioPlayerService`, `InnerTubeStreamResolver`, `UserSettingsRepository`, `AudioQuality`, `LibraryRepository.kt`, `MusicViewModel.kt`, `NewPipeStreamResolver.kt`?**
  _High betweenness centrality (0.077) - this node is a cross-community bridge._
- **Should `RecapScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.1443850267379679 - nodes in this community are weakly interconnected._