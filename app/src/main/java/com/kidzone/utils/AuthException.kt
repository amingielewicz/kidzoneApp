package com.kidzone.utils

/**
 * Typowane błędy uwierzytelniania – warstwa data tłumaczy na nie wyjątki
 * Firebase, dzięki czemu warstwa presentation może je dyskryminować bez
 * sięgania do typów Firebase.
 */
sealed class AuthException(message: String) : Exception(message) {

    /** Konto z podanym e-mailem nie istnieje. */
    data object UserNotFound : AuthException("Konto nie istnieje")

    /** Nieprawidłowy e-mail lub hasło. */
    data object InvalidCredentials : AuthException("Nieprawidłowy e-mail lub hasło")

    /** Konto z tym e-mailem już istnieje (rejestracja). */
    data object EmailAlreadyInUse : AuthException("Konto z tym adresem e-mail już istnieje")

    /**
     * Wybrany login (publiczny nick) jest już używany przez innego użytkownika.
     * Ekran rejestracji / edycji profilu pyta usera o inną nazwę.
     *
     * Niezależny od [EmailAlreadyInUse] - email kontroluje Firebase Auth,
     * login kontrolujemy my po stronie Firestore (kolekcja `users`).
     */
    data object UsernameAlreadyTaken :
        AuthException("Ta nazwa użytkownika jest już zajęta. Wybierz inną.")

    /**
     * Hasło nie spełnia wymagań siły. Komunikat zsynchronizowany z lokalną
     * polityką (zob. [PasswordPolicy]) - Firebase server-side wymaga tylko
     * 6 znaków, ale my egzekwujemy mocniejszą politykę przed wysłaniem.
     */
    data object WeakPassword : AuthException(PasswordPolicy.DEFAULT_ERROR_MESSAGE)

    /** Niepoprawny format adresu e-mail. */
    data object InvalidEmail : AuthException("Niepoprawny format adresu e-mail")

    /** Adres e-mail nie został potwierdzony (link weryfikacyjny nie kliknięty). */
    data object EmailNotVerified :
        AuthException("Potwierdź swój adres e-mail. Sprawdź skrzynkę pocztową.")

    /** Brak Internetu, timeout, błąd po stronie Firebase. */
    data class Network(val networkCause: Throwable) :
        AuthException(networkCause.message ?: "Błąd połączenia z serwerem")

    /** Konto użytkownika zostało zablokowane przez administratora. */
    data class AccountBanned(val banMessage: String, val banReason: String) :
        AuthException(banMessage)
}
