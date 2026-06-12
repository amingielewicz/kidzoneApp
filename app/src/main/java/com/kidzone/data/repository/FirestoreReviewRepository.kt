package com.kidzone.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kidzone.data.local.ReviewDao
import com.kidzone.data.local.ReviewEntity
import com.kidzone.data.local.sync.OperationType
import com.kidzone.data.remote.FirestoreCollections
import com.kidzone.data.remote.dto.ReviewDto
import com.kidzone.domain.model.Review
import com.kidzone.domain.repository.ReviewRepository
import com.kidzone.sync.NetworkUtils
import com.kidzone.sync.OfflinePayload
import com.kidzone.sync.SyncManager
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
 *
 * Wzorzec offline-first (analogiczny do FirestorePlaceRepository):
 *  - Room jest lokalnym source, UI obserwuje Flow z Room.
 *  - Firestore snapshot listener w tle synchronizuje dane do Room.
 *  - Przy błędzie sieci UI nadal widzi ostatnio zcache'owane opinie.
 */
@Singleton
class FirestoreReviewRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val reviewDao: ReviewDao,
    private val syncManager: SyncManager
) : ReviewRepository {

    override fun observeReviewsForPlace(placeId: String): Flow<List<Review>> = channelFlow {
        if (placeId.isBlank()) {
            trySend(emptyList())
            return@channelFlow
        }

        // 1. Room jako local source – emitujemy z niego do kanału.
        val localFlow = reviewDao.observeByPlace(placeId)

        // 2. Firestore snapshot listener – aktualizuje Room w tle.
        val syncJob = launch {
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
                // Sync do Room: nadpisz cache dla tego placeId
                reviewDao.deleteByPlace(placeId)
                reviewDao.upsertAll(reviews.map(ReviewEntity::fromDomain))
            }
        }

        // 3. Emituj dane z Room (re-emituje automatycznie po upsert z synca).
        //    Filtrujemy reportedAsSpam klient-side – Room nie ma tego pola
        //    (nie cache'ujemy spamu, bo deleteByPlace + upsertAll z przefiltrowaną
        //    listą już to załatwia).
        localFlow.collectLatest { entities ->
            trySend(entities.map { it.toDomain() })
        }
    }

    override fun observeReviewsByUser(userId: String): Flow<List<Review>> = channelFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            return@channelFlow
        }

        // 1. Room jako local source.
        val localFlow = reviewDao.observeByUser(userId)

        // 2. Firestore snapshot listener – sync do Room.
        val syncJob = launch {
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
                // Upsert all – nie czyścimy tu bo user może mieć opinie
                // w różnych miejscach, a observeByUser zwraca wszystkie.
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
            "Review.comment przekracza limit $AppConfig.REVIEW_COMMENT_MAX_LENGTH znaków"
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
            // Timeout — queue offline
            reviewDao.upsert(ReviewEntity.fromDomain(reviewWithId))
            syncManager.enqueue(OperationType.ADD_REVIEW, OfflinePayload.serializeReview(reviewWithId))
            OpResult.success(reviewWithId)
        } else {
            reviewDao.upsert(ReviewEntity.fromDomain(reviewWithId))
            OpResult.success(reviewWithId)
        }
    } catch (e: Exception) {
        if (NetworkUtils.isNetworkError(e)) {
            val reviewId = reviewsCollection().document().id
            val reviewWithId = review.copy(id = reviewId)
            reviewDao.upsert(ReviewEntity.fromDomain(reviewWithId))
            syncManager.enqueue(OperationType.ADD_REVIEW, OfflinePayload.serializeReview(reviewWithId))
            OpResult.success(reviewWithId)
        } else {
            OpResult.failure(e)
        }
    }

    override suspend fun updateReview(review: Review): OpResult<Review> = try {
        require(review.id.isNotBlank()) { "Review.id musi być znane przy update" }
        require(review.rating in 1..5) { "Review.rating musi być w zakresie 1..5" }
        require(review.comment.length <= AppConfig.REVIEW_COMMENT_MAX_LENGTH) {
            "Review.comment przekracza limit $AppConfig.REVIEW_COMMENT_MAX_LENGTH znaków"
        }

        val updatedReview = review.copy(updatedAtMillis = System.currentTimeMillis())

        val completed = withTimeoutOrNull(AppConfig.WRITE_TIMEOUT_MS) {
            reviewsCollection().document(updatedReview.id)
                .set(ReviewDto.fromDomain(updatedReview))
                .await()
            true
        }
        if (completed == null) {
            // Timeout — queue offline
            reviewDao.upsert(ReviewEntity.fromDomain(updatedReview))
            syncManager.enqueue(OperationType.UPDATE_REVIEW, OfflinePayload.serializeReview(updatedReview))
            OpResult.success(updatedReview)
        } else {
            reviewDao.upsert(ReviewEntity.fromDomain(updatedReview))
            OpResult.success(updatedReview)
        }
    } catch (e: Exception) {
        if (NetworkUtils.isNetworkError(e)) {
            val updatedReview = review.copy(updatedAtMillis = System.currentTimeMillis())
            reviewDao.upsert(ReviewEntity.fromDomain(updatedReview))
            syncManager.enqueue(OperationType.UPDATE_REVIEW, OfflinePayload.serializeReview(updatedReview))
            OpResult.success(updatedReview)
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

        // Sprawdź czy użytkownik już zgłosił tę opinię (1 zgłoszenie na użytkownika na cel)
        val existing = firestore.collection(FirestoreCollections.REVIEW_REPORTS)
            .whereEqualTo("reporterId", reporterId)
            .whereEqualTo("reviewId", reviewId)
            .get()
            .await()
        if (existing.documents.isNotEmpty()) {
            throw IllegalStateException("Już zgłosiłeś tę opinię")
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
            OpResult.failure(
                java.util.concurrent.TimeoutException(
                    "Wysłanie zgłoszenia trwa zbyt długo. Spróbuj ponownie."
                )
            )
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
            // Timeout — queue offline
            reviewDao.deleteById(reviewId)
            syncManager.enqueue(OperationType.DELETE_REVIEW, OfflinePayload.serializeId(reviewId))
            OpResult.success(Unit)
        } else {
            reviewDao.deleteById(reviewId)
            OpResult.success(Unit)
        }
    } catch (e: Exception) {
        if (NetworkUtils.isNetworkError(e)) {
            reviewDao.deleteById(reviewId)
            syncManager.enqueue(OperationType.DELETE_REVIEW, OfflinePayload.serializeId(reviewId))
            OpResult.success(Unit)
        } else {
            OpResult.failure(e)
        }
    }

    private fun reviewsCollection() = firestore.collection(FirestoreCollections.REVIEWS)
}
