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
    val createdAtMillis: Long = 0L,
    /**
     * Lowercase wersja [name] dla case-insensitive zapytań w Firestore
     * (whereEqualTo("nameLowercase", ...)). Default "" zachowuje wsteczną
     * kompatybilność z dokumentami z legacy schema; FirebaseAuthRepository
     * uzupełnia pole przy najbliższej operacji write.
     */
    val nameLowercase: String = "",
    /**
     * Mapa odznak (UserBadge.name) -> timestamp zdobycia (millis).
     * Wypełniana przez ProfileViewModel via AuthRepository.recordBadgesEarned
     * w momencie pierwszej detekcji nowej odznaki na danym urządzeniu.
     *
     * Default emptyMap() = legacy / brak danych; sortowanie chronologiczne
     * w UI traktuje brak wpisu jako "nieznana data" i sortuje takie
     * odznaki na koniec.
     */
    val badgeEarnedAt: Map<String, Long> = emptyMap(),
    val bannedUntilMillis: Long = 0L,
    val banReason: String = "",
    val emailNotificationsEnabled: Boolean = true
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
        createdAtMillis = createdAtMillis,
        nameLowercase = nameLowercase,
        badgeEarnedAt = badgeEarnedAt,
        bannedUntilMillis = bannedUntilMillis,
        banReason = banReason,
        emailNotificationsEnabled = emailNotificationsEnabled
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
            createdAtMillis = user.createdAtMillis,
            nameLowercase = user.nameLowercase,
            badgeEarnedAt = user.badgeEarnedAt,
            bannedUntilMillis = user.bannedUntilMillis,
            banReason = user.banReason,
            emailNotificationsEnabled = user.emailNotificationsEnabled
        )
    }
}
