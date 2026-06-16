package com.kidzone.data.remote.dto

/**
 * Prywatna część profilu użytkownika przechowywana pod:
 * users/{uid}/private/profile
 *
 * Ten dokument powinien być dostępny wyłącznie dla właściciela konta i admina.
 * Nie należy używać go na ekranach publicznych, takich jak ranking, lista autorów
 * czy publiczny profil użytkownika.
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
