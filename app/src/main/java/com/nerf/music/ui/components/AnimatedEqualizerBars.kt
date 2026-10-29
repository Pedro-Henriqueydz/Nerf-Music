package com.nerf.music.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val RestingFractions = floatArrayOf(0.28f, 0.42f, 0.22f, 0.32f)

// Curvas irregulares evitam o aspecto mecânico de um loop espelhado. Cada alvo é
// alcançado por uma mola, então a velocidade permanece contínua entre os pulsos.
private val PlayingPatterns = arrayOf(
    floatArrayOf(0.76f, 0.38f, 0.88f, 0.48f, 0.68f, 0.30f),
    floatArrayOf(0.48f, 0.94f, 0.58f, 0.34f, 0.82f, 0.44f),
    floatArrayOf(0.32f, 0.70f, 0.42f, 0.86f, 0.52f, 0.24f),
    floatArrayOf(0.64f, 0.36f, 0.80f, 0.46f, 0.92f, 0.40f)
)

private val PlayingSpring = spring<Float>(
    dampingRatio = 0.86f,
    stiffness = 210f
)

private val RestingSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * Indicador de reprodução com movimento orgânico e baseado em molas.
 *
 * O desenho acontece em um único Canvas para evitar relayout a cada frame. Ao
 * pausar, as barras preservam a velocidade atual e assentam suavemente; com as
 * animações do sistema desativadas, o indicador permanece em uma pose estática.
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
    val visibleBarCount = barCount.coerceIn(1, PlayingPatterns.size)
    val bars = remember {
        List(PlayingPatterns.size) { index -> Animatable(RestingFractions[index]) }
    }
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()

    LaunchedEffect(isPlaying, animationsEnabled) {
        coroutineScope {
            bars.forEachIndexed { index, bar ->
                launch {
                    if (!isPlaying || !animationsEnabled) {
                        if (animationsEnabled) {
                            bar.animateTo(RestingFractions[index], RestingSpring)
                        } else {
                            bar.snapTo(RestingFractions[index])
                        }
                        return@launch
                    }

                    delay(index * 42L)
                    var pulse = index
                    while (isActive) {
                        val pattern = PlayingPatterns[index]
                        bar.animateTo(
                            targetValue = pattern[pulse % pattern.size],
                            animationSpec = PlayingSpring
                        )
                        pulse++
                    }
                }
            }
        }
    }

    val canvasWidth = barWidth * visibleBarCount + barSpacing * (visibleBarCount - 1)
    val density = LocalDensity.current
    val barWidthPx = with(density) { barWidth.toPx() }
    val spacingPx = with(density) { barSpacing.toPx() }

    Canvas(
        modifier = modifier
            .size(width = canvasWidth, height = maxHeight)
            .semantics { contentDescription = if (isPlaying) "Reproduzindo" else "Pausado" }
    ) {
        repeat(visibleBarCount) { index ->
            val height = size.height * bars[index].value.coerceIn(0.16f, 1f)
            val left = index * (barWidthPx + spacingPx)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, size.height - height),
                size = Size(barWidthPx, height),
                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
            )
        }
    }
}
