package com.gsoft.opus.presentation.plainte

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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
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
import com.gsoft.opus.domain.model.PlainteSortieAttachment
import com.gsoft.opus.presentation.personnel.DetailRow
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.presentation.personnel.formatFileSize
import com.gsoft.opus.presentation.personnel.openUrl
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.ImageViewerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainteSortieDetailScreen(
    onEdit: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: PlainteSortieDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var viewerTarget by remember { mutableStateOf<PlainteSortieAttachment?>(null) }

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
                title = { Text("Détail de la sortie", fontWeight = FontWeight.Bold) },
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
                            state.sortie?.id?.let { onEdit(it) }
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

        val sortie = state.sortie
        if (sortie == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                ErrorMessage(message = state.errorMessage ?: "Sortie introuvable")
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
            // ─── ENTRÉE associée section ───────────────────────────────
            FormSectionCard(
                title = "Plainte ENTRÉE associée",
                icon = Icons.Outlined.Description,
                subtitle = "Plainte d'origine"
            ) {
                DetailRow("Type", sortie.entreeTypeLabel)
                DetailRow("Numéro du dossier", sortie.entreeNumeroDossier ?: "—")
                DetailRow("Date", formatDateDisplay(sortie.entreeDatePlainte))
                DetailRow("Infraction", sortie.entreeInfraction ?: "—")
                DetailRow("Mise en cause (MC)", sortie.entreeMiseEnCause ?: "—")
                if (!sortie.entreePartieCivile.isNullOrBlank()) {
                    DetailRow("Partie civile (PC)", sortie.entreePartieCivile)
                }
                DetailRow("OPJ", sortie.entreeOpjDisplayName)
            }

            // ─── Sortie section ───────────────────────────────────────
            FormSectionCard(
                title = "Sortie",
                icon = Icons.Outlined.Gavel,
                subtitle = "Détails de la sortie"
            ) {
                NatureBadge(nature = sortie.nature)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Nature", sortie.natureLabel)
                DetailRow("Date", formatDateDisplay(sortie.dateSortie))
                DetailRow("Numéro", sortie.numero)
                DetailRow("N° TTR", sortie.numeroTtr ?: "—")
                DetailRow("Nom du Substitut", sortie.nomSubstitut ?: "—")
                if (!sortie.dateDeferrement.isNullOrBlank()) {
                    DetailRow("Date du déferrement", formatDateDisplay(sortie.dateDeferrement))
                }
                if (!sortie.observation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Observation",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = sortie.observation,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (sortie.agentDisplayName.isNotBlank()) {
                    DetailRow("Agent", sortie.agentDisplayName)
                }
            }

            // ─── Pièces jointes section ─────────────────────────────────
            FormSectionCard(
                title = "Pièces jointes (${state.attachments.size})",
                icon = Icons.Outlined.AttachFile,
                subtitle = "Documents associés"
            ) {
                if (state.attachments.isEmpty()) {
                    Text(
                        text = "Aucun fichier joint",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.attachments.forEach { att ->
                        AttachmentRow(
                            attachment = att,
                            sortieId = sortie.id,
                            onPreview = {
                                if (isImageAttachment(att.mimeType, att.originalFilename)) {
                                    viewerTarget = att
                                } else {
                                    openUrl(context, plainteSortieAttachmentDownloadUrl(sortie.id, att.id))
                                }
                            },
                            onDownload = {
                                openUrl(context, plainteSortieAttachmentDownloadUrl(sortie.id, att.id))
                            }
                        )
                    }
                }
            }
        }
    }

    ImageViewerDialog(
        imageUrl = viewerTarget?.let { plainteSortieAttachmentDownloadUrl(state.sortie?.id ?: 0, it.id) } ?: "",
        title = viewerTarget?.title,
        onDismiss = { viewerTarget = null }
    )
}

@Composable
private fun NatureBadge(nature: String) {
    val (bg, fg) = when (nature) {
        "DAT" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "DEFERREMENT" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = PLAINTE_SORTIE_NATURE_LABELS[nature] ?: nature,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AttachmentRow(
    attachment: PlainteSortieAttachment,
    sortieId: Int,
    onPreview: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = attachment.originalFilename,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (attachment.fileSize != null) {
                Text(
                    text = formatFileSize(attachment.fileSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (isImageAttachment(attachment.mimeType, attachment.originalFilename)) {
            IconButton(onClick = onPreview) {
                Icon(Icons.Outlined.Visibility, contentDescription = "Aperçu")
            }
        }
        IconButton(onClick = onDownload) {
            Icon(Icons.Outlined.Download, contentDescription = "Télécharger")
        }
    }
}
