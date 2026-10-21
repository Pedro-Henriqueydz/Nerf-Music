package com.nerf.music.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.nerf.music.ui.viewmodel.MusicViewModel

/**
 * Itens da barra de navegação principal (Bottom Navigation).
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

    data object Search : Screen(
        route = "search",
        title = "Buscar",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    )

    data object Library : Screen(
        route = "library",
        title = "Biblioteca",
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic
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

    // Modal de Autoreparo (In-App Updater): exibido caso haja uma tag mais nova no GitHub Releases
    availableUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Nova versão disponível",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Uma nova versão do NERF Music (${update.tagName}) está pronta para instalação. Sua versão atual é v${BuildConfig.VERSION_NAME}.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!update.releaseNotes.isNullOrBlank()) {
                        Text(
                            text = update.releaseNotes,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmAndDownloadUpdate(context) },
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Atualizar agora")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissUpdateDialog() }
                ) {
                    Text("Mais tarde")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF1E1E1E)
        )
    }

    val items = listOf(Screen.Home, Screen.Search, Screen.Library)

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Mini Player persistente: posicionado logo acima da NavigationBar
                    if (playbackState.currentTrack != null) {
                        MiniPlayerBar(
                            state = playbackState,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipNext() },
                            onExpandPlayer = { viewModel.openFullPlayer() }
                        )
                    }

                    // Barra de navegação inferior (Material 3) com 3 abas
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        items.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(viewModel = viewModel)
                }
                composable(Screen.Search.route) {
                    SearchScreen(viewModel = viewModel)
                }
                composable(Screen.Library.route) {
                    LibraryScreen()
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
