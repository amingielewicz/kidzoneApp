package com.playground.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.playground.data.remote.FirestoreCollections
import com.playground.data.remote.dto.ReviewDto
import com.playground.domain.model.Review
import com.playground.domain.repository.ReviewRepository
import com.playground.utils.OpResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacja [ReviewRepository] oparta o Firestore.
 *
 * `addReview` i `reportReviewAsSpam` są jeszcze placeholderami – do
 * uzupełnienia w osobnym PR-ze (wymagają transakcji aktualizującej
 * `averageRating` na dokumencie Place).
 */
@Singleton
class FirestoreReviewRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewRepository {

    override fun observeReviewsForPlace(placeId: String): Flow<List<Review>> = callbackFlow {
        // Filtr `reportedAsSpam=false` celowo robimy klient-side – inaczej
        // Firestore wymagałby composite indexu (placeId + reportedAsSpam),
        // którego użytkownik musiałby ręcznie utworzyć w konsoli.
        val registration = reviewsCollection()
            .whereEqualTo("placeId", placeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val reviews = snapshot?.documents
                    ?.mapNotNull { it.toObject<ReviewDto>() }
                    ?.filterNot { it.reportedAsSpam }
                    ?.map { it.toDomain() }
                    .orEmpty()
                trySend(reviews)
            }
        awaitClose { registration.remove() }
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

    private fun reviewsCollection() = firestore.collection(FirestoreCollections.REVIEWS)
}
