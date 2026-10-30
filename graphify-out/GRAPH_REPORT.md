# Graph Report - Nerf Music  (2026-10-30)

## Corpus Check
- 140 files · ~161,064 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 59 file(s) not represented in the graph (top: .xml 44, .otf 5, (none) 2)

## Summary
- 2610 nodes · 6607 edges · 110 communities (53 shown, 57 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 71 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a65a04c0`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DownloadChunkRangeStressTest.kt
- PlayerScreen.kt
- RecapScreen.kt
- MigrationManager
- SettingsScreen.kt
- AppUpdateManager.kt
- MusicViewModel
- InnerTubeCatalogClientTest
- JamSessionManager
- steps
- VocalIsolationAudioProcessor
- UpNextResult
- HomeScreen.kt
- .syncTimelineState
- ConnectivityObserver
- KaraokeLyricsEngine
- DownloadedTrackDao
- LibraryScreen
- Track
- SpotifyCanvasRepositoryImpl
- DownloadedTrackEntity
- UserSettingsRepository
- .rank
- AudioMetadataTagger
- JamSessionSheet.kt
- StreamResolver
- Block3EmpiricalChallengeTest
- LoudnessNormalizationAudioProcessor
- OnboardingScreen.kt
- RecapVideoExporter.kt
- MigrationScreen.kt
- LibraryRepository.kt
- SmartDownloadWorker.kt
- UserHistoryRepositoryImpl
- MusicRepository
- LyricsRepositoryImpl
- HomeSectionStyle
- FullPlayerSheet
- SharePreviewActivity.kt
- UserHistoryRepositoryImpl
- LyricsNullSanitizationStressTest
- .update
- UserSettingsRepositoryImpl
- LyricsData
- MusicViewModel.kt
- AudioPlayerService
- OkHttpDownloader.kt
- .loadHomeFeed
- HomeScreen.kt
- InnerTubeParserAdversarialStressTest
- SearchResult
- StorageUtils
- NERF Music
- HomeSection
- NewPipeStreamResolver.kt
- WidgetArtworkCacheInstrumentedTest.kt
- SettingsSubScreen
- MimeAndContainerDetectionStressTest.kt
- .selectDiscoveryTracks
- CrossfadeEmpiricalTest
- optCleanString
- AudioCoreDownloadsCrossfadeQualityTest.kt
- DataSaverInterceptor.kt
- locale
- DownloadAudioQuality
- AudioCoreDownloadsCrossfadeQualityTest.kt
- InnerTubeCatalogClient
- JamWebSocketServer.kt
- .applyKaraokeState
- log
- JamGlobalRelayClient
- ShareRules
- SocialShareHelper
- .extract
- InnerTubeStreamResolver
- LoudnessStandard
- AudioQuality
- SearchScreen
- BackupPreviewState
- RadioTuningMode
- SharePreviewSheet
- Context
- .onCreate
- .updateMediaNotificationLayout
- Listener
- Artist
- TrackRelatedData
- ShareCardInstrumentedTest.kt
- Track
- DownloadOutcome
- .testRelatedMultiTierSynthesisAlwaysPopulatesContent
- NewPipeStreamResolverTest.kt
- ExploreScreen.kt
- NERF Music — widget e prévia de compartilhamento
- NERF Music 1.5.0
- LibrarySortOption

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 228 edges
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

## Communities (110 total, 57 thin omitted)

### Community 0 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.11
Nodes (3): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler

### Community 1 - "PlayerScreen.kt"
Cohesion: 0.03
Nodes (3): NestedScrollConnection, largestFittingCharacterBoundary(), splitInlineLyricIntoSegments()

### Community 2 - "RecapScreen.kt"
Cohesion: 0.14
Nodes (19): RecapArtistStats, RecapSummary, RecapTrackStats, ArtistRow(), EmptyRecap(), formatRecapPlayCount(), formattedRecapDuration(), LegacyRecapStoryPage() (+11 more)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (4): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.23
Nodes (22): AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), LoudnessNormalizationBottomSheet(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), SettingsMainContent() (+14 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 8 - "JamSessionManager"
Cohesion: 0.05
Nodes (35): JamCommandType, ADD_TO_QUEUE, KICK_PARTICIPANT, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK (+27 more)

### Community 10 - "steps"
Cohesion: 0.05
Nodes (39): if, runs-on, steps, GH_REPO, GH_TOKEN, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64 (+31 more)

### Community 13 - "HomeScreen.kt"
Cohesion: 0.11
Nodes (17): rememberSmoothFlingBehavior(), FlingBehavior, ExploreScreen(), HomeScreen(), HomeSongCard(), HomeSpeedDialPlaybackSection(), YtmDailyDiscoverHeroSection(), YtmDownloadCard() (+9 more)

### Community 15 - "ConnectivityObserver"
Cohesion: 0.08
Nodes (14): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+6 more)

### Community 16 - "KaraokeLyricsEngine"
Cohesion: 0.12
Nodes (4): BatchTranslationResult, KaraokeLyricsEngine, TranslationLanguageOption, KaraokeAndJamTest

### Community 18 - "LibraryScreen"
Cohesion: 0.11
Nodes (18): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS, LibraryGridCard() (+10 more)

### Community 19 - "Track"
Cohesion: 0.07
Nodes (4): Track, UserPlaylist, LibraryRepository, LibraryRepositoryImpl

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 21 - "DownloadedTrackEntity"
Cohesion: 0.18
Nodes (4): DownloadedTrackEntity, DownloadedTrack, PersistenceAndOrphanScannerStressTest, RoomDownloadedTrackEntityTest

### Community 22 - "UserSettingsRepository"
Cohesion: 0.09
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepository

### Community 24 - "AudioMetadataTagger"
Cohesion: 0.13
Nodes (3): AudioMetadataTagger, ExtractedMetadata, AudioMetadataTaggerTest

### Community 25 - "JamSessionSheet.kt"
Cohesion: 0.06
Nodes (3): JamSessionSheet(), JamSettingToggleRow(), JamVisualMatrixBadge()

### Community 28 - "LoudnessNormalizationAudioProcessor"
Cohesion: 0.09
Nodes (3): Biquad, LoudnessNormalizationAudioProcessor, LoudnessNormalizationAudioProcessorTest

### Community 29 - "OnboardingScreen.kt"
Cohesion: 0.06
Nodes (12): applePressFeedback(), AppleArtistSelectionStep(), AppleArtistSphereCard(), AppleGlassFeatureCard(), AppleWelcomeStep(), OnboardingScreen(), artistListeningShares(), ArtistMosaicTile() (+4 more)

### Community 32 - "MigrationScreen.kt"
Cohesion: 0.17
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 34 - "SmartDownloadWorker.kt"
Cohesion: 0.05
Nodes (6): MainActivity, NERFMusicTheme(), UpdateCheckWorker, UpdateNotificationHelper, SmartDownloadNotificationHelper, SmartDownloadWorker

### Community 38 - "HomeSectionStyle"
Cohesion: 0.12
Nodes (10): HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL (+2 more)

### Community 39 - "FullPlayerSheet"
Cohesion: 0.07
Nodes (24): PlaybackState, KaraokeSyncSnapshot, JamPersonWavesIcon(), AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar(), ApplePopoverOptionRow(), formatDurationMs() (+16 more)

### Community 40 - "SharePreviewActivity.kt"
Cohesion: 0.05
Nodes (7): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar()

### Community 41 - "UserHistoryRepositoryImpl"
Cohesion: 0.09
Nodes (4): UserAffinityRepositoryImpl, OnboardingFlowTest, ContextWrapper, InvocationHandler

### Community 43 - ".update"
Cohesion: 0.07
Nodes (8): CompactPlayerWidgetProvider, ExpandedPlayerWidgetProvider, ThreeByOneWidgetProvider, Size, FOUR, THREE, TWO, WidgetRenderer

### Community 44 - "UserSettingsRepositoryImpl"
Cohesion: 0.07
Nodes (8): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, UserSettingsRepositoryImpl, StreamResolver

### Community 45 - "LyricsData"
Cohesion: 0.33
Nodes (5): LyricsData, SyncedLyricsLine, SyncedLyricsWord, inlineLyricSegmentIndex(), InlineSyncedLyrics()

### Community 51 - "HomeScreen.kt"
Cohesion: 0.06
Nodes (8): ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow(), ArtistVideoCard(), TrackOptionRow(), TrackOptionsBottomSheet()

### Community 56 - "NERF Music"
Cohesion: 0.22
Nodes (8): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão

### Community 59 - "WidgetArtworkCacheInstrumentedTest.kt"
Cohesion: 0.21
Nodes (3): WidgetArtworkCacheInstrumentedTest, Cover, WidgetArtworkCache

### Community 60 - "SettingsSubScreen"
Cohesion: 0.20
Nodes (10): SearchableSetting, SettingsSubScreen, ABOUT, CONTENT_DISPLAY, DATA_SAVER, MAIN, MEDIA_QUALITY, MIGRATION (+2 more)

### Community 61 - "MimeAndContainerDetectionStressTest.kt"
Cohesion: 0.11
Nodes (4): StreamResolver, MimeAndContainerDetectionStressTest, ContextWrapper, InvocationHandler

### Community 68 - "AudioCoreDownloadsCrossfadeQualityTest.kt"
Cohesion: 0.11
Nodes (4): AudioCoreDownloadsCrossfadeQualityTest, ContextWrapper, StreamResolver, StreamResolver

### Community 74 - "DownloadAudioQuality"
Cohesion: 0.13
Nodes (7): SmartDownloadManager, SmartDownloadSyncState, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 80 - "log"
Cohesion: 0.05
Nodes (17): AnnualTrackStatsEntity, AppDatabase, AnnualTrackStatsWithTrack, LibraryTrackDao, LibraryTrackEntity, BackupPreferenceEntry, BackupRepository, BackupRestoreMode (+9 more)

### Community 81 - "JamGlobalRelayClient"
Cohesion: 0.11
Nodes (3): JamGlobalRelayClient, WebSocketListener, NetworkCallback

### Community 83 - "SocialShareHelper"
Cohesion: 0.26
Nodes (6): PaletteColors, SocialShareHelper, Style, FULL_COVER, GLASS, MINIMAL

### Community 84 - ".extract"
Cohesion: 0.27
Nodes (3): ArtworkColorsInstrumentedTest, ArtworkColors, Colors

### Community 85 - "InnerTubeStreamResolver"
Cohesion: 0.10
Nodes (5): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest, StreamQualitySelectorStressTest

### Community 86 - "LoudnessStandard"
Cohesion: 0.17
Nodes (7): LoudnessStandard, APPLE_MUSIC, BROADCAST_EBU, CD_LOUD, CD_STANDARD, OFF, SPOTIFY

### Community 87 - "AudioQuality"
Cohesion: 0.16
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 88 - "SearchScreen"
Cohesion: 0.67
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 90 - "RadioTuningMode"
Cohesion: 0.33
Nodes (5): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR

### Community 91 - "SharePreviewSheet"
Cohesion: 0.33
Nodes (7): CircleAction(), DividerLine(), SectionLabel(), ShareAction(), ShareActionModern(), SharePreviewSheet(), StyleOption()

### Community 96 - "Artist"
Cohesion: 0.12
Nodes (9): AlbumDetail, Artist, ArtistDetail, SearchFilter, ALBUMS, ALL, ARTISTS, SONGS (+1 more)

### Community 103 - "DownloadOutcome"
Cohesion: 0.40
Nodes (5): DownloadOutcome, ALLOWED, BLOCKED_ALWAYS_ON, BLOCKED_AUTOMATIC_DATA_SAVER, BLOCKED_CELLULAR_DISABLED

### Community 106 - "ExploreScreen.kt"
Cohesion: 0.07
Nodes (6): AnimatedEqualizerBars(), AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow(), ExploreMotionItem(), YtmExploreQuickButton()

### Community 108 - "NERF Music — widget e prévia de compartilhamento"
Cohesion: 0.40
Nodes (4): NERF Music — widget e prévia de compartilhamento, Prévia de compartilhamento, Revisão visual, Widget expandido

### Community 110 - "NERF Music 1.5.0"
Cohesion: 0.33
Nodes (5): Adicionado, Alterado e corrigido, NERF Music 1.5.0, Notas técnicas da versão, Seu ano com música está aqui 🎧

### Community 114 - "LibrarySortOption"
Cohesion: 0.50
Nodes (4): LibrarySortOption, A_TO_Z, RECENT, RECENTLY_ADDED

## Knowledge Gaps
- **96 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+91 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 576 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **57 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Track` connect `Track` to `DownloadChunkRangeStressTest.kt`, `PlayerScreen.kt`, `RecapScreen.kt`, `MigrationManager`, `MusicViewModel`, `InnerTubeCatalogClientTest`, `JamSessionManager`, `SocialShareHelper.kt`, `UpNextResult`, `HomeScreen.kt`, `.syncTimelineState`, `KaraokeLyricsEngine`, `LibraryScreen`, `DownloadedTrackEntity`, `.rank`, `AudioMetadataTagger`, `.isConnectedToWifi`, `LibraryRepository.kt`, `UserHistoryRepositoryImpl`, `HomeSectionStyle`, `FullPlayerSheet`, `SharePreviewActivity.kt`, `UserHistoryRepositoryImpl`, `.update`, `MusicViewModel.kt`, `HomeScreen.kt`, `SearchResult`, `InnerTubeCatalogClient.kt`, `HomeSection`, `.selectDiscoveryTracks`, `.loadLyrics`, `DownloadAudioQuality`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `InnerTubeCatalogClient`, `AudioPlayerService.kt`, `log`, `SocialShareHelper`, `SearchScreen`, `BackupPreviewState`, `RadioTuningMode`, `SharePreviewSheet`, `.updateMediaNotificationLayout`, `Artist`, `TrackRelatedData`, `ShareCardInstrumentedTest.kt`, `Track`, `.playTrackList`, `.testRelatedMultiTierSynthesisAlwaysPopulatesContent`, `ExploreScreen.kt`?**
  _High betweenness centrality (0.235) - this node is a cross-community bridge._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _96 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DownloadChunkRangeStressTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.10846560846560846 - nodes in this community are weakly interconnected._
- **Why does `MusicViewModel` connect `MusicViewModel` to `RecapScreen.kt`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `JamSessionManager`, `HomeScreen.kt`, `.syncTimelineState`, `ConnectivityObserver`, `KaraokeLyricsEngine`, `LibraryScreen`, `Track`, `SpotifyCanvasRepositoryImpl`, `DownloadedTrackEntity`, `UserSettingsRepository`, `.isConnectedToWifi`, `MigrationScreen.kt`, `LibraryRepository.kt`, `SmartDownloadWorker.kt`, `MusicRepository`, `HomeSectionStyle`, `FullPlayerSheet`, `SharePreviewActivity.kt`, `UserSettingsRepositoryImpl`, `LyricsData`, `MusicViewModel.kt`, `.loadHomeFeed`, `.updateCurrentLyricsLine`, `HomeScreen.kt`, `StorageUtils`, `HomeSection`, `NewPipeStreamResolver.kt`, `.clearStreamCache`, `.loadLyrics`, `locale`, `DownloadAudioQuality`, `AudioPlayerService.kt`, `.applyKaraokeState`, `LoudnessStandard`, `SearchScreen`, `BackupPreviewState`, `RadioTuningMode`, `Artist`, `TrackRelatedData`, `Track`, `.playTrackList`, `ExploreScreen.kt`?**
  _High betweenness centrality (0.229) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.028481012658227847 - nodes in this community are weakly interconnected._
- **Why does `AudioPlayerService` connect `AudioPlayerService` to `.clearStreamCache`, `.loadLyrics`, `.onCreate`, `.playWidgetMix`, `JamSessionManager`, `SocialShareHelper.kt`, `.update`, `VocalIsolationAudioProcessor`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `AudioPlayerService.kt`, `.applyKaraokeState`, `MusicViewModel.kt`, `Track`, `UserSettingsRepository`, `LoudnessStandard`, `Context`, `.updateMediaNotificationLayout`, `Listener`?**
  _High betweenness centrality (0.066) - this node is a cross-community bridge._
- **Should `RecapScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.13781512605042018 - nodes in this community are weakly interconnected._