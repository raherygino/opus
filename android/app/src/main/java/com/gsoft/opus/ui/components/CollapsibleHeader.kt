package com.gsoft.opus.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gsoft.opus.R

val AppBarHeight = 70.dp
val AppBarLogoSize = 65.dp

private val AvatarShape = RoundedCornerShape(12.dp)

@Composable
fun OpusTopAppBar(
    onMenuClick: () -> Unit = {},
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppBarHeight)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Image(
                painter = painterResource(id = R.drawable.logo_csp_150),
                contentDescription = "CSP",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(AppBarLogoSize)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OPUS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Image(
                painter = painterResource(id = R.drawable.logo_pn_150),
                contentDescription = "Opus",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(AppBarLogoSize)
            )

            Spacer(modifier = Modifier.width(8.dp))
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
        .clip(AvatarShape)
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
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}
