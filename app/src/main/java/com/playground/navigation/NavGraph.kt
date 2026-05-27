package com.playground.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.playground.presentation.auth.LoginScreen
import com.playground.presentation.auth.RegisterScreen
import com.playground.presentation.main.MainScreen
import com.playground.presentation.place.add.AddPlaceScreen
import com.playground.presentation.place.details.PlaceDetailsScreen
import com.playground.presentation.splash.SplashScreen

/**
 * Główny graf nawigacji aplikacji – obsługuje przejścia pre-auth oraz
 * pchanie ekranów stackowych ponad shellem [MainScreen].
 */
@Composable
fun PlaygroundNavGraph(
    navController: NavHostController = rememberNavController()
) {
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
                },
                onSignedOut = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
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
                    navController.navigate(Route.Main.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.Main.path) {
            MainScreen(
                onOpenPlaceDetails = { placeId ->
                    navController.navigate(Route.PlaceDetails.create(placeId))
                },
                onOpenAddPlace = { navController.navigate(Route.AddPlace.create()) },
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
                onSaved = {
                    if (isEdit) {
                        navController.popBackStack(Route.Main.path, inclusive = false)
                    } else {
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
    }
}
