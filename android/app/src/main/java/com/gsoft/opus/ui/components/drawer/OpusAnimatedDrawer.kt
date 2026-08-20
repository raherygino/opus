package com.gsoft.opus.ui.components.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val DrawerWidth = 280.dp
private const val ContentScale = 0.82f
private val ContentCornerRadius = 30.dp
private val ContentElevation = 24.dp
private const val OverlayMaxAlpha = 0.25f
private const val ContentRotationY = -5f
/** Camera distance multiplier expressed in density-independent units. */
private const val CameraDistanceScale = 32f

/**
 * Premium animated navigation drawer.
 *
 * The drawer layer stays fixed on the left while the entire app [content]
 * translates right, scales down, gains rounded corners, elevation and a
 * subtle Y-axis rotation - turning it into a floating card, similar to
 * modern banking apps / Flutter Advanced Drawer.
 *
 * All transformations are driven by a single spring-animated fraction held
 * in [state], read exclusively inside [graphicsLayer] blocks so the
 * animation never triggers recomposition — only the GPU composited layer is
 * re-drawn each frame.
 *
 * Recomposition discipline:
 * - [state].progress is read **only** inside `graphicsLayer {}` lambdas
 *   (draw phase), so animating it never invalidates composition.
 * - [state].isOpen / [state].isOpened are read inside isolated leaf
 *   composables ([DrawerBackHandler] and [DrawerScrim]) so that when they
 *   flip (twice per open/close cycle) only those tiny leaves recompose —
 *   the host [content] (Scaffold + NavHost) is **not** re-invoked.
 *
 * Interaction:
 * - Tap the dark overlay to close
 * - System back closes the drawer before leaving the screen
 *
 * @param state         drawer state, see [rememberOpusDrawerState].
 * @param drawerContent fixed left pane; receives the live open fraction for
 *                      stagger animations.
 * @param content       main application content.
 */
@Composable
fun OpusAnimatedDrawer(
    state: OpusDrawerState,
    drawerContent: @Composable (progress: () -> Float) -> Unit,
    modifier: Modifier = Modifier,
    drawerWidth: Dp = DrawerWidth,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Precompute every density-dependent constant once per recomposition
    // (i.e. only when density / drawerWidth change) instead of re-running
    // toPx()/float math inside the per-frame graphicsLayer lambdas.
    val drawerWidthPx = remember(density, drawerWidth) {
        with(density) { drawerWidth.toPx() }
    }
    val cameraDistancePx = remember(density) {
        CameraDistanceScale * density.density
    }
    val contentElevationPx = remember(density) {
        with(density) { ContentElevation.toPx() }
    }

    // Isolated leaf: only this composable recomposes when isOpen flips,
    // keeping the heavy content() lambda out of the invalidation scope.
    DrawerBackHandler(state = state) {
        scope.launch { state.close() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // ── Fixed drawer layer ──
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .graphicsLayer {
                    // Gentle parallax so the drawer "settles" into place.
                    val p = state.progress.value.coerceIn(0f, 1f)
                    translationX = -0.25f * drawerWidthPx * (1f - p)
                    alpha = p
                }
        ) {
            drawerContent { state.progress.value }
        }

        // ── Transforming content layer ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Clamp to [0,1]: the spring is slightly underdamped and may
                    // overshoot below 0 when closing, which would produce negative
                    // corner sizes / elevation and crash the renderer.
                    val p = state.progress.value.coerceIn(0f, 1f)
                    translationX = drawerWidthPx * p
                    scaleX = 1f - (1f - ContentScale) * p
                    scaleY = 1f - (1f - ContentScale) * p
                    rotationY = ContentRotationY * p
                    cameraDistance = cameraDistancePx
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    shadowElevation = contentElevationPx * p
                    shape = RoundedCornerShape(ContentCornerRadius * p)
                    clip = p > 0.001f
                }
        ) {
            content()

            // Isolated leaf scrim: only this composable recomposes when the
            // drawer settles (isOpened flips), not the content above it.
            // matchParentSize is resolved in this BoxScope and passed in so
            // the scrim stays a measurement-free overlay.
            DrawerScrim(
                state = state,
                modifier = Modifier.matchParentSize(),
                onClose = { scope.launch { state.close() } }
            )
        }
    }
}

/**
 * System back handler leaf.
 *
 * Reading [OpusDrawerState.isOpen] here (instead of in [OpusAnimatedDrawer])
 * scopes the recomposition to this tiny composable, so toggling the back
 * handler on/off does not re-invoke the host `content` lambda.
 */
@Composable
private fun DrawerBackHandler(
    state: OpusDrawerState,
    onClose: () -> Unit
) {
    BackHandler(enabled = state.isOpen, onBack = onClose)
}

/**
 * Dimmed overlay leaf that closes the drawer on tap.
 *
 * Reads [OpusDrawerState.progress] inside `graphicsLayer` (draw phase) and
 * [OpusDrawerState.isOpened] in composition. Because this is its own
 * composable, the `isOpened` flip only invalidates [DrawerScrim] — the
 * sibling `content()` is untouched.
 */
@Composable
private fun DrawerScrim(
    state: OpusDrawerState,
    modifier: Modifier,
    onClose: () -> Unit
) {
    Box(
        modifier = modifier
            .graphicsLayer { alpha = state.progress.value.coerceIn(0f, 1f) }
            .background(Color.Black.copy(alpha = OverlayMaxAlpha))
            .then(
                if (state.isOpened) {
                    // Keyed on Unit: the gesture block does not depend on any
                    // changing value, so it is set up once and reused.
                    Modifier.pointerInput(Unit) {
                        detectTapGestures { onClose() }
                    }
                } else {
                    Modifier
                }
            )
    )
}
