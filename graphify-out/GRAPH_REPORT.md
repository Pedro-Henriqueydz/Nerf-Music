# Graph Report - Nerf Music  (2026-10-27)

## Corpus Check
- 97 files · ~105,164 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 1726 nodes · 4081 edges · 73 communities (38 shown, 35 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 61 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `40f6faa9`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SearchResult
- PlayerScreen.kt
- AudioMetadataTagger
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- HomeSection
- ArtistScreen
- DownloadChunkRangeStressTest.kt
- steps
- LyricsData
- DownloadedTrackEntity
- HomeScreen.kt
- .syncTimelineState
- MainScreen.kt
- SearchScreen.kt
- LibraryScreen.kt
- AnimatedEqualizerBars.kt
- SpotifyCanvasRepositoryImpl
- UserHistoryRepositoryImpl
- FullPlayerSheet
- Track
- AudioPlayerService.kt
- LibraryRepositoryImpl
- Registro de Alterações (Changelog) - NERF Music
- UserSettingsRepositoryImpl
- ExploreScreen.kt
- AppDatabase
- AudioCoreDownloadsCrossfadeQualityTest.kt
- MigrationScreen
- SmartDownloadWorker.kt
- MimeAndContainerDetectionStressTest.kt
- InnerTubeCatalogClientTest
- InnerTubeStreamResolver
- .syncNow
- DownloadAudioQuality
- CompositeStreamResolver
- DataSaverMode
- ArtistScreen.kt
- AudioQuality
- DownloadedTrackDao
- UserSettingsRepository
- Track
- .loadHomeFeed
- SmartDownloadManager
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- InnerTubeCatalogClient
- MimeAndContainerDetectionStressTest
- v1.3.5 — Core de Áudio, Parser, Metadados & Sincronização
- NewPipeStreamResolver.kt
- AudioMetadataTaggerTest.kt
- .isConnectedToWifi
- AudioCoreDownloadsCrossfadeQualityTest
- NERF Music
- TrackRelatedData
- DataSaverInterceptor.kt
- MusicRepository
- .idFor
- QueueAndConnectivityChallengerTest
- CrossfadeEmpiricalTest

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 173 edges
2. `Track` - 134 edges
3. `InnerTubeCatalogClient` - 55 edges
4. `LibraryRepositoryImpl` - 48 edges
5. `AudioQuality` - 48 edges
6. `MigrationManager` - 46 edges
7. `AudioPlayerService` - 40 edges
8. `UserSettingsRepositoryImpl` - 36 edges
9. `UserSettingsRepository` - 28 edges
10. `Block3EmpiricalChallengeTest` - 28 edges

## Surprising Connections (you probably didn't know these)
- `Adicionado` --references--> `AudioMetadataTagger`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/AudioMetadataTagger.kt
- `Adicionado` --references--> `ConnectivityObserver`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/ConnectivityObserver.kt
- `4. Metadados e Sincronização Local de Downloads` --references--> `DownloadedTrackEntity`  [INFERRED]
  RELEASE_NOTES.md → app/src/main/java/com/nerf/music/data/db/DownloadedTrackEntity.kt
- `Adicionado` --references--> `HomeFeedSynthesizer`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/HomeFeedSynthesizer.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt

## Import Cycles
- None detected.

## Communities (73 total, 35 thin omitted)

### Community 1 - "PlayerScreen.kt"
Cohesion: 0.02
Nodes (4): largestFittingCharacterBoundary(), normalizeDynamicPaletteColor(), BroadcastReceiver, splitInlineLyricIntoSegments()

### Community 2 - "AudioMetadataTagger"
Cohesion: 0.36
Nodes (3): AudioMetadataTagger, ExtractedMetadata, 4. Metadados e Sincronização Local de Downloads

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.09
Nodes (34): SmartDownloadSyncState, AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting (+26 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 7 - "HomeSection"
Cohesion: 0.08
Nodes (12): HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL (+4 more)

### Community 8 - "ArtistScreen"
Cohesion: 0.61
Nodes (6): ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow(), ArtistVideoCard()

### Community 9 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.11
Nodes (3): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 11 - "LyricsData"
Cohesion: 0.05
Nodes (15): LyricsData, SyncedLyricsLine, LyricsRepository, LyricsRepositoryImpl, optCleanString(), inlineLyricSegmentIndex(), InlineSyncedLyrics(), Block3EmpiricalChallengeTest (+7 more)

### Community 12 - "DownloadedTrackEntity"
Cohesion: 0.17
Nodes (4): DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 13 - "HomeScreen.kt"
Cohesion: 0.19
Nodes (13): HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel(), YtmSectionHeader() (+5 more)

### Community 16 - "MainScreen.kt"
Cohesion: 0.08
Nodes (9): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar(), SaveToPlaylistSheet() (+1 more)

### Community 17 - "SearchScreen.kt"
Cohesion: 0.14
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 18 - "LibraryScreen.kt"
Cohesion: 0.12
Nodes (18): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS, LibraryGridCard() (+10 more)

### Community 19 - "AnimatedEqualizerBars.kt"
Cohesion: 0.09
Nodes (3): AnimatedEqualizerBars(), PlaylistOptionItem(), PlaylistOptionsBottomSheet()

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 22 - "FullPlayerSheet"
Cohesion: 0.09
Nodes (17): PlaybackState, AppleMusicLyricsView(), AppleMusicVolumeBar(), formatDurationMs(), FullPlayerSheet(), Listener, NestedScrollConnection, MiniPlayerBar() (+9 more)

### Community 24 - "AudioPlayerService.kt"
Cohesion: 0.06
Nodes (3): AudioPlayerService, Listener, CachedStreamUrl

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.10
Nodes (21): [1.0.8] - 2026-10-23, [1.1.0] - 2026-10-24, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.2] - 2026-10-25, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25, Adicionado (+13 more)

### Community 31 - "AudioCoreDownloadsCrossfadeQualityTest.kt"
Cohesion: 0.11
Nodes (3): ContextWrapper, InvocationHandler, RelatedMultiTierFallbackTest

### Community 32 - "MigrationScreen"
Cohesion: 0.10
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 34 - "SmartDownloadWorker.kt"
Cohesion: 0.06
Nodes (5): MainActivity, NERFMusicTheme(), UpdateCheckWorker, UpdateNotificationHelper, SmartDownloadWorker

### Community 36 - "MimeAndContainerDetectionStressTest.kt"
Cohesion: 0.11
Nodes (3): ContextWrapper, InvocationHandler, StreamResolver

### Community 38 - "InnerTubeStreamResolver"
Cohesion: 0.09
Nodes (5): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest, StreamQualitySelectorStressTest

### Community 40 - "DownloadAudioQuality"
Cohesion: 0.25
Nodes (5): DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 41 - "CompositeStreamResolver"
Cohesion: 0.15
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 42 - "DataSaverMode"
Cohesion: 0.29
Nodes (4): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC

### Community 43 - "ArtistScreen.kt"
Cohesion: 0.12
Nodes (3): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow()

### Community 44 - "AudioQuality"
Cohesion: 0.15
Nodes (8): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, StreamResolver, StreamResolver

### Community 48 - "Track"
Cohesion: 0.09
Nodes (9): Track, UserAffinityRepository, UserAffinityRepositoryImpl, RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR (+1 more)

### Community 56 - "v1.3.5 — Core de Áudio, Parser, Metadados & Sincronização"
Cohesion: 0.25
Nodes (5): 1. Recomendações e Conteúdo Relacionado, 2. Letras e Exibição, 3. Core de Áudio, Crossfade e Segundo Plano, 🎵 NERF Music v1.3.5, v1.3.5 — Core de Áudio, Parser, Metadados & Sincronização

### Community 57 - "NewPipeStreamResolver.kt"
Cohesion: 0.10
Nodes (4): NerfMusicApp, NewPipeStreamResolver, OkHttpDownloader, NewPipeStreamResolverTest

### Community 59 - "AudioMetadataTaggerTest.kt"
Cohesion: 0.24
Nodes (5): AudioMetadataTaggerTest, [1.3.5] - 2026-10-27 [RELEASED], Adicionado, Corrigido, Testes

### Community 62 - "AudioCoreDownloadsCrossfadeQualityTest"
Cohesion: 0.17
Nodes (3): AudioCoreDownloadsCrossfadeQualityTest, StreamResolver, StreamResolver

### Community 65 - "NERF Music"
Cohesion: 0.25
Nodes (8): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão

### Community 74 - "MusicRepository"
Cohesion: 0.14
Nodes (8): AlbumDetail, ArtistDetail, SearchFilter, ALL, SONGS, QueueFilterChip, UpNextResult, MusicRepository

### Community 81 - "QueueAndConnectivityChallengerTest"
Cohesion: 0.07
Nodes (15): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+7 more)

## Knowledge Gaps
- **54 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+49 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 447 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **35 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `SearchResult`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `HomeSection`, `ArtistScreen`, `LyricsData`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `MainScreen.kt`, `SearchScreen.kt`, `LibraryScreen.kt`, `SpotifyCanvasRepositoryImpl`, `FullPlayerSheet`, `Track`, `MusicViewModel.kt`, `ExploreScreen.kt`, `MigrationScreen`, `SmartDownloadWorker.kt`, `DownloadAudioQuality`, `DataSaverMode`, `ArtistScreen.kt`, `AudioQuality`, `UserSettingsRepository`, `Track`, `.loadHomeFeed`, `SmartDownloadManager`, `NewPipeStreamResolver.kt`, `.isConnectedToWifi`, `.loadCanvasVideo`, `TrackRelatedData`, `MusicRepository`, `.clearStreamCache`, `QueueAndConnectivityChallengerTest`?**
  _High betweenness centrality (0.232) - this node is a cross-community bridge._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _54 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.023106840539922215 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `SearchResult`, `PlayerScreen.kt`, `AudioMetadataTagger`, `MigrationManager`, `MusicViewModel`, `HomeSection`, `ArtistScreen`, `DownloadChunkRangeStressTest.kt`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `MainScreen.kt`, `SearchScreen.kt`, `LibraryScreen.kt`, `UserHistoryRepositoryImpl`, `FullPlayerSheet`, `Track`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `InnerTubeCatalogClient.kt`, `LibraryRepository.kt`, `InnerTubeCatalogClientTest`, `ArtistScreen.kt`, `SmartDownloadManager`, `MusicRepositoryImpl`, `InnerTubeCatalogClient`, `AudioMetadataTaggerTest.kt`, `.isConnectedToWifi`, `.loadCanvasVideo`, `TrackRelatedData`, `MusicRepository`, `AudioMetadataTagger.kt`?**
  _High betweenness centrality (0.191) - this node is a cross-community bridge._
- **Should `MigrationManager` be split into smaller, more focused modules?**
  _Cohesion score 0.06753246753246753 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `InnerTubeCatalogClient.kt`, `SettingsScreen.kt`, `MimeAndContainerDetectionStressTest.kt`, `InnerTubeStreamResolver`, `MusicViewModel`, `CompositeStreamResolver`, `DownloadChunkRangeStressTest.kt`, `LyricsData`, `UserSettingsRepository`, `.clearStreamCache`, `AudioPlayerService.kt`, `NewPipeStreamResolver.kt`, `MusicViewModel.kt`, `UserSettingsRepositoryImpl`, `AudioCoreDownloadsCrossfadeQualityTest`, `AudioCoreDownloadsCrossfadeQualityTest.kt`?**
  _High betweenness centrality (0.113) - this node is a cross-community bridge._
- **Should `SettingsScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.09158249158249158 - nodes in this community are weakly interconnected._