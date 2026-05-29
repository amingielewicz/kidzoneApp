package com.kidzone.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.kidzone.data.remote.FirestoreCollections
import com.kidzone.data.remote.dto.ReviewDto
import com.kidzone.domain.model.Review
import com.kidzone.domain.repository.ReviewRepository
import com.kidzone.utils.OpResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maks. czas na zapis do Firestore (w ms) – patrz komentarz w [FirestorePlaceRepository].
 */
private const val WRITE_TIMEOUT_MS = 30_000L

/**
 * Implementacja [ReviewRepository] oparta o Firestore.
 *
 * `reportReviewAsSpam` jest jeszcze placeholderem – do uzupełnienia w
 * osobnym PR-ze (powinien analogicznie zaktualizować `averageRating`
 * miejsca, jeśli traktujemy spam jako "wycofanie" oceny).
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

    override suspend fun addReview(review: Review): OpResult<Review> = try {
        require(review.placeId.isNotBlank()) { "Review.placeId nie może być puste" }
        require(review.rating in 1..5) { "Review.rating musi być w zakresie 1..5" }

        val reviewRef = reviewsCollection().document()
        val placeRef = firestore.collection(FirestoreCollections.PLACES).document(review.placeId)
        val userRef = review.userId
            .takeIf { it.isNotBlank() }
            ?.let { firestore.collection(FirestoreCollections.USERS).document(it) }

        val reviewWithId = review.copy(id = reviewRef.id)

        // Transakcja zapewnia ATOMOWOŚĆ całej operacji "dodaj opinię":
        //  1) zapis dokumentu Review,
        //  2) przeliczenie i zapis nowego averageRating + reviewsCount na Place,
        //  3) inkrementacja reviewsCount na User (do rankingu).
        //
        // Reguła Firestore: wszystkie READ-y muszą być przed WRITE-ami.
        // Dlatego najpierw pobieramy aktualny stan miejsca.
        val completed = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            firestore.runTransaction<Unit> { tx ->
                val placeSnap = tx.get(placeRef)
                if (!placeSnap.exists()) {
                    throw NoSuchElementException("Brak miejsca o id=${review.placeId}")
                }
                val oldCount = placeSnap.getLong("reviewsCount")?.toInt() ?: 0
                val oldAvg = placeSnap.getDouble("averageRating") ?: 0.0
                val newCount = oldCount + 1
                // Średnia krocząca: nie trzymamy historii pojedynczych ocen,
                // tylko aktualizujemy agregat. Dla setek ocen precyzja Double
                // jest w pełni wystarczająca.
                val newAvg = (oldAvg * oldCount + reviewWithId.rating) / newCount

                tx.set(reviewRef, ReviewDto.fromDomain(reviewWithId))
                tx.update(
                    placeRef,
                    mapOf(
                        "reviewsCount" to newCount,
                        "averageRating" to newAvg
                    )
                )
                // FieldValue.increment jest atomowy po stronie serwera.
                // Pomijamy gdy userRef = null (anonimowy / brak uid), żeby
                // nie wywalać całej transakcji.
                userRef?.let {
                    tx.update(it, "reviewsCount", FieldValue.increment(1))
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
            OpResult.success(reviewWithId)
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    override suspend fun reportReviewAsSpam(reviewId: String): OpResult<Unit> {
        // TODO: reviewsCollection().document(reviewId).update("reportedAsSpam", true)
        //  + ewentualne przeliczenie averageRating na Place w transakcji.
        return OpResult.failure(NotImplementedError("reportReviewAsSpam – do uzupełnienia"))
    }

    private fun reviewsCollection() = firestore.collection(FirestoreCollections.REVIEWS)
}
