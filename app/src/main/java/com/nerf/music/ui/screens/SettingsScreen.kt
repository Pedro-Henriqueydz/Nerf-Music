package com.nerf.music.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerf.music.BuildConfig
import com.nerf.music.data.repository.AudioQuality
import com.nerf.music.data.repository.DataSaverMode
import com.nerf.music.ui.theme.*
import com.nerf.music.ui.viewmodel.MusicViewModel

/**
 * Cores de estilo idênticas ao design do Spotify 9.1.94
 */
private val SpotifyBlack = Color(0xFF121212)
private val SpotifyCardBg = Color(0xFF242424)
private val SpotifyGreen = Color(0xFF1ED760)
private val SpotifyTextSecondary = Color(0xFFB3B3B3)
private val SpotifyTrackInactive = Color(0xFF4D4D4D)
private val SpotifyNoteBg = Color(0xFF1E1E1E)

/**
 * Subtelas de navegação das configurações hierárquicas (padrão Spotify).
 */
enum class SettingsSubScreen {
    MAIN,
    DATA_SAVER,
    MEDIA_QUALITY,
    PLAYBACK,
    CONTENT_DISPLAY,
    ABOUT
}

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

    // Intercepta botão Voltar do sistema Android quando em uma sub-tela
    BackHandler(enabled = currentSubScreen != SettingsSubScreen.MAIN) {
        currentSubScreen = SettingsSubScreen.MAIN
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
                    onNavigateBack = onNavigateBack,
                    onOpenDataSaver = { currentSubScreen = SettingsSubScreen.DATA_SAVER },
                    onOpenMediaQuality = { currentSubScreen = SettingsSubScreen.MEDIA_QUALITY },
                    onOpenPlayback = { currentSubScreen = SettingsSubScreen.PLAYBACK },
                    onOpenContentDisplay = { currentSubScreen = SettingsSubScreen.CONTENT_DISPLAY },
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
 * Tela Principal de Configurações do Spotify:
 * - TopBar centralizada com pesquisa
 * - Header de Perfil / App com avatar circular e tag de status (sem botões redundantes como "ver perfil")
 * - Grade de 2 colunas com cartões de acesso rápido funcionais
 * - Lista de categorias de navegação hierárquica com ícones e subtítulos separados por pontos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMainContent(
    dataSaverMode: DataSaverMode,
    audioQuality: AudioQuality,
    onNavigateBack: () -> Unit,
    onOpenDataSaver: () -> Unit,
    onOpenMediaQuality: () -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenContentDisplay: () -> Unit,
    onOpenAbout: () -> Unit,
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
                    IconButton(onClick = { /* Pesquisa de configurações */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Pesquisar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpotifyBlack
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Card / Header do App (sem conta online, com foco no Nerf Music)
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
                subtitle = "Crossfade • Sem pausas • Automix",
                onClick = onOpenPlayback
            )

            SpotifyCategoryRow(
                icon = Icons.Default.SmartDisplay,
                title = "Conteúdo e exibição",
                subtitle = "Canvas • Canvas no Wi-Fi • Letras sincronizadas",
                onClick = onOpenContentDisplay
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

/**
 * Sub-tela de Economia de Dados (Data-saving and offline):
 * Idêntica ao Spotify (Modo economia de dados com opções, Downloads via dados móveis, Canvas no Wi-Fi)
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpotifyBlack
                )
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
 * Sub-tela de Qualidade da Mídia (Media Quality):
 * Idêntica ao Spotify (Aviso de alteração na próxima faixa, qualidade no Wi-Fi e qualidade em dados móveis)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaQualitySettingsScreen(
    wifiQuality: AudioQuality,
    onSelectWifiQuality: (AudioQuality) -> Unit,
    cellularQuality: AudioQuality,
    onSelectCellularQuality: (AudioQuality) -> Unit,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpotifyBlack
                )
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
            // Nota explicativa estilo Spotify
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
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
 * Sub-tela de Reprodução (Playback):
 * Idêntica à página Playback do Spotify (Transições de faixas, Crossfade slider, Gapless, Automix)
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpotifyBlack
                )
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Sub-tela de Conteúdo e Exibição (Content and Display):
 * Idêntica à página Content and display do Spotify (Canvas, Canvas no Wi-Fi, Letras sincronizadas)
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpotifyBlack
                )
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
 * Sub-tela Sobre (About):
 * Exibe versão do aplicativo e acesso ao Change Log
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpotifyBlack
                )
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
 * Item de configuração com Switch no estilo oficial do Spotify:
 * - Título em destaque
 * - Descrição explicativa
 * - Switch com trilha verde Spotify (#1ED760) quando ativo
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
 * Item de Slider no estilo Spotify (Crossfade):
 * - Título e status atual ("Desativado" ou "${seconds}s" em verde Spotify)
 * - Slider com cor ativa verde Spotify e trilha inativa cinza
 * - Rótulos inferior esquerdo e direito (0s / 12s)
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
 * Item clicável de ação em sub-tela do Spotify
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
