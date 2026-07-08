package com.kidzone.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Schemat kolorów kidZone – odwzorowuje paletę marki.
 *
 *  - primary  = niebieski (akcje, paski, FAB)
 *  - secondary = zielony (potwierdzenia, sukces)
 *  - tertiary = żółty (akcenty, ranking, odznaki)
 */
private val LightColors = lightColorScheme(
    primary = BrandBlueAccessible,
    onPrimary = BrandWhite,
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = BrandWhite,

    secondary = BrandGreenAccessible,
    onSecondary = BrandWhite,
    secondaryContainer = BrandGreenDark,
    onSecondaryContainer = BrandWhite,

    tertiary = BrandYellow,
    onTertiary = BrandDark,
    tertiaryContainer = BrandYellowAccessible,
    onTertiaryContainer = BrandWhite,

    background = BrandWhite,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFF0F4F8),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFF9CA3AF),
    outlineVariant = Color(0xFFE5E7EB),

    // Neutralne surfaces dla menu, dropdownów, kart i sheetów.
    // Jawne wartości eliminują domyślne różowo/fioletowe tony Material3.
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F9FA),
    surfaceContainer = Color(0xFFF8F9FA),
    surfaceContainerHigh = Color(0xFFF0F4F8),
    surfaceContainerHighest = Color(0xFFE5E7EB)
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

/**
 * Główny motyw aplikacji kidZone.
 *
 * @param darkTheme true = ciemny motyw (domyślnie z ustawień systemu)
 * @param dynamicColor true = Material You dynamic colors z tapety (Android 12+).
 *   Na starszych urządzeniach fallbackuje do statycznej palety kidZone.
 *   Wyłączalne np. w ustawieniach apki jeśli user preferuje brand colors.
 */
@Composable
@Suppress("FunctionNaming")
fun KidZoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = when {
        // Material You dynamic colors (Android 12+ / API 31+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // Static brand palette fallback
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = KidZoneTypography,
        content = content
    )
}
