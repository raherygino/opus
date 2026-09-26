package com.gsoft.opus.presentation.registreenquete

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
import com.gsoft.opus.domain.model.RegistreEnqueteAttachment
import com.gsoft.opus.presentation.personnel.DetailRow
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.presentation.personnel.formatFileSize
import com.gsoft.opus.presentation.personnel.openUrl
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.ImageViewerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistreEnqueteDetailScreen(
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    viewModel: RegistreEnqueteDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var viewerTarget by remember { mutableStateOf<RegistreEnqueteAttachment?>(null) }

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
                title = { Text("Registre d'enquête", fontWeight = FontWeight.Bold) },
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

        val enqueteurName = listOfNotNull(entry.enqueteurPrenoms, entry.enqueteurNom)
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
        val enqueteurLabel = when {
            enqueteurName == null -> null
            !entry.enqueteurGrade.isNullOrBlank() -> "$enqueteurName (${entry.enqueteurGrade})"
            else -> enqueteurName
        }
        val opjName = listOfNotNull(entry.opjPrenoms, entry.opjNom)
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
        val opjLabel = when {
            opjName == null -> null
            !entry.opjGrade.isNullOrBlank() -> "$opjName (${entry.opjGrade})"
            else -> opjName
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
                DetailRow("Date d'ouverture", formatDateDisplay(entry.dateOuverture))
                if (!entry.numeroDossier.isNullOrBlank()) {
                    DetailRow("N° du dossier rattaché", entry.numeroDossier)
                }
                DetailRow("Statut", REGISTRE_ENQUETE_STATUT_LABELS[entry.statut] ?: entry.statut)
            }

            FormSectionCard(
                title = "Infraction",
                icon = Icons.Outlined.Description,
                subtitle = "Nature des faits poursuivis"
            ) {
                DetailRow("Nature de l'infraction", entry.natureInfraction)
                if (!entry.dateLieuFaits.isNullOrBlank()) {
                    DetailRow("Date et lieu des faits", entry.dateLieuFaits)
                }
            }

            FormSectionCard(
                title = "Personnes",
                icon = Icons.Outlined.Person,
                subtitle = "Enquêteur, OPJ, parties"
            ) {
                DetailRow("Enquêteur", enqueteurLabel ?: "—")
                if (opjLabel != null) {
                    DetailRow("OPJ", opjLabel)
                }
                if (!entry.plaignant.isNullOrBlank()) {
                    DetailRow("Plaignant / Partie civile", entry.plaignant)
                }
                if (!entry.miseEnCause.isNullOrBlank()) {
                    DetailRow("Mise en cause", entry.miseEnCause)
                }
            }

            if (!entry.observations.isNullOrBlank()) {
                FormSectionCard(
                    title = "Observations",
                    icon = Icons.Outlined.Description,
                    subtitle = "Renseignements complémentaires"
                ) {
                    DetailRow("Observations", entry.observations ?: "—")
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
                                if (isEnqueteImageAttachment(att.mimeType, att.originalFilename)) {
                                    viewerTarget = att
                                } else {
                                    openUrl(context, enqueteAttachmentDownloadUrl(entry.id, att.id))
                                }
                            },
                            onDownload = {
                                openUrl(context, enqueteAttachmentDownloadUrl(entry.id, att.id))
                            }
                        )
                    }
                }
            }
        }
    }

    ImageViewerDialog(
        imageUrl = viewerTarget?.let { enqueteAttachmentDownloadUrl(state.entry?.id ?: 0, it.id) } ?: "",
        title = viewerTarget?.title,
        onDismiss = { viewerTarget = null }
    )
}

@Composable
private fun AttachmentRow(
    attachment: RegistreEnqueteAttachment,
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
        if (isEnqueteImageAttachment(attachment.mimeType, attachment.originalFilename)) {
            IconButton(onClick = onPreview) {
                Icon(Icons.Outlined.Visibility, contentDescription = "Aperçu")
            }
        }
        IconButton(onClick = onDownload) {
            Icon(Icons.Outlined.Download, contentDescription = "Télécharger")
        }
    }
}
