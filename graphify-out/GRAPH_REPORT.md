# Graph Report - Nerf Music  (2026-10-27)

## Corpus Check
- 94 files · ~100,187 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 1670 nodes · 3933 edges · 83 communities (29 shown, 54 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 46 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1c0ae539`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- InnerTubeCatalogClient
- AudioMetadataTagger
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- Registro de Alterações (Changelog) - NERF Music
- ArtistScreen.kt
- NewPipeStreamResolver.kt
- steps
- LyricsData
- ExploreScreen.kt
- HomeScreen.kt
- .syncTimelineState
- SearchScreen.kt
- Type.kt
- LibraryScreen.kt
- AnimatedEqualizerBars.kt
- SpotifyCanvasRepositoryImpl
- UserHistoryRepositoryImpl
- FullPlayerSheet
- LibraryRepository
- AudioPlayerService.kt
- LibraryRepositoryImpl
- DownloadChunkRangeStressTest.kt
- UserSettingsRepositoryImpl
- Track
- StreamQualitySelectorStressTest
- AudioCoreDownloadsCrossfadeQualityTest.kt
- MigrationScreen
- SmartDownloadWorker
- SmartDownloadWorker.kt
- SearchResult
- DownloadedTrackEntity
- InnerTubeCatalogClientTest
- .idFor
- MainScreen.kt
- CompositeStreamResolver
- HomeSection
- PlaylistOptionsBottomSheet.kt
- AudioQuality
- DownloadedTrackEntity
- PlayerUiUtilsTest.kt
- MainActivity.kt
- RadioTuningMode
- .loadHomeFeed
- DownloadedTrack
- SmartDownloadManager.kt
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- NestedScrollConnection
- InnerTubeStreamResolverTest
- NewPipeStreamResolver.kt
- .syncNow
- LibraryRepository.kt
- InnerTubeStreamResolver
- AudioCoreDownloadsCrossfadeQualityTest
- rememberSystemVolume
- LibraryFilter
- Listener
- normalizeDynamicPaletteColor
- UpdateNotificationHelper
- MusicRepository
- AppDatabase
- TrackRelatedData
- Artist
- Theme.kt
- .testExplicitDownloadQualityNotOverriddenByDataSaverInCompositeResolver
- QueueAndConnectivityChallengerTest
- CrossfadeEmpiricalTest

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 166 edges
2. `Track` - 130 edges
3. `InnerTubeCatalogClient` - 55 edges
4. `AudioQuality` - 48 edges
5. `LibraryRepositoryImpl` - 46 edges
6. `MigrationManager` - 46 edges
7. `AudioPlayerService` - 38 edges
8. `UserSettingsRepositoryImpl` - 34 edges
9. `Block3EmpiricalChallengeTest` - 28 edges
10. `LibraryRepository` - 27 edges

## Surprising Connections (you probably didn't know these)
- `Adicionado` --references--> `HomeFeedSynthesizer`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/HomeFeedSynthesizer.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt
- `Adicionado` --references--> `UserAffinityRepository`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/UserAffinityRepository.kt
- `Adicionado` --references--> `AnimatedEqualizerBars()`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/ui/components/AnimatedEqualizerBars.kt
- `Segurança e Estabilidade` --references--> `AppUpdateManager`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/updater/AppUpdateManager.kt

## Import Cycles
- None detected.

## Communities (83 total, 54 thin omitted)

### Community 2 - "AudioMetadataTagger"
Cohesion: 0.13
Nodes (3): AudioMetadataTagger, ExtractedMetadata, AudioMetadataTaggerTest

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.09
Nodes (33): AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting, SettingsMainContent() (+25 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 7 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.07
Nodes (27): [1.0.8] - 2026-10-23, [1.1.0] - 2026-10-24, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.2] - 2026-10-25, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25, Adicionado (+19 more)

### Community 8 - "ArtistScreen.kt"
Cohesion: 0.13
Nodes (9): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow() (+1 more)

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 11 - "LyricsData"
Cohesion: 0.05
Nodes (15): LyricsData, SyncedLyricsLine, LyricsRepository, LyricsRepositoryImpl, optCleanString(), inlineLyricSegmentIndex(), InlineSyncedLyrics(), Block3EmpiricalChallengeTest (+7 more)

### Community 13 - "HomeScreen.kt"
Cohesion: 0.22
Nodes (13): HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel(), YtmSectionHeader() (+5 more)

### Community 16 - "SearchScreen.kt"
Cohesion: 0.17
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 18 - "LibraryScreen.kt"
Cohesion: 0.18
Nodes (11): LibraryGridCard(), LibraryListItem(), LibraryScreen(), LibrarySortOption, A_TO_Z, RECENT, RECENTLY_ADDED, NewBottomSheetOption() (+3 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 22 - "FullPlayerSheet"
Cohesion: 0.21
Nodes (13): PlaybackState, AppleMusicLyricsView(), AppleMusicVolumeBar(), FullPlayerSheet(), MiniPlayerBar(), PlayerActionRow(), PlayerRelatedSheetContent(), QueueDragHeader() (+5 more)

### Community 24 - "AudioPlayerService.kt"
Cohesion: 0.06
Nodes (3): AudioPlayerService, Listener, CachedStreamUrl

### Community 27 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.05
Nodes (8): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler, StreamResolver, MimeAndContainerDetectionStressTest, ContextWrapper, InvocationHandler, StreamResolver

### Community 28 - "UserSettingsRepositoryImpl"
Cohesion: 0.05
Nodes (11): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL (+3 more)

### Community 29 - "Track"
Cohesion: 0.12
Nodes (3): Track, UserAffinityRepository, UserAffinityRepositoryImpl

### Community 32 - "MigrationScreen"
Cohesion: 0.11
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 35 - "SearchResult"
Cohesion: 0.19
Nodes (4): SearchFilter, ALL, SONGS, SearchResult

### Community 39 - "MainScreen.kt"
Cohesion: 0.08
Nodes (9): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar(), SaveToPlaylistSheet() (+1 more)

### Community 41 - "CompositeStreamResolver"
Cohesion: 0.15
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 42 - "HomeSection"
Cohesion: 0.17
Nodes (12): AlbumDetail, HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID (+4 more)

### Community 44 - "AudioQuality"
Cohesion: 0.18
Nodes (7): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, StreamResolver

### Community 48 - "RadioTuningMode"
Cohesion: 0.27
Nodes (6): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR, SmartRadioEngine

### Community 57 - "NewPipeStreamResolver.kt"
Cohesion: 0.13
Nodes (3): NerfMusicApp, OkHttpDownloader, NewPipeStreamResolverTest

### Community 61 - "InnerTubeStreamResolver"
Cohesion: 0.27
Nodes (3): CandidateFormat, ClientConfig, InnerTubeStreamResolver

### Community 65 - "LibraryFilter"
Cohesion: 0.29
Nodes (7): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS

### Community 74 - "MusicRepository"
Cohesion: 0.27
Nodes (3): QueueFilterChip, UpNextResult, MusicRepository

### Community 81 - "QueueAndConnectivityChallengerTest"
Cohesion: 0.12
Nodes (13): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+5 more)

## Knowledge Gaps
- **54 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+49 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 439 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **54 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `ArtistScreen.kt`, `NewPipeStreamResolver.kt`, `LyricsData`, `ExploreScreen.kt`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `SearchScreen.kt`, `LibraryScreen.kt`, `SpotifyCanvasRepositoryImpl`, `UserHistoryRepositoryImpl`, `FullPlayerSheet`, `LibraryRepository`, `AudioPlayerService.kt`, `MusicViewModel.kt`, `UserSettingsRepositoryImpl`, `Track`, `MigrationScreen`, `SmartDownloadWorker`, `SearchResult`, `MainScreen.kt`, `HomeSection`, `AudioQuality`, `MainActivity.kt`, `RadioTuningMode`, `.loadHomeFeed`, `DownloadedTrack`, `SmartDownloadManager.kt`, `.initializePlayerController`, `.seekTo`, `MusicRepository`, `TrackRelatedData`, `Artist`, `.clearStreamCache`?**
  _High betweenness centrality (0.243) - this node is a cross-community bridge._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _54 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.022222222222222223 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `InnerTubeCatalogClient`, `PlayerScreen.kt`, `AudioMetadataTagger`, `MigrationManager`, `MusicViewModel`, `ArtistScreen.kt`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `SearchScreen.kt`, `LibraryScreen.kt`, `UserHistoryRepositoryImpl`, `FullPlayerSheet`, `LibraryRepository`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `DownloadChunkRangeStressTest.kt`, `SearchResult`, `DownloadedTrackEntity`, `InnerTubeCatalogClientTest`, `MainScreen.kt`, `InnerTubeCatalogClient.kt`, `HomeSection`, `PlaylistOptionsBottomSheet.kt`, `DownloadedTrackEntity`, `PlayerUiUtilsTest.kt`, `RadioTuningMode`, `DownloadedTrack`, `SmartDownloadManager.kt`, `MusicRepositoryImpl`, `LibraryRepository.kt`, `MusicRepository`, `TrackRelatedData`, `Artist`?**
  _High betweenness centrality (0.199) - this node is a cross-community bridge._
- **Should `AudioMetadataTagger` be split into smaller, more focused modules?**
  _Cohesion score 0.12903225806451613 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `InnerTubeStreamResolverTest`, `SettingsScreen.kt`, `MusicViewModel`, `InnerTubeCatalogClient.kt`, `CompositeStreamResolver`, `NewPipeStreamResolver.kt`, `LyricsData`, `.testExplicitDownloadQualityNotOverriddenByDataSaverInCompositeResolver`, `.clearStreamCache`, `AudioPlayerService.kt`, `MusicViewModel.kt`, `DownloadChunkRangeStressTest.kt`, `UserSettingsRepositoryImpl`, `InnerTubeStreamResolver`, `AudioCoreDownloadsCrossfadeQualityTest.kt`?**
  _High betweenness centrality (0.116) - this node is a cross-community bridge._
- **Should `MigrationManager` be split into smaller, more focused modules?**
  _Cohesion score 0.06753246753246753 - nodes in this community are weakly interconnected._