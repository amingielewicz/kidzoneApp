package com.playground.presentation.place.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playground.domain.model.Amenity
import com.playground.domain.model.Place
import com.playground.domain.model.PlaceCategory
import com.playground.domain.repository.AuthRepository
import com.playground.domain.repository.PlaceRepository
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
 * ViewModel ekranu dodawania miejsca.
 *
 * Trzyma stan formularza i obsługuje akcje użytkownika – zmiana pól,
 * pobranie GPS, toggle udogodnień, zapis do Firestore.
 */
@HiltViewModel
class AddPlaceViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Stan UI ekranu "Dodaj miejsce".
     *
     * @property latitude współrzędna geograficzna – null gdy nie pobrano
     * @property longitude współrzędna geograficzna – null gdy nie pobrano
     * @property isFetchingLocation true podczas pobierania GPS
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
        val isFetchingLocation: Boolean = false,
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
     * Wymaga zalogowanego użytkownika – jego id staje się [Place.ownerUserId].
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

            val place = Place(
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

            val result = placeRepository.addPlace(place)
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(isSaving = false, isSaved = true)
                    is OpResult.Failure -> it.copy(
                        isSaving = false,
                        errorMessage = result.error.message ?: "Błąd zapisu miejsca"
                    )
                }
            }
        }
    }
}
