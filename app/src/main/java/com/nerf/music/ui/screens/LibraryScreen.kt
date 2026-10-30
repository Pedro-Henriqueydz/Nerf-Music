package com.nerf.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    val mostPlayedTracks by viewModel.mostPlayedTracks.collectAsState()
    val recentlyAddedTracks by viewModel.recentlyAddedTracks.collectAsState()
    val isGridView by viewModel.isLibraryGridView.collectAsState()

    var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var sortOption by remember { mutableStateOf(LibrarySortOption.RECENT) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    // Estado da barra de busca interna da biblioteca
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // BottomSheet "Nova" estilo YouTube Music
    var showNewBottomSheet by remember { mutableStateOf(false) }

    // Modal oficial "Nova playlist" idêntico ao YouTube Music
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }
    var newPlaylistVisibility by remember { mutableStateOf("Pública") }
    var isCollaborative by remember { mutableStateOf(false) }
    var showVisibilitySheet by remember { mutableStateOf(false) }

    // Sub-telas de navegação direta
    var selectedUserPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
    var showLikedTracksView by remember { mutableStateOf(false) }
    var showDownloadedTracksView by remember { mutableStateOf(false) }
    var selectedSmartPlaylist by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YtmBlack)
    ) {
        val isInsideSubView = showLikedTracksView || showDownloadedTracksView || selectedUserPlaylist != null || selectedSmartPlaylist != null

        // 1. TopBar Oficial da Biblioteca (visível apenas na raiz da biblioteca)
        if (!isInsideSubView) {
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
                    IconButton(onClick = { isSearchActive = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Pesquisar na biblioteca",
                            tint = YtmWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
        }

        // Sub-telas de navegação direta (Músicas Curtidas / Downloads / Playlist do Usuário)
        when {
            selectedSmartPlaylist != null -> {
                val smartTitle = selectedSmartPlaylist.orEmpty()
                val smartTracks = if (smartTitle == "Mais Tocadas") mostPlayedTracks else recentlyAddedTracks
                TrackListDetailView(
                    title = smartTitle,
                    subtitle = "Playlist automática • ${smartTracks.size} músicas",
                    tracks = smartTracks,
                    onBack = { selectedSmartPlaylist = null },
                    onPlayTrack = { _, index -> viewModel.playTrackList(smartTitle, smartTracks, index, false) },
                    onShuffleAll = { viewModel.playTrackList(smartTitle, smartTracks, 0, true) }
                )
            }
            showLikedTracksView -> {
                TrackListDetailView(
                    title = "Músicas Curtidas",
                    subtitle = "Playlist automática • ${likedTracks.size} músicas",
                    tracks = likedTracks,
                    isLikedMusic = true,
                    onBack = { showLikedTracksView = false },
                    onPlayTrack = { track, index ->
                        viewModel.playTrackList("Músicas Curtidas", likedTracks, index, false)
                    },
                    onShuffleAll = {
                        viewModel.playTrackList("Músicas Curtidas", likedTracks, 0, true)
                    },
                    onDownloadAll = {
                        viewModel.downloadAll(likedTracks)
                    }
                )
            }
            showDownloadedTracksView -> {
                val downloadedList = downloadedTracks.map { it.track }
                TrackListDetailView(
                    title = "Músicas Baixadas",
                    subtitle = "Downloads offline • ${downloadedList.size} músicas",
                    tracks = downloadedList,
                    isDownloads = true,
                    onBack = { showDownloadedTracksView = false },
                    onPlayTrack = { track, index ->
                        viewModel.playTrackList("Músicas Baixadas", downloadedList, index, false)
                    },
                    onShuffleAll = {
                        viewModel.playTrackList("Músicas Baixadas", downloadedList, 0, true)
                    }
                )
            }
            selectedUserPlaylist != null -> {
                val currentPl = userPlaylists.firstOrNull { it.id == selectedUserPlaylist!!.id } ?: selectedUserPlaylist!!
                UserPlaylistDetailView(
                    playlist = currentPl,
                    viewModel = viewModel,
                    onBack = { selectedUserPlaylist = null }
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

                // 3. Sub-header de Ordenação e Alternador de Lista/Grade (com persistência)
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

                    // Botão alternador de Lista vs Grade (persiste via ViewModel)
                    IconButton(
                        onClick = { viewModel.setLibraryGridView(!isGridView) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "Exibir em lista" else "Exibir em grade",
                            tint = YtmWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 4. Conteúdo Filtrado
                Box(modifier = Modifier.weight(1f)) {
                    val q = searchQuery.trim().lowercase()

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
                            if (mostPlayedTracks.isNotEmpty()) {
                                item(key = "most_played_smart") {
                                    LibraryListItem(
                                        title = "Mais Tocadas",
                                        subtitle = "Playlist automática • ${mostPlayedTracks.size} músicas",
                                        playlistTracks = mostPlayedTracks,
                                        onClick = { selectedSmartPlaylist = "Mais Tocadas" }
                                    )
                                }
                            }
                            if (recentlyAddedTracks.isNotEmpty()) {
                                item(key = "recently_added_smart") {
                                    LibraryListItem(
                                        title = "Adicionadas Recentemente",
                                        subtitle = "Playlist automática • ${recentlyAddedTracks.size} músicas",
                                        playlistTracks = recentlyAddedTracks,
                                        onClick = { selectedSmartPlaylist = "Adicionadas Recentemente" }
                                    )
                                }
                            }
                            if (shouldShowLiked) {
                                item(key = "liked_music_item") {
                                    LibraryListItem(
                                        title = "Músicas Curtidas",
                                        subtitle = "Playlist automática • ${likedTracks.size} músicas",
                                        isSpecialLiked = true,
                                        onClick = { showLikedTracksView = true }
                                    )
                                }
                            }

                            if (shouldShowDownloads) {
                                item(key = "downloaded_songs_item") {
                                    LibraryListItem(
                                        title = "Músicas Baixadas",
                                        subtitle = "Downloads offline • ${downloadedTracks.size} músicas",
                                        isSpecialDownload = true,
                                        onClick = { showDownloadedTracksView = true }
                                    )
                                }
                            }

                            if (selectedFilter == LibraryFilter.ALL || selectedFilter == LibraryFilter.PLAYLISTS) {
                                items(filteredPlaylists, key = { it.id }) { pl ->
                                    LibraryListItem(
                                        title = pl.title,
                                        subtitle = "Playlist • ${pl.trackCount} músicas",
                                        playlistTracks = pl.tracks,
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
                        // Visualização em Grade (Padrão Oficial)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (mostPlayedTracks.isNotEmpty()) {
                                item(key = "most_played_smart_grid") {
                                    LibraryGridCard(
                                        title = "Mais Tocadas",
                                        subtitle = "Playlist automática",
                                        playlistTracks = mostPlayedTracks,
                                        onClick = { selectedSmartPlaylist = "Mais Tocadas" }
                                    )
                                }
                            }
                            if (recentlyAddedTracks.isNotEmpty()) {
                                item(key = "recently_added_smart_grid") {
                                    LibraryGridCard(
                                        title = "Adicionadas Recentemente",
                                        subtitle = "Playlist automática",
                                        playlistTracks = recentlyAddedTracks,
                                        onClick = { selectedSmartPlaylist = "Adicionadas Recentemente" }
                                    )
                                }
                            }
                            if (shouldShowLiked) {
                                item(key = "liked_music_grid") {
                                    LibraryGridCard(
                                        title = "Músicas Curtidas",
                                        subtitle = "Playlist automática",
                                        isSpecialLiked = true,
                                        onClick = { showLikedTracksView = true }
                                    )
                                }
                            }

                            if (shouldShowDownloads) {
                                item(key = "downloaded_songs_grid") {
                                    LibraryGridCard(
                                        title = "Músicas Baixadas",
                                        subtitle = "Downloads • ${downloadedTracks.size} músicas",
                                        isSpecialDownload = true,
                                        onClick = { showDownloadedTracksView = true }
                                    )
                                }
                            }

                            if (selectedFilter == LibraryFilter.ALL || selectedFilter == LibraryFilter.PLAYLISTS) {
                                items(filteredPlaylists, key = { it.id }) { pl ->
                                    LibraryGridCard(
                                        title = pl.title,
                                        subtitle = "Playlist • ${pl.trackCount} músicas",
                                        playlistTracks = pl.tracks,
                                        onClick = { selectedUserPlaylist = pl }
                                    )
                                }
                            }
                        }
                    }

                    // Botão Flutuante Cápsula estilo YouTube Music (+ Nova)
                    FloatingActionButton(
                        onClick = { showNewBottomSheet = true },
                        containerColor = YtmGrey10,
                        contentColor = YtmWhite,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nova",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // ModalBottomSheet "Nova" idêntico ao YouTube Music
    if (showNewBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNewBottomSheet = false },
            containerColor = YtmGrey11,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Nova",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = YtmWhite,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                NewBottomSheetOption(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "Playlist",
                    subtitle = "Reúna músicas e vídeos",
                    onClick = {
                        showNewBottomSheet = false
                        showCreatePlaylistDialog = true
                    }
                )

                NewBottomSheetOption(
                    icon = Icons.Default.GroupAdd,
                    title = "Playlist colaborativa",
                    subtitle = "Crie uma playlist com amigos",
                    onClick = {
                        showNewBottomSheet = false
                        showCreatePlaylistDialog = true
                    }
                )

                NewBottomSheetOption(
                    icon = Icons.Default.JoinInner,
                    title = "Taste match",
                    subtitle = "Combine o gosto musical dos seus amigos",
                    onClick = {
                        showNewBottomSheet = false
                        showCreatePlaylistDialog = true
                    }
                )

                NewBottomSheetOption(
                    icon = Icons.Default.Radio,
                    title = "Mix",
                    subtitle = "Escolha um artista e criaremos um mix",
                    onClick = {
                        showNewBottomSheet = false
                        showCreatePlaylistDialog = true
                    }
                )
            }
        }
    }

    // Diálogo "Nova playlist" Fiel ao Print do YouTube Music
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                newPlaylistTitle = ""
            },
            containerColor = Color(0xFF282828),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Nova playlist",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = YtmWhite
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Campo Título
                    OutlinedTextField(
                        value = newPlaylistTitle,
                        onValueChange = { newPlaylistTitle = it },
                        placeholder = {
                            Text(text = "Título", color = YtmTextSecondary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = YtmWhite,
                            unfocusedTextColor = YtmWhite,
                            focusedBorderColor = YtmWhite,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Campo Visibilidade (conforme print oficial)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVisibilitySheet = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Visibilidade",
                                    fontFamily = YouTubeSansFontFamily,
                                    fontSize = 12.sp,
                                    color = YtmTextSecondary
                                )
                                Text(
                                    text = newPlaylistVisibility,
                                    fontFamily = YouTubeSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = YtmWhite
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = YtmWhite
                            )
                        }
                    }

                    // Linha Colaborar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Colaborar",
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = YtmWhite
                        )
                        Switch(
                            checked = isCollaborative,
                            onCheckedChange = { isCollaborative = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = YtmWhite,
                                checkedTrackColor = OrangePrimary,
                                uncheckedThumbColor = Color(0xFF909090),
                                uncheckedTrackColor = Color(0xFF383838)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                val isEnabled = newPlaylistTitle.isNotBlank()
                Button(
                    onClick = {
                        if (isEnabled) {
                            val created = viewModel.createPlaylist(newPlaylistTitle.trim())
                            showCreatePlaylistDialog = false
                            newPlaylistTitle = ""
                            if (created != null) {
                                selectedUserPlaylist = created
                            }
                        }
                    },
                    enabled = isEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEnabled) YtmWhite else Color(0xFF3A3A3A),
                        contentColor = if (isEnabled) YtmBlack else YtmTextSecondary
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(text = "Criar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreatePlaylistDialog = false
                    newPlaylistTitle = ""
                }) {
                    Text(text = "Cancelar", color = YtmWhite, fontSize = 14.sp)
                }
            }
        )
    }

    if (showVisibilitySheet) {
        SetVisibilitySheet(
            currentVisibility = newPlaylistVisibility,
            onSelectVisibility = { newPlaylistVisibility = it },
            onDismiss = { showVisibilitySheet = false }
        )
    }
}

@Composable
private fun NewBottomSheetOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = YtmWhite,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = YtmWhite
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = YouTubeSansFontFamily,
                fontSize = 13.sp,
                color = YtmTextSecondary
            )
        }
    }
}

/**
 * Renderiza uma colagem 2x2 das capas das 4 primeiras músicas da playlist
 * (ou capa individual se tiver < 4 músicas, ou gradiente estilizado se vazia).
 */
@Composable
fun PlaylistCollageCover(
    tracks: List<Track>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(YtmGrey10),
        contentAlignment = Alignment.Center
    ) {
        when {
            tracks.size >= 4 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = tracks[0].thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        AsyncImage(
                            model = tracks[1].thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = tracks[2].thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        AsyncImage(
                            model = tracks[3].thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
            tracks.size == 3 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = tracks[0].thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AsyncImage(
                            model = tracks[1].thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                        AsyncImage(
                            model = tracks[2].thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }
            }
            tracks.size == 2 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = tracks[0].thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    AsyncImage(
                        model = tracks[1].thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
            tracks.size == 1 -> {
                AsyncImage(
                    model = tracks[0].thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF2C2459), Color(0xFF131313)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = YtmWhite.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryListItem(
    title: String,
    subtitle: String,
    imageUrl: String? = null,
    iconVector: ImageVector? = null,
    playlistTracks: List<Track> = emptyList(),
    isSpecialLiked: Boolean = false,
    isSpecialDownload: Boolean = false,
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
        if (playlistTracks.isNotEmpty()) {
            PlaylistCollageCover(
                tracks = playlistTracks,
                modifier = Modifier.size(60.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            isSpecialLiked -> Brush.linearGradient(listOf(Color(0xFF5C3592), Color(0xFF2F2458)))
                            isSpecialDownload -> Brush.linearGradient(listOf(Color(0xFF1B4E43), Color(0xFF102824)))
                            else -> Brush.linearGradient(listOf(YtmGrey10, YtmGrey10))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSpecialLiked) {
                    Icon(
                        imageVector = Icons.Filled.ThumbUp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(28.dp)
                    )
                } else if (isSpecialDownload) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(28.dp)
                    )
                } else if (!imageUrl.isNullOrBlank()) {
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
                        tint = YtmWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSpecialLiked || isSpecialDownload) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = YtmTextSecondary,
                        modifier = Modifier
                            .size(12.dp)
                            .padding(end = 2.dp)
                    )
                }
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
    playlistTracks: List<Track> = emptyList(),
    isSpecialLiked: Boolean = false,
    isSpecialDownload: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        if (playlistTracks.isNotEmpty()) {
            PlaylistCollageCover(
                tracks = playlistTracks,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isSpecialLiked -> Brush.linearGradient(listOf(Color(0xFF5C3592), Color(0xFF2F2458)))
                            isSpecialDownload -> Brush.linearGradient(listOf(Color(0xFF1B4E43), Color(0xFF102824)))
                            else -> Brush.linearGradient(listOf(YtmGrey10, YtmGrey10))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSpecialLiked) {
                    Icon(
                        imageVector = Icons.Filled.ThumbUp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(54.dp)
                    )
                } else if (isSpecialDownload) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(54.dp)
                    )
                } else if (!imageUrl.isNullOrBlank()) {
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
                        tint = YtmWhite,
                        modifier = Modifier.size(42.dp)
                    )
                }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSpecialLiked || isSpecialDownload) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = null,
                    tint = YtmTextSecondary,
                    modifier = Modifier
                        .size(11.dp)
                        .padding(end = 2.dp)
                )
            }
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
}

/**
 * Tela de Detalhes Completa da Playlist do Usuário
 * Fiel ao print oficial do YouTube Music com colagem 2x2, botões de ação e FAB de play ao rolar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPlaylistDetailView(
    playlist: UserPlaylist,
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()
    val showFloatingPlay by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    var showEditTitleDialog by remember { mutableStateOf(false) }
    var editTitleText by remember { mutableStateOf(playlist.title) }

    // Diálogo de Busca para adicionar música à playlist
    var showAddSongSheet by remember { mutableStateOf(false) }
    var searchSongQuery by remember { mutableStateOf("") }
    val searchResult by viewModel.searchResult.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    var showPlaylistOptionsSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isPlaylistFilterActive by remember { mutableStateOf(false) }
    var playlistFilterQuery by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YtmBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header TopBar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = YtmWhite
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isPlaylistFilterActive = !isPlaylistFilterActive }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Pesquisar na playlist",
                            tint = if (isPlaylistFilterActive) OrangePrimary else YtmWhite
                        )
                    }

                    IconButton(onClick = { showPlaylistOptionsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opções da playlist",
                            tint = YtmWhite
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isPlaylistFilterActive) {
                OutlinedTextField(
                    value = playlistFilterQuery,
                    onValueChange = { playlistFilterQuery = it },
                    placeholder = { Text("Pesquisar na playlist...", color = YtmTextSecondary, fontSize = 14.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = YtmTextSecondary, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (playlistFilterQuery.isNotEmpty()) {
                            IconButton(onClick = { playlistFilterQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = YtmWhite, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = YtmWhite,
                        unfocusedTextColor = YtmWhite,
                        focusedBorderColor = YtmWhite,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            val displayedTracks = if (playlistFilterQuery.isBlank()) {
                playlist.tracks
            } else {
                playlist.tracks.filter {
                    it.title.contains(playlistFilterQuery, ignoreCase = true) ||
                    it.artist.contains(playlistFilterQuery, ignoreCase = true)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // 1. Capa em Colagem 2x2
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PlaylistCollageCover(
                            tracks = playlist.tracks,
                            modifier = Modifier
                                .size(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }

                // 2. Título & Metadados
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = playlist.title,
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = YtmWhite,
                            lineHeight = 32.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(OrangePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "V",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = YtmWhite
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Você • Pública • ${playlist.trackCount} músicas",
                                fontFamily = YouTubeSansFontFamily,
                                fontSize = 13.sp,
                                color = YtmTextSecondary
                            )
                        }
                    }
                }

                // 3. Fileira de Botões de Ação estilo YouTube Music
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Botões de ação circulares à esquerda
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Botão Download All
                            Surface(
                                shape = CircleShape,
                                color = YtmGrey10,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable {
                                        viewModel.downloadAll(playlist.tracks)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Baixar tudo",
                                        tint = YtmWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Botão Editar Título
                            Surface(
                                shape = CircleShape,
                                color = YtmGrey10,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { showEditTitleDialog = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = YtmWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Botão 3 pontinhos oficial
                            Surface(
                                shape = CircleShape,
                                color = YtmGrey10,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { showPlaylistOptionsSheet = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Mais opções",
                                        tint = YtmWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Botões de Reprodução à direita (Shuffle + Grande Play Branco)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Botão Aleatório
                            IconButton(
                                onClick = {
                                    viewModel.playUserPlaylist(playlist, isShuffle = true)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Aleatório",
                                    tint = YtmWhite,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Botão Grande Play Branco
                            Surface(
                                shape = CircleShape,
                                color = YtmWhite,
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable {
                                        viewModel.playUserPlaylist(playlist, isShuffle = false)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Tocar",
                                        tint = YtmBlack,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Item "+ Add a song"
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddSongSheet = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(YtmGrey10),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = YtmWhite,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "Adicionar uma música",
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = YtmWhite
                        )
                    }
                }

                // 5. Faixas da Playlist
                itemsIndexed(displayedTracks, key = { index, track -> "${track.id}_$index" }) { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.playUserPlaylist(playlist, startIndex = index, isShuffle = false)
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(YtmGrey10)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

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
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (downloadedTracks.any { it.track.id == track.id }) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Baixada",
                                        tint = YtmWhite.copy(alpha = 0.7f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
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

                        IconButton(onClick = {
                            viewModel.removeTrackFromPlaylist(playlist.id, track.id)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remover",
                                tint = YtmTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Botão Flutuante de Play no Rodapé à Direita (surge suavemente ao rolar para baixo)
        AnimatedVisibility(
            visible = showFloatingPlay,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    viewModel.playUserPlaylist(playlist, isShuffle = false)
                },
                containerColor = YtmWhite,
                contentColor = YtmBlack,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Tocar",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }

    // Modal para renomear playlist
    if (showEditTitleDialog) {
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            containerColor = Color(0xFF282828),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Editar playlist",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = YtmWhite
                )
            },
            text = {
                OutlinedTextField(
                    value = editTitleText,
                    onValueChange = { editTitleText = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = YtmWhite,
                        unfocusedTextColor = YtmWhite,
                        focusedBorderColor = YtmWhite,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitleText.isNotBlank()) {
                            viewModel.updatePlaylistTitle(playlist.id, editTitleText.trim())
                            showEditTitleDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YtmWhite, contentColor = YtmBlack),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(text = "Salvar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) {
                    Text(text = "Cancelar", color = YtmWhite)
                }
            }
        )
    }

    // Bottom Sheet para pesquisar e adicionar música à playlist
    if (showAddSongSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSongSheet = false },
            containerColor = YtmGrey11,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Adicionar à playlist",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = YtmWhite,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                OutlinedTextField(
                    value = searchSongQuery,
                    onValueChange = {
                        searchSongQuery = it
                        viewModel.onQueryChanged(it)
                        if (it.length >= 2) {
                            viewModel.performSearch()
                        }
                    },
                    placeholder = {
                        Text(text = "Pesquisar músicas...", color = YtmTextSecondary)
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = YtmTextSecondary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = YtmWhite,
                        unfocusedTextColor = YtmWhite,
                        focusedBorderColor = YtmWhite,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResult.songs, key = { it.id }) { song ->
                        val isAlreadyIn = playlist.tracks.any { it.id == song.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (!isAlreadyIn) {
                                        viewModel.addTrackToPlaylist(playlist.id, song)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
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
                                    text = song.title,
                                    fontFamily = YouTubeSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = YtmWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${song.artist} • ${song.durationText}",
                                    fontFamily = YouTubeSansFontFamily,
                                    fontSize = 12.sp,
                                    color = YtmTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = {
                                if (!isAlreadyIn) {
                                    viewModel.addTrackToPlaylist(playlist.id, song)
                                }
                            }) {
                                Icon(
                                    imageVector = if (isAlreadyIn) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = if (isAlreadyIn) "Já adicionada" else "Adicionar",
                                    tint = if (isAlreadyIn) Color(0xFF4CAF50) else YtmWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPlaylistOptionsSheet) {
        PlaylistOptionsBottomSheet(
            playlist = playlist,
            onDismiss = { showPlaylistOptionsSheet = false },
            onPlayNext = { viewModel.addTracksToPlayNext(playlist.tracks) },
            onShufflePlay = { viewModel.playUserPlaylist(playlist, isShuffle = true) },
            onSortManual = { /* ordem mantida */ },
            onFindInPlaylist = { isPlaylistFilterActive = true },
            onStartMix = {
                if (playlist.tracks.isNotEmpty()) {
                    viewModel.playTrack(playlist.tracks.first())
                }
            },
            onAddToQueue = { viewModel.addTracksToQueue(playlist.tracks) },
            onSaveToPlaylist = { showAddSongSheet = true },
            onDeletePlaylist = { showDeleteConfirmDialog = true },
            onPinToSpeedDial = { /* feedback */ },
            onHelpAndFeedback = { /* feedback */ }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Color(0xFF282828),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Excluir playlist?",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = YtmWhite
                )
            },
            text = {
                Text(
                    text = "Tem certeza de que deseja excluir '${playlist.title}'? Essa ação não pode ser desfeita.",
                    fontFamily = YouTubeSansFontFamily,
                    color = YtmTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        showDeleteConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorColor,
                        contentColor = YtmWhite
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(text = "Excluir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(text = "Cancelar", color = YtmWhite)
                }
            }
        )
    }
}

@Composable
fun TrackListDetailView(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    isLikedMusic: Boolean = false,
    isDownloads: Boolean = false,
    onBack: () -> Unit,
    onPlayTrack: (Track, Int) -> Unit,
    onShuffleAll: () -> Unit,
    onDownloadAll: (() -> Unit)? = null
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
            if (onDownloadAll != null) {
                IconButton(onClick = onDownloadAll) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Baixar todas",
                        tint = YtmWhite
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
                    onClick = { onPlayTrack(tracks.first(), 0) },
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
                itemsIndexed(tracks, key = { index, track -> "${track.id}_$index" }) { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPlayTrack(track, index) }
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isDownloads) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Baixada",
                                        tint = YtmWhite.copy(alpha = 0.7f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
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
}
