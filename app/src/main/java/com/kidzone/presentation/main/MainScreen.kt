package com.kidzone.presentation.main

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.LatLng
import com.kidzone.R
import com.kidzone.navigation.Route
import com.kidzone.presentation.common.NetworkStatus
import com.kidzone.presentation.common.NoInternetBanner
import com.kidzone.presentation.common.rememberNetworkStatus
import com.kidzone.presentation.home.HomeScreen
import com.kidzone.presentation.map.MapScreen
import com.kidzone.presentation.place.list.PlaceListScreen
import com.kidzone.presentation.profile.ProfileScreen
import com.kidzone.presentation.ranking.RankingScreen

private data class BottomNavItem(
    val route: Route,
    val titleRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

/**
 * Główny shell aplikacji po zalogowaniu – zawiera własny [NavHost]
 * z kartami (home, map, list, ranking, profile) i [NavigationBar].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    onOpenPlaceDetails: (placeId: String, source: String?) -> Unit,
    onOpenUserProfile: (userId: String) -> Unit,
    onOpenAddPlace: () -> Unit,
    onOpenMyPlaces: () -> Unit,
    onOpenMyReviews: () -> Unit,
    onSignOut: () -> Unit,
    focusLatitude: Double? = null,
    focusLongitude: Double? = null,
    focusTab: String = "",
    rankingTab: String = "",
    onFocusConsumed: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val context = LocalContext.current
    val networkStatus by rememberNetworkStatus()

    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ ->
        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(focusLatitude, focusLongitude) {
        if (focusLatitude != null && focusLongitude != null) {
            navController.navigate(Route.Map.path) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(focusTab) {
        if (focusTab.isNotBlank()) {
            navController.navigate(focusTab) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val navItems = listOf(
        BottomNavItem(Route.Home, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
        BottomNavItem(Route.Map, R.string.nav_map, Icons.Outlined.Map, Icons.Filled.Map),
        BottomNavItem(Route.PlaceList, R.string.nav_list, Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Filled.List),
        BottomNavItem(Route.Ranking, R.string.nav_ranking, Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents),
        BottomNavItem(Route.Profile, R.string.nav_profile, Icons.Outlined.Person, Icons.Filled.Person)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    val selected = currentRoute?.let { r ->
                        backStackEntry?.destination?.hierarchy?.any { it.route == item.route.path }
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route.path) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.icon,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(item.titleRes)) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Route.Home.path || currentRoute == Route.Map.path || currentRoute == Route.PlaceList.path) {
                FloatingActionButton(
                    onClick = onOpenAddPlace,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_place),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(padding)) {
            if (networkStatus == NetworkStatus.UNAVAILABLE) {
                NoInternetBanner()
            }
            NavHost(
                navController = navController,
                startDestination = Route.Home.path,
                modifier = Modifier.weight(1f)
            ) {
                composable(Route.Home.path) {
                    HomeScreen(
                        onOpenPlaceDetails = onOpenPlaceDetails,
                        onOpenMap = {
                            navController.navigate(Route.Map.path) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = this@composable
                    )
                }
                composable(Route.Map.path) {
                    MapScreen(
                        onOpenPlaceDetails = { pid -> onOpenPlaceDetails(pid, "map") },
                        focusOn = if (focusLatitude != null && focusLongitude != null) {
                            LatLng(focusLatitude, focusLongitude)
                        } else null,
                        onFocusConsumed = onFocusConsumed,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = this@composable
                    )
                }
                composable(Route.PlaceList.path) {
                    PlaceListScreen(
                        onOpenPlaceDetails = onOpenPlaceDetails,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = this@composable
                    )
                }
                composable(Route.Ranking.path) {
                    RankingScreen(
                        onOpenPlaceDetails = onOpenPlaceDetails,
                        onOpenUserProfile = onOpenUserProfile,
                        initialTab = rankingTab,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = this@composable
                    )
                }
                composable(Route.Profile.path) {
                    ProfileScreen(
                        onOpenMyPlaces = onOpenMyPlaces,
                        onOpenMyReviews = onOpenMyReviews,
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }
}
