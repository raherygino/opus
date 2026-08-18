package com.gsoft.opus.presentation.personnel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.OpusDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonnelScreen(
    onPersonnelClick: (Int) -> Unit,
    onCreatePersonnel: () -> Unit,
    onCreateMouvement: () -> Unit,
    onCreateComportement: () -> Unit,
    viewModel: PersonnelViewModel = hiltViewModel(),
    mouvementViewModel: MouvementViewModel = hiltViewModel(),
    comportementViewModel: ComportementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val mvtState by mouvementViewModel.state.collectAsState()
    val compState by comportementViewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Refresh data when screen resumes (e.g. returning from form screen)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
                mouvementViewModel.refresh()
                comportementViewModel.refresh()
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
    LaunchedEffect(mvtState.userMessage) {
        mvtState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            mouvementViewModel.dismissMessage()
        }
    }
    LaunchedEffect(compState.userMessage) {
        compState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            comportementViewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            when (selectedTab) {
                0 -> if (state.canCreate) {
                    FloatingActionButton(
                        onClick = onCreatePersonnel,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Ajouter")
                    }
                }
                1 -> FloatingActionButton(
                    onClick = onCreateMouvement,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Nouveau mouvement")
                }
                2 -> if (compState.canCreate) {
                    FloatingActionButton(
                        onClick = onCreateComportement,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Nouveau comportement")
                    }
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
                    text = { Text("Personnel") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Mouvements") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Comportement") }
                )
            }

            when (selectedTab) {
                0 -> PersonnelListTab(
                    state = state,
                    onSearch = viewModel::setSearchQuery,
                    onRefresh = viewModel::refresh,
                    onPersonnelClick = onPersonnelClick,
                    onRequestDelete = viewModel::requestDelete
                )
                1 -> MouvementListTab(
                    state = mvtState,
                    onSearch = mouvementViewModel::setSearchQuery,
                    onRefresh = mouvementViewModel::refresh,
                    onOpenDetail = mouvementViewModel::openDetail,
                    onOpenRetour = mouvementViewModel::openRetourDialog,
                    onRequestDelete = mouvementViewModel::requestDelete
                )
                2 -> ComportementListTab(
                    state = compState,
                    onSearch = comportementViewModel::setSearchQuery,
                    onRefresh = comportementViewModel::refresh,
                    onOpenDetail = comportementViewModel::openDetail,
                    onRequestDelete = comportementViewModel::requestDelete
                )
            }
        }
    }

    // Personnel delete confirmation
    OpusDialog(
        visible = state.deleteTarget != null,
        onDismiss = viewModel::cancelDelete,
        title = "Supprimer le personnel",
        message = "Voulez-vous vraiment supprimer ${state.deleteTarget?.firstname} ${state.deleteTarget?.lastname} ?",
        confirmText = if (state.isDeleting) "..." else "Supprimer",
        onConfirm = viewModel::confirmDelete,
        cancelText = "Annuler",
        onCancel = viewModel::cancelDelete
    )

    // Mouvement detail dialog (short info — dialog is appropriate)
    if (mvtState.detailTarget != null) {
        MouvementDetailDialog(
            state = mvtState,
            onDismiss = mouvementViewModel::closeDetail,
            onDeleteAttachment = mouvementViewModel::deleteDetailAttachment
        )
    }

    if (mvtState.retourTarget != null) {
        MouvementRetourDialog(
            state = mvtState,
            onDateChange = mouvementViewModel::onRetourDateChange,
            onConfirm = mouvementViewModel::confirmRetour,
            onDismiss = mouvementViewModel::closeRetourDialog
        )
    }

    OpusDialog(
        visible = mvtState.deleteTarget != null,
        onDismiss = mouvementViewModel::cancelDelete,
        title = "Supprimer le mouvement",
        message = "Voulez-vous vraiment supprimer ce mouvement ?",
        confirmText = if (mvtState.isDeleting) "..." else "Supprimer",
        onConfirm = mouvementViewModel::confirmDelete,
        cancelText = "Annuler",
        onCancel = mouvementViewModel::cancelDelete
    )

    // Comportement detail dialog
    if (compState.detailTarget != null) {
        ComportementDetailDialog(
            state = compState,
            onDismiss = comportementViewModel::closeDetail
        )
    }

    // Comportement delete confirmation
    OpusDialog(
        visible = compState.deleteTarget != null,
        onDismiss = comportementViewModel::cancelDelete,
        title = "Supprimer le comportement",
        message = "Voulez-vous vraiment supprimer ce comportement ?",
        confirmText = if (compState.isDeleting) "..." else "Supprimer",
        onConfirm = comportementViewModel::confirmDelete,
        cancelText = "Annuler",
        onCancel = comportementViewModel::cancelDelete
    )
}

@Composable
private fun PersonnelListTab(
    state: PersonnelUiState,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onPersonnelClick: (Int) -> Unit,
    onRequestDelete: (Personnel) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = state.searchQuery,
            onQueryChange = onSearch,
            onRefresh = onRefresh,
            modifier = Modifier.padding(16.dp)
        )

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.errorMessage != null) {
            ErrorMessage(
                message = state.errorMessage,
                modifier = Modifier.padding(16.dp)
            )
        } else if (state.filtered.isEmpty()) {
            EmptyState("Aucun personnel trouvé")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.filtered, key = { it.id }) { personnel ->
                    PersonnelListItem(
                        personnel = personnel,
                        canDelete = state.canDelete,
                        onClick = { onPersonnelClick(personnel.id) },
                        onDelete = { onRequestDelete(personnel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonnelListItem(
    personnel: Personnel,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Photo
        val photoUrl = personnel.photo?.let { personnelPhotoUrl(personnel.id) }
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${personnel.firstname} ${personnel.lastname}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${personnel.grade} • IM: ${personnel.im}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!personnel.affectation.isNullOrBlank()) {
                Text(
                    text = personnel.affectation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        StatusBadge(status = personnel.status)

        // Explicit delete button — clearly separated from click-to-open-details
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

@Composable
fun StatusBadge(status: String) {
    val (color, label) = when (status.lowercase()) {
        "present" -> Color(0xFF22C55E) to "Présent"
        "absent" -> Color(0xFFEF4444) to "Absent"
        "conge" -> Color(0xFFF59E0B) to "Congé"
        else -> MaterialTheme.colorScheme.outline to status
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
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
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Rechercher...") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Rafraîchir")
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
