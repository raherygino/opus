package com.gsoft.opus.presentation.objet

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.presentation.personnel.uriToUploadFile
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton
import com.gsoft.opus.ui.components.OpusDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjetSaisiFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: ObjetSaisiFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.saved) {
        if (state.saved) {
            onSaved()
        }
    }

    var attachmentFilePickerIndex by remember { mutableStateOf(-1) }
    val attachmentFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && attachmentFilePickerIndex >= 0) {
            val uploadFile = uriToUploadFile(context, uri)
            if (uploadFile != null) {
                viewModel.setAttachmentFile(attachmentFilePickerIndex, uploadFile)
            }
        }
        attachmentFilePickerIndex = -1
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEdit) "Modifier l'objet saisi" else "Nouvel objet saisi",
                        fontWeight = FontWeight.Bold
                    )
                },
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FormSectionCard(
                title = "Objet saisi",
                icon = Icons.Outlined.Inventory2,
                subtitle = "Informations sur l'objet saisi"
            ) {
                OutlinedTextField(
                    value = state.numeroDossier,
                    onValueChange = viewModel::updateNumeroDossier,
                    label = { Text("N° du dossier concerné") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OpusDropdown(
                    label = "Type d'objet *",
                    options = OBJET_SAISI_TYPE_LABELS.values.toList(),
                    selected = OBJET_SAISI_TYPE_LABELS[state.typeObjet] ?: state.typeObjet,
                    onSelect = { label ->
                        OBJET_SAISI_TYPE_LABELS.entries.firstOrNull { it.value == label }
                            ?.let { viewModel.updateTypeObjet(it.key) }
                    },
                    isError = state.typeObjet.isBlank() && state.errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.proprietaire,
                    onValueChange = viewModel::updateProprietaire,
                    label = { Text("Nom et prénom du propriétaire") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.motif,
                    onValueChange = viewModel::updateMotif,
                    label = { Text("Motif *") },
                    isError = state.motif.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FormSectionCard(
                title = "Pièces jointes",
                icon = Icons.Outlined.AttachFile,
                subtitle = "Documents associés"
            ) {
                state.attachments.filter { !it.isDeleted }.forEach { item ->
                    val realIndex = state.attachments.indexOf(item)
                    ObjetAttachmentFieldCard(
                        title = item.title,
                        fileName = item.uploadFile?.fileName ?: item.existingFilename,
                        onTitleChange = { viewModel.updateAttachmentTitle(realIndex, it) },
                        onPickFile = {
                            attachmentFilePickerIndex = realIndex
                            attachmentFilePickerLauncher.launch("*/*")
                        },
                        onRemove = { viewModel.removeAttachment(realIndex) }
                    )
                }

                ObjetAddAttachmentButton(onClick = { viewModel.addAttachment() })
            }

            if (state.errorMessage != null) {
                ErrorMessage(message = state.errorMessage!!)
            }

            Spacer(modifier = Modifier.height(4.dp))

            GradientButton(
                text = if (state.isEdit) "Mettre à jour" else "Enregistrer",
                onClick = { viewModel.save() },
                isLoading = state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
