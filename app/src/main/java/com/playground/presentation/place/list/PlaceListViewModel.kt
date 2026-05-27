package com.playground.presentation.place.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playground.domain.model.Place
import com.playground.domain.model.PlaceCategory
import com.playground.domain.repository.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel ekranu listy miejsc.
 *
 * Subskrybuje [PlaceRepository.observePlaces] – snapshot listener Firestore
 * automatycznie pushuje nowe miejsca do UI bez ręcznego refresh-u.
 *
 * Zmiana wybranej kategorii rebinduje strumień przez `flatMapLatest`, więc
 * lista od razu ogranicza się do nowego filtra (a stary listener jest
 * unsubscribowany).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaceListViewModel @Inject constructor(
    private val placeRepository: PlaceRepository
) : ViewModel() {

    /**
     * @property places aktualnie pokazywana lista (już posortowana – najnowsze pierwsze)
     * @property selectedCategory filtr kategorii; null = wszystkie
     * @property isLoading true do pierwszego emita z Firestore (po rebindzie też)
     * @property errorMessage komunikat błędu z snapshot listenera
     */
    data class UiState(
        val places: List<Place> = emptyList(),
        val selectedCategory: PlaceCategory? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    private val selectedCategory = MutableStateFlow<PlaceCategory?>(null)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            selectedCategory
                .flatMapLatest { category ->
                    _uiState.update {
                        it.copy(
                            selectedCategory = category,
                            isLoading = true,
                            errorMessage = null
                        )
                    }
                    placeRepository.observePlaces(category)
                        .catch { e ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = e.message
                                        ?: "Nie udało się wczytać listy miejsc"
                                )
                            }
                        }
                }
                .collect { places ->
                    // Sortujemy po dacie dodania, najnowsze na górze. Bez tego
                    // Firestore zwraca dokumenty w kolejności document-id, czyli
                    // praktycznie losowej – świeżo dodane miejsce pojawiałoby się
                    // gdzieś w środku listy zamiast na początku, co wygląda jakby
                    // "nie weszło".
                    val sorted = places.sortedByDescending { it.createdAtMillis }
                    _uiState.update {
                        it.copy(
                            places = sorted,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun onCategorySelected(category: PlaceCategory?) {
        if (selectedCategory.value != category) {
            selectedCategory.value = category
        }
    }
}
