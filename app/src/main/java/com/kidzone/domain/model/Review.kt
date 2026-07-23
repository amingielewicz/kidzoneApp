package com.kidzone.domain.model

/**
 * Opinia użytkownika o miejscu.
 *
 * Odpowiada tabeli REVIEWS z dokumentu projektu.
 *
 * @property updatedAtMillis czas ostatniej edycji (jeśli była). 0 = nigdy nie
 *   edytowano. UI używa do pokazania plakietki "edytowana DD.MM.YYYY" gdy
 *   `updatedAtMillis > createdAtMillis`.
 */
data class Review(
    val id: String,
    val placeId: String,
    val userId: String,
    val authorName: String,
    val rating: Int, // 1..5
    val comment: String,
    val photoUrls: List<String> = emptyList(),
    /** Mapowanie URL zdjęcia -> MD5 skompresowanych bajtów. */
    val photoHashes: Map<String, String> = emptyMap(),
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)
