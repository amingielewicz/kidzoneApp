package com.kidzone.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.R
import com.kidzone.domain.model.GeoBounds
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.utils.OpResult
import com.kidzone.utils.UiText
import com.kidzone.utils.toPlacesErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val VIEWPORT_DEBOUNCE_MS = 300L
private const val VIEWPORT_CACHE_SIZE = 20
private const val MAP_PLACES_LIMIT = 500
private const val CACHE_EXPIRATION_MS = 600_000L
private const val FLOW_SUBSCRIPTION_TIMEOUT_MS = 5000L
private const val TOP_RATED_THRESHOLD = 4.0

private val MAP_ERROR_FALLBACK = UiText.StringResource(R.string.error_fetch_places)

/**
 * ViewModel dla MapScreen.
 */
@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MapViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    data class UiState(
        val places: List<Place> = emptyList(),
        val selectedCategory: PlaceCategory? = null,
        val topRatedOnly: Boolean = false,
        val addedByMeOnly: Boolean = false,
        val selectedPlaceId: String? = null,
        val isLoading: Boolean = false,
        val errorMessage: UiText? = null
    )

    private val selectedCategory = MutableStateFlow<PlaceCategory?>(null)
    private val topRatedOnly = MutableStateFlow(false)
    private val addedByMeOnly = MutableStateFlow(false)
    private val selectedPlaceId = MutableStateFlow<String?>(null)
    private val viewport = MutableStateFlow<GeoBounds?>(null)
    private val retryRequest = MutableStateFlow(0)

    private sealed interface PlacesLoad {
        data class Loading(val previous: List<Place>) : PlacesLoad
        data class Success(val list: List<Place>) : PlacesLoad
        data class Error(val message: UiText, val previous: List<Place>) : PlacesLoad
    }

    private val viewportCache = object : LinkedHashMap<String, CacheEntry>(
        VIEWPORT_CACHE_SIZE,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(eldest: Map.Entry<String, CacheEntry>?): Boolean {
            return size > VIEWPORT_CACHE_SIZE
        }
    }

    private data class CacheEntry(val places: List<Place>, val createdAtMillis: Long)

    private val placesLoad = combine(
        viewport
            .filterNotNull()
            .distinctUntilChanged { old, new -> old.isSimilarTo(new) }
            .debounce(VIEWPORT_DEBOUNCE_MS),
        selectedCategory,
        retryRequest
    ) { bounds, category, retry -> Triple(bounds, category, retry) }
        .flatMapLatest { (bounds, category, _) ->
            val cacheKey = "${category?.name ?: "all"}_${bounds.geohashPrefix()}"
            val cached = viewportCache[cacheKey]

            if (cached != null && System.currentTimeMillis() - cached.createdAtMillis < CACHE_EXPIRATION_MS) {
                flowOf<PlacesLoad>(PlacesLoad.Success(cached.places))
            } else {
                flow<PlacesLoad> {
                    emit(PlacesLoad.Loading(lastPlaces))
                    when (val result = placeRepository.getPlacesInBounds(bounds, category, MAP_PLACES_LIMIT)) {
                        is OpResult.Success -> {
                            viewportCache[cacheKey] = CacheEntry(result.data, System.currentTimeMillis())
                            emit(PlacesLoad.Success(result.data))
                        }
                        is OpResult.Failure -> {
                            emit(PlacesLoad.Error(result.error.toPlacesErrorMessage(MAP_ERROR_FALLBACK), lastPlaces))
                        }
                    }
                }
            }
        }

    private var lastPlaces: List<Place> = emptyList()

    val uiState: StateFlow<UiState> = combine(
        placesLoad,
        selectedCategory,
        topRatedOnly,
        addedByMeOnly,
        selectedPlaceId,
        authRepository.currentUser
    ) { args ->
        @Suppress("MagicNumber")
        val load = args[0] as PlacesLoad
        @Suppress("MagicNumber")
        val category = args[1] as PlaceCategory?
        @Suppress("MagicNumber")
        val topRated = args[2] as Boolean
        @Suppress("MagicNumber")
        val addedByMe = args[3] as Boolean
        @Suppress("MagicNumber")
        val selectedId = args[4] as String?
        @Suppress("MagicNumber")
        val user = args[5] as User?

        val rawPlaces = when (load) {
            is PlacesLoad.Loading -> load.previous
            is PlacesLoad.Success -> load.list
            is PlacesLoad.Error -> load.previous
        }
        lastPlaces = rawPlaces

        val filtered = rawPlaces.filter { place ->
            val matchesTopRated = !topRated || place.averageRating >= TOP_RATED_THRESHOLD
            val matchesAddedByMe = !addedByMe || (user != null && place.ownerUserId == user.id)
            matchesTopRated && matchesAddedByMe
        }

        UiState(
            places = filtered,
            selectedCategory = category,
            topRatedOnly = topRated,
            addedByMeOnly = addedByMe,
            selectedPlaceId = selectedId,
            isLoading = load is PlacesLoad.Loading,
            errorMessage = if (load is PlacesLoad.Error) load.message else null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_SUBSCRIPTION_TIMEOUT_MS), UiState())

    fun onCategorySelect(category: PlaceCategory?) {
        selectedCategory.value = category
    }

    fun toggleTopRated() {
        topRatedOnly.update { !it }
    }

    fun toggleAddedByMe() {
        addedByMeOnly.update { !it }
    }

    fun selectPlace(placeId: String?) {
        selectedPlaceId.value = placeId
    }

    fun onViewportChanged(bounds: GeoBounds) {
        viewport.value = bounds
    }

    fun retry() {
        if (viewport.value != null) retryRequest.update { it + 1 }
    }
}
