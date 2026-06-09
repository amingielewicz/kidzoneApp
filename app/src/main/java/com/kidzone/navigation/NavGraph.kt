package com.kidzone.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.kidzone.presentation.auth.LoginScreen
import com.kidzone.presentation.auth.RegisterScreen
import com.kidzone.presentation.main.MainScreen
import com.kidzone.presentation.place.add.AddPlaceScreen
import com.kidzone.presentation.place.details.PlaceDetailsScreen
import com.kidzone.presentation.place.myplaces.MyPlacesScreen
import com.kidzone.presentation.review.myreviews.MyReviewsScreen
import com.kidzone.presentation.splash.SplashScreen

/**
 * Klucze sygnalizujące "po dodaniu miejsca skacz na Map i wycentruj kamerę".
 * Używane przez [Route.AddPlace] (write) i [Route.Main] (read), oba przez
 * `savedStateHandle` na NavBackStackEntry – to standardowa droga przekazywania
 * jednorazowych "wyników" w Navigation Compose, bez globalnego SharedFlow.
 */
private const val NEW_PLACE_LAT = "newPlaceLat"
private const val NEW_PLACE_LNG = "newPlaceLng"

/**
 * Główny graf nawigacji aplikacji – obsługuje przejścia pre-auth oraz
 * pchanie ekranów stackowych ponad shellem [MainScreen].
 */
@Composable
fun KidZoneNavGraph(
    navController: NavHostController = rememberNavController(),
    intent: android.content.Intent? = null
) {
    // Deep link z push notification — konsumujemy po zalogowaniu.
    val pendingDeepLink = androidx.compose.runtime.remember {
        val uriFromData = intent?.data
        val uriFromExtra = intent?.getStringExtra("deepLink")?.let { android.net.Uri.parse(it) }
        mutableStateOf(uriFromData ?: uriFromExtra)
    }

    NavHost(
        navController = navController,
        startDestination = Route.Splash.path
    ) {
        composable(Route.Splash.path) {
            SplashScreen(
                onSignedIn = {
                    navController.navigate(Route.Main.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                    // Obsłuż deep link z push notification
                    pendingDeepLink.value?.let { uri ->
                        val host = uri.host.orEmpty()
                        val firstSegment = uri.pathSegments?.firstOrNull().orEmpty()
                        when (host) {
                            "place" -> {
                                val placeId = firstSegment
                                if (placeId.isNotBlank()) {
                                    navController.navigate(Route.PlaceDetails.create(placeId))
                                }
                            }
                            "profile" -> {
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("focusTab", Route.Profile.path)
                            }
                            "ranking" -> {
                                // firstSegment = "places" or "users"
                                val tab = if (firstSegment == "users") "users" else "places"
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("focusTab", Route.Ranking.path)
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("rankingTab", tab)
                            }
                        }
                        pendingDeepLink.value = null
                    }
                },
                onSignedOut = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                    pendingDeepLink.value = null
                }
            )
        }

        composable(Route.Login.path) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.Main.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Route.Register.path) }
            )
        }

        composable(Route.Register.path) {
            RegisterScreen(
                onRegisterSuccess = {
                    // Po rejestracji user jest wylogowany (musi potwierdzić email).
                    // Wracamy na Login z komunikatem o weryfikacji.
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.Register.path) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.Main.path) { backStackEntry ->
            // Po pomyślnym `addPlace` (tryb create) NavGraph zapisuje
            // współrzędne nowego miejsca w savedStateHandle tego wpisu –
            // MainScreen je odczytuje i nawiguje na zakładkę Map +
            // wyświetla toast.
            val savedHandle = backStackEntry.savedStateHandle
            val focusLat by savedHandle
                .getStateFlow<Double?>(NEW_PLACE_LAT, null)
                .collectAsState()
            val focusLng by savedHandle
                .getStateFlow<Double?>(NEW_PLACE_LNG, null)
                .collectAsState()
            val focusTab by savedHandle
                .getStateFlow("focusTab", "")
                .collectAsState()
            val rankingTab by savedHandle
                .getStateFlow("rankingTab", "")
                .collectAsState()

            MainScreen(
                focusLatitude = focusLat,
                focusLongitude = focusLng,
                focusTab = focusTab,
                rankingTab = rankingTab,
                onFocusConsumed = {
                    savedHandle[NEW_PLACE_LAT] = null
                    savedHandle["focusTab"] = ""
                    savedHandle["rankingTab"] = ""
                    savedHandle[NEW_PLACE_LNG] = null
                },
                onOpenPlaceDetails = { placeId ->
                    navController.navigate(Route.PlaceDetails.create(placeId))
                },
                onOpenAddPlace = { navController.navigate(Route.AddPlace.create()) },
                onOpenMyPlaces = { navController.navigate(Route.MyPlaces.path) },
                onOpenMyReviews = { navController.navigate(Route.MyReviews.path) },
                onSignOut = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.Main.path) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Route.AddPlace.path,
            arguments = listOf(
                navArgument(Route.AddPlace.ARG_PLACE_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            // Tryb edit gdy placeId jest podany – w tym wypadku po zapisie
            // chcemy popować ZARÓWNO AddPlace JAK I PlaceDetails (były pod
            // spodem), żeby user wylądował z powrotem na liście / shellu Main
            // i nie zobaczył szczegółów ze stale-data.
            val isEdit = backStackEntry.arguments
                ?.getString(Route.AddPlace.ARG_PLACE_ID)
                ?.isNotBlank() == true
            AddPlaceScreen(
                onSaved = { newLat, newLng ->
                    if (isEdit) {
                        navController.popBackStack(Route.Main.path, inclusive = false)
                    } else {
                        // Tryb create: zapisujemy współrzędne w savedStateHandle
                        // poprzedniego wpisu (czyli Main), żeby MainScreen po
                        // popBackStack mógł przełączyć na Map i wycentrować.
                        if (newLat != null && newLng != null) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.apply {
                                    set(NEW_PLACE_LAT, newLat)
                                    set(NEW_PLACE_LNG, newLng)
                                }
                        }
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.PlaceDetails.path,
            arguments = listOf(
                navArgument(Route.PlaceDetails.ARG_PLACE_ID) { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://kidzone.app/place/{placeId}" },
                navDeepLink { uriPattern = "kidzone://place/{placeId}" }
            )
        ) {
            PlaceDetailsScreen(
                onBack = { navController.popBackStack() },
                onEditPlace = { placeId ->
                    navController.navigate(Route.AddPlace.create(placeId))
                },
                onDeleted = {
                    // Po usunięciu wracamy do shellu Main – snapshot listener
                    // na liście usunie kartę sam.
                    navController.popBackStack(Route.Main.path, inclusive = false)
                }
            )
        }

        composable(Route.MyPlaces.path) {
            MyPlacesScreen(
                onBack = { navController.popBackStack() },
                onOpenPlaceDetails = { placeId ->
                    navController.navigate(Route.PlaceDetails.create(placeId))
                }
            )
        }

        composable(Route.MyReviews.path) {
            MyReviewsScreen(
                onBack = { navController.popBackStack() },
                onOpenPlaceDetails = { placeId ->
                    navController.navigate(Route.PlaceDetails.create(placeId))
                }
            )
        }
    }
}
