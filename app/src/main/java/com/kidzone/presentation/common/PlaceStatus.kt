@file:Suppress("MagicNumber", "FunctionNaming")

package com.kidzone.presentation.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kidzone.R
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
            text = stringResource(R.string.place_status_new),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
        )
    }
}

/**
 * Wspólny komponent wyświetlający ocenę i status miejsca.
 * Obsługuje:
 *  - Średnią ocenę + opcjonalną liczbę opinii.
 *  - Badge „Nowe” dla świeżo dodanych miejsc.
 *  - Etykietę „Brak opinii” dla starszych miejsc bez ocen.
 *
 * @param place model miejsca.
 * @param showCount czy wyświetlać liczbę opinii w nawiasie.
 * @param iconSize rozmiar gwiazdki.
 */
@Composable
fun PlaceRatingStatus(
    place: Place,
    modifier: Modifier = Modifier,
    showCount: Boolean = true,
    iconSize: Dp = 15.dp
) {
    when {
        place.reviewsCount > 0 -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RatingIcon(size = iconSize)
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "%.1f".format(place.averageRating),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                if (showCount) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "(${place.reviewsCount})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        place.isNewWithoutReviews() -> NewPlaceBadge(modifier = modifier)
        else -> NoReviewsLabel(modifier = modifier, iconSize = iconSize)
    }
}

/**
 * Standardowa etykieta dla miejsc bez opinii (szara gwiazdka).
 */
@Composable
fun NoReviewsLabel(
    modifier: Modifier = Modifier,
    iconSize: Dp = 15.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = stringResource(R.string.rating),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(iconSize)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = stringResource(R.string.map_no_reviews),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
            fontWeight = FontWeight.Medium
        )
    }
}
