package com.kidzone.presentation.place.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.presentation.place.add.fetchCurrentLocation
import com.kidzone.presentation.place.add.hasLocationPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val PAGE_SIZE = 20

/**
 * ViewModel ekranu listy miejsc.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class PlaceListViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    enum class SortOrder(val label: String) {
        NEAREST("Najbli\u017csze"),
        RECENTLY_ADDED("Ostatnio dodane"),
        ADDED_BY_ME("Dodane przez Ciebie"),
        BEST_RATED("Najlepiej oceniane"),
        WORST_RATED("Najgorzej oceniane")
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
        val errorMessage: String? = null,
        val hasMore: Boolean = false,
        val totalCount: Int = 0,
        val searchQuery: String = ""
    )

    private val selectedCategory = MutableStateFlow<PlaceCategory?>(null)
    private val selectedAmenities = MutableStateFlow<Set<Amenity>>(emptySet())
    private val sortOrder = MutableStateFlow(SortOrder.NEAREST)
    private val searchQuery = MutableStateFlow("")
    private val userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    private val visibleCount = MutableStateFlow(PAGE_SIZE)

    private sealed interface PlacesLoad {
        data object Loading : PlacesLoad
        data class Success(val list: List<Place>) : PlacesLoad
        data class Error(val message: String) : PlacesLoad
    }

    private val placesLoad: Flow<PlacesLoad> = combine(
        selectedCategory,
        searchQuery.debounce { if (it.isEmpty()) 0L else 300L }.distinctUntilChanged()
    ) { category, query -> category to query }
        .flatMapLatest { (category, query) ->
            placeRepository.observePlaces(category, query)
                .map<List<Place>, PlacesLoad> { PlacesLoad.Success(it) }
                .onStart { emit(PlacesLoad.Loading) }
                .catch { e ->
                    emit(PlacesLoad.Error(e.message ?: "Nie uda\u0142o si\u0119 wczyta\u0107 listy miejsc"))
                }
        }

    private val currentUserIdFlow: Flow<String?> = authRepository.currentUser
        .map { it?.id }

    private data class SortContext(
        val sortOrder: SortOrder,
        val userLocation: Pair<Double, Double>?,
        val currentUserId: String?
    )

    private val sortContextFlow: Flow<SortContext> = combine(
        sortOrder,
        userLocation,
        currentUserIdFlow
    ) { sort, loc, uid -> SortContext(sort, loc, uid) }

    // Backup current results to avoid flickering during loading
    private var lastPlaces: List<Place> = emptyList()
    private var lastTotalCount: Int = 0

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<UiState> = combine(
        placesLoad,
        selectedCategory,
        selectedAmenities,
        sortContextFlow,
        _isRefreshing,
        visibleCount,
        searchQuery
    ) { args ->
        val load = args[0] as PlacesLoad
        val category = args[1] as PlaceCategory?
        val amenities = args[2] as Set<Amenity>
        val sortCtx = args[3] as SortContext
        val refreshing = args[4] as Boolean
        val visible = args[5] as Int
        val query = args[6] as String

        when (load) {
            PlacesLoad.Loading -> UiState(
                selectedCategory = category,
                selectedAmenities = amenities,
                sortOrder = sortCtx.sortOrder,
                userLocation = sortCtx.userLocation,
                currentUserId = sortCtx.currentUserId,
                isLoading = true,
                isRefreshing = refreshing,
                searchQuery = query,
                places = lastPlaces,
                totalCount = lastTotalCount
            )
            is PlacesLoad.Success -> {
                val filtered = load.list
                    .filter { place -> amenities.all { it in place.amenities } }
                    .let { list ->
                        if (sortCtx.sortOrder == SortOrder.ADDED_BY_ME) {
                            val uid = sortCtx.currentUserId
                            if (uid.isNullOrBlank()) emptyList()
                            else list.filter { it.ownerUserId == uid }
                        } else list
                    }

                val sorted = applySort(filtered, sortCtx.sortOrder, sortCtx.userLocation)
                val totalCount = sorted.size
                val paginated = sorted.take(visible)
                
                lastPlaces = paginated
                lastTotalCount = totalCount

                UiState(
                    places = paginated,
                    selectedCategory = category,
                    selectedAmenities = amenities,
                    sortOrder = sortCtx.sortOrder,
                    userLocation = sortCtx.userLocation,
                    currentUserId = sortCtx.currentUserId,
                    nearestUnavailable = sortCtx.sortOrder == SortOrder.NEAREST && sortCtx.userLocation == null,
                    isLoading = false,
                    isRefreshing = refreshing,
                    hasMore = paginated.size < totalCount,
                    totalCount = totalCount,
                    searchQuery = query
                )
            }
            is PlacesLoad.Error -> {
                lastPlaces = emptyList()
                lastTotalCount = 0
                UiState(
                    selectedCategory = category,
                    selectedAmenities = amenities,
                    sortOrder = sortCtx.sortOrder,
                    userLocation = sortCtx.userLocation,
                    currentUserId = sortCtx.currentUserId,
                    isLoading = false,
                    isRefreshing = refreshing,
                    errorMessage = load.message,
                    searchQuery = query
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState()
    )

    init {
        refreshLocation()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        visibleCount.value = PAGE_SIZE
    }

    fun onCategorySelected(category: PlaceCategory?) {
        selectedCategory.value = category
        visibleCount.value = PAGE_SIZE
    }

    fun onAmenityToggled(amenity: Amenity) {
        selectedAmenities.update { current ->
            if (amenity in current) current - amenity else current + amenity
        }
        visibleCount.value = PAGE_SIZE
    }

    fun onAmenitiesCleared() {
        selectedAmenities.value = emptySet()
        visibleCount.value = PAGE_SIZE
    }

    fun onSortOrderChange(order: SortOrder) {
        sortOrder.value = order
        visibleCount.value = PAGE_SIZE
        if (order == SortOrder.NEAREST && userLocation.value == null) {
            refreshLocation()
        }
    }

    fun loadMore() {
        visibleCount.update { it + PAGE_SIZE }
    }

    fun refreshLocation() {
        if (!hasLocationPermission(appContext)) return
        viewModelScope.launch {
            val coords = runCatching { fetchCurrentLocation(appContext) }.getOrNull()
            if (coords != null) userLocation.value = coords
        }
    }

    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            if (hasLocationPermission(appContext)) {
                val coords = runCatching { fetchCurrentLocation(appContext) }.getOrNull()
                if (coords != null) userLocation.value = coords
            }
            kotlinx.coroutines.delay(300)
            _isRefreshing.value = false
        }
    }

    private fun applySort(list: List<Place>, sortOrder: SortOrder, userLocation: Pair<Double, Double>?): List<Place> = 
        when (sortOrder) {
            SortOrder.NEAREST -> {
                if (userLocation != null) {
                    val (lat, lng) = userLocation
                    list.sortedBy { haversineKm(lat, lng, it.latitude, it.longitude) }
                } else {
                    list.sortedByDescending { it.createdAtMillis }
                }
            }
            SortOrder.RECENTLY_ADDED -> list.sortedByDescending { it.createdAtMillis }
            SortOrder.ADDED_BY_ME -> list.sortedByDescending { it.createdAtMillis }
            SortOrder.BEST_RATED -> list.sortedWith(
                compareByDescending<Place> { it.averageRating }
                    .thenByDescending { it.reviewsCount }
                    .thenByDescending { it.createdAtMillis }
            )
            SortOrder.WORST_RATED -> list.sortedWith(
                compareBy<Place> { it.reviewsCount == 0 }
                    .thenBy { it.averageRating }
                    .thenByDescending { it.createdAtMillis }
            )
        }
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).let { it * it } +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2).let { it * it }
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}
