package com.playground.utils

/**
 * Typowane bledy uwierzytelniania - warstwa data tlumaczy na nie wyjatki
 * Firebase, dzieki czemu warstwa presentation moze je dyskryminowac bez
 * siegania do typow Firebase.
 */
sealed class AuthException(message: String) : Exception(message) {

    /** Konto z podanym e-mailem nie istnieje. */
    data object UserNotFound : AuthException("Konto nie istnieje")

    /** Nieprawidlowy e-mail lub haslo. */
    data object InvalidCredentials : AuthException("Nieprawidlowy e-mail lub haslo")

    /** Konto z tym e-mailem juz istnieje (rejestracja). */
    data object EmailAlreadyInUse : AuthException("Ten e-mail jest juz zajety")

    /** Haslo nie spelnia wymagan Firebase (zwykle min. 6 znakow). */
    data object WeakPassword : AuthException("Haslo jest za slabe (min. 6 znakow)")

    /** Niepoprawny format adresu e-mail. */
    data object InvalidEmail : AuthException("Niepoprawny format adresu e-mail")

    /** Brak Internetu, timeout, blad po stronie Firebase. */
    data class Network(val cause: Throwable) :
        AuthException(cause.message ?: "Blad polaczenia z serwerem")
}
