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
 * Wylicza odznaki użytkownika na podstawie rankingów (top userów + top miejsc).
 *
 * Logika rankingowa jest spójna z [com.kidzone.presentation.ranking.RankingViewModel]:
 *  - userzy filtrujemy do aktywnych (>=1 miejsce LUB >=1 opinia),
 *  - miejsca filtrujemy do takich z >0 opinii i >0.0 średniej.
 *
 * Best-effort: przy błędzie fetcha zwracamy pusty [BadgeContext],
 * co oznacza brak odznak rankingowych (count-based dalej działają).
 */
class ComputeBadgesUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val placeRepository: PlaceRepository
) {

    data class BadgeResult(
        val obtainedBadges: List<UserBadge>,
        val userRank: Int?,
        val bestPlaceRank: Int?
    )

    /**
     * @param user bogaty profil usera z Firestore (z licznikami)
     * @param rankPool max ilość userów/miejsc do pobrania (domyślnie 100)
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
