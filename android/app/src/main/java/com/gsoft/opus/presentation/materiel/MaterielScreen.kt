package com.gsoft.opus.presentation.materiel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gsoft.opus.domain.model.AffectationMateriel
import com.gsoft.opus.domain.model.TypeMateriel
import com.gsoft.opus.presentation.personnel.EmptyState
import com.gsoft.opus.presentation.personnel.SearchBar
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.OpusDialog

@Composable
fun MaterielScreen(
    onAffectationClick: (Int) -> Unit,
    onCreateAffectation: () -> Unit,
    onReintegrate: (Int) -> Unit,
    viewModel: MaterielViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.canCreate) {
                FloatingActionButton(
                    onClick = onCreateAffectation,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Nouvelle affectation")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Affectations (${state.affectations.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Types (${state.types.size})") }
                )
            }

            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(16.dp)
            )

            val loadError = state.errorMessage
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (loadError != null) {
                ErrorMessage(message = loadError, modifier = Modifier.padding(16.dp))
            } else if (selectedTab == 0) {
                if (state.filteredAffectations.isEmpty()) {
                    EmptyState("Aucune affectation trouvée")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.filteredAffectations, key = { it.id }) { aff ->
                            AffectationListItem(
                                affectation = aff,
                                canEdit = state.canEdit,
                                canDelete = state.canDelete,
                                onClick = { onAffectationClick(aff.id) },
                                onReintegrate = { onReintegrate(aff.id) },
                                onDelete = { viewModel.requestDelete(aff) }
                            )
                        }
                    }
                }
            } else {
                if (state.filteredTypes.isEmpty()) {
                    EmptyState("Aucun type de matériel trouvé")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.filteredTypes, key = { it.id }) { type ->
                            TypeMaterielItem(type = type)
                        }
                    }
                }
            }
        }
    }

    OpusDialog(
        visible = state.deleteTarget != null,
        onDismiss = viewModel::cancelDelete,
        title = "Supprimer l'affectation",
        message = "Voulez-vous vraiment supprimer l'affectation du ${state.deleteTarget?.datePerception?.let { formatDateDisplay(it) }} ?",
        confirmText = if (state.isDeleting) "..." else "Supprimer",
        onConfirm = viewModel::confirmDelete,
        cancelText = "Annuler",
        onCancel = viewModel::cancelDelete
    )
}

@Composable
private fun AffectationListItem(
    affectation: AffectationMateriel,
    canEdit: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onReintegrate: () -> Unit,
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
                        text = affectation.materielsSummary.ifBlank { "Affectation #${affectation.id}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Text(
                        text = "Perçue le ${formatDateDisplay(affectation.datePerception)} à ${affectation.heurePerceptionDisplay}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatutChip(reintegre = affectation.isReintegre)
                if (canEdit && !affectation.isReintegre) {
                    IconButton(onClick = onReintegrate) {
                        Icon(
                            Icons.Outlined.TaskAlt,
                            contentDescription = "Réintégrer",
                            tint = MaterialTheme.colorScheme.primary,
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

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Inventory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "Agent: ${affectation.agentDisplay.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TypeMaterielItem(type: TypeMateriel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = type.nom,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (!type.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = type.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatutChip(reintegre: Boolean) {
    val background = if (reintegre) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val foreground = if (reintegre) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (reintegre) "Réintégré" else "Assigné",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = foreground
        )
    }
}
