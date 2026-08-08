package com.gsoft.opus.presentation.personnel

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpusDatePickerDialog(
    visible: Boolean,
    initialMillis: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (millis: Long) -> Unit
) {
    if (!visible) return
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onConfirm(it) }
                }
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    ) {
        DatePicker(state = state)
    }
}
