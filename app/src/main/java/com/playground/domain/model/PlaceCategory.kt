package com.playground.domain.model

import androidx.annotation.StringRes
import com.playground.R

/**
 * Kategorie miejsc, jakie użytkownik może dodać i filtrować.
 */
enum class PlaceCategory(@StringRes val labelRes: Int) {
    PLAYGROUND(R.string.category_playground),
    RESTAURANT(R.string.category_restaurant),
    PLAY_ROOM(R.string.category_play_room),
    CAFE(R.string.category_cafe),
    PARK(R.string.category_park),
    ATTRACTION(R.string.category_attraction),
    OTHER(R.string.category_other);

    companion object {
        fun fromKey(key: String?): PlaceCategory =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: OTHER
    }
}
