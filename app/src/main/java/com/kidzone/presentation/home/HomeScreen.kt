@file:Suppress("FunctionNaming", "LongMethod", "LongParameterList", "TooManyFunctions")

package com.kidzone.presentation.home

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kidzone.R
import com.kidzone.domain.model.Place
import com.kidzone.presentation.common.style
import com.kidzone.presentation.common.CategoryIcon
import com.kidzone.presentation.common.KidZoneRadii
import com.kidzone.presentation.common.KidZoneSpacing
import com.kidzone.presentation.common.NewPlaceBadge
import com.kidzone.presentation.common.isNewWithoutReviews
import com.kidzone.presentation.common.rememberLocationServiceEnabled
import com.kidzone.presentation.common.shimmerEffect

private val PLACE_ROW_HEIGHT = 168.dp
private val PLACE_CARD_WIDTH = 178.dp
private val PLACE_CARD_ICON_SIZE = 30.dp
private val PLACE_CARD_CONTENT_PADDING = 14.dp
private val PLACE_NAME_BLOCK_HEIGHT = 40.dp
private val PLACE_STATUS_HEIGHT = 24.dp
private val PLACE_CARD_MAIN_GAP = 10.dp
private val PLACE_CARD_STATUS_GAP = 6.dp
private val LOCATION_PANEL_MIN_HEIGHT = 360.dp
private val LOCATION_CTA_HEIGHT = 48.dp
private val HOME_HORIZONTAL_PADDING = 16.dp
private const val MANUAL_CITY_HINT = "Wybierz miasto ręcznie"
private const val GPS_STATUS_MESSAGE = "Nie widzimy Twojej lokalizacji. Włącz GPS, aby zobaczyć atrakcje w pobliżu."
private const val GPS_SECTION_EMPTY_MESSAGE = "Włącz lokalizację, aby zobaczyć, co polecają inni rodzice."
private const val ENABLE_GPS = "Włącz GPS"
private const val CHECKING_GPS = "Trwa sprawdzanie..."
private const val WAVE_EMOJI = "👋"
private const val WAVE_INITIAL_ROTATION = -12f
private const val WAVE_TARGET_ROTATION = 16f
private const val WAVE_TRANSFORM_ORIGIN_X = 0.8f
private const val WAVE_TRANSFORM_ORIGIN_Y = 0.8f
private const val WAVE_DURATION_MS = 650
private const val NEARBY_SECTION_TITLE = "📍 W pobliżu"
private const val TOP_SECTION_TITLE = "🏆 Najpopularniejsze"
private const val RECENT_SECTION_TITLE = "🆕 Nowości w okolicy"
private const val VERY_CLOSE_DISTANCE_LABEL = "Tuż obok"
private const val VERY_CLOSE_DISTANCE_KM = 0.05
private const val METER_DISTANCE_THRESHOLD_KM = 1.0
private const val METERS_PER_KILOMETER = 1000
private const val DISTANCE_ROUNDING_OFFSET_METERS = 25
private const val DISTANCE_ROUNDING_STEP_METERS = 50
private const val INTEGER_DISTANCE_THRESHOLD_KM = 100.0
private const val EMPTY_NEARBY_ICON = "📍"
private const val EMPTY_TOP_ICON = "★"
private const val EMPTY_RECENT_ICON = "NEW"
private const val NO_REVIEWS_LABEL = "Brak ocen"

private data class HomePlaceItem(
    val place: Place,
    val distanceKm: Double? = null,
    val staleLocationAgeMinutes: Int? = null
)

@OptIn(ExperimentalSharedTransitionApi::class)
private data class PlaceCardAnimation(
    val sharedTransitionScope: SharedTransitionScope?,
    val animatedContentScope: AnimatedContentScope?
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Suppress("CyclomaticComplexMethod", "FunctionNaming", "LongMethod", "LongParameterList")
@Composable
fun HomeScreen(
    onOpenPlaceDetails: (placeId: String, source: String?) -> Unit,
    onOpenMap: () -> Unit,
    onRequestLocation: () -> Unit,
    onDismissIntro: () -> Unit,
    showIntro: Boolean = true,
    locationPermissionGranted: Boolean = false,
    locationRefreshSignal: Int = 0,
    viewModel: HomeViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val state by viewModel.uiState.collectAsState()
    val gpsEnabled = rememberLocationServiceEnabled(locationRefreshSignal)
    val networkStatus by com.kidzone.presentation.common.rememberNetworkStatus()
    val hasLocationPermission = locationPermissionGranted || state.locationGranted
    val hasLocationContext = hasLocationPermission && gpsEnabled
    val showGpsDisabledBanner = hasLocationPermission && !gpsEnabled
    val showLocationAwareContent = hasLocationContext || showGpsDisabledBanner
    val staleLocationAgeMinutes = state.staleLocationAgeMinutes.takeIf { state.isUsingStaleLocation }
    var isGpsCheckInProgress by remember { mutableStateOf(false) }

    var previousNetworkStatus by remember { mutableStateOf(networkStatus) }
    LaunchedEffect(networkStatus) {
        if (previousNetworkStatus == com.kidzone.presentation.common.NetworkStatus.UNAVAILABLE &&
            networkStatus == com.kidzone.presentation.common.NetworkStatus.AVAILABLE
        ) {
            viewModel.refresh()
        }
        previousNetworkStatus = networkStatus
    }

    LaunchedEffect(locationPermissionGranted, locationRefreshSignal) {
        if (locationPermissionGranted) {
            viewModel.onLocationPermissionGranted()
        }
        if (locationRefreshSignal > 0) {
            isGpsCheckInProgress = false
        }
    }

    LaunchedEffect(gpsEnabled) {
        if (gpsEnabled) {
            isGpsCheckInProgress = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLocationGranted()
                isGpsCheckInProgress = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var previousGpsEnabled by remember { mutableStateOf(gpsEnabled) }
    LaunchedEffect(gpsEnabled, hasLocationPermission) {
        if (!previousGpsEnabled && gpsEnabled && hasLocationPermission) {
            viewModel.refresh()
        }
        previousGpsEnabled = gpsEnabled
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!showIntro && !hasLocationPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = HOME_HORIZONTAL_PADDING),
                    contentAlignment = Alignment.Center
                ) {
                    HomeLocationEmptyState(
                        onEnableLocationClick = onRequestLocation,
                        onManualCityClick = onOpenMap
                    )
                }
            } else {
                if (showGpsDisabledBanner) {
                    LocationStatusBanner(
                        modifier = Modifier.padding(
                            start = HOME_HORIZONTAL_PADDING,
                            top = 18.dp,
                            end = HOME_HORIZONTAL_PADDING
                        ),
                        isChecking = isGpsCheckInProgress,
                        onEnableGpsClick = {
                            isGpsCheckInProgress = true
                            onRequestLocation()
                        },
                        onManualCityClick = onOpenMap
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(
                        top = when {
                            showGpsDisabledBanner -> 22.dp
                            showIntro -> 0.dp
                            else -> 18.dp
                        },
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    if (showIntro) {
                        item {
                            WelcomeIntroCard(
                                modifier = Modifier.padding(
                                    start = HOME_HORIZONTAL_PADDING,
                                    top = 18.dp,
                                    end = HOME_HORIZONTAL_PADDING
                                ),
                                onDismiss = onDismissIntro
                            )
                        }
                    }

                    if (!hasLocationPermission) {
                        item {
                            HomeLocationEmptyState(
                                modifier = Modifier.padding(horizontal = HOME_HORIZONTAL_PADDING),
                                onEnableLocationClick = onRequestLocation,
                                onManualCityClick = onOpenMap
                            )
                        }
                    }

                    if (showLocationAwareContent) {
                        item {
                            HomeSection(
                                title = NEARBY_SECTION_TITLE,
                                items = if (hasLocationContext) {
                                    state.nearbyPlaces.map {
                                        HomePlaceItem(it.place, it.distanceKm, staleLocationAgeMinutes)
                                    }
                                } else {
                                    emptyList()
                                },
                                isLoading = hasLocationContext && state.isNearbyLoading,
                                emptyMessage = GPS_SECTION_EMPTY_MESSAGE,
                                emptyIcon = EMPTY_NEARBY_ICON,
                                onPlaceClick = { onOpenPlaceDetails(it, "nearby") },
                                animation = PlaceCardAnimation(sharedTransitionScope, animatedContentScope),
                                keyPrefix = "nearby"
                            )
                        }

                        item {
                            HomeSection(
                                title = TOP_SECTION_TITLE,
                                items = if (hasLocationContext) {
                                    state.topPlaces.map {
                                        HomePlaceItem(it.place, it.distanceKm, staleLocationAgeMinutes)
                                    }
                                } else {
                                    emptyList()
                                },
                                isLoading = hasLocationContext && state.isTopLoading,
                                emptyMessage = GPS_SECTION_EMPTY_MESSAGE,
                                emptyIcon = EMPTY_TOP_ICON,
                                onPlaceClick = { onOpenPlaceDetails(it, "top") },
                                animation = PlaceCardAnimation(sharedTransitionScope, animatedContentScope),
                                keyPrefix = "top"
                            )
                        }

                        item {
                            HomeSection(
                                title = RECENT_SECTION_TITLE,
                                items = if (hasLocationContext) {
                                    state.recentlyAddedPlaces.map {
                                        HomePlaceItem(it.place, it.distanceKm, staleLocationAgeMinutes)
                                    }
                                } else {
                                    emptyList()
                                },
                                isLoading = hasLocationContext && state.isRecentlyAddedLoading,
                                emptyMessage = GPS_SECTION_EMPTY_MESSAGE,
                                emptyIcon = EMPTY_RECENT_ICON,
                                onPlaceClick = { onOpenPlaceDetails(it, "recent") },
                                animation = PlaceCardAnimation(sharedTransitionScope, animatedContentScope),
                                keyPrefix = "recent"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming", "LongMethod")
private fun WelcomeIntroCard(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val desc = stringResource(R.string.home_find_nearby_description)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = desc
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 48.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WavingHand()
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_intro_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(26.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Zamknij powitanie",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun WavingHand() {
    val infiniteTransition = rememberInfiniteTransition(label = "home_wave")
    val rotation by infiniteTransition.animateFloat(
        initialValue = WAVE_INITIAL_ROTATION,
        targetValue = WAVE_TARGET_ROTATION,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WAVE_DURATION_MS),
            repeatMode = RepeatMode.Reverse
        ),
        label = "home_wave_rotation"
    )

    Text(
        text = WAVE_EMOJI,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.graphicsLayer {
            rotationZ = rotation
            transformOrigin = TransformOrigin(
                WAVE_TRANSFORM_ORIGIN_X,
                WAVE_TRANSFORM_ORIGIN_Y
            )
        }
    )
}

@Composable
@Suppress("FunctionNaming")
private fun LocationStatusBanner(
    modifier: Modifier = Modifier,
    isChecking: Boolean,
    onEnableGpsClick: () -> Unit,
    onManualCityClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(PLACE_ROW_HEIGHT),
        shape = RoundedCornerShape(KidZoneRadii.Card),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = GPS_STATUS_MESSAGE,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onEnableGpsClick,
                enabled = !isChecking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(CHECKING_GPS, style = MaterialTheme.typography.labelMedium)
                } else {
                    Text(ENABLE_GPS, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = onManualCityClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Text(
                    text = MANUAL_CITY_HINT,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun HomeLocationEmptyState(
    modifier: Modifier = Modifier,
    onEnableLocationClick: () -> Unit,
    onManualCityClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LOCATION_PANEL_MIN_HEIGHT),
        shape = RoundedCornerShape(KidZoneRadii.Card),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LOCATION_PANEL_MIN_HEIGHT)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(58.dp)
                )
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .size(22.dp)
                )
            }
            Text(
                text = stringResource(R.string.home_location_rationale),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onEnableLocationClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LOCATION_CTA_HEIGHT),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_enable_location))
            }
            TextButton(onClick = onManualCityClick) {
                Text(
                    text = MANUAL_CITY_HINT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun HomeSection(
    title: String,
    items: List<HomePlaceItem>,
    isLoading: Boolean,
    emptyMessage: String,
    onPlaceClick: (placeId: String) -> Unit,
    animation: PlaceCardAnimation,
    keyPrefix: String,
    emptyIcon: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = title,
            modifier = Modifier.padding(horizontal = HOME_HORIZONTAL_PADDING)
        )
        HorizontalPlacesRow(
            items = items,
            isLoading = isLoading,
            emptyMessage = emptyMessage,
            emptyIcon = emptyIcon,
            onPlaceClick = onPlaceClick,
            animation = animation,
            keyPrefix = keyPrefix
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun HorizontalPlacesRow(
    items: List<HomePlaceItem>,
    isLoading: Boolean,
    emptyMessage: String,
    onPlaceClick: (placeId: String) -> Unit,
    animation: PlaceCardAnimation,
    keyPrefix: String = "",
    emptyIcon: String? = null
) {
    val rowHeight = PLACE_ROW_HEIGHT
    when {
        isLoading -> {
            LazyRow(
                modifier = Modifier.height(rowHeight),
                contentPadding = PaddingValues(horizontal = HOME_HORIZONTAL_PADDING),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                userScrollEnabled = false
            ) {
                items(5) {
                    PlaceCardSkeleton()
                }
            }
        }
        items.isEmpty() -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(horizontal = HOME_HORIZONTAL_PADDING),
                shape = RoundedCornerShape(KidZoneRadii.Card),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    emptyIcon?.let {
                        EmptyStateIcon(icon = it)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        else -> {
            LazyRow(
                modifier = Modifier.height(rowHeight),
                contentPadding = PaddingValues(horizontal = HOME_HORIZONTAL_PADDING),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(items, key = { "${keyPrefix}_${it.place.id}" }) { item ->
                    PlaceCard(
                        item = item,
                        onClick = { onPlaceClick(item.place.id) },
                        animation = animation,
                        keyPrefix = keyPrefix
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateIcon(icon: String) {
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    if (icon == EMPTY_TOP_ICON) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
            contentColor = contentColor
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    } else {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PlaceCard(
    item: HomePlaceItem,
    onClick: () -> Unit,
    animation: PlaceCardAnimation,
    keyPrefix: String = ""
) {
    val place = item.place
    val categoryLabel = stringResource(place.category.labelRes)
    val distanceLabel = item.distanceKm?.let {
        ", ${stringResource(R.string.distance_away, formatDistance(it, item.staleLocationAgeMinutes))}"
    }.orEmpty()
    val ratingLabel = place.ratingAccessibilityLabel()
    val animationKey = if (keyPrefix.isBlank()) "" else "${keyPrefix}_"

    Card(
        modifier = Modifier
            .width(PLACE_CARD_WIDTH)
            .height(PLACE_ROW_HEIGHT)
            .semantics(mergeDescendants = true) {
                contentDescription = "${place.name}, $categoryLabel$distanceLabel, $ratingLabel"
            }
            .clickable(
                onClickLabel = stringResource(R.string.map_open_place_details_label),
                role = Role.Button,
                onClick = onClick
            ),
        shape = RoundedCornerShape(KidZoneRadii.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PLACE_CARD_CONTENT_PADDING)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(
                    category = place.category,
                    animationKey = "${animationKey}place_icon_${place.id}",
                    sharedTransitionScope = animation.sharedTransitionScope,
                    animatedContentScope = animation.animatedContentScope,
                    size = PLACE_CARD_ICON_SIZE,
                    iconSize = 18.dp
                )
                Spacer(Modifier.width(KidZoneSpacing.GapSmall))
                CategoryFilledBadge(
                    category = place.category,
                    label = categoryLabel,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(PLACE_CARD_MAIN_GAP))
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.height(PLACE_NAME_BLOCK_HEIGHT),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(PLACE_CARD_MAIN_GAP))
            item.distanceKm?.let {
                DistanceLabel(
                    distanceKm = it,
                    staleLocationAgeMinutes = item.staleLocationAgeMinutes
                )
            }
            Spacer(Modifier.height(PLACE_CARD_STATUS_GAP))
            PlaceRatingStatus(place = place)
        }
    }
}

@Composable
private fun PlaceRatingStatus(place: Place) {
    when {
        place.reviewsCount > 0 -> {
            Row(
                modifier = Modifier.height(PLACE_STATUS_HEIGHT),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.rating),
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(KidZoneSpacing.GapTiny))
                Text(
                    text = "%.1f (%d)".format(place.averageRating, place.reviewsCount),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        place.isNewWithoutReviews() -> NewPlaceBadge()
        else -> {
            Row(
                modifier = Modifier.height(PLACE_STATUS_HEIGHT),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.rating),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(KidZoneSpacing.GapTiny))
                Text(
                    text = NO_REVIEWS_LABEL,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@Composable
private fun CategoryFilledBadge(
    category: com.kidzone.domain.model.PlaceCategory,
    label: String,
    modifier: Modifier = Modifier
) {
    val style = category.style

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = style.color.copy(alpha = 0.10f),
        contentColor = style.color
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DistanceLabel(
    distanceKm: Double,
    staleLocationAgeMinutes: Int?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = formatDistance(distanceKm, staleLocationAgeMinutes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun formatDistance(km: Double, staleLocationAgeMinutes: Int? = null): String {
    val distance = when {
        km < VERY_CLOSE_DISTANCE_KM -> VERY_CLOSE_DISTANCE_LABEL
        km < METER_DISTANCE_THRESHOLD_KM -> {
            val meters = (km * METERS_PER_KILOMETER).toInt()
            val rounded = (
                (meters + DISTANCE_ROUNDING_OFFSET_METERS) /
                    DISTANCE_ROUNDING_STEP_METERS
                ) * DISTANCE_ROUNDING_STEP_METERS
            if (rounded == 0) VERY_CLOSE_DISTANCE_LABEL else stringResource(R.string.distance_m, rounded)
        }
        km < INTEGER_DISTANCE_THRESHOLD_KM -> stringResource(R.string.distance_km, km)
        else -> stringResource(R.string.distance_km_integer, km.toInt())
    }
    return staleLocationAgeMinutes?.let { "$distance (${staleAgeLabel(it)})" } ?: distance
}

private fun staleAgeLabel(ageMinutes: Int): String = when {
    ageMinutes <= 1 -> "1 min temu"
    else -> "$ageMinutes min temu"
}

@Composable
private fun Place.ratingAccessibilityLabel(): String = when {
    reviewsCount > 0 -> stringResource(R.string.rating_accessibility_label, averageRating, reviewsCount)
    isNewWithoutReviews() -> stringResource(R.string.new_place_no_reviews)
    else -> stringResource(R.string.map_no_reviews)
}

@Composable
private fun PlaceCardSkeleton() {
    Card(
        modifier = Modifier
            .width(PLACE_CARD_WIDTH)
            .height(PLACE_ROW_HEIGHT),
        shape = RoundedCornerShape(KidZoneRadii.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(PLACE_CARD_CONTENT_PADDING)
                    .clip(RoundedCornerShape(KidZoneRadii.Control))
                    .shimmerEffect()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PLACE_CARD_CONTENT_PADDING),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(16.dp)
                            .clip(MaterialTheme.shapes.small)
                            .shimmerEffect()
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(12.dp)
                            .clip(MaterialTheme.shapes.small)
                            .shimmerEffect()
                    )
                }
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(12.dp)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect()
                )
            }
        }
    }
}
