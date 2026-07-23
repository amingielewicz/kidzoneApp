package com.kidzone.data.remote.dto

/**
 * ⚙️ Techniczne:
 * Prywatna część profilu użytkownika (`users/{uid}/private/profile`).
 *
 * 🛡️ Bezpieczeństwo:
 * Dane wrażliwe (PII) oddzielone od profilu publicznego. Dostępne wyłącznie dla właściciela i adminów.
 */
data class UserPrivateDto(
    val userId: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val emailNotificationsEnabled: Boolean = true,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)
