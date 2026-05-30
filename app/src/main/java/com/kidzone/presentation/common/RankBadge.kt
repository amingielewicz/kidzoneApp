package com.kidzone.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Plakietka rankingowa "label + gwiazdka z numerem rangi" wspólna dla:
 *  - profilu osoby (label "TOP", pula 1..100),
 *  - szczegółów miejsca (label "TOP 100", pula 1..10 - tylko top 10
 *    z setki dostaje plakietkę).
 *
 * Wizualnie spójna z konwencją medali olimpijskich:
 *  - **1..3** - złota gwiazdka (1./2./3. miejsce, prestiżowe),
 *  - **4..10** - srebrna gwiazdka (top dziesiątka, ale poza podium),
 *  - **11..100** - zielona gwiazdka (kolor brand-secondary, "fajna pozycja",
 *    ale bez "luksusowego" poczucia).
 *
 * Layer-cake: Column z labelem nad Boxem zawierającym Icon (gwiazdka tinted)
 * i Text (numer rangi nałożony pośrodku). Numer i tint zawsze pochodzą z
 * tej samej palety, żeby tekst był czytelny na tle gwiazdki.
 *
 * @param rank 1-based pozycja w rankingu. Funkcja nie clamp-uje - jeśli
 *   call-site przekaże rank > limit (np. 105), plakietka i tak się
 *   wyrenderuje z zieloną gwiazdką. Filtrowanie "nie pokazuj jak ma 101"
 *   jest odpowiedzialnością wywołującego.
 * @param label krótki tekst nad gwiazdką ("TOP", "TOP 100" itp.).
 * @param starSize rozmiar boxu z gwiazdką. 40dp = standard, ale można
 *   nadpisać dla mniejszych miejsc (np. 32dp na karcie listy).
 */
@Composable
fun RankBadge(
    rank: Int,
    label: String,
    modifier: Modifier = Modifier,
    starSize: Dp = 40.dp
) {
    val palette = rankBadgePaletteFor(rank)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = palette.labelColor
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier.size(starSize),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "$label: $rank",
                tint = palette.starTint,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.numberColor
            )
        }
    }
}

/**
 * Trójka kolorów składająca się na plakietkę rangi: gwiazdka, numer, label.
 *
 * Dlaczego data class a nie 3 osobne funkcje: użytkownik (RankBadge composable)
 * zawsze potrzebuje wszystkich trzech naraz, więc trzymanie ich razem jest
 * łatwiejsze do utrzymania (zmiana palety = jeden return) niż 3 niezależne
 * gałęzie `when` w 3 funkcjach, które łatwo rozjechać.
 */
data class RankBadgePalette(
    /** Tint gwiazdki (Icons.Filled.Star). */
    val starTint: Color,
    /** Kolor cyfry rangi nałożonej na gwiazdkę - musi mieć dobry kontrast z [starTint]. */
    val numberColor: Color,
    /** Kolor labela nad gwiazdką ("TOP" / "TOP 100"). */
    val labelColor: Color
)

/**
 * Paleta plakietki dla danego ranga.
 *
 * Progi (zob. dokumentacja [RankBadge]):
 *  - 1..3 = gold (Material amber 600 + black numer + ten sam gold label),
 *  - 4..10 = silver (Material grey 400 dla gwiazdki + black numer +
 *    grey 600 dla labela; ciemniejszy odcień labela poprawia kontrast
 *    na białym tle),
 *  - 11+ (też domyślny dla wartości spoza zakresu) = brand-secondary
 *    z theme (zielony w naszej palecie) + onSecondary dla numeru.
 *
 * Czarny `numberColor` dla gold/silver jest celowy - oba odcienie mają
 * wystarczająco wysoką luminancję, żeby białe cyfry znikały, a Material
 * `onSecondary` (zwykle biały / prawie biały) by się też nie nadał.
 * Black daje WCAG AAA na obu tłach.
 */
@Composable
fun rankBadgePaletteFor(rank: Int): RankBadgePalette = when {
    rank in 1..3 -> RankBadgePalette(
        starTint = Color(0xFFFFB300),
        numberColor = Color.Black,
        labelColor = Color(0xFFFFB300)
    )
    rank in 4..10 -> RankBadgePalette(
        starTint = Color(0xFFBDBDBD),
        numberColor = Color.Black,
        labelColor = Color(0xFF757575)
    )
    else -> RankBadgePalette(
        starTint = MaterialTheme.colorScheme.secondary,
        numberColor = MaterialTheme.colorScheme.onSecondary,
        labelColor = MaterialTheme.colorScheme.secondary
    )
}
