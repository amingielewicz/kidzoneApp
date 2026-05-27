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
 * ViewModel ekranu rejestracji.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    data class UiState(
        val name: String = "",
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val isRegistered: Boolean = false
    ) {
        val isNameValid: Boolean
            get() = name.trim().isNotBlank()

        /** Prosta walidacja formatu – Firebase i tak zweryfikuje server-side. */
        val isEmailValid: Boolean
            get() = email.trim().let { trimmed ->
                trimmed.contains('@') &&
                    trimmed.substringAfter('@').contains('.') &&
                    trimmed.length >= 5
            }

        val isPasswordValid: Boolean
            get() = password.length >= 6

        /** Wszystkie pola spełniają warunki – można klikać "Zarejestruj się". */
        val isFormValid: Boolean
            get() = isNameValid && isEmailValid && isPasswordValid
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun register() {
        val state = _uiState.value
        val name = state.name.trim()
        val email = state.email.trim()
        val password = state.password

        when {
            name.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Podaj imię / nazwę użytkownika") }
                return
            }
            email.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Podaj e-mail") }
                return
            }
            password.length < 6 -> {
                _uiState.update { it.copy(errorMessage = "Hasło musi mieć min. 6 znaków") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.registerWithEmail(name, email, password)
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(isLoading = false, isRegistered = true)
                    is OpResult.Failure -> it.copy(
                        isLoading = false,
                        errorMessage = mapError(result.error)
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
