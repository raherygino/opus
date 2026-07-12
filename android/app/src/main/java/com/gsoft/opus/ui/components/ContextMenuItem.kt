package com.gsoft.opus.ui.components

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data model for a navigation drawer entry.
 *
 * Designed to be extensible: future enhancements such as [badgeCount],
 * nested actions, or enabled/disabled states can be added without
 * changing the drawer composable signatures.
 *
 * @param id              unique identifier for this menu item.
 * @param title           primary label displayed to the user.
 * @param icon            Material [ImageVector] shown as the leading icon.
 * @param subtitle        optional secondary text shown below the title.
 * @param badgeCount      optional badge number (e.g. unread count). When
 *                        non-null and greater than zero a badge is rendered.
 * @param children        optional nested sub-items. When non-null the item
 *                        becomes expandable/collapsible instead of navigable.
 * @param isSectionHeader when true the item is rendered as a non-interactive
 *                        section header label (no icon, no ripple).
 */
data class ContextMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector? = null,
    val subtitle: String? = null,
    val badgeCount: Int? = null,
    val children: List<ContextMenuItem>? = null,
    val isSectionHeader: Boolean = false
)
