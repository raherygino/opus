package com.gsoft.opus.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.ui.graphics.vector.ImageVector
import com.gsoft.opus.R

/**
 * Model describing a single entry of the bottom navigation bar.
 *
 * Being a plain immutable model, new items (or badges in the future) can be
 * added without touching the navigation bar composable itself.
 */
sealed class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Dashboard : BottomNavItem(
        route = MainRoutes.Dashboard.route,
        labelRes = R.string.nav_dashboard,
        icon = Icons.Outlined.SpaceDashboard,
        selectedIcon = Icons.Filled.SpaceDashboard
    )

    data object Notifications : BottomNavItem(
        route = MainRoutes.Notifications.route,
        labelRes = R.string.nav_notifications,
        icon = Icons.Outlined.Notifications,
        selectedIcon = Icons.Filled.Notifications
    )

    data object Settings : BottomNavItem(
        route = MainRoutes.Settings.route,
        labelRes = R.string.nav_settings,
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings
    )

    data object Profile : BottomNavItem(
        route = MainRoutes.Profile.route,
        labelRes = R.string.nav_profile,
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person
    )

    companion object {
        /** Items rendered inside the pill container, in display order. */
        val items: List<BottomNavItem> = listOf(Dashboard, Notifications, Settings, Profile)
    }
}
