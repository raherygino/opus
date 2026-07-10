package com.gsoft.opus.presentation.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gsoft.opus.R
import com.gsoft.opus.ui.components.PlaceholderScreen

/**
 * Notifications destination of the main bottom navigation.
 */
@Composable
fun NotificationsScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.nav_notifications),
        description = stringResource(R.string.placeholder_coming_soon),
        icon = Icons.Outlined.Notifications
    )
}
