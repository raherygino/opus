package com.gsoft.opus.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gsoft.opus.R

/**
 * Collapsible top header inspired by Google Play Store.
 *
 * Shows the app logo on the left and the user's profile avatar on the right.
 * The header smoothly collapses/expands based on [collapseProgress]:
 *   0f = fully expanded, 1f = fully collapsed.
 *
 * @param collapseProgress 0f..1f
 * @param photoUrl         optional profile photo URL
 * @param onProfileClick   invoked when the avatar is tapped
 */
@Composable
fun CollapsibleHeader(
    collapseProgress: Float,
    photoUrl: String? = null,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = collapseProgress,
        animationSpec = tween(durationMillis = 200),
        label = "header_collapse"
    )

    val expandedHeight = 56.dp
    val alpha = 1f - animatedProgress

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(expandedHeight)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo on the left
            Image(
                painter = painterResource(id = R.drawable.logo_opus),
                contentDescription = "Opus",
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer { this.alpha = alpha }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "OPUS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Profile avatar on the right
            ProfileAvatar(
                photoUrl = photoUrl,
                size = 38.dp,
                onClick = onProfileClick,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )
        }
    }
}

@Composable
fun ProfileAvatar(
    photoUrl: String? = null,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val modifierWithClick = modifier
        .size(size)
        .clip(CircleShape)
        .clickable(onClick = onClick)

    if (photoUrl != null) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Profile",
            contentScale = ContentScale.Crop,
            modifier = modifierWithClick
        )
    } else {
        Box(
            modifier = modifierWithClick
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}
