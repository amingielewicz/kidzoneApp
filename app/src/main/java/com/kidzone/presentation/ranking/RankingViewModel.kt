package com.kidzone.presentation.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.utils.OpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel zakładki Ranking.
 *
 * Pobiera *one-shot* dwie listy równolegle:
 *  - top miejsc wg [Place.averageRating] (przez [PlaceRepository.getTopPlaces]),
 *  - top użytkowników wg [User.placesAddedCount] (przez [AuthRepository.getTopUsers]).
 *
 * Świadomie nie używamy snapshot listenera – ranking nie musi być real-time,
 * a one-shot redukuje zużycie kwoty Firestore. [refresh] pozwala użytkownikowi
 * odświeżyć ręcznie (pull-to-refresh / przycisk).
 */
@HiltViewModel
class RankingViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * @property topPlaces TOP 10 miejsc wg średniej oceny (malejąco)
     * @property topUsers  TOP 10 użytkowników wg liczby dodanych miejsc, drugorzędnie po liczbie opinii
     * @property isLoading aktywne podczas pierwszego ładowania i każdego refreshu
     * @property errorMessage komunikat błędu (jeśli któraś z list nie wczytała się)
     */
    data class UiState(
        val topPlaces: List<Place> = emptyList(),
        val topUsers: List<User> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Równoległy fetch obu list – ranking ładuje się tak szybko jak
            // wolniejsze z dwóch zapytań, a nie jako ich suma.
            val (placesResult, usersResult) = coroutineScope {
                val placesDeferred = async { placeRepository.getTopPlaces(limit = TOP_LIMIT) }
                val usersDeferred = async { authRepository.getTopUsers(limit = TOP_LIMIT) }
                placesDeferred.await() to usersDeferred.await()
            }

            val places = (placesResult as? OpResult.Success)?.data.orEmpty()
            val users = (usersResult as? OpResult.Success)?.data.orEmpty()

            // Łączymy komunikaty błędów z obu fetchów – jeśli np. użytkownicy
            // się wczytali a miejsca nie, pokażemy błąd nie tracąc danych.
            val error = listOfNotNull(
                (placesResult as? OpResult.Failure)?.error?.message,
                (usersResult as? OpResult.Failure)?.error?.message
            ).joinToString("\n").ifBlank { null }

            _uiState.value = UiState(
                topPlaces = places,
                topUsers = users,
                isLoading = false,
                errorMessage = error
            )
        }
    }

    private companion object {
        const val TOP_LIMIT = 10
    }
}
