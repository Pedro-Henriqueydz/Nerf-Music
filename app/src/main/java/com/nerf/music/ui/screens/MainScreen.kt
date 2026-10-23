package com.nerf.music.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
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
    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val trackRelatedData by viewModel.trackRelatedData.collectAsState()
    val isLoadingRelated by viewModel.isLoadingRelated.collectAsState()
    val coroutineScope = rememberCoroutineScope()

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
                    Surface(
                        color = YtmGrey11,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = changelog,
                            fontFamily = YouTubeSansFontFamily,
                            fontSize = 12.5.sp,
                            color = YtmTextSecondary,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
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
                Column(modifier = Modifier.fillMaxWidth()) {
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
                                    navController.navigate(screen.route) {
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
                        viewModel = viewModel
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
                        }
                    )
                }
            }
        }

        // Full Player Screen (Sobreposto ao app inteiro com animação de slide)
        AnimatedVisibility(
            visible = isFullPlayerVisible && playbackState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            FullPlayerSheet(
                state = playbackState,
                canvasVideoUrl = if (isCanvasEnabled) canvasVideoUrl else null,
                lyricsLine = if (isLyricsAboveTitleEnabled) currentLyricsLine else null,
                lyrics = currentLyrics,
                onDismiss = { viewModel.closeFullPlayer() },
                onTogglePlay = { viewModel.togglePlayPause() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onSkipNext = { viewModel.skipNext() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onSeekTo = { positionMs -> viewModel.seekTo(positionMs) },
                onToggleFavorite = { trackId -> viewModel.toggleFavorite(trackId) },
                onToggleSave = { trackId -> viewModel.toggleSaveToPlaylist(trackId) },
                onToggleDownload = { trackId -> viewModel.toggleDownload(trackId) },
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
                onPlayRelatedTrack = { relTrack -> viewModel.playTrack(relTrack) },
                onOpenAlbum = { albumId ->
                    viewModel.closeFullPlayer()
                    navController.navigate("album/$albumId") {
                        launchSingleTop = true
                    }
                }
            )
        }
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
