package com.nerf.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import com.nerf.music.data.model.AlbumDetail
import com.nerf.music.data.model.Track
import com.nerf.music.ui.theme.*
import com.nerf.music.ui.viewmodel.MusicViewModel

/**
 * Tela de detalhes do Álbum/Playlist réplica do YouTube Music:
 * - Capa grande em destaque no topo com transição suave para fundo preto (#000000).
 * - Título em negrito e subtítulo com Avatar do artista • Tipo de Álbum • Ano.
 * - Barra de ações: Download, Salvar e Compartilhar à esquerda + Botão circular Play branco à direita.
 * - Lista numerada de faixas (1, 2, 3...) com indicador de reprodução ativa e menu de 3 pontos.
 */
@Composable
fun AlbumScreen(
    browseId: String,
    viewModel: MusicViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val album by viewModel.albumDetail.collectAsState()
    val isLoading by viewModel.isLoadingAlbum.collectAsState()
    val albumError by viewModel.albumError.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    LaunchedEffect(browseId) {
        if (browseId.isNotBlank()) {
            viewModel.loadAlbum(browseId)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YtmBlack)
    ) {
        when {
            isLoading && album == null -> {
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
                            text = "Carregando álbum...",
                            fontFamily = YouTubeSansFontFamily,
                            fontSize = 14.sp,
                            color = YtmTextSecondary
                        )
                    }
                }
            }

            albumError != null && album == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = albumError ?: "Erro ao carregar álbum",
                            fontFamily = YouTubeSansFontFamily,
                            fontSize = 15.sp,
                            color = YtmTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadAlbum(browseId) },
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

            album != null -> {
                val currentAlbum = album!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // 1. Cabeçalho: Capa em destaque, título, avatar do artista e ações
                    item {
                        AlbumHeroHeader(
                            album = currentAlbum,
                            onPlayAll = { viewModel.playAlbum(currentAlbum, startIndex = 0) },
                            onDownload = {
                                val first = currentAlbum.tracks.firstOrNull()
                                if (first != null) viewModel.toggleDownload(first.id)
                            }
                        )
                    }

                    // 2. Lista numerada de faixas
                    itemsIndexed(currentAlbum.tracks, key = { index, track -> "${track.id}_$index" }) { index, track ->
                        val isCurrentPlaying = track.id == playbackState.currentTrack?.id
                        AlbumTrackRow(
                            trackNumber = index + 1,
                            track = track,
                            isPlaying = isCurrentPlaying,
                            onClick = {
                                viewModel.playAlbum(currentAlbum, startIndex = index)
                            }
                        )
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
 * Cabeçalho do álbum estilo YouTube Music:
 * - Capa quadrada no topo (com gradiente suave na base)
 * - Título centralizado
 * - Avatar circular do artista + "Artista • Tipo • Ano"
 * - Ações circulares (Download, Salvar, Compartilhar) + Botão Play circular grande branco
 */
@Composable
private fun AlbumHeroHeader(
    album: AlbumDetail,
    onPlayAll: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Imagem da Capa com gradiente na parte inferior fundindo com o preto puro
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f)
        ) {
            val coverUrl = album.thumbnailUrl
            if (!coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = album.title,
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
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = YtmIconInactive,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            // Gradiente suave no topo para legibilidade dos ícones da TopBar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Gradiente na base fundindo com o fundo preto absoluto
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
                                Color.Black.copy(alpha = 0.88f),
                                YtmBlack
                            )
                        )
                    )
            )
        }

        // Título do Álbum
        Text(
            text = album.title,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = YtmWhite,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subtítulo: Avatar circular + Artista • Álbum • Ano
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            if (!album.artistThumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = album.artistThumbnailUrl,
                    contentDescription = album.artistName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            val subtitleText = buildString {
                append(album.artistName)
                if (!album.albumType.isNullOrBlank()) {
                    append(" • ").append(album.albumType)
                }
                if (!album.year.isNullOrBlank()) {
                    append(" • ").append(album.year)
                }
            }

            Text(
                text = subtitleText,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.5.sp,
                color = YtmTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Linha de Ações: [Download] [Bookmark] [Share] ........ [Big Play]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Ações secundárias circulares à esquerda
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Download
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .clickable(onClick = onDownload),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = YtmWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Bookmark / Salvar na Biblioteca
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Salvar",
                        tint = YtmWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Compartilhar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartilhar",
                        tint = YtmWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Botão Play grande branco à direita
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(YtmWhite)
                    .clickable(onClick = onPlayAll),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reproduzir tudo",
                    tint = YtmBlack,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

/**
 * Item numerado da lista de faixas do álbum (1, 2, 3...):
 * Mostra número, título, artista • duração e botão de 3 pontos.
 */
@Composable
private fun AlbumTrackRow(
    trackNumber: Int,
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Número da faixa ou ícone de Equalizador se estiver tocando
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Tocando",
                    tint = OrangeSecondary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = "$trackNumber",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = YtmWhite
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Título e Subtítulo da faixa
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = if (isPlaying) OrangeSecondary else YtmWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val durationInfo = if (track.durationText != "--:--" && track.durationText.isNotBlank()) {
                "${track.artist} • ${track.durationText}"
            } else {
                track.artist
            }

            Text(
                text = durationInfo,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = YtmTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Menu de opções (3 pontos)
        IconButton(
            onClick = { /* Opções da faixa */ },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opções",
                tint = YtmIconInactive,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
