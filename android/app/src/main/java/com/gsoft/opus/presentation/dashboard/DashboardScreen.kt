package com.gsoft.opus.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.LocalPolice
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.DashboardStats
import com.gsoft.opus.domain.model.User
import com.gsoft.opus.presentation.home.HomeViewModel
import com.gsoft.opus.ui.components.ErrorMessage
import kotlinx.coroutines.delay

private const val STATS_REFRESH_INTERVAL_MS = 60_000L

@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onPersonnelList: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onGavList: () -> Unit = {},
    onArmementList: () -> Unit = {},
    onMaterielRoulantList: () -> Unit = {},
    onActivitesList: () -> Unit = {},
    onUtilisateurs: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val homeViewModel: HomeViewModel = hiltViewModel()
    val state by homeViewModel.state.collectAsState()
    val statsState by viewModel.state.collectAsState()

    val isCommand = state.roleCode in listOf("SUPER_ADMIN", "CHIEF", "STATION_ADMIN")

    // Refresh when the app comes back to the foreground.
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

    // Periodic refresh while the dashboard tab is displayed.
    LaunchedEffect(Unit) {
        while (true) {
            delay(STATS_REFRESH_INTERVAL_MS)
            viewModel.refresh()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = !state.isLoading,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(200))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickStats(
                    statsState = statsState,
                    user = state.user,
                    onRefresh = viewModel::refresh,
                    onPersonnelList = onPersonnelList,
                    onNotifications = onNotifications,
                    onGavList = onGavList,
                    onArmementList = onArmementList,
                    onMaterielRoulantList = onMaterielRoulantList,
                    onActivitesList = onActivitesList,
                    onUtilisateurs = onUtilisateurs
                )

                if (isCommand) {
                    QuickLinks()
                }

                AccountInfo(state)
            }
        }
    }
}

@Composable
private fun QuickStats(
    statsState: DashboardUiState,
    user: User?,
    onRefresh: () -> Unit,
    onPersonnelList: () -> Unit,
    onNotifications: () -> Unit,
    onGavList: () -> Unit,
    onArmementList: () -> Unit,
    onMaterielRoulantList: () -> Unit,
    onActivitesList: () -> Unit,
    onUtilisateurs: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Vue d'ensemble",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Actualiser",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        statsState.errorMessage?.let { message ->
            ErrorMessage(message = message)
        }

        val stats = statsState.stats
        if (stats == null) {
            if (statsState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            StatGrid(stats = stats, user = user,
                onPersonnelList = onPersonnelList,
                onNotifications = onNotifications,
                onGavList = onGavList,
                onArmementList = onArmementList,
                onMaterielRoulantList = onMaterielRoulantList,
                onActivitesList = onActivitesList,
                onUtilisateurs = onUtilisateurs
            )

            TodayStrip(
                stats = stats,
                onNotifications = onNotifications
            )
        }
    }
}

@Composable
private fun StatGrid(
    stats: DashboardStats,
    user: User?,
    onPersonnelList: () -> Unit,
    onNotifications: () -> Unit,
    onGavList: () -> Unit,
    onArmementList: () -> Unit,
    onMaterielRoulantList: () -> Unit,
    onActivitesList: () -> Unit,
    onUtilisateurs: () -> Unit
) {
    val canViewPersonnel = hasPermission(user, "personnel", PermissionAction.VIEW)
    val canViewUsers = hasPermission(user, "users", PermissionAction.VIEW)
    val canViewActivites = hasPermission(user, "sg_activite", PermissionAction.VIEW)
    val canViewGav = hasPermission(user, "pj_gav", PermissionAction.VIEW)
    val canViewArmement = hasPermission(user, "sedentaire_poste_armement", PermissionAction.VIEW)
    val canViewMaterielRoulant = hasPermission(user, "sedentaire_poste_materiel_roulant", PermissionAction.VIEW)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Personnel actif",
                value = stats.personnelEnService.toString(),
                icon = Icons.Outlined.Badge,
                description = "${stats.personnelEnMouvement} en mouvement · ${stats.personnelTotal} total",
                onClick = onPersonnelList.takeIf { canViewPersonnel }
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Utilisateurs",
                value = stats.usersTotal.toString(),
                icon = Icons.Outlined.Shield,
                description = "${stats.usersActifs} comptes actifs",
                onClick = onUtilisateurs.takeIf { canViewUsers }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Activités",
                value = stats.activites7j.toString(),
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                description = "7 jours · ${stats.activitesAujourdhui} aujourd'hui",
                onClick = onActivitesList.takeIf { canViewActivites }
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "GAV en cours",
                value = stats.gavEnCours.toString(),
                icon = Icons.Outlined.Gavel,
                description = "Gardes à vue en cours",
                alert = true,
                onClick = onGavList.takeIf { canViewGav }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Armes perçues",
                value = stats.armesEnService.toString(),
                icon = Icons.Outlined.MilitaryTech,
                description = "Non réintégrées",
                alert = true,
                onClick = onArmementList.takeIf { canViewArmement }
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Véhicules en service",
                value = stats.vehiculesEnService.toString(),
                icon = Icons.Outlined.DirectionsCar,
                description = "Matériel roulant en mission",
                onClick = onMaterielRoulantList.takeIf { canViewMaterielRoulant }
            )
        }
    }
}

/** Slim "Aujourd'hui" counters: events, logbook entries, unread notifications. */
@Composable
private fun TodayStrip(
    stats: DashboardStats,
    onNotifications: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            TodayMetric(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.WarningAmber,
                label = "Événements",
                value = stats.evenementsAujourdhui
            )
            TodayMetric(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Outlined.EventNote,
                label = "Main courante",
                value = stats.mainCouranteAujourdhui
            )
            TodayMetric(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNotifications() },
                icon = Icons.Outlined.NotificationsNone,
                label = "Non lues",
                value = stats.notificationsNonLues
            )
        }
    }
}

@Composable
private fun TodayMetric(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: Int
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    description: String,
    alert: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val isAlertActive = alert && value != "0"
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            if (isAlertActive) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isAlertActive) MaterialTheme.colorScheme.tertiary
                               else MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickLinks() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Accès rapide",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickLinkItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Business,
                        label = "Division Sédentaire",
                        description = "Secrétariat & Chef de Poste"
                    )
                    QuickLinkItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.LocalPolice,
                        label = "Division SG",
                        description = "Patrouilles & Interventions"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickLinkItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Gavel,
                        label = "Division PJ",
                        description = "Enquêtes & Procédure"
                    )
                    QuickLinkItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.People,
                        label = "Personnel",
                        description = "Gestion des effectifs"
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickLinkItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    description: String
) {
    Card(
        modifier = modifier.clickable { /* TODO: navigate */ },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

@Composable
private fun AccountInfo(state: com.gsoft.opus.presentation.home.HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Informations du compte",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoItem(
                        modifier = Modifier.weight(1f),
                        label = "Nom d'utilisateur",
                        value = state.username
                    )
                    InfoItem(
                        modifier = Modifier.weight(1f),
                        label = "IM",
                        value = state.personnelId?.toString() ?: "—"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoItem(
                        modifier = Modifier.weight(1f),
                        label = "Grade",
                        value = state.grade ?: "—"
                    )
                    InfoItem(
                        modifier = Modifier.weight(1f),
                        label = "Affectation",
                        value = state.affectation ?: "—"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoItem(
                        modifier = Modifier.weight(1f),
                        label = "Rôle système",
                        value = state.roleName ?: "—"
                    )
                    InfoItem(
                        modifier = Modifier.weight(1f),
                        label = "Dernière connexion",
                        value = "—"
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InfoItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
