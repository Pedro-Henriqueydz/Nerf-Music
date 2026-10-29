# Graph Report - Nerf Music  (2026-10-28)

## Corpus Check
- 115 files · ~143,762 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 40 file(s) not represented in the graph (top: .xml 26, .otf 5, (none) 2)

## Summary
- 2232 nodes · 5468 edges · 100 communities (47 shown, 53 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 115 edges (avg confidence: 0.9)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `f8c53bfa`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DownloadChunkRangeStressTest.kt
- PlayerScreen.kt
- ArtistScreen.kt
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager
- MusicViewModel
- InnerTubeCatalogClientTest
- JamSessionManager
- steps
- VocalIsolationAudioProcessor
- PersistenceAndOrphanScannerStressTest
- AnimatedEqualizerBars
- .syncTimelineState
- LyricsRepositoryImpl
- LyricsNullSanitizationStressTest
- MainScreen
- Block3EmpiricalChallengeTest
- SpotifyCanvasRepositoryImpl
- DownloadedTrackDao
- UserSettingsRepository
- Track
- AudioMetadataTagger
- LibraryRepositoryImpl
- Registro de Alterações (Changelog) - NERF Music
- LoudnessNormalizationAudioProcessor
- OnboardingScreen.kt
- UserAffinityRepositoryImpl
- AudioCoreDownloadsCrossfadeQualityTest
- MigrationScreen
- LyricsData
- SmartDownloadWorker.kt
- AudioCoreDownloadsCrossfadeQualityTest.kt
- HomeSection
- KaraokeLyricsEngine
- OnboardingFlowTest.kt
- FullPlayerSheet
- MainScreen.kt
- CompositeStreamResolver
- DownloadedTrack
- Result
- AudioQuality
- KaraokeAndJamTest
- HomeSection
- AudioPlayerService
- RadioTuningMode
- .loadHomeFeed
- .updateCurrentLyricsLine
- LibraryScreen.kt
- InnerTubeParserAdversarialStressTest
- MusicRepositoryImpl
- InnerTubeCatalogClient
- MimeAndContainerDetectionStressTest.kt
- UserHistoryRepository
- LoudnessStandard
- SearchScreen.kt
- SettingsSubScreen
- AppUpdateManager.kt
- LibraryRepository.kt
- SemanticVersion
- file
- .loadCanvasVideo
- .prefetchTrack
- dispatchers
- NewPipeStreamResolver.kt
- DownloadAudioQuality
- LoudnessNormalizationAudioProcessorTest.kt
- JamWebSocketServer.kt
- .applyKaraokeState
- AppDatabase
- JamGlobalRelayClient
- ConnectivityObserver
- Artist
- CompactPlayerWidgetProvider
- DefaultRenderersFactory
- StreamQualitySelectorStressTest
- InnerTubeStreamResolverTest
- InnerTubeStreamResolver
- ContextWrapper
- LibrarySortOption
- Listener
- ContextWrapper
- NewPipeStreamResolverTest.kt
- DataSaverInterceptor.kt
- OkHttpDownloader
- DownloadOutcome
- .onTaskRemoved
- [1.3.5] - 2026-10-27 [RELEASED]

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 209 edges
2. `Track` - 155 edges
3. `AudioPlayerService` - 64 edges
4. `InnerTubeCatalogClient` - 55 edges
5. `LibraryRepositoryImpl` - 48 edges
6. `AudioQuality` - 48 edges
7. `MigrationManager` - 46 edges
8. `UserSettingsRepositoryImpl` - 42 edges
9. `JamSessionManager` - 36 edges
10. `UserSettingsRepository` - 32 edges

## Surprising Connections (you probably didn't know these)
- `2. 🎵 Menu Oficial de Opções da Faixa (3 Pontinhos)` --references--> `TrackOptionsBottomSheet()`  [INFERRED]
  RELEASE_NOTES.md → app/src/main/java/com/nerf/music/ui/screens/TrackOptionsBottomSheet.kt
- `Adicionado` --references--> `AudioMetadataTagger`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/AudioMetadataTagger.kt
- `Adicionado` --references--> `ConnectivityObserver`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/ConnectivityObserver.kt
- `Adicionado` --references--> `MainActivity`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/MainActivity.kt
- `Adicionado` --references--> `MainActivity`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/MainActivity.kt

## Import Cycles
- None detected.

## Communities (100 total, 53 thin omitted)

### Community 0 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.09
Nodes (4): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler, StreamResolver

### Community 2 - "ArtistScreen.kt"
Cohesion: 0.11
Nodes (10): AnimatedEqualizerBars(), AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen() (+2 more)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.20
Nodes (23): AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), LoudnessNormalizationBottomSheet(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SearchableSetting (+15 more)

### Community 5 - "AppUpdateManager"
Cohesion: 0.22
Nodes (4): UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager

### Community 8 - "JamSessionManager"
Cohesion: 0.05
Nodes (36): JamCommandType, ADD_TO_QUEUE, KICK_PARTICIPANT, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK (+28 more)

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 13 - "AnimatedEqualizerBars"
Cohesion: 0.17
Nodes (15): ExploreScreen(), HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel() (+7 more)

### Community 15 - "LyricsRepositoryImpl"
Cohesion: 0.21
Nodes (3): LyricsRepository, LyricsRepositoryImpl, optCleanString()

### Community 18 - "MainScreen"
Cohesion: 0.14
Nodes (16): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS, LibraryGridCard() (+8 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.10
Nodes (7): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success, LyricsRepositorySanitizationTest

### Community 22 - "UserSettingsRepository"
Cohesion: 0.10
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepository

### Community 23 - "Track"
Cohesion: 0.08
Nodes (4): Track, LibraryRepository, UserAffinityRepository, SmartRadioEngine

### Community 24 - "AudioMetadataTagger"
Cohesion: 0.13
Nodes (3): AudioMetadataTagger, ExtractedMetadata, AudioMetadataTaggerTest

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.05
Nodes (39): JamSessionSheet(), JamSettingToggleRow(), JamVisualMatrixBadge(), [1.0.8] - 2026-10-23, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.2] - 2026-10-25, [1.3.3] - 2026-10-25 (+31 more)

### Community 28 - "LoudnessNormalizationAudioProcessor"
Cohesion: 0.09
Nodes (3): Biquad, LoudnessNormalizationAudioProcessor, LoudnessNormalizationAudioProcessorTest

### Community 29 - "OnboardingScreen.kt"
Cohesion: 0.05
Nodes (10): applePressFeedback(), AppleArtistSelectionStep(), AppleArtistSphereCard(), AppleGlassFeatureCard(), AppleWelcomeStep(), OnboardingScreen(), 1. ✨ Onboarding Elegante & Personalização de Primeiro Acesso, 2. 🎵 Menu Oficial de Opções da Faixa (3 Pontinhos) (+2 more)

### Community 31 - "AudioCoreDownloadsCrossfadeQualityTest"
Cohesion: 0.17
Nodes (3): AudioCoreDownloadsCrossfadeQualityTest, StreamResolver, StreamResolver

### Community 32 - "MigrationScreen"
Cohesion: 0.11
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 33 - "LyricsData"
Cohesion: 0.27
Nodes (6): LyricsData, SyncedLyricsLine, SyncedLyricsWord, TranslationLanguageOption, inlineLyricSegmentIndex(), InlineSyncedLyrics()

### Community 34 - "SmartDownloadWorker.kt"
Cohesion: 0.05
Nodes (6): MainActivity, NERFMusicTheme(), UpdateCheckWorker, UpdateNotificationHelper, SmartDownloadNotificationHelper, SmartDownloadWorker

### Community 35 - "AudioCoreDownloadsCrossfadeQualityTest.kt"
Cohesion: 0.11
Nodes (3): StreamResolver, InvocationHandler, WebSocketListener

### Community 37 - "KaraokeLyricsEngine"
Cohesion: 0.20
Nodes (3): BatchTranslationResult, KaraokeLyricsEngine, Corrigido

### Community 38 - "OnboardingFlowTest.kt"
Cohesion: 0.08
Nodes (4): UserHistoryRepositoryImpl, OnboardingFlowTest, ContextWrapper, InvocationHandler

### Community 39 - "FullPlayerSheet"
Cohesion: 0.05
Nodes (25): PlaybackState, QueueFilterChip, JamPersonWavesIcon(), AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar(), ApplePopoverOptionRow(), formatDurationMs() (+17 more)

### Community 40 - "MainScreen.kt"
Cohesion: 0.08
Nodes (9): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar(), SaveToPlaylistSheet() (+1 more)

### Community 41 - "CompositeStreamResolver"
Cohesion: 0.15
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 42 - "DownloadedTrack"
Cohesion: 0.16
Nodes (3): DownloadedTrack, NetworkUtils, RoomDownloadedTrackEntityTest

### Community 43 - "Result"
Cohesion: 0.20
Nodes (3): SearchFilter, ALL, SONGS

### Community 44 - "AudioQuality"
Cohesion: 0.08
Nodes (7): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, UserSettingsRepositoryImpl

### Community 46 - "HomeSection"
Cohesion: 0.14
Nodes (14): HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL (+6 more)

### Community 48 - "RadioTuningMode"
Cohesion: 0.33
Nodes (5): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR

### Community 51 - "LibraryScreen.kt"
Cohesion: 0.07
Nodes (3): YtmExploreQuickButton(), TrackOptionRow(), TrackOptionsBottomSheet()

### Community 56 - "MimeAndContainerDetectionStressTest.kt"
Cohesion: 0.16
Nodes (3): MimeAndContainerDetectionStressTest, InvocationHandler, StreamResolver

### Community 58 - "LoudnessStandard"
Cohesion: 0.17
Nodes (7): LoudnessStandard, APPLE_MUSIC, BROADCAST_EBU, CD_LOUD, CD_STANDARD, OFF, SPOTIFY

### Community 59 - "SearchScreen.kt"
Cohesion: 0.15
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 60 - "SettingsSubScreen"
Cohesion: 0.22
Nodes (9): SettingsSubScreen, ABOUT, CONTENT_DISPLAY, DATA_SAVER, MAIN, MEDIA_QUALITY, MIGRATION, PLAYBACK (+1 more)

### Community 62 - "LibraryRepository.kt"
Cohesion: 0.13
Nodes (4): HomeFeedSynthesizer, Http200RangeUnsupportedException, [1.1.0] - 2026-10-24, Adicionado

### Community 74 - "DownloadAudioQuality"
Cohesion: 0.14
Nodes (7): SmartDownloadManager, SmartDownloadSyncState, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 81 - "JamGlobalRelayClient"
Cohesion: 0.07
Nodes (4): JamGlobalRelayClient, WebSocketListener, NetworkConnectivityObserver, NetworkCallback

### Community 82 - "ConnectivityObserver"
Cohesion: 0.12
Nodes (13): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+5 more)

### Community 83 - "Artist"
Cohesion: 0.21
Nodes (3): Artist, ArtistDetail, SearchResult

### Community 88 - "InnerTubeStreamResolver"
Cohesion: 0.23
Nodes (3): CandidateFormat, ClientConfig, InnerTubeStreamResolver

### Community 90 - "LibrarySortOption"
Cohesion: 0.50
Nodes (4): LibrarySortOption, A_TO_Z, RECENT, RECENTLY_ADDED

### Community 99 - "DownloadOutcome"
Cohesion: 0.40
Nodes (5): DownloadOutcome, ALLOWED, BLOCKED_ALWAYS_ON, BLOCKED_AUTOMATIC_DATA_SAVER, BLOCKED_CELLULAR_DISABLED

### Community 104 - "[1.3.5] - 2026-10-27 [RELEASED]"
Cohesion: 0.67
Nodes (3): [1.3.5] - 2026-10-27 [RELEASED], Adicionado, Testes

## Knowledge Gaps
- **86 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+81 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 530 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **53 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MusicViewModel` connect `MusicViewModel` to `ArtistScreen.kt`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager`, `JamSessionManager`, `AnimatedEqualizerBars`, `.syncTimelineState`, `LyricsRepositoryImpl`, `.playTrackList`, `MainScreen`, `SpotifyCanvasRepositoryImpl`, `UserSettingsRepository`, `Track`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `Registro de Alterações (Changelog) - NERF Music`, `MigrationScreen`, `LyricsData`, `SmartDownloadWorker.kt`, `HomeSection`, `KaraokeLyricsEngine`, `FullPlayerSheet`, `MainScreen.kt`, `DownloadedTrack`, `Result`, `AudioQuality`, `HomeSection`, `RadioTuningMode`, `.loadHomeFeed`, `.updateCurrentLyricsLine`, `LibraryScreen.kt`, `.initializePlayerController`, `UserHistoryRepository`, `LoudnessStandard`, `SearchScreen.kt`, `LibraryRepository.kt`, `file`, `.clearStreamCache`, `.loadCanvasVideo`, `.prefetchTrack`, `NewPipeStreamResolver.kt`, `DownloadAudioQuality`, `ConnectivityObserver`, `Artist`, `.setKaraokeMode`?**
  _High betweenness centrality (0.286) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AudioPlayerService` (e.g. with `VocalIsolationAudioProcessor` and `Corrigido`) actually correct?**
  _`AudioPlayerService` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _86 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DownloadChunkRangeStressTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.0907258064516129 - nodes in this community are weakly interconnected._
- **Why does `Track` connect `Track` to `DownloadChunkRangeStressTest.kt`, `PlayerScreen.kt`, `ArtistScreen.kt`, `MigrationManager`, `MusicViewModel`, `InnerTubeCatalogClientTest`, `JamSessionManager`, `CompactPlayerWidgetProvider.kt`, `PersistenceAndOrphanScannerStressTest`, `AnimatedEqualizerBars`, `.syncTimelineState`, `.playTrackList`, `MainScreen`, `DownloadedTrackDao`, `AudioMetadataTagger`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `UserAffinityRepositoryImpl`, `LyricsData`, `SmartDownloadWorker.kt`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `HomeSection`, `KaraokeLyricsEngine`, `OnboardingFlowTest.kt`, `FullPlayerSheet`, `MainScreen.kt`, `DownloadedTrack`, `KaraokeAndJamTest`, `HomeSection`, `RadioTuningMode`, `LibraryScreen.kt`, `MusicRepositoryImpl`, `InnerTubeCatalogClient`, `.initializePlayerController`, `UserHistoryRepository`, `SearchScreen.kt`, `LibraryRepository.kt`, `.loadCanvasVideo`, `dispatchers`, `DownloadAudioQuality`, `MigrationManager.kt`, `Artist`?**
  _High betweenness centrality (0.182) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.02411873840445269 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `DownloadChunkRangeStressTest.kt`, `.clearStreamCache`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `.prefetchTrack`, `SettingsScreen.kt`, `MusicViewModel`, `MimeAndContainerDetectionStressTest.kt`, `CompositeStreamResolver`, `NewPipeStreamResolver.kt`, `AudioPlayerService.kt`, `Block3EmpiricalChallengeTest`, `UserSettingsRepository`, `InnerTubeStreamResolverTest`, `InnerTubeStreamResolver`, `MusicViewModel.kt`, `AudioCoreDownloadsCrossfadeQualityTest`?**
  _High betweenness centrality (0.076) - this node is a cross-community bridge._