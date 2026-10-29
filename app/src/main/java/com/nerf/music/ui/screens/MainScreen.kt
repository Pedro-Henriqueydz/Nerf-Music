package com.nerf.music.ui.screens

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.nerf.music.BuildConfig
import com.nerf.music.ui.theme.*
import com.nerf.music.data.model.Track
import com.nerf.music.ui.viewmodel.MusicViewModel
import com.nerf.music.updater.UpdateNotificationHelper
import com.nerf.music.updater.UpdateCheckWorker

/**
 * Rotas principais da aplicação baseadas na arquitetura do YouTube Music (`pivot_bar` + Search).
 */
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "Início",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Explore : Screen(
        route = "explore",
        title = "Explorar",
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore
    )

    data object Library : Screen(
        route = "library",
        title = "Biblioteca",
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic
    )

    data object Search : Screen(
        route = "search",
        title = "Pesquisar",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    )
}

@Composable
fun MainScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route ?: Screen.Home.route

    val playbackState by viewModel.playbackState.collectAsState()
    val isFullPlayerVisible by viewModel.isFullPlayerVisible.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val postUpdateChangelog by viewModel.postUpdateChangelog.collectAsState()
    val canvasVideoUrl by viewModel.canvasVideoUrl.collectAsState()
    val isCanvasEnabled by viewModel.isCanvasEnabled.collectAsState()
    val isLyricsAboveTitleEnabled by viewModel.isLyricsAboveTitleEnabled.collectAsState()
    val currentLyricsLine by viewModel.currentLyricsLine.collectAsState()
    val downloadProgressMap by viewModel.downloadProgressMap.collectAsState()
    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val trackRelatedData by viewModel.trackRelatedData.collectAsState()
    val isLoadingRelated by viewModel.isLoadingRelated.collectAsState()
    val karaokeSyncSnapshot by viewModel.karaokeSyncSnapshot.collectAsState()
    val isKaraokeVocalIsolationEnabled by viewModel.isKaraokeVocalIsolationEnabled.collectAsState()
    val karaokeVocalLevel by viewModel.karaokeVocalLevel.collectAsState()
    val lyricsOffsetMs by viewModel.lyricsOffsetMs.collectAsState()
    val showLyricsTranslation by viewModel.showLyricsTranslation.collectAsState()
    val showLyricsPronunciation by viewModel.showLyricsPronunciation.collectAsState()
    val hideOriginalLyrics by viewModel.hideOriginalLyrics.collectAsState()
    val isTranslatingLyrics by viewModel.isTranslatingLyrics.collectAsState()
    val translationTargetLanguage by viewModel.translationTargetLanguage.collectAsState()
    val isInstrumentalTrackReady by viewModel.isInstrumentalTrackReady.collectAsState()
    val isUsingRealInstrumental by viewModel.isUsingRealInstrumental.collectAsState()
    val jamSessionState by viewModel.jamSessionState.collectAsState()
    val isJamSheetVisible by viewModel.isJamSheetVisible.collectAsState()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val userPlaylists by viewModel.userPlaylists.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    var showSaveToPlaylistSheet by remember { mutableStateOf(false) }
    var trackForOptions by remember { mutableStateOf<Track?>(null) }
    var trackToSaveInPlaylist by remember { mutableStateOf<Track?>(null) }
    var showCreatePlaylistFromPlayer by remember { mutableStateOf(false) }
    var newPlaylistFromPlayerTitle by remember { mutableStateOf("") }
    var newPlaylistFromPlayerVisibility by remember { mutableStateOf("Pública") }
    var showVisibilitySheetFromPlayer by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Solicita permissão de notificação no Android 13+ (API 33+) para controle de mídia em segundo plano
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        var hasNotificationPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        }
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasNotificationPermission = isGranted
            if (isGranted) {
                UpdateNotificationHelper.createNotificationChannel(context)
                UpdateCheckWorker.schedule(context)
            }
        }

        LaunchedEffect(Unit) {
            UpdateNotificationHelper.createNotificationChannel(context)
            UpdateCheckWorker.schedule(context)
            if (!hasNotificationPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Notificações rápidas (Toasts) de mensagens ou erros
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    // Modal de Novidades (Change Log pós-atualização)
    postUpdateChangelog?.let { changelog ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissChangelogDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Novidades da Atualização",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "O NERF Music foi atualizado com sucesso para a versão v${BuildConfig.VERSION_NAME}! Confira o que mudou:",
                        fontFamily = YouTubeSansFontFamily,
                        fontSize = 14.sp,
                        color = YtmWhite
                    )
                    val changelogScrollState = rememberScrollState()
                    Surface(
                        color = YtmGrey11,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(changelogScrollState)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = changelog,
                                fontFamily = YouTubeSansFontFamily,
                                fontSize = 13.sp,
                                color = YtmTextSecondary,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissChangelogDialog() },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YtmWhite,
                        contentColor = YtmBlack
                    )
                ) {
                    Text(
                        text = "Entendi",
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = YtmPivotBarBg
        )
    }

    // Modal de Autoreparo (In-App Updater)
    availableUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Nova versão disponível",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Uma nova versão do NERF Music (${update.tagName}) está pronta para instalação. Sua versão atual é v${BuildConfig.VERSION_NAME}.",
                        fontFamily = YouTubeSansFontFamily,
                        fontSize = 14.sp,
                        color = YtmTextSecondary
                    )
                    if (!update.releaseNotes.isNullOrBlank()) {
                        Text(
                            text = update.releaseNotes,
                            fontFamily = YouTubeSansFontFamily,
                            fontSize = 12.sp,
                            color = YtmTextSecondary,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmAndDownloadUpdate(context) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YtmWhite,
                        contentColor = YtmBlack
                    )
                ) {
                    Text(
                        text = "Atualizar agora",
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                    Text(
                        text = "Mais tarde",
                        fontFamily = YouTubeSansFontFamily,
                        color = YtmTextSecondary
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = YtmPivotBarBg
        )
    }

    val pivotItems = listOf(Screen.Home, Screen.Explore, Screen.Search, Screen.Library)

    val navigateToSearch: () -> Unit = {
        if (currentRoute != Screen.Search.route) {
            navController.navigate(Screen.Search.route) {
                launchSingleTop = true
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(YtmBlack)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = YtmBlack,
            bottomBar = {
                val isDetailRoute = currentRoute.startsWith("album") || currentRoute.startsWith("artist")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isDetailRoute) Modifier.navigationBarsPadding() else Modifier)
                ) {
                    // Mini Player persistente: visível em todas as telas (incluindo Álbum)
                    if (playbackState.currentTrack != null) {
                        MiniPlayerBar(
                            state = playbackState,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipNext() },
                            onExpandPlayer = { viewModel.openFullPlayer() }
                        )
                    }

                    // Pivot Bar oficial do YouTube Music (#1D1D1D, oculta dentro de telas de detalhes de álbum)
                    if (!isDetailRoute) {
                        YtmPivotBar(
                            items = pivotItems,
                            currentRoute = currentRoute,
                            onTabSelected = { screen ->
                                if (currentRoute != screen.route) {
                                    if (screen == Screen.Home) {
                                        val popped = navController.popBackStack(Screen.Home.route, false)
                                        if (!popped) {
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    } else navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenAlbum = { browseId ->
                            navController.navigate("album/$browseId") {
                                launchSingleTop = true
                            }
                        },
                        onOpenSettings = {
                            navController.navigate("settings") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToDownloads = {
                            navController.navigate(Screen.Library.route) {
                                launchSingleTop = true
                            }
                        },
                        onTrackOptionsClick = { track ->
                            trackForOptions = track
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Explore.route) {
                    ExploreScreen(
                        viewModel = viewModel,
                        onOpenAlbum = { id -> navController.navigate("album/$id") { launchSingleTop = true } }
                        onOpenArtist = { id -> navController.navigate("artist/$id") { launchSingleTop = true } }
                    )
                }
                composable(Screen.Search.route) {
                    SearchScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            if (navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                        },
                        onOpenAlbum = { albumId ->
                            navController.navigate("album/$albumId") {
                                launchSingleTop = true
                            }
                        },
                        onOpenArtist = { artistId ->
                            navController.navigate("artist/$artistId") {
                                launchSingleTop = true
                            }
                        },
                        onTrackOptionsClick = { track ->
                            trackForOptions = track
                        }
                    )
                }
                composable(Screen.Library.route) {
                    LibraryScreen(
                        viewModel = viewModel,
                        onOpenSettings = {
                            navController.navigate("settings") {
                                launchSingleTop = true
                            }
                        },
                        onOpenAlbum = { browseId ->
                            navController.navigate("album/$browseId") {
                                launchSingleTop = true
                            }
                        },
                        onOpenArtist = { artistId ->
                            navController.navigate("artist/$artistId") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(
                    route = "album/{browseId}",
                    arguments = listOf(navArgument("browseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val browseId = backStackEntry.arguments?.getString("browseId").orEmpty()
                    AlbumScreen(
                        browseId = browseId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenArtist = { artistId ->
                            navController.navigate("artist/$artistId") {
                                launchSingleTop = true
                            }
                        },
                        onTrackOptionsClick = { track ->
                            trackForOptions = track
                        }
                    )
                }
                composable(
                    route = "artist/{browseId}",
                    arguments = listOf(navArgument("browseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val browseId = backStackEntry.arguments?.getString("browseId").orEmpty()
                    ArtistScreen(
                        browseId = browseId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenAlbum = { albumId ->
                            navController.navigate("album/$albumId") {
                                launchSingleTop = true
                            }
                        },
                        onOpenArtist = { artistId ->
                            navController.navigate("artist/$artistId") {
                                launchSingleTop = true
                            }
                        },
                        onTrackOptionsClick = { track ->
                            trackForOptions = track
                        }
                    )
                }
            }
        }

        // Full Player Screen (Sobreposto ao app inteiro com animação de slide)
        AnimatedVisibility(
            visible = isFullPlayerVisible && playbackState.currentTrack != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f)
            ),
            exit = androidx.compose.animation.ExitTransition.None
        ) {
            FullPlayerSheet(
                state = playbackState,
                canvasVideoUrl = if (isCanvasEnabled) canvasVideoUrl else null,
                lyricsLine = if (isLyricsAboveTitleEnabled) currentLyricsLine else null,
                lyrics = currentLyrics,
                onDismiss = { viewModel.closeFullPlayer() },
                onTogglePlay = { viewModel.togglePlayPause() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onForceSkipPrevious = { viewModel.forceSkipPrevious() },
                onSkipNext = { viewModel.skipNext() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onSeekTo = { positionMs -> viewModel.seekTo(positionMs) },
                onToggleFavorite = { trackId -> viewModel.toggleFavorite(trackId) },
                onToggleSave = { _ -> showSaveToPlaylistSheet = true },
                onToggleDownload = { trackId -> viewModel.toggleDownload(trackId) },
                onOpenTrackOptions = { trackForOptions = it },
                downloadProgress = playbackState.currentTrack?.let { downloadProgressMap[it.id] },
                onSelectQueueChip = { chip -> viewModel.selectQueueFilterChip(chip) },
                onPlayQueueItem = { index -> viewModel.playQueueItemAt(index) },
                onMoveQueueItem = { fromIndex, toIndex -> viewModel.moveQueueItem(fromIndex, toIndex) },
                onRemoveQueueItem = { index -> viewModel.removeQueueItem(index) },
                onOpenArtist = { artistIdentifier ->
                    viewModel.closeFullPlayer()
                    if (artistIdentifier.startsWith("UC") || artistIdentifier.startsWith("FE")) {
                        navController.navigate("artist/$artistIdentifier") {
                            launchSingleTop = true
                        }
                    } else {
                        coroutineScope.launch {
                            val resolved = viewModel.resolveArtistBrowseId(artistIdentifier)
                            val target = resolved ?: artistIdentifier
                            navController.navigate("artist/$target") {
                                launchSingleTop = true
                            }
                        }
                    }
                },
                relatedData = trackRelatedData,
                isLoadingRelated = isLoadingRelated,
                onLoadRelatedIfNeeded = {
                    playbackState.currentTrack?.let { track ->
                        viewModel.loadTrackRelatedForTrack(track, forceReload = true)
                    }
                },
                onPlayRelatedTrack = { relTrack -> viewModel.playTrack(relTrack) },
                onOpenAlbum = { albumId ->
                    viewModel.closeFullPlayer()
                    navController.navigate("album/$albumId") {
                        launchSingleTop = true
                    }
                },
                karaokeSyncSnapshot = karaokeSyncSnapshot,
                isKaraokeVocalIsolationEnabled = isKaraokeVocalIsolationEnabled,
                karaokeVocalLevel = karaokeVocalLevel,
                lyricsOffsetMs = lyricsOffsetMs,
                showLyricsTranslation = showLyricsTranslation,
                showLyricsPronunciation = showLyricsPronunciation,
                hideOriginalLyrics = hideOriginalLyrics,
                isTranslatingLyrics = isTranslatingLyrics,
                translationTargetLanguage = translationTargetLanguage,
                isInstrumentalTrackReady = isInstrumentalTrackReady,
                isUsingRealInstrumental = isUsingRealInstrumental,
                jamSessionState = jamSessionState,
                onToggleKaraokeVocalIsolation = { viewModel.toggleKaraokeVocalIsolation() },
                onSetKaraokeVocalLevel = { level -> viewModel.setKaraokeVocalLevel(level) },
                onAdjustLyricsOffset = { deltaMs -> viewModel.adjustLyricsOffset(deltaMs) },
                onResetLyricsOffset = { viewModel.resetLyricsOffset() },
                onToggleLyricsTranslation = { viewModel.toggleLyricsTranslation() },
                onToggleLyricsPronunciation = { viewModel.toggleLyricsPronunciation() },
                onToggleHideOriginalLyrics = { viewModel.toggleHideOriginalLyrics() },
                onSelectTranslationTargetLanguage = { lang -> viewModel.selectTranslationTargetLanguage(lang) },
                onOpenJamSheet = { viewModel.openJamSheet() }
            )
        }

        // Onboarding Screen de Primeiro Acesso com Apple-like Motion
        AnimatedVisibility(
            visible = !isOnboardingCompleted,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(320))
        ) {
            OnboardingScreen(
                onComplete = { selectedArtists ->
                    viewModel.completeOnboarding(selectedArtists)
                },
                onSearchArtists = { query ->
                    viewModel.searchArtistsForOnboarding(query)
                }
            )
        }
    }

    if (isJamSheetVisible) {
        JamSessionSheet(
            jamState = jamSessionState,
            onDismiss = { viewModel.closeJamSheet() },
            onStartHostJam = { customRelayWs -> viewModel.startHostJam(customRelayWs) },
            onJoinJamInput = { input -> viewModel.joinJamFromInput(input) },
            onLeaveJam = { viewModel.leaveJam() },
            onUpdateDisplayName = { name -> viewModel.updateJamDisplayName(name) },
            onToggleAllowGuestsToQueue = { allowed -> viewModel.setJamAllowGuestsToQueue(allowed) },
            onToggleAllowGuestsToControlPlayback = { allowed -> viewModel.setJamAllowGuestsToControlPlayback(allowed) },
            onSelectRepeatMode = { modeName ->
                val mode = when (modeName) {
                    "ONE" -> androidx.media3.common.Player.REPEAT_MODE_ONE
                    "ALL" -> androidx.media3.common.Player.REPEAT_MODE_ALL
                    else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                }
                viewModel.setRepeatModeExplicit(mode)
            },
            onChangeHostCrossfadeSeconds = { seconds -> viewModel.setCrossfadeSeconds(seconds) },
            onToggleSyncKaraokeWithRoom = { sync -> viewModel.setJamSyncKaraokeWithRoom(sync) },
            onKickParticipant = { peerId -> viewModel.kickJamParticipant(peerId) },
            onRequestAudioResync = { viewModel.requestJamAudioResync() }
        )
    }

    trackForOptions?.let { track ->
        val isLiked = likedTracks.any { it.id == track.id }
        val isDownloaded = downloadedTracks.any { it.track.id == track.id }
        TrackOptionsBottomSheet(
            track = track,
            isLiked = isLiked,
            isDownloaded = isDownloaded,
            onDismiss = { trackForOptions = null },
            onPlayNext = {
                viewModel.addTracksToPlayNext(listOf(track))
                trackForOptions = null
            },
            onAddToQueue = {
                viewModel.addTracksToQueue(listOf(track))
                trackForOptions = null
            },
            onStartRadio = {
                viewModel.playTrack(track)
                trackForOptions = null
            },
            onSaveToPlaylist = {
                trackToSaveInPlaylist = track
                trackForOptions = null
                showSaveToPlaylistSheet = true
            },
            onToggleFavorite = {
                viewModel.toggleFavorite(track.id)
            },
            onToggleDownload = {
                viewModel.toggleDownload(track.id)
            },
            onOpenArtist = { artistId ->
                trackForOptions = null
                viewModel.closeFullPlayer()
                navController.navigate("artist/$artistId") {
                    launchSingleTop = true
                }
            },
            onOpenAlbum = { albumId ->
                trackForOptions = null
                viewModel.closeFullPlayer()
                navController.navigate("album/$albumId") {
                    launchSingleTop = true
                }
            }
        )
    }

    if (showSaveToPlaylistSheet && (trackToSaveInPlaylist != null || playbackState.currentTrack != null)) {
        val currentTrack = trackToSaveInPlaylist ?: playbackState.currentTrack!!
        SaveToPlaylistSheet(
            track = currentTrack,
            userPlaylists = userPlaylists,
            isLiked = likedTracks.any { it.id == currentTrack.id },
            likedCount = likedTracks.size,
            onDismiss = {
                showSaveToPlaylistSheet = false
                trackToSaveInPlaylist = null
            },
            onToggleLiked = { viewModel.toggleFavorite(currentTrack.id) },
            onTogglePlaylist = { pl, shouldAdd ->
                if (shouldAdd) {
                    viewModel.addTrackToPlaylist(pl.id, currentTrack)
                } else {
                    viewModel.removeTrackFromPlaylist(pl.id, currentTrack.id)
                }
            },
            onCreateNewPlaylist = {
                showCreatePlaylistFromPlayer = true
            }
        )
    }

    if (showCreatePlaylistFromPlayer) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistFromPlayer = false
                newPlaylistFromPlayerTitle = ""
            },
            containerColor = Color(0xFF282828),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Nova playlist",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = YtmWhite
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = newPlaylistFromPlayerTitle,
                        onValueChange = { newPlaylistFromPlayerTitle = it },
                        placeholder = {
                            Text(text = "Título", color = YtmTextSecondary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = YtmWhite,
                            unfocusedTextColor = YtmWhite,
                            focusedBorderColor = YtmWhite,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVisibilitySheetFromPlayer = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Visibilidade",
                                    fontFamily = YouTubeSansFontFamily,
                                    fontSize = 12.sp,
                                    color = YtmTextSecondary
                                )
                                Text(
                                    text = newPlaylistFromPlayerVisibility,
                                    fontFamily = YouTubeSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = YtmWhite
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = YtmWhite
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val isEnabled = newPlaylistFromPlayerTitle.isNotBlank()
                Button(
                    onClick = {
                        if (isEnabled) {
                            val created = viewModel.createPlaylist(newPlaylistFromPlayerTitle.trim())
                            if (created != null && playbackState.currentTrack != null) {
                                viewModel.addTrackToPlaylist(created.id, playbackState.currentTrack!!)
                            }
                            showCreatePlaylistFromPlayer = false
                            newPlaylistFromPlayerTitle = ""
                        }
                    },
                    enabled = isEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEnabled) YtmWhite else Color(0xFF3A3A3A),
                        contentColor = if (isEnabled) YtmBlack else YtmTextSecondary
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(text = "Criar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreatePlaylistFromPlayer = false
                    newPlaylistFromPlayerTitle = ""
                }) {
                    Text(text = "Cancelar", color = YtmWhite, fontSize = 14.sp)
                }
            }
        )
    }

    if (showVisibilitySheetFromPlayer) {
        SetVisibilitySheet(
            currentVisibility = newPlaylistFromPlayerVisibility,
            onSelectVisibility = { newPlaylistFromPlayerVisibility = it },
            onDismiss = { showVisibilitySheetFromPlayer = false }
        )
    }
}

/**
 * Réplica fiel da `layout/pivot_bar` (`res/msd.xml`) do APK oficial do YouTube Music:
 * - Fundo `#1D1D1D` (`ytm_color_grey_12`)
 * - Sem indicador oval (`tabIndicatorHeight = 0dp`)
 * - Ícone e texto `#FFFFFF` quando ativo e `#909090` (`ytm_color_grey_06`) quando inativo.
 */
@Composable
fun YtmPivotBar(
    items: List<Screen>,
    currentRoute: String,
    onTabSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(YtmPivotBarBg)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = YtmDivider
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { screen ->
                val isSelected = currentRoute == screen.route
                val itemColor = if (isSelected) YtmWhite else YtmIconInactive
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onTabSelected(screen) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title,
                        tint = itemColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = screen.title,
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        color = itemColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
