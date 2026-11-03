package com.gsoft.opus.presentation.users

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.ui.components.ErrorMessage
import com.gsoft.opus.ui.components.FormSectionCard
import com.gsoft.opus.ui.components.GradientButton
import com.gsoft.opus.ui.components.OpusDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: UserFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPassword by remember { mutableStateOf(false) }

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
                        if (state.isEdit) "Modifier l'utilisateur" else "Nouvel utilisateur",
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
                title = "Informations du compte",
                icon = Icons.Outlined.Shield,
                subtitle = if (state.isEdit) "Modifier les informations du compte"
                else "Créer un compte pour un agent"
            ) {
                if (!state.isEdit) {
                    val agents = state.availablePersonnel
                    val selectedAgent = agents
                        .firstOrNull { it.id == state.personnelId }
                        ?.let { "${it.lastname} ${it.firstname} (${it.im}) — ${it.grade}" }
                        ?: ""
                    OpusDropdown(
                        label = "Agent *",
                        options = agents.map { "${it.lastname} ${it.firstname} (${it.im}) — ${it.grade}" },
                        selected = selectedAgent,
                        onSelect = { label ->
                            agents.firstOrNull { "${it.lastname} ${it.firstname} (${it.im}) — ${it.grade}" == label }
                                ?.let { viewModel.updatePersonnelId(it.id) }
                        },
                        placeholder = "Sélectionner un agent",
                        isError = state.personnelId == 0 && state.errorMessage != null
                    )
                    if (agents.isEmpty()) {
                        Text(
                            text = "Tous les agents ont déjà un compte utilisateur.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = state.username,
                    onValueChange = viewModel::updateUsername,
                    label = { Text("Nom d'utilisateur *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    label = {
                        Text(
                            if (state.isEdit) "Mot de passe (laisser vide pour conserver l'actuel)"
                            else "Mot de passe *"
                        )
                    },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Outlined.VisibilityOff
                                else Icons.Outlined.Visibility,
                                contentDescription = if (showPassword) "Masquer" else "Afficher",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val roles = state.roles
                val selectedRole = roles.firstOrNull { it.id == state.roleId }?.name ?: ""
                OpusDropdown(
                    label = "Rôle *",
                    options = roles.map { it.name },
                    selected = selectedRole,
                    onSelect = { label ->
                        roles.firstOrNull { it.name == label }
                            ?.let { viewModel.updateRoleId(it.id) }
                    },
                    placeholder = "Sélectionner un rôle",
                    isError = state.roleId == 0 && state.errorMessage != null
                )

                if (state.isEdit) {
                    OpusDropdown(
                        label = "Statut",
                        options = listOf("Actif", "Inactif"),
                        selected = if (state.isActive) "Actif" else "Inactif",
                        onSelect = { label -> viewModel.updateIsActive(label == "Actif") }
                    )
                }
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
