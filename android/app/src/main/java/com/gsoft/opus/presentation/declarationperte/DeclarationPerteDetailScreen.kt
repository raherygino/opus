package com.gsoft.opus.presentation.declarationperte

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
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkOutline
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
import com.gsoft.opus.domain.model.DeclarationPerteAttachment
import com.gsoft.opus.presentation.personnel.DetailRow
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.presentation.personnel.formatFileSize
import com.gsoft.opus.presentation.personnel.openUrl
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.ImageViewerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeclarationPerteDetailScreen(
    onEdit: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: DeclarationPerteDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var viewerTarget by remember { mutableStateOf<DeclarationPerteAttachment?>(null) }

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
                title = { Text("Détail de la déclaration de perte", fontWeight = FontWeight.Bold) },
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
                            state.declaration?.id?.let { onEdit(it) }
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

        val declaration = state.declaration
        if (declaration == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                ErrorMessage(message = state.errorMessage ?: "Déclaration de perte introuvable")
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
            // ─── Déclaration section ──────────────────────────────────
            FormSectionCard(
                title = "Déclaration",
                icon = Icons.Outlined.Description,
                subtitle = "Date, heure et identité du déclarant"
            ) {
                DetailRow("Date de déclaration", formatDateDisplay(declaration.dateDeclaration))
                DetailRow("Heure de déclaration", declaration.heureDisplay)
                DetailRow("Identité du déclarant", declaration.identiteDeclarant)
                DetailRow("Nom de l'agent", declaration.nomAgent)
            }

            // ─── Objet perdu section ──────────────────────────────────
            FormSectionCard(
                title = "Objet perdu",
                icon = Icons.Outlined.WorkOutline,
                subtitle = "Nature et description de l'objet"
            ) {
                DetailRow("Nature de l'objet", declaration.natureObjet)
                DetailRow("Description", declaration.descriptionObjet)
            }

            // ─── Perte présumée section ───────────────────────────────
            FormSectionCard(
                title = "Perte présumée",
                icon = Icons.Outlined.Place,
                subtitle = "Date et lieu présumés de la perte"
            ) {
                DetailRow("Date de perte", formatDateDisplay(declaration.datePerte))
                DetailRow("Lieu de perte", declaration.lieuPerte)
            }

            // ─── Attestation section ──────────────────────────────────
            FormSectionCard(
                title = "Attestation",
                icon = Icons.Outlined.FileCopy,
                subtitle = "Attestation de perte délivrée"
            ) {
                DetailRow("Numéro d'attestation", declaration.numeroAttestation)
                if (declaration.agentDisplayName.isNotBlank()) {
                    DetailRow("Agent secrétariat", declaration.agentDisplayName)
                }
            }

            // ─── Pièces jointes section ─────────────────────────────────
            FormSectionCard(
                title = "Pièces jointes",
                icon = Icons.Outlined.AttachFile,
                subtitle = "Documents associés à la déclaration de perte"
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
                            if (isImageDeclarationPerteAttachment(att.mimeType, att.originalFilename)) {
                                IconButton(onClick = { viewerTarget = att }) {
                                    Icon(Icons.Outlined.Visibility, contentDescription = "Aperçu", modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(onClick = {
                                val url = declarationPerteAttachmentDownloadUrl(declaration.id, att.id)
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

    // Full-screen zoomable viewer for image attachments
    viewerTarget?.let { att ->
        state.declaration?.let { decl ->
            ImageViewerDialog(
                imageUrl = declarationPerteAttachmentDownloadUrl(decl.id, att.id),
                title = att.title,
                onDismiss = { viewerTarget = null }
            )
        }
    }
}
