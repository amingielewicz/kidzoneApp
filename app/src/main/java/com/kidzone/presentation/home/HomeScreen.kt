package com.kidzone.presentation.home

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kidzone.R
import com.kidzone.domain.model.Place
import com.kidzone.presentation.common.CategoryBadge
import com.kidzone.presentation.common.CategoryIcon
import com.kidzone.presentation.common.KidZoneCard
import com.kidzone.presentation.common.KidZoneRadii
import com.kidzone.presentation.common.KidZoneSpacing
import com.kidzone.presentation.common.NewPlaceBadge
import com.kidzone.presentation.common.isNewWithoutReviews
import com.kidzone.presentation.common.rememberLocationServiceEnabled
import com.kidzone.presentation.common.shimmerEffect

private val PLACE_ROW_HEIGHT = 164.dp
private val PLACE_CARD_WIDTH = 176.dp
private val PLACE_CARD_ICON_SIZE = 30.dp
private val PLACE_CARD_CONTENT_PADDING = 12.dp
private const val MANUAL_CITY_HINT = "Nie chcesz używać GPS? Kliknij tutaj, aby wybrać miasto ręcznie"

private data class HomePlaceItem(
    val place: Place,
    val distanceKm: Double? = null
)

@OptIn(ExperimentalSharedTransitionApi::class)
private data class PlaceCardAnimation(
    val sharedTransitionScope: SharedTransitionScope?,
    val animatedContentScope: AnimatedContentScope?
)

/**
 * Ekran "Start" – pierwsza zakładka po zalogowaniu.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Suppress("CyclomaticComplexMethod", "FunctionNaming", "LongMethod", "LongParameterList")
@Composable
fun HomeScreen(
    onOpenPlaceDetails: (placeId: String, source: String?) -> Unit,
    onOpenMap: () -> Unit,
    onRequestLocation: () -> Unit,
    showIntro: Boolean = true,
    locationPermissionGranted: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val state by viewModel.uiState.collectAsState()
    val gpsEnabled = rememberLocationServiceEnabled()
    val networkStatus by com.kidzone.presentation.common.rememberNetworkStatus()
    val hasLocationContext = locationPermissionGranted || state.locationGranted

    var previousNetworkStatus by remember { mutableStateOf(networkStatus) }
    LaunchedEffect(networkStatus) {
        if (previousNetworkStatus == com.kidzone.presentation.common.NetworkStatus.UNAVAILABLE &&
            networkStatus == com.kidzone.presentation.common.NetworkStatus.AVAILABLE
        ) {
            viewModel.refresh()
        }
        previousNetworkStatus = networkStatus
    }

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            viewModel.onLocationPermissionGranted()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLocationGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var previousGpsEnabled by remember { mutableStateOf(gpsEnabled) }
    LaunchedEffect(gpsEnabled, state.locationGranted) {
        if (!previousGpsEnabled && gpsEnabled && state.locationGranted) {
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showIntro) {
                    item {
                        WelcomeIntroCard(
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                        )
                    }
                }

                if (!hasLocationContext) {
                    item {
                        HomeLocationEmptyState(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onEnableLocationClick = onRequestLocation,
                            onManualCityClick = onOpenMap
                        )
                    }
                }

                if (hasLocationContext) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.home_nearby_places),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item {
                        HorizontalPlacesRow(
                            items = state.nearbyPlaces.map {
                                HomePlaceItem(it.place, it.distanceKm)
                            },
                            isLoading = state.isNearbyLoading,
                            emptyMessage = stringResource(R.string.home_no_nearby_places),
                            onPlaceClick = { onOpenPlaceDetails(it, "nearby") },
                            animation = PlaceCardAnimation(sharedTransitionScope, animatedContentScope),
                            keyPrefix = "nearby"
                        )
                    }

                    item {
                        SectionHeader(
                            title = stringResource(R.string.home_top_places),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item {
                        HorizontalPlacesRow(
                            items = state.topPlaces.map {
                                HomePlaceItem(it.place, it.distanceKm)
                            },
                            isLoading = state.isTopLoading,
                            emptyMessage = stringResource(R.string.home_no_top_places),
                            onPlaceClick = { onOpenPlaceDetails(it, "top") },
                            animation = PlaceCardAnimation(sharedTransitionScope, animatedContentScope),
                            keyPrefix = "top"
                        )
                    }

                    item {
                        SectionHeader(
                            title = stringResource(R.string.home_recent_nearby_places),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item {
                        HorizontalPlacesRow(
                            items = state.recentlyAddedPlaces.map {
                                HomePlaceItem(it.place, it.distanceKm)
                            },
                            isLoading = state.isRecentlyAddedLoading,
                            emptyMessage = stringResource(R.string.home_no_recent_nearby_places),
                            onPlaceClick = { onOpenPlaceDetails(it, "recent") },
                            animation = PlaceCardAnimation(sharedTransitionScope, animatedContentScope),
                            keyPrefix = "recent"
                        )
                    }
                }

                state.errorMessage?.let { msg ->
                    item {
                        Text(
                            text = msg.asString(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming", "LongMethod")
private fun WelcomeIntroCard(
    modifier: Modifier = Modifier
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KidZoneRadii.Card),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
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
    keyPrefix: String = ""
) {
    val rowHeight = PLACE_ROW_HEIGHT
    when {
        isLoading -> {
            LazyRow(
                modifier = Modifier.height(rowHeight),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(KidZoneRadii.Card),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        else -> {
            LazyRow(
                modifier = Modifier.height(rowHeight),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
        ", ${stringResource(R.string.distance_away, formatDistance(it))}"
    }.orEmpty()
    val ratingLabel = place.ratingAccessibilityLabel()
    val animationKey = if (keyPrefix.isBlank()) "" else "${keyPrefix}_"

    KidZoneCard(
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
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PLACE_CARD_CONTENT_PADDING),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(KidZoneSpacing.GapSmall)) {
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
                    CategoryBadge(
                        category = place.category,
                        modifier = Modifier.weight(1f)
                    )
                }
                Column {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    item.distanceKm?.let {
                        Spacer(Modifier.height(KidZoneSpacing.GapTiny))
                        DistanceLabel(distanceKm = it)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    place.reviewsCount > 0 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = stringResource(R.string.rating),
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(KidZoneSpacing.GapTiny))
                            Text(
                                text = "%.1f (%d)".format(place.averageRating, place.reviewsCount),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    place.isNewWithoutReviews() -> {
                        NewPlaceBadge()
                    }
                }
            }
        }
    }
}

@Composable
private fun DistanceLabel(distanceKm: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = formatDistance(distanceKm),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

@Composable
private fun formatDistance(km: Double): String = when {
    km < 1.0 -> {
        val meters = (km * 1000).toInt()
        val rounded = ((meters + 25) / 50) * 50
        stringResource(R.string.distance_m, rounded)
    }
    km < 100.0 -> stringResource(R.string.distance_km, km)
    else -> stringResource(R.string.distance_km_integer, km.toInt())
}

@Composable
private fun Place.ratingAccessibilityLabel(): String = when {
    reviewsCount > 0 -> stringResource(R.string.rating_accessibility_label, averageRating, reviewsCount)
    isNewWithoutReviews() -> stringResource(R.string.new_place_no_reviews)
    else -> stringResource(R.string.map_no_reviews)
}

@Composable
private fun PlaceCardSkeleton() {
    KidZoneCard(
        modifier = Modifier
            .width(PLACE_CARD_WIDTH)
            .height(PLACE_ROW_HEIGHT)
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
