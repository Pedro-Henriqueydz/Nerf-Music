# Graph Report - Nerf Music  (2026-10-30)

## Corpus Check
- 138 files · ~158,396 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 59 file(s) not represented in the graph (top: .xml 44, .otf 5, (none) 2)

## Summary
- 2569 nodes · 6510 edges · 121 communities (53 shown, 68 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 71 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `259e7668`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MimeAndContainerDetectionStressTest.kt
- PlayerScreen.kt
- RecapScreen.kt
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- InnerTubeCatalogClientTest
- JamSessionManager
- SocialShareHelper.kt
- steps
- VocalIsolationAudioProcessor
- InnerTubeCatalogClient
- HomeScreen
- .syncTimelineState
- JamSessionSheet.kt
- KaraokeLyricsEngine
- DownloadedTrackDao
- LibraryScreen
- ConnectivityObserver
- SpotifyCanvasRepositoryImpl
- DownloadedTrackEntity
- UserSettingsRepository
- .rank
- AudioMetadataTagger
- LibraryRepositoryImpl
- Artist
- Block3EmpiricalChallengeTest
- LoudnessNormalizationAudioProcessor
- OnboardingScreen.kt
- Track
- RecapVideoExporter.kt
- MigrationScreen.kt
- ContextWrapper
- SmartDownloadWorker.kt
- .`JamWebSocketServer performs RFC6455 handshake and bidirectional message exchange`
- MusicRepository
- LyricsRepositoryImpl
- OnboardingFlowTest.kt
- FullPlayerSheet
- MainScreen.kt
- UserHistoryRepositoryImpl
- LyricsNullSanitizationStressTest
- WidgetRenderer.kt
- UserSettingsRepositoryImpl
- StreamQualitySelectorStressTest
- MainActivity.kt
- AudioPlayerService
- RadioTuningMode
- .loadHomeFeed
- LibraryScreen.kt
- InnerTubeParserAdversarialStressTest
- SearchResult
- NewPipeStreamResolver.kt
- .initializePlayerController
- NERF Music
- UserHistoryRepository
- LoudnessStandard
- SearchScreen.kt
- SettingsSubScreen
- ShareCardInstrumentedTest.kt
- MusicViewModel.kt
- CrossfadeEmpiricalTest
- file
- AudioQuality
- AudioCoreDownloadsCrossfadeQualityTest
- MigrationManager.kt
- DownloadAudioQuality
- LoudnessNormalizationAudioProcessorTest.kt
- LyricsData
- JamWebSocketServer.kt
- .applyKaraokeState
- log
- JamGlobalRelayClient
- ShareRules
- SocialShareHelper
- ArtworkColorsInstrumentedTest.kt
- InnerTubeStreamResolver
- DownloadChunkRangeStressTest.kt
- CompositeStreamResolver
- UserAffinityRepositoryImpl
- .testRelatedMultiTierSynthesisAlwaysPopulatesContent
- AudioMetadataTagger
- SharePreviewSheet
- Context
- .onCreate
- .syncNow
- InnerTubeCatalogClient.kt
- .onCreate
- SmartDownloadWorker
- UserAffinityRepository
- UpdateNotificationHelper
- LibraryFilter
- DataSaverInterceptor.kt
- ArtistScreen.kt
- SmoothFlingBehavior.kt
- NERF Music — widget e prévia de compartilhamento
- LibrarySortOption
- Theme.kt
- DownloadOutcome
- .prefetchTrack
- LoudnessNormalizationAudioProcessorTest
- BiquadFilter
- Test
- StreamResolver

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 222 edges
2. `Track` - 193 edges
3. `AudioPlayerService` - 80 edges
4. `InnerTubeCatalogClient` - 62 edges
5. `LibraryRepositoryImpl` - 51 edges
6. `AudioQuality` - 48 edges
7. `MigrationManager` - 46 edges
8. `HomeSection` - 43 edges
9. `UserSettingsRepositoryImpl` - 42 edges
10. `UserHistoryRepositoryImpl` - 36 edges

## Surprising Connections (you probably didn't know these)
- `AudioPlayerService` --calls--> `VocalIsolationAudioProcessor`  [INFERRED]
  app/src/main/java/com/nerf/music/player/AudioPlayerService.kt → app/src/main/java/com/nerf/music/player/VocalIsolationAudioProcessor.kt
- `createDefaultStreamResolver()` --calls--> `InnerTubeStreamResolver`  [INFERRED]
  app/src/main/java/com/nerf/music/player/CompositeStreamResolver.kt → app/src/main/java/com/nerf/music/player/InnerTubeStreamResolver.kt
- `createDefaultStreamResolver()` --calls--> `NewPipeStreamResolver`  [INFERRED]
  app/src/main/java/com/nerf/music/player/CompositeStreamResolver.kt → app/src/main/java/com/nerf/music/player/NewPipeStreamResolver.kt
- `AnimatedEqualizerBars()` --calls--> `Size`  [INFERRED]
  app/src/main/java/com/nerf/music/ui/components/AnimatedEqualizerBars.kt → app/src/main/java/com/nerf/music/widget/WidgetRenderer.kt
- `JamPersonWavesIcon()` --calls--> `Size`  [INFERRED]
  app/src/main/java/com/nerf/music/ui/components/JamPersonWavesIcon.kt → app/src/main/java/com/nerf/music/widget/WidgetRenderer.kt

## Import Cycles
- None detected.

## Communities (121 total, 68 thin omitted)

### Community 0 - "MimeAndContainerDetectionStressTest.kt"
Cohesion: 0.12
Nodes (4): MimeAndContainerDetectionStressTest, ContextWrapper, InvocationHandler, StreamResolver

### Community 1 - "PlayerScreen.kt"
Cohesion: 0.03
Nodes (3): largestFittingCharacterBoundary(), BroadcastReceiver, splitInlineLyricIntoSegments()

### Community 2 - "RecapScreen.kt"
Cohesion: 0.14
Nodes (19): RecapArtistStats, RecapSummary, RecapTrackStats, ArtistRow(), EmptyRecap(), formatRecapPlayCount(), formattedRecapDuration(), LegacyRecapStoryPage() (+11 more)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (4): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.20
Nodes (24): SmartDownloadSyncState, AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), LoudnessNormalizationBottomSheet(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard() (+16 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.10
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 8 - "JamSessionManager"
Cohesion: 0.06
Nodes (35): JamCommandType, ADD_TO_QUEUE, KICK_PARTICIPANT, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK (+27 more)

### Community 10 - "steps"
Cohesion: 0.05
Nodes (38): if, runs-on, steps, GH_REPO, GH_TOKEN, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64 (+30 more)

### Community 13 - "HomeScreen"
Cohesion: 0.13
Nodes (17): ExploreMotionItem(), ExploreScreen(), YtmExploreQuickButton(), HomeScreen(), HomeSongCard(), HomeSpeedDialPlaybackSection(), YtmDailyDiscoverHeroSection(), YtmDownloadCard() (+9 more)

### Community 15 - "JamSessionSheet.kt"
Cohesion: 0.07
Nodes (3): JamSessionSheet(), JamSettingToggleRow(), JamVisualMatrixBadge()

### Community 16 - "KaraokeLyricsEngine"
Cohesion: 0.14
Nodes (4): BatchTranslationResult, KaraokeLyricsEngine, TranslationLanguageOption, KaraokeAndJamTest

### Community 18 - "LibraryScreen"
Cohesion: 0.26
Nodes (9): LibraryGridCard(), LibraryListItem(), LibraryScreen(), NewBottomSheetOption(), PlaylistCollageCover(), TrackListDetailView(), UserPlaylistDetailView(), SaveToPlaylistSheet() (+1 more)

### Community 19 - "ConnectivityObserver"
Cohesion: 0.08
Nodes (14): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+6 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 21 - "DownloadedTrackEntity"
Cohesion: 0.16
Nodes (4): DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 23 - ".rank"
Cohesion: 0.11
Nodes (4): ExploreRecommendations, ReleaseRecommendations, ExploreRecommendationsTest, ReleaseRecommendationsTest

### Community 29 - "OnboardingScreen.kt"
Cohesion: 0.05
Nodes (12): applePressFeedback(), AppleArtistSelectionStep(), AppleArtistSphereCard(), AppleGlassFeatureCard(), AppleWelcomeStep(), OnboardingScreen(), artistListeningShares(), ArtistMosaicTile() (+4 more)

### Community 30 - "Track"
Cohesion: 0.09
Nodes (4): UserPlaylist, LibraryRepository, PlaylistOptionItem(), PlaylistOptionsBottomSheet()

### Community 32 - "MigrationScreen.kt"
Cohesion: 0.11
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 37 - "LyricsRepositoryImpl"
Cohesion: 0.20
Nodes (3): LyricsRepository, LyricsRepositoryImpl, optCleanString()

### Community 38 - "OnboardingFlowTest.kt"
Cohesion: 0.15
Nodes (3): OnboardingFlowTest, ContextWrapper, InvocationHandler

### Community 39 - "FullPlayerSheet"
Cohesion: 0.08
Nodes (23): PlaybackState, KaraokeSyncSnapshot, AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar(), ApplePopoverOptionRow(), formatDurationMs(), FullPlayerSheet() (+15 more)

### Community 40 - "MainScreen.kt"
Cohesion: 0.06
Nodes (7): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar()

### Community 43 - "WidgetRenderer.kt"
Cohesion: 0.07
Nodes (8): CompactPlayerWidgetProvider, ExpandedPlayerWidgetProvider, ThreeByOneWidgetProvider, Size, FOUR, THREE, TWO, WidgetRenderer

### Community 44 - "UserSettingsRepositoryImpl"
Cohesion: 0.09
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepositoryImpl

### Community 48 - "RadioTuningMode"
Cohesion: 0.33
Nodes (5): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR

### Community 54 - "NewPipeStreamResolver.kt"
Cohesion: 0.10
Nodes (4): NerfMusicApp, NewPipeStreamResolver, OkHttpDownloader, NewPipeStreamResolverTest

### Community 55 - ".initializePlayerController"
Cohesion: 0.16
Nodes (5): normalizeRecapArtistName(), RecapArtistContribution, RecapArtistCredit, splitNames(), splitRecapArtistCredit()

### Community 56 - "NERF Music"
Cohesion: 0.22
Nodes (8): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão

### Community 57 - "UserHistoryRepository"
Cohesion: 0.08
Nodes (12): HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL (+4 more)

### Community 58 - "LoudnessStandard"
Cohesion: 0.17
Nodes (7): LoudnessStandard, APPLE_MUSIC, BROADCAST_EBU, CD_LOUD, CD_STANDARD, OFF, SPOTIFY

### Community 59 - "SearchScreen.kt"
Cohesion: 0.16
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 60 - "SettingsSubScreen"
Cohesion: 0.22
Nodes (9): SettingsSubScreen, ABOUT, CONTENT_DISPLAY, DATA_SAVER, MAIN, MEDIA_QUALITY, MIGRATION, PLAYBACK (+1 more)

### Community 65 - "AudioQuality"
Cohesion: 0.10
Nodes (7): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, StreamResolver

### Community 68 - "AudioCoreDownloadsCrossfadeQualityTest"
Cohesion: 0.17
Nodes (3): AudioCoreDownloadsCrossfadeQualityTest, StreamResolver, StreamResolver

### Community 74 - "DownloadAudioQuality"
Cohesion: 0.16
Nodes (6): SmartDownloadManager, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 76 - "LyricsData"
Cohesion: 0.38
Nodes (5): LyricsData, SyncedLyricsLine, SyncedLyricsWord, inlineLyricSegmentIndex(), InlineSyncedLyrics()

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 80 - "log"
Cohesion: 0.07
Nodes (8): AppDatabase, LibraryTrackDao, LibraryTrackEntity, BackupPreferenceEntry, BackupRepository, BackupRestoreSummary, NerfMusicBackup, QolFeaturesTest

### Community 81 - "JamGlobalRelayClient"
Cohesion: 0.08
Nodes (4): JamGlobalRelayClient, WebSocketListener, JamPersonWavesIcon(), NetworkCallback

### Community 83 - "SocialShareHelper"
Cohesion: 0.26
Nodes (6): PaletteColors, SocialShareHelper, Style, FULL_COVER, GLASS, MINIMAL

### Community 84 - "ArtworkColorsInstrumentedTest.kt"
Cohesion: 0.23
Nodes (3): ArtworkColorsInstrumentedTest, ArtworkColors, Colors

### Community 85 - "InnerTubeStreamResolver"
Cohesion: 0.13
Nodes (4): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest

### Community 86 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.10
Nodes (3): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler

### Community 87 - "CompositeStreamResolver"
Cohesion: 0.16
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 91 - "SharePreviewSheet"
Cohesion: 0.33
Nodes (7): CircleAction(), DividerLine(), SectionLabel(), ShareAction(), ShareActionModern(), SharePreviewSheet(), StyleOption()

### Community 96 - "InnerTubeCatalogClient.kt"
Cohesion: 0.13
Nodes (11): AlbumDetail, ArtistDetail, SearchFilter, ALBUMS, ALL, ARTISTS, SONGS, RelatedSection (+3 more)

### Community 99 - "UserAffinityRepository"
Cohesion: 0.13
Nodes (3): Track, UserAffinityRepository, SmartRadioEngine

### Community 103 - "LibraryFilter"
Cohesion: 0.29
Nodes (7): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS

### Community 105 - "ArtistScreen.kt"
Cohesion: 0.11
Nodes (10): AnimatedEqualizerBars(), AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen() (+2 more)

### Community 108 - "NERF Music — widget e prévia de compartilhamento"
Cohesion: 0.40
Nodes (4): NERF Music — widget e prévia de compartilhamento, Prévia de compartilhamento, Revisão visual, Widget expandido

### Community 109 - "LibrarySortOption"
Cohesion: 0.50
Nodes (4): LibrarySortOption, A_TO_Z, RECENT, RECENTLY_ADDED

### Community 112 - "DownloadOutcome"
Cohesion: 0.40
Nodes (5): DownloadOutcome, ALLOWED, BLOCKED_ALWAYS_ON, BLOCKED_AUTOMATIC_DATA_SAVER, BLOCKED_CELLULAR_DISABLED

## Knowledge Gaps
- **94 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+89 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 568 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **68 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Track` connect `UserAffinityRepository` to `PlayerScreen.kt`, `RecapScreen.kt`, `MigrationManager`, `MusicViewModel`, `InnerTubeCatalogClientTest`, `JamSessionManager`, `SocialShareHelper.kt`, `InnerTubeCatalogClient`, `HomeScreen`, `.syncTimelineState`, `KaraokeLyricsEngine`, `LibraryScreen`, `DownloadedTrackEntity`, `.rank`, `AudioMetadataTagger`, `LibraryRepositoryImpl`, `Artist`, `Track`, `OnboardingFlowTest.kt`, `FullPlayerSheet`, `MainScreen.kt`, `UserHistoryRepositoryImpl`, `WidgetRenderer.kt`, `RadioTuningMode`, `LibraryScreen.kt`, `SearchResult`, `.initializePlayerController`, `UserHistoryRepository`, `SearchScreen.kt`, `ShareCardInstrumentedTest.kt`, `MusicViewModel.kt`, `.loadLyrics`, `LibraryRepository.kt`, `MigrationManager.kt`, `DownloadAudioQuality`, `LoudnessNormalizationAudioProcessorTest.kt`, `AudioPlayerService.kt`, `log`, `SocialShareHelper`, `DownloadChunkRangeStressTest.kt`, `UserAffinityRepositoryImpl`, `.testRelatedMultiTierSynthesisAlwaysPopulatesContent`, `AudioMetadataTagger`, `SharePreviewSheet`, `InnerTubeCatalogClient.kt`, `.onCreate`, `.playTrackList`, `ArtistScreen.kt`, `.playWidgetMix`?**
  _High betweenness centrality (0.237) - this node is a cross-community bridge._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _94 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `MimeAndContainerDetectionStressTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.11954022988505747 - nodes in this community are weakly interconnected._
- **Why does `MusicViewModel` connect `MusicViewModel` to `RecapScreen.kt`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `InnerTubeCatalogClientTest`, `JamSessionManager`, `HomeScreen`, `.syncTimelineState`, `KaraokeLyricsEngine`, `LibraryScreen`, `ConnectivityObserver`, `SpotifyCanvasRepositoryImpl`, `DownloadedTrackEntity`, `UserSettingsRepository`, `.rank`, `OnboardingScreen.kt`, `Track`, `MigrationScreen.kt`, `MusicRepository`, `LyricsRepositoryImpl`, `FullPlayerSheet`, `MainScreen.kt`, `UserSettingsRepositoryImpl`, `MainActivity.kt`, `RadioTuningMode`, `.loadHomeFeed`, `.updateCurrentLyricsLine`, `LibraryScreen.kt`, `NewPipeStreamResolver.kt`, `.initializePlayerController`, `UserHistoryRepository`, `LoudnessStandard`, `SearchScreen.kt`, `MusicViewModel.kt`, `file`, `AudioQuality`, `.loadLyrics`, `DownloadAudioQuality`, `LyricsData`, `InnerTubeCatalogClient.kt`, `SmartDownloadWorker`, `UserAffinityRepository`, `.setKaraokeMode`, `UpdateNotificationHelper`, `.playTrackList`, `ArtistScreen.kt`, `.prefetchTrack`?**
  _High betweenness centrality (0.199) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.02836637047163363 - nodes in this community are weakly interconnected._
- **Why does `AudioPlayerService` connect `AudioPlayerService` to `JamSessionManager`, `VocalIsolationAudioProcessor`, `UserSettingsRepository`, `LibraryRepositoryImpl`, `WidgetRenderer.kt`, `LoudnessStandard`, `MusicViewModel.kt`, `AudioQuality`, `.loadLyrics`, `LoudnessNormalizationAudioProcessorTest.kt`, `AudioPlayerService.kt`, `.applyKaraokeState`, `Context`, `Listener`, `.onCreate`, `.setKaraokeMode`, `.playWidgetMix`, `.prefetchTrack`, `.applyKaraokeState`?**
  _High betweenness centrality (0.052) - this node is a cross-community bridge._
- **Should `RecapScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.1443850267379679 - nodes in this community are weakly interconnected._