package com.gsoft.opus.presentation.registredeferrement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Person
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
import com.gsoft.opus.domain.model.RegistreDeferrementAttachment
import com.gsoft.opus.presentation.gardeavue.formatDateTimeDisplay
import com.gsoft.opus.presentation.personnel.DetailRow
import com.gsoft.opus.presentation.personnel.formatFileSize
import com.gsoft.opus.presentation.personnel.openUrl
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.ImageViewerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistreDeferrementDetailScreen(
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    viewModel: RegistreDeferrementDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var viewerTarget by remember { mutableStateOf<RegistreDeferrementAttachment?>(null) }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    LaunchedEffect(state.notFound) {
        if (state.notFound) onBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Registre de déferrement", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (state.canEdit) {
                        state.entry?.let { entry ->
                            IconButton(onClick = { onEdit(entry.id) }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Modifier")
                            }
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
            ErrorMessage(message = state.errorMessage ?: "Entrée introuvable")
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
            FormSectionCard(
                title = "Enregistrement",
                icon = Icons.Outlined.FilePresent,
                subtitle = "Informations générales"
            ) {
                DetailRow("Numéro", entry.numero)
                DetailRow("Date et heure du déferrement", formatDateTimeDisplay(entry.dateHeureDeferrement))
            }

            FormSectionCard(
                title = "Personne déférée",
                icon = Icons.Outlined.Person,
                subtitle = "Identité et infraction reprochée"
            ) {
                DetailRow("Nom et prénom", entry.personneNom)
                if (!entry.dateLieuNaissance.isNullOrBlank()) {
                    DetailRow("Date et lieu de naissance", entry.dateLieuNaissance)
                }
                if (!entry.infraction.isNullOrBlank()) {
                    DetailRow("Infraction reprochée", entry.infraction)
                }
            }

            FormSectionCard(
                title = "Autorité & escorte",
                icon = Icons.Outlined.Gavel,
                subtitle = "Magistrat saisi et éléments d'escorte"
            ) {
                if (!entry.autorite.isNullOrBlank()) {
                    DetailRow("Autorité judiciaire", entry.autorite)
                }
                if (!entry.destination.isNullOrBlank()) {
                    DetailRow("Destination", entry.destination)
                }
                if (!entry.escorte.isNullOrBlank()) {
                    DetailRow("Éléments d'escorte", entry.escorte)
                }
            }

            FormSectionCard(
                title = "Suite & dossier rattaché",
                icon = Icons.Outlined.Description,
                subtitle = "Décision et références"
            ) {
                if (!entry.suiteDonnee.isNullOrBlank()) {
                    DetailRow("Suite donnée", entry.suiteDonnee)
                }
                if (!entry.numeroDossier.isNullOrBlank()) {
                    DetailRow("N° du dossier rattaché", entry.numeroDossier)
                }
                if (!entry.observations.isNullOrBlank()) {
                    DetailRow("Observations", entry.observations)
                }
            }

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
                                if (isDeferrementImageAttachment(att.mimeType, att.originalFilename)) {
                                    viewerTarget = att
                                } else {
                                    openUrl(context, deferrementAttachmentDownloadUrl(entry.id, att.id))
                                }
                            },
                            onDownload = {
                                openUrl(context, deferrementAttachmentDownloadUrl(entry.id, att.id))
                            }
                        )
                    }
                }
            }
        }
    }

    ImageViewerDialog(
        imageUrl = viewerTarget?.let { deferrementAttachmentDownloadUrl(state.entry?.id ?: 0, it.id) } ?: "",
        title = viewerTarget?.title,
        onDismiss = { viewerTarget = null }
    )
}

@Composable
private fun AttachmentRow(
    attachment: RegistreDeferrementAttachment,
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
        if (isDeferrementImageAttachment(attachment.mimeType, attachment.originalFilename)) {
            IconButton(onClick = onPreview) {
                Icon(Icons.Outlined.Visibility, contentDescription = "Aperçu")
            }
        }
        IconButton(onClick = onDownload) {
            Icon(Icons.Outlined.Download, contentDescription = "Télécharger")
        }
    }
}
