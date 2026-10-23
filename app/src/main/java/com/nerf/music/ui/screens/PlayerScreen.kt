package com.nerf.music.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import com.nerf.music.ui.theme.OrangePrimary
import com.nerf.music.ui.theme.YouTubeSansFontFamily
import kotlin.math.roundToInt
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import com.nerf.music.ui.theme.SfProFontFamily
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.nerf.music.R
import android.content.Context
import android.view.LayoutInflater
import androidx.compose.ui.draw.scale
import android.media.AudioManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Chat
import com.nerf.music.data.model.LyricsData
import com.nerf.music.data.model.PlaybackState
import com.nerf.music.data.model.QueueFilterChip
import com.nerf.music.data.model.Track
import com.nerf.music.data.model.TrackRelatedData
import com.nerf.music.data.model.RelatedSection
import com.nerf.music.data.model.HomeSectionStyle
import com.nerf.music.ui.theme.YtmBlack
import com.nerf.music.ui.theme.YtmGrey11
import com.nerf.music.ui.theme.YtmGrey10
import com.nerf.music.ui.theme.YtmWhite
import com.nerf.music.ui.theme.YtmTextSecondary
import com.nerf.music.ui.theme.YtmIconInactive
import com.nerf.music.ui.theme.OrangeSecondary
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Estados de expansão da Fila de Reprodução ("Up Next" / "A seguir"):
 * - [COLLAPSED]: Estado 0 (Oculto) — Apenas o Full Player visível com a alça da gaveta no rodapé.
 * - [HALF_EXPANDED]: Estado 1 (Meio Expandido) — Tela dividida ao meio; capa reduzida no topo e fila na metade inferior.
 * - [EXPANDED]: Estado 2 (Totalmente Expandido) — A fila ocupa a tela inteira.
 */
enum class QueueExpansionState {
    COLLAPSED,
    HALF_EXPANDED,
    EXPANDED
}

/**
 * Formata milissegundos no padrão MM:SS (ex: 01:20, 03:45).
 */
fun formatDurationMs(durationMs: Long): String {
    if (durationMs <= 0L) return "00:00"
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

/**
 * Ajusta a cor extraída da capa via Palette para garantir contraste suficiente com textos brancos.
 */
fun normalizeDynamicPaletteColor(rawRgb: Int): Color {
    val blended = ColorUtils.blendARGB(rawRgb, android.graphics.Color.BLACK, 0.32f)
    val composeColor = Color(blended)
    return if (composeColor.luminance() > 0.35f) {
        Color(ColorUtils.blendARGB(blended, android.graphics.Color.BLACK, 0.45f))
    } else {
        composeColor
    }
}

/**
 * Mini Player Persistente (Bottom Bar) no estilo YouTube Music / Material 3.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayerBar(
    state: PlaybackState,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onExpandPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack ?: return

    val targetProgress = if (state.durationMs > 0L) {
        (state.currentPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        label = "MiniPlayerProgress"
    )

    Surface(
        onClick = onExpandPlayer,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        color = Color(0xFF212121),
        tonalElevation = 10.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF333333))
                ) {
                    if (!track.thumbnailUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onTogglePlay) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) {
                                stringResource(id = R.string.pause)
                            } else {
                                stringResource(id = R.string.play)
                            },
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                IconButton(onClick = onSkipNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = stringResource(id = R.string.next_track),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFF383838)
            )
        }
    }
}

/**
 * Full Player Screen (Tela Completa de Reprodução) estilo YouTube Music / Material 3.
 *
 * Funcionalidades:
 * - Fundo dinâmico extraído da capa da música via Palette API (`darkMutedSwatch` / `dominantSwatch`)
 *   animado suavemente com `animateColorAsState`.
 * - Título com Chevron (`>`) à direita e Nome do Artista clicável com feedback via Toast.
 * - Fileira de botões arredondados de ação: Favoritar, Salvar e Download.
 * - Barra de progresso (Slider) fina e elegante no estilo Material 3 / YouTube Music.
 * - Controles de mídia na ordem: Aleatório (Shuffle), Anterior, Play/Pause (maior), Próxima, Repetição (Repeat).
 * - Gaveta da Fila ("Up Next") acionada por gestos (swipe up/down) com 3 estados,
 *   cabeçalho minimalista (`DragHandle` + `"Tocando de: ..."`) e Chips de filtro dinâmicos.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FullPlayerSheet(
    state: PlaybackState,
    canvasVideoUrl: String? = null,
    lyricsLine: String? = null,
    lyrics: LyricsData? = null,
    onDismiss: () -> Unit,
    onTogglePlay: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    onToggleDownload: (String) -> Unit,
    onSelectQueueChip: (QueueFilterChip) -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onOpenArtist: (String) -> Unit = {},
    relatedData: TrackRelatedData? = null,
    isLoadingRelated: Boolean = false,
    onPlayRelatedTrack: (Track) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {}
) {
    val track = state.currentTrack ?: return
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // 1. Extração de Cor Dinâmica da Capa via Palette API
    var targetPaletteColor by remember { mutableStateOf(Color(0xFF381E17)) }
    val artworkUrl = track.highResThumbnailUrl ?: track.thumbnailUrl

    LaunchedEffect(track.id, artworkUrl) {
        if (artworkUrl.isNullOrBlank()) {
            targetPaletteColor = Color(0xFF2C1914)
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .allowHardware(false) // Obrigatório para leitura de pixels pelo Palette
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        val palette = Palette.from(bitmap).generate()
                        val extractedRgb = palette.darkMutedSwatch?.rgb
                            ?: palette.dominantSwatch?.rgb
                            ?: palette.darkVibrantSwatch?.rgb
                            ?: palette.mutedSwatch?.rgb
                            ?: Color(0xFF2C1914).toArgb()
                        targetPaletteColor = normalizeDynamicPaletteColor(extractedRgb)
                    }
                }
            } catch (_: Exception) {
                targetPaletteColor = Color(0xFF2C1914)
            }
        }
    }

    val animatedTopColor by animateColorAsState(
        targetValue = targetPaletteColor,
        animationSpec = tween(durationMillis = 750),
        label = "DynamicCoverGradientColor"
    )

    // 2. Estado de Expansão da Fila (0 = Oculto, 1 = Meio Expandido, 2 = Totalmente Expandido)
    var queueState by remember { mutableStateOf(QueueExpansionState.COLLAPSED) }
    var showRelatedSheet by remember { mutableStateOf(false) }
    var isFullscreenLyricsActive by remember { mutableStateOf(false) }

    // Tratamento do botão Voltar do Android: se letras em tela cheia estiverem ativas, fecha letras; se a fila estiver aberta, recolhe a fila; senão, fecha o Player
    BackHandler {
        if (showRelatedSheet) {
            showRelatedSheet = false
        } else if (isFullscreenLyricsActive) {
            isFullscreenLyricsActive = false
        } else when (queueState) {
            QueueExpansionState.EXPANDED -> queueState = QueueExpansionState.HALF_EXPANDED
            QueueExpansionState.HALF_EXPANDED -> queueState = QueueExpansionState.COLLAPSED
            QueueExpansionState.COLLAPSED -> onDismiss()
        }
    }

    // 3. Estado do Slider de Progresso
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionFraction by remember { mutableFloatStateOf(0f) }
    val sliderInteractionSource = remember { MutableInteractionSource() }

    val effectiveDurationMs = state.durationMs.coerceAtLeast(0L)
    val currentFraction = if (effectiveDurationMs > 0L) {
        (state.currentPositionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val displayedFraction = if (isDraggingSlider) dragPositionFraction else currentFraction
    val displayedPositionMs = if (isDraggingSlider && effectiveDurationMs > 0L) {
        (dragPositionFraction * effectiveDurationMs).toLong()
    } else {
        state.currentPositionMs
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            animatedTopColor,
            ColorUtils.blendARGB(animatedTopColor.toArgb(), 0xFF0F0F0F.toInt(), 0.65f).let { Color(it) },
            Color(0xFF0F0F0F)
        )
    )

    var canvasPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(canvasVideoUrl) {
        if (!canvasVideoUrl.isNullOrBlank()) {
            val player = ExoPlayer.Builder(context).build().apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ALL
                val mediaItem = MediaItem.fromUri(canvasVideoUrl)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
            canvasPlayer = player
            onDispose {
                player.release()
                canvasPlayer = null
            }
        } else {
            onDispose {
                canvasPlayer?.release()
                canvasPlayer = null
            }
        }
    }

    LaunchedEffect(state.isPlaying) {
        canvasPlayer?.let { cp ->
            if (state.isPlaying) cp.play() else cp.pause()
        }
    }

    val isCanvasActive = !canvasVideoUrl.isNullOrBlank()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F0F0F)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // =========================================================================
            // FUNDO: Canvas ou Capa Ampliada com Desfoque Profundo (Efeito Aurora Apple Music)
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (isFullscreenLyricsActive) 55.dp else 0.dp)
            ) {
                if (isCanvasActive && canvasPlayer != null) {
                    key(canvasVideoUrl) {
                        AndroidView(
                            factory = { ctx ->
                                val view = LayoutInflater.from(ctx).inflate(R.layout.canvas_player_view, null) as PlayerView
                                view.player = canvasPlayer
                                view
                            },
                            update = { playerView ->
                                if (playerView.player != canvasPlayer) {
                                    playerView.player = canvasPlayer
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.60f),
                                        Color.Black.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!artworkUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = artworkUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(1.25f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            animatedTopColor.copy(alpha = 0.70f),
                                            Color(0xFF141414).copy(alpha = 0.85f),
                                            Color(0xFF0F0F0F)
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            // Overlay escuro suave que deixa as cores fluírem com destaque para o texto das letras
            if (isFullscreenLyricsActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.50f),
                                    Color.Black.copy(alpha = 0.80f)
                                )
                            )
                        )
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .pointerInput(queueState, showRelatedSheet) {
                        if (queueState == QueueExpansionState.COLLAPSED && !showRelatedSheet) {
                            var totalDragY = 0f
                            detectVerticalDragGestures(
                                onDragStart = { totalDragY = 0f },
                                onDragEnd = {
                                    if (totalDragY > 100f) {
                                        onDismiss()
                                    } else if (totalDragY < -100f) {
                                        queueState = QueueExpansionState.HALF_EXPANDED
                                    }
                                    totalDragY = 0f
                                },
                                onDragCancel = { totalDragY = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragY += dragAmount
                                }
                            )
                        }
                    }
            ) {
            val totalHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            val collapsedPeekHeightDp = 64.dp
            val collapsedPeekPx = with(density) { collapsedPeekHeightDp.toPx() }

            // Âncoras verticais (coordenada Y do topo da folha da Fila)
            val anchorCollapsedY = (totalHeightPx - collapsedPeekPx).coerceAtLeast(0f)
            val anchorHalfExpandedY = totalHeightPx * 0.44f
            val anchorExpandedY = 0f

            val sheetOffsetY = remember { Animatable(anchorCollapsedY) }

            // Sincroniza âncora quando queueState muda programaticamente
            LaunchedEffect(queueState, anchorCollapsedY, anchorHalfExpandedY) {
                val targetY = when (queueState) {
                    QueueExpansionState.COLLAPSED -> anchorCollapsedY
                    QueueExpansionState.HALF_EXPANDED -> anchorHalfExpandedY
                    QueueExpansionState.EXPANDED -> anchorExpandedY
                }
                sheetOffsetY.animateTo(
                    targetValue = targetY,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }

            val currentSheetY = sheetOffsetY.value.coerceIn(anchorExpandedY, anchorCollapsedY)

            // Fração de transição entre Estado 0 (Oculto) -> Estado 1 (Meio Expandido)
            val halfExpansionFraction = if (anchorCollapsedY > anchorHalfExpandedY) {
                ((anchorCollapsedY - currentSheetY) / (anchorCollapsedY - anchorHalfExpandedY)).coerceIn(0f, 1f)
            } else {
                0f
            }

            // Fração de transição entre Estado 1 (Meio Expandido) -> Estado 2 (Totalmente Expandido)
            val fullExpansionFraction = if (anchorHalfExpandedY > anchorExpandedY) {
                ((anchorHalfExpandedY - currentSheetY) / (anchorHalfExpandedY - anchorExpandedY)).coerceIn(0f, 1f)
            } else {
                0f
            }

            // Tamanho dinâmico da capa (estilo Spotify): largura total com margens de 24dp
            val screenWidthDp = with(density) { constraints.maxWidth.toDp() }
            val maxArtworkSizeDp = (screenWidthDp - 48.dp).coerceIn(310.dp, 335.dp)
            val targetArtworkSizeDp = maxArtworkSizeDp - (165.dp * halfExpansionFraction)
            val animatedArtworkSize by animateDpAsState(
                targetValue = targetArtworkSizeDp,
                label = "ArtworkDynamicSize"
            )

            // Função para finalizar o gesto de arraste da folha da Fila e ancorar em um dos 3 estados
            fun snapQueueSheet(velocityY: Float) {
                val pos = sheetOffsetY.value
                val nextState = when {
                    // Swipe rápido para cima
                    velocityY < -900f -> {
                        if (queueState == QueueExpansionState.COLLAPSED && pos > anchorHalfExpandedY * 0.7f) {
                            QueueExpansionState.HALF_EXPANDED
                        } else {
                            QueueExpansionState.EXPANDED
                        }
                    }
                    // Swipe rápido para baixo
                    velocityY > 900f -> {
                        if (queueState == QueueExpansionState.EXPANDED && pos < anchorHalfExpandedY * 1.3f) {
                            QueueExpansionState.HALF_EXPANDED
                        } else {
                            QueueExpansionState.COLLAPSED
                        }
                    }
                    // Escolha pela âncora mais próxima da posição solta
                    else -> {
                        val distCollapsed = abs(pos - anchorCollapsedY)
                        val distHalf = abs(pos - anchorHalfExpandedY)
                        val distExpanded = abs(pos - anchorExpandedY)
                        when (minOf(distCollapsed, distHalf, distExpanded)) {
                            distExpanded -> QueueExpansionState.EXPANDED
                            distHalf -> QueueExpansionState.HALF_EXPANDED
                            else -> QueueExpansionState.COLLAPSED
                        }
                    }
                }
                queueState = nextState
                val targetY = when (nextState) {
                    QueueExpansionState.COLLAPSED -> anchorCollapsedY
                    QueueExpansionState.HALF_EXPANDED -> anchorHalfExpandedY
                    QueueExpansionState.EXPANDED -> anchorExpandedY
                }
                coroutineScope.launch {
                    sheetOffsetY.animateTo(
                        targetValue = targetY,
                        initialVelocity = velocityY,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }

            val queueSheetDraggableState = rememberDraggableState { deltaY ->
                coroutineScope.launch {
                    val newY = (sheetOffsetY.value + deltaY).coerceIn(anchorExpandedY, anchorCollapsedY)
                    sheetOffsetY.snapTo(newY)
                }
            }

            // =========================================================================
            // CAMADA 1: CONTEÚDO PRINCIPAL DO FULL PLAYER (Alterna entre Player Padrão e Letras Apple Music)
            // =========================================================================
            AnimatedContent(
                targetState = isFullscreenLyricsActive,
                transitionSpec = {
                    if (targetState) {
                        (fadeIn(animationSpec = tween(320)) + slideInVertically(animationSpec = tween(320)) { it / 8 })
                            .togetherWith(fadeOut(animationSpec = tween(220)) + scaleOut(targetScale = 0.92f, animationSpec = tween(220)))
                    } else {
                        (fadeIn(animationSpec = tween(320)) + scaleIn(initialScale = 0.92f, animationSpec = tween(320)))
                            .togetherWith(fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(220)) { it / 8 })
                    }
                },
                label = "PlayerLyricsModeTransition"
            ) { inLyricsMode ->
                if (inLyricsMode) {
                    AppleMusicLyricsView(
                        track = track,
                        artworkUrl = artworkUrl,
                        lyrics = lyrics,
                        currentPositionMs = state.currentPositionMs,
                        durationMs = effectiveDurationMs,
                        isPlaying = state.isPlaying,
                        isBuffering = state.isBuffering,
                        favoriteTrackIds = state.favoriteTrackIds,
                        onTogglePlay = onTogglePlay,
                        onSkipPrevious = onSkipPrevious,
                        onSkipNext = onSkipNext,
                        onSeekTo = onSeekTo,
                        onToggleFavorite = onToggleFavorite,
                        onCloseLyrics = { isFullscreenLyricsActive = false },
                        onOpenRelated = { showRelatedSheet = true },
                        onOpenQueue = { queueState = QueueExpansionState.HALF_EXPANDED }
                    )
                } else {
                    Column(
                modifier = Modifier
                    .then(if (isCanvasActive) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                    .alpha((1f - fullExpansionFraction * 1.25f).coerceIn(0f, 1f))
                    .padding(horizontal = 24.dp)
                    .padding(bottom = collapsedPeekHeightDp + 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabeçalho Superior
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(id = R.string.collapse_player),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.now_playing).uppercase(Locale.getDefault()),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!track.album.isNullOrBlank()) {
                            Text(
                                text = track.album,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height((16f * (1f - halfExpansionFraction * 0.75f)).dp))

                if (isCanvasActive) {
                    // MODO CANVAS (SPOTIFY): Vídeo 9:16 preenche todo o fundo, área central flexível
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    // MODO CAPA PADRÃO: Imagem de capa grande dinâmica com Palette
                    Box(
                        modifier = Modifier
                            .size(animatedArtworkSize)
                            .shadow(
                                elevation = (22f * (1f - halfExpansionFraction * 0.5f)).dp,
                                shape = RoundedCornerShape(18.dp),
                                clip = false
                            )
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF262626)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!artworkUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = artworkUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height((20f * (1f - halfExpansionFraction * 0.65f)).dp))
                }

                    AnimatedVisibility(
                        visible = !lyricsLine.isNullOrBlank() && halfExpansionFraction < 0.45f,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isFullscreenLyricsActive = true }
                            .padding(bottom = 8.dp)
                    ) {
                        AnimatedContent(
                            targetState = lyricsLine.orEmpty(),
                            transitionSpec = {
                                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(180))
                            },
                            label = "LyricsChunkTransition"
                        ) { chunkText ->
                            Text(
                                text = chunkText,
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                                color = Color.White,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { isFullscreenLyricsActive = true }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCanvasActive && halfExpansionFraction < 0.45f) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF262626)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!artworkUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = artworkUrl,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = if (halfExpansionFraction > 0.5f) Alignment.CenterHorizontally else Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = if (halfExpansionFraction > 0.5f) Arrangement.Center else Arrangement.Start
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { showRelatedSheet = true }
                                        .padding(vertical = 2.dp, horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = track.title,
                                        fontSize = (if (isCanvasActive) 20f else (22f - (4f * halfExpansionFraction))).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Relacionados",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = track.artist,
                                fontSize = (15f - (2f * halfExpansionFraction)).sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val bId = track.artistBrowseId
                                        if (!bId.isNullOrBlank()) {
                                            onOpenArtist(bId)
                                        } else {
                                            onOpenArtist(track.artist)
                                        }
                                    }
                                    .padding(vertical = 2.dp, horizontal = 2.dp)
                            )
                        }
                    }

                // Ações do Full Player (Favoritar, Salvar, Download) e Barra de Progresso estilo Spotify — visíveis no Estado 0
                if (halfExpansionFraction < 0.45f) {
                    Spacer(modifier = Modifier.height(14.dp))
                    PlayerActionRow(
                        isFavorite = state.favoriteTrackIds.contains(track.id),
                        isSaved = state.savedTrackIds.contains(track.id),
                        isDownloaded = state.downloadedTrackIds.contains(track.id),
                        isLyricsActive = isFullscreenLyricsActive,
                        onToggleFavorite = { onToggleFavorite(track.id) },
                        onToggleSave = { onToggleSave(track.id) },
                        onToggleDownload = { onToggleDownload(track.id) },
                        onToggleLyrics = { isFullscreenLyricsActive = !isFullscreenLyricsActive },
                        modifier = Modifier.alpha((1f - halfExpansionFraction * 2.2f).coerceIn(0f, 1f))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Barra de Progresso Interativa estilo Spotify (trilha branca + thumb circular branco) + Tempos MM:SS
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha((1f - halfExpansionFraction * 2.2f).coerceIn(0f, 1f))
                    ) {
                        Slider(
                            value = displayedFraction,
                            onValueChange = { newFraction ->
                                isDraggingSlider = true
                                dragPositionFraction = newFraction.coerceIn(0f, 1f)
                            },
                            onValueChangeFinished = {
                                if (effectiveDurationMs > 0L) {
                                    val targetMs = (dragPositionFraction * effectiveDurationMs).toLong()
                                    onSeekTo(targetMs)
                                }
                                isDraggingSlider = false
                            },
                            enabled = effectiveDurationMs > 0L,
                            interactionSource = sliderInteractionSource,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                            ),
                            thumb = {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isDraggingSlider) 13.dp else 9.dp)
                                            .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            },
                            track = { sliderState ->
                                val fraction = sliderState.value.coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.5.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.White.copy(alpha = 0.28f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(50))
                                                .background(Color.White)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDurationMs(displayedPositionMs),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                            val totalTimeText = if (effectiveDurationMs > 0L) {
                                formatDurationMs(effectiveDurationMs)
                            } else if (track.durationText.isNotBlank() && track.durationText != "--:--") {
                                track.durationText
                            } else {
                                "00:00"
                            }
                            Text(
                                text = totalTimeText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.72f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height((18f * (1f - halfExpansionFraction * 0.5f)).dp))

                // Controles de Mídia: Aleatório, Anterior, Play/Pause (maior), Próxima, Repetição
                val mainPlayButtonSize = (70f - (16f * halfExpansionFraction)).dp
                val sideButtonSize = (50f - (10f * halfExpansionFraction)).dp
                val modeButtonSize = (42f - (6f * halfExpansionFraction)).dp

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Aleatório (Shuffle) — extrema esquerda
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.size(modeButtonSize)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Aleatório",
                            tint = if (state.shuffleModeEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 2. Anterior (SkipPrevious)
                    FilledTonalIconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier.size(sideButtonSize),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFF262626),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = stringResource(id = R.string.previous_track),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // 3. Play/Pause (maior)
                    Surface(
                        onClick = onTogglePlay,
                        modifier = Modifier.size(mainPlayButtonSize),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 10.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (state.isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (state.isPlaying) {
                                        stringResource(id = R.string.pause)
                                    } else {
                                        stringResource(id = R.string.play)
                                    },
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }

                    // 4. Próxima (SkipNext)
                    FilledTonalIconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.size(sideButtonSize),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFF262626),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = stringResource(id = R.string.next_track),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // 5. Repetição (Loop/Repeat) — extrema direita
                    val isRepeatActive = state.repeatMode != Player.REPEAT_MODE_OFF
                    val repeatIcon = if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Default.RepeatOne
                    } else {
                        Icons.Default.Repeat
                    }
                    IconButton(
                        onClick = onToggleRepeat,
                        modifier = Modifier.size(modeButtonSize)
                    ) {
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = "Repetição",
                            tint = if (isRepeatActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
                }
            }

            // =========================================================================
            // CAMADA 2: FILA DE REPRODUÇÃO ("A SEGUIR" / UP NEXT) COM 3 ESTADOS POR GESTO
            // =========================================================================
            val isSheetExpandedOrDragging = halfExpansionFraction > 0.03f || queueState != QueueExpansionState.COLLAPSED
            val queueSourceText = state.queueSourceTitle?.takeIf { it.isNotBlank() } ?: track.title

            if (!isFullscreenLyricsActive) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(0, currentSheetY.roundToInt()) },
                shape = RoundedCornerShape(
                    topStart = (22f * (1f - fullExpansionFraction)).dp,
                    topEnd = (22f * (1f - fullExpansionFraction)).dp
                ),
                color = if (isSheetExpandedOrDragging) {
                    Color(0xFF181818)
                } else {
                    Color.Transparent
                },
                tonalElevation = if (isSheetExpandedOrDragging) 12.dp else 0.dp,
                shadowElevation = if (isSheetExpandedOrDragging) 16.dp else 0.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. DragHandle e texto "Tocando de: [Nome da Música Inicial ou Mix]"
                    QueueDragHeader(
                        queueSourceTitle = queueSourceText,
                        modifier = Modifier.draggable(
                            orientation = Orientation.Vertical,
                            state = queueSheetDraggableState,
                            onDragStopped = { velocity -> snapQueueSheet(velocity) }
                        )
                    )

                    if (isSheetExpandedOrDragging) {
                        HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 1.dp)

                        // 2. LazyRow com os Chips arredondados (Familiar, Descobrir, Popular, Em alta, Tudo)
                        if (state.queueFilterChips.isNotEmpty()) {
                            QueueFilterChipsRow(
                                chips = state.queueFilterChips,
                                selectedChipParams = state.selectedQueueChipParams,
                                onSelectChip = onSelectQueueChip
                            )
                        }

                        if (state.isLoadingQueueRadio) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.Transparent
                            )
                        }

                        // 3. LazyColumn com as músicas da fila (suporta Drag & Drop e Swipe to Dismiss)
                        QueueTrackList(
                            queue = state.queue,
                            currentQueueIndex = state.currentQueueIndex,
                            isPlaying = state.isPlaying,
                            onPlayQueueItem = onPlayQueueItem,
                            onMoveQueueItem = onMoveQueueItem,
                            onRemoveQueueItem = onRemoveQueueItem,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
            }

            if (showRelatedSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showRelatedSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = YtmBlack,
                    contentColor = YtmWhite,
                    dragHandle = {
                        BottomSheetDefaults.DragHandle(
                            color = YtmWhite.copy(alpha = 0.35f)
                        )
                    }
                ) {
                    PlayerRelatedSheetContent(
                        relatedData = relatedData,
                        isLoading = isLoadingRelated,
                        currentPlayingId = track.id,
                        onPlayTrack = { relTrack ->
                            onPlayRelatedTrack(relTrack)
                            showRelatedSheet = false
                        },
                        onOpenArtist = { artistId ->
                            onOpenArtist(artistId)
                            showRelatedSheet = false
                        },
                        onOpenAlbum = { albumId ->
                            onOpenAlbum(albumId)
                            showRelatedSheet = false
                        }
                    )
                }
            }
        }
    }
}
}

/**
 * Fileira de botões arredondados abaixo do título/artista (estilo YouTube Music):
 * Favoritar, Salvar e Download.
 */
@Composable
fun PlayerActionRow(
    isFavorite: Boolean,
    isSaved: Boolean,
    isDownloaded: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleDownload: () -> Unit,
    isLyricsActive: Boolean = false,
    onToggleLyrics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundedActionPillButton(
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = if (isFavorite) {
                stringResource(id = R.string.action_favorited)
            } else {
                stringResource(id = R.string.action_favorite)
            },
            isActive = isFavorite,
            onClick = onToggleFavorite
        )

        RoundedActionPillButton(
            icon = if (isSaved) Icons.AutoMirrored.Filled.PlaylistAddCheck else Icons.AutoMirrored.Filled.PlaylistAdd,
            label = if (isSaved) {
                stringResource(id = R.string.action_saved)
            } else {
                stringResource(id = R.string.action_save)
            },
            isActive = isSaved,
            onClick = onToggleSave
        )

        RoundedActionPillButton(
            icon = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
            label = if (isDownloaded) {
                stringResource(id = R.string.action_downloaded)
            } else {
                stringResource(id = R.string.action_download)
            },
            isActive = isDownloaded,
            onClick = onToggleDownload
        )

        RoundedActionPillButton(
            icon = Icons.Default.Subtitles,
            label = "Letra",
            isActive = isLyricsActive,
            onClick = onToggleLyrics
        )
    }
}

@Composable
private fun RoundedActionPillButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (isActive) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
        } else {
            Color(0xFF282828)
        },
        border = if (isActive) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 36.dp)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.White
            )
        }
    }
}

/**
 * Cabeçalho minimalista da Gaveta da Fila (Queue Drawer):
 * Exibe o `DragHandle` (traço horizontal no topo) e, logo abaixo,
 * um texto pequeno e discreto indicando a origem da fila: "Tocando de: [Nome da Música Inicial ou Mix]".
 */
@Composable
private fun QueueDragHeader(
    queueSourceTitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(top = 10.dp, bottom = 6.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(4.5.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF8A8A8A))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tocando de: $queueSourceTitle",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Fileira horizontal de Chips arredondados para filtrar a Rádio Dinâmica na Gaveta da Fila
 * (ex: "Tudo", "Familiar", "Descobrir", "Popular", "Em alta").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueFilterChipsRow(
    chips: List<QueueFilterChip>,
    selectedChipParams: String?,
    onSelectChip: (QueueFilterChip) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = chips,
            key = { chip -> "${chip.title}_${chip.params}" }
        ) { chip ->
            val isSelected = if (selectedChipParams != null) {
                chip.params == selectedChipParams
            } else {
                chip.isSelected
            }

            FilterChip(
                selected = isSelected,
                onClick = { onSelectChip(chip) },
                label = {
                    Text(
                        text = chip.title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF252525),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = Color(0xFF383838),
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                )
            )
        }
    }
}

/**
 * Lista interativa da Fila de Reprodução sincronizada com `MediaController.currentTimeline`:
 * - Músicas anteriores (Histórico) exibidas com opacidade reduzida
 * - Música atual em destaque
 * - Próximas faixas
 * - Reordenação via Drag-and-Drop isolada da BottomSheet
 * - Remoção via Swipe-to-Dismiss
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueTrackList(
    queue: List<Track>,
    currentQueueIndex: Int,
    isPlaying: Boolean,
    onPlayQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 68.dp.toPx() }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val currentQueueState by rememberUpdatedState(queue)
    val currentMoveCallback by rememberUpdatedState(onMoveQueueItem)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(
            items = queue,
            key = { _, track -> track.id }
        ) { index, track ->
            val currentIndex by rememberUpdatedState(index)
            val isHistory = index < currentQueueIndex
            val isCurrent = index == currentQueueIndex
            val isBeingDragged = draggedIndex == index

            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (
                        (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) &&
                        currentQueueState.size > 1 &&
                        draggedIndex == null
                    ) {
                        onRemoveQueueItem(currentIndex)
                        true
                    } else {
                        false
                    }
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = queue.size > 1 && draggedIndex == null,
                enableDismissFromEndToStart = queue.size > 1 && draggedIndex == null,
                modifier = Modifier
                    .zIndex(if (isBeingDragged) 10f else 0f)
                    .graphicsLayer {
                        translationY = if (isBeingDragged) dragOffsetY else 0f
                        scaleX = if (isBeingDragged) 1.03f else 1f
                        scaleY = if (isBeingDragged) 1.03f else 1f
                    },
                backgroundContent = {
                    val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSwiping) Color(0xFFB00020) else Color.Transparent)
                            .padding(horizontal = 20.dp),
                        contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                            Alignment.CenterStart
                        } else {
                            Alignment.CenterEnd
                        }
                    ) {
                        if (isSwiping) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remover da fila",
                                tint = Color.White
                            )
                        }
                    }
                }
            ) {
                QueueTrackRow(
                    track = track,
                    isHistory = isHistory,
                    isCurrent = isCurrent,
                    isPlaying = isPlaying && isCurrent,
                    isBeingDragged = isBeingDragged,
                    onClick = { onPlayQueueItem(currentIndex) },
                    onStartDrag = {
                        draggedIndex = currentIndex
                        dragOffsetY = 0f
                    },
                    onDragDelta = { deltaY ->
                        val activeIdx = draggedIndex ?: return@QueueTrackRow
                        dragOffsetY += deltaY
                        val steps = (dragOffsetY / itemHeightPx).toInt()
                        if (steps != 0) {
                            val targetIdx = (activeIdx + steps).coerceIn(0, currentQueueState.lastIndex)
                            if (targetIdx != activeIdx) {
                                currentMoveCallback(activeIdx, targetIdx)
                                draggedIndex = targetIdx
                                dragOffsetY -= steps * itemHeightPx
                            }
                        }
                    },
                    onEndDrag = {
                        draggedIndex = null
                        dragOffsetY = 0f
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
private fun QueueTrackRow(
    track: Track,
    isHistory: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isBeingDragged: Boolean,
    onClick: () -> Unit,
    onStartDrag: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onEndDrag: () -> Unit
) {
    val currentOnStartDrag by rememberUpdatedState(onStartDrag)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnEndDrag by rememberUpdatedState(onEndDrag)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isHistory && !isBeingDragged) 0.48f else 1f)
            .clickable(onClick = onClick)
            // Suporte a clique longo no item para iniciar Drag-and-Drop isolado da BottomSheet
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { currentOnStartDrag() },
                    onDragEnd = { currentOnEndDrag() },
                    onDragCancel = { currentOnEndDrag() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnDragDelta(dragAmount.y)
                    }
                )
            }
            .then(
                if (isCurrent || isBeingDragged) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isBeingDragged -> Color(0xFF303030)
                isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
                else -> Color(0xFF222222)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Capa da Faixa na Fila
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF333333)),
                contentAlignment = Alignment.Center
            ) {
                if (!track.thumbnailUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Equalizer else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Título, Artista e Status (Histórico / Tocando agora / Duração)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val statusPrefix = when {
                    isCurrent -> "${stringResource(id = R.string.now_playing)} • "
                    isHistory -> "${stringResource(id = R.string.queue_history_badge)} • "
                    else -> ""
                }
                Text(
                    text = "$statusPrefix${track.artist} • ${track.durationText}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Alça de Reordenação (Drag Handle) isolada para arraste imediato sem conflito com a BottomSheet
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { currentOnStartDrag() },
                            onDragEnd = { currentOnEndDrag() },
                            onDragCancel = { currentOnEndDrag() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentOnDragDelta(dragAmount.y)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Segure e arraste para reordenar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Conteúdo do BottomSheet de "Relacionados" (Related) idêntico ao YouTube Music:
 * - "Talvez você também goste de" (carrossel em grade com músicas parecidas).
 * - "Playlists recomendadas" (carrossel de cards).
 * - "Artistas parecidos" (cards de artistas).
 * - "Sobre o artista" (biografia).
 */
@Composable
fun PlayerRelatedSheetContent(
    relatedData: TrackRelatedData?,
    isLoading: Boolean,
    currentPlayingId: String,
    onPlayTrack: (Track) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Relacionados",
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = YtmWhite,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        HorizontalDivider(color = Color(0xFF262626), thickness = 1.dp)

        if (isLoading && relatedData == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = YtmWhite,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (relatedData == null || relatedData.sections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma recomendação disponível no momento.",
                    fontFamily = YouTubeSansFontFamily,
                    fontSize = 14.sp,
                    color = YtmTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(relatedData.sections) { section ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = section.title,
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = YtmWhite,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )

                        if (section.style == HomeSectionStyle.QUICK_PICKS_GRID) {
                            YtmQuickPicksGridCarousel(
                                tracks = section.items,
                                currentPlayingId = currentPlayingId,
                                onTrackClick = onPlayTrack
                            )
                        } else {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(section.items, key = { it.id }) { item ->
                                    HomeSongCard(
                                        track = item,
                                        isPlaying = item.id == currentPlayingId,
                                        onClick = {
                                            val bId = item.browseId ?: item.id
                                            if (bId.startsWith("UC")) {
                                                onOpenArtist(bId)
                                            } else if (item.isAlbumOrPlaylist || bId.startsWith("MPREb_") || bId.startsWith("VLPL")) {
                                                onOpenAlbum(bId)
                                            } else {
                                                onPlayTrack(item)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (!relatedData.aboutArtistDescription.isNullOrBlank()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E1E))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = relatedData.aboutArtistHeader ?: "Sobre o artista",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = YtmWhite
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = relatedData.aboutArtistDescription,
                                fontFamily = YouTubeSansFontFamily,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = YtmTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun rememberSystemVolume(context: Context): Pair<Float, (Float) -> Unit> {
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var currentVolumeFraction by remember {
        mutableFloatStateOf((audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume).coerceIn(0f, 1f))
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    currentVolumeFraction = (vol.toFloat() / maxVolume).coerceIn(0f, 1f)
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    val setVolume: (Float) -> Unit = { fraction ->
        val target = (fraction * maxVolume).toInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        currentVolumeFraction = fraction.coerceIn(0f, 1f)
    }

    return Pair(currentVolumeFraction, setVolume)
}

@Composable
private fun AppleMusicVolumeBar(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isInteracting by remember { mutableStateOf(false) }
    val animatedHeight by animateDpAsState(
        targetValue = if (isInteracting) 10.dp else 4.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "VolumeBarThickness"
    )

    BoxWithConstraints(
        modifier = modifier
            .height(30.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isInteracting = true
                        val newVol = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onVolumeChange(newVol)
                        tryAwaitRelease()
                        isInteracting = false
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isInteracting = true
                        val newVol = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onVolumeChange(newVol)
                    },
                    onDragEnd = { isInteracting = false },
                    onDragCancel = { isInteracting = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val newVol = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onVolumeChange(newVol)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.24f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = volume.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppleMusicLyricsView(
    track: Track,
    artworkUrl: String?,
    lyrics: LyricsData?,
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    favoriteTrackIds: Set<String>,
    onTogglePlay: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCloseLyrics: () -> Unit,
    onOpenRelated: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (systemVolume, setSystemVolume) = rememberSystemVolume(context)

    val listState = rememberLazyListState()
    val activeIndex = remember(lyrics, currentPositionMs) {
        lyrics?.syncedLines?.indexOfLast { currentPositionMs >= it.timeMs } ?: -1
    }

    // Reset de rolagem imediato quando a faixa muda (garante início limpo)
    LaunchedEffect(track.id) {
        listState.scrollToItem(0, 0)
    }

    // Auto-scroll inteligente: se a distância for grande (ex: voltando do background), faz snap direto
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            val currentIndex = listState.firstVisibleItemIndex
            if (kotlin.math.abs(activeIndex - currentIndex) > 3) {
                listState.scrollToItem(
                    index = activeIndex,
                    scrollOffset = -220
                )
            } else {
                listState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = -220
                )
            }
        }
    }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionFraction by remember { mutableFloatStateOf(0f) }
    val effectiveDurationMs = durationMs.coerceAtLeast(0L)
    val currentFraction = if (effectiveDurationMs > 0L) {
        (currentPositionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val displayedFraction = if (isDraggingSlider) dragPositionFraction else currentFraction
    val displayedPositionMs = if (isDraggingSlider && effectiveDurationMs > 0L) {
        (dragPositionFraction * effectiveDurationMs).toLong()
    } else {
        currentPositionMs
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 34.dp)
    ) {
        // 1. Barra Superior Compacta (Mini Capa + Título + Artista + Estrela + Opções)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF262626)),
                contentAlignment = Alignment.Center
            ) {
                if (!artworkUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    fontFamily = SfProFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    fontFamily = SfProFontFamily,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val isFavorite = favoriteTrackIds.contains(track.id)
            IconButton(onClick = { onToggleFavorite(track.id) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favoritar",
                    tint = if (isFavorite) Color.White else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onOpenRelated) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opções",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 2. Área Central de Letras (Scrollable com destaque suave)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.07f to Color.Black,
                            0.90f to Color.Black,
                            1.0f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            val hasSyncedLyrics = lyrics != null && lyrics.isSynced && lyrics.syncedLines.isNotEmpty()
            val hasPlainLyrics = lyrics != null && !lyrics.plainLyrics.isNullOrBlank()

            if (hasSyncedLyrics) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 40.dp, bottom = 120.dp, start = 24.dp, end = 24.dp)
                ) {
                    itemsIndexed(lyrics!!.syncedLines) { index, line ->
                        val isCurrent = index == activeIndex
                        val animatedAlpha by animateFloatAsState(
                            targetValue = if (isCurrent) 1f else 0.35f,
                            animationSpec = tween(durationMillis = 350),
                            label = "LyricAlpha_$index"
                        )
                        val animatedScale by animateFloatAsState(
                            targetValue = if (isCurrent) 1.04f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "LyricScale_$index"
                        )

                        Text(
                            text = line.text,
                            fontFamily = SfProFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCurrent) 28.sp else 22.sp,
                            lineHeight = if (isCurrent) 38.sp else 32.sp,
                            color = Color.White.copy(alpha = animatedAlpha),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onSeekTo(line.timeMs)
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            } else if (hasPlainLyrics) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 40.dp, bottom = 120.dp, start = 24.dp, end = 24.dp)
                ) {
                    item {
                        Text(
                            text = lyrics!!.plainLyrics.orEmpty(),
                            fontFamily = SfProFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            lineHeight = 32.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Letra não disponível para esta música",
                        fontFamily = SfProFontFamily,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // 3. Barra de Progresso Estilo Apple Music (com tempo decorrido e restante -M:SS)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Slider(
                value = displayedFraction,
                onValueChange = { newFraction ->
                    isDraggingSlider = true
                    dragPositionFraction = newFraction.coerceIn(0f, 1f)
                },
                onValueChangeFinished = {
                    if (effectiveDurationMs > 0L) {
                        val targetMs = (dragPositionFraction * effectiveDurationMs).toLong()
                        onSeekTo(targetMs)
                    }
                    isDraggingSlider = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                ),
                thumb = {
                    Box(
                        modifier = Modifier.size(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isDraggingSlider) 12.dp else 8.dp)
                                .shadow(2.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                },
                track = { sliderState ->
                    val fraction = sliderState.value.coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.25f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDurationMs(displayedPositionMs),
                    fontSize = 11.sp,
                    fontFamily = SfProFontFamily,
                    color = Color.White.copy(alpha = 0.65f)
                )
                val remainingMs = (effectiveDurationMs - displayedPositionMs).coerceAtLeast(0L)
                Text(
                    text = "-${formatDurationMs(remainingMs)}",
                    fontSize = 11.sp,
                    fontFamily = SfProFontFamily,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Controles de Reprodução Grandes (Anterior, Play/Pause, Próxima)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Anterior",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Surface(
                onClick = onTogglePlay,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = Color.Black
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onSkipNext,
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Próxima",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Slider de Volume do Sistema
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VolumeDown,
                contentDescription = "Volume Baixo",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            AppleMusicVolumeBar(
                volume = systemVolume,
                onVolumeChange = setSystemVolume,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Volume Alto",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 6. Barra de Ações Inferior (Botão Letras Ativo + Cast + Fila)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão de Letras Ativo (estilo Apple Music pill ativo)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .clickable { onCloseLyrics() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Fechar Letras",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Cast,
                    contentDescription = "Transmitir",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onOpenQueue) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Fila",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
