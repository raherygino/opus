package com.gsoft.opus.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val HeaderHeight = 56.dp
private val HeaderHeightWithStatusBar = 56.dp

/**
 * Scaffold for the Main screen with a collapsible header and sticky tab bar.
 *
 * CRITICAL SCROLL BEHAVIOR:
 * - The top logo/profile header collapses when scrolling down
 * - The tab bar remains pinned/sticky at the top
 * - When scrolling back up, the header smoothly expands again
 *
 * Uses [nestedScroll] to intercept scroll deltas from the content area
 * and drive the header collapse/expand animation.
 *
 * @param photoUrl         profile photo URL for the header avatar
 * @param onProfileClick   invoked when the avatar is tapped
 * @param tabs             tab items for the sticky tab bar
 * @param selectedRoute    currently selected tab route
 * @param onTabSelected    invoked when a tab is tapped
 * @param content          the scrollable content area
 */
@Composable
fun MainScaffold(
    photoUrl: String? = null,
    onProfileClick: () -> Unit = {},
    tabs: List<TabItem> = emptyList(),
    selectedRoute: String? = null,
    onTabSelected: (TabItem) -> Unit = {},
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val headerHeightPx = with(density) { HeaderHeight.toPx() }

    var collapseOffset by remember { mutableFloatStateOf(0f) }

    val animatedCollapse by animateFloatAsState(
        targetValue = collapseOffset,
        animationSpec = tween(durationMillis = 100),
        label = "header_collapse_offset"
    )

    val collapseProgress = (animatedCollapse / headerHeightPx).coerceIn(0f, 1f)
    val headerHeightDp = with(density) {
        (headerHeightPx + animatedCollapse).toDp().coerceAtLeast(0.dp)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0 && collapseOffset > -headerHeightPx) {
                    // Scrolling up → collapse header
                    val newOffset = (collapseOffset + delta).coerceIn(-headerHeightPx, 0f)
                    val consumed = newOffset - collapseOffset
                    collapseOffset = newOffset
                    return Offset(0f, consumed)
                } else if (delta > 0 && collapseOffset < 0f) {
                    // Scrolling down → expand header
                    val newOffset = (collapseOffset + delta).coerceIn(-headerHeightPx, 0f)
                    val consumed = newOffset - collapseOffset
                    collapseOffset = newOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Collapsible header — collapses based on scroll offset
            CollapsibleHeader(
                collapseProgress = collapseProgress,
                photoUrl = photoUrl,
                onProfileClick = onProfileClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeightDp)
            )

            // Sticky tab bar — always visible, pinned at top
            if (tabs.isNotEmpty()) {
                StickyTabBar(
                    tabs = tabs,
                    selectedRoute = selectedRoute,
                    onTabSelected = onTabSelected
                )
            }

            // Content area — nested scroll drives header collapse
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
            ) {
                content()
            }
        }
    }
}
