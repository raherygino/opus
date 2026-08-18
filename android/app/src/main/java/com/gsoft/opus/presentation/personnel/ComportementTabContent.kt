package com.gsoft.opus.presentation.personnel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gsoft.opus.domain.model.Comportement
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.OpusDetailDialog

@Composable
fun ComportementListTab(
    state: ComportementUiState,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenDetail: (Comportement) -> Unit,
    onRequestDelete: (Comportement) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SearchBar(
            query = state.searchQuery,
            onQueryChange = onSearch,
            onRefresh = onRefresh,
            modifier = Modifier.padding(16.dp)
        )

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.errorMessage != null) {
            ErrorMessage(message = state.errorMessage, modifier = Modifier.padding(16.dp))
        } else if (state.filtered.isEmpty()) {
            EmptyState("Aucun comportement trouvé")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.filtered, key = { it.id }) { comp ->
                    ComportementListItem(
                        comportement = comp,
                        canDelete = state.canDelete,
                        onClick = { onOpenDetail(comp) },
                        onDelete = { onRequestDelete(comp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ComportementListItem(
    comportement: Comportement,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val name = listOfNotNull(comportement.prenoms, comportement.nom)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                    if (name.isNotBlank()) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "IM: ${comportement.im}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ComportementTypeBadge(type = comportement.type)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date: ${formatDateDisplay(comportement.dateComportement)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (canDelete) {
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
    }
}

@Composable
fun ComportementTypeBadge(type: String) {
    val (color, label) = when (type.lowercase()) {
        "positive" -> androidx.compose.ui.graphics.Color(0xFF22C55E) to "Positive"
        "negative" -> androidx.compose.ui.graphics.Color(0xFFEF4444) to "Négative"
        else -> MaterialTheme.colorScheme.outline to type
    }
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ComportementDetailDialog(
    state: ComportementUiState,
    onDismiss: () -> Unit
) {
    val comp = state.detailTarget ?: return

    OpusDetailDialog(
        visible = true,
        onDismiss = onDismiss,
        title = "Détail du comportement",
        subtitle = listOfNotNull(comp.prenoms, comp.nom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { null },
        dismissText = "Fermer"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailRow("IM", comp.im)
            comp.grade?.let { DetailRow("Grade", it) }
            comp.service?.let { DetailRow("Service", it) }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Type: ",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                ComportementTypeBadge(type = comp.type)
            }
            DetailRow("Date", formatDateDisplay(comp.dateComportement))

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Motif",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = comp.motif,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!comp.decision.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Décision de la hiérarchie",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = comp.decision,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
