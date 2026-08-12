package com.kidzone.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.data.remote.PerformanceConfig
import com.kidzone.data.remote.PerformanceConfigProvider
import com.kidzone.domain.model.Place
import com.kidzone.domain.repository.IpLocationRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.service.LocationPreferences
import com.kidzone.domain.service.LocationProvider
import com.kidzone.utils.GeoUtils
import com.kidzone.utils.OpResult
import com.kidzone.utils.UiText
import com.kidzone.utils.toPlacesErrorMessage
import com.kidzone.widget.NearbyPlacesWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val RECENTLY_ADDED_RADIUS_KM = 5.0
private const val LOCATION_RETRY_DELAY_MS = 1_000L
private const val LOCATION_RETRY_COUNT = 3
private const val ONE_MINUTE_MILLIS = 60_000L
private const val DEFAULT_CITY_LAT = 52.2297
private const val DEFAULT_CITY_LNG = 21.0122

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie sekcjami lokalizacyjnymi ekranu Start.
 * - Budowanie sekcji (najlepiej oceniane, najbliższe, nowości) z jednego zestawu danych.
 *
 * 🚫 Poza zakresem:
 * - Brak decyzji o offline queue (obsługiwane przez Repository).
 * - Brak retry logiki (delegowane do mechanizmów niższego poziomu).
 * - Brak zarządzania sesją użytkownika.
 *
 * 📥 Wejście:
 * - Strumień lokalizacji z [LocationProvider].
 * - Dane o miejscach z [PlaceRepository].
 *
 * 📤 Wyjście:
 * - Stan UI zawierający posegregowane listy miejsc.
 *
 * ✅ Gwarancje:
 * - Spójność danych między sekcjami.
 * - Użycie ostatniej poprawnej lokalizacji przy błędach odczytu.
 *
 * 🔌 Offline:
 * - Wspiera odczyt z cache Room, gdy Firestore jest niedostępny.
 *
 * 🧵 Wątki:
 * - viewModelScope dla operacji asynchronicznych.
 * - Brak blokujących operacji na wątku Main.
 *
 * 🧪 Testowalność:
 * - Pełne wstrzykiwanie zależności (DI).
 * - Brak ukrytych singletonów.
 * - Deterministyczne zachowanie stanu.
 *
 * 🧼 Lifecycle:
 * - Odświeżanie danych przy starcie ekranu (wykrywanie powrotu online).
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val locationProvider: LocationProvider,
    private val ipLocationRepository: IpLocationRepository,
    private val locationPreferences: LocationPreferences,
    private val performanceConfigProvider: PerformanceConfigProvider,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    /**
     * Miejsce połączone z odległością od pozycji użytej do budowy sekcji.
     *
     * @property place model miejsca.
     * @property distanceKm odległość w kilometrach (null, jeśli lokalizacja nieznana).
     */
    data class PlaceWithDistance(
        val place: Place,
        val distanceKm: Double?
    )

    private data class LastKnownLocation(
        val lat: Double,
        val lng: Double,
        val timestampMillis: Long
    )

    /**
     * Niezmienny stan ekranu Start.
     *
     * @property topPlaces lokalny ranking najlepiej ocenianych miejsc w pobliżu.
     * @property nearbyPlaces miejsca najbliższe pozycji użytkownika.
     * @property recentlyAddedPlaces miejsca dodane w ciągu ostatnich 14 dni.
     * @property isTopLoading czy trwa ładowanie sekcji top.
     * @property isNearbyLoading czy trwa ładowanie sekcji najbliższych miejsc.
     * @property isRecentlyAddedLoading czy trwa ładowanie sekcji nowych miejsc.
     * @property isRefreshing czy trwa jawne odświeżenie użytkownika.
     * @property locationGranted czy aplikacja ma co najmniej przybliżone uprawnienie lokalizacji.
     * @property errorMessage zmapowany komunikat ostatniego błędu pobierania.
     * @property isAcquiringLocation czy trwa ponawianie próby uzyskania fixa lokalizacji.
     * @property isUsingStaleLocation czy sekcje zostały obliczone z ostatniej znanej pozycji.
     * @property staleLocationAgeMinutes wiek użytej pozycji w pełnych minutach.
     * @property hasWeakGpsSignal czy usługa jest dostępna, ale nie udało się uzyskać nowego fixa.
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
        val isAcquiringLocation: Boolean = false,
        val isUsingStaleLocation: Boolean = false,
        val staleLocationAgeMinutes: Int? = null,
        val hasWeakGpsSignal: Boolean = false,
        val isGlobalFallback: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())

    /** Stan obserwowany przez ekran Compose. */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var lastKnownLocation: LastKnownLocation? = null

    init {
        refreshLocationGranted()
    }

    /**
     * Synchronizuje stan UI z rzeczywistym stanem uprawnienia lokalizacji.
     *
     * Metodę należy wywołać po powrocie ekranu do aktywnego lifecycle, szczególnie po otwarciu
     * ustawień aplikacji. Cofnięcie zgody zatrzymuje loadery i usuwa oznaczenia stale location.
     */
    fun refreshLocationGranted() {
        val granted = locationProvider.hasPermission()
        val prevGranted = _uiState.value.locationGranted
        
        _uiState.update { it.copy(locationGranted = granted) }
        
        if (granted && !prevGranted) {
            loadLocationBasedPlaces()
        } else if (!granted) {
            loadGlobalFallbackPlaces()
        }
    }

    /**
     * Informuje ViewModel o pomyślnym zakończeniu systemowego flow uprawnienia lokalizacji.
     *
     * UI nadal powinno odświeżyć rzeczywisty stan podczas kolejnego lifecycle eventu.
     */
    fun onLocationPermissionGranted() {
        _uiState.update { it.copy(locationGranted = true, isGlobalFallback = false) }
        loadLocationBasedPlaces()
    }

    /**
     * Ponownie pobiera lokalizację i dane wszystkich sekcji.
     *
     * Brak uprawnienia kończy się krótkim, kontrolowanym stanem refresh bez uruchamiania requestu.
     * Gdy GPS nie zwróci nowej pozycji, stosowany jest jawny fallback słabego sygnału.
     */
    fun refresh() {
        if (!locationProvider.hasPermission()) {
            loadGlobalFallbackPlaces()
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
            loadPlacesForLocation(lat, lng, isStale = false)
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
            loadGlobalFallbackPlaces()
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTopLoading = true,
                    isNearbyLoading = true,
                    isRecentlyAddedLoading = true,
                    errorMessage = null,
                    hasWeakGpsSignal = false,
                    isGlobalFallback = false
                )
            }

            // KROK 1: Natychmiastowe użycie ostatniej znanej lokalizacji (jeśli dostępna)
            // Pozwala na wyświetlenie danych w < 1s zamiast czekania na świeży fix GPS.
            val lastKnown = locationProvider.getLastKnownLocation()
            if (lastKnown != null) {
                saveLastKnownLocation(lastKnown.first, lastKnown.second)
                loadPlacesForLocation(
                    lat = lastKnown.first,
                    lng = lastKnown.second,
                    isStale = true
                )
            }

            // KROK 2: Próba uzyskania świeżego, dokładnego fixu GPS w tle.
            var freshLocation: Pair<Double, Double>? = null
            repeat(LOCATION_RETRY_COUNT) { attempt ->
                freshLocation = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
                if (freshLocation != null) return@repeat
                if (attempt < LOCATION_RETRY_COUNT - 1) {
                    _uiState.update { it.copy(isAcquiringLocation = true) }
                    delay(LOCATION_RETRY_DELAY_MS)
                }
            }
            _uiState.update { it.copy(isAcquiringLocation = false) }

            // KROK 3: Jeśli uzyskaliśmy świeży fix, sprawdzamy czy warto odświeżyć dane.
            if (freshLocation != null) {
                val (lat, lng) = freshLocation!!
                val distanceMoved = lastKnown?.let { (lLat, lLng) ->
                    GeoUtils.haversineKm(lLat, lLng, lat, lng)
                } ?: Double.MAX_VALUE

                // Odświeżamy tylko jeśli przesunęliśmy się o > 500m lub nie mieliśmy danych z kroku 1.
                @Suppress("MagicNumber")
                if (distanceMoved > 0.5 || lastKnown == null) {
                    saveLastKnownLocation(lat, lng)
                    loadPlacesForLocation(
                        lat = lat,
                        lng = lng,
                        isStale = false
                    )
                } else {
                    // Mamy świeży sygnał, ale lokalizacja jest podobna – usuwamy tylko znacznik "stale"
                    _uiState.update {
                        it.copy(
                            isUsingStaleLocation = false,
                            staleLocationAgeMinutes = null,
                            hasWeakGpsSignal = false
                        )
                    }
                }
            } else if (lastKnown == null) {
                // Całkowity brak sygnału i brak cache'u lokalizacji
                applyWeakGpsFallback()
            } else {
                // Nie mamy świeżego sygnału, ale mamy dane z lastKnown – zostawiamy stan z kroku 1.
                _uiState.update { it.copy(hasWeakGpsSignal = true) }
            }
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
            loadGlobalFallbackPlaces()
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
            loadGlobalFallbackPlaces()
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

        // Najpierw próbujemy załadować dane z cache lokalnego natychmiast (Stale-While-Revalidate)
        // Jeśli mamy internet, i tak pobierzemy świeże dane, ale user zobaczy coś od razu.
        val cachedPlaces = placeRepository.getCachedPlacesNear(lat, lng, performanceConfig.homeFetchRadiusKm)
        if (cachedPlaces.isNotEmpty()) {
            updateHomeSections(cachedPlaces, lat, lng, isStale)
        }

        // Następnie (lub równolegle przez Repository) pobieramy dane z sieci.
        // Repository samo zarządza tym, czy najpierw zwraca cache.
        // Pobieramy dane lokalne (promień 50km). Sekcje same odfiltrują mniejsze promienie.
        when (
            val result = placeRepository.getPlacesNear(
                lat,
                lng,
                performanceConfig.homeFetchRadiusKm
            )
        ) {
            is OpResult.Success -> {
                updateHomeSections(result.data, lat, lng, isStale)
            }
            is OpResult.Failure -> {
                if (_uiState.value.nearbyPlaces.isEmpty()) {
                    _uiState.update {
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
        }
    }

    private fun updateHomeSections(places: List<Place>, lat: Double, lng: Double, isStale: Boolean) {
        val performanceConfig = performanceConfigProvider.performanceConfig
        val placesWithDistance = places
            .map { it to GeoUtils.haversineKm(lat, lng, it.latitude, it.longitude) }

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
                isGlobalFallback = false,
                errorMessage = null
            )
        }
    }

    /**
     * Pobiera dane globalne (Top, Recent) oraz z domyślnego miasta, gdy nie można użyć GPS.
     */
    private fun loadGlobalFallbackPlaces() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTopLoading = true,
                    isNearbyLoading = true,
                    isRecentlyAddedLoading = true,
                    errorMessage = null,
                    isGlobalFallback = true
                )
            }

            val coords = resolveFallbackCoordinates()
            val performanceConfig = performanceConfigProvider.performanceConfig

            coroutineScope {
                val topTask = async { placeRepository.getTopPlaces(performanceConfig.homeTopPlacesLimit) }
                val nearbyTask = async {
                    placeRepository.getPlacesNear(coords.first, coords.second, performanceConfig.homeFetchRadiusKm)
                }
                val recentTask = async {
                    placeRepository.getPlacesPage(pageSize = performanceConfig.homeRecentlyAddedLimit)
                }

                val topResult = topTask.await()
                val nearbyResult = nearbyTask.await()
                val recentResult = recentTask.await()

                _uiState.update { state ->
                    state.copy(
                        topPlaces = (topResult as? OpResult.Success)?.data
                            ?.map { PlaceWithDistance(it, null) }.orEmpty(),
                        nearbyPlaces = (nearbyResult as? OpResult.Success)?.data
                            ?.map { PlaceWithDistance(it, null) }.orEmpty(),
                        recentlyAddedPlaces = (recentResult as? OpResult.Success)?.data?.items
                            ?.map { PlaceWithDistance(it, null) }.orEmpty(),
                        isTopLoading = false,
                        isNearbyLoading = false,
                        isRecentlyAddedLoading = false,
                        isRefreshing = false,
                        isAcquiringLocation = false,
                        isUsingStaleLocation = false,
                        staleLocationAgeMinutes = null,
                        hasWeakGpsSignal = false
                    )
                }
            }
        }
    }

    private suspend fun resolveFallbackCoordinates(): Pair<Double, Double> {
        if (locationPreferences.isIpLocationValid()) {
            locationPreferences.getIpLocation()?.let { return it }
        }

        return when (val ipResult = ipLocationRepository.getApproximateLocation()) {
            is OpResult.Success -> {
                locationPreferences.saveIpLocation(ipResult.data.first, ipResult.data.second)
                ipResult.data
            }
            else -> DEFAULT_CITY_LAT to DEFAULT_CITY_LNG
        }
    }

    private fun saveLastKnownLocation(lat: Double, lng: Double) {
        val now = System.currentTimeMillis()
        lastKnownLocation = LastKnownLocation(lat, lng, now)
        persistLocationForWidget(lat, lng, now)
    }

    /**
     * Zapisuje ostatnią poprawną lokalizację dla widgetu Glance.
     *
     * Dane są prywatnym stanem urządzenia. Nie mogą trafiać do logów i muszą zostać usunięte przy
     * zakończeniu sesji użytkownika.
     */
    private fun persistLocationForWidget(
        lat: Double,
        lng: Double,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        appContext.getSharedPreferences(NearbyPlacesWidget.LOCATION_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(NearbyPlacesWidget.KEY_LAST_LAT, lat.toFloat())
            .putFloat(NearbyPlacesWidget.KEY_LAST_LNG, lng.toFloat())
            .putLong(NearbyPlacesWidget.KEY_LAST_LOCATION_TIME, timestampMillis)
            .apply()
    }

    private fun LastKnownLocation.ageMinutes(): Int =
        ((System.currentTimeMillis() - timestampMillis) / ONE_MINUTE_MILLIS).toInt().coerceAtLeast(0)
}

/**
 * Zestaw sekcji przygotowanych dla ekranu Start.
 *
 * @property topPlaces najlepiej oceniane miejsca w lokalnym promieniu.
 * @property nearbyPlaces najbliższe miejsca.
 * @property recentlyAddedPlaces miejsca dodane w ostatnim oknie czasowym.
 */
internal data class HomeSections(
    val topPlaces: List<HomeViewModel.PlaceWithDistance>,
    val nearbyPlaces: List<HomeViewModel.PlaceWithDistance>,
    val recentlyAddedPlaces: List<HomeViewModel.PlaceWithDistance>
)

/**
 * Buduje sekcje ekranu Start z jednego zestawu miejsc i ich odległości.
 *
 * @param placesWithDistance miejsca połączone z odległością od użytkownika.
 * @param performanceConfig limity i promienie pobrane z Remote Config.
 * @return gotowe, posortowane i ograniczone sekcje.
 */
internal fun buildHomeSections(
    placesWithDistance: List<Pair<Place, Double>>,
    performanceConfig: PerformanceConfig = PerformanceConfig()
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

    val recentlyAdded = placesWithDistance
        .filter { (_, distanceKm) -> distanceKm <= RECENTLY_ADDED_RADIUS_KM }
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
