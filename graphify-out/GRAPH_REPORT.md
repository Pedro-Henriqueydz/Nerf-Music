# Graph Report - Nerf Music  (2026-10-27)

## Corpus Check
- 95 files · ~101,820 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 1684 nodes · 3970 edges · 84 communities (32 shown, 52 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 54 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e199898d`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- InnerTubeCatalogClient
- AudioMetadataTagger
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- NERF Music
- ArtistScreen.kt
- DownloadChunkRangeStressTest.kt
- steps
- LyricsData
- AudioPlayerService
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
- Registro de Alterações (Changelog) - NERF Music
- UserSettingsRepositoryImpl
- UserAffinityRepositoryImpl
- InnerTubeStreamResolverTest
- AudioCoreDownloadsCrossfadeQualityTest.kt
- MigrationScreen
- SmartDownloadWorker.kt
- SearchResult
- DownloadedTrackEntity
- InnerTubeCatalogClientTest
- Track
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
- InnerTubeCatalogClient.kt
- SmartDownloadManager.kt
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- NestedScrollConnection
- .compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure
- NewPipeStreamResolver.kt
- InnerTubeStreamResolver
- LibraryRepository.kt
- OkHttpDownloader.kt
- AudioCoreDownloadsCrossfadeQualityTest
- .createExoPlayer
- rememberSystemVolume
- DownloadedTrack
- Listener
- normalizeDynamicPaletteColor
- .syncNow
- MusicRepository
- AppDatabase
- TrackRelatedData
- Artist
- DownloadAudioQuality
- NetworkUtils
- AudioMetadataTaggerTest.kt
- QueueAndConnectivityChallengerTest
- [1.3.6] - 2026-10-27

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
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt
- `Corrigido` --references--> `AudioPlayerService`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/player/AudioPlayerService.kt
- `Corrigido` --references--> `CompositeStreamResolver`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/player/CompositeStreamResolver.kt
- `Adicionado` --references--> `AnimatedEqualizerBars()`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/ui/components/AnimatedEqualizerBars.kt

## Import Cycles
- None detected.

## Communities (84 total, 52 thin omitted)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.10
Nodes (33): AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting, SettingsMainContent() (+25 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.06
Nodes (8): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest, LyricsRepositorySanitizationTest

### Community 7 - "NERF Music"
Cohesion: 0.17
Nodes (9): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão (+1 more)

### Community 8 - "ArtistScreen.kt"
Cohesion: 0.13
Nodes (9): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow() (+1 more)

### Community 9 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.05
Nodes (8): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler, StreamResolver, MimeAndContainerDetectionStressTest, ContextWrapper, InvocationHandler, StreamResolver

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 11 - "LyricsData"
Cohesion: 0.06
Nodes (14): LyricsData, SyncedLyricsLine, LyricsRepository, LyricsRepositoryImpl, optCleanString(), inlineLyricSegmentIndex(), InlineSyncedLyrics(), Block3EmpiricalChallengeTest (+6 more)

### Community 13 - "HomeScreen.kt"
Cohesion: 0.22
Nodes (13): HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel(), YtmSectionHeader() (+5 more)

### Community 16 - "SearchScreen.kt"
Cohesion: 0.15
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 18 - "LibraryScreen.kt"
Cohesion: 0.10
Nodes (18): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS, LibraryGridCard() (+10 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.19
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 22 - "FullPlayerSheet"
Cohesion: 0.21
Nodes (13): PlaybackState, AppleMusicLyricsView(), AppleMusicVolumeBar(), FullPlayerSheet(), MiniPlayerBar(), PlayerActionRow(), PlayerRelatedSheetContent(), QueueDragHeader() (+5 more)

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.12
Nodes (17): [1.0.8] - 2026-10-23, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.2] - 2026-10-25, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25, [1.3.5] - 2026-10-27, Adicionado (+9 more)

### Community 28 - "UserSettingsRepositoryImpl"
Cohesion: 0.10
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepositoryImpl

### Community 31 - "AudioCoreDownloadsCrossfadeQualityTest.kt"
Cohesion: 0.06
Nodes (6): ImportedPlaylistIdentity, ContextWrapper, InvocationHandler, CrossfadeEmpiricalTest, ImportedPlaylistIdentityTest, StreamQualitySelectorStressTest

### Community 32 - "MigrationScreen"
Cohesion: 0.11
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 34 - "SmartDownloadWorker.kt"
Cohesion: 0.06
Nodes (5): MainActivity, NERFMusicTheme(), UpdateCheckWorker, UpdateNotificationHelper, SmartDownloadWorker

### Community 35 - "SearchResult"
Cohesion: 0.19
Nodes (4): SearchFilter, ALL, SONGS, SearchResult

### Community 38 - "Track"
Cohesion: 0.17
Nodes (5): Track, HomeFeedSynthesizer, UserAffinityRepository, [1.1.0] - 2026-10-24, Adicionado

### Community 39 - "MainScreen.kt"
Cohesion: 0.08
Nodes (9): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar(), SaveToPlaylistSheet() (+1 more)

### Community 41 - "CompositeStreamResolver"
Cohesion: 0.23
Nodes (3): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest

### Community 42 - "HomeSection"
Cohesion: 0.18
Nodes (11): AlbumDetail, HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID (+3 more)

### Community 43 - "PlaylistOptionsBottomSheet.kt"
Cohesion: 0.07
Nodes (4): ExploreScreen(), YtmExploreQuickButton(), PlaylistOptionItem(), PlaylistOptionsBottomSheet()

### Community 44 - "AudioQuality"
Cohesion: 0.16
Nodes (7): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, StreamResolver

### Community 48 - "RadioTuningMode"
Cohesion: 0.27
Nodes (6): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR, SmartRadioEngine

### Community 58 - "InnerTubeStreamResolver"
Cohesion: 0.29
Nodes (3): CandidateFormat, ClientConfig, InnerTubeStreamResolver

### Community 62 - "AudioCoreDownloadsCrossfadeQualityTest"
Cohesion: 0.17
Nodes (3): AudioCoreDownloadsCrossfadeQualityTest, StreamResolver, StreamResolver

### Community 76 - "TrackRelatedData"
Cohesion: 0.42
Nodes (3): RelatedSection, TrackRelatedData, RelatedMultiTierFallbackTest

### Community 78 - "DownloadAudioQuality"
Cohesion: 0.25
Nodes (5): DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 81 - "QueueAndConnectivityChallengerTest"
Cohesion: 0.12
Nodes (13): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+5 more)

### Community 83 - "[1.3.6] - 2026-10-27"
Cohesion: 0.50
Nodes (4): [1.3.6] - 2026-10-27, Adicionado, Corrigido, Testes

## Knowledge Gaps
- **54 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+49 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 440 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **52 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `ArtistScreen.kt`, `LyricsData`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `SearchScreen.kt`, `LibraryScreen.kt`, `SpotifyCanvasRepositoryImpl`, `UserHistoryRepositoryImpl`, `FullPlayerSheet`, `LibraryRepository`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `UserSettingsRepositoryImpl`, `MigrationScreen`, `SmartDownloadWorker.kt`, `SearchResult`, `Track`, `MainScreen.kt`, `HomeSection`, `PlaylistOptionsBottomSheet.kt`, `AudioQuality`, `UserSettingsRepository`, `RadioTuningMode`, `.loadHomeFeed`, `SmartDownloadManager.kt`, `NewPipeStreamResolver.kt`, `DownloadedTrack`, `.seekTo`, `MusicRepository`, `TrackRelatedData`, `Artist`, `DownloadAudioQuality`, `.clearStreamCache`?**
  _High betweenness centrality (0.235) - this node is a cross-community bridge._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _54 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.023529411764705882 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `InnerTubeCatalogClient`, `PlayerScreen.kt`, `AudioMetadataTagger`, `MigrationManager`, `MusicViewModel`, `ArtistScreen.kt`, `DownloadChunkRangeStressTest.kt`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `SearchScreen.kt`, `LibraryScreen.kt`, `UserHistoryRepositoryImpl`, `FullPlayerSheet`, `LibraryRepository`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `UserAffinityRepositoryImpl`, `log`, `SearchResult`, `InnerTubeCatalogClientTest`, `MainScreen.kt`, `HomeSection`, `PlaylistOptionsBottomSheet.kt`, `DownloadedTrackEntity`, `PlayerUiUtilsTest.kt`, `RadioTuningMode`, `InnerTubeCatalogClient.kt`, `SmartDownloadManager.kt`, `MusicRepositoryImpl`, `AudioMetadataTagger.kt`, `LibraryRepository.kt`, `DownloadedTrack`, `TrackRelatedData`, `Artist`, `AudioMetadataTaggerTest.kt`?**
  _High betweenness centrality (0.212) - this node is a cross-community bridge._
- **Should `MigrationManager` be split into smaller, more focused modules?**
  _Cohesion score 0.06753246753246753 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `MusicViewModel.kt`, `SettingsScreen.kt`, `.compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure`, `MusicViewModel`, `CompositeStreamResolver`, `DownloadChunkRangeStressTest.kt`, `LyricsData`, `AudioPlayerService`, `UserSettingsRepository`, `NetworkUtils`, `.clearStreamCache`, `InnerTubeStreamResolverTest`, `AudioPlayerService.kt`, `NewPipeStreamResolver.kt`, `InnerTubeStreamResolver`, `UserSettingsRepositoryImpl`, `AudioCoreDownloadsCrossfadeQualityTest`, `AudioCoreDownloadsCrossfadeQualityTest.kt`?**
  _High betweenness centrality (0.122) - this node is a cross-community bridge._
- **Should `SettingsScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.09954751131221719 - nodes in this community are weakly interconnected._