package com.nerf.music.ui.motion

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.abs

private const val FLING_FRICTION_MULTIPLIER = 0.48f
private const val FLING_STOP_VELOCITY_PX_PER_SECOND = 20f
private const val UNCONSUMED_DELTA_TOLERANCE_PX = 0.5f

/**
 * A velocity-preserving fling with a longer, exponential deceleration tail.
 *
 * Dragging remains one-to-one with the finger. This behavior only takes over after release and
 * can be interrupted immediately by the next touch.
 */
@Composable
fun rememberSmoothFlingBehavior(): FlingBehavior {
    val decay = remember {
        exponentialDecay<Float>(
            frictionMultiplier = FLING_FRICTION_MULTIPLIER,
            absVelocityThreshold = FLING_STOP_VELOCITY_PX_PER_SECOND
        )
    }

    return remember(decay) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                var previousValue = 0f
                var remainingVelocity = initialVelocity
                var reachedBoundary = false

                AnimationState(
                    initialValue = 0f,
                    initialVelocity = initialVelocity
                ).animateDecay(decay) {
                    val delta = value - previousValue
                    val consumed = scrollBy(delta)
                    previousValue = value
                    remainingVelocity = velocity

                    if (abs(delta - consumed) > UNCONSUMED_DELTA_TOLERANCE_PX) {
                        reachedBoundary = true
                        cancelAnimation()
                    }
                }

                return if (reachedBoundary) remainingVelocity else 0f
            }
        }
    }
}
