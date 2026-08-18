package com.gsoft.opus.presentation.personnel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.ui.components.OpusDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonnelDetailScreen(
    onEdit: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: PersonnelDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var attachmentTitle by remember { mutableStateOf("") }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val title = if (attachmentTitle.isNotBlank()) attachmentTitle else "Pièce jointe"
            val uploadFile = uriToUploadFile(context, uri)
            if (uploadFile != null) {
                viewModel.addAttachment(title, uploadFile)
                attachmentTitle = ""
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Détail du personnel", fontWeight = FontWeight.Bold) },
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
                            state.personnel?.id?.let { onEdit(it) }
                        }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Modifier")
                        }
                    }
                }
            )
        }
    ) { padding ->
        PersonnelProfileContent(
            state = state,
            onEdit = if (state.canEdit) onEdit else null,
            onAddAttachment = { filePickerLauncher.launch("*/*") },
            onDeleteAttachment = if (state.canDelete) viewModel::requestDeleteAttachment else null,
            contentPadding = padding
        )
    }

    // Delete attachment confirmation
    OpusDialog(
        visible = state.deleteAttachmentTarget != null,
        onDismiss = viewModel::cancelDeleteAttachment,
        title = "Supprimer la pièce jointe",
        message = "Voulez-vous vraiment supprimer \"${state.deleteAttachmentTarget?.title}\" ?",
        confirmText = "Supprimer",
        onConfirm = viewModel::confirmDeleteAttachment,
        cancelText = "Annuler",
        onCancel = viewModel::cancelDeleteAttachment
    )
}
