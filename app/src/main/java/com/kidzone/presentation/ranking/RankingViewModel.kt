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
    private val authRepository: AuthRepository,
    private val performanceConfigProvider: PerformanceConfigProvider
) : ViewModel() {

    /**
     * @property topPlaces top miejsc wg średniej oceny (malejąco), do limitu z Remote Config
     * @property topUsers  top użytkowników wg liczby dodanych miejsc, drugorzędnie po liczbie opinii, do limitu z Remote Config
     * @property userBadges precomputowane odznaki per user (uid -> lista odznak),
     *   uwzględniają KONTEKST rankingowy (LEADER_*, PLACE_TOP*) - inaczej karta
     *   usera w rankingu pokazywałaby mniej odznak niż ten sam user widzi na
     *   swoim profilu, co jest mylące. UI tylko odczytuje, nie liczy.
     * @property isLoading aktywne podczas pierwszego ładowania i każdego refreshu
     * @property errorMessage komunikat błędu (jeśli któraś z list nie wczytała się)
     */
    data class UiState(
        val topPlaces: List<Place> = emptyList(),
        val topUsers: List<User> = emptyList(),
        val userBadges: Map<String, List<UserBadge>> = emptyMap(),
        val isLoading: Boolean = true,
        val errorMessage: UiText? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh(forceShowLoading: Boolean = false) {
        viewModelScope.launch {
            if (forceShowLoading || (_uiState.value.topPlaces.isEmpty() && _uiState.value.topUsers.isEmpty())) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            // Równoległy fetch obu list – ranking ładuje się tak szybko jak
            // wolniejsze z dwóch zapytań, a nie jako ich suma.
            //
            // Fetch pool > visible limit - bierzemy z zapasem, żeby po
            // odfiltrowaniu "nieaktywnych" wpisów (zob. niżej) i tak mieć
            // szansę zapełnić limit aktywnymi userami / miejscami.
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

            // --- Filtry "aktywności" ---
            // Świadomie ukrywamy świeże / nieaktywne wpisy z rankingu, żeby
            // top był sensowny merytorycznie:
            //  - miejsce z 0 opinii lub averageRating == 0.0 nie zasłużyło
            //    jeszcze na pozycję na liście (każde nowe miejsce startuje
            //    z 0/0 - inaczej top byłby zalany świeżakami);
            //  - user bez ani jednego dodanego miejsca i bez ani jednej opinii
            //    nie ma czego "rankingować" - pojawi się dopiero po
            //    pierwszej aktywności.
            //
            // Po filtrze tnijemy do limitu z Remote Config - to twardy sufit dla UI.
            val places = (placesResult as? OpResult.Success)?.data.orEmpty()
                .filter { it.reviewsCount > 0 && it.averageRating > 0.0 }
                .take(performanceConfig.rankingTopLimit)
            val users = (usersResult as? OpResult.Success)?.data.orEmpty()
                .filter { it.placesAddedCount > 0 || it.reviewsCount > 0 }
                .take(performanceConfig.rankingTopLimit)

            // Łączymy komunikaty błędów z obu fetchów
            val error = when {
                placesResult is OpResult.Failure && usersResult is OpResult.Failure ->
                    placesResult.error.toPlacesErrorMessage(RANKING_ERROR_FALLBACK)
                placesResult is OpResult.Failure ->
                    placesResult.error.toPlacesErrorMessage(RANKING_ERROR_FALLBACK)
                usersResult is OpResult.Failure ->
                    usersResult.error.toPlacesErrorMessage(RANKING_ERROR_FALLBACK)
                else -> null
            }

            // Precomputuj odznaki per user z pełnym BadgeContext (rank w
            // rankingu userów + najlepsza pozycja jakiegokolwiek miejsca
            // tego usera w rankingu miejsc). Dzięki temu karta usera w
            // rankingu pokazuje TE SAME odznaki co user widzi na swoim
            // profilu - żadnego "tu mam 5, tam tylko 3" mylącego.
            //
            // Reguły spójne z ProfileViewModel.computeBadgeContext: filtry
            // aktywności już zaaplikowane w `users` / `places` powyżej,
            // więc indeks 1-based w tych listach to dokładnie ranga, którą
            // user widzi w UI.
            val userBadges: Map<String, List<UserBadge>> = users.mapIndexed { idx, u ->
                val userRank = (idx + 1).takeIf { it in 1..3 }
                val bestPlaceRank = places
                    .mapIndexedNotNull { pIdx, p ->
                        if (p.ownerUserId == u.id) pIdx + 1 else null
                    }
                    .minOrNull()
                u.id to u.computeBadges(BadgeContext(userRank, bestPlaceRank))
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
