package com.gsoft.opus.presentation.materielroulant

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.SvgSignatureView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterielRoulantDetailScreen(
    onEdit: (Int) -> Unit,
    onReintegrate: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: MaterielRoulantDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détail matériel roulant") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val item = state.item
        if (item == null) {
            ErrorMessage(
                message = state.errorMessage ?: "Matériel roulant introuvable",
                modifier = Modifier.padding(16.dp)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.typeMateriel} — ${formatDateDisplay(item.datePerception)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatutChip(reintegre = item.isReintegre)
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.canEdit && !item.isReintegre) {
                    Button(
                        onClick = { onReintegrate(item.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Réintégration")
                    }
                }
                if (state.canEdit) {
                    OutlinedButton(
                        onClick = { onEdit(item.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Modifier")
                    }
                }
            }

            // Véhicule & Perception
            DetailCard(title = "Véhicule & Perception", icon = Icons.Outlined.DirectionsCar) {
                DetailRow("Type de véhicule", item.typeMateriel)
                DetailRow("Immatriculation", item.numeroImmatriculation)
                DetailRow("Description", item.descriptionVehicule)
                DetailRow("Date de la perception", formatDateDisplay(item.datePerception))
                DetailRow("Heure de la perception", item.heurePerceptionDisplay)
            }

            // Conducteur
            DetailCard(title = "Agent conducteur", icon = Icons.Outlined.Person) {
                DetailRow("IM", item.agentConducteurIm)
                DetailRow("Grade", item.agentConducteurGrade)
                DetailRow("Nom complet", item.agentConducteurNom)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.agentVerifie) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "  Identité vérifiée via code secret",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "Identité non vérifiée (enregistrée avant la fonctionnalité de vérification)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (item.signatureSvg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Signature du conducteur",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SvgSignatureView(
                        svg = item.signatureSvg!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(top = 4.dp)
                    )
                }
            }

            // Chef de bord (if any)
            if (item.chefDeBordPersonnelId != null) {
                DetailCard(title = "Chef de bord", icon = Icons.Outlined.Person) {
                    DetailRow("IM", item.chefDeBordIm)
                    DetailRow("Grade", item.chefDeBordGrade)
                    DetailRow("Nom complet", item.chefDeBordNom)
                }
            }

            // Compteurs de départ
            DetailCard(title = "Compteurs de départ", icon = Icons.Outlined.Speed) {
                DetailRow("Kilométrage de départ (km)", item.kilometrageDepart)
                DetailRow("Carburant de départ (%)", item.niveauCarburantDepart)
            }

            // Réintégration (if reintegrated)
            if (item.isReintegre) {
                DetailCard(title = "Réintégration", icon = Icons.Outlined.TaskAlt) {
                    DetailRow("Date de la réintégration", formatDateDisplay(item.dateReintegration ?: ""))
                    DetailRow("Heure de la réintégration", item.heureReintegrationDisplay)
                    DetailRow("Kilométrage de retour (km)", item.kilometrageRetour)
                    DetailRow("Carburant de retour (%)", item.niveauCarburantRetour)
                }

                DetailCard(title = "Observations techniques", icon = Icons.Outlined.Build) {
                    Text(
                        text = item.observationsTechniques?.ifBlank { null } ?: "Aucune observation technique",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                DetailCard(title = "Défaillances signalées", icon = Icons.Outlined.WarningAmber) {
                    if (item.hasDefaillances) {
                        Text(
                            text = item.defaillances ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "Aucune défaillance signalée",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    icon: ImageVector,
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
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (reintegre) "Réintégré" else "En service",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = foreground
        )
    }
}
