package com.playground.presentation.place.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playground.domain.model.Amenity
import com.playground.domain.model.Place
import com.playground.domain.model.PlaceCategory
import com.playground.domain.repository.AuthRepository
import com.playground.domain.repository.PlaceRepository
import com.playground.navigation.Route
import com.playground.utils.OpResult
import dagger.hilt.android.lifecycle.HiltViewModel
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
    savedStateHandle: SavedStateHandle,
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository
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
     */
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
        val isSaved: Boolean = false
    ) {
        /** Wszystkie wymagane pola wypełnione – można kliknąć "Zapisz". */
        val isFormValid: Boolean
            get() = name.trim().isNotBlank() &&
                address.trim().isNotBlank() &&
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
                            isLoadingPlace = false,
                            errorMessage = null
                        )
                    }
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
    }

    fun onLocationError(message: String) {
        _uiState.update { it.copy(isFetchingLocation = false, errorMessage = message) }
    }

    /**
     * Buduje [Place] z aktualnego stanu UI i zapisuje przez repozytorium.
     *
     * - W trybie create: świeży [Place] z `id=""`, owner = aktualny user,
     *   `createdAtMillis = now`. Repo nada id i wstawi do Firestore.
     * - W trybie edit: kopia oryginału z UI-edytowalnymi polami nadpisanymi
     *   nowymi wartościami. Niezmienne (ownerUserId, createdAtMillis,
     *   averageRating, reviewsCount) zostają jak były.
     */
    fun save() {
        val state = _uiState.value
        if (!state.isFormValid) {
            _uiState.update { it.copy(errorMessage = "Wypełnij wymagane pola i pobierz lokalizację") }
            return
        }

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

            val result = if (state.isEditMode && editingOriginal != null) {
                val original = editingOriginal!!
                val updated = original.copy(
                    name = state.name.trim(),
                    description = state.description.trim(),
                    category = state.category,
                    address = state.address.trim(),
                    latitude = state.latitude!!,
                    longitude = state.longitude!!,
                    amenities = state.amenities
                    // ownerUserId, createdAtMillis, averageRating, reviewsCount,
                    // photoUrls, id – zachowujemy z oryginału.
                )
                placeRepository.updatePlace(updated)
            } else {
                val newPlace = Place(
                    id = "",
                    ownerUserId = currentUser.id,
                    name = state.name.trim(),
                    description = state.description.trim(),
                    category = state.category,
                    latitude = state.latitude!!,
                    longitude = state.longitude!!,
                    address = state.address.trim(),
                    amenities = state.amenities,
                    createdAtMillis = System.currentTimeMillis()
                )
                placeRepository.addPlace(newPlace)
            }

            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(isSaving = false, isSaved = true)
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
