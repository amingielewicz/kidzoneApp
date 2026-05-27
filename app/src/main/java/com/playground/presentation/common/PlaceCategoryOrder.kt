package com.playground.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.playground.domain.model.PlaceCategory

/**
 * Pojedyncze źródło prawdy o kolejności wyświetlania kategorii w UI.
 *
 * Sortowanie alfabetyczne po polskim labelu, lowercased dla stabilności
 * (locale-aware – "Ł" wyląduje po "L"). Polskie etykiety znamy dopiero
 * w runtime, więc kompozycja musi mieć dostęp do [LocalContext]; wynik
 * cache'ujemy przez [remember] z kluczem `context`, żeby nie sortować
 * przy każdej rekompozycji.
 *
 * Używane przez:
 *  - [com.playground.presentation.place.list.PlaceListScreen] (pasek
 *    chipów filtra kategorii),
 *  - [com.playground.presentation.map.MapScreen] (pasek chipów na
 *    overlayu nad mapą).
 *
 * Wcześniej obie te ekrany miały zduplikowany identyczny kod sortujący;
 * wyciągnięcie go tu daje matematyczną pewność, że kolejność jest 1:1
 * – każda przyszła zmiana semantyki sortowania (np. zmiana na kolejność
 * deklaracji enuma, albo grupowanie po popularności) propaguje się
 * automatycznie do obu callsite'ów.
 */
@Composable
fun rememberPlaceCategoriesInDisplayOrder(): List<PlaceCategory> {
    val context = LocalContext.current
    return remember(context) {
        PlaceCategory.entries.sortedBy { context.getString(it.labelRes).lowercase() }
    }
}
