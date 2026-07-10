package com.gsoft.opus.presentation.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gsoft.opus.R
import com.gsoft.opus.ui.components.PlaceholderScreen

/**
 * Profile destination of the main bottom navigation.
 */
@Composable
fun ProfileScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.nav_profile),
        description = stringResource(R.string.placeholder_coming_soon),
        icon = Icons.Outlined.Person
    )
}
