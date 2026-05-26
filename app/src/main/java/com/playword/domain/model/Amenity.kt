package com.playword.domain.model

import androidx.annotation.StringRes
import com.playword.R

/**
 * Udogodnienia oferowane przez miejsce (przewijak, parking, itp.).
 */
enum class Amenity(@StringRes val labelRes: Int) {
    CHANGING_TABLE(R.string.amenity_changing_table),
    KIDS_MENU(R.string.amenity_kids_menu),
    PARKING(R.string.amenity_parking),
    TOILET(R.string.amenity_toilet),
    STROLLER_ACCESS(R.string.amenity_stroller_access);

    companion object {
        fun fromKey(key: String?): Amenity? =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
    }
}
