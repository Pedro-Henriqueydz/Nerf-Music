package com.nerf.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nerf.music.data.model.DownloadedTrack
import com.nerf.music.data.model.Track
import com.nerf.music.data.model.UserPlaylist
import com.nerf.music.ui.theme.*
import com.nerf.music.ui.viewmodel.MusicViewModel

enum class LibraryFilter(val label: String, val icon: ImageVector? = null) {
    ALL("Tudo"),
    DOWNLOADS("Downloads", Icons.Filled.Download),
    PLAYLISTS("Playlists"),
    SONGS("Músicas"),
    ALBUMS("Álbuns"),
    ARTISTS("Artistas")
}

enum class LibrarySortOption(val label: String) {
    RECENT("Atividade recente"),
    RECENTLY_ADDED("Adicionados recentemente"),
    A_TO_Z("De A a Z")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onOpenSettings: () -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val likedTracks by viewModel.likedTracks.collectAsState()
    val userPlaylists by viewModel.userPlaylists.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()

    var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var isGridView by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(LibrarySortOption.RECENT) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    // Estado da barra de busca interna da biblioteca
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog para criar nova playlist
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }

    // Dialog para ver/reproduzir playlist do usuário
    var selectedUserPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
    var showLikedTracksView by remember { mutableStateOf(false) }
    var showDownloadedTracksView by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YtmBlack)
    ) {
        // 1. TopBar Oficial da Biblioteca
        if (!isSearchActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Biblioteca",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    letterSpacing = (-0.5).sp,
                    color = YtmWhite
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Lupa de busca exclusiva da biblioteca
                    IconButton(onClick = { isSearchActive = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Pesquisar na biblioteca",
                            tint = YtmWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Engrenagem de Configurações
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
        } else {
            // Barra de busca ativa
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    isSearchActive = false
                    searchQuery = ""
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Fechar pesquisa",
                        tint = YtmWhite
                    )
                }

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Pesquisar na biblioteca",
                            color = YtmTextSecondary,
                            fontFamily = YouTubeSansFontFamily,
                            fontSize = 16.sp
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = YtmWhite,
                        unfocusedTextColor = YtmWhite
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpar busca",
                            tint = YtmTextSecondary
                        )
                    }
                }
            }
        }

        // Sub-telas de navegação direta (Músicas Curtidas / Downloads / Playlist)
        when {
            showLikedTracksView -> {
                TrackListDetailView(
                    title = "Músicas Curtidas",
                    subtitle = "Playlist automática • ${likedTracks.size} músicas",
                    tracks = likedTracks,
                    onBack = { showLikedTracksView = false },
                    onPlayTrack = { track ->
                        viewModel.playTrack(track)
                    },
                    onShuffleAll = {
                        if (likedTracks.isNotEmpty()) {
                            viewModel.playTrack(likedTracks.shuffled().first())
                        }
                    }
                )
            }
            showDownloadedTracksView -> {
                val downloadedList = downloadedTracks.map { it.track }
                TrackListDetailView(
                    title = "Músicas Baixadas",
                    subtitle = "Downloads offline • ${downloadedList.size} músicas",
                    tracks = downloadedList,
                    onBack = { showDownloadedTracksView = false },
                    onPlayTrack = { track ->
                        viewModel.playTrack(track)
                    },
                    onShuffleAll = {
                        if (downloadedList.isNotEmpty()) {
                            viewModel.playTrack(downloadedList.shuffled().first())
                        }
                    }
                )
            }
            selectedUserPlaylist != null -> {
                val currentPl = userPlaylists.firstOrNull { it.id == selectedUserPlaylist!!.id } ?: selectedUserPlaylist!!
                TrackListDetailView(
                    title = currentPl.title,
                    subtitle = "Playlist personalizada • ${currentPl.trackCount} músicas",
                    tracks = currentPl.tracks,
                    onBack = { selectedUserPlaylist = null },
                    onPlayTrack = { track ->
                        viewModel.playTrack(track)
                    },
                    onShuffleAll = {
                        if (currentPl.tracks.isNotEmpty()) {
                            viewModel.playTrack(currentPl.tracks.shuffled().first())
                        }
                    },
                    onDelete = {
                        viewModel.deletePlaylist(currentPl.id)
                        selectedUserPlaylist = null
                    }
                )
            }
            else -> {
                // 2. Fileira de Chips Horizontais
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedFilter != LibraryFilter.ALL) {
                        Surface(
                            shape = CircleShape,
                            color = YtmWhite10Pct,
                            modifier = Modifier
                                .clickable { selectedFilter = LibraryFilter.ALL }
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpar filtro",
                                tint = YtmWhite,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                    }

                    LibraryFilter.values().filter { it != LibraryFilter.ALL }.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) YtmWhite else YtmWhite10Pct,
                            modifier = Modifier.clickable {
                                selectedFilter = if (isSelected) LibraryFilter.ALL else filter
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (filter.icon != null) {
                                    Icon(
                                        imageVector = filter.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) YtmBlack else YtmWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = filter.label,
                                    fontFamily = YouTubeSansFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (isSelected) YtmBlack else YtmWhite
                                )
                            }
                        }
                    }
                }

                // 3. Sub-header de Ordenação e Alternador de Lista/Grade
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { isSortMenuExpanded = true }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = sortOption.label,
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = YtmWhite
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = YtmWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = isSortMenuExpanded,
                            onDismissRequest = { isSortMenuExpanded = false },
                            modifier = Modifier.background(YtmGrey11)
                        ) {
                            LibrarySortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontFamily = YouTubeSansFontFamily,
                                            color = if (sortOption == option) OrangePrimary else YtmWhite
                                        )
                                    },
                                    onClick = {
                                        sortOption = option
                                        isSortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Botão alternador de Lista vs Grade
                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "Exibir em lista" else "Exibir em grade",
                            tint = YtmWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 4. Conteúdo Filtrado
                Box(modifier = Modifier.weight(1f)) {
                    val q = searchQuery.trim().lowercase()

                    // Filtragem com busca
                    val filteredLiked = likedTracks.filter {
                        q.isEmpty() || it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
                    }
                    val filteredDownloads = downloadedTracks.filter {
                        q.isEmpty() || it.track.title.lowercase().contains(q) || it.track.artist.lowercase().contains(q)
                    }
                    val filteredPlaylists = userPlaylists.filter {
                        q.isEmpty() || it.title.lowercase().contains(q)
                    }

                    val shouldShowLiked = (selectedFilter == LibraryFilter.ALL || selectedFilter == LibraryFilter.PLAYLISTS || selectedFilter == LibraryFilter.SONGS) &&
                            (q.isEmpty() || filteredLiked.isNotEmpty())
                    val shouldShowDownloads = (selectedFilter == LibraryFilter.ALL || selectedFilter == LibraryFilter.DOWNLOADS || selectedFilter == LibraryFilter.PLAYLISTS) &&
                            (q.isEmpty() || filteredDownloads.isNotEmpty())

                    if (!isGridView) {
                        // Visualização em Lista
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (shouldShowLiked) {
                                item(key = "liked_music_item") {
                                    LibraryListItem(
                                        title = "Músicas Curtidas",
                                        subtitle = "Playlist automática • ${likedTracks.size} músicas",
                                        iconVector = Icons.Filled.Favorite,
                                        iconTint = Color(0xFFE91E63),
                                        onClick = { showLikedTracksView = true }
                                    )
                                }
                            }

                            if (shouldShowDownloads) {
                                item(key = "downloaded_songs_item") {
                                    LibraryListItem(
                                        title = "Músicas Baixadas",
                                        subtitle = "Downloads offline • ${downloadedTracks.size} músicas",
                                        iconVector = Icons.Filled.DownloadDone,
                                        iconTint = Color(0xFF4CAF50),
                                        onClick = { showDownloadedTracksView = true }
                                    )
                                }
                            }

                            if (selectedFilter == LibraryFilter.ALL || selectedFilter == LibraryFilter.PLAYLISTS) {
                                items(filteredPlaylists, key = { it.id }) { pl ->
                                    LibraryListItem(
                                        title = pl.title,
                                        subtitle = "Playlist • ${pl.trackCount} músicas",
                                        imageUrl = pl.thumbnailUrl,
                                        iconVector = Icons.Default.QueueMusic,
                                        onClick = { selectedUserPlaylist = pl }
                                    )
                                }
                            }

                            if (selectedFilter == LibraryFilter.SONGS) {
                                items(filteredLiked, key = { it.id }) { track ->
                                    LibraryListItem(
                                        title = track.title,
                                        subtitle = "${track.artist} • ${track.durationText}",
                                        imageUrl = track.thumbnailUrl,
                                        onClick = { viewModel.playTrack(track) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Visualização em Grade
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (shouldShowLiked) {
                                item(key = "liked_music_grid") {
                                    LibraryGridCard(
                                        title = "Músicas Curtidas",
                                        subtitle = "Playlist • ${likedTracks.size} músicas",
                                        iconVector = Icons.Filled.Favorite,
                                        iconTint = Color(0xFFE91E63),
                                        onClick = { showLikedTracksView = true }
                                    )
                                }
                            }

                            if (shouldShowDownloads) {
                                item(key = "downloaded_songs_grid") {
                                    LibraryGridCard(
                                        title = "Músicas Baixadas",
                                        subtitle = "Downloads • ${downloadedTracks.size} músicas",
                                        iconVector = Icons.Filled.DownloadDone,
                                        iconTint = Color(0xFF4CAF50),
                                        onClick = { showDownloadedTracksView = true }
                                    )
                                }
                            }

                            if (selectedFilter == LibraryFilter.ALL || selectedFilter == LibraryFilter.PLAYLISTS) {
                                items(filteredPlaylists, key = { it.id }) { pl ->
                                    LibraryGridCard(
                                        title = pl.title,
                                        subtitle = "Playlist • ${pl.trackCount} músicas",
                                        imageUrl = pl.thumbnailUrl,
                                        iconVector = Icons.Default.QueueMusic,
                                        onClick = { selectedUserPlaylist = pl }
                                    )
                                }
                            }
                        }
                    }

                    // Botão Flutuante "+ Nova"
                    FloatingActionButton(
                        onClick = { showCreatePlaylistDialog = true },
                        containerColor = YtmGrey10,
                        contentColor = YtmWhite,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Nova playlist",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal para criar nova playlist
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                newPlaylistTitle = ""
            },
            containerColor = YtmGrey11,
            title = {
                Text(
                    text = "Nova playlist",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = YtmWhite
                )
            },
            text = {
                OutlinedTextField(
                    value = newPlaylistTitle,
                    onValueChange = { newPlaylistTitle = it },
                    placeholder = {
                        Text(text = "Título da playlist", color = YtmTextSecondary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = YtmWhite,
                        unfocusedTextColor = YtmWhite,
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = YtmWhite20Pct
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistTitle.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistTitle.trim())
                            showCreatePlaylistDialog = false
                            newPlaylistTitle = ""
                        }
                    }
                ) {
                    Text(text = "Criar", color = OrangePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreatePlaylistDialog = false
                    newPlaylistTitle = ""
                }) {
                    Text(text = "Cancelar", color = YtmTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun LibraryListItem(
    title: String,
    subtitle: String,
    imageUrl: String? = null,
    iconVector: ImageVector? = null,
    iconTint: Color = YtmWhite,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(YtmGrey10),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
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
                fontSize = 13.sp,
                color = YtmTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Mais opções",
                tint = YtmTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun LibraryGridCard(
    title: String,
    subtitle: String,
    imageUrl: String? = null,
    iconVector: ImageVector? = null,
    iconTint: Color = YtmWhite,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(YtmGrey10),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = YtmWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = YtmTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TrackListDetailView(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    onBack: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onShuffleAll: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YtmBlack)
    ) {
        // Header com Voltar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = YtmWhite
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = YtmWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontFamily = YouTubeSansFontFamily,
                    fontSize = 12.sp,
                    color = YtmTextSecondary
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Excluir playlist",
                        tint = ErrorColor
                    )
                }
            }
        }

        // Botões de Ação Rápida (Tocar / Aleatório)
        if (tracks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onPlayTrack(tracks.first()) },
                    colors = ButtonDefaults.buttonColors(containerColor = YtmWhite, contentColor = YtmBlack),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Tocar", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onShuffleAll,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = YtmWhite),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(YtmWhite20Pct)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Aleatório", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma música adicionada ainda.",
                    fontFamily = YouTubeSansFontFamily,
                    fontSize = 15.sp,
                    color = YtmTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tracks, key = { it.id }) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPlayTrack(track) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(YtmGrey10)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = YtmWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${track.artist} • ${track.durationText}",
                                fontFamily = YouTubeSansFontFamily,
                                fontSize = 12.sp,
                                color = YtmTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
