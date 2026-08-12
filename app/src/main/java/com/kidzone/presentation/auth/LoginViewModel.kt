package com.kidzone.presentation.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.R
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.service.DataPrefetchService
import com.kidzone.utils.AuthException
import com.kidzone.utils.OpResult
import com.kidzone.utils.UiText
import com.kidzone.utils.toAuthErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val VERIFICATION_RESEND_COOLDOWN_SECONDS = 60
private const val ONE_SECOND_DELAY_MS = 1000L
internal const val KEY_REGISTRATION_SUCCESS = "registration_success"

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie formularzem logowania, resetem hasła i ponowną wysyłką weryfikacji.
 * - Mapowanie błędów autoryzacji na bezpieczne komunikaty UI.
 * - Rozróżnianie stanów konta (niezweryfikowane, zablokowane).
 *
 * 🚫 Poza zakresem:
 * - Brak bezpośredniego zarządzania sesją (delegowane do [AuthRepository]).
 * - Brak retry logiki dla operacji sieciowych.
 * - Brak przechowywania haseł w pamięci trwałej.
 *
 * 📥 Wejście:
 * - Interakcje użytkownika z polami formularza (email, hasło).
 * - Żądania logowania (E-mail/Password, Google).
 *
 * 📤 Wyjście:
 * - Stan ekranu logowania ([UiState]).
 * - Flaga sukcesu zalogowania ([isSignedIn]).
 *
 * ✅ Gwarancje:
 * - Brak logowania wrażliwych danych (hasła, tokeny).
 * - Bezpieczne mapowanie technicznych błędów Firebase na zrozumiały język.
 *
 * 🔌 Offline:
 * - Nie wspiera operacji w trybie offline (wymagana łączność z serwerami Auth).
 *
 * 🧵 Wątki:
 * - viewModelScope dla wszystkich operacji autoryzacji.
 * - Brak blokujących operacji na wątku Main.
 *
 * 🧪 Testowalność:
 * - Pełne DI (mockowanie repozytorium autoryzacji).
 * - Deterministyczne zmiany stanu w odpowiedzi na błędy i sukcesy.
 *
 * 🧼 Lifecycle:
 * - Operacje wiązane z viewModelScope (anulowane automatycznie).
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefetchService: DataPrefetchService,
    private val savedStateHandle: SavedStateHandle
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
        val resendCooldownSeconds: Int = 0,
        val banMessage: UiText? = null,
        val banReason: UiText? = null
    ) {
        /** Czy oba wymagane pola formularza są niepuste. */
        val isFormValid: Boolean
            get() = email.isNotBlank() && password.isNotBlank()
    }

    private val _uiState = MutableStateFlow(UiState())

    /** Stan obserwowany przez ekran Compose. */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Sprawdź czy wróciliśmy z ekranu rejestracji z sukcesem
        if (savedStateHandle.get<Boolean>(KEY_REGISTRATION_SUCCESS) == true) {
            _uiState.update {
                it.copy(
                    message = UiText.StringResource(R.string.register_success_verify_email),
                    isMessageError = false
                )
            }
            savedStateHandle.remove<Boolean>(KEY_REGISTRATION_SUCCESS)
        }
    }

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
            if (result is OpResult.Success) {
                prefetchService.startPrefetch()
            }
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(isLoading = false, isSignedIn = true)
                    is OpResult.Failure -> {
                        val isBanned = result.error is AuthException.AccountBanned
                        val isEmailNotVerified = result.error is AuthException.EmailNotVerified
                        it.copy(
                            isLoading = false,
                            message = if (isBanned) null else mapError(result.error),
                            isMessageError = true,
                            showResendVerification = isEmailNotVerified,
                            banMessage = if (isBanned) {
                                mapError(result.error)
                            } else {
                                null
                            },
                            banReason = if (isBanned) {
                                val banError = result.error as AuthException.AccountBanned
                                UiText.StringResource(
                                    R.string.ban_reason_prefix,
                                    UiText.StringResource(banError.banReasonRes)
                                )
                            } else {
                                null
                            }
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
            if (result is OpResult.Success) {
                prefetchService.startPrefetch()
            }
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(isLoading = false, isSignedIn = true)
                    is OpResult.Failure -> {
                        val isBanned = result.error is AuthException.AccountBanned
                        it.copy(
                            isLoading = false,
                            message = if (isBanned) null else mapError(result.error),
                            isMessageError = true,
                            banMessage = if (isBanned) {
                                mapError(result.error)
                            } else {
                                null
                            },
                            banReason = if (isBanned) {
                                val banError = result.error as AuthException.AccountBanned
                                UiText.StringResource(
                                    R.string.ban_reason_prefix,
                                    UiText.StringResource(banError.banReasonRes)
                                )
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }

    /** Pokazuje błąd przekazany jako UiText. */
    fun showErrorMessage(message: UiText) {
        _uiState.update { it.copy(message = message, isMessageError = true) }
    }

    /** Pokazuje informację przekazaną jako UiText. */
    fun showInfoMessage(message: UiText) {
        _uiState.update { it.copy(message = message, isMessageError = false) }
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
                        message = result.error.toAuthErrorMessage(R.string.error_unknown),
                        isMessageError = true
                    )
                }
            }
        }
    }

    @Suppress("SpreadOperator")
    private fun mapError(throwable: Throwable): UiText = when (throwable) {
        is AuthException.Network -> UiText.StringResource(R.string.error_no_internet)
        is AuthException -> UiText.StringResource(throwable.messageRes, *throwable.args)
        else -> UiText.StringResource(R.string.error_unknown)
    }

    /**
     * Ponownie wysyła wiadomość weryfikacyjną dla danych wpisanych w formularzu.
     *
     * Metoda wymaga e-maila i hasła, ponieważ repository może wykonać reautoryzację przed wysyłką.
     */
    fun resendVerificationEmail() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank() || state.resendCooldownSeconds > 0) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.resendVerificationEmail(state.email.trim(), state.password)
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(
                        isLoading = false,
                        message = UiText.StringResource(R.string.verification_email_sent),
                        isMessageError = false,
                        showResendVerification = true, // Keep showing but with cooldown
                        resendCooldownSeconds = VERIFICATION_RESEND_COOLDOWN_SECONDS
                    )
                    is OpResult.Failure -> it.copy(
                        isLoading = false,
                        message = UiText.StringResource(R.string.verification_email_error),
                        isMessageError = true
                    )
                }
            }
            
            // Start countdown
            while (_uiState.value.resendCooldownSeconds > 0) {
                delay(ONE_SECOND_DELAY_MS)
                _uiState.update { it.copy(resendCooldownSeconds = it.resendCooldownSeconds - 1) }
            }
        }
    }
}
