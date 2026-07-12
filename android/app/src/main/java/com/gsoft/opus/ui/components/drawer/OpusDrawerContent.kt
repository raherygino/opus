package com.gsoft.opus.ui.components.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gsoft.opus.R
import com.gsoft.opus.ui.components.ContextMenuItem
import com.gsoft.opus.ui.components.OpusDialog

private val ItemHeight = 56.dp
private val ItemCorner = 16.dp
private val IconSize = 24.dp
private val ProfileSize = 72.dp
private const val StaggerStep = 0.03f
private const val StaggerWindow = 0.45f

/**
 * Fixed left pane of [OpusAnimatedDrawer].
 *
 * Renders the profile header, the navigation entries (with section headers
 * and expandable groups), a logout action and the app version footer.
 * Items stagger-animate in as the drawer opens, driven by [progress].
 *
 * @param items       navigation entries (reuses the [ContextMenuItem] model).
 * @param selectedId  id of the currently selected entry, or null.
 * @param username    display name shown in the header.
 * @param subtitle    optional secondary line (role / email).
 * @param progress    live drawer open fraction for stagger animations.
 * @param onItemClick invoked for leaf items.
 * @param onLogout    invoked when the logout row is tapped.
 * @param appVersion  version label displayed at the bottom.
 */
@Composable
fun OpusDrawerContent(
    items: List<ContextMenuItem>,
    selectedId: String?,
    username: String,
    subtitle: String?,
    progress: () -> Float,
    onItemClick: (ContextMenuItem) -> Unit,
    onLogout: () -> Unit,
    appVersion: String,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        DrawerHeader(username = username, subtitle = subtitle)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                StaggeredEntry(index = index, progress = progress) {
                    when {
                        item.isSectionHeader -> DrawerSectionHeader(title = item.title)
                        item.children != null -> DrawerExpandableItem(
                            item = item,
                            selectedId = selectedId,
                            onItemClick = onItemClick
                        )
                        else -> DrawerItem(
                            item = item,
                            selected = item.id == selectedId,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        DrawerItem(
            item = ContextMenuItem(
                id = "logout",
                title = "Déconnexion",
                icon = Icons.AutoMirrored.Outlined.Logout
            ),
            selected = false,
            onClick = { showLogoutDialog = true }
        )

        Text(
            text = "Version $appVersion",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp)
        )
    }

    OpusDialog(
        visible = showLogoutDialog,
        onDismiss = { showLogoutDialog = false },
        title = stringResource(R.string.logout_title),
        message = stringResource(R.string.logout_message),
        confirmText = stringResource(R.string.logout_confirm),
        onConfirm = {
            showLogoutDialog = false
            onLogout()
        },
        cancelText = stringResource(R.string.logout_cancel),
        onCancel = { showLogoutDialog = false }
    )
}

/** Applies an index-based stagger (fade + slide) driven by drawer progress. */
@Composable
private fun StaggeredEntry(
    index: Int,
    progress: () -> Float,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.graphicsLayer {
            val delay = (index * StaggerStep).coerceAtMost(StaggerWindow)
            val p = ((progress() - delay) / (1f - delay)).coerceIn(0f, 1f)
            alpha = p
            translationX = -24.dp.toPx() * (1f - p)
        }
    ) {
        content()
    }
}

@Composable
private fun DrawerHeader(username: String, subtitle: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(ProfileSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = username.take(1).uppercase().ifEmpty { "?" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = username,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
    )
}

/**
 * A single drawer row with selection indicator, icon scale and color
 * micro-animations following the reference design.
 */
@Composable
private fun DrawerItem(
    item: ContextMenuItem,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (selected) 0.16f else 0f,
        animationSpec = tween(250),
        label = "drawer_item_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "drawer_item_color"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "drawer_item_icon_scale"
    )
    val indicatorHeight by animateFloatAsState(
        targetValue = if (selected) 24f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "drawer_item_indicator"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ItemHeight)
            .clip(RoundedCornerShape(ItemCorner))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                role = Role.Button,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated left indicator bar.
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(indicatorHeight.dp)
                .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.width(12.dp))

        if (item.icon != null) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(IconSize)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        trailing?.invoke()

        Spacer(modifier = Modifier.width(12.dp))
    }
}

/** Expandable group: parent row toggles an animated list of child items. */
@Composable
private fun DrawerExpandableItem(
    item: ContextMenuItem,
    selectedId: String?,
    onItemClick: (ContextMenuItem) -> Unit
) {
    val children = item.children.orEmpty()
    val hasSelectedChild = remember(selectedId, children) {
        children.any { it.id == selectedId }
    }
    var expanded by rememberSaveable(item.id) { mutableStateOf(hasSelectedChild) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "drawer_chevron"
    )

    Column {
        DrawerItem(
            item = item,
            selected = hasSelectedChild && !expanded,
            onClick = { expanded = !expanded },
            trailing = {
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(250)),
            exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(tween(150))
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                children.forEach { child ->
                    DrawerItem(
                        item = child,
                        selected = child.id == selectedId,
                        onClick = { onItemClick(child) }
                    )
                }
            }
        }
    }
}
