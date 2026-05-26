package com.playword.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.playword.presentation.auth.LoginScreen
import com.playword.presentation.auth.RegisterScreen
import com.playword.presentation.main.MainScreen
import com.playword.presentation.place.add.AddPlaceScreen
import com.playword.presentation.place.details.PlaceDetailsScreen
import com.playword.presentation.splash.SplashScreen

/**
 * Główny graf nawigacji aplikacji – obsługuje przejścia pre-auth oraz
 * pchanie ekranów stackowych ponad shellem [MainScreen].
 */
@Composable
fun PlayWordNavGraph(
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
                onOpenAddPlace = { navController.navigate(Route.AddPlace.path) },
                onSignOut = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.Main.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.AddPlace.path) {
            AddPlaceScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.PlaceDetails.path,
            arguments = listOf(
                navArgument(Route.PlaceDetails.ARG_PLACE_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val placeId = backStackEntry.arguments
                ?.getString(Route.PlaceDetails.ARG_PLACE_ID)
                .orEmpty()
            PlaceDetailsScreen(
                placeId = placeId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
