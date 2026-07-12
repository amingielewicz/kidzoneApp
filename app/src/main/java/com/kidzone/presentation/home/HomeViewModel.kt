package com.kidzone.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.data.remote.PerformanceConfig
import com.kidzone.data.remote.PerformanceConfigProvider
import com.kidzone.domain.model.Place
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.service.LocationProvider
import com.kidzone.utils.OpResult
import com.kidzone.utils.UiText
import com.kidzone.utils.toPlacesErrorMessage
import com.kidzone.widget.NearbyPlacesWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
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

/** Zakres czasu dla sekcji "Ostatnio dodane w okolicy". */
private const val RECENTLY_ADDED_WINDOW_MILLIS = 14L * 24L * 60L * 60L * 1000L
private const val LOCATION_RETRY_DELAY_MS = 3_000L
private const val LOCATION_RETRY_COUNT = 3
private const val ONE_MINUTE_MILLIS = 60_000L
private const val KEY_LAST_LOCATION_TIME = "last_location_time"

/**
 * ViewModel ekranu Home (zakładka "Start" w bottom navigation).
 *
 * Trzyma zestawy danych zależne od aktualnej lokalizacji:
 *  - **Ostatnio dodane w okolicy** – nowe miejsca z ostatnich 14 dni.
 *  - **Top miejsca** – 20 najlepiej ocenianych miejsc w promieniu
 *    z Remote Config od użytkownika (lokalny ranking).
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
    private val performanceConfigProvider: PerformanceConfigProvider,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class PlaceWithDistance(
        val place: Place,
        val distanceKm: Double
    )

    private data class LastKnownLocation(
        val lat: Double,
        val lng: Double,
        val timestampMillis: Long
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
     * @property isUsingStaleLocation true gdy pokazujemy dane z ostatniej poprawnej pozycji
     * @property staleLocationAgeMinutes wiek ostatniej znanej pozycji w minutach
     * @property hasWeakGpsSignal true gdy GPS jest włączony, ale nie udało się złapać fixa
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
        val errorMessage: UiText? = null,
        /** true gdy GPS jest włączony ale lokalizacja jeszcze nie ustalona (trwa retry). */
        val isAcquiringLocation: Boolean = false,
        val isUsingStaleLocation: Boolean = false,
        val staleLocationAgeMinutes: Int? = null,
        val hasWeakGpsSignal: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private var lastKnownLocation: LastKnownLocation? = null

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
                isRecentlyAddedLoading = if (granted) it.isRecentlyAddedLoading else false,
                hasWeakGpsSignal = if (granted) it.hasWeakGpsSignal else false,
                isUsingStaleLocation = if (granted) it.isUsingStaleLocation else false,
                staleLocationAgeMinutes = if (granted) it.staleLocationAgeMinutes else null
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
                delay(300)
                _uiState.update { it.copy(isRefreshing = false) }
            }
            return
        }
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val location = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
            if (location == null) {
                delay(300)
                applyWeakGpsFallback()
                return@launch
            }

            val (lat, lng) = location
            saveLastKnownLocation(lat, lng)
            loadPlacesForLocation(
                lat = lat,
                lng = lng,
                isStale = false
            )
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
                    isAcquiringLocation = false,
                    isUsingStaleLocation = false,
                    staleLocationAgeMinutes = null,
                    hasWeakGpsSignal = false
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
                    errorMessage = null,
                    hasWeakGpsSignal = false
                )
            }

            var location: Pair<Double, Double>? = null
            repeat(LOCATION_RETRY_COUNT) { attempt ->
                location = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
                if (location != null) return@repeat
                if (attempt < LOCATION_RETRY_COUNT - 1) {
                    _uiState.update { it.copy(isAcquiringLocation = true) }
                    delay(LOCATION_RETRY_DELAY_MS)
                }
            }
            _uiState.update { it.copy(isAcquiringLocation = false) }

            if (location == null) {
                applyWeakGpsFallback()
                return@launch
            }

            val (lat, lng) = location ?: return@launch
            saveLastKnownLocation(lat, lng)
            loadPlacesForLocation(
                lat = lat,
                lng = lng,
                isStale = false
            )
        }
    }

    private suspend fun applyWeakGpsFallback() {
        val fallbackLocation = lastKnownLocation
        if (fallbackLocation == null) {
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    isTopLoading = false,
                    isNearbyLoading = false,
                    isRecentlyAddedLoading = false,
                    isAcquiringLocation = false,
                    isUsingStaleLocation = false,
                    staleLocationAgeMinutes = null,
                    hasWeakGpsSignal = true,
                    errorMessage = null
                )
            }
            return
        }

        val hasExistingPlaces = _uiState.value.nearbyPlaces.isNotEmpty() ||
            _uiState.value.topPlaces.isNotEmpty() ||
            _uiState.value.recentlyAddedPlaces.isNotEmpty()

        if (hasExistingPlaces) {
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    isTopLoading = false,
                    isNearbyLoading = false,
                    isRecentlyAddedLoading = false,
                    isAcquiringLocation = false,
                    isUsingStaleLocation = true,
                    staleLocationAgeMinutes = fallbackLocation.ageMinutes(),
                    hasWeakGpsSignal = true,
                    errorMessage = null
                )
            }
            return
        }

        loadPlacesForLocation(
            lat = fallbackLocation.lat,
            lng = fallbackLocation.lng,
            isStale = true
        )
    }

    private suspend fun loadPlacesForLocation(
        lat: Double,
        lng: Double,
        isStale: Boolean
    ) {
        val performanceConfig = performanceConfigProvider.performanceConfig
        if (!isStale) {
            persistLocationForWidget(lat, lng)
        }
        when (
            val result = placeRepository.getPlacesNear(
                lat,
                lng,
                performanceConfig.homeFetchRadiusKm
            )
        ) {
            is OpResult.Success -> {
                val placesWithDistance = result.data
                    .map { it to haversineKm(lat, lng, it.latitude, it.longitude) }

                val homeSections = buildHomeSections(placesWithDistance, performanceConfig)

                _uiState.update {
                    it.copy(
                        topPlaces = homeSections.topPlaces,
                        nearbyPlaces = homeSections.nearbyPlaces,
                        recentlyAddedPlaces = homeSections.recentlyAddedPlaces,
                        isTopLoading = false,
                        isNearbyLoading = false,
                        isRecentlyAddedLoading = false,
                        isRefreshing = false,
                        isAcquiringLocation = false,
                        isUsingStaleLocation = isStale,
                        staleLocationAgeMinutes = lastKnownLocation?.ageMinutes().takeIf { isStale },
                        hasWeakGpsSignal = isStale,
                        errorMessage = null
                    )
                }
            }
            is OpResult.Failure -> _uiState.update {
                it.copy(
                    isTopLoading = false,
                    isNearbyLoading = false,
                    isRecentlyAddedLoading = false,
                    isRefreshing = false,
                    errorMessage = result.error.toPlacesErrorMessage(
                        UiText.StringResource(com.kidzone.R.string.error_fetch_places)
                    )
                )
            }
        }
    }

    private fun saveLastKnownLocation(lat: Double, lng: Double) {
        val now = System.currentTimeMillis()
        lastKnownLocation = LastKnownLocation(lat, lng, now)
        persistLocationForWidget(lat, lng, now)
    }

    /** Persists last known location to SharedPreferences for the Glance widget. */
    private fun persistLocationForWidget(lat: Double, lng: Double, timestampMillis: Long = System.currentTimeMillis()) {
        appContext.getSharedPreferences(NearbyPlacesWidget.LOCATION_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(NearbyPlacesWidget.KEY_LAST_LAT, lat.toFloat())
            .putFloat(NearbyPlacesWidget.KEY_LAST_LNG, lng.toFloat())
            .putLong(KEY_LAST_LOCATION_TIME, timestampMillis)
            .apply()
    }

    private fun LastKnownLocation.ageMinutes(): Int =
        ((System.currentTimeMillis() - timestampMillis) / ONE_MINUTE_MILLIS).toInt().coerceAtLeast(0)
}

internal data class HomeSections(
    val topPlaces: List<HomeViewModel.PlaceWithDistance>,
    val nearbyPlaces: List<HomeViewModel.PlaceWithDistance>,
    val recentlyAddedPlaces: List<HomeViewModel.PlaceWithDistance>
)

internal fun buildHomeSections(
    placesWithDistance: List<Pair<Place, Double>>,
    performanceConfig: PerformanceConfig = PerformanceConfig(),
    nowMillis: Long = System.currentTimeMillis()
): HomeSections {
    val nearby = placesWithDistance
        .sortedBy { it.second }
        .take(performanceConfig.homeNearbyLimit)
        .map { (place, distanceKm) -> HomeViewModel.PlaceWithDistance(place, distanceKm) }

    val topNearby = placesWithDistance
        .filter { (place, distanceKm) ->
            place.reviewsCount > 0 && distanceKm <= performanceConfig.homeTopPlacesRadiusKm
        }
        .sortedWith(
            compareByDescending<Pair<Place, Double>> { it.first.averageRating }
                .thenByDescending { it.first.reviewsCount }
                .thenBy { it.second }
        )
        .take(performanceConfig.homeTopPlacesLimit)
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
        .take(performanceConfig.homeRecentlyAddedLimit)
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
