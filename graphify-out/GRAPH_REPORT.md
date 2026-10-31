# Graph Report - Nerf Music  (2026-10-30)

## Corpus Check
- 140 files · ~161,820 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 59 file(s) not represented in the graph (top: .xml 44, .otf 5, (none) 2)

## Summary
- 2624 nodes · 6640 edges · 130 communities (56 shown, 74 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 71 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d552c272`
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
- .parseResponsiveListItem
- HomeScreen.kt
- .syncTimelineState
- LyricsData
- KaraokeLyricsEngine
- DownloadedTrackDao
- LibraryScreen
- UserPlaylist
- SpotifyCanvasRepositoryImpl
- PersistenceAndOrphanScannerStressTest
- UserSettingsRepository
- .rank
- AudioMetadataTagger
- JamSessionSheet.kt
- LibraryRepositoryImpl
- Block3EmpiricalChallengeTest
- LoudnessNormalizationAudioProcessor
- OnboardingScreen.kt
- DownloadedTrack
- RecapVideoExporter.kt
- MigrationScreen.kt
- ExploreScreen
- SmartDownloadWorker.kt
- DownloadChunkRangeStressTest.kt
- MusicRepository
- UserHistoryRepositoryImpl
- LibraryTrackDao
- FullPlayerSheet
- SharePreviewActivity.kt
- UserAffinityRepositoryImpl
- HomeSectionStyle
- .update
- UserSettingsRepositoryImpl
- LyricsNullSanitizationStressTest
- AudioPlayerService
- MigrationManager.kt
- .loadHomeFeed
- LibraryTrackEntity
- SearchScreen.kt
- InnerTubeParserAdversarialStressTest
- SearchResult
- InnerTubeCatalogClient.kt
- StorageUtils
- NERF Music
- HomeSection
- NewPipeStreamResolver.kt
- ShareCardInstrumentedTest.kt
- SettingsSubScreen
- AnimatedEqualizerBars
- AppDatabase
- MainActivity.kt
- .`JamWebSocketServer performs RFC6455 handshake and bidirectional message exchange`
- AudioCoreDownloadsCrossfadeQualityTest.kt
- DataSaverInterceptor.kt
- MusicViewModel.kt
- DownloadAudioQuality
- AudioCoreDownloadsCrossfadeQualityTest.kt
- JamWebSocketServer.kt
- .applyKaraokeState
- BackupRepository
- JamGlobalRelayClient
- ShareRules
- SocialShareHelper
- .extract
- InnerTubeStreamResolver
- LoudnessStandard
- AudioQuality
- SearchScreen
- BackupRepository.kt
- RadioTuningMode
- JamPersonWavesIcon
- Context
- .onCreate
- LoudnessNormalizationAudioProcessorTest
- Listener
- MainScreen
- TrackRelatedData
- Artist
- Track
- KaraokeAndJamTest.kt
- OnboardingFlowTest.kt
- LibrarySortOption
- NERF Music — widget e prévia de compartilhamento
- .syncNow
- NERF Music 1.5.0
- CrossfadeEmpiricalTest
- SearchFilter
- .updateMediaNotificationLayout
- StreamResolver
- BiquadFilter
- UpdateNotificationHelper
- SmartDownloadWorker
- Test
- LibraryFilter
- NERFMusicTheme
- RecapArtistCredits.kt
- AlbumScreen
- PlaylistOptionsBottomSheet
- AnnualTrackStatsEntity
- Style
- StreamResolver

## God Nodes (most connected - your core abstractions)
1. `MusicViewModel` - 232 edges
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

## Communities (130 total, 74 thin omitted)

### Community 0 - "MimeAndContainerDetectionStressTest.kt"
Cohesion: 0.12
Nodes (4): MimeAndContainerDetectionStressTest, ContextWrapper, InvocationHandler, StreamResolver

### Community 1 - "PlayerScreen.kt"
Cohesion: 0.03
Nodes (3): NestedScrollConnection, largestFittingCharacterBoundary(), splitInlineLyricIntoSegments()

### Community 2 - "RecapScreen.kt"
Cohesion: 0.11
Nodes (18): RecapArtistStats, RecapSummary, RecapTrackStats, ArtistRow(), EmptyRecap(), formatRecapPlayCount(), formattedRecapDuration(), LegacyRecapStoryPage() (+10 more)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (4): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.23
Nodes (23): AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), LoudnessNormalizationBottomSheet(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), RecommendationsSettingsScreen() (+15 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 8 - "JamSessionManager"
Cohesion: 0.06
Nodes (35): JamCommandType, ADD_TO_QUEUE, KICK_PARTICIPANT, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK (+27 more)

### Community 10 - "steps"
Cohesion: 0.05
Nodes (39): if, runs-on, steps, GH_REPO, GH_TOKEN, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64 (+31 more)

### Community 13 - "HomeScreen.kt"
Cohesion: 0.20
Nodes (14): HomeScreen(), HomeSongCard(), HomeSpeedDialPlaybackSection(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel() (+6 more)

### Community 15 - "LyricsData"
Cohesion: 0.15
Nodes (6): LyricsData, SyncedLyricsLine, SyncedLyricsWord, LyricsRepository, LyricsRepositoryImpl, optCleanString()

### Community 16 - "KaraokeLyricsEngine"
Cohesion: 0.14
Nodes (4): BatchTranslationResult, KaraokeLyricsEngine, TranslationLanguageOption, KaraokeAndJamTest

### Community 18 - "LibraryScreen"
Cohesion: 0.26
Nodes (9): LibraryGridCard(), LibraryListItem(), LibraryScreen(), NewBottomSheetOption(), PlaylistCollageCover(), TrackListDetailView(), UserPlaylistDetailView(), SaveToPlaylistSheet() (+1 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.17
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 22 - "UserSettingsRepository"
Cohesion: 0.09
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepository

### Community 23 - ".rank"
Cohesion: 0.13
Nodes (4): ExploreRecommendations, ReleaseRecommendations, ExploreRecommendationsTest, ReleaseRecommendationsTest

### Community 24 - "AudioMetadataTagger"
Cohesion: 0.13
Nodes (3): AudioMetadataTagger, ExtractedMetadata, AudioMetadataTaggerTest

### Community 25 - "JamSessionSheet.kt"
Cohesion: 0.08
Nodes (3): JamSessionSheet(), JamSettingToggleRow(), JamVisualMatrixBadge()

### Community 27 - "Block3EmpiricalChallengeTest"
Cohesion: 0.07
Nodes (19): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+11 more)

### Community 29 - "OnboardingScreen.kt"
Cohesion: 0.04
Nodes (12): applePressFeedback(), AppleArtistSelectionStep(), AppleArtistSphereCard(), AppleGlassFeatureCard(), AppleWelcomeStep(), OnboardingScreen(), artistListeningShares(), ArtistMosaicTile() (+4 more)

### Community 32 - "MigrationScreen.kt"
Cohesion: 0.11
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 33 - "ExploreScreen"
Cohesion: 0.31
Nodes (5): rememberSmoothFlingBehavior(), FlingBehavior, ExploreMotionItem(), ExploreScreen(), YtmExploreQuickButton()

### Community 35 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.11
Nodes (3): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler

### Community 39 - "FullPlayerSheet"
Cohesion: 0.08
Nodes (25): PlaybackState, KaraokeSyncSnapshot, AppleLyricsOptionsPopover(), AppleMusicLyricsView(), AppleMusicVolumeBar(), ApplePopoverOptionRow(), formatDurationMs(), FullPlayerSheet() (+17 more)

### Community 40 - "SharePreviewActivity.kt"
Cohesion: 0.07
Nodes (7): CircleAction(), DividerLine(), SectionLabel(), ShareAction(), ShareActionModern(), SharePreviewSheet(), StyleOption()

### Community 42 - "HomeSectionStyle"
Cohesion: 0.13
Nodes (11): HomeChip, HomeFeedData, HomeSection, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL (+3 more)

### Community 43 - ".update"
Cohesion: 0.07
Nodes (8): CompactPlayerWidgetProvider, ExpandedPlayerWidgetProvider, ThreeByOneWidgetProvider, Size, FOUR, THREE, TWO, WidgetRenderer

### Community 44 - "UserSettingsRepositoryImpl"
Cohesion: 0.08
Nodes (7): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH, UserSettingsRepositoryImpl

### Community 56 - "NERF Music"
Cohesion: 0.22
Nodes (8): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão

### Community 58 - "NewPipeStreamResolver.kt"
Cohesion: 0.12
Nodes (4): NerfMusicApp, NewPipeStreamResolver, OkHttpDownloader, NewPipeStreamResolverTest

### Community 60 - "SettingsSubScreen"
Cohesion: 0.18
Nodes (11): SearchableSetting, SettingsSubScreen, ABOUT, CONTENT_DISPLAY, DATA_SAVER, MAIN, MEDIA_QUALITY, MIGRATION (+3 more)

### Community 62 - "AnimatedEqualizerBars"
Cohesion: 0.29
Nodes (7): AnimatedEqualizerBars(), ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow(), ArtistVideoCard()

### Community 68 - "AudioCoreDownloadsCrossfadeQualityTest.kt"
Cohesion: 0.20
Nodes (3): AudioCoreDownloadsCrossfadeQualityTest, StreamResolver, StreamResolver

### Community 74 - "DownloadAudioQuality"
Cohesion: 0.13
Nodes (7): SmartDownloadManager, SmartDownloadSyncState, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 80 - "BackupRepository"
Cohesion: 0.21
Nodes (4): BackupPreferenceEntry, BackupRepository, BackupRestoreSummary, NerfMusicBackup

### Community 81 - "JamGlobalRelayClient"
Cohesion: 0.07
Nodes (4): JamGlobalRelayClient, WebSocketListener, NetworkConnectivityObserver, NetworkCallback

### Community 83 - "SocialShareHelper"
Cohesion: 0.31
Nodes (3): RecapScreen(), PaletteColors, SocialShareHelper

### Community 84 - ".extract"
Cohesion: 0.23
Nodes (3): ArtworkColorsInstrumentedTest, ArtworkColors, Colors

### Community 85 - "InnerTubeStreamResolver"
Cohesion: 0.10
Nodes (5): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest, StreamQualitySelectorStressTest

### Community 86 - "LoudnessStandard"
Cohesion: 0.17
Nodes (7): LoudnessStandard, APPLE_MUSIC, BROADCAST_EBU, CD_LOUD, CD_STANDARD, OFF, SPOTIFY

### Community 87 - "AudioQuality"
Cohesion: 0.15
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 88 - "SearchScreen"
Cohesion: 0.67
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 89 - "BackupRepository.kt"
Cohesion: 0.19
Nodes (5): BackupPreview, BackupPreviewState, BackupRestoreMode, MERGE, REPLACE

### Community 90 - "RadioTuningMode"
Cohesion: 0.33
Nodes (5): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR

### Community 96 - "MainScreen"
Cohesion: 0.24
Nodes (7): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar()

### Community 98 - "Artist"
Cohesion: 0.13
Nodes (6): AlbumDetail, Artist, ArtistDetail, SearchResult, QueueFilterChip, UpNextResult

### Community 99 - "Track"
Cohesion: 0.11
Nodes (3): Track, UserAffinityRepository, SmartRadioEngine

### Community 105 - "OnboardingFlowTest.kt"
Cohesion: 0.15
Nodes (3): OnboardingFlowTest, ContextWrapper, InvocationHandler

### Community 106 - "LibrarySortOption"
Cohesion: 0.50
Nodes (4): LibrarySortOption, A_TO_Z, RECENT, RECENTLY_ADDED

### Community 108 - "NERF Music — widget e prévia de compartilhamento"
Cohesion: 0.40
Nodes (4): NERF Music — widget e prévia de compartilhamento, Prévia de compartilhamento, Revisão visual, Widget expandido

### Community 110 - "NERF Music 1.5.0"
Cohesion: 0.33
Nodes (5): Adicionado, Alterado e corrigido, NERF Music 1.5.0, Notas técnicas da versão, Seu ano com música está aqui 🎧

### Community 113 - "SearchFilter"
Cohesion: 0.29
Nodes (5): SearchFilter, ALBUMS, ALL, ARTISTS, SONGS

### Community 120 - "LibraryFilter"
Cohesion: 0.29
Nodes (7): LibraryFilter, ALBUMS, ALL, ARTISTS, DOWNLOADS, PLAYLISTS, SONGS

### Community 123 - "RecapArtistCredits.kt"
Cohesion: 0.53
Nodes (5): normalizeRecapArtistName(), RecapArtistContribution, RecapArtistCredit, splitNames(), splitRecapArtistCredit()

### Community 124 - "AlbumScreen"
Cohesion: 0.80
Nodes (3): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow()

### Community 128 - "Style"
Cohesion: 0.50
Nodes (4): Style, FULL_COVER, GLASS, MINIMAL

## Knowledge Gaps
- **97 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+92 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 580 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **74 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Track` connect `Track` to `PlayerScreen.kt`, `RecapScreen.kt`, `MigrationManager`, `MusicViewModel`, `InnerTubeCatalogClientTest`, `JamSessionManager`, `SocialShareHelper.kt`, `.parseResponsiveListItem`, `HomeScreen.kt`, `.syncTimelineState`, `LyricsData`, `KaraokeLyricsEngine`, `DownloadedTrackDao`, `LibraryScreen`, `UserPlaylist`, `PersistenceAndOrphanScannerStressTest`, `.rank`, `AudioMetadataTagger`, `LibraryRepositoryImpl`, `DownloadedTrack`, `DownloadChunkRangeStressTest.kt`, `UserHistoryRepositoryImpl`, `FullPlayerSheet`, `SharePreviewActivity.kt`, `UserAffinityRepositoryImpl`, `HomeSectionStyle`, `.update`, `.parseResponsiveListItem`, `MigrationManager.kt`, `LibraryTrackEntity`, `SearchScreen.kt`, `SearchResult`, `InnerTubeCatalogClient.kt`, `HomeSection`, `ShareCardInstrumentedTest.kt`, `AnimatedEqualizerBars`, `.loadLyrics`, `MusicViewModel.kt`, `DownloadAudioQuality`, `.initializePlayerController`, `AudioPlayerService.kt`, `BackupRepository`, `SocialShareHelper`, `SearchScreen`, `BackupRepository.kt`, `RadioTuningMode`, `TrackRelatedData`, `Artist`, `.playWidgetMix`, `.playTrackList`, `KaraokeAndJamTest.kt`, `OnboardingFlowTest.kt`, `.updateMediaNotificationLayout`, `AlbumScreen`?**
  _High betweenness centrality (0.262) - this node is a cross-community bridge._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _97 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `MimeAndContainerDetectionStressTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.11954022988505747 - nodes in this community are weakly interconnected._
- **Why does `MusicViewModel` connect `MusicViewModel` to `RecapScreen.kt`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `JamSessionManager`, `HomeScreen.kt`, `.syncTimelineState`, `LyricsData`, `KaraokeLyricsEngine`, `LibraryScreen`, `UserPlaylist`, `SpotifyCanvasRepositoryImpl`, `UserSettingsRepository`, `.rank`, `Block3EmpiricalChallengeTest`, `OnboardingScreen.kt`, `DownloadedTrack`, `MigrationScreen.kt`, `ExploreScreen`, `MusicRepository`, `FullPlayerSheet`, `SharePreviewActivity.kt`, `HomeSectionStyle`, `UserSettingsRepositoryImpl`, `AudioPlayerService`, `.loadHomeFeed`, `SearchScreen.kt`, `InnerTubeCatalogClient.kt`, `StorageUtils`, `HomeSection`, `NewPipeStreamResolver.kt`, `.clearStreamCache`, `AnimatedEqualizerBars`, `MainActivity.kt`, `.loadLyrics`, `MusicViewModel.kt`, `DownloadAudioQuality`, `.initializePlayerController`, `AudioPlayerService.kt`, `LoudnessStandard`, `SearchScreen`, `BackupRepository.kt`, `RadioTuningMode`, `MainScreen`, `TrackRelatedData`, `Artist`, `Track`, `.updateCurrentLyricsLine`, `.playTrackList`, `.setKaraokeMode`, `SearchFilter`, `UpdateNotificationHelper`, `SmartDownloadWorker`, `AlbumScreen`?**
  _High betweenness centrality (0.207) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.029304029304029304 - nodes in this community are weakly interconnected._
- **Why does `AudioPlayerService` connect `AudioPlayerService` to `JamSessionManager`, `VocalIsolationAudioProcessor`, `UserSettingsRepository`, `LibraryRepositoryImpl`, `.update`, `InnerTubeCatalogClient.kt`, `.clearStreamCache`, `.loadLyrics`, `MusicViewModel.kt`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `AudioPlayerService.kt`, `.applyKaraokeState`, `LoudnessStandard`, `AudioQuality`, `Context`, `Listener`, `.playWidgetMix`, `.setKaraokeMode`, `.updateMediaNotificationLayout`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Should `RecapScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.10931174089068826 - nodes in this community are weakly interconnected._