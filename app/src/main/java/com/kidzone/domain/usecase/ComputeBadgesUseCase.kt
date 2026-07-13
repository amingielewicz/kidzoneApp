package com.kidzone.domain.usecase

import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.presentation.common.BadgeContext
import com.kidzone.presentation.common.UserBadge
import com.kidzone.presentation.common.computeBadges
import com.kidzone.utils.OpResult
import javax.inject.Inject

/**
 * Wylicza odznaki użytkownika wymagające kontekstu rankingowego.
 *
 * Use case pobiera ograniczoną pulę użytkowników i miejsc, wyznacza pozycję użytkownika oraz
 * najlepszą pozycję należącego do niego miejsca, a następnie łączy te dane z odznakami licznikowymi.
 * Błąd pobrania rankingu jest obsługiwany best-effort i nie blokuje odznak zależnych wyłącznie od
 * danych profilu.
 */
class ComputeBadgesUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val placeRepository: PlaceRepository
) {

    /**
     * Wynik obliczenia odznak i pozycji rankingowych.
     *
     * @property obtainedBadges wszystkie aktualnie spełnione odznaki użytkownika.
     * @property userRank pozycja użytkownika w rankingu albo `null`, gdy nie został sklasyfikowany.
     * @property bestPlaceRank najlepsza pozycja miejsca użytkownika albo `null`, gdy brak wyniku.
     */
    data class BadgeResult(
        val obtainedBadges: List<UserBadge>,
        val userRank: Int?,
        val bestPlaceRank: Int?
    )

    /**
     * Oblicza odznaki dla wskazanego profilu.
     *
     * @param user pełny profil użytkownika zawierający liczniki aktywności.
     * @param rankPool maksymalna liczba użytkowników i miejsc pobierana do obliczeń.
     * @return odznaki oraz dostępne pozycje rankingowe.
     */
    suspend operator fun invoke(user: User, rankPool: Int = RANK_POOL): BadgeResult {
        val context = computeBadgeContext(user, rankPool)
        val obtained = user.computeBadges(context)
        return BadgeResult(
            obtainedBadges = obtained,
            userRank = context.userRank,
            bestPlaceRank = context.bestPlaceRank
        )
    }

    private suspend fun computeBadgeContext(user: User, rankPool: Int): BadgeContext {
        return try {
            val users = (authRepository.getTopUsers(rankPool) as? OpResult.Success)
                ?.data
                .orEmpty()
                .filter { it.placesAddedCount > 0 || it.reviewsCount > 0 }
            val userRank = users.indexOfFirst { it.id == user.id }
                .takeIf { it >= 0 }
                ?.plus(1)

            val topPlaces = (placeRepository.getTopPlaces(rankPool) as? OpResult.Success)
                ?.data
                .orEmpty()
                .filter { it.reviewsCount > 0 && it.averageRating > 0.0 }
            val bestPlaceRank = topPlaces
                .mapIndexedNotNull { idx, place ->
                    if (place.ownerUserId == user.id) idx + 1 else null
                }
                .minOrNull()

            BadgeContext(userRank = userRank, bestPlaceRank = bestPlaceRank)
        } catch (_: Exception) {
            BadgeContext()
        }
    }

    private companion object {
        const val RANK_POOL = 100
    }
}
