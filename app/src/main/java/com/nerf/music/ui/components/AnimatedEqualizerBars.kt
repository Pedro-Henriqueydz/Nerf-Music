package com.nerf.music.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Indicador animado de reprodução (Equalizador de áudio estilo YouTube Music Premium).
 * Exibe 3 (ou 4) barrinhas verticais que pulsam de forma suave, sofisticada e orgânica
 * exclusivamente quando `isPlaying == true`. Quando pausado, as barras repousam
 * de forma estática e elegante, sem nenhuma oscilação.
 */
@Composable
fun AnimatedEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = Color.White,
    barCount: Int = 3,
    barWidth: Dp = 2.5.dp,
    barSpacing: Dp = 1.5.dp,
    maxHeight: Dp = 14.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PremiumEqualizerTransition")

    // Animações suaves com curvas senoidais/FastOutSlowInEasing calibradas para um ritmo nobre
    val active1 by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PremiumBar1"
    )
    val active2 by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 440, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PremiumBar2"
    )
    val active3 by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.74f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PremiumBar3"
    )
    val active4 by infiniteTransition.animateFloat(
        initialValue = 0.32f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PremiumBar4"
    )

    // Alturas de repouso (quando pausado, as barras ficam 100% estáticas)
    val target1 = if (isPlaying) active1 else 0.28f
    val target2 = if (isPlaying) active2 else 0.42f
    val target3 = if (isPlaying) active3 else 0.20f
    val target4 = if (isPlaying) active4 else 0.30f

    // Transição suave ao pausar / despausar sem cortes bruscos
    val f1 by animateFloatAsState(targetValue = target1, animationSpec = tween(220, easing = FastOutSlowInEasing), label = "Bar1Fraction")
    val f2 by animateFloatAsState(targetValue = target2, animationSpec = tween(220, easing = FastOutSlowInEasing), label = "Bar2Fraction")
    val f3 by animateFloatAsState(targetValue = target3, animationSpec = tween(220, easing = FastOutSlowInEasing), label = "Bar3Fraction")
    val f4 by animateFloatAsState(targetValue = target4, animationSpec = tween(220, easing = FastOutSlowInEasing), label = "Bar4Fraction")

    val fractions = listOf(f1, f2, f3, f4)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.Bottom
    ) {
        fractions.take(barCount.coerceIn(1, 4)).forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(maxHeight * fraction)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
            )
        }
    }
}
