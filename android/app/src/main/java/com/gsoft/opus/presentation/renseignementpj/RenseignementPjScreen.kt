package com.gsoft.opus.presentation.renseignementpj

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gsoft.opus.domain.model.RenseignementPj
import com.gsoft.opus.presentation.personnel.EmptyState
import com.gsoft.opus.presentation.personnel.SearchBar
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.OpusDialog

@Composable
fun RenseignementPjScreen(
    onItemClick: (Int) -> Unit,
    onCreate: () -> Unit,
    viewModel: RenseignementPjViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                FloatingActionButton(onClick = onCreate) {
                    Icon(Icons.Outlined.Add, contentDescription = "Nouveau renseignement")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.errorMessage != null && state.items.isEmpty()) {
                ErrorMessage(message = state.errorMessage!!)
            } else {
                RenseignementPjList(
                    items = state.filtered,
                    onItemClick = onItemClick,
                    onDelete = if (state.canDelete) viewModel::requestDelete else null
                )
            }
        }
    }

    OpusDialog(
        visible = state.deleteTarget != null,
        title = "Supprimer le renseignement",
        message = "Voulez-vous vraiment supprimer le renseignement « ${state.deleteTarget?.natureInfraction} » ? " +
            "Toutes les pièces jointes seront également supprimées.",
        confirmText = "Supprimer",
        cancelText = "Annuler",
        onConfirm = { viewModel.confirmDelete() },
        onCancel = { viewModel.cancelDelete() },
        onDismiss = { viewModel.cancelDelete() }
    )
}

@Composable
private fun RenseignementPjList(
    items: List<RenseignementPj>,
    onItemClick: (Int) -> Unit,
    onDelete: ((RenseignementPj) -> Unit)?
) {
    if (items.isEmpty()) {
        EmptyState(message = "Aucun renseignement à afficher.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.id }) { item ->
            RenseignementPjCard(
                item = item,
                onClick = { onItemClick(item.id) },
                onDelete = onDelete?.let { { it(item) } }
            )
        }
    }
}

@Composable
private fun RenseignementPjCard(
    item: RenseignementPj,
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
                Text(
                    text = item.natureInfraction,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                if (!item.dateLieuFaits.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.dateLieuFaits,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (!item.prejudices.isNullOrBlank()) {
                    Text(
                        text = "Préjudices: ${item.prejudices}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
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
