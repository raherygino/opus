package com.gsoft.opus.presentation.gardeavue

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
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
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
import com.gsoft.opus.domain.model.GardeAVueAttachment
import com.gsoft.opus.presentation.personnel.DetailRow
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.presentation.personnel.formatFileSize
import com.gsoft.opus.presentation.personnel.openUrl
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.ImageViewerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardeAVueDetailScreen(
    onEdit: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: GardeAVueDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var viewerTarget by remember { mutableStateOf<GardeAVueAttachment?>(null) }

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
                title = { Text("Détail de la garde à vue", fontWeight = FontWeight.Bold) },
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
                ErrorMessage(message = state.errorMessage ?: "Garde à vue introuvable")
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
            // ─── Identité section ───────────────────────────────────
            FormSectionCard(
                title = "Identité",
                icon = Icons.Outlined.Description,
                subtitle = "Personne en garde à vue"
            ) {
                DetailRow("Nom", entry.nom)
                if (!entry.prenoms.isNullOrBlank()) {
                    DetailRow("Prénoms", entry.prenoms)
                }
                if (!entry.dateNaissance.isNullOrBlank()) {
                    DetailRow("Date de naissance", formatDateDisplay(entry.dateNaissance))
                }
                if (!entry.adresse.isNullOrBlank()) {
                    DetailRow("Adresse", entry.adresse)
                }
            }

            // ─── Enquête section ────────────────────────────────────
            FormSectionCard(
                title = "Enquête",
                icon = Icons.Outlined.Description,
                subtitle = "Enquêteur et OPJ"
            ) {
                if (!entry.enqueteurPermance.isNullOrBlank()) {
                    DetailRow("Enquêteur de permanence", entry.enqueteurPermance)
                }
                if (!entry.opjGav.isNullOrBlank()) {
                    DetailRow("OPJ ayant décidé la garde à vue", entry.opjGav)
                }
                if (!entry.motif.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Motif",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = entry.motif,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // ─── Santé & droits section ─────────────────────────────
            FormSectionCard(
                title = "Santé & droits",
                icon = Icons.Outlined.Description,
                subtitle = "État de santé, droits notifiés, contact"
            ) {
                if (!entry.etatSante.isNullOrBlank()) {
                    DetailRow("État de santé", entry.etatSante)
                }
                if (!entry.droitsNotifies.isNullOrBlank()) {
                    DetailRow("Droits notifiés", entry.droitsNotifies)
                }
                if (!entry.personneContacter.isNullOrBlank()) {
                    DetailRow("Personne à contacter", entry.personneContacter)
                }
            }

            // ─── Dates section ──────────────────────────────────────
            FormSectionCard(
                title = "Dates de la garde à vue",
                icon = Icons.Outlined.Description,
                subtitle = "Début, fin et prolongation"
            ) {
                DetailRow("Début de GAV", formatDateTimeDisplay(entry.debutGav))
                DetailRow("Fin de GAV", formatDateTimeDisplay(entry.finGav))
                if (!entry.prolongationGav.isNullOrBlank()) {
                    DetailRow("Prolongation de GAV", formatDateTimeDisplay(entry.prolongationGav))
                }
            }

            // ─── Pièces jointes section ─────────────────────────────
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
                            onPreview = {
                                if (isGavImageAttachment(att.mimeType, att.originalFilename)) {
                                    viewerTarget = att
                                } else {
                                    openUrl(context, gardeAVueAttachmentDownloadUrl(entry.id, att.id))
                                }
                            },
                            onDownload = {
                                openUrl(context, gardeAVueAttachmentDownloadUrl(entry.id, att.id))
                            }
                        )
                    }
                }
            }
        }
    }

    ImageViewerDialog(
        imageUrl = viewerTarget?.let { gardeAVueAttachmentDownloadUrl(state.entry?.id ?: 0, it.id) } ?: "",
        title = viewerTarget?.title,
        onDismiss = { viewerTarget = null }
    )
}

@Composable
private fun AttachmentRow(
    attachment: GardeAVueAttachment,
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
        if (isGavImageAttachment(attachment.mimeType, attachment.originalFilename)) {
            IconButton(onClick = onPreview) {
                Icon(Icons.Outlined.Visibility, contentDescription = "Aperçu")
            }
        }
        IconButton(onClick = onDownload) {
            Icon(Icons.Outlined.Download, contentDescription = "Télécharger")
        }
    }
}
