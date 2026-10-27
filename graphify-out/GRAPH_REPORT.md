# Graph Report - Nerf Music  (2026-10-27)

## Corpus Check
- 104 files · ~122,238 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 1941 nodes · 4633 edges · 90 communities (42 shown, 48 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 80 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `ac3a46f2`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DownloadAudioQuality
- UserHistoryRepositoryImpl
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- UserHistoryRepository
- JamSessionManager.kt
- DownloadChunkRangeStressTest.kt
- steps
- LyricsData
- DownloadedTrackEntity
- HomeScreen.kt
- .syncTimelineState
- .trackToMediaItem
- MainScreen.kt
- SearchScreen.kt
- LibraryScreen
- JamSessionSheet.kt
- SpotifyCanvasRepositoryImpl
- KaraokeAndJamTest.kt
- FullPlayerSheet
- Track
- AudioPlayerService.kt
- LibraryRepositoryImpl
- Registro de Alterações (Changelog) - NERF Music
- UserSettingsRepository
- PlaylistOptionsBottomSheet.kt
- AppDatabase
- AudioCoreDownloadsCrossfadeQualityTest.kt
- MigrationScreen
- InnerTubeCatalogClient.kt
- SmartDownloadWorker.kt
- LibraryRepository.kt
- Block3EmpiricalChallengeTest
- InnerTubeCatalogClientTest
- InnerTubeStreamResolverTest
- AudioMetadataTagger
- Result
- CompositeStreamResolver
- StreamResolver
- ArtistScreen.kt
- AudioQuality
- DownloadedTrackDao
- RadioTuningMode
- LyricsNullSanitizationStressTest
- UserAffinityRepositoryImpl
- .loadHomeFeed
- .updateCurrentLyricsLine
- ExploreScreen.kt
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- InnerTubeCatalogClient
- NestedScrollConnection
- InnerTubeStreamResolver
- Artist
- file
- .isConnectedToWifi
- Context
- TextStyle
- Listener
- NewPipeStreamResolver
- NERF Music
- HomeSection
- DataSaverInterceptor.kt
- .checkAndTriggerTransition
- SettingsSubScreen
- TrackRelatedData
- AudioPlayerService
- JamWebSocketServer.kt
- Listener
- LyricsData
- NetworkConnectivityObserver.kt
- .compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure
- Test
- ArtistScreen
- LibraryFilter
- .onTaskRemoved
- NetworkCallback

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 197 edges
2. `Track` - 140 edges
3. `InnerTubeCatalogClient` - 55 edges
4. `AudioPlayerService` - 50 edges
5. `LibraryRepositoryImpl` - 48 edges
6. `AudioQuality` - 48 edges
7. `MigrationManager` - 46 edges
8. `UserSettingsRepositoryImpl` - 36 edges
9. `LyricsData` - 28 edges
10. `UserSettingsRepository` - 28 edges

## Surprising Connections (you probably didn't know these)
- `3. 🎧 NERF JAM — Escuta Sincronizada em Grupo (Estilo Spotify Jam)` --references--> `JamWebSocketServer`  [INFERRED]
  RELEASE_NOTES.md → app/src/main/java/com/nerf/music/jam/JamWebSocketServer.kt
- `1. 🎤 Modo Karaokê de Próxima Geração & Troca Instantânea por Instrumental Oficial` --references--> `VocalIsolationAudioProcessor`  [INFERRED]
  RELEASE_NOTES.md → app/src/main/java/com/nerf/music/player/VocalIsolationAudioProcessor.kt
- `Adicionado` --references--> `AudioMetadataTagger`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/AudioMetadataTagger.kt
- `Adicionado` --references--> `ConnectivityObserver`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/ConnectivityObserver.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt

## Import Cycles
- None detected.

## Communities (90 total, 48 thin omitted)

### Community 0 - "DownloadAudioQuality"
Cohesion: 0.12
Nodes (7): SmartDownloadManager, SmartDownloadSyncState, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.21
Nodes (22): AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting, SettingsMainContent() (+14 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 8 - "JamSessionManager.kt"
Cohesion: 0.07
Nodes (31): JamCommandType, ADD_TO_QUEUE, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK, REMOVE_QUEUE_ITEM (+23 more)

### Community 9 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.05
Nodes (8): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler, StreamResolver, MimeAndContainerDetectionStressTest, ContextWrapper, InvocationHandler, StreamResolver

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 11 - "LyricsData"
Cohesion: 0.20
Nodes (3): LyricsRepository, LyricsRepositoryImpl, optCleanString()

### Community 12 - "DownloadedTrackEntity"
Cohesion: 0.15
Nodes (4): DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 13 - "HomeScreen.kt"
Cohesion: 0.18
Nodes (14): ExploreScreen(), HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel() (+6 more)

### Community 16 - "MainScreen.kt"
Cohesion: 0.08
Nodes (10): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), Explore, Home, Library, MainScreen(), Screen (+2 more)

### Community 17 - "SearchScreen.kt"
Cohesion: 0.15
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 18 - "LibraryScreen"
Cohesion: 0.26
Nodes (9): LibraryGridCard(), LibraryListItem(), LibraryScreen(), NewBottomSheetOption(), PlaylistCollageCover(), TrackListDetailView(), UserPlaylistDetailView(), SaveToPlaylistSheet() (+1 more)

### Community 19 - "JamSessionSheet.kt"
Cohesion: 0.06
Nodes (3): AnimatedEqualizerBars(), JamSessionSheet(), JamVisualMatrixBadge()

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 21 - "KaraokeAndJamTest.kt"
Cohesion: 0.15
Nodes (5): BatchTranslationResult, KaraokeLyricsEngine, TranslationLanguageOption, KaraokeAndJamTest, WebSocketListener

### Community 22 - "FullPlayerSheet"
Cohesion: 0.17
Nodes (16): PlaybackState, AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar(), ApplePopoverOptionRow(), FullPlayerSheet(), KaraokeCountdownDotsRow(), MiniPlayerBar() (+8 more)

### Community 23 - "Track"
Cohesion: 0.08
Nodes (7): Track, UserPlaylist, HomeFeedSynthesizer, LibraryRepository, UserAffinityRepository, [1.1.0] - 2026-10-24, Adicionado

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.12
Nodes (17): [1.0.8] - 2026-10-23, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.2] - 2026-10-25, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25, Adicionado, Adicionado (+9 more)

### Community 28 - "UserSettingsRepository"
Cohesion: 0.10
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepository

### Community 31 - "AudioCoreDownloadsCrossfadeQualityTest.kt"
Cohesion: 0.05
Nodes (9): formatDurationMs(), AudioCoreDownloadsCrossfadeQualityTest, ContextWrapper, InvocationHandler, StreamResolver, StreamResolver, CrossfadeEmpiricalTest, PlayerUiUtilsTest (+1 more)

### Community 32 - "MigrationScreen"
Cohesion: 0.11
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 34 - "SmartDownloadWorker.kt"
Cohesion: 0.05
Nodes (6): MainActivity, NERFMusicTheme(), UpdateCheckWorker, UpdateNotificationHelper, SmartDownloadNotificationHelper, SmartDownloadWorker

### Community 36 - "Block3EmpiricalChallengeTest"
Cohesion: 0.07
Nodes (19): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+11 more)

### Community 39 - "AudioMetadataTagger"
Cohesion: 0.08
Nodes (4): VocalIsolationAudioProcessor, AudioMetadataTagger, ExtractedMetadata, AudioMetadataTaggerTest

### Community 40 - "Result"
Cohesion: 0.20
Nodes (3): SearchFilter, ALL, SONGS

### Community 41 - "CompositeStreamResolver"
Cohesion: 0.21
Nodes (3): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest

### Community 43 - "ArtistScreen.kt"
Cohesion: 0.07
Nodes (4): LibrarySortOption, A_TO_Z, RECENT, RECENTLY_ADDED

### Community 44 - "AudioQuality"
Cohesion: 0.08
Nodes (7): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, UserSettingsRepositoryImpl

### Community 46 - "RadioTuningMode"
Cohesion: 0.27
Nodes (6): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR, SmartRadioEngine

### Community 56 - "InnerTubeStreamResolver"
Cohesion: 0.27
Nodes (3): CandidateFormat, ClientConfig, InnerTubeStreamResolver

### Community 64 - "NewPipeStreamResolver"
Cohesion: 0.14
Nodes (4): NerfMusicApp, NewPipeStreamResolver, OkHttpDownloader, NewPipeStreamResolverTest

### Community 65 - "NERF Music"
Cohesion: 0.15
Nodes (10): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão (+2 more)

### Community 68 - "HomeSection"
Cohesion: 0.17
Nodes (7): AlbumDetail, ArtistDetail, HomeChip, HomeFeedData, HomeSection, UpNextResult, MusicRepository

### Community 73 - ".checkAndTriggerTransition"
Cohesion: 0.24
Nodes (4): [1.3.5] - 2026-10-27 [RELEASED], Adicionado, Corrigido, Testes

### Community 74 - "SettingsSubScreen"
Cohesion: 0.22
Nodes (9): SettingsSubScreen, ABOUT, CONTENT_DISPLAY, DATA_SAVER, MAIN, MEDIA_QUALITY, MIGRATION, PLAYBACK (+1 more)

### Community 75 - "TrackRelatedData"
Cohesion: 0.20
Nodes (10): HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL, TWO_ROW_CAROUSEL, YOUR_DOWNLOADS, RelatedSection (+2 more)

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 80 - "LyricsData"
Cohesion: 0.50
Nodes (5): LyricsData, SyncedLyricsLine, SyncedLyricsWord, inlineLyricSegmentIndex(), InlineSyncedLyrics()

### Community 84 - "ArtistScreen"
Cohesion: 0.61
Nodes (6): ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow(), ArtistVideoCard()

### Community 86 - "LibraryFilter"
Cohesion: 0.29
Nodes (7): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS

### Community 87 - ".onTaskRemoved"
Cohesion: 0.18
Nodes (9): [1.4.0] - 2026-10-27 [RELEASED], Adicionado, Adicionado, Corrigido, 1. 🎤 Modo Karaokê de Próxima Geração & Troca Instantânea por Instrumental Oficial, 2. 🌍 Tradução e Pronúncia Apple Music Glass (`emil-design-eng`), 3. 🎧 NERF JAM — Escuta Sincronizada em Grupo (Estilo Spotify Jam), 4. ⚡ Otimizações do Player, Performance e Downloads Granulares (+1 more)

## Knowledge Gaps
- **77 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+72 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 496 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **48 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `DownloadAudioQuality`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `UserHistoryRepository`, `JamSessionManager.kt`, `LyricsData`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `.trackToMediaItem`, `MainScreen.kt`, `SearchScreen.kt`, `LibraryScreen`, `SpotifyCanvasRepositoryImpl`, `FullPlayerSheet`, `Track`, `MusicViewModel.kt`, `UserSettingsRepository`, `MigrationScreen`, `SmartDownloadWorker.kt`, `LibraryRepository.kt`, `Block3EmpiricalChallengeTest`, `Result`, `ArtistScreen.kt`, `AudioQuality`, `RadioTuningMode`, `.loadHomeFeed`, `.updateCurrentLyricsLine`, `ExploreScreen.kt`, `Artist`, `file`, `.isConnectedToWifi`, `NewPipeStreamResolver`, `.loadCanvasVideo`, `HomeSection`, `TrackRelatedData`, `.setKaraokeMode`, `LyricsData`, `ArtistScreen`, `.clearStreamCache`?**
  _High betweenness centrality (0.209) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `AudioPlayerService` (e.g. with `Corrigido` and `Corrigido`) actually correct?**
  _`AudioPlayerService` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _77 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DownloadAudioQuality` be split into smaller, more focused modules?**
  _Cohesion score 0.12121212121212122 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `DownloadAudioQuality`, `PlayerScreen.kt`, `UserHistoryRepositoryImpl`, `MigrationManager`, `MusicViewModel`, `UserHistoryRepository`, `JamSessionManager.kt`, `DownloadChunkRangeStressTest.kt`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `.trackToMediaItem`, `MainScreen.kt`, `SearchScreen.kt`, `LibraryScreen`, `FullPlayerSheet`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `InnerTubeCatalogClient.kt`, `SmartDownloadWorker.kt`, `LibraryRepository.kt`, `InnerTubeCatalogClientTest`, `AudioMetadataTagger`, `ArtistScreen.kt`, `RadioTuningMode`, `UserAffinityRepositoryImpl`, `MusicRepositoryImpl`, `InnerTubeCatalogClient`, `Artist`, `.isConnectedToWifi`, `.loadCanvasVideo`, `HomeSection`, `TrackRelatedData`, `ArtistScreen`?**
  _High betweenness centrality (0.192) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.02197802197802198 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `NewPipeStreamResolver`, `InnerTubeCatalogClient.kt`, `MusicViewModel.kt`, `SettingsScreen.kt`, `Block3EmpiricalChallengeTest`, `MusicViewModel`, `InnerTubeStreamResolverTest`, `CompositeStreamResolver`, `StreamResolver`, `DownloadChunkRangeStressTest.kt`, `AudioPlayerService`, `.compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure`, `AudioPlayerService.kt`, `.clearStreamCache`, `InnerTubeStreamResolver`, `UserSettingsRepository`, `AudioCoreDownloadsCrossfadeQualityTest.kt`?**
  _High betweenness centrality (0.106) - this node is a cross-community bridge._