package com.playground.domain.model

/**
 * Opinia użytkownika o miejscu.
 *
 * Odpowiada tabeli REVIEWS z dokumentu projektu.
 */
data class Review(
    val id: String,
    val placeId: String,
    val userId: String,
    val authorName: String,
    val rating: Int, // 1..5
    val comment: String,
    val photoUrls: List<String> = emptyList(),
    val createdAtMillis: Long = 0L
)
