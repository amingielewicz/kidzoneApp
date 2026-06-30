package com.kidzone.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.kidzone.data.local.ReviewDao
import com.kidzone.data.local.ReviewEntity
import com.kidzone.data.remote.FirestoreCollections
import com.kidzone.data.remote.dto.ReviewDto
import com.kidzone.domain.model.Review
import com.kidzone.domain.repository.ReviewRepository
import com.kidzone.sync.NetworkUtils
import com.kidzone.utils.AppConfig
import com.kidzone.utils.OpResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacja [ReviewRepository] oparta o Firestore + Room cache.
 */
@Singleton
class FirestoreReviewRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val reviewDao: ReviewDao
) : ReviewRepository {

    override fun observeReviewsForPlace(placeId: String): Flow<List<Review>> = channelFlow {
        if (placeId.isBlank()) {
            trySend(emptyList())
            return@channelFlow
        }

        val localFlow = reviewDao.observeByPlace(placeId)

        launch {
            val firestoreFlow = callbackFlow {
                val registration = reviewsCollection()
                    .whereEqualTo("placeId", placeId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val reviews = snapshot?.documents
                            ?.mapNotNull { it.toObject(ReviewDto::class.java) }
                            ?.filterNot { it.reportedAsSpam }
                            ?.map { it.toDomain() }
                            .orEmpty()
                        trySend(reviews)
                    }
                awaitClose { registration.remove() }
            }
            firestoreFlow.collect { reviews ->
                reviewDao.deleteByPlace(placeId)
                reviewDao.upsertAll(reviews.map(ReviewEntity::fromDomain))
            }
        }

        localFlow.collectLatest { entities ->
            trySend(entities.map { it.toDomain() })
        }
    }

    override fun observeReviewsByUser(userId: String): Flow<List<Review>> = channelFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            return@channelFlow
        }

        val localFlow = reviewDao.observeByUser(userId)

        launch {
            val firestoreFlow = callbackFlow {
                val registration = reviewsCollection()
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val reviews = snapshot?.documents
                            ?.mapNotNull { it.toObject(ReviewDto::class.java)?.toDomain() }
                            ?.sortedByDescending { it.createdAtMillis }
                            .orEmpty()
                        trySend(reviews)
                    }
                awaitClose { registration.remove() }
            }
            firestoreFlow.collect { reviews ->
                reviewDao.upsertAll(reviews.map(ReviewEntity::fromDomain))
            }
        }

        // 3. Emituj dane z Room.
        localFlow.collectLatest { entities ->
            trySend(entities.map { it.toDomain() })
        }
    }

    override suspend fun addReview(review: Review): OpResult<Review> = try {
        require(review.placeId.isNotBlank()) { "Review.placeId nie może być puste" }
        require(review.rating in 1..5) { "Review.rating musi być w zakresie 1..5" }
        require(review.comment.length <= AppConfig.REVIEW_COMMENT_MAX_LENGTH) {
            "Review.comment przekracza limit ${AppConfig.REVIEW_COMMENT_MAX_LENGTH} znaków"
        }

        val reviewRef = reviewsCollection().document()
        val reviewWithId = review.copy(id = reviewRef.id)

        val completed = withTimeoutOrNull(AppConfig.WRITE_TIMEOUT_MS) {
            reviewsCollection().document(reviewRef.id)
                .set(ReviewDto.fromDomain(reviewWithId))
                .await()
            true
        }
        if (completed == null) {
            offlineSyncDisabledFailure()
        } else {
            reviewDao.upsert(ReviewEntity.fromDomain(reviewWithId))
            OpResult.success(reviewWithId)
        }
    } catch (e: Exception) {
        if (NetworkUtils.isNetworkError(e)) {
            offlineSyncDisabledFailure()
        } else {
            OpResult.failure(e)
        }
    }

    override suspend fun updateReview(review: Review): OpResult<Review> = try {
        require(review.id.isNotBlank()) { "Review.id musi być znane przy update" }
        require(review.rating in 1..5) { "Review.rating musi być w zakresie 1..5" }
        require(review.comment.length <= AppConfig.REVIEW_COMMENT_MAX_LENGTH) {
            "Review.comment przekracza limit ${AppConfig.REVIEW_COMMENT_MAX_LENGTH} znaków"
        }

        val updatedReview = review.copy(updatedAtMillis = System.currentTimeMillis())

        val completed = withTimeoutOrNull(AppConfig.WRITE_TIMEOUT_MS) {
            reviewsCollection().document(updatedReview.id)
                .set(ReviewDto.fromDomain(updatedReview))
                .await()
            true
        }
        if (completed == null) {
            offlineSyncDisabledFailure()
        } else {
            reviewDao.upsert(ReviewEntity.fromDomain(updatedReview))
            OpResult.success(updatedReview)
        }
    } catch (e: Exception) {
        if (NetworkUtils.isNetworkError(e)) {
            offlineSyncDisabledFailure()
        } else {
            OpResult.failure(e)
        }
    }

    override suspend fun reportReviewAsSpam(
        reviewId: String,
        reporterId: String,
        reason: String,
        comment: String
    ): OpResult<Unit> = try {
        require(reviewId.isNotBlank()) { "reviewId nie może być puste" }
        require(reporterId.isNotBlank()) { "reporterId nie może być puste" }

        val existing = firestore.collection(FirestoreCollections.REVIEW_REPORTS)
            .whereEqualTo("reporterId", reporterId)
            .whereEqualTo("reviewId", reviewId)
            .get()
            .await()
        if (existing.documents.isNotEmpty()) {
            throw AlreadyReportedException()
        }

        val reportData = mapOf(
            "reviewId" to reviewId,
            "reporterId" to reporterId,
            "reason" to reason,
            "comment" to comment,
            "createdAtMillis" to System.currentTimeMillis(),
            "status" to "pending"
        )
        val completed = withTimeoutOrNull(AppConfig.WRITE_TIMEOUT_MS) {
            firestore.collection(FirestoreCollections.REVIEW_REPORTS)
                .add(reportData)
                .await()
            true
        }
        if (completed == null) {
            OpResult.failure(java.util.concurrent.TimeoutException("Przekroczono czas oczekiwania"))
        } else {
            OpResult.success(Unit)
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    override suspend fun getReportedReviews(userId: String): Set<String> = try {
        val snapshot = firestore.collection(FirestoreCollections.REVIEW_REPORTS)
            .whereEqualTo("reporterId", userId)
            .get()
            .await()
        snapshot.documents.mapNotNull { it.getString("reviewId") }.toSet()
    } catch (_: Exception) {
        emptySet()
    }

    override suspend fun deleteReview(reviewId: String): OpResult<Unit> = try {
        require(reviewId.isNotBlank()) { "reviewId nie może być puste" }

        val completed = withTimeoutOrNull(AppConfig.WRITE_TIMEOUT_MS) {
            reviewsCollection().document(reviewId).delete().await()
            true
        }
        if (completed == null) {
            offlineSyncDisabledFailure()
        } else {
            reviewDao.deleteById(reviewId)
            OpResult.success(Unit)
        }
    } catch (e: Exception) {
        if (NetworkUtils.isNetworkError(e)) {
            offlineSyncDisabledFailure()
        } else {
            OpResult.failure(e)
        }
    }

    private fun reviewsCollection() = firestore.collection(FirestoreCollections.REVIEWS)

    private fun <T> offlineSyncDisabledFailure(): OpResult<T> =
        OpResult.failure(OfflineReviewSyncDisabledException())

    class OfflineReviewSyncDisabledException : IllegalStateException(
        "Nie udało się zapisać opinii offline. Sprawdź połączenie i spróbuj ponownie."
    )
    class AlreadyReportedException : IllegalStateException("Już zgłosiłeś tę opinię")
}
