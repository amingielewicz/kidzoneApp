package com.kidzone.presentation.place.list

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kidzone.R
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.presentation.common.CategoryIcon
import com.kidzone.presentation.common.EmptyState
import com.kidzone.presentation.common.EmptyStateAction
import com.kidzone.presentation.common.KidZoneCard
import com.kidzone.presentation.common.KidZoneSpacing
import com.kidzone.presentation.common.isNewWithoutReviews
import com.kidzone.presentation.common.shimmerEffect
import com.kidzone.presentation.common.style
import com.kidzone.presentation.place.add.hasLocationPermission
import kotlinx.coroutines.launch

private val QUICK_AMENITIES = setOf(
    Amenity.CHANGING_TABLE,
    Amenity.TOILET,
    Amenity.STROLLER_ACCESS,
    Amenity.PARKING
)

private val ListContentPadding = PaddingValues(
    start = 16.dp,
    top = 12.dp,
    end = 16.dp,
    bottom = 104.dp
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlaceListScreen(
    onOpenPlaceDetails: (placeId: String, source: String?) -> Unit,
    viewModel: PlaceListViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val state by viewModel.uiState.collectAsState()
    val networkStatus by com.kidzone.presentation.common.rememberNetworkStatus()
    var previousNetworkStatus by remember { mutableStateOf(networkStatus) }

    LaunchedEffect(networkStatus) {
        if (previousNetworkStatus == com.kidzone.presentation.common.NetworkStatus.UNAVAILABLE &&
            networkStatus == com.kidzone.presentation.common.NetworkStatus.AVAILABLE
        ) {
            viewModel.refresh()
        }
        previousNetworkStatus = networkStatus
    }

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val returningFromDetails = remember { viewModel.consumeReturnFromDetails() }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(returningFromDetails) {
        if (returningFromDetails) {
            lazyListState.scrollToItem(viewModel.savedScrollIndex, viewModel.savedScrollOffset)
        } else {
            lazyListState.scrollToItem(0)
            viewModel.refreshLocation()
        }
    }

    var lastAppliedCategory by rememberSaveable {
        mutableStateOf(state.selectedCategory?.name.orEmpty())
    }
    var lastAppliedSortOrder by rememberSaveable { mutableStateOf(state.sortOrder.name) }
    var lastAppliedSearchQuery by rememberSaveable { mutableStateOf(state.searchQuery) }

    LaunchedEffect(state.selectedCategory) {
        val categoryName = state.selectedCategory?.name.orEmpty()
        if (categoryName != lastAppliedCategory) {
            lastAppliedCategory = categoryName
            lazyListState.scrollToItem(0)
        }
    }
    LaunchedEffect(state.sortOrder) {
        if (state.sortOrder.name != lastAppliedSortOrder) {
            lastAppliedSortOrder = state.sortOrder.name
            lazyListState.scrollToItem(0)
        }
    }
    LaunchedEffect(state.searchQuery) {
        if (state.searchQuery != lastAppliedSearchQuery) {
            lastAppliedSearchQuery = state.searchQuery
            lazyListState.scrollToItem(0)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            viewModel.refreshLocation()
        }
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

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = state.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                CategoryFilterBar(
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = viewModel::onCategorySelect
                )

                FilterAndSortBar(
                    advancedFiltersCount = state.selectedAmenities.size,
                    sortOrder = state.sortOrder,
                    currentUserSignedIn = state.currentUserId != null,
                    onOpenFilterSheet = { showFilterSheet = true },
                    onSortOrderChange = viewModel::onSortOrderChange
                )
            }
        }

        if (state.nearestUnavailable) {
            EnableLocationForSortingBanner(
                onAllowClick = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.isLoading && state.places.isEmpty() -> LoadingList()
                state.errorMessage != null && state.places.isEmpty() -> ListError(state.errorMessage!!.asString())
                state.places.isEmpty() -> EmptyListState(
                    state = state,
                    onClear = {
                        viewModel.onSearchQueryChange("")
                        viewModel.onCategorySelect(null)
                        viewModel.onAmenitiesCleared()
                    }
                )
                else -> {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = ListContentPadding,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = state.places, key = { "list_${it.id}" }) { place ->
                            PlaceCard(
                                place = place,
                                distanceKm = state.userLocation?.let { (lat, lng) ->
                                    haversineKm(lat, lng, place.latitude, place.longitude)
                                },
                                showDistance = state.sortOrder == PlaceListViewModel.SortOrder.NEAREST &&
                                    state.userLocation != null,
                                onClick = {
                                    viewModel.saveScrollPosition(
                                        firstVisibleItemIndex = lazyListState.firstVisibleItemIndex,
                                        firstVisibleItemScrollOffset = lazyListState.firstVisibleItemScrollOffset
                                    )
                                    viewModel.markNavigatingToDetails()
                                    onOpenPlaceDetails(place.id, "list")
                                },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedContentScope = animatedContentScope
                            )
                        }
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }

                    val shouldLoadMore = remember {
                        derivedStateOf {
                            val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val totalItems = lazyListState.layoutInfo.totalItemsCount
                            lastVisible >= totalItems - 3 && state.hasMore && !state.isLoadingMore
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

@Composable
private fun LoadingList() {
    var showSkeleton by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        showSkeleton = true
    }
    if (showSkeleton) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = ListContentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(10) { PlaceRowSkeleton() }
        }
    }
}

@Composable
private fun ListError(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyListState(
    state: PlaceListViewModel.UiState,
    onClear: () -> Unit
) {
    val canClearFilters = state.searchQuery.isNotBlank() ||
        state.selectedCategory != null ||
        state.selectedAmenities.isNotEmpty()
    EmptyState(
        icon = Icons.Filled.Search,
        title = emptyTitleFor(state),
        message = emptyMessageFor(state),
        action = if (canClearFilters) {
            EmptyStateAction(
                label = stringResource(R.string.clear_filters),
                onClick = onClear
            )
        } else {
            null
        }
    )
}

@Composable
private fun emptyMessageFor(state: PlaceListViewModel.UiState): String = when {
    state.sortOrder == PlaceListViewModel.SortOrder.ADDED_BY_ME && state.currentUserId == null ->
        stringResource(R.string.empty_added_by_me_logged_out)
    state.sortOrder == PlaceListViewModel.SortOrder.ADDED_BY_ME ->
        stringResource(R.string.empty_added_by_me_logged_in)
    state.searchQuery.isNotBlank() ->
        stringResource(R.string.empty_search_query)
    state.selectedCategory != null || state.selectedAmenities.isNotEmpty() ->
        stringResource(R.string.empty_filters)
    else -> stringResource(R.string.empty_default)
}

@Composable
private fun emptyTitleFor(state: PlaceListViewModel.UiState): String = when {
    state.sortOrder == PlaceListViewModel.SortOrder.ADDED_BY_ME && state.currentUserId == null ->
        stringResource(R.string.empty_title_added_by_me_logged_out)
    state.sortOrder == PlaceListViewModel.SortOrder.ADDED_BY_ME ->
        stringResource(R.string.empty_title_added_by_me_logged_in)
    state.searchQuery.isNotBlank() -> stringResource(R.string.empty_title_search)
    state.selectedCategory != null || state.selectedAmenities.isNotEmpty() ->
        stringResource(R.string.empty_title_filters)
    else -> stringResource(R.string.empty_title_default)
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var localText by rememberSaveable { mutableStateOf(query) }

    LaunchedEffect(query) {
        if (query.isEmpty() && localText.isNotEmpty()) {
            localText = ""
        }
    }

    OutlinedTextField(
        value = localText,
        onValueChange = {
            localText = it
            onQueryChange(it)
        },
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (localText.isNotBlank()) {
                IconButton(
                    onClick = {
                        localText = ""
                        onQueryChange("")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.clear_search),
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
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text(stringResource(R.string.category_all)) }
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
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(
            modifier = Modifier.semantics {
                contentDescription = context.getString(R.string.filters)
                stateDescription = if (advancedFiltersCount > 0) {
                    context.getString(R.string.active_filters_count, advancedFiltersCount)
                } else {
                    context.getString(R.string.no_active_filters)
                }
            },
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
                    contentDescription = stringResource(R.string.filters)
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
            label = { Text(stringResource(R.string.sort_prefix, stringResource(current.labelRes))) },
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
                    text = { Text(stringResource(option.labelRes)) },
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

@Composable
private fun EnableLocationForSortingBanner(onAllowClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.location_sorting_rationale),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onAllowClick) {
                Text(stringResource(R.string.allow))
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
    val categoryLabel = stringResource(place.category.labelRes)
    val distanceLabel = if (showDistance && distanceKm != null) {
        ", ${stringResource(R.string.distance_away, formatDistance(distanceKm))}"
    } else {
        ""
    }
    val addressLabel = place.address.takeIf { it.isNotBlank() }?.let {
        ", ${stringResource(R.string.address_prefix, it)}"
    }.orEmpty()
    val ratingLabel = place.ratingAccessibilityLabel()

    KidZoneCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${place.name}, $categoryLabel$distanceLabel$addressLabel, $ratingLabel"
            }
            .clickable(
                onClickLabel = stringResource(R.string.map_open_place_details_label),
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                CategoryIcon(
                    category = place.category,
                    animationKey = "list_place_icon_${place.id}",
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                    size = 32.dp,
                    iconSize = 19.dp
                )
                Spacer(Modifier.width(KidZoneSpacing.Gap))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SoftCategoryTag(category = place.category)
                        if (place.isNewWithoutReviews()) {
                            SoftNewTag()
                        }
                    }
                }
                if (place.reviewsCount > 0) {
                    RatingBadge(place = place)
                }
            }

            if (place.address.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        fontWeight = FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (showDistance && distanceKm != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatDistance(distanceKm),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
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
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SoftCategoryTag(category: PlaceCategory) {
    val style = category.style
    Surface(
        shape = RoundedCornerShape(50),
        color = style.color.copy(alpha = 0.10f),
        contentColor = style.color
    ) {
        Text(
            text = stringResource(category.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SoftNewTag() {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.50f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = stringResource(R.string.new_place_no_reviews),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun RatingBadge(place: Place) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "%.1f".format(place.averageRating),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
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
    KidZoneCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
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
