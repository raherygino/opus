package com.gsoft.opus.presentation.personnel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gsoft.opus.domain.model.Mouvement
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.ui.components.ErrorMessage

@Composable
fun MouvementListTab(
    state: MouvementUiState,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenDetail: (Mouvement) -> Unit,
    onOpenRetour: (Mouvement) -> Unit,
    onRequestDelete: (Mouvement) -> Unit
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
            EmptyState("Aucun mouvement trouvé")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.filtered, key = { it.id }) { mvt ->
                    MouvementListItem(
                        mouvement = mvt,
                        onClick = { onOpenDetail(mvt) },
                        onRetour = { onOpenRetour(mvt) },
                        onDelete = { onRequestDelete(mvt) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MouvementListItem(
    mouvement: Mouvement,
    onClick: () -> Unit,
    onRetour: () -> Unit,
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
                    Text(
                        text = mouvement.typeMouvement,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val name = listOfNotNull(mouvement.prenoms, mouvement.nom)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                    if (name.isNotBlank()) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "IM: ${mouvement.im}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = if (mouvement.isReturned) "present" else "absent")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Départ: ${formatDateDisplay(mouvement.dateDepart)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (!mouvement.isReturned) {
                    TextButton(onClick = onRetour) {
                        Icon(Icons.Outlined.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retour", style = MaterialTheme.typography.labelSmall)
                    }
                }
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

// ─── Mouvement Detail Dialog ────────────────────────────────────────────────

@Composable
fun MouvementDetailDialog(
    state: MouvementUiState,
    onDismiss: () -> Unit,
    onDeleteAttachment: (Int) -> Unit
) {
    val mvt = state.detailTarget ?: return
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Détail du mouvement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRow("Type", mvt.typeMouvement)
                DetailRow("IM", mvt.im)
                mvt.grade?.let { DetailRow("Grade", it) }
                mvt.service?.let { DetailRow("Service", it) }
                mvt.nom?.let { DetailRow("Nom", "${mvt.prenoms ?: ""} $it".trim()) }
                DetailRow("Date départ", formatDateDisplay(mvt.dateDepart))
                mvt.days?.let { DetailRow("Durée", "$it jour(s)") }
                DetailRow("Date retour", formatDateDisplay(mvt.dateRetour))
                DetailRow("Statut", mvt.retour)

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pièces jointes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (state.isLoadingAttachments) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else if (state.detailAttachments.isEmpty()) {
                    Text(
                        text = "Aucune pièce jointe",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.detailAttachments.forEach { att ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = att.title,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                val url = mouvementAttachmentDownloadUrl(mvt.id, att.id)
                                openUrl(context, url)
                            }) {
                                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onDeleteAttachment(att.id) }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Mouvement Retour Dialog ────────────────────────────────────────────────

@Composable
fun MouvementRetourDialog(
    state: MouvementUiState,
    onDateChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmer le retour") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enregistrer la date de retour pour ce mouvement",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = state.retourDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date de retour") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.isSavingRetour) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enregistrement...")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !state.isSavingRetour) {
                Text("Confirmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSavingRetour) {
                Text("Annuler")
            }
        }
    )

    if (showDatePicker) {
        OpusDatePickerDialog(
            visible = true,
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                showDatePicker = false
                onDateChange(millisToIsoDate(millis))
            }
        )
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────────

fun uriToUploadFile(context: android.content.Context, uri: android.net.Uri): UploadFile? {
    return runCatching {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)
        val fileName = queryFileName(resolver, uri) ?: "file"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        UploadFile(fileName = fileName, mimeType = mimeType, bytes = bytes)
    }.getOrNull()
}

private fun queryFileName(
    resolver: android.content.ContentResolver,
    uri: android.net.Uri
): String? {
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            return cursor.getString(nameIndex)
        }
    }
    return uri.lastPathSegment
}

fun openUrl(context: android.content.Context, url: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
