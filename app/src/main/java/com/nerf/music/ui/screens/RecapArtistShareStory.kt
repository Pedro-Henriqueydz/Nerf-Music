package com.nerf.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nerf.music.data.model.RecapArtistStats
import com.nerf.music.data.model.RecapSummary
import com.nerf.music.ui.theme.SfProFontFamily
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private data class ArtistShare(
    val name: String,
    val percent: Int,
    val rank: Int?,
    val isOther: Boolean,
    val imageNames: List<String>,
    val fallbackImageUrl: String?
)

private val MosaicColors = listOf(
    Color(0xFFB64F59), Color(0xFFA44E79), Color(0xFF76679D),
    Color(0xFF577F70), Color(0xFFB58C52), Color(0xFF57545E)
)

@Composable
internal fun RecapArtistShareStory(
    summary: RecapSummary,
    revealStep: Int,
    resolveArtistImage: suspend (String) -> String?
) {
    val shares = remember(summary.topArtists) { artistListeningShares(summary.topArtists) }
    var artistImages by remember(summary.topArtists) { mutableStateOf(emptyMap<String, String>()) }

    LaunchedEffect(shares) {
        val names = shares.flatMap { it.imageNames }.distinct()
        artistImages = coroutineScope {
            names.map { name ->
                async { name to runCatching { resolveArtistImage(name) }.getOrNull() }
            }.awaitAll().mapNotNull { (name, url) ->
                url?.takeIf(String::isNotBlank)?.let { name to it }
            }.toMap()
        }
    }

    Column(
        Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = 158.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.Top
    ) {
        AnimatedVisibility(visible = revealStep >= 1, enter = artistStoryEnter(12)) {
            Text(
                "O SOM QUE VIROU A SUA ASSINATURA",
                fontFamily = SfProFontFamily,
                fontSize = 10.sp,
                letterSpacing = 1.7.sp,
                color = Color.White.copy(alpha = .58f)
            )
        }
        Spacer(Modifier.height(20.dp))
        AnimatedVisibility(visible = revealStep >= 2, enter = artistStoryEnter(10)) {
            Text(
                "Quem fez parte\ndo seu som.",
                fontFamily = SfProFontFamily,
                fontSize = 42.sp,
                lineHeight = 46.sp,
                letterSpacing = (-1).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8F7F2),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(8.dp))
        AnimatedVisibility(visible = revealStep >= 3, enter = artistStoryEnter(8)) {
            Text(
                "Sua escuta, dividida entre artistas.",
                fontFamily = SfProFontFamily,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = .7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(42.dp))
        AnimatedVisibility(visible = revealStep >= 4, enter = artistStoryEnter(8)) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "100% DA ESCUTA",
                        fontFamily = SfProFontFamily,
                        fontSize = 10.sp,
                        letterSpacing = .7.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = .64f)
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (shares.isEmpty()) {
                    Text(
                        "Continue ouvindo e seu Recap vai ganhar vida com as suas descobertas.",
                        fontFamily = SfProFontFamily,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = .72f)
                    )
                } else {
                    ArtistShareMosaic(shares, artistImages)
                }
            }
        }
        if (revealStep >= 5) {
            Spacer(Modifier.height(18.dp))
            AnimatedVisibility(visible = true, enter = artistStoryEnter(6)) {
                Text(
                    "Artistas que fizeram parte da sua trilha.",
                    fontFamily = SfProFontFamily,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = .66f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Sua assinatura sonora em ${summary.year}.",
                fontFamily = SfProFontFamily,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = .62f)
            )
        }
    }
}

@Composable
private fun ArtistShareMosaic(shares: List<ArtistShare>, images: Map<String, String>) {
    val featured = shares.first()
    val supporting = shares.drop(1)
    val supportingWeight = supporting.sumOf { it.percent.coerceAtLeast(1) }.toFloat()
    val stackGap = 3.dp
    val stackSpace = (300.dp - stackGap * (supporting.size - 1).coerceAtLeast(0)).coerceAtLeast(0.dp)
    val minimumTileHeight = if (supporting.isEmpty()) 0.dp else 38.dp.coerceAtMost(stackSpace / supporting.size)
    val proportionalSpace = (stackSpace - minimumTileHeight * supporting.size).coerceAtLeast(0.dp)
    Row(
        Modifier.fillMaxWidth().height(300.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ArtistMosaicTile(
            share = featured,
            index = 0,
            color = MosaicColors[0],
            featured = true,
            images = images,
            modifier = Modifier.weight(featured.percent.coerceAtLeast(1).toFloat()).fillMaxHeight()
        )
        if (supporting.isNotEmpty()) {
            Column(
                Modifier.weight(supportingWeight).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(stackGap)
            ) {
                supporting.forEachIndexed { index, share ->
                    val tileHeightWeight = minimumTileHeight.value +
                        proportionalSpace.value * share.percent.coerceAtLeast(1) / supportingWeight
                    ArtistMosaicTile(
                        share = share,
                        index = index + 1,
                        color = MosaicColors[(index + 1).coerceAtMost(MosaicColors.lastIndex)],
                        featured = false,
                        images = images,
                        modifier = Modifier.weight(tileHeightWeight).fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistMosaicTile(
    share: ArtistShare,
    index: Int,
    color: Color,
    featured: Boolean,
    images: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reveal = remember(share.name) { Animatable(0f) }
    val count = remember(share.name) { Animatable(0f) }

    LaunchedEffect(share.percent) {
        delay(index * 54L)
        coroutineScope {
            launch { reveal.animateTo(1f, spring(dampingRatio = .88f, stiffness = 420f)) }
            launch { count.animateTo(share.percent.toFloat(), spring(dampingRatio = 1f, stiffness = 74f)) }
        }
    }

    val shape = RoundedCornerShape(if (featured) 18.dp else 14.dp)
    val innerPadding = if (featured) 12.dp else if (share.percent <= 6) 4.dp else 8.dp
    Box(
        modifier.graphicsLayer {
            alpha = reveal.value
            translationY = (1f - reveal.value) * 10.dp.toPx()
            scaleX = .985f + reveal.value * .015f
            scaleY = .985f + reveal.value * .015f
        }
            .clip(shape)
            .background(color)
            .border(.75.dp, Color.White.copy(alpha = .13f), shape)
    ) {
        if (share.isOther) {
            Row(Modifier.fillMaxSize()) {
                share.imageNames.take(3).forEachIndexed { imageIndex, name ->
                    val image = images[name] ?: share.fallbackImageUrl
                    if (image != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(image).crossfade(320).build(),
                            contentDescription = name,
                            modifier = Modifier.weight(1f).fillMaxHeight().alpha(.64f),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            Modifier.weight(1f).fillMaxHeight()
                                .background(MosaicColors[(imageIndex + 1).coerceAtMost(MosaicColors.lastIndex)])
                        )
                    }
                }
            }
        } else {
            val image = images[share.name] ?: share.fallbackImageUrl
            if (image != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(image).crossfade(320).build(),
                    contentDescription = share.name,
                    modifier = Modifier.fillMaxSize().alpha(.67f),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = .08f), Color.Black.copy(alpha = .66f))
                )
            )
        )

        val percentText = "${count.value.roundToInt().coerceIn(0, share.percent)}%"
        if (featured) {
            Column(
                Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "ARTISTA Nº 1",
                    fontFamily = SfProFontFamily,
                    fontSize = 9.sp,
                    letterSpacing = .9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = .86f),
                    maxLines = 1
                )
                Text(
                    share.name.take(1).uppercase(Locale.getDefault()),
                    fontFamily = SfProFontFamily,
                    fontSize = 58.sp,
                    lineHeight = 62.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = .24f)
                )
                Column {
                    Text(
                        share.name,
                        fontFamily = SfProFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        percentText,
                        fontFamily = SfProFontFamily,
                        fontSize = 31.sp,
                        lineHeight = 34.sp,
                        letterSpacing = (-.8).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        "DA SUA ESCUTA",
                        fontFamily = SfProFontFamily,
                        fontSize = 8.sp,
                        letterSpacing = .7.sp,
                        color = Color.White.copy(alpha = .88f)
                    )
                }
            }
        } else {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.CenterStart) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (share.rank != null) {
                        Text(
                            share.rank.toString().padStart(2, '0'),
                            fontFamily = SfProFontFamily,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = .78f)
                        )
                    }
                    Text(
                        share.name,
                        modifier = Modifier.weight(1f),
                        fontFamily = SfProFontFamily,
                        fontSize = if (share.percent <= 6) 10.sp else 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        percentText,
                        fontFamily = SfProFontFamily,
                        fontSize = if (share.percent <= 6) 10.sp else 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun artistListeningShares(artists: List<RecapArtistStats>): List<ArtistShare> {
    val ranked = artists
        .map { artist -> artist to artist.listenedMs }
        .filter { it.second > 0L }
        .sortedWith(compareByDescending<Pair<RecapArtistStats, Long>> { it.second }.thenByDescending { it.first.plays })
    if (ranked.isEmpty()) return emptyList()

    val top = ranked.take(5)
    val others = ranked.drop(5)
    val categories = top.map { (artist, weight) ->
        ArtistShare(
            name = artist.name,
            percent = 0,
            rank = null,
            isOther = false,
            imageNames = listOf(artist.name),
            fallbackImageUrl = artist.representativeTrack?.thumbnailUrl
        ) to weight.toDouble()
    } + listOfNotNull(
        if (others.isNotEmpty()) {
            ArtistShare(
                name = "Outros artistas",
                percent = 0,
                rank = null,
                isOther = true,
                imageNames = others.take(3).map { it.first.name },
                fallbackImageUrl = others.firstOrNull()?.first?.representativeTrack?.thumbnailUrl
            ) to others.sumOf { it.second }.toDouble()
        } else null
    )

    val total = categories.sumOf { it.second }
    val raw = categories.map { it.second * 100.0 / total }
    val whole = raw.map { it.toInt() }.toMutableList()
    val remaining = 100 - whole.sum()
    raw.indices.sortedByDescending { raw[it] - whole[it] }.take(remaining).forEach { whole[it]++ }

    return categories.mapIndexed { index, (artistShare, _) ->
        artistShare.copy(
            percent = whole[index],
            rank = if (artistShare.isOther) null else index + 1
        )
    }
}

private fun artistStoryEnter(offsetFraction: Int) =
    fadeIn(spring(dampingRatio = .92f, stiffness = 420f)) +
        slideInVertically(initialOffsetY = { it / offsetFraction }, animationSpec = spring(dampingRatio = .88f, stiffness = 420f)) +
        scaleIn(initialScale = .98f, animationSpec = spring(dampingRatio = .9f, stiffness = 420f))
