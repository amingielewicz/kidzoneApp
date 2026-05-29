package com.kidzone.data.remote.dto

import com.kidzone.domain.model.User

/**
 * Reprezentacja [User] zapisywana w kolekcji `users` w Firestore.
 *
 * Pusty konstruktor jest wymagany przez Firestore do deserializacji.
 *
 * Wszystkie pola mają domyślne wartości – dzięki temu dokumenty zapisane
 * w starszej wersji schematu (np. bez `firstName`/`lastName`) nadal się
 * poprawnie deserializują, a brakujące pola po prostu mają wartość pustą.
 */
data class UserDto(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val avatarUrl: String? = null,
    val placesAddedCount: Int = 0,
    val reviewsCount: Int = 0,
    val createdAtMillis: Long = 0L
) {
    fun toDomain(): User = User(
        id = id,
        name = name,
        email = email,
        firstName = firstName,
        lastName = lastName,
        avatarUrl = avatarUrl,
        placesAddedCount = placesAddedCount,
        reviewsCount = reviewsCount,
        createdAtMillis = createdAtMillis
    )

    companion object {
        fun fromDomain(user: User): UserDto = UserDto(
            id = user.id,
            name = user.name,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            avatarUrl = user.avatarUrl,
            placesAddedCount = user.placesAddedCount,
            reviewsCount = user.reviewsCount,
            createdAtMillis = user.createdAtMillis
        )
    }
}
