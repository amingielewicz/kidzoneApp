package com.kidzone.domain.repository

import com.kidzone.domain.model.Review
import com.kidzone.utils.OpResult
import kotlinx.coroutines.flow.Flow

/**
 * Operacje na opiniach o miejscach.
 */
interface ReviewRepository {

    fun observeReviewsForPlace(placeId: String): Flow<List<Review>>

    /**
     * Strumień opinii konkretnego użytkownika (snapshot listener).
     *
     * Używane przez ekran "Moje opinie" w profilu. Filtr `reportedAsSpam`
     * jest po stronie klienta (jak w [observeReviewsForPlace]) – chcemy
     * jednak pokazywać też swoje opinie oznaczone jako spam, żeby user
     * widział, co napisał (UX > "ukrywaj na siłę").
     *
     * Sortowanie: po `createdAtMillis` malejąco (najnowsze na górze).
     * Wykonywane klient-side, żeby uniknąć composite indexu.
     *
     * Emituje pustą listę gdy [userId] jest pusty.
     */
    fun observeReviewsByUser(userId: String): Flow<List<Review>>

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

    /**
     * Usuwa opinię i przelicza agregaty:
     *  - `place.averageRating` (klient-side, średnia krocząca),
     *  - `place.reviewsCount` (decrement),
     *  - `users/{authorId}.reviewsCount` (decrement, atomowo
     *    przez [com.google.firebase.firestore.FieldValue.increment]).
     *
     * Reguły Firestore na razie pozwalają usunąć tylko własną opinię
     * (`userId == auth.uid`).
     */
    suspend fun deleteReview(reviewId: String): OpResult<Unit>

    suspend fun reportReviewAsSpam(reviewId: String): OpResult<Unit>
}
