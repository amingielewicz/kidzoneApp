package com.playword.navigation

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
    data object AddPlace : Route("add_place")

    data object PlaceDetails : Route("place_details/{placeId}") {
        const val ARG_PLACE_ID = "placeId"
        fun create(placeId: String): String = "place_details/$placeId"
    }
}
