package com.kidzone.domain.model

import androidx.annotation.StringRes
import com.kidzone.R

/**
 * Kategorie miejsc, jakie użytkownik może dodać i filtrować.
 *
 * **Kolejność deklaracji jest istotna** – `entries` w tej kolejności jest
 * wyświetlane w UI (chipy filtrów na liście, dropdown w AddPlace).
 * Niealfabetycznie – ułożone według logicznej grupy: dwa "place zabaw"
 * (outdoor + indoor), dwie "jedzeniowe" (kawiarnia + restauracja), park,
 * atrakcje, inne.
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
        fun fromKey(key: String?): PlaceCategory =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: OTHER
    }
}
