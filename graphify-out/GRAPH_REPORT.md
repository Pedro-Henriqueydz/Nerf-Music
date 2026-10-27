# Graph Report - Nerf Music  (2026-10-27)

## Corpus Check
- 95 files · ~100,938 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 1678 nodes · 3956 edges · 82 communities (31 shown, 51 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 46 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a40099ec`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AudioMetadataTagger
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- Registro de Alterações (Changelog) - NERF Music
- ArtistScreen.kt
- MimeAndContainerDetectionStressTest.kt
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
- AudioPlayerService.kt
- LibraryRepositoryImpl
- DownloadChunkRangeStressTest.kt
- UserSettingsRepositoryImpl
- UserAffinityRepositoryImpl
- InnerTubeStreamResolver
- AudioCoreDownloadsCrossfadeQualityTest.kt
- MigrationScreen
- SmartDownloadWorker
- SmartDownloadWorker.kt
- SearchResult
- DownloadedTrackEntity
- InnerTubeCatalogClientTest
- .idFor
- MainScreen.kt
- UserHistoryRepository
- CompositeStreamResolver
- HomeSection
- PlaylistOptionsBottomSheet.kt
- AudioQuality
- DownloadedTrackEntity
- PlayerUiUtilsTest.kt
- MainActivity.kt
- RadioTuningMode
- .loadHomeFeed
- MimeAndContainerDetectionStressTest
- SmartDownloadManager.kt
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- Result
- NestedScrollConnection
- .compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure
- NewPipeStreamResolver.kt
- .createTestRepository
- LibraryRepository.kt
- AudioCoreDownloadsCrossfadeQualityTest
- rememberSystemVolume
- StreamResolver
- Listener
- normalizeDynamicPaletteColor
- UpdateNotificationHelper
- MusicRepository
- AppDatabase
- TrackRelatedData
- Track
- Theme.kt
- StreamResolver
- Block3EmpiricalChallengeTest
- CrossfadeEmpiricalTest

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 168 edges
2. `Track` - 133 edges
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

## Communities (82 total, 51 thin omitted)

### Community 2 - "AudioMetadataTagger"
Cohesion: 0.12
Nodes (3): AudioMetadataTagger, ExtractedMetadata, AudioMetadataTaggerTest

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.10
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
Cohesion: 0.06
Nodes (9): LyricsData, SyncedLyricsLine, LyricsRepository, LyricsRepositoryImpl, optCleanString(), inlineLyricSegmentIndex(), InlineSyncedLyrics(), LyricsNullSanitizationStressTest (+1 more)

### Community 13 - "HomeScreen.kt"
Cohesion: 0.22
Nodes (13): HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel(), YtmSectionHeader() (+5 more)

### Community 16 - "SearchScreen.kt"
Cohesion: 0.14
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 18 - "LibraryScreen.kt"
Cohesion: 0.09
Nodes (20): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS, LibraryGridCard() (+12 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 22 - "FullPlayerSheet"
Cohesion: 0.19
Nodes (13): PlaybackState, QueueFilterChip, AppleMusicLyricsView(), AppleMusicVolumeBar(), FullPlayerSheet(), MiniPlayerBar(), PlayerActionRow(), PlayerRelatedSheetContent() (+5 more)

### Community 24 - "AudioPlayerService.kt"
Cohesion: 0.06
Nodes (3): AudioPlayerService, Listener, CachedStreamUrl

### Community 28 - "UserSettingsRepositoryImpl"
Cohesion: 0.06
Nodes (11): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL (+3 more)

### Community 30 - "InnerTubeStreamResolver"
Cohesion: 0.10
Nodes (5): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest, StreamQualitySelectorStressTest

### Community 32 - "MigrationScreen"
Cohesion: 0.10
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 35 - "SearchResult"
Cohesion: 0.29
Nodes (3): SearchFilter, ALL, SONGS

### Community 39 - "MainScreen.kt"
Cohesion: 0.09
Nodes (7): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar()

### Community 41 - "CompositeStreamResolver"
Cohesion: 0.18
Nodes (3): CompositeStreamResolver, createDefaultStreamResolver(), NetworkUtils

### Community 42 - "HomeSection"
Cohesion: 0.24
Nodes (11): HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL (+3 more)

### Community 44 - "AudioQuality"
Cohesion: 0.14
Nodes (6): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH

### Community 45 - "DownloadedTrackEntity"
Cohesion: 0.18
Nodes (4): DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 48 - "RadioTuningMode"
Cohesion: 0.13
Nodes (7): UserAffinityRepository, RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR, SmartRadioEngine

### Community 51 - "SmartDownloadManager.kt"
Cohesion: 0.14
Nodes (3): SmartDownloadManager, SmartDownloadSyncState, SmartDownloadNotificationHelper

### Community 56 - ".compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure"
Cohesion: 0.29
Nodes (3): DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 57 - "NewPipeStreamResolver.kt"
Cohesion: 0.12
Nodes (4): NerfMusicApp, NewPipeStreamResolver, OkHttpDownloader, NewPipeStreamResolverTest

### Community 62 - "AudioCoreDownloadsCrossfadeQualityTest"
Cohesion: 0.13
Nodes (4): AudioCoreDownloadsCrossfadeQualityTest, InvocationHandler, StreamResolver, StreamResolver

### Community 76 - "TrackRelatedData"
Cohesion: 0.33
Nodes (3): RelatedSection, TrackRelatedData, RelatedMultiTierFallbackTest

### Community 77 - "Track"
Cohesion: 0.17
Nodes (6): AlbumDetail, Artist, ArtistDetail, SearchResult, Track, UpNextResult

### Community 81 - "Block3EmpiricalChallengeTest"
Cohesion: 0.07
Nodes (19): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+11 more)

## Knowledge Gaps
- **54 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+49 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 439 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **51 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `ArtistScreen.kt`, `LyricsData`, `ExploreScreen.kt`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `SearchScreen.kt`, `LibraryScreen.kt`, `SpotifyCanvasRepositoryImpl`, `FullPlayerSheet`, `LibraryRepository`, `MusicViewModel.kt`, `UserSettingsRepositoryImpl`, `MigrationScreen`, `SmartDownloadWorker`, `SearchResult`, `MainScreen.kt`, `UserHistoryRepository`, `HomeSection`, `AudioQuality`, `DownloadedTrackEntity`, `MainActivity.kt`, `RadioTuningMode`, `.loadHomeFeed`, `SmartDownloadManager.kt`, `NewPipeStreamResolver.kt`, `.initializePlayerController`, `.seekTo`, `MusicRepository`, `TrackRelatedData`, `Track`?**
  _High betweenness centrality (0.247) - this node is a cross-community bridge._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _54 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.023529411764705882 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `.parseResponsiveListItem`, `PlayerScreen.kt`, `AudioMetadataTagger`, `MigrationManager`, `MusicViewModel`, `ArtistScreen.kt`, `LyricsData`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `SearchScreen.kt`, `LibraryScreen.kt`, `UserHistoryRepositoryImpl`, `FullPlayerSheet`, `LibraryRepository`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `DownloadChunkRangeStressTest.kt`, `UserAffinityRepositoryImpl`, `SearchResult`, `InnerTubeCatalogClientTest`, `UserHistoryRepository`, `HomeSection`, `PlaylistOptionsBottomSheet.kt`, `DownloadedTrackEntity`, `PlayerUiUtilsTest.kt`, `RadioTuningMode`, `SmartDownloadManager.kt`, `MusicRepositoryImpl`, `.createTestRepository`, `LibraryRepository.kt`, `TrackRelatedData`?**
  _High betweenness centrality (0.212) - this node is a cross-community bridge._
- **Should `AudioMetadataTagger` be split into smaller, more focused modules?**
  _Cohesion score 0.12310606060606061 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `StreamResolver`, `.compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure`, `SettingsScreen.kt`, `MusicViewModel`, `CompositeStreamResolver`, `MimeAndContainerDetectionStressTest.kt`, `LyricsData`, `StreamResolver`, `AudioCoreDownloadsCrossfadeQualityTest`, `AudioPlayerService.kt`, `NewPipeStreamResolver.kt`, `MusicViewModel.kt`, `DownloadChunkRangeStressTest.kt`, `UserSettingsRepositoryImpl`, `InnerTubeStreamResolver`, `AudioCoreDownloadsCrossfadeQualityTest.kt`?**
  _High betweenness centrality (0.126) - this node is a cross-community bridge._
- **Should `MigrationManager` be split into smaller, more focused modules?**
  _Cohesion score 0.06753246753246753 - nodes in this community are weakly interconnected._