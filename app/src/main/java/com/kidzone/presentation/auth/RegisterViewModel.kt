package com.kidzone.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.R
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.utils.OpResult
import com.kidzone.utils.PasswordPolicy
import com.kidzone.utils.UiText
import com.kidzone.utils.toAuthErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie formularzem rejestracji nowego konta użytkownika.
 * - Lokalna walidacja danych (format e-mail, siła hasła, unikalność nazwy).
 * - Mapowanie błędów rejestracji na bezpieczne komunikaty UI.
 *
 * 🚫 Poza zakresem:
 * - Brak zarządzania sesją (sesja jest kończona zaraz po rejestracji).
 * - Brak retry logiki dla operacji sieciowych.
 * - Brak decyzji o kolejce offline.
 *
 * 📥 Wejście:
 * - Dane wejściowe od użytkownika (nazwa, email, hasło).
 * - Akcja zatwierdzenia formularza.
 *
 * 📤 Wyjście:
 * - Stan ekranu rejestracji ([UiState]).
 * - Status pomyślnej rejestracji ([UiState.isRegistered]).
 *
 * ✅ Gwarancje:
 * - Wymuszenie wylogowania po rejestracji (wymuszona weryfikacja e-mail).
 * - Brak logowania wrażliwych danych użytkownika.
 *
 * 🔌 Offline:
 * - Nie wspiera tworzenia konta w trybie offline.
 *
 * 🧵 Wątki:
 * - viewModelScope dla operacji I/O i sieciowych.
 * - Brak blokujących operacji na wątku Main.
 *
 * 🧪 Testowalność:
 * - Pełne DI.
 * - Deterministyczne mapowanie walidacji na stan błędu.
 *
 * 🧼 Lifecycle:
 * - Kończenie sesji tymczasowej po pomyślnej rejestracji.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Niezmienny stan ekranu rejestracji.
     *
     * @property name publiczna nazwa użytkownika.
     * @property email adres nowego konta.
     * @property password hasło przechowywane wyłącznie w bieżącym stanie formularza.
     * @property isLoading czy trwa tworzenie konta.
     * @property errorMessage komunikat błędu przeznaczony dla UI.
     * @property successMessage komunikat pokazywany po poprawnej rejestracji.
     * @property isRegistered czy proces utworzenia konta zakończył się sukcesem.
     */
    data class UiState(
        val name: String = "",
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val errorMessage: UiText? = null,
        val successMessage: UiText? = null,
        val isRegistered: Boolean = false
    ) {
        /** Czy nazwa użytkownika zawiera co najmniej jeden znak niebędący spacją. */
        val isNameValid: Boolean
            get() = name.trim().isNotBlank()

        /** Czy adres ma minimalną strukturę wymaganą przez formularz klienta. */
        val isEmailValid: Boolean
            get() = email.trim().let { trimmed ->
                trimmed.contains('@') &&
                    trimmed.substringAfter('@').contains('.') &&
                    trimmed.length >= 5
            }

        /** Czy hasło spełnia aktualną [PasswordPolicy]. */
        val isPasswordValid: Boolean
            get() = PasswordPolicy.isValid(password)

        /** Czy wszystkie pola formularza przechodzą walidację lokalną. */
        val isFormValid: Boolean
            get() = isNameValid && isEmailValid && isPasswordValid
    }

    private val _uiState = MutableStateFlow(UiState())

    /** Stan obserwowany przez ekran Compose. */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Aktualizuje nazwę użytkownika i czyści poprzedni błąd. */
    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    /** Aktualizuje adres e-mail i czyści poprzedni błąd. */
    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    /** Aktualizuje hasło i czyści poprzedni błąd. */
    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    /** Pokazuje standardowy komunikat problemu z połączeniem. */
    fun showConnectionError() {
        _uiState.update { it.copy(errorMessage = UiText.StringResource(R.string.error_network)) }
    }

    /** Oznacza bieżący komunikat błędu jako obsłużony. */
    fun consumeErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Waliduje formularz i rozpoczyna tworzenie konta.
     *
     * Sukces oznacza utworzenie wymaganych danych konta przez repository. Następnie wykonywane jest
     * wylogowanie, aby wymusić świadome przejście przez weryfikację e-mail. Błąd częściowy nie może
     * zostać przedstawiony jako pełna rejestracja.
     */
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
                        errorMessage = result.error.toAuthErrorMessage(R.string.error_unknown)
                    )
                }
            }
        }
    }

}
