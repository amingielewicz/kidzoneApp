package com.kidzone.presentation.place.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.navigation.Route
import com.kidzone.utils.OpResult
import com.kidzone.utils.PhotoUploader
import com.kidzone.utils.TextNormalization
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.net.Uri
import com.kidzone.utils.ImageCompressor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel ekranu dodawania / edycji miejsca.
 *
 * Działa w dwóch trybach:
 *  - **create** (placeId = null) – formularz pusty, `save()` wola
 *    `addPlace()`.
 *  - **edit** (placeId z nawigacji) – pre-filluje stan z `getPlace()` i
 *    `save()` woła `updatePlace()` zachowując immutowalne pola
 *    (id, ownerUserId, createdAtMillis, averageRating, reviewsCount).
 */
@HiltViewModel
class AddPlaceViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository,
    private val photoUploader: PhotoUploader,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    /**
     * Stan UI ekranu "Dodaj / edytuj miejsce".
     *
     * @property isEditMode true gdy ładujemy istniejące miejsce (placeId != null)
     * @property editingPlaceId id edytowanego miejsca, null w trybie create
     * @property latitude współrzędna geograficzna – null gdy nie pobrano
     * @property longitude współrzędna geograficzna – null gdy nie pobrano
     * @property isFetchingLocation true podczas pobierania GPS
     * @property isLoadingPlace true gdy ładujemy istniejące miejsce do edycji
     * @property isSaving true podczas zapisu do Firestore
     * @property errorMessage komunikat błędu (np. brak GPS, błąd zapisu)
     * @property isSaved true po pomyślnym zapisie – sygnał do nawigacji
     * @property savedNewLatitude współrzędne nowo utworzonego miejsca (tylko create);
     *           pozwalają wyświetlić mapę wycentrowaną na pinie po popBackStack
     * @property savedNewLongitude jak wyżej
     */
    /**
     * Miejsce znalezione w pobliżu aktualnej lokalizacji (potencjalny duplikat).
     */
    data class NearbyPlace(
        val id: String,
        val name: String,
        val category: PlaceCategory,
        val distanceMeters: Int
    )

    data class UiState(
        val name: String = "",
        val description: String = "",
        val category: PlaceCategory = PlaceCategory.PLAYGROUND,
        val address: String = "",
        val latitude: Double? = null,
        val longitude: Double? = null,
        val amenities: Set<Amenity> = emptySet(),
        val isEditMode: Boolean = false,
        val editingPlaceId: String? = null,
        val isFetchingLocation: Boolean = false,
        val isLoadingPlace: Boolean = false,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val isSaved: Boolean = false,
        val savedNewLatitude: Double? = null,
        val savedNewLongitude: Double? = null,
        /**
         * Mapa: udogodnienie -> liczba istniejących miejsc, w których jest
         * zaznaczone. Używana przez UI do sortowania chipów udogodnień
         * od najczęściej do najrzadziej używanych.
         *
         * Pusta mapa = jeszcze nie wczytane (lub fetch padł). UI w tym
         * stanie pokazuje udogodnienia w kolejności z enuma (logiczne
         * grupowanie wg PlaceCategory) - to bezpieczny fallback.
         */
        val amenityFrequency: Map<Amenity, Int> = emptyMap(),
        /** Miejsca w promieniu 200m od pobranej lokalizacji GPS. */
        val nearbyPlaces: List<NearbyPlace> = emptyList(),
        /** True gdy wykryty potencjalny duplikat i czekamy na decyzję usera. */
        val showDuplicateWarning: Boolean = false,
        /** Potencjalny duplikat (ta sama kategoria w <100m) do wyświetlenia w dialogu. */
        val duplicateCandidate: NearbyPlace? = null,
        /** Lokalne URI zdjęć do uploadu (z photo pickera). */
        val photoUris: List<Uri> = emptyList(),
        /** Istniejące URL-e zdjęć (tryb edycji – zdjęcia już uploadowane). */
        val existingPhotoUrls: List<String> = emptyList(),
        /** True podczas uploadu zdjęć. */
        val isUploadingPhotos: Boolean = false,
        /** Komunikat o duplikatach (event jednorazowy, konsumowany przez UI). */
        val photoDuplicateMessage: String? = null
    ) {
        /** Max 5 zdjęć łącznie (nowe + istniejące). */
        val canAddMorePhotos: Boolean
            get() = (photoUris.size + existingPhotoUrls.size) < MAX_PLACE_PHOTOS
        /** Wszystkie wymagane pola wypełnione – można kliknąć "Zapisz". */
        val isFormValid: Boolean
            get() = name.trim().isNotBlank() &&
                latitude != null && longitude != null
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Pełen oryginalny obiekt edytowanego miejsca – trzymamy go po stronie
     * VM, żeby przy save w trybie edit móc skopiować immutowalne pola
     * (ownerUserId, createdAtMillis, ratingi) bez wystawiania ich w UiState.
     */
    private var editingOriginal: Place? = null

    init {
        val placeId = savedStateHandle.get<String>(Route.AddPlace.ARG_PLACE_ID)
        if (!placeId.isNullOrBlank()) {
            loadForEdit(placeId)
        }
        loadAmenityFrequency()
    }

    /**
     * Liczy częstość występowania każdego udogodnienia we wszystkich
     * miejscach w bazie - jednorazowo, na początku ekranu.
     *
     * Strategia: bierzemy 1 snapshot ze strumienia [PlaceRepository.observePlaces]
     * (przez `.first()`), zliczamy `amenities`, wpychamy do state.
     *
     * Best-effort - błąd / brak miejsc = pusta mapa, UI fallbackuje wtedy
     * na kolejność z enuma. Świadomie nie blokujemy ekranu (`isLoading`),
     * bo bez frequency formularz dalej działa, tylko mniej "smart".
     *
     * Uwaga skali: dla MVP (~100 miejsc) full-scan jest OK; przy rosnącej
     * bazie warto przepisać na dedykowaną kolekcję `amenity_counts`
     * utrzymywaną przez Cloud Functions.
     */
    private fun loadAmenityFrequency() {
        viewModelScope.launch {
            val frequency = runCatching {
                val places = placeRepository.observePlaces(category = null).first()
                val counts = mutableMapOf<Amenity, Int>()
                for (place in places) {
                    for (amenity in place.amenities) {
                        counts[amenity] = (counts[amenity] ?: 0) + 1
                    }
                }
                counts.toMap()
            }.getOrElse { emptyMap() }

            _uiState.update { it.copy(amenityFrequency = frequency) }
        }
    }

    private fun loadForEdit(placeId: String) {
        _uiState.update {
            it.copy(
                isEditMode = true,
                editingPlaceId = placeId,
                isLoadingPlace = true,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            when (val result = placeRepository.getPlace(placeId)) {
                is OpResult.Success -> {
                    editingOriginal = result.data
                    _uiState.update {
                        it.copy(
                            name = result.data.name,
                            description = result.data.description,
                            category = result.data.category,
                            address = result.data.address,
                            latitude = result.data.latitude,
                            longitude = result.data.longitude,
                            amenities = result.data.amenities,
                            existingPhotoUrls = result.data.photoUrls,
                            isLoadingPlace = false,
                            errorMessage = null
                        )
                    }
                    // Seed hash set z istniejących zdjęć dla dedup detection
                    seedPhotoHashes(result.data.photoUrls)
                }
                is OpResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoadingPlace = false,
                            errorMessage = result.error.message
                                ?: "Nie udało się wczytać miejsca do edycji"
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) =
        _uiState.update { it.copy(name = value, errorMessage = null) }

    fun onDescriptionChange(value: String) =
        _uiState.update { it.copy(description = value, errorMessage = null) }

    fun onCategoryChange(category: PlaceCategory) {
        _uiState.update { state ->
            // Po zmianie kategorii pruneujemy wybrane udogodnienia, żeby nie
            // zostawić zaznaczonych takich, które nie pasują do nowej kategorii
            // (są wtedy poza widokiem usera, ale nadal w state.amenities).
            val pruned = state.amenities
                .filter { category in it.applicableCategories }
                .toSet()
            state.copy(category = category, amenities = pruned, errorMessage = null)
        }
    }

    fun onAddressChange(value: String) =
        _uiState.update { it.copy(address = value, errorMessage = null) }

    fun toggleAmenity(amenity: Amenity) {
        _uiState.update { state ->
            val updated = if (amenity in state.amenities) {
                state.amenities - amenity
            } else {
                state.amenities + amenity
            }
            state.copy(amenities = updated)
        }
    }

    fun onFetchingLocationStart() {
        _uiState.update { it.copy(isFetchingLocation = true, errorMessage = null) }
    }

    /**
     * Po udanym pobraniu GPS (i ewentualnym reverse geocodingu) zapisuje
     * współrzędne i nadpisuje pole adresu jeśli geocoder coś zwrócił. Jeśli
     * [address] jest null/puste, zachowujemy to, co użytkownik wpisał ręcznie.
     *
     * Dodatkowo uruchamia fetch miejsc w promieniu 200m, żeby user widział
     * co już jest dodane w okolicy (ochrona przed duplikatami).
     */
    fun onLocationFetched(latitude: Double, longitude: Double, address: String? = null) {
        _uiState.update {
            it.copy(
                latitude = latitude,
                longitude = longitude,
                address = address?.takeIf { it.isNotBlank() } ?: it.address,
                isFetchingLocation = false,
                errorMessage = null
            )
        }
        loadNearbyPlaces(latitude, longitude)
    }

    /**
     * Ładuje miejsca w promieniu [NEARBY_RADIUS_KM] od podanych współrzędnych.
     * Wynik zapisywany do [UiState.nearbyPlaces] — UI może pokazać mini-listę
     * istniejących miejsc pod przyciskiem GPS, żeby user sam zauważył duplikaty.
     */
    private fun loadNearbyPlaces(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val nearby = runCatching {
                when (val result = placeRepository.getPlacesNear(latitude, longitude, NEARBY_RADIUS_KM)) {
                    is OpResult.Success -> result.data
                        .filter { it.id != _uiState.value.editingPlaceId } // nie pokazuj edytowanego
                        .map { place ->
                            val distMeters = (haversineKm(
                                latitude, longitude,
                                place.latitude, place.longitude
                            ) * 1000).toInt()
                            NearbyPlace(
                                id = place.id,
                                name = place.name,
                                category = place.category,
                                distanceMeters = distMeters
                            )
                        }
                        .filter { it.distanceMeters <= NEARBY_RADIUS_METERS }
                        .sortedWith(
                            // Priorytet: 1) podobna nazwa na górze, 2) bliskość
                            compareByDescending<NearbyPlace> { nearby ->
                                val input = _uiState.value.name.trim().lowercase()
                                if (input.isNotEmpty() &&
                                    (nearby.name.lowercase().contains(input) ||
                                        input.contains(nearby.name.lowercase()))
                                ) 1 else 0
                            }.thenBy { it.distanceMeters }
                        )
                    is OpResult.Failure -> emptyList()
                }
            }.getOrElse { emptyList() }
            _uiState.update { it.copy(nearbyPlaces = nearby) }
        }
    }

    fun onLocationError(message: String) {
        _uiState.update { it.copy(isFetchingLocation = false, errorMessage = message) }
    }

    /**
     * Próba zapisu. Jeśli wykryty potencjalny duplikat (ta sama kategoria
     * w promieniu [DUPLICATE_RADIUS_METERS]) — zamiast od razu zapisywać,
     * ustawiamy [UiState.showDuplicateWarning] = true. User musi potwierdzić.
     */
    fun save() {
        val state = _uiState.value
        if (!state.isFormValid) {
            _uiState.update { it.copy(errorMessage = "Wypełnij wymagane pola i pobierz lokalizację") }
            return
        }

        // Sprawdź potencjalne duplikaty:
        //  1. Ta sama kategoria w promieniu 100m
        //  2. Podobna nazwa (case-insensitive contains) w promieniu 200m,
        //     niezależnie od kategorii — ktoś mógł dodać to samo miejsce
        //     pod inną kategorią.
        if (!state.isEditMode && !state.showDuplicateWarning) {
            val inputName = state.name.trim().lowercase()
            val duplicate = state.nearbyPlaces.firstOrNull { nearby ->
                val sameCategoryClose = nearby.category == state.category &&
                    nearby.distanceMeters <= DUPLICATE_RADIUS_METERS
                val similarName = inputName.isNotEmpty() &&
                    (nearby.name.lowercase().contains(inputName) ||
                        inputName.contains(nearby.name.lowercase()))
                sameCategoryClose || similarName
            }
            if (duplicate != null) {
                _uiState.update {
                    it.copy(showDuplicateWarning = true, duplicateCandidate = duplicate)
                }
                return
            }
        }

        performSave()
    }

    /** User potwierdził "Dodaj mimo to" w dialogu duplikatów. */
    fun confirmSaveDespiteDuplicate() {
        _uiState.update { it.copy(showDuplicateWarning = false, duplicateCandidate = null) }
        performSave()
    }

    /** User anulował dialog duplikatów. */
    fun dismissDuplicateWarning() {
        _uiState.update { it.copy(showDuplicateWarning = false, duplicateCandidate = null) }
    }

    fun consumePhotoDuplicateMessage() {
        _uiState.update { it.copy(photoDuplicateMessage = null) }
    }

    // --- Zarządzanie zdjęciami ---

    /** Zbiór hashów (MD5 skompresowanych bajtów) istniejących zdjęć. */
    private val photoContentHashes: MutableSet<String> =
        (savedStateHandle.get<List<String>>("photoHashes") ?: emptyList()).toMutableSet()

    private fun persistHashes() {
        savedStateHandle["photoHashes"] = photoContentHashes.toList()
    }

    /** Seeduje hash set z Firestore (pole `photoContentHashes` na dokumencie miejsca). */
    private fun seedPhotoHashes(urls: List<String>) {
        // Hashe są teraz trzymane w Firestore na dokumencie miejsca (pole photoHashes).
        // Przy edycji pobieramy je stamtąd — zero downloadu obrazów po sieci.
        viewModelScope.launch {
            val placeId = _uiState.value.editingPlaceId ?: return@launch
            try {
                val place = (placeRepository.getPlace(placeId) as? OpResult.Success)?.data
                val storedHashes = place?.photoHashes.orEmpty()
                photoContentHashes.addAll(storedHashes)
                persistHashes()
            } catch (_: Exception) { /* best-effort */ }
        }
    }

    /** Dodaje zdjęcia z photo pickera (respektuje limit MAX_PLACE_PHOTOS). */
    fun addPhotos(uris: List<Uri>) {
        _uiState.update { state ->
            val currentTotal = state.photoUris.size + state.existingPhotoUrls.size
            val available = MAX_PLACE_PHOTOS - currentTotal
            val toAdd = uris.take(available)
            state.copy(photoUris = state.photoUris + toAdd)
        }
    }

    /** Usuwa nowe (jeszcze nie-uploadowane) zdjęcie po indeksie. */
    fun removeNewPhoto(index: Int) {
        _uiState.update { state ->
            state.copy(photoUris = state.photoUris.toMutableList().apply { removeAt(index) })
        }
    }

    /** URL-e zdjęć usuniętych przez usera (do skasowania z Storage przy save). */
    private val removedPhotoUrls: MutableList<String> =
        (savedStateHandle.get<List<String>>("removedPhotos") ?: emptyList()).toMutableList()

    private fun persistRemovedPhotos() {
        savedStateHandle["removedPhotos"] = removedPhotoUrls.toList()
    }

    /** Usuwa istniejące (już uploadowane) zdjęcie po indeksie. */
    fun removeExistingPhoto(index: Int) {
        _uiState.update { state ->
            val removed = state.existingPhotoUrls[index]
            removedPhotoUrls.add(removed)
            persistRemovedPhotos()
            state.copy(
                existingPhotoUrls = state.existingPhotoUrls.toMutableList().apply { removeAt(index) }
            )
        }
    }

    private fun performSave() {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val currentUser = authRepository.currentUser.first()
            if (currentUser == null) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Musisz być zalogowany, by dodać miejsce"
                    )
                }
                return@launch
            }

            // Upload nowych zdjęć (kompresja + Firebase Storage + dedup)
            val uploadedUrls = mutableListOf<String>()
            var duplicatesSkipped = 0
            if (state.photoUris.isNotEmpty()) {
                _uiState.update { it.copy(isUploadingPhotos = true) }
                for (uri in state.photoUris) {
                    val bytes = ImageCompressor.compressToWebp(appContext, uri)
                    if (bytes != null) {
                        // Dedup check na bazie hash skompresowanych bajtów
                        val hash = java.security.MessageDigest.getInstance("MD5")
                            .digest(bytes)
                            .joinToString("") { "%02x".format(it) }
                        if (hash in photoContentHashes) {
                            duplicatesSkipped++
                            continue
                        }
                        photoContentHashes.add(hash)
                        persistHashes()

                        try {
                            val tempId = state.editingPlaceId ?: "pending_${System.currentTimeMillis()}"
                            val url = photoUploader.uploadPlacePhoto(tempId, bytes)
                            uploadedUrls.add(url)
                        } catch (e: Exception) {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    isUploadingPhotos = false,
                                    errorMessage = "Błąd uploadu zdjęcia: ${e.message}"
                                )
                            }
                            return@launch
                        }
                    }
                }
                _uiState.update { it.copy(isUploadingPhotos = false) }
            }

            // Komunikat o duplikatach
            if (duplicatesSkipped > 0 && uploadedUrls.isEmpty() && state.existingPhotoUrls.isNotEmpty()) {
                // Wszystkie nowe zdjęcia to duplikaty – nie zapisujemy, pokazujemy błąd
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        photoDuplicateMessage = "To zdjęcie zostało już dodane. Nie można dodać duplikatu."
                    )
                }
                return@launch
            } else if (duplicatesSkipped > 0) {
                // Część zdjęć pominięta – kontynuujemy zapis z resztą
                _uiState.update {
                    it.copy(
                        photoDuplicateMessage = "To zdjęcie zostało już dodane. Nie można dodać duplikatu."
                    )
                }
            }

            // Łączymy istniejące URL-e (edycja) + nowo uploadowane
            val allPhotoUrls = state.existingPhotoUrls + uploadedUrls

            // Budujemy mapę photoUploadedBy: zachowujemy istniejącą (edycja)
            // + dodajemy nowo-uploadowane URL-e z bieżącym userId
            val existingUploadedBy = editingOriginal?.photoUploadedBy.orEmpty()
            val newUploadedBy = uploadedUrls.associateWith { currentUser.id }
            val allPhotoUploadedBy = existingUploadedBy + newUploadedBy

            // Persist all known hashes for future dedup (no more downloading images)
            val allPhotoHashes = photoContentHashes.toList()

            val result = if (state.isEditMode && editingOriginal != null) {
                val original = editingOriginal!!
                val updated = original.copy(
                    name = TextNormalization.toTitleCase(state.name),
                    description = TextNormalization.toSentenceCase(state.description),
                    category = state.category,
                    address = TextNormalization.toTitleCase(state.address),
                    latitude = state.latitude!!,
                    longitude = state.longitude!!,
                    amenities = state.amenities,
                    photoUrls = allPhotoUrls,
                    photoUploadedBy = allPhotoUploadedBy,
                    photoHashes = allPhotoHashes
                )
                placeRepository.updatePlace(updated)
            } else {
                val newPlace = Place(
                    id = "",
                    ownerUserId = currentUser.id,
                    name = TextNormalization.toTitleCase(state.name),
                    description = TextNormalization.toSentenceCase(state.description),
                    category = state.category,
                    latitude = state.latitude!!,
                    longitude = state.longitude!!,
                    address = TextNormalization.toTitleCase(state.address),
                    amenities = state.amenities,
                    photoUrls = allPhotoUrls,
                    photoUploadedBy = allPhotoUploadedBy,
                    photoHashes = allPhotoHashes,
                    createdAtMillis = System.currentTimeMillis()
                )
                placeRepository.addPlace(newPlace)
            }

            _uiState.update {
                when (result) {
                    is OpResult.Success -> {
                        // Usuń z Storage zdjęcia oznaczone do usunięcia (best-effort)
                        for (url in removedPhotoUrls) {
                            photoUploader.deletePhoto(url)
                        }
                        removedPhotoUrls.clear()

                        val isCreate = !state.isEditMode
                        it.copy(
                            isSaving = false,
                            isSaved = true,
                            savedNewLatitude = if (isCreate) result.data.latitude else null,
                            savedNewLongitude = if (isCreate) result.data.longitude else null
                        )
                    }
                    is OpResult.Failure -> it.copy(
                        isSaving = false,
                        errorMessage = result.error.message
                            ?: if (state.isEditMode) "Błąd aktualizacji miejsca"
                            else "Błąd zapisu miejsca"
                    )
                }
            }
        }
    }
}

/** Promień (km) w jakim szukamy istniejących miejsc do wyświetlenia pod GPS. */
private const val NEARBY_RADIUS_KM = 0.5

/** Promień (metry) do wyświetlenia jako "w pobliżu". */
private const val NEARBY_RADIUS_METERS = 200

/** Promień (metry) dla wykrywania duplikatów (ta sama kategoria). */
private const val DUPLICATE_RADIUS_METERS = 100

/** Maksymalna liczba zdjęć na jedno miejsce. */
const val MAX_PLACE_PHOTOS = 5

/** Odległość w km między dwoma punktami (formuła haversine). */
private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2).let { it * it } +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2).let { it * it }
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return r * c
}
