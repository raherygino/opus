package com.gsoft.opus.presentation.armement

import android.Manifest
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.presentation.personnel.OpusDatePickerDialog
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import com.gsoft.opus.presentation.personnel.millisToIsoDate
import com.gsoft.opus.presentation.personnel.uriToUploadFile
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton
import com.gsoft.opus.ui.components.OpusDropdown

private fun personnelLabel(p: Personnel): String =
    "${p.lastname} ${p.firstname} (${p.im}) — ${p.grade}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmementFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: ArmementFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

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
                        if (state.isEdit) "Modifier la perception" else "Nouvelle perception",
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
            // ─── Perception section ────────────────────────────────────
            FormSectionCard(
                title = "Perception",
                icon = Icons.Outlined.Shield,
                subtitle = "Date, heure et arme perçue"
            ) {
                OutlinedTextField(
                    value = if (state.datePerception.isBlank()) "" else formatDateDisplay(state.datePerception),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date de la perception *") },
                    placeholder = { Text("JJ/MM/AAAA") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choisir la date")
                        }
                    },
                    singleLine = true,
                    isError = state.datePerception.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.heure,
                    onValueChange = viewModel::updateHeure,
                    label = { Text("Heure de la perception *") },
                    placeholder = { Text("HH:MM") },
                    singleLine = true,
                    isError = state.heure.isBlank() && state.errorMessage != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.typeArme,
                    onValueChange = viewModel::updateTypeArme,
                    label = { Text("Type d'arme *") },
                    placeholder = { Text("Ex : Pistolet PA 9mm") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.matriculeArme,
                    onValueChange = viewModel::updateMatriculeArme,
                    label = { Text("Matricule de l'arme *") },
                    placeholder = { Text("Ex : PA-0001") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.munitions,
                    onValueChange = { viewModel.updateMunitions(it.filter(Char::isDigit)) },
                    label = { Text("Munitions") },
                    placeholder = { Text("Nombre de munitions perçues") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.secteurMission,
                    onValueChange = viewModel::updateSecteurMission,
                    label = { Text("Secteur / Mission") },
                    placeholder = { Text("Ex : Patrouille Centre-ville") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.etatPerception,
                    onValueChange = viewModel::updateEtatPerception,
                    label = { Text("État à la perception") },
                    placeholder = { Text("État de l'arme lors de la perception...") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── Agent preneur (personnel picker + verification + signature) ─
            FormSectionCard(
                title = "Agent preneur",
                icon = Icons.Outlined.Person,
                subtitle = "Agent qui perçoit l'arme"
            ) {
                val options = state.personnelOptions
                val selectedLabel = options
                    .firstOrNull { it.id == state.agentPreneurPersonnelId }
                    ?.let { personnelLabel(it) }
                    ?: ""
                OpusDropdown(
                    label = "Agent *",
                    options = options.map { personnelLabel(it) },
                    selected = selectedLabel,
                    onSelect = { label ->
                        options.firstOrNull { personnelLabel(it) == label }
                            ?.let { viewModel.updateAgentPreneur(it.id) }
                    },
                    placeholder = "Sélectionner un agent",
                    isError = state.agentPreneurPersonnelId == 0 && state.errorMessage != null
                )
                Text(
                    text = "L'identité de l'agent (IM, grade, nom) est enregistrée au moment de la perception.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Verification status (edit mode) — read-only.
                if (state.isEdit) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.verified) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "  Identité vérifiée au moment de la perception",
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
                }

                // Verification step (create mode only).
                if (!state.isEdit) {
                    var showCode by remember { mutableStateOf(false) }
                    Text(
                        text = "Vérification de l'identité",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = "L'agent doit fournir son code secret pour confirmer son identité avant la remise de l'arme.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.codeSecret,
                            onValueChange = viewModel::updateCodeSecret,
                            label = { Text("Code secret de l'agent") },
                            singleLine = true,
                            enabled = !state.verified && state.agentPreneurPersonnelId > 0,
                            leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showCode = !showCode }) {
                                    Icon(
                                        if (showCode) Icons.Outlined.Close else Icons.Outlined.RemoveRedEye,
                                        contentDescription = if (showCode) "Masquer" else "Afficher"
                                    )
                                }
                            },
                            visualTransformation = if (showCode) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        if (state.verified) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = " Vérifié",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            GradientButton(
                                text = "Vérifier",
                                onClick = { viewModel.verifyCode() },
                                isLoading = state.verifying,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    if (state.verifyError != null) {
                        Text(
                            text = state.verifyError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Signature capture (after verification).
                    // After verification, the signature is pulled from the
                    // personnel's existing data. If they have none, the user
                    // can capture one. The user can always choose to draw a
                    // new one to override.
                    if (state.verified) {
                        var showSignatureDialog by remember { mutableStateOf(false) }
                        Text(
                            text = "Signature de l'agent",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            text = when {
                                state.signatureFromPersonnel -> "Signature récupérée depuis les données du personnel. Vous pouvez la garder ou en dessiner une nouvelle."
                                state.signatureSvg != null -> "Signature dessinée pour cette perception."
                                else -> "Aucune signature dans les données du personnel. Vous pouvez en capturer une ou laisser vide."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.signatureSvg != null) {
                            // Show the SVG signature preview.
                            SignatureSvgPreview(svg = state.signatureSvg!!)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(onClick = { showSignatureDialog = true }) {
                                    Icon(Icons.Outlined.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("  Refaire")
                                }
                                IconButton(onClick = { viewModel.setSignatureSvg(null) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showSignatureDialog = true },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Icon(Icons.Outlined.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("  Capturer la signature")
                            }
                        }

                        if (showSignatureDialog) {
                            SignatureCaptureDialog(
                                onConfirm = { svg ->
                                    viewModel.setSignatureSvg(svg)
                                    showSignatureDialog = false
                                },
                                onDismiss = { showSignatureDialog = false }
                            )
                        }
                    }
                }
            }

            // ─── Pièces jointes ────────────────────────────────────────
            FormSectionCard(
                title = "Pièces jointes",
                icon = Icons.Outlined.AttachFile,
                subtitle = "Documents associés à la perception"
            ) {
                state.attachments.filter { !it.isDeleted }.forEachIndexed { _, item ->
                    val realIndex = state.attachments.indexOf(item)
                    AttachmentFieldCard(
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
                AddAttachmentButton(onClick = { viewModel.addAttachment() })
            }

            // ─── Localisation GPS ─────────────────────────────────────
            // On Android, the agent must enable location services and
            // capture the device's GPS coordinates before creating an
            // armement perception. This is required — the save is blocked
            // until a location is captured (on create).
            if (!state.isEdit) {
                FormSectionCard(
                    title = "Localisation GPS",
                    icon = Icons.Outlined.LocationOn,
                    subtitle = "Position requise pour enregistrer la perception"
                ) {
                    val locationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val granted = permissions.values.any { it }
                        if (granted) {
                            viewModel.captureLocation()
                        }
                    }

                    if (state.isCapturingLocation) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Capture de la position...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (state.latitude != null && state.longitude != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Position capturée",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Lat: ${"%.6f".format(state.latitude)}, Lon: ${"%.6f".format(state.longitude)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = { viewModel.captureLocation() }) {
                                Text("Recapturer")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (viewModel.hasLocationPermission()) {
                                    viewModel.captureLocation()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Capturer ma position")
                        }
                    }

                    if (state.locationError != null) {
                        Text(
                            text = state.locationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // ─── Error + submit ───────────────────────────────────────
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

    if (showDatePicker) {
        OpusDatePickerDialog(
            visible = true,
            title = "Date de la perception",
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                showDatePicker = false
                viewModel.updateDatePerception(millisToIsoDate(millis))
            }
        )
    }
}

@Composable
private fun AttachmentFieldCard(
    title: String,
    fileName: String?,
    onTitleChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Titre") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onPickFile() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = fileName ?: "Aucun fichier — touchez pour choisir",
                style = MaterialTheme.typography.bodySmall,
                color = if (fileName.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AddAttachmentButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Ajouter une pièce jointe",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Render an SVG signature string using a WebView preview. Used in the
 * form to show the signature pulled from personnel data or drawn by
 * the user.
 */
@Composable
private fun SignatureSvgPreview(svg: String) {
    val context = LocalContext.current
    val webView = remember(svg) {
        android.webkit.WebView(context).apply {
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            setBackgroundColor(android.graphics.Color.WHITE)
            loadDataWithBaseURL(null, svg, "image/svg+xml", "UTF-8", null)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(androidx.compose.ui.graphics.Color.White)
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )
    }
}
