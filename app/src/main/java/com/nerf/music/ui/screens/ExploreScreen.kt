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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    onOpenSearch: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val homeSections by viewModel.homeSections.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

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
            // 3 Botões clássicos do topo da aba Explorar do YouTube Music
            item {
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
                        onClick = { onOpenSearch?.invoke() }
                    )
                    YtmExploreQuickButton(
                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                        label = "Paradas\nmusicais",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSearch?.invoke() }
                    )
                    YtmExploreQuickButton(
                        icon = Icons.Outlined.Mood,
                        label = "Momentos\ne gêneros",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSearch?.invoke() }
                    )
                }
            }

            items(homeSections, key = { "explore_${it.title}" }) { section ->
                val isQuickPicks = section.style == HomeSectionStyle.QUICK_PICKS_GRID
                YtmSectionHeader(
                    title = section.title,
                    actionLabel = if (isQuickPicks) "Reproduzir tudo" else null,
                    onActionClick = if (isQuickPicks) {
                        { viewModel.playSection(section, startIndex = 0) }
                    } else null
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (isQuickPicks) {
                    YtmQuickPicksGridCarousel(
                        tracks = section.items,
                        currentPlayingId = playbackState.currentTrack?.id,
                        onTrackClick = { track ->
                            val idx = section.items.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                            viewModel.playSection(section, startIndex = idx)
                        }
                    )
                } else {
                    YtmTwoRowCarousel(
                        tracks = section.items,
                        currentPlayingId = playbackState.currentTrack?.id,
                        onTrackClick = { track -> viewModel.playTrack(track) }
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
