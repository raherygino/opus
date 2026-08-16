package com.gsoft.opus.presentation.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.R
import com.gsoft.opus.domain.model.AppNotification
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.OpusDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Semantic colors, aligned with the desktop notifications page.
private val TypeInfoColor = Color(0xFF3B82F6)
private val TypeWarningColor = Color(0xFFF59E0B)
private val TypeSuccessColor = Color(0xFF22C55E)
private val TypeErrorColor = Color(0xFFEF4444)
private val ServicePjColor = Color(0xFF3B82F6)
private val ServiceSgColor = Color(0xFF22C55E)
private val ServiceSedentaireColor = Color(0xFFA855F7)

private fun typeIcon(type: String): ImageVector = when (type) {
    "warning" -> Icons.Outlined.Warning
    "success" -> Icons.Outlined.CheckCircle
    "error" -> Icons.Outlined.ErrorOutline
    else -> Icons.Outlined.Info
}

private fun typeColor(type: String): Color = when (type) {
    "warning" -> TypeWarningColor
    "success" -> TypeSuccessColor
    "error" -> TypeErrorColor
    else -> TypeInfoColor
}

private fun serviceIcon(service: String): ImageVector = when (service) {
    "PJ" -> Icons.Outlined.Gavel
    "SG" -> Icons.Outlined.DirectionsCar
    "Sedentaire" -> Icons.Outlined.Business
    else -> Icons.Outlined.Info
}

private fun serviceColor(service: String): Color = when (service) {
    "PJ" -> ServicePjColor
    "SG" -> ServiceSgColor
    "Sedentaire" -> ServiceSedentaireColor
    else -> Color.Gray
}

private fun serviceLabel(service: String): String = when (service) {
    "PJ" -> "Police Judiciaire"
    "SG" -> "Service Général"
    "Sedentaire" -> "Sédentaire"
    else -> "Système"
}

/** Relative time in French, identical to the desktop implementation. */
private fun formatTimeAgo(dateStr: String): String {
    val date = runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(dateStr)
    }.getOrNull() ?: return dateStr

    val diff = ((Date().time - date.time) / 1000).coerceAtLeast(0)
    return when {
        diff < 60 -> "à l'instant"
        diff < 3600 -> "il y a ${diff / 60} min"
        diff < 86400 -> "il y a ${diff / 3600} h"
        diff < 604800 -> "il y a ${diff / 86400} j"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(date)
    }
}

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (state.isAdmin) R.string.notifications_subtitle_admin
                        else R.string.notifications_subtitle_user
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { viewModel.refresh() }, enabled = !state.isLoading && !state.isRefreshing) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.notifications_refresh),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.unreadCount > 0) {
                TextButton(onClick = viewModel::markAllAsRead, enabled = !state.isMarkingAll) {
                    Icon(
                        imageVector = Icons.Outlined.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.notifications_mark_all_read),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        state.errorMessage?.let {
            ErrorMessage(message = it, modifier = Modifier.padding(top = 8.dp))
        }

        FilterRow(
            state = state,
            onFilterSelected = viewModel::setFilter,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.filtered.isEmpty() -> EmptyState(filter = state.activeFilter)

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.filtered, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = { viewModel.markAsRead(notification.id) },
                        onDelete = { viewModel.requestDelete(notification) }
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }

    OpusDialog(
        visible = state.deleteTarget != null,
        onDismiss = viewModel::cancelDelete,
        title = stringResource(R.string.notifications_delete_title),
        message = stringResource(R.string.notifications_delete_message),
        confirmText = stringResource(R.string.notifications_delete_confirm),
        onConfirm = viewModel::confirmDelete,
        cancelText = stringResource(R.string.logout_cancel),
        onCancel = viewModel::cancelDelete
    )
}

@Composable
private fun FilterRow(
    state: NotificationsUiState,
    onFilterSelected: (NotificationFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    data class FilterSpec(val filter: NotificationFilter, val label: String, val count: Int)

    val filters = buildList {
        add(FilterSpec(NotificationFilter.ALL, "Toutes", state.notifications.size))
        add(FilterSpec(NotificationFilter.UNREAD, "Non lues", state.unreadCount))
        if (state.isAdmin) {
            add(FilterSpec(NotificationFilter.PJ, "PJ", state.notifications.count { it.service == "PJ" && !it.isRead }))
            add(FilterSpec(NotificationFilter.SG, "Service Général", state.notifications.count { it.service == "SG" && !it.isRead }))
            add(FilterSpec(NotificationFilter.SEDENTAIRE, "Sédentaire", state.notifications.count { it.service == "Sedentaire" && !it.isRead }))
        }
    }

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { spec ->
            FilterChip(
                selected = state.activeFilter == spec.filter,
                onClick = { onFilterSelected(spec.filter) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(spec.label)
                        if (spec.count > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (state.activeFilter == spec.filter)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .padding(horizontal = 7.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = spec.count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (state.activeFilter == spec.filter)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = typeColor(notification.type)
    val unread = !notification.isRead

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unread)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (unread)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon(notification.type),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (unread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                notification.message?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = serviceIcon(notification.service),
                            contentDescription = null,
                            tint = serviceColor(notification.service),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = serviceLabel(notification.service),
                            style = MaterialTheme.typography.labelSmall,
                            color = serviceColor(notification.service)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatTimeAgo(notification.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val personnelLine = buildString {
                    if (!notification.personnelNom.isNullOrBlank()) {
                        append(
                            listOfNotNull(notification.personnelPrenoms, notification.personnelNom)
                                .joinToString(" ")
                        )
                        notification.personnelIm?.let { append(" (IM: $it)") }
                    }
                }
                val creatorLine = notification.createdByUsername?.let { "par $it" }
                val metaLine = listOfNotNull(
                    personnelLine.takeIf { it.isNotBlank() },
                    creatorLine
                ).joinToString(" · ")
                if (metaLine.isNotBlank()) {
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Column {
                if (unread) {
                    IconButton(onClick = onMarkAsRead, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.DoneAll,
                            contentDescription = stringResource(R.string.notifications_mark_read),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.notifications_delete_confirm),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(filter: NotificationFilter) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(
                if (filter == NotificationFilter.UNREAD) R.string.notifications_empty_unread
                else R.string.notifications_empty
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
