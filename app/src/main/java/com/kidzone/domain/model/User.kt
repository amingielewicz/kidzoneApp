package com.kidzone.domain.model

/**
 * Użytkownik aplikacji kidZone.
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val firstName: String = "",
    val lastName: String = "",
    val avatarUrl: String? = null,
    val placesAddedCount: Int = 0,
    val reviewsCount: Int = 0,
    val createdAtMillis: Long = 0L,
    val nameLowercase: String = "",
    val badgeEarnedAt: Map<String, Long> = emptyMap(),
    val bannedUntilMillis: Long = 0L,
    val banReason: String = "",
    val emailNotificationsEnabled: Boolean = true
) {
    /** Czy konto jest aktualnie zablokowane. */
    val isBanned: Boolean
        get() = bannedUntilMillis == -1L || (bannedUntilMillis > 0L && bannedUntilMillis > System.currentTimeMillis())
}
