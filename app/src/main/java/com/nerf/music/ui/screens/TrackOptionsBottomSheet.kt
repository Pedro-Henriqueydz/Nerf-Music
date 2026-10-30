package com.nerf.music.ui.screens

import android.content.Intent
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nerf.music.data.model.Track
import com.nerf.music.ui.theme.*

/**
 * BottomSheet oficial de opções da música com os 3 pontinhos (idêntico ao YouTube Music).
 * Permite:
 * - Tocar a seguir
 * - Iniciar rádio
 * - Adicionar à fila
 * - Salvar na playlist
 * - Favoritar / Desfavoritar
 * - Fazer download / Remover download
 * - Compartilhar
 * - Navegar para Artista / Álbum
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsBottomSheet(
    track: Track,
    isLiked: Boolean,
    isDownloaded: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onStartRadio: () -> Unit,
    onSaveToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleDownload: () -> Unit,
    onOpenSleepTimer: (() -> Unit)? = null,
    sleepTimerStatus: String? = null,
    onOpenArtist: ((String) -> Unit)? = null,
    onOpenAlbum: ((String) -> Unit)? = null
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF212121),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF606060)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            // 1. Cabeçalho com Thumbnail, Título, Artista e Botão Fechar 'X'
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val artwork = track.highResThumbnailUrl ?: track.thumbnailUrl
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF333333)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!artwork.isNullOrEmpty()) {
                        AsyncImage(
                            model = artwork,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = YtmWhite,
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
                        fontSize = 13.sp,
                        color = YtmTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = YtmWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Dois Cards de Ação Rápida Lado a Lado ("Tocar a seguir" e "Iniciar rádio")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Tocar a seguir
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2C))
                        .clickable {
                            onDismiss()
                            onPlayNext()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = null,
                            tint = YtmWhite,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tocar a seguir",
                            fontFamily = YouTubeSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = YtmWhite
                        )
                    }
                }

                // Card 2: Iniciar rádio
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2C))
                        .clickable {
                            onDismiss()
                            onStartRadio()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            tint = YtmWhite,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Iniciar rádio",
                            fontFamily = YouTubeSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = YtmWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Linhas verticais de opções estilo YouTube Music
            TrackOptionRow(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                title = "Adicionar à fila",
                onClick = {
                    onDismiss()
                    onAddToQueue()
                }
            )

            TrackOptionRow(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                title = "Salvar na playlist",
                onClick = {
                    onDismiss()
                    onSaveToPlaylist()
                }
            )

            TrackOptionRow(
                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                title = if (isLiked) "Remover das Músicas Curtidas" else "Salvar em Músicas Curtidas",
                iconTint = if (isLiked) OrangePrimary else YtmWhite,
                onClick = {
                    onToggleFavorite()
                }
            )

            TrackOptionRow(
                icon = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                title = if (isDownloaded) "Remover download" else "Fazer download",
                iconTint = if (isDownloaded) OrangeSecondary else YtmWhite,
                onClick = {
                    onToggleDownload()
                }
            )

            if (onOpenSleepTimer != null) {
                TrackOptionRow(
                    icon = Icons.Default.Bedtime,
                    title = sleepTimerStatus?.let { "Temporizador • $it" } ?: "Temporizador de sono",
                    onClick = {
                        onDismiss()
                        onOpenSleepTimer()
                    }
                )
            }

            TrackOptionRow(
                icon = Icons.Default.Share,
                title = "Compartilhar Card Social",
                onClick = {
                    onDismiss()
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        try {
                            com.nerf.music.util.SocialShareHelper.openPreview(context, track)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Erro ao gerar card: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            if (onOpenArtist != null && !track.artistBrowseId.isNullOrBlank()) {
                TrackOptionRow(
                    icon = Icons.Default.Person,
                    title = "Ver artista",
                    onClick = {
                        onDismiss()
                        onOpenArtist(track.artistBrowseId)
                    }
                )
            }

            if (onOpenAlbum != null && !track.browseId.isNullOrBlank()) {
                TrackOptionRow(
                    icon = Icons.Default.Album,
                    title = "Ver álbum",
                    onClick = {
                        onDismiss()
                        onOpenAlbum(track.browseId)
                    }
                )
            }
        }
    }
}

@Composable
private fun TrackOptionRow(
    icon: ImageVector,
    title: String,
    iconTint: Color = YtmWhite,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = title,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            color = YtmWhite
        )
    }
}
