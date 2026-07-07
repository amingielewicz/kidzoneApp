package com.kidzone.presentation.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.kidzone.R
import com.kidzone.navigation.Route
import com.kidzone.presentation.common.NetworkStatus
import com.kidzone.presentation.common.NoInternetBanner
import com.kidzone.presentation.common.NotificationPromptReason
import com.kidzone.presentation.common.NotificationSoftPromptDialog
import com.kidzone.presentation.common.rememberNetworkStatus
import com.kidzone.presentation.common.shouldShowNotificationPrompt
import com.kidzone.presentation.home.HomeScreen
import com.kidzone.presentation.map.MapScreen
import com.kidzone.presentation.place.add.isLocationServiceEnabled
import com.kidzone.presentation.place.list.PlaceListScreen
import com.kidzone.presentation.profile.ProfileScreen
import com.kidzone.presentation.ranking.RankingScreen

private const val MAIN_UI_PREFS = "main_ui_prefs"
private const val KEY_HOME_INTRO_USED = "home_intro_used"
private const val LOCATION_REQUEST_INTERVAL_MS = 10_000L
private const val LOCATION_REQUEST_MIN_INTERVAL_MS = 5_000L

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
    val prefs = remember(context) {
        context.getSharedPreferences(MAIN_UI_PREFS, Context.MODE_PRIVATE)
    }
    val networkStatus by rememberNetworkStatus()
    var showHomeIntro by remember {
        mutableStateOf(!prefs.getBoolean(KEY_HOME_INTRO_USED, false))
    }
    var notificationPromptReason by remember {
        mutableStateOf<NotificationPromptReason?>(null)
    }
    var locationPermissionGranted by remember {
        mutableStateOf(hasRuntimePermission(context, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) {
        locationPermissionGranted = hasRuntimePermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationPermissionGranted = granted
        if (granted) {
            context.checkLocationSettings(
                onResolutionRequired = { request -> locationSettingsLauncher.launch(request) },
                onFallbackToSettings = { context.openLocationSettings() }
            )
        }
    }
    val showAddPlaceFab = currentRoute in setOf(
        Route.Home.path,
        Route.Map.path,
        Route.PlaceList.path
    )

    // Lokalny stan przekazywany dalej do MapScreen. Trzymamy go obok sygnału
    // z parent NavGraph, bo `onFocusConsumed()` od razu wyczyści savedStateHandle,
    // a my chcemy, by MapScreen otrzymał współrzędne i sam je skonsumował, gdy
    // zakończy animację kamery.
    var pendingMapFocus by remember { mutableStateOf<LatLng?>(null) }
    var pendingRankingTab by remember { mutableStateOf(rankingTab) }

    fun markHomeIntroUsed() {
        if (showHomeIntro) {
            showHomeIntro = false
            prefs.edit().putBoolean(KEY_HOME_INTRO_USED, true).apply()
        }
    }

    fun markHomeIntroUsedWhenLeavingHome() {
        if (currentRoute == Route.Home.path) {
            markHomeIntroUsed()
        }
    }

    fun requestLocationFromHome() {
        if (hasRuntimePermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            locationPermissionGranted = true
            context.checkLocationSettings(
                onResolutionRequired = { request -> locationSettingsLauncher.launch(request) },
                onFallbackToSettings = { context.openLocationSettings() }
            )
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun openMapFromHome() {
        markHomeIntroUsedWhenLeavingHome()
        navController.navigate(Route.Map.path) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(rankingTab) {
        if (rankingTab.isNotBlank()) {
            pendingRankingTab = rankingTab
        }
    }

    // Rejestruj FCM token po zalogowaniu – Application.onCreate() może
    // nie mieć uid (cold start bez sesji). Tu user jest na pewno zalogowany.
    LaunchedEffect(Unit) {
        com.kidzone.messaging.KidZoneMessagingService.registerCurrentToken(context)
    }

    // Deep link: przełączenie na konkretną zakładkę (profile, ranking, map)
    LaunchedEffect(focusTab) {
        if (focusTab.isNotBlank()) {
            if (focusTab != Route.Home.path) {
                markHomeIntroUsedWhenLeavingHome()
            }
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
            markHomeIntroUsedWhenLeavingHome()
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
            if (shouldShowNotificationPrompt(context, NotificationPromptReason.FirstPlace)) {
                notificationPromptReason = NotificationPromptReason.FirstPlace
            }
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
                        modifier = Modifier.size(52.dp)
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
                                if (tab.route != Route.Home) {
                                    markHomeIntroUsedWhenLeavingHome()
                                }
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
            if (showAddPlaceFab) {
                ExtendedFloatingActionButton(
                    onClick = onOpenAddPlace,
                    modifier = Modifier.height(48.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.add_place),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
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
            NavHost(
                navController = navController,
                startDestination = Route.Home.path,
                modifier = Modifier.weight(1f)
            ) {
                composable(Route.Home.path) {
                    HomeScreen(
                        onOpenPlaceDetails = onOpenPlaceDetails,
                        onOpenMap = ::openMapFromHome,
                        onRequestLocation = ::requestLocationFromHome,
                        showIntro = showHomeIntro,
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
                        initialTab = pendingRankingTab,
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

    notificationPromptReason?.let { reason ->
        NotificationSoftPromptDialog(
            reason = reason,
            onDismiss = { notificationPromptReason = null }
        )
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

private fun hasRuntimePermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.checkLocationSettings(
    onResolutionRequired: (IntentSenderRequest) -> Unit,
    onFallbackToSettings: () -> Unit
) {
    if (isLocationServiceEnabled(this)) return

    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        LOCATION_REQUEST_INTERVAL_MS
    )
        .setMinUpdateIntervalMillis(LOCATION_REQUEST_MIN_INTERVAL_MS)
        .build()
    val settingsRequest = LocationSettingsRequest.Builder()
        .addLocationRequest(locationRequest)
        .setAlwaysShow(true)
        .build()

    LocationServices.getSettingsClient(this)
        .checkLocationSettings(settingsRequest)
        .addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    onResolutionRequired(
                        IntentSenderRequest.Builder(exception.resolution).build()
                    )
                } catch (_: IntentSender.SendIntentException) {
                    onFallbackToSettings()
                }
            } else {
                onFallbackToSettings()
            }
        }
}

private fun Context.openLocationSettings() {
    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
}
