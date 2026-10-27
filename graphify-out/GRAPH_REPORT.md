# Graph Report - Nerf Music  (2026-10-27)

## Corpus Check
- 97 files · ~104,744 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 1723 nodes · 4075 edges · 82 communities (32 shown, 50 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 58 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d172595a`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Artist
- PlayerScreen.kt
- AudioMetadataTagger
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- UserHistoryRepository
- ArtistScreen
- DownloadChunkRangeStressTest.kt
- steps
- LyricsData
- DownloadedTrack
- HomeScreen.kt
- .syncTimelineState
- MainActivity.kt
- HomeSection
- MainScreen
- AnimatedEqualizerBars.kt
- SpotifyCanvasRepositoryImpl
- UserHistoryRepositoryImpl
- FullPlayerSheet
- Track
- AudioPlayerService.kt
- LibraryRepositoryImpl
- Registro de Alterações (Changelog) - NERF Music
- UserSettingsRepositoryImpl
- UserAffinityRepositoryImpl
- AudioCoreDownloadsCrossfadeQualityTest.kt
- MigrationScreen
- SmartDownloadWorker.kt
- LibraryRepository.kt
- MimeAndContainerDetectionStressTest.kt
- InnerTubeCatalogClientTest
- InnerTubeStreamResolver
- .syncNow
- NestedScrollConnection
- CompositeStreamResolver
- .testRelatedMultiTierSynthesisAlwaysPopulatesContent
- SearchScreen.kt
- AudioQuality
- DownloadedTrackEntity
- UpdateNotificationHelper
- UserSettingsRepository
- Track
- .loadHomeFeed
- SmartDownloadWorker
- SmartDownloadManager
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- InnerTubeCatalogClient
- MimeAndContainerDetectionStressTest
- v1.3.5 — Core de Áudio, Parser, Metadados & Sincronização
- NewPipeStreamResolver.kt
- SearchResult
- AudioMetadataTaggerTest.kt
- .isConnectedToWifi
- OkHttpDownloader.kt
- AudioCoreDownloadsCrossfadeQualityTest
- Corrigido
- PlayerUiUtilsTest.kt
- NERF Music
- TrackRelatedData
- DataSaverInterceptor.kt
- Theme.kt
- MusicRepository
- .idFor
- QueueAndConnectivityChallengerTest
- LibrarySortOption
- StreamResolver
- CrossfadeEmpiricalTest

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 173 edges
2. `Track` - 134 edges
3. `InnerTubeCatalogClient` - 55 edges
4. `LibraryRepositoryImpl` - 48 edges
5. `AudioQuality` - 48 edges
6. `MigrationManager` - 46 edges
7. `AudioPlayerService` - 39 edges
8. `UserSettingsRepositoryImpl` - 36 edges
9. `UserSettingsRepository` - 28 edges
10. `Block3EmpiricalChallengeTest` - 28 edges

## Surprising Connections (you probably didn't know these)
- `Adicionado` --references--> `AudioMetadataTagger`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/AudioMetadataTagger.kt
- `4. Metadados e Sincronização Local de Downloads` --references--> `DownloadedTrackEntity`  [INFERRED]
  RELEASE_NOTES.md → app/src/main/java/com/nerf/music/data/db/DownloadedTrackEntity.kt
- `Adicionado` --references--> `HomeFeedSynthesizer`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/HomeFeedSynthesizer.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt
- `Adicionado` --references--> `UserAffinityRepository`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/UserAffinityRepository.kt

## Import Cycles
- None detected.

## Communities (82 total, 50 thin omitted)

### Community 0 - "Artist"
Cohesion: 0.22
Nodes (3): Artist, QueueFilterChip, UpNextResult

### Community 1 - "PlayerScreen.kt"
Cohesion: 0.02
Nodes (3): largestFittingCharacterBoundary(), normalizeDynamicPaletteColor(), splitInlineLyricIntoSegments()

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.09
Nodes (34): SmartDownloadSyncState, AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting (+26 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

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

### Community 13 - "HomeScreen.kt"
Cohesion: 0.15
Nodes (15): ExploreScreen(), YtmExploreQuickButton(), HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem() (+7 more)

### Community 17 - "HomeSection"
Cohesion: 0.18
Nodes (12): ArtistDetail, HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID (+4 more)

### Community 18 - "MainScreen"
Cohesion: 0.09
Nodes (28): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS, LibraryGridCard() (+20 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 22 - "FullPlayerSheet"
Cohesion: 0.13
Nodes (15): PlaybackState, AppleMusicLyricsView(), AppleMusicVolumeBar(), FullPlayerSheet(), Listener, MiniPlayerBar(), PlayerActionRow(), PlayerRelatedSheetContent() (+7 more)

### Community 24 - "AudioPlayerService.kt"
Cohesion: 0.06
Nodes (3): AudioPlayerService, Listener, CachedStreamUrl

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.11
Nodes (18): [1.0.8] - 2026-10-23, [1.1.0] - 2026-10-24, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.2] - 2026-10-25, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25, Adicionado (+10 more)

### Community 28 - "UserSettingsRepositoryImpl"
Cohesion: 0.07
Nodes (10): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL (+2 more)

### Community 32 - "MigrationScreen"
Cohesion: 0.06
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 36 - "MimeAndContainerDetectionStressTest.kt"
Cohesion: 0.11
Nodes (3): ContextWrapper, InvocationHandler, StreamResolver

### Community 38 - "InnerTubeStreamResolver"
Cohesion: 0.11
Nodes (5): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest, StreamQualitySelectorStressTest

### Community 41 - "CompositeStreamResolver"
Cohesion: 0.16
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 43 - "SearchScreen.kt"
Cohesion: 0.05
Nodes (5): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), PlaylistOptionItem(), PlaylistOptionsBottomSheet()

### Community 44 - "AudioQuality"
Cohesion: 0.15
Nodes (7): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, StreamResolver

### Community 45 - "DownloadedTrackEntity"
Cohesion: 0.09
Nodes (4): AppDatabase, DownloadedTrackDao, DownloadedTrackEntity, PersistenceAndOrphanScannerStressTest

### Community 48 - "Track"
Cohesion: 0.12
Nodes (8): Track, UserAffinityRepository, RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR, SmartRadioEngine

### Community 56 - "v1.3.5 — Core de Áudio, Parser, Metadados & Sincronização"
Cohesion: 0.25
Nodes (6): 1. Recomendações e Conteúdo Relacionado, 2. Letras e Exibição, 3. Core de Áudio, Crossfade e Segundo Plano, 4. Metadados e Sincronização Local de Downloads, 🎵 NERF Music v1.3.5, v1.3.5 — Core de Áudio, Parser, Metadados & Sincronização

### Community 58 - "SearchResult"
Cohesion: 0.21
Nodes (4): SearchFilter, ALL, SONGS, SearchResult

### Community 62 - "AudioCoreDownloadsCrossfadeQualityTest"
Cohesion: 0.20
Nodes (3): AudioCoreDownloadsCrossfadeQualityTest, StreamResolver, StreamResolver

### Community 63 - "Corrigido"
Cohesion: 0.50
Nodes (4): [1.3.5] - 2026-10-27, Adicionado, Corrigido, Testes

### Community 65 - "NERF Music"
Cohesion: 0.20
Nodes (8): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão

### Community 81 - "QueueAndConnectivityChallengerTest"
Cohesion: 0.07
Nodes (15): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+7 more)

### Community 83 - "LibrarySortOption"
Cohesion: 0.50
Nodes (4): LibrarySortOption, A_TO_Z, RECENT, RECENTLY_ADDED

## Knowledge Gaps
- **54 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+49 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 447 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **50 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `UserHistoryRepository`, `ArtistScreen`, `LyricsData`, `DownloadedTrack`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `MainActivity.kt`, `HomeSection`, `MainScreen`, `SpotifyCanvasRepositoryImpl`, `FullPlayerSheet`, `Track`, `MusicViewModel.kt`, `UserSettingsRepositoryImpl`, `MigrationScreen`, `SearchScreen.kt`, `AudioQuality`, `UpdateNotificationHelper`, `UserSettingsRepository`, `Track`, `.loadHomeFeed`, `SmartDownloadWorker`, `SmartDownloadManager`, `NewPipeStreamResolver.kt`, `SearchResult`, `.isConnectedToWifi`, `.loadCanvasVideo`, `TrackRelatedData`, `MusicRepository`, `.clearStreamCache`, `QueueAndConnectivityChallengerTest`?**
  _High betweenness centrality (0.235) - this node is a cross-community bridge._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _54 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.02400408580183861 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `Artist`, `PlayerScreen.kt`, `AudioMetadataTagger`, `MigrationManager`, `MusicViewModel`, `UserHistoryRepository`, `ArtistScreen`, `DownloadChunkRangeStressTest.kt`, `DownloadedTrack`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrack`, `HomeSection`, `MainScreen`, `UserHistoryRepositoryImpl`, `FullPlayerSheet`, `Track`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `UserAffinityRepositoryImpl`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `InnerTubeCatalogClient.kt`, `LibraryRepository.kt`, `InnerTubeCatalogClientTest`, `.testRelatedMultiTierSynthesisAlwaysPopulatesContent`, `SearchScreen.kt`, `DownloadedTrackEntity`, `SmartDownloadManager`, `MusicRepositoryImpl`, `InnerTubeCatalogClient`, `SearchResult`, `AudioMetadataTaggerTest.kt`, `.isConnectedToWifi`, `PlayerUiUtilsTest.kt`, `.loadCanvasVideo`, `TrackRelatedData`, `MusicRepository`, `AudioMetadataTagger.kt`?**
  _High betweenness centrality (0.190) - this node is a cross-community bridge._
- **Should `MigrationManager` be split into smaller, more focused modules?**
  _Cohesion score 0.06753246753246753 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `InnerTubeCatalogClient.kt`, `SettingsScreen.kt`, `MimeAndContainerDetectionStressTest.kt`, `InnerTubeStreamResolver`, `MusicViewModel`, `CompositeStreamResolver`, `DownloadChunkRangeStressTest.kt`, `LyricsData`, `UserSettingsRepository`, `.clearStreamCache`, `StreamResolver`, `AudioPlayerService.kt`, `NewPipeStreamResolver.kt`, `MusicViewModel.kt`, `UserSettingsRepositoryImpl`, `AudioCoreDownloadsCrossfadeQualityTest`, `AudioCoreDownloadsCrossfadeQualityTest.kt`?**
  _High betweenness centrality (0.115) - this node is a cross-community bridge._
- **Should `SettingsScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.08896103896103896 - nodes in this community are weakly interconnected._