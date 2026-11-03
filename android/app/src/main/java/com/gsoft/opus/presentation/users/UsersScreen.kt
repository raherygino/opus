package com.gsoft.opus.presentation.users

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gsoft.opus.domain.model.User
import com.gsoft.opus.presentation.personnel.EmptyState
import com.gsoft.opus.presentation.personnel.SearchBar
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.OpusDialog

@Composable
fun UsersScreen(
    onUserClick: (Int) -> Unit,
    onCreateUser: () -> Unit,
    viewModel: UsersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
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
                    onClick = onCreateUser,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Nouvel utilisateur")
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
            } else if (state.filtered.isEmpty()) {
                EmptyState("Aucun utilisateur trouvé")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filtered, key = { it.id }) { user ->
                        UserListItem(
                            user = user,
                            canEdit = state.canEditUser(user),
                            canDelete = state.canDeleteUser(user),
                            onClick = { onUserClick(user.id) },
                            onDelete = { viewModel.requestDelete(user) }
                        )
                    }
                }
            }
        }
    }

    OpusDialog(
        visible = state.deleteTarget != null,
        onDismiss = viewModel::cancelDelete,
        title = "Supprimer l'utilisateur",
        message = "Voulez-vous vraiment supprimer l'utilisateur \"${state.deleteTarget?.username}\" ?",
        confirmText = if (state.isDeleting) "..." else "Supprimer",
        onConfirm = viewModel::confirmDelete,
        cancelText = "Annuler",
        onCancel = viewModel::cancelDelete
    )
}

/** Role accent color — mirrors the desktop `roleColors` map. */
private fun roleColor(roleCode: String?): Color = when (roleCode) {
    "SUPER_ADMIN" -> Color(0xFFEF4444)
    "CHIEF" -> Color(0xFFA855F7)
    "STATION_ADMIN" -> Color(0xFF3B82F6)
    "HEAD_SG" -> Color(0xFF22C55E)
    "HEAD_SED" -> Color(0xFFEAB308)
    "HEAD_PJ" -> Color(0xFFF97316)
    "INVESTIGATOR" -> Color(0xFF06B6D4)
    "OFFICER" -> Color(0xFF6366F1)
    "RECEPTION" -> Color(0xFFEC4899)
    "CLERK" -> Color(0xFF64748B)
    "CUSTODY" -> Color(0xFF14B8A6)
    else -> Color(0xFF94A3B8)
}

@Composable
private fun UserListItem(
    user: User,
    canEdit: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canEdit, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(user)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName(user),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${user.username} · IM ${user.im ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleChip(user.roleName, roleColor(user.roleCode))
                    StatusChip(user.isActive)
                }
                Text(
                    text = "Dernière connexion : ${user.lastLogin?.let { formatLastLogin(it) } ?: "Jamais"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

private fun displayName(user: User): String {
    val name = listOfNotNull(user.lastName, user.firstName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
    return name.ifBlank { user.username }
}

/** "2026-10-19 08:30:00" → "19/10/2026 08:30" (fallback: raw value). */
private fun formatLastLogin(raw: String): String {
    return try {
        val date = raw.substring(0, 10).split("-")
        val time = raw.substring(11, 16)
        "${date[2]}/${date[1]}/${date[0]} $time"
    } catch (_: Exception) {
        raw
    }
}

@Composable
private fun UserAvatar(user: User) {
    val initials = listOfNotNull(user.lastName?.firstOrNull(), user.firstName?.firstOrNull())
        .joinToString("")
        .uppercase()
        .ifBlank { user.username.take(2).uppercase() }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(roleColor(user.roleCode).copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        if (user.isAdminRole) {
            Icon(
                Icons.Outlined.Badge,
                contentDescription = null,
                tint = roleColor(user.roleCode),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = roleColor(user.roleCode)
            )
        }
    }
}

@Composable
private fun RoleChip(roleName: String?, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = roleName ?: "—",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusChip(isActive: Boolean) {
    val background = if (isActive) Color(0xFF22C55E).copy(alpha = 0.12f)
    else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
    val foreground = if (isActive) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (isActive) "Actif" else "Inactif",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = foreground
        )
    }
}
