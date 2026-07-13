package com.kidzone.presentation.place.myplaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.Place
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Udostępnia miejsca należące do aktualnie zalogowanego użytkownika.
 *
 * Zmiana stanu sesji przełącza aktywny strumień przez `flatMapLatest`. Po wylogowaniu, banie albo
 * usunięciu konta listener poprzedniego użytkownika jest anulowany i emitowana jest pusta lista,
 * dzięki czemu prywatne dane nie pozostają widoczne podczas zmiany graphu nawigacji.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyPlacesViewModel @Inject constructor(
    authRepository: AuthRepository,
    placeRepository: PlaceRepository
) : ViewModel() {

    /**
     * Stan ekranu „Moje miejsca”.
     */
    sealed interface UiState {
        /** Trwa pierwsze ładowanie danych właściciela. */
        data object Loading : UiState

        /**
         * Dane zostały załadowane.
         *
         * @property places miejsca należące do aktualnego użytkownika; pusta lista oznacza empty state.
         */
        data class Ready(val places: List<Place>) : UiState

        /**
         * Nie udało się odczytać listy.
         *
         * @property message zmapowany komunikat błędu przeznaczony dla UI.
         */
        data class Error(val message: String) : UiState
    }

    /**
     * Stan obserwowany przez ekran Compose.
     *
     * Brak aktywnej sesji jest reprezentowany przez `Ready(emptyList())`, a nie nieskończony loading.
     */
    val uiState: StateFlow<UiState> = authRepository.currentUser
        .flatMapLatest { current ->
            if (current == null) {
                flowOf<UiState>(UiState.Ready(emptyList()))
            } else {
                placeRepository.observePlacesByOwner(current.id)
                    .map<List<Place>, UiState> { UiState.Ready(it) }
                    .onStart { emit(UiState.Loading) }
                    .catch { error ->
                        emit(UiState.Error(error.message ?: "Could not load your places"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )
}
