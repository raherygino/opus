package com.gsoft.opus.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.core.Constants
import com.gsoft.opus.domain.model.ColorPalette
import com.gsoft.opus.domain.model.ThemeMode
import com.gsoft.opus.presentation.home.HomeViewModel
import com.gsoft.opus.presentation.settings.SettingsViewModel
import com.gsoft.opus.ui.components.ProfileAvatar

/**
 * Profile destination of the main bottom navigation.
 *
 * Shows the user card, account entries, tools (signature / photo pairing)
 * and preferences. Theme mode and theme color are chosen via dropdown menus.
 */
@Composable
fun ProfileScreen(
    onNavigateToSignature: () -> Unit = {},
    onNavigateToPhoto: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val homeState by homeViewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()
    var themeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var paletteMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val displayName = remember(homeState.firstName, homeState.lastName) {
        val first = homeState.firstName?.takeIf { it.isNotBlank() } ?: ""
        val last = homeState.lastName?.takeIf { it.isNotBlank() } ?: ""
        if (first.isBlank() && last.isBlank()) homeState.username else "$first $last".trim()
    }

    val profilePhotoUrl = homeState.personnelId?.let { id ->
        val baseUrl = Constants.BASE_URL.trimEnd('/')
        val photo = homeState.photo
        if (photo != null) "$baseUrl/api/personnel/$id/photo?v=$photo" else null
    }

    val themeModes = listOf(
        Triple(ThemeMode.LIGHT, "Clair", Icons.Outlined.LightMode),
        Triple(ThemeMode.DARK, "Sombre", Icons.Outlined.DarkMode),
        Triple(ThemeMode.SYSTEM, "Système", Icons.Outlined.SettingsBrightness)
    )

    val themeLabel = themeModes.firstOrNull { it.first == settingsState.themeMode }?.second ?: "Système"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Profile card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(photoUrl = profilePhotoUrl, size = 64.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = homeState.username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Compte
        ProfileSectionLabel("Compte")
        ProfileMenuCard {
            ProfileMenuRow(
                icon = Icons.Outlined.Person,
                label = "Gérer le profil",
                onClick = { }
            )
            ProfileMenuDivider()
            ProfileMenuRow(
                icon = Icons.Outlined.Lock,
                label = "Mot de passe & Sécurité",
                onClick = { }
            )
            ProfileMenuDivider()
            ProfileMenuRow(
                icon = Icons.Outlined.Notifications,
                label = "Notifications",
                onClick = onNavigateToNotifications
            )
        }

        // Outils
        ProfileSectionLabel("Outils")
        ProfileMenuCard {
            ProfileMenuRow(
                icon = Icons.Outlined.Draw,
                label = "Signature",
                onClick = onNavigateToSignature
            )
            ProfileMenuDivider()
            ProfileMenuRow(
                icon = Icons.Outlined.PhotoCamera,
                label = "Photo",
                onClick = onNavigateToPhoto
            )
        }

        // Préférences
        ProfileSectionLabel("Préférences")
        ProfileMenuCard {
            ProfileMenuRow(
                icon = Icons.Outlined.Info,
                label = "À propos",
                onClick = { }
            )
            ProfileMenuDivider()

            // Thème — dropdown with icons
            Box(modifier = Modifier.fillMaxWidth()) {
                ProfileMenuRow(
                    icon = Icons.Outlined.SettingsBrightness,
                    label = "Thème",
                    value = themeLabel,
                    onClick = { themeMenuExpanded = true }
                )
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    DropdownMenu(
                        expanded = themeMenuExpanded,
                        onDismissRequest = { themeMenuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        themeModes.forEach { (mode, label, icon) ->
                        val selected = settingsState.themeMode == mode
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                settingsViewModel.setThemeMode(mode)
                                themeMenuExpanded = false
                            }
                        )
                    }
                    }
                }
            }
            ProfileMenuDivider()

            // Couleur du thème — dropdown with color swatch + label
            Box(modifier = Modifier.fillMaxWidth()) {
                ProfileMenuRow(
                    icon = Icons.Outlined.Palette,
                    label = "Couleur du thème",
                    value = settingsState.colorPalette.displayName,
                    onClick = { paletteMenuExpanded = true }
                )
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    DropdownMenu(
                        expanded = paletteMenuExpanded,
                        onDismissRequest = { paletteMenuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        ColorPalette.entries.forEach { palette ->
                        val selected = settingsState.colorPalette == palette
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = palette.displayName,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(palette.previewColor, CircleShape)
                                )
                            },
                            trailingIcon = {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                settingsViewModel.setColorPalette(palette)
                                paletteMenuExpanded = false
                            }
                        )
                    }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun ProfileMenuCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun ProfileMenuDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 64.dp)
    )
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
