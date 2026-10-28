# Graph Report - Nerf Music  (2026-10-28)

## Corpus Check
- 107 files · ~132,160 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 2051 nodes · 4953 edges · 103 communities (42 shown, 61 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 104 edges (avg confidence: 0.9)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `449e2d0e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- FullPlayerSheet
- UserHistoryRepositoryImpl
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- InnerTubeCatalogClientTest
- JamSessionManager
- DownloadChunkRangeStressTest.kt
- steps
- LyricsRepositoryImpl
- DownloadedTrackEntity
- HomeScreen.kt
- .syncTimelineState
- SearchScreen.kt
- MainScreen.kt
- DownloadedTrackDao
- LibraryScreen.kt
- JamSessionSheet.kt
- SpotifyCanvasRepositoryImpl
- KaraokeAndJamTest.kt
- NestedScrollConnection
- Track
- LyricsNullSanitizationStressTest
- LibraryRepositoryImpl
- MusicViewModel.kt
- Registro de Alterações (Changelog) - NERF Music
- UserSettingsRepository
- AnimatedEqualizerBars.kt
- ConnectivityObserver
- AudioCoreDownloadsCrossfadeQualityTest
- MigrationScreen
- LibraryRepository.kt
- SmartDownloadWorker.kt
- KaraokeAndJamTest.kt
- QueueExpansionState
- AudioMetadataTagger.kt
- QueueAndConnectivityChallengerTest
- VocalIsolationAudioProcessor
- CompositeStreamResolver
- KaraokeAndJamTest
- ArtistScreen.kt
- AudioQuality
- MainActivity.kt
- RadioTuningMode
- AudioPlayerService
- Track
- .loadHomeFeed
- .updateCurrentLyricsLine
- TrackOptionsBottomSheet.kt
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- InnerTubeCatalogClient
- 2. 🎧 NERF JAM Global & Governança da Sala pelo Host
- PlayerUiUtilsTest.kt
- ExploreScreen.kt
- Block3EmpiricalChallengeTest
- JamSessionManager.kt
- JamPersonWavesIcon
- UserHistoryRepository
- NERF Music
- NewPipeStreamResolver.kt
- SmartDownloadManager
- .loadCanvasVideo
- HomeSection
- AudioCoreDownloadsCrossfadeQualityTest.kt
- .syncNow
- RelatedMultiTierFallbackTest.kt
- JamWebSocketServer.kt
- Listener
- AppDatabase
- JamGlobalRelayClient
- InnerTubeStreamResolver
- UpdateNotificationHelper
- SmartDownloadWorker
- .onCreate
- .onTaskRemoved
- StreamQualitySelectorStressTest
- AudioQuality
- AudioMetadataTagger
- InlineSyncedLyrics
- .isConnectedToWifi
- Context
- DownloadAudioQuality
- Theme.kt
- LyricsData
- Test
- TrackRelatedData
- SearchResult
- DownloadOutcome
- AlbumDetail
- Listener

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 204 edges
2. `Track` - 148 edges
3. `InnerTubeCatalogClient` - 55 edges
4. `AudioPlayerService` - 55 edges
5. `LibraryRepositoryImpl` - 48 edges
6. `AudioQuality` - 48 edges
7. `MigrationManager` - 46 edges
8. `UserSettingsRepositoryImpl` - 36 edges
9. `JamSessionManager` - 36 edges
10. `LyricsData` - 28 edges

## Surprising Connections (you probably didn't know these)
- `Adicionado` --references--> `AudioMetadataTagger`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/AudioMetadataTagger.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt
- `Adicionado` --references--> `UserAffinityRepository`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/UserAffinityRepository.kt
- `Adicionado` --references--> `JamGlobalRelayClient`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/jam/JamGlobalRelayClient.kt
- `Adicionado` --references--> `JamRoomSettings`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/jam/JamModels.kt

## Import Cycles
- None detected.

## Communities (103 total, 61 thin omitted)

### Community 0 - "FullPlayerSheet"
Cohesion: 0.16
Nodes (18): PlaybackState, AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar(), ApplePopoverOptionRow(), FullPlayerSheet(), inlineLyricSegmentIndex(), InlineSyncedLyrics() (+10 more)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.09
Nodes (34): SmartDownloadSyncState, AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting (+26 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 8 - "JamSessionManager"
Cohesion: 0.06
Nodes (35): JamCommandType, ADD_TO_QUEUE, KICK_PARTICIPANT, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK (+27 more)

### Community 9 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.05
Nodes (8): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler, StreamResolver, MimeAndContainerDetectionStressTest, ContextWrapper, InvocationHandler, StreamResolver

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 11 - "LyricsRepositoryImpl"
Cohesion: 0.21
Nodes (3): LyricsRepository, LyricsRepositoryImpl, optCleanString()

### Community 12 - "DownloadedTrackEntity"
Cohesion: 0.16
Nodes (4): DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 13 - "HomeScreen.kt"
Cohesion: 0.17
Nodes (13): HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel(), YtmSectionHeader() (+5 more)

### Community 15 - "SearchScreen.kt"
Cohesion: 0.15
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 16 - "MainScreen.kt"
Cohesion: 0.07
Nodes (9): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar(), SaveToPlaylistSheet() (+1 more)

### Community 18 - "LibraryScreen.kt"
Cohesion: 0.11
Nodes (18): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS, LibraryGridCard() (+10 more)

### Community 19 - "JamSessionSheet.kt"
Cohesion: 0.09
Nodes (3): JamSessionSheet(), JamSettingToggleRow(), JamVisualMatrixBadge()

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 21 - "KaraokeAndJamTest.kt"
Cohesion: 0.19
Nodes (4): BatchTranslationResult, KaraokeLyricsEngine, TranslationLanguageOption, Corrigido

### Community 26 - "MusicViewModel.kt"
Cohesion: 0.10
Nodes (3): HomeFeedSynthesizer, [1.1.0] - 2026-10-24, Adicionado

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.12
Nodes (17): [1.0.8] - 2026-10-23, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.2] - 2026-10-25, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25, [1.4.1] - 2026-10-27 [RELEASED], Adicionado (+9 more)

### Community 30 - "ConnectivityObserver"
Cohesion: 0.20
Nodes (9): ConnectivityObserver, Status, Available, Losing, Lost, Unavailable, [1.4.0] - 2026-10-27 [RELEASED], Adicionado (+1 more)

### Community 31 - "AudioCoreDownloadsCrossfadeQualityTest"
Cohesion: 0.21
Nodes (3): AudioCoreDownloadsCrossfadeQualityTest, StreamResolver, StreamResolver

### Community 32 - "MigrationScreen"
Cohesion: 0.11
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 36 - "QueueExpansionState"
Cohesion: 0.27
Nodes (5): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, QueueScrollControllerMock

### Community 37 - "AudioMetadataTagger.kt"
Cohesion: 0.13
Nodes (4): AudioMetadataTaggerTest, [1.3.5] - 2026-10-27 [RELEASED], Adicionado, Testes

### Community 41 - "CompositeStreamResolver"
Cohesion: 0.17
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 43 - "ArtistScreen.kt"
Cohesion: 0.15
Nodes (11): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow() (+3 more)

### Community 44 - "AudioQuality"
Cohesion: 0.08
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepositoryImpl

### Community 46 - "RadioTuningMode"
Cohesion: 0.33
Nodes (5): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR

### Community 48 - "Track"
Cohesion: 0.11
Nodes (4): Track, UserAffinityRepository, UserAffinityRepositoryImpl, SmartRadioEngine

### Community 51 - "TrackOptionsBottomSheet.kt"
Cohesion: 0.11
Nodes (4): PlaylistOptionItem(), PlaylistOptionsBottomSheet(), TrackOptionRow(), TrackOptionsBottomSheet()

### Community 55 - "2. 🎧 NERF JAM Global & Governança da Sala pelo Host"
Cohesion: 0.25
Nodes (5): 1. 🎤 Karaokê de Alta Precisão & DSP Anti-Abafamento, 2. 🎧 NERF JAM Global & Governança da Sala pelo Host, 🔥 Destaques Principais da Versão 1.4.1, 📦 Informações Técnicas da Compilação, 🎵 NERF Music v1.4.1 — Correções, Precisão de Karaokê & JAM Global! 🚀

### Community 63 - "NERF Music"
Cohesion: 0.25
Nodes (8): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão

### Community 64 - "NewPipeStreamResolver.kt"
Cohesion: 0.09
Nodes (4): NerfMusicApp, NewPipeStreamResolver, OkHttpDownloader, NewPipeStreamResolverTest

### Community 66 - ".loadCanvasVideo"
Cohesion: 0.15
Nodes (3): Adicionado, Corrigido, Corrigido

### Community 68 - "HomeSection"
Cohesion: 0.14
Nodes (10): ArtistDetail, HomeChip, HomeFeedData, HomeSection, SearchFilter, ALL, SONGS, QueueFilterChip (+2 more)

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 81 - "JamGlobalRelayClient"
Cohesion: 0.07
Nodes (4): JamGlobalRelayClient, WebSocketListener, NetworkConnectivityObserver, NetworkCallback

### Community 82 - "InnerTubeStreamResolver"
Cohesion: 0.15
Nodes (4): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest

### Community 89 - "AudioQuality"
Cohesion: 0.16
Nodes (7): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, StreamResolver

### Community 94 - "DownloadAudioQuality"
Cohesion: 0.22
Nodes (5): DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 96 - "LyricsData"
Cohesion: 0.54
Nodes (3): LyricsData, SyncedLyricsLine, SyncedLyricsWord

### Community 98 - "TrackRelatedData"
Cohesion: 0.20
Nodes (10): HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL, TWO_ROW_CAROUSEL, YOUR_DOWNLOADS, RelatedSection (+2 more)

### Community 100 - "DownloadOutcome"
Cohesion: 0.40
Nodes (5): DownloadOutcome, ALLOWED, BLOCKED_ALWAYS_ON, BLOCKED_AUTOMATIC_DATA_SAVER, BLOCKED_CELLULAR_DISABLED

## Knowledge Gaps
- **80 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+75 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 508 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **61 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `FullPlayerSheet`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `JamSessionManager`, `LyricsRepositoryImpl`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `SearchScreen.kt`, `MainScreen.kt`, `LibraryScreen.kt`, `SpotifyCanvasRepositoryImpl`, `KaraokeAndJamTest.kt`, `Track`, `MusicViewModel.kt`, `UserSettingsRepository`, `ConnectivityObserver`, `MigrationScreen`, `.clearStreamCache`, `ArtistScreen.kt`, `AudioQuality`, `MainActivity.kt`, `RadioTuningMode`, `Track`, `.loadHomeFeed`, `.updateCurrentLyricsLine`, `.playTrack`, `ExploreScreen.kt`, `JamSessionManager.kt`, `UserHistoryRepository`, `NewPipeStreamResolver.kt`, `SmartDownloadManager`, `.loadCanvasVideo`, `HomeSection`, `UpdateNotificationHelper`, `SmartDownloadWorker`, `.initializePlayerController`, `AudioQuality`, `.isConnectedToWifi`, `DownloadAudioQuality`, `LyricsData`, `TrackRelatedData`, `SearchResult`, `AlbumDetail`?**
  _High betweenness centrality (0.268) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AudioPlayerService` (e.g. with `VocalIsolationAudioProcessor` and `Corrigido`) actually correct?**
  _`AudioPlayerService` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _80 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.02247191011235955 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `FullPlayerSheet`, `PlayerScreen.kt`, `UserHistoryRepositoryImpl`, `MigrationManager`, `MusicViewModel`, `InnerTubeCatalogClientTest`, `JamSessionManager`, `DownloadChunkRangeStressTest.kt`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `SearchScreen.kt`, `MainScreen.kt`, `LibraryScreen.kt`, `KaraokeAndJamTest.kt`, `Track`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `LibraryRepository.kt`, `KaraokeAndJamTest.kt`, `AudioMetadataTagger.kt`, `KaraokeAndJamTest`, `ArtistScreen.kt`, `RadioTuningMode`, `.updateCurrentLyricsLine`, `TrackOptionsBottomSheet.kt`, `MusicRepositoryImpl`, `InnerTubeCatalogClient`, `.playTrack`, `PlayerUiUtilsTest.kt`, `JamSessionManager.kt`, `UserHistoryRepository`, `SmartDownloadManager`, `.loadCanvasVideo`, `HomeSection`, `AudioMetadataTagger`, `.isConnectedToWifi`, `LyricsData`, `TrackRelatedData`, `SearchResult`, `AlbumDetail`?**
  _High betweenness centrality (0.215) - this node is a cross-community bridge._
- **Should `MigrationManager` be split into smaller, more focused modules?**
  _Cohesion score 0.06753246753246753 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `NewPipeStreamResolver.kt`, `LibraryRepository.kt`, `KaraokeAndJamTest.kt`, `SettingsScreen.kt`, `MusicViewModel`, `.clearStreamCache`, `CompositeStreamResolver`, `DownloadChunkRangeStressTest.kt`, `AudioQuality`, `AudioPlayerService`, `InnerTubeStreamResolver`, `.onCreate`, `MusicViewModel.kt`, `Block3EmpiricalChallengeTest`, `UserSettingsRepository`, `AudioCoreDownloadsCrossfadeQualityTest`?**
  _High betweenness centrality (0.066) - this node is a cross-community bridge._