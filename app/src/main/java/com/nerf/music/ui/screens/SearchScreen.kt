package com.nerf.music.ui.screens

import com.nerf.music.ui.components.applePressFeedback

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nerf.music.data.model.Artist
import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.Track
import com.nerf.music.ui.theme.*
import com.nerf.music.ui.viewmodel.MusicViewModel

/**
 * Tela de Pesquisa redesenhada réplica do YouTube Music:
 * - Foco automático na barra de busca com teclado virtual aberto ao entrar.
 * - Barra superior com voltar, campo de busca com microfone (pesquisa por voz nativa) e limpar.
 * - Estado vazio:
 *   1. "Recent searches": carrossel horizontal de faixas e álbuns tocados recentemente.
 *   2. "Histórico de pesquisas": consultas anteriores com ícone de relógio, setinha diagonal para autocompletar e remoção.
 * - Estado com busca ativa:
 *   - Filtros de pesquisa ("Geral", "Músicas").
 *   - Destaque do Artista (abre a nova tela ArtistScreen ao clicar).
 *   - Lista de Músicas com reprodução imediata.
 */
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onTrackOptionsClick: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    // Reconhecimento de voz nativo do Android
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.onQueryChanged(spokenText)
                viewModel.performSearch()
            }
        }
    }

    // Auto-foco na barra de pesquisa ao entrar na tela
    LaunchedEffect(Unit) {
        viewModel.loadSearchHistory()
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YtmBlack)
            .statusBarsPadding()
    ) {
        // Barra Superior de Pesquisa (YouTube Music style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onNavigateBack != null) {
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        onNavigateBack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = YtmWhite
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Input Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF212121))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Pesquise músicas, artistas...",
                                fontFamily = YouTubeSansFontFamily,
                                color = YtmTextSecondary,
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onQueryChanged(it) },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = YtmWhite,
                                fontSize = 15.sp,
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(OrangeSecondary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focusManager.clearFocus()
                                viewModel.performSearch()
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onQueryChanged("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpar busca",
                                tint = YtmWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Botão Microfone (Reconhecimento de Voz Nativo)
                    IconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale o que deseja ouvir")
                            }
                            try {
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Reconhecimento de voz indisponível", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Pesquisa por voz",
                            tint = YtmWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Conteúdo da Tela de Pesquisa
        if (searchQuery.isBlank()) {
            // ESTADO INICIAL: Recent Searches + Histórico de Pesquisas
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // 1. Carrossel de Buscas Recentes ("Recent searches")
                if (recentlyPlayed.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Buscas recentes",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = YtmWhite,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(recentlyPlayed) { track ->
                                    RecentSearchCard(
                                        track = track,
                                        onClick = {
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
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }

                // 2. Histórico de Consultas de Texto (com ícone de relógio e setinha)
                if (searchHistory.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Histórico de pesquisas",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = YtmWhite
                            )
                            Text(
                                text = "Limpar",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = YtmTextSecondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { viewModel.clearSearchHistory() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    items(searchHistory, key = { it }) { queryText ->
                        SearchHistoryRowItem(
                            query = queryText,
                            onQueryClick = {
                                focusManager.clearFocus()
                                viewModel.onQueryChanged(queryText)
                                viewModel.performSearch()
                            },
                            onInsertQuery = {
                                viewModel.onQueryChanged(queryText)
                            },
                            onRemoveQuery = {
                                viewModel.removeSearchHistory(queryText)
                            }
                        )
                    }
                }
            }
        } else {
            // ESTADO COM BUSCA ATIVA: Filtros + Resultados
            Column(modifier = Modifier.fillMaxSize()) {
                // Filtros de busca (Geral, Músicas)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchFilter.values().forEach { filter ->
                        val isSelected = filter == selectedFilter
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.onFilterSelected(filter) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) YtmWhite else YtmGrey11
                        ) {
                            Text(
                                text = filter.displayName,
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) YtmBlack else YtmWhite,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = YtmWhite,
                        trackColor = Color.Transparent
                    )
                }

                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = errorMessage ?: "",
                                fontFamily = YouTubeSansFontFamily,
                                color = YtmTextSecondary,
                                fontSize = 14.sp
                            )
                            if (errorMessage?.startsWith("Erro na busca:") == true) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "O catálogo pode estar indisponível ou a conexão pode ter caído.",
                                    fontFamily = YouTubeSansFontFamily,
                                    color = YtmTextSecondary,
                                    fontSize = 12.sp
                                )
                                TextButton(onClick = { viewModel.performSearch() }) {
                                    Text("Tentar novamente", color = YtmWhite)
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Destaque do Artista (Top Result)
                    val primaryArtist = searchResult.topArtist ?: searchResult.artists.firstOrNull()
                    if (primaryArtist != null) {
                        item {
                            Text(
                                text = "Artista",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = YtmWhite,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            ArtistResultCard(
                                artist = primaryArtist,
                                onClick = { onOpenArtist(primaryArtist.id) }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // Músicas encontradas
                    items(searchResult.songs, key = { it.id }) { track ->
                        val isPlaying = track.id == playbackState.currentTrack?.id
                        SearchSongRowItem(
                            track = track,
                            isPlaying = isPlaying,
                            onClick = {
                                if (track.isAlbumOrPlaylist || !track.browseId.isNullOrBlank()) {
                                    onOpenAlbum(track.browseId ?: track.id)
                                } else {
                                    viewModel.playTrack(track)
                                }
                            },
                            onOptionsClick = { onTrackOptionsClick(track) }
                        )
                    }

                    // Outros Artistas
                    if (searchResult.artists.size > 1) {
                        item {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Outros Artistas",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = YtmWhite,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(searchResult.artists.drop(1), key = { it.id }) { artist ->
                            ArtistResultCard(
                                artist = artist,
                                onClick = { onOpenArtist(artist.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card para o carrossel de Buscas Recentes ("Recent searches").
 */
@Composable
private fun RecentSearchCard(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(115.dp)
            .clip(RoundedCornerShape(8.dp))
            .applePressFeedback(targetScale = 0.97f, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(115.dp)
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
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = YtmIconInactive,
                    modifier = Modifier.size(36.dp).align(Alignment.Center)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = track.title,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            color = YtmWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            fontFamily = YouTubeSansFontFamily,
            fontSize = 11.5.sp,
            color = YtmTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Linha de item do histórico de pesquisa com ícone de relógio e setinha diagonal.
 */
@Composable
private fun SearchHistoryRowItem(
    query: String,
    onQueryClick: () -> Unit,
    onInsertQuery: () -> Unit,
    onRemoveQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onQueryClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = YtmTextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = query,
            fontFamily = YouTubeSansFontFamily,
            fontSize = 14.5.sp,
            color = YtmWhite,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(
            onClick = onInsertQuery,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NorthWest,
                contentDescription = "Completar pesquisa",
                tint = YtmTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = onRemoveQuery,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remover do histórico",
                tint = YtmIconInactive,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Card de Artista nos resultados de busca com avatar circular.
 */
@Composable
private fun ArtistResultCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .applePressFeedback(targetScale = 0.98f, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
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
                    modifier = Modifier.size(28.dp).align(Alignment.Center)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = YtmWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = artist.subtitle ?: "Artista",
                fontFamily = YouTubeSansFontFamily,
                fontSize = 13.sp,
                color = YtmTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Linha de resultado para faixa musical.
 */
@Composable
private fun SearchSongRowItem(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .applePressFeedback(targetScale = 0.98f, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
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
        Column(modifier = Modifier.weight(1f)) {
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
        IconButton(onClick = onOptionsClick, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Mais opções",
                tint = YtmIconInactive,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
