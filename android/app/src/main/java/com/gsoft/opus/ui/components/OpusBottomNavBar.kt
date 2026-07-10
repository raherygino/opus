package com.gsoft.opus.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gsoft.opus.R
import com.gsoft.opus.navigation.BottomNavItem
import kotlin.math.roundToInt

private val BarHeight = 68.dp
private val FabSize = 60.dp
private val IndicatorPadding = 6.dp

/**
 * Premium floating bottom navigation bar.
 *
 * A rounded pill container hosts the regular navigation items with an animated
 * primary-colored selection indicator that slides between items, while a
 * detached circular FAB ("More") floats at the trailing edge.
 *
 * @param items          navigation entries rendered inside the pill.
 * @param selectedRoute  route of the currently selected destination (or null).
 * @param onItemSelected invoked when a navigation item is tapped.
 * @param fabExpanded    whether the FAB is in its expanded/active state.
 * @param onFabClick     invoked when the trailing FAB is tapped.
 * @param modifier       modifier for the bar.
 */
@Composable
fun OpusBottomNavBar(
    items: List<BottomNavItem>,
    selectedRoute: String?,
    onItemSelected: (BottomNavItem) -> Unit,
    fabExpanded: Boolean,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavPill(
            items = items,
            selectedRoute = selectedRoute,
            onItemSelected = onItemSelected,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        MoreFab(
            expanded = fabExpanded,
            onClick = onFabClick
        )
    }
}

/**
 * Rounded container hosting the navigation items and the sliding indicator.
 */
@Composable
private fun NavPill(
    items: List<BottomNavItem>,
    selectedRoute: String?,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = remember(selectedRoute, items) {
        items.indexOfFirst { it.route == selectedRoute }
    }

    Surface(
        modifier = modifier
            .height(BarHeight)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(BarHeight / 2),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(BarHeight / 2),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Box(modifier = Modifier.padding(IndicatorPadding)) {
            var containerWidthPx by remember { mutableIntStateOf(0) }
            val itemWidthPx by remember {
                derivedStateOf {
                    if (items.isEmpty()) 0f else containerWidthPx / items.size.toFloat()
                }
            }

            // Sliding primary-colored indicator behind the selected item.
            val indicatorX by animateFloatAsState(
                targetValue = if (selectedIndex >= 0) selectedIndex * itemWidthPx else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "indicator_x"
            )
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (selectedIndex >= 0) 1f else 0f,
                animationSpec = tween(200),
                label = "indicator_alpha"
            )

            if (itemWidthPx > 0f) {
                val itemWidthDp = with(LocalDensity.current) { itemWidthPx.toDp() }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorX.roundToInt(), 0) }
                        .width(itemWidthDp)
                        .height(BarHeight - IndicatorPadding * 2)
                        .graphicsLayer { alpha = indicatorAlpha }
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape((BarHeight - IndicatorPadding * 2) / 2)
                        )
                )
            }

            Row(
                modifier = Modifier
                    .matchParentSize()
                    .onSizeChanged { containerWidthPx = it.width },
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    NavPillItem(
                        item = item,
                        selected = item.route == selectedRoute,
                        onClick = { onItemSelected(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * A single icon + label entry with color, scale and press micro-interactions.
 */
@Composable
private fun NavPillItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(250),
        label = "nav_item_color"
    )
    val iconScale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.85f
            selected -> 1.1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_item_scale"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.7f,
        animationSpec = tween(250),
        label = "nav_item_label_alpha"
    )

    Column(
        modifier = modifier
            .height(BarHeight - IndicatorPadding * 2)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = 36.dp,
                    color = MaterialTheme.colorScheme.primary
                ),
                role = Role.Tab,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.icon,
            contentDescription = stringResource(item.labelRes),
            tint = contentColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.graphicsLayer { alpha = labelAlpha }
        )
    }
}

/**
 * Detached circular "More" button with a gentle floating motion, press scale,
 * elevation change and a subtle rotation bounce on every tap.
 */
@Composable
private fun MoreFab(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Gentle idle floating effect.
    val floatTransition = rememberInfiniteTransition(label = "fab_float")
    val floatOffset by floatTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_float_offset"
    )

    val fabScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab_scale"
    )
    val fabElevation by animateFloatAsState(
        targetValue = if (pressed) 4f else 14f,
        animationSpec = tween(150),
        label = "fab_elevation"
    )

    // Rotation: 45deg when open (turns + into x), bounce back on close.
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab_rotation"
    )

    Surface(
        modifier = modifier
            .size(FabSize)
            .offset { IntOffset(0, floatOffset.roundToInt()) }
            .graphicsLayer {
                scaleX = fabScale
                scaleY = fabScale
            }
            .shadow(
                elevation = fabElevation.dp,
                shape = CircleShape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                role = Role.Button,
                onClick = onClick
            ),
        shape = CircleShape,
        color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.nav_more),
                tint = if (expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

