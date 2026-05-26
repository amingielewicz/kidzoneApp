package com.playword.domain.model

/**
 * Zdjęcie powiązane z miejscem lub recenzją.
 *
 * Odpowiada tabeli PHOTOS z dokumentu projektu.
 */
data class Photo(
    val id: String,
    val placeId: String,
    val imageUrl: String,
    val uploadedByUserId: String? = null,
    val uploadedAtMillis: Long = 0L
)
