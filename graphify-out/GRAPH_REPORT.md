# Graph Report - Nerf Music  (2026-10-28)

## Corpus Check
- 112 files · ~139,032 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 25 file(s) not represented in the graph (top: .xml 12, .otf 5, (none) 2)

## Summary
- 2174 nodes · 5271 edges · 93 communities (41 shown, 52 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 101 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `f372a56a`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DownloadChunkRangeStressTest.kt
- PlayerScreen.kt
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- InnerTubeCatalogClientTest
- JamSessionManager
- steps
- MimeAndContainerDetectionStressTest.kt
- DownloadedTrackEntity
- AnimatedEqualizerBars
- .syncTimelineState
- SearchScreen.kt
- MainScreen.kt
- LibraryScreen.kt
- LyricsData
- SpotifyCanvasRepositoryImpl
- DownloadedTrackDao
- OkHttpDownloader.kt
- Track
- AudioMetadataTagger
- LibraryRepositoryImpl
- MusicViewModel.kt
- Registro de Alterações (Changelog) - NERF Music
- LoudnessNormalizationAudioProcessor
- OnboardingScreen.kt
- UserAffinityRepositoryImpl
- AudioCoreDownloadsCrossfadeQualityTest
- MigrationScreen
- SmartDownloadWorker.kt
- AudioCoreDownloadsCrossfadeQualityTest.kt
- MusicRepository
- KaraokeLyricsEngine
- OnboardingFlowTest.kt
- JamPersonWavesIcon
- JamSessionSheet
- CompositeStreamResolver
- .isConnectedToWifi
- JamSessionManager.kt
- AudioQuality
- SearchFilter
- UserHistoryRepositoryImpl
- AudioPlayerService
- RadioTuningMode
- .loadHomeFeed
- .updateCurrentLyricsLine
- HomeScreen.kt
- InnerTubeParserAdversarialStressTest
- Artist
- InnerTubeCatalogClient
- Context
- LoudnessNormalizationAudioProcessorTest.kt
- UserHistoryRepository
- LoudnessStandard
- ConnectivityObserver
- NestedScrollConnection
- AudioMetadataTaggerTest.kt
- LibraryRepository.kt
- .prefetchTrack
- NewPipeStreamResolver.kt
- TrackOptionsBottomSheet
- NERF Music
- SmartDownloadManager
- RelatedMultiTierFallbackTest.kt
- Listener
- JamWebSocketServer.kt
- .applyKaraokeState
- AppDatabase
- JamGlobalRelayClient
- DataSaverInterceptor.kt
- NewPipeStreamResolverTest.kt
- TrackRelatedData
- .onTaskRemoved
- InnerTubeStreamResolver
- StreamResolver
- [1.3.2] - 2026-10-25
- Adicionado
- HomeSection

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 207 edges
2. `Track` - 148 edges
3. `AudioPlayerService` - 56 edges
4. `InnerTubeCatalogClient` - 55 edges
5. `LibraryRepositoryImpl` - 48 edges
6. `AudioQuality` - 48 edges
7. `MigrationManager` - 46 edges
8. `UserSettingsRepositoryImpl` - 42 edges
9. `JamSessionManager` - 36 edges
10. `UserSettingsRepository` - 32 edges

## Surprising Connections (you probably didn't know these)
- `Adicionado` --references--> `AudioMetadataTagger`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/AudioMetadataTagger.kt
- `Adicionado` --references--> `ConnectivityObserver`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/ConnectivityObserver.kt
- `Adicionado` --references--> `MainActivity`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/MainActivity.kt
- `1. ✨ Onboarding Elegante & Personalização de Primeiro Acesso` --references--> `MainActivity`  [INFERRED]
  RELEASE_NOTES.md → app/src/main/java/com/nerf/music/MainActivity.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt

## Import Cycles
- None detected.

## Communities (93 total, 52 thin omitted)

### Community 0 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.09
Nodes (4): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler, StreamResolver

### Community 1 - "PlayerScreen.kt"
Cohesion: 0.03
Nodes (21): PlaybackState, AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar(), ApplePopoverOptionRow(), FullPlayerSheet(), Listener, KaraokeCountdownDotsRow() (+13 more)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.10
Nodes (35): SmartDownloadSyncState, AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), LoudnessNormalizationBottomSheet(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard() (+27 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.10
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 8 - "JamSessionManager"
Cohesion: 0.06
Nodes (35): JamCommandType, ADD_TO_QUEUE, KICK_PARTICIPANT, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK (+27 more)

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 11 - "MimeAndContainerDetectionStressTest.kt"
Cohesion: 0.10
Nodes (4): MimeAndContainerDetectionStressTest, ContextWrapper, InvocationHandler, StreamResolver

### Community 12 - "DownloadedTrackEntity"
Cohesion: 0.18
Nodes (4): DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 13 - "AnimatedEqualizerBars"
Cohesion: 0.15
Nodes (22): AnimatedEqualizerBars(), ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow(), ArtistVideoCard(), ExploreScreen() (+14 more)

### Community 15 - "SearchScreen.kt"
Cohesion: 0.16
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 16 - "MainScreen.kt"
Cohesion: 0.09
Nodes (9): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar(), SaveToPlaylistSheet() (+1 more)

### Community 18 - "LibraryScreen.kt"
Cohesion: 0.13
Nodes (18): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS, LibraryGridCard() (+10 more)

### Community 19 - "LyricsData"
Cohesion: 0.05
Nodes (16): LyricsData, SyncedLyricsLine, SyncedLyricsWord, LyricsRepository, LyricsRepositoryImpl, optCleanString(), inlineLyricSegmentIndex(), InlineSyncedLyrics() (+8 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 23 - "Track"
Cohesion: 0.09
Nodes (3): Track, LibraryRepository, UserAffinityRepository

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.15
Nodes (13): [1.0.8] - 2026-10-23, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25, Adicionado, Adicionado, Adicionado (+5 more)

### Community 29 - "OnboardingScreen.kt"
Cohesion: 0.05
Nodes (6): applePressFeedback(), AppleArtistSelectionStep(), AppleArtistSphereCard(), AppleGlassFeatureCard(), AppleWelcomeStep(), OnboardingScreen()

### Community 31 - "AudioCoreDownloadsCrossfadeQualityTest"
Cohesion: 0.17
Nodes (3): AudioCoreDownloadsCrossfadeQualityTest, StreamResolver, StreamResolver

### Community 32 - "MigrationScreen"
Cohesion: 0.11
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 34 - "SmartDownloadWorker.kt"
Cohesion: 0.05
Nodes (6): MainActivity, NERFMusicTheme(), UpdateCheckWorker, UpdateNotificationHelper, SmartDownloadNotificationHelper, SmartDownloadWorker

### Community 35 - "AudioCoreDownloadsCrossfadeQualityTest.kt"
Cohesion: 0.08
Nodes (4): ContextWrapper, InvocationHandler, WebSocketListener, RelatedMultiTierFallbackTest

### Community 37 - "KaraokeLyricsEngine"
Cohesion: 0.06
Nodes (7): BatchTranslationResult, KaraokeLyricsEngine, TranslationLanguageOption, BiquadFilter, VocalIsolationAudioProcessor, KaraokeAndJamTest, Corrigido

### Community 38 - "OnboardingFlowTest.kt"
Cohesion: 0.17
Nodes (3): OnboardingFlowTest, ContextWrapper, InvocationHandler

### Community 40 - "JamSessionSheet"
Cohesion: 0.19
Nodes (9): JamSessionSheet(), JamSettingToggleRow(), JamVisualMatrixBadge(), [1.4.0] - 2026-10-27 [RELEASED], [1.4.2] - 2026-10-28 [RELEASED], Adicionado, Adicionado, Adicionado (+1 more)

### Community 41 - "CompositeStreamResolver"
Cohesion: 0.15
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 44 - "AudioQuality"
Cohesion: 0.04
Nodes (17): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, DataSaverMode, ALWAYS_OFF (+9 more)

### Community 45 - "SearchFilter"
Cohesion: 0.40
Nodes (3): SearchFilter, ALL, SONGS

### Community 48 - "RadioTuningMode"
Cohesion: 0.27
Nodes (6): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR, SmartRadioEngine

### Community 51 - "HomeScreen.kt"
Cohesion: 0.05
Nodes (5): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), PlaylistOptionItem(), PlaylistOptionsBottomSheet()

### Community 53 - "Artist"
Cohesion: 0.14
Nodes (6): AlbumDetail, Artist, ArtistDetail, SearchResult, UpNextResult, MusicRepositoryImpl

### Community 58 - "LoudnessStandard"
Cohesion: 0.17
Nodes (7): LoudnessStandard, APPLE_MUSIC, BROADCAST_EBU, CD_LOUD, CD_STANDARD, OFF, SPOTIFY

### Community 59 - "ConnectivityObserver"
Cohesion: 0.10
Nodes (15): formatDurationMs(), QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available (+7 more)

### Community 61 - "AudioMetadataTaggerTest.kt"
Cohesion: 0.23
Nodes (5): AudioMetadataTaggerTest, [1.3.5] - 2026-10-27 [RELEASED], Adicionado, Corrigido, Testes

### Community 68 - "TrackOptionsBottomSheet"
Cohesion: 0.22
Nodes (6): TrackOptionRow(), TrackOptionsBottomSheet(), 1. ✨ Onboarding Elegante & Personalização de Primeiro Acesso, 2. 🎵 Menu Oficial de Opções da Faixa (3 Pontinhos), 3. 🎧 Painel NERF JAM com Design Glassmorphic & Ergonomia, 🔥 Destaques Principais da Versão 1.4.2

### Community 69 - "NERF Music"
Cohesion: 0.15
Nodes (10): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão (+2 more)

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 81 - "JamGlobalRelayClient"
Cohesion: 0.07
Nodes (4): JamGlobalRelayClient, WebSocketListener, NetworkConnectivityObserver, NetworkCallback

### Community 88 - "InnerTubeStreamResolver"
Cohesion: 0.10
Nodes (5): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest, StreamQualitySelectorStressTest

### Community 90 - "[1.3.2] - 2026-10-25"
Cohesion: 0.50
Nodes (4): [1.3.2] - 2026-10-25, Corrigido, Segurança, Testes

### Community 98 - "HomeSection"
Cohesion: 0.14
Nodes (13): HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL (+5 more)

## Knowledge Gaps
- **86 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+81 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 527 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **52 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `PlayerScreen.kt`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `JamSessionManager`, `DownloadedTrackEntity`, `AnimatedEqualizerBars`, `.syncTimelineState`, `SearchScreen.kt`, `MainScreen.kt`, `.playTrackList`, `LibraryScreen.kt`, `LyricsData`, `SpotifyCanvasRepositoryImpl`, `Track`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `MigrationScreen`, `SmartDownloadWorker.kt`, `MusicRepository`, `KaraokeLyricsEngine`, `.isConnectedToWifi`, `JamSessionManager.kt`, `AudioQuality`, `SearchFilter`, `RadioTuningMode`, `.loadHomeFeed`, `.updateCurrentLyricsLine`, `HomeScreen.kt`, `Artist`, `UserHistoryRepository`, `LoudnessStandard`, `ConnectivityObserver`, `NewPipeStreamResolver.kt`, `.clearStreamCache`, `.loadCanvasVideo`, `SmartDownloadManager`, `TrackRelatedData`, `HomeSection`?**
  _High betweenness centrality (0.274) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AudioPlayerService` (e.g. with `VocalIsolationAudioProcessor` and `Corrigido`) actually correct?**
  _`AudioPlayerService` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _86 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DownloadChunkRangeStressTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.09462365591397849 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `DownloadChunkRangeStressTest.kt`, `PlayerScreen.kt`, `MigrationManager`, `MusicViewModel`, `InnerTubeCatalogClientTest`, `JamSessionManager`, `AudioMetadataTagger.kt`, `DownloadedTrackEntity`, `AnimatedEqualizerBars`, `.syncTimelineState`, `SearchScreen.kt`, `MainScreen.kt`, `.playTrackList`, `LibraryScreen.kt`, `AudioMetadataTagger`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `UserAffinityRepositoryImpl`, `InnerTubeCatalogClient.kt`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `KaraokeLyricsEngine`, `.isConnectedToWifi`, `JamSessionManager.kt`, `UserHistoryRepositoryImpl`, `RadioTuningMode`, `HomeScreen.kt`, `Artist`, `InnerTubeCatalogClient`, `UserHistoryRepository`, `ConnectivityObserver`, `AudioMetadataTaggerTest.kt`, `LibraryRepository.kt`, `.loadCanvasVideo`, `TrackOptionsBottomSheet`, `SmartDownloadManager`, `TrackRelatedData`, `HomeSection`?**
  _High betweenness centrality (0.178) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.027861047434314935 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `NewPipeStreamResolver.kt`, `.clearStreamCache`, `DownloadChunkRangeStressTest.kt`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `SettingsScreen.kt`, `MusicViewModel`, `CompositeStreamResolver`, `MimeAndContainerDetectionStressTest.kt`, `AudioPlayerService.kt`, `LyricsData`, `InnerTubeStreamResolver`, `StreamResolver`, `MusicViewModel.kt`, `AudioCoreDownloadsCrossfadeQualityTest`, `LibraryRepository.kt`, `.prefetchTrack`?**
  _High betweenness centrality (0.066) - this node is a cross-community bridge._