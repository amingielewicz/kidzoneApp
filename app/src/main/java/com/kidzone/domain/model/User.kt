package com.kidzone.domain.model

/**
 * Domenowy model użytkownika kidZone.
 *
 * Model łączy publiczne dane profilu, liczniki aktywności oraz pola potrzebne do zarządzania stanem
 * konta. Nie wszystkie właściwości powinny być przechowywane w publicznym dokumencie Firestore;
 * prywatne dane muszą być mapowane zgodnie z aktualnym schematem bezpieczeństwa.
 *
 * @property id identyfikator użytkownika zgodny z Firebase Auth UID.
 * @property name publiczna nazwa wyświetlana w aplikacji.
 * @property email adres e-mail; pole prywatne, nieprzeznaczone do publicznego profilu.
 * @property firstName opcjonalne imię.
 * @property lastName opcjonalne nazwisko.
 * @property avatarUrl URL aktualnego avatara albo `null`.
 * @property placesAddedCount zmaterializowana liczba dodanych miejsc.
 * @property reviewsCount zmaterializowana liczba opinii.
 * @property createdAtMillis czas utworzenia konta.
 * @property nameLowercase znormalizowana nazwa używana do wyszukiwania i sortowania.
 * @property badgeEarnedAt mapa nazw odznak do czasu ich pierwszego zdobycia.
 * @property bannedUntilMillis czas końca blokady, `-1` dla blokady bezterminowej, `0` dla braku blokady.
 * @property banReason administracyjny powód blokady przeznaczony do kontrolowanego komunikatu UI.
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
