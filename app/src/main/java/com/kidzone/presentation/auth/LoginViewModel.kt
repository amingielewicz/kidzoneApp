package com.kidzone.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.utils.AuthException
import com.kidzone.utils.OpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel ekranu logowania.
 *
 * Trzyma stan formularza ([UiState]), dyspozycjonuje akcje użytkownika
 * (zaloguj, reset hasła) i tłumaczy błędy z [AuthException] na czytelne
 * komunikaty po polsku.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Stan UI logowania.
     *
     * @property email aktualna wartość pola e-mail
     * @property password aktualna wartość pola hasło
     * @property isLoading czy trwa request do Firebase
     * @property message komunikat do pokazania użytkownikowi (błąd lub info)
     * @property isMessageError true gdy [message] jest błędem (kolor czerwony),
     *           false gdy informacja (np. "wysłano link resetujący")
     * @property isSignedIn true po pomyślnym logowaniu – sygnał dla UI by
     *           wykonać nawigację na main
     */
    data class UiState(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val message: String? = null,
        val isMessageError: Boolean = true,
        val isSignedIn: Boolean = false
    ) {
        /** Oba pola wypełnione – tylko wtedy można kliknąć "Zaloguj się". */
        val isFormValid: Boolean
            get() = email.isNotBlank() && password.isNotBlank()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, message = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, message = null) }
    }

    fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(message = "Wypełnij e-mail i hasło", isMessageError = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val result = authRepository.signInWithEmail(state.email.trim(), state.password)
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(isLoading = false, isSignedIn = true)
                    is OpResult.Failure -> it.copy(
                        isLoading = false,
                        message = mapError(result.error),
                        isMessageError = true
                    )
                }
            }
        }
    }

    /**
     * Wywoływane z UI po uzyskaniu idToken z Google Sign-In (Credential Manager).
     * Przekazuje token do repo, który wymienia go na sesję FirebaseAuth.
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val result = authRepository.signInWithGoogle(idToken)
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(isLoading = false, isSignedIn = true)
                    is OpResult.Failure -> it.copy(
                        isLoading = false,
                        message = mapError(result.error),
                        isMessageError = true
                    )
                }
            }
        }
    }

    /** Pokazuje użytkownikowi błąd/info pochodzący z procesu Google Sign-In w UI. */
    fun showInlineMessage(text: String, isError: Boolean = true) {
        _uiState.update { it.copy(message = text, isMessageError = isError) }
    }

    fun forgotPassword() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update {
                it.copy(
                    message = "Wpisz e-mail w polu wyżej, żeby zresetować hasło",
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
                        message = "Wysłaliśmy link do zresetowania hasła na $email",
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

    private fun mapError(throwable: Throwable): String = when (throwable) {
        is AuthException -> throwable.message ?: "Nieznany błąd"
        else -> throwable.message ?: "Nieznany błąd"
    }
}
