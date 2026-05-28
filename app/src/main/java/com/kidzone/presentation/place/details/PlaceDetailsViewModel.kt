package com.kidzone.presentation.place.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.Review
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.ReviewRepository
import com.kidzone.navigation.Route
import com.kidzone.utils.OpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel ekranu szczegółów miejsca.
 *
 * Ładuje:
 *  - pojedyncze [Place] przez [PlaceRepository.getPlace] (one-shot),
 *  - dane autora przez [AuthRepository.getUserById] (one-shot, po załadowaniu
 *    place'a),
 *  - listę opinii przez [ReviewRepository.observeReviewsForPlace] (live).
 *
 * Eksponuje też strumień [currentUser] do wyliczenia czy aktualny użytkownik
 * jest właścicielem miejsca (i tym samym czy może edytować/usuwać).
 *
 * Akcja [delete] usuwa miejsce po potwierdzeniu w UI – zwraca przez
 * `isDeleted=true` w state, na który ekran nawiguje.
 */
@HiltViewModel
class PlaceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository,
    reviewRepository: ReviewRepository
) : ViewModel() {

    /**
     * @property place załadowane miejsce; null gdy jeszcze nie wczytane lub nie istnieje
     * @property author dane autora; null gdy jeszcze nie pobrane lub nie znaleziono
     * @property reviews opinie z snapshot listenera (już posortowane po dacie desc)
     * @property isLoading true do pierwszego wyniku `getPlace`
     * @property errorMessage komunikat błędu z `getPlace` lub strumienia opinii
     * @property isDeleting true podczas wywoływania `deletePlace`
     * @property isDeleted true po pomyślnym usunięciu – sygnał dla UI do nawigacji
     * @property deleteErrorMessage komunikat błędu z `deletePlace`
     */
    data class UiState(
        val place: Place? = null,
        val author: User? = null,
        val reviews: List<Review> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val isDeleting: Boolean = false,
        val isDeleted: Boolean = false,
        val deleteErrorMessage: String? = null
    )

    private val placeId: String =
        savedStateHandle.get<String>(Route.PlaceDetails.ARG_PLACE_ID).orEmpty()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Aktualnie zalogowany użytkownik – używamy go w UI do sprawdzenia
     * czy pokazać akcje edytuj/usuń.
     */
    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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
                is OpResult.Success -> {
                    _uiState.update {
                        it.copy(place = result.data, isLoading = false, errorMessage = null)
                    }
                    loadAuthor(result.data.ownerUserId)
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

    private fun loadAuthor(ownerUserId: String) {
        if (ownerUserId.isBlank()) return
        viewModelScope.launch {
            // Best-effort – brak autora nie blokuje wyświetlenia szczegółów,
            // pokazujemy wtedy tylko datę bez nicka.
            when (val result = authRepository.getUserById(ownerUserId)) {
                is OpResult.Success -> _uiState.update { it.copy(author = result.data) }
                is OpResult.Failure -> { /* zostawiamy author = null */ }
            }
        }
    }

    /**
     * Usuwa aktualnie wyświetlane miejsce. Powinno być wywołane TYLKO gdy
     * [isCurrentUserOwner] = true (UI tak filtruje), ale dodatkowo tu robimy
     * sanity-check zalogowanego użytkownika.
     */
    fun delete() {
        val place = _uiState.value.place ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteErrorMessage = null) }

            val user = authRepository.currentUser.first()
            if (user == null || user.id != place.ownerUserId) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        deleteErrorMessage = "Nie masz uprawnień, by usunąć to miejsce"
                    )
                }
                return@launch
            }

            when (val result = placeRepository.deletePlace(place.id)) {
                is OpResult.Success -> _uiState.update {
                    it.copy(isDeleting = false, isDeleted = true)
                }
                is OpResult.Failure -> _uiState.update {
                    it.copy(
                        isDeleting = false,
                        deleteErrorMessage = result.error.message ?: "Nie udało się usunąć miejsca"
                    )
                }
            }
        }
    }

    fun consumeDeleteError() {
        _uiState.update { it.copy(deleteErrorMessage = null) }
    }
}
