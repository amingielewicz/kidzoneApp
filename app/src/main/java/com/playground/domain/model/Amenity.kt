package com.playground.domain.model

import androidx.annotation.StringRes
import com.playground.R
import com.playground.domain.model.PlaceCategory.ATTRACTION
import com.playground.domain.model.PlaceCategory.CAFE
import com.playground.domain.model.PlaceCategory.OTHER
import com.playground.domain.model.PlaceCategory.PARK
import com.playground.domain.model.PlaceCategory.PLAYGROUND
import com.playground.domain.model.PlaceCategory.PLAY_ROOM
import com.playground.domain.model.PlaceCategory.RESTAURANT

/**
 * Udogodnienia oferowane przez miejsce.
 *
 * Każde udogodnienie deklaruje, dla których [PlaceCategory] jest sensowne –
 * UI w [com.playground.presentation.place.add.AddPlaceScreen] filtruje listę
 * po aktualnie wybranej kategorii. Wartości enuma trzymają się stabilnie
 * (nie zmieniaj nazw), bo trafiają do Firestore jako `name`.
 */
enum class Amenity(
    @StringRes val labelRes: Int,
    val applicableCategories: Set<PlaceCategory>
) {
    // === TL;DR – wszędzie istotne ===
    CHANGING_TABLE(
        R.string.amenity_changing_table,
        setOf(PLAYGROUND, RESTAURANT, PLAY_ROOM, CAFE, PARK, ATTRACTION, OTHER)
    ),
    TOILET(
        R.string.amenity_toilet,
        setOf(PLAYGROUND, RESTAURANT, PLAY_ROOM, CAFE, PARK, ATTRACTION, OTHER)
    ),
    STROLLER_ACCESS(
        R.string.amenity_stroller_access,
        setOf(PLAYGROUND, RESTAURANT, PLAY_ROOM, CAFE, PARK, ATTRACTION, OTHER)
    ),
    PARKING(
        R.string.amenity_parking,
        setOf(RESTAURANT, PLAY_ROOM, CAFE, PARK, ATTRACTION, OTHER)
    ),

    // === Plac zabaw ===
    FENCING(R.string.amenity_fencing, setOf(PLAYGROUND)),
    SOFT_SURFACE(R.string.amenity_soft_surface, setOf(PLAYGROUND)),
    SHADED_BENCHES(R.string.amenity_shaded_benches, setOf(PLAYGROUND, PARK)),
    TODDLER_ZONE(R.string.amenity_toddler_zone, setOf(PLAYGROUND, PLAY_ROOM)),
    CAR_FREE_AREA(R.string.amenity_car_free_area, setOf(PLAYGROUND, PARK)),

    // === Restauracja / Kawiarnia (food places) ===
    KIDS_MENU(R.string.amenity_kids_menu, setOf(RESTAURANT, CAFE, ATTRACTION)),
    HIGH_CHAIR(R.string.amenity_high_chair, setOf(RESTAURANT, CAFE)),
    KIDS_TABLEWARE(R.string.amenity_kids_tableware, setOf(RESTAURANT, CAFE)),
    FAST_SERVICE(R.string.amenity_fast_service, setOf(RESTAURANT, CAFE)),
    KIDS_ENTERTAINMENT(R.string.amenity_kids_entertainment, setOf(RESTAURANT, CAFE)),
    KIDS_CORNER_VISIBLE(R.string.amenity_kids_corner_visible, setOf(RESTAURANT, CAFE)),

    // === Sala zabaw ===
    AGE_ZONES(R.string.amenity_age_zones, setOf(PLAY_ROOM)),
    ANIMATOR(R.string.amenity_animator, setOf(PLAY_ROOM)),
    MONITORING(R.string.amenity_monitoring, setOf(PLAY_ROOM)),
    TOY_SANITIZATION(R.string.amenity_toy_sanitization, setOf(PLAY_ROOM)),
    PARENT_ZONE(R.string.amenity_parent_zone, setOf(PLAY_ROOM, ATTRACTION)),
    LOCKERS(R.string.amenity_lockers, setOf(PLAY_ROOM, ATTRACTION)),
    SOFT_PROTECTION(R.string.amenity_soft_protection, setOf(PLAY_ROOM, PLAYGROUND)),

    // === Kawiarnia (specyficzne) ===
    QUIET_FEEDING(R.string.amenity_quiet_feeding, setOf(CAFE, RESTAURANT)),
    MICROWAVE(R.string.amenity_microwave, setOf(CAFE, RESTAURANT, OTHER)),
    NO_LOUD_MUSIC(R.string.amenity_no_loud_music, setOf(CAFE, RESTAURANT)),
    SENSORY_TOYS(R.string.amenity_sensory_toys, setOf(CAFE, PLAY_ROOM)),

    // === Park ===
    PICNIC_AREA(R.string.amenity_picnic_area, setOf(PARK)),
    SAFE_PATHS(R.string.amenity_safe_paths, setOf(PARK)),
    DRINKING_WATER(R.string.amenity_drinking_water, setOf(PARK, ATTRACTION)),
    BREASTFEEDING_AREA(R.string.amenity_breastfeeding_area, setOf(PARK, ATTRACTION, CAFE)),
    GOOD_LIGHTING(R.string.amenity_good_lighting, setOf(PARK, PLAYGROUND)),

    // === Atrakcja ===
    STROLLER_RENTAL(R.string.amenity_stroller_rental, setOf(ATTRACTION)),
    REST_AREAS(R.string.amenity_rest_areas, setOf(ATTRACTION)),
    FAMILY_FAST_TRACK(R.string.amenity_family_fast_track, setOf(ATTRACTION)),
    PARENT_CHILD_ROOM(R.string.amenity_parent_child_room, setOf(ATTRACTION)),
    LOST_CHILD_POINT(R.string.amenity_lost_child_point, setOf(ATTRACTION)),

    // === Inne (ogólne) ===
    WIDE_DOORS(
        R.string.amenity_wide_doors,
        setOf(RESTAURANT, PLAY_ROOM, CAFE, ATTRACTION, OTHER)
    ),
    FAMILY_PARKING(
        R.string.amenity_family_parking,
        setOf(RESTAURANT, PLAY_ROOM, CAFE, ATTRACTION, OTHER)
    ),
    KID_FRIENDLY_SIGNS(
        R.string.amenity_kid_friendly_signs,
        setOf(RESTAURANT, PLAY_ROOM, CAFE, PARK, ATTRACTION, OTHER)
    ),
    WIFI(
        R.string.amenity_wifi,
        setOf(RESTAURANT, PLAY_ROOM, CAFE, ATTRACTION, OTHER)
    ),
    QUIET_AREAS(
        R.string.amenity_quiet_areas,
        setOf(CAFE, ATTRACTION, OTHER)
    );

    companion object {
        /** Mapowanie nazw z Firestore na enum (case-insensitive); null gdy nieznane. */
        fun fromKey(key: String?): Amenity? =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) }

        /** Lista udogodnień, które warto pokazać dla danej [category]. */
        fun forCategory(category: PlaceCategory): List<Amenity> =
            entries.filter { category in it.applicableCategories }
    }
}
