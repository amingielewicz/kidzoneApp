package com.kidzone.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
 * Maksymalna długość komentarza w opinii. Trzymane jako stała w warstwie
 * data, bo to ostateczny strażnik – UI również cappuje (AddReviewSheet),
 * ale walidacja po stronie repo gwarantuje kontrakt domeny.
 */
private const val REVIEW_COMMENT_MAX_LENGTH = 1000

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
        // Defense-in-depth: UI też cappuje na 1000 (AddReviewSheet),
        // ale walidujemy tu na wypadek gdyby ktoś zawołał repo z innego
        // miejsca lub spreparował dane z poziomu testu.
        require(review.comment.length <= REVIEW_COMMENT_MAX_LENGTH) {
            "Review.comment przekracza limit $REVIEW_COMMENT_MAX_LENGTH znaków"
        }

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
                //
                // set + merge zamiast update: nie wywali się jeśli doc usera
                // nie istnieje – po prostu utworzy minimalny doc z samym
                // licznikiem. Brakujące pola dopełni ensureUserDoc() przy
                // najbliższym logowaniu.
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

        // Transakcja:
        //  1) READ aktualna opinia (po `oldRating`) – jest też walidacją,
        //     że doc istnieje i należy do `userId` z payload-u (rules to
        //     egzekwują, ale lepiej rzucić jasny błąd zamiast czekać na
        //     PERMISSION_DENIED).
        //  2) READ miejsce – musimy znać aktualne `averageRating` i
        //     `reviewsCount`, żeby przeliczyć średnią po edycji.
        //  3) WRITE opinia z nowymi polami + `updatedAtMillis = now`.
        //  4) WRITE place z nowym `averageRating` (count bez zmian).
        //
        // `users.reviewsCount` zostaje – edycja to nadal jedna opinia.
        val updatedReview = review.copy(updatedAtMillis = System.currentTimeMillis())
        val completed = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            firestore.runTransaction<Unit> { tx ->
                val reviewSnap = tx.get(reviewRef)
                if (!reviewSnap.exists()) {
                    throw NoSuchElementException("Brak opinii o id=${review.id}")
                }
                val existing = reviewSnap.toObject<ReviewDto>()
                    ?: throw IllegalStateException("Nieczytelny dokument opinii ${review.id}")
                if (existing.userId != review.userId) {
                    // Defensywnie – właściwie zatrzymają to security rules,
                    // ale eksplicytny komunikat jest dla nas czytelniejszy.
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

                // Średnia krocząca – delta z różnicy ratingów.
                // Gdyby z jakiegoś powodu count == 0 (sytuacja niespójna –
                // doc opinii istnieje, ale licznik miejsca = 0), traktujemy
                // edycję jak pierwszą ocenę: average = newRating.
                val newAvg = if (count > 0) {
                    (oldAvg * count - oldRating + newRating) / count
                } else {
                    newRating.toDouble()
                }

                // Składamy DTO zachowując pola immutowalne z istniejącego
                // dokumentu (placeId, userId, authorName, createdAtMillis,
                // photoUrls), nadpisując tylko to, co user mógł zmienić.
                val merged = existing.copy(
                    rating = newRating,
                    comment = updatedReview.comment,
                    updatedAtMillis = updatedReview.updatedAtMillis
                )
                tx.set(reviewRef, merged)
                tx.update(placeRef, mapOf("averageRating" to newAvg))
                // tx.update zwraca Transaction; lambda runTransaction<Unit>
                // wymaga ostatniego wyrażenia typu Unit, więc jawnie kończymy
                // blok Unitem. (Analogiczny problem w addReview rozwiązany
                // tam przez `if (userRef != null) { ... }` jako ostatni
                // statement – tu nie ma naturalnego warunku.)
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
            OpResult.success(updatedReview)
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
