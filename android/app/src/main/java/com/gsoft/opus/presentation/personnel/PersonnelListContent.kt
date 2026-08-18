package com.gsoft.opus.presentation.personnel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.ui.components.ErrorMessage

/**
 * Shared Messages-style personnel list used by both the Gestion Personnel tab
 * and the bottom-navigation Personnels tab:
 * - search bar
 * - horizontal service tabs ("Tous" + each affectation)
 * - rows with photo avatar or Gmail-style letter avatar, name, subtitle,
 *   status badge and a divider between items.
 */
@Composable
fun PersonnelListContent(
    state: PersonnelUiState,
    onSearch: (String) -> Unit,
    onServiceFilter: (String) -> Unit,
    onRefresh: () -> Unit,
    onPersonnelClick: (Int) -> Unit,
    onRequestDelete: ((Personnel) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = state.searchQuery,
            onQueryChange = onSearch,
            onRefresh = onRefresh,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Service tabs — "Tous" plus each affectation present in the data
        ServiceTabsRow(
            services = state.services,
            selected = state.serviceFilter,
            onSelect = onServiceFilter,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            state.errorMessage != null -> ErrorMessage(
                message = state.errorMessage,
                modifier = Modifier.padding(16.dp)
            )
            state.filtered.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun personnel trouvé",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(state.filtered, key = { _, p -> p.id }) { index, personnel ->
                    PersonnelListRow(
                        personnel = personnel,
                        onClick = { onPersonnelClick(personnel.id) },
                        onDelete = onRequestDelete?.let { { it(personnel) } }
                    )
                    if (index < state.filtered.lastIndex) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            modifier = Modifier.padding(start = 76.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceTabsRow(
    services: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ServiceTab(
            label = "Tous",
            selected = selected.isEmpty(),
            onClick = { onSelect("") }
        )
        services.forEach { service ->
            ServiceTab(
                label = service,
                selected = selected == service,
                onClick = { onSelect(service) }
            )
        }
    }
}

@Composable
private fun ServiceTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PersonnelListRow(
    personnel: Personnel,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PersonnelAvatar(personnel = personnel, size = 48.dp)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${personnel.firstname} ${personnel.lastname}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${personnel.grade} • IM: ${personnel.im}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        StatusBadge(status = personnel.status)

        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Gmail-style palette for letter avatars
private val AvatarColors = listOf(
    Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
    Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF009688), Color(0xFF4CAF50),
    Color(0xFFFF9800), Color(0xFF795548)
)

/**
 * Circular personnel avatar: the photo when available, otherwise the first
 * letter of the lastname on a deterministic color (Contacts/Gmail style).
 */
@Composable
fun PersonnelAvatar(
    personnel: Personnel,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val photoUrl = personnel.photo?.let { personnelPhotoUrl(personnel.id) }
    if (photoUrl != null) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        val initial = personnel.lastname.firstOrNull()?.uppercase() ?: "?"
        val color = AvatarColors[
            kotlin.math.abs(personnel.lastname.hashCode()) % AvatarColors.size
        ]
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
