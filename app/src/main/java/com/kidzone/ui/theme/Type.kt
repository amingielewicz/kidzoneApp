package com.kidzone.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kidzone.R

/**
 * Brand font kidZone – Poppins (OFL, bundlowany w `res/font/`).
 *
 * Bundlujemy 3 weights: Regular (400), SemiBold (600), Bold (700) – razem
 * ~470 KB w APK. Compose interpoluje pozostałe weights z najbliższego
 * dostępnego (np. `FontWeight.Medium` -> Regular, `FontWeight.ExtraBold`
 * -> Bold). Dla naszego UI te 3 weights wystarczą.
 *
 * Plik znaleziony przez `R.font.poppins_*` resolver – Android scaler
 * automatycznie podmienia wariant pod typografią Material.
 */
internal val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

/**
 * Typografia kidZone – Poppins na nagłówkach (display/headline/title) +
 * przyciskach (label), SansSerif dla body (czytelniejszy w dłuższych
 * tekstach jak adresy, opisy miejsc, listy opinii).
 *
 * Mapowanie:
 *  - `displayLarge` (32sp Bold)      -> Poppins Bold
 *  - `headlineMedium` (24sp SemiBold) -> Poppins SemiBold
 *  - `titleLarge` (20sp SemiBold)     -> Poppins SemiBold  <- TopAppBar
 *  - `titleMedium` (16sp Medium)      -> Poppins (Compose interpoluje
 *    Medium z Regular, bo w bundlu nie ma 500)
 *  - `bodyLarge`/`bodyMedium`         -> system SansSerif (Roboto)
 *  - `labelLarge` (14sp SemiBold)     -> Poppins SemiBold <- przyciski
 *
 * `titleLarge` jest stylem domyślnym dla `TopAppBar` Material 3, więc
 * wystarczy zdefiniować go w Poppins, by napis "kidZone" w `MainScreen`
 * (i kolejne tytuły w stack ekranów) automatycznie używały brand fontu
 * – bez ręcznej zmiany w każdym callsite.
 */
internal val KidZoneTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    )
)
