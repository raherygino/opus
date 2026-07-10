package com.gsoft.opus.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val MenuMaxWidth = 300.dp
private val MenuMaxHeight = 440.dp
private val MenuCornerRadius = 24.dp
private val MenuItemCornerRadius = 16.dp

/**
 * Chrome-inspired context menu that floats above the screen content,
 * anchored to the bottom-right where the FAB resides.
 *
 * Supports:
 * - Section headers via [ContextMenuItem.isSectionHeader]
 * - Expandable/collapsible nested items via [ContextMenuItem.children]
 * - Scrolling when the list is taller than [MenuMaxHeight]
 * - Scale + fade-in animation, scrim dismiss, back button dismiss
 *
 * @param expanded    whether the menu is visible.
 * @param onDismiss   called when the scrim is tapped, back is pressed,
 *                    or a leaf item is selected.
 * @param items       menu entries to display.
 * @param onItemClick invoked when a menu item without children is tapped.
 * @param content     the main screen content behind the menu.
 */
@Composable
fun OpusContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<ContextMenuItem>,
    onItemClick: (ContextMenuItem) -> Unit,
    content: @Composable () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "menu_progress"
    )

    val menuAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "menu_alpha"
    )

    val menuElevation by animateFloatAsState(
        targetValue = if (expanded) 16f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "menu_elevation"
    )

    BackHandler(enabled = expanded) {
        onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (progress > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f * progress))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 92.dp)
                    .width(MenuMaxWidth)
                    .graphicsLayer {
                        alpha = menuAlpha
                        scaleX = 0.85f + 0.15f * progress
                        scaleY = 0.85f + 0.15f * progress
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)
                    }
                    .shadow(
                        elevation = menuElevation.dp,
                        shape = RoundedCornerShape(MenuCornerRadius),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ),
                shape = RoundedCornerShape(MenuCornerRadius),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                ContextMenuContent(
                    items = items,
                    onItemClick = { item ->
                        onItemClick(item)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun ContextMenuContent(
    items: List<ContextMenuItem>,
    onItemClick: (ContextMenuItem) -> Unit
) {
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .wrapContentSize()
            .heightIn(max = MenuMaxHeight)
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp)
    ) {
        items.forEachIndexed { index, item ->
            if (item.isSectionHeader) {
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            } else {
                ContextMenuRow(
                    item = item,
                    isExpanded = expandedStates[item.id] ?: false,
                    onToggleExpand = {
                        expandedStates[item.id] = !(expandedStates[item.id] ?: false)
                    },
                    onItemClick = onItemClick,
                    depth = 0
                )
            }
        }
    }
}

@Composable
private fun ContextMenuRow(
    item: ContextMenuItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onItemClick: (ContextMenuItem) -> Unit,
    depth: Int
) {
    val hasChildren = item.children != null && item.children.isNotEmpty()
    val interactionSource = remember { MutableInteractionSource() }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(200),
        label = "chevron_rotation"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(MenuItemCornerRadius))
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = MaterialTheme.colorScheme.primary),
                    role = Role.Button,
                    onClick = {
                        if (hasChildren) {
                            onToggleExpand()
                        } else {
                            onItemClick(item)
                        }
                    }
                )
                .padding(
                    start = (16 + depth * 20).dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = if (depth > 0) 0.5f else 0.7f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.title,
                    style = if (depth > 0) {
                        MaterialTheme.typography.bodyMedium
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    fontWeight = if (depth > 0) FontWeight.Normal else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.subtitle != null) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (item.badgeCount != null && item.badgeCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                BadgedBox(
                    badge = {
                        Badge { Text(text = item.badgeCount.coerceAtMost(99).toString()) }
                    }
                ) {}
            }

            if (hasChildren) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }
        }

        if (hasChildren) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
            ) {
                Column {
                    item.children.forEach { child ->
                        ContextMenuRow(
                            item = child,
                            isExpanded = false,
                            onToggleExpand = {},
                            onItemClick = onItemClick,
                            depth = depth + 1
                        )
                    }
                }
            }
        }
    }
}
