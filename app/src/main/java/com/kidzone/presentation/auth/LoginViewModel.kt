package com.kidzone.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.R
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.utils.AuthException
import com.kidzone.utils.OpResult
import com.kidzone.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Zarządza formularzem logowania, resetem hasła i ponowną wysyłką weryfikacji e-mail.
 *
 * ViewModel obsługuje logowanie e-mail/hasło i Google, mapuje błędy domenowe na bezpieczne
 * komunikaty UI oraz rozróżnia brak weryfikacji e-mail od blokady konta. Nie przechowuje haseł poza
 * bieżącym stanem formularza i nie loguje tokenów ani danych uwierzytelniających.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Niezmienny stan ekranu logowania.
     *
     * @property email bieżąca wartość pola e-mail.
     * @property password bieżąca wartość pola hasła.
     * @property isLoading czy trwa operacja uwierzytelniania lub wysyłki wiadomości.
     * @property message komunikat przeznaczony do jednorazowego pokazania w UI.
     * @property isMessageError czy [message] reprezentuje błąd.
     * @property isSignedIn czy ostatnia próba logowania zakończyła się sukcesem.
     * @property showResendVerification czy należy pokazać akcję ponownej wysyłki weryfikacji.
     * @property banMessage komunikat o blokadzie zwrócony przez warstwę domenową.
     * @property banReason opcjonalny powód blokady.
     */
    data class UiState(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val message: UiText? = null,
        val isMessageError: Boolean = true,
        val isSignedIn: Boolean = false,
        val showResendVerification: Boolean = false,
        val banMessage: String? = null,
        val banReason: String? = null
    ) {
        /** Czy oba wymagane pola formularza są niepuste. */
        val isFormValid: Boolean
            get() = email.isNotBlank() && password.isNotBlank()
    }

    private val _uiState = MutableStateFlow(UiState())

    /** Stan obserwowany przez ekran Compose. */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Aktualizuje e-mail i czyści poprzedni komunikat formularza. */
    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, message = null) }
    }

    /** Aktualizuje hasło i czyści poprzedni komunikat formularza. */
    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, message = null) }
    }

    /**
     * Próbuje zalogować użytkownika danymi e-mail/hasło.
     *
     * Puste pola kończą się lokalnym błędem walidacji. Sukces ustawia [UiState.isSignedIn], a błąd
     * jest mapowany bez ujawniania surowego komunikatu Firebase.
     */
    fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update {
                it.copy(
                    message = UiText.StringResource(R.string.login_validation_empty),
                    isMessageError = true
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val result = authRepository.signInWithEmail(state.email.trim(), state.password)
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(isLoading = false, isSignedIn = true)
                    is OpResult.Failure -> {
                        val isBanned = result.error is AuthException.AccountBanned
                        val isEmailNotVerified = result.error is AuthException.EmailNotVerified
                        it.copy(
                            isLoading = false,
                            message = mapError(result.error),
                            isMessageError = true,
                            showResendVerification = isEmailNotVerified,
                            banMessage = if (isBanned) (result.error as AuthException.AccountBanned).banMessage else null,
                            banReason = if (isBanned) (result.error as AuthException.AccountBanned).banReason else null
                        )
                    }
                }
            }
        }
    }

    /**
     * Loguje użytkownika poświadczeniem Google.
     *
     * @param idToken krótkotrwały token ID uzyskany przez Credential Manager; nie może być logowany.
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, banMessage = null) }
            val result = authRepository.signInWithGoogle(idToken)
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(isLoading = false, isSignedIn = true)
                    is OpResult.Failure -> {
                        val isBanned = result.error is AuthException.AccountBanned
                        it.copy(
                            isLoading = false,
                            message = mapError(result.error),
                            isMessageError = true,
                            banMessage = if (isBanned) (result.error as AuthException.AccountBanned).banMessage else null,
                            banReason = if (isBanned) (result.error as AuthException.AccountBanned).banReason else null
                        )
                    }
                }
            }
        }
    }

    /** Pokazuje komunikat przekazany przez warstwę UI lub integrację zewnętrzną. */
    fun showInlineMessage(text: String, isError: Boolean = true) {
        _uiState.update { it.copy(message = UiText.DynamicString(text), isMessageError = isError) }
    }

    /** Pokazuje standardowy komunikat braku połączenia. */
    fun showConnectionError() {
        _uiState.update {
            it.copy(
                message = UiText.StringResource(R.string.error_no_internet),
                isMessageError = true
            )
        }
    }

    /** Oznacza aktualny komunikat jako obsłużony. */
    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * Wysyła wiadomość resetującą hasło na adres wpisany w formularzu.
     *
     * Brak adresu kończy się lokalnym komunikatem. Szczegóły o istnieniu konta nie powinny być
     * ujawniane użytkownikowi.
     */
    fun forgotPassword() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update {
                it.copy(
                    message = UiText.StringResource(R.string.forgot_password_hint),
                    isMessageError = true
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val result = authRepository.sendPasswordResetEmail(email)
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(
                        isLoading = false,
                        message = UiText.StringResource(R.string.password_reset_sent, email),
                        isMessageError = false
                    )
                    is OpResult.Failure -> it.copy(
                        isLoading = false,
                        message = mapError(result.error),
                        isMessageError = true
                    )
                }
            }
        }
    }

    private fun mapError(throwable: Throwable): UiText = when (throwable) {
        is AuthException.AccountBanned -> UiText.DynamicString(throwable.banMessage)
        is AuthException.Network -> UiText.StringResource(R.string.error_no_internet)
        is AuthException -> UiText.StringResource(throwable.messageRes)
        else -> UiText.StringResource(R.string.error_unknown)
    }

    /**
     * Ponownie wysyła wiadomość weryfikacyjną dla danych wpisanych w formularzu.
     *
     * Metoda wymaga e-maila i hasła, ponieważ repository może wykonać reautoryzację przed wysyłką.
     */
    fun resendVerificationEmail() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.resendVerificationEmail(state.email.trim(), state.password)
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(
                        isLoading = false,
                        message = UiText.StringResource(R.string.verification_email_sent),
                        isMessageError = false,
                        showResendVerification = false
                    )
                    is OpResult.Failure -> it.copy(
                        isLoading = false,
                        message = UiText.StringResource(R.string.verification_email_error),
                        isMessageError = true
                    )
                }
            }
        }
    }
}
