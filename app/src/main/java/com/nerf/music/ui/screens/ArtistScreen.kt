package com.nerf.music.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import com.nerf.music.ui.components.AnimatedEqualizerBars
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nerf.music.data.model.Artist
import com.nerf.music.data.model.ArtistDetail
import com.nerf.music.data.model.Track
import com.nerf.music.ui.theme.*
import com.nerf.music.ui.viewmodel.MusicViewModel

/**
 * Tela oficial de Perfil de Artista inspirada no YouTube Music e Spotify:
 * 1. Banner Imersivo com Efeito Parallax suave e gradiente para preto puro (#000000).
 * 2. Top songs (As 5 mais tocadas com numeração e capa).
 * 3. Albums (Carrossel de álbuns de estúdio).
 * 4. Singles & EPs (Carrossel de lançamentos rápidos).
 * 5. Videos (Carrossel em formato widescreen 16:9).
 * 6. From your library (Faixas deste artista salvas no histórico local do usuário).
 * 7. Fans might also like (Artistas similares com foto circular).
 * 8. Sobre o Artista (Card de biografia expansível).
 */
@Composable
fun ArtistScreen(
    browseId: String,
    viewModel: MusicViewModel,
    onNavigateBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onTrackOptionsClick: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val artist by viewModel.artistDetail.collectAsState()
    val isLoading by viewModel.isLoadingArtist.collectAsState()
    val error by viewModel.artistError.collectAsState()
    val libraryTracks by viewModel.libraryTracksForArtist.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    LaunchedEffect(browseId) {
        if (browseId.isNotBlank()) {
            viewModel.loadArtist(browseId)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YtmBlack)
    ) {
        when {
            isLoading && artist == null -> {
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
                            text = "Carregando perfil do artista...",
                            fontFamily = YouTubeSansFontFamily,
                            fontSize = 14.sp,
                            color = YtmTextSecondary
                        )
                    }
                }
            }

            error != null && artist == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = error ?: "Erro ao carregar artista",
                            fontFamily = YouTubeSansFontFamily,
                            fontSize = 15.sp,
                            color = YtmTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadArtist(browseId) },
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
            }

            artist != null -> {
                val currentArtist = artist!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // 1. Cabeçalho Imersivo: Banner, Nome, Inscritos e Botão de Rádio
                    item {
                        ArtistHeroHeader(
                            artist = currentArtist,
                            onPlayRadio = {
                                val seed = currentArtist.radioVideoId
                                    ?: currentArtist.topSongs.firstOrNull()?.id
                                if (seed != null) {
                                    val seedTrack = currentArtist.topSongs.firstOrNull { it.id == seed }
                                        ?: Track(
                                            id = seed,
                                            title = "Rádio de ${currentArtist.name}",
                                            artist = currentArtist.name,
                                            thumbnailUrl = currentArtist.bannerUrl
                                        )
                                    viewModel.playTrack(seedTrack)
                                }
                            }
                        )
                    }

                    // 2. Top songs (Músicas mais tocadas)
                    if (currentArtist.topSongs.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                YtmSectionHeader(
                                    title = "Músicas mais tocadas",
                                    strapline = "TOP SONGS",
                                    actionLabel = "Reproduzir tudo",
                                    onActionClick = {
                                        viewModel.playTrackList("Top músicas de ${currentArtist.name}", currentArtist.topSongs, startIndex = 0)
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                currentArtist.topSongs.forEachIndexed { index, track ->
                                    val isCurrent = track.id == playbackState.currentTrack?.id
                                    ArtistTopSongRow(
                                        trackNumber = index + 1,
                                        track = track,
                                        isCurrentTrack = isCurrent,
                                        isPlaying = isCurrent && playbackState.isPlaying,
                                        onClick = {
                                            viewModel.playTrackList("Top músicas de ${currentArtist.name}", currentArtist.topSongs, startIndex = index)
                                        },
                                        onOptionsClick = { onTrackOptionsClick(track) }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Da sua biblioteca (From your library - faixas já ouvidas deste artista)
                    if (libraryTracks.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                YtmSectionHeader(
                                    title = "Da sua biblioteca",
                                    strapline = "COM BASE NO SEU HISTÓRICO LOCAL",
                                    actionLabel = null,
                                    onActionClick = null
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(libraryTracks, key = { "lib_${it.id}" }) { track ->
                                        HomeSongCard(
                                            track = track,
                                            isPlaying = track.id == playbackState.currentTrack?.id && playbackState.isPlaying,
                                            onClick = { viewModel.playTrack(track) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Álbuns (Albums)
                    if (currentArtist.albums.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                YtmSectionHeader(
                                    title = "Álbuns",
                                    strapline = null,
                                    actionLabel = null,
                                    onActionClick = null
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(currentArtist.albums, key = { it.id }) { albumTrack ->
                                        HomeSongCard(
                                            track = albumTrack,
                                            isPlaying = false,
                                            onClick = { onOpenAlbum(albumTrack.browseId ?: albumTrack.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Singles & EPs
                    if (currentArtist.singles.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                YtmSectionHeader(
                                    title = "Singles e EPs",
                                    strapline = null,
                                    actionLabel = null,
                                    onActionClick = null
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(currentArtist.singles, key = { it.id }) { singleTrack ->
                                        HomeSongCard(
                                            track = singleTrack,
                                            isPlaying = false,
                                            onClick = { onOpenAlbum(singleTrack.browseId ?: singleTrack.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. Vídeos (Widescreen 16:9)
                    if (currentArtist.videos.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                YtmSectionHeader(
                                    title = "Vídeos",
                                    strapline = "CLIPES E APRESENTAÇÕES",
                                    actionLabel = null,
                                    onActionClick = null
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(currentArtist.videos, key = { it.id }) { videoTrack ->
                                        ArtistVideoCard(
                                            track = videoTrack,
                                            isPlaying = videoTrack.id == playbackState.currentTrack?.id && playbackState.isPlaying,
                                            onClick = { viewModel.playTrack(videoTrack) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 7. Fans might also like (Outros fãs também gostam / Artistas parecidos)
                    if (currentArtist.fansMightAlsoLike.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                YtmSectionHeader(
                                    title = "Outros fãs também gostam",
                                    strapline = "ARTISTAS PARECIDOS",
                                    actionLabel = null,
                                    onActionClick = null
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                                ) {
                                    items(currentArtist.fansMightAlsoLike, key = { it.id }) { related ->
                                        ArtistCircularCard(
                                            artist = related,
                                            onClick = { onOpenArtist(related.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 8. Sobre o Artista (Card de biografia expansível)
                    if (!currentArtist.description.isNullOrBlank()) {
                        item {
                            ArtistAboutCard(
                                artistName = currentArtist.name,
                                description = currentArtist.description,
                                bannerUrl = currentArtist.bannerUrl
                            )
                        }
                    }
                }
            }
        }

        // Barra superior flutuante transparente (Voltar, Cast, Mais opções)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = YtmWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { /* Cast */ }) {
                    Icon(
                        imageVector = Icons.Outlined.Cast,
                        contentDescription = "Transmitir",
                        tint = YtmWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { /* Mais opções */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Mais opções",
                        tint = YtmWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Banner superior do artista com foto em destaque, nome, inscritos e botão de ação (Rádio / Shuffle).
 */
@Composable
private fun ArtistHeroHeader(
    artist: ArtistDetail,
    onPlayRadio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.1f)
        ) {
            val bannerUrl = artist.bannerUrl
            if (!bannerUrl.isNullOrBlank()) {
                AsyncImage(
                    model = bannerUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(YtmGrey11),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = YtmIconInactive,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            // Gradiente suave no topo para ícones de voltar/ações
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Gradiente na base fundindo com o fundo preto puro
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.9f),
                                YtmBlack
                            )
                        )
                    )
            )
        }

        // Nome do Artista
        Text(
            text = artist.name,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = YtmWhite,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Contagem de inscritos ou audiência mensal
        val subText = artist.subscriberCount ?: "Artista"
        Text(
            text = subText,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = YtmTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Botões de Ação: [Shuffle / Rádio] e [Inscrever-se / Seguir]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Botão Iniciar Rádio / Shuffle
            Button(
                onClick = onPlayRadio,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YtmWhite,
                    contentColor = YtmBlack
                ),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rádio",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Botão Inscrever-se (outline)
            OutlinedButton(
                onClick = { /* Inscrever-se */ },
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55FFFFFF)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = YtmWhite
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Inscrever-se",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Linha de faixa individual na seção Top Songs com capa miniatura, título e duração.
 */
@Composable
private fun ArtistTopSongRow(
    trackNumber: Int,
    track: Track,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Número da faixa ou ícone Equalizador
        Box(
            modifier = Modifier.width(28.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isCurrentTrack) {
                AnimatedEqualizerBars(
                    isPlaying = isPlaying,
                    barColor = OrangeSecondary,
                    barCount = 3,
                    barWidth = 2.5.dp,
                    barSpacing = 1.5.dp,
                    maxHeight = 15.dp
                )
            } else {
                Text(
                    text = "$trackNumber",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = YtmTextSecondary
                )
            }
        }

        // Capa miniatura
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(YtmGrey11)
        ) {
            val thumb = track.thumbnailUrl
            if (!thumb.isNullOrBlank()) {
                AsyncImage(
                    model = thumb,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = YtmIconInactive,
                    modifier = Modifier.size(24.dp).align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Título e Subtítulo
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.5.sp,
                color = if (isPlaying) OrangeSecondary else YtmWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val sub = if (track.durationText != "--:--" && track.durationText.isNotBlank()) {
                "${track.artist} • ${track.durationText}"
            } else {
                track.artist
            }
            Text(
                text = sub,
                fontFamily = YouTubeSansFontFamily,
                fontSize = 12.5.sp,
                color = YtmTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onOptionsClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Mais opções",
                tint = YtmIconInactive,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Card de vídeo widescreen 16:9 para a seção Vídeos.
 */
@Composable
private fun ArtistVideoCard(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(220.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(124.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(YtmGrey11)
        ) {
            val thumb = track.thumbnailUrl
            if (!thumb.isNullOrBlank()) {
                AsyncImage(
                    model = thumb,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Botão Play translúcido
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
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
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.5.sp,
            color = if (isPlaying) OrangeSecondary else YtmWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = track.durationText,
            fontFamily = YouTubeSansFontFamily,
            fontSize = 12.sp,
            color = YtmTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Card circular de artista relacionado para a seção Fans Might Also Like.
 */
@Composable
private fun ArtistCircularCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(115.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(115.dp)
                .clip(CircleShape)
                .background(YtmGrey11)
        ) {
            val thumb = artist.thumbnailUrl
            if (!thumb.isNullOrBlank()) {
                AsyncImage(
                    model = thumb,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = YtmIconInactive,
                    modifier = Modifier.size(48.dp).align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist.name,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
            color = YtmWhite,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (!artist.subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = artist.subtitle,
                fontFamily = YouTubeSansFontFamily,
                fontSize = 11.5.sp,
                color = YtmTextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Card biográfico com visual elegante do Spotify / Apple Music, expansível com "Ler mais".
 */
@Composable
private fun ArtistAboutCard(
    artistName: String,
    description: String,
    bannerUrl: String?,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        YtmSectionHeader(
            title = "Sobre $artistName",
            strapline = "BIOGRAFIA",
            actionLabel = null,
            onActionClick = null
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = YtmGrey11),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .animateContentSize()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = description,
                    fontFamily = YouTubeSansFontFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFFDDDDDD),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isExpanded) "Mostrar menos" else "Ler mais",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = OrangeSecondary
                )
            }
        }
    }
}
