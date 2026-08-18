package com.gsoft.opus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A polished, fully opaque detail/dialog container built on top of [Dialog].
 *
 * The default Material 3 [androidx.compose.material3.AlertDialog] renders its
 * container transparently in some theme configurations, which lets underlying
 * content bleed through. This component wraps the content in an explicit
 * [Surface] with the theme's opaque `surface` color, rounded corners, tonal
 * and shadow elevation, a scrim backdrop, and a consistent header/divider/
 * actions layout — so the dialog is always readable in both light and dark
 * themes.
 *
 * @param visible      Whether the dialog is currently shown.
 * @param onDismiss    Called when the user dismisses the dialog (scrim tap,
 *                     back press, or an explicit close action).
 * @param title        The dialog heading.
 * @param subtitle     Optional secondary line under the title.
 * @param confirmText  The primary action button label (empty = hidden).
 * @param onConfirm    Called when the primary action is tapped.
 * @param dismissText  The secondary action button label (empty = hidden).
 * @param onDismissClick Called when the secondary action is tapped (defaults
 *                     to [onDismiss]).
 * @param isConfirmLoading When true, the confirm button shows a spinner.
 * @param confirmEnabled Whether the confirm button is enabled.
 * @param confirmColor Color of the confirm button label.
 * @param dismissColor Color of the dismiss button label.
 * @param content      The dialog body, placed in a scrollable column.
 */
@Composable
fun OpusDetailDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    subtitle: String? = null,
    confirmText: String = "",
    onConfirm: () -> Unit = {},
    dismissText: String = "Fermer",
    onDismissClick: () -> Unit = onDismiss,
    isConfirmLoading: Boolean = false,
    confirmEnabled: Boolean = true,
    confirmColor: Color = MaterialTheme.colorScheme.primary,
    dismissColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    if (!visible) return

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
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 22.dp, end = 24.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )

                // Scrollable body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    content()
                }

                // Actions
                if (confirmText.isNotEmpty() || dismissText.isNotEmpty()) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissText.isNotEmpty()) {
                            DialogActionButton(
                                text = dismissText,
                                color = dismissColor,
                                enabled = true,
                                onClick = onDismissClick
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (confirmText.isNotEmpty()) {
                            DialogActionButton(
                                text = confirmText,
                                color = confirmColor,
                                enabled = confirmEnabled && !isConfirmLoading,
                                isLoading = isConfirmLoading,
                                onClick = onConfirm
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogActionButton(
    text: String,
    color: Color,
    enabled: Boolean,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.38f
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .height(18.dp)
                    .width(18.dp),
                strokeWidth = 2.dp,
                color = color
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = color.copy(alpha = alpha)
            )
        }
    }
}
