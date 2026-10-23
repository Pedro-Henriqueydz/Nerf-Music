package com.nerf.music.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerf.music.data.model.UserPlaylist
import com.nerf.music.ui.theme.*

/**
 * BottomSheet oficial de opções da playlist com os 3 pontinhos (print do YouTube Music).
 * Inclui:
 * - Cabeçalho com título, visibilidade, autor, contagem de músicas e botão 'X' de fechar.
 * - Dois cards lado a lado: "Tocar a seguir" (Play next) e "Compartilhar" (Share).
 * - Lista vertical com todas as opções: Ordem aleatória, Ordem manual, Pesquisar na playlist,
 *   Iniciar rádio, Adicionar à fila, Salvar na playlist, Excluir playlist, Fixar e Ajuda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistOptionsBottomSheet(
    playlist: UserPlaylist,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onShufflePlay: () -> Unit,
    onSortManual: () -> Unit,
    onFindInPlaylist: () -> Unit,
    onStartMix: () -> Unit,
    onAddToQueue: () -> Unit,
    onSaveToPlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onPinToSpeedDial: () -> Unit,
    onHelpAndFeedback: () -> Unit
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
            // 1. Cabeçalho com Título, Metadados e Botão de Fechar 'X'
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.title,
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = YtmWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pública • Você • ${playlist.trackCount} músicas",
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

            // 2. Dois Cards de Ação Rápida Lado a Lado ("Tocar a seguir" e "Compartilhar")
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
                        .height(84.dp)
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

                // Card 2: Compartilhar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2C))
                        .clickable {
                            onDismiss()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, playlist.title)
                                val trackNames = playlist.tracks.take(5).joinToString(", ") { it.title }
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Ouça a playlist '${playlist.title}' no Nerf Music:\n$trackNames"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar playlist"))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = YtmWhite,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Compartilhar",
                            fontFamily = YouTubeSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = YtmWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Lista de Opções Verticais
            PlaylistOptionItem(
                icon = Icons.Default.Shuffle,
                title = "Tocar em ordem aleatória",
                onClick = {
                    onDismiss()
                    onShufflePlay()
                }
            )

            PlaylistOptionItem(
                icon = Icons.AutoMirrored.Filled.Sort,
                title = "Classificar: Ordem manual",
                onClick = {
                    onDismiss()
                    onSortManual()
                }
            )

            PlaylistOptionItem(
                icon = Icons.Default.Search,
                title = "Pesquisar na playlist",
                onClick = {
                    onDismiss()
                    onFindInPlaylist()
                }
            )

            PlaylistOptionItem(
                icon = Icons.Default.Radio,
                title = "Iniciar rádio",
                onClick = {
                    onDismiss()
                    onStartMix()
                }
            )

            PlaylistOptionItem(
                icon = Icons.Default.Queue,
                title = "Adicionar à fila",
                onClick = {
                    onDismiss()
                    onAddToQueue()
                }
            )

            PlaylistOptionItem(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                title = "Salvar na playlist",
                onClick = {
                    onDismiss()
                    onSaveToPlaylist()
                }
            )

            PlaylistOptionItem(
                icon = Icons.Default.Delete,
                title = "Excluir playlist",
                onClick = {
                    onDismiss()
                    onDeletePlaylist()
                }
            )

            PlaylistOptionItem(
                icon = Icons.Default.PushPin,
                title = "Fixar nos favoritos",
                onClick = {
                    onDismiss()
                    onPinToSpeedDial()
                }
            )

            PlaylistOptionItem(
                icon = Icons.Default.HelpOutline,
                title = "Ajuda e feedback",
                onClick = {
                    onDismiss()
                    onHelpAndFeedback()
                }
            )
        }
    }
}

@Composable
private fun PlaylistOptionItem(
    icon: ImageVector,
    title: String,
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
            tint = YtmWhite,
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
