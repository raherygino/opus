package com.gsoft.opus.presentation.armement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gsoft.opus.domain.model.Armement
import com.gsoft.opus.domain.repository.ReintegrationData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated réintégration form — only the three reintegration fields are
 * asked; all perception data is preserved server-side. Shown from both the
 * list and the detail screen while the weapon is still en cours.
 */
@Composable
fun ArmementReintegrationDialog(
    armement: Armement,
    isSubmitting: Boolean,
    errorMessage: String?,
    onConfirm: (ReintegrationData) -> Unit,
    onDismiss: () -> Unit
) {
    var heure by remember(armement.id) {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.US).format(Date()))
    }
    var etat by remember(armement.id) { mutableStateOf("") }
    var munitionsConsommees by remember(armement.id) { mutableStateOf("") }
    var localError by remember(armement.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Column {
                Text("Réintégration de l'arme", fontWeight = FontWeight.Bold)
                Text(
                    text = armement.armeDisplay +
                        if (armement.agentPreneurDisplay.isNotBlank()) " · ${armement.agentPreneurDisplay}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = heure,
                    onValueChange = { heure = it },
                    label = { Text("Heure de la réintégration *") },
                    placeholder = { Text("HH:MM") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = etat,
                    onValueChange = { etat = it },
                    label = { Text("État à la réintégration *") },
                    placeholder = { Text("État de l'arme au retour...") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = munitionsConsommees,
                    onValueChange = { munitionsConsommees = it.filter(Char::isDigit) },
                    label = {
                        Text(
                            "Munitions consommées *" +
                                (armement.munitions?.let { " (perçues : $it)" } ?: "")
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val error = localError ?: errorMessage
                if (error != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validationError = ArmementFormValidator.validateReintegration(
                        heureReintegration = heure,
                        etatReintegration = etat,
                        munitionsConsommees = munitionsConsommees,
                        munitionsPercues = armement.munitions
                    )
                    if (validationError != null) {
                        localError = validationError
                        return@Button
                    }
                    localError = null
                    onConfirm(
                        ReintegrationData(
                            heureReintegration = heure,
                            etatReintegration = etat,
                            munitionsConsommees = munitionsConsommees.trim().toInt()
                        )
                    )
                },
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Réintégrer")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Annuler")
            }
        }
    )
}
