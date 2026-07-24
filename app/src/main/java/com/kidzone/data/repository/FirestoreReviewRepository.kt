package com.kidzone.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.kidzone.data.local.ReviewDao
import com.kidzone.data.local.ReviewEntity
import com.kidzone.data.remote.FirestoreCollections
import com.kidzone.data.remote.dto.ReviewDto
import com.kidzone.domain.model.Review
import com.kidzone.domain.repository.ReviewRepository
import com.kidzone.R
import com.kidzone.sync.NetworkUtils
import com.kidzone.utils.AppConfig
import com.kidzone.utils.OpResult
import com.kidzone.utils.RepositoryException
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
        require(review.placeId.isNotBlank()) { "Review.placeId cannot be blank" }
        require(review.rating in 1..5) { "Review.rating must be in range 1..5" }
        require(review.comment.length <= AppConfig.REVIEW_COMMENT_MAX_LENGTH) {
            "Review.comment exceeds the ${AppConfig.REVIEW_COMMENT_MAX_LENGTH} character limit"
        }

        val reviewId = review.id.ifBlank {
            reviewsCollection().document().id
        }

        val reviewWithId = review.copy(id = reviewId)

        val completed = withTimeoutOrNull(AppConfig.WRITE_TIMEOUT_MS) {
            reviewsCollection().document(reviewId)
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
        require(review.id.isNotBlank()) { "Review.id must be known for update" }
        require(review.rating in 1..5) { "Review.rating must be in range 1..5" }
        require(review.comment.length <= AppConfig.REVIEW_COMMENT_MAX_LENGTH) {
            "Review.comment exceeds the ${AppConfig.REVIEW_COMMENT_MAX_LENGTH} character limit"
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
        require(reviewId.isNotBlank()) { "reviewId cannot be blank" }
        require(reporterId.isNotBlank()) { "reporterId cannot be blank" }

        val existing = firestore.collection(FirestoreCollections.REVIEW_REPORTS)
            .whereEqualTo("reporterId", reporterId)
            .whereEqualTo("reviewId", reviewId)
            .get()
            .await()
        if (existing.documents.isNotEmpty()) {
            throw RepositoryException.AlreadyReported(R.string.error_already_reported_review)
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
            OpResult.failure(RepositoryException.Timeout(R.string.error_timeout_report_place))
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
        require(reviewId.isNotBlank()) { "reviewId cannot be blank" }

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
        OpResult.failure(RepositoryException.OfflineSyncDisabled(R.string.error_offline_sync_disabled))
}
