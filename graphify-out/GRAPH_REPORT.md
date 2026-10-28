# Graph Report - Nerf Music  (2026-10-27)

## Corpus Check
- 106 files · ~130,253 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 2041 nodes · 4899 edges · 97 communities (40 shown, 57 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 89 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `857dd36f`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- FullPlayerSheet
- UserHistoryRepositoryImpl
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- UserHistoryRepository
- JamSessionManager
- DownloadChunkRangeStressTest.kt
- steps
- LyricsData
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
- LibraryRepositoryImpl
- Registro de Alterações (Changelog) - NERF Music
- UserSettingsRepository
- OkHttpDownloader.kt
- AudioCoreDownloadsCrossfadeQualityTest
- MigrationScreen
- LibraryRepository.kt
- SmartDownloadWorker.kt
- KaraokeAndJamTest.kt
- ConnectivityObserver
- InnerTubeCatalogClientTest
- MimeAndContainerDetectionStressTest.kt
- VocalIsolationAudioProcessor
- CompositeStreamResolver
- DownloadAudioQuality
- ArtistScreen.kt
- AudioQuality
- JamSessionManager.kt
- RadioTuningMode
- LyricsNullSanitizationStressTest
- Track
- .loadHomeFeed
- .updateCurrentLyricsLine
- PlaylistOptionsBottomSheet.kt
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- InnerTubeCatalogClient
- InnerTubeCatalogClient
- .isConnectedToWifi
- PlayerUiUtilsTest.kt
- ExploreScreen.kt
- Block3EmpiricalChallengeTest
- TrackRelatedData
- JamPersonWavesIcon
- DataSaverInterceptor.kt
- MimeAndContainerDetectionStressTest
- NewPipeStreamResolver
- DownloadOutcome
- AlbumDetail
- .prefetchTrack
- AudioCoreDownloadsCrossfadeQualityTest.kt
- RelatedMultiTierFallbackTest.kt
- AudioPlayerService
- JamWebSocketServer.kt
- Listener
- .applyKaraokeState
- AppDatabase
- JamGlobalRelayClient
- InnerTubeStreamResolver
- Test
- LyricsData
- .onCreate
- .onTaskRemoved
- .compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure
- AudioQuality
- InlineSyncedLyrics
- UpNextResult
- Context
- StreamResolver
- SearchResult
- HomeSection

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 203 edges
2. `Track` - 145 edges
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
- `Adicionado` --references--> `ConnectivityObserver`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/ConnectivityObserver.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt
- `Adicionado` --references--> `UserAffinityRepository`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/UserAffinityRepository.kt
- `Adicionado` --references--> `JamGlobalRelayClient`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/jam/JamGlobalRelayClient.kt

## Import Cycles
- None detected.

## Communities (97 total, 57 thin omitted)

### Community 0 - "FullPlayerSheet"
Cohesion: 0.14
Nodes (18): PlaybackState, AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar(), ApplePopoverOptionRow(), FullPlayerSheet(), Listener, KaraokeCountdownDotsRow() (+10 more)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.10
Nodes (34): SmartDownloadSyncState, AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting (+26 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 8 - "JamSessionManager"
Cohesion: 0.06
Nodes (35): JamCommandType, ADD_TO_QUEUE, KICK_PARTICIPANT, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK (+27 more)

### Community 9 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.11
Nodes (3): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 11 - "LyricsData"
Cohesion: 0.20
Nodes (3): LyricsRepository, LyricsRepositoryImpl, optCleanString()

### Community 12 - "DownloadedTrackEntity"
Cohesion: 0.18
Nodes (4): DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 13 - "HomeScreen.kt"
Cohesion: 0.18
Nodes (14): ExploreScreen(), HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel() (+6 more)

### Community 15 - "SearchScreen.kt"
Cohesion: 0.15
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 16 - "MainScreen.kt"
Cohesion: 0.07
Nodes (9): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar(), SaveToPlaylistSheet() (+1 more)

### Community 18 - "LibraryScreen.kt"
Cohesion: 0.10
Nodes (18): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS, LibraryGridCard() (+10 more)

### Community 19 - "JamSessionSheet.kt"
Cohesion: 0.05
Nodes (4): AnimatedEqualizerBars(), JamSessionSheet(), JamSettingToggleRow(), JamVisualMatrixBadge()

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 21 - "KaraokeAndJamTest.kt"
Cohesion: 0.13
Nodes (5): BatchTranslationResult, KaraokeLyricsEngine, TranslationLanguageOption, KaraokeAndJamTest, Corrigido

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.05
Nodes (38): HomeFeedSynthesizer, [1.0.8] - 2026-10-23, [1.1.0] - 2026-10-24, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.2] - 2026-10-25, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25 (+30 more)

### Community 31 - "AudioCoreDownloadsCrossfadeQualityTest"
Cohesion: 0.21
Nodes (3): AudioCoreDownloadsCrossfadeQualityTest, StreamResolver, StreamResolver

### Community 32 - "MigrationScreen"
Cohesion: 0.11
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 34 - "SmartDownloadWorker.kt"
Cohesion: 0.05
Nodes (6): MainActivity, NERFMusicTheme(), UpdateCheckWorker, UpdateNotificationHelper, SmartDownloadNotificationHelper, SmartDownloadWorker

### Community 36 - "ConnectivityObserver"
Cohesion: 0.12
Nodes (13): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+5 more)

### Community 38 - "MimeAndContainerDetectionStressTest.kt"
Cohesion: 0.12
Nodes (3): ContextWrapper, InvocationHandler, StreamResolver

### Community 39 - "VocalIsolationAudioProcessor"
Cohesion: 0.06
Nodes (5): BiquadFilter, VocalIsolationAudioProcessor, AudioMetadataTagger, ExtractedMetadata, AudioMetadataTaggerTest

### Community 41 - "CompositeStreamResolver"
Cohesion: 0.23
Nodes (3): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest

### Community 42 - "DownloadAudioQuality"
Cohesion: 0.14
Nodes (6): SmartDownloadManager, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 43 - "ArtistScreen.kt"
Cohesion: 0.13
Nodes (9): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow() (+1 more)

### Community 44 - "AudioQuality"
Cohesion: 0.10
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepositoryImpl

### Community 46 - "RadioTuningMode"
Cohesion: 0.27
Nodes (6): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR, SmartRadioEngine

### Community 48 - "Track"
Cohesion: 0.12
Nodes (3): Track, UserAffinityRepository, UserAffinityRepositoryImpl

### Community 53 - "MusicRepositoryImpl"
Cohesion: 0.18
Nodes (3): AlbumDetail, ArtistDetail, MusicRepositoryImpl

### Community 60 - "TrackRelatedData"
Cohesion: 0.33
Nodes (3): RelatedSection, TrackRelatedData, RelatedMultiTierFallbackTest

### Community 65 - "DownloadOutcome"
Cohesion: 0.40
Nodes (5): DownloadOutcome, ALLOWED, BLOCKED_ALWAYS_ON, BLOCKED_AUTOMATIC_DATA_SAVER, BLOCKED_CELLULAR_DISABLED

### Community 76 - "AudioPlayerService"
Cohesion: 0.16
Nodes (5): AudioPlayerService, [1.3.5] - 2026-10-27 [RELEASED], Adicionado, Corrigido, Testes

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 81 - "JamGlobalRelayClient"
Cohesion: 0.07
Nodes (4): JamGlobalRelayClient, WebSocketListener, NetworkConnectivityObserver, NetworkCallback

### Community 82 - "InnerTubeStreamResolver"
Cohesion: 0.10
Nodes (5): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest, StreamQualitySelectorStressTest

### Community 84 - "LyricsData"
Cohesion: 0.42
Nodes (3): LyricsData, SyncedLyricsLine, SyncedLyricsWord

### Community 89 - "AudioQuality"
Cohesion: 0.14
Nodes (7): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, StreamResolver

### Community 91 - "InlineSyncedLyrics"
Cohesion: 0.47
Nodes (4): inlineLyricSegmentIndex(), InlineSyncedLyrics(), largestFittingCharacterBoundary(), splitInlineLyricIntoSegments()

### Community 98 - "HomeSection"
Cohesion: 0.18
Nodes (13): HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL (+5 more)

## Knowledge Gaps
- **80 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+75 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 509 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **57 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `FullPlayerSheet`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `UserHistoryRepository`, `JamSessionManager`, `LyricsData`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `SearchScreen.kt`, `MainScreen.kt`, `LibraryScreen.kt`, `SpotifyCanvasRepositoryImpl`, `KaraokeAndJamTest.kt`, `Track`, `MusicViewModel.kt`, `Registro de Alterações (Changelog) - NERF Music`, `UserSettingsRepository`, `MigrationScreen`, `SmartDownloadWorker.kt`, `ConnectivityObserver`, `.clearStreamCache`, `DownloadAudioQuality`, `ArtistScreen.kt`, `AudioQuality`, `JamSessionManager.kt`, `RadioTuningMode`, `Track`, `.loadHomeFeed`, `.updateCurrentLyricsLine`, `MusicRepositoryImpl`, `.isConnectedToWifi`, `ExploreScreen.kt`, `TrackRelatedData`, `NewPipeStreamResolver`, `.loadCanvasVideo`, `AlbumDetail`, `.setKaraokeMode`, `LyricsData`, `.initializePlayerController`, `AudioQuality`, `UpNextResult`, `SearchResult`, `HomeSection`?**
  _High betweenness centrality (0.223) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AudioPlayerService` (e.g. with `VocalIsolationAudioProcessor` and `Corrigido`) actually correct?**
  _`AudioPlayerService` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _80 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `FullPlayerSheet` be split into smaller, more focused modules?**
  _Cohesion score 0.1396011396011396 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `FullPlayerSheet`, `PlayerScreen.kt`, `UserHistoryRepositoryImpl`, `MigrationManager`, `MusicViewModel`, `UserHistoryRepository`, `JamSessionManager`, `DownloadChunkRangeStressTest.kt`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `SearchScreen.kt`, `MainScreen.kt`, `LibraryScreen.kt`, `KaraokeAndJamTest.kt`, `Track`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `InnerTubeCatalogClient.kt`, `LibraryRepository.kt`, `KaraokeAndJamTest.kt`, `InnerTubeCatalogClientTest`, `VocalIsolationAudioProcessor`, `DownloadAudioQuality`, `ArtistScreen.kt`, `JamSessionManager.kt`, `RadioTuningMode`, `PlaylistOptionsBottomSheet.kt`, `MusicRepositoryImpl`, `InnerTubeCatalogClient`, `.isConnectedToWifi`, `PlayerUiUtilsTest.kt`, `TrackRelatedData`, `.loadCanvasVideo`, `UpNextResult`, `SearchResult`, `HomeSection`?**
  _High betweenness centrality (0.198) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.023255813953488372 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `SettingsScreen.kt`, `MusicViewModel`, `DownloadChunkRangeStressTest.kt`, `AudioPlayerService.kt`, `MusicViewModel.kt`, `UserSettingsRepository`, `InnerTubeCatalogClient.kt`, `AudioCoreDownloadsCrossfadeQualityTest`, `KaraokeAndJamTest.kt`, `MimeAndContainerDetectionStressTest.kt`, `.clearStreamCache`, `CompositeStreamResolver`, `AudioQuality`, `NewPipeStreamResolver`, `.prefetchTrack`, `InnerTubeStreamResolver`, `LyricsData`, `.compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure`, `StreamResolver`?**
  _High betweenness centrality (0.099) - this node is a cross-community bridge._