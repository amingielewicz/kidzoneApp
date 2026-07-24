package com.kidzone.utils

import com.kidzone.R

/**
 * Typowane błędy uwierzytelniania.
 */
sealed class AuthException(
    val messageRes: Int,
    val args: Array<out Any> = emptyArray()
) : Exception() {

    /** Konto z podanym e-mailem nie istnieje. */
    data object UserNotFound : AuthException(R.string.error_user_not_found)

    /** Nieprawidłowy e-mail lub hasło. */
    data object InvalidCredentials : AuthException(R.string.error_invalid_credentials)

    /** Konto z tym e-mailem już istnieje (rejestracja). */
    data object EmailAlreadyInUse : AuthException(R.string.error_email_already_in_use)

    /**
     * Wybrany login jest już używany przez innego użytkownika.
     */
    data object UsernameAlreadyTaken : AuthException(R.string.error_username_taken)

    /**
     * Hasło nie spełnia wymagań siły.
     */
    data object WeakPassword : AuthException(R.string.error_weak_password)

    /** Niepoprawny format adresu e-mail. */
    data object InvalidEmail : AuthException(R.string.invalid_email_format)

    /** Adres e-mail nie został potwierdzony. */
    data object EmailNotVerified : AuthException(R.string.error_email_not_verified)

    /** Usuwanie konta przez Google nie jest bezpośrednio wspierane w tym kanale. */
    data object AccountDeletionUnsupported : AuthException(R.string.error_delete_account_google_unsupported)

    /** Brak Internetu, timeout, błąd po stronie Firebase. */
    data class Network(val networkCause: Throwable) : AuthException(R.string.error_network)

    /** Konto użytkownika zostało zablokowane przez administratora. */
    class AccountBanned(
        @androidx.annotation.StringRes resId: Int = R.string.error_account_banned,
        banArgs: Array<out Any> = emptyArray(),
        val banReasonRes: Int = R.string.ban_reason_other
    ) : AuthException(resId, banArgs)
}
