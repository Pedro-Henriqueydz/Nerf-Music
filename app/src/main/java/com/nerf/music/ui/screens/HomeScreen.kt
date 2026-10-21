package com.nerf.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nerf.music.data.model.HomeSection
import com.nerf.music.data.model.Track
import com.nerf.music.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val homeSections by viewModel.homeSections.collectAsState()
    val isLoading by viewModel.isLoadingHome.collectAsState()
    val homeError by viewModel.homeError.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NERF Music",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Início",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading && homeSections.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Carregando músicas em alta...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            text = homeError ?: "Erro desconhecido",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadHomeFeed() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tentar Novamente")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. Seção Speed Dial (Ouvidas Recentemente na sessão atual)
                        if (recentlyPlayed.isNotEmpty()) {
                            item {
                                HomeSectionHeader(title = "Ouvidas recentemente")
                                Spacer(modifier = Modifier.height(8.dp))
                                HomeTrackCarousel(
                                    tracks = recentlyPlayed,
                                    currentPlayingId = playbackState.currentTrack?.id,
                                    onTrackClick = { track -> viewModel.playTrack(track) }
                                )
                            }
                        }

                        // 2. Seções dinâmicas da API InnerTube (ex: Escolha a dedo, Músicas em alta)
                        items(homeSections) { section ->
                            HomeSectionHeader(title = section.title)
                            Spacer(modifier = Modifier.height(8.dp))
                            HomeTrackCarousel(
                                tracks = section.items,
                                currentPlayingId = playbackState.currentTrack?.id,
                                onTrackClick = { track -> viewModel.playTrack(track) }
                            )
                        }

                        // Espaçador inferior para garantir rolagem livre acima do Mini Player e Nav Bar
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun HomeTrackCarousel(
    tracks: List<Track>,
    currentPlayingId: String?,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(tracks, key = { it.id }) { track ->
            HomeSongCard(
                track = track,
                isPlaying = track.id == currentPlayingId,
                onClick = { onTrackClick(track) }
            )
        }
    }
}

/**
 * Card quadrado grande com bordas arredondadas, título em negrito (truncado em 1 linha)
 * e artista/subtítulo em cinza abaixo (Material 3).
 */
@Composable
fun HomeSongCard(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        // Capa quadrada grande
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF242424))
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
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }

            // Indicador sutil de reprodução atual
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶ Tocando",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Título em negrito (truncado em 1 linha)
        Text(
            text = track.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Artista / Subtítulo em cinza
        Text(
            text = track.artist,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
