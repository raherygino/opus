package com.gsoft.opus.presentation.objet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.gsoft.opus.domain.model.ObjetSaisi
import com.gsoft.opus.domain.model.ObjetTrouve
import com.gsoft.opus.presentation.personnel.EmptyState
import com.gsoft.opus.presentation.personnel.SearchBar
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.OpusDialog
import com.gsoft.opus.ui.theme.SuccessGreen

@Composable
fun ObjetScreen(
    onSaisiClick: (Int) -> Unit,
    onTrouveClick: (Int) -> Unit,
    onCreateSaisi: () -> Unit,
    onCreateTrouve: () -> Unit,
    viewModel: ObjetViewModel = hiltViewModel()
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
                FloatingActionButton(
                    onClick = {
                        if (state.selectedTab == 0) onCreateSaisi() else onCreateTrouve()
                    }
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Nouvel objet")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = state.selectedTab) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.setTab(0) },
                    text = { Text("Objet saisi") }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.setTab(1) },
                    text = { Text("Objet trouvé") }
                )
            }

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
            } else if (state.errorMessage != null && state.saisiItems.isEmpty() && state.trouveItems.isEmpty()) {
                ErrorMessage(message = state.errorMessage!!)
            } else {
                when (state.selectedTab) {
                    0 -> ObjetSaisiList(
                        items = viewModel.filteredSaisi(),
                        onItemClick = onSaisiClick,
                        onDelete = if (state.canDelete) viewModel::requestDeleteSaisi else null
                    )
                    else -> ObjetTrouveList(
                        items = viewModel.filteredTrouve(),
                        onItemClick = onTrouveClick,
                        onDelete = if (state.canDelete) viewModel::requestDeleteTrouve else null
                    )
                }
            }
        }
    }

    OpusDialog(
        visible = state.deleteSaisiTarget != null || state.deleteTrouveTarget != null,
        title = "Supprimer l'objet",
        message = "Voulez-vous vraiment supprimer cet objet ? Toutes les pièces jointes seront également supprimées.",
        confirmText = "Supprimer",
        cancelText = "Annuler",
        onConfirm = { viewModel.confirmDelete() },
        onCancel = { viewModel.cancelDelete() },
        onDismiss = { viewModel.cancelDelete() }
    )
}

// ─────────────────────────────────────────────────────────────
// OBJET SAISI list
// ─────────────────────────────────────────────────────────────

@Composable
private fun ObjetSaisiList(
    items: List<ObjetSaisi>,
    onItemClick: (Int) -> Unit,
    onDelete: ((ObjetSaisi) -> Unit)?
) {
    if (items.isEmpty()) {
        EmptyState(message = "Aucun objet saisi à afficher.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            ObjetSaisiCard(
                item = item,
                onClick = { onItemClick(item.id) },
                onDelete = onDelete?.let { { it(item) } }
            )
        }
    }
}

@Composable
private fun ObjetSaisiCard(
    item: ObjetSaisi,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    ObjetItemCard(
        leadingIcon = saisiTypeIcon(item.typeObjet),
        accentContainer = MaterialTheme.colorScheme.primaryContainer,
        accentContent = MaterialTheme.colorScheme.onPrimaryContainer,
        imageUrl = item.attachments
            .firstOrNull { isObjetImageAttachment(it.mimeType, it.originalFilename) }
            ?.let { objetSaisiAttachmentDownloadUrl(item.id, it.id) },
        categoryLabel = item.typeObjetLabel,
        statusLabel = null,
        statusPositive = true,
        title = item.motif,
        subtitle = item.proprietaire?.takeIf { it.isNotBlank() }?.let { "Propriétaire : $it" },
        meta = buildList {
            item.numeroDossier?.takeIf { it.isNotBlank() }
                ?.let { add(ObjetMeta(Icons.Outlined.Folder, "Dossier n° $it")) }
            item.createdAt?.takeIf { it.isNotBlank() }
                ?.let { add(ObjetMeta(Icons.Outlined.CalendarMonth, formatDateDisplay(it))) }
            agentDisplayName(item.agentPrenoms, item.agentNom)
                ?.let { add(ObjetMeta(Icons.Outlined.Person, it)) }
        },
        onClick = onClick,
        onDelete = onDelete
    )
}

// ─────────────────────────────────────────────────────────────
// OBJET TROUVÉ list
// ─────────────────────────────────────────────────────────────

@Composable
private fun ObjetTrouveList(
    items: List<ObjetTrouve>,
    onItemClick: (Int) -> Unit,
    onDelete: ((ObjetTrouve) -> Unit)?
) {
    if (items.isEmpty()) {
        EmptyState(message = "Aucun objet trouvé à afficher.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            ObjetTrouveCard(
                item = item,
                onClick = { onItemClick(item.id) },
                onDelete = onDelete?.let { { it(item) } }
            )
        }
    }
}

@Composable
private fun ObjetTrouveCard(
    item: ObjetTrouve,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    ObjetItemCard(
        leadingIcon = Icons.Outlined.Inventory2,
        accentContainer = MaterialTheme.colorScheme.secondaryContainer,
        accentContent = MaterialTheme.colorScheme.onSecondaryContainer,
        imageUrl = item.attachments
            .firstOrNull { isObjetImageAttachment(it.mimeType, it.originalFilename) }
            ?.let { objetTrouveAttachmentDownloadUrl(item.id, it.id) },
        categoryLabel = item.motifDecouverteLabel,
        statusLabel = if (item.restitution) "Restitué" else "Non restitué",
        statusPositive = item.restitution,
        title = item.affaire,
        subtitle = null,
        meta = buildList {
            item.createdAt?.takeIf { it.isNotBlank() }
                ?.let { add(ObjetMeta(Icons.Outlined.CalendarMonth, formatDateDisplay(it))) }
            agentDisplayName(item.agentPrenoms, item.agentNom)
                ?.let { add(ObjetMeta(Icons.Outlined.Person, it)) }
        },
        onClick = onClick,
        onDelete = onDelete
    )
}

// ─────────────────────────────────────────────────────────────
// Shared outlined item card
// ─────────────────────────────────────────────────────────────

private class ObjetMeta(val icon: ImageVector, val text: String)

/**
 * Shared outlined card for both OBJET tabs, following Material 3: subtle
 * outline and rounded corners, a tonal leading visual (first image attachment
 * when available, otherwise a type icon), category and status pills, a title,
 * an optional subtitle, and a small wrapping metadata row (dossier, date,
 * agent). Content adapts to any card width and text length via ellipsis and
 * [FlowRow] wrapping.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ObjetItemCard(
    leadingIcon: ImageVector,
    accentContainer: Color,
    accentContent: Color,
    imageUrl: String?,
    categoryLabel: String,
    statusLabel: String?,
    statusPositive: Boolean,
    title: String,
    subtitle: String?,
    meta: List<ObjetMeta>,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                // Leading visual: attachment image when available, else icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = categoryLabel,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = accentContent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ObjetPill(
                            text = categoryLabel,
                            containerColor = accentContainer,
                            contentColor = accentContent
                        )
                        if (statusLabel != null) {
                            ObjetStatusPill(
                                text = statusLabel,
                                dotColor = if (statusPositive) SuccessGreen
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (meta.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    meta.forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = m.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = m.text,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ObjetPill(text: String, containerColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ObjetStatusPill(text: String, dotColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(dotColor.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.size(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = dotColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun saisiTypeIcon(typeObjet: String): ImageVector = when (typeObjet) {
    "TELEPHONE" -> Icons.Outlined.Smartphone
    "ORDINATEUR" -> Icons.Outlined.Laptop
    "VEHICULE" -> Icons.Outlined.DirectionsCar
    "DOCUMENT" -> Icons.Outlined.Description
    "ARGENT" -> Icons.Outlined.Payments
    "ARME" -> Icons.Outlined.Shield
    "EFFETS_PERSONNELS" -> Icons.Outlined.Backpack
    else -> Icons.Outlined.Inventory2
}

private fun agentDisplayName(prenoms: String?, nom: String?): String? =
    listOfNotNull(prenoms, nom)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .takeIf { it.isNotEmpty() }
