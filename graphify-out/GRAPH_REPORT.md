# Graph Report - Nerf Music  (2026-10-27)

## Corpus Check
- 106 files · ~130,003 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 2040 nodes · 4890 edges · 86 communities (40 shown, 46 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 81 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e602f077`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- VocalIsolationAudioProcessor
- PlayerScreen.kt
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
- MainScreen.kt
- AnimatedEqualizerBars.kt
- LibraryScreen.kt
- JamSessionSheet.kt
- SpotifyCanvasRepositoryImpl
- KaraokeAndJamTest.kt
- NestedScrollConnection
- Track
- LibraryRepositoryImpl
- MusicViewModel.kt
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
- UserPlaylist
- AudioMetadataTagger
- CompositeStreamResolver
- DownloadAudioQuality
- ArtistScreen.kt
- AudioQuality
- KaraokeAndJamTest
- RadioTuningMode
- LyricsNullSanitizationStressTest
- UserAffinityRepositoryImpl
- .loadHomeFeed
- .updateCurrentLyricsLine
- ExploreScreen.kt
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- InnerTubeCatalogClient
- SearchResult
- .isConnectedToWifi
- PlayerUiUtilsTest.kt
- Artist
- Block3EmpiricalChallengeTest
- TrackRelatedData
- LibraryFilter
- DataSaverInterceptor.kt
- RelatedMultiTierFallbackTest.kt
- NewPipeStreamResolver
- DownloadOutcome
- AlbumDetail
- LibrarySortOption
- AudioCoreDownloadsCrossfadeQualityTest.kt
- RelatedMultiTierFallbackTest.kt
- AudioPlayerService
- JamWebSocketServer.kt
- Listener
- JamGlobalRelayClient
- InnerTubeStreamResolver
- Test
- .onCreate
- .onTaskRemoved
- AudioQuality
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
- `3. 🎧 NERF JAM — Escuta Sincronizada em Grupo (Estilo Spotify Jam)` --references--> `JamWebSocketServer`  [INFERRED]
  RELEASE_NOTES.md → app/src/main/java/com/nerf/music/jam/JamWebSocketServer.kt
- `1. 🎤 Modo Karaokê de Próxima Geração & Troca Instantânea por Instrumental Oficial` --references--> `VocalIsolationAudioProcessor`  [INFERRED]
  RELEASE_NOTES.md → app/src/main/java/com/nerf/music/player/VocalIsolationAudioProcessor.kt
- `Adicionado` --references--> `ConnectivityObserver`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/ConnectivityObserver.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt
- `Adicionado` --references--> `UserAffinityRepository`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/UserAffinityRepository.kt

## Import Cycles
- None detected.

## Communities (86 total, 46 thin omitted)

### Community 1 - "PlayerScreen.kt"
Cohesion: 0.02
Nodes (23): PlaybackState, AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar(), ApplePopoverOptionRow(), FullPlayerSheet(), Listener, inlineLyricSegmentIndex() (+15 more)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.08
Nodes (35): SmartDownloadManager, SmartDownloadSyncState, AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard() (+27 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 8 - "JamSessionManager"
Cohesion: 0.06
Nodes (35): JamCommandType, ADD_TO_QUEUE, KICK_PARTICIPANT, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK (+27 more)

### Community 9 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.05
Nodes (8): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler, StreamResolver, MimeAndContainerDetectionStressTest, ContextWrapper, InvocationHandler, StreamResolver

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 11 - "LyricsData"
Cohesion: 0.17
Nodes (6): LyricsData, SyncedLyricsLine, SyncedLyricsWord, LyricsRepository, LyricsRepositoryImpl, optCleanString()

### Community 12 - "DownloadedTrackEntity"
Cohesion: 0.08
Nodes (6): AppDatabase, DownloadedTrackDao, DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 13 - "HomeScreen.kt"
Cohesion: 0.14
Nodes (15): ExploreScreen(), YtmExploreQuickButton(), HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem() (+7 more)

### Community 16 - "MainScreen.kt"
Cohesion: 0.09
Nodes (7): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar()

### Community 18 - "LibraryScreen.kt"
Cohesion: 0.20
Nodes (9): LibraryGridCard(), LibraryListItem(), LibraryScreen(), NewBottomSheetOption(), PlaylistCollageCover(), TrackListDetailView(), UserPlaylistDetailView(), SaveToPlaylistSheet() (+1 more)

### Community 19 - "JamSessionSheet.kt"
Cohesion: 0.06
Nodes (4): JamPersonWavesIcon(), JamSessionSheet(), JamSettingToggleRow(), JamVisualMatrixBadge()

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 21 - "KaraokeAndJamTest.kt"
Cohesion: 0.20
Nodes (3): BatchTranslationResult, KaraokeLyricsEngine, TranslationLanguageOption

### Community 23 - "Track"
Cohesion: 0.09
Nodes (3): Track, LibraryRepository, UserAffinityRepository

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.06
Nodes (31): [1.0.8] - 2026-10-23, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.2] - 2026-10-25, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25, Adicionado, Adicionado (+23 more)

### Community 31 - "AudioCoreDownloadsCrossfadeQualityTest"
Cohesion: 0.20
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

### Community 38 - "UserPlaylist"
Cohesion: 0.22
Nodes (3): UserPlaylist, PlaylistOptionItem(), PlaylistOptionsBottomSheet()

### Community 39 - "AudioMetadataTagger"
Cohesion: 0.11
Nodes (6): AudioMetadataTagger, ExtractedMetadata, AudioMetadataTaggerTest, [1.3.5] - 2026-10-27 [RELEASED], Adicionado, Testes

### Community 42 - "DownloadAudioQuality"
Cohesion: 0.20
Nodes (5): DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 43 - "ArtistScreen.kt"
Cohesion: 0.12
Nodes (9): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow() (+1 more)

### Community 44 - "AudioQuality"
Cohesion: 0.10
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepositoryImpl

### Community 46 - "RadioTuningMode"
Cohesion: 0.27
Nodes (6): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR, SmartRadioEngine

### Community 51 - "ExploreScreen.kt"
Cohesion: 0.06
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 53 - "MusicRepositoryImpl"
Cohesion: 0.22
Nodes (3): QueueFilterChip, UpNextResult, MusicRepositoryImpl

### Community 55 - "SearchResult"
Cohesion: 0.27
Nodes (4): SearchFilter, ALL, SONGS, SearchResult

### Community 61 - "LibraryFilter"
Cohesion: 0.29
Nodes (7): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS

### Community 65 - "DownloadOutcome"
Cohesion: 0.40
Nodes (5): DownloadOutcome, ALLOWED, BLOCKED_ALWAYS_ON, BLOCKED_AUTOMATIC_DATA_SAVER, BLOCKED_CELLULAR_DISABLED

### Community 69 - "LibrarySortOption"
Cohesion: 0.50
Nodes (4): LibrarySortOption, A_TO_Z, RECENT, RECENTLY_ADDED

### Community 76 - "AudioPlayerService"
Cohesion: 0.14
Nodes (3): AudioPlayerService, CachedStreamUrl, Corrigido

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 81 - "JamGlobalRelayClient"
Cohesion: 0.07
Nodes (4): JamGlobalRelayClient, WebSocketListener, NetworkConnectivityObserver, NetworkCallback

### Community 82 - "InnerTubeStreamResolver"
Cohesion: 0.10
Nodes (5): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest, StreamQualitySelectorStressTest

### Community 87 - ".onTaskRemoved"
Cohesion: 0.29
Nodes (5): [1.4.0] - 2026-10-27 [RELEASED], Adicionado, Adicionado, Corrigido, 4. ⚡ Otimizações do Player, Performance e Downloads Granulares

### Community 89 - "AudioQuality"
Cohesion: 0.10
Nodes (10): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, StreamResolver, DefaultStreamResolverTest (+2 more)

### Community 98 - "HomeSection"
Cohesion: 0.18
Nodes (13): HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL (+5 more)

## Knowledge Gaps
- **80 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+75 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 509 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **46 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `PlayerScreen.kt`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `UserHistoryRepository`, `JamSessionManager`, `LyricsData`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrackList`, `MainScreen.kt`, `LibraryScreen.kt`, `SpotifyCanvasRepositoryImpl`, `KaraokeAndJamTest.kt`, `Track`, `MusicViewModel.kt`, `UserSettingsRepository`, `MigrationScreen`, `SmartDownloadWorker.kt`, `ConnectivityObserver`, `UserPlaylist`, `.clearStreamCache`, `DownloadAudioQuality`, `ArtistScreen.kt`, `AudioQuality`, `RadioTuningMode`, `.loadHomeFeed`, `.updateCurrentLyricsLine`, `ExploreScreen.kt`, `MusicRepositoryImpl`, `SearchResult`, `.isConnectedToWifi`, `Artist`, `TrackRelatedData`, `NewPipeStreamResolver`, `.loadCanvasVideo`, `AlbumDetail`, `.setKaraokeMode`, `AudioQuality`, `HomeSection`?**
  _High betweenness centrality (0.220) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AudioPlayerService` (e.g. with `VocalIsolationAudioProcessor` and `Corrigido`) actually correct?**
  _`AudioPlayerService` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _80 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `VocalIsolationAudioProcessor` be split into smaller, more focused modules?**
  _Cohesion score 0.13105413105413105 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `PlayerScreen.kt`, `UserHistoryRepositoryImpl`, `MigrationManager`, `SettingsScreen.kt`, `MusicViewModel`, `UserHistoryRepository`, `JamSessionManager`, `DownloadChunkRangeStressTest.kt`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrackList`, `LibraryScreen.kt`, `KaraokeAndJamTest.kt`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `InnerTubeCatalogClient.kt`, `LibraryRepository.kt`, `KaraokeAndJamTest.kt`, `InnerTubeCatalogClientTest`, `UserPlaylist`, `AudioMetadataTagger`, `ArtistScreen.kt`, `KaraokeAndJamTest`, `RadioTuningMode`, `UserAffinityRepositoryImpl`, `ExploreScreen.kt`, `MusicRepositoryImpl`, `InnerTubeCatalogClient`, `SearchResult`, `.isConnectedToWifi`, `PlayerUiUtilsTest.kt`, `Artist`, `TrackRelatedData`, `RelatedMultiTierFallbackTest.kt`, `.loadCanvasVideo`, `AlbumDetail`, `HomeSection`?**
  _High betweenness centrality (0.205) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.023341201153947024 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `NewPipeStreamResolver`, `KaraokeAndJamTest.kt`, `SettingsScreen.kt`, `MusicViewModel`, `.clearStreamCache`, `CompositeStreamResolver`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `DownloadAudioQuality`, `AudioQuality`, `AudioPlayerService`, `DownloadChunkRangeStressTest.kt`, `InnerTubeStreamResolver`, `AudioPlayerService.kt`, `MusicViewModel.kt`, `UserSettingsRepository`, `InnerTubeCatalogClient.kt`, `AudioCoreDownloadsCrossfadeQualityTest`?**
  _High betweenness centrality (0.098) - this node is a cross-community bridge._