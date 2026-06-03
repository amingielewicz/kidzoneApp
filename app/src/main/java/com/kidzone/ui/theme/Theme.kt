package com.kidzone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Schemat kolorów kidZone – odwzorowuje paletę marki.
 *
 *  - primary  = niebieski (akcje, paski, FAB)
 *  - secondary = zielony (potwierdzenia, sukces)
 *  - tertiary = żółty (akcenty, ranking, odznaki)
 */
private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = BrandWhite,
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = BrandWhite,

    secondary = BrandGreen,
    onSecondary = BrandWhite,
    secondaryContainer = BrandGreenDark,
    onSecondaryContainer = BrandWhite,

    tertiary = BrandYellow,
    onTertiary = BrandDark,
    tertiaryContainer = BrandYellowDark,
    onTertiaryContainer = BrandDark,

    background = BrandWhite,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueLighter,
    onPrimary = BrandDark,
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = BrandBlueLighter,

    secondary = BrandGreenLighter,
    onSecondary = BrandDark,
    secondaryContainer = BrandGreenDark,
    onSecondaryContainer = BrandGreenLighter,

    tertiary = BrandYellowLighter,
    onTertiary = BrandDark,
    tertiaryContainer = BrandYellowDark,
    onTertiaryContainer = BrandYellowLighter,

    background = SurfaceDarkBg,
    onBackground = OnSurfaceDark,
    surface = SurfaceDarkBg,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDarkVariant,
    onSurfaceVariant = OnSurfaceDarkVariant,
    outline = OutlineDark,
    outlineVariant = Color(0xFF383838),

    // Podwyższone surfaces (karty, dialogi, sheety)
    surfaceContainerLowest = Color(0xFF0E0E0E),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = SurfaceDarkElevated,
    surfaceContainerHigh = Color(0xFF252525),
    surfaceContainerHighest = SurfaceDarkVariant,

    error = Color(0xFFEF5350),
    onError = BrandWhite,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun KidZoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = KidZoneTypography,
        content = content
    )
}
