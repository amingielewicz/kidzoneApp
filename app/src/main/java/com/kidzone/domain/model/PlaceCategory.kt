package com.kidzone.domain.model

import androidx.annotation.StringRes
import com.kidzone.R

/**
 * Kategorie miejsc dostępne w formularzach, filtrach, mapie i rankingu.
 *
 * Nazwa wartości enuma jest trwałym kluczem zapisywanym w Firestore, dlatego istniejących nazw nie
 * należy zmieniać bez migracji danych i kompatybilności ze starszymi buildami. Kolejność deklaracji
 * jest równocześnie domyślną kolejnością prezentacji w UI.
 *
 * @property labelRes zasób przetłumaczonej etykiety kategorii.
 */
enum class PlaceCategory(@StringRes val labelRes: Int) {
    PLAYGROUND(R.string.category_playground),
    PLAY_ROOM(R.string.category_play_room),
    CAFE(R.string.category_cafe),
    RESTAURANT(R.string.category_restaurant),
    PARK(R.string.category_park),
    ATTRACTION(R.string.category_attraction),
    OTHER(R.string.category_other);

    companion object {
        /**
         * Mapuje klucz zapisany w backendzie na kategorię domenową.
         *
         * @param key nazwa enuma, bez rozróżniania wielkości liter.
         * @return dopasowana kategoria albo [OTHER], gdy klucz jest pusty lub nieznany.
         */
        fun fromKey(key: String?): PlaceCategory =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: OTHER
    }
}
