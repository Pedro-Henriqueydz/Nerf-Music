# Graph Report - Nerf Music  (2026-10-31)

## Corpus Check
- 141 files · ~161,903 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 59 file(s) not represented in the graph (top: .xml 44, .otf 5, (none) 2)

## Summary
- 2630 nodes · 6645 edges · 121 communities (58 shown, 63 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 71 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d7d78e59`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DownloadChunkRangeStressTest.kt
- PlayerScreen.kt
- RecapStoryPage
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
- .rank
- LibraryScreen
- UserPlaylist
- SpotifyCanvasRepositoryImpl
- DownloadedTrackEntity
- LoudnessStandard
- UserAffinityRepositoryImpl
- AudioMetadataTagger
- LibraryRepositoryImpl
- Block3EmpiricalChallengeTest
- LoudnessNormalizationAudioProcessor
- OnboardingScreen.kt
- RecapScreen.kt
- RecapVideoExporter.kt
- MigrationScreen.kt
- JamWireMessage
- JamCommandType
- AudioMetadataTagger
- MusicRepository
- AlbumDetail
- ConnectivityObserver
- FullPlayerSheet
- LyricsData
- HomeSectionStyle
- .update
- AudioQuality
- LyricsNullSanitizationStressTest
- SharePreviewSheet
- AudioPlayerService
- .loadHomeFeed
- .prefetchTrack
- LibraryScreen.kt
- InnerTubeParserAdversarialStressTest
- SearchResult
- WebSocket
- StorageUtils
- NERF Music
- HomeSection
- NewPipeStreamResolver.kt
- ShareCardInstrumentedTest.kt
- SettingsSubScreen
- AudioQuality
- ArtistScreen.kt
- SmoothFlingBehavior.kt
- MainScreen
- PlayerUiUtilsTest.kt
- AudioCoreDownloadsCrossfadeQualityTest.kt
- LibraryRepository.kt
- MusicViewModel.kt
- LibraryFilter
- AudioCoreDownloadsCrossfadeQualityTest.kt
- SearchScreen
- JamWebSocketServer.kt
- .applyKaraokeState
- LibraryTrackEntity
- JamGlobalRelayClient
- ShareRules
- SocialShareHelper
- .extract
- InnerTubeStreamResolver
- Test
- AudioQuality
- .onCreate
- NERF Music 1.5.1
- RadioTuningMode
- StreamResolver
- Context
- .onCreate
- PlaylistOptionsBottomSheet
- Listener
- MainScreen
- Artist
- UserAffinityRepository
- .isConnectedToWifi
- TrackRelatedData
- UserAffinityRepositoryImpl
- LibrarySortOption
- NERF Music — widget e prévia de compartilhamento
- .syncNow
- NERF Music 1.5.0
- LoudnessNormalizationAudioProcessorTest.kt
- SearchFilter
- .onCreate
- StreamResolver
- BiquadFilter
- UpdateNotificationHelper
- SmartDownloadWorker
- Test
- LibraryFilter
- ExploreScreen.kt
- AlbumScreen
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

## Communities (121 total, 63 thin omitted)

### Community 0 - "DownloadChunkRangeStressTest.kt"
Cohesion: 0.13
Nodes (3): MimeAndContainerDetectionStressTest, ContextWrapper, InvocationHandler

### Community 1 - "PlayerScreen.kt"
Cohesion: 0.03
Nodes (3): NestedScrollConnection, largestFittingCharacterBoundary(), splitInlineLyricIntoSegments()

### Community 2 - "RecapStoryPage"
Cohesion: 0.13
Nodes (19): RecapArtistStats, RecapSummary, RecapTrackStats, ArtistRow(), EmptyRecap(), formatRecapPlayCount(), formattedRecapDuration(), LegacyRecapStoryPage() (+11 more)

### Community 3 - "MigrationManager"
Cohesion: 0.07
Nodes (4): MigrationManager, PlaylistSummary, RawImportedItem, SecureStorageHelper

### Community 4 - "SettingsScreen.kt"
Cohesion: 0.21
Nodes (23): AboutSettingsScreen(), ContentDisplaySettingsScreen(), DataSaverSettingsScreen(), LoudnessNormalizationBottomSheet(), MediaQualitySettingsScreen(), PlaybackSettingsScreen(), QuickAccessCard(), RecommendationsSettingsScreen() (+15 more)

### Community 5 - "AppUpdateManager.kt"
Cohesion: 0.08
Nodes (7): SemanticVersion, UpdateRepository, UpdateRepositoryImpl, AppUpdateInfo, AppUpdateManager, BroadcastReceiver, AppUpdateManagerTest

### Community 8 - "JamSessionManager"
Cohesion: 0.05
Nodes (36): JamCommandType, ADD_TO_QUEUE, KICK_PARTICIPANT, MOVE_QUEUE_ITEM, PAUSE, PLAY, PLAY_NEXT_IN_QUEUE, PLAY_TRACK (+28 more)

### Community 10 - "steps"
Cohesion: 0.05
Nodes (39): if, runs-on, steps, GH_REPO, GH_TOKEN, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_KEYSTORE_BASE64 (+31 more)

### Community 13 - "HomeScreen.kt"
Cohesion: 0.32
Nodes (14): HomeScreen(), HomeSongCard(), HomeSpeedDialPlaybackSection(), YtmDailyDiscoverHeroSection(), YtmDownloadCard(), YtmMoodChipsRow(), YtmQuickPickRowItem(), YtmQuickPicksGridCarousel() (+6 more)

### Community 15 - "LyricsData"
Cohesion: 0.19
Nodes (3): LyricsRepository, LyricsRepositoryImpl, optCleanString()

### Community 16 - "KaraokeLyricsEngine"
Cohesion: 0.14
Nodes (4): BatchTranslationResult, KaraokeLyricsEngine, TranslationLanguageOption, KaraokeAndJamTest

### Community 18 - "LibraryScreen"
Cohesion: 0.17
Nodes (11): LibraryGridCard(), LibraryListItem(), LibraryScreen(), NewBottomSheetOption(), PlaylistCollageCover(), TrackListDetailView(), UserPlaylistDetailView(), PlaylistOptionItem() (+3 more)

### Community 20 - "SpotifyCanvasRepositoryImpl"
Cohesion: 0.19
Nodes (6): CanvasStatus, NotFound, ServerError, SpotifyCanvasRepository, SpotifyCanvasRepositoryImpl, Success

### Community 23 - "UserAffinityRepositoryImpl"
Cohesion: 0.10
Nodes (6): ExploreRecommendations, RecapCalendar, RecapCalendarState, ReleaseRecommendations, ExploreRecommendationsTest, ReleaseRecommendationsTest

### Community 24 - "AudioMetadataTagger"
Cohesion: 0.13
Nodes (3): AudioMetadataTagger, ExtractedMetadata, AudioMetadataTaggerTest

### Community 27 - "Block3EmpiricalChallengeTest"
Cohesion: 0.14
Nodes (6): Block3EmpiricalChallengeTest, DownloadOutcome, ALLOWED, BLOCKED_ALWAYS_ON, BLOCKED_AUTOMATIC_DATA_SAVER, BLOCKED_CELLULAR_DISABLED

### Community 29 - "OnboardingScreen.kt"
Cohesion: 0.04
Nodes (13): AnimatedEqualizerBars(), applePressFeedback(), AppleArtistSelectionStep(), AppleArtistSphereCard(), AppleGlassFeatureCard(), AppleWelcomeStep(), OnboardingScreen(), artistListeningShares() (+5 more)

### Community 32 - "MigrationScreen.kt"
Cohesion: 0.11
Nodes (7): AnonymousFileCard(), AnonymousLinkCard(), ConnectedServiceCard(), MigrationScreen(), WebViewClient, PrivacyBadgeCard(), SectionHeader()

### Community 33 - "JamWireMessage"
Cohesion: 0.53
Nodes (3): ExploreMotionItem(), ExploreScreen(), YtmExploreQuickButton()

### Community 35 - "AudioMetadataTagger"
Cohesion: 0.11
Nodes (3): DownloadChunkRangeStressTest, ContextWrapper, InvocationHandler

### Community 38 - "ConnectivityObserver"
Cohesion: 0.08
Nodes (14): QueueExpansionState, COLLAPSED, EXPANDED, HALF_EXPANDED, ConnectivityObserver, Status, Available, Losing (+6 more)

### Community 39 - "FullPlayerSheet"
Cohesion: 0.07
Nodes (25): PlaybackState, KaraokeSyncSnapshot, JamPersonWavesIcon(), JamSessionSheet(), JamSettingToggleRow(), JamVisualMatrixBadge(), AppleLyricsOptionsPopover(), AppleMusicLyricsView() (+17 more)

### Community 41 - "LyricsData"
Cohesion: 0.32
Nodes (5): LyricsData, SyncedLyricsLine, SyncedLyricsWord, inlineLyricSegmentIndex(), InlineSyncedLyrics()

### Community 42 - "HomeSectionStyle"
Cohesion: 0.21
Nodes (9): HomeChip, HomeFeedData, HomeSectionStyle, COMMUNITY_PLAYLIST, DAILY_DISCOVER, QUICK_PICKS_GRID, SPEED_DIAL, TWO_ROW_CAROUSEL (+1 more)

### Community 43 - ".update"
Cohesion: 0.07
Nodes (8): CompactPlayerWidgetProvider, ExpandedPlayerWidgetProvider, ThreeByOneWidgetProvider, Size, FOUR, THREE, TWO, WidgetRenderer

### Community 44 - "AudioQuality"
Cohesion: 0.09
Nodes (5): DataSaverMode, ALWAYS_OFF, ALWAYS_ON, AUTOMATIC, UserSettingsRepositoryImpl

### Community 46 - "SharePreviewSheet"
Cohesion: 0.33
Nodes (7): CircleAction(), DividerLine(), SectionLabel(), ShareAction(), ShareActionModern(), SharePreviewSheet(), StyleOption()

### Community 56 - "NERF Music"
Cohesion: 0.22
Nodes (8): Compilar e instalar, NERF Music, Organização do código, Recursos, Requisitos, Tecnologias, Testes, Versão

### Community 58 - "NewPipeStreamResolver.kt"
Cohesion: 0.10
Nodes (4): NerfMusicApp, NewPipeStreamResolver, OkHttpDownloader, NewPipeStreamResolverTest

### Community 60 - "SettingsSubScreen"
Cohesion: 0.18
Nodes (11): SearchableSetting, SettingsSubScreen, ABOUT, CONTENT_DISPLAY, DATA_SAVER, MAIN, MEDIA_QUALITY, MIGRATION (+3 more)

### Community 61 - "AudioQuality"
Cohesion: 0.12
Nodes (6): AudioQuality, AUTOMATIC, HIGH, LOW, NORMAL, VERY_HIGH

### Community 62 - "ArtistScreen.kt"
Cohesion: 0.61
Nodes (6): ArtistAboutCard(), ArtistCircularCard(), ArtistHeroHeader(), ArtistScreen(), ArtistTopSongRow(), ArtistVideoCard()

### Community 68 - "AudioCoreDownloadsCrossfadeQualityTest.kt"
Cohesion: 0.20
Nodes (3): AudioCoreDownloadsCrossfadeQualityTest, StreamResolver, StreamResolver

### Community 74 - "LibraryFilter"
Cohesion: 0.15
Nodes (7): SmartDownloadManager, SmartDownloadSyncState, DownloadAudioQuality, HIGH, LOSSLESS, NORMAL, VERY_HIGH

### Community 75 - "AudioCoreDownloadsCrossfadeQualityTest.kt"
Cohesion: 0.09
Nodes (3): ContextWrapper, InvocationHandler, WebSocketListener

### Community 76 - "SearchScreen"
Cohesion: 0.18
Nodes (5): normalizeRecapArtistName(), RecapArtistContribution, RecapArtistCredit, splitNames(), splitRecapArtistCredit()

### Community 77 - "JamWebSocketServer.kt"
Cohesion: 0.08
Nodes (5): ImportedPlaylistIdentity, ClientConnection, JamWebSocketServer, WsFrame, ImportedPlaylistIdentityTest

### Community 80 - "LibraryTrackEntity"
Cohesion: 0.05
Nodes (14): AnnualTrackStatsEntity, AppDatabase, AnnualTrackStatsWithTrack, LibraryTrackDao, LibraryTrackEntity, BackupPreferenceEntry, BackupPreview, BackupPreviewState (+6 more)

### Community 81 - "JamGlobalRelayClient"
Cohesion: 0.10
Nodes (3): JamGlobalRelayClient, WebSocketListener, NetworkCallback

### Community 83 - "SocialShareHelper"
Cohesion: 0.27
Nodes (6): PaletteColors, SocialShareHelper, Style, FULL_COVER, GLASS, MINIMAL

### Community 84 - ".extract"
Cohesion: 0.21
Nodes (3): ArtworkColorsInstrumentedTest, ArtworkColors, Colors

### Community 85 - "InnerTubeStreamResolver"
Cohesion: 0.10
Nodes (5): CandidateFormat, ClientConfig, InnerTubeStreamResolver, InnerTubeStreamResolverTest, StreamQualitySelectorStressTest

### Community 86 - "Test"
Cohesion: 0.17
Nodes (7): LoudnessStandard, APPLE_MUSIC, BROADCAST_EBU, CD_LOUD, CD_STANDARD, OFF, SPOTIFY

### Community 87 - "AudioQuality"
Cohesion: 0.16
Nodes (5): CompositeStreamResolver, createDefaultStreamResolver(), DefaultStreamResolverTest, StreamResolver, StreamResolver

### Community 88 - ".onCreate"
Cohesion: 0.67
Nodes (5): ArtistResultCard(), RecentSearchCard(), SearchHistoryRowItem(), SearchScreen(), SearchSongRowItem()

### Community 89 - "NERF Music 1.5.1"
Cohesion: 0.33
Nodes (5): Adicionado, Aprimorado e corrigido, Backups mais seguros e recomendações no seu controle, NERF Music 1.5.1, Notas técnicas da versão

### Community 90 - "RadioTuningMode"
Cohesion: 0.33
Nodes (5): RadioTuningMode, BALANCED, DEEP_CUTS, DISCOVERY, FAMILIAR

### Community 96 - "MainScreen"
Cohesion: 0.24
Nodes (7): Explore, Home, Library, MainScreen(), Screen, Search, YtmPivotBar()

### Community 98 - "Artist"
Cohesion: 0.16
Nodes (6): AlbumDetail, Artist, ArtistDetail, SearchResult, QueueFilterChip, UpNextResult

### Community 99 - "UserAffinityRepository"
Cohesion: 0.11
Nodes (3): Track, UserAffinityRepository, SmartRadioEngine

### Community 104 - "TrackRelatedData"
Cohesion: 0.42
Nodes (3): RelatedSection, TrackRelatedData, RelatedMultiTierFallbackTest

### Community 105 - "UserAffinityRepositoryImpl"
Cohesion: 0.09
Nodes (5): artistMatchKey(), UserAffinityRepositoryImpl, OnboardingFlowTest, ContextWrapper, InvocationHandler

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

### Community 124 - "AlbumScreen"
Cohesion: 0.80
Nodes (3): AlbumHeroHeader(), AlbumScreen(), AlbumTrackRow()

## Knowledge Gaps
- **97 isolated node(s):** `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL`, `DAILY_DISCOVER`, `YOUR_DOWNLOADS` (+92 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 584 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **63 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Track` connect `UserAffinityRepository` to `PlayerScreen.kt`, `RecapStoryPage`, `MigrationManager`, `MusicViewModel`, `InnerTubeCatalogClientTest`, `JamSessionManager`, `SocialShareHelper.kt`, `.parseResponsiveListItem`, `HomeScreen.kt`, `.syncTimelineState`, `KaraokeLyricsEngine`, `.rank`, `LibraryScreen`, `UserPlaylist`, `DownloadedTrackEntity`, `UserAffinityRepositoryImpl`, `AudioMetadataTagger`, `LibraryRepositoryImpl`, `RecapScreen.kt`, `AudioMetadataTagger`, `AlbumDetail`, `FullPlayerSheet`, `SharePreviewActivity.kt`, `LyricsData`, `.update`, `SharePreviewSheet`, `LibraryScreen.kt`, `SearchResult`, `WebSocket`, `HomeSection`, `ShareCardInstrumentedTest.kt`, `ArtistScreen.kt`, `PlayerUiUtilsTest.kt`, `.loadLyrics`, `LibraryRepository.kt`, `MusicViewModel.kt`, `LibraryFilter`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `SearchScreen`, `AudioPlayerService.kt`, `LibraryTrackEntity`, `SocialShareHelper`, `.onCreate`, `RadioTuningMode`, `Artist`, `.isConnectedToWifi`, `TrackRelatedData`, `UserAffinityRepositoryImpl`, `.onCreate`, `AlbumScreen`?**
  _High betweenness centrality (0.271) - this node is a cross-community bridge._
- **What connects `SPEED_DIAL`, `QUICK_PICKS_GRID`, `TWO_ROW_CAROUSEL` to the rest of the system?**
  _97 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DownloadChunkRangeStressTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.13227513227513227 - nodes in this community are weakly interconnected._
- **Why does `MusicViewModel` connect `MusicViewModel` to `RecapStoryPage`, `MigrationManager`, `SettingsScreen.kt`, `AppUpdateManager.kt`, `JamSessionManager`, `HomeScreen.kt`, `.syncTimelineState`, `LyricsData`, `KaraokeLyricsEngine`, `LibraryScreen`, `UserPlaylist`, `SpotifyCanvasRepositoryImpl`, `LoudnessStandard`, `UserAffinityRepositoryImpl`, `OnboardingScreen.kt`, `RecapScreen.kt`, `MigrationScreen.kt`, `JamWireMessage`, `MusicRepository`, `ConnectivityObserver`, `FullPlayerSheet`, `SharePreviewActivity.kt`, `LyricsData`, `HomeSectionStyle`, `AudioQuality`, `.loadHomeFeed`, `.prefetchTrack`, `LibraryScreen.kt`, `WebSocket`, `StorageUtils`, `HomeSection`, `NewPipeStreamResolver.kt`, `AudioQuality`, `ArtistScreen.kt`, `MainScreen`, `.loadLyrics`, `MusicViewModel.kt`, `LibraryFilter`, `SearchScreen`, `AudioPlayerService.kt`, `.applyKaraokeState`, `LibraryTrackEntity`, `Test`, `.onCreate`, `RadioTuningMode`, `MainScreen`, `Artist`, `UserAffinityRepository`, `.updateCurrentLyricsLine`, `.isConnectedToWifi`, `TrackRelatedData`, `SearchFilter`, `UpdateNotificationHelper`, `SmartDownloadWorker`, `AlbumScreen`?**
  _High betweenness centrality (0.207) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.02586206896551724 - nodes in this community are weakly interconnected._
- **Why does `AudioPlayerService` connect `AudioPlayerService` to `.loadLyrics`, `JamSessionManager`, `MusicViewModel.kt`, `.update`, `VocalIsolationAudioProcessor`, `AudioCoreDownloadsCrossfadeQualityTest.kt`, `AudioPlayerService.kt`, `.applyKaraokeState`, `.onCreate`, `.prefetchTrack`, `LoudnessStandard`, `Test`, `LibraryRepositoryImpl`, `Context`, `AudioQuality`, `Listener`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Should `RecapStoryPage` be split into smaller, more focused modules?**
  _Cohesion score 0.12612612612612611 - nodes in this community are weakly interconnected._