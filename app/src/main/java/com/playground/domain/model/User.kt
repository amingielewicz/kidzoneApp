package com.playground.domain.model

/**
 * Użytkownik aplikacji Playground.
 *
 * Odpowiada tabeli USERS z dokumentu projektu.
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val placesAddedCount: Int = 0,
    val reviewsCount: Int = 0,
    val createdAtMillis: Long = 0L
)
