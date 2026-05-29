package com.kidzone.domain.repository

import com.kidzone.domain.model.Review
import com.kidzone.utils.OpResult
import kotlinx.coroutines.flow.Flow

/**
 * Operacje na opiniach o miejscach.
 */
interface ReviewRepository {

    fun observeReviewsForPlace(placeId: String): Flow<List<Review>>

    suspend fun addReview(review: Review): OpResult<Review>

    /**
     * Aktualizuje istniejącą opinię (zmiana ratingu i/lub komentarza).
     *
     * Implementacja powinna w transakcji:
     *  1) odczytać starą wartość `rating` z dokumentu opinii,
     *  2) zapisać nowe pola opinii + `updatedAtMillis = now`,
     *  3) przeliczyć `place.averageRating` o deltę
     *     `(newRating - oldRating) / reviewsCount` (count się nie zmienia).
     *
     * Pola immutowalne (`id`, `placeId`, `userId`, `authorName`, `createdAtMillis`)
     * są ignorowane przez implementację – używa wartości z istniejącego dokumentu.
     */
    suspend fun updateReview(review: Review): OpResult<Review>

    suspend fun reportReviewAsSpam(reviewId: String): OpResult<Unit>
}
