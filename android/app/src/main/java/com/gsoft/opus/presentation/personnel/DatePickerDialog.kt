package com.gsoft.opus.presentation.personnel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A polished, fully opaque date picker dialog.
 *
 * The default Material 3 [androidx.compose.material3.DatePickerDialog] renders
 * its container transparently, which made the underlying content bleed through
 * (the "transparency issue"). Here we wrap the [DatePicker] in an explicit
 * [Surface] with the theme's opaque `surface` color, rounded corners and
 * elevation, so the dialog is always readable in both light and dark themes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpusDatePickerDialog(
    visible: Boolean,
    initialMillis: Long? = null,
    title: String = "Sélectionner une date",
    onDismiss: () -> Unit,
    onConfirm: (millis: Long) -> Unit
) {
    if (!visible) return
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Title bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Date picker body — explicit container color guarantees opacity
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DatePicker(
                        state = state,
                        colors = DatePickerDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            headlineContentColor = MaterialTheme.colorScheme.onSurface,
                            weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            subheadContentColor = MaterialTheme.colorScheme.onSurface,
                            yearContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            currentYearContentColor = MaterialTheme.colorScheme.primary,
                            selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                            selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                            dayContentColor = MaterialTheme.colorScheme.onSurface,
                            disabledDayContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                            selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                            todayContentColor = MaterialTheme.colorScheme.primary,
                            todayDateBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = { state.selectedDateMillis?.let { onConfirm(it) } },
                        enabled = state.selectedDateMillis != null
                    ) {
                        Text(
                            "OK",
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.selectedDateMillis != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }
}
