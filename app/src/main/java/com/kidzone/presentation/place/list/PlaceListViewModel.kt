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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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

/**
 * Maks. liczba miejsc pokazywanych na liście.
 *
 * 100 to świadoma decyzja - lista nie jest paginowana, ale Firestore zwraca
 * cały zbiór dla MVP. Sortujemy po stronie klienta i bierzemy "head" z
 * pierwszych 100 wyników. Dla obecnej skali apki to grubo z zapasem;
 * przy realnym wzroście bazy miejsc trzeba będzie dorobić paginację.
 *
 * Twardy limit ma też walor wydajnościowy: LazyColumn nie musi materializować
 * setek elementów + recompose pasków filtrów jest tańszy.
 */
private const val LIST_LIMIT = 100

/**
 * ViewModel ekranu listy miejsc.
 *
 * Subskrybuje [PlaceRepository.observePlaces] - snapshot listener Firestore
 * automatycznie pushuje nowe miejsca do UI bez ręcznego refresh-u.
 *
 * Filtrowanie:
 *  - **kategoria** - po stronie Firestore (`whereEqualTo("category", ...)`).
 *    Zmiana rebinduje strumień przez `flatMapLatest`, stary listener jest
 *    unsubscribowany.
 *  - **udogodnienia** - po stronie klienta (Firestore w jednym query
 *    nie umie AND po wielu `array-contains`). Działa na żywo na strumieniu
 *    streamowanej listy, więc wciąż mamy real-time updates.
 *
 * Sortowanie - 5 trybów (zob. [SortOrder]) wybierane przez UI:
 *  - **NEAREST** (domyślny, gdy znamy lokalizację): haversine od pozycji usera.
 *    Bez fixu lokalizacji fallbackujemy do RECENTLY_ADDED, a state ujawnia
 *    flagę `nearestUnavailable`, by UI mógł pokazać zachętę do włączenia GPS.
 *  - **RECENTLY_ADDED**: createdAtMillis desc.
 *  - **ADDED_BY_ME**: filtruje do `ownerUserId == currentUserId` i sortuje
 *    po createdAtMillis desc. Gdy user jest wylogowany - pusta lista.
 *  - **BEST_RATED** / **WORST_RATED**: averageRating (desc / asc), z drugorzędnym
 *    sortem po reviewsCount, żeby miejsca z 0 ocen nie wskakiwały na pierwszą
 *    pozycję "ex aequo".
 *
 * Po sortowaniu i filtrach lista jest twardo cięta do [LIST_LIMIT] elementów.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaceListViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    /**
     * Tryby sortowania listy. Etykiety po polsku, bo idą wprost do UI
     * (DropdownMenuItem-ów). Kolejność na enumie odpowiada kolejności w
     * dropdownie - "Najbliższe" pierwsze, jako domyślne.
     */
    enum class SortOrder(val label: String) {
        NEAREST("Najbliższe"),
        RECENTLY_ADDED("Ostatnio dodane"),
        ADDED_BY_ME("Dodane przez Ciebie"),
        BEST_RATED("Najlepiej oceniane"),
        WORST_RATED("Najgorzej oceniane")
    }

    /**
     * @property places aktualnie pokazywana lista (po obu filtrach + sortowaniu,
     *   przycięta do [LIST_LIMIT])
     * @property selectedCategory filtr kategorii; null = wszystkie
     * @property selectedAmenities multi-select udogodnień (logika AND)
     * @property sortOrder aktualne sortowanie listy
     * @property userLocation (lat, lng) lub null gdy brak fixu / brak permission
     * @property currentUserId id zalogowanego usera; null = wylogowany
     * @property nearestUnavailable true gdy user wybrał NEAREST, ale brak
     *   lokalizacji - wtedy lista jest sortowana RECENTLY_ADDED jako fallback,
     *   a UI może pokazać banner "Włącz lokalizację, by sortować po odległości".
     * @property isLoading true do pierwszego emita z Firestore (po rebindzie też)
     * @property errorMessage komunikat błędu z snapshot listenera
     */
    data class UiState(
        val places: List<Place> = emptyList(),
        val selectedCategory: PlaceCategory? = null,
        val selectedAmenities: Set<Amenity> = emptySet(),
        val sortOrder: SortOrder = SortOrder.NEAREST,
        val userLocation: Pair<Double, Double>? = null,
        val currentUserId: String? = null,
        val nearestUnavailable: Boolean = false,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    private val selectedCategory = MutableStateFlow<PlaceCategory?>(null)
    private val selectedAmenities = MutableStateFlow<Set<Amenity>>(emptySet())
    private val sortOrder = MutableStateFlow(SortOrder.NEAREST)

    /**
     * Lokalizacja usera - fetched async po nadaniu uprawnienia (zob. [refreshLocation]).
     * MutableStateFlow, bo wartość zmienia się w czasie (init -> fetch -> success).
     */
    private val userLocation = MutableStateFlow<Pair<Double, Double>?>(null)

    /** Wewnętrzny model wyniku ze strumienia Firestore. */
    private sealed interface PlacesLoad {
        data object Loading : PlacesLoad
        data class Success(val list: List<Place>) : PlacesLoad
        data class Error(val message: String) : PlacesLoad
    }

    private val placesLoad: Flow<PlacesLoad> = selectedCategory
        .flatMapLatest { category ->
            placeRepository.observePlaces(category)
                .map<List<Place>, PlacesLoad> { PlacesLoad.Success(it) }
                .onStart { emit(PlacesLoad.Loading) }
                .catch { e ->
                    emit(PlacesLoad.Error(e.message ?: "Nie udało się wczytać listy miejsc"))
                }
        }

    /** Strumień zalogowanego usera - tylko id. */
    private val currentUserIdFlow: Flow<String?> = authRepository.currentUser
        .map { it?.id }

    /**
     * Pakujemy "kontekst sortowania" (sortOrder + lokalizacja + uid) w jeden
     * Triple, żeby zmieścić wszystko w 4-argumentowej wersji `combine`.
     */
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

    val uiState: StateFlow<UiState> = combine(
        placesLoad,
        selectedCategory,
        selectedAmenities,
        sortContextFlow
    ) { load, category, amenities, sortCtx ->
        when (load) {
            PlacesLoad.Loading -> UiState(
                selectedCategory = category,
                selectedAmenities = amenities,
                sortOrder = sortCtx.sortOrder,
                userLocation = sortCtx.userLocation,
                currentUserId = sortCtx.currentUserId,
                isLoading = true,
                errorMessage = null,
                places = emptyList()
            )
            is PlacesLoad.Success -> {
                val filtered = load.list
                    .filter { place -> amenities.all { it in place.amenities } }
                    .let { list ->
                        // ADDED_BY_ME działa równocześnie jako filtr i jako
                        // sortowanie - filtrujemy do swoich miejsc, sort
                        // dzieje się dalej w applySort.
                        if (sortCtx.sortOrder == SortOrder.ADDED_BY_ME) {
                            val uid = sortCtx.currentUserId
                            if (uid.isNullOrBlank()) emptyList()
                            else list.filter { it.ownerUserId == uid }
                        } else list
                    }

                val sorted = applySort(
                    list = filtered,
                    sortOrder = sortCtx.sortOrder,
                    userLocation = sortCtx.userLocation
                )

                UiState(
                    places = sorted.take(LIST_LIMIT),
                    selectedCategory = category,
                    selectedAmenities = amenities,
                    sortOrder = sortCtx.sortOrder,
                    userLocation = sortCtx.userLocation,
                    currentUserId = sortCtx.currentUserId,
                    nearestUnavailable = sortCtx.sortOrder == SortOrder.NEAREST &&
                        sortCtx.userLocation == null,
                    isLoading = false,
                    errorMessage = null
                )
            }
            is PlacesLoad.Error -> UiState(
                selectedCategory = category,
                selectedAmenities = amenities,
                sortOrder = sortCtx.sortOrder,
                userLocation = sortCtx.userLocation,
                currentUserId = sortCtx.currentUserId,
                isLoading = false,
                errorMessage = load.message,
                places = emptyList()
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState()
    )

    init {
        // Próbujemy pobrać lokalizację już na start - jeśli user wcześniej
        // nadał permission, lista od razu pojawi się posortowana po odległości.
        // Bez permission [refreshLocation] nic nie robi (no-op).
        refreshLocation()
    }

    fun onCategorySelected(category: PlaceCategory?) {
        selectedCategory.value = category
    }

    fun onAmenityToggled(amenity: Amenity) {
        selectedAmenities.update { current ->
            if (amenity in current) current - amenity else current + amenity
        }
    }

    fun onAmenitiesCleared() {
        selectedAmenities.value = emptySet()
    }

    fun onSortOrderChange(order: SortOrder) {
        sortOrder.value = order
        // Jeśli user wybrał NEAREST a nie mamy jeszcze fixu, próbujemy go
        // pobrać "w locie" - bez tego user musiałby ręcznie szarpnąć
        // ekran by wymusić odświeżenie.
        if (order == SortOrder.NEAREST && userLocation.value == null) {
            refreshLocation()
        }
    }

    /**
     * Asynchroniczny fetch lokalizacji przez [fetchCurrentLocation].
     *
     * Bezpiecznie wołać wielokrotnie (np. po nadaniu uprawnienia z UI).
     * Bez uprawnienia: no-op. Przy timeoucie / braku fixu pozostawiamy
     * `userLocation = null` - UI pokaże banner "Włącz lokalizację".
     */
    fun refreshLocation() {
        if (!hasLocationPermission(appContext)) return
        viewModelScope.launch {
            val coords = runCatching { fetchCurrentLocation(appContext) }.getOrNull()
            if (coords != null) userLocation.value = coords
        }
    }

    private fun applySort(
        list: List<Place>,
        sortOrder: SortOrder,
        userLocation: Pair<Double, Double>?
    ): List<Place> = when (sortOrder) {
        SortOrder.NEAREST -> {
            if (userLocation != null) {
                val (lat, lng) = userLocation
                list.sortedBy { haversineKm(lat, lng, it.latitude, it.longitude) }
            } else {
                // Brak lokalizacji = nie udajemy, że umiemy posortować po
                // odległości. Spadamy na "ostatnio dodane" jako sensowny
                // domyślny porządek (najnowsze są najczęściej najbardziej
                // istotne dla rodziców szukających "co nowego w okolicy").
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
            // Świadomie miejsca z 0 ocenami lądują na koniec (a nie na
            // początku jako "najgorsze") - reviewsCount > 0 ma pierwszeństwo.
            compareBy<Place> { it.reviewsCount == 0 }
                .thenBy { it.averageRating }
                .thenByDescending { it.createdAtMillis }
        )
    }
}

/** Odległość w km między dwoma punktami (formuła haversine). */
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
