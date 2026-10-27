# Graph Report - Nerf Music  (2026-10-27)

## Corpus Check
- 95 files · ~102,357 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 1687 nodes · 3977 edges · 86 communities (34 shown, 52 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 58 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `080729a7`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SearchResult
- AudioMetadataTagger
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- UserHistoryRepository
- ArtistScreen.kt
- DownloadChunkRangeStressTest.kt
- steps
- LyricsData
- PlaylistOptionsBottomSheet.kt
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
- AudioPlayerService.kt
- LibraryRepositoryImpl
- MusicViewModel.kt
- Registro de Alterações (Changelog) - NERF Music
- UserSettingsRepositoryImpl
- UserAffinityRepositoryImpl
- InnerTubeStreamResolver
- AudioCoreDownloadsCrossfadeQualityTest.kt
- MigrationScreen
- SmartDownloadWorker.kt
- DownloadedTrackEntity
- InnerTubeCatalogClientTest
- MainActivity.kt
- MainScreen.kt
- ArtistScreen
- CompositeStreamResolver
- HomeSection
- ExploreScreen.kt
- AudioQuality
- DownloadedTrackEntity
- PlayerUiUtilsTest.kt
- UserSettingsRepository
- RadioTuningMode
- .loadHomeFeed
- NewPipeStreamResolverTest.kt
- SmartDownloadManager
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- InnerTubeCatalogClient
- NestedScrollConnection
- v1.3.5 — Core de Áudio, Parser, Metadados & Sincronização
- NewPipeStreamResolver.kt
- AudioMetadataTaggerTest.kt
- LibraryRepository.kt
- OkHttpDownloader.kt
- AudioCoreDownloadsCrossfadeQualityTest
- Corrigido
- rememberSystemVolume
- NERF Music
- Listener
- normalizeDynamicPaletteColor
- .syncNow
- HomeSection
- AppDatabase
- .idFor
- SmartDownloadWorker
- LibraryFilter
- QueueAndConnectivityChallengerTest
- UpdateNotificationHelper
- .testExplicitDownloadQualityNotOverriddenByDataSaverInCompositeResolver
- Theme.kt
- CrossfadeEmpiricalTest

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
- `4. Metadados e Sincronização Local de Downloads` --references--> `DownloadedTrackEntity`  [INFERRED]
  RELEASE_NOTES.md → app/src/main/java/com/nerf/music/data/db/DownloadedTrackEntity.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt
- `Adicionado` --references--> `UserAffinityRepository`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/UserAffinityRepository.kt
- `Corrigido` --references--> `AudioPlayerService`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/player/AudioPlayerService.kt

## Import Cycles
- None detected.

## Communities (86 total, 52 thin omitted)

### Community 2 - "AudioMetadataTagger"
Cohesion: 0.36
Nodes (3): AudioMetadataTagger, ExtractedMetadata, 4. Metadados e Sincronização Local de Downloads

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.10
Nodes (34): SmartDownloadSyncState, AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting (+26 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 8 - "ArtistScreen.kt"
Cohesion: 0.10
Nodes (3): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow()

### Community 9 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.05
Nodes (8): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler, StreamResolver, MimeAndContainerDetectionStressTest, ContextWrapper, InvocationHandler, StreamResolver

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 11 - "LyricsData"
Cohesion: 0.05
Nodes (15): LyricsData, SyncedLyricsLine, LyricsRepository, LyricsRepositoryImpl, optCleanString(), inlineLyricSegmentIndex(), InlineSyncedLyrics(), Block3EmpiricalChallengeTest (+7 more)

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
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 22 - "FullPlayerSheet"
Cohesion: 0.21
Nodes (13): PlaybackState, AppleMusicLyricsView(), AppleMusicVolumeBar(), FullPlayerSheet(), MiniPlayerBar(), PlayerActionRow(), PlayerRelatedSheetContent(), QueueDragHeader() (+5 more)

### Community 23 - "Track"
Cohesion: 0.08
Nodes (4): Track, LibraryRepository, UserAffinityRepository, SmartRadioEngine

### Community 24 - "AudioPlayerService.kt"
Cohesion: 0.06
Nodes (3): AudioPlayerService, Listener, CachedStreamUrl

### Community 26 - "MusicViewModel.kt"
Cohesion: 0.10
Nodes (6): SearchFilter, ALL, SONGS, HomeFeedSynthesizer, [1.1.0] - 2026-10-24, Adicionado

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.12
Nodes (16): [1.0.8] - 2026-10-23, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.2] - 2026-10-25, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25, Adicionado, Adicionado (+8 more)

### Community 28 - "UserSettingsRepositoryImpl"
Cohesion: 0.08
Nodes (6): DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH, UserSettingsRepositoryImpl

### Community 30 - "InnerTubeStreamResolver"
Cohesion: 0.10
Nodes (5): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest, StreamQualitySelectorStressTest

### Community 32 - "MigrationScreen"
Cohesion: 0.14
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 39 - "MainScreen.kt"
Cohesion: 0.07
Nodes (9): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar(), SaveToPlaylistSheet() (+1 more)

### Community 40 - "ArtistScreen"
Cohesion: 0.61
Nodes (6): ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow(), ArtistVideoCard()

### Community 41 - "CompositeStreamResolver"
Cohesion: 0.16
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 42 - "HomeSection"
Cohesion: 0.20
Nodes (10): HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL, TWO_ROW_CAROUSEL, YOUR_DOWNLOADS, RelatedSection (+2 more)

### Community 44 - "AudioQuality"
Cohesion: 0.18
Nodes (7): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, StreamResolver

### Community 45 - "DownloadedTrackEntity"
Cohesion: 0.18
Nodes (4): DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 47 - "UserSettingsRepository"
Cohesion: 0.10
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepository

### Community 48 - "RadioTuningMode"
Cohesion: 0.33
Nodes (5): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR

### Community 56 - "v1.3.5 — Core de Áudio, Parser, Metadados & Sincronização"
Cohesion: 0.25
Nodes (5): 1. Recomendações e Conteúdo Relacionado, 2. Letras e Exibição, 3. Core de Áudio, Crossfade e Segundo Plano, 🎵 NERF Music v1.3.5, v1.3.5 — Core de Áudio, Parser, Metadados & Sincronização

### Community 63 - "Corrigido"
Cohesion: 0.50
Nodes (4): [1.3.5] - 2026-10-27, Adicionado, Corrigido, Testes

### Community 65 - "NERF Music"
Cohesion: 0.25
Nodes (8): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão

### Community 74 - "HomeSection"
Cohesion: 0.16
Nodes (8): AlbumDetail, ArtistDetail, HomeChip, HomeFeedData, HomeSection, QueueFilterChip, UpNextResult, MusicRepository

### Community 80 - "LibraryFilter"
Cohesion: 0.29
Nodes (7): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS

### Community 81 - "QueueAndConnectivityChallengerTest"
Cohesion: 0.12
Nodes (13): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+5 more)

## Knowledge Gaps
- **54 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+49 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 440 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **52 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `SearchResult`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `UserHistoryRepository`, `ArtistScreen.kt`, `LyricsData`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `SearchScreen.kt`, `LibraryScreen.kt`, `SpotifyCanvasRepositoryImpl`, `FullPlayerSheet`, `Track`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `UserSettingsRepositoryImpl`, `MigrationScreen`, `MainActivity.kt`, `MainScreen.kt`, `ArtistScreen`, `HomeSection`, `ExploreScreen.kt`, `AudioQuality`, `DownloadedTrackEntity`, `UserSettingsRepository`, `RadioTuningMode`, `.loadHomeFeed`, `SmartDownloadManager`, `NewPipeStreamResolver.kt`, `.seekTo`, `HomeSection`, `.initializePlayerController`, `SmartDownloadWorker`?**
  _High betweenness centrality (0.227) - this node is a cross-community bridge._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _54 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.023529411764705882 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `SearchResult`, `PlayerScreen.kt`, `AudioMetadataTagger`, `MigrationManager`, `MusicViewModel`, `UserHistoryRepository`, `ArtistScreen.kt`, `DownloadChunkRangeStressTest.kt`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `SearchScreen.kt`, `LibraryScreen.kt`, `UserHistoryRepositoryImpl`, `FullPlayerSheet`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `UserAffinityRepositoryImpl`, `InnerTubeCatalogClient.kt`, `log`, `InnerTubeCatalogClientTest`, `MainScreen.kt`, `ArtistScreen`, `HomeSection`, `DownloadedTrackEntity`, `PlayerUiUtilsTest.kt`, `RadioTuningMode`, `SmartDownloadManager`, `MusicRepositoryImpl`, `InnerTubeCatalogClient`, `AudioMetadataTaggerTest.kt`, `LibraryRepository.kt`, `HomeSection`, `AudioMetadataTagger.kt`?**
  _High betweenness centrality (0.209) - this node is a cross-community bridge._
- **Should `MigrationManager` be split into smaller, more focused modules?**
  _Cohesion score 0.06753246753246753 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `InnerTubeCatalogClient.kt`, `SettingsScreen.kt`, `MusicViewModel`, `CompositeStreamResolver`, `DownloadChunkRangeStressTest.kt`, `LyricsData`, `UserSettingsRepository`, `.testExplicitDownloadQualityNotOverriddenByDataSaverInCompositeResolver`, `InnerTubeStreamResolver`, `AudioPlayerService.kt`, `NewPipeStreamResolver.kt`, `MusicViewModel.kt`, `UserSettingsRepositoryImpl`, `AudioCoreDownloadsCrossfadeQualityTest`, `AudioCoreDownloadsCrossfadeQualityTest.kt`?**
  _High betweenness centrality (0.123) - this node is a cross-community bridge._
- **Should `SettingsScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.09724238026124818 - nodes in this community are weakly interconnected._