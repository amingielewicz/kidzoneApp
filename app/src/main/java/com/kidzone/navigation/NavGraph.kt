package com.kidzone.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import com.kidzone.presentation.maintenance.MaintenanceScreen
import com.kidzone.presentation.onboarding.OnboardingScreen
import com.kidzone.presentation.place.add.AddPlaceScreen
import com.kidzone.presentation.place.details.PlaceDetailsScreen
import com.kidzone.presentation.place.myplaces.MyPlacesScreen
import com.kidzone.presentation.review.myreviews.MyReviewsScreen
import com.kidzone.presentation.splash.SplashScreen
import com.kidzone.data.remote.RemoteConfigService

/**
 * Klucze sygnalizujące "po dodaniu miejsca skacz na Map i wycentruj kamerę".
 * Używane przez [Route.AddPlace] (write) i [Route.Main] (read), oba przez
 * `savedStateHandle` na NavBackStackEntry – to standardowa droga przekazywania
 * jednorazowych "wyników" w Navigation Compose, bez globalnego SharedFlow.
 */
private const val NEW_PLACE_LAT = "newPlaceLat"
private const val NEW_PLACE_LNG = "newPlaceLng"

/** SharedPreferences klucz – czy user widzial onboarding. */
private const val ONBOARDING_PREFS = "kidzone_onboarding"
private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

/**
 * Główny graf nawigacji aplikacji – obsługuje przejścia pre-auth oraz
 * pchanie ekranów stackowych ponad shellem [MainScreen].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun KidZoneNavGraph(
    navController: NavHostController = rememberNavController(),
    intent: android.content.Intent? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pendingDeepLink = androidx.compose.runtime.remember {
        val uriFromData = intent?.data
        val uriFromExtra = intent?.getStringExtra("deepLink")?.let { android.net.Uri.parse(it) }
        // Fallback: odczytaj z SharedPreferences (przetrwa kill process na MIUI)
        val uriFromPrefs = context.getSharedPreferences("push_deep_links", android.content.Context.MODE_PRIVATE)
            .getString("pending_deep_link", null)
            ?.let { android.net.Uri.parse(it) }
        // Wyczyść po odczytaniu
        if (uriFromPrefs != null) {
            context.getSharedPreferences("push_deep_links", android.content.Context.MODE_PRIVATE)
                .edit().remove("pending_deep_link").apply()
        }
        mutableStateOf(uriFromData ?: uriFromExtra ?: uriFromPrefs)
    }


    val onboardingPrefs = androidx.compose.runtime.remember {
        context.getSharedPreferences(ONBOARDING_PREFS, android.content.Context.MODE_PRIVATE)
    }

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Route.Splash.path
        ) {
            composable(Route.Splash.path) {
                SplashScreen(
                    onSignedIn = {
                        // Gate: maintenance mode check (Remote Config)
                        val remoteConfig = dagger.hilt.android.EntryPointAccessors
                            .fromApplication(
                                context.applicationContext,
                                RemoteConfigEntryPoint::class.java
                            ).remoteConfigService()
                        if (remoteConfig.isMaintenanceMode) {
                            navController.navigate(Route.Maintenance.path) {
                                popUpTo(Route.Splash.path) { inclusive = true }
                            }
                            return@SplashScreen
                        }

                        val onboardingDone = onboardingPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
                        val destination = if (onboardingDone) Route.Main.path else Route.Onboarding.path
                        navController.navigate(destination) {
                            popUpTo(Route.Splash.path) { inclusive = true }
                        }
                        // Obsłuż deep link z push notification
                        pendingDeepLink.value?.let { uri ->
                            val host = uri.host.orEmpty()
                            val firstSegment = uri.pathSegments?.firstOrNull().orEmpty()
                            when (host) {
                                "place" -> {
                                    val placeId = NavigationArgumentValidator.sanitizePlaceId(firstSegment)
                                    if (placeId != null) {
                                        navController.navigate(Route.PlaceDetails.create(placeId))
                                    }
                                }
                                "profile" -> {
                                    navController.currentBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("focusTab", Route.Profile.path)
                                    // Jeśli deep link zawiera segment (np. "badges"),
                                    // przekazujemy go jako sygnał do ProfileScreen
                                    if (firstSegment.isNotBlank()) {
                                        navController.currentBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("profileSection", firstSegment)
                                    }
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

            composable(Route.Maintenance.path) {
                val remoteConfig = dagger.hilt.android.EntryPointAccessors
                    .fromApplication(
                        context.applicationContext,
                        RemoteConfigEntryPoint::class.java
                    ).remoteConfigService()
                MaintenanceScreen(message = remoteConfig.maintenanceMessage)
            }

            composable(Route.Login.path) {
                LoginScreen(
                    onLoginSuccess = {
                        val onboardingDone = onboardingPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
                        val destination = if (onboardingDone) Route.Main.path else Route.Onboarding.path
                        navController.navigate(destination) {
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

            composable(Route.Onboarding.path) {
                OnboardingScreen(
                    onComplete = {
                        onboardingPrefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
                        navController.navigate(Route.Main.path) {
                            popUpTo(Route.Onboarding.path) { inclusive = true }
                        }
                    }
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
                val profileSection by savedHandle
                    .getStateFlow("profileSection", "")
                    .collectAsState()

                MainScreen(
                    focusLatitude = focusLat,
                    focusLongitude = focusLng,
                    focusTab = focusTab,
                    rankingTab = rankingTab,
                    profileSection = profileSection,
                    onFocusConsumed = {
                        savedHandle[NEW_PLACE_LAT] = null
                        savedHandle["focusTab"] = ""
                        savedHandle["rankingTab"] = ""
                        savedHandle["profileSection"] = ""
                        savedHandle[NEW_PLACE_LNG] = null
                    },
                    onOpenPlaceDetails = { placeId, source ->
                        navController.navigate(Route.PlaceDetails.create(placeId, source)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenAddPlace = { navController.navigate(Route.AddPlace.create()) },
                    onOpenMyPlaces = { navController.navigate(Route.MyPlaces.path) },
                    onOpenMyReviews = { navController.navigate(Route.MyReviews.path) },
                    onSignOut = {
                        navController.navigate(Route.Login.path) {
                            popUpTo(Route.Main.path) { inclusive = true }
                        }
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable
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
                    navArgument(Route.PlaceDetails.ARG_PLACE_ID) { type = NavType.StringType },
                    navArgument(Route.PlaceDetails.ARG_SOURCE) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                ),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "https://playground-705e7162.web.app/place/{placeId}" },
                    navDeepLink { uriPattern = "kidzone://place/{placeId}" }
                )
            ) { backStackEntry ->
                val source = backStackEntry.arguments?.getString(Route.PlaceDetails.ARG_SOURCE)
                val placeId = NavigationArgumentValidator.sanitizePlaceId(
                    backStackEntry.arguments?.getString(Route.PlaceDetails.ARG_PLACE_ID)
                ).orEmpty()

                if (placeId.isBlank()) {
                    navController.popBackStack()
                    return@composable
                }

                PlaceDetailsScreen(
                    onBack = { navController.popBackStack() },
                    onEditPlace = { navController.navigate(Route.AddPlace.create(it)) },
                    onDeleted = {
                        // Po usunięciu wracamy do shellu Main – snapshot listener
                        // na liście usunie kartę sam.
                        navController.popBackStack(Route.Main.path, inclusive = false)
                    },
                    placeId = placeId,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    animationSource = source
                )
            }

            composable(Route.MyPlaces.path) {
                MyPlacesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPlaceDetails = { placeId, source ->
                        navController.navigate(Route.PlaceDetails.create(placeId, source)) {
                            launchSingleTop = true
                        }
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable
                )
            }

            composable(Route.MyReviews.path) {
                MyReviewsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPlaceDetails = { placeId, source ->
                        navController.navigate(Route.PlaceDetails.create(placeId, source)) {
                            launchSingleTop = true
                        }
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable
                )
            }
        }
    }
}
