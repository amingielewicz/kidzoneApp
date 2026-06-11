package com.kidzone.presentation.place.list

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.presentation.common.CategoryIcon
import com.kidzone.presentation.common.GpsDisabledBanner
import com.kidzone.presentation.common.rememberLocationServiceEnabled
import com.kidzone.presentation.common.shimmerEffect
import com.kidzone.presentation.common.style
import com.kidzone.presentation.place.add.hasLocationPermission
import kotlinx.coroutines.launch

/**
 * Lista miejsc - LazyColumn kart z filtrami + sortowaniem.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlaceListScreen(
    onOpenPlaceDetails: (placeId: String, source: String?) -> Unit,
    viewModel: PlaceListViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val gpsEnabled = rememberLocationServiceEnabled()

    val networkStatus by com.kidzone.presentation.common.rememberNetworkStatus()
    var previousNetworkStatus by remember { mutableStateOf(networkStatus) }
    LaunchedEffect(networkStatus) {
        if (
            (previousNetworkStatus == com.kidzone.presentation.common.NetworkStatus.UNAVAILABLE)
            && (networkStatus == com.kidzone.presentation.common.NetworkStatus.AVAILABLE)
        ) {
            viewModel.refresh()
        }
        previousNetworkStatus = networkStatus
    }

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(state.sortOrder) {
        lazyListState.animateScrollToItem(0)
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context) && state.userLocation == null) {
            viewModel.refreshLocation()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasLocationPermission(context)) {
                viewModel.refreshLocation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    val advancedAmenitiesCount = state.selectedAmenities.size

    Column(modifier = Modifier.fillMaxSize()) {
        if (!gpsEnabled && hasLocationPermission(context)) {
            GpsDisabledBanner()
        }

        SearchBar(
            query = state.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        CategoryFilterBar(
            selectedCategory = state.selectedCategory,
            onCategorySelected = viewModel::onCategorySelected
        )

        FilterAndSortBar(
            advancedFiltersCount = advancedAmenitiesCount,
            sortOrder = state.sortOrder,
            currentUserSignedIn = state.currentUserId != null,
            onOpenFilterSheet = { showFilterSheet = true },
            onSortOrderChange = viewModel::onSortOrderChange
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
        when {
            state.isLoading && state.places.isEmpty() -> {
                var showSkeleton by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(150)
                    showSkeleton = true
                }
                if (showSkeleton) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        userScrollEnabled = false
                    ) {
                        items(10) {
                            PlaceRowSkeleton()
                        }
                    }
                }
            }

            state.errorMessage != null && state.places.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            state.places.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = emptyMessageFor(state.sortOrder, state.currentUserId != null),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = state.places, key = { it.id }) { place ->
                        PlaceCard(
                            place = place,
                            distanceKm = state.userLocation?.let { (lat, lng) ->
                                haversineKm(lat, lng, place.latitude, place.longitude)
                            },
                            showDistance = state.sortOrder == PlaceListViewModel.SortOrder.NEAREST && state.userLocation != null,
                            onClick = { onOpenPlaceDetails(place.id, "list") },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope
                        )
                    }
                    if (state.hasMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .shimmerEffect()
                                )
                            }
                        }
                    }
                }

                val shouldLoadMore = remember {
                    derivedStateOf {
                        val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val totalItems = lazyListState.layoutInfo.totalItemsCount
                        lastVisible >= totalItems - 3 && state.hasMore
                    }
                }
                LaunchedEffect(shouldLoadMore.value) {
                    if (shouldLoadMore.value) {
                        viewModel.loadMore()
                    }
                }
            }
        }
        }
    }

    if (showFilterSheet) {
        AmenityFilterSheet(
            sheetState = sheetState,
            selectedCategory = state.selectedCategory,
            selectedAmenities = state.selectedAmenities,
            totalResultsCount = state.places.size,
            onAmenityToggled = viewModel::onAmenityToggled,
            onClearAll = viewModel::onAmenitiesCleared,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    if (!sheetState.isVisible) showFilterSheet = false
                }
            }
        )
    }
}

private fun emptyMessageFor(
    sortOrder: PlaceListViewModel.SortOrder,
    isSignedIn: Boolean
): String = when {
    sortOrder == PlaceListViewModel.SortOrder.ADDED_BY_ME && !isSignedIn ->
        "Zaloguj si\u0119, by zobaczy\u0107 swoje miejsca"
    sortOrder == PlaceListViewModel.SortOrder.ADDED_BY_ME ->
        "Nie doda\u0142a\u015B/e\u015B jeszcze \u017cadnego miejsca"
    else -> "Brak miejsc pasuj\u0105cych do filtr\u00f3w"
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Szukaj miejsca po nazwie\u2026") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Wyczy\u015B\u0107 wyszukiwanie",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(52.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterBar(
    selectedCategory: PlaceCategory?,
    onCategorySelected: (PlaceCategory?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text("Wszystkie") }
        )
        PlaceCategory.entries.forEach { category ->
            val style = category.style
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                leadingIcon = {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.color
                    )
                },
                label = { Text(stringResource(category.labelRes)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterAndSortBar(
    advancedFiltersCount: Int,
    sortOrder: PlaceListViewModel.SortOrder,
    currentUserSignedIn: Boolean,
    onOpenFilterSheet: () -> Unit,
    onSortOrderChange: (PlaceListViewModel.SortOrder) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(
            badge = {
                if (advancedFiltersCount > 0) {
                    Badge { Text(advancedFiltersCount.toString()) }
                }
            }
        ) {
            FilledTonalIconButton(
                onClick = onOpenFilterSheet,
                colors = if (advancedFiltersCount > 0) {
                    IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    IconButtonDefaults.filledTonalIconButtonColors()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = "Filtry"
                )
            }
        }

        SortChip(
            current = sortOrder,
            currentUserSignedIn = currentUserSignedIn,
            onChange = onSortOrderChange
        )
    }
}

@Composable
private fun SortChip(
    current: PlaceListViewModel.SortOrder,
    currentUserSignedIn: Boolean,
    onChange: (PlaceListViewModel.SortOrder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text("Sortuj: ${current.label}") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null
                )
            },
            shape = RoundedCornerShape(50),
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PlaceListViewModel.SortOrder.entries.forEach { option ->
                val enabled = !(option == PlaceListViewModel.SortOrder.ADDED_BY_ME && !currentUserSignedIn)
                DropdownMenuItem(
                    text = { Text(option.label) },
                    enabled = enabled,
                    onClick = {
                        onChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PlaceCard(
    place: Place,
    distanceKm: Double?,
    showDistance: Boolean,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(
                    category = place.category,
                    animationKey = "list_place_icon_${place.id}",
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                    size = 28.dp,
                    iconSize = 18.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(place.category.labelRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (place.reviewsCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "%.1f".format(place.averageRating),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            if (place.address.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (showDistance && distanceKm != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatDistance(distanceKm),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            if (place.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = place.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun formatDistance(km: Double): String = when {
    km < 1.0 -> {
        val meters = (km * 1000).toInt()
        val rounded = ((meters + 25) / 50) * 50
        "$rounded m"
    }
    km < 100.0 -> "%.1f km".format(km)
    else -> "%d km".format(km.toInt())
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2).let { it * it } +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2).let { it * it }
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return r * c
}

@Composable
private fun PlaceRowSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(20.dp)
                            .clip(MaterialTheme.shapes.small)
                            .shimmerEffect()
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(14.dp)
                            .clip(MaterialTheme.shapes.small)
                            .shimmerEffect()
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .shimmerEffect()
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect()
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(16.dp)
                    .clip(MaterialTheme.shapes.small)
                    .shimmerEffect()
            )
        }
    }
}
