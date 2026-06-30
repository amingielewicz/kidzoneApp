package com.kidzone.presentation.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
@Composable
fun MainScreen(
    onOpenPlaceDetails: (placeId: String, source: String?) -> Unit,
    onOpenAddPlace: () -> Unit,
    onOpenMyPlaces: () -> Unit,
    onOpenMyReviews: () -> Unit,
    onSignOut: () -> Unit,
    focusLatitude: Double? = null,
    focusLongitude: Double? = null,
    focusTab: String = "",
    rankingTab: String = "",
    profileSection: String = "",
    onFocusConsumed: () -> Unit = {},
    onLocaleChanged: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val context = LocalContext.current
    val networkStatus by rememberNetworkStatus()
    val showExtendedAddPlaceFab = currentRoute in setOf(
        Route.Home.path,
        Route.Map.path,
        Route.PlaceList.path
    )

    var locationPermissionGranted by remember {
        mutableStateOf(hasRuntimePermission(context, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    var notificationPermissionGranted by remember {
        mutableStateOf(hasNotificationPermission(context))
    }
    var locationRationaleDismissed by remember { mutableStateOf(false) }
    var notificationRationaleDismissed by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationPermissionGranted = granted
        locationRationaleDismissed = granted
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
        notificationRationaleDismissed = granted
    }

    val showNotificationRationale = !notificationPermissionGranted && !notificationRationaleDismissed
    val showLocationRationale = !showNotificationRationale &&
        !locationPermissionGranted &&
        !locationRationaleDismissed

    // Lokalny stan przekazywany dalej do MapScreen. Trzymamy go obok sygnału
    // z parent NavGraph, bo `onFocusConsumed()` od razu wyczyści savedStateHandle,
    // a my chcemy, by MapScreen otrzymał współrzędne i sam je skonsumował, gdy
    // zakończy animację kamery.
    var pendingMapFocus by remember { mutableStateOf<LatLng?>(null) }

    // Rejestruj FCM token po zalogowaniu – Application.onCreate() może
    // nie mieć uid (cold start bez sesji). Tu user jest na pewno zalogowany.
    LaunchedEffect(Unit) {
        com.kidzone.messaging.KidZoneMessagingService.registerCurrentToken(context)
    }

    // Deep link: przełączenie na konkretną zakładkę (profile, ranking, map)
    LaunchedEffect(focusTab) {
        if (focusTab.isNotBlank()) {
            navController.navigate(focusTab) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onFocusConsumed()
        }
    }

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
            Toast.makeText(context, context.getString(R.string.place_added_success), Toast.LENGTH_SHORT).show()
            onFocusConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            })
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
            if (showExtendedAddPlaceFab) {
                ExtendedFloatingActionButton(
                    onClick = onOpenAddPlace,
                    icon = {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    },
                    text = {
                        Text(stringResource(R.string.add_place))
                    }
                )
            } else {
                FloatingActionButton(onClick = onOpenAddPlace) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_place))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedVisibility(
                visible = networkStatus == NetworkStatus.UNAVAILABLE,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                NoInternetBanner()
            }
            if (showNotificationRationale) {
                PermissionRationaleBanner(
                    rationale = PermissionRationale(
                        title = stringResource(R.string.notification_permission_title),
                        message = stringResource(R.string.notification_permission_message),
                        primaryActionLabel = stringResource(R.string.enable)
                    ),
                    onPrimaryAction = {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onDismiss = { notificationRationaleDismissed = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (showLocationRationale) {
                PermissionRationaleBanner(
                    rationale = PermissionRationale(
                        title = stringResource(R.string.location_permission_title),
                        message = stringResource(R.string.location_permission_message),
                        primaryActionLabel = stringResource(R.string.allow)
                    ),
                    onPrimaryAction = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    onDismiss = { locationRationaleDismissed = true },
                    modifier = Modifier.fillMaxWidth()
                )
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
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        locationPermissionGranted = locationPermissionGranted,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope
                    )
                }
                composable(Route.Map.path) {
                    MapScreen(
                        onOpenPlaceDetails = { onOpenPlaceDetails(it, null) },
                        focusOn = pendingMapFocus,
                        locationPermissionGrantedSignal = locationPermissionGranted,
                        onFocusConsumed = { pendingMapFocus = null }
                    )
                }
                composable(Route.PlaceList.path) {
                    PlaceListScreen(
                        onOpenPlaceDetails = onOpenPlaceDetails,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope
                    )
                }
                composable(Route.Ranking.path) {
                    RankingScreen(
                        onOpenPlaceDetails = onOpenPlaceDetails,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope
                    )
                }
                composable(Route.Profile.path) {
                    ProfileScreen(
                        onSignOut = onSignOut,
                        onOpenMyPlaces = onOpenMyPlaces,
                        onOpenMyReviews = onOpenMyReviews,
                        scrollToSection = profileSection,
                        onLocaleChanged = onLocaleChanged
                    )
                }
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

private data class PermissionRationale(
    val title: String,
    val message: String,
    val primaryActionLabel: String
)

@Suppress("FunctionNaming")
@Composable
private fun PermissionRationaleBanner(
    rationale: PermissionRationale,
    onPrimaryAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Polite
        },
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = rationale.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = rationale.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.later))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onPrimaryAction) {
                    Text(rationale.primaryActionLabel)
                }
            }
        }
    }
}

private fun hasRuntimePermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        hasRuntimePermission(context, Manifest.permission.POST_NOTIFICATIONS)
