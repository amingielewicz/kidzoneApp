package com.playground.presentation.place.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playground.domain.model.Place
import com.playground.domain.repository.PlaceRepository
import com.playground.navigation.Route
import com.playground.utils.OpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel ekranu pełnoekranowej mapy dla pojedynczego miejsca.
 *
 * Jednorazowo ładuje [Place] przez [PlaceRepository.getPlace] – nic więcej
 * nie potrzebujemy bo widok pokazuje tylko marker + nazwę.
 */
@HiltViewModel
class PlaceMapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placeRepository: PlaceRepository
) : ViewModel() {

    /**
     * @property place załadowane miejsce; null gdy jeszcze nie wczytane lub nie istnieje
     * @property isLoading true do pierwszego wyniku
     * @property errorMessage komunikat błędu z `getPlace`
     */
    data class UiState(
        val place: Place? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    private val placeId: String =
        savedStateHandle.get<String>(Route.PlaceMap.ARG_PLACE_ID).orEmpty()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        if (placeId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Brak identyfikatora miejsca") }
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
