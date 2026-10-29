package com.nerf.music.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nerf.music.R
import com.nerf.music.data.model.HomeChip
import com.nerf.music.data.model.HomeSection
import com.nerf.music.data.model.HomeSectionStyle
import com.nerf.music.data.model.Track
import com.nerf.music.jam.JamSessionState
import com.nerf.music.ui.components.AnimatedEqualizerBars
import com.nerf.music.ui.components.JamPersonWavesIcon
import com.nerf.music.ui.theme.*
import com.nerf.music.ui.viewmodel.MusicViewModel
import com.nerf.music.ui.motion.rememberSmoothFlingBehavior
import com.nerf.music.util.ConnectivityObserver
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onOpenAlbum: (browseId: String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onTrackOptionsClick: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val smoothFlingBehavior = rememberSmoothFlingBehavior()
    val homeChips by viewModel.homeChips.collectAsState()
    val selectedChip by viewModel.selectedHomeChip.collectAsState()
    val homeSections by viewModel.homeSections.collectAsState()
    val isLoading by viewModel.isLoadingHome.collectAsState()
    val homeError by viewModel.homeError.collectAsState()
    val playbackIdentityFlow = remember(viewModel) {
        viewModel.playbackState
            .map { it.currentTrack?.id to it.isPlaying }
            .distinctUntilChanged()
    }
    val playbackIdentity by playbackIdentityFlow.collectAsState(initial = null to false)
    val currentPlayingId = playbackIdentity.first
    val isPlaybackActive = playbackIdentity.second
    val connectivityStatus by viewModel.connectivityStatus.collectAsState()
    val jamSessionState by viewModel.jamSessionState.collectAsState()

    val isOffline = connectivityStatus == ConnectivityObserver.Status.Unavailable ||
            connectivityStatus == ConnectivityObserver.Status.Lost
    val showOfflineCard = (isOffline || homeError != null) && homeSections.isEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YtmBlack)
    ) {
        // Gradiente atmosférico superior idêntico ao cabeçalho da Home do YouTube Music
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF38131A).copy(alpha = 0.65f),
                            Color(0xFF18121E).copy(alpha = 0.35f),
                            YtmBlack
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navbar oficial estilo YouTube Music + Botão rápido da Jam
            YtmTopBar(
                onOpenSettings = onOpenSettings,
                onOpenJam = { viewModel.openJamSheet() },
                jamSessionState = jamSessionState
            )

            when {
                isLoading && homeSections.isEmpty() && !isOffline -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = YtmWhite,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Carregando o seu feed...",
                                fontFamily = YouTubeSansFontFamily,
                                fontSize = 14.sp,
                                color = YtmTextSecondary
                            )
                        }
                    }
                }

                showOfflineCard -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF212121)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Offline",
                                    tint = YtmWhite,
                                    modifier = Modifier.size(44.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Você está off-line",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = YtmWhite,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "As músicas salvas estão disponíveis nos seus downloads. Conecte-se à internet para atualizar seu feed inicial.",
                                fontFamily = YouTubeSansFontFamily,
                                fontSize = 14.sp,
                                color = YtmTextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            Button(
                                onClick = onNavigateToDownloads,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = YtmWhite,
                                    contentColor = YtmBlack
                                ),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = YtmBlack
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ir para downloads",
                                    fontFamily = YouTubeSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = { viewModel.loadHomeFeed(force = true) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = YtmTextSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tentar novamente",
                                    fontFamily = YouTubeSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = YtmTextSecondary
                                )
                            }
                        }
                    }
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = isLoading && homeSections.isNotEmpty(),
                        onRefresh = { viewModel.loadHomeFeed(force = true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            flingBehavior = smoothFlingBehavior,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 110.dp),
                            verticalArrangement = Arrangement.spacedBy(28.dp)
                        ) {
                        // 1. Fileira de Chips de Humor e Atividade (Energia, Relax, Foco, etc.)
                        if (homeChips.isNotEmpty()) {
                            item {
                                YtmMoodChipsRow(
                                    chips = homeChips,
                                    selectedChip = selectedChip,
                                    onChipClick = { chip -> viewModel.selectHomeChip(chip) }
                                )
                            }
                        }

                        // Indicador sutil de atualização ao trocar de chip
                        if (isLoading) {
                            item {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp),
                                    color = YtmWhite,
                                    trackColor = Color.Transparent
                                )
                            }
                        }

                        // 2. Seções Dinâmicas Premium (Speed Dial 3x3, Seus Downloads, Descoberta Diária, Covers e Remixes, etc.)
                        items(homeSections, key = { it.title }) { section ->
                            when (section.style) {
                                HomeSectionStyle.SPEED_DIAL -> {
                                    HomeSpeedDialPlaybackSection(viewModel = viewModel, section = section)
                                }

                                HomeSectionStyle.YOUR_DOWNLOADS -> {
                                    YtmYourDownloadsSection(
                                        section = section,
                                        onTrackClick = { track ->
                                            viewModel.handleHomeTrackClick(track, section)
                                        }
                                    )
                                }

                                HomeSectionStyle.DAILY_DISCOVER -> {
                                    YtmDailyDiscoverHeroSection(
                                        section = section,
                                        onTrackClick = { track ->
                                            viewModel.handleHomeTrackClick(track, section)
                                        }
                                    )
                                }

                                HomeSectionStyle.QUICK_PICKS_GRID -> {
                                    val defaultStrapline = when {
                                        !section.strapline.isNullOrBlank() -> section.strapline
                                        section.title.contains("Escolha a dedo", ignoreCase = true) ->
                                            "COMECE UMA RÁDIO COM BASE EM UMA MÚSICA"
                                        section.title.contains("Em alta", ignoreCase = true) ->
                                            "AS MÚSICAS MAIS TOCADAS DO MOMENTO"
                                        else -> null
                                    }

                                    YtmSectionHeader(
                                        title = section.title,
                                        strapline = defaultStrapline,
                                        actionLabel = "Reproduzir tudo",
                                        onActionClick = { viewModel.playSection(section, startIndex = 0) }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    YtmQuickPicksGridCarousel(
                                        tracks = section.items,
                                        currentPlayingId = currentPlayingId,
                                        isPlaybackActive = isPlaybackActive,
                                        onTrackClick = { track ->
                                            val idx = section.items.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                            viewModel.playSection(section, startIndex = idx)
                                        },
                                        onTrackOptionsClick = onTrackOptionsClick
                                    )
                                }

                                HomeSectionStyle.TWO_ROW_CAROUSEL,
                                HomeSectionStyle.COMMUNITY_PLAYLIST -> {
                                    YtmSectionHeader(
                                        title = section.title,
                                        strapline = section.strapline,
                                        actionLabel = null,
                                        onActionClick = null
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    YtmTwoRowCarousel(
                                        tracks = section.items,
                                        currentPlayingId = currentPlayingId,
                                        isPlaybackActive = isPlaybackActive,
                                        onTrackClick = { track ->
                                            val albumId = track.browseId?.takeIf { it.isNotBlank() }
                                                ?: if (track.isAlbumOrPlaylist) track.id else null
                                            if (albumId != null) {
                                                onOpenAlbum(albumId)
                                            } else {
                                                viewModel.handleHomeTrackClick(track, section)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

}

@Composable
private fun HomeSpeedDialPlaybackSection(
    viewModel: MusicViewModel,
    section: HomeSection
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val progressFraction = if (playbackState.durationMs > 0L) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    YtmSpeedDialPagerSection(
        tracks = section.items,
        currentPlayingId = playbackState.currentTrack?.id,
        isPlaybackActive = playbackState.isPlaying,
        playbackProgressFraction = progressFraction,
        onTrackClick = { track -> viewModel.handleHomeTrackClick(track, section) }
    )
}

/**
 * Barra superior idêntica ao YouTube Music:
 * - Esquerda: Emblema circular vermelho com play branco + título "Music" em YouTube Sans Bold.
 * - Direita: Ícone de Configurações.
 */
@Composable
fun YtmTopBar(
    onOpenSettings: (() -> Unit)? = null,
    onOpenJam: (() -> Unit)? = null,
    jamSessionState: JamSessionState? = null,
    modifier: Modifier = Modifier
) {
    val isInJam = jamSessionState?.isInJam == true
    val jamGreen = Color(0xFF1ED760)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo NERF Music
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_nerf_logo),
                contentDescription = "NERF Music Logo",
                modifier = Modifier
                    .height(26.dp)
                    .width(42.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Music",
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp,
                letterSpacing = (-0.5).sp,
                color = YtmWhite
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (onOpenJam != null) {
                if (isInJam) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = jamGreen.copy(alpha = 0.22f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, jamGreen.copy(alpha = 0.65f), RoundedCornerShape(50))
                            .clickable(onClick = onOpenJam)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            JamPersonWavesIcon(
                                tint = jamGreen,
                                size = 18.dp
                            )
                            Text(
                                text = "JAM • ${jamSessionState?.participants?.size ?: 1}",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = jamGreen
                            )
                        }
                    }
                } else {
                    IconButton(onClick = onOpenJam) {
                        JamPersonWavesIcon(
                            tint = YtmWhite,
                            size = 24.dp
                        )
                    }
                }
            }

            if (onOpenSettings != null) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurações",
                        tint = YtmWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Fileira horizontal de chips de humor/atividade (Energia, Relax, Para treinar, Foco, etc.)
 * com as dimensões exatas extraídas do APK do YouTube Music (14sp, raio 8dp, fundo #1AFFFFFF).
 */
@Composable
fun YtmMoodChipsRow(
    chips: List<HomeChip>,
    selectedChip: HomeChip?,
    onChipClick: (HomeChip) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips, key = { it.title }) { chip ->
            val isSelected = selectedChip?.params == chip.params
            Surface(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onChipClick(chip) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) YtmWhite else YtmWhite10Pct,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, YtmDivider)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chip.title,
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (isSelected) YtmBlack else YtmWhite,
                        maxLines = 1
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpar filtro",
                            tint = YtmBlack,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cabeçalho de seção estilo YouTube Music (`music_carousel_shelf_basic_header`).
 */
@Composable
fun YtmSectionHeader(
    title: String,
    strapline: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (!strapline.isNullOrBlank()) {
                Text(
                    text = strapline.uppercase(),
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    letterSpacing = 0.4.sp,
                    color = YtmTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = title,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = YtmWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (actionLabel != null && onActionClick != null) {
            Surface(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onActionClick() },
                shape = RoundedCornerShape(50),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, YtmWhite20Pct)
            ) {
                Text(
                    text = actionLabel,
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = YtmWhite,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * SPEED DIAL OFICIAL DO YOUTUBE MUSIC (Paginado 3x3):
 * 3 colunas x 3 linhas = 9 cartões por página com dots indicadores.
 * Contém playlists do usuário, atalho de curtidas e faixas recentes com alta afinidade.
 */
@Composable
fun YtmSpeedDialPagerSection(
    tracks: List<Track>,
    currentPlayingId: String?,
    isPlaybackActive: Boolean,
    playbackProgressFraction: Float,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = remember(tracks) { tracks.chunked(9) }
    if (pages.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { pageIndex ->
            val pageTracks = pages[pageIndex]
            val rows = remember(pageTracks) { pageTracks.chunked(3) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rows.forEach { rowTracks ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTracks.forEach { track ->
                            val isCurrent = track.id == currentPlayingId
                            YtmSpeedDialCard(
                                track = track,
                                isCurrentTrack = isCurrent,
                                isPlaying = isCurrent && isPlaybackActive,
                                progressFraction = if (isCurrent) playbackProgressFraction else 0f,
                                onClick = { onTrackClick(track) },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        }
                        // Preenche espaços vazios na última linha se houver menos de 3 itens
                        if (rowTracks.size < 3) {
                            repeat(3 - rowTracks.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Indicador de Dots de Paginação (se houver mais de uma página)
        if (pages.size > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(
                                width = if (isSelected) 14.dp else 6.dp,
                                height = 6.dp
                            )
                            .clip(CircleShape)
                            .background(if (isSelected) YtmWhite else YtmWhite30Pct)
                    )
                }
            }
        }
    }
}

/**
 * Cartão quadrado individual do Speed Dial (9 por página):
 * - Capa inteira ou gradiente vibrante se for playlist de curtidas.
 * - Sombra em gradiente na base.
 * - Título em branco (12sp SemiBold).
 * - Borda branca de 2dp se estiver tocando no momento.
 * - Mini equalizador animado no canto superior direito.
 * - Barra de progresso vermelha na base se estiver parcialmente tocada.
 */
@Composable
fun YtmSpeedDialCard(
    track: Track,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    progressFraction: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLikedShortcut = track.sourceIdentifier == "SHORTCUT_LIKED" || track.id == "shortcut_liked_music"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isCurrentTrack) {
                    Modifier.border(2.dp, YtmWhite, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    ) {
        when {
            isLikedShortcut -> {
                // Gradiente característico de músicas curtidas do YouTube Music
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF8A2387),
                                    Color(0xFFE94057),
                                    Color(0xFFF27121)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Curtidas",
                        tint = YtmWhite.copy(alpha = 0.4f),
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
            !track.thumbnailUrl.isNullOrEmpty() -> {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(YtmGrey11),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = YtmIconInactive,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Gradiente escuro para legibilidade perfeita do texto
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.90f)
                        )
                    )
                )
        )

        // Mini Equalizador animado no topo à direita se estiver tocando
        if (isCurrentTrack) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedEqualizerBars(
                    isPlaying = isPlaying,
                    barColor = YtmWhite,
                    barCount = 3,
                    barWidth = 2.dp,
                    barSpacing = 1.dp,
                    maxHeight = 10.dp
                )
            }
        }

        // Título da faixa / playlist no rodapé do card
        Text(
            text = track.title,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = YtmWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )

        // Barra de progresso vermelha na base (estilo oficial YTM)
        if (progressFraction > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progressFraction)
                    .height(3.dp)
                    .background(YtmRed)
            )
        }
    }
}

/**
 * SEÇÃO "SEUS DOWNLOADS" (Gradientes Oficiais Liked Music & Offline Mix):
 * Replicando com fidelidade visual os cartões imersivos de downloads do YouTube Music 9.41.
 */
@Composable
fun YtmYourDownloadsSection(
    section: HomeSection,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        YtmSectionHeader(
            title = section.title,
            strapline = section.strapline ?: "DISPONÍVEIS OFFLINE"
        )
        Spacer(modifier = Modifier.height(14.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(section.items, key = { it.id }) { item ->
                val isLiked = item.id.contains("liked", ignoreCase = true)
                YtmDownloadCard(
                    title = item.title,
                    subtitle = item.durationText.ifBlank { item.artist },
                    isLikedMusic = isLiked,
                    onClick = { onTrackClick(item) }
                )
            }
        }
    }
}

@Composable
fun YtmDownloadCard(
    title: String,
    subtitle: String,
    isLikedMusic: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = if (isLikedMusic) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF8A2387),
                Color(0xFFE94057),
                Color(0xFFF27121)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF4A00E0),
                Color(0xFF8E2DE2)
            )
        )
    }

    Box(
        modifier = modifier
            .width(165.dp)
            .height(205.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        // Ícone ilustrativo sutil centralizado
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isLikedMusic) Icons.Default.ThumbUp else Icons.Default.Download,
                contentDescription = null,
                tint = YtmWhite.copy(alpha = 0.28f),
                modifier = Modifier.size(54.dp)
            )
        }

        // Textos inferiores
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(end = 40.dp)
        ) {
            Text(
                text = title,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = YtmWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = YtmWhite.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Botão Play flutuante no canto inferior direito
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Reproduzir",
                tint = YtmWhite,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * HERO CARD "SUA DESCOBERTA DIÁRIA" (Your Daily Discover):
 * Visual nobre com o subtítulo em itálico "Parecido com X de Y" e artwork imersivo.
 */
@Composable
fun YtmDailyDiscoverHeroSection(
    section: HomeSection,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val track = section.items.firstOrNull() ?: return

    Column(modifier = modifier.fillMaxWidth()) {
        YtmSectionHeader(
            title = section.title,
            strapline = section.strapline ?: "ESCOLHA ESPECIAL DE HOJE"
        )
        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(210.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onTrackClick(track) }
        ) {
            // Capa de fundo
            val artworkUrl = track.highResThumbnailUrl ?: track.thumbnailUrl
            if (!artworkUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E28))
                )
            }

            // Gradiente escuro para legibilidade
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.50f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            // Badge superior "NOVA DESCOBERTA"
            Surface(
                modifier = Modifier
                    .padding(14.dp)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, YtmWhite20Pct)
            ) {
                Text(
                    text = section.heroCategory ?: "NOVA DESCOBERTA",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = YtmWhite,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Conteúdo inferior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = track.title,
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = YtmWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = YtmTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!section.heroSubtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = section.heroSubtitle,
                            fontFamily = YouTubeSansFontFamily,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = Color(0xFFFFA726),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Botão Play branco sólido característico
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(YtmWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Tocar descoberta",
                        tint = YtmBlack,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * Grade horizontal de 4 faixas por coluna ("Quick Picks" / "Escolha a dedo" / "Covers e remixes"),
 * replicando fielmente o `numItemsPerColumn = 4` do YouTube Music.
 */
@Composable
fun YtmQuickPicksGridCarousel(
    tracks: List<Track>,
    currentPlayingId: String?,
    isPlaybackActive: Boolean = false,
    onTrackClick: (Track) -> Unit,
    onTrackOptionsClick: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val columns = remember(tracks) { tracks.chunked(4) }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(columns) { columnTracks ->
            Column(
                modifier = Modifier.width(310.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                columnTracks.forEach { track ->
                    val isCurrent = track.id == currentPlayingId
                    YtmQuickPickRowItem(
                        track = track,
                        isCurrentTrack = isCurrent,
                        isPlaying = isCurrent && isPlaybackActive,
                        onClick = { onTrackClick(track) },
                        onOptionsClick = { onTrackOptionsClick(track) }
                    )
                }
            }
        }
    }
}

@Composable
fun YtmQuickPickRowItem(
    track: Track,
    isCurrentTrack: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail 52x52dp com cantos de 4dp (padrão music_responsive_list_item)
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(YtmGrey11),
            contentAlignment = Alignment.Center
        ) {
            val artworkUrl = track.highResThumbnailUrl ?: track.thumbnailUrl
            if (!artworkUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = YtmIconInactive,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isCurrentTrack) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedEqualizerBars(
                        isPlaying = isPlaying,
                        barColor = YtmWhite,
                        barCount = 3,
                        barWidth = 2.5.dp,
                        barSpacing = 1.5.dp,
                        maxHeight = 15.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Título e Artista
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (isCurrentTrack) OrangeSecondary else YtmWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val subtitleText = if (track.durationText != "--:--" && track.durationText.isNotBlank()) {
                "${track.artist} • ${track.durationText}"
            } else {
                track.artist
            }
            Text(
                text = subtitleText,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = YtmTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onOptionsClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opções",
                tint = YtmIconInactive,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Carrossel horizontal de cards grandes (116dp-155dp) para Álbuns, Mixes, Playlists e Vídeos
 * (`music_two_row_item` do APK do YouTube Music).
 */
@Composable
fun YtmTwoRowCarousel(
    tracks: List<Track>,
    currentPlayingId: String?,
    isPlaybackActive: Boolean = false,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(tracks, key = { it.id }) { track ->
            val isCurrent = track.id == currentPlayingId
            HomeSongCard(
                track = track,
                isCurrentTrack = isCurrent,
                isPlaying = isCurrent && isPlaybackActive,
                onClick = { onTrackClick(track) }
            )
        }
    }
}

/**
 * Card `music_two_row_item` oficial do YouTube Music:
 * - Capa quadrada de 116dp com cantos arredondados de 8dp e botão Play sobreposto.
 * - Título em YouTube Sans Bold/SemiBold e subtítulo cinza (#AAAAAA) abaixo.
 */
@Composable
fun HomeSongCard(
    track: Track,
    isCurrentTrack: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(116.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(116.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(YtmGrey11)
        ) {
            val artworkUrl = track.highResThumbnailUrl ?: track.thumbnailUrl
            if (!artworkUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = YtmIconInactive,
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )
            }

            // Botão Play translúcido no canto inferior direito igual ao overlay do YT Music
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentTrack) {
                    AnimatedEqualizerBars(
                        isPlaying = isPlaying,
                        barColor = YtmWhite,
                        barCount = 3,
                        barWidth = 2.dp,
                        barSpacing = 1.dp,
                        maxHeight = 12.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = YtmWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = track.title,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = YtmWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = track.artist,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = YtmTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
