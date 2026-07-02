package com.kidzone.presentation.place.list

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
import com.kidzone.utils.OpResult
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
import java.text.Normalizer
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

private const val FLOW_SUBSCRIPTION_TIMEOUT_MS = 5000L
private const val EARTH_RADIUS_KM = 6371.0
private const val SEARCH_CONTAINS_RANK_OFFSET = 100

/**
 * ViewModel listy miejsc.
 */
@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class PlaceListViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    enum class SortOrder(@androidx.annotation.StringRes val labelRes: Int) {
        NEAREST(R.string.sort_nearest),
        RECENTLY_ADDED(R.string.sort_recently_added),
        ADDED_BY_ME(R.string.sort_added_by_me),
        BEST_RATED(R.string.sort_best_rated),
        WORST_RATED(R.string.sort_worst_rated)
    }

    data class UiState(
        val places: List<Place> = emptyList(),
        val selectedCategory: PlaceCategory? = null,
        val selectedAmenities: Set<Amenity> = emptySet(),
        val sortOrder: SortOrder = SortOrder.NEAREST,
        val userLocation: Pair<Double, Double>? = null,
        val currentUserId: String? = null,
        val nearestUnavailable: Boolean = false,
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val errorMessage: UiText? = null,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false,
        val totalCount: Int = 0,
        val searchQuery: String = ""
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

    // Flag helping to restore scroll position when returning from details
    private var isReturningFromDetails = false
    private var isPrefetchingSearchPool = false
    var savedScrollIndex = 0
        private set
    var savedScrollOffset = 0
        private set

    val uiState: StateFlow<UiState> = combine(
        selectedCategory,
        selectedAmenities,
        sortOrder,
        searchQuery,
        _userLocation,
        authRepository.currentUser,
        _isRefreshing,
        _isLoadingMore,
        _lastResult,
        _errorMessage
    ) { args ->
        @Suppress("MagicNumber")
        val category = args[0] as PlaceCategory?
        @Suppress("MagicNumber")
        val amenities = args[1] as Set<Amenity>
        @Suppress("MagicNumber")
        val order = args[2] as SortOrder
        @Suppress("MagicNumber")
        val query = args[3] as String
        @Suppress("MagicNumber")
        val location = args[4] as Pair<Double, Double>?
        @Suppress("MagicNumber")
        val user = args[5] as User?
        @Suppress("MagicNumber")
        val refreshing = args[6] as Boolean
        @Suppress("MagicNumber")
        val loadingMore = args[7] as Boolean
        @Suppress("MagicNumber")
        val paged = args[8] as PagedResult<Place>?
        @Suppress("MagicNumber")
        val error = args[9] as UiText?

        val places = paged?.items.orEmpty()
        val filtered = filterAndSort(
            places,
            FilterParams(location, user?.id, order, query, category, amenities)
        )

        UiState(
            places = filtered,
            selectedCategory = category,
            selectedAmenities = amenities,
            sortOrder = order,
            userLocation = location,
            currentUserId = user?.id,
            nearestUnavailable = order == SortOrder.NEAREST && location == null,
            isLoading = paged == null && error == null,
            isRefreshing = refreshing,
            errorMessage = error,
            hasMore = query.isBlank() && (paged?.hasMore ?: false),
            isLoadingMore = loadingMore,
            totalCount = paged?.items?.size ?: 0,
            searchQuery = query
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_SUBSCRIPTION_TIMEOUT_MS), UiState())

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
        viewModelScope.launch {
            _userLocation.value = locationProvider.getCurrentLocation()
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
            SortOrder.WORST_RATED -> compareBy<Place> { it.reviewsCount == 0 }
                .thenBy { it.averageRating }
                .thenByDescending { it.reviewsCount }
            SortOrder.NEAREST -> if (params.location != null) {
                compareBy<Place> { distance(it.latitude, it.longitude, params.location.first, params.location.second) }
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
        val normalizedName = name.normalizedForSearch()
        val normalizedQuery = params.query.normalizedForSearch()
        val matchesQuery = normalizedQuery.isBlank() || normalizedName.contains(normalizedQuery)
        val matchesCategory = params.category == null || category == params.category
        val matchesAmenities = params.amenities.isEmpty() || amenities.containsAll(params.amenities)
        val matchesOwner = params.order != SortOrder.ADDED_BY_ME ||
            params.userId?.let { ownerUserId == it } == true
        return matchesQuery && matchesCategory && matchesAmenities && matchesOwner
    }

    private fun Place.searchRank(query: String): Int {
        val normalizedName = name.normalizedForSearch()
        val normalizedQuery = query.normalizedForSearch()
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

    private data class FilterParams(
        val location: Pair<Double, Double>?,
        val userId: String?,
        val order: SortOrder,
        val query: String,
        val category: PlaceCategory?,
        val amenities: Set<Amenity>
    )

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    private fun String.normalizedForSearch(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("ł", "l", ignoreCase = true)
            .lowercase(Locale("pl", "PL"))
}
