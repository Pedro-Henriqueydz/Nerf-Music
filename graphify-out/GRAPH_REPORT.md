# Graph Report - Nerf Music  (2026-10-29)

## Corpus Check
- 126 files · ~148,577 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 59 file(s) not represented in the graph (top: .xml 44, .otf 5, (none) 2)

## Summary
- 2329 nodes · 5707 edges · 114 communities (55 shown, 59 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 100 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `dd2613f7`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MimeAndContainerDetectionStressTest.kt
- PlayerScreen.kt
- ArtistScreen
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- InnerTubeCatalogClientTest
- JamSessionManager
- SocialShareHelper.kt
- steps
- VocalIsolationAudioProcessor
- DownloadedTrackEntity
- HomeScreen.kt
- .syncTimelineState
- HomeSection
- LyricsData
- .isConnectedToWifi
- LibraryScreen
- Block3EmpiricalChallengeTest
- SpotifyCanvasRepositoryImpl
- DownloadedTrackDao
- UserSettingsRepository
- JamWireMessage
- AudioMetadataTagger
- LibraryRepositoryImpl
- Artist
- Registro de Alterações (Changelog) - NERF Music
- LoudnessNormalizationAudioProcessor
- OnboardingScreen.kt
- Track
- MigrationScreen.kt
- AudioCoreDownloadsCrossfadeQualityTest.kt
- SmartDownloadWorker.kt
- KaraokeAndJamTest.kt
- MusicRepository
- JamCommandType
- OnboardingFlowTest.kt
- FullPlayerSheet
- MainScreen.kt
- DownloadChunkRangeStressTest.kt
- JamSessionSheet
- WidgetRenderer.kt
- AudioQuality
- StreamQualitySelectorStressTest
- JamSessionManager.kt
- AudioPlayerService
- RadioTuningMode
- .loadHomeFeed
- .updateCurrentLyricsLine
- ArtistScreen.kt
- InnerTubeParserAdversarialStressTest
- SearchResult
- InnerTubeCatalogClient
- NERF Music
- UserHistoryRepository
- LoudnessStandard
- SearchScreen.kt
- SettingsSubScreen
- ShareCardInstrumentedTest.kt
- UserAffinityRepositoryImpl
- CrossfadeEmpiricalTest
- file
- AudioCoreDownloadsCrossfadeQualityTest
- NewPipeStreamResolver.kt
- DownloadAudioQuality
- JamWebSocketServer.kt
- Listener
- AppDatabase
- JamGlobalRelayClient
- ShareRules
- SocialShareHelper
- ArtworkColorsInstrumentedTest.kt
- InnerTubeStreamResolverTest
- NewPipeStreamResolver
- CompositeStreamResolver
- [1.3.2] - 2026-10-25
- TrackRelatedData
- .prefetchTrack
- InnerTubeStreamResolver
- Context
- README.md
- AudioMetadataTagger
- SearchResult
- .onCreate
- Adicionado
- UserAffinityRepository
- [1.4.0] - 2026-10-27 [RELEASED]
- ExploreScreen.kt
- LibraryFilter
- DataSaverInterceptor.kt
- AlbumScreen
- PlaylistOptionsBottomSheet
- NERF Music — widget e prévia de compartilhamento
- LibrarySortOption
- Style
- [1.3.3] - 2026-10-25
- StreamResolver
- StreamResolver

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 208 edges
2. `Track` - 164 edges
3. `AudioPlayerService` - 74 edges
4. `InnerTubeCatalogClient` - 55 edges
5. `LibraryRepositoryImpl` - 52 edges
6. `AudioQuality` - 48 edges
7. `MigrationManager` - 46 edges
8. `UserSettingsRepositoryImpl` - 42 edges
9. `JamSessionManager` - 36 edges
10. `UserHistoryRepositoryImpl` - 32 edges

## Surprising Connections (you probably didn't know these)
- `Adicionado` --references--> `AudioMetadataTagger`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/AudioMetadataTagger.kt
- `Adicionado` --references--> `ConnectivityObserver`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/ConnectivityObserver.kt
- `Adicionado` --references--> `MainActivity`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/MainActivity.kt
- `Adicionado` --references--> `HomeFeedSynthesizer`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/HomeFeedSynthesizer.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt

## Import Cycles
- None detected.

## Communities (114 total, 59 thin omitted)

### Community 0 - "MimeAndContainerDetectionStressTest.kt"
Cohesion: 0.12
Nodes (4): MimeAndContainerDetectionStressTest, ContextWrapper, InvocationHandler, StreamResolver

### Community 1 - "PlayerScreen.kt"
Cohesion: 0.02
Nodes (3): largestFittingCharacterBoundary(), BroadcastReceiver, splitInlineLyricIntoSegments()

### Community 2 - "ArtistScreen"
Cohesion: 0.61
Nodes (6): ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow(), ArtistVideoCard()

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.20
Nodes (23): AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), LoudnessNormalizationBottomSheet(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SettingsMainContent() (+15 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 12 - "DownloadedTrackEntity"
Cohesion: 0.18
Nodes (4): DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 13 - "HomeScreen.kt"
Cohesion: 0.16
Nodes (14): ExploreScreen(), HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel() (+6 more)

### Community 15 - "HomeSection"
Cohesion: 0.17
Nodes (12): AlbumDetail, HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID (+4 more)

### Community 16 - "LyricsData"
Cohesion: 0.06
Nodes (14): LyricsData, SyncedLyricsLine, SyncedLyricsWord, LyricsRepository, LyricsRepositoryImpl, optCleanString(), BatchTranslationResult, KaraokeLyricsEngine (+6 more)

### Community 18 - "LibraryScreen"
Cohesion: 0.26
Nodes (9): LibraryGridCard(), LibraryListItem(), LibraryScreen(), NewBottomSheetOption(), PlaylistCollageCover(), TrackListDetailView(), UserPlaylistDetailView(), SaveToPlaylistSheet() (+1 more)

### Community 19 - "Block3EmpiricalChallengeTest"
Cohesion: 0.07
Nodes (19): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+11 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 22 - "UserSettingsRepository"
Cohesion: 0.10
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepository

### Community 23 - "JamWireMessage"
Cohesion: 0.15
Nodes (18): JamConnectionStatus, CONNECTED, CONNECTING, DISCONNECTED, ERROR, RECONNECTING, JamMessageType, COMMAND (+10 more)

### Community 24 - "AudioMetadataTagger"
Cohesion: 0.11
Nodes (5): AudioMetadataTaggerTest, [1.3.5] - 2026-10-27 [RELEASED], Adicionado, Corrigido, Testes

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.11
Nodes (18): [1.0.8] - 2026-10-23, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.4] - 2026-10-25, [1.4.3] - 2026-10-29 [RELEASED], [1.4.4] - 2026-10-29 [RELEASED], [1.4.5] - 2026-10-29 [RELEASED], Adicionado (+10 more)

### Community 28 - "LoudnessNormalizationAudioProcessor"
Cohesion: 0.12
Nodes (3): Biquad, LoudnessNormalizationAudioProcessor, LoudnessNormalizationAudioProcessorTest

### Community 29 - "OnboardingScreen.kt"
Cohesion: 0.05
Nodes (7): AnimatedEqualizerBars(), applePressFeedback(), AppleArtistSelectionStep(), AppleArtistSphereCard(), AppleGlassFeatureCard(), AppleWelcomeStep(), OnboardingScreen()

### Community 30 - "Track"
Cohesion: 0.10
Nodes (3): Track, UserPlaylist, LibraryRepository

### Community 32 - "MigrationScreen.kt"
Cohesion: 0.11
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 34 - "SmartDownloadWorker.kt"
Cohesion: 0.05
Nodes (6): MainActivity, NERFMusicTheme(), UpdateCheckWorker, UpdateNotificationHelper, SmartDownloadNotificationHelper, SmartDownloadWorker

### Community 36 - "MusicRepository"
Cohesion: 0.27
Nodes (3): QueueFilterChip, UpNextResult, MusicRepository

### Community 37 - "JamCommandType"
Cohesion: 0.12
Nodes (15): JamCommandType, ADD_TO_QUEUE, KICK_PARTICIPANT, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK (+7 more)

### Community 38 - "OnboardingFlowTest.kt"
Cohesion: 0.08
Nodes (4): UserHistoryRepositoryImpl, OnboardingFlowTest, ContextWrapper, InvocationHandler

### Community 39 - "FullPlayerSheet"
Cohesion: 0.07
Nodes (22): PlaybackState, JamPersonWavesIcon(), AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar(), ApplePopoverOptionRow(), formatDurationMs(), FullPlayerSheet() (+14 more)

### Community 40 - "MainScreen.kt"
Cohesion: 0.09
Nodes (7): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar()

### Community 41 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.11
Nodes (3): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler

### Community 42 - "JamSessionSheet"
Cohesion: 0.21
Nodes (8): JamSessionSheet(), JamSettingToggleRow(), JamVisualMatrixBadge(), [1.4.1] - 2026-10-27 [RELEASED], [1.4.2] - 2026-10-28 [RELEASED], Adicionado, Adicionado, Aprimorado & Corrigido

### Community 43 - "WidgetRenderer.kt"
Cohesion: 0.09
Nodes (7): ExpandedPlayerWidgetProvider, onReceive(), Size, FOUR, THREE, TWO, WidgetRenderer

### Community 44 - "AudioQuality"
Cohesion: 0.08
Nodes (7): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, UserSettingsRepositoryImpl

### Community 48 - "RadioTuningMode"
Cohesion: 0.33
Nodes (5): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR

### Community 56 - "NERF Music"
Cohesion: 0.25
Nodes (8): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão

### Community 58 - "LoudnessStandard"
Cohesion: 0.17
Nodes (7): LoudnessStandard, APPLE_MUSIC, BROADCAST_EBU, CD_LOUD, CD_STANDARD, OFF, SPOTIFY

### Community 59 - "SearchScreen.kt"
Cohesion: 0.13
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 60 - "SettingsSubScreen"
Cohesion: 0.20
Nodes (10): SearchableSetting, SettingsSubScreen, ABOUT, CONTENT_DISPLAY, DATA_SAVER, MAIN, MEDIA_QUALITY, MIGRATION (+2 more)

### Community 68 - "AudioCoreDownloadsCrossfadeQualityTest"
Cohesion: 0.21
Nodes (3): AudioCoreDownloadsCrossfadeQualityTest, StreamResolver, StreamResolver

### Community 74 - "DownloadAudioQuality"
Cohesion: 0.11
Nodes (7): SmartDownloadManager, SmartDownloadSyncState, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 81 - "JamGlobalRelayClient"
Cohesion: 0.08
Nodes (4): JamGlobalRelayClient, WebSocketListener, NetworkConnectivityObserver, NetworkCallback

### Community 84 - "ArtworkColorsInstrumentedTest.kt"
Cohesion: 0.21
Nodes (3): ArtworkColorsInstrumentedTest, ArtworkColors, Colors

### Community 87 - "CompositeStreamResolver"
Cohesion: 0.15
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 88 - "[1.3.2] - 2026-10-25"
Cohesion: 0.50
Nodes (4): [1.3.2] - 2026-10-25, Corrigido, Segurança, Testes

### Community 89 - "TrackRelatedData"
Cohesion: 0.42
Nodes (3): RelatedSection, TrackRelatedData, RelatedMultiTierFallbackTest

### Community 91 - "InnerTubeStreamResolver"
Cohesion: 0.22
Nodes (3): CandidateFormat, ClientConfig, InnerTubeStreamResolver

### Community 93 - "README.md"
Cohesion: 0.33
Nodes (3): Correções, Informações da compilação, NERF Music v1.4.5 — Correção do widget expandido

### Community 96 - "SearchResult"
Cohesion: 0.19
Nodes (4): SearchFilter, ALL, SONGS, SearchResult

### Community 101 - "[1.4.0] - 2026-10-27 [RELEASED]"
Cohesion: 0.67
Nodes (3): [1.4.0] - 2026-10-27 [RELEASED], Adicionado, Adicionado

### Community 103 - "LibraryFilter"
Cohesion: 0.29
Nodes (7): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS

### Community 105 - "AlbumScreen"
Cohesion: 0.80
Nodes (3): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow()

### Community 108 - "NERF Music — widget e prévia de compartilhamento"
Cohesion: 0.40
Nodes (4): NERF Music — widget e prévia de compartilhamento, Prévia de compartilhamento, Revisão visual, Widget expandido

### Community 109 - "LibrarySortOption"
Cohesion: 0.50
Nodes (4): LibrarySortOption, A_TO_Z, RECENT, RECENTLY_ADDED

### Community 110 - "Style"
Cohesion: 0.50
Nodes (4): Style, FULL_COVER, GLASS, MINIMAL

### Community 111 - "[1.3.3] - 2026-10-25"
Cohesion: 0.67
Nodes (3): [1.3.3] - 2026-10-25, Corrigido, Melhorado

## Knowledge Gaps
- **92 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+87 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 554 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **59 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Track` connect `Track` to `PlayerScreen.kt`, `ArtistScreen`, `MigrationManager`, `MusicViewModel`, `InnerTubeCatalogClientTest`, `JamSessionManager`, `SocialShareHelper.kt`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `HomeSection`, `LyricsData`, `.isConnectedToWifi`, `LibraryScreen`, `JamWireMessage`, `AudioMetadataTagger`, `LibraryRepositoryImpl`, `Artist`, `SharePreviewActivity.kt`, `KaraokeAndJamTest.kt`, `MusicRepository`, `OnboardingFlowTest.kt`, `FullPlayerSheet`, `MainScreen.kt`, `DownloadChunkRangeStressTest.kt`, `WidgetRenderer.kt`, `JamSessionManager.kt`, `RadioTuningMode`, `ArtistScreen.kt`, `SearchResult`, `InnerTubeCatalogClient`, `.initializePlayerController`, `UserHistoryRepository`, `SearchScreen.kt`, `ShareCardInstrumentedTest.kt`, `UserAffinityRepositoryImpl`, `.loadLyrics`, `LibraryRepository.kt`, `DownloadAudioQuality`, `InnerTubeCatalogClient.kt`, `MusicViewModel.kt`, `SocialShareHelper`, `TrackRelatedData`, `AudioMetadataTagger`, `.triggerPrefetchNextTrack`, `SearchResult`, `.onCreate`, `UserAffinityRepository`, `AlbumScreen`?**
  _High betweenness centrality (0.236) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AudioPlayerService` (e.g. with `VocalIsolationAudioProcessor` and `Corrigido`) actually correct?**
  _`AudioPlayerService` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _92 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `MimeAndContainerDetectionStressTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.11954022988505747 - nodes in this community are weakly interconnected._
- **Why does `MusicViewModel` connect `MusicViewModel` to `ArtistScreen`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `JamSessionManager`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `HomeSection`, `LyricsData`, `.isConnectedToWifi`, `LibraryScreen`, `Block3EmpiricalChallengeTest`, `SpotifyCanvasRepositoryImpl`, `UserSettingsRepository`, `JamWireMessage`, `Artist`, `Track`, `MigrationScreen.kt`, `SmartDownloadWorker.kt`, `MusicRepository`, `FullPlayerSheet`, `MainScreen.kt`, `AudioQuality`, `JamSessionManager.kt`, `RadioTuningMode`, `.loadHomeFeed`, `.updateCurrentLyricsLine`, `ArtistScreen.kt`, `.initializePlayerController`, `UserHistoryRepository`, `LoudnessStandard`, `SearchScreen.kt`, `file`, `.clearStreamCache`, `.loadLyrics`, `DownloadAudioQuality`, `MusicViewModel.kt`, `NewPipeStreamResolver`, `TrackRelatedData`, `.prefetchTrack`, `SearchResult`, `UserAffinityRepository`, `.setKaraokeMode`, `ExploreScreen.kt`, `AlbumScreen`?**
  _High betweenness centrality (0.220) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.022893772893772892 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `MimeAndContainerDetectionStressTest.kt`, `SettingsScreen.kt`, `MusicViewModel`, `Block3EmpiricalChallengeTest`, `UserSettingsRepository`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `DownloadChunkRangeStressTest.kt`, `.clearStreamCache`, `AudioCoreDownloadsCrossfadeQualityTest`, `NewPipeStreamResolver.kt`, `InnerTubeCatalogClient.kt`, `MusicViewModel.kt`, `InnerTubeStreamResolverTest`, `NewPipeStreamResolver`, `CompositeStreamResolver`, `.prefetchTrack`, `InnerTubeStreamResolver`, `StreamResolver`, `StreamResolver`?**
  _High betweenness centrality (0.076) - this node is a cross-community bridge._