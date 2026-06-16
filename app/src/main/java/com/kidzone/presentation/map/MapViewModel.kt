package com.kidzone.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.GeoBounds
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.utils.OpResult
import com.kidzone.utils.toPlacesErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * ViewModel ekranu mapy.
 *
 * Pobiera ograniczony zestaw miejsc dla aktualnego viewportu. Zmiany kamery
 * są debouncowane, a ostatnie obszary są cache'owane w pamięci ViewModelu.
 *
 * Filtrowanie:
 *  - **kategoria** – po stronie Firestore razem z bucketami geohash.
 *  - **najlepiej oceniane** – po stronie klienta, próg [TOP_RATED_THRESHOLD].
 *    Trzymamy lokalnie, bo Firestore w jednym query nie umie
 *    `whereEqualTo(category) AND whereGreaterThan(averageRating)` bez
 *    composite indexa, a dla MVP nie chcemy go zmuszać tworzyć.
 *
 * Filter "darmowe" jest wzmiankowany w docs ekranu, ale `Place` nie ma na
 * dziś pola ceny – świadomie pomijamy do czasu rozszerzenia modelu.
 */
@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * @property places aktualnie pokazywane miejsca (po filtrach)
     * @property selectedCategory filtr kategorii; null = wszystkie
     * @property topRatedOnly true = pokaż tylko `averageRating >= 4.0`
     * @property addedByMeOnly true = pokaż tylko miejsca dodane przez
     *   zalogowanego usera (`Place.ownerUserId == currentUser.id`).
     *   Gdy user jest wylogowany, filtr jest "no-op" - zawsze zwraca pustą
     *   listę, a UI normalnie chowa chip (chowanie ChIP'a robi się w
     *   ekranie, nie tutaj).
     * @property selectedPlaceId id pinezki, na której pokazujemy bottom sheet
     * @property currentUserId id zalogowanego usera; null = wylogowany.
     *   Wystawiamy w state, żeby UI wiedział czy w ogóle pokazywać chip
     *   "Dodane przez Ciebie".
     * @property isLoading true do pierwszego emita ze strumienia
     * @property errorMessage komunikat błędu z snapshot listenera
     */
    data class UiState(
        val places: List<Place> = emptyList(),
        val selectedCategory: PlaceCategory? = null,
        val topRatedOnly: Boolean = false,
        val addedByMeOnly: Boolean = false,
        val selectedPlaceId: String? = null,
        val currentUserId: String? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    private val selectedCategory = MutableStateFlow<PlaceCategory?>(null)
    private val topRatedOnly = MutableStateFlow(false)
    private val addedByMeOnly = MutableStateFlow(false)
    private val selectedPlaceId = MutableStateFlow<String?>(null)
    private val viewport = MutableStateFlow<GeoBounds?>(null)

    private sealed interface PlacesLoad {
        data class Loading(val previous: List<Place>) : PlacesLoad
        data class Success(val list: List<Place>) : PlacesLoad
        data class Error(val message: String, val previous: List<Place>) : PlacesLoad
    }

    private data class ViewportRequest(
        val bounds: GeoBounds,
        val category: PlaceCategory?
    )

    private data class CacheEntry(
        val places: List<Place>,
        val createdAtMillis: Long
    )

    private val viewportCache = object : LinkedHashMap<String, CacheEntry>(
        VIEWPORT_CACHE_SIZE,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, CacheEntry>?
        ): Boolean = size > VIEWPORT_CACHE_SIZE
    }

    private var lastLoadedPlaces: List<Place> = emptyList()

    private val debouncedViewport = viewport
        .filterNotNull()
        .debounce(VIEWPORT_DEBOUNCE_MS)
        .distinctUntilChangedBy { bounds -> viewportKey(bounds) }

    private val placesLoad: Flow<PlacesLoad> = combine(
        debouncedViewport,
        selectedCategory
    ) { bounds, category ->
        ViewportRequest(bounds, category)
    }.flatMapLatest { request ->
        flow {
            val key = viewportKey(request.bounds, request.category)
            val cached = viewportCache[key]
                ?.takeIf { System.currentTimeMillis() - it.createdAtMillis <= VIEWPORT_CACHE_TTL_MS }
            if (cached != null) {
                lastLoadedPlaces = cached.places
                emit(PlacesLoad.Success(cached.places))
                return@flow
            }

            emit(PlacesLoad.Loading(lastLoadedPlaces))
            when (
                val result = placeRepository.getPlacesInBounds(
                    bounds = request.bounds,
                    category = request.category,
                    limit = MAP_MARKERS_LIMIT
                )
            ) {
                is OpResult.Success -> {
                    lastLoadedPlaces = result.data
                    viewportCache[key] = CacheEntry(result.data, System.currentTimeMillis())
                    emit(PlacesLoad.Success(result.data))
                }
                is OpResult.Failure -> {
                    emit(
                        PlacesLoad.Error(
                            message = result.error.toPlacesErrorMessage(MAP_ERROR_FALLBACK),
                            previous = lastLoadedPlaces
                        )
                    )
                }
            }
        }.catch { error ->
            if (error is CancellationException) throw error
            emit(
                PlacesLoad.Error(
                    message = error.toPlacesErrorMessage(MAP_ERROR_FALLBACK),
                    previous = lastLoadedPlaces
                )
            )
        }
    }

    /**
     * Strumień zalogowanego usera ścieśniony do samego id (lub null gdy
     * wylogowany). Łączymy go z filtrami "addedByMeOnly", bo bez user-id
     * filtr nie ma sensu (UI w MapScreen.kt chowa wtedy cały chip).
     */
    private val currentUserIdFlow: Flow<String?> = authRepository.currentUser
        .map { it?.id }

    /**
     * Wszystkie boolean/string filtry zwijamy w jednego Triple-a, żeby zmieścić
     * się w 4-argumentowej wersji `combine` razem z [placesLoad],
     * [selectedCategory] i [selectedPlaceId]. Bez tego musielibyśmy iść w
     * vararg-ową wersję `combine`, która gubi typowanie.
     */
    private data class Filters(
        val topRatedOnly: Boolean,
        val addedByMeOnly: Boolean,
        val currentUserId: String?
    )

    private val filtersFlow: Flow<Filters> = combine(
        topRatedOnly,
        addedByMeOnly,
        currentUserIdFlow
    ) { top, mine, uid -> Filters(top, mine, uid) }

    val uiState: StateFlow<UiState> = combine(
        placesLoad,
        selectedCategory,
        filtersFlow,
        selectedPlaceId
    ) { load, category, filters, sel ->
        when (load) {
            is PlacesLoad.Loading -> UiState(
                places = load.previous,
                selectedCategory = category,
                topRatedOnly = filters.topRatedOnly,
                addedByMeOnly = filters.addedByMeOnly,
                currentUserId = filters.currentUserId,
                selectedPlaceId = sel,
                isLoading = true
            )
            is PlacesLoad.Success -> {
                // Filtrowanie: najpierw kategoria (już zaaplikowana w
                // observePlaces po stronie Firestore), potem topRated,
                // potem addedByMe. addedByMe to "no-op" gdy user jest
                // wylogowany - filtrujemy do pustej listy zamiast
                // potencjalnie pokazać cudze miejsca.
                val afterTopRated = if (filters.topRatedOnly) {
                    load.list.filter { it.averageRating >= TOP_RATED_THRESHOLD }
                } else {
                    load.list
                }
                val filtered = if (filters.addedByMeOnly) {
                    val uid = filters.currentUserId
                    if (uid.isNullOrBlank()) emptyList()
                    else afterTopRated.filter { it.ownerUserId == uid }
                } else {
                    afterTopRated
                }
                UiState(
                    places = filtered,
                    selectedCategory = category,
                    topRatedOnly = filters.topRatedOnly,
                    addedByMeOnly = filters.addedByMeOnly,
                    currentUserId = filters.currentUserId,
                    // Jeśli wybrane miejsce wypadło z listy po zmianie filtra –
                    // kasujemy zaznaczenie, żeby sheet się zamknął sam.
                    selectedPlaceId = sel?.takeIf { id -> filtered.any { it.id == id } },
                    isLoading = false,
                    errorMessage = null
                )
            }
            is PlacesLoad.Error -> UiState(
                places = load.previous,
                selectedCategory = category,
                topRatedOnly = filters.topRatedOnly,
                addedByMeOnly = filters.addedByMeOnly,
                currentUserId = filters.currentUserId,
                selectedPlaceId = sel,
                isLoading = false,
                errorMessage = load.message
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState()
    )

    fun onCategorySelected(category: PlaceCategory?) {
        selectedCategory.value = category
    }

    fun onViewportChanged(bounds: GeoBounds) {
        viewport.value = bounds
    }

    fun toggleTopRated() {
        topRatedOnly.value = !topRatedOnly.value
    }

    /**
     * Włącza/wyłącza filtr "tylko dodane przeze mnie". Bezpiecznie wołać
     * nawet dla wylogowanego usera - filtr efektywnie wybierze pustą listę
     * (zob. logika w `combine`), ale UI w MapScreen i tak chowa wtedy chip.
     */
    fun toggleAddedByMe() {
        addedByMeOnly.value = !addedByMeOnly.value
    }

    /** Ustaw `null`, żeby zamknąć bottom sheet. */
    fun onPlaceSelected(id: String?) {
        selectedPlaceId.value = id
    }

    private companion object {
        /**
         * Próg "najlepiej oceniane" – świadomie wybrany jako 4.0 (a nie 4.5),
         * żeby przy małej liczbie opinii sekcja nie była pusta. Łatwo zmienić
         * gdy bazka miejsc urośnie.
         */
        const val TOP_RATED_THRESHOLD = 4.0
        const val MAP_MARKERS_LIMIT = 1000
        const val VIEWPORT_DEBOUNCE_MS = 450L
        const val VIEWPORT_CACHE_TTL_MS = 5 * 60 * 1000L
        const val VIEWPORT_CACHE_SIZE = 12
        const val MAP_ERROR_FALLBACK = "Nie udało się wczytać miejsc na mapie"
    }

    private fun viewportKey(bounds: GeoBounds, category: PlaceCategory? = null): String =
        listOf(
            (bounds.centerLatitude * 1000).roundToInt(),
            (bounds.centerLongitude * 1000).roundToInt(),
            ((bounds.north - bounds.south) * 1000).roundToInt(),
            ((longitudeSpan(bounds)) * 1000).roundToInt(),
            category?.name.orEmpty()
        ).joinToString(":")

    private fun longitudeSpan(bounds: GeoBounds): Double =
        if (bounds.west <= bounds.east) {
            bounds.east - bounds.west
        } else {
            (180.0 - bounds.west) + (bounds.east + 180.0)
        }
}
