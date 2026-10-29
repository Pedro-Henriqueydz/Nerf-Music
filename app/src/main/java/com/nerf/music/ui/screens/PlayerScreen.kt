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
import com.nerf.music.ui.components.JamPersonWavesIcon
import com.nerf.music.ui.components.applePressFeedback
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
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.FormatQuote
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.nerf.music.data.model.LyricsData
import com.nerf.music.data.model.SyncedLyricsLine
import com.nerf.music.jam.JamSessionState
import com.nerf.music.player.KaraokeLyricsEngine
import com.nerf.music.player.KaraokeSyncSnapshot
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
import com.nerf.music.ui.components.AnimatedEqualizerBars
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
    onForceSkipPrevious: () -> Unit = onSkipPrevious,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    onToggleDownload: (String) -> Unit,
    downloadProgress: Float? = null,
    onSelectQueueChip: (QueueFilterChip) -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onOpenArtist: (String) -> Unit = {},
    relatedData: TrackRelatedData? = null,
    isLoadingRelated: Boolean = false,
    onLoadRelatedIfNeeded: () -> Unit = {},
    onPlayRelatedTrack: (Track) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    karaokeSyncSnapshot: KaraokeSyncSnapshot = KaraokeSyncSnapshot(),
    isKaraokeVocalIsolationEnabled: Boolean = false,
    karaokeVocalLevel: Float = 0.08f,
    lyricsOffsetMs: Long = 0L,
    showLyricsTranslation: Boolean = false,
    showLyricsPronunciation: Boolean = false,
    hideOriginalLyrics: Boolean = false,
    isTranslatingLyrics: Boolean = false,
    translationTargetLanguage: String = "auto",
    isInstrumentalTrackReady: Boolean = false,
    isUsingRealInstrumental: Boolean = false,
    jamSessionState: JamSessionState = JamSessionState(),
    onToggleKaraokeVocalIsolation: () -> Unit = {},
    onSetKaraokeVocalLevel: (Float) -> Unit = {},
    onAdjustLyricsOffset: (Long) -> Unit = {},
    onResetLyricsOffset: () -> Unit = {},
    onToggleLyricsTranslation: () -> Unit = {},
    onToggleLyricsPronunciation: () -> Unit = {},
    onToggleHideOriginalLyrics: () -> Unit = {},
    onSelectTranslationTargetLanguage: (String) -> Unit = {},
    onOpenJamSheet: () -> Unit = {}
) {
    val currentTrack = state.currentTrack ?: return
    var pendingSwipeTrack by remember { mutableStateOf<Track?>(null) }
    LaunchedEffect(currentTrack.id) {
        if (pendingSwipeTrack?.id == currentTrack.id) {
            pendingSwipeTrack = null
        }
    }
    val track = pendingSwipeTrack ?: currentTrack

    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Pré-carregamento imediato no cache de memória do Coil para as capas próxima e anterior
    LaunchedEffect(state.currentQueueIndex, state.queue) {
        val next = state.queue.getOrNull(state.currentQueueIndex + 1)
        val prev = state.queue.getOrNull(state.currentQueueIndex - 1)
        listOfNotNull(next, prev).forEach { item ->
            val f = java.io.File(context.filesDir, "offline_audio/${item.id}.jpg")
            val art = if (f.exists()) "file://${f.absolutePath}" else (item.highResThumbnailUrl ?: item.thumbnailUrl)
            if (!art.isNullOrBlank()) {
                val req = ImageRequest.Builder(context)
                    .data(art)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                ImageLoader(context).enqueue(req)
            }
        }
    }

    // 1. Extração de Cor Dinâmica da Capa via Palette API
    var targetPaletteColor by remember { mutableStateOf(Color(0xFF381E17)) }
    val localArtwork = remember(track.id) {
        val artFile = java.io.File(context.filesDir, "offline_audio/${track.id}.jpg")
        if (artFile.exists()) "file://${artFile.absolutePath}" else null
    }
    val isDataSaverActive = remember(context) {
        val prefs = context.getSharedPreferences("nerf_user_settings_prefs", Context.MODE_PRIVATE)
        val mode = prefs.getString("key_data_saver_mode", "ALWAYS_OFF") ?: "ALWAYS_OFF"
        val isWifi = com.nerf.music.util.NetworkUtils.isConnectedToWifi(context)
        mode == "ALWAYS_ON" || (mode == "AUTOMATIC" && !isWifi)
    }
    val candidateArtUrl = if (isDataSaverActive) track.thumbnailUrl else (track.highResThumbnailUrl ?: track.thumbnailUrl)
    val artworkUrl = localArtwork ?: candidateArtUrl

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

    LaunchedEffect(showRelatedSheet, track.id) {
        if (showRelatedSheet && (relatedData == null || relatedData.sections.isEmpty())) {
            onLoadRelatedIfNeeded()
        }
    }

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
    var isCanvasFirstFrameRendered by remember(canvasVideoUrl) { mutableStateOf(false) }

    DisposableEffect(canvasVideoUrl) {
        if (!canvasVideoUrl.isNullOrBlank()) {
            val player = ExoPlayer.Builder(context).build().apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ALL
                val mediaItem = MediaItem.fromUri(canvasVideoUrl)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        isCanvasFirstFrameRendered = true
                    }
                })
            }
            canvasPlayer = player
            onDispose {
                isCanvasFirstFrameRendered = false
                player.release()
                canvasPlayer = null
            }
        } else {
            isCanvasFirstFrameRendered = false
            onDispose {
                canvasPlayer?.release()
                canvasPlayer = null
            }
        }
    }

    val hasCanva = !canvasVideoUrl.isNullOrBlank()
    var isCanvaSelected by remember(track.id, hasCanva) {
        mutableStateOf(hasCanva)
    }
    val isCanvasActive = hasCanva && isCanvaSelected
    val canvasAlpha by animateFloatAsState(
        targetValue = if (isCanvasActive && isCanvasFirstFrameRendered) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "canvasAlpha"
    )

    LaunchedEffect(state.isPlaying, isCanvasActive) {
        canvasPlayer?.let { cp ->
            if (state.isPlaying && isCanvasActive) cp.play() else cp.pause()
        }
    }

    val playerDismissOffsetY = remember { Animatable(0f) }
    var dismissDistancePx by remember { mutableFloatStateOf(1f) }
    val dismissProgress = (playerDismissOffsetY.value / dismissDistancePx).coerceIn(0f, 1f)
    val dismissCornerRadius = (dismissProgress * 14f).dp

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = playerDismissOffsetY.value
                shape = RoundedCornerShape(
                    topStart = dismissCornerRadius,
                    topEnd = dismissCornerRadius,
                    bottomStart = dismissCornerRadius,
                    bottomEnd = dismissCornerRadius
                )
                clip = playerDismissOffsetY.value > 2f
            },
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
                // Camada 1: Capa estática de fundo com gradiente escuro sempre presente (evita tela preta ou piscar)
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

                // Camada 2: Vídeo Canvas em looping suave com fade-in progressivo (alpha 0f -> 1f)
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
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = canvasAlpha }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = canvasAlpha }
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
            ) {
                val totalHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                val collapsedPeekHeightDp = 64.dp
                val collapsedPeekPx = with(density) { collapsedPeekHeightDp.toPx() }
                val miniHeaderHeightDp = 64.dp
                val miniHeaderHeightPx = with(density) { miniHeaderHeightDp.toPx() }

                // Distância total exata de descida do Full Player até ancorar no Mini Player
                val calculatedDismissDistancePx = with(density) { (totalHeightPx - 76.dp.toPx()).coerceAtLeast(1f) }
                LaunchedEffect(calculatedDismissDistancePx) {
                    dismissDistancePx = calculatedDismissDistancePx
                }

                // Âncoras verticais (coordenada Y do topo da folha da Fila)
                val anchorCollapsedY = (totalHeightPx - collapsedPeekPx).coerceAtLeast(0f)
                val anchorHalfExpandedY = totalHeightPx * 0.50f
                val anchorExpandedY = miniHeaderHeightPx

                val sheetOffsetY = remember { Animatable(anchorCollapsedY) }

                // Sincroniza âncora quando queueState muda programaticamente
                LaunchedEffect(queueState, anchorCollapsedY, anchorHalfExpandedY, anchorExpandedY) {
                    val targetY = when (queueState) {
                        QueueExpansionState.COLLAPSED -> anchorCollapsedY
                        QueueExpansionState.HALF_EXPANDED -> anchorHalfExpandedY
                        QueueExpansionState.EXPANDED -> anchorExpandedY
                    }
                    sheetOffsetY.animateTo(
                        targetValue = targetY,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = 380f
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

                // Tamanho dinâmico da capa 1:1 estilo YouTube Music:
                // - Com a fila recolhida: base de ~300dp
                // - Ao abrir a fila: encolhe para 135dp
                // - Ao arrastar para baixo para fechar o player: a capa encolhe em direção a 48dp
                val screenWidthDp = with(density) { constraints.maxWidth.toDp() }
                val screenWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val artworkOffsetX = remember { Animatable(0f) }
                val maxArtworkSizeDp = (screenWidthDp - 48.dp).coerceIn(290.dp, 330.dp)
                val minArtworkSizeDp = 135.dp
                val baseArtworkSizeDp = maxArtworkSizeDp - ((maxArtworkSizeDp - minArtworkSizeDp) * halfExpansionFraction)
                val currentArtworkSizeDp = if (dismissProgress > 0f) {
                    baseArtworkSizeDp - ((baseArtworkSizeDp - 46.dp) * dismissProgress)
                } else {
                    baseArtworkSizeDp
                }
                val artworkCornerRadiusDp = (16f - (8f * dismissProgress)).dp
                // Deslocamento horizontal da capa para pousar alinhada à esquerda do miniplayer (padding inicial 12dp)
                val targetArtworkTranslateXPx = with(density) {
                    val fullCenterXPx = constraints.maxWidth / 2f
                    val miniCenterXPx = 12.dp.toPx() + 23.dp.toPx()
                    miniCenterXPx - fullCenterXPx
                }
                val artworkDismissTranslateXPx = targetArtworkTranslateXPx * dismissProgress

                // Controles e botões do player desvanecem suavemente no primeiro terço da descida
                val controlsAlpha = (1f - dismissProgress * 3.0f).coerceIn(0f, 1f)
                val headerAlpha = (1f - dismissProgress * 3.0f).coerceIn(0f, 1f)
                val queueDrawerAlpha = (1f - dismissProgress * 3.5f).coerceIn(0f, 1f)
                val miniPlayerRevealAlpha = ((dismissProgress - 0.40f) / 0.60f).coerceIn(0f, 1f)

                // Função para finalizar o gesto de arraste da folha da Fila e ancorar em um dos 3 estados
                fun snapQueueSheet(velocityY: Float) {
                    val pos = sheetOffsetY.value
                    val nextState = when {
                        // Swipe rápido para cima
                        velocityY < -700f -> {
                            if (queueState == QueueExpansionState.COLLAPSED && pos > anchorHalfExpandedY * 0.75f) {
                                QueueExpansionState.HALF_EXPANDED
                            } else {
                                QueueExpansionState.EXPANDED
                            }
                        }
                        // Swipe rápido para baixo
                        velocityY > 700f -> {
                            if (queueState == QueueExpansionState.EXPANDED && pos < anchorHalfExpandedY * 1.25f) {
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
                                stiffness = 380f
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

                val queueLazyListState = rememberLazyListState()

                // 1. Ao abrir a gaveta da fila, foca e rola imediatamente para a faixa atual em execução
                LaunchedEffect(queueState) {
                    if (queueState != QueueExpansionState.COLLAPSED && state.queue.isNotEmpty()) {
                        val targetIndex = state.currentQueueIndex.coerceIn(0, state.queue.lastIndex)
                        queueLazyListState.scrollToItem(targetIndex)
                    }
                }

                // 2. Ao mudar de faixa com a fila aberta, faz scroll suave sem interromper gestos ativos do usuário
                LaunchedEffect(state.currentQueueIndex) {
                    if (queueState != QueueExpansionState.COLLAPSED && state.queue.isNotEmpty()) {
                        val targetIndex = state.currentQueueIndex.coerceIn(0, state.queue.lastIndex)
                        if (!queueLazyListState.isScrollInProgress) {
                            queueLazyListState.animateScrollToItem(targetIndex)
                        }
                    }
                }

                val queueNestedScrollConnection = remember(anchorExpandedY, anchorHalfExpandedY, anchorCollapsedY) {
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            val deltaY = available.y
                            // Se estiver rolando para baixo e a lista já estiver no topo, a folha da fila desce
                            val isAtTop = queueLazyListState.firstVisibleItemIndex == 0 &&
                                    queueLazyListState.firstVisibleItemScrollOffset == 0
                            if (deltaY > 0f && isAtTop) {
                                val currentY = sheetOffsetY.value
                                if (currentY < anchorCollapsedY) {
                                    val newY = (currentY + deltaY).coerceIn(anchorExpandedY, anchorCollapsedY)
                                    val consumed = newY - currentY
                                    coroutineScope.launch { sheetOffsetY.snapTo(newY) }
                                    return Offset(0f, consumed)
                                }
                            }
                            return Offset.Zero
                        }

                        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                            val deltaY = available.y
                            // Se a lista terminou de rolar e sobrou arrasto para cima ou para baixo
                            if (deltaY != 0f) {
                                val currentY = sheetOffsetY.value
                                val newY = (currentY + deltaY).coerceIn(anchorExpandedY, anchorCollapsedY)
                                val consumedDelta = newY - currentY
                                if (abs(consumedDelta) > 0.5f) {
                                    coroutineScope.launch { sheetOffsetY.snapTo(newY) }
                                    return Offset(0f, consumedDelta)
                                }
                            }
                            return Offset.Zero
                        }

                        override suspend fun onPreFling(available: Velocity): Velocity {
                            val isAtTop = queueLazyListState.firstVisibleItemIndex == 0 &&
                                    queueLazyListState.firstVisibleItemScrollOffset == 0
                            if (available.y > 0f && isAtTop && sheetOffsetY.value < anchorCollapsedY) {
                                snapQueueSheet(available.y)
                                return available
                            }
                            return Velocity.Zero
                        }

                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                            if (available.y != 0f) {
                                snapQueueSheet(available.y)
                                return available
                            }
                            return Velocity.Zero
                        }
                    }
                }

                // =========================================================================
                // MINI-PLAYER HEADER DO TOPO (Aparece no Estado 2 - Fila Totalmente Expandida)
                // =========================================================================
                if (fullExpansionFraction > 0.02f) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(miniHeaderHeightDp)
                            .alpha(fullExpansionFraction)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            queueState = QueueExpansionState.HALF_EXPANDED
                        }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Recolher fila",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(6.dp))
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
                                    modifier = Modifier.size(22.dp)
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
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = track.artist,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.70f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = onOpenJamSheet) {
                            Box(contentAlignment = Alignment.Center) {
                                JamPersonWavesIcon(
                                    tint = if (jamSessionState.isInJam) Color(0xFF1DB954) else Color.White,
                                    size = 23.dp
                                )
                                if (jamSessionState.isInJam) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1DB954))
                                    )
                                }
                            }
                        }

                        IconButton(onClick = onTogglePlay) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pausar" else "Reproduzir",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // =========================================================================
                // CAMADA 1: CONTEÚDO PRINCIPAL DO FULL PLAYER (Alterna entre Player Padrão e Letras)
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
                            karaokeSyncSnapshot = karaokeSyncSnapshot,
                            isKaraokeVocalIsolationEnabled = isKaraokeVocalIsolationEnabled,
                            karaokeVocalLevel = karaokeVocalLevel,
                            lyricsOffsetMs = lyricsOffsetMs,
                            showLyricsTranslation = showLyricsTranslation,
                            showLyricsPronunciation = showLyricsPronunciation,
                            hideOriginalLyrics = hideOriginalLyrics,
                            isTranslatingLyrics = isTranslatingLyrics,
                            translationTargetLanguage = translationTargetLanguage,
                            isInstrumentalTrackReady = isInstrumentalTrackReady,
                            isUsingRealInstrumental = isUsingRealInstrumental,
                            jamSessionState = jamSessionState,
                            onTogglePlay = onTogglePlay,
                            onSkipPrevious = onSkipPrevious,
                            onSkipNext = onSkipNext,
                            onSeekTo = onSeekTo,
                            onToggleFavorite = onToggleFavorite,
                            onCloseLyrics = { isFullscreenLyricsActive = false },
                            onOpenRelated = { showRelatedSheet = true },
                            onOpenQueue = { queueState = QueueExpansionState.HALF_EXPANDED },
                            onToggleKaraokeVocalIsolation = onToggleKaraokeVocalIsolation,
                            onSetKaraokeVocalLevel = onSetKaraokeVocalLevel,
                            onAdjustLyricsOffset = onAdjustLyricsOffset,
                            onResetLyricsOffset = onResetLyricsOffset,
                            onToggleLyricsTranslation = onToggleLyricsTranslation,
                            onToggleLyricsPronunciation = onToggleLyricsPronunciation,
                            onToggleHideOriginalLyrics = onToggleHideOriginalLyrics,
                            onSelectTranslationTargetLanguage = onSelectTranslationTargetLanguage,
                            onOpenJamSheet = onOpenJamSheet
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .then(if (isCanvasActive) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                                .alpha((1f - fullExpansionFraction * 1.5f).coerceIn(0f, 1f))
                                .graphicsLayer {
                                    translationY = -fullExpansionFraction * 80f
                                }
                                .padding(horizontal = 24.dp)
                                .padding(bottom = collapsedPeekHeightDp + 16.dp)
                                .pointerInput(queueState, showRelatedSheet, track.id) {
                                    if (showRelatedSheet) return@pointerInput

                                    var totalDragX = 0f
                                    var totalDragY = 0f
                                    var isHorizontalGesture = false
                                    var isVerticalGesture = false

                                    detectDragGestures(
                                        onDragStart = {
                                            totalDragX = 0f
                                            totalDragY = 0f
                                            isHorizontalGesture = false
                                            isVerticalGesture = false
                                        },
                                        onDragEnd = {
                                            if (isHorizontalGesture) {
                                                val swipeThreshold = 65.dp.toPx()
                                                if (totalDragX < -swipeThreshold) {
                                                    // Arrastou para a esquerda -> Próxima música
                                                    val nextTrack = state.queue.getOrNull(state.currentQueueIndex + 1)
                                                        ?: if (state.queue.size > 1) state.queue.firstOrNull() else null
                                                    if (nextTrack != null) {
                                                        pendingSwipeTrack = nextTrack
                                                    }
                                                    coroutineScope.launch {
                                                        artworkOffsetX.animateTo(-screenWidthPx * 0.75f, tween(140, easing = FastOutLinearInEasing))
                                                        onSkipNext()
                                                        artworkOffsetX.snapTo(screenWidthPx * 0.75f)
                                                        artworkOffsetX.animateTo(0f, spring(Spring.DampingRatioNoBouncy, 380f))
                                                    }
                                                } else if (totalDragX > swipeThreshold) {
                                                    // Arrastou para a direita -> Música anterior direta (sem resetar!)
                                                    val prevTrack = state.queue.getOrNull(state.currentQueueIndex - 1)
                                                        ?: if (state.queue.size > 1) state.queue.lastOrNull() else null
                                                    if (prevTrack != null) {
                                                        pendingSwipeTrack = prevTrack
                                                    }
                                                    coroutineScope.launch {
                                                        artworkOffsetX.animateTo(screenWidthPx * 0.75f, tween(140, easing = FastOutLinearInEasing))
                                                        onForceSkipPrevious()
                                                        artworkOffsetX.snapTo(-screenWidthPx * 0.75f)
                                                        artworkOffsetX.animateTo(0f, spring(Spring.DampingRatioNoBouncy, 380f))
                                                    }
                                                } else {
                                                    coroutineScope.launch {
                                                        artworkOffsetX.animateTo(0f, spring(Spring.DampingRatioNoBouncy, 380f))
                                                    }
                                                }
                                            } else if (isVerticalGesture) {
                                                if (queueState == QueueExpansionState.COLLAPSED) {
                                                    // No estado 0:
                                                    // - Se arrastou para baixo: recolhe para Mini Player
                                                    // - Se arrastou para cima: sobe a fila para HALF_EXPANDED
                                                    if (playerDismissOffsetY.value > dismissDistancePx * 0.35f || totalDragY > 350f) {
                                                        coroutineScope.launch {
                                                            playerDismissOffsetY.animateTo(
                                                                targetValue = dismissDistancePx,
                                                                animationSpec = tween(180, easing = FastOutLinearInEasing)
                                                            )
                                                            onDismiss()
                                                        }
                                                    } else if (playerDismissOffsetY.value > 0f) {
                                                        coroutineScope.launch {
                                                            playerDismissOffsetY.animateTo(0f, spring(Spring.DampingRatioNoBouncy, 400f))
                                                        }
                                                    } else if (totalDragY < -40f || sheetOffsetY.value < anchorCollapsedY - 50f) {
                                                        snapQueueSheet(-1000f)
                                                    } else {
                                                        coroutineScope.launch {
                                                            sheetOffsetY.animateTo(anchorCollapsedY, spring(Spring.DampingRatioNoBouncy, 380f))
                                                        }
                                                    }
                                                } else if (queueState == QueueExpansionState.HALF_EXPANDED) {
                                                    // No estado 1 (metade ocupada pela fila):
                                                    // Toque na área livre superior:
                                                    // - Se arrastar para baixo: recolhe a fila para COLLAPSED
                                                    // - Se arrastar forte para cima: expande a fila para EXPANDED
                                                    snapQueueSheet(if (totalDragY > 40f) 1000f else if (totalDragY < -40f) -1000f else 0f)
                                                }
                                            }
                                            totalDragX = 0f
                                            totalDragY = 0f
                                        },
                                        onDragCancel = {
                                            totalDragX = 0f
                                            totalDragY = 0f
                                            coroutineScope.launch {
                                                artworkOffsetX.animateTo(0f, spring(Spring.DampingRatioNoBouncy, 380f))
                                                if (playerDismissOffsetY.value > 0f) {
                                                    playerDismissOffsetY.animateTo(0f, spring(Spring.DampingRatioNoBouncy, 400f))
                                                }
                                                val targetY = when (queueState) {
                                                    QueueExpansionState.COLLAPSED -> anchorCollapsedY
                                                    QueueExpansionState.HALF_EXPANDED -> anchorHalfExpandedY
                                                    QueueExpansionState.EXPANDED -> anchorExpandedY
                                                }
                                                sheetOffsetY.animateTo(targetY, spring(Spring.DampingRatioNoBouncy, 380f))
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            totalDragX += dragAmount.x
                                            totalDragY += dragAmount.y

                                            if (!isHorizontalGesture && !isVerticalGesture) {
                                                if (abs(totalDragX) > 18f && abs(totalDragX) > abs(totalDragY)) {
                                                    isHorizontalGesture = true
                                                } else if (abs(totalDragY) > 18f && abs(totalDragY) > abs(totalDragX)) {
                                                    isVerticalGesture = true
                                                }
                                            }

                                            if (isHorizontalGesture) {
                                                coroutineScope.launch {
                                                    artworkOffsetX.snapTo(artworkOffsetX.value + dragAmount.x)
                                                }
                                            } else if (isVerticalGesture) {
                                                if (queueState == QueueExpansionState.COLLAPSED) {
                                                    if (dragAmount.y > 0f && sheetOffsetY.value >= anchorCollapsedY) {
                                                        // Puxando para baixo com fila fechada -> Desce o Full Player
                                                        coroutineScope.launch {
                                                            val nextDismissY = (playerDismissOffsetY.value + dragAmount.y).coerceAtLeast(0f)
                                                            playerDismissOffsetY.snapTo(nextDismissY)
                                                        }
                                                    } else if (playerDismissOffsetY.value > 0f) {
                                                        // Se já estiver descendo o Full Player
                                                        coroutineScope.launch {
                                                            val nextDismissY = (playerDismissOffsetY.value + dragAmount.y).coerceAtLeast(0f)
                                                            playerDismissOffsetY.snapTo(nextDismissY)
                                                        }
                                                    } else {
                                                        // Puxando para cima -> Sobe a fila suavemente
                                                        coroutineScope.launch {
                                                            val newY = (sheetOffsetY.value + dragAmount.y).coerceIn(anchorHalfExpandedY, anchorCollapsedY)
                                                            sheetOffsetY.snapTo(newY)
                                                        }
                                                    }
                                                } else if (queueState == QueueExpansionState.HALF_EXPANDED) {
                                                    // Na área superior com fila na metade: arrastar move a fila de volta
                                                    coroutineScope.launch {
                                                        val newY = (sheetOffsetY.value + dragAmount.y).coerceIn(anchorExpandedY, anchorCollapsedY)
                                                        sheetOffsetY.snapTo(newY)
                                                    }
                                                }
                                            }
                                        }
                                    )
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Cabeçalho Superior Glass estilo Apple / Fila de Reprodução perfeitamente alinhado
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .padding(top = 2.dp)
                                    .alpha(headerAlpha)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(0.75.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                        .applePressFeedback(
                                            onClick = {
                                                coroutineScope.launch {
                                                    playerDismissOffsetY.animateTo(
                                                        targetValue = dismissDistancePx,
                                                        animationSpec = tween(180, easing = FastOutLinearInEasing)
                                                    )
                                                    onDismiss()
                                                    playerDismissOffsetY.snapTo(0f)
                                                }
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = stringResource(id = R.string.collapse_player),
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Segmented Pill: [ Música | Canva ] estilo glass da fila
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.14f)),
                                    modifier = Modifier.align(Alignment.Center)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (!isCanvasActive) Color.White.copy(alpha = 0.22f) else Color.Transparent,
                                            modifier = Modifier.applePressFeedback { isCanvaSelected = false }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = if (!isCanvasActive) Color.White else Color.White.copy(alpha = 0.55f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Música",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (!isCanvasActive) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (!isCanvasActive) Color.White else Color.White.copy(alpha = 0.55f)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (isCanvasActive) Color.White.copy(alpha = 0.22f) else Color.Transparent,
                                            modifier = Modifier.applePressFeedback(enabled = hasCanva) {
                                                if (hasCanva) isCanvaSelected = true
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = if (isCanvasActive) Color.White else Color.White.copy(alpha = if (hasCanva) 0.55f else 0.25f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Canva",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isCanvasActive) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isCanvasActive) Color.White else Color.White.copy(alpha = if (hasCanva) 0.55f else 0.25f)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Menu Mais Opções Glass Circle
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(0.75.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                        .applePressFeedback(onClick = { showRelatedSheet = true }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Mais opções",
                                        tint = Color.White.copy(alpha = 0.90f),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height((16f * (1f - halfExpansionFraction * 0.75f)).dp))

                            if (isCanvasActive) {
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .graphicsLayer {
                                                translationX = artworkOffsetX.value + artworkDismissTranslateXPx
                                                rotationZ = if (dismissProgress > 0f) 0f else (artworkOffsetX.value / screenWidthPx) * 7f
                                                alpha = if (dismissProgress > 0f) 1f else (1f - (abs(artworkOffsetX.value) / screenWidthPx) * 0.40f).coerceIn(0.55f, 1f)
                                            }
                                            .size(currentArtworkSizeDp)
                                            .shadow(
                                                elevation = ((18f * (1f - halfExpansionFraction * 0.5f)) * (1f - dismissProgress)).dp,
                                                shape = RoundedCornerShape(artworkCornerRadiusDp),
                                                clip = false
                                            )
                                            .clip(RoundedCornerShape(artworkCornerRadiusDp))
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
                                                modifier = Modifier.size(56.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Linha de Letra Sincronizada com altura fixa para evitar empurrão dos botões
                            val hasInlineLyric = !lyricsLine.isNullOrBlank() && !lyricsLine.equals("null", ignoreCase = true) && halfExpansionFraction < 0.35f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                                    .padding(bottom = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = hasInlineLyric,
                                    enter = fadeIn(animationSpec = tween(300)),
                                    exit = fadeOut(animationSpec = tween(200)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .alpha(controlsAlpha)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { isFullscreenLyricsActive = true }
                                ) {
                                    InlineSyncedLyrics(
                                        lyricText = lyricsLine.orEmpty(),
                                        lyrics = lyrics,
                                        positionMs = state.currentPositionMs,
                                        durationMs = effectiveDurationMs
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(controlsAlpha),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isCanvasActive && halfExpansionFraction < 0.35f) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF262626)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AnimatedContent(
                                            targetState = artworkUrl,
                                            transitionSpec = {
                                                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200), targetAlpha = 0.25f)
                                            },
                                            label = "CanvasMiniCoverTransition"
                                        ) { currentArt ->
                                            if (!currentArt.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = currentArt,
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
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f, fill = false)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { showRelatedSheet = true }
                                                .padding(vertical = 2.dp)
                                        ) {
                                            AnimatedContent(
                                                targetState = track.title,
                                                transitionSpec = {
                                                    (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { it / 6 })
                                                        .togetherWith(fadeOut(animationSpec = tween(160)) + slideOutHorizontally(animationSpec = tween(160)) { -it / 6 })
                                                },
                                                label = "TrackTitleTransition"
                                            ) { currentTitle ->
                                                Text(
                                                    text = currentTitle,
                                                    fontSize = (if (isCanvasActive) 20f else (22f - (4f * halfExpansionFraction))).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    modifier = Modifier.basicMarquee()
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Relacionados",
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    AnimatedContent(
                                        targetState = track.artist,
                                        transitionSpec = {
                                            fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(160))
                                        },
                                        label = "TrackArtistTransition"
                                    ) { currentArtist ->
                                        Text(
                                            text = currentArtist,
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
                                                .padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Ações do Full Player (Favoritar, Salvar, Download) — suavemente desaparece no Estado 1
                            if (halfExpansionFraction < 0.35f) {
                                Spacer(modifier = Modifier.height(12.dp))
                                PlayerActionRow(
                                    isFavorite = state.favoriteTrackIds.contains(track.id),
                                    isSaved = state.savedTrackIds.contains(track.id),
                                    isDownloaded = state.downloadedTrackIds.contains(track.id),
                                    isLyricsActive = isFullscreenLyricsActive,
                                    isJamActive = jamSessionState.isInJam,
                                    jamParticipantCount = jamSessionState.participants.size,
                                    onToggleFavorite = { onToggleFavorite(track.id) },
                                    onToggleSave = { onToggleSave(track.id) },
                                    onToggleDownload = { onToggleDownload(track.id) },
                                    downloadProgress = downloadProgress,
                                    onToggleLyrics = { isFullscreenLyricsActive = !isFullscreenLyricsActive },
                                    onOpenJam = onOpenJamSheet,
                                    onShareTrack = {
                                        coroutineScope.launch {
                                            try {
                                                com.nerf.music.util.SocialShareHelper.openPreview(context, track)
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "Erro ao gerar card: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.alpha(((1f - halfExpansionFraction * 2.8f) * controlsAlpha).coerceIn(0f, 1f))
                                )
                            }

                            Spacer(modifier = Modifier.height((12f * (1f - halfExpansionFraction * 0.5f)).dp))

                            // Barra de Progresso Interativa + Tempos MM:SS (100% visível no Estado 0 e Estado 1)
                            Column(modifier = Modifier.fillMaxWidth().alpha(controlsAlpha)) {
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
                                            val thumbScale by animateFloatAsState(
                                                targetValue = if (isDraggingSlider) 1.55f else 1.0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = 500f
                                                ),
                                                label = "ThumbScale"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .graphicsLayer {
                                                        scaleX = thumbScale
                                                        scaleY = thumbScale
                                                    }
                                                    .shadow(elevation = if (isDraggingSlider) 6.dp else 2.dp, shape = CircleShape, clip = false)
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
                                                    .height(3.dp)
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
                                        color = Color.White.copy(alpha = 0.70f)
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
                                        color = Color.White.copy(alpha = 0.70f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height((16f * (1f - halfExpansionFraction * 0.5f)).dp))

                            // Controles de Mídia Glass Apple (Play/Pause, Skip, Shuffle, Repeat)
                            val mainPlayButtonSize = (68f - (16f * halfExpansionFraction)).dp
                            val sideButtonSize = (48f - (8f * halfExpansionFraction)).dp
                            val modeButtonSize = (40f - (6f * halfExpansionFraction)).dp

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(controlsAlpha),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Aleatório (Shuffle)
                                Box(
                                    modifier = Modifier
                                        .size(modeButtonSize)
                                        .clip(CircleShape)
                                        .background(if (state.shuffleModeEnabled) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                                        .applePressFeedback(onClick = onToggleShuffle),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Aleatório",
                                        tint = if (state.shuffleModeEnabled) {
                                            Color.White
                                        } else {
                                            Color.White.copy(alpha = 0.65f)
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // 2. Anterior (SkipPrevious)
                                Box(
                                    modifier = Modifier
                                        .size(sideButtonSize)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .border(0.75.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                        .applePressFeedback(onClick = onSkipPrevious),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = stringResource(id = R.string.previous_track),
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // 3. Play/Pause Glass Disc com brilho acetinado
                                Box(
                                    modifier = Modifier
                                        .size(mainPlayButtonSize)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.dp, Color.White.copy(alpha = 0.40f), CircleShape)
                                        .applePressFeedback(onClick = onTogglePlay),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.isBuffering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 3.dp,
                                            color = Color.Black
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (state.isPlaying) {
                                                stringResource(id = R.string.pause)
                                            } else {
                                                stringResource(id = R.string.play)
                                            },
                                            tint = Color.Black,
                                            modifier = Modifier.size((34f - 4f * halfExpansionFraction).dp)
                                        )
                                    }
                                }

                                // 4. Próxima (SkipNext)
                                Box(
                                    modifier = Modifier
                                        .size(sideButtonSize)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .border(0.75.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                        .applePressFeedback(onClick = onSkipNext),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = stringResource(id = R.string.next_track),
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // 5. Repetição (Loop/Repeat)
                                val isRepeatActive = state.repeatMode != Player.REPEAT_MODE_OFF
                                val repeatIcon = if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                                    Icons.Default.RepeatOne
                                } else {
                                    Icons.Default.Repeat
                                }
                                Box(
                                    modifier = Modifier
                                        .size(modeButtonSize)
                                        .clip(CircleShape)
                                        .background(if (isRepeatActive) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                                        .applePressFeedback(onClick = onToggleRepeat),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = repeatIcon,
                                        contentDescription = "Repetição",
                                        tint = if (isRepeatActive) {
                                            Color.White
                                        } else {
                                            Color.White.copy(alpha = 0.65f)
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // CAMADA 2: FILA DE REPRODUÇÃO ("A SEGUIR" / UP NEXT) COM 3 ESTADOS POR GESTO
                // =========================================================================
                val isSheetExpandedOrDragging = halfExpansionFraction > 0.02f || queueState != QueueExpansionState.COLLAPSED
                val queueSourceText = state.queueSourceTitle?.takeIf { it.isNotBlank() } ?: track.title

                // Fundo da fila: 100% transparente no estado recolhido (sem caixa, sem borda).
                // Ao puxar, revela gradiente translúcido adaptativo com ambiência suave.
                val queueSheetBackgroundBrush = if (isSheetExpandedOrDragging) {
                    Brush.verticalGradient(
                        0.0f to targetPaletteColor.copy(alpha = 0.50f * halfExpansionFraction + 0.18f * fullExpansionFraction),
                        0.10f to targetPaletteColor.copy(alpha = 0.30f * halfExpansionFraction).compositeOver(Color(0xFF141414).copy(alpha = 0.85f * halfExpansionFraction)),
                        0.26f to Color(0xFF121212).copy(alpha = 0.94f * halfExpansionFraction),
                        1.0f to Color(0xFF090909).copy(alpha = halfExpansionFraction.coerceAtLeast(0.85f))
                    )
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                }

                if (!isFullscreenLyricsActive) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(queueDrawerAlpha)
                            .offset { IntOffset(0, currentSheetY.roundToInt()) },
                        shape = RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp
                        ),
                        color = Color.Transparent,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .background(queueSheetBackgroundBrush)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // 1. DragHandle e cabeçalho ("Tocando de" + botão "+ Salvar")
                                QueueDragHeader(
                                    queueSourceTitle = queueSourceText,
                                    isExpanded = isSheetExpandedOrDragging,
                                    onSaveQueue = { onToggleSave(track.id) },
                                    modifier = Modifier.draggable(
                                        orientation = Orientation.Vertical,
                                        state = queueSheetDraggableState,
                                        onDragStopped = { velocity -> snapQueueSheet(velocity) }
                                    )
                                )

                                if (isSheetExpandedOrDragging) {
                                    // 2. LazyRow com os Chips de filtro
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

                                    // 3. LazyColumn com as músicas da fila (com fade gradiente suave no topo e nested scroll conectado)
                                    QueueTrackList(
                                        queue = state.queue,
                                        currentQueueIndex = state.currentQueueIndex,
                                        isPlaying = state.isPlaying,
                                        listState = queueLazyListState,
                                        onPlayQueueItem = onPlayQueueItem,
                                        onMoveQueueItem = onMoveQueueItem,
                                        onRemoveQueueItem = onRemoveQueueItem,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .nestedScroll(queueNestedScrollConnection)
                                    )
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // DOCKED MINI-PLAYER METADATA (Revela suavemente enquanto a capa pousa na barra)
                    // =========================================================================
                    if (miniPlayerRevealAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .alpha(miniPlayerRevealAlpha)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .padding(start = 70.dp, end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                    Icon(
                                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (state.isPlaying) "Pausar" else "Reproduzir",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(30.dp)
                                    )
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
                        onRetry = onLoadRelatedIfNeeded,
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

/** Displays a long synced lyric as timed, one-line segments without growing the player layout. */
@Composable
private fun InlineSyncedLyrics(
    lyricText: String,
    lyrics: LyricsData?,
    positionMs: Long,
    durationMs: Long
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val maxWidthPx = constraints.maxWidth
        val textMeasurer = rememberTextMeasurer()
        val textStyle = remember {
            TextStyle(
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Start
            )
        }
        val segments = remember(lyricText, maxWidthPx, density, textStyle) {
            splitInlineLyricIntoSegments(lyricText, maxWidthPx, textStyle, textMeasurer)
        }
        val segmentIndex = inlineLyricSegmentIndex(
            segments = segments,
            lyrics = lyrics,
            positionMs = positionMs,
            durationMs = durationMs
        )
        val visibleSegment = segments.getOrNull(segmentIndex).orEmpty()

        AnimatedContent(
            targetState = visibleSegment,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "LyricsChunkTransition"
        ) { chunkText ->
            Text(
                text = chunkText,
                style = textStyle,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
            )
        }
    }
}

private fun splitInlineLyricIntoSegments(
    lyricText: String,
    maxWidthPx: Int,
    textStyle: TextStyle,
    textMeasurer: TextMeasurer
): List<String> {
    val originalText = lyricText.trim().replace(Regex("\\s+"), " ")
    if (originalText.isEmpty() || maxWidthPx <= 0) return listOf(originalText)

    val segments = mutableListOf<String>()
    var remaining = originalText
    while (remaining.isNotEmpty()) {
        val layout = textMeasurer.measure(
            text = AnnotatedString(remaining),
            style = textStyle,
            overflow = TextOverflow.Clip,
            softWrap = true,
            maxLines = 1,
            constraints = Constraints(maxWidth = maxWidthPx)
        )
        if (!layout.hasVisualOverflow) {
            segments += remaining
            break
        }

        val measuredLineEnd = layout.getLineEnd(0).coerceIn(1, remaining.length)
        val wordBoundary = remaining.lastIndexOf(' ', measuredLineEnd - 1)
        val cutIndex = if (wordBoundary > 0) {
            wordBoundary
        } else {
            largestFittingCharacterBoundary(remaining, maxWidthPx, textStyle, textMeasurer)
        }.coerceIn(1, remaining.length)

        val segment = remaining.substring(0, cutIndex).trim()
        if (segment.isNotEmpty()) segments += segment
        remaining = remaining.substring(cutIndex).trimStart()
    }

    return segments.ifEmpty { listOf(originalText) }
}

private fun largestFittingCharacterBoundary(
    text: String,
    maxWidthPx: Int,
    textStyle: TextStyle,
    textMeasurer: TextMeasurer
): Int {
    val boundaries = mutableListOf<Int>()
    val iterator = java.text.BreakIterator.getCharacterInstance(Locale.ROOT)
    iterator.setText(text)
    var boundary = iterator.first()
    while (boundary != java.text.BreakIterator.DONE) {
        boundaries += boundary
        boundary = iterator.next()
    }
    if (boundaries.size <= 1) return text.length

    var low = 1
    var high = boundaries.lastIndex
    var bestBoundary = boundaries[1]
    while (low <= high) {
        val middle = (low + high) / 2
        val candidateBoundary = boundaries[middle]
        val layout = textMeasurer.measure(
            text = AnnotatedString(text.substring(0, candidateBoundary)),
            style = textStyle,
            overflow = TextOverflow.Clip,
            softWrap = false,
            maxLines = 1,
            constraints = Constraints(maxWidth = maxWidthPx)
        )
        if (layout.hasVisualOverflow) {
            high = middle - 1
        } else {
            bestBoundary = candidateBoundary
            low = middle + 1
        }
    }
    return bestBoundary
}

private fun inlineLyricSegmentIndex(
    segments: List<String>,
    lyrics: LyricsData?,
    positionMs: Long,
    durationMs: Long
): Int {
    if (segments.size <= 1 || lyrics?.isSynced != true) return 0

    val lines = lyrics.syncedLines
    val activeIndex = lines.indexOfLast { positionMs >= it.timeMs }
    val activeLine = lines.getOrNull(activeIndex) ?: return 0
    val nextTimestamp = lines.drop(activeIndex + 1)
        .firstOrNull { it.timeMs > activeLine.timeMs }
        ?.timeMs
        ?: durationMs
    val intervalMs = nextTimestamp - activeLine.timeMs
    if (intervalMs <= 0L) return 0

    val elapsedMs = (positionMs - activeLine.timeMs).coerceIn(0L, intervalMs)
    val weights = segments.map { segment ->
        segment.codePointCount(0, segment.length).coerceAtLeast(1).toLong()
    }
    val totalWeight = weights.sum().coerceAtLeast(1L)
    var elapsedWeight = 0L

    for (index in segments.indices) {
        elapsedWeight += weights[index]
        val boundaryMs = intervalMs.toDouble() * elapsedWeight / totalWeight
        if (elapsedMs.toDouble() < boundaryMs) return index
    }
    return segments.lastIndex
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
    downloadProgress: Float? = null,
    isLyricsActive: Boolean = false,
    isJamActive: Boolean = false,
    jamParticipantCount: Int = 0,
    onToggleLyrics: () -> Unit = {},
    onOpenJam: () -> Unit = {},
    onShareTrack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Letras Glass Pill
        RoundedActionPillButton(
            icon = Icons.AutoMirrored.Filled.Chat,
            label = "Letra",
            isActive = isLyricsActive,
            onClick = onToggleLyrics
        )

        // 2. JAM Glass Pill
        RoundedActionPillButton(
            icon = Icons.Default.Group,
            customIconContent = {
                JamPersonWavesIcon(
                    tint = if (isJamActive) Color(0xFF4ADE80) else Color.White,
                    size = 17.dp
                )
            },
            label = if (isJamActive) "JAM (${jamParticipantCount.coerceAtLeast(1)})" else "JAM",
            isActive = isJamActive,
            activeColor = Color(0xFF1DB954),
            onClick = onOpenJam
        )

        // 3. Compartilhar Card Social
        RoundedActionPillButton(
            icon = Icons.Default.Share,
            label = "Compartilhar",
            isActive = false,
            onClick = onShareTrack
        )

        // 4. Favoritar
        RoundedActionPillButton(
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = if (isFavorite) {
                stringResource(id = R.string.action_favorited)
            } else {
                stringResource(id = R.string.action_favorite)
            },
            isActive = isFavorite,
            activeColor = Color(0xFFFF2D55),
            onClick = onToggleFavorite
        )

        // 4. Salvar Playlist
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

        // 5. Download
        RoundedActionPillButton(
            icon = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
            label = when {
                downloadProgress != null -> if (downloadProgress > 0f) "${(downloadProgress * 100).toInt()}%" else "Baixando..."
                isDownloaded -> stringResource(id = R.string.action_downloaded)
                else -> stringResource(id = R.string.action_download)
            },
            isActive = isDownloaded || downloadProgress != null,
            progress = downloadProgress,
            onClick = onToggleDownload
        )
    }
}

@Composable
private fun RoundedActionPillButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    progress: Float? = null,
    activeColor: Color = Color.White,
    customIconContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isActive) {
            activeColor.copy(alpha = 0.22f)
        } else {
            Color.White.copy(alpha = 0.08f)
        },
        border = BorderStroke(
            0.75.dp,
            if (isActive) activeColor.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.14f)
        ),
        modifier = Modifier
            .defaultMinSize(minHeight = 34.dp)
            .applePressFeedback(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 13.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (progress != null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
                    if (progress > 0f) {
                        CircularProgressIndicator(
                            progress = { progress },
                            color = activeColor,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            color = activeColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = activeColor,
                        modifier = Modifier.size(10.dp)
                    )
                }
            } else if (customIconContent != null) {
                customIconContent()
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) activeColor else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontFamily = SfProFontFamily,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) activeColor else Color.White.copy(alpha = 0.90f)
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
    isExpanded: Boolean = false,
    onSaveQueue: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isExpanded) 68.dp else 52.dp)
            .padding(top = 8.dp, bottom = 4.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isExpanded) Arrangement.SpaceBetween else Arrangement.Center
    ) {
        // Centered drag bar
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(3.5.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.38f))
        )

        if (!isExpanded) {
            Spacer(modifier = Modifier.height(5.dp))
            // Fiel ao segundo print do YouTube Music: apenas o nome centralizado, sem prefixo "A seguir:" e sem bordas
            Text(
                text = queueSourceTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Cabeçalho expandido (Estados 1 e 2): "Tocando de" na esquerda, [+ Salvar] na direita
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tocando de",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                    Text(
                        text = queueSourceTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onSaveQueue != null) {
                    Surface(
                        onClick = onSaveQueue,
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Salvar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
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
                    containerColor = Color.White.copy(alpha = 0.08f),
                    labelColor = Color.White,
                    selectedContainerColor = Color.White,
                    selectedLabelColor = Color.Black
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = Color.White.copy(alpha = 0.14f),
                    selectedBorderColor = Color.Transparent,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 0.dp
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
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
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
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.035f to Color.Black,
                        0.975f to Color.Black,
                        1.0f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(
            items = queue,
            key = { index, track -> "${track.id}_$index" }
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
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isBeingDragged -> Color(0xFF333333)
                isCurrent -> Color.White.copy(alpha = 0.10f)
                else -> Color.Transparent
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
                        AnimatedEqualizerBars(
                            isPlaying = isPlaying,
                            barColor = Color.White,
                            barCount = 3,
                            barWidth = 2.5.dp,
                            barSpacing = 2.dp,
                            maxHeight = 16.dp
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
                    color = Color.White,
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
                    tint = Color.White.copy(alpha = 0.40f),
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
    onRetry: () -> Unit = {},
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

        val validSections = remember(relatedData) {
            relatedData?.sections?.map { sec ->
                sec.copy(items = sec.items.filter { it.id.isNotBlank() }.distinctBy { it.id })
            }?.filter { it.items.isNotEmpty() }.orEmpty()
        }
        val hasBio = !relatedData?.aboutArtistDescription.isNullOrBlank()
        val hasContent = validSections.isNotEmpty() || hasBio

        if (isLoading && !hasContent) {
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
        } else if (!hasContent) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = YtmWhite.copy(alpha = 0.35f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Nenhuma recomendação disponível",
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = YtmWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Não encontramos músicas ou artistas relacionados para esta faixa.",
                        fontFamily = YouTubeSansFontFamily,
                        fontSize = 13.sp,
                        color = YtmTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF262626),
                            contentColor = YtmWhite
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Buscar recomendações",
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(validSections) { section ->
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
                                items(section.items, key = { it.id.ifBlank { "${it.hashCode()}" } }) { item ->
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

                if (!relatedData?.aboutArtistDescription.isNullOrBlank()) {
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
                                text = relatedData?.aboutArtistHeader ?: "Sobre o artista",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = YtmWhite
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = relatedData?.aboutArtistDescription.orEmpty(),
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

    Box(
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
    karaokeSyncSnapshot: KaraokeSyncSnapshot = KaraokeSyncSnapshot(),
    isKaraokeVocalIsolationEnabled: Boolean = false,
    karaokeVocalLevel: Float = 0.08f,
    lyricsOffsetMs: Long = 0L,
    showLyricsTranslation: Boolean = false,
    showLyricsPronunciation: Boolean = false,
    hideOriginalLyrics: Boolean = false,
    isTranslatingLyrics: Boolean = false,
    translationTargetLanguage: String = "auto",
    isInstrumentalTrackReady: Boolean = false,
    isUsingRealInstrumental: Boolean = false,
    jamSessionState: JamSessionState = JamSessionState(),
    onTogglePlay: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCloseLyrics: () -> Unit,
    onOpenRelated: () -> Unit,
    onOpenQueue: () -> Unit,
    onToggleKaraokeVocalIsolation: () -> Unit = {},
    onSetKaraokeVocalLevel: (Float) -> Unit = {},
    onAdjustLyricsOffset: (Long) -> Unit = {},
    onResetLyricsOffset: () -> Unit = {},
    onToggleLyricsTranslation: () -> Unit = {},
    onToggleLyricsPronunciation: () -> Unit = {},
    onToggleHideOriginalLyrics: () -> Unit = {},
    onSelectTranslationTargetLanguage: (String) -> Unit = {},
    onOpenJamSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (systemVolume, setSystemVolume) = rememberSystemVolume(context)

    val listState = rememberLazyListState()
    val adjustedPositionMs = currentPositionMs + lyricsOffsetMs
    val activeIndex = remember(lyrics, adjustedPositionMs, karaokeSyncSnapshot.activeLineIndex) {
        if (karaokeSyncSnapshot.activeLineIndex >= 0) {
            karaokeSyncSnapshot.activeLineIndex
        } else {
            lyrics?.syncedLines?.indexOfLast { adjustedPositionMs >= it.timeMs } ?: -1
        }
    }

    var showTranslationMenu by remember { mutableStateOf(false) }
    var showVocalLevelPopup by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedLyricIndices = remember { mutableStateListOf<Int>() }
    val selectionScope = rememberCoroutineScope()
    val lyricCount = if (lyrics?.isSynced == true) lyrics.syncedLines.size else
        lyrics?.let { it.plainLines.ifEmpty { KaraokeLyricsEngine.buildPlainLinesWithPronunciation(it.plainLyrics) }.size } ?: 0
    val selectionGesture = Modifier.pointerInput(track.id, lyrics) {
        var anchor = -1
        fun lineAt(y: Float): Int? = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { y >= it.offset && y < it.offset + it.size }
            ?.key?.toString()?.removePrefix("lyric_")?.toIntOrNull()
        detectDragGesturesAfterLongPress(
            onDragStart = { position ->
                anchor = lineAt(position.y) ?: -1
                if (anchor >= 0) {
                    isSelectionMode = true
                    selectedLyricIndices.clear()
                    selectedLyricIndices.add(anchor)
                }
            },
            onDragEnd = { anchor = -1 },
            onDragCancel = { anchor = -1 },
            onDrag = { change, _ ->
                if (anchor >= 0) {
                    change.consume()
                    lineAt(change.position.y)?.let { end ->
                        selectedLyricIndices.clear()
                        selectedLyricIndices.addAll(com.nerf.music.util.ShareRules.lyricRange(anchor, end, lyricCount))
                    }
                    val height = listState.layoutInfo.viewportEndOffset
                    val amount = when {
                        change.position.y < 64f -> -24f
                        change.position.y > height - 64f -> 24f
                        else -> 0f
                    }
                    if (amount != 0f) selectionScope.launch { listState.scrollBy(amount) }
                }
            }
        )
    }

    // Reset de rolagem imediato quando a faixa muda (garante início limpo)
    LaunchedEffect(track.id, lyrics) {
        selectedLyricIndices.clear()
        isSelectionMode = false
        listState.scrollToItem(0, 0)
    }

    // Auto-scroll inteligente: se a distância for grande (ex: voltando do background), faz snap direto
    LaunchedEffect(activeIndex, isSelectionMode) {
        if (!isSelectionMode && activeIndex >= 0) {
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

        // 2. Área Central de Letras (Scrollable com destaque suave + Karaokê + Tradução + Pronúncia)
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
            val hasSyncedLyrics = lyrics != null && lyrics.isSynced && lyrics.syncedLines.any { it.text.isNotBlank() && !it.text.trim().equals("null", ignoreCase = true) }
            val hasPlainLyrics = lyrics != null && !lyrics.plainLyrics.isNullOrBlank() && !lyrics.plainLyrics.trim().equals("null", ignoreCase = true)

            if (hasSyncedLyrics) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().then(selectionGesture),
                    contentPadding = PaddingValues(top = 36.dp, bottom = 120.dp, start = 24.dp, end = 68.dp)
                ) {
                    // Indicador de contagem regressiva instrumental (• • •) apenas quando o Karaokê está ativo
                    if (isKaraokeVocalIsolationEnabled && karaokeSyncSnapshot.isInstrumentalCountdown && activeIndex == -1) {
                        item(key = "instrumental_intro_dots") {
                            KaraokeCountdownDotsRow(activeDots = karaokeSyncSnapshot.countdownDots)
                        }
                    }

                    itemsIndexed(lyrics!!.syncedLines, key = { index, _ -> "lyric_$index" }) { index, line ->
                        val isCurrent = index == activeIndex
                        val isSelected = selectedLyricIndices.contains(index)
                        val animatedAlpha by animateFloatAsState(
                            targetValue = if (isSelectionMode) {
                                if (isSelected) 1f else 0.40f
                            } else {
                                if (isCurrent) 1f else 0.34f
                            },
                            animationSpec = tween(durationMillis = 320),
                            label = "LyricAlpha_$index"
                        )
                        val animatedScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else if (isCurrent && !isSelectionMode) 1.04f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "LyricScale_$index"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) Color(0xFFFF6600).copy(alpha = 0.18f) else Color.Transparent
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (isSelectionMode) {
                                        val next = com.nerf.music.util.ShareRules.toggleLine(selectedLyricIndices.toList(), index)
                                        selectedLyricIndices.clear()
                                        selectedLyricIndices.addAll(next)
                                    } else {
                                        onSeekTo((line.timeMs - lyricsOffsetMs).coerceAtLeast(0L))
                                    }
                                }
                                .padding(horizontal = if (isSelected) 12.dp else 0.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelectionMode) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFFFF6600) else Color.White.copy(alpha = 0.20f))
                                        .border(1.5.dp, if (isSelected) Color(0xFFFF6600) else Color.White.copy(alpha = 0.50f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                            // 1) Linha Original: destaque palavra-por-palavra SOMENTE quando o microfone (Karaokê) estiver ativo
                            if (!hideOriginalLyrics || (!showLyricsTranslation && !showLyricsPronunciation)) {
                                if (isKaraokeVocalIsolationEnabled && isCurrent && line.words.size > 1) {
                                    val annotatedKaraoke = remember(
                                        line.text,
                                        line.words,
                                        karaokeSyncSnapshot.activeWordIndex,
                                        karaokeSyncSnapshot.wordProgress
                                    ) {
                                        buildAnnotatedString {
                                            line.words.forEachIndexed { wIdx, w ->
                                                val wordAlpha = when {
                                                    wIdx < karaokeSyncSnapshot.activeWordIndex -> 1.0f
                                                    wIdx == karaokeSyncSnapshot.activeWordIndex -> {
                                                        (0.58f + 0.42f * karaokeSyncSnapshot.wordProgress).coerceIn(0.58f, 1.0f)
                                                    }
                                                    else -> 0.42f
                                                }
                                                withStyle(
                                                    SpanStyle(
                                                        color = Color.White.copy(alpha = wordAlpha),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                ) {
                                                    append(w.text)
                                                }
                                                if (wIdx < line.words.lastIndex) {
                                                    append(" ")
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = annotatedKaraoke,
                                        fontFamily = SfProFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        lineHeight = 36.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(
                                        text = line.text,
                                        fontFamily = SfProFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCurrent) 28.sp else 22.sp,
                                        lineHeight = if (isCurrent) 36.sp else 30.sp,
                                        color = Color.White.copy(alpha = animatedAlpha),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // 2) Show Pronunciation (Pronúncia fonética / romanizada estilo Apple Music)
                            AnimatedVisibility(
                                visible = showLyricsPronunciation && !line.pronunciationText.isNullOrBlank(),
                                enter = fadeIn(tween(180)),
                                exit = fadeOut(tween(140))
                            ) {
                                Text(
                                    text = line.pronunciationText.orEmpty(),
                                    fontFamily = SfProFontFamily,
                                    fontWeight = if (hideOriginalLyrics) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = when {
                                        hideOriginalLyrics && isCurrent -> 26.sp
                                        hideOriginalLyrics -> 21.sp
                                        isCurrent -> 17.sp
                                        else -> 15.sp
                                    },
                                    lineHeight = if (hideOriginalLyrics) 33.sp else 23.sp,
                                    color = if (isCurrent) {
                                        Color(0xFFFFD54F).copy(alpha = 0.94f)
                                    } else {
                                        Color(0xFFFFD54F).copy(alpha = 0.36f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = if (hideOriginalLyrics) 0.dp else 4.dp)
                                )
                            }

                            // 3) Lyrics Translation (Tradução linha a linha estilo Apple Music)
                            AnimatedVisibility(
                                visible = showLyricsTranslation && !line.translatedText.isNullOrBlank(),
                                enter = fadeIn(tween(180)),
                                exit = fadeOut(tween(140))
                            ) {
                                Text(
                                    text = line.translatedText.orEmpty(),
                                    fontFamily = SfProFontFamily,
                                    fontWeight = if (hideOriginalLyrics && !showLyricsPronunciation) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = when {
                                        hideOriginalLyrics && !showLyricsPronunciation && isCurrent -> 26.sp
                                        hideOriginalLyrics && !showLyricsPronunciation -> 21.sp
                                        isCurrent -> 17.sp
                                        else -> 15.sp
                                    },
                                    lineHeight = if (hideOriginalLyrics && !showLyricsPronunciation) 33.sp else 23.sp,
                                    color = Color.White.copy(
                                        alpha = if (isCurrent) 0.84f else 0.30f
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = if (hideOriginalLyrics && !showLyricsPronunciation) 0.dp else 4.dp)
                                )
                            }

                            // Contagem regressiva instrumental entre versos longos (• • •) apenas quando Karaokê ativo
                            if (isKaraokeVocalIsolationEnabled && isCurrent && karaokeSyncSnapshot.isInstrumentalCountdown) {
                                Spacer(modifier = Modifier.height(8.dp))
                                KaraokeCountdownDotsRow(activeDots = karaokeSyncSnapshot.countdownDots)
                            }
                        }
                    }
                }
                    item {
                        val langBadge = buildString {
                            append("Fonte: ${lyrics!!.source}")
                            if (!lyrics.detectedLanguage.isNullOrBlank()) {
                                append(" • Idioma: ${lyrics.detectedLanguage.uppercase()}")
                            }
                            if (lyricsOffsetMs != 0L) {
                                val sign = if (lyricsOffsetMs > 0) "+" else ""
                                append(" • Offset: ${sign}${lyricsOffsetMs}ms")
                            }
                        }
                        Text(
                            text = langBadge,
                            fontFamily = SfProFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.40f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 8.dp)
                        )
                    }
                }
            } else if (hasPlainLyrics) {
                val plainStructuredLines = remember(lyrics!!.plainLyrics, lyrics.plainLines) {
                    lyrics.plainLines.ifEmpty {
                        KaraokeLyricsEngine.buildPlainLinesWithPronunciation(lyrics.plainLyrics)
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().then(selectionGesture),
                    contentPadding = PaddingValues(top = 36.dp, bottom = 120.dp, start = 24.dp, end = 68.dp)
                ) {
                    itemsIndexed(plainStructuredLines, key = { index, _ -> "lyric_$index" }) { index, line ->
                        val isSelected = selectedLyricIndices.contains(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) Color(0xFFFF6600).copy(alpha = 0.18f) else Color.Transparent
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (isSelectionMode) {
                                        val next = com.nerf.music.util.ShareRules.toggleLine(selectedLyricIndices.toList(), index)
                                        selectedLyricIndices.clear()
                                        selectedLyricIndices.addAll(next)
                                    }
                                }
                                .padding(horizontal = if (isSelected) 10.dp else 0.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelectionMode) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFFFF6600) else Color.White.copy(alpha = 0.20f))
                                        .border(1.5.dp, if (isSelected) Color(0xFFFF6600) else Color.White.copy(alpha = 0.50f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                            }

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                            if (!hideOriginalLyrics || (!showLyricsTranslation && !showLyricsPronunciation)) {
                                Text(
                                    text = line.text,
                                    fontFamily = SfProFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 21.sp,
                                    lineHeight = 29.sp,
                                    color = Color.White.copy(alpha = if (isSelectionMode && !isSelected) 0.40f else 0.90f),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            AnimatedVisibility(
                                visible = showLyricsPronunciation && !line.pronunciationText.isNullOrBlank(),
                                enter = fadeIn(tween(180)),
                                exit = fadeOut(tween(140))
                            ) {
                                Text(
                                    text = line.pronunciationText.orEmpty(),
                                    fontFamily = SfProFontFamily,
                                    fontWeight = if (hideOriginalLyrics) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = if (hideOriginalLyrics) 20.sp else 15.sp,
                                    lineHeight = if (hideOriginalLyrics) 28.sp else 22.sp,
                                    color = Color(0xFFFFD54F).copy(alpha = 0.88f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = if (hideOriginalLyrics) 0.dp else 3.dp)
                                )
                            }
                            AnimatedVisibility(
                                visible = showLyricsTranslation && !line.translatedText.isNullOrBlank(),
                                enter = fadeIn(tween(180)),
                                exit = fadeOut(tween(140))
                            ) {
                                Text(
                                    text = line.translatedText.orEmpty(),
                                    fontFamily = SfProFontFamily,
                                    fontWeight = if (hideOriginalLyrics && !showLyricsPronunciation) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = if (hideOriginalLyrics && !showLyricsPronunciation) 20.sp else 15.sp,
                                    lineHeight = if (hideOriginalLyrics && !showLyricsPronunciation) 28.sp else 22.sp,
                                    color = Color.White.copy(alpha = 0.76f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = if (hideOriginalLyrics && !showLyricsPronunciation) 0.dp else 3.dp)
                                )
                            }
                        }
                    }
                }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        val langBadge = buildString {
                            append("Fonte: ${lyrics!!.source}")
                            if (!lyrics.detectedLanguage.isNullOrBlank()) {
                                append(" • Idioma: ${lyrics.detectedLanguage.uppercase()}")
                            }
                        }
                        Text(
                            text = langBadge,
                            fontFamily = SfProFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.40f),
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

            // Botão Flutuante de Karaokê / Apple Music Sing (Isolamento Vocal / Troca Instrumental + Slider Vertical) na lateral direita
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(
                    visible = showVocalLevelPopup && isKaraokeVocalIsolationEnabled,
                    enter = fadeIn(tween(180)) + scaleIn(
                        initialScale = 0.92f,
                        transformOrigin = TransformOrigin(0.5f, 1f),
                        animationSpec = tween(180, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f))
                    ),
                    exit = fadeOut(tween(140)) + scaleOut(
                        targetScale = 0.95f,
                        transformOrigin = TransformOrigin(0.5f, 1f),
                        animationSpec = tween(140)
                    )
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xEB1C1C1E),
                        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.16f)),
                        shadowElevation = 14.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 12.dp)
                                .width(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${(karaokeVocalLevel * 100).roundToInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Barra vertical interativa de nível vocal (0% = instrumental puro, 100% = voz original)
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.20f))
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val frac = 1f - (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                                                onSetKaraokeVocalLevel(frac)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val frac = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                                onSetKaraokeVocalLevel(frac)
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(karaokeVocalLevel.coerceIn(0.04f, 1f))
                                        .clip(RoundedCornerShape(50))
                                        .background(OrangePrimary)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isUsingRealInstrumental || isInstrumentalTrackReady) "INST" else "Voz",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isUsingRealInstrumental || isInstrumentalTrackReady) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Pill do Microfone (exclusivo no modo Letras Fullscreen)
                Box(contentAlignment = Alignment.TopEnd) {
                    Surface(
                        onClick = {
                            if (!isKaraokeVocalIsolationEnabled) {
                                onToggleKaraokeVocalIsolation()
                                showVocalLevelPopup = true
                            } else if (!showVocalLevelPopup) {
                                showVocalLevelPopup = true
                            } else {
                                onToggleKaraokeVocalIsolation()
                                showVocalLevelPopup = false
                            }
                        },
                        shape = CircleShape,
                        color = if (isKaraokeVocalIsolationEnabled) OrangePrimary else Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isKaraokeVocalIsolationEnabled) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.22f)
                        ),
                        shadowElevation = if (isKaraokeVocalIsolationEnabled) 8.dp else 0.dp,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isKaraokeVocalIsolationEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Modo Karaokê",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    // Ponto verde sutil indicando que a faixa instrumental oficial já está em buffer pronta para troca instantânea
                    if (isInstrumentalTrackReady) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4ADE80))
                                .border(1.5.dp, Color(0xFF121212), CircleShape)
                        )
                    }
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

        // 6. Barra de Ações Inferior (Fechar Letras + Tradução & Pronúncia Apple Popover + JAM)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão de Letras Ativo (estilo Apple Music pill ativo)
            val closeInteraction = remember { MutableInteractionSource() }
            val isClosePressed by closeInteraction.collectIsPressedAsState()
            val closeScale by animateFloatAsState(
                targetValue = if (isClosePressed) 0.94f else 1f,
                animationSpec = tween(120),
                label = "CloseLyricsPressScale"
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .graphicsLayer {
                        scaleX = closeScale
                        scaleY = closeScale
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .clickable(
                        interactionSource = closeInteraction,
                        indication = null
                    ) { onCloseLyrics() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Fechar Letras",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Botão Apple Music Translation & Pronunciation Glass Popover (emil-design-eng)
            Box {
                val isTransOrPronActive = showLyricsTranslation || showLyricsPronunciation
                val translateInteraction = remember { MutableInteractionSource() }
                val isTranslatePressed by translateInteraction.collectIsPressedAsState()
                val translateScale by animateFloatAsState(
                    targetValue = if (isTranslatePressed) 0.94f else 1f,
                    animationSpec = tween(120),
                    label = "TranslateBtnPressScale"
                )
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .graphicsLayer {
                            scaleX = translateScale
                            scaleY = translateScale
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isTransOrPronActive) Color.White.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            width = 0.75.dp,
                            color = if (isTransOrPronActive) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(
                            interactionSource = translateInteraction,
                            indication = null
                        ) { showTranslationMenu = !showTranslationMenu },
                    contentAlignment = Alignment.Center
                ) {
                    if (isTranslatingLyrics) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Tradução e Pronúncia",
                            tint = if (isTransOrPronActive) Color.White else Color.White.copy(alpha = 0.78f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                if (showTranslationMenu) {
                    Popup(
                        alignment = Alignment.BottomCenter,
                        offset = androidx.compose.ui.unit.IntOffset(0, -140),
                        onDismissRequest = { showTranslationMenu = false },
                        properties = PopupProperties(focusable = true)
                    ) {
                        AppleLyricsOptionsPopover(
                            showLyricsTranslation = showLyricsTranslation,
                            showLyricsPronunciation = showLyricsPronunciation,
                            hideOriginalLyrics = hideOriginalLyrics,
                            isTranslatingLyrics = isTranslatingLyrics,
                            translationTargetLanguage = translationTargetLanguage,
                            detectedSourceLanguage = lyrics?.detectedLanguage,
                            lyricsOffsetMs = lyricsOffsetMs,
                            isSyncedLyrics = lyrics?.isSynced == true,
                            onToggleLyricsTranslation = onToggleLyricsTranslation,
                            onToggleLyricsPronunciation = onToggleLyricsPronunciation,
                            onToggleHideOriginalLyrics = onToggleHideOriginalLyrics,
                            onSelectTranslationTargetLanguage = onSelectTranslationTargetLanguage,
                            onAdjustLyricsOffset = onAdjustLyricsOffset,
                            onResetLyricsOffset = onResetLyricsOffset
                        )
                    }
                }
            }

            // Botão Selecionar / Compartilhar Trecho de Letras (Lyrics Card)
            val shareLyricsInteraction = remember { MutableInteractionSource() }
            val isShareLyricsPressed by shareLyricsInteraction.collectIsPressedAsState()
            val shareLyricsScale by animateFloatAsState(
                targetValue = if (isShareLyricsPressed) 0.94f else 1f,
                animationSpec = tween(120),
                label = "ShareLyricsBtnPressScale"
            )
            Surface(
                onClick = {
                    if (isSelectionMode) {
                        if (selectedLyricIndices.isNotEmpty()) {
                            // Gerar card de trechos
                            val lines = if (lyrics?.isSynced == true) {
                                selectedLyricIndices.sorted().mapNotNull { idx -> lyrics.syncedLines.getOrNull(idx)?.text }
                            } else {
                                val plain = lyrics?.plainLines?.ifEmpty {
                                    KaraokeLyricsEngine.buildPlainLinesWithPronunciation(lyrics.plainLyrics)
                                } ?: emptyList()
                                selectedLyricIndices.sorted().mapNotNull { idx -> plain.getOrNull(idx)?.text }
                            }
                            if (lines.isNotEmpty()) {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    try {
                                        com.nerf.music.util.SocialShareHelper.openPreview(context, track, lines)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Erro ao gerar card: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            isSelectionMode = false
                        }
                    } else {
                        isSelectionMode = true
                        selectedLyricIndices.clear()
                        if (lyricCount > 0) {
                            selectedLyricIndices.add(activeIndex.coerceIn(0, lyricCount - 1))
                        }
                        android.widget.Toast.makeText(context, "Selecione de 1 a 4 linhas. Segure e arraste para selecionar um trecho.", android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                interactionSource = shareLyricsInteraction,
                shape = RoundedCornerShape(12.dp),
                color = if (isSelectionMode) Color(0xFFFF6600).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
                border = BorderStroke(
                    width = 0.75.dp,
                    color = if (isSelectionMode) Color(0xFFFF6600).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .height(42.dp)
                    .graphicsLayer {
                        scaleX = shareLyricsScale
                        scaleY = shareLyricsScale
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isSelectionMode && selectedLyricIndices.isNotEmpty()) Icons.Default.Share else Icons.Default.FormatQuote,
                        contentDescription = "Compartilhar Letra",
                        tint = if (isSelectionMode) Color(0xFFFF6600) else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = if (isSelectionMode) {
                            if (selectedLyricIndices.isNotEmpty()) "Pr\u00e9via (${selectedLyricIndices.size}/4)" else "Cancelar (0/4)"
                        } else {
                            "Card Letra"
                        },
                        fontFamily = SfProFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (isSelectionMode) Color(0xFFFF6600) else Color.White.copy(alpha = 0.90f)
                    )
                }
            }

            // Botão JAM exclusivo na direita (substituindo os antigos botões de Transmissão e Fila)
            val jamInteraction = remember { MutableInteractionSource() }
            val isJamPressed by jamInteraction.collectIsPressedAsState()
            val jamScale by animateFloatAsState(
                targetValue = if (isJamPressed) 0.94f else 1f,
                animationSpec = tween(120),
                label = "JamBtnPressScale"
            )
            Surface(
                onClick = onOpenJamSheet,
                interactionSource = jamInteraction,
                shape = RoundedCornerShape(12.dp),
                color = if (jamSessionState.isInJam) Color(0xFF1DB954).copy(alpha = 0.24f) else Color.White.copy(alpha = 0.10f),
                border = BorderStroke(
                    width = 0.75.dp,
                    color = if (jamSessionState.isInJam) Color(0xFF1DB954).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.14f)
                ),
                modifier = Modifier
                    .height(42.dp)
                    .graphicsLayer {
                        scaleX = jamScale
                        scaleY = jamScale
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    JamPersonWavesIcon(
                        tint = if (jamSessionState.isInJam) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.85f),
                        size = 20.dp
                    )
                    Text(
                        text = if (jamSessionState.isInJam) {
                            "JAM (${jamSessionState.participants.size.coerceAtLeast(1)})"
                        } else {
                            "JAM"
                        },
                        fontFamily = SfProFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (jamSessionState.isInJam) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.90f)
                    )
                }
            }
        }
    }
}

/**
 * Popover translúcido estilo iOS 18 / Apple Music para Tradução, Pronúncia, Seleção de Idioma
 * e Ajuste de Offset LRC, desenhado segundo os princípios `emil-design-eng`:
 * - Origin-aware scale + fade (`TransformOrigin(0.5f, 1.0f)`) com `CubicBezierEasing(0.23f, 1f, 0.32f, 1f)` (<200ms)
 * - Superfície dark glass (`#1C1C1E` @ 94% alpha) com borda especular `0.75.dp` (`White` 14% alpha)
 * - Feedback tátil visual (`scale(0.97f)`) em cada linha interativa e seletor horizontal de idiomas em pílulas.
 */
@Composable
private fun AppleLyricsOptionsPopover(
    showLyricsTranslation: Boolean,
    showLyricsPronunciation: Boolean,
    hideOriginalLyrics: Boolean,
    isTranslatingLyrics: Boolean,
    translationTargetLanguage: String,
    detectedSourceLanguage: String?,
    lyricsOffsetMs: Long,
    isSyncedLyrics: Boolean,
    onToggleLyricsTranslation: () -> Unit,
    onToggleLyricsPronunciation: () -> Unit,
    onToggleHideOriginalLyrics: () -> Unit,
    onSelectTranslationTargetLanguage: (String) -> Unit,
    onAdjustLyricsOffset: (Long) -> Unit,
    onResetLyricsOffset: () -> Unit
) {
    val appleEaseOut = remember { CubicBezierEasing(0.23f, 1f, 0.32f, 1f) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val popScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.92f,
        animationSpec = tween(durationMillis = 180, easing = appleEaseOut),
        label = "ApplePopoverScale"
    )
    val popAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 150, easing = appleEaseOut),
        label = "ApplePopoverAlpha"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xF01C1C1E),
        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.14f)),
        shadowElevation = 24.dp,
        modifier = Modifier
            .width(304.dp)
            .padding(horizontal = 12.dp)
            .graphicsLayer {
                scaleX = popScale
                scaleY = popScale
                alpha = popAlpha
                transformOrigin = TransformOrigin(0.5f, 1.0f)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            // Cabeçalho minimalista estilo Apple Music
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OPÇÕES DE LETRA",
                    fontFamily = SfProFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = Color.White.copy(alpha = 0.48f)
                )
                if (isTranslatingLyrics) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(11.dp),
                            strokeWidth = 1.6.dp,
                            color = OrangePrimary
                        )
                        Text(
                            text = "Traduzindo...",
                            fontFamily = SfProFontFamily,
                            fontSize = 11.sp,
                            color = OrangePrimary
                        )
                    }
                } else if (!detectedSourceLanguage.isNullOrBlank()) {
                    Text(
                        text = "Origem: ${detectedSourceLanguage.uppercase()}",
                        fontFamily = SfProFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.42f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Toggle: Mostrar Tradução
            ApplePopoverOptionRow(
                icon = Icons.Default.Translate,
                iconTint = if (showLyricsTranslation) OrangePrimary else Color.White.copy(alpha = 0.80f),
                title = "Mostrar Tradução",
                subtitle = "Tradução linha a linha em tempo real",
                checked = showLyricsTranslation,
                accentColor = OrangePrimary,
                onClick = onToggleLyricsTranslation
            )

            // Seletor de Idioma da Tradução (Auto, PT, EN, ES, FR, JA, KO, DE, IT)
            val langScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 6.dp)
            ) {
                Text(
                    text = "Idioma da tradução",
                    fontFamily = SfProFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.50f),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(langScrollState)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KaraokeLyricsEngine.SUPPORTED_TRANSLATION_LANGUAGES.forEach { option ->
                        val isSelected = translationTargetLanguage.equals(option.code, ignoreCase = true)
                        Surface(
                            onClick = { onSelectTranslationTargetLanguage(option.code) },
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) OrangePrimary else Color.White.copy(alpha = 0.09f),
                            border = BorderStroke(
                                width = 0.75.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = option.shortBadge,
                                    fontFamily = SfProFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = option.label,
                                    fontFamily = SfProFontFamily,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.58f)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // 2. Toggle: Mostrar Pronúncia
            ApplePopoverOptionRow(
                icon = Icons.Default.RecordVoiceOver,
                iconTint = if (showLyricsPronunciation) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.80f),
                title = "Mostrar Pronúncia",
                subtitle = "Guia fonético e romanizado para cantar",
                checked = showLyricsPronunciation,
                accentColor = Color(0xFFFFD54F),
                onClick = onToggleLyricsPronunciation
            )

            // 3. Toggle condicional: Ocultar Letra Original
            AnimatedVisibility(
                visible = showLyricsTranslation || showLyricsPronunciation,
                enter = fadeIn(tween(150)) + expandVertically(tween(160)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(140))
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    )
                    ApplePopoverOptionRow(
                        icon = Icons.Default.Subtitles,
                        iconTint = if (hideOriginalLyrics) Color.White else Color.White.copy(alpha = 0.72f),
                        title = "Ocultar Verso Original",
                        subtitle = "Destacar apenas tradução ou pronúncia",
                        checked = hideOriginalLyrics,
                        accentColor = OrangePrimary,
                        onClick = onToggleHideOriginalLyrics
                    )
                }
            }

            // 4. Ajuste Fino de Sincronia LRC (apenas quando a letra é sincronizada)
            if (isSyncedLyrics) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                    color = Color.White.copy(alpha = 0.08f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sincronia LRC",
                        fontFamily = SfProFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { onAdjustLyricsOffset(-150L) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = "-150ms",
                                fontFamily = SfProFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            onClick = onResetLyricsOffset,
                            shape = RoundedCornerShape(8.dp),
                            color = if (lyricsOffsetMs != 0L) OrangePrimary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f)
                        ) {
                            val sign = if (lyricsOffsetMs > 0) "+" else ""
                            Text(
                                text = "${sign}${lyricsOffsetMs}ms",
                                fontFamily = SfProFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (lyricsOffsetMs != 0L) OrangePrimary else Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            onClick = { onAdjustLyricsOffset(150L) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = "+150ms",
                                fontFamily = SfProFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplePopoverOptionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "PopoverRowScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (checked) accentColor.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = SfProFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontFamily = SfProFontFamily,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.54f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Switch compacto estilo iOS
        val thumbOffsetDp by animateDpAsState(
            targetValue = if (checked) 18.dp else 2.dp,
            animationSpec = tween(durationMillis = 160, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)),
            label = "IosSwitchThumb"
        )
        val trackColor by animateColorAsState(
            targetValue = if (checked) accentColor else Color.White.copy(alpha = 0.18f),
            animationSpec = tween(durationMillis = 160),
            label = "IosSwitchTrack"
        )
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(50))
                .background(trackColor),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(start = thumbOffsetDp)
                    .size(22.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun KaraokeCountdownDotsRow(activeDots: Int) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (dotIdx in 1..3) {
            val isLit = dotIdx <= activeDots
            val dotScale by animateFloatAsState(
                targetValue = if (isLit) 1.25f else 0.85f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "CountdownDotScale_$dotIdx"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer {
                        scaleX = dotScale
                        scaleY = dotScale
                    }
                    .clip(CircleShape)
                    .background(
                        if (isLit) Color.White else Color.White.copy(alpha = 0.28f)
                    )
            )
        }
    }
}
