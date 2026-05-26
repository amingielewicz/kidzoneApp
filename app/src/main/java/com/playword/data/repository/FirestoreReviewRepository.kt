package com.playword.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.playword.data.remote.FirestoreCollections
import com.playword.data.remote.dto.ReviewDto
import com.playword.domain.model.Review
import com.playword.domain.repository.ReviewRepository
import com.playword.utils.OpResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacja [ReviewRepository] oparta o Firestore.
 *
 * Implementacje są placeholderami – do uzupełnienia w kolejnych iteracjach.
 */
@Singleton
class FirestoreReviewRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewRepository {

    override fun observeReviewsForPlace(placeId: String): Flow<List<Review>> {
        // TODO: snapshotListener na reviewsCollection().whereEqualTo("placeId", placeId)
        return flowOf(emptyList())
    }

    override suspend fun addReview(review: Review): OpResult<Review> {
        // TODO: reviewsCollection().add(ReviewDto.fromDomain(review)) + transakcja
        // aktualizująca averageRating na Place.
        return OpResult.failure(NotImplementedError("addReview – do uzupełnienia"))
    }

    override suspend fun reportReviewAsSpam(reviewId: String): OpResult<Unit> {
        // TODO: reviewsCollection().document(reviewId).update("reportedAsSpam", true)
        return OpResult.failure(NotImplementedError("reportReviewAsSpam – do uzupełnienia"))
    }

    @Suppress("unused")
    private fun reviewsCollection() = firestore.collection(FirestoreCollections.REVIEWS)

    @Suppress("unused")
    private fun ReviewDto.dummyReference(): ReviewDto = this // marker, by import nie został wycięty
}
