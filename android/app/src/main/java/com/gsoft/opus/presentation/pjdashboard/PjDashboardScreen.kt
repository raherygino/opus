package com.gsoft.opus.presentation.pjdashboard

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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalPolice
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ViewColumn
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
fun PjDashboardScreen(
    onPlainteList: () -> Unit,
    onPlainteDetail: (Int) -> Unit,
    onCreatePlainte: () -> Unit,
    onEnquete: () -> Unit,
    onMandatList: () -> Unit,
    onMandatDetail: (Int) -> Unit,
    onCreateMandat: () -> Unit,
    onConvocationList: () -> Unit,
    onConvocationDetail: (Int) -> Unit,
    onCreateConvocation: () -> Unit,
    onArrestationList: () -> Unit,
    onArrestationDetail: (Int) -> Unit,
    onCreateArrestation: () -> Unit,
    onGavList: () -> Unit,
    onGavDetail: (Int) -> Unit,
    onCreateGav: () -> Unit,
    onRequisitionList: () -> Unit,
    onRequisitionDetail: (Int) -> Unit,
    onCreateRequisition: () -> Unit,
    onPersonneRechercheeList: () -> Unit,
    onPersonneRechercheeDetail: (Int) -> Unit,
    onCreatePersonneRecherchee: () -> Unit,
    onObjetsList: () -> Unit,
    onObjetSaisiDetail: (Int) -> Unit,
    onObjetTrouveDetail: (Int) -> Unit,
    onCreateObjet: () -> Unit,
    onPerquisitionList: () -> Unit,
    onPerquisitionDetail: (Int) -> Unit,
    onCreatePerquisition: () -> Unit,
    onDeferrement: () -> Unit,
    onRenseignementList: () -> Unit,
    onRenseignementDetail: (Int) -> Unit,
    onCreateRenseignement: () -> Unit,
    viewModel: PjDashboardViewModel = hiltViewModel()
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
                    onPlainte = onPlainteList,
                    onEnquete = onEnquete,
                    onMandat = onMandatList,
                    onConvocation = onConvocationList,
                    onArrestation = onArrestationList,
                    onGav = onGavList,
                    onRequisition = onRequisitionList,
                    onPersonneRecherchee = onPersonneRechercheeList,
                    onObjets = onObjetsList,
                    onPerquisition = onPerquisitionList,
                    onDeferrement = onDeferrement,
                    onRenseignement = onRenseignementList
                )

                RecentActivityCard(
                    items = state.recentActivity,
                    onItemClick = { item ->
                        when (item.type) {
                            PjActivityType.PLAINTE -> onPlainteDetail(item.targetId)
                            PjActivityType.MANDAT -> onMandatDetail(item.targetId)
                            PjActivityType.CONVOCATION -> onConvocationDetail(item.targetId)
                            PjActivityType.ARRESTATION -> onArrestationDetail(item.targetId)
                            PjActivityType.GAV -> onGavDetail(item.targetId)
                            PjActivityType.REQUISITION -> onRequisitionDetail(item.targetId)
                            PjActivityType.PERSONNE_RECHERCHEE -> onPersonneRechercheeDetail(item.targetId)
                            PjActivityType.OBJET_SAISI -> onObjetSaisiDetail(item.targetId)
                            PjActivityType.OBJET_TROUVE -> onObjetTrouveDetail(item.targetId)
                            PjActivityType.PERQUISITION -> onPerquisitionDetail(item.targetId)
                            PjActivityType.RENSEIGNEMENT -> onRenseignementDetail(item.targetId)
                        }
                    }
                )

                QuickActionsCard(
                    state = state,
                    onPlainte = onCreatePlainte,
                    onEnquete = onEnquete,
                    onMandat = onCreateMandat,
                    onConvocation = onCreateConvocation,
                    onArrestation = onCreateArrestation,
                    onGav = onCreateGav,
                    onRequisition = onCreateRequisition,
                    onPersonneRecherchee = onCreatePersonneRecherchee,
                    onObjet = onCreateObjet,
                    onPerquisition = onCreatePerquisition,
                    onDeferrement = onDeferrement,
                    onRenseignement = onCreateRenseignement
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
                text = "DIVISION POLICE JUDICIAIRE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enquêtes, plaintes et procédure",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Vue d'ensemble des dossiers",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

private data class PjStatData(
    val label: String,
    val value: String,
    val description: String,
    val icon: ImageVector,
    val comingSoon: Boolean = false,
    val onClick: () -> Unit
)

@Composable
private fun StatCardsSection(
    state: PjDashboardUiState,
    onPlainte: () -> Unit,
    onEnquete: () -> Unit,
    onMandat: () -> Unit,
    onConvocation: () -> Unit,
    onArrestation: () -> Unit,
    onGav: () -> Unit,
    onRequisition: () -> Unit,
    onPersonneRecherchee: () -> Unit,
    onObjets: () -> Unit,
    onPerquisition: () -> Unit,
    onDeferrement: () -> Unit,
    onRenseignement: () -> Unit
) {
    val cards = buildList {
        if (state.canViewPlainte) {
            add(PjStatData(
                label = "Plaintes reçues",
                value = state.plaintes.size.toString(),
                description = "${state.plaintesPending.size} sans sortie",
                icon = Icons.Outlined.Description,
                onClick = onPlainte
            ))
        }
        if (state.canViewEnquete) {
            add(PjStatData(
                label = "Registre d'enquête",
                value = "Bientôt",
                description = "Bientôt disponible",
                icon = Icons.Outlined.FindInPage,
                comingSoon = true,
                onClick = onEnquete
            ))
        }
        if (state.canViewMandat) {
            add(PjStatData(
                label = "Mandats",
                value = state.mandats.size.toString(),
                description = "Total enregistré",
                icon = Icons.Outlined.FilePresent,
                onClick = onMandat
            ))
        }
        if (state.canViewConvocation) {
            add(PjStatData(
                label = "Convocations",
                value = state.convocations.size.toString(),
                description = "Total enregistré",
                icon = Icons.Outlined.Email,
                onClick = onConvocation
            ))
        }
        if (state.canViewArrestation) {
            add(PjStatData(
                label = "Arrestations",
                value = state.arrestations.size.toString(),
                description = "Total enregistré",
                icon = Icons.Outlined.LocalPolice,
                onClick = onArrestation
            ))
        }
        if (state.canViewGav) {
            add(PjStatData(
                label = "Gardes à vue",
                value = state.gavs.size.toString(),
                description = "Total enregistré",
                icon = Icons.Outlined.Lock,
                onClick = onGav
            ))
        }
        if (state.canViewRequisition) {
            add(PjStatData(
                label = "Réquisitions",
                value = state.requisitions.size.toString(),
                description = "Total enregistré",
                icon = Icons.Outlined.FilePresent,
                onClick = onRequisition
            ))
        }
        if (state.canViewPersonneRecherchee) {
            add(PjStatData(
                label = "Personnes rech.",
                value = state.personnesRecherchees.size.toString(),
                description = "Total enregistré",
                icon = Icons.Outlined.PersonSearch,
                onClick = onPersonneRecherchee
            ))
        }
        if (state.canViewObjets) {
            add(PjStatData(
                label = "Objets",
                value = (state.objetsSaisis.size + state.objetsTrouves.size).toString(),
                description = "${state.objetsSaisis.size} saisis · ${state.objetsTrouves.size} trouvés",
                icon = Icons.Outlined.Inventory2,
                onClick = onObjets
            ))
        }
        if (state.canViewPerquisition) {
            add(PjStatData(
                label = "Perquisitions",
                value = state.perquisitions.size.toString(),
                description = "Total enregistré",
                icon = Icons.Outlined.FindInPage,
                onClick = onPerquisition
            ))
        }
        if (state.canViewDeferrement) {
            add(PjStatData(
                label = "Registre déferrement",
                value = "Bientôt",
                description = "Bientôt disponible",
                icon = Icons.Outlined.Gavel,
                comingSoon = true,
                onClick = onDeferrement
            ))
        }
        if (state.canViewRenseignement) {
            add(PjStatData(
                label = "Renseignements",
                value = state.renseignements.size.toString(),
                description = "Total enregistré",
                icon = Icons.Outlined.Message,
                onClick = onRenseignement
            ))
        }
    }

    if (cards.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { card ->
                    PjDashboardStatCard(
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
private fun PjDashboardStatCard(
    modifier: Modifier = Modifier,
    stat: PjStatData
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
                style = if (stat.comingSoon) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = if (stat.comingSoon) FontWeight.SemiBold else FontWeight.Bold,
                color = if (stat.comingSoon) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurface
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
    items: List<PjActivityItem>,
    onItemClick: (PjActivityItem) -> Unit
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
                                    color = pjActivityDotColor(item.type),
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

private data class PjQuickAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionsCard(
    state: PjDashboardUiState,
    onPlainte: () -> Unit,
    onEnquete: () -> Unit,
    onMandat: () -> Unit,
    onConvocation: () -> Unit,
    onArrestation: () -> Unit,
    onGav: () -> Unit,
    onRequisition: () -> Unit,
    onPersonneRecherchee: () -> Unit,
    onObjet: () -> Unit,
    onPerquisition: () -> Unit,
    onDeferrement: () -> Unit,
    onRenseignement: () -> Unit
) {
    val actions = buildList {
        if (state.canCreatePlainte) {
            add(PjQuickAction("Nouvelle plainte", Icons.Outlined.Description, onPlainte))
        }
        if (state.canCreateEnquete) {
            add(PjQuickAction("Registre d'enquête", Icons.Outlined.FindInPage, onEnquete))
        }
        if (state.canCreateMandat) {
            add(PjQuickAction("Créer un mandat", Icons.Outlined.FilePresent, onMandat))
        }
        if (state.canCreateConvocation) {
            add(PjQuickAction("Nouvelle convocation", Icons.Outlined.Email, onConvocation))
        }
        if (state.canCreateArrestation) {
            add(PjQuickAction("Nouvelle arrestation", Icons.Outlined.LocalPolice, onArrestation))
        }
        if (state.canCreateGav) {
            add(PjQuickAction("Enregistrer une GAV", Icons.Outlined.Lock, onGav))
        }
        if (state.canCreateRequisition) {
            add(PjQuickAction("Nouvelle réquisition", Icons.Outlined.FilePresent, onRequisition))
        }
        if (state.canCreatePersonneRecherchee) {
            add(PjQuickAction("Personne recherchée", Icons.Outlined.PersonSearch, onPersonneRecherchee))
        }
        if (state.canCreateObjets) {
            add(PjQuickAction("Objet saisi", Icons.Outlined.Inventory2, onObjet))
        }
        if (state.canCreatePerquisition) {
            add(PjQuickAction("Nouvelle perquisition", Icons.Outlined.FindInPage, onPerquisition))
        }
        if (state.canCreateDeferrement) {
            add(PjQuickAction("Registre de déferrement", Icons.Outlined.Gavel, onDeferrement))
        }
        if (state.canCreateRenseignement) {
            add(PjQuickAction("Nouveau renseignement", Icons.Outlined.Message, onRenseignement))
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
private fun pjActivityDotColor(type: PjActivityType): Color = when (type) {
    PjActivityType.PLAINTE -> Color(0xFF3B82F6) // blue
    PjActivityType.MANDAT -> Color(0xFF7C3AED) // purple
    PjActivityType.CONVOCATION -> Color(0xFF6366F1) // indigo
    PjActivityType.ARRESTATION -> Color(0xFFEF4444) // red
    PjActivityType.GAV -> Color(0xFFD97706) // amber
    PjActivityType.REQUISITION -> Color(0xFF0D9488) // teal
    PjActivityType.PERSONNE_RECHERCHEE -> Color(0xFFF97316) // orange
    PjActivityType.OBJET_SAISI -> Color(0xFF06B6D4) // cyan
    PjActivityType.OBJET_TROUVE -> Color(0xFF10B981) // emerald
    PjActivityType.PERQUISITION -> Color(0xFFEC4899) // pink
    PjActivityType.RENSEIGNEMENT -> Color(0xFF8B5CF6) // violet
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
