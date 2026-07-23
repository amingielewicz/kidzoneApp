package com.kidzone.presentation.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.data.remote.PerformanceConfigProvider
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.presentation.common.BadgeContext
import com.kidzone.presentation.common.UserBadge
import com.kidzone.presentation.common.computeBadges
import com.kidzone.utils.OpResult
import com.kidzone.utils.UiText
import com.kidzone.utils.toPlacesErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val RANKING_ERROR_FALLBACK = UiText.StringResource(com.kidzone.R.string.error_fetch_list)

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie jednorazowym pobieraniem rankingów miejsc i użytkowników.
 * - Równoległe pobieranie danych i ich agregacja w stan UI.
 * - Prekomputacja odznak rankingowych dla użytkowników.
 *
 * 🚫 Poza zakresem:
 * - Brak decyzji o offline queue.
 * - Brak retry logiki dla operacji sieciowych.
 * - Brak zarządzania sesją użytkownika.
 *
 * 📥 Wejście:
 * - Dane konfiguracyjne z Remote Config poprzez [PerformanceConfigProvider].
 * - Wyniki zapytań z [PlaceRepository] i [AuthRepository].
 *
 * 📤 Wyjście:
 * - Stan zakładki rankingu ([UiState]) zawierający listy top miejsc i użytkowników.
 *
 * ✅ Gwarancje:
 * - Spójność prezentowanych odznak między rankingiem a profilem użytkownika.
 * - Minimalizacja odczytów Firestore poprzez zapytania one-shot (brak listenerów).
 *
 * 🔌 Offline:
 * - Nie wspiera odświeżania rankingu w trybie offline (brak cache'owania rankingów).
 *
 * 🧵 Wątki:
 * - viewModelScope dla operacji asynchronicznych.
 * - Wykorzystanie [coroutineScope] i [async] do równoległego pobierania list.
 *
 * 🧪 Testowalność:
 * - Pełne DI.
 * - Deterministyczne mapowanie pozycji w rankingu na odznaki UI.
 *
 * 🧼 Lifecycle:
 * - One-shot loading wyzwalany przy inicjalizacji.
 */
@HiltViewModel
class RankingViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository,
    private val performanceConfigProvider: PerformanceConfigProvider
) : ViewModel() {

    /**
     * Niezmienny stan zakładki rankingu.
     *
     * @property topPlaces aktywne miejsca uporządkowane według średniej oceny.
     * @property topUsers aktywni użytkownicy uporządkowani według liczby dodanych miejsc.
     * @property userBadges odznaki obliczone dla każdego użytkownika z kontekstem rankingowym.
     * @property isLoading czy trwa pierwsze ładowanie lub jawne odświeżenie.
     * @property errorMessage bezpieczny komunikat błędu; częściowy sukces może nadal zawierać dane.
     */
    data class UiState(
        val topPlaces: List<Place> = emptyList(),
        val topUsers: List<User> = emptyList(),
        val userBadges: Map<String, List<UserBadge>> = emptyMap(),
        val isLoading: Boolean = true,
        val errorMessage: UiText? = null
    )

    private val _uiState = MutableStateFlow(UiState())

    /** Stan obserwowany przez ekran Compose. */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /**
     * Pobiera ranking miejsc i użytkowników oraz przelicza odznaki.
     *
     * @param forceShowLoading gdy `true`, pokazuje pełny stan ładowania także przy istniejących
     * danych; przy `false` odświeżenie może zachować poprzednią treść na ekranie.
     */
    fun refresh(forceShowLoading: Boolean = false) {
        viewModelScope.launch {
            if (forceShowLoading || (_uiState.value.topPlaces.isEmpty() && _uiState.value.topUsers.isEmpty())) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val performanceConfig = performanceConfigProvider.performanceConfig
            val (placesResult, usersResult) = coroutineScope {
                val placesDeferred = async {
                    placeRepository.getTopPlaces(limit = performanceConfig.rankingFetchPool)
                }
                val usersDeferred = async {
                    authRepository.getTopUsers(limit = performanceConfig.rankingFetchPool)
                }
                placesDeferred.await() to usersDeferred.await()
            }

            val places = (placesResult as? OpResult.Success)?.data.orEmpty()
                .filter { it.reviewsCount > 0 && it.averageRating > 0.0 }
                .take(performanceConfig.rankingTopLimit)
            val users = (usersResult as? OpResult.Success)?.data.orEmpty()
                .filter { it.placesAddedCount > 0 || it.reviewsCount > 0 }
                .take(performanceConfig.rankingTopLimit)

            val error = when {
                placesResult is OpResult.Failure && usersResult is OpResult.Failure ->
                    placesResult.error.toPlacesErrorMessage(RANKING_ERROR_FALLBACK)
                placesResult is OpResult.Failure ->
                    placesResult.error.toPlacesErrorMessage(RANKING_ERROR_FALLBACK)
                usersResult is OpResult.Failure ->
                    usersResult.error.toPlacesErrorMessage(RANKING_ERROR_FALLBACK)
                else -> null
            }

            val userBadges: Map<String, List<UserBadge>> = users.mapIndexed { idx, user ->
                val userRank = (idx + 1).takeIf { it in 1..3 }
                val bestPlaceRank = places
                    .mapIndexedNotNull { placeIndex, place ->
                        if (place.ownerUserId == user.id) placeIndex + 1 else null
                    }
                    .minOrNull()
                user.id to user.computeBadges(BadgeContext(userRank, bestPlaceRank))
            }.toMap()

            _uiState.value = UiState(
                topPlaces = places,
                topUsers = users,
                userBadges = userBadges,
                isLoading = false,
                errorMessage = error
            )
        }
    }
}
