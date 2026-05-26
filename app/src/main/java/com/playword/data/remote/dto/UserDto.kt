package com.playword.data.remote.dto

import com.playword.domain.model.User

/**
 * Reprezentacja [User] zapisywana w kolekcji `users` w Firestore.
 *
 * Pusty konstruktor jest wymagany przez Firestore do deserializacji.
 */
data class UserDto(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val placesAddedCount: Int = 0,
    val reviewsCount: Int = 0,
    val createdAtMillis: Long = 0L
) {
    fun toDomain(): User = User(
        id = id,
        name = name,
        email = email,
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
            avatarUrl = user.avatarUrl,
            placesAddedCount = user.placesAddedCount,
            reviewsCount = user.reviewsCount,
            createdAtMillis = user.createdAtMillis
        )
    }
}
