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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.presentation.common.CategoryBadge
import com.kidzone.presentation.common.CategoryIcon
import com.kidzone.presentation.common.GpsDisabledBanner
import com.kidzone.presentation.common.KidZoneCard
import com.kidzone.presentation.common.KidZoneSpacing
import com.kidzone.presentation.common.NewPlaceBadge
import com.kidzone.presentation.common.isNewWithoutReviews
import com.kidzone.presentation.common.rememberLocationServiceEnabled
import com.kidzone.presentation.common.shimmerEffect
import com.kidzone.presentation.common.style
import com.kidzone.presentation.place.add.hasLocationPermission
import kotlinx.coroutines.launch

/**
 * Dawne 4 "quick" udogodnienia. Usunięte z UI listy – teraz wszystkie
 * udogodnienia są dostępne wyłącznie z bottom sheeta filtrów.
 * Stała zachowana, bo [PlaceListViewModel] nadal ich używa do logiki
 * (zachowanie kompatybilności wstecznej – brak wpływu na UX).
 */
@Suppress("unused")
private val QUICK_AMENITIES = setOf(
    Amenity.CHANGING_TABLE,
    Amenity.TOILET,
    Amenity.STROLLER_ACCESS,
    Amenity.PARKING
)

/**
 * Lista miejsc - LazyColumn kart z filtrami + sortowaniem + twardym
 * cap-em [PlaceListViewModel] do 100 najistotniejszych pozycji.
 *
 * Subskrybuje Firestore przez [PlaceListViewModel] - snapshot listener
 * w repo automatycznie aktualizuje listę po dodaniu nowego miejsca z
 * [com.kidzone.presentation.place.add.AddPlaceScreen], bez potrzeby
 * pull-to-refresh.
 *
 * Filtry:
 *  - kategoria (single-select chipy w pierwszym rzędzie)
 *  - 4 uniwersalne udogodnienia (multi-select chipy w drugim rzędzie)
 *  - pełna lista udogodnień (multi-select w bottom sheecie pod "Filtry · N")
 *
 * Sortowanie - 5 trybów (zob. [PlaceListViewModel.SortOrder]) wybierane
 * przez "chip" z dropdownem na pasku filtrów. Domyślny: "Najbliższe"
 * (wymaga lokalizacji - jeżeli brak, banner zachęca do włączenia).
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

    // Auto-refresh po przywróceniu internetu
    val networkStatus by com.kidzone.presentation.common.rememberNetworkStatus()
    var previousNetworkStatus by remember { mutableStateOf(networkStatus) }
    LaunchedEffect(networkStatus) {
        if (previousNetworkStatus == com.kidzone.presentation.common.NetworkStatus.UNAVAILABLE
            && networkStatus == com.kidzone.presentation.common.NetworkStatus.AVAILABLE
        ) {
            viewModel.refresh()
        }
        previousNetworkStatus = networkStatus
    }

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    // Stan przewijania listy — inicjalizowany z pozycji zapisanej w ViewModel
    // TYLKO gdy user wraca z PlaceDetails. Przy powrocie z innej zakładki
    // (lub pierwszym wejściu) zaczynamy od góry.
    val returningFromDetails = remember { viewModel.consumeReturnFromDetails() }
    val lazyListState = rememberLazyListState()

    // Przy wejściu na zakładkę Lista z innej zakładki (NIE z PlaceDetails)
    // odświeżamy lokalizację i scrollujemy na górę. Dane i tak są real-time
    // przez snapshot listener, ale lokalizacja mogła się zmienić.
    // Przy powrocie z PlaceDetails — przywracamy zapisaną pozycję scrollu.
    LaunchedEffect(returningFromDetails) {
        if (returningFromDetails) {
            // Przywróć scroll do miejsca, gdzie user kliknął na item
            lazyListState.scrollToItem(
                viewModel.savedScrollIndex,
                viewModel.savedScrollOffset
            )
        } else {
            // Powrót z innej zakładki lub pierwsze wejście — scroll na górze
            lazyListState.scrollToItem(0)
            viewModel.refreshLocation()
        }
    }

    // Po zmianie kategorii/sortowania automatycznie przewijamy listę na górę.
    // Bez tego user widziałby "tę samą pozycję pod palcem", ale w nowym
    // porządku - co jest mylące (nie wiadomo, czy to jeszcze ten sam wynik
    // czy nowy item w środku rankingu).
    //
    // Używamy rememberSaveable, żeby uniknąć scrollowania na górę po
    // nawigacji powrotnej z PlaceDetails (LaunchedEffect odpalałby się
    // ponownie z tym samym kluczem przy re-compose).
    //
    // Uwaga: nie używamy animateScrollToItem(). Na Compose 2024.09.x potrafi
    // zderzyć się z LazyColumn + shared transition/lookahead przy nagłej
    // zmianie listy i skończyć crashem "Placement happened before lookahead".
    var lastAppliedCategory by rememberSaveable {
        mutableStateOf(state.selectedCategory?.name.orEmpty())
    }
    var lastAppliedSortOrder by rememberSaveable { mutableStateOf(state.sortOrder.name) }
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

    // Launcher requestu uprawnienia. Po nadaniu odświeżamy lokalizację -
    // sortowanie "Najbliższe" zaczyna działać bez restartu ekranu.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            viewModel.refreshLocation()
        }
    }

    // Po wejściu na ekran - jeśli user już ma permission, ale fixu jeszcze
    // nie pobraliśmy (np. ekran się zrekonstruował po deep-linku), próbujemy
    // ponownie. To no-op gdy lokalizacja już jest w state.
    LaunchedEffect(Unit) {
        if (hasLocationPermission(context) && state.userLocation == null) {
            viewModel.refreshLocation()
        }
    }

    // Gdy user wraca z ustawień Androida (gdzie ręcznie dał lokalizację),
    // odświeżamy fix - banner sam zniknie.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasLocationPermission(context)) {
                viewModel.refreshLocation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    // Liczba aktywnych filtrów udogodnień w sheecie.
    val advancedAmenitiesCount = state.selectedAmenities.size
    val gpsEnabled = rememberLocationServiceEnabled()

    Column(modifier = Modifier.fillMaxSize()) {
        // Banner informujący o wyłączonej lokalizacji - na samej górze
        if (!gpsEnabled && hasLocationPermission(context)) {
            GpsDisabledBanner()
        }

        // Wyszukiwarka po nazwie miejsca
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
                // Smart Loading: don't show skeleton immediately to avoid flickering on fast cache hits
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessageFor(state.sortOrder, state.currentUserId != null),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = state.places, key = { "list_${it.id}" }) { place ->
                        PlaceCard(
                            place = place,
                            distanceKm = state.userLocation?.let { (lat, lng) ->
                                haversineKm(lat, lng, place.latitude, place.longitude)
                            },
                            showDistance = state.sortOrder ==
                                PlaceListViewModel.SortOrder.NEAREST &&
                                state.userLocation != null,
                            onClick = {
                                // Zapisz pozycję scrollu i ustaw flagę przed nawigacją
                                // do szczegółów — po powrocie lista wróci w to samo miejsce.
                                viewModel.saveScrollPosition(
                                    firstVisibleItemIndex = lazyListState.firstVisibleItemIndex,
                                    firstVisibleItemScrollOffset = lazyListState.firstVisibleItemScrollOffset
                                )
                                viewModel.markNavigatingToDetails()
                                onOpenPlaceDetails(place.id, "list")
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            animationSource = "list"
                        )
                    }
                    if (state.hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
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

                // Infinite scroll: doładuj następną stronę gdy user dojdzie
                // blisko końca listy (ostatnie 3 elementy).
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
        "Zaloguj się, by zobaczyć swoje miejsca"
    sortOrder == PlaceListViewModel.SortOrder.ADDED_BY_ME ->
        "Nie dodałaś/eś jeszcze żadnego miejsca"
    else -> "Brak miejsc pasujących do filtrów"
}

/**
 * Pole wyszukiwania po nazwie miejsca. Debouncing jest naturalny
 * (MutableStateFlow w VM pomija duplikaty), więc nie potrzebujemy
 * dodatkowego delay — lista filtruje się natychmiast.
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Lokalny stan zapobiega "skakaniu" kursora przy aktualizacji stanu z VM
    var localText by remember { mutableStateOf(query) }

    // Synchronizacja, jeśli query zmieni się z zewnątrz (np. przycisk wyczyść)
    LaunchedEffect(query) {
        if (localText != query) {
            localText = query
        }
    }

    OutlinedTextField(
        value = localText,
        onValueChange = {
            localText = it
            onQueryChange(it)
        },
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = modifier.height(52.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterBar(
    selectedCategory: PlaceCategory?,
    onCategorySelected: (PlaceCategory?) -> Unit
) {
    // Kolejność jak w enum PlaceCategory (świadomie nie alfabetycznie -
    // logiczne grupowanie: place zabaw -> sale -> kawiarnia/restauracja ->
    // park -> atrakcje -> inne). "Wszystkie" zostaje pierwsze jako reset.
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

/**
 * Rząd z przyciskiem filtrów (ikona Tune) + chipem sortowania.
 *
 * Quick-amenity chipy usunięte – wszystkie udogodnienia dostępne wyłącznie
 * z bottom sheeta (po kliknięciu ikony Tune). Dzięki temu ekran listy jest
 * czystszy i mniej przytłaczający na mniejszych ekranach.
 */
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
        // 1. Button "Filtry" (ikona Tune z badge liczbą aktywnych filtrów)
        BadgedBox(
            modifier = Modifier.semantics {
                contentDescription = "Filtry"
                stateDescription = if (advancedFiltersCount > 0) {
                    "Aktywne filtry: $advancedFiltersCount"
                } else {
                    "Brak aktywnych filtrów"
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
                    contentDescription = "Filtry"
                )
            }
        }

        // 2. Sortowanie
        SortChip(
            current = sortOrder,
            currentUserSignedIn = currentUserSignedIn,
            onChange = onSortOrderChange
        )
    }
}

/**
 * Chip "Sortuj: <label>" z dropdownem 5 trybów. Implementacja przez
 * `AssistChip` (a nie FilterChip), bo wartość nie jest binarna - wybór
 * jednego z wielu, a kontrolka i tak otwiera natywny menu.
 *
 * `currentUserSignedIn` reguluje, czy "Dodane przez Ciebie" jest enabled
 * w menu - dla wylogowanego usera ten wybór nie ma sensu (zwróciłby
 * pustą listę), ale zostawiamy go widocznego, żeby user widział co go
 * czeka po zalogowaniu.
 */
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
                val enabled = !(option == PlaceListViewModel.SortOrder.ADDED_BY_ME &&
                    !currentUserSignedIn)
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

/**
 * Banner pokazywany pod paskiem filtrów, gdy user wybrał "Najbliższe", a
 * lokalizacji nie mamy. Tłumaczy dlaczego sortowanie nie działa i daje
 * przycisk requesta uprawnienia.
 */
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
                text = "Włącz lokalizację, by sortować miejsca po odległości.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onAllowClick) {
                Text("Pozwól")
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
    animatedContentScope: AnimatedContentScope? = null,
    animationSource: String? = "list"
) {
    val categoryLabel = stringResource(place.category.labelRes)
    val distanceLabel = if (showDistance && distanceKm != null) {
        ", ${formatDistance(distanceKm)} od Ciebie"
    } else {
        ""
    }
    val addressLabel = place.address.takeIf { it.isNotBlank() }?.let { ", adres: $it" }.orEmpty()
    val ratingLabel = place.ratingAccessibilityLabel()

    KidZoneCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${place.name}, $categoryLabel$distanceLabel$addressLabel, $ratingLabel"
            }
            .clickable(
                onClickLabel = "Otwórz szczegóły miejsca",
                role = Role.Button,
                onClick = onClick
            )
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
                Spacer(Modifier.width(KidZoneSpacing.Gap))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(KidZoneSpacing.GapTiny))
                    CategoryBadge(category = place.category)
                }
                when {
                    place.reviewsCount > 0 -> {
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
                    place.isNewWithoutReviews() -> {
                        NewPlaceBadge()
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
                    // Odległość pokazujemy tylko gdy aktywne sortowanie po
                    // odległości - inaczej byłoby "głośno" przy sortach
                    // niezwiązanych z lokalizacją.
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

/**
 * Formatuje odległość w km do user-friendly stringu:
 *  - < 1 km: w metrach z zaokrągleniem do 50 m ("420 m"),
 *  - >= 1 km: z 1 miejscem po przecinku ("3.5 km"),
 *  - >= 100 km: bez ułamka ("125 km") - taka rozdzielczość nie ma znaczenia
 *    dla user experience na liście miejsc dla dzieci.
 */
private fun formatDistance(km: Double): String = when {
    km < 1.0 -> {
        val meters = (km * 1000).toInt()
        val rounded = ((meters + 25) / 50) * 50
        "$rounded m"
    }
    km < 100.0 -> "%.1f km".format(km)
    else -> "%d km".format(km.toInt())
}

private fun Place.ratingAccessibilityLabel(): String = when {
    reviewsCount > 0 -> "ocena %.1f, liczba opinii %d".format(averageRating, reviewsCount)
    isNewWithoutReviews() -> "nowe miejsce bez opinii"
    else -> "brak opinii"
}

/** Odległość w km między dwoma punktami (formuła haversine). */
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
