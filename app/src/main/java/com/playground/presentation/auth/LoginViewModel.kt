package com.playground.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playground.domain.repository.AuthRepository
import com.playground.utils.AuthException
import com.playground.utils.OpResult
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
 * Trzyma stan formularza ([UiState]), dyspozycjonuje akcje uzytkownika
 * (zaloguj, reset hasla) i tlumaczy bledy z [AuthException] na czytelne
 * komunikaty po polsku.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Stan UI logowania.
     *
     * @property email aktualna wartosc pola e-mail
     * @property password aktualna wartosc pola haslo
     * @property isLoading czy trwa request do Firebase
     * @property message komunikat do pokazania uzytkownikowi (blad lub info)
     * @property isMessageError true gdy [message] jest bledem (kolor czerwony),
     *           false gdy informacja (np. "wyslano link resetujacy")
     * @property isSignedIn true po pomyslnym logowaniu - sygnal dla UI by
     *           wykonac nawigacje na main
     */
    data class UiState(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val message: String? = null,
        val isMessageError: Boolean = true,
        val isSignedIn: Boolean = false
    )

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
            _uiState.update { it.copy(message = "Wypelnij e-mail i haslo", isMessageError = true) }
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

    fun forgotPassword() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update {
                it.copy(
                    message = "Wpisz e-mail w polu wyzej, zeby zresetowac haslo",
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
                        message = "Wyslalismy link do zresetowania hasla na $email",
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
        is AuthException -> throwable.message ?: "Nieznany blad"
        else -> throwable.message ?: "Nieznany blad"
    }
}
