package com.nerf.music.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerf.music.data.model.Track
import com.nerf.music.data.model.UserPlaylist
import com.nerf.music.ui.theme.*

/**
 * BottomSheet oficial "Salvar música na playlist" fiel ao YouTube Music (print do usuário).
 * Permite:
 * 1. Criar uma nova playlist e salvar a música diretamente nela.
 * 2. Alternar status em "Músicas Curtidas".
 * 3. Adicionar/remover a música das playlists criadas pelo usuário com checkboxes reativos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveToPlaylistSheet(
    track: Track,
    userPlaylists: List<UserPlaylist>,
    isLiked: Boolean,
    likedCount: Int,
    onDismiss: () -> Unit,
    onToggleLiked: () -> Unit,
    onTogglePlaylist: (UserPlaylist, Boolean) -> Unit,
    onCreateNewPlaylist: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF212121),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF606060)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Salvar 1 música na playlist",
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = YtmWhite,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Opção "+ Nova playlist"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onCreateNewPlaylist()
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF383838)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nova playlist",
                        tint = YtmWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Nova playlist",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = YtmWhite
                )
            }

            // 2. Opção "Músicas Curtidas"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleLiked() }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF7E3FF2), Color(0xFFE8428C))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ThumbUp,
                        contentDescription = null,
                        tint = YtmWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Músicas Curtidas",
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = YtmWhite
                    )
                    Text(
                        text = "$likedCount músicas",
                        fontFamily = YouTubeSansFontFamily,
                        fontSize = 13.sp,
                        color = YtmTextSecondary
                    )
                }

                if (isLiked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Curtida",
                        tint = YtmWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 3. Lista de Playlists do Usuário
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(userPlaylists, key = { it.id }) { pl ->
                    val isTrackInPlaylist = pl.tracks.any { it.id == track.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTogglePlaylist(pl, !isTrackInPlaylist)
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistCollageCover(
                            tracks = pl.tracks,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pl.title,
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = YtmWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pl.trackCount} músicas",
                                fontFamily = YouTubeSansFontFamily,
                                fontSize = 13.sp,
                                color = YtmTextSecondary
                            )
                        }

                        Checkbox(
                            checked = isTrackInPlaylist,
                            onCheckedChange = { checked ->
                                onTogglePlaylist(pl, checked)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = OrangePrimary,
                                uncheckedColor = Color(0xFF707070),
                                checkmarkColor = YtmWhite
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * BottomSheet oficial "Definir visibilidade" (print do usuário).
 * Permite selecionar entre "Pública" e "Particular" com checkmark indicador.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetVisibilitySheet(
    currentVisibility: String,
    onSelectVisibility: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF212121),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF606060)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Definir visibilidade",
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = YtmWhite,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Pública
            val isPublic = currentVisibility.equals("Pública", ignoreCase = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectVisibility("Pública")
                        onDismiss()
                    }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = YtmWhite,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pública",
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = YtmWhite
                    )
                    Text(
                        text = "Qualquer pessoa pode pesquisar e ver",
                        fontFamily = YouTubeSansFontFamily,
                        fontSize = 13.sp,
                        color = YtmTextSecondary
                    )
                }

                if (isPublic) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selecionada",
                        tint = YtmWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 2. Particular
            val isPrivate = currentVisibility.equals("Particular", ignoreCase = true) || currentVisibility.equals("Privada", ignoreCase = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectVisibility("Particular")
                        onDismiss()
                    }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = YtmWhite,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Particular",
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = YtmWhite
                    )
                    Text(
                        text = "Somente você pode ver",
                        fontFamily = YouTubeSansFontFamily,
                        fontSize = 13.sp,
                        color = YtmTextSecondary
                    )
                }

                if (isPrivate) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selecionada",
                        tint = YtmWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
