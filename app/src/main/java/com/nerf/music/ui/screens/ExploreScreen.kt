package com.nerf.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerf.music.data.model.HomeSectionStyle
import com.nerf.music.ui.theme.*
import com.nerf.music.ui.viewmodel.MusicViewModel

@Composable
fun ExploreScreen(
    viewModel: MusicViewModel,
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val homeSections by viewModel.exploreSections.collectAsState()
    val isLoading by viewModel.exploreLoading.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadExplore(force = true) }
    val playbackState by viewModel.playbackState.collectAsState()
    val moods by viewModel.moodCategories.collectAsState()
    val moodError by viewModel.moodError.collectAsState()
    val moodSections by viewModel.moodSections.collectAsState()
    val moodLoading by viewModel.moodLoading.collectAsState()
    val charts by viewModel.chartsSections.collectAsState()
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMood by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMoodParams by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(category, selectedMoodParams) {
        if (category == "Momentos e gêneros" && selectedMoodParams != null) viewModel.loadMoodFeed(selectedMoodParams!!)
    }
    LaunchedEffect(category) {
        if (category == "Momentos e gêneros" && moods.isEmpty()) viewModel.loadMoodCategories()
        if (category == "Paradas musicais") viewModel.loadCharts()
    }
    BackHandler(enabled = category != null) { if (selectedMood != null) { selectedMood = null; selectedMoodParams = null } else category = null }
    val displayedSections = when (category) {
        "Novos lançamentos" -> homeSections.filter { it.title.contains("lançamento", true) || it.title.contains("álbu", true) || it.title.contains("release", true) }
        "Paradas musicais" -> charts
        "Momentos e gêneros" -> if (selectedMood == null) emptyList() else moodSections
        else -> homeSections
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YtmBlack)
    ) {
        YtmTopBar()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            if (isLoading && homeSections.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = YtmWhite)
                    }
                }
            } else if (homeSections.isEmpty()) {
                item {
                    Text("Não foi possível carregar o Explorar. Toque para tentar novamente.",
                        color = YtmWhite, modifier = Modifier.fillMaxWidth().clickable { viewModel.loadExplore() }.padding(24.dp))
                }
            }
            // 3 Botões clássicos do topo da aba Explorar do YouTube Music
            if (category != null) item {
                Text("‹  ${selectedMood ?: category}", color = YtmWhite, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().clickable { if (selectedMood != null) { selectedMood = null; selectedMoodParams = null } else category = null }.padding(horizontal = 16.dp, vertical = 12.dp))
            }
            if (category == null) item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    YtmExploreQuickButton(
                        icon = Icons.Outlined.Album,
                        label = "Novos\nlançamentos",
                        modifier = Modifier.weight(1f),
                        onClick = { category = "Novos lançamentos" }
                    )
                    YtmExploreQuickButton(
                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                        label = "Paradas\nmusicais",
                        modifier = Modifier.weight(1f),
                        onClick = { category = "Paradas musicais" }
                    )
                    YtmExploreQuickButton(
                        icon = Icons.Outlined.Mood,
                        label = "Momentos\ne gêneros",
                        modifier = Modifier.weight(1f),
                        onClick = { category = "Momentos e gêneros" }
                    )
                }
            }

            if (category == "Momentos e gêneros" && selectedMood == null) {
                if (moodError != null) item {
                    Text(moodError.orEmpty(), color = YtmWhite,
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.loadMoodCategories() }.padding(24.dp))
                }
                items(moods.chunked(2)) { row ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { mood ->
                            Surface(modifier = Modifier.weight(1f).height(72.dp).clickable { selectedMood = mood.first; selectedMoodParams = mood.second }, shape = RoundedCornerShape(8.dp), color = YtmGrey11) {
                                Box(Modifier.padding(14.dp), contentAlignment = Alignment.CenterStart) { Text(mood.first, color = YtmWhite, fontWeight = FontWeight.Bold) }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (category == "Momentos e gêneros" && moodLoading) item { CircularProgressIndicator(modifier = Modifier.padding(24.dp), color = YtmWhite) }
            if (category != null && displayedSections.isEmpty() && !isLoading && !moodLoading && (category != "Momentos e gêneros" || selectedMood != null)) item {
                Text("Nenhum conteúdo disponível nesta categoria no momento. Toque para tentar novamente.", color = YtmWhite,
                    modifier = Modifier.clickable { if (category == "Momentos e gêneros" && selectedMoodParams != null) viewModel.loadMoodFeed(selectedMoodParams!!) else if (category == "Paradas musicais") viewModel.loadCharts() else viewModel.loadExplore(force = true) }.padding(24.dp))
            }
            items(displayedSections) { section ->
                val isQuickPicks = section.style == HomeSectionStyle.QUICK_PICKS_GRID
                YtmSectionHeader(
                    title = section.title,
                    strapline = section.strapline,
                    actionLabel = if (isQuickPicks) "Reproduzir tudo" else null,
                    onActionClick = if (isQuickPicks) {
                        { viewModel.playSection(section, startIndex = 0) }
                    } else null
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (section.items.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isLoading) "Carregando lançamentos…" else "O catálogo não retornou lançamentos recentes compatíveis com suas recomendações atuais.",
                            color = YtmTextSecondary,
                            fontSize = 14.sp
                        )
                        if (!isLoading) Text(
                            text = "Tentar novamente", color = YtmWhite,
                            modifier = Modifier.clickable { viewModel.loadExplore(force = true) }.padding(vertical = 12.dp)
                        )
                    }
                } else if (isQuickPicks) {
                    YtmQuickPicksGridCarousel(
                        tracks = section.items,
                        currentPlayingId = playbackState.currentTrack?.id,
                        onTrackClick = { track ->
                            if (section.title == "Top artistas" && track.artistBrowseId != null) onOpenArtist(track.artistBrowseId)
                            else if (track.isAlbumOrPlaylist) onOpenAlbum(track.browseId ?: track.id)
                            else {
                                val idx = section.items.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                viewModel.playSection(section, startIndex = idx)
                            }
                        }
                    )
                } else {
                    YtmTwoRowCarousel(
                        tracks = section.items,
                        currentPlayingId = playbackState.currentTrack?.id,
                        onTrackClick = { track ->
                            if (section.title == "Top artistas" && track.artistBrowseId != null) onOpenArtist(track.artistBrowseId)
                            else if (track.isAlbumOrPlaylist) onOpenAlbum(track.browseId ?: track.id)
                            else viewModel.playTrack(track)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun YtmExploreQuickButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = YtmGrey11
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = YtmWhite,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                color = YtmWhite
            )
        }
    }
}
