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
 * ViewModel ekranu logowania.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Stan UI logowania.
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

    fun showInlineMessage(text: String, isError: Boolean = true) {
        _uiState.update { it.copy(message = UiText.DynamicString(text), isMessageError = isError) }
    }

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
        is AuthException -> UiText.StringResource(throwable.messageRes)
        else -> UiText.StringResource(R.string.error_unknown)
    }

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
