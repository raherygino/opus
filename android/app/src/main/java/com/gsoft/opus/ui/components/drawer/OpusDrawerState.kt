package com.gsoft.opus.ui.components.drawer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * State holder driving the animated navigation drawer.
 *
 * The drawer is modelled as a single [progress] fraction in `[0f..1f]`:
 * - `0f` -> fully closed (content full screen)
 * - `1f` -> fully open (content pushed aside as a floating card)
 *
 * Using one animatable fraction keeps every visual property
 * (translation, scale, corner radius, rotation, elevation, overlay alpha)
 * perfectly in sync.
 *
 * [isOpened] is a stable boolean that only flips when an open/close
 * animation **completes**. It is read by the overlay to decide whether to
 * intercept taps (so the dimmed scrim only closes the drawer once it is
 * fully open, not while it is animating).
 */
@Stable
class OpusDrawerState(initiallyOpen: Boolean = false) {

    /** Animated open fraction. */
    val progress = Animatable(if (initiallyOpen) 1f else 0f)

    /**
     * Stable settled state — `true` only when the drawer has finished
     * animating open, `false` only when it has finished animating closed.
     */
    var isOpened by mutableStateOf(initiallyOpen)
        private set

    /** Whether the drawer is (or is settling) open. */
    val isOpen: Boolean
        get() = progress.targetValue > 0.5f

    /** Animates the drawer fully open. Suspends until the animation completes. */
    suspend fun open() {
        progress.animateTo(1f, AnimationSpec)
        isOpened = true
    }

    /** Animates the drawer fully closed. Suspends until the animation completes. */
    suspend fun close() {
        progress.animateTo(0f, AnimationSpec)
        isOpened = false
    }

    companion object {
        /** Physically natural spring, ~450ms settle, no visible overshoot on clip. */
        val AnimationSpec: SpringSpec<Float> = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        )

        val Saver: Saver<OpusDrawerState, Boolean> = Saver(
            save = { it.isOpened },
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
