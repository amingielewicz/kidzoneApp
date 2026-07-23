package com.kidzone.domain.model

/**
 * 📌 Przeznaczenie:
 * Domenowy model opinii użytkownika o miejscu.
 * Odpowiada za przechowywanie oceny, komentarza oraz dowodów wizualnych (zdjęcia).
 *
 * @property id identyfikator dokumentu opinii.
 * @property placeId identyfikator ocenianego miejsca.
 * @property userId identyfikator autora opinii.
 * @property authorName publiczna nazwa autora utrwalona przy zapisie.
 * @property rating ocena w zakresie 1–5.
 * @property comment treść opinii.
 * @property photoUrls URL-e zdjęć dołączonych do opinii.
 * @property photoHashes mapa URL zdjęcia do jego hashu MD5 (deduplikacja).
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
    val photoHashes: Map<String, String> = emptyMap(),
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)
