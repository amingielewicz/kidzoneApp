package com.kidzone.domain.model

/**
 * 📌 Przeznaczenie:
 * Domenowy model użytkownika kidZone.
 * Łączy publiczne dane profilu, statystyki aktywności oraz stan konta (blokady).
 *
 * @property id identyfikator użytkownika zgodny z Firebase Auth UID.
 * @property name publiczna nazwa wyświetlana w aplikacji.
 * @property email adres e-mail (pole prywatne).
 * @property firstName opcjonalne imię.
 * @property lastName opcjonalne nazwisko.
 * @property avatarUrl URL aktualnego avatara albo `null`.
 * @property placesAddedCount liczba dodanych miejsc (materialized counter).
 * @property reviewsCount liczba opinii (materialized counter).
 * @property createdAtMillis czas utworzenia konta.
 * @property nameLowercase znormalizowana nazwa do wyszukiwania i sortowania.
 * @property badgeEarnedAt mapa nazw odznak do czasu ich pierwszego zdobycia.
 * @property bannedUntilMillis czas końca blokady (-1 = permanentna, 0 = brak).
 * @property banReason administracyjny powód blokady.
 * @property tosAcceptedAtMillis czas akceptacji Regulaminu (UGC compliance).
 * @property emailNotificationsEnabled prywatna zgoda na powiadomienia e-mail.
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
    val tosAcceptedAtMillis: Long = 0L,
    val emailNotificationsEnabled: Boolean = true
) {
    /**
     * Czy konto jest aktualnie zablokowane.
     *
     * Wartość jest obliczana względem bieżącego czasu urządzenia i nie zastępuje autorytatywnej
     * kontroli po stronie backendu.
     */
    val isBanned: Boolean
        get() = bannedUntilMillis == -1L ||
            (bannedUntilMillis > 0L && bannedUntilMillis > System.currentTimeMillis())
}
