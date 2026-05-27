package com.playground.presentation.place.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playground.domain.model.Place
import com.playground.domain.model.Review
import com.playground.domain.repository.PlaceRepository
import com.playground.domain.repository.ReviewRepository
import com.playground.navigation.Route
import com.playground.utils.OpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel ekranu szczegółów miejsca.
 *
 * Ładuje pojedyncze [Place] przez [PlaceRepository.getPlace] (jednorazowo)
 * oraz subskrybuje [ReviewRepository.observeReviewsForPlace] – snapshot
 * listener pcha nowe opinie do UI bez refresh-u.
 *
 * Nie korzystamy tu z `observePlaces`, bo tej funkcji w repo nie ma w wersji
 * "pojedynczego dokumentu" – `getPlace` jest one-shot i wystarczy do
 * pierwszego wyrenderowania. Jeśli średnia ocen się zmieni (po dodaniu
 * opinii), można później dodać `observePlace(id)` w repo.
 */
@HiltViewModel
class PlaceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placeRepository: PlaceRepository,
    reviewRepository: ReviewRepository
) : ViewModel() {

    /**
     * @property place załadowane miejsce; null gdy jeszcze nie wczytane lub nie istnieje
     * @property reviews opinie z snapshot listenera (już posortowane po dacie desc)
     * @property isLoading true do pierwszego wyniku `getPlace`
     * @property errorMessage komunikat błędu z `getPlace` lub strumienia opinii
     */
    data class UiState(
        val place: Place? = null,
        val reviews: List<Review> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    private val placeId: String =
        savedStateHandle.get<String>(Route.PlaceDetails.ARG_PLACE_ID).orEmpty()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadPlace()

        // Subskrypcja opinii – live, snapshot listener z Firestore.
        viewModelScope.launch {
            reviewRepository.observeReviewsForPlace(placeId)
                .catch { e ->
                    // Błąd opinii nie powinien blokować widoku samego miejsca –
                    // pokazujemy go tylko gdy żaden place też się nie załadował.
                    _uiState.update {
                        if (it.place == null) {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.message ?: "Nie udało się wczytać opinii"
                            )
                        } else {
                            it
                        }
                    }
                }
                .collect { reviews ->
                    _uiState.update {
                        it.copy(
                            reviews = reviews.sortedByDescending { r -> r.createdAtMillis }
                        )
                    }
                }
        }
    }

    fun retry() {
        loadPlace()
    }

    private fun loadPlace() {
        if (placeId.isBlank()) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Brak identyfikatora miejsca")
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = placeRepository.getPlace(placeId)) {
                is OpResult.Success -> _uiState.update {
                    it.copy(place = result.data, isLoading = false, errorMessage = null)
                }
                is OpResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.error.message ?: "Nie udało się wczytać miejsca"
                    )
                }
            }
        }
    }
}
