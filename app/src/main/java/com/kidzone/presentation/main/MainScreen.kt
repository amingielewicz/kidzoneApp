package com.kidzone.presentation.main

import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.LatLng
import com.kidzone.R
import com.kidzone.navigation.Route
import com.kidzone.presentation.home.HomeScreen
import com.kidzone.presentation.map.MapScreen
import com.kidzone.presentation.place.list.PlaceListScreen
import com.kidzone.presentation.profile.ProfileScreen
import com.kidzone.presentation.ranking.RankingScreen

/**
 * Główny shell aplikacji po zalogowaniu – zawiera własny [NavHost]
 * z kartami (home, map, list, ranking, profile) i [NavigationBar].
 *
 * Otwarcie ekranów stackowych (szczegóły, dodawanie miejsca) lub wylogowanie
 * jest delegowane do rodzica przez callbacki.
 *
 * @param focusLatitude / [focusLongitude] – jeśli niepuste, ekran przełączy
 *   się na zakładkę "Mapa" i wycentruje kamerę na tych współrzędnych.
 *   Wykorzystywane po pomyślnym dodaniu nowego miejsca przez [AddPlaceScreen]
 *   (parent NavGraph wstrzykuje wartości przez `savedStateHandle`).
 * @param onFocusConsumed wywołane raz po skonsumowaniu sygnału (czyści
 *   savedStateHandle, żeby kolejne wejście na ten ekran bez nowego dodawania
 *   nie odpalało powtórnie nawigacji).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenPlaceDetails: (placeId: String) -> Unit,
    onOpenAddPlace: () -> Unit,
    onSignOut: () -> Unit,
    focusLatitude: Double? = null,
    focusLongitude: Double? = null,
    onFocusConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val context = LocalContext.current

    // Lokalny stan przekazywany dalej do MapScreen. Trzymamy go obok sygnału
    // z parent NavGraph, bo `onFocusConsumed()` od razu wyczyści savedStateHandle,
    // a my chcemy, by MapScreen otrzymał współrzędne i sam je skonsumował, gdy
    // zakończy animację kamery.
    var pendingMapFocus by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(focusLatitude, focusLongitude) {
        if (focusLatitude != null && focusLongitude != null) {
            pendingMapFocus = LatLng(focusLatitude, focusLongitude)
            // Przełącz na zakładkę Map z pełną semantyką bottom-nav (saveState /
            // restoreState), żeby zachowanie kart pozostało spójne z klikaniem
            // ich ręcznie.
            navController.navigate(Route.Map.path) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            Toast.makeText(context, "Dodano nowe miejsce", Toast.LENGTH_SHORT).show()
            onFocusConsumed()
        }
    }

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
                MapScreen(
                    onOpenPlaceDetails = onOpenPlaceDetails,
                    focusOn = pendingMapFocus,
                    onFocusConsumed = { pendingMapFocus = null }
                )
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
