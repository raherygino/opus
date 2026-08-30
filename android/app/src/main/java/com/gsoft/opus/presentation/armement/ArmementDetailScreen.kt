package com.gsoft.opus.presentation.armement

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.domain.model.ArmementAttachment
import com.gsoft.opus.presentation.personnel.DetailRow
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.presentation.personnel.formatFileSize
import com.gsoft.opus.presentation.personnel.openUrl
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.ImageViewerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmementDetailScreen(
    onEdit: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: ArmementDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var viewerTarget by remember { mutableStateOf<ArmementAttachment?>(null) }

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
                title = { Text("Détail de l'armement", fontWeight = FontWeight.Bold) },
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
                            state.armement?.id?.let { onEdit(it) }
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

        val armement = state.armement
        if (armement == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                ErrorMessage(message = state.errorMessage ?: "Armement introuvable")
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
            // ─── Statut + réintégration action ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (armement.isReintegree) "Arme réintégrée" else "En cours de perception",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (armement.isReintegree) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // ─── Perception section ────────────────────────────────────
            FormSectionCard(
                title = "Perception",
                icon = Icons.Outlined.Shield,
                subtitle = "Date, heure et arme perçue"
            ) {
                DetailRow("Date de la perception", formatDateDisplay(armement.datePerception))
                DetailRow("Heure de la perception", armement.heurePerceptionDisplay)
                DetailRow("Type d'arme", armement.typeArme.ifBlank { "—" })
                DetailRow("Matricule de l'arme", armement.matriculeArme.ifBlank { "—" })
                DetailRow("Munitions", armement.munitions?.toString() ?: "—")
                DetailRow("Secteur / Mission", armement.secteurMission ?: "—")
                DetailRow("État à la perception", armement.etatPerception ?: "—")
            }

            // ─── Agent preneur ─────────────────────────────────────────
            FormSectionCard(
                title = "Agent preneur",
                icon = Icons.Outlined.Person,
                subtitle = "Agent qui a perçu l'arme"
            ) {
                DetailRow("IM", armement.agentPreneurIm ?: "—")
                DetailRow("Grade", armement.agentPreneurGrade ?: "—")
                DetailRow("Nom complet", armement.agentPreneurNom ?: "—")
            }

            // ─── Réintégration ─────────────────────────────────────────
            if (armement.isReintegree) {
                FormSectionCard(
                    title = "Réintégration",
                    icon = Icons.Outlined.TaskAlt,
                    subtitle = "Retour de l'arme"
                ) {
                    DetailRow("Heure de la réintégration", armement.heureReintegrationDisplay ?: "—")
                    DetailRow("État à la réintégration", armement.etatReintegration ?: "—")
                    DetailRow(
                        "Munitions consommées",
                        armement.munitionsConsommees?.toString()?.let { consommees ->
                            armement.munitionsRestantes?.let { "$consommees (restantes : $it)" } ?: consommees
                        } ?: "—"
                    )
                }
            } else if (state.canEdit) {
                Button(
                    onClick = viewModel::requestReintegration,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Réintégration")
                }
            }

            // ─── Pièces jointes ────────────────────────────────────────
            FormSectionCard(
                title = "Pièces jointes",
                icon = Icons.Outlined.AttachFile,
                subtitle = "Documents associés à la perception"
            ) {
                if (state.attachments.isEmpty()) {
                    Text(
                        text = "Aucune pièce jointe",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.attachments.forEach { att ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = att.title,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                val size = formatFileSize(att.fileSize)
                                if (size.isNotBlank()) {
                                    Text(
                                        text = size,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isImageArmementAttachment(att.mimeType, att.originalFilename)) {
                                IconButton(onClick = { viewerTarget = att }) {
                                    Icon(Icons.Outlined.Visibility, contentDescription = "Aperçu", modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(onClick = {
                                val url = armementAttachmentDownloadUrl(armement.id, att.id)
                                openUrl(context, url)
                            }) {
                                Icon(Icons.Outlined.Download, contentDescription = "Télécharger", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (state.showReintegrationDialog) {
        state.armement?.let { armement ->
            ArmementReintegrationDialog(
                armement = armement,
                isSubmitting = state.isReintegrating,
                errorMessage = state.reintegrationError,
                onConfirm = viewModel::confirmReintegration,
                onDismiss = viewModel::cancelReintegration
            )
        }
    }

    viewerTarget?.let { att ->
        state.armement?.let { arm ->
            ImageViewerDialog(
                imageUrl = armementAttachmentDownloadUrl(arm.id, att.id),
                title = att.title,
                onDismiss = { viewerTarget = null }
            )
        }
    }
}
