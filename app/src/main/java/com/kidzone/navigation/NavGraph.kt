@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.kidzone.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
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
import com.kidzone.presentation.onboarding.OnboardingScreen
import com.kidzone.presentation.place.add.AddPlaceScreen
import com.kidzone.presentation.place.details.PlaceDetailsScreen
import com.kidzone.presentation.place.myplaces.MyPlacesScreen
import com.kidzone.presentation.review.myreviews.MyReviewsScreen
import com.kidzone.presentation.splash.SplashScreen

private const val NEW_PLACE_LAT = "newPlaceLat"
private const val NEW_PLACE_LNG = "newPlaceLng"
private const val ONBOARDING_PREFS = "kidzone_onboarding"
private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

/**
 * Główny graf nawigacji aplikacji – obsługuje przejścia pre-auth oraz
 * pchanie ekranów stackowych ponad shellem [MainScreen].
 */
@Composable
fun KidZoneNavGraph(
    navController: NavHostController = rememberNavController(),
    intent: android.content.Intent? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pendingDeepLink = androidx.compose.runtime.remember {
        val uriFromData = intent?.data
        val uriFromExtra = intent?.getStringExtra("deepLink")?.let { android.net.Uri.parse(it) }
        val uriFromPrefs = context.getSharedPreferences("push_deep_links", android.content.Context.MODE_PRIVATE)
            .getString("pending_deep_link", null)
            ?.let { android.net.Uri.parse(it) }
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
                        val onboardingDone = onboardingPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
                        val destination = if (onboardingDone) Route.Main.path else Route.Onboarding.path
                        navController.navigate(destination) {
                            popUpTo(Route.Splash.path) { inclusive = true }
                        }
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
                val savedHandle = backStackEntry.savedStateHandle
                val focusLat by savedHandle.getStateFlow<Double?>(NEW_PLACE_LAT, null).collectAsState()
                val focusLng by savedHandle.getStateFlow<Double?>(NEW_PLACE_LNG, null).collectAsState()
                val focusTab by savedHandle.getStateFlow("focusTab", "").collectAsState()
                val rankingTab by savedHandle.getStateFlow("rankingTab", "").collectAsState()

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
                    onOpenPlaceDetails = { placeId, source ->
                        navController.navigate(Route.PlaceDetails.create(placeId, source))
                    },
                    onOpenUserProfile = { userId ->
                        navController.navigate(Route.UserProfile.create(userId))
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
                val isEdit = backStackEntry.arguments?.getString(Route.AddPlace.ARG_PLACE_ID)?.isNotBlank() == true
                AddPlaceScreen(
                    onSaved = { newLat, newLng ->
                        if (isEdit) {
                            navController.popBackStack(Route.Main.path, inclusive = false)
                        } else {
                            if (newLat != null && newLng != null) {
                                navController.previousBackStackEntry?.savedStateHandle?.apply {
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
                val placeId = backStackEntry.arguments?.getString(Route.PlaceDetails.ARG_PLACE_ID).orEmpty()
                val source = backStackEntry.arguments?.getString(Route.PlaceDetails.ARG_SOURCE)
                PlaceDetailsScreen(
                    onBack = { navController.popBackStack() },
                    onEditPlace = { pid ->
                        navController.navigate(Route.AddPlace.create(pid))
                    },
                    onDeleted = {
                        navController.popBackStack(Route.Main.path, inclusive = false)
                    },
                    placeId = placeId,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    animationSource = source
                )
            }

            composable(
                route = Route.UserProfile.path,
                arguments = listOf(
                    navArgument(Route.UserProfile.ARG_USER_ID) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                com.kidzone.presentation.profile.userprofile.UserProfileScreen(
                    onBack = { navController.popBackStack() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable
                )
            }

            composable(Route.MyPlaces.path) {
                MyPlacesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPlaceDetails = { pid, source ->
                        navController.navigate(Route.PlaceDetails.create(pid, source))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable
                )
            }

            composable(Route.MyReviews.path) {
                MyReviewsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPlaceDetails = { pid, source ->
                        navController.navigate(Route.PlaceDetails.create(pid, source))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable
                )
            }
        }
    }
}
