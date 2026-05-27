package com.playground.navigation

/**
 * Wszystkie trasy nawigacyjne aplikacji w jednym miejscu.
 *
 * Trasy z parametrem mają formę szablonu (np. `place_details/{placeId}`)
 * oraz funkcję pomocniczą [PlaceDetails.create] do podstawienia argumentu.
 */
sealed class Route(val path: String) {

    // --- Pre-auth ---
    data object Splash : Route("splash")
    data object Login : Route("login")
    data object Register : Route("register")

    // --- Main shell (zawiera bottom navigation) ---
    data object Main : Route("main")

    // Karty w obrębie Main:
    data object Home : Route("home")
    data object Map : Route("map")
    data object PlaceList : Route("place_list")
    data object Ranking : Route("ranking")
    data object Profile : Route("profile")

    // --- Ekrany "stack-owe" otwierane ponad shellem ---

    /**
     * Dodawanie / edycja miejsca.
     *
     * Bez argumentu działa jako "create" (`add_place`), z `?placeId=X`
     * jako "edit" – ekran pre-fillu danymi i `save()` woła `updatePlace`
     * zamiast `addPlace`.
     */
    data object AddPlace : Route("add_place?placeId={placeId}") {
        const val ARG_PLACE_ID = "placeId"

        /** Tworzy URL nawigacji – z [placeId] dla edycji, bez = dla nowego. */
        fun create(placeId: String? = null): String =
            if (placeId == null) "add_place" else "add_place?placeId=$placeId"
    }

    data object PlaceDetails : Route("place_details/{placeId}") {
        const val ARG_PLACE_ID = "placeId"
        fun create(placeId: String): String = "place_details/$placeId"
    }

    /** Mapa w pełnoekranie skupiona na pojedynczym miejscu (z markerem). */
    data object PlaceMap : Route("place_map/{placeId}") {
        const val ARG_PLACE_ID = "placeId"
        fun create(placeId: String): String = "place_map/$placeId"
    }
}
