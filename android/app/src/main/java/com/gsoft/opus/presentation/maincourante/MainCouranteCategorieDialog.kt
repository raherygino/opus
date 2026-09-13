package com.gsoft.opus.presentation.maincourante

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.domain.repository.MainCouranteCategorie
import com.gsoft.opus.ui.components.ErrorMessage

@Composable
fun MainCouranteCategorieDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCategoriesChanged: (List<MainCouranteCategorie>) -> Unit = {},
    viewModel: MainCouranteCategorieDialogViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(visible) {
        if (visible) viewModel.load()
    }

    LaunchedEffect(state.categories) {
        onCategoriesChanged(state.categories)
    }

    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // ─── Header ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Tag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gérer les catégories",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Catégories de la main courante",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ─── Create new ────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.newLabel,
                        onValueChange = viewModel::setNewLabel,
                        label = { Text("Nouvelle catégorie") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.createCategory() }),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = viewModel::createCategory,
                        enabled = !state.isCreating && state.newLabel.isNotBlank()
                    ) {
                        if (state.isCreating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Add, contentDescription = "Ajouter")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─── Error ─────────────────────────────────────────────
                state.errorMessage?.let { msg ->
                    ErrorMessage(message = msg)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ─── List ──────────────────────────────────────────────
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (state.categories.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucune catégorie. Ajoutez-en une ci-dessus.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.categories, key = { it.id }) { cat ->
                            CategoryRow(
                                category = cat,
                                isEditing = state.editingId == cat.id,
                                editingLabel = state.editingLabel,
                                isSavingEdit = state.isSavingEdit,
                                onEditingLabelChange = viewModel::setEditingLabel,
                                onStartEdit = viewModel::startEdit,
                                onCancelEdit = viewModel::cancelEdit,
                                onSaveEdit = viewModel::saveEdit,
                                onDelete = { viewModel.requestDelete(cat) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─── Footer ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fermer")
                    }
                }
            }
        }
    }

    // ─── Delete confirmation ──────────────────────────────────────────
    val deleteTarget = state.deleteTarget
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Supprimer la catégorie") },
            text = {
                Text("Voulez-vous vraiment supprimer « ${deleteTarget.label} » ? Les entrées existantes conservent leur libellé.")
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    enabled = !state.isDeleting
                ) {
                    if (state.isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete, enabled = !state.isDeleting) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun CategoryRow(
    category: MainCouranteCategorie,
    isEditing: Boolean,
    editingLabel: String,
    isSavingEdit: Boolean,
    onEditingLabelChange: (String) -> Unit,
    onStartEdit: (MainCouranteCategorie) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEditing) {
            OutlinedTextField(
                value = editingLabel,
                onValueChange = onEditingLabelChange,
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSaveEdit() }),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSaveEdit, enabled = !isSavingEdit && editingLabel.isNotBlank()) {
                if (isSavingEdit) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.Check, contentDescription = "Enregistrer", tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onCancelEdit, enabled = !isSavingEdit) {
                Icon(Icons.Outlined.Close, contentDescription = "Annuler")
            }
        } else {
            Text(
                text = category.label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { onStartEdit(category) }) {
                Icon(Icons.Outlined.Edit, contentDescription = "Modifier", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
