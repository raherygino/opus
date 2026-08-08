package com.gsoft.opus.presentation.personnel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.GradientButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MouvementFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: MouvementFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var dateTarget by remember { mutableStateOf("depart") }
    var attachmentTitle by remember { mutableStateOf("") }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
            onSaved()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            if (!state.saved) snackbarHostState.showSnackbar(it)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val title = if (attachmentTitle.isNotBlank()) attachmentTitle else "Pièce jointe"
            val uploadFile = uriToUploadFile(context, uri)
            if (uploadFile != null) {
                viewModel.addPendingFile(title, uploadFile)
                attachmentTitle = ""
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Nouveau mouvement", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Personnel search with autocomplete
            OutlinedTextField(
                value = state.searchName,
                onValueChange = viewModel::onSearchNameChange,
                label = { Text("Rechercher un personnel") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.autocomplete.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    state.autocomplete.take(5).forEach { person ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectPersonnel(person) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${person.firstname} ${person.lastname} (${person.im})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (state.personnelId != 0) {
                Text(
                    text = "Sélectionné: ${state.prenoms} ${state.nom}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Type dropdown
            var typeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.typeMouvement,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type de mouvement") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    MOUVEMENT_TYPES.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                viewModel.onTypeChange(type)
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            // Date depart
            OutlinedTextField(
                value = state.dateDepart,
                onValueChange = {},
                readOnly = true,
                label = { Text("Date de départ") },
                trailingIcon = {
                    IconButton(onClick = {
                        dateTarget = "depart"
                        showDatePicker = true
                    }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Days
            OutlinedTextField(
                value = state.days,
                onValueChange = viewModel::onDaysChange,
                label = { Text("Nombre de jours") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Date retour
            OutlinedTextField(
                value = state.dateRetour,
                onValueChange = {},
                readOnly = true,
                label = { Text("Date de retour") },
                trailingIcon = {
                    IconButton(onClick = {
                        dateTarget = "retour"
                        showDatePicker = true
                    }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Attachments
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pièces jointes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            state.pendingFiles.forEachIndexed { index, pf ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${pf.title}: ${pf.fileName}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.removePendingFile(index) }) {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = attachmentTitle,
                    onValueChange = { attachmentTitle = it },
                    label = { Text("Titre (optionnel)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = "Ajouter fichier")
                }
            }

            if (state.errorMessage != null && !state.saved) {
                ErrorMessage(message = state.errorMessage!!)
            }

            Spacer(modifier = Modifier.height(8.dp))

            GradientButton(
                text = "Enregistrer",
                onClick = { viewModel.save() },
                isLoading = state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        OpusDatePickerDialog(
            visible = true,
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                showDatePicker = false
                val dateStr = millisToIsoDate(millis)
                if (dateTarget == "depart") viewModel.onDateDepartChange(dateStr)
                else viewModel.onDateRetourChange(dateStr)
            }
        )
    }
}
