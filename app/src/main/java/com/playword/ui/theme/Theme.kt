package com.playword.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Schemat kolorów PlayWord – odwzorowuje paletę marki.
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
    primary = BrandBlue,
    onPrimary = BrandWhite,

    secondary = BrandGreen,
    onSecondary = BrandWhite,

    tertiary = BrandYellow,
    onTertiary = BrandDark,

    background = SurfaceDarkBg,
    onBackground = OnSurfaceDark,
    surface = SurfaceDarkBg,
    onSurface = OnSurfaceDark
)

@Composable
fun PlayWordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = PlayWordTypography,
        content = content
    )
}
