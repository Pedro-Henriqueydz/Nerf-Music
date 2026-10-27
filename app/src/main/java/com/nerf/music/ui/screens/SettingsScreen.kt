package com.nerf.music.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerf.music.BuildConfig
import com.nerf.music.data.repository.AudioQuality
import com.nerf.music.data.repository.DataSaverMode
import com.nerf.music.data.repository.DownloadAudioQuality
import com.nerf.music.data.repository.SmartDownloadManager
import com.nerf.music.data.repository.SmartDownloadSyncState
import com.nerf.music.ui.theme.*
import com.nerf.music.ui.viewmodel.MusicViewModel
import com.nerf.music.util.StorageBreakdown
import com.nerf.music.util.StorageUtils

/**
 * Cores de estilo idênticas ao design do Spotify 9.1.94
 */
private val SpotifyBlack = Color(0xFF121212)
private val SpotifyCardBg = Color(0xFF242424)
private val SpotifyGreen = Color(0xFF1ED760)
private val SpotifyTextSecondary = Color(0xFFB3B3B3)
private val SpotifyTrackInactive = Color(0xFF4D4D4D)
private val SpotifyNoteBg = Color(0xFF1E1E1E)
private val SpotifyStorageCache = Color(0xFF1ED760)
private val SpotifyStorageDownloads = Color(0xFF3D91F4)
private val SpotifyStorageFree = Color(0xFF535353)

/**
 * Subtelas de navegação das configurações hierárquicas (padrão Spotify).
 */
enum class SettingsSubScreen {
    MAIN,
    DATA_SAVER,
    MEDIA_QUALITY,
    PLAYBACK,
    CONTENT_DISPLAY,
    STORAGE,
    MIGRATION,
    ABOUT
}

/**
 * Item de índice para busca global rápida nas configurações
 */
private data class SearchableSetting(
    val title: String,
    val subtitle: String,
    val categoryName: String,
    val targetSubScreen: SettingsSubScreen
)

/**
 * Tela de Configurações do Nerf Music recriada fielmente segundo a arquitetura,
 * hierarquia de páginas e design visual do Spotify 9.1.94.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSleepTimerSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Intercepta botão Voltar do sistema Android quando em uma sub-tela ou busca ativa
    BackHandler(enabled = isSearchActive || currentSubScreen != SettingsSubScreen.MAIN) {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else {
            currentSubScreen = SettingsSubScreen.MAIN
        }
    }

    val isCanvasEnabled by viewModel.isCanvasEnabled.collectAsState()
    val isCanvasWifiOnly by viewModel.isCanvasWifiOnly.collectAsState()
    val isLyricsAboveTitleEnabled by viewModel.isLyricsAboveTitleEnabled.collectAsState()
    val isGaplessEnabled by viewModel.isGaplessEnabled.collectAsState()
    val isAutomixEnabled by viewModel.isAutomixEnabled.collectAsState()
    val crossfadeSeconds by viewModel.crossfadeSeconds.collectAsState()
    val dataSaverMode by viewModel.dataSaverMode.collectAsState()
    val isDownloadOverCellular by viewModel.isDownloadOverCellular.collectAsState()
    val wifiAudioQuality by viewModel.wifiAudioQuality.collectAsState()
    val cellularAudioQuality by viewModel.cellularAudioQuality.collectAsState()
    val sleepTimerRemainingSeconds by viewModel.sleepTimerRemainingSeconds.collectAsState()
    val sleepTimerAtEndOfTrack by viewModel.sleepTimerAtEndOfTrack.collectAsState()
    val storageBreakdown by viewModel.storageBreakdown.collectAsState()
    val downloadAudioQuality by viewModel.downloadAudioQuality.collectAsState()
    val isSmartDownloadsEnabled by viewModel.isSmartDownloadsEnabled.collectAsState()
    val smartDownloadsLimit by viewModel.smartDownloadsLimit.collectAsState()
    val isSmartDownloadsWifiOnly by viewModel.isSmartDownloadsWifiOnly.collectAsState()
    val smartDownloadSyncState by viewModel.smartDownloadSyncState.collectAsState()

    // Atualiza armazenamento ao entrar
    LaunchedEffect(Unit) {
        viewModel.refreshStorageBreakdown(context)
    }

    // Modal de Temporizador de Sono (Sleep Timer)
    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            remainingSeconds = sleepTimerRemainingSeconds,
            isEndOfTrack = sleepTimerAtEndOfTrack,
            onSelectMinutes = {
                viewModel.setSleepTimerMinutes(it)
                showSleepTimerSheet = false
            },
            onSelectEndOfTrack = {
                viewModel.setSleepTimerEndOfTrack()
                showSleepTimerSheet = false
            },
            onCancelTimer = {
                viewModel.cancelSleepTimer()
                showSleepTimerSheet = false
            },
            onDismiss = { showSleepTimerSheet = false }
        )
    }

    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = {
            if (targetState != SettingsSubScreen.MAIN && initialState == SettingsSubScreen.MAIN) {
                (slideInHorizontally(animationSpec = tween(220)) { it } + fadeIn()).togetherWith(
                    slideOutHorizontally(animationSpec = tween(220)) { -it / 3 } + fadeOut()
                )
            } else {
                (slideInHorizontally(animationSpec = tween(220)) { -it / 3 } + fadeIn()).togetherWith(
                    slideOutHorizontally(animationSpec = tween(220)) { it } + fadeOut()
                )
            }
        },
        label = "SettingsSubScreenTransition"
    ) { subScreen ->
        when (subScreen) {
            SettingsSubScreen.MAIN -> {
                SettingsMainContent(
                    dataSaverMode = dataSaverMode,
                    audioQuality = wifiAudioQuality,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onStartSearch = { isSearchActive = true },
                    onCloseSearch = {
                        isSearchActive = false
                        searchQuery = ""
                    },
                    onSelectSearchResult = { target ->
                        isSearchActive = false
                        searchQuery = ""
                        currentSubScreen = target
                    },
                    onNavigateBack = onNavigateBack,
                    onOpenDataSaver = { currentSubScreen = SettingsSubScreen.DATA_SAVER },
                    onOpenMediaQuality = { currentSubScreen = SettingsSubScreen.MEDIA_QUALITY },
                    onOpenPlayback = { currentSubScreen = SettingsSubScreen.PLAYBACK },
                    onOpenContentDisplay = { currentSubScreen = SettingsSubScreen.CONTENT_DISPLAY },
                    onOpenStorage = { currentSubScreen = SettingsSubScreen.STORAGE },
                    onOpenMigration = { currentSubScreen = SettingsSubScreen.MIGRATION },
                    onOpenAbout = { currentSubScreen = SettingsSubScreen.ABOUT },
                    modifier = modifier
                )
            }
            SettingsSubScreen.DATA_SAVER -> {
                DataSaverSettingsScreen(
                    currentMode = dataSaverMode,
                    onSelectMode = { viewModel.setDataSaverMode(it) },
                    isDownloadOverCellular = isDownloadOverCellular,
                    onDownloadOverCellularChange = { viewModel.setDownloadOverCellular(it) },
                    isCanvasWifiOnly = isCanvasWifiOnly,
                    onCanvasWifiOnlyChange = { viewModel.setCanvasWifiOnly(it) },
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    modifier = modifier
                )
            }
            SettingsSubScreen.MEDIA_QUALITY -> {
                MediaQualitySettingsScreen(
                    wifiQuality = wifiAudioQuality,
                    onSelectWifiQuality = { viewModel.setWifiAudioQuality(it) },
                    cellularQuality = cellularAudioQuality,
                    onSelectCellularQuality = { viewModel.setCellularAudioQuality(it) },
                    downloadQuality = downloadAudioQuality,
                    onSelectDownloadQuality = { viewModel.setDownloadAudioQuality(it) },
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    modifier = modifier
                )
            }
            SettingsSubScreen.PLAYBACK -> {
                PlaybackSettingsScreen(
                    crossfadeSeconds = crossfadeSeconds,
                    onCrossfadeChange = { viewModel.setCrossfadeSeconds(it) },
                    isGaplessEnabled = isGaplessEnabled,
                    onGaplessChange = { viewModel.setGaplessEnabled(it) },
                    isAutomixEnabled = isAutomixEnabled,
                    onAutomixChange = { viewModel.setAutomixEnabled(it) },
                    sleepTimerRemainingSeconds = sleepTimerRemainingSeconds,
                    sleepTimerAtEndOfTrack = sleepTimerAtEndOfTrack,
                    onOpenSleepTimer = { showSleepTimerSheet = true },
                    onOpenEqualizer = { viewModel.openEqualizer(context) },
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    modifier = modifier
                )
            }
            SettingsSubScreen.CONTENT_DISPLAY -> {
                ContentDisplaySettingsScreen(
                    isCanvasEnabled = isCanvasEnabled,
                    onCanvasChange = { viewModel.setCanvasEnabled(it) },
                    isCanvasWifiOnly = isCanvasWifiOnly,
                    onCanvasWifiOnlyChange = { viewModel.setCanvasWifiOnly(it) },
                    isLyricsAboveTitleEnabled = isLyricsAboveTitleEnabled,
                    onLyricsAboveTitleChange = { viewModel.setLyricsAboveTitleEnabled(it) },
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    modifier = modifier
                )
            }
            SettingsSubScreen.STORAGE -> {
                StorageSettingsScreen(
                    breakdown = storageBreakdown,
                    isSmartDownloadsEnabled = isSmartDownloadsEnabled,
                    onSmartDownloadsEnabledChange = { viewModel.setSmartDownloadsEnabled(it) },
                    smartDownloadsLimit = smartDownloadsLimit,
                    onSmartDownloadsLimitChange = { viewModel.setSmartDownloadsLimit(it) },
                    isSmartDownloadsWifiOnly = isSmartDownloadsWifiOnly,
                    onSmartDownloadsWifiOnlyChange = { viewModel.setSmartDownloadsWifiOnly(it) },
                    syncState = smartDownloadSyncState,
                    onTriggerSyncNow = { viewModel.triggerSmartDownloadSyncNow() },
                    onSyncDownloads = { onDone ->
                        viewModel.syncDownloadsWithDisk { count ->
                            android.widget.Toast.makeText(
                                context,
                                "Downloads sincronizados: $count faixas reconciliadas",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            onDone()
                        }
                    },
                    onClearCache = { viewModel.clearAppCache(context) },
                    onRefresh = { viewModel.refreshStorageBreakdown(context) },
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    modifier = modifier
                )
            }
            SettingsSubScreen.MIGRATION -> {
                MigrationScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    modifier = modifier
                )
            }
            SettingsSubScreen.ABOUT -> {
                AboutSettingsScreen(
                    onCheckUpdates = { viewModel.checkForAppUpdates(manual = true) },
                    onShowChangeLog = { viewModel.showChangeLogManually() },
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    modifier = modifier
                )
            }
        }
    }
}

/**
 * Tela Principal de Configurações do Spotify
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMainContent(
    dataSaverMode: DataSaverMode,
    audioQuality: AudioQuality,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onStartSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSelectSearchResult: (SettingsSubScreen) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenDataSaver: () -> Unit,
    onOpenMediaQuality: () -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenContentDisplay: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenMigration: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSearchableItems = remember {
        listOf(
            SearchableSetting("Economia de dados", "Modo economia de dados permanente ou automático", "Economia de dados", SettingsSubScreen.DATA_SAVER),
            SearchableSetting("Downloads via dados móveis", "Inicia ou continua downloads sem Wi-Fi", "Economia de dados", SettingsSubScreen.DATA_SAVER),
            SearchableSetting("Qualidade do streaming no Wi-Fi", "Automática, Baixa, Normal, Alta ou Muito alta", "Qualidade da mídia", SettingsSubScreen.MEDIA_QUALITY),
            SearchableSetting("Qualidade em dados móveis", "Reduz o consumo de rede no 4G/5G", "Qualidade da mídia", SettingsSubScreen.MEDIA_QUALITY),
            SearchableSetting("Qualidade dos downloads (Lossless)", "Lossless / Máxima Fidelidade, Muito alta, Alta ou Normal", "Qualidade da mídia", SettingsSubScreen.MEDIA_QUALITY),
            SearchableSetting("Downloads Inteligentes (Smart Downloads)", "Limite de músicas, apenas no Wi-Fi e sincronização automática", "Armazenamento", SettingsSubScreen.STORAGE),
            SearchableSetting("Crossfade", "Transição suave e sobreposição entre faixas (0s-12s)", "Reprodução", SettingsSubScreen.PLAYBACK),
            SearchableSetting("Reprodução sem pausas (Gapless)", "Elimina o silêncio entre músicas consecutivas", "Reprodução", SettingsSubScreen.PLAYBACK),
            SearchableSetting("Automix", "Transições fluidas inteligentes em playlists", "Reprodução", SettingsSubScreen.PLAYBACK),
            SearchableSetting("Equalizador", "Abre o painel de equalização de áudio", "Reprodução", SettingsSubScreen.PLAYBACK),
            SearchableSetting("Temporizador de sono (Sleep Timer)", "Pausa a reprodução após tempo determinado ou fim da faixa", "Reprodução", SettingsSubScreen.PLAYBACK),
            SearchableSetting("Canvas", "Exibe efeitos visuais curtos em loop no player", "Conteúdo e exibição", SettingsSubScreen.CONTENT_DISPLAY),
            SearchableSetting("Canvas apenas no Wi-Fi", "Economiza dados móveis reproduzindo Canvas só no Wi-Fi", "Conteúdo e exibição", SettingsSubScreen.CONTENT_DISPLAY),
            SearchableSetting("Letras sincronizadas no Player", "Exibe a frase da letra em tempo real acima do título", "Conteúdo e exibição", SettingsSubScreen.CONTENT_DISPLAY),
            SearchableSetting("Armazenamento e Cache", "Espaço de downloads, cache e botão de limpar cache", "Armazenamento", SettingsSubScreen.STORAGE),
        SearchableSetting("Importar biblioteca e migração", "Migre do Spotify, YouTube Music ou arquivos de backup", "Migração", SettingsSubScreen.MIGRATION),
        SearchableSetting("Conectar YouTube Music", "Sincronização de Músicas Curtidas e Playlists", "Migração", SettingsSubScreen.MIGRATION),
        SearchableSetting("Conectar Spotify", "Importação de Liked Songs e Playlists", "Migração", SettingsSubScreen.MIGRATION),
        SearchableSetting("Importar link de playlist", "Cole link público do Spotify ou YouTube Music", "Migração", SettingsSubScreen.MIGRATION),
        SearchableSetting("Importar arquivo CSV / Backup", "Restaure músicas via Soundiiz ou TuneMyMusic", "Migração", SettingsSubScreen.MIGRATION),
            SearchableSetting("Versão do aplicativo", "Verificar atualizações do Nerf Music", "Sobre", SettingsSubScreen.ABOUT),
            SearchableSetting("Novidades da versão (Change Log)", "Relembre os novos recursos adicionados", "Sobre", SettingsSubScreen.ABOUT)
        )
    }

    val filteredResults = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            allSearchableItems.filter {
                it.title.lowercase().contains(q) ||
                it.subtitle.lowercase().contains(q) ||
                it.categoryName.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        containerColor = SpotifyBlack,
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpotifyCardBg)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                textStyle = TextStyle(
                                    fontFamily = YouTubeSansFontFamily,
                                    fontSize = 15.sp,
                                    color = Color.White
                                ),
                                cursorBrush = SolidColor(SpotifyGreen),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Buscar nas configurações...",
                                            fontFamily = YouTubeSansFontFamily,
                                            fontSize = 14.sp,
                                            color = SpotifyTextSecondary
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onCloseSearch) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Fechar busca",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpar busca",
                                    tint = SpotifyTextSecondary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SpotifyBlack)
                )
            } else {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Configurações",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onStartSearch) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Pesquisar",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SpotifyBlack)
                )
            }
        }
    ) { innerPadding ->
        if (isSearchActive) {
            // Lista de resultados filtrados da busca
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (searchQuery.isBlank()) {
                    Text(
                        text = "Digite o nome de uma configuração para buscar.",
                        fontFamily = YouTubeSansFontFamily,
                        fontSize = 13.sp,
                        color = SpotifyTextSecondary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else if (filteredResults.isEmpty()) {
                    Text(
                        text = "Nenhuma configuração encontrada para \"$searchQuery\".",
                        fontFamily = YouTubeSansFontFamily,
                        fontSize = 13.sp,
                        color = SpotifyTextSecondary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    filteredResults.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSearchResult(item.targetSubScreen) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontFamily = YouTubeSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.categoryName} • ${item.subtitle}",
                                    fontFamily = YouTubeSansFontFamily,
                                    fontSize = 13.sp,
                                    color = SpotifyTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = SpotifyTextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // Card / Header do App (limpo e objetivo)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(SpotifyCardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Nerf Music",
                            tint = SpotifyGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Nerf Music",
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, SpotifyTextSecondary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "Música sem limites • Livre de anúncios",
                            fontFamily = YouTubeSansFontFamily,
                            fontSize = 12.sp,
                            color = SpotifyTextSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Cartões de acesso rápido em grade de 2 colunas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickAccessCard(
                        icon = if (dataSaverMode != DataSaverMode.ALWAYS_OFF) Icons.Default.DataSaverOn else Icons.Default.DataSaverOff,
                        title = "Economia de dados",
                        status = dataSaverMode.displayName,
                        onClick = onOpenDataSaver,
                        modifier = Modifier.weight(1f)
                    )

                    QuickAccessCard(
                        icon = Icons.Default.GraphicEq,
                        title = "Qualidade do áudio",
                        status = audioQuality.displayName,
                        onClick = onOpenMediaQuality,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de categorias de navegação hierárquica (padrão Spotify)
                SpotifyCategoryRow(
                    icon = Icons.Default.Savings,
                    title = "Economia de dados",
                    subtitle = "Modo economia de dados • Downloads via dados móveis",
                    onClick = onOpenDataSaver
                )

                SpotifyCategoryRow(
                    icon = Icons.Default.Tune,
                    title = "Qualidade da mídia",
                    subtitle = "Qualidade no Wi-Fi • Qualidade em dados móveis",
                    onClick = onOpenMediaQuality
                )

                SpotifyCategoryRow(
                    icon = Icons.Default.PlayCircleOutline,
                    title = "Reprodução",
                    subtitle = "Crossfade • Sem pausas • Equalizador • Sleep timer",
                    onClick = onOpenPlayback
                )

                SpotifyCategoryRow(
                    icon = Icons.Default.SmartDisplay,
                    title = "Conteúdo e exibição",
                    subtitle = "Canvas • Canvas no Wi-Fi • Letras sincronizadas",
                    onClick = onOpenContentDisplay
                )

                SpotifyCategoryRow(
                    icon = Icons.Default.Storage,
                    title = "Armazenamento",
                    subtitle = "Espaço ocupado • Limpar cache do app",
                    onClick = onOpenStorage
                )

                SpotifyCategoryRow(
                    icon = Icons.Default.CloudSync,
                    title = "Importar biblioteca e migração",
                    subtitle = "Migre do Spotify, YouTube Music ou arquivos de backup",
                    onClick = onOpenMigration
                )

                SpotifyCategoryRow(
                    icon = Icons.Default.Info,
                    title = "Sobre",
                    subtitle = "Versão do aplicativo • Change Log",
                    onClick = onOpenAbout
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * Sub-tela de Armazenamento (Storage):
 * Gráfico de barra proporcional estilo Spotify, detalhamento por categoria e ação de Limpar Cache
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorageSettingsScreen(
    breakdown: StorageBreakdown?,
    isSmartDownloadsEnabled: Boolean,
    onSmartDownloadsEnabledChange: (Boolean) -> Unit,
    smartDownloadsLimit: Int,
    onSmartDownloadsLimitChange: (Int) -> Unit,
    isSmartDownloadsWifiOnly: Boolean,
    onSmartDownloadsWifiOnlyChange: (Boolean) -> Unit,
    syncState: SmartDownloadSyncState,
    onTriggerSyncNow: () -> Unit,
    onSyncDownloads: (onDone: () -> Unit) -> Unit = {},
    onClearCache: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        containerColor = SpotifyBlack,
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Armazenamento",
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.padding(end = 48.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpotifyBlack)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SpotifySectionHeader(title = "Uso de armazenamento")

            val cacheSize = breakdown?.cacheSizeBytes ?: 0L
            val downloadsSize = breakdown?.downloadsSizeBytes ?: 0L
            val freeSpace = breakdown?.freeSpaceBytes ?: 1L
            val totalSpace = breakdown?.totalDeviceSizeBytes?.coerceAtLeast(1L) ?: 1L

            // Barra visual de armazenamento proporcional (estilo Spotify)
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(SpotifyStorageFree)
                ) {
                    val cacheFraction = (cacheSize.toFloat() / totalSpace.toFloat()).coerceIn(0.01f, 0.4f)
                    val downloadsFraction = (downloadsSize.toFloat() / totalSpace.toFloat()).coerceIn(0.01f, 0.4f)

                    Box(
                        modifier = Modifier
                            .weight(downloadsFraction)
                            .fillMaxHeight()
                            .background(SpotifyStorageDownloads)
                    )
                    Box(
                        modifier = Modifier
                            .weight(cacheFraction)
                            .fillMaxHeight()
                            .background(SpotifyStorageCache)
                    )
                    Box(
                        modifier = Modifier
                            .weight((1f - cacheFraction - downloadsFraction).coerceAtLeast(0.1f))
                            .fillMaxHeight()
                            .background(SpotifyStorageFree)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Legenda de cores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StorageLegendItem(color = SpotifyStorageDownloads, label = "Downloads")
                    StorageLegendItem(color = SpotifyStorageCache, label = "Cache")
                    StorageLegendItem(color = SpotifyStorageFree, label = "Espaço livre")
                }
            }

            HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

            // Detalhes numéricos
            StorageDetailRow(
                title = "Músicas baixadas offline",
                size = StorageUtils.formatFileSize(downloadsSize),
                dotColor = SpotifyStorageDownloads
            )

            StorageDetailRow(
                title = "Cache temporário",
                size = StorageUtils.formatFileSize(cacheSize),
                dotColor = SpotifyStorageCache
            )

            StorageDetailRow(
                title = "Espaço livre no dispositivo",
                size = StorageUtils.formatFileSize(freeSpace),
                dotColor = SpotifyStorageFree
            )

            Spacer(modifier = Modifier.height(24.dp))

            var isSyncingDownloads by remember { mutableStateOf(false) }

            // Botão Sincronizar Downloads (Reconciliação e recuperação de arquivos órfãos)
            Button(
                onClick = {
                    if (!isSyncingDownloads) {
                        isSyncingDownloads = true
                        onSyncDownloads {
                            isSyncingDownloads = false
                        }
                    }
                },
                enabled = !isSyncingDownloads,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyCardBg,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isSyncingDownloads) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = SpotifyGreen
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sincronizando...",
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sincronizar Downloads",
                        tint = SpotifyGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sincronizar Downloads",
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Verifica o armazenamento local e recupera faixas baixadas não listadas na biblioteca.",
                fontFamily = YouTubeSansFontFamily,
                fontSize = 12.sp,
                color = SpotifyTextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Botão Limpar Cache (estilo Spotify)
            Button(
                onClick = onClearCache,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyCardBg,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Limpar cache",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Seus downloads e músicas salvas para reprodução offline não serão removidos.",
                fontFamily = YouTubeSansFontFamily,
                fontSize = 12.sp,
                color = SpotifyTextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            HorizontalDivider(
                color = Color(0x1AFFFFFF),
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 18.dp)
            )

            SpotifySectionHeader(title = "Downloads Inteligentes (Smart Downloads)")

            Text(
                text = "Baixa automaticamente suas músicas favoritas e novidades semanais para que você nunca fique sem música offline.",
                fontFamily = YouTubeSansFontFamily,
                fontSize = 13.sp,
                color = SpotifyTextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SpotifySwitchItem(
                title = "Ativar Smart Downloads",
                subtitle = "Mantém uma seleção offline atualizada automaticamente",
                checked = isSmartDownloadsEnabled,
                onCheckedChange = onSmartDownloadsEnabledChange
            )

            if (isSmartDownloadsEnabled) {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Cota de músicas offline",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Chips de cota: 50, 100, 250, 500
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50, 100, 250, 500).forEach { quota ->
                        val isSelected = smartDownloadsLimit == quota
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSmartDownloadsLimitChange(quota) },
                            label = {
                                Text(
                                    text = "$quota",
                                    fontFamily = YouTubeSansFontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpotifyGreen,
                                selectedLabelColor = Color.Black,
                                containerColor = SpotifyCardBg,
                                labelColor = Color.White
                            ),
                            border = null,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Espaço estimado: ~${SmartDownloadManager.formatStorageEstimate(smartDownloadsLimit)} (Padrão Lossless)",
                    fontFamily = YouTubeSansFontFamily,
                    fontSize = 12.sp,
                    color = SpotifyTextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                SpotifySwitchItem(
                    title = "Apenas no Wi-Fi",
                    subtitle = "Sincroniza os downloads inteligentes sem gastar dados móveis",
                    checked = isSmartDownloadsWifiOnly,
                    onCheckedChange = onSmartDownloadsWifiOnlyChange
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Card de status da sincronização
                Surface(
                    color = SpotifyCardBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (syncState.isSyncing) "Sincronizando faixas..." else "Mix Offline pronto",
                                    fontFamily = YouTubeSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (syncState.isSyncing) {
                                        syncState.currentTrackTitle ?: "Conectando ao catálogo..."
                                    } else {
                                        "${syncState.currentDownloadedCount} de ${smartDownloadsLimit} faixas disponíveis"
                                    },
                                    fontFamily = YouTubeSansFontFamily,
                                    fontSize = 12.sp,
                                    color = SpotifyTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = onTriggerSyncNow,
                                enabled = !syncState.isSyncing
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sincronizar agora",
                                    tint = if (syncState.isSyncing) SpotifyTextSecondary else SpotifyGreen
                                )
                            }
                        }

                        if (syncState.isSyncing) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { syncState.currentTrackProgress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = SpotifyGreen,
                                trackColor = SpotifyTrackInactive
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StorageLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontFamily = YouTubeSansFontFamily,
            fontSize = 12.sp,
            color = SpotifyTextSecondary
        )
    }
}

@Composable
private fun StorageDetailRow(title: String, size: String, dotColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontFamily = YouTubeSansFontFamily,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = size,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = SpotifyTextSecondary
        )
    }
}

/**
 * Sub-tela de Economia de Dados (Data-saving and offline)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataSaverSettingsScreen(
    currentMode: DataSaverMode,
    onSelectMode: (DataSaverMode) -> Unit,
    isDownloadOverCellular: Boolean,
    onDownloadOverCellularChange: (Boolean) -> Unit,
    isCanvasWifiOnly: Boolean,
    onCanvasWifiOnlyChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        containerColor = SpotifyBlack,
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Economia de dados",
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.padding(end = 48.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpotifyBlack)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SpotifySectionHeader(title = "Modo de economia de dados")

            Text(
                text = "Escolha como deseja otimizar o uso de dados. Quando ativado, reduz a qualidade do streaming de áudio e desativa recursos de alto consumo em redes móveis.",
                fontFamily = YouTubeSansFontFamily,
                fontSize = 13.sp,
                color = SpotifyTextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            DataSaverMode.values().forEach { mode ->
                SpotifyRadioOptionItem(
                    title = mode.displayName,
                    subtitle = mode.description,
                    selected = currentMode == mode,
                    onSelect = { onSelectMode(mode) }
                )
            }

            SpotifySectionHeader(title = "Downloads e streaming")

            SpotifySwitchItem(
                title = "Downloads via dados móveis",
                subtitle = "Inicia ou continua downloads mesmo sem conexão Wi-Fi.",
                checked = isDownloadOverCellular,
                onCheckedChange = onDownloadOverCellularChange
            )

            SpotifySwitchItem(
                title = "Canvas apenas no Wi-Fi",
                subtitle = "Economiza dados móveis reproduzindo o Canvas somente conectado ao Wi-Fi.",
                checked = isCanvasWifiOnly,
                onCheckedChange = onCanvasWifiOnlyChange
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Sub-tela de Qualidade da Mídia (Media Quality)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaQualitySettingsScreen(
    wifiQuality: AudioQuality,
    onSelectWifiQuality: (AudioQuality) -> Unit,
    cellularQuality: AudioQuality,
    onSelectCellularQuality: (AudioQuality) -> Unit,
    downloadQuality: DownloadAudioQuality,
    onSelectDownloadQuality: (DownloadAudioQuality) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        containerColor = SpotifyBlack,
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Qualidade da mídia",
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.padding(end = 48.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpotifyBlack)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                color = SpotifyNoteBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "As alterações de qualidade entram em vigor na próxima faixa (a menos que a faixa já esteja em cache ou baixada no dispositivo).",
                    fontFamily = YouTubeSansFontFamily,
                    fontSize = 12.sp,
                    color = SpotifyTextSecondary,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            SpotifySectionHeader(title = "Qualidade do streaming no Wi-Fi")

            Text(
                text = "Escolha a qualidade da transmissão de áudio quando conectado à internet via Wi-Fi.",
                fontFamily = YouTubeSansFontFamily,
                fontSize = 13.sp,
                color = SpotifyTextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            AudioQuality.values().forEach { quality ->
                SpotifyRadioOptionItem(
                    title = quality.displayName,
                    subtitle = quality.bitrateLabel,
                    selected = wifiQuality == quality,
                    onSelect = { onSelectWifiQuality(quality) }
                )
            }

            SpotifySectionHeader(title = "Qualidade do streaming em dados móveis")

            Text(
                text = "Escolha a qualidade da transmissão de áudio ao usar rede de dados móveis.",
                fontFamily = YouTubeSansFontFamily,
                fontSize = 13.sp,
                color = SpotifyTextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            listOf(
                AudioQuality.AUTOMATIC,
                AudioQuality.LOW,
                AudioQuality.NORMAL,
                AudioQuality.HIGH
            ).forEach { quality ->
                SpotifyRadioOptionItem(
                    title = quality.displayName,
                    subtitle = quality.bitrateLabel,
                    selected = cellularQuality == quality,
                    onSelect = { onSelectCellularQuality(quality) }
                )
            }

            SpotifySectionHeader(title = "Qualidade dos downloads")

            Text(
                text = "Escolha a fidelidade das faixas salvas para reprodução offline e Smart Downloads. O padrão Lossless extrai o áudio bruto sem compressão redutora.",
                fontFamily = YouTubeSansFontFamily,
                fontSize = 13.sp,
                color = SpotifyTextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            DownloadAudioQuality.values().forEach { quality ->
                SpotifyRadioOptionItem(
                    title = quality.displayName,
                    subtitle = quality.description,
                    selected = downloadQuality == quality,
                    onSelect = { onSelectDownloadQuality(quality) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Sub-tela de Reprodução (Playback):
 * Inclui Crossfade, Gapless, Automix, Equalizador nativo e Temporizador de sono
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackSettingsScreen(
    crossfadeSeconds: Int,
    onCrossfadeChange: (Int) -> Unit,
    isGaplessEnabled: Boolean,
    onGaplessChange: (Boolean) -> Unit,
    isAutomixEnabled: Boolean,
    onAutomixChange: (Boolean) -> Unit,
    sleepTimerRemainingSeconds: Long?,
    sleepTimerAtEndOfTrack: Boolean,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        containerColor = SpotifyBlack,
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Reprodução",
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.padding(end = 48.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpotifyBlack)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SpotifySectionHeader(title = "Transições de faixas")

            SpotifySliderItem(
                title = "Crossfade",
                subtitle = "Permite fazer crossfade entre músicas",
                value = crossfadeSeconds,
                valueRange = 0f..12f,
                steps = 11,
                minLabel = "0s",
                maxLabel = "12s",
                onValueChange = onCrossfadeChange
            )

            SpotifySwitchItem(
                title = "Reprodução sem pausas",
                subtitle = "Elimina pausas e silêncio entre faixas consecutivas.",
                checked = isGaplessEnabled,
                onCheckedChange = onGaplessChange
            )

            SpotifySwitchItem(
                title = "Automix",
                subtitle = "Permite transições fluidas e inteligentes entre as músicas de playlists.",
                checked = isAutomixEnabled,
                onCheckedChange = onAutomixChange
            )

            SpotifySectionHeader(title = "Controles de áudio")

            // Equalizador de som
            SpotifyActionItemWithIcon(
                title = "Equalizador",
                subtitle = "Abre o painel de equalização de áudio do dispositivo",
                trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                onClick = onOpenEqualizer
            )

            // Temporizador de sono (Sleep Timer)
            val sleepTimerStatusText = when {
                sleepTimerAtEndOfTrack -> "Pausará ao final desta música"
                sleepTimerRemainingSeconds != null -> {
                    val mins = sleepTimerRemainingSeconds / 60
                    val secs = sleepTimerRemainingSeconds % 60
                    "Pausará em %02d:%02d min".format(mins, secs)
                }
                else -> "Desativado"
            }

            SpotifyActionItemWithStatus(
                title = "Temporizador de sono",
                subtitle = "Pausa automaticamente a reprodução após o período escolhido",
                statusText = sleepTimerStatusText,
                isStatusActive = sleepTimerRemainingSeconds != null || sleepTimerAtEndOfTrack,
                onClick = onOpenSleepTimer
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Modal BottomSheet estilo Spotify para o Temporizador de Sono
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerBottomSheet(
    remainingSeconds: Long?,
    isEndOfTrack: Boolean,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfTrack: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SpotifyCardBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SpotifyTextSecondary) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Temporizador de sono",
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val timerOptions = listOf(5, 15, 30, 45, 60)
            timerOptions.forEach { mins ->
                val label = if (mins >= 60) "1 hora" else "$mins minutos"
                SleepTimerOptionRow(
                    label = label,
                    selected = remainingSeconds != null && (remainingSeconds + 30) / 60 == mins.toLong() && !isEndOfTrack,
                    onClick = { onSelectMinutes(mins) }
                )
            }

            SleepTimerOptionRow(
                label = "Fim da música atual",
                selected = isEndOfTrack,
                onClick = onSelectEndOfTrack
            )

            if (remainingSeconds != null || isEndOfTrack) {
                HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                SleepTimerOptionRow(
                    label = "Desativar temporizador",
                    selected = false,
                    isDestructive = true,
                    onClick = onCancelTimer
                )
            }
        }
    }
}

@Composable
private fun SleepTimerOptionRow(
    label: String,
    selected: Boolean,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp,
            color = when {
                selected -> SpotifyGreen
                isDestructive -> Color(0xFFFF5252)
                else -> Color.White
            },
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Sub-tela de Conteúdo e Exibição (Content and Display)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentDisplaySettingsScreen(
    isCanvasEnabled: Boolean,
    onCanvasChange: (Boolean) -> Unit,
    isCanvasWifiOnly: Boolean,
    onCanvasWifiOnlyChange: (Boolean) -> Unit,
    isLyricsAboveTitleEnabled: Boolean,
    onLyricsAboveTitleChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        containerColor = SpotifyBlack,
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Conteúdo e exibição",
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.padding(end = 48.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpotifyBlack)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SpotifySectionHeader(title = "Vídeos e Canvas")

            SpotifySwitchItem(
                title = "Canvas",
                subtitle = "Exibe efeitos visuais curtos em loop nas músicas.",
                checked = isCanvasEnabled,
                onCheckedChange = onCanvasChange
            )

            SpotifySwitchItem(
                title = "Canvas apenas no Wi-Fi",
                subtitle = "Economiza dados móveis reproduzindo o Canvas somente conectado ao Wi-Fi.",
                checked = isCanvasWifiOnly,
                enabled = isCanvasEnabled,
                onCheckedChange = onCanvasWifiOnlyChange
            )

            SpotifySectionHeader(title = "Letras")

            SpotifySwitchItem(
                title = "Letras sincronizadas no Player",
                subtitle = "Exibe a frase da letra em tempo real logo acima do título da faixa.",
                checked = isLyricsAboveTitleEnabled,
                onCheckedChange = onLyricsAboveTitleChange
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Sub-tela Sobre (About)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSettingsScreen(
    onCheckUpdates: () -> Unit,
    onShowChangeLog: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        containerColor = SpotifyBlack,
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sobre",
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.padding(end = 48.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpotifyBlack)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SpotifySectionHeader(title = "Sobre o Nerf Music")

            SpotifyActionItem(
                title = "Versão do aplicativo",
                subtitle = "Nerf Music v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                onClick = onCheckUpdates
            )

            SpotifyActionItem(
                title = "Novidades da versão (Change Log)",
                subtitle = "Relembre os novos recursos adicionados nesta versão",
                onClick = onShowChangeLog
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Título de seção em sub-telas do Spotify (negrito, branco, espaçamento característico)
 */
@Composable
private fun SpotifySectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = YouTubeSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = Color.White,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

/**
 * Item de opção com seleção estilo rádio / checkmark verde do Spotify
 */
@Composable
private fun SpotifyRadioOptionItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (selected) SpotifyGreen else Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = YouTubeSansFontFamily,
                fontSize = 13.sp,
                color = SpotifyTextSecondary,
                lineHeight = 17.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selecionado",
                tint = SpotifyGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Cartão de acesso rápido (Quick-Access Tile) do Spotify
 */
@Composable
private fun QuickAccessCard(
    icon: ImageVector,
    title: String,
    status: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(78.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SpotifyCardBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = status,
                    fontFamily = YouTubeSansFontFamily,
                    fontSize = 11.sp,
                    color = SpotifyTextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Item de categoria do Spotify (ícone, título, subtítulo com bullet points, chevron à direita)
 */
@Composable
private fun SpotifyCategoryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SpotifyTextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = YouTubeSansFontFamily,
                fontSize = 13.sp,
                color = SpotifyTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SpotifyTextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Item de configuração com Switch no estilo oficial do Spotify
 */
@Composable
private fun SpotifySwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (enabled) Color.White else SpotifyTextSecondary.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = YouTubeSansFontFamily,
                fontSize = 13.sp,
                color = if (enabled) SpotifyTextSecondary else SpotifyTextSecondary.copy(alpha = 0.3f),
                lineHeight = 17.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SpotifyGreen,
                uncheckedThumbColor = SpotifyTextSecondary,
                uncheckedTrackColor = SpotifyTrackInactive
            )
        )
    }
}

/**
 * Item de Slider no estilo Spotify (Crossfade)
 */
@Composable
private fun SpotifySliderItem(
    title: String,
    subtitle: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    minLabel: String = "0s",
    maxLabel: String = "12s",
    enabled: Boolean = true,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (enabled) Color.White else SpotifyTextSecondary.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = YouTubeSansFontFamily,
                    fontSize = 13.sp,
                    color = if (enabled) SpotifyTextSecondary else SpotifyTextSecondary.copy(alpha = 0.3f),
                    lineHeight = 17.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (value == 0) "Desativado" else "${value}s",
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (value > 0) SpotifyGreen else SpotifyTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = SpotifyGreen,
                activeTrackColor = SpotifyGreen,
                inactiveTrackColor = SpotifyTrackInactive
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = minLabel,
                fontFamily = YouTubeSansFontFamily,
                fontSize = 11.sp,
                color = SpotifyTextSecondary.copy(alpha = 0.6f)
            )
            Text(
                text = maxLabel,
                fontFamily = YouTubeSansFontFamily,
                fontSize = 11.sp,
                color = SpotifyTextSecondary.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Item clicável com badge de status dinâmico (ex: Temporizador de sono)
 */
@Composable
private fun SpotifyActionItemWithStatus(
    title: String,
    subtitle: String,
    statusText: String,
    isStatusActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = YouTubeSansFontFamily,
                fontSize = 13.sp,
                color = SpotifyTextSecondary,
                lineHeight = 17.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = statusText,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = if (isStatusActive) SpotifyGreen else SpotifyTextSecondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SpotifyTextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Item clicável de ação com ícone final específico
 */
@Composable
private fun SpotifyActionItemWithIcon(
    title: String,
    subtitle: String,
    trailingIcon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = YouTubeSansFontFamily,
                fontSize = 13.sp,
                color = SpotifyTextSecondary,
                lineHeight = 17.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = SpotifyTextSecondary.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Item clicável de ação padrão em sub-tela do Spotify
 */
@Composable
private fun SpotifyActionItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = YouTubeSansFontFamily,
                fontSize = 13.sp,
                color = SpotifyTextSecondary,
                lineHeight = 17.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SpotifyTextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
