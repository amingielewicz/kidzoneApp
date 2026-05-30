package com.kidzone.presentation.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.presentation.common.BadgeContext
import com.kidzone.presentation.common.UserBadge
import com.kidzone.presentation.common.computeBadges
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
     * @property topPlaces top miejsc wg średniej oceny (malejąco), do [TOP_LIMIT] pozycji
     * @property topUsers  top użytkowników wg liczby dodanych miejsc, drugorzędnie po liczbie opinii, do [TOP_LIMIT] pozycji
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
            //
            // FETCH_POOL > TOP_LIMIT - bierzemy z zapasem, żeby po
            // odfiltrowaniu "nieaktywnych" wpisów (zob. niżej) i tak mieć
            // szansę zapełnić TOP_LIMIT pozycji aktywnymi userami / miejscami.
            val (placesResult, usersResult) = coroutineScope {
                val placesDeferred = async { placeRepository.getTopPlaces(limit = FETCH_POOL) }
                val usersDeferred = async { authRepository.getTopUsers(limit = FETCH_POOL) }
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
            // Po filtrze tnijemy do TOP_LIMIT - to nasz twardy sufit dla UI.
            val places = (placesResult as? OpResult.Success)?.data.orEmpty()
                .filter { it.reviewsCount > 0 && it.averageRating > 0.0 }
                .take(TOP_LIMIT)
            val users = (usersResult as? OpResult.Success)?.data.orEmpty()
                .filter { it.placesAddedCount > 0 || it.reviewsCount > 0 }
                .take(TOP_LIMIT)

            // Łączymy komunikaty błędów z obu fetchów – jeśli np. użytkownicy
            // się wczytali a miejsca nie, pokażemy błąd nie tracąc danych.
            val error = listOfNotNull(
                (placesResult as? OpResult.Failure)?.error?.message,
                (usersResult as? OpResult.Failure)?.error?.message
            ).joinToString("\n").ifBlank { null }

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

    private companion object {
        /**
         * Twardy sufit liczby pozycji widocznych na każdej z list rankingu.
         *
         * 100 to świadomy kompromis - wystarczy dla obecnej skali aplikacji
         * bez paginacji, a jednocześnie wymusza skupienie na "topowych"
         * pozycjach (długi ogon ratuje placeholder "brak ocen" w UI).
         */
        const val TOP_LIMIT = 100

        /**
         * Liczba rekordów pobieranych z repo, zanim odfiltrujemy nieaktywne
         * (zob. komentarz w [refresh]).
         *
         * Większa niż [TOP_LIMIT], żeby dać miejsce na odpadnięcie świeżych
         * miejsc z 0 opinii i userów z 0 aktywnością. 200 to praktyczny
         * sufit - przy obecnym minSdk / wczesnej fazie projektu nawet
         * 1 fetch po 200 dokumentów to ciągle szybki snapshot Firestore.
         * Jak baza userów / miejsc dramatycznie urośnie, lepiej przejść na
         * server-side filtering (zapytanie po `reviewsCount > 0`).
         */
        const val FETCH_POOL = 200
    }
}
