package com.kidzone.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.Place
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.service.LocationProvider
import com.kidzone.presentation.place.add.LOCATION_TIMEOUT_USER_MESSAGE
import com.kidzone.utils.OpResult
import com.kidzone.widget.NearbyPlacesWidget
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

/** Ile nowych miejsc maksymalnie pokazujemy w sekcji "Ostatnio dodane". */
private const val RECENTLY_ADDED_LIMIT = 10

/** Zakres czasu dla sekcji "Ostatnio dodane w okolicy". */
private const val RECENTLY_ADDED_WINDOW_MILLIS = 14L * 24L * 60L * 60L * 1000L

/** Promień lokalnego rankingu "Top miejsca" (km). */
private const val TOP_PLACES_RADIUS_KM = 10.0

/**
 * Promień techniczny fetcha miejsc do sekcji startowych.
 *
 * Repo wykonuje ograniczone zapytanie po prefiksie geohash i docina wynik
 * dokładnym dystansem po stronie klienta.
 */
private const val HOME_PLACES_FETCH_RADIUS_KM = 50.0

/**
 * ViewModel ekranu Home (zakładka "Start" w bottom navigation).
 *
 * Trzyma zestawy danych zależne od aktualnej lokalizacji:
 *  - **Ostatnio dodane w okolicy** – nowe miejsca z ostatnich 14 dni.
 *  - **Top miejsca** – 20 najlepiej ocenianych miejsc w promieniu
 *    [TOP_PLACES_RADIUS_KM] od użytkownika (lokalny ranking).
 *  - **Blisko Ciebie** – 20 najbliższych miejsc, bez względu na ocenę i liczbę
 *    opinii.
 *
 * Sekcje korzystają z jednego pobrania lokalizacji i jednego fetcha miejsc,
 * żeby nie dublować pracy FusedLocationProviderClient / Firestore.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val locationProvider: LocationProvider,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class PlaceWithDistance(
        val place: Place,
        val distanceKm: Double
    )

    /**
     * @property topPlaces lokalny ranking najlepiej ocenianych miejsc w pobliżu
     * @property nearbyPlaces lista najbliższych miejsc bez względu na ocenę
     * @property recentlyAddedPlaces nowe miejsca w okolicy z ostatnich 14 dni
     * @property isTopLoading true do zakończenia pierwszego fetcha topu
     * @property isNearbyLoading true gdy lecimy fetchem "blisko Ciebie"
     * @property isRecentlyAddedLoading true gdy ładujemy sekcję nowych miejsc
     * @property isRefreshing true podczas pull-to-refresh (kręci spinner)
     * @property locationGranted true gdy user nadał ACCESS_*_LOCATION
     * @property errorMessage błąd ostatniego fetcha (top lub nearby)
     */
    data class UiState(
        val topPlaces: List<PlaceWithDistance> = emptyList(),
        val nearbyPlaces: List<PlaceWithDistance> = emptyList(),
        val recentlyAddedPlaces: List<PlaceWithDistance> = emptyList(),
        val isTopLoading: Boolean = false,
        val isNearbyLoading: Boolean = false,
        val isRecentlyAddedLoading: Boolean = false,
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
        val granted = locationProvider.hasPermission()
        val shouldLoad = granted && !_uiState.value.locationGranted
        _uiState.update {
            it.copy(
                locationGranted = granted,
                isTopLoading = if (granted) it.isTopLoading else false,
                isNearbyLoading = if (granted) it.isNearbyLoading else false,
                isRecentlyAddedLoading = if (granted) it.isRecentlyAddedLoading else false
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
        if (!locationProvider.hasPermission()) {
            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                kotlinx.coroutines.delay(300)
                _uiState.update { it.copy(isRefreshing = false) }
            }
            return
        }
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val location = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
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
            persistLocationForWidget(lat, lng)
            when (val result = placeRepository.getPlacesNear(lat, lng, HOME_PLACES_FETCH_RADIUS_KM)) {
                is OpResult.Success -> {
                    val placesWithDistance = result.data
                        .map { it to haversineKm(lat, lng, it.latitude, it.longitude) }

                    val homeSections = buildHomeSections(placesWithDistance)

                    _uiState.update {
                        it.copy(
                            topPlaces = homeSections.topPlaces,
                            nearbyPlaces = homeSections.nearbyPlaces,
                            recentlyAddedPlaces = homeSections.recentlyAddedPlaces,
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
        if (!locationProvider.hasPermission()) {
            _uiState.update {
                it.copy(
                    locationGranted = false,
                    isTopLoading = false,
                    isNearbyLoading = false,
                    isRecentlyAddedLoading = false,
                    isAcquiringLocation = false
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTopLoading = true,
                    isNearbyLoading = true,
                    isRecentlyAddedLoading = true,
                    errorMessage = null
                )
            }

            // Retry up to 3 times when GPS fix fails (weak signal, cold start).
            // Between retries, show "Ustalanie lokalizacji…" banner.
            var location: Pair<Double, Double>? = null
            val maxRetries = 3
            for (attempt in 1..maxRetries) {
                location = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
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
                        isRecentlyAddedLoading = false,
                        topPlaces = emptyList(),
                        nearbyPlaces = emptyList(),
                        recentlyAddedPlaces = emptyList(),
                        errorMessage = LOCATION_TIMEOUT_USER_MESSAGE
                    )
                }
                return@launch
            }

            val (lat, lng) = location
            persistLocationForWidget(lat, lng)
            when (val result = placeRepository.getPlacesNear(lat, lng, HOME_PLACES_FETCH_RADIUS_KM)) {
                is OpResult.Success -> {
                    // Repo zwraca ograniczony bucket geohash; dokładny dystans
                    // liczymy na kliencie. "Blisko Ciebie" to 20 najbliższych, bez
                    // patrzenia na oceny. "Top miejsca" to ranking z miejsc
                    // znajdujących się blisko usera.
                    val placesWithDistance = result.data
                        .map { it to haversineKm(lat, lng, it.latitude, it.longitude) }

                    val homeSections = buildHomeSections(placesWithDistance)

                    _uiState.update {
                        it.copy(
                            topPlaces = homeSections.topPlaces,
                            nearbyPlaces = homeSections.nearbyPlaces,
                            recentlyAddedPlaces = homeSections.recentlyAddedPlaces,
                            isTopLoading = false,
                            isNearbyLoading = false,
                            isRecentlyAddedLoading = false
                        )
                    }
                }
                is OpResult.Failure -> _uiState.update {
                    it.copy(
                        isTopLoading = false,
                        isNearbyLoading = false,
                        isRecentlyAddedLoading = false,
                        errorMessage = result.error.message
                            ?: "Nie udało się wczytać miejsc w pobliżu"
                    )
                }
            }
        }
    }

    /** Persists last known location to SharedPreferences for the Glance widget. */
    private fun persistLocationForWidget(lat: Double, lng: Double) {
        appContext.getSharedPreferences(NearbyPlacesWidget.LOCATION_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(NearbyPlacesWidget.KEY_LAST_LAT, lat.toFloat())
            .putFloat(NearbyPlacesWidget.KEY_LAST_LNG, lng.toFloat())
            .apply()
    }
}

internal data class HomeSections(
    val topPlaces: List<HomeViewModel.PlaceWithDistance>,
    val nearbyPlaces: List<HomeViewModel.PlaceWithDistance>,
    val recentlyAddedPlaces: List<HomeViewModel.PlaceWithDistance>
)

internal fun buildHomeSections(
    placesWithDistance: List<Pair<Place, Double>>,
    nowMillis: Long = System.currentTimeMillis()
): HomeSections {
    val nearby = placesWithDistance
        .sortedBy { it.second }
        .take(NEARBY_LIMIT)
        .map { (place, distanceKm) -> HomeViewModel.PlaceWithDistance(place, distanceKm) }

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
        .map { (place, distanceKm) -> HomeViewModel.PlaceWithDistance(place, distanceKm) }

    val recentThresholdMillis = nowMillis - RECENTLY_ADDED_WINDOW_MILLIS
    val recentlyAdded = placesWithDistance
        .filter { (place, _) ->
            place.createdAtMillis >= recentThresholdMillis && place.createdAtMillis <= nowMillis
        }
        .sortedWith(
            compareByDescending<Pair<Place, Double>> { it.first.createdAtMillis }
                .thenBy { it.second }
        )
        .take(RECENTLY_ADDED_LIMIT)
        .map { (place, distanceKm) -> HomeViewModel.PlaceWithDistance(place, distanceKm) }

    return HomeSections(
        topPlaces = topNearby,
        nearbyPlaces = nearby,
        recentlyAddedPlaces = recentlyAdded
    )
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
