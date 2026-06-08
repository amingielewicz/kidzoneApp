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
 * To lokalny ranking: najpierw ograniczamy bazę do miejsc w pobliżu usera,
 * a potem wybieramy 20 najlepiej ocenianych.
 */
private const val TOP_PLACES_LIMIT = 20

/**
 * Ile miejsc maksymalnie pokazujemy w sekcji "Blisko Ciebie".
 *
 * Symetrycznie do [TOP_PLACES_LIMIT] - obie sekcje wyglądają tak samo,
 * więc takie same liczby kart wzmacniają poczucie spójności.
 */
private const val NEARBY_LIMIT = 20

/** Promień lokalnego rankingu "Top miejsca" (km). */
private const val TOP_PLACES_RADIUS_KM = 10.0

/**
 * Promień techniczny fetcha miejsc do sekcji startowych.
 *
 * Aktualne repo MVP i tak zwraca całą kolekcję, ale podajemy duży promień,
 * żeby przyszła implementacja geo-query miała sensowny limit dla "Blisko Ciebie".
 */
private const val HOME_PLACES_FETCH_RADIUS_KM = 50.0

/**
 * ViewModel ekranu Home (zakładka "Start" w bottom navigation).
 *
 * Trzyma dwa zestawy danych zależne od aktualnej lokalizacji:
 *  - **Top miejsca** – 20 najlepiej ocenianych miejsc w promieniu
 *    [TOP_PLACES_RADIUS_KM] od użytkownika (lokalny ranking).
 *  - **Blisko Ciebie** – 20 najbliższych miejsc, bez względu na ocenę i liczbę
 *    opinii.
 *
 * Obie sekcje korzystają z jednego pobrania lokalizacji i jednego fetcha miejsc,
 * żeby nie dublować pracy FusedLocationProviderClient / Firestore.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    /**
     * @property topPlaces lokalny ranking najlepiej ocenianych miejsc w pobliżu
     * @property nearbyPlaces lista najbliższych miejsc bez względu na ocenę
     * @property isTopLoading true do zakończenia pierwszego fetcha topu
     * @property isNearbyLoading true gdy lecimy fetchem "blisko Ciebie"
     * @property isRefreshing true podczas pull-to-refresh (kręci spinner)
     * @property locationGranted true gdy user nadał ACCESS_*_LOCATION
     * @property errorMessage błąd ostatniego fetcha (top lub nearby)
     */
    data class UiState(
        val topPlaces: List<Place> = emptyList(),
        val nearbyPlaces: List<Place> = emptyList(),
        val isTopLoading: Boolean = false,
        val isNearbyLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val locationGranted: Boolean = false,
        val errorMessage: String? = null,
        /** true gdy GPS jest włączony ale lokalizacja jeszcze nie ustalona (trwa retry). */
        val isAcquiringLocation: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refreshLocationGranted()
    }

    /**
     * Synchronizuje flagę [UiState.locationGranted] z aktualnym stanem
     * uprawnień systemowych. Wywoływać przy każdej zmianie cyklu życia
     * (np. powrót z ekranu ustawień), żeby UI nie został z nieaktualną
     * informacją.
     */
    fun refreshLocationGranted() {
        val granted = hasLocationPermission(appContext)
        val shouldLoad = granted && !_uiState.value.locationGranted
        _uiState.update {
            it.copy(
                locationGranted = granted,
                isTopLoading = if (granted) it.isTopLoading else false,
                isNearbyLoading = if (granted) it.isNearbyLoading else false
            )
        }
        if (shouldLoad) loadLocationBasedPlaces()
    }

    /** Wywoływać po pomyślnym requeście permissionsa – uruchamia load obu sekcji. */
    fun onLocationPermissionGranted() {
        _uiState.update { it.copy(locationGranted = true) }
        loadLocationBasedPlaces()
    }

    /** Pull-to-refresh – zawsze przeładowuje dane niezależnie od stanu permission. */
    fun refresh() {
        if (!hasLocationPermission(appContext)) {
            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                kotlinx.coroutines.delay(300)
                _uiState.update { it.copy(isRefreshing = false) }
            }
            return
        }
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val location = runCatching { fetchCurrentLocation(appContext) }.getOrNull()
            if (location == null) {
                // Minimalny delay żeby PullToRefreshBox zdążył zarejestrować
                // przejście true→false (bez tego spinner może „zawisnąć" gdy
                // fetchCurrentLocation zwróci null natychmiast – np. GPS off).
                kotlinx.coroutines.delay(300)
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = LOCATION_TIMEOUT_USER_MESSAGE
                    )
                }
                return@launch
            }

            val (lat, lng) = location
            when (val result = placeRepository.getPlacesNear(lat, lng, HOME_PLACES_FETCH_RADIUS_KM)) {
                is OpResult.Success -> {
                    val placesWithDistance = result.data
                        .map { it to haversineKm(lat, lng, it.latitude, it.longitude) }

                    val nearby = placesWithDistance
                        .sortedBy { it.second }
                        .take(NEARBY_LIMIT)
                        .map { it.first }

                    val topNearby = placesWithDistance
                        .filter { (place, distanceKm) ->
                            place.reviewsCount > 0 && distanceKm <= TOP_PLACES_RADIUS_KM
                        }
                        .sortedWith(
                            compareByDescending<Pair<Place, Double>> { it.first.averageRating }
                                .thenByDescending { it.first.reviewsCount }
                                .thenBy { it.second }
                        )
                        .take(TOP_PLACES_LIMIT)
                        .map { it.first }

                    _uiState.update {
                        it.copy(
                            topPlaces = topNearby,
                            nearbyPlaces = nearby,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                }
                is OpResult.Failure -> _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = result.error.message
                            ?: "Nie udało się wczytać miejsc w pobliżu"
                    )
                }
            }
        }
    }

    private fun loadLocationBasedPlaces() {
        if (!hasLocationPermission(appContext)) {
            _uiState.update {
                it.copy(
                    locationGranted = false,
                    isTopLoading = false,
                    isNearbyLoading = false,
                    isAcquiringLocation = false
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isTopLoading = true, isNearbyLoading = true, errorMessage = null)
            }

            // Retry up to 3 times when GPS fix fails (weak signal, cold start).
            // Between retries, show "Ustalanie lokalizacji…" banner.
            var location: Pair<Double, Double>? = null
            val maxRetries = 3
            for (attempt in 1..maxRetries) {
                location = runCatching { fetchCurrentLocation(appContext) }.getOrNull()
                if (location != null) break
                if (attempt < maxRetries) {
                    _uiState.update { it.copy(isAcquiringLocation = true) }
                    kotlinx.coroutines.delay(3_000L)
                }
            }
            _uiState.update { it.copy(isAcquiringLocation = false) }

            if (location == null) {
                _uiState.update {
                    it.copy(
                        isTopLoading = false,
                        isNearbyLoading = false,
                        topPlaces = emptyList(),
                        nearbyPlaces = emptyList(),
                        errorMessage = LOCATION_TIMEOUT_USER_MESSAGE
                    )
                }
                return@launch
            }

            val (lat, lng) = location
            when (val result = placeRepository.getPlacesNear(lat, lng, HOME_PLACES_FETCH_RADIUS_KM)) {
                is OpResult.Success -> {
                    // Repo MVP zwraca wszystkie miejsca – liczymy dystans na kliencie.
                    // "Blisko Ciebie" to po prostu 20 najbliższych miejsc, bez
                    // patrzenia na oceny. "Top miejsca" to ranking z miejsc
                    // znajdujących się blisko usera.
                    val placesWithDistance = result.data
                        .map { it to haversineKm(lat, lng, it.latitude, it.longitude) }

                    val nearby = placesWithDistance
                        .sortedBy { it.second }
                        .take(NEARBY_LIMIT)
                        .map { it.first }

                    val topNearby = placesWithDistance
                        .filter { (place, distanceKm) ->
                            place.reviewsCount > 0 && distanceKm <= TOP_PLACES_RADIUS_KM
                        }
                        .sortedWith(
                            compareByDescending<Pair<Place, Double>> { it.first.averageRating }
                                .thenByDescending { it.first.reviewsCount }
                                .thenBy { it.second }
                        )
                        .take(TOP_PLACES_LIMIT)
                        .map { it.first }

                    _uiState.update {
                        it.copy(
                            topPlaces = topNearby,
                            nearbyPlaces = nearby,
                            isTopLoading = false,
                            isNearbyLoading = false
                        )
                    }
                }
                is OpResult.Failure -> _uiState.update {
                    it.copy(
                        isTopLoading = false,
                        isNearbyLoading = false,
                        errorMessage = result.error.message
                            ?: "Nie udało się wczytać miejsc w pobliżu"
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
