package com.kidzone.domain.repository

import com.kidzone.domain.model.Review
import com.kidzone.utils.OpResult
import kotlinx.coroutines.flow.Flow

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie opiniami o miejscach (dodawanie, edycja, usuwanie).
 * - Obsługa zgłoszeń (spam, naruszenia).
 * - Koordynacja agregatów ocen na poziomie miejsca.
 *
 * 🔌 Strategia Cache:
 * - Single Source of Truth (Room) dla wyświetlanych list opinii.
 * - Aktywne nasłuchiwanie (Snapshot Listeners) dla aktualnego miejsca.
 *
 * 🛡️ Autoryzacja i Bezpieczeństwo:
 * - Odczyt opinii jest publiczny.
 * - Tworzenie/edycja wymaga zalogowanego użytkownika.
 * - Edycja i usuwanie dozwolone wyłącznie dla autora opinii lub administratora.
 *
 * ✅ Gwarancje spójności:
 * - Atomowe przeliczanie średniej oceny miejsca przy dodawaniu/usuwaniu opinii (backend-side).
 * - Automatyczne ukrywanie opinii zgłoszonych jako spam w strumieniu publicznym.
 *
 * 📤 Mapowanie błędów:
 * - Błędy sieci i uprawnień mapowane na [OpResult].
 *
 * 🧵 Threading:
 * - Bezpieczne do wywołania z dowolnego wątku.
 */
interface ReviewRepository {

    /**
     * Obserwuje publiczne opinie przypisane do wskazanego miejsca.
     *
     * Implementacja może najpierw wyemitować dane z cache, a następnie odświeżyć je ze źródła
     * zdalnego. Opinie ukryte przez moderację nie powinny trafiać do publicznego strumienia.
     *
     * @param placeId identyfikator miejsca; pusty identyfikator powinien zwrócić pusty strumień.
     * @return strumień aktualnej listy opinii dla miejsca.
     */
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
     *
     * @param userId identyfikator autora opinii.
     * @return strumień opinii użytkownika.
     */
    fun observeReviewsByUser(userId: String): Flow<List<Review>>

    /**
     * Dodaje nową opinię i aktualizuje agregaty miejsca oraz użytkownika.
     *
     * Operacja powinna być odporna na ponowienie i nie tworzyć duplikatu po timeoutcie lub
     * wielokrotnym kliknięciu. Sukces oznacza potwierdzony zapis po stronie backendu.
     *
     * @param review kompletna opinia do zapisania.
     * @return zapisana opinia albo zmapowany błąd domenowy.
     */
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
     *
     * @param review opinia zawierająca nową ocenę lub komentarz.
     * @return zaktualizowana opinia albo zmapowany błąd domenowy.
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
     *
     * @param reviewId identyfikator opinii do usunięcia.
     * @return sukces po zakończeniu zapisu i aktualizacji agregatów albo błąd.
     */
    suspend fun deleteReview(reviewId: String): OpResult<Unit>

    /**
     * Zgłasza opinię jako spam/naruszenie.
     *
     * Analogicznie do [PlaceRepository.reportPlace]: zapis do kolekcji
     * `review_reports` z danymi zgłaszającego, powodem i komentarzem.
     *
     * @param reviewId identyfikator zgłaszanej opinii.
     * @param reporterId identyfikator zgłaszającego użytkownika.
     * @param reason wybrany powód zgłoszenia.
     * @param comment opcjonalny opis uzupełniający.
     */
    suspend fun reportReviewAsSpam(
        reviewId: String,
        reporterId: String,
        reason: String,
        comment: String = ""
    ): OpResult<Unit>

    /**
     * Pobiera identyfikatory opinii zgłoszonych przez użytkownika.
     *
     * @param userId identyfikator zgłaszającego.
     * @return zbiór identyfikatorów opinii; pusty zbiór, gdy brak zgłoszeń.
     */
    suspend fun getReportedReviews(userId: String): Set<String>
}
