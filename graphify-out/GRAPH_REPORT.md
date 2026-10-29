# Graph Report - Nerf Music  (2026-10-29)

## Corpus Check
- 126 files · ~148,249 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 59 file(s) not represented in the graph (top: .xml 44, .otf 5, (none) 2)

## Summary
- 2322 nodes · 5691 edges · 121 communities (50 shown, 71 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 100 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d788a89a`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MimeAndContainerDetectionStressTest.kt
- PlayerScreen.kt
- ArtistScreen.kt
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- InnerTubeCatalogClientTest
- JamSessionManager
- SocialShareHelper.kt
- steps
- VocalIsolationAudioProcessor
- DownloadedTrackEntity
- HomeScreen.kt
- .syncTimelineState
- HomeSection
- LyricsNullSanitizationStressTest
- LibraryScreen
- Block3EmpiricalChallengeTest
- SpotifyCanvasRepositoryImpl
- DownloadedTrackDao
- UserSettingsRepository
- MainActivity.kt
- AudioMetadataTagger
- LibraryRepositoryImpl
- MusicViewModel.kt
- Registro de Alterações (Changelog) - NERF Music
- LoudnessNormalizationAudioProcessor
- OnboardingScreen.kt
- Track
- MigrationScreen.kt
- LyricsRepositoryImpl
- SmartDownloadWorker.kt
- AudioCoreDownloadsCrossfadeQualityTest.kt
- MusicRepository
- KaraokeLyricsEngine
- OnboardingFlowTest.kt
- FullPlayerSheet
- MainScreen.kt
- DownloadChunkRangeStressTest.kt
- JamSessionSheet
- .update
- AudioQuality
- StreamQualitySelectorStressTest
- KaraokeAndJamTest
- AudioPlayerService
- RadioTuningMode
- .loadHomeFeed
- LibraryScreen.kt
- InnerTubeParserAdversarialStressTest
- SearchResult
- InnerTubeCatalogClient
- NERF Music
- UserHistoryRepository
- LoudnessStandard
- SearchScreen.kt
- SettingsSubScreen
- ShareCardInstrumentedTest.kt
- UserAffinityRepositoryImpl
- CrossfadeEmpiricalTest
- file
- AudioQuality
- NewPipeStreamResolver.kt
- DownloadAudioQuality
- JamWebSocketServer.kt
- Listener
- AppDatabase
- JamGlobalRelayClient
- ShareRules
- SocialShareHelper
- ArtworkColorsInstrumentedTest.kt
- InnerTubeStreamResolverTest
- Test
- AudioCoreDownloadsCrossfadeQualityTest.kt
- [1.3.2] - 2026-10-25
- TrackRelatedData
- .prefetchTrack
- InnerTubeStreamResolver
- Context
- README.md
- UpdateNotificationHelper
- [1.4.3] - 2026-10-29 [RELEASED]
- Adicionado
- .onReceive
- .playWidgetMix
- JamPersonWavesIcon
- .createTestRepository
- MimeAndContainerDetectionStressTest
- .syncNow
- NERF Music — widget e prévia de compartilhamento
- NestedScrollConnection
- Style
- PlayerUiUtilsTest.kt
- SearchScreen
- launch
- StreamResolver
- Context
- UpNextResult
- StreamResolver
- StreamResolver
- Listener

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 208 edges
2. `Track` - 164 edges
3. `AudioPlayerService` - 72 edges
4. `InnerTubeCatalogClient` - 55 edges
5. `LibraryRepositoryImpl` - 52 edges
6. `AudioQuality` - 48 edges
7. `MigrationManager` - 46 edges
8. `UserSettingsRepositoryImpl` - 42 edges
9. `JamSessionManager` - 36 edges
10. `UserHistoryRepositoryImpl` - 32 edges

## Surprising Connections (you probably didn't know these)
- `Adicionado` --references--> `ConnectivityObserver`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/util/ConnectivityObserver.kt
- `Adicionado` --references--> `MainActivity`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/MainActivity.kt
- `Adicionado` --references--> `HomeFeedSynthesizer`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/HomeFeedSynthesizer.kt
- `Adicionado` --references--> `LibraryRepositoryImpl`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/LibraryRepository.kt
- `Adicionado` --references--> `UserAffinityRepository`  [INFERRED]
  CHANGELOG.md → app/src/main/java/com/nerf/music/data/repository/UserAffinityRepository.kt

## Import Cycles
- None detected.

## Communities (121 total, 71 thin omitted)

### Community 2 - "ArtistScreen.kt"
Cohesion: 0.11
Nodes (10): AnimatedEqualizerBars(), AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen() (+2 more)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (6): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper, [1.3.1] - 2026-10-25, Segurança e Estabilidade

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.22
Nodes (23): AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), LoudnessNormalizationBottomSheet(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SettingsMainContent() (+15 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 8 - "JamSessionManager"
Cohesion: 0.05
Nodes (36): JamCommandType, ADD_TO_QUEUE, KICK_PARTICIPANT, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK (+28 more)

### Community 10 - "steps"
Cohesion: 0.07
Nodes (27): if, runs-on, steps, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD (+19 more)

### Community 12 - "DownloadedTrackEntity"
Cohesion: 0.18
Nodes (4): DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 13 - "HomeScreen.kt"
Cohesion: 0.21
Nodes (14): ExploreScreen(), HomeScreen(), HomeSongCard(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel() (+6 more)

### Community 15 - "HomeSection"
Cohesion: 0.18
Nodes (13): HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL (+5 more)

### Community 18 - "LibraryScreen"
Cohesion: 0.13
Nodes (18): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS, LibraryGridCard() (+10 more)

### Community 19 - "Block3EmpiricalChallengeTest"
Cohesion: 0.06
Nodes (21): SyncedLyricsLine, SyncedLyricsWord, QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status (+13 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 24 - "AudioMetadataTagger"
Cohesion: 0.11
Nodes (7): AudioMetadataTagger, ExtractedMetadata, AudioMetadataTaggerTest, [1.3.5] - 2026-10-27 [RELEASED], Adicionado, Corrigido, Testes

### Community 25 - "LibraryRepositoryImpl"
Cohesion: 0.11
Nodes (3): UserPlaylist, Http200RangeUnsupportedException, LibraryRepositoryImpl

### Community 26 - "MusicViewModel.kt"
Cohesion: 0.12
Nodes (3): Artist, ArtistDetail, HomeFeedSynthesizer

### Community 27 - "Registro de Alterações (Changelog) - NERF Music"
Cohesion: 0.12
Nodes (17): [1.0.8] - 2026-10-23, [1.2.0] - 2026-10-24, [1.3.0] - 2026-10-24, [1.3.3] - 2026-10-25, [1.3.4] - 2026-10-25, [1.4.4] - 2026-10-29 [RELEASED], [1.4.5] - 2026-10-29 [RELEASED], Adicionado (+9 more)

### Community 28 - "LoudnessNormalizationAudioProcessor"
Cohesion: 0.14
Nodes (3): Biquad, LoudnessNormalizationAudioProcessor, LoudnessNormalizationAudioProcessorTest

### Community 29 - "OnboardingScreen.kt"
Cohesion: 0.06
Nodes (6): applePressFeedback(), AppleArtistSelectionStep(), AppleArtistSphereCard(), AppleGlassFeatureCard(), AppleWelcomeStep(), OnboardingScreen()

### Community 30 - "Track"
Cohesion: 0.08
Nodes (4): Track, LibraryRepository, UserAffinityRepository, SmartRadioEngine

### Community 32 - "MigrationScreen.kt"
Cohesion: 0.11
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 33 - "LyricsRepositoryImpl"
Cohesion: 0.17
Nodes (6): LyricsData, LyricsRepository, LyricsRepositoryImpl, optCleanString(), inlineLyricSegmentIndex(), InlineSyncedLyrics()

### Community 37 - "KaraokeLyricsEngine"
Cohesion: 0.22
Nodes (4): BatchTranslationResult, KaraokeLyricsEngine, TranslationLanguageOption, Corrigido

### Community 38 - "OnboardingFlowTest.kt"
Cohesion: 0.08
Nodes (4): UserHistoryRepositoryImpl, OnboardingFlowTest, ContextWrapper, InvocationHandler

### Community 39 - "FullPlayerSheet"
Cohesion: 0.16
Nodes (18): PlaybackState, KaraokeSyncSnapshot, AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar(), ApplePopoverOptionRow(), FullPlayerSheet(), KaraokeCountdownDotsRow() (+10 more)

### Community 40 - "MainScreen.kt"
Cohesion: 0.09
Nodes (9): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar(), SaveToPlaylistSheet() (+1 more)

### Community 42 - "JamSessionSheet"
Cohesion: 0.21
Nodes (8): JamSessionSheet(), JamSettingToggleRow(), JamVisualMatrixBadge(), [1.4.1] - 2026-10-27 [RELEASED], [1.4.2] - 2026-10-28 [RELEASED], Adicionado, Adicionado, Aprimorado & Corrigido

### Community 43 - ".update"
Cohesion: 0.24
Nodes (5): Size, FOUR, THREE, TWO, WidgetRenderer

### Community 44 - "AudioQuality"
Cohesion: 0.09
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepositoryImpl

### Community 48 - "RadioTuningMode"
Cohesion: 0.33
Nodes (5): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR

### Community 51 - "LibraryScreen.kt"
Cohesion: 0.06
Nodes (4): PlaylistOptionItem(), PlaylistOptionsBottomSheet(), TrackOptionRow(), TrackOptionsBottomSheet()

### Community 53 - "SearchResult"
Cohesion: 0.16
Nodes (3): AlbumDetail, SearchResult, MusicRepositoryImpl

### Community 56 - "NERF Music"
Cohesion: 0.25
Nodes (8): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão

### Community 58 - "LoudnessStandard"
Cohesion: 0.17
Nodes (7): LoudnessStandard, APPLE_MUSIC, BROADCAST_EBU, CD_LOUD, CD_STANDARD, OFF, SPOTIFY

### Community 60 - "SettingsSubScreen"
Cohesion: 0.20
Nodes (10): SearchableSetting, SettingsSubScreen, ABOUT, CONTENT_DISPLAY, DATA_SAVER, MAIN, MEDIA_QUALITY, MIGRATION (+2 more)

### Community 65 - "AudioQuality"
Cohesion: 0.12
Nodes (6): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH

### Community 73 - "NewPipeStreamResolver.kt"
Cohesion: 0.05
Nodes (7): NerfMusicApp, NewPipeStreamResolver, OkHttpDownloader, DataSaverInterceptor, NetworkConnectivityObserver, NetworkUtils, NewPipeStreamResolverTest

### Community 74 - "DownloadAudioQuality"
Cohesion: 0.10
Nodes (7): SmartDownloadManager, SmartDownloadSyncState, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 81 - "JamGlobalRelayClient"
Cohesion: 0.10
Nodes (3): JamGlobalRelayClient, WebSocketListener, NetworkCallback

### Community 84 - "ArtworkColorsInstrumentedTest.kt"
Cohesion: 0.24
Nodes (3): ArtworkColorsInstrumentedTest, ArtworkColors, Colors

### Community 87 - "AudioCoreDownloadsCrossfadeQualityTest.kt"
Cohesion: 0.06
Nodes (10): CompositeStreamResolver, createDefaultStreamResolver(), AudioCoreDownloadsCrossfadeQualityTest, ContextWrapper, InvocationHandler, StreamResolver, StreamResolver, DefaultStreamResolverTest (+2 more)

### Community 88 - "[1.3.2] - 2026-10-25"
Cohesion: 0.50
Nodes (4): [1.3.2] - 2026-10-25, Corrigido, Segurança, Testes

### Community 89 - "TrackRelatedData"
Cohesion: 0.33
Nodes (3): RelatedSection, TrackRelatedData, RelatedMultiTierFallbackTest

### Community 91 - "InnerTubeStreamResolver"
Cohesion: 0.27
Nodes (3): CandidateFormat, ClientConfig, InnerTubeStreamResolver

### Community 93 - "README.md"
Cohesion: 0.33
Nodes (3): Correções, Informações da compilação, NERF Music v1.4.5 — Correção do widget expandido

### Community 96 - "[1.4.3] - 2026-10-29 [RELEASED]"
Cohesion: 0.67
Nodes (3): [1.4.3] - 2026-10-29 [RELEASED], Adicionado, Aprimorado & Corrigido

### Community 101 - ".playWidgetMix"
Cohesion: 0.22
Nodes (4): [1.4.0] - 2026-10-27 [RELEASED], Adicionado, Adicionado, Corrigido

### Community 108 - "NERF Music — widget e prévia de compartilhamento"
Cohesion: 0.40
Nodes (4): NERF Music — widget e prévia de compartilhamento, Prévia de compartilhamento, Revisão visual, Widget expandido

### Community 110 - "Style"
Cohesion: 0.50
Nodes (4): Style, FULL_COVER, GLASS, MINIMAL

### Community 112 - "SearchScreen"
Cohesion: 0.67
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

## Knowledge Gaps
- **92 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+87 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 552 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **71 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Track` connect `Track` to `PlayerScreen.kt`, `ArtistScreen.kt`, `MigrationManager`, `MusicViewModel`, `InnerTubeCatalogClientTest`, `JamSessionManager`, `SocialShareHelper.kt`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `HomeSection`, `.playTrackList`, `LibraryScreen`, `AudioMetadataTagger`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `WidgetRenderer.kt`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `KaraokeLyricsEngine`, `OnboardingFlowTest.kt`, `FullPlayerSheet`, `MainScreen.kt`, `DownloadChunkRangeStressTest.kt`, `.update`, `KaraokeAndJamTest`, `AudioPlayerService`, `RadioTuningMode`, `LibraryScreen.kt`, `SearchResult`, `InnerTubeCatalogClient`, `.initializePlayerController`, `UserHistoryRepository`, `SearchScreen.kt`, `ShareCardInstrumentedTest.kt`, `UserAffinityRepositoryImpl`, `.loadLyrics`, `LibraryRepository.kt`, `NewPipeStreamResolver.kt`, `DownloadAudioQuality`, `InnerTubeCatalogClient.kt`, `AudioPlayerService.kt`, `SocialShareHelper`, `TrackRelatedData`, `.buildTrackUri`, `.trackToMediaItem`, `.createTestRepository`, `PlayerUiUtilsTest.kt`, `SearchScreen`, `UpNextResult`?**
  _High betweenness centrality (0.223) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AudioPlayerService` (e.g. with `VocalIsolationAudioProcessor` and `Corrigido`) actually correct?**
  _`AudioPlayerService` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _92 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `MimeAndContainerDetectionStressTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.1437908496732026 - nodes in this community are weakly interconnected._
- **Why does `MusicViewModel` connect `MusicViewModel` to `ArtistScreen.kt`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `JamSessionManager`, `DownloadedTrackEntity`, `HomeScreen.kt`, `.syncTimelineState`, `HomeSection`, `.playTrackList`, `LibraryScreen`, `Block3EmpiricalChallengeTest`, `SpotifyCanvasRepositoryImpl`, `UserSettingsRepository`, `MainActivity.kt`, `LibraryRepositoryImpl`, `MusicViewModel.kt`, `Track`, `MigrationScreen.kt`, `LyricsRepositoryImpl`, `SmartDownloadWorker.kt`, `MusicRepository`, `KaraokeLyricsEngine`, `FullPlayerSheet`, `MainScreen.kt`, `AudioQuality`, `RadioTuningMode`, `.loadHomeFeed`, `.updateCurrentLyricsLine`, `SearchResult`, `.initializePlayerController`, `UserHistoryRepository`, `LoudnessStandard`, `SearchScreen.kt`, `file`, `AudioQuality`, `.loadLyrics`, `NewPipeStreamResolver.kt`, `DownloadAudioQuality`, `TrackRelatedData`, `.prefetchTrack`, `.trackToMediaItem`, `SearchScreen`?**
  _High betweenness centrality (0.207) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.024484536082474227 - nodes in this community are weakly interconnected._
- **Why does `AudioQuality` connect `AudioQuality` to `MimeAndContainerDetectionStressTest.kt`, `SettingsScreen.kt`, `MusicViewModel`, `Block3EmpiricalChallengeTest`, `UserSettingsRepository`, `MusicViewModel.kt`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `DownloadChunkRangeStressTest.kt`, `AudioQuality`, `NewPipeStreamResolver.kt`, `InnerTubeCatalogClient.kt`, `AudioPlayerService.kt`, `InnerTubeStreamResolverTest`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `.prefetchTrack`, `InnerTubeStreamResolver`, `StreamResolver`, `StreamResolver`, `StreamResolver`?**
  _High betweenness centrality (0.071) - this node is a cross-community bridge._