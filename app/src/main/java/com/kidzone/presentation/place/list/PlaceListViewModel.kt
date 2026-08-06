package com.kidzone.presentation.place.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.R
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.PagedResult
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.service.LocationProvider
import com.kidzone.presentation.common.ScreenState
import com.kidzone.utils.AppConfig
import com.kidzone.utils.GeoUtils
import com.kidzone.utils.OpResult
import com.kidzone.utils.SearchNormalization
import com.kidzone.utils.UiText
import com.kidzone.utils.toPlacesErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val LIST_PAGE_SIZE = 15
private const val SEARCH_PREFETCH_PAGE_SIZE = 500
private val LIST_ERROR_FALLBACK = UiText.StringResource(R.string.error_fetch_list)
private val LIST_MORE_ERROR_FALLBACK = UiText.StringResource(R.string.error_fetch_more)

private const val SEARCH_CONTAINS_RANK_OFFSET = 100
private const val HAS_REVIEWS_SORT_WEIGHT = 0
private const val NO_REVIEWS_SORT_WEIGHT = 1

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie stanem listy miejsc (wyniki wyszukiwania, sortowanie, stronicowanie).
 * - Koordynacja wyszukiwania pełnotekstowego z filtrami kategorii i udogodnień.
 * - Obliczanie dystansów i stanów pustych dla list miejsc.
 *
 * 🚫 Poza zakresem:
 * - Brak decyzji o offline queue.
 * - Brak retry logiki dla operacji sieciowych.
 * - Brak zarządzania sesją użytkownika.
 *
 * 📥 Wejście:
 * - Parametry wyszukiwania (fraza, kategoria, wybrane udogodnienia).
 * - Sygnały o przewijaniu listy (stronicowanie).
 * - Lokalizacja użytkownika z [LocationProvider].
 *
 * 📤 Wyjście:
 * - Stan UI zawierający listę miejsc ([UiState]).
 *
 * ✅ Gwarancje:
 * - Normalizacja tekstu przed wyszukiwaniem.
 * - Stabilne sortowanie wyników na podstawie dystansu i dopasowania nazwy.
 *
 * 🔌 Offline:
 * - Wspiera wyświetlanie miejsc z lokalnego cache Room.
 *
 * 🧵 Wątki:
 * - viewModelScope dla reaktywnych strumieni Flow i operacji wyszukiwania.
 * - Brak blokujących operacji na wątku Main.
 *
 * 🧪 Testowalność:
 * - Pełne DI.
 * - Deterministyczne łączenie filtrów w końcowe zapytanie.
 *
 * 🧼 Lifecycle:
 * - Zarządzanie stanem paginacji w cyklu życia ekranu.
 */
@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class PlaceListViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    /**
     * Dostępne opcje sortowania listy miejsc.
     *
     * @property labelRes zasób zlokalizowanej etykiety opcji.
     */
    enum class SortOrder(@androidx.annotation.StringRes val labelRes: Int) {
        NEAREST(R.string.sort_nearest),
        RECENTLY_ADDED(R.string.sort_recently_added),
        ADDED_BY_ME(R.string.sort_added_by_me),
        BEST_RATED(R.string.sort_best_rated),
        WORST_RATED(R.string.sort_worst_rated)
    }

    /**
     * Niezmienny stan ekranu listy miejsc.
     *
     * @property screenState ogólny stan ładowania/treści/błędu dla głównej listy.
     * @property selectedCategory wybrana kategoria filtra.
     * @property selectedAmenities zestaw wybranych udogodnień.
     * @property sortOrder bieżący sposób sortowania wyników.
     * @property userLocation współrzędne użytkownika (jeśli dostępne).
     * @property currentUserId ID zalogowanego użytkownika (do filtra ADDED_BY_ME).
     * @property nearestUnavailable true gdy wybrano NEAREST, ale brak uprawnień/usług lokalizacji.
     * @property isRefreshing czy trwa odświeżanie typu pull-to-refresh.
     * @property hasMore czy istnieją kolejne strony wyników w repository.
     * @property isLoadingMore czy trwa dociąganie kolejnej strony (pagination).
     * @property totalCount liczba elementów w bieżącym zbiorze wyników.
     * @property searchQuery wpisana fraza wyszukiwania.
     * @property isUsingStaleLocation czy wyświetlany dystans oparty jest na nieaktualnej lokalizacji.
     * @property staleLocationAgeMinutes wiek nieaktualnej lokalizacji w minutach.
     * @property hasLocationPermission czy aplikacja posiada uprawnienia do GPS.
     * @property isLocationServiceEnabled czy usługi lokalizacji są włączone w systemie.
     */
    data class UiState(
        val screenState: ScreenState<List<Place>> = ScreenState.Loading,
        val selectedCategory: PlaceCategory? = null,
        val selectedAmenities: Set<Amenity> = emptySet(),
        val sortOrder: SortOrder = SortOrder.NEAREST,
        val userLocation: Pair<Double, Double>? = null,
        val currentUserId: String? = null,
        val nearestUnavailable: Boolean = false,
        val isRefreshing: Boolean = false,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false,
        val totalCount: Int = 0,
        val searchQuery: String = "",
        val isUsingStaleLocation: Boolean = false,
        val staleLocationAgeMinutes: Int? = null,
        val hasLocationPermission: Boolean = false,
        val isLocationServiceEnabled: Boolean = false,
    )

    private val selectedCategory = MutableStateFlow<PlaceCategory?>(null)
    private val selectedAmenities = MutableStateFlow<Set<Amenity>>(emptySet())
    private val sortOrder = MutableStateFlow(SortOrder.NEAREST)
    private val searchQuery = MutableStateFlow("")

    private val _isRefreshing = MutableStateFlow(false)
    private val _isLoadingMore = MutableStateFlow(false)
    private val _lastResult = MutableStateFlow<PagedResult<Place>?>(null)
    private val _errorMessage = MutableStateFlow<UiText?>(null)
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _isUsingStaleLocation = MutableStateFlow(false)
    private val _staleLocationAgeMinutes = MutableStateFlow<Int?>(null)
    private val _hasLocationPermission = MutableStateFlow(false)
    private val _isLocationServiceEnabled = MutableStateFlow(false)

    // Flag helping to restore scroll position when returning from details
    private var isReturningFromDetails = false
    private var isPrefetchingSearchPool = false
    var savedScrollIndex = 0
        private set
    var savedScrollOffset = 0
        private set

    private val filterInputs = combine(
        selectedCategory,
        selectedAmenities,
        sortOrder,
        searchQuery,
        authRepository.currentUser
    ) { category, amenities, order, query, user ->
        FilterInputs(
            category = category,
            amenities = amenities,
            order = order,
            query = query,
            user = user
        )
    }

    private val loadingInputs = combine(
        _isRefreshing,
        _isLoadingMore,
        _lastResult,
        _errorMessage
    ) { refreshing, loadingMore, paged, error ->
        LoadingInputs(
            refreshing = refreshing,
            loadingMore = loadingMore,
            paged = paged,
            error = error
        )
    }

    private val locationInputs = combine(
        _userLocation,
        _isUsingStaleLocation,
        _staleLocationAgeMinutes,
        _hasLocationPermission,
        _isLocationServiceEnabled
    ) { location, isUsingStaleLocation, staleLocationAgeMinutes, hasLocationPermission, isLocationServiceEnabled ->
        LocationInputs(
            location = location,
            isUsingStaleLocation = isUsingStaleLocation,
            staleLocationAgeMinutes = staleLocationAgeMinutes,
            hasLocationPermission = hasLocationPermission,
            isLocationServiceEnabled = isLocationServiceEnabled
        )
    }

    val uiState: StateFlow<UiState> = combine(
        filterInputs,
        loadingInputs,
        locationInputs
    ) { filter, loading, location ->
        val places = loading.paged?.items.orEmpty()
        val filtered = filterAndSort(
            places,
            FilterParams(
                location = location.location,
                userId = filter.user?.id,
                order = filter.order,
                query = filter.query,
                category = filter.category,
                amenities = filter.amenities
            )
        )

        val screenState = when {
            loading.paged != null -> {
                if (filtered.isEmpty()) ScreenState.Empty else ScreenState.Content(filtered)
            }
            loading.error != null -> ScreenState.Error(loading.error)
            else -> ScreenState.Loading
        }

        UiState(
            screenState = screenState,
            selectedCategory = filter.category,
            selectedAmenities = filter.amenities,
            sortOrder = filter.order,
            userLocation = location.location,
            hasLocationPermission = location.hasLocationPermission,
            isLocationServiceEnabled = location.isLocationServiceEnabled,
            currentUserId = filter.user?.id,
            nearestUnavailable = filter.order == SortOrder.NEAREST &&
                (!location.hasLocationPermission || !location.isLocationServiceEnabled),
            isRefreshing = loading.refreshing,
            hasMore = filter.query.isBlank() && (loading.paged?.hasMore ?: false),
            isLoadingMore = loading.loadingMore,
            totalCount = loading.paged?.items?.size ?: 0,
            searchQuery = filter.query,
            isUsingStaleLocation = location.isUsingStaleLocation,
            staleLocationAgeMinutes = location.staleLocationAgeMinutes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConfig.FLOW_SUBSCRIPTION_TIMEOUT_MS), UiState())

    init {
        refresh()
        refreshLocation()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            loadPage(null)
            _isRefreshing.value = false
        }
    }

    fun refreshLocation() {
        val hasPermission = locationProvider.hasPermission()
        val isServiceEnabled = locationProvider.isServiceEnabled()

        _hasLocationPermission.value = hasPermission
        _isLocationServiceEnabled.value = isServiceEnabled

        if (!hasPermission || !isServiceEnabled) {
            val staleLocation: Pair<Double, Double>? = locationProvider.getLastKnownLocation()

            _userLocation.value = staleLocation
            _isUsingStaleLocation.value = staleLocation != null
            _staleLocationAgeMinutes.value = if (staleLocation != null) {
                locationProvider.getLastKnownLocationAgeMinutes()
            } else {
                null
            }

            return
        }

        viewModelScope.launch {
            val location: Pair<Double, Double>? = locationProvider.getCurrentLocation()

            if (location != null) {
                _userLocation.value = location
                _isUsingStaleLocation.value = false
                _staleLocationAgeMinutes.value = null
            } else {
                val staleLocation: Pair<Double, Double>? = locationProvider.getLastKnownLocation()

                _userLocation.value = staleLocation
                _isUsingStaleLocation.value = staleLocation != null
                _staleLocationAgeMinutes.value = if (staleLocation != null) {
                    locationProvider.getLastKnownLocationAgeMinutes()
                } else {
                    null
                }
            }
        }
    }

    fun loadMore() {
        if (searchQuery.value.isNotBlank()) return

        val current = _lastResult.value
        if (current?.nextCursor == null || _isLoadingMore.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            val loaded = loadPage(current.nextCursor)
            if (!loaded) {
                _lastResult.value = _lastResult.value?.copy(nextCursor = null, hasMore = false)
            }
            _isLoadingMore.value = false
        }
    }

    private suspend fun loadPage(cursor: String?): Boolean {
        val result = placeRepository.getPlacesPage(
            pageSize = LIST_PAGE_SIZE,
            cursor = cursor,
            category = selectedCategory.value,
            query = searchQuery.value
        )
        return when (result) {
            is OpResult.Success -> {
                if (cursor == null) {
                    _lastResult.value = result.data.copy(items = result.data.items.distinctBy { it.id })
                } else {
                    val prev = _lastResult.value
                    _lastResult.value = PagedResult(
                        items = (prev?.items.orEmpty() + result.data.items).distinctBy { it.id },
                        nextCursor = result.data.nextCursor
                    )
                }
                true
            }
            is OpResult.Failure -> {
                _errorMessage.value = result.error.toPlacesErrorMessage(
                    if (cursor == null) LIST_ERROR_FALLBACK else LIST_MORE_ERROR_FALLBACK
                )
                false
            }
        }
    }

    fun onCategorySelect(category: PlaceCategory?) {
        selectedCategory.value = category
        refresh()
    }

    // Temporary alias for Screen code that uses wrong name
    fun onCategorySelected(category: PlaceCategory?) = onCategorySelect(category)

    fun onAmenityToggled(amenity: Amenity) {
        selectedAmenities.update {
            if (amenity in it) it - amenity else it + amenity
        }
        refresh()
    }

    fun onAmenitiesCleared() {
        selectedAmenities.value = emptySet()
        refresh()
    }

    fun onSortOrderChange(order: SortOrder) {
        sortOrder.value = order
    }

    fun onSearchQueryChange(query: String) {
        val wasBlank = searchQuery.value.isBlank()
        searchQuery.value = query
        if (wasBlank && query.isNotBlank()) {
            prefetchSearchPool()
        }
    }

    private fun prefetchSearchPool() {
        val current = _lastResult.value
        if (current?.hasMore != true || isPrefetchingSearchPool) return

        viewModelScope.launch {
            isPrefetchingSearchPool = true
            val result = placeRepository.getPlacesPage(
                pageSize = SEARCH_PREFETCH_PAGE_SIZE,
                cursor = null,
                category = selectedCategory.value,
                query = null
            )
            if (result is OpResult.Success) {
                _lastResult.value = result.data.copy(items = result.data.items.distinctBy { it.id })
            }
            isPrefetchingSearchPool = false
        }
    }

    fun saveScrollPosition(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        savedScrollIndex = firstVisibleItemIndex
        savedScrollOffset = firstVisibleItemScrollOffset
    }

    fun markNavigatingToDetails() {
        isReturningFromDetails = true
    }

    fun consumeReturnFromDetails(): Boolean {
        val wasReturning = isReturningFromDetails
        isReturningFromDetails = false
        return wasReturning
    }

    private fun filterAndSort(
        list: List<Place>,
        params: FilterParams
    ): List<Place> {
        val filtered = list.filter { place -> place.matchesFilters(params) }

        val comparator = when (params.order) {
            SortOrder.RECENTLY_ADDED -> compareByDescending<Place> { it.createdAtMillis }
            SortOrder.ADDED_BY_ME -> compareByDescending<Place> { it.createdAtMillis }
            SortOrder.BEST_RATED -> compareByDescending<Place> { it.averageRating }
                .thenByDescending { it.reviewsCount }
            SortOrder.WORST_RATED -> compareBy<Place> {
                if (it.reviewsCount == 0) NO_REVIEWS_SORT_WEIGHT else HAS_REVIEWS_SORT_WEIGHT
            }
                .thenBy { it.averageRating }
                .thenByDescending { it.reviewsCount }
            SortOrder.NEAREST -> if (params.location != null) {
                compareBy<Place> {
                    GeoUtils.haversineKm(
                        it.latitude, it.longitude,
                        params.location.first, params.location.second
                    )
                }
            } else {
                compareByDescending<Place> { it.createdAtMillis }
            }
        }

        return if (params.query.isBlank()) {
            filtered.sortedWith(comparator)
        } else {
            filtered.sortedWith(compareBy<Place> { it.searchRank(params.query) }.then(comparator))
        }
    }

    private fun Place.matchesFilters(params: FilterParams): Boolean {
        val normalizedName = SearchNormalization.normalize(name)
        val normalizedQuery = SearchNormalization.normalize(params.query)
        val matchesQuery = normalizedQuery.isBlank() || normalizedName.contains(normalizedQuery)
        val matchesCategory = params.category == null || category == params.category
        val matchesAmenities = params.amenities.isEmpty() || amenities.containsAll(params.amenities)
        val matchesOwner = params.order != SortOrder.ADDED_BY_ME ||
            params.userId?.let { ownerUserId == it } == true
        return matchesQuery && matchesCategory && matchesAmenities && matchesOwner
    }

    private fun Place.searchRank(query: String): Int {
        val normalizedName = SearchNormalization.normalize(name)
        val normalizedQuery = SearchNormalization.normalize(query)
        val wordPrefixIndex = normalizedName
            .split(" ")
            .indexOfFirst { it.startsWith(normalizedQuery) }
        val matchIndex = normalizedName.indexOf(normalizedQuery)

        return when {
            normalizedName.startsWith(normalizedQuery) -> 0
            wordPrefixIndex >= 0 -> 1 + wordPrefixIndex
            matchIndex >= 0 -> SEARCH_CONTAINS_RANK_OFFSET + matchIndex
            else -> Int.MAX_VALUE
        }
    }

    private data class FilterInputs(
        val category: PlaceCategory?,
        val amenities: Set<Amenity>,
        val order: SortOrder,
        val query: String,
        val user: User?
    )

    private data class LoadingInputs(
        val refreshing: Boolean,
        val loadingMore: Boolean,
        val paged: PagedResult<Place>?,
        val error: UiText?
    )

    private data class LocationInputs(
        val location: Pair<Double, Double>?,
        val isUsingStaleLocation: Boolean,
        val staleLocationAgeMinutes: Int?,
        val hasLocationPermission: Boolean,
        val isLocationServiceEnabled: Boolean
    )

    private data class FilterParams(
        val location: Pair<Double, Double>?,
        val userId: String?,
        val order: SortOrder,
        val query: String,
        val category: PlaceCategory?,
        val amenities: Set<Amenity>
    )

}
