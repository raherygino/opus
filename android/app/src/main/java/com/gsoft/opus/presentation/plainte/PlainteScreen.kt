package com.gsoft.opus.presentation.plainte

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gsoft.opus.domain.model.PlainteEntree
import com.gsoft.opus.domain.model.PlainteSortie
import com.gsoft.opus.presentation.personnel.EmptyState
import com.gsoft.opus.presentation.personnel.SearchBar
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.OpusDialog

@Composable
fun PlainteScreen(
    onEntryClick: (Int) -> Unit,
    onSortieClick: (Int) -> Unit,
    onCreateEntree: () -> Unit,
    onCreateSortie: () -> Unit,
    viewModel: PlainteViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Refresh data when screen resumes (e.g. returning from form screen)
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

    // Show user messages as snackbars
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
                FloatingActionButton(onClick = {
                    if (state.tab == PlainteTab.ENTREE) onCreateEntree() else onCreateSortie()
                }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Nouveau")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ─── Tabs ────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = if (state.tab == PlainteTab.ENTREE) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = state.tab == PlainteTab.ENTREE,
                    onClick = { viewModel.setTab(PlainteTab.ENTREE) },
                    text = { Text("ENTRÉE (${state.entrees.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = state.tab == PlainteTab.SORTIE,
                    onClick = { viewModel.setTab(PlainteTab.SORTIE) },
                    text = { Text("SORTIE (${state.sorties.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            // ─── Search bar ───────────────────────────────────────────
            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ─── Content ─────────────────────────────────────────────
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.errorMessage != null && state.entrees.isEmpty() && state.sorties.isEmpty()) {
                ErrorMessage(message = state.errorMessage!!)
            } else {
                when (state.tab) {
                    PlainteTab.ENTREE -> EntreeList(
                        entries = state.filteredEntrees,
                        onItemClick = onEntryClick,
                        onDelete = if (state.canDelete) viewModel::requestDeleteEntree else null
                    )
                    PlainteTab.SORTIE -> SortieList(
                        sorties = state.filteredSorties,
                        onItemClick = onSortieClick,
                        onDelete = if (state.canDelete) viewModel::requestDeleteSortie else null
                    )
                }
            }
        }
    }

    // ─── Delete confirmation dialogs ────────────────────────────────
    OpusDialog(
        visible = state.deleteTargetEntree != null,
        title = "Supprimer la plainte",
        message = "Voulez-vous vraiment supprimer la plainte « ${state.deleteTargetEntree?.numeroDossier} » ? " +
            "La sortie associée et toutes les pièces jointes seront également supprimées.",
        confirmText = "Supprimer",
        cancelText = "Annuler",
        onConfirm = { viewModel.confirmDelete() },
        onCancel = { viewModel.cancelDelete() },
        onDismiss = { viewModel.cancelDelete() }
    )

    OpusDialog(
        visible = state.deleteTargetSortie != null,
        title = "Supprimer la sortie",
        message = "Voulez-vous vraiment supprimer la sortie « ${state.deleteTargetSortie?.numero} » ?",
        confirmText = "Supprimer",
        cancelText = "Annuler",
        onConfirm = { viewModel.confirmDelete() },
        onCancel = { viewModel.cancelDelete() },
        onDismiss = { viewModel.cancelDelete() }
    )
}

@Composable
private fun EntreeList(
    entries: List<PlainteEntree>,
    onItemClick: (Int) -> Unit,
    onDelete: ((PlainteEntree) -> Unit)?
) {
    if (entries.isEmpty()) {
        EmptyState(message = "Aucune plainte ENTRÉE à afficher.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(entries, key = { it.id }) { entry ->
            EntreeCard(
                entry = entry,
                onClick = { onItemClick(entry.id) },
                onDelete = onDelete?.let { { it(entry) } }
            )
        }
    }
}

@Composable
private fun EntreeCard(
    entry: PlainteEntree,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TypeChip(type = entry.type)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = entry.numeroDossier,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatDateDisplay(entry.datePlainte),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!entry.partieCivile.isNullOrBlank()) {
                    Text(
                        text = "PC: ${entry.partieCivile}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "MC: ${entry.miseEnCause ?: "—"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!entry.infraction.isNullOrBlank()) {
                    Text(
                        text = "Infraction: ${entry.infraction}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SortieList(
    sorties: List<PlainteSortie>,
    onItemClick: (Int) -> Unit,
    onDelete: ((PlainteSortie) -> Unit)?
) {
    if (sorties.isEmpty()) {
        EmptyState(message = "Aucune sortie à afficher.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sorties, key = { it.id }) { sortie ->
            SortieCard(
                sortie = sortie,
                onClick = { onItemClick(sortie.id) },
                onDelete = onDelete?.let { { it(sortie) } }
            )
        }
    }
}

@Composable
private fun SortieCard(
    sortie: PlainteSortie,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NatureChip(nature = sortie.nature)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = sortie.numero,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatDateDisplay(sortie.dateSortie),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!sortie.entreeNumeroDossier.isNullOrBlank()) {
                    Text(
                        text = "Entrée: ${sortie.entreeNumeroDossier}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "TTR: ${sortie.numeroTtr ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Substitut: ${sortie.nomSubstitut ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeChip(type: String) {
    val (bg, fg) = when (type) {
        "ST_PARQUET" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "PLAINTE_DIRECTE" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "RAPPORT_POLICE" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = PLAINTE_ENTREE_TYPE_LABELS[type] ?: type,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun NatureChip(nature: String) {
    val (bg, fg) = when (nature) {
        "DAT" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "DEFERREMENT" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = PLAINTE_SORTIE_NATURE_LABELS[nature] ?: nature,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}
