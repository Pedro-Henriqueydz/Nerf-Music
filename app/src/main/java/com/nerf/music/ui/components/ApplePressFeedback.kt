package com.nerf.music.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Curva de aceleração ultra-responsiva e desaceleração física Apple (Spring no-bouncy / damping alto).
 */
val AppleSpringSpec = spring<Float>(
    dampingRatio = 0.80f,
    stiffness = 380f
)

/**
 * Modificador de feedback de pressão física sem Ripple (Padrão Emil Kowalski):
 * Transforma escala diretamente no RenderThread da GPU (graphicsLayer), sem recomposição e a 120Hz.
 */
@Composable
fun Modifier.applePressFeedback(
    enabled: Boolean = true,
    targetScale: Float = 0.94f,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && animationsEnabled) targetScale else 1f,
        animationSpec = AppleSpringSpec,
        label = "applePressScale"
    )

    val pressTransform = if (scale < 0.999f) {
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    } else {
        Modifier
    }

    return this
        .then(pressTransform)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null, // Elimina o efeito ripple do Material
                    enabled = enabled,
                    onClick = onClick
                )
            } else Modifier
        )
}
