package com.playground.presentation.place.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playground.domain.model.Amenity
import com.playground.domain.model.Place
import com.playground.domain.model.PlaceCategory
import com.playground.domain.repository.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

/**
 * ViewModel ekranu listy miejsc.
 *
 * Subskrybuje [PlaceRepository.observePlaces] – snapshot listener Firestore
 * automatycznie pushuje nowe miejsca do UI bez ręcznego refresh-u.
 *
 * Filtrowanie:
 *  - **kategoria** – po stronie Firestore (`whereEqualTo("category", ...)`).
 *    Zmiana rebinduje strumień przez `flatMapLatest`, stary listener jest
 *    unsubscribowany.
 *  - **udogodnienia** – po stronie klienta (Firestore w jednym query
 *    nie umie AND po wielu `array-contains`). Działa na żywo na strumieniu
 *    streamowanej listy, więc wciąż mamy real-time updates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaceListViewModel @Inject constructor(
    private val placeRepository: PlaceRepository
) : ViewModel() {

    /**
     * @property places aktualnie pokazywana lista (po obu filtrach, posortowana
     *   po `createdAtMillis` malejąco – najnowsze na górze)
     * @property selectedCategory filtr kategorii; null = wszystkie
     * @property selectedAmenities multi-select udogodnień (logika AND)
     * @property isLoading true do pierwszego emita z Firestore (po rebindzie też)
     * @property errorMessage komunikat błędu z snapshot listenera
     */
    data class UiState(
        val places: List<Place> = emptyList(),
        val selectedCategory: PlaceCategory? = null,
        val selectedAmenities: Set<Amenity> = emptySet(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    private val selectedCategory = MutableStateFlow<PlaceCategory?>(null)
    private val selectedAmenities = MutableStateFlow<Set<Amenity>>(emptySet())

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

    val uiState: StateFlow<UiState> = combine(
        placesLoad,
        selectedCategory,
        selectedAmenities
    ) { load, category, amenities ->
        when (load) {
            PlacesLoad.Loading -> UiState(
                selectedCategory = category,
                selectedAmenities = amenities,
                isLoading = true,
                errorMessage = null,
                places = emptyList()
            )
            is PlacesLoad.Success -> UiState(
                places = load.list
                    .filter { place -> amenities.all { it in place.amenities } }
                    .sortedByDescending { it.createdAtMillis },
                selectedCategory = category,
                selectedAmenities = amenities,
                isLoading = false,
                errorMessage = null
            )
            is PlacesLoad.Error -> UiState(
                selectedCategory = category,
                selectedAmenities = amenities,
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
}
