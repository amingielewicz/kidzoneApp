package com.playground.presentation.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.playground.R
import com.playground.navigation.Route
import com.playground.presentation.home.HomeScreen
import com.playground.presentation.map.MapScreen
import com.playground.presentation.place.list.PlaceListScreen
import com.playground.presentation.profile.ProfileScreen
import com.playground.presentation.ranking.RankingScreen

/**
 * Główny shell aplikacji po zalogowaniu – zawiera własny [NavHost]
 * z kartami (home, map, list, ranking, profile) i [NavigationBar].
 *
 * Otwarcie ekranów stackowych (szczegóły, dodawanie miejsca) lub wylogowanie
 * jest delegowane do rodzica przez callbacki.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenPlaceDetails: (placeId: String) -> Unit,
    onOpenAddPlace: () -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
        bottomBar = {
            NavigationBar {
                BottomTab.entries.forEach { tab ->
                    val selected = currentRoute == tab.route.path
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(tab.route.path) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
                // wskazówka by wykorzystać `hierarchy` (dla zagnieżdżonych grafów w przyszłości)
                @Suppress("UNUSED_EXPRESSION")
                backStackEntry?.destination?.hierarchy
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenAddPlace) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_place))
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.path,
            modifier = Modifier.padding(padding)
        ) {
            composable(Route.Home.path) {
                HomeScreen(
                    onOpenPlaceDetails = onOpenPlaceDetails,
                    onOpenMap = {
                        navController.navigate(Route.Map.path) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Route.Map.path) {
                MapScreen(onOpenPlaceDetails = onOpenPlaceDetails)
            }
            composable(Route.PlaceList.path) {
                PlaceListScreen(onOpenPlaceDetails = onOpenPlaceDetails)
            }
            composable(Route.Ranking.path) {
                RankingScreen(onOpenPlaceDetails = onOpenPlaceDetails)
            }
            composable(Route.Profile.path) {
                ProfileScreen(onSignOut = onSignOut)
            }
        }
    }
}

private enum class BottomTab(
    val route: Route,
    val icon: ImageVector,
    @StringRes val labelRes: Int
) {
    Home(Route.Home, Icons.Filled.Home, R.string.nav_home),
    Map(Route.Map, Icons.Filled.Place, R.string.nav_map),
    List(Route.PlaceList, Icons.AutoMirrored.Filled.List, R.string.nav_list),
    Ranking(Route.Ranking, Icons.Filled.EmojiEvents, R.string.nav_ranking),
    Profile(Route.Profile, Icons.Filled.Person, R.string.nav_profile);
}
