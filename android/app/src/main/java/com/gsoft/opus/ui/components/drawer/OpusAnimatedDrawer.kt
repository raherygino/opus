package com.gsoft.opus.ui.components.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val DrawerWidth = 280.dp
private val EdgeSwipeWidth = 24.dp
private const val ContentScale = 0.82f
private val ContentCornerRadius = 30.dp
private val ContentElevation = 24.dp
private const val OverlayMaxAlpha = 0.25f
private const val ContentRotationY = -5f

/**
 * Premium animated navigation drawer.
 *
 * The drawer layer stays fixed on the left while the entire app [content]
 * translates right, scales down, gains rounded corners, elevation and a
 * subtle Y-axis rotation - turning it into a floating card, similar to
 * modern banking apps / Flutter Advanced Drawer.
 *
 * All transformations are driven by a single spring-animated fraction held
 * in [state], read exclusively inside [graphicsLayer] blocks so scrubbing
 * and animation never trigger recomposition.
 *
 * Gestures:
 * - Swipe right from the left edge to open
 * - Swipe left anywhere (when open) to close
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
    val drawerWidthPx = remember(density, drawerWidth) {
        with(density) { drawerWidth.toPx() }
    }

    val isOpen by remember(state) { derivedStateOf { state.isOpen } }
    val overlayInteractive by remember(state) {
        derivedStateOf { state.progress.value > 0.5f }
    }

    BackHandler(enabled = isOpen) {
        scope.launch { state.close() }
    }

    val dragState = rememberDraggableState { delta ->
        scope.launch { state.dragTo(state.progress.value + delta / drawerWidthPx) }
    }
    val settleDrag: suspend CoroutineScope.(Float) -> Unit = { velocity ->
        state.settle(velocity / drawerWidthPx)
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
                    cameraDistance = 32f * density.density
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    shadowElevation = ContentElevation.toPx() * p
                    shape = RoundedCornerShape(ContentCornerRadius * p)
                    clip = p > 0.001f
                }
        ) {
            content()

            // Dark overlay: fades in with progress, catches taps/swipes when open.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = state.progress.value.coerceIn(0f, 1f) }
                    .background(Color.Black.copy(alpha = OverlayMaxAlpha))
                    .then(
                        if (overlayInteractive) {
                            Modifier
                                .pointerInput(state) {
                                    detectTapGestures { scope.launch { state.close() } }
                                }
                                .draggable(
                                    state = dragState,
                                    orientation = Orientation.Horizontal,
                                    onDragStopped = settleDrag
                                )
                        } else {
                            Modifier
                        }
                    )
            )
        }

        // ── Left-edge swipe catcher (only while closed) ──
        if (!overlayInteractive) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(EdgeSwipeWidth)
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Horizontal,
                        onDragStopped = settleDrag
                    )
            )
        }
    }
}
