package com.nerf.music.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nerf.music.data.model.Artist
import com.nerf.music.ui.components.applePressFeedback
import com.nerf.music.ui.theme.SfProFontFamily
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Paleta de materiais e luz inspirada no Apple Music Dark Canvas
private val AppleObsidian = Color(0xFF070709)
private val AppleCrimson = Color(0xFFFF2D55)
private val AppleFlame = Color(0xFFFF5E3A)
private val AppleAmber = Color(0xFFFF9500)
private val AppleViolet = Color(0xFF5E5CE6)

private val AppleGlassBorder = Color(0x2EFFFFFF)
private val AppleGlassSurface = Color(0x14FFFFFF)
private val AppleGlassSurfaceHighlight = Color(0x24FFFFFF)

private val AppleTextPrimary = Color(0xFFFFFFFF)
private val AppleTextSecondary = Color(0xB3FFFFFF) // 70% white
private val AppleTextTertiary = Color(0x66FFFFFF)  // 40% white

/**
 * Tela de Onboarding de Primeiro Acesso com estética e materiais inspirados no Apple Music.
 */
@Composable
fun OnboardingScreen(
    onComplete: (selectedArtists: List<Artist>) -> Unit,
    onSearchArtists: suspend (query: String) -> List<Artist>,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val selectedArtists = remember { mutableStateListOf<Artist>() }

    // Iluminação ambiente viva em camadas com difusão contínua
    val infiniteTransition = rememberInfiniteTransition(label = "ambientAtmosphere")
    val auraDrift by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraDrift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleObsidian)
            .drawBehind {
                // Glow carmesim/laranja superior
                val topAura = Brush.radialGradient(
                    colors = listOf(
                        AppleCrimson.copy(alpha = 0.16f),
                        AppleFlame.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.4f + auraDrift, size.height * 0.12f),
                    radius = size.width * 0.9f
                )
                drawRect(brush = topAura)

                // Glow violeta sutil inferior
                val bottomAura = Brush.radialGradient(
                    colors = listOf(
                        AppleViolet.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.8f - auraDrift, size.height * 0.85f),
                    radius = size.width * 0.75f
                )
                drawRect(brush = bottomAura)
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(
                        initialOffsetX = { it / 3 },
                        animationSpec = tween(340, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f))
                    ) + fadeIn(
                        initialAlpha = 0.3f,
                        animationSpec = tween(300)
                    ) + scaleIn(
                        initialScale = 0.96f,
                        animationSpec = tween(340)
                    )).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { -it / 4 },
                            animationSpec = tween(260)
                        ) + fadeOut(
                            animationSpec = tween(220)
                        ) + scaleOut(
                            targetScale = 0.96f,
                            animationSpec = tween(260)
                        )
                    )
                } else {
                    (slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(340, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f))
                    ) + fadeIn(
                        initialAlpha = 0.3f,
                        animationSpec = tween(300)
                    ) + scaleIn(
                        initialScale = 0.96f,
                        animationSpec = tween(340)
                    )).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { it / 4 },
                            animationSpec = tween(260)
                        ) + fadeOut(
                            animationSpec = tween(220)
                        ) + scaleOut(
                            targetScale = 0.96f,
                            animationSpec = tween(260)
                        )
                    )
                }
            },
            label = "StepTransition",
            modifier = Modifier.fillMaxSize()
        ) { step ->
            when (step) {
                0 -> AppleWelcomeStep(
                    onProceed = { currentStep = 1 }
                )
                1 -> AppleArtistSelectionStep(
                    selectedArtists = selectedArtists,
                    onToggleArtist = { artist ->
                        val existing = selectedArtists.find { it.name.equals(artist.name, ignoreCase = true) }
                        if (existing != null) {
                            selectedArtists.remove(existing)
                        } else {
                            selectedArtists.add(artist)
                        }
                    },
                    onBack = { currentStep = 0 },
                    onComplete = { onComplete(selectedArtists.toList()) },
                    onSearchArtists = onSearchArtists
                )
            }
        }
    }
}

/**
 * Passo 1: Boas-vindas com visual sofisticado Apple e cartões de vidro translúcido.
 */
@Composable
private fun AppleWelcomeStep(
    onProceed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.93f) }
    val headerAlpha = remember { Animatable(0f) }
    val headerOffsetY = remember { Animatable(22f) }
    val cardsAlpha = remember { Animatable(0f) }
    val cardsOffsetY = remember { Animatable(26f) }
    val ctaAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, tween(360))
            logoScale.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 320f))
        }
        delay(70)
        launch {
            headerAlpha.animateTo(1f, tween(320))
            headerOffsetY.animateTo(0f, spring(dampingRatio = 0.80f, stiffness = 340f))
        }
        delay(80)
        launch {
            cardsAlpha.animateTo(1f, tween(320))
            cardsOffsetY.animateTo(0f, spring(dampingRatio = 0.80f, stiffness = 340f))
        }
        delay(90)
        launch {
            ctaAlpha.animateTo(1f, tween(280))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Badge Apple Music Style com aura e gradiente
        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = logoAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                }
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(AppleCrimson, AppleFlame, AppleAmber)
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = "NERF Music",
                tint = AppleTextPrimary,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Tipografia com proporção Display Apple
        Column(
            modifier = Modifier.graphicsLayer {
                alpha = headerAlpha.value
                translationY = headerOffsetY.value
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NERF Music",
                fontFamily = SfProFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
                color = AppleTextPrimary,
                letterSpacing = (-0.8).sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Música em fidelidade máxima, sem anúncios e com a elegância que seu som merece.",
                fontFamily = SfProFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = AppleTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Cartões translúcidos de vidro fumê (Frosted Glass)
        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    alpha = cardsAlpha.value
                    translationY = cardsOffsetY.value
                },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppleGlassFeatureCard(
                icon = Icons.Default.Headphones,
                title = "Lossless Puro & Sem Anúncios",
                subtitle = "Transmissão contínua em taxa máxima sem nenhuma interrupção."
            )
            AppleGlassFeatureCard(
                icon = Icons.Default.GraphicEq,
                title = "Letras Vivas & Karaokê",
                subtitle = "Sincronia linha por linha idêntica ao Apple Music com isolamento vocal."
            )
            AppleGlassFeatureCard(
                icon = Icons.Default.CloudDownload,
                title = "Smart Downloads",
                subtitle = "Suas músicas favoritas baixadas automaticamente para ouvir em qualquer lugar."
            )
            AppleGlassFeatureCard(
                icon = Icons.Default.Groups,
                title = "Sessões Jam Globais",
                subtitle = "Fila colaborativa sincronizada com amigos em tempo real."
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botão de Continuar estilo Apple Pill com luz interna
        Box(
            modifier = Modifier
                .graphicsLayer { alpha = ctaAlpha.value }
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(AppleCrimson, AppleFlame)
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(27.dp))
                .applePressFeedback(onClick = onProceed),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Começar a personalizar",
                    fontFamily = SfProFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = AppleTextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AppleTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Card de destaque em vidro fumê estilo Apple com borda translúcida.
 */
@Composable
private fun AppleGlassFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppleGlassSurface)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(AppleGlassBorder, Color(0x0FFFFFFF))
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppleFlame,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = SfProFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = AppleTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = SfProFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = AppleTextSecondary,
                lineHeight = 17.sp
            )
        }
    }
}

/**
 * Passo 2: Seleção de Artistas em Esferas Magnéticas estilo Apple Music Onboarding.
 */
@Composable
private fun AppleArtistSelectionStep(
    selectedArtists: List<Artist>,
    onToggleArtist: (Artist) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onSearchArtists: suspend (query: String) -> List<Artist>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Lista de artistas com fotos reais da CDN oficial do YouTube Music
    val curatedArtists = remember {
        listOf(
            Artist(id = "c_taylorswift", name = "Taylor Swift", thumbnailUrl = "https://lh3.googleusercontent.com/FMh1mOI0ufvUCAkbUM6aUmU5WK7O5PnndyyXKP1-DCEip20SQz5eeYn3lZ29p-ASb-19ZfBVc_NKe5Ko=w300-h300-p-l90-rj"),
            Artist(id = "c_theweeknd", name = "The Weeknd", thumbnailUrl = "https://lh3.googleusercontent.com/U-SAmNOu4TynE818gLCfKsuHZ0U5YNEtO9mrjSI9WCCKERs98LzrCal5kajBBTQNwdcisoB2Bn-pHp4=w300-h300-p-l90-rj"),
            Artist(id = "c_drake", name = "Drake", thumbnailUrl = "https://lh3.googleusercontent.com/t-PqprTZvBMmHJ8v9zP0BV4wa7CBCHdF-CODInG-7-EsKWLhOg4lLKsjMeVYUgnTG6FlkWZoa8-new=w300-h300-p-l90-rj"),
            Artist(id = "c_billieeilish", name = "Billie Eilish", thumbnailUrl = "https://lh3.googleusercontent.com/tQC4rOL6xz6FhmFr0ggQExxyGbYSOsyveXVSnPBh2WjEyIzQ9pMHablLJ-0GlMBrLBlBrbWQGmzrV6KN=w300-h300-p-l90-rj"),
            Artist(id = "c_brunomars", name = "Bruno Mars", thumbnailUrl = "https://lh3.googleusercontent.com/hnefGBrazRhn4Z92bdSZBUENl40ONjRiVDsmZKZh-WZ2iCKE-2c7KKR7SNcZfzLHoRyB3E6as8L87YA=w300-h300-p-l90-rj"),
            Artist(id = "c_dualipa", name = "Dua Lipa", thumbnailUrl = "https://lh3.googleusercontent.com/aFx8s1fTuelgxONGbezmTG0EKR8r82uB5H-Q6ZJtssyCWLJWF8GfZNr4tHo84sXdFCPBKrA4R6zXOss=w300-h300-p-l90-rj"),
            Artist(id = "c_travisscott", name = "Travis Scott", thumbnailUrl = "https://lh3.googleusercontent.com/iPLK-SftNUxhdxy5-_g6_o_-r6faLLlCP4UebYWQwF-r2SSsTB2iM59h7EPJTLPP8rmsCYsfD2zIb4Y=w300-h300-p-l90-rj"),
            Artist(id = "c_coldplay", name = "Coldplay", thumbnailUrl = "https://lh3.googleusercontent.com/IOKuXtp8PCQ_Fc-vaRKm3sKIXBxFV51gZheLTH5br-YGnWHFQf_Jywcuk7wbprYRoEbQyS_XZY6-nMJX=w300-h300-p-l90-rj"),
            Artist(id = "c_imaginedragons", name = "Imagine Dragons", thumbnailUrl = "https://lh3.googleusercontent.com/TAadzeojHFGU1EJ5jnOWDn6K8Cf8O2x0F04PVxnZwUEhcaYN0pA0dic49VU7OKGs7oovBTylWx70xhY=w300-h300-p-l90-rj"),
            Artist(id = "c_badbunny", name = "Bad Bunny", thumbnailUrl = "https://lh3.googleusercontent.com/Yzqsh_83kXz4dl0dvPlHTFeQjAkKbX3u-TV-9I7mo-bWm4sCKiHcCBTis2GculkVVNGG76Wwk3uQePeD=w300-h300-p-l90-rj"),
            Artist(id = "c_eminem", name = "Eminem", thumbnailUrl = "https://lh3.googleusercontent.com/JFI6JZrS-Lco4UdpqDfHY5Wgwy51VXWxmNdI7bCBU5CDlIpN6WWyisZ7MGlpjbrxEGYMFpsqoR_UwcE=w300-h300-p-l90-rj"),
            Artist(id = "c_postmalone", name = "Post Malone", thumbnailUrl = "https://lh3.googleusercontent.com/48LfK4z6o-CCEWgHQnQfg0ltcT9tbZSN0qjSh0FSJsJI5GF48j2-pH219ciG1ML-PI80ZGD4Vz6sjg=w300-h300-p-l90-rj"),
            Artist(id = "c_jorgemateus", name = "Jorge & Mateus", thumbnailUrl = "https://lh3.googleusercontent.com/eaGuRLB-CrqZ2n_ZBcxJFWZsLqyHdSZINUnIBx6DJpPSDCgDlB6IsC-gC5Gd2b416s0oQbZ0zWgJqWI=w300-h300-p-l90-rj"),
            Artist(id = "c_henriquejuliano", name = "Henrique & Juliano", thumbnailUrl = "https://lh3.googleusercontent.com/FkRYhIqHJj-8HFPeA6V_vqVIWTFoAh7LMWbZtDHl9TozaTXK3TLqrUJaCHFgn4Rryz7I5c5g5wFUmmtf=w300-h300-p-l90-rj"),
            Artist(id = "c_mariliamendonca", name = "Marília Mendonça", thumbnailUrl = "https://yt3.googleusercontent.com/DHC4m9MMWSFa_phIDlrkVAO62CuY97hupCwvoFnkMtKUsR2GOacQ7ee2khndxl7m96v2EihORHFLFi4=w300-h300-p-l90-rj"),
            Artist(id = "c_anitta", name = "Anitta", thumbnailUrl = "https://yt3.googleusercontent.com/4zSDSTUYxZNl9kL5qy64JyJ4MAsbNuFg46vMol4ncfOJ3i-Ods1B0I6gGc6IZMjeMcq3HPhMn4dEYyk=w300-h300-p-l90-rj"),
            Artist(id = "c_alok", name = "Alok", thumbnailUrl = "https://lh3.googleusercontent.com/Iq1j2UgH4EUoIMlouQS5MSj6KvKlC-Sdr_w_RRTP8cGcsOxFNixlA_S02omyVFMdUaScv50hAh1O4g=w300-h300-p-l90-rj"),
            Artist(id = "c_matue", name = "Matuê", thumbnailUrl = "https://yt3.googleusercontent.com/9YYouyA67mwuoTSJx8vFd297OqtIAN_78jApbTs6aRmMFaOhrGvU0RQcIs4xT8awUbfJeBymgFzWguY=w300-h300-l90-rj"),
            Artist(id = "c_bk", name = "BK'", thumbnailUrl = "https://yt3.googleusercontent.com/paoEq8SYuOejUviXeko1YJPhJb5YQ1KNFLMIIcyAgHBD_o-qgmP2Y6cvYXNRi5hvcrMX65HXIEYUDg=w300-h300-p-l90-rj"),
            Artist(id = "c_caetanoveloso", name = "Caetano Veloso", thumbnailUrl = "https://lh3.googleusercontent.com/W8KmLcIC3XeX92DcqNSxVfzcpvo5EqjISjZgGptVSdKhs8s_Yiqk-OVW1VAU9b59uOj6YURAwgwnfVec=w300-h300-p-l90-rj"),
            Artist(id = "c_charliebrownjr", name = "Charlie Brown Jr.", thumbnailUrl = "https://lh3.googleusercontent.com/4TPPRu4fNGirk0yWtv_5PPABLJg34-ZxNYp8aAjzjTyPr_PWE8Ph7rJQU4PODDBjVdLqmytPOuFdw8X5=w300-h300-p-l90-rj"),
            Artist(id = "c_linkinpark", name = "Linkin Park", thumbnailUrl = "https://lh3.googleusercontent.com/uE72emEZFH3TVtCZoIFYKmnf7vsb42RYQxb4X-lonyqPQuS_mLtKpLfBQ5JdHwUijfQo06BtSB7LoQ=w300-h300-p-l90-rj"),
            Artist(id = "c_gusttavolima", name = "Gusttavo Lima", thumbnailUrl = "https://lh3.googleusercontent.com/H9p3PGJsPryag_qmCOfStskSHc7hYBrPsilqTpzGK-2mh6jsnDGuZ5vBieD1-3VDQJFY8eQyc0r4qPg=w300-h300-p-l90-rj"),
            Artist(id = "c_luansantana", name = "Luan Santana", thumbnailUrl = "https://lh3.googleusercontent.com/dGYIq1NB_f0tbV1KfmEgUEVlPt3bV0FbmISybFywtrnu_3Jpx5z0TRy41aQ3_kN7R-VAU1HPa2JzVQ=w300-h300-p-l90-rj")
        )
    }

    fun onQueryChange(newQuery: String) {
        searchQuery = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            isSearching = false
            searchResults = emptyList()
            return
        }
        searchJob = coroutineScope.launch {
            isSearching = true
            delay(320)
            val results = onSearchArtists(newQuery)
            searchResults = results
            isSearching = false
        }
    }

    val displayArtists = if (searchQuery.isNotBlank()) searchResults else curatedArtists
    val canProceed = selectedArtists.size >= 3

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        // Barra Superior com botão circular estilo Apple
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(AppleGlassSurface)
                    .border(1.dp, AppleGlassBorder, CircleShape)
                    .applePressFeedback(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = AppleTextPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Escolha seus Artistas",
                    fontFamily = SfProFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    color = AppleTextPrimary,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    text = "No mínimo 3 para calibrar sua Home",
                    fontFamily = SfProFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = AppleTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Barra de Pesquisa estilo Pílula Apple (Capsule Search)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(AppleGlassSurface)
                .border(1.dp, AppleGlassBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = AppleTextTertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { onQueryChange(it) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = SfProFontFamily,
                        fontSize = 15.sp,
                        color = AppleTextPrimary
                    ),
                    cursorBrush = SolidColor(AppleCrimson),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Buscar artista...",
                                fontFamily = SfProFontFamily,
                                fontSize = 15.sp,
                                color = AppleTextTertiary
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .applePressFeedback(onClick = { onQueryChange("") }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpar",
                            tint = AppleTextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Grade de Esferas Magnéticas de Artistas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AppleCrimson,
                        modifier = Modifier.size(34.dp),
                        strokeWidth = 2.5.dp
                    )
                }
            } else if (searchQuery.isNotBlank() && displayArtists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum artista encontrado para \"$searchQuery\"",
                        fontFamily = SfProFontFamily,
                        fontSize = 14.sp,
                        color = AppleTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(displayArtists, key = { it.id.ifBlank { it.name } }) { artist ->
                        val isSelected = selectedArtists.any { it.name.equals(artist.name, ignoreCase = true) }
                        AppleArtistSphereCard(
                            artist = artist,
                            isSelected = isSelected,
                            onToggle = { onToggleArtist(artist) }
                        )
                    }
                }
            }
        }

        // Barra Flutuante de Conclusão (Floating Glass Pill)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF141416).copy(alpha = 0.94f))
                .border(1.dp, AppleGlassBorder, RoundedCornerShape(32.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Ticker vertical suave
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = selectedArtists.size,
                        transitionSpec = {
                            (slideInVertically { it / 2 } + fadeIn()).togetherWith(
                                slideOutVertically { -it / 2 } + fadeOut()
                            )
                        },
                        label = "Ticker"
                    ) { count ->
                        val label = when {
                            count == 0 -> "Escolha pelo menos 3"
                            count == 1 -> "Faltam mais 2 artistas"
                            count == 2 -> "Falta mais 1 artista"
                            else -> "$count selecionados • Pronto!"
                        }
                        Text(
                            text = label,
                            fontFamily = SfProFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (canProceed) AppleFlame else AppleTextPrimary
                        )
                    }
                    Text(
                        text = "Calibra suas rádios e feed",
                        fontFamily = SfProFontFamily,
                        fontSize = 12.sp,
                        color = AppleTextSecondary
                    )
                }

                // Botão Continuar
                Box(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(
                            if (canProceed) {
                                Brush.horizontalGradient(listOf(AppleCrimson, AppleFlame))
                            } else {
                                SolidColor(Color.White.copy(alpha = 0.08f))
                            }
                        )
                        .padding(horizontal = 22.dp)
                        .applePressFeedback(
                            enabled = canProceed,
                            onClick = { if (canProceed) onComplete() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Continuar",
                        fontFamily = SfProFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (canProceed) AppleTextPrimary else AppleTextTertiary
                    )
                }
            }
        }
    }
}

/**
 * Esfera individual de Artista (Apple Music Sphere) com mola física na GPU.
 */
@Composable
private fun AppleArtistSphereCard(
    artist: Artist,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Mola elástica tipo Apple Music
    val scaleAnim by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 340f),
        label = "sphereScale"
    )

    val checkAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(180),
        label = "checkAlpha"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.75f,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 420f),
        label = "checkScale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .applePressFeedback(onClick = onToggle),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    brush = if (isSelected) {
                        Brush.sweepGradient(listOf(AppleCrimson, AppleFlame, AppleCrimson))
                    } else {
                        SolidColor(Color.White.copy(alpha = 0.12f))
                    },
                    shape = CircleShape
                )
                .background(Color(0xFF141416)),
            contentAlignment = Alignment.Center
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
                    tint = AppleTextTertiary,
                    modifier = Modifier.size(44.dp)
                )
            }

            // Overlay translúcido com checkmark Apple
            if (checkAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = checkAlpha
                            scaleX = checkScale
                            scaleY = checkScale
                        }
                        .background(AppleCrimson.copy(alpha = 0.40f * checkAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selecionado",
                            tint = AppleCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist.name,
            fontFamily = SfProFontFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
            color = if (isSelected) AppleTextPrimary else AppleTextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
