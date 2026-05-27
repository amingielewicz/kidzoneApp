package com.playground.domain.repository

import com.playground.domain.model.Review
import com.playground.utils.OpResult
import kotlinx.coroutines.flow.Flow

/**
 * Operacje na opiniach o miejscach.
 */
interface ReviewRepository {

    fun observeReviewsForPlace(placeId: String): Flow<List<Review>>

    suspend fun addReview(review: Review): OpResult<Review>

    suspend fun reportReviewAsSpam(reviewId: String): OpResult<Unit>
}
