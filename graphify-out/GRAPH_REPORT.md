# Graph Report - Nerf Music  (2026-10-28)

## Corpus Check
- 110 files · ~135,668 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 2113 nodes · 5143 edges · 90 communities (45 shown, 45 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 105 edges (avg confidence: 0.9)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c0a080a0`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- FullPlayerSheet
- JamSessionSheet.kt
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- InnerTubeCatalogClientTest
- JamSessionManager
- AudioMetadataTagger.kt
- steps
- DownloadChunkRangeStressTest.kt
- DownloadedTrackEntity
- HomeScreen.kt
- .syncTimelineState
- SearchScreen.kt
- MainScreen.kt
- LibraryScreen.kt
- LyricsData
- SpotifyCanvasRepositoryImpl
- KaraokeLyricsEngine
- OkHttpDownloader.kt
- Track
- AudioMetadataTagger
- LibraryRepositoryImpl
- MusicViewModel.kt
- Registro de Alterações (Changelog) - NERF Music
- UserSettingsRepository
- OnboardingScreen.kt
- UserAffinityRepositoryImpl
- AudioCoreDownloadsCrossfadeQualityTest
- MigrationScreen
- LibraryRepository.kt
- SmartDownloadWorker.kt
- AudioCoreDownloadsCrossfadeQualityTest.kt
- HomeSection
- VocalIsolationAudioProcessor
- OnboardingFlowTest.kt
- JamPersonWavesIcon
- ConnectivityObserver
- CompositeStreamResolver
- .isConnectedToWifi
- ArtistScreen.kt
- AudioQuality
- SearchFilter
- KaraokeAndJamTest
- AudioPlayerService
- RadioTuningMode
- .loadHomeFeed
- TrackOptionsBottomSheet.kt
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- InnerTubeCatalogClient
- Context
- ExploreScreen.kt
- UserHistoryRepository
- QueueAndConnectivityChallengerTest
- QueueExpansionState
- NestedScrollConnection
- ContextWrapper
- 2. 🎧 NERF JAM Global & Governança da Sala pelo Host
- NewPipeStreamResolver.kt
- .loadCanvasVideo
- Track
- NERF Music
- Artist
- SmartDownloadManager
- RelatedMultiTierFallbackTest.kt
- .triggerPrefetchNextTrack
- JamWebSocketServer.kt
- .applyKaraokeState
- Context
- JamGlobalRelayClient
- TextStyle
- Listener
- .onTaskRemoved
- InnerTubeStreamResolver
- AudioQuality
- PlayerUiUtilsTest.kt
- TrackRelatedData

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 206 edges
2. `Track` - 148 edges
3. `InnerTubeCatalogClient` - 55 edges
4. `AudioPlayerService` - 55 edges
5. `LibraryRepositoryImpl` - 48 edges
6. `AudioQuality` - 48 edges
7. `MigrationManager` - 46 edges
8. `UserSettingsRepositoryImpl` - 39 edges
9. `JamSessionManager` - 36 edges
10. `UserSettingsRepository` - 29 edges

## Surprising Connections (you probably didn't know these)
- `Adicionado` --references--> `AudioMetadataTagger`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/AudioMetadataTagger.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt
- `Adicionado` --references--> `JamGlobalRelayClient`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/jam/JamGlobalRelayClient.kt
- `Adicionado` --references--> `JamRoomSettings`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/jam/JamModels.kt
- `2. 🎧 NERF JAM Global & Governança da Sala pelo Host` --references--> `JamRoomSettings`  [INFERRED]
  RELEASE_NOTES.md → app/src/main/java/com/nerf/music/jam/JamModels.kt

## Import Cycles
- None detected.

## Communities (90 total, 45 thin omitted)

### Community 0 - "FullPlayerSheet"
Cohesion: 0.16
Nodes (18): PlaybackState, KaraokeSyncSnapshot, AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar(), ApplePopoverOptionRow(), FullPlayerSheet(), KaraokeCountdownDotsRow() (+10 more)

### Community 2 - "JamSessionSheet.kt"
Cohesion: 0.09
Nodes (3): JamSessionSheet(), JamSettingToggleRow(), JamVisualMatrixBadge()

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.10
Nodes (34): SmartDownloadSyncState, AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting (+26 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.09
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 8 - "JamSessionManager"
Cohesion: 0.06
Nodes (35): JamCommandType, ADD_TO_QUEUE, KICK_PARTICIPANT, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK (+27 more)

### Community 9 - "AudioMetadataTagger.kt"
Cohesion: 0.12
Nodes (4): AudioMetadataTaggerTest, [1.3.5] - 2026-10-27 [RELEASED], Adicionado, Testes

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 11 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.05
Nodes (8): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler, StreamResolver, MimeAndContainerDetectionStressTest, ContextWrapper, InvocationHandler, StreamResolver

### Community 12 - "DownloadedTrackEntity"
Cohesion: 0.08
Nodes (6): AppDatabase, DownloadedTrackDao, DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 13 - "HomeScreen.kt"
Cohesion: 0.17
Nodes (14): ExploreScreen(), HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel() (+6 more)

### Community 15 - "SearchScreen.kt"
Cohesion: 0.14
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 16 - "MainScreen.kt"
Cohesion: 0.08
Nodes (9): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar(), SaveToPlaylistSheet() (+1 more)

### Community 18 - "LibraryScreen.kt"
Cohesion: 0.10
Nodes (18): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS, LibraryGridCard() (+10 more)

### Community 19 - "LyricsData"
Cohesion: 0.05
Nodes (16): LyricsData, SyncedLyricsLine, SyncedLyricsWord, LyricsRepository, LyricsRepositoryImpl, optCleanString(), inlineLyricSegmentIndex(), InlineSyncedLyrics() (+8 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 21 - "KaraokeLyricsEngine"
Cohesion: 0.20
Nodes (3): BatchTranslationResult, KaraokeLyricsEngine, TranslationLanguageOption

### Community 22 - "OkHttpDownloader.kt"
Cohesion: 0.14
Nodes (3): NerfMusicApp, OkHttpDownloader, NewPipeStreamResolverTest

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.12
Nodes (17): [1.0.8] - 2026-10-23, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.2] - 2026-10-25, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25, [1.4.1] - 2026-10-27 [RELEASED], Adicionado (+9 more)

### Community 29 - "OnboardingScreen.kt"
Cohesion: 0.05
Nodes (6): applePressFeedback(), AppleArtistSelectionStep(), AppleArtistSphereCard(), AppleGlassFeatureCard(), AppleWelcomeStep(), OnboardingScreen()

### Community 31 - "AudioCoreDownloadsCrossfadeQualityTest"
Cohesion: 0.13
Nodes (4): AudioCoreDownloadsCrossfadeQualityTest, InvocationHandler, StreamResolver, StreamResolver

### Community 32 - "MigrationScreen"
Cohesion: 0.10
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 33 - "LibraryRepository.kt"
Cohesion: 0.06
Nodes (3): Http200RangeUnsupportedException, JamPlaybackSnapshot, DataSaverInterceptor

### Community 34 - "SmartDownloadWorker.kt"
Cohesion: 0.05
Nodes (6): MainActivity, NERFMusicTheme(), UpdateCheckWorker, UpdateNotificationHelper, SmartDownloadNotificationHelper, SmartDownloadWorker

### Community 36 - "HomeSection"
Cohesion: 0.17
Nodes (7): AlbumDetail, ArtistDetail, HomeChip, HomeFeedData, HomeSection, UpNextResult, MusicRepository

### Community 37 - "VocalIsolationAudioProcessor"
Cohesion: 0.10
Nodes (3): BiquadFilter, VocalIsolationAudioProcessor, Corrigido

### Community 38 - "OnboardingFlowTest.kt"
Cohesion: 0.08
Nodes (4): UserHistoryRepositoryImpl, OnboardingFlowTest, ContextWrapper, InvocationHandler

### Community 40 - "ConnectivityObserver"
Cohesion: 0.20
Nodes (9): ConnectivityObserver, Status, Available, Losing, Lost, Unavailable, [1.4.0] - 2026-10-27 [RELEASED], Adicionado (+1 more)

### Community 41 - "CompositeStreamResolver"
Cohesion: 0.16
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 43 - "ArtistScreen.kt"
Cohesion: 0.12
Nodes (12): AnimatedEqualizerBars(), AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen() (+4 more)

### Community 44 - "AudioQuality"
Cohesion: 0.09
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepositoryImpl

### Community 45 - "SearchFilter"
Cohesion: 0.20
Nodes (3): SearchFilter, ALL, SONGS

### Community 48 - "RadioTuningMode"
Cohesion: 0.27
Nodes (6): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR, SmartRadioEngine

### Community 51 - "TrackOptionsBottomSheet.kt"
Cohesion: 0.10
Nodes (4): PlaylistOptionItem(), PlaylistOptionsBottomSheet(), TrackOptionRow(), TrackOptionsBottomSheet()

### Community 59 - "QueueExpansionState"
Cohesion: 0.27
Nodes (5): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, QueueScrollControllerMock

### Community 63 - "2. 🎧 NERF JAM Global & Governança da Sala pelo Host"
Cohesion: 0.25
Nodes (5): 1. 🎤 Karaokê de Alta Precisão & DSP Anti-Abafamento, 2. 🎧 NERF JAM Global & Governança da Sala pelo Host, 🔥 Destaques Principais da Versão 1.4.1, 📦 Informações Técnicas da Compilação, 🎵 NERF Music v1.4.1 — Correções, Precisão de Karaokê & JAM Global! 🚀

### Community 66 - ".loadCanvasVideo"
Cohesion: 0.15
Nodes (3): Adicionado, Corrigido, Corrigido

### Community 68 - "Track"
Cohesion: 0.13
Nodes (5): Track, HomeFeedSynthesizer, UserAffinityRepository, [1.1.0] - 2026-10-24, Adicionado

### Community 69 - "NERF Music"
Cohesion: 0.25
Nodes (8): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão

### Community 74 - "SmartDownloadManager"
Cohesion: 0.14
Nodes (6): SmartDownloadManager, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 81 - "JamGlobalRelayClient"
Cohesion: 0.07
Nodes (4): JamGlobalRelayClient, WebSocketListener, NetworkConnectivityObserver, NetworkCallback

### Community 88 - "InnerTubeStreamResolver"
Cohesion: 0.10
Nodes (5): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest, StreamQualitySelectorStressTest

### Community 89 - "AudioQuality"
Cohesion: 0.14
Nodes (7): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, StreamResolver

### Community 98 - "TrackRelatedData"
Cohesion: 0.20
Nodes (10): HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL, TWO_ROW_CAROUSEL, YOUR_DOWNLOADS, RelatedSection (+2 more)

## Knowledge Gaps
- **80 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+75 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 510 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **45 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `FullPlayerSheet`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `JamSessionManager`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `SearchScreen.kt`, `MainScreen.kt`, `.playTrack`, `LibraryScreen.kt`, `LyricsData`, `SpotifyCanvasRepositoryImpl`, `KaraokeLyricsEngine`, `Track`, `MusicViewModel.kt`, `UserSettingsRepository`, `MigrationScreen`, `LibraryRepository.kt`, `SmartDownloadWorker.kt`, `HomeSection`, `ConnectivityObserver`, `.isConnectedToWifi`, `ArtistScreen.kt`, `AudioQuality`, `SearchFilter`, `RadioTuningMode`, `.loadHomeFeed`, `.updateCurrentLyricsLine`, `ExploreScreen.kt`, `UserHistoryRepository`, `NewPipeStreamResolver.kt`, `.clearStreamCache`, `.loadCanvasVideo`, `Track`, `Artist`, `SmartDownloadManager`, `AudioQuality`, `TrackRelatedData`?**
  _High betweenness centrality (0.283) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AudioPlayerService` (e.g. with `VocalIsolationAudioProcessor` and `Corrigido`) actually correct?**
  _`AudioPlayerService` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _80 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.02702702702702703 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `FullPlayerSheet`, `PlayerScreen.kt`, `MigrationManager`, `MusicViewModel`, `InnerTubeCatalogClientTest`, `JamSessionManager`, `AudioMetadataTagger.kt`, `DownloadChunkRangeStressTest.kt`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `SearchScreen.kt`, `MainScreen.kt`, `.playTrack`, `LibraryScreen.kt`, `KaraokeLyricsEngine`, `Track`, `AudioMetadataTagger`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `UserAffinityRepositoryImpl`, `LibraryRepository.kt`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `HomeSection`, `OnboardingFlowTest.kt`, `.isConnectedToWifi`, `ArtistScreen.kt`, `KaraokeAndJamTest`, `RadioTuningMode`, `TrackOptionsBottomSheet.kt`, `MusicRepositoryImpl`, `InnerTubeCatalogClient`, `UserHistoryRepository`, `.loadCanvasVideo`, `Artist`, `SmartDownloadManager`, `PlayerUiUtilsTest.kt`, `TrackRelatedData`?**
  _High betweenness centrality (0.160) - this node is a cross-community bridge._
- **Should `JamSessionSheet.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.0873015873015873 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `NewPipeStreamResolver.kt`, `LibraryRepository.kt`, `.clearStreamCache`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `SettingsScreen.kt`, `MusicViewModel`, `CompositeStreamResolver`, `DownloadChunkRangeStressTest.kt`, `AudioQuality`, `AudioPlayerService.kt`, `AudioPlayerService`, `LyricsData`, `InnerTubeStreamResolver`, `MusicViewModel.kt`, `UserSettingsRepository`, `AudioCoreDownloadsCrossfadeQualityTest`?**
  _High betweenness centrality (0.094) - this node is a cross-community bridge._