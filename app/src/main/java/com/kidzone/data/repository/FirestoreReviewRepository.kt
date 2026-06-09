package com.kidzone.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kidzone.data.local.ReviewDao
import com.kidzone.data.local.ReviewEntity
import com.kidzone.data.remote.FirestoreCollections
import com.kidzone.data.remote.dto.ReviewDto
import com.kidzone.domain.model.Review
import com.kidzone.domain.repository.ReviewRepository
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
 * Maks. czas na zapis do Firestore (w ms) – patrz komentarz w [FirestorePlaceRepository].
 */
private const val WRITE_TIMEOUT_MS = 30_000L

/**
 * Maksymalna długość komentarza w opinii. Trzymane jako stała w warstwie
 * data, bo to ostateczny strażnik – UI również cappuje (AddReviewSheet),
 * ale walidacja po stronie repo gwarantuje kontrakt domeny.
 */
private const val REVIEW_COMMENT_MAX_LENGTH = 1000

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
    private val reviewDao: ReviewDao
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

        syncJob.cancel()
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

        syncJob.cancel()
    }

    override suspend fun addReview(review: Review): OpResult<Review> = try {
        require(review.placeId.isNotBlank()) { "Review.placeId nie może być puste" }
        require(review.rating in 1..5) { "Review.rating musi być w zakresie 1..5" }
        require(review.comment.length <= REVIEW_COMMENT_MAX_LENGTH) {
            "Review.comment przekracza limit $REVIEW_COMMENT_MAX_LENGTH znaków"
        }

        val reviewRef = reviewsCollection().document()
        val placeRef = firestore.collection(FirestoreCollections.PLACES).document(review.placeId)
        val userRef = review.userId
            .takeIf { it.isNotBlank() }
            ?.let { firestore.collection(FirestoreCollections.USERS).document(it) }

        val reviewWithId = review.copy(id = reviewRef.id)

        val completed = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            firestore.runTransaction<Unit> { tx ->
                val placeSnap = tx.get(placeRef)
                if (!placeSnap.exists()) {
                    throw NoSuchElementException("Brak miejsca o id=${review.placeId}")
                }
                val oldCount = placeSnap.getLong("reviewsCount")?.toInt() ?: 0
                val oldAvg = placeSnap.getDouble("averageRating") ?: 0.0
                val newCount = oldCount + 1
                val newAvg = (oldAvg * oldCount + reviewWithId.rating) / newCount

                tx.set(reviewRef, ReviewDto.fromDomain(reviewWithId))
                tx.update(
                    placeRef,
                    mapOf(
                        "reviewsCount" to newCount,
                        "averageRating" to newAvg
                    )
                )
                if (userRef != null) {
                    tx.set(
                        userRef,
                        mapOf("reviewsCount" to FieldValue.increment(1)),
                        SetOptions.merge()
                    )
                }
            }.await()
            true
        }
        if (completed == null) {
            OpResult.failure(
                java.util.concurrent.TimeoutException(
                    "Zapis trwa zbyt długo. Sprawdź połączenie z Internetem."
                )
            )
        } else {
            // Persystuj do cache.
            reviewDao.upsert(ReviewEntity.fromDomain(reviewWithId))
            OpResult.success(reviewWithId)
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    override suspend fun updateReview(review: Review): OpResult<Review> = try {
        require(review.id.isNotBlank()) { "Review.id musi być znane przy update" }
        require(review.rating in 1..5) { "Review.rating musi być w zakresie 1..5" }
        require(review.comment.length <= REVIEW_COMMENT_MAX_LENGTH) {
            "Review.comment przekracza limit $REVIEW_COMMENT_MAX_LENGTH znaków"
        }

        val reviewRef = reviewsCollection().document(review.id)
        val updatedReview = review.copy(updatedAtMillis = System.currentTimeMillis())

        val completed = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            firestore.runTransaction<Unit> { tx ->
                val reviewSnap = tx.get(reviewRef)
                if (!reviewSnap.exists()) {
                    throw NoSuchElementException("Brak opinii o id=${review.id}")
                }
                val existing = reviewSnap.toObject(ReviewDto::class.java)
                    ?: throw IllegalStateException("Nieczytelny dokument opinii ${review.id}")
                if (existing.userId != review.userId) {
                    throw SecurityException("Można edytować tylko własne opinie")
                }
                val placeId = existing.placeId.ifBlank { review.placeId }
                val placeRef = firestore
                    .collection(FirestoreCollections.PLACES)
                    .document(placeId)
                val placeSnap = tx.get(placeRef)
                if (!placeSnap.exists()) {
                    throw NoSuchElementException("Brak miejsca o id=$placeId")
                }
                val count = placeSnap.getLong("reviewsCount")?.toInt() ?: 0
                val oldAvg = placeSnap.getDouble("averageRating") ?: 0.0
                val oldRating = existing.rating
                val newRating = updatedReview.rating

                val newAvg = if (count > 0) {
                    (oldAvg * count - oldRating + newRating) / count
                } else {
                    newRating.toDouble()
                }

                val merged = existing.copy(
                    rating = newRating,
                    comment = updatedReview.comment,
                    photoUrls = updatedReview.photoUrls,
                    updatedAtMillis = updatedReview.updatedAtMillis
                )
                tx.set(reviewRef, merged)
                tx.update(placeRef, mapOf("averageRating" to newAvg))
                Unit
            }.await()
            true
        }
        if (completed == null) {
            OpResult.failure(
                java.util.concurrent.TimeoutException(
                    "Zapis trwa zbyt długo. Sprawdź połączenie z Internetem."
                )
            )
        } else {
            // Zaktualizuj cache.
            reviewDao.upsert(ReviewEntity.fromDomain(updatedReview))
            OpResult.success(updatedReview)
        }
    } catch (e: Exception) {
        OpResult.failure(e)
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
        val completed = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
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

    override suspend fun deleteReview(reviewId: String): OpResult<Unit> = try {
        require(reviewId.isNotBlank()) { "reviewId nie może być puste" }
        val reviewRef = reviewsCollection().document(reviewId)

        val completed = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            firestore.runTransaction<Unit> { tx ->
                val reviewSnap = tx.get(reviewRef)
                if (!reviewSnap.exists()) {
                    return@runTransaction
                }
                val existing = reviewSnap.toObject(ReviewDto::class.java)
                    ?: throw IllegalStateException("Nieczytelny dokument opinii $reviewId")

                val placeRef = firestore
                    .collection(FirestoreCollections.PLACES)
                    .document(existing.placeId)
                val placeSnap = tx.get(placeRef)
                val placeExists = placeSnap.exists()

                val authorRef = existing.userId
                    .takeIf { it.isNotBlank() }
                    ?.let { firestore.collection(FirestoreCollections.USERS).document(it) }

                tx.delete(reviewRef)

                if (placeExists) {
                    val oldCount = placeSnap.getLong("reviewsCount")?.toInt() ?: 0
                    val oldAvg = placeSnap.getDouble("averageRating") ?: 0.0
                    val newCount = (oldCount - 1).coerceAtLeast(0)
                    val newAvg = if (newCount > 0) {
                        ((oldAvg * oldCount) - existing.rating) / newCount
                    } else {
                        0.0
                    }
                    tx.update(
                        placeRef,
                        mapOf(
                            "reviewsCount" to newCount,
                            "averageRating" to newAvg
                        )
                    )
                }

                if (authorRef != null) {
                    tx.set(
                        authorRef,
                        mapOf("reviewsCount" to FieldValue.increment(-1)),
                        SetOptions.merge()
                    )
                }
            }.await()
            true
        }
        if (completed == null) {
            OpResult.failure(
                java.util.concurrent.TimeoutException(
                    "Usunięcie trwa zbyt długo. Sprawdź połączenie z Internetem."
                )
            )
        } else {
            // Usuń z cache.
            reviewDao.deleteById(reviewId)
            OpResult.success(Unit)
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    private fun reviewsCollection() = firestore.collection(FirestoreCollections.REVIEWS)
}
