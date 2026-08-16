package com.gsoft.opus.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DividerHeight = 2.dp

@Composable
fun MainScaffold(
    onMenuClick: () -> Unit = {},
    showHeader: Boolean = true,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    // Set status bar color to match the app bar surface when header is shown
    val view = LocalView.current
    if (!view.isInEditMode && showHeader) {
        val surfaceColor = MaterialTheme.colorScheme.surface
        val isLight = surfaceColor.luminance() > 0.5f
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = surfaceColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            if (showHeader) {
                OpusTopAppBar(onMenuClick = onMenuClick, subtitle = subtitle)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DividerHeight)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50),
                                    Color.White,
                                    Color(0xFFF44336)
                                )
                            )
                        )
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
