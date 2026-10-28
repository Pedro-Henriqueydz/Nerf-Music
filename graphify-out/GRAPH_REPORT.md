# Graph Report - Nerf Music  (2026-10-27)

## Corpus Check
- 105 files · ~126,877 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 2007 nodes · 4817 edges · 101 communities (39 shown, 62 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 90 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `97aa6114`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- LibraryRepository.kt
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
- SearchScreen.kt
- LibraryScreen.kt
- JamSessionSheet.kt
- SpotifyCanvasRepositoryImpl
- KaraokeAndJamTest.kt
- FullPlayerSheet
- Track
- LibraryRepositoryImpl
- MusicViewModel.kt
- Registro de Alterações (Changelog) - NERF Music
- UserSettingsRepository
- PlaylistOptionsBottomSheet.kt
- AppDatabase
- AudioCoreDownloadsCrossfadeQualityTest
- MigrationScreen
- SmartDownloadWorker.kt
- JamSessionManager.kt
- ConnectivityObserver
- InnerTubeCatalogClientTest
- InnerTubeStreamResolverTest
- AudioMetadataTagger
- CompositeStreamResolver
- AudioCoreDownloadsCrossfadeQualityTest.kt
- ArtistScreen.kt
- AudioQuality
- DownloadedTrackDao
- RadioTuningMode
- LyricsNullSanitizationStressTest
- Track
- .loadHomeFeed
- .updateCurrentLyricsLine
- ExploreScreen.kt
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- InnerTubeCatalogClient
- NestedScrollConnection
- InnerTubeStreamResolver
- OkHttpDownloader.kt
- SearchResult
- Block3EmpiricalChallengeTest
- .isConnectedToWifi
- Context
- TextStyle
- Listener
- NewPipeStreamResolver
- JamMessageType
- HomeSection
- DataSaverInterceptor.kt
- KaraokeAndJamTest.kt
- MimeAndContainerDetectionStressTest
- RelatedMultiTierFallbackTest.kt
- AudioPlayerService
- JamWebSocketServer.kt
- Listener
- LyricsData
- JamGlobalRelayClient
- StreamQualitySelectorStressTest
- Test
- ArtistScreen
- .onCreate
- JamCommandType
- .onTaskRemoved
- NetworkCallback
- AudioQuality
- MusicRepository
- .syncNow
- Context
- ContextWrapper
- PlayerUiUtilsTest.kt
- Artist
- DownloadOutcome
- TrackRelatedData
- StreamResolver
- StreamResolver

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 197 edges
2. `Track` - 145 edges
3. `InnerTubeCatalogClient` - 55 edges
4. `AudioPlayerService` - 53 edges
5. `LibraryRepositoryImpl` - 48 edges
6. `AudioQuality` - 48 edges
7. `MigrationManager` - 46 edges
8. `UserSettingsRepositoryImpl` - 36 edges
9. `JamSessionManager` - 30 edges
10. `LyricsData` - 28 edges

## Surprising Connections (you probably didn't know these)
- `3. 🎧 NERF JAM — Escuta Sincronizada em Grupo (Estilo Spotify Jam)` --references--> `JamWebSocketServer`  [INFERRED]
  RELEASE_NOTES.md → app/src/main/java/com/nerf/music/jam/JamWebSocketServer.kt
- `1. 🎤 Modo Karaokê de Próxima Geração & Troca Instantânea por Instrumental Oficial` --references--> `VocalIsolationAudioProcessor`  [INFERRED]
  RELEASE_NOTES.md → app/src/main/java/com/nerf/music/player/VocalIsolationAudioProcessor.kt
- `Adicionado` --references--> `ConnectivityObserver`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/ConnectivityObserver.kt
- `Adicionado` --references--> `HomeFeedSynthesizer`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/HomeFeedSynthesizer.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt

## Import Cycles
- None detected.

## Communities (101 total, 62 thin omitted)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.10
Nodes (34): SmartDownloadSyncState, AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting (+26 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 8 - "JamSessionManager"
Cohesion: 0.31
Nodes (3): JamParticipant, JamWireMessage, JamSessionManager

### Community 9 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.11
Nodes (3): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 11 - "LyricsData"
Cohesion: 0.20
Nodes (3): LyricsRepository, LyricsRepositoryImpl, optCleanString()

### Community 12 - "DownloadedTrackEntity"
Cohesion: 0.18
Nodes (4): DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 13 - "HomeScreen.kt"
Cohesion: 0.18
Nodes (14): ExploreScreen(), HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel() (+6 more)

### Community 16 - "MainScreen.kt"
Cohesion: 0.09
Nodes (7): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar()

### Community 17 - "SearchScreen.kt"
Cohesion: 0.15
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 18 - "LibraryScreen.kt"
Cohesion: 0.09
Nodes (20): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS, LibraryGridCard() (+12 more)

### Community 19 - "JamSessionSheet.kt"
Cohesion: 0.06
Nodes (3): AnimatedEqualizerBars(), JamSessionSheet(), JamVisualMatrixBadge()

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 21 - "KaraokeAndJamTest.kt"
Cohesion: 0.12
Nodes (4): BatchTranslationResult, KaraokeLyricsEngine, TranslationLanguageOption, KaraokeAndJamTest

### Community 22 - "FullPlayerSheet"
Cohesion: 0.17
Nodes (16): PlaybackState, AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar(), ApplePopoverOptionRow(), FullPlayerSheet(), KaraokeCountdownDotsRow(), MiniPlayerBar() (+8 more)

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.06
Nodes (33): [1.0.8] - 2026-10-23, [1.1.0] - 2026-10-24, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.2] - 2026-10-25, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25, Adicionado (+25 more)

### Community 31 - "AudioCoreDownloadsCrossfadeQualityTest"
Cohesion: 0.17
Nodes (3): AudioCoreDownloadsCrossfadeQualityTest, StreamResolver, StreamResolver

### Community 32 - "MigrationScreen"
Cohesion: 0.11
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 34 - "SmartDownloadWorker.kt"
Cohesion: 0.06
Nodes (5): MainActivity, NERFMusicTheme(), UpdateCheckWorker, UpdateNotificationHelper, SmartDownloadWorker

### Community 36 - "ConnectivityObserver"
Cohesion: 0.08
Nodes (14): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+6 more)

### Community 39 - "AudioMetadataTagger"
Cohesion: 0.06
Nodes (9): BiquadFilter, VocalIsolationAudioProcessor, AudioMetadataTagger, ExtractedMetadata, AudioMetadataTaggerTest, [1.3.5] - 2026-10-27 [RELEASED], Adicionado, Corrigido (+1 more)

### Community 41 - "CompositeStreamResolver"
Cohesion: 0.16
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 42 - "AudioCoreDownloadsCrossfadeQualityTest.kt"
Cohesion: 0.11
Nodes (4): StreamResolver, InvocationHandler, ContextWrapper, InvocationHandler

### Community 43 - "ArtistScreen.kt"
Cohesion: 0.10
Nodes (3): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow()

### Community 44 - "AudioQuality"
Cohesion: 0.08
Nodes (10): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL (+2 more)

### Community 46 - "RadioTuningMode"
Cohesion: 0.27
Nodes (6): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR, SmartRadioEngine

### Community 48 - "Track"
Cohesion: 0.11
Nodes (3): Track, UserAffinityRepository, UserAffinityRepositoryImpl

### Community 56 - "InnerTubeStreamResolver"
Cohesion: 0.27
Nodes (3): CandidateFormat, ClientConfig, InnerTubeStreamResolver

### Community 58 - "SearchResult"
Cohesion: 0.19
Nodes (4): SearchFilter, ALL, SONGS, SearchResult

### Community 65 - "JamMessageType"
Cohesion: 0.13
Nodes (15): JamConnectionStatus, CONNECTED, CONNECTING, DISCONNECTED, ERROR, RECONNECTING, JamMessageType, COMMAND (+7 more)

### Community 68 - "HomeSection"
Cohesion: 0.17
Nodes (13): AlbumDetail, HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID (+5 more)

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 80 - "LyricsData"
Cohesion: 0.38
Nodes (5): LyricsData, SyncedLyricsLine, SyncedLyricsWord, inlineLyricSegmentIndex(), InlineSyncedLyrics()

### Community 84 - "ArtistScreen"
Cohesion: 0.61
Nodes (6): ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow(), ArtistVideoCard()

### Community 86 - "JamCommandType"
Cohesion: 0.17
Nodes (12): JamCommandType, ADD_TO_QUEUE, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK, REMOVE_QUEUE_ITEM (+4 more)

### Community 87 - ".onTaskRemoved"
Cohesion: 0.29
Nodes (5): [1.4.0] - 2026-10-27 [RELEASED], Adicionado, Adicionado, Corrigido, 4. ⚡ Otimizações do Player, Performance e Downloads Granulares

### Community 89 - "AudioQuality"
Cohesion: 0.14
Nodes (6): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH

### Community 97 - "DownloadOutcome"
Cohesion: 0.40
Nodes (5): DownloadOutcome, ALLOWED, BLOCKED_ALWAYS_ON, BLOCKED_AUTOMATIC_DATA_SAVER, BLOCKED_CELLULAR_DISABLED

## Knowledge Gaps
- **77 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+72 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 502 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **62 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `LibraryRepository.kt`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `UserHistoryRepository`, `JamSessionManager`, `LyricsData`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrackList`, `MainScreen.kt`, `SearchScreen.kt`, `LibraryScreen.kt`, `SpotifyCanvasRepositoryImpl`, `KaraokeAndJamTest.kt`, `FullPlayerSheet`, `Track`, `MusicViewModel.kt`, `UserSettingsRepository`, `MigrationScreen`, `SmartDownloadWorker.kt`, `JamSessionManager.kt`, `ConnectivityObserver`, `ArtistScreen.kt`, `AudioQuality`, `RadioTuningMode`, `Track`, `.loadHomeFeed`, `.updateCurrentLyricsLine`, `ExploreScreen.kt`, `SearchResult`, `.isConnectedToWifi`, `NewPipeStreamResolver`, `JamMessageType`, `.loadCanvasVideo`, `HomeSection`, `AudioPlayerService`, `.setKaraokeMode`, `LyricsData`, `ArtistScreen`, `AudioQuality`, `MusicRepository`, `.initializePlayerController`, `Artist`, `TrackRelatedData`?**
  _High betweenness centrality (0.230) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AudioPlayerService` (e.g. with `VocalIsolationAudioProcessor` and `Corrigido`) actually correct?**
  _`AudioPlayerService` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _77 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `LibraryRepository.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.10507246376811594 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `LibraryRepository.kt`, `PlayerScreen.kt`, `UserHistoryRepositoryImpl`, `MigrationManager`, `MusicViewModel`, `UserHistoryRepository`, `JamSessionManager`, `DownloadChunkRangeStressTest.kt`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `.playTrackList`, `SearchScreen.kt`, `LibraryScreen.kt`, `KaraokeAndJamTest.kt`, `FullPlayerSheet`, `Track`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `InnerTubeCatalogClient.kt`, `JamSessionManager.kt`, `InnerTubeCatalogClientTest`, `AudioMetadataTagger`, `ArtistScreen.kt`, `RadioTuningMode`, `MusicRepositoryImpl`, `InnerTubeCatalogClient`, `SearchResult`, `.isConnectedToWifi`, `JamMessageType`, `.loadCanvasVideo`, `HomeSection`, `KaraokeAndJamTest.kt`, `RelatedMultiTierFallbackTest.kt`, `ArtistScreen`, `PlayerUiUtilsTest.kt`, `Artist`, `TrackRelatedData`?**
  _High betweenness centrality (0.194) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.022222222222222223 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `SettingsScreen.kt`, `MusicViewModel`, `DownloadChunkRangeStressTest.kt`, `AudioPlayerService.kt`, `MusicViewModel.kt`, `UserSettingsRepository`, `AudioCoreDownloadsCrossfadeQualityTest`, `InnerTubeCatalogClient.kt`, `InnerTubeStreamResolverTest`, `CompositeStreamResolver`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `AudioQuality`, `InnerTubeStreamResolver`, `NewPipeStreamResolver`, `KaraokeAndJamTest.kt`, `AudioPlayerService`, `LyricsData`, `StreamResolver`, `StreamResolver`?**
  _High betweenness centrality (0.117) - this node is a cross-community bridge._