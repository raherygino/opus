package com.gsoft.opus.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.gsoft.opus.domain.model.ColorPalette

private val IndigoLight = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = Color(0xFF2D1A6B),
    secondary = BrandSecondary,
    onSecondary = Color(0xFF003544),
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = Color(0xFF001E26),
    tertiary = BrandTertiary,
    onTertiary = Color.White,
    tertiaryContainer = BrandTertiaryContainer,
    onTertiaryContainer = Color(0xFF5C0017),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = ErrorRed,
    onError = Color.White
)

private val IndigoDark = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF1A0066),
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFE8E5FF),
    secondary = BrandSecondaryDark,
    onSecondary = Color(0xFF001E26),
    secondaryContainer = BrandSecondaryContainerDark,
    onSecondaryContainer = Color(0xFFD0F4FF),
    tertiary = BrandTertiaryDark,
    onTertiary = Color(0xFF5C0017),
    tertiaryContainer = BrandTertiaryContainerDark,
    onTertiaryContainer = Color(0xFFFFD9E0),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = ErrorRed,
    onError = Color.White
)

private val BlueLight = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2D9FF),
    onTertiaryContainer = Color(0xFF251431),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = ErrorRed,
    onError = Color.White
)

private val BlueDark = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD6BEE4),
    onTertiary = Color(0xFF3B2947),
    tertiaryContainer = Color(0xFF52405F),
    onTertiaryContainer = Color(0xFFF2D9FF),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = ErrorRed,
    onError = Color.White
)

private val GreenLight = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8F0CB),
    onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFF4F6352),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8D3),
    onSecondaryContainer = Color(0xFF0D1F13),
    tertiary = Color(0xFF3A656F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBEEAF6),
    onTertiaryContainer = Color(0xFF001F24),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = ErrorRed,
    onError = Color.White
)

private val GreenDark = darkColorScheme(
    primary = Color(0xFF8AD991),
    onPrimary = Color(0xFF00391B),
    primaryContainer = Color(0xFF115E23),
    onPrimaryContainer = Color(0xFFC8F0CB),
    secondary = Color(0xFFB6CCB8),
    onSecondary = Color(0xFF223527),
    secondaryContainer = Color(0xFF384B3D),
    onSecondaryContainer = Color(0xFFD2E8D3),
    tertiary = Color(0xFFA2CEDA),
    onTertiary = Color(0xFF013644),
    tertiaryContainer = Color(0xFF1F4D57),
    onTertiaryContainer = Color(0xFFBEEAF6),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = ErrorRed,
    onError = Color.White
)

private val PurpleLight = lightColorScheme(
    primary = Color(0xFF9C27B0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDD4FF),
    onPrimaryContainer = Color(0xFF2E003E),
    secondary = Color(0xFF6B586B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3DAF2),
    onSecondaryContainer = Color(0xFF251726),
    tertiary = Color(0xFF825547),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCF),
    onTertiaryContainer = Color(0xFF321207),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = ErrorRed,
    onError = Color.White
)

private val PurpleDark = darkColorScheme(
    primary = Color(0xFFE0B0FF),
    onPrimary = Color(0xFF4B0064),
    primaryContainer = Color(0xFF790091),
    onPrimaryContainer = Color(0xFFFDD4FF),
    secondary = Color(0xFFD6BFDA),
    onSecondary = Color(0xFF3B2B3C),
    secondaryContainer = Color(0xFF534153),
    onSecondaryContainer = Color(0xFFF3DAF2),
    tertiary = Color(0xFFF6B8A5),
    onTertiary = Color(0xFF4C2517),
    tertiaryContainer = Color(0xFF663B2C),
    onTertiaryContainer = Color(0xFFFFDBCF),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = ErrorRed,
    onError = Color.White
)

private val RedLight = lightColorScheme(
    primary = Color(0xFFE53935),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF775651),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF2C1512),
    tertiary = Color(0xFF705C2E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCDFA6),
    onTertiaryContainer = Color(0xFF261900),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = ErrorRed,
    onError = Color.White
)

private val RedDark = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFFB3261E),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB6),
    onSecondary = Color(0xFF442C28),
    secondaryContainer = Color(0xFF5D403B),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFDDC488),
    onTertiary = Color(0xFF3E2E04),
    tertiaryContainer = Color(0xFF564419),
    onTertiaryContainer = Color(0xFFFCDFA6),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = ErrorRed,
    onError = Color.White
)

private val OrangeLight = lightColorScheme(
    primary = Color(0xFFEF6C00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC9),
    onPrimaryContainer = Color(0xFF331100),
    secondary = Color(0xFF755846),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBC9),
    onSecondaryContainer = Color(0xFF2A170C),
    tertiary = Color(0xFF5F6131),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5E7AA),
    onTertiaryContainer = Color(0xFF1B1D00),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = ErrorRed,
    onError = Color.White
)

private val OrangeDark = darkColorScheme(
    primary = Color(0xFFFFB59B),
    onPrimary = Color(0xFF542200),
    primaryContainer = Color(0xFFC24A00),
    onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = Color(0xFFE4BFA9),
    onSecondary = Color(0xFF43290C),
    secondaryContainer = Color(0xFF5B402A),
    onSecondaryContainer = Color(0xFFFFDBC9),
    tertiary = Color(0xFFC9CB90),
    onTertiary = Color(0xFF31340A),
    tertiaryContainer = Color(0xFF484A1C),
    onTertiaryContainer = Color(0xFFE5E7AA),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = ErrorRed,
    onError = Color.White
)

private val TealLight = lightColorScheme(
    primary = Color(0xFF00897B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1E6),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF4A635F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8E2),
    onSecondaryContainer = Color(0xFF06201C),
    tertiary = Color(0xFF476079),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCEE5FF),
    onTertiaryContainer = Color(0xFF001E2F),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = ErrorRed,
    onError = Color.White
)

private val TealDark = darkColorScheme(
    primary = Color(0xFF80D4C7),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFF9CF1E6),
    secondary = Color(0xFFB0CCC7),
    onSecondary = Color(0xFF1B3531),
    secondaryContainer = Color(0xFF324B47),
    onSecondaryContainer = Color(0xFFCCE8E2),
    tertiary = Color(0xFFAFC8E4),
    onTertiary = Color(0xFF163249),
    tertiaryContainer = Color(0xFF2E4860),
    onTertiaryContainer = Color(0xFFCEE5FF),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = ErrorRed,
    onError = Color.White
)

private val BlackLight = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDEDED),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFF555555),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF1F1F1F),
    tertiary = Color(0xFF777777),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8E8E8),
    onTertiaryContainer = Color(0xFF1A1A1A),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF444444),
    error = ErrorRed,
    onError = Color.White
)

private val BlackDark = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFE6E6E6),
    secondary = Color(0xFFAAAAAA),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFD9D9D9),
    tertiary = Color(0xFF888888),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF2E2E2E),
    onTertiaryContainer = Color(0xFFE0E0E0),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFBBBBBB),
    error = ErrorRed,
    onError = Color.White
)

fun getColorScheme(palette: ColorPalette, darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        when (palette) {
            ColorPalette.INDIGO -> IndigoDark
            ColorPalette.BLUE -> BlueDark
            ColorPalette.GREEN -> GreenDark
            ColorPalette.PURPLE -> PurpleDark
            ColorPalette.RED -> RedDark
            ColorPalette.ORANGE -> OrangeDark
            ColorPalette.TEAL -> TealDark
            ColorPalette.BLACK -> BlackDark
        }
    } else {
        when (palette) {
            ColorPalette.INDIGO -> IndigoLight
            ColorPalette.BLUE -> BlueLight
            ColorPalette.GREEN -> GreenLight
            ColorPalette.PURPLE -> PurpleLight
            ColorPalette.RED -> RedLight
            ColorPalette.ORANGE -> OrangeLight
            ColorPalette.TEAL -> TealLight
            ColorPalette.BLACK -> BlackLight
        }
    }
}

fun getGradientColors(palette: ColorPalette, darkTheme: Boolean): List<Color> {
    return if (darkTheme) {
        when (palette) {
            ColorPalette.INDIGO -> listOf(GradientDarkStart, GradientDarkEnd)
            ColorPalette.BLUE -> listOf(Color(0xFF1A2A4A), Color(0xFF0D1A2E))
            ColorPalette.GREEN -> listOf(Color(0xFF1A2E1D), Color(0xFF0D1A0F))
            ColorPalette.PURPLE -> listOf(Color(0xFF2A1A2E), Color(0xFF1A0D1E))
            ColorPalette.RED -> listOf(Color(0xFF2E1A1A), Color(0xFF1E0D0D))
            ColorPalette.ORANGE -> listOf(Color(0xFF2E221A), Color(0xFF1E160D))
            ColorPalette.TEAL -> listOf(Color(0xFF1A2E2A), Color(0xFF0D1A17))
            ColorPalette.BLACK -> listOf(Color(0xFF1A1A1A), Color(0xFF000000))
        }
    } else {
        when (palette) {
            ColorPalette.INDIGO -> listOf(GradientStart, GradientEnd)
            ColorPalette.BLUE -> listOf(Color(0xFF1976D2), Color(0xFF64B5F6))
            ColorPalette.GREEN -> listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))
            ColorPalette.PURPLE -> listOf(Color(0xFF9C27B0), Color(0xFFCE93D8))
            ColorPalette.RED -> listOf(Color(0xFFE53935), Color(0xFFEF9A9A))
            ColorPalette.ORANGE -> listOf(Color(0xFFEF6C00), Color(0xFFFFB74D))
            ColorPalette.TEAL -> listOf(Color(0xFF00897B), Color(0xFF4DB6AC))
            ColorPalette.BLACK -> listOf(Color(0xFF000000), Color(0xFF444444))
        }
    }
}
