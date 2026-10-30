package com.nerf.music.ui.screens

import android.graphics.Bitmap
import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil.compose.AsyncImage
import com.nerf.music.data.model.RecapArtistStats
import com.nerf.music.data.model.RecapSummary
import com.nerf.music.data.model.RecapTrackStats
import com.nerf.music.data.model.Track
import com.nerf.music.ui.theme.SfProFontFamily
import com.nerf.music.util.SocialShareHelper
import com.nerf.music.util.RecapVideoExporter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Ink = Color(0xFF09090B)
private val Mint = Color(0xFFB7F3D0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecapScreen(
    summary: RecapSummary,
    onBack: () -> Unit,
    onPlayRecapMix: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    resolveArtistImage: suspend (String) -> String?
) {
    val context = LocalContext.current
    // Snapshot the year when the recap opens; playback of the recap mix must not rewrite its own story.
    var recapSnapshot by remember(summary.year) { mutableStateOf<RecapSummary?>(null) }
    LaunchedEffect(summary) {
        if (recapSnapshot == null && summary.hasEnoughData) recapSnapshot = summary
    }
    val stableSummary = recapSnapshot ?: summary
    val storySummary = stableSummary
    val window = (context as? Activity)?.window
    val view = LocalView.current
    DisposableEffect(window, view) {
        val wasVisible = ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: true
        window?.let { WindowCompat.getInsetsController(it, view).hide(WindowInsetsCompat.Type.statusBars()) }
        onDispose {
            if (wasVisible) window?.let { WindowCompat.getInsetsController(it, view).show(WindowInsetsCompat.Type.statusBars()) }
        }
    }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 6 })
    val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val storyProgress = remember { Animatable(0f) }
    var mixStarted by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var shareBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(false) }
    var isExportingVideo by remember { mutableStateOf(false) }
    var shareError by remember { mutableStateOf<String?>(null) }
    var isSavingImage by remember { mutableStateOf(false) }

    LaunchedEffect(storySummary.year, storySummary.mixTracks.isNotEmpty()) {
        if (storySummary.mixTracks.isNotEmpty() && !mixStarted) {
            mixStarted = true
            onPlayRecapMix()
        }
    }

    LaunchedEffect(showShareSheet, isExportingVideo) {
        if (showShareSheet || isExportingVideo) {
            storyProgress.snapTo(0f)
        } else {
            snapshotFlow { pagerState.settledPage }.collectLatest { settledPage ->
                storyProgress.snapTo(0f)
                if (settledPage < 5) {
                    storyProgress.animateTo(1f, tween(8_500, easing = LinearEasing))
                    if (pagerState.settledPage == settledPage) {
                        pagerState.animateScrollToPage(settledPage + 1, animationSpec = spring(dampingRatio = .88f, stiffness = 420f))
                    }
                } else storyProgress.snapTo(1f)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            pageSpacing = 0.dp,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = spring(dampingRatio = .88f, stiffness = 420f)
            )
        ) { page ->
            RecapStoryPage(page, storySummary, pagerState.currentPage == page, onBack, onPlayTrack, resolveArtistImage) {
                showShareSheet = true
                if (shareBitmap == null && !isRendering) {
                    isRendering = true
                    scope.launch {
                        runCatching { SocialShareHelper.renderRecapShareCard(context, storySummary) }
                            .onSuccess { shareBitmap = it }
                            .onFailure { shareError = it.message ?: "Erro ao montar imagem" }
                        isRendering = false
                    }
                }
            }
        }

        // Story controls sit on the clear side gutters so content buttons remain tappable.
        Box(Modifier.align(Alignment.CenterStart).width(42.dp).fillMaxHeight().padding(top = 82.dp, bottom = 76.dp)
            .pointerInput(pagerState.currentPage) {
                detectTapGestures {
                    if (pagerState.currentPage > 0) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = spring(dampingRatio = .88f, stiffness = 420f)) }
                }
            })
        Box(Modifier.align(Alignment.CenterEnd).width(42.dp).fillMaxHeight().padding(top = 82.dp, bottom = 76.dp)
            .pointerInput(pagerState.currentPage) {
                detectTapGestures {
                    if (pagerState.currentPage < 5) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = spring(dampingRatio = .88f, stiffness = 420f)) }
                }
            })

        Row(Modifier.padding(horizontal = 28.dp).padding(top = 26.dp).align(Alignment.TopCenter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(6) { index ->
                val selected = pagerState.currentPage == index
                Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = if (selected) .25f else .42f))) {
                    val fill = when {
                        index < pagerState.currentPage || (index == 5 && pagerState.currentPage == 5) -> 1f
                        index > pagerState.currentPage -> 0f
                        else -> storyProgress.value
                    }
                    Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(Color.White))
                }
            }
        }
        if (showShareSheet) {
            ModalBottomSheet(
                onDismissRequest = { showShareSheet = false },
                sheetState = shareSheetState,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                scrimColor = Color.Black.copy(.68f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = null,
                tonalElevation = 0.dp
            ) {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 600.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(Color(0xF5111317))
                        .border(.75.dp, Color.White.copy(.14f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp).padding(top = 32.dp, bottom = 16.dp)
                ) {
                    Text("Compartilhar seu Recap", fontFamily = SfProFontFamily, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF7F6F2))
                    Text("Sua hist\u00f3ria em um card, pronta para guardar.", fontFamily = SfProFontFamily, fontSize = 14.sp, color = Color.White.copy(.64f), modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 18.dp).height(92.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(.05f)).border(.75.dp, Color.White.copy(.11f), RoundedCornerShape(20.dp)).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        val lead = summary.topTracks.firstOrNull()?.track
                        Box(Modifier.size(58.dp).clip(CircleShape).background(Color(0xFFA94C77))) {
                            lead?.let { AsyncImage(it.highResThumbnailUrl ?: it.thumbnailUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        }
                        Column(Modifier.padding(start = 14.dp)) {
                            Text("NERF RECAP ${storySummary.year}", fontFamily = SfProFontFamily, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFF4F2EE))
                            Text("${formattedRecapDuration(storySummary.totalMinutes)}  ·  ${storySummary.topArtist?.name ?: "seu artista favorito"}  ·  ${lead?.title ?: "sua faixa número um"}", fontFamily = SfProFontFamily, fontSize = 10.sp, color = Color.White.copy(.6f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    shareError?.let { Text(it, color = Color(0xFFFFA6A6), fontFamily = SfProFontFamily, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
                    Column(Modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RecapActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.SaveAlt,
                                label = if (isSavingImage) "Salvando" else "Salvar",
                                enabled = shareBitmap != null && !isSavingImage
                            ) {
                                val bitmap = shareBitmap ?: return@RecapActionButton
                                isSavingImage = true
                                scope.launch {
                                    runCatching {
                                        SocialShareHelper.saveBitmapToGallery(context, bitmap, "NERF-Recap-${storySummary.year}.png", "Pictures/NERF Music/Recap")
                                    }.onSuccess {
                                        Toast.makeText(context, "Imagem salva em Pictures/NERF Music/Recap", Toast.LENGTH_LONG).show()
                                    }.onFailure { error ->
                                        Toast.makeText(context, "Falha ao salvar: ${error.message}", Toast.LENGTH_LONG).show()
                                    }
                                    isSavingImage = false
                                }
                            }
                            RecapActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.IosShare,
                                label = "Compartilhar",
                                enabled = shareBitmap != null
                            ) {
                                shareBitmap?.let { bitmap -> scope.launch {
                                    runCatching { SocialShareHelper.shareBitmap(context, bitmap, "NERF Recap ${summary.year}", "https://nerfmusic.app/recap") }
                                        .onFailure { error -> Toast.makeText(context, "Falha ao compartilhar: ${error.message}", Toast.LENGTH_LONG).show() }
                                } }
                            }
                        }
                        RecapActionButton(
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.Download,
                            label = if (isExportingVideo) "Gerando v\u00eddeo..." else "Baixar Recap em v\u00eddeo",
                            enabled = !isExportingVideo
                        ) {
                            val exportWindow = window ?: return@RecapActionButton
                            isExportingVideo = true
                            showShareSheet = false
                            scope.launch {
                                Toast.makeText(context, "Preparando o v\u00eddeo do Recap\u2026", Toast.LENGTH_SHORT).show()
                                runCatching {
                                    delay(350)
                                    pagerState.scrollToPage(0)
                                    storyProgress.snapTo(0f)
                                    delay(80)
                                    RecapVideoExporter.export(
                                        context = context,
                                        window = exportWindow,
                                        width = view.width,
                                        height = view.height,
                                        displayName = "NERF-Recap-${storySummary.year}.mp4"
                                    ) {
                                        for (page in 0 until 6) {
                                            if (page > 0) pagerState.animateScrollToPage(page, animationSpec = spring(dampingRatio = .88f, stiffness = 420f))
                                            storyProgress.snapTo(0f)
                                            storyProgress.animateTo(1f, tween(if (page == 5) 4_100 else 3_500, easing = LinearEasing))
                                        }
                                    }
                                }.onSuccess {
                                    Toast.makeText(context, "V\u00eddeo salvo em Filmes/NERF Music/Recap", Toast.LENGTH_LONG).show()
                                }.onFailure { error ->
                                    Toast.makeText(context, "Falha ao criar v\u00eddeo: ${error.message}", Toast.LENGTH_LONG).show()
                                }
                                isExportingVideo = false
                            }
                        }
                    }
                    Text("Agora não", fontFamily = SfProFontFamily, fontSize = 13.sp, color = Color.White.copy(.72f), modifier = Modifier.align(Alignment.CenterHorizontally).clickable { showShareSheet = false }.padding(top = 14.dp, bottom = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun LegacyRecapStoryPage(page: Int, summary: RecapSummary, onPlayTrack: (Track) -> Unit, onShare: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(start = 28.dp, end = 28.dp, top = 82.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start
    ) {
        when (page) {
            0 -> {
                Text("NERF MUSIC  ·  ${summary.year}", fontFamily = SfProFontFamily, fontSize = 13.sp, letterSpacing = 2.sp, color = Mint)
                Spacer(Modifier.height(24.dp))
                Text("O seu ano\ntinha um som.", fontFamily = SfProFontFamily, fontSize = 54.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(18.dp))
                Text("Aqui está a trilha sonora que você construiu.", fontFamily = SfProFontFamily, fontSize = 18.sp, color = Color.White.copy(.76f))
            }
            1 -> {
                Text("TEMPO BEM GASTO", fontFamily = SfProFontFamily, fontSize = 13.sp, letterSpacing = 2.sp, color = Mint)
                Spacer(Modifier.height(24.dp))
                Text("${summary.totalMinutes / 60}", fontFamily = SfProFontFamily, fontSize = 92.sp, lineHeight = 96.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("horas em movimento", fontFamily = SfProFontFamily, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(18.dp))
                Text("${summary.totalPlays} reproduções que fizeram parte do seu ${summary.year}.", fontFamily = SfProFontFamily, fontSize = 17.sp, color = Color.White.copy(.7f))
            }
            2 -> {
                val stat = summary.topTracks.firstOrNull()
                Text("NO REPEAT", fontFamily = SfProFontFamily, fontSize = 13.sp, letterSpacing = 2.sp, color = Mint)
                Spacer(Modifier.height(24.dp))
                stat?.let {
                    AsyncImage(it.track.highResThumbnailUrl ?: it.track.thumbnailUrl, it.track.title, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.height(20.dp))
                    Text(it.track.title, fontFamily = SfProFontFamily, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(it.track.artist, fontFamily = SfProFontFamily, fontSize = 17.sp, color = Color.White.copy(.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${it.plays} reproduções", fontFamily = SfProFontFamily, fontSize = 14.sp, color = Mint, modifier = Modifier.padding(top = 9.dp))
                    StoryButton("Tocar faixa", Icons.Default.PlayArrow) { onPlayTrack(it.track) }
                } ?: EmptyRecap()
            }
            3 -> {
                Text("A SUA ROTA MUSICAL", fontFamily = SfProFontFamily, fontSize = 13.sp, letterSpacing = 2.sp, color = Mint)
                Spacer(Modifier.height(22.dp))
                summary.topArtists.take(5).forEachIndexed { i, artist -> ArtistRow(i, artist) }
                if (summary.topArtists.isEmpty()) EmptyRecap()
            }
            else -> {
                Text("FAIXAS QUE FICARAM", fontFamily = SfProFontFamily, fontSize = 13.sp, letterSpacing = 2.sp, color = Mint)
                Spacer(Modifier.height(18.dp))
                summary.topTracks.take(5).forEachIndexed { i, stat -> TrackRow(i, stat, onPlayTrack) }
                Spacer(Modifier.height(18.dp))
                StoryButton("Compartilhar meu Recap", Icons.Default.IosShare, onShare)
            }
        }
    }
}

@Composable
private fun RecapStoryPage(
    page: Int,
    summary: RecapSummary,
    isActive: Boolean,
    onBack: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    resolveArtistImage: suspend (String) -> String?,
    onShare: () -> Unit
) {
    var revealStep by remember(page) { mutableIntStateOf(0) }
    LaunchedEffect(page, isActive) {
        if (isActive) {
            revealStep = 0
            delay(40)
            revealStep = 1
            val stages = when (page) { 0 -> 3; 1 -> 4; 2 -> 4; 3 -> 6; 4 -> 5; else -> 5 }
            for (stage in 2..stages) {
                delay(if (page == 3 || page == 4) 115 else 160)
                revealStep = stage
            }
        } else revealStep = 0
    }
    Box(Modifier.fillMaxSize().background(Ink)) {
        RecapAmbientBackground(page)
        Row(Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = 46.dp).clickable(onClick = onBack), verticalAlignment = Alignment.CenterVertically) {
            Text("NERF  /  RECAP ${summary.year}", fontFamily = SfProFontFamily, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(.72f))
            Box(Modifier.padding(start = 8.dp).size(7.dp).clip(CircleShape).background(Mint))
        }
        if (page == 0) {
            RecapCoverStory(summary, revealStep)
        } else if (page == 4) {
            RecapArtistShareStory(summary, revealStep, resolveArtistImage)
        } else Column(
            Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = 182.dp, bottom = 32.dp)
                .animateContentSize(spring(dampingRatio = .9f, stiffness = 420f)),
            verticalArrangement = Arrangement.Top
        ) {
            when (page) {
                1 -> {
                    val artist = summary.topArtist
                    StagedRecapBlock(revealStep >= 1) {
                        RecapEyebrow("UM NOME QUE SEMPRE VOLTAVA")
                        Spacer(Modifier.height(44.dp))
                    }
                    StagedRecapBlock(revealStep >= 2) {
                        Text("Você e a\n${artist?.name ?: "sua música"}.", fontFamily = SfProFontFamily, fontSize = 42.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(26.dp))
                    }
                    StagedRecapBlock(revealStep >= 3) {
                        Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(.055f))) {
                            Box(Modifier.align(Alignment.Center).size(150.dp).background(Brush.radialGradient(listOf(Color(0xFFFFCE76), Color(0xFFE68858), Color(0xFFD66772)), center = androidx.compose.ui.geometry.Offset.Unspecified), CircleShape))
                            artist?.representativeTrack?.let { track -> AsyncImage(track.highResThumbnailUrl ?: track.thumbnailUrl, null, Modifier.align(Alignment.Center).size(110.dp).clip(CircleShape), contentScale = ContentScale.Crop) }
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                    StagedRecapBlock(revealStep >= 4) {
                        Text(artist?.name ?: "Seu artista mais ouvido", fontFamily = SfProFontFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Seu artista mais ouvido no ano.", fontFamily = SfProFontFamily, fontSize = 15.sp, color = Color.White.copy(.62f))
                    }
                }
                2 -> {
                    val stat = summary.topTracks.firstOrNull()
                    StagedRecapBlock(revealStep >= 1) {
                        RecapEyebrow("A FAIXA VIROU CASA")
                        Spacer(Modifier.height(68.dp))
                    }
                    StagedRecapBlock(revealStep >= 2) {
                        stat?.let { AsyncImage(it.track.highResThumbnailUrl ?: it.track.thumbnailUrl, it.track.title, Modifier.fillMaxWidth().aspectRatio(1.15f).clip(RoundedCornerShape(25.dp)), contentScale = ContentScale.Crop) }
                        Spacer(Modifier.height(16.dp))
                    }
                    StagedRecapBlock(revealStep >= 3) {
                        stat?.let {
                        Text(it.track.title, fontFamily = SfProFontFamily, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(it.track.artist, fontFamily = SfProFontFamily, fontSize = 14.sp, color = Color.White.copy(.68f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("A faixa que você mais repetiu.", fontFamily = SfProFontFamily, fontSize = 17.sp, color = Color.White.copy(.68f), modifier = Modifier.padding(top = 10.dp))
                        Spacer(Modifier.height(13.dp)); MetricPill("${it.plays} VEZES")
                        }
                    }
                    StagedRecapBlock(revealStep >= 4) {
                        stat?.let {
                        StoryButton("Ouvir de novo", Icons.Default.PlayArrow) { onPlayTrack(it.track) }
                        } ?: EmptyRecap()
                    }
                }
                3 -> {
                    StagedRecapBlock(revealStep >= 1) {
                        RecapEyebrow("O QUE VOLTOU PRA VOCÊ")
                        Spacer(Modifier.height(14.dp))
                        Text("Em repeat.", fontFamily = SfProFontFamily, fontSize = 43.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF8F7F3))
                        Spacer(Modifier.height(8.dp))
                        Text("Suas cinco faixas mais ouvidas.", fontFamily = SfProFontFamily, fontSize = 16.sp, color = Color.White.copy(.7f))
                        Spacer(Modifier.height(26.dp))
                    }
                    summary.topTracks.take(5).forEachIndexed { i, stat ->
                        StagedRecapBlock(revealStep >= i + 2) { TrackRow(i, stat, onPlayTrack) }
                    }
                    if (summary.topTracks.isEmpty() && revealStep >= 2) EmptyRecap()
                }
                else -> {
                    StagedRecapBlock(revealStep >= 1) {
                        RecapEyebrow("NERF RECAP  ·  ${summary.year}")
                        Spacer(Modifier.height(55.dp))
                        Text("Seu ano.\nSeu som.", fontFamily = SfProFontFamily, fontSize = 43.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(28.dp))
                    }
                    StagedRecapBlock(revealStep >= 2) {
                        Text("Músicas, momentos e tudo que colocou no repeat.", fontFamily = SfProFontFamily, fontSize = 15.sp, color = Color.White.copy(.72f))
                        Spacer(Modifier.height(55.dp))
                    }
                    StagedRecapBlock(revealStep >= 3) {
                    Row(Modifier.fillMaxWidth().height(155.dp).clip(RoundedCornerShape(23.dp)).background(Color.White.copy(.06f)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        summary.topArtists.take(3).forEachIndexed { i, artist ->
                            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(15.dp)).background(listOf(Color(0xFFB83D72), Color(0xFF7652A8), Color(0xFF78B9A1))[i])) {
                                artist.representativeTrack?.let { track -> AsyncImage(track.highResThumbnailUrl ?: track.thumbnailUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .62f) }
                                Column(Modifier.align(Alignment.BottomStart).padding(9.dp)) {
                                    Text(artist.name, fontFamily = SfProFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("TOP ARTISTA", fontFamily = SfProFontFamily, fontSize = 8.sp, letterSpacing = .6.sp, color = Color.White.copy(.78f))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    }
                    StagedRecapBlock(revealStep >= 4) {
                        MetricPill("${formattedRecapDuration(summary.totalMinutes)}  ·  ${formatRecapPlayCount(summary.totalPlays)} REPRODUÇÕES")
                        StoryButton("COMPARTILHAR MINHA HISTÓRIA", Icons.Default.IosShare, onShare)
                    }
                    StagedRecapBlock(revealStep >= 5) {
                        Text("OUVIR A HISTÓRIA DE NOVO", fontFamily = SfProFontFamily, fontSize = 10.sp, letterSpacing = 1.3.sp, color = Color.White.copy(.52f), modifier = Modifier.padding(top = 18.dp))
                    }
                }
            }
        }
        if (page == 1 && revealStep >= 4) {
            val artist = summary.topArtist
            val listenedMinutes = (artist?.listenedMs ?: 0L) / 60_000L
            MetricPill(
                "${formattedRecapDuration(listenedMinutes)}  ·  ${artist?.plays ?: 0} PLAYS",
                Modifier.align(Alignment.BottomStart).padding(start = 28.dp, bottom = 46.dp)
            )
        }
    }
}

@Composable
private fun ColumnScope.StagedRecapBlock(
    visible: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = recapSpringEnter(),
        exit = fadeOut(tween(90))
    ) {
        Column(content = content)
    }
}

@Composable
private fun RecapCoverStory(summary: RecapSummary, revealStep: Int) {
    Box(Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
        AnimatedVisibility(
            visible = revealStep >= 1,
            modifier = Modifier.align(Alignment.TopStart).offset(y = 190.dp),
            enter = recapSpringEnter()
        ) {
            Column {
                RecapEyebrow("SEU ANO EM MÚSICA")
                Spacer(Modifier.height(49.dp))
                Text(
                    when {
                        summary.totalMinutes < 60 -> "${summary.totalMinutes}m"
                        summary.totalMinutes < 1440 -> "${summary.totalMinutes / 60}h ${summary.totalMinutes % 60}m"
                        else -> "${summary.totalMinutes / 1440}d ${(summary.totalMinutes / 60) % 24}h"
                    },
                    fontFamily = SfProFontFamily,
                    fontSize = 68.sp,
                    lineHeight = 76.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8F7F3)
                )
            }
        }
        AnimatedVisibility(
            visible = revealStep >= 2,
            modifier = Modifier.align(Alignment.TopStart).offset(y = 352.dp),
            enter = recapSpringEnter(offsetFraction = 10)
        ) {
            Text("Foi o tempo que você passou\ncom música por perto.", fontFamily = SfProFontFamily, fontSize = 21.sp, lineHeight = 25.sp, color = Color.White.copy(.82f))
        }
        AnimatedVisibility(
            visible = revealStep >= 3,
            modifier = Modifier.align(Alignment.TopStart).offset(y = 454.dp),
            enter = recapSpringEnter(offsetFraction = 8)
        ) {
            Column {
                Text("um ano inteiro…", fontFamily = SfProFontFamily, fontSize = 18.sp, color = Color.White.copy(.32f))
                Spacer(Modifier.height(35.dp))
                Text("…em mais de ${formatRecapPlayCount(summary.totalPlays)} momentos.", fontFamily = SfProFontFamily, fontSize = 17.sp, color = Color.White.copy(.25f))
            }
        }
        AnimatedVisibility(
            visible = revealStep >= 3,
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 108.dp),
            enter = recapSpringEnter(offsetFraction = 6)
        ) {
            MetricPill("${formattedRecapDuration(summary.totalMinutes)}  ·  ${formatRecapPlayCount(summary.totalPlays)} REPRODUÇÕES")
        }
        AnimatedVisibility(
            visible = revealStep >= 3,
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 42.dp),
            enter = fadeIn(spring(dampingRatio = .94f, stiffness = 420f))
        ) {
            Text("DESLIZE PARA CONTINUAR", fontFamily = SfProFontFamily, fontSize = 11.sp, letterSpacing = 1.2.sp, color = Color.White.copy(.48f))
        }
    }
}

private fun recapSpringEnter(offsetFraction: Int = 12) =
    fadeIn(spring(dampingRatio = .9f, stiffness = 420f)) +
        slideInVertically(initialOffsetY = { it / offsetFraction }, animationSpec = spring(dampingRatio = .88f, stiffness = 420f)) +
        scaleIn(initialScale = .97f, animationSpec = spring(dampingRatio = .88f, stiffness = 420f))

@Composable
private fun RecapAmbientBackground(page: Int) {
    val colors = listOf(Color(0xFF58366A), Color(0xFF6B345B), Color(0xFF6B48B5), Color(0xFF226A52), Color(0xFFB64F59), Color(0xFF914570))
    val glow = colors[page.coerceIn(colors.indices)]
    val transition = rememberInfiniteTransition(label = "recapAmbient")
    val drift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8_000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "ambientDrift"
    )
    val counterDrift by transition.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(tween(10_500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "ambientCounterDrift"
    )
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Ink, Color(0xFF0D0D12), Ink)))
        val primaryCenter = Offset(size.width * (.68f + drift * .085f), size.height * (.34f + counterDrift * .035f))
        val primaryRadius = size.width * .76f
        drawCircle(
            brush = Brush.radialGradient(listOf(glow.copy(alpha = .52f), glow.copy(alpha = .2f), Color.Transparent), primaryCenter, primaryRadius),
            radius = primaryRadius,
            center = primaryCenter
        )
        val greenCenter = Offset(size.width * (.82f - drift * .11f), size.height * (.78f + counterDrift * .025f))
        val greenRadius = size.width * .58f
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF1ED760).copy(alpha = .2f), Color(0xFF1ED760).copy(alpha = .07f), Color.Transparent), greenCenter, greenRadius),
            radius = greenRadius,
            center = greenCenter
        )
    }
}

@Composable private fun RecapEyebrow(text: String) {
    Text(text, fontFamily = SfProFontFamily, fontSize = 10.sp, letterSpacing = 1.7.sp, color = Color.White.copy(.58f))
}

private fun formattedRecapDuration(minutes: Long): String = when {
    minutes < 60 -> "${minutes} MIN"
    else -> "${minutes / 60} HORAS"
}

private fun formatRecapPlayCount(plays: Int): String = if (plays >= 1_000) {
    String.format(java.util.Locale("pt", "BR"), "%.1f MIL", plays / 1_000.0).replace(",0 MIL", " MIL")
} else plays.toString()

@Composable private fun MetricPill(text: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(.065f)).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(text, fontFamily = SfProFontFamily, fontSize = 10.sp, letterSpacing = .6.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(.76f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable private fun ArtistRow(index: Int, artist: RecapArtistStats) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${index + 1}".padStart(2, '0'), fontFamily = SfProFontFamily, fontSize = 15.sp, color = Mint, modifier = Modifier.width(42.dp))
        Column(Modifier.weight(1f)) {
            Text(artist.name, fontFamily = SfProFontFamily, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${artist.plays} reproduções", fontFamily = SfProFontFamily, fontSize = 13.sp, color = Color.White.copy(.58f))
        }
    }
}

@Composable private fun TrackRow(index: Int, stat: RecapTrackStats, onPlay: (Track) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onPlay(stat.track) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${index + 1}", fontFamily = SfProFontFamily, fontSize = 17.sp, color = Mint, modifier = Modifier.width(30.dp))
        AsyncImage(stat.track.highResThumbnailUrl ?: stat.track.thumbnailUrl, null, Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(stat.track.title, fontFamily = SfProFontFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stat.track.artist, fontFamily = SfProFontFamily, fontSize = 12.sp, color = Color.White.copy(.68f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${stat.plays} plays", fontFamily = SfProFontFamily, fontSize = 10.sp, color = Color.White.copy(.48f), maxLines = 1)
        }
        Icon(Icons.Default.PlayArrow, null, tint = Color.White.copy(.8f))
    }
}

@Composable private fun EmptyRecap() {
    Text("Continue ouvindo e seu Recap vai ganhar vida com as suas descobertas.", fontFamily = SfProFontFamily, fontSize = 17.sp, color = Color.White.copy(.72f))
}

@Composable private fun StoryButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val interactions = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .98f else 1f, spring(dampingRatio = .88f, stiffness = 420f), label = "recapButtonPress")
    FilledTonalButton(onClick = onClick, interactionSource = interactions, modifier = Modifier.padding(top = 16.dp).height(50.dp).scale(scale), shape = RoundedCornerShape(25.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFF7F6F2), contentColor = Ink)) {
        Icon(icon, null); Spacer(Modifier.width(8.dp)); Text(label, fontFamily = SfProFontFamily, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RecapActionButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) .96f else 1f,
        animationSpec = spring(dampingRatio = .72f, stiffness = 480f),
        label = "recapActionPress"
    )
    val tint = Color.White.copy(alpha = if (enabled) .9f else .42f)
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = .08f),
        border = BorderStroke(.75.dp, Color.White.copy(alpha = .14f)),
        modifier = modifier.height(56.dp).scale(scale)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontFamily = SfProFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = tint, maxLines = 1, softWrap = false)
        }
    }
}
