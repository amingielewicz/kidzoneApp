@file:Suppress("MagicNumber")

package com.kidzone.presentation.common

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kidzone.domain.model.Place

private const val NEW_PLACE_WINDOW_DAYS = 30L
private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
private val NewPlaceContainerColor = Color(0xFFE3F2FD)
private val NewPlaceContentColor = Color(0xFF0D47A1)

/**
 * Sprawdza, czy miejsce powinno otrzymać status „Nowe”.
 *
 * Status jest przyznawany wyłącznie miejscom bez opinii, utworzonym nie wcześniej niż 30 dni temu.
 * Przyszły timestamp, brak daty albo istniejąca opinia wyłączają status.
 *
 * @param reviewsCount liczba opinii miejsca.
 * @param createdAtMillis czas utworzenia miejsca.
 * @param nowMillis punkt odniesienia, domyślnie bieżący czas urządzenia.
 * @return `true`, gdy miejsce spełnia warunki statusu „Nowe”.
 */
fun isNewWithoutReviews(
    reviewsCount: Int,
    createdAtMillis: Long,
    nowMillis: Long = System.currentTimeMillis()
): Boolean {
    if (reviewsCount > 0 || createdAtMillis <= 0L) return false

    val ageMillis = nowMillis - createdAtMillis
    return ageMillis in 0..(NEW_PLACE_WINDOW_DAYS * MILLIS_PER_DAY)
}

/**
 * Wygodna wersja [isNewWithoutReviews] dla modelu [Place].
 */
fun Place.isNewWithoutReviews(nowMillis: Long = System.currentTimeMillis()): Boolean =
    isNewWithoutReviews(
        reviewsCount = reviewsCount,
        createdAtMillis = createdAtMillis,
        nowMillis = nowMillis
    )

/**
 * Renderuje dostępny wizualnie badge „Nowe” dla miejsca bez opinii.
 *
 * Komponent nie decyduje samodzielnie o statusie miejsca. Warstwa wywołująca powinna użyć
 * [Place.isNewWithoutReviews] i zadbać o semantykę całej karty.
 */
@Composable
fun NewPlaceBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .widthIn(min = 46.dp)
            .heightIn(min = 24.dp),
        shape = RoundedCornerShape(KidZoneRadii.Badge),
        color = NewPlaceContainerColor,
        contentColor = NewPlaceContentColor
    ) {
        Text(
            text = "Nowe",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
        )
    }
}
