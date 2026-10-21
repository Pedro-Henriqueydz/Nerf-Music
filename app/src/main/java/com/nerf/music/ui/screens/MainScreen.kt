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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nerf.music.BuildConfig
import com.nerf.music.ui.theme.*
import com.nerf.music.ui.viewmodel.MusicViewModel

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

    val context = LocalContext.current

    // Notificações rápidas (Toasts) de mensagens ou erros
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
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

    val pivotItems = listOf(Screen.Home, Screen.Explore, Screen.Library)

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
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Mini Player persistente: posicionado logo acima da PivotBar em todas as abas
                    if (playbackState.currentTrack != null) {
                        MiniPlayerBar(
                            state = playbackState,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipNext() },
                            onExpandPlayer = { viewModel.openFullPlayer() }
                        )
                    }

                    // Pivot Bar oficial do YouTube Music (#1D1D1D, sem pílula de seleção, fonte YouTube Sans)
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
                        onOpenSearch = navigateToSearch
                    )
                }
                composable(Screen.Explore.route) {
                    ExploreScreen(
                        viewModel = viewModel,
                        onOpenSearch = navigateToSearch
                    )
                }
                composable(Screen.Library.route) {
                    LibraryScreen(
                        onOpenSearch = navigateToSearch
                    )
                }
                composable(Screen.Search.route) {
                    SearchScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
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
                onRemoveQueueItem = { index -> viewModel.removeQueueItem(index) }
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
