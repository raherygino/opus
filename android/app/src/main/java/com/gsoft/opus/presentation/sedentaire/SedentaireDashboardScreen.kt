package com.gsoft.opus.presentation.sedentaire

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gsoft.opus.ui.components.ErrorMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun SedentaireDashboardScreen(
    onCorrespondanceList: () -> Unit,
    onCorrespondanceDetail: (Int) -> Unit,
    onCreateCorrespondance: () -> Unit,
    onDeclarationList: () -> Unit,
    onDeclarationDetail: (Int) -> Unit,
    onCreateDeclaration: () -> Unit,
    onPassationList: () -> Unit,
    onPassationDetail: (Int) -> Unit,
    onCreatePassation: () -> Unit,
    onPersonnelList: () -> Unit,
    onPersonnelDetail: (Int) -> Unit,
    onCreatePersonnel: () -> Unit,
    viewModel: SedentaireDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Refresh when returning to the screen (e.g. after creating a record).
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderCard()

            state.errorMessage?.let { ErrorMessage(message = it) }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                StatCardsSection(state = state,
                    onCorrespondance = onCorrespondanceList,
                    onDeclaration = onDeclarationList,
                    onPassation = onPassationList,
                    onPersonnel = onPersonnelList
                )

                RecentActivityCard(
                    items = state.recentActivity,
                    onItemClick = { item ->
                        when (item.type) {
                            ActivityType.CORRESPONDANCE -> onCorrespondanceDetail(item.targetId)
                            ActivityType.DECLARATION -> onDeclarationDetail(item.targetId)
                            ActivityType.PASSATION -> onPassationDetail(item.targetId)
                            ActivityType.PERSONNEL -> onPersonnelDetail(item.targetId)
                        }
                    }
                )

                QuickActionsCard(
                    state = state,
                    onCorrespondance = onCreateCorrespondance,
                    onDeclaration = onCreateDeclaration,
                    onPassation = onCreatePassation,
                    onPersonnel = onCreatePersonnel
                )
            }
        }
    }
}

@Composable
private fun HeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "DIVISION SÉDENTAIRE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Secrétariat et Chef de Poste",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Vue d'ensemble des activités",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun StatCardsSection(
    state: SedentaireDashboardUiState,
    onCorrespondance: () -> Unit,
    onDeclaration: () -> Unit,
    onPassation: () -> Unit,
    onPersonnel: () -> Unit
) {
    val cards = buildList {
        if (state.canViewCorrespondance) {
            add(StatData(
                label = "Correspondances",
                value = state.correspondances.size.toString(),
                description = "${state.correspondances.count { it.sens.equals("Entrant", ignoreCase = true) }} entrants",
                icon = Icons.Outlined.Email,
                onClick = onCorrespondance
            ))
        }
        if (state.canViewPersonnel) {
            add(StatData(
                label = "Personnel actif",
                value = state.personnel.size.toString(),
                description = "${state.personnel.count { it.status.equals("Actif", ignoreCase = true) }} en service",
                icon = Icons.Outlined.People,
                onClick = onPersonnel
            ))
        }
        if (state.canViewDeclaration) {
            add(StatData(
                label = "Décl. de perte",
                value = state.declarations.size.toString(),
                description = "Total enregistré",
                icon = Icons.Outlined.FileCopy,
                onClick = onDeclaration
            ))
        }
        if (state.canViewPassation) {
            add(StatData(
                label = "Passations",
                value = state.passations.size.toString(),
                description = "Total enregistré",
                icon = Icons.Outlined.Handshake,
                onClick = onPassation
            ))
        }
    }

    if (cards.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { card ->
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        stat = card
                    )
                }
                // Fill the row if there's an odd card out.
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class StatData(
    val label: String,
    val value: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun DashboardStatCard(
    modifier: Modifier = Modifier,
    stat: StatData
) {
    Card(
        modifier = modifier.clickable { stat.onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stat.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = stat.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stat.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stat.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentActivityCard(
    items: List<ActivityItem>,
    onItemClick: (ActivityItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Activité récente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (items.isEmpty()) {
                Text(
                    text = "Aucune activité récente à afficher.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = activityDotColor(item.type),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.action,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatRelativeTime(item.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    state: SedentaireDashboardUiState,
    onCorrespondance: () -> Unit,
    onDeclaration: () -> Unit,
    onPassation: () -> Unit,
    onPersonnel: () -> Unit
) {
    val actions = buildList {
        if (state.canCreateCorrespondance) {
            add(QuickAction("Enregistrer un courrier", Icons.Outlined.Email, onCorrespondance))
        }
        if (state.canCreateDeclaration) {
            add(QuickAction("Déclaration de perte", Icons.Outlined.FileCopy, onDeclaration))
        }
        if (state.canCreatePassation) {
            add(QuickAction("Nouvelle passation", Icons.Outlined.Handshake, onPassation))
        }
        if (state.canCreatePersonnel) {
            add(QuickAction("Nouveau personnel", Icons.Outlined.People, onPersonnel))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Actions rapides",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (actions.isEmpty()) {
                Text(
                    text = "Aucune action disponible pour vos permissions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                actions.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { action.onClick() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun activityDotColor(type: ActivityType): Color = when (type) {
    ActivityType.CORRESPONDANCE -> MaterialTheme.colorScheme.primary
    ActivityType.DECLARATION -> Color(0xFFD97706) // amber
    ActivityType.PASSATION -> Color(0xFF7C3AED) // purple
    ActivityType.PERSONNEL -> Color(0xFF16A34A) // green
}

/** Format an ISO timestamp (e.g. "2026-09-06 14:30:00") as a French relative time string. */
private fun formatRelativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val parsed = runCatching {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.parse(iso.replace('T', ' '))
    }.getOrNull() ?: return iso
    val diffMs = Date().time - parsed.time
    val sec = diffMs / 1000
    if (sec < 60) return "À l'instant"
    val min = sec / 60
    if (min < 60) return "Il y a ${min} min"
    val h = min / 60
    if (h < 24) return "Il y a ${h} h"
    val d = h / 24
    if (d < 7) return "Il y a ${d} j"
    return runCatching {
        val out = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        out.format(parsed)
    }.getOrDefault(iso)
}
