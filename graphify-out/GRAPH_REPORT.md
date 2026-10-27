# Graph Report - Nerf Music  (2026-10-27)

## Corpus Check
- 92 files · ~96,717 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 1636 nodes · 3841 edges · 86 communities (28 shown, 58 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 46 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a11c8baf`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- InnerTubeCatalogClient
- AudioPlayerService
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- Registro de Alterações (Changelog) - NERF Music
- ArtistScreen.kt
- MainScreen.kt
- steps
- LyricsData
- ExploreScreen.kt
- HomeScreen.kt
- .syncTimelineState
- SearchScreen.kt
- TextStyle
- LibraryScreen.kt
- AnimatedEqualizerBars.kt
- SpotifyCanvasRepositoryImpl
- UserHistoryRepositoryImpl
- FullPlayerSheet
- LibraryRepository
- LibraryRepositoryImpl
- DownloadChunkRangeStressTest.kt
- UserSettingsRepositoryImpl
- Track
- StreamQualitySelectorStressTest
- AudioCoreDownloadsCrossfadeQualityTest.kt
- MigrationScreen
- NetworkUtils
- UserSettingsRepository
- DownloadedTrackEntity
- InnerTubeCatalogClientTest
- .idFor
- UpdateNotificationHelper.kt
- DefaultStreamResolverTest.kt
- HomeSection
- PlaylistOptionsBottomSheet.kt
- AudioQuality
- DownloadedTrackEntity
- PlayerUiUtilsTest.kt
- OkHttpDownloader.kt
- RadioTuningMode
- .loadHomeFeed
- DownloadedTrack
- SmartDownloadManager.kt
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- NestedScrollConnection
- Listener
- NewPipeStreamResolver.kt
- SearchResult
- LibraryRepository.kt
- InnerTubeStreamResolver
- AudioCoreDownloadsCrossfadeQualityTest
- ArtistScreen
- rememberSystemVolume
- LibraryFilter
- Listener
- normalizeDynamicPaletteColor
- UserHistoryRepository
- MusicRepository
- AppDatabase
- TrackRelatedData
- Artist
- .compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure
- .testExplicitDownloadQualityNotOverriddenByDataSaverInCompositeResolver
- QueueAndConnectivityChallengerTest
- CrossfadeEmpiricalTest
- StreamResolver
- UpNextResult

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 166 edges
2. `Track` - 123 edges
3. `InnerTubeCatalogClient` - 54 edges
4. `AudioQuality` - 48 edges
5. `LibraryRepositoryImpl` - 46 edges
6. `MigrationManager` - 46 edges
7. `AudioPlayerService` - 38 edges
8. `UserSettingsRepositoryImpl` - 34 edges
9. `Block3EmpiricalChallengeTest` - 28 edges
10. `LibraryRepository` - 27 edges

## Surprising Connections (you probably didn't know these)
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt
- `Adicionado` --references--> `UserAffinityRepository`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/UserAffinityRepository.kt
- `Adicionado` --references--> `AnimatedEqualizerBars()`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/ui/components/AnimatedEqualizerBars.kt
- `Segurança e Estabilidade` --references--> `AppUpdateManager`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/updater/AppUpdateManager.kt
- `Adicionado` --references--> `SmartDownloadWorker`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/work/SmartDownloadWorker.kt

## Import Cycles
- None detected.

## Communities (86 total, 58 thin omitted)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.10
Nodes (33): AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting, SettingsMainContent() (+25 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.06
Nodes (8): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest, LyricsRepositorySanitizationTest

### Community 7 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.06
Nodes (28): HomeFeedSynthesizer, [1.0.8] - 2026-10-23, [1.1.0] - 2026-10-24, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.2] - 2026-10-25, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25 (+20 more)

### Community 8 - "ArtistScreen.kt"
Cohesion: 0.10
Nodes (3): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow()

### Community 9 - "MainScreen.kt"
Cohesion: 0.07
Nodes (9): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar(), SaveToPlaylistSheet() (+1 more)

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 11 - "LyricsData"
Cohesion: 0.06
Nodes (14): LyricsData, SyncedLyricsLine, LyricsRepository, LyricsRepositoryImpl, optCleanString(), inlineLyricSegmentIndex(), InlineSyncedLyrics(), Block3EmpiricalChallengeTest (+6 more)

### Community 13 - "HomeScreen.kt"
Cohesion: 0.19
Nodes (14): ExploreScreen(), HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel() (+6 more)

### Community 16 - "SearchScreen.kt"
Cohesion: 0.14
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 18 - "LibraryScreen.kt"
Cohesion: 0.15
Nodes (11): LibraryGridCard(), LibraryListItem(), LibraryScreen(), LibrarySortOption, A_TO_Z, RECENT, RECENTLY_ADDED, NewBottomSheetOption() (+3 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.19
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 22 - "FullPlayerSheet"
Cohesion: 0.22
Nodes (12): PlaybackState, AppleMusicLyricsView(), AppleMusicVolumeBar(), FullPlayerSheet(), MiniPlayerBar(), PlayerActionRow(), PlayerRelatedSheetContent(), QueueDragHeader() (+4 more)

### Community 27 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.05
Nodes (8): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler, StreamResolver, MimeAndContainerDetectionStressTest, ContextWrapper, InvocationHandler, StreamResolver

### Community 28 - "UserSettingsRepositoryImpl"
Cohesion: 0.09
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepositoryImpl

### Community 29 - "Track"
Cohesion: 0.11
Nodes (3): Track, UserAffinityRepository, UserAffinityRepositoryImpl

### Community 32 - "MigrationScreen"
Cohesion: 0.14
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 35 - "UserSettingsRepository"
Cohesion: 0.11
Nodes (6): DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH, UserSettingsRepository

### Community 39 - "UpdateNotificationHelper.kt"
Cohesion: 0.06
Nodes (6): MainActivity, NERFMusicTheme(), UpdateCheckWorker, UpdateNotificationHelper, SmartDownloadNotificationHelper, SmartDownloadWorker

### Community 41 - "DefaultStreamResolverTest.kt"
Cohesion: 0.23
Nodes (3): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest

### Community 42 - "HomeSection"
Cohesion: 0.22
Nodes (10): HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL (+2 more)

### Community 44 - "AudioQuality"
Cohesion: 0.17
Nodes (6): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH

### Community 48 - "RadioTuningMode"
Cohesion: 0.27
Nodes (6): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR, SmartRadioEngine

### Community 58 - "SearchResult"
Cohesion: 0.24
Nodes (4): SearchFilter, ALL, SONGS, SearchResult

### Community 61 - "InnerTubeStreamResolver"
Cohesion: 0.29
Nodes (3): CandidateFormat, ClientConfig, InnerTubeStreamResolver

### Community 63 - "ArtistScreen"
Cohesion: 0.61
Nodes (6): ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow(), ArtistVideoCard()

### Community 65 - "LibraryFilter"
Cohesion: 0.29
Nodes (7): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS

### Community 81 - "QueueAndConnectivityChallengerTest"
Cohesion: 0.12
Nodes (13): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+5 more)

## Knowledge Gaps
- **54 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+49 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 438 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **58 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `AudioPlayerService`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `Registro de Alterações (Changelog) - NERF Music`, `ArtistScreen.kt`, `MainScreen.kt`, `LyricsData`, `ExploreScreen.kt`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `SearchScreen.kt`, `LibraryScreen.kt`, `SpotifyCanvasRepositoryImpl`, `FullPlayerSheet`, `LibraryRepository`, `MusicViewModel.kt`, `UserSettingsRepositoryImpl`, `Track`, `MigrationScreen`, `UserSettingsRepository`, `UpdateNotificationHelper.kt`, `HomeSection`, `AudioQuality`, `RadioTuningMode`, `.loadHomeFeed`, `DownloadedTrack`, `SmartDownloadManager.kt`, `MusicRepositoryImpl`, `NewPipeStreamResolver.kt`, `SearchResult`, `.initializePlayerController`, `ArtistScreen`, `.seekTo`, `UserHistoryRepository`, `MusicRepository`, `TrackRelatedData`, `Artist`, `.clearStreamCache`?**
  _High betweenness centrality (0.232) - this node is a cross-community bridge._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _54 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.023529411764705882 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `InnerTubeCatalogClient`, `PlayerScreen.kt`, `MigrationManager`, `MusicViewModel`, `ArtistScreen.kt`, `MainScreen.kt`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `SearchScreen.kt`, `LibraryScreen.kt`, `UserHistoryRepositoryImpl`, `FullPlayerSheet`, `LibraryRepository`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `DownloadChunkRangeStressTest.kt`, `DownloadedTrackEntity`, `InnerTubeCatalogClientTest`, `UpdateNotificationHelper.kt`, `InnerTubeCatalogClient.kt`, `HomeSection`, `DownloadedTrackEntity`, `PlayerUiUtilsTest.kt`, `RadioTuningMode`, `DownloadedTrack`, `SmartDownloadManager.kt`, `MusicRepositoryImpl`, `SearchResult`, `LibraryRepository.kt`, `ArtistScreen`, `UserHistoryRepository`, `TrackRelatedData`, `Artist`, `UpNextResult`?**
  _High betweenness centrality (0.195) - this node is a cross-community bridge._
- **Should `MigrationManager` be split into smaller, more focused modules?**
  _Cohesion score 0.06936026936026936 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `AudioPlayerService`, `SettingsScreen.kt`, `MusicViewModel`, `AudioPlayerService.kt`, `MusicViewModel.kt`, `DownloadChunkRangeStressTest.kt`, `UserSettingsRepositoryImpl`, `StreamQualitySelectorStressTest`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `UserSettingsRepository`, `InnerTubeCatalogClientTest`, `DefaultStreamResolverTest.kt`, `DownloadedTrackEntity`, `NewPipeStreamResolver.kt`, `InnerTubeStreamResolver`, `.compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure`, `.testExplicitDownloadQualityNotOverriddenByDataSaverInCompositeResolver`, `.clearStreamCache`, `StreamResolver`?**
  _High betweenness centrality (0.104) - this node is a cross-community bridge._
- **Should `SettingsScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.1027450980392157 - nodes in this community are weakly interconnected._