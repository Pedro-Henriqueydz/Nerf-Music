# Graph Report - Nerf Music  (2026-10-27)

## Corpus Check
- 95 files · ~102,318 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 1686 nodes · 3976 edges · 91 communities (32 shown, 59 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 58 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `cc7888f9`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Artist
- AudioMetadataTagger
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- MimeAndContainerDetectionStressTest.kt
- ArtistScreen.kt
- DownloadChunkRangeStressTest.kt
- steps
- LyricsData
- .prefetchTrack
- HomeScreen.kt
- .syncTimelineState
- SearchScreen.kt
- TextStyle
- LibraryScreen.kt
- AnimatedEqualizerBars.kt
- SpotifyCanvasRepositoryImpl
- UserHistoryRepositoryImpl
- FullPlayerSheet
- Track
- LibraryRepositoryImpl
- Registro de Alterações (Changelog) - NERF Music
- UserSettingsRepositoryImpl
- UserAffinityRepositoryImpl
- InnerTubeStreamResolver
- AudioCoreDownloadsCrossfadeQualityTest.kt
- MigrationScreen
- InnerTubeCatalogClient.kt
- SmartDownloadWorker.kt
- SearchResult
- DownloadedTrackEntity
- InnerTubeCatalogClientTest
- MainActivity.kt
- MainScreen.kt
- Listener
- CompositeStreamResolver
- HomeSection
- PlaylistOptionsBottomSheet.kt
- AudioQuality
- DownloadedTrackEntity
- PlayerUiUtilsTest.kt
- UserSettingsRepository
- RadioTuningMode
- .loadHomeFeed
- UpNextResult
- SmartDownloadManager.kt
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- InnerTubeCatalogClient
- NestedScrollConnection
- .compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure
- NewPipeStreamResolver.kt
- AudioMetadataTagger.kt
- LibraryRepository.kt
- OkHttpDownloader.kt
- AudioCoreDownloadsCrossfadeQualityTest
- AudioPlayerService
- rememberSystemVolume
- MimeAndContainerDetectionStressTest
- Listener
- normalizeDynamicPaletteColor
- .syncNow
- MusicRepository
- AppDatabase
- .testRelatedMultiTierSynthesisAlwaysPopulatesContent
- .idFor
- SmartDownloadWorker
- LibraryFilter
- QueueAndConnectivityChallengerTest
- UpdateNotificationHelper
- .testExplicitDownloadQualityNotOverriddenByDataSaverInCompositeResolver
- Theme.kt
- CrossfadeEmpiricalTest
- StreamResolver
- StreamResolver
- StreamResolver

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 168 edges
2. `Track` - 133 edges
3. `InnerTubeCatalogClient` - 55 edges
4. `AudioQuality` - 48 edges
5. `LibraryRepositoryImpl` - 46 edges
6. `MigrationManager` - 46 edges
7. `AudioPlayerService` - 39 edges
8. `UserSettingsRepositoryImpl` - 34 edges
9. `Block3EmpiricalChallengeTest` - 28 edges
10. `LibraryRepository` - 27 edges

## Surprising Connections (you probably didn't know these)
- `Adicionado` --references--> `AudioMetadataTagger`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/AudioMetadataTagger.kt
- `v1.3.6 — Parser, Metadados, Relacionados & Sincronização` --references--> `DownloadedTrackEntity`  [INFERRED]
  RELEASE_NOTES.md → app/src/main/java/com/nerf/music/data/db/DownloadedTrackEntity.kt
- `Adicionado` --references--> `HomeFeedSynthesizer`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/HomeFeedSynthesizer.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt
- `Adicionado` --references--> `UserAffinityRepository`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/UserAffinityRepository.kt

## Import Cycles
- None detected.

## Communities (91 total, 59 thin omitted)

### Community 2 - "AudioMetadataTagger"
Cohesion: 0.34
Nodes (3): AudioMetadataTagger, ExtractedMetadata, v1.3.6 — Parser, Metadados, Relacionados & Sincronização

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.10
Nodes (33): AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting, SettingsMainContent() (+25 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 8 - "ArtistScreen.kt"
Cohesion: 0.13
Nodes (9): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow() (+1 more)

### Community 9 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.11
Nodes (3): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler

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
Cohesion: 0.15
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 18 - "LibraryScreen.kt"
Cohesion: 0.15
Nodes (11): LibraryGridCard(), LibraryListItem(), LibraryScreen(), LibrarySortOption, A_TO_Z, RECENT, RECENTLY_ADDED, NewBottomSheetOption() (+3 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 22 - "FullPlayerSheet"
Cohesion: 0.21
Nodes (13): PlaybackState, AppleMusicLyricsView(), AppleMusicVolumeBar(), FullPlayerSheet(), MiniPlayerBar(), PlayerActionRow(), PlayerRelatedSheetContent(), QueueDragHeader() (+5 more)

### Community 23 - "Track"
Cohesion: 0.08
Nodes (4): Track, UserPlaylist, LibraryRepository, UserAffinityRepository

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.06
Nodes (28): [1.0.8] - 2026-10-23, [1.1.0] - 2026-10-24, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.2] - 2026-10-25, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25, Adicionado (+20 more)

### Community 28 - "UserSettingsRepositoryImpl"
Cohesion: 0.08
Nodes (10): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL (+2 more)

### Community 30 - "InnerTubeStreamResolver"
Cohesion: 0.09
Nodes (5): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest, StreamQualitySelectorStressTest

### Community 32 - "MigrationScreen"
Cohesion: 0.10
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 35 - "SearchResult"
Cohesion: 0.21
Nodes (4): SearchFilter, ALL, SONGS, SearchResult

### Community 39 - "MainScreen.kt"
Cohesion: 0.08
Nodes (9): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar(), SaveToPlaylistSheet() (+1 more)

### Community 42 - "HomeSection"
Cohesion: 0.18
Nodes (12): HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL (+4 more)

### Community 43 - "PlaylistOptionsBottomSheet.kt"
Cohesion: 0.08
Nodes (4): ExploreScreen(), YtmExploreQuickButton(), PlaylistOptionItem(), PlaylistOptionsBottomSheet()

### Community 44 - "AudioQuality"
Cohesion: 0.14
Nodes (6): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH

### Community 45 - "DownloadedTrackEntity"
Cohesion: 0.16
Nodes (4): DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 48 - "RadioTuningMode"
Cohesion: 0.27
Nodes (6): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR, SmartRadioEngine

### Community 53 - "MusicRepositoryImpl"
Cohesion: 0.18
Nodes (3): AlbumDetail, ArtistDetail, MusicRepositoryImpl

### Community 56 - ".compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure"
Cohesion: 0.29
Nodes (3): DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 59 - "AudioMetadataTagger.kt"
Cohesion: 0.12
Nodes (5): AudioMetadataTaggerTest, [1.3.6] - 2026-10-27, Adicionado, Corrigido, Testes

### Community 61 - "OkHttpDownloader.kt"
Cohesion: 0.13
Nodes (3): NerfMusicApp, OkHttpDownloader, NewPipeStreamResolverTest

### Community 63 - "AudioPlayerService"
Cohesion: 0.23
Nodes (3): AudioPlayerService, [1.3.5] - 2026-10-27, Corrigido

### Community 80 - "LibraryFilter"
Cohesion: 0.29
Nodes (7): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS

### Community 81 - "QueueAndConnectivityChallengerTest"
Cohesion: 0.12
Nodes (13): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+5 more)

## Knowledge Gaps
- **54 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+49 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 439 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **59 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `ArtistScreen.kt`, `LyricsData`, `.prefetchTrack`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `SearchScreen.kt`, `LibraryScreen.kt`, `SpotifyCanvasRepositoryImpl`, `UserHistoryRepositoryImpl`, `FullPlayerSheet`, `Track`, `MusicViewModel.kt`, `UserSettingsRepositoryImpl`, `MigrationScreen`, `InnerTubeCatalogClient.kt`, `SearchResult`, `MainActivity.kt`, `MainScreen.kt`, `HomeSection`, `PlaylistOptionsBottomSheet.kt`, `AudioQuality`, `DownloadedTrackEntity`, `UserSettingsRepository`, `RadioTuningMode`, `.loadHomeFeed`, `SmartDownloadManager.kt`, `MusicRepositoryImpl`, `NewPipeStreamResolver.kt`, `.seekTo`, `MusicRepository`, `.initializePlayerController`, `SmartDownloadWorker`?**
  _High betweenness centrality (0.229) - this node is a cross-community bridge._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _54 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.023529411764705882 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `Artist`, `PlayerScreen.kt`, `AudioMetadataTagger`, `MigrationManager`, `MusicViewModel`, `ArtistScreen.kt`, `DownloadChunkRangeStressTest.kt`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `SearchScreen.kt`, `LibraryScreen.kt`, `UserHistoryRepositoryImpl`, `FullPlayerSheet`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `UserAffinityRepositoryImpl`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `InnerTubeCatalogClient.kt`, `SearchResult`, `InnerTubeCatalogClientTest`, `MainScreen.kt`, `HomeSection`, `PlaylistOptionsBottomSheet.kt`, `DownloadedTrackEntity`, `PlayerUiUtilsTest.kt`, `RadioTuningMode`, `UpNextResult`, `SmartDownloadManager.kt`, `MusicRepositoryImpl`, `InnerTubeCatalogClient`, `AudioMetadataTagger.kt`, `LibraryRepository.kt`, `.testRelatedMultiTierSynthesisAlwaysPopulatesContent`, `.downloadTrack`?**
  _High betweenness centrality (0.206) - this node is a cross-community bridge._
- **Should `MigrationManager` be split into smaller, more focused modules?**
  _Cohesion score 0.06753246753246753 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `SettingsScreen.kt`, `MusicViewModel`, `MimeAndContainerDetectionStressTest.kt`, `DownloadChunkRangeStressTest.kt`, `LyricsData`, `.prefetchTrack`, `AudioPlayerService.kt`, `MusicViewModel.kt`, `UserSettingsRepositoryImpl`, `InnerTubeStreamResolver`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `InnerTubeCatalogClient.kt`, `CompositeStreamResolver`, `UserSettingsRepository`, `.compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure`, `NewPipeStreamResolver.kt`, `.testExplicitDownloadQualityNotOverriddenByDataSaverInCompositeResolver`, `StreamResolver`, `StreamResolver`, `StreamResolver`?**
  _High betweenness centrality (0.126) - this node is a cross-community bridge._
- **Should `SettingsScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.1027450980392157 - nodes in this community are weakly interconnected._