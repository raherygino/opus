package com.gsoft.opus.presentation.sgdashboard

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
import androidx.compose.material.icons.outlined.CrisisAlert
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Shield
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
fun SgDashboardScreen(
    onRassemblementList: () -> Unit,
    onRassemblementDetail: (Int) -> Unit,
    onCreateRassemblement: () -> Unit,
    onEvenementList: () -> Unit,
    onEvenementDetail: (Int) -> Unit,
    onCreateEvenement: () -> Unit,
    onActiviteList: () -> Unit,
    onActiviteDetail: (Int) -> Unit,
    onCreateActivite: () -> Unit,
    onDispositifList: () -> Unit,
    onDispositifDetail: (Int) -> Unit,
    onCreateDispositif: () -> Unit,
    viewModel: SgDashboardViewModel = hiltViewModel()
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
                StatCardsSection(
                    state = state,
                    onRassemblement = onRassemblementList,
                    onEvenement = onEvenementList,
                    onActivite = onActiviteList,
                    onDispositif = onDispositifList
                )

                RecentActivityCard(
                    items = state.recentActivity,
                    onItemClick = { item ->
                        when (item.type) {
                            SgActivityType.RASSEMBLEMENT -> onRassemblementDetail(item.targetId)
                            SgActivityType.EVENEMENT -> onEvenementDetail(item.targetId)
                            SgActivityType.ACTIVITE -> onActiviteDetail(item.targetId)
                            SgActivityType.DISPOSITIF -> onDispositifDetail(item.targetId)
                        }
                    }
                )

                QuickActionsCard(
                    state = state,
                    onRassemblement = onCreateRassemblement,
                    onEvenement = onCreateEvenement,
                    onActivite = onCreateActivite,
                    onDispositif = onCreateDispositif
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
                text = "DIVISION SERVICE GÉNÉRAL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Patrouilles, interventions et sécurité",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Vue d'ensemble des opérations",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

private data class SgStatData(
    val label: String,
    val value: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun StatCardsSection(
    state: SgDashboardUiState,
    onRassemblement: () -> Unit,
    onEvenement: () -> Unit,
    onActivite: () -> Unit,
    onDispositif: () -> Unit
) {
    val cards = buildList {
        if (state.canViewRassemblement) {
            val latest = state.rassemblements.firstOrNull()
            add(SgStatData(
                label = "Rassemblements",
                value = state.rassemblements.size.toString(),
                description = latest?.let { "Dernier : ${it.present} présents" } ?: "Aucun enregistré",
                icon = Icons.Outlined.Groups,
                onClick = onRassemblement
            ))
        }
        if (state.canViewEvenement) {
            add(SgStatData(
                label = "Événements survenus",
                value = state.evenements.size.toString(),
                description = "${state.evenementsToday} aujourd'hui",
                icon = Icons.Outlined.CrisisAlert,
                onClick = onEvenement
            ))
        }
        if (state.canViewActivite) {
            add(SgStatData(
                label = "Activités",
                value = state.activites.size.toString(),
                description = "${state.activitesToday} aujourd'hui",
                icon = Icons.Outlined.Route,
                onClick = onActivite
            ))
        }
        if (state.canViewDispositif) {
            add(SgStatData(
                label = "Dispositifs",
                value = state.dispositifs.size.toString(),
                description = "${state.dispositifsActifs} en cours",
                icon = Icons.Outlined.Shield,
                onClick = onDispositif
            ))
        }
    }

    if (cards.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { card ->
                    SgStatCard(
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

@Composable
private fun SgStatCard(
    modifier: Modifier = Modifier,
    stat: SgStatData
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
    items: List<SgActivityItem>,
    onItemClick: (SgActivityItem) -> Unit
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
                                    color = sgActivityDotColor(item.type),
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

private data class SgQuickAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionsCard(
    state: SgDashboardUiState,
    onRassemblement: () -> Unit,
    onEvenement: () -> Unit,
    onActivite: () -> Unit,
    onDispositif: () -> Unit
) {
    val actions = buildList {
        if (state.canCreateRassemblement) {
            add(SgQuickAction("Nouveau rassemblement", Icons.Outlined.Groups, onRassemblement))
        }
        if (state.canCreateEvenement) {
            add(SgQuickAction("Signaler un événement", Icons.Outlined.CrisisAlert, onEvenement))
        }
        if (state.canCreateActivite) {
            add(SgQuickAction("Nouvelle activité", Icons.Outlined.Route, onActivite))
        }
        if (state.canCreateDispositif) {
            add(SgQuickAction("Nouveau dispositif", Icons.Outlined.Shield, onDispositif))
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

@Composable
private fun sgActivityDotColor(type: SgActivityType): Color = when (type) {
    SgActivityType.RASSEMBLEMENT -> Color(0xFF10B981) // emerald
    SgActivityType.EVENEMENT -> Color(0xFFEF4444) // red
    SgActivityType.ACTIVITE -> Color(0xFF3B82F6) // blue
    SgActivityType.DISPOSITIF -> Color(0xFFD97706) // amber
}

/** Format an ISO timestamp (e.g. "2026-09-06 14:30:00") as a French relative time string. */
private fun formatRelativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val parsed = runCatching {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.parse(iso)?.time
    }.getOrNull() ?: return iso
    val diffMs = Date().time - parsed
    val sec = diffMs / 1000
    if (sec < 60) return "À l'instant"
    val min = sec / 60
    if (min < 60) return "Il y a ${min} min"
    val h = min / 60
    if (h < 24) return "Il y a ${h} h"
    val d = h / 24
    if (d < 7) return "Il y a ${d} j"
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
    return format.format(Date(parsed))
}
