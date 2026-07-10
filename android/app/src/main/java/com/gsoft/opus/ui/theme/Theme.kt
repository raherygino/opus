package com.gsoft.opus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.gsoft.opus.domain.model.ColorPalette
import com.gsoft.opus.domain.model.ThemeMode

fun gradientBackground(darkTheme: Boolean): Brush {
    return if (darkTheme) {
        Brush.verticalGradient(
            colors = listOf(GradientDarkStart, GradientDarkEnd)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(GradientStart, GradientEnd)
        )
    }
}

@Composable
fun OpusTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorPalette: ColorPalette = ColorPalette.INDIGO,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    val targetScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getColorScheme(colorPalette, darkTheme)
    }

    val animatedColorScheme = animateColorScheme(targetScheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val animationSpec = tween<Color>(durationMillis = 400)

    val primary by animateColorAsState(target.primary, animationSpec, label = "primary")
    val onPrimary by animateColorAsState(target.onPrimary, animationSpec, label = "onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, animationSpec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, animationSpec, label = "onPrimaryContainer")
    val secondary by animateColorAsState(target.secondary, animationSpec, label = "secondary")
    val onSecondary by animateColorAsState(target.onSecondary, animationSpec, label = "onSecondary")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, animationSpec, label = "secondaryContainer")
    val onSecondaryContainer by animateColorAsState(target.onSecondaryContainer, animationSpec, label = "onSecondaryContainer")
    val tertiary by animateColorAsState(target.tertiary, animationSpec, label = "tertiary")
    val onTertiary by animateColorAsState(target.onTertiary, animationSpec, label = "onTertiary")
    val tertiaryContainer by animateColorAsState(target.tertiaryContainer, animationSpec, label = "tertiaryContainer")
    val onTertiaryContainer by animateColorAsState(target.onTertiaryContainer, animationSpec, label = "onTertiaryContainer")
    val background by animateColorAsState(target.background, animationSpec, label = "background")
    val onBackground by animateColorAsState(target.onBackground, animationSpec, label = "onBackground")
    val surface by animateColorAsState(target.surface, animationSpec, label = "surface")
    val onSurface by animateColorAsState(target.onSurface, animationSpec, label = "onSurface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, animationSpec, label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, animationSpec, label = "onSurfaceVariant")
    val surfaceTint by animateColorAsState(target.surfaceTint, animationSpec, label = "surfaceTint")
    val outline by animateColorAsState(target.outline, animationSpec, label = "outline")
    val outlineVariant by animateColorAsState(target.outlineVariant, animationSpec, label = "outlineVariant")
    val error by animateColorAsState(target.error, animationSpec, label = "error")
    val onError by animateColorAsState(target.onError, animationSpec, label = "onError")
    val errorContainer by animateColorAsState(target.errorContainer, animationSpec, label = "errorContainer")
    val onErrorContainer by animateColorAsState(target.onErrorContainer, animationSpec, label = "onErrorContainer")
    val inverseSurface by animateColorAsState(target.inverseSurface, animationSpec, label = "inverseSurface")
    val inverseOnSurface by animateColorAsState(target.inverseOnSurface, animationSpec, label = "inverseOnSurface")
    val inversePrimary by animateColorAsState(target.inversePrimary, animationSpec, label = "inversePrimary")
    val scrim by animateColorAsState(target.scrim, animationSpec, label = "scrim")

    return ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        outline = outline,
        outlineVariant = outlineVariant,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        inversePrimary = inversePrimary,
        scrim = scrim
    )
}