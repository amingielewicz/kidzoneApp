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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.kidzone.presentation.common.NotificationPromptReason
import com.kidzone.presentation.common.NotificationSoftPromptDialog
import com.kidzone.presentation.common.SystemStatusIcons
import com.kidzone.presentation.common.rememberLocationServiceEnabled
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
private const val KEY_ADD_PLACE_FAB_LABEL_USED = "add_place_fab_label_used"
private const val LOCATION_REQUEST_INTERVAL_MS = 10_000L
private const val LOCATION_REQUEST_MIN_INTERVAL_MS = 5_000L

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
        mutableStateOf(
            !prefs.getBoolean(KEY_HOME_INTRO_USED, false) &&
                !hasRuntimePermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        )
    }
    var showAddPlaceFabLabel by remember {
        mutableStateOf(!prefs.getBoolean(KEY_ADD_PLACE_FAB_LABEL_USED, false))
    }
    var notificationPromptReason by remember {
        mutableStateOf<NotificationPromptReason?>(null)
    }
    var locationPermissionGranted by remember {
        mutableStateOf(hasRuntimePermission(context, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    var locationRefreshSignal by remember { mutableIntStateOf(0) }
    val locationServiceEnabled = rememberLocationServiceEnabled(locationRefreshSignal)

    fun markHomeIntroUsed() {
        if (showHomeIntro) {
            showHomeIntro = false
            prefs.edit().putBoolean(KEY_HOME_INTRO_USED, true).apply()
        }
    }

    fun openAddPlaceFromFab() {
        if (showAddPlaceFabLabel) {
            showAddPlaceFabLabel = false
            prefs.edit().putBoolean(KEY_ADD_PLACE_FAB_LABEL_USED, true).apply()
        }
        onOpenAddPlace()
    }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) {
        locationPermissionGranted = hasRuntimePermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (locationPermissionGranted) {
            markHomeIntroUsed()
            locationRefreshSignal += 1
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationPermissionGranted = granted
        if (granted) {
            markHomeIntroUsed()
            context.checkLocationSettings(
                onResolutionRequired = { request -> locationSettingsLauncher.launch(request) },
                onFallbackToSettings = { context.openLocationSettings() }
            )
            locationRefreshSignal += 1
        }
    }
    val showAddPlaceFab = currentRoute in setOf(
        Route.Home.path,
        Route.Map.path,
        Route.PlaceList.path
    )

    var pendingMapFocus by remember { mutableStateOf<LatLng?>(null) }
    var pendingRankingTab by remember { mutableStateOf(rankingTab) }

    fun requestLocationFromHome() {
        if (hasRuntimePermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            locationPermissionGranted = true
            markHomeIntroUsed()
            context.checkLocationSettings(
                onResolutionRequired = { request -> locationSettingsLauncher.launch(request) },
                onFallbackToSettings = { context.openLocationSettings() }
            )
            locationRefreshSignal += 1
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun openMapFromHome() {
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

    LaunchedEffect(Unit) {
        com.kidzone.messaging.KidZoneMessagingService.registerCurrentToken(context)
    }

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
            TopAppBar(
                title = {
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
                },
                actions = {
                    SystemStatusIcons(
                        isNetworkAvailable = networkStatus == NetworkStatus.AVAILABLE,
                        isLocationAvailable = locationPermissionGranted && locationServiceEnabled,
                        onNetworkClick = {
                            Toast.makeText(
                                context,
                                "Brak internetu. Sprawdź połączenie sieciowe.",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onLocationClick = { requestLocationFromHome() }
                    )
                }
            )
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
                @Suppress("UNUSED_EXPRESSION")
                backStackEntry?.destination?.hierarchy
            }
        },
        floatingActionButton = {
            if (showAddPlaceFab) {
                if (showAddPlaceFabLabel) {
                    ExtendedFloatingActionButton(
                        onClick = ::openAddPlaceFromFab,
                        modifier = Modifier.padding(bottom = 16.dp),
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
                } else {
                    FloatingActionButton(
                        onClick = ::openAddPlaceFromFab,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_place)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                        onDismissIntro = ::markHomeIntroUsed,
                        showIntro = showHomeIntro,
                        locationPermissionGranted = locationPermissionGranted,
                        locationRefreshSignal = locationRefreshSignal,
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
