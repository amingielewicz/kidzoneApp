package com.kidzone.data.remote.dto

import com.kidzone.domain.model.User

/**
 * ⚙️ Techniczne:
 * Reprezentacja [User] zapisywana w kolekcji `users` w Firestore.
 *
 * @property nameLowercase Wersja małych liter [name] dla zapytań case-insensitive.
 * @property badgeEarnedAt Mapa nazw odznak na czas ich zdobycia (timestamp).
 * @property emailNotificationsEnabled Flaga statusu powiadomień e-mail (legacy fallback).
 */
data class UserDto(
    val id: String = "",
    val name: String = "",
    val role: String = "user",
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
    fun toDomain(
        privateProfile: UserPrivateDto? = null,
        includeLegacyPrivateFallback: Boolean = privateProfile == null
    ): User = User(
        id = id,
        name = name,
        email = privateProfile?.email ?: email.takeIf { includeLegacyPrivateFallback }.orEmpty(),
        firstName = privateProfile?.firstName ?: firstName.takeIf { includeLegacyPrivateFallback }.orEmpty(),
        lastName = privateProfile?.lastName ?: lastName.takeIf { includeLegacyPrivateFallback }.orEmpty(),
        avatarUrl = avatarUrl,
        placesAddedCount = placesAddedCount,
        reviewsCount = reviewsCount,
        createdAtMillis = createdAtMillis,
        nameLowercase = nameLowercase,
        badgeEarnedAt = badgeEarnedAt,
        bannedUntilMillis = bannedUntilMillis,
        banReason = banReason,
        emailNotificationsEnabled = privateProfile?.emailNotificationsEnabled
            ?: emailNotificationsEnabled.takeIf { includeLegacyPrivateFallback }
            ?: true
    )

    fun toPublicDomain(): User = toDomain(includeLegacyPrivateFallback = false)

    fun toPublicFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "role" to role,
        "avatarUrl" to avatarUrl,
        "placesAddedCount" to placesAddedCount,
        "reviewsCount" to reviewsCount,
        "createdAtMillis" to createdAtMillis,
        "nameLowercase" to nameLowercase,
        "badgeEarnedAt" to badgeEarnedAt,
        "bannedUntilMillis" to bannedUntilMillis,
        "banReason" to banReason
    )

    companion object {
        fun fromDomain(user: User): UserDto = UserDto(
            id = user.id,
            name = user.name,
            avatarUrl = user.avatarUrl,
            placesAddedCount = user.placesAddedCount,
            reviewsCount = user.reviewsCount,
            createdAtMillis = user.createdAtMillis,
            nameLowercase = user.nameLowercase,
            badgeEarnedAt = user.badgeEarnedAt,
            bannedUntilMillis = user.bannedUntilMillis,
            banReason = user.banReason
        )
    }
}
