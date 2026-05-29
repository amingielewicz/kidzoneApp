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
    data object EmailAlreadyInUse : AuthException("Ten e-mail jest już zajęty")

    /**
     * Hasło nie spełnia wymagań siły. Komunikat zsynchronizowany z lokalną
     * polityką (zob. [PasswordPolicy]) - Firebase server-side wymaga tylko
     * 6 znaków, ale my egzekwujemy mocniejszą politykę przed wysłaniem.
     */
    data object WeakPassword : AuthException(PasswordPolicy.DEFAULT_ERROR_MESSAGE)

    /** Niepoprawny format adresu e-mail. */
    data object InvalidEmail : AuthException("Niepoprawny format adresu e-mail")

    /** Brak Internetu, timeout, błąd po stronie Firebase. */
    data class Network(val networkCause: Throwable) :
        AuthException(networkCause.message ?: "Błąd połączenia z serwerem")
}
