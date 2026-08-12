@file:Suppress("ReturnCount")

package com.kidzone.presentation.place.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.R
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.service.ImageCompressorPort
import com.kidzone.navigation.Route
import com.kidzone.review.InAppReviewManager
import com.kidzone.utils.GeoUtils
import com.kidzone.utils.OpResult
import com.kidzone.utils.PhotoHasher
import com.kidzone.utils.PhotoUploader
import com.kidzone.utils.TextNormalization
import com.kidzone.utils.UiText
import com.kidzone.utils.toPlacesErrorMessage
import com.kidzone.utils.toUploadErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie stanem formularza dodawania i edycji miejsca.
 * - Walidacja pól (nazwa, opis, adres, udogodnienia) przed zapisem.
 * - Koordynacja kompresji i uploadu zdjęć.
 *
 * 🚫 Poza zakresem:
 * - Brak decyzji o offline queue (obsługiwane przez Repository).
 * - Brak retry logiki dla operacji sieciowych.
 * - Brak zarządzania sesją użytkownika.
 *
 * 📥 Wejście:
 * - [SavedStateHandle] z opcjonalnym ID edytowanego miejsca.
 * - Interakcje użytkownika z polami formularza.
 *
 * 📤 Wyjście:
 * - Stan formularza ([UiState]).
 * - Status zapisu i ewentualne zdarzenia nawigacji.
 *
 * ✅ Gwarancje:
 * - Zachowanie pól immutowalnych (id, ownerId, counters) przy edycji.
 * - Normalizacja tekstu przed zapisem do bazy.
 *
 * 🔌 Offline:
 * - Wspiera odczyt danych edytowanego miejsca z cache lokalnego.
 *
 * 🧵 Wątki:
 * - viewModelScope dla wszystkich operacji asynchronicznych (zapis, upload).
 * - Brak blokujących operacji na wątku Main.
 *
 * 🧪 Testowalność:
 * - Pełne DI.
 * - Brak zależności od singletonów.
 * - Deterministyczność stanu formularza.
 *
 * 🧼 Lifecycle:
 * - Pre-fillowanie stanu przy startu (tryb edycji) na podstawie SavedStateHandle.
 */
@HiltViewModel
class AddPlaceViewModel @Inject @Suppress("LongParameterList") constructor(
    private val savedStateHandle: SavedStateHandle,
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository,
    private val photoUploader: PhotoUploader,
    private val imageCompressor: ImageCompressorPort,
    private val photoHasher: PhotoHasher,
    private val inAppReviewManager: InAppReviewManager
) : ViewModel() {

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
        val errorMessage: UiText? = null,
        val isSaved: Boolean = false,
        val savedNewLatitude: Double? = null,
        val savedNewLongitude: Double? = null,
        /**
         * Mapa: udogodnienie -> liczba istniejących miejsc, w których jest
         * zaznaczone. Używana przez UI do sortowania chipów udogodnień.
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
        val photoDuplicateMessage: UiText? = null,
        /** True after user attempted to save an invalid form. */
        val hasTriedToSave: Boolean = false,
        /** True when in-app review should be requested. */
        val shouldRequestReview: Boolean = false
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
     * Pełen oryginalny obiekt edytowanego miejsca.
     */
    private var editingOriginal: Place? = null

    init {
        val placeId = savedStateHandle.get<String>(Route.AddPlace.ARG_PLACE_ID)
        if (!placeId.isNullOrBlank()) {
            loadForEdit(placeId)
        }
        loadAmenityFrequency()
    }

    private fun loadAmenityFrequency() {
        val cached = cachedAmenityFrequency
        val age = System.currentTimeMillis() - cachedAmenityFrequencyTimestamp
        if (cached != null && age < FREQUENCY_CACHE_TTL_MS) {
            _uiState.update { it.copy(amenityFrequency = cached) }
            return
        }

        viewModelScope.launch {
            val frequency = runCatching {
                val places = placeRepository.observePlaces(category = null).first()
                    .take(FREQUENCY_SAMPLE_LIMIT)
                val counts = mutableMapOf<Amenity, Int>()
                for (place in places) {
                    for (amenity in place.amenities) {
                        counts[amenity] = (counts[amenity] ?: 0) + 1
                    }
                }
                counts.toMap()
            }.getOrElse { emptyMap() }

            cachedAmenityFrequency = frequency
            cachedAmenityFrequencyTimestamp = System.currentTimeMillis()

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
                            name = result.data.name.take(PLACE_NAME_MAX_LENGTH),
                            description = result.data.description.take(PLACE_DESCRIPTION_MAX_LENGTH),
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
                    selectedPhotoHashes.addAll(result.data.photoHashes.values)
                }
                is OpResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoadingPlace = false,
                            errorMessage = UiText.StringResource(R.string.error_load_place)
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) =
        _uiState.update {
            it.copy(name = value.take(PLACE_NAME_MAX_LENGTH), errorMessage = null)
        }

    fun onDescriptionChange(value: String) =
        _uiState.update {
            it.copy(description = value.take(PLACE_DESCRIPTION_MAX_LENGTH), errorMessage = null)
        }

    fun onCategoryChange(category: PlaceCategory) {
        _uiState.update { state ->
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

    private fun loadNearbyPlaces(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val nearby = runCatching {
                when (val result = placeRepository.getPlacesNear(latitude, longitude, NEARBY_RADIUS_KM)) {
                    is OpResult.Success -> result.data
                        .filter { it.id != _uiState.value.editingPlaceId }
                        .map { place ->
                            val distMeters = GeoUtils.haversineMeters(
                                latitude, longitude,
                                place.latitude, place.longitude
                            )
                            NearbyPlace(
                                id = place.id,
                                name = place.name,
                                category = place.category,
                                distanceMeters = distMeters
                            )
                        }
                        .filter { it.distanceMeters <= NEARBY_RADIUS_METERS }
                        .sortedWith(
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

    fun onLocationError(message: UiText) {
        _uiState.update { it.copy(isFetchingLocation = false, errorMessage = message) }
    }

    fun save(isOffline: Boolean = false) {
        val state = _uiState.value

        if (isOffline) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    isUploadingPhotos = false,
                )
            }
            return
        }

        if (!state.isFormValid) {
            _uiState.update {
                it.copy(
                    hasTriedToSave = true,
                    errorMessage = UiText.StringResource(R.string.error_fill_required_fields)
                )
            }
            return
        }

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

    fun confirmSaveDespiteDuplicate() {
        _uiState.update { it.copy(showDuplicateWarning = false, duplicateCandidate = null) }
        performSave()
    }

    fun dismissDuplicateWarning() {
        _uiState.update { it.copy(showDuplicateWarning = false, duplicateCandidate = null) }
    }

    fun consumePhotoDuplicateMessage() {
        _uiState.update { it.copy(photoDuplicateMessage = null) }
    }

    fun consumeReviewRequest() {
        _uiState.update { it.copy(shouldRequestReview = false) }
    }

    // --- Zarządzanie zdjęciami ---

    /** Dodaje zdjęcia z photo pickera. */
    fun addPhotos(uris: List<Uri>) {
        _uiState.update { state ->
            val currentTotal = state.photoUris.size + state.existingPhotoUrls.size
            val available = MAX_PLACE_PHOTOS - currentTotal
            val toAdd = uris.take(available)
            state.copy(photoUris = state.photoUris + toAdd)
        }
    }

    private val selectedPhotoHashes = mutableSetOf<String>()

    /** Usuwa nowe zdjęcie po indeksie. */
    fun removeNewPhoto(index: Int) {
        _uiState.update { state ->
            state.copy(photoUris = state.photoUris.toMutableList().apply { removeAt(index) })
        }
    }

    /** URL-e zdjęć usuniętych przez usera. */
    private val removedPhotoUrls: MutableList<String> =
        (savedStateHandle.get<List<String>>("removedPhotos") ?: emptyList()).toMutableList()

    private fun persistRemovedPhotos() {
        savedStateHandle["removedPhotos"] = removedPhotoUrls.toList()
    }

    /** Usuwa istniejące zdjęcie po indeksie. */
    fun removeExistingPhoto(index: Int) {
        _uiState.update { state ->
            val removedUrl = state.existingPhotoUrls.getOrNull(index)
                ?: return@update state

            removedPhotoUrls.add(removedUrl)
            editingOriginal?.photoHashes?.get(removedUrl)?.let(selectedPhotoHashes::remove)

            persistRemovedPhotos()
            state.copy(
                existingPhotoUrls = state.existingPhotoUrls.filterNot { it == removedUrl }
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
                        errorMessage = UiText.StringResource(R.string.error_must_be_logged_in)
                    )
                }
                return@launch
            }

            if (currentUser.isBanned) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = if (currentUser.bannedUntilMillis == -1L) {
                            UiText.StringResource(R.string.ban_permanent)
                        } else {
                            val date = java.text.SimpleDateFormat(
                                "dd.MM.yyyy HH:mm",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date(currentUser.bannedUntilMillis))
                            UiText.StringResource(R.string.ban_temporary, date)
                        }
                    )
                }
                return@launch
            }

            val targetPlaceId = state.editingPlaceId ?: UUID.randomUUID().toString()

            // Upload nowych zdjęć
            val uploadedUrls = mutableListOf<String>()
            val uploadedHashes = mutableMapOf<String, String>()
            var duplicatesSkipped = 0
            if (state.photoUris.isNotEmpty()) {
                _uiState.update { it.copy(isUploadingPhotos = true) }
                for (uri in state.photoUris) {
                    val bytes = imageCompressor.compressToWebp(uri)
                    if (bytes != null) {
                        val hash = photoHasher.computeHash(bytes)
                        if (photoHasher.isDuplicate(hash, selectedPhotoHashes)) {
                            duplicatesSkipped++
                            continue
                        }
                        try {
                            val url = photoUploader.uploadPlacePhoto(
                                ownerUserId = currentUser.id,
                                placeId = targetPlaceId,
                                imageBytes = bytes
                            )
                            uploadedUrls.add(url)
                            selectedPhotoHashes.add(hash)
                            uploadedHashes[url] = hash
                        } catch (e: Exception) {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    isUploadingPhotos = false,
                                    errorMessage = e.toUploadErrorMessage()
                                )
                            }
                            return@launch
                        }
                    }
                }
                _uiState.update { it.copy(isUploadingPhotos = false) }
            }

            if (duplicatesSkipped > 0 && uploadedUrls.isEmpty() && state.existingPhotoUrls.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        photoDuplicateMessage = UiText.StringResource(R.string.duplicate_photo_error)
                    )
                }
                return@launch
            } else if (duplicatesSkipped > 0) {
                _uiState.update {
                    it.copy(photoDuplicateMessage = UiText.StringResource(R.string.duplicate_photo_error))
                }
            }

            val allPhotoUrls = state.existingPhotoUrls + uploadedUrls
            val retainedExistingUrls = state.existingPhotoUrls.toSet()

            val existingUploadedBy = editingOriginal?.photoUploadedBy.orEmpty()
                .filterKeys { it in retainedExistingUrls }
            val newUploadedBy = uploadedUrls.associateWith { currentUser.id }
            val allPhotoUploadedBy = existingUploadedBy + newUploadedBy

            val existingPhotoHashes = editingOriginal?.photoHashes.orEmpty()
                .filterKeys { it in retainedExistingUrls }
            val allPhotoHashes = existingPhotoHashes + uploadedHashes

            val result = if (state.isEditMode && editingOriginal != null) {
                val original = editingOriginal!!
                val normalizedName = TextNormalization.toTitleCase(state.name)
                    .take(PLACE_NAME_MAX_LENGTH).trim()
                val updated = original.copy(
                    name = normalizedName,
                    description = TextNormalization.toSentenceCase(state.description)
                        .take(PLACE_DESCRIPTION_MAX_LENGTH),
                    category = state.category,
                    address = TextNormalization.toTitleCase(state.address),
                    latitude = state.latitude!!,
                    longitude = state.longitude!!,
                    amenities = state.amenities,
                    photoUrls = allPhotoUrls,
                    photoUploadedBy = allPhotoUploadedBy,
                    photoHashes = allPhotoHashes,
                )
                placeRepository.updatePlace(updated)
            } else {
                val normalizedName = TextNormalization.toTitleCase(state.name)
                    .take(PLACE_NAME_MAX_LENGTH).trim()
                val newPlace = Place(
                    id = targetPlaceId,
                    ownerUserId = currentUser.id,
                    name = normalizedName,
                    description = TextNormalization.toSentenceCase(state.description)
                        .take(PLACE_DESCRIPTION_MAX_LENGTH),
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

            when (result) {
                is OpResult.Success -> {
                    for (url in removedPhotoUrls.toList()) {
                        try {
                            photoUploader.deletePhoto(url)
                        } catch (_: Exception) { }
                    }
                    removedPhotoUrls.clear()
                    persistRemovedPhotos()

                    val isCreate = !state.isEditMode
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSaved = true,
                            savedNewLatitude = if (isCreate) result.data.latitude else null,
                            savedNewLongitude = if (isCreate) result.data.longitude else null,
                            shouldRequestReview = if (isCreate) {
                                inAppReviewManager.onPlaceAdded()
                            } else {
                                false
                            }
                        )
                    }
                }

                is OpResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.error.toPlacesErrorMessage(
                                if (state.isEditMode) {
                                    UiText.StringResource(R.string.error_update_place)
                                } else {
                                    UiText.StringResource(R.string.error_save_place)
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val FREQUENCY_SAMPLE_LIMIT = 200
        const val FREQUENCY_CACHE_TTL_MS = 5L * 60 * 1000

        @Volatile
        var cachedAmenityFrequency: Map<Amenity, Int>? = null

        @Volatile
        var cachedAmenityFrequencyTimestamp: Long = 0L
    }
}

private const val NEARBY_RADIUS_KM = 0.5
private const val NEARBY_RADIUS_METERS = 200
private const val DUPLICATE_RADIUS_METERS = 100
const val MAX_PLACE_PHOTOS = 5
const val PLACE_NAME_MAX_LENGTH = 50
const val PLACE_DESCRIPTION_MAX_LENGTH = 500
