package com.gsoft.opus.presentation.materiel

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.SvgSignatureView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterielDetailScreen(
    onEdit: (Int) -> Unit,
    onReintegrate: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: MaterielDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détail de l'affectation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
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

        val aff = state.affectation
        if (aff == null) {
            ErrorMessage(
                message = state.errorMessage ?: "Affectation introuvable",
                modifier = Modifier.padding(16.dp)
            )
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Affectation du ${formatDateDisplay(aff.datePerception)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${aff.heurePerceptionDisplay} — ${aff.agentDisplay.ifBlank { "—" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatutChip(reintegre = aff.isReintegre)
            }

            // Agent card
            DetailCard(title = "Agent utilisateur", icon = Icons.Outlined.Person) {
                DetailRow("IM", aff.agentIm)
                DetailRow("Grade", aff.agentGrade)
                DetailRow("Nom complet", aff.agentNom)
            }

            // Verification card
            DetailCard(title = "Vérification de l'identité", icon = Icons.Outlined.CheckCircle) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (aff.agentVerifie) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "  Identité vérifiée",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Identité non vérifiée",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (aff.agentVerifieAt != null) {
                    DetailRow("Vérifiée le", aff.agentVerifieAt.replace('T', ' ').take(16))
                }
                // Signature preview (if any)
                if (!aff.signatureSvg.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Signature",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SvgSignatureView(
                        svg = aff.signatureSvg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(top = 4.dp)
                    )
                }
            }

            // Perception card
            DetailCard(title = "Perception", icon = Icons.Outlined.Inventory) {
                DetailRow("Date", formatDateDisplay(aff.datePerception))
                DetailRow("Heure", aff.heurePerceptionDisplay)
                DetailRow("Observations", aff.observations)
            }

            // Matériels card
            DetailCard(title = "Matériels (${aff.lignes.size})", icon = Icons.Outlined.Inventory) {
                if (aff.lignes.isEmpty()) {
                    Text("Aucun matériel", style = MaterialTheme.typography.bodyMedium)
                } else {
                    aff.lignes.forEach { ligne ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                DetailRow("Type de matériel", ligne.typeMaterielNom)
                                DetailRow("ID Matériel", ligne.numeroMateriel)
                                DetailRow("État à l'emport", ligne.etatEmport)
                                DetailRow("État à la réintégration", ligne.etatReintegration)
                            }
                        }
                    }
                }
            }

            // Réintégration card (if returned)
            if (aff.isReintegre) {
                DetailCard(title = "Réintégration", icon = Icons.Outlined.TaskAlt) {
                    DetailRow("Date", aff.dateReintegration?.let { formatDateDisplay(it) })
                    DetailRow("Heure", aff.heureReintegrationDisplay)
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.canEdit && !aff.isReintegre) {
                    Button(
                        onClick = { onReintegrate(aff.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.TaskAlt, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Réintégration")
                    }
                }
                if (state.canEdit) {
                    OutlinedButton(
                        onClick = { onEdit(aff.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Modifier")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value?.ifBlank { null } ?: "—",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StatutChip(reintegre: Boolean) {
    val bgColor = if (reintegre) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val foreground = if (reintegre) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (reintegre) "Réintégré" else "Assigné",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = foreground
        )
    }
}
