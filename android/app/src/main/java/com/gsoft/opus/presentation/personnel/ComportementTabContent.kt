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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.gsoft.opus.ui.components.OpusDialog

@Composable
fun ComportementListTab(
    state: ComportementUiState,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenDetail: (Comportement) -> Unit,
    onRequestDelete: (Comportement) -> Unit,
    onRequestConfirm: (Comportement) -> Unit,
    onRequestReject: (Comportement) -> Unit,
    onStatusFilterChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SearchBar(
            query = state.searchQuery,
            onQueryChange = onSearch,
            onRefresh = onRefresh,
            modifier = Modifier.padding(16.dp)
        )

        // Status filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                "" to "Tous",
                "pending" to "En attente",
                "confirmed" to "Confirmés",
                "rejected" to "Rejetés"
            )
            filters.forEach { (value, label) ->
                FilterChip(
                    selected = state.statusFilter == value,
                    onClick = { onStatusFilterChange(value) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                        isAdmin = state.isAdmin,
                        onClick = { onOpenDetail(comp) },
                        onDelete = { onRequestDelete(comp) },
                        onConfirm = { onRequestConfirm(comp) },
                        onReject = { onRequestReject(comp) }
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
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    onReject: () -> Unit
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Date: ${formatDateDisplay(comportement.dateComportement)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ComportementStatusBadge(status = comportement.status)
                }
                if (isAdmin && comportement.isPending) {
                    IconButton(onClick = onConfirm) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = "Confirmer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onReject) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Rejeter",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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
fun ComportementStatusBadge(status: String) {
    val (color, label) = when (status) {
        "confirmed" -> androidx.compose.ui.graphics.Color(0xFF22C55E) to "Confirmé"
        "rejected" -> androidx.compose.ui.graphics.Color(0xFFEF4444) to "Rejeté"
        else -> androidx.compose.ui.graphics.Color(0xFFF59E0B) to "En attente"
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
    onDismiss: () -> Unit,
    onConfirm: (Comportement) -> Unit,
    onReject: (Comportement) -> Unit
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

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Statut: ",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                ComportementStatusBadge(status = comp.status)
            }

            if (comp.isRejected && !comp.rejectedReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Raison du rejet",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = comp.rejectedReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!comp.confirmedByUsername.isNullOrBlank()) {
                Text(
                    text = if (comp.isRejected) "Rejeté par ${comp.confirmedByUsername}" else "Confirmé par ${comp.confirmedByUsername}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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

            // Admin confirm/reject actions for pending records
            if (state.isAdmin && comp.isPending) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                            .clickable { onReject(comp) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Rejeter",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable { onConfirm(comp) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Confirmer",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reject dialog with an optional reason text field.
 */
@Composable
fun ComportementRejectDialog(
    state: ComportementUiState,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val target = state.rejectTarget ?: return
    OpusDetailDialog(
        visible = true,
        onDismiss = onDismiss,
        title = "Rejeter le comportement",
        subtitle = listOfNotNull(target.prenoms, target.nom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { null },
        dismissText = if (state.isRejecting) "..." else "Annuler"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Indiquer la raison du rejet (optionnel)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = state.rejectReason,
                onValueChange = onReasonChange,
                placeholder = { Text("Raison du rejet...") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                    .clickable { if (!state.isRejecting) onConfirm() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (state.isRejecting) "Traitement..." else "Rejeter",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
