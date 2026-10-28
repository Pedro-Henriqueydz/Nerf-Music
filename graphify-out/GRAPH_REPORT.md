# Graph Report - Nerf Music  (2026-10-28)

## Corpus Check
- 107 files · ~131,801 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 2049 nodes · 4937 edges · 97 communities (40 shown, 57 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 90 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `418d3b07`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- FullPlayerSheet
- UserHistoryRepositoryImpl
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- InnerTubeCatalogClientTest
- JamSessionManager
- DownloadChunkRangeStressTest.kt
- steps
- DownloadedTrack
- DownloadedTrackEntity
- HomeScreen.kt
- .syncTimelineState
- SearchScreen.kt
- MainScreen.kt
- DownloadedTrackDao
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
- ConnectivityObserver
- AudioCoreDownloadsCrossfadeQualityTest
- MigrationScreen
- LibraryRepository.kt
- SmartDownloadWorker.kt
- KaraokeAndJamTest.kt
- QueueExpansionState
- Corrigido
- QueueAndConnectivityChallengerTest
- VocalIsolationAudioProcessor
- CompositeStreamResolver
- KaraokeAndJamTest
- ArtistScreen.kt
- AudioQuality
- MainActivity.kt
- RadioTuningMode
- .prefetchTrack
- Track
- .loadHomeFeed
- .updateCurrentLyricsLine
- TrackOptionsBottomSheet.kt
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- InnerTubeCatalogClient
- 2. 🎧 NERF JAM Global & Governança da Sala pelo Host
- PlayerUiUtilsTest.kt
- ExploreScreen.kt
- LyricsData
- TrackRelatedData
- JamPersonWavesIcon
- LibraryRepository.kt
- NERF Music
- NewPipeStreamResolver.kt
- SmartDownloadManager
- AlbumDetail
- AudioCoreDownloadsCrossfadeQualityTest.kt
- .syncNow
- RelatedMultiTierFallbackTest.kt
- AudioPlayerService
- JamWebSocketServer.kt
- Listener
- AppDatabase
- JamGlobalRelayClient
- InnerTubeStreamResolver
- UpdateNotificationHelper
- SmartDownloadWorker
- .onCreate
- .onTaskRemoved
- NewPipeStreamResolverTest.kt
- AudioQuality
- .compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure
- InlineSyncedLyrics
- UpNextResult
- Context
- [1.3.2] - 2026-10-25
- Theme.kt
- HomeSection

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 203 edges
2. `Track` - 148 edges
3. `InnerTubeCatalogClient` - 55 edges
4. `AudioPlayerService` - 55 edges
5. `LibraryRepositoryImpl` - 48 edges
6. `AudioQuality` - 48 edges
7. `MigrationManager` - 46 edges
8. `UserSettingsRepositoryImpl` - 36 edges
9. `JamSessionManager` - 36 edges
10. `LyricsData` - 28 edges

## Surprising Connections (you probably didn't know these)
- `Adicionado` --references--> `AudioMetadataTagger`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/AudioMetadataTagger.kt
- `Adicionado` --references--> `HomeFeedSynthesizer`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/HomeFeedSynthesizer.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt
- `Adicionado` --references--> `UserAffinityRepository`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/UserAffinityRepository.kt
- `Adicionado` --references--> `JamGlobalRelayClient`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/jam/JamGlobalRelayClient.kt

## Import Cycles
- None detected.

## Communities (97 total, 57 thin omitted)

### Community 0 - "FullPlayerSheet"
Cohesion: 0.14
Nodes (17): PlaybackState, AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar(), ApplePopoverOptionRow(), FullPlayerSheet(), Listener, KaraokeCountdownDotsRow() (+9 more)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.09
Nodes (34): SmartDownloadSyncState, AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting (+26 more)

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

### Community 13 - "HomeScreen.kt"
Cohesion: 0.17
Nodes (15): ExploreScreen(), HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel() (+7 more)

### Community 15 - "SearchScreen.kt"
Cohesion: 0.17
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 16 - "MainScreen.kt"
Cohesion: 0.09
Nodes (7): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar()

### Community 18 - "LibraryScreen.kt"
Cohesion: 0.12
Nodes (20): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS, LibraryGridCard() (+12 more)

### Community 19 - "JamSessionSheet.kt"
Cohesion: 0.06
Nodes (4): AnimatedEqualizerBars(), JamSessionSheet(), JamSettingToggleRow(), JamVisualMatrixBadge()

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 21 - "KaraokeAndJamTest.kt"
Cohesion: 0.20
Nodes (3): BatchTranslationResult, KaraokeLyricsEngine, TranslationLanguageOption

### Community 26 - "MusicViewModel.kt"
Cohesion: 0.11
Nodes (3): RelatedSection, TrackRelatedData, HomeFeedSynthesizer

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.13
Nodes (15): [1.0.8] - 2026-10-23, [1.1.0] - 2026-10-24, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25, Adicionado, Adicionado (+7 more)

### Community 30 - "ConnectivityObserver"
Cohesion: 0.20
Nodes (9): ConnectivityObserver, Status, Available, Losing, Lost, Unavailable, [1.4.0] - 2026-10-27 [RELEASED], Adicionado (+1 more)

### Community 31 - "AudioCoreDownloadsCrossfadeQualityTest"
Cohesion: 0.18
Nodes (3): AudioCoreDownloadsCrossfadeQualityTest, StreamResolver, StreamResolver

### Community 32 - "MigrationScreen"
Cohesion: 0.11
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 36 - "QueueExpansionState"
Cohesion: 0.27
Nodes (5): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, QueueScrollControllerMock

### Community 37 - "Corrigido"
Cohesion: 0.50
Nodes (4): [1.3.5] - 2026-10-27 [RELEASED], Adicionado, Corrigido, Testes

### Community 39 - "VocalIsolationAudioProcessor"
Cohesion: 0.06
Nodes (5): BiquadFilter, VocalIsolationAudioProcessor, AudioMetadataTagger, ExtractedMetadata, AudioMetadataTaggerTest

### Community 41 - "CompositeStreamResolver"
Cohesion: 0.23
Nodes (3): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest

### Community 43 - "ArtistScreen.kt"
Cohesion: 0.13
Nodes (9): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow() (+1 more)

### Community 44 - "AudioQuality"
Cohesion: 0.08
Nodes (10): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL (+2 more)

### Community 46 - "RadioTuningMode"
Cohesion: 0.18
Nodes (5): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR

### Community 48 - "Track"
Cohesion: 0.11
Nodes (4): Track, UserAffinityRepository, UserAffinityRepositoryImpl, SmartRadioEngine

### Community 51 - "TrackOptionsBottomSheet.kt"
Cohesion: 0.12
Nodes (4): PlaylistOptionItem(), PlaylistOptionsBottomSheet(), TrackOptionRow(), TrackOptionsBottomSheet()

### Community 53 - "MusicRepositoryImpl"
Cohesion: 0.16
Nodes (3): AlbumDetail, SearchResult, MusicRepositoryImpl

### Community 55 - "2. 🎧 NERF JAM Global & Governança da Sala pelo Host"
Cohesion: 0.25
Nodes (5): 1. 🎤 Karaokê de Alta Precisão & DSP Anti-Abafamento, 2. 🎧 NERF JAM Global & Governança da Sala pelo Host, 🔥 Destaques Principais da Versão 1.4.1, 📦 Informações Técnicas da Compilação, 🎵 NERF Music v1.4.1 — Correções, Precisão de Karaokê & JAM Global! 🚀

### Community 59 - "LyricsData"
Cohesion: 0.05
Nodes (14): LyricsData, SyncedLyricsLine, SyncedLyricsWord, LyricsRepository, LyricsRepositoryImpl, optCleanString(), Block3EmpiricalChallengeTest, DownloadOutcome (+6 more)

### Community 61 - "JamPersonWavesIcon"
Cohesion: 0.19
Nodes (3): JamPersonWavesIcon(), [1.4.1] - 2026-10-27 [RELEASED], Adicionado

### Community 63 - "NERF Music"
Cohesion: 0.25
Nodes (8): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 81 - "JamGlobalRelayClient"
Cohesion: 0.06
Nodes (6): JamGlobalRelayClient, WebSocketListener, DataSaverInterceptor, NetworkConnectivityObserver, NetworkCallback, NetworkUtils

### Community 82 - "InnerTubeStreamResolver"
Cohesion: 0.10
Nodes (5): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest, StreamQualitySelectorStressTest

### Community 89 - "AudioQuality"
Cohesion: 0.15
Nodes (7): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, StreamResolver

### Community 91 - "InlineSyncedLyrics"
Cohesion: 0.47
Nodes (4): inlineLyricSegmentIndex(), InlineSyncedLyrics(), largestFittingCharacterBoundary(), splitInlineLyricIntoSegments()

### Community 94 - "[1.3.2] - 2026-10-25"
Cohesion: 0.50
Nodes (4): [1.3.2] - 2026-10-25, Corrigido, Segurança, Testes

### Community 98 - "HomeSection"
Cohesion: 0.18
Nodes (13): HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL (+5 more)

## Knowledge Gaps
- **80 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+75 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 509 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **57 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `FullPlayerSheet`, `UserHistoryRepositoryImpl`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `JamSessionManager`, `DownloadedTrack`, `HomeScreen.kt`, `.syncTimelineState`, `SearchScreen.kt`, `MainScreen.kt`, `LibraryScreen.kt`, `SpotifyCanvasRepositoryImpl`, `KaraokeAndJamTest.kt`, `Track`, `MusicViewModel.kt`, `UserSettingsRepository`, `ConnectivityObserver`, `MigrationScreen`, `LibraryRepository.kt`, `.clearStreamCache`, `ArtistScreen.kt`, `AudioQuality`, `MainActivity.kt`, `RadioTuningMode`, `Track`, `.loadHomeFeed`, `.updateCurrentLyricsLine`, `MusicRepositoryImpl`, `.playTrackList`, `ExploreScreen.kt`, `LyricsData`, `NewPipeStreamResolver.kt`, `SmartDownloadManager`, `.loadCanvasVideo`, `AlbumDetail`, `.applyKaraokeState`, `JamGlobalRelayClient`, `UpdateNotificationHelper`, `SmartDownloadWorker`, `.initializePlayerController`, `AudioQuality`, `HomeSection`?**
  _High betweenness centrality (0.267) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AudioPlayerService` (e.g. with `VocalIsolationAudioProcessor` and `Corrigido`) actually correct?**
  _`AudioPlayerService` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _80 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `FullPlayerSheet` be split into smaller, more focused modules?**
  _Cohesion score 0.14153846153846153 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `FullPlayerSheet`, `PlayerScreen.kt`, `UserHistoryRepositoryImpl`, `MigrationManager`, `MusicViewModel`, `InnerTubeCatalogClientTest`, `JamSessionManager`, `DownloadChunkRangeStressTest.kt`, `DownloadedTrack`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `SearchScreen.kt`, `MainScreen.kt`, `LibraryScreen.kt`, `KaraokeAndJamTest.kt`, `Track`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `LibraryRepository.kt`, `KaraokeAndJamTest.kt`, `VocalIsolationAudioProcessor`, `KaraokeAndJamTest`, `ArtistScreen.kt`, `RadioTuningMode`, `TrackOptionsBottomSheet.kt`, `MusicRepositoryImpl`, `InnerTubeCatalogClient`, `.playTrackList`, `PlayerUiUtilsTest.kt`, `ExploreScreen.kt`, `TrackRelatedData`, `LibraryRepository.kt`, `SmartDownloadManager`, `.loadCanvasVideo`, `AlbumDetail`, `JamGlobalRelayClient`, `UpNextResult`, `HomeSection`?**
  _High betweenness centrality (0.211) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.022222222222222223 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `NewPipeStreamResolver.kt`, `LibraryRepository.kt`, `KaraokeAndJamTest.kt`, `SettingsScreen.kt`, `.compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure`, `MusicViewModel`, `.clearStreamCache`, `CompositeStreamResolver`, `DownloadChunkRangeStressTest.kt`, `AudioQuality`, `.prefetchTrack`, `InnerTubeStreamResolver`, `AudioPlayerService.kt`, `MusicViewModel.kt`, `LyricsData`, `UserSettingsRepository`, `AudioCoreDownloadsCrossfadeQualityTest`?**
  _High betweenness centrality (0.066) - this node is a cross-community bridge._