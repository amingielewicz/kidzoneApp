package com.kidzone.presentation.place.myplaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.Place
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
 * ViewModel ekranu "Moje miejsca" – lista miejsc dodanych przez aktualnie
 * zalogowanego użytkownika.
 *
 * Łączymy auth state z [PlaceRepository.observePlacesByOwner] przez
 * `flatMapLatest`: gdy user się wyloguje (np. zaraz po deleteAccount),
 * stary listener jest unsubscribowany, a nowy nie jest tworzony – ekran
 * zobaczy pustą listę / loading, NavGraph i tak za chwilę przerzuci
 * usera na Login.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyPlacesViewModel @Inject constructor(
    authRepository: AuthRepository,
    placeRepository: PlaceRepository
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Ready(val places: List<Place>) : UiState
        data class Error(val message: String) : UiState
    }

    val uiState: StateFlow<UiState> = authRepository.currentUser
        .flatMapLatest { current ->
            if (current == null) {
                // Brak usera → pusta lista jako Ready, żeby UI pokazało
                // "empty state" zamiast wieczystego spinnera (NavGraph
                // i tak zaraz przerzuci na Login).
                flowOf<UiState>(UiState.Ready(emptyList()))
            } else {
                placeRepository.observePlacesByOwner(current.id)
                    .map<List<Place>, UiState> { UiState.Ready(it) }
                    .onStart { emit(UiState.Loading) }
                    .catch { e ->
                        emit(UiState.Error(e.message ?: "Nie udało się wczytać Twoich miejsc"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )
}
