package com.kidzone.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.Place
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.presentation.place.add.LOCATION_TIMEOUT_USER_MESSAGE
import com.kidzone.presentation.place.add.fetchCurrentLocation
import com.kidzone.presentation.place.add.hasLocationPermission
import com.kidzone.utils.OpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ile miejsc maksymalnie pokazujemy w sekcji "Top miejsca".
 *
 * 20 to świadomy kompromis: na ekranie horyzontalnej LazyRow user widzi
 * 1.5 karty naraz i może przewinąć do reszty. 20 daje zauważalnie więcej
 * niż 5 (czuje się jak "ranking", nie jak "podgląd"), a Firestore
 * `getTopPlaces(20)` mieści się w jednej operacji bez paginacji.
 */
private const val TOP_PLACES_LIMIT = 20

/**
 * Ile miejsc maksymalnie pokazujemy w sekcji "Blisko Ciebie".
 *
 * Symetrycznie do [TOP_PLACES_LIMIT] - obie sekcje wyglądają tak samo,
 * więc takie same liczby kart wzmacniają poczucie spójności. Filtrowanie
 * po promieniu [NEARBY_RADIUS_KM] dalej obowiązuje, więc w okolicy z
 * mniejszą bazą miejsc rząd po prostu będzie krótszy.
 */
private const val NEARBY_LIMIT = 20

/** Promień wyszukiwania pobliskich miejsc (km). */
private const val NEARBY_RADIUS_KM = 10.0

/**
 * ViewModel ekranu Home (zakładka "Start" w bottom navigation).
 *
 * Trzyma niezależnie dwa zestawy danych:
 *  - **Top miejsca** – pobierane raz przez [PlaceRepository.getTopPlaces].
 *  - **Blisko Ciebie** – pobierane dopiero gdy user nadał uprawnienie do
 *    lokalizacji; wymaga fixu z FusedLocationProviderClient. Sortujemy
 *    haversine'em po stronie klienta i bierzemy pierwsze [NEARBY_LIMIT].
 *
 * Decyzja: nie subskrybujemy [PlaceRepository.observePlaces] – to byłby
 * snapshot listener na całej kolekcji tylko po to żeby wziąć top 5,
 * marnotrawstwo. Zwykły one-shot fetch + [refresh] gdy user wraca na
 * ekran wystarczy dla MVP. Można w przyszłości podpiąć listener na
 * znormalizowanej kolekcji `top_places` jeżeli zaczniemy ją utrzymywać
 * po stronie Cloud Functions.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    /**
     * @property topPlaces lista najwyżej ocenianych miejsc
     * @property nearbyPlaces lista miejsc w okolicy ([NEARBY_RADIUS_KM])
     * @property isTopLoading true do zakończenia pierwszego fetcha topu
     * @property isNearbyLoading true gdy lecimy fetchem "blisko Ciebie"
     * @property locationGranted true gdy user nadał ACCESS_*_LOCATION
     * @property errorMessage błąd ostatniego fetcha (top lub nearby)
     */
    data class UiState(
        val topPlaces: List<Place> = emptyList(),
        val nearbyPlaces: List<Place> = emptyList(),
        val isTopLoading: Boolean = true,
        val isNearbyLoading: Boolean = false,
        val locationGranted: Boolean = false,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refreshLocationGranted()
        loadTopPlaces()
        if (_uiState.value.locationGranted) {
            loadNearbyPlaces()
        }
    }

    /**
     * Synchronizuje flagę [UiState.locationGranted] z aktualnym stanem
     * uprawnień systemowych. Wywoływać przy każdej zmianie cyklu życia
     * (np. powrót z ekranu ustawień), żeby UI nie został z nieaktualną
     * informacją.
     */
    fun refreshLocationGranted() {
        _uiState.update { it.copy(locationGranted = hasLocationPermission(appContext)) }
    }

    /** Wywoływać po pomyślnym requeście permissionsa – uruchamia load. */
    fun onLocationPermissionGranted() {
        _uiState.update { it.copy(locationGranted = true) }
        loadNearbyPlaces()
    }

    private fun loadTopPlaces() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTopLoading = true, errorMessage = null) }
            when (val result = placeRepository.getTopPlaces(TOP_PLACES_LIMIT)) {
                is OpResult.Success -> _uiState.update {
                    it.copy(topPlaces = result.data, isTopLoading = false)
                }
                is OpResult.Failure -> _uiState.update {
                    it.copy(
                        isTopLoading = false,
                        errorMessage = result.error.message
                            ?: "Nie udało się wczytać top miejsc"
                    )
                }
            }
        }
    }

    private fun loadNearbyPlaces() {
        if (!hasLocationPermission(appContext)) {
            _uiState.update { it.copy(locationGranted = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isNearbyLoading = true) }

            // fetchCurrentLocation może rzucić jeżeli FusedLocation explosion
            // (np. niezainicjalizowane Play Services), ale zwykle zwraca null
            // przy timeoucie / braku fixu (zob. LocationHelper.fetchCurrentLocation).
            // Łapiemy żeby UI nie pełzł crashem; przy null/błędzie zachowujemy
            // pustą listę "nearby" + ustawiamy delikatny komunikat zamiast
            // wieczystego spinnera.
            val location = runCatching { fetchCurrentLocation(appContext) }.getOrNull()
            if (location == null) {
                _uiState.update {
                    it.copy(
                        isNearbyLoading = false,
                        nearbyPlaces = emptyList(),
                        errorMessage = LOCATION_TIMEOUT_USER_MESSAGE
                    )
                }
                return@launch
            }
            val (lat, lng) = location
            when (val result = placeRepository.getPlacesNear(lat, lng, NEARBY_RADIUS_KM)) {
                is OpResult.Success -> {
                    // Repo MVP zwraca wszystkie miejsca – sortujemy haversine'em
                    // i bierzemy pierwsze NEARBY_LIMIT, plus filtrujemy ręcznie
                    // po promieniu (bo getPlacesNear w aktualnej implementacji
                    // jeszcze nie respektuje radiusa).
                    val sorted = result.data
                        .map { it to haversineKm(lat, lng, it.latitude, it.longitude) }
                        .filter { it.second <= NEARBY_RADIUS_KM }
                        .sortedBy { it.second }
                        .take(NEARBY_LIMIT)
                        .map { it.first }
                    _uiState.update {
                        it.copy(nearbyPlaces = sorted, isNearbyLoading = false)
                    }
                }
                is OpResult.Failure -> _uiState.update {
                    it.copy(
                        isNearbyLoading = false,
                        errorMessage = result.error.message
                            ?: "Nie udało się wczytać pobliskich miejsc"
                    )
                }
            }
        }
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
