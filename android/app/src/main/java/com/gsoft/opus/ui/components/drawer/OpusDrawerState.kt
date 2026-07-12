package com.gsoft.opus.ui.components.drawer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * State holder driving the animated navigation drawer.
 *
 * The drawer is modelled as a single [progress] fraction in `[0f..1f]`:
 * - `0f` -> fully closed (content full screen)
 * - `1f` -> fully open (content pushed aside as a floating card)
 *
 * Using one animatable fraction keeps every visual property
 * (translation, scale, corner radius, rotation, elevation, overlay alpha)
 * perfectly in sync and allows gesture-driven scrubbing.
 */
@Stable
class OpusDrawerState(initiallyOpen: Boolean = false) {

    /** Animated open fraction, gesture-scrubable. */
    val progress = Animatable(if (initiallyOpen) 1f else 0f)

    /** Whether the drawer is (or is settling) open. */
    val isOpen: Boolean
        get() = progress.targetValue > 0.5f

    /** Animates the drawer fully open. Suspends until the animation completes. */
    suspend fun open() {
        progress.animateTo(1f, AnimationSpec)
    }

    /** Animates the drawer fully closed. Suspends until the animation completes. */
    suspend fun close() {
        progress.animateTo(0f, AnimationSpec)
    }

    /** Instantly moves the drawer to [fraction] (used while dragging). */
    suspend fun dragTo(fraction: Float) {
        progress.snapTo(fraction.coerceIn(0f, 1f))
    }

    /**
     * Settles the drawer after a drag ends, honouring fling [velocityFraction]
     * (velocity expressed in drawer-widths per second).
     */
    suspend fun settle(velocityFraction: Float) {
        val target = when {
            velocityFraction > FlingThreshold -> 1f
            velocityFraction < -FlingThreshold -> 0f
            else -> if (progress.value >= 0.5f) 1f else 0f
        }
        progress.animateTo(target, AnimationSpec, initialVelocity = velocityFraction)
    }

    companion object {
        /** Physically natural spring, ~450ms settle, no visible overshoot on clip. */
        val AnimationSpec: SpringSpec<Float> = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        )

        private const val FlingThreshold = 1.2f

        val Saver: Saver<OpusDrawerState, Boolean> = Saver(
            save = { it.isOpen },
            restore = { OpusDrawerState(initiallyOpen = it) }
        )
    }
}

/** Remembers an [OpusDrawerState] surviving configuration changes. */
@Composable
fun rememberOpusDrawerState(initiallyOpen: Boolean = false): OpusDrawerState =
    rememberSaveable(saver = OpusDrawerState.Saver) {
        OpusDrawerState(initiallyOpen = initiallyOpen)
    }
