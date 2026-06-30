package com.kidzone.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.R
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.utils.AuthException
import com.kidzone.utils.OpResult
import com.kidzone.utils.PasswordPolicy
import com.kidzone.utils.UiText
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
        val errorMessage: UiText? = null,
        val successMessage: UiText? = null,
        val isRegistered: Boolean = false
    ) {
        val isNameValid: Boolean
            get() = name.trim().isNotBlank()

        val isEmailValid: Boolean
            get() = email.trim().let { trimmed ->
                trimmed.contains('@') &&
                    trimmed.substringAfter('@').contains('.') &&
                    trimmed.length >= 5
            }

        val isPasswordValid: Boolean
            get() = PasswordPolicy.isValid(password)

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
                _uiState.update { it.copy(errorMessage = UiText.StringResource(R.string.error_enter_username)) }
                return
            }
            email.isBlank() -> {
                _uiState.update { it.copy(errorMessage = UiText.StringResource(R.string.error_enter_email)) }
                return
            }
            password.isBlank() -> {
                _uiState.update { it.copy(errorMessage = UiText.StringResource(R.string.error_enter_password)) }
                return
            }
            !PasswordPolicy.isValid(password) -> {
                _uiState.update { it.copy(errorMessage = UiText.StringResource(R.string.error_weak_password)) }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.registerWithEmail(name, email, password)
            if (result is OpResult.Success) {
                authRepository.signOut()
            }
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(
                        isLoading = false,
                        isRegistered = true,
                        successMessage = UiText.StringResource(R.string.register_success_message)
                    )
                    is OpResult.Failure -> it.copy(
                        isLoading = false,
                        errorMessage = mapError(result.error)
                    )
                }
            }
        }
    }

    private fun mapError(throwable: Throwable): UiText = when (throwable) {
        is AuthException -> UiText.StringResource(throwable.messageRes)
        else -> UiText.StringResource(R.string.error_unknown)
    }
}
