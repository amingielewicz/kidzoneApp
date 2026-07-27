package com.kidzone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kidzone.domain.model.User

/**
 * ⚙️ Techniczne:
 * Reprezentacja użytkownika w lokalnym cache'u Room (np. dla potrzeb rankingu i profilu offline).
 *
 * @property id identyfikator użytkownika zgodny z Firebase Auth UID.
 * @property name publiczna nazwa wyświetlana.
 * @property email prywatny adres e-mail.
 * @property firstName opcjonalne imię.
 * @property lastName opcjonalne nazwisko.
 * @property avatarUrl URL aktualnego avatara albo `null`.
 * @property placesAddedCount liczba dodanych miejsc.
 * @property reviewsCount liczba opinii.
 * @property createdAtMillis czas utworzenia konta.
 * @property nameLowercase znormalizowana nazwa do wyszukiwania.
 * @property badgeEarnedAtJson mapa odznak w formacie JSON (URL -> timestamp).
 * @property bannedUntilMillis czas końca blokady.
 * @property banReason administracyjny powód blokady.
 * @property emailNotificationsEnabled prywatna zgoda na powiadomienia e-mail.
 * @property cachedAtMillis czas zapisu rekordu w lokalnej bazie.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?,
    val placesAddedCount: Int,
    val reviewsCount: Int,
    val createdAtMillis: Long,
    val nameLowercase: String,
    val badgeEarnedAtJson: String, // JSON map
    val bannedUntilMillis: Long,
    val banReason: String,
    val emailNotificationsEnabled: Boolean,
    val cachedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain(): User {
        // Simple JSON parsing placeholder - in real app use Gson/Moshi
        val badges = mutableMapOf<String, Long>()
        badgeEarnedAtJson.removeSurrounding("{", "}")
            .split(",")
            .filter { it.contains(":") }
            .forEach {
                val parts = it.split(":")
                val key = parts[0].trim().removeSurrounding("\"")
                val value = parts[1].trim().toLongOrNull() ?: 0L
                badges[key] = value
            }

        return User(
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
            badgeEarnedAt = badges,
            bannedUntilMillis = bannedUntilMillis,
            banReason = banReason,
            emailNotificationsEnabled = emailNotificationsEnabled
        )
    }

    companion object {
        fun fromDomain(user: User): UserEntity {
            val badgesJson = user.badgeEarnedAt.entries.joinToString(
                prefix = "{",
                postfix = "}",
                separator = ","
            ) { "\"${it.key}\":${it.value}" }

            return UserEntity(
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
                badgeEarnedAtJson = badgesJson,
                bannedUntilMillis = user.bannedUntilMillis,
                banReason = user.banReason,
                emailNotificationsEnabled = user.emailNotificationsEnabled
            )
        }
    }
}
