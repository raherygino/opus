package com.gsoft.opus.presentation.roles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.domain.repository.ModulePermissions
import com.gsoft.opus.presentation.roles.RoleModules.groupLabel
import com.gsoft.opus.presentation.roles.RoleModules.shortLabel
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: RoleFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEdit) "Modifier le rôle" else "Nouveau rôle",
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
                title = "Informations du rôle",
                icon = Icons.Outlined.Shield,
                subtitle = if (state.isEdit) "Code : ${state.code}" else "Créer un nouveau rôle système"
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("Nom du rôle *") },
                    placeholder = { Text("Ex : Manager RH") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("Description") },
                    placeholder = { Text("Description du rôle...") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FormSectionCard(
                title = "Permissions",
                icon = Icons.Outlined.Check,
                subtitle = "Accès par module et par action"
            ) {
                PermissionMatrix(
                    permissions = state.permissions,
                    onToggle = viewModel::togglePermission,
                    onSetAll = viewModel::setAllPermissions
                )
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

@Composable
private fun PermissionMatrix(
    permissions: Map<String, ModulePermissions>,
    onToggle: (String, PermissionAction) -> Unit,
    onSetAll: (String, Boolean) -> Unit
) {
    // Group modules by their "Division > Section" prefix, preserving order.
    val groups = RoleModules.MODULES.groupBy { it.groupLabel }
    groups.forEach { (groupLabel, modules) ->
        Text(
            text = groupLabel.ifBlank { "Modules globaux" },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        modules.forEach { module ->
            ModulePermissionRow(
                module = module,
                perms = permissions[module.key] ?: ModulePermissions.NONE,
                onToggle = { action -> onToggle(module.key, action) },
                onSetAll = { grant -> onSetAll(module.key, grant) }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModulePermissionRow(
    module: RoleModules.ModuleDef,
    perms: ModulePermissions,
    onToggle: (PermissionAction) -> Unit,
    onSetAll: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = module.shortLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onSetAll(!perms.allGranted) }) {
                Text(
                    text = if (perms.allGranted) "Aucun" else "Tous",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RoleModules.ACTIONS.forEach { (action, label) ->
                val granted = when (action) {
                    PermissionAction.VIEW -> perms.canView
                    PermissionAction.CREATE -> perms.canCreate
                    PermissionAction.EDIT -> perms.canEdit
                    PermissionAction.DELETE -> perms.canDelete
                    PermissionAction.EXPORT -> perms.canExport
                }
                FilterChip(
                    selected = granted,
                    onClick = { onToggle(action) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}
