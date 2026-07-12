package com.kidzone.domain.model

import androidx.annotation.StringRes
import com.kidzone.R
import com.kidzone.domain.model.PlaceCategory.ATTRACTION
import com.kidzone.domain.model.PlaceCategory.CAFE
import com.kidzone.domain.model.PlaceCategory.OTHER
import com.kidzone.domain.model.PlaceCategory.PARK
import com.kidzone.domain.model.PlaceCategory.PLAYGROUND
import com.kidzone.domain.model.PlaceCategory.PLAY_ROOM
import com.kidzone.domain.model.PlaceCategory.RESTAURANT

/**
 * Udogodnienia oferowane przez miejsce.
 *
 * Każde udogodnienie deklaruje, dla których [PlaceCategory] jest sensowne.
 * UI w [com.kidzone.presentation.place.add.AddPlaceScreen] pokazuje listę
 * zależną od kategorii i używa kolejności z [categoryPriorityMap]. Wartości
 * enuma trzymają się stabilnie (nie zmieniaj nazw), bo trafiają do Firestore
 * jako `name`.
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
    WHEELCHAIR_ACCESSIBLE(
        R.string.amenity_stroller_access,
        setOf(PLAYGROUND, RESTAURANT, PLAY_ROOM, CAFE, PARK, ATTRACTION, OTHER)
    ),
    PARKING(
        R.string.amenity_parking,
        setOf(RESTAURANT, PLAY_ROOM, CAFE, PARK, ATTRACTION, OTHER)
    ),

    // === Plac zabaw ===
    FENCED(R.string.amenity_fencing, setOf(PLAYGROUND)),
    SOFT_SURFACE(R.string.amenity_soft_surface, setOf(PLAYGROUND)),
    SHADE(R.string.amenity_shaded_benches, setOf(PLAYGROUND, PARK)),
    TODDLER_ZONE(R.string.amenity_toddler_zone, setOf(PLAYGROUND, PLAY_ROOM)),
    LOW_TRAFFIC(R.string.amenity_car_free_area, setOf(PLAYGROUND, PARK)),

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
    SOFT_SAFETY(R.string.amenity_soft_protection, setOf(PLAY_ROOM, PLAYGROUND)),

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
    EVENING_LIGHTING(R.string.amenity_good_lighting, setOf(PARK, PLAYGROUND)),

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
        /**
         * Mapa kategorii -> priorytetowa lista udogodnień.
         * Kolejność jest UX-owa: bezpieczeństwo, opieka nad dzieckiem, wygoda,
         * dodatki. Dzięki temu rodzic najpierw widzi rzeczy krytyczne.
         */
        val categoryPriorityMap: Map<PlaceCategory, List<Amenity>> = mapOf(
            PLAYGROUND to listOf(
                FENCED,
                SOFT_SURFACE,
                TODDLER_ZONE,
                LOW_TRAFFIC,
                TOILET,
                CHANGING_TABLE,
                WHEELCHAIR_ACCESSIBLE,
                SHADE,
                EVENING_LIGHTING,
                SOFT_SAFETY
            ),
            RESTAURANT to listOf(
                CHANGING_TABLE,
                HIGH_CHAIR,
                KIDS_MENU,
                KIDS_TABLEWARE,
                KIDS_CORNER_VISIBLE,
                TOILET,
                WHEELCHAIR_ACCESSIBLE,
                FAST_SERVICE,
                QUIET_FEEDING,
                MICROWAVE,
                NO_LOUD_MUSIC,
                KIDS_ENTERTAINMENT,
                PARKING,
                FAMILY_PARKING,
                WIDE_DOORS,
                WIFI,
                KID_FRIENDLY_SIGNS
            ),
            CAFE to listOf(
                CHANGING_TABLE,
                HIGH_CHAIR,
                QUIET_FEEDING,
                TOILET,
                WHEELCHAIR_ACCESSIBLE,
                KIDS_MENU,
                KIDS_TABLEWARE,
                MICROWAVE,
                NO_LOUD_MUSIC,
                KIDS_ENTERTAINMENT,
                KIDS_CORNER_VISIBLE,
                SENSORY_TOYS,
                BREASTFEEDING_AREA,
                PARKING,
                FAMILY_PARKING,
                WIDE_DOORS,
                WIFI,
                QUIET_AREAS,
                KID_FRIENDLY_SIGNS
            ),
            PLAY_ROOM to listOf(
                AGE_ZONES,
                SOFT_SAFETY,
                MONITORING,
                TOY_SANITIZATION,
                TOILET,
                CHANGING_TABLE,
                WHEELCHAIR_ACCESSIBLE,
                TODDLER_ZONE,
                ANIMATOR,
                PARENT_ZONE,
                LOCKERS,
                SENSORY_TOYS,
                PARKING,
                FAMILY_PARKING,
                WIDE_DOORS,
                WIFI
            ),
            PARK to listOf(
                SAFE_PATHS,
                LOW_TRAFFIC,
                TOILET,
                WHEELCHAIR_ACCESSIBLE,
                CHANGING_TABLE,
                EVENING_LIGHTING,
                SHADE,
                PICNIC_AREA,
                DRINKING_WATER,
                BREASTFEEDING_AREA,
                PARKING,
                KID_FRIENDLY_SIGNS
            ),
            ATTRACTION to listOf(
                TOILET,
                CHANGING_TABLE,
                WHEELCHAIR_ACCESSIBLE,
                PARENT_CHILD_ROOM,
                LOST_CHILD_POINT,
                REST_AREAS,
                DRINKING_WATER,
                FAMILY_FAST_TRACK,
                STROLLER_RENTAL,
                KIDS_MENU,
                BREASTFEEDING_AREA,
                PARENT_ZONE,
                LOCKERS,
                PARKING,
                FAMILY_PARKING,
                WIDE_DOORS,
                WIFI,
                QUIET_AREAS,
                KID_FRIENDLY_SIGNS
            ),
            OTHER to listOf(
                TOILET,
                CHANGING_TABLE,
                WHEELCHAIR_ACCESSIBLE,
                MICROWAVE,
                PARKING,
                FAMILY_PARKING,
                WIDE_DOORS,
                WIFI,
                QUIET_AREAS
            )
        )

        /** Mapowanie nazw z Firestore na enum (case-insensitive); null gdy nieznane. */
        fun fromKey(key: String?): Amenity? =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) }

        /** Lista udogodnień, które warto pokazać dla danej [category], w kolejności UX. */
        fun forCategory(category: PlaceCategory): List<Amenity> =
            categoryPriorityMap[category].orEmpty()
                .filter { category in it.applicableCategories }
    }
}
