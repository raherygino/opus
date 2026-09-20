package com.gsoft.opus.presentation.rassemblement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.presentation.personnel.DetailRow
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RassemblementDetailScreen(
    onEdit: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: RassemblementDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Rassemblement journalier", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Rafraîchir")
                    }
                    if (state.canEdit) {
                        IconButton(onClick = {
                            state.entry?.id?.let { onEdit(it) }
                        }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Modifier")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val entry = state.entry
        if (entry == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                ErrorMessage(message = state.errorMessage ?: "Rassemblement introuvable")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ─── Informations générales + situation de prise d'arme ──
            FormSectionCard(
                title = "Informations générales",
                icon = Icons.Outlined.Groups,
                subtitle = "Rassemblement du ${formatDateDisplay(entry.dateRassemblement)} à ${entry.heureDisplay}"
            ) {
                DetailRow("Date", formatDateDisplay(entry.dateRassemblement))
                DetailRow("Heure", entry.heureDisplay)
                DetailRow("Brigade de service", entry.brigadeService)
                DetailRow("Officier de permanence", entry.officierPermanence ?: "—")
                DetailRow("Inspecteur de permanence", entry.inspecteurPermanence ?: "—")
                DetailRow("Chef de poste", entry.chefPoste ?: "—")

                // Situation de prise d'arme — part of the record itself.
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
                Text(
                    text = "Situation de prise d'arme",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DetailRow("Effectif théorique", entry.effectifTheorique.toString())
                DetailRow("Présent", entry.present.toString())
                DetailRow("Absent", entry.absent.toString())
                DetailRow("Motif d'absence", entry.motifAbsence ?: "—")

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
                DetailRow("Instructions de l'autorité", entry.instructionsAutorite ?: "—")
                if (entry.agentDisplayName.isNotBlank()) {
                    DetailRow("Agent", entry.agentDisplayName)
                }
            }

            // ─── Répartition par secteur (one unified table: Diurne / Nocturne) ──
            FormSectionCard(
                title = "Répartition par secteur",
                icon = Icons.Outlined.ViewColumn,
                subtitle = "Répartition Diurne et Nocturne"
            ) {
                // Both sections come from the same shared structure; only the type differs.
                REPARTITION_SECTIONS.forEachIndexed { index, (type, label, icon) ->
                    if (index > 0) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    }
                    RepartitionSection(
                        type = type,
                        label = label,
                        icon = icon,
                        rows = entry.repartitions.filter { it.type == type }.map { it.toRow() },
                        readOnly = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
