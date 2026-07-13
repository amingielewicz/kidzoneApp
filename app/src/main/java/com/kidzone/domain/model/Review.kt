package com.kidzone.domain.model

/**
 * Domenowy model opinii użytkownika o miejscu.
 *
 * Model zawiera wyłącznie dane potrzebne do prezentacji i operacji domenowych. Autoryzacja edycji
 * oraz usuwania musi być egzekwowana przez backend na podstawie [userId].
 *
 * @property id identyfikator dokumentu opinii.
 * @property placeId identyfikator ocenianego miejsca.
 * @property userId identyfikator autora opinii.
 * @property authorName publiczna nazwa autora utrwalona przy zapisie.
 * @property rating ocena w zakresie 1–5.
 * @property comment treść opinii.
 * @property photoUrls URL-e zdjęć dołączonych do opinii.
 * @property createdAtMillis czas utworzenia opinii.
 * @property updatedAtMillis czas ostatniej edycji; `0` oznacza brak edycji.
 */
data class Review(
    val id: String,
    val placeId: String,
    val userId: String,
    val authorName: String,
    val rating: Int,
    val comment: String,
    val photoUrls: List<String> = emptyList(),
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)
