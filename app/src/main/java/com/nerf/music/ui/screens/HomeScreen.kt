package com.nerf.music.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import com.nerf.music.ui.components.AnimatedEqualizerBars
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.nerf.music.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nerf.music.data.model.HomeChip
import com.nerf.music.data.model.HomeSection
import com.nerf.music.data.model.HomeSectionStyle
import com.nerf.music.data.model.Track
import com.nerf.music.ui.theme.*
import com.nerf.music.ui.viewmodel.MusicViewModel

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onOpenAlbum: (browseId: String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val homeChips by viewModel.homeChips.collectAsState()
    val selectedChip by viewModel.selectedHomeChip.collectAsState()
    val homeSections by viewModel.homeSections.collectAsState()
    val isLoading by viewModel.isLoadingHome.collectAsState()
    val homeError by viewModel.homeError.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

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
            // Top Navbar oficial estilo YouTube Music
            YtmTopBar()

            when {
                isLoading && homeSections.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = YtmWhite,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Carregando o seu feed...",
                                fontFamily = YouTubeSansFontFamily,
                                fontSize = 14.sp,
                                color = YtmTextSecondary
                            )
                        }
                    }
                }

                homeError != null && homeSections.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = homeError ?: "Erro ao carregar a Home",
                            fontFamily = YouTubeSansFontFamily,
                            color = ErrorColor,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadHomeFeed() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YtmWhite,
                                contentColor = YtmBlack
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tentar novamente",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // 1. Fileira de Chips de Humor e Atividade (chipCloudRenderer)
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

                        // 2. Seção "Ouvir novamente" (Speed Dial da sessão atual)
                        if (recentlyPlayed.isNotEmpty() && selectedChip == null) {
                            item {
                                YtmSectionHeader(
                                    title = "Ouvir novamente",
                                    strapline = "COM BASE NO QUE VOCÊ OUVIU",
                                    actionLabel = null,
                                    onActionClick = null
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(recentlyPlayed, key = { "recent_${it.id}" }) { track ->
                                        val isCurrent = track.id == playbackState.currentTrack?.id
                                        HomeSongCard(
                                            track = track,
                                            isCurrentTrack = isCurrent,
                                            isPlaying = isCurrent && playbackState.isPlaying,
                                            onClick = { viewModel.playTrack(track) }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Seções do YouTube Music (Quick Picks 4xN e Carrosséis TwoRow 155dp)
                        items(homeSections, key = { it.title }) { section ->
                            val isQuickPicks = section.style == HomeSectionStyle.QUICK_PICKS_GRID
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
                                actionLabel = if (isQuickPicks) "Reproduzir tudo" else null,
                                onActionClick = if (isQuickPicks) {
                                    { viewModel.playSection(section) }
                                } else null
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (isQuickPicks) {
                                YtmQuickPicksGridCarousel(
                                    tracks = section.items,
                                    currentPlayingId = playbackState.currentTrack?.id,
                                    isPlaybackActive = playbackState.isPlaying,
                                    onTrackClick = { track -> viewModel.playTrack(track) }
                                )
                            } else {
                                YtmTwoRowCarousel(
                                    tracks = section.items,
                                    currentPlayingId = playbackState.currentTrack?.id,
                                    isPlaybackActive = playbackState.isPlaying,
                                    onTrackClick = { track ->
                                        val albumId = track.browseId?.takeIf { it.isNotBlank() }
                                            ?: if (track.isAlbumOrPlaylist) track.id else null
                                        if (albumId != null) {
                                            onOpenAlbum(albumId)
                                        } else {
                                            viewModel.playTrack(track)
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

/**
 * Barra superior idêntica ao YouTube Music:
 * - Esquerda: Emblema circular vermelho com play branco + título "Music" em YouTube Sans Bold.
 * - Direita: Ícone Cast, Lupa de Busca (abre a SearchScreen) e Avatar do usuário.
 */
@Composable
fun YtmTopBar(
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
 * Grade horizontal de 4 faixas por coluna ("Quick Picks" / "Escolha a dedo" / "Em alta"),
 * replicando fielmente o `numItemsPerColumn = 4` do YouTube Music.
 */
@Composable
fun YtmQuickPicksGridCarousel(
    tracks: List<Track>,
    currentPlayingId: String?,
    isPlaybackActive: Boolean = false,
    onTrackClick: (Track) -> Unit,
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
                        onClick = { onTrackClick(track) }
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
            onClick = onClick,
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
 * Carrossel horizontal de cards grandes (155dp) para Álbuns, Mixes, Playlists e Vídeos
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
 * - Capa quadrada de 155dp com cantos arredondados de 8dp e botão Play sobreposto.
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
            .width(155.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(155.dp)
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
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }

            // Botão Play translúcido no canto inferior direito igual ao overlay do YT Music
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentTrack) {
                    AnimatedEqualizerBars(
                        isPlaying = isPlaying,
                        barColor = YtmWhite,
                        barCount = 3,
                        barWidth = 2.5.dp,
                        barSpacing = 1.5.dp,
                        maxHeight = 14.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = YtmWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = if (isCurrentTrack) OrangeSecondary else YtmWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = track.artist,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = YtmTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
