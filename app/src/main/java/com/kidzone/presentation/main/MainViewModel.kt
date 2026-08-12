package com.kidzone.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.utils.OpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie globalnym stanem powłoki aplikacji (MainScreen).
 * - Weryfikacja wymogu akceptacji Regulaminu (TOS) po zalogowaniu.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    data class UiState(
        val showTosDialog: Boolean = false,
        val isAcceptingTos: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser
                .flatMapLatest { user ->
                    if (user != null) {
                        authRepository.observeUser(user.id)
                    } else {
                        flowOf(null)
                    }
                }
                .collect { fullUser ->
                    // Jeśli użytkownik właśnie kliknął "Akceptuję", ignorujemy stan z DB
                    // dopóki nie zostanie on trwale zapisany.
                    if (_uiState.value.isAcceptingTos) return@collect

                    val needsTos = fullUser != null && fullUser.tosAcceptedAtMillis == 0L
                    _uiState.update { it.copy(showTosDialog = needsTos) }
                }
        }
    }

    fun acceptTos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAcceptingTos = true, showTosDialog = false) }
            val result = authRepository.acceptTos()
            if (result is OpResult.Failure) {
                // Przy błędzie przywracamy dialog
                _uiState.update { it.copy(showTosDialog = true, isAcceptingTos = false) }
            } else {
                _uiState.update { it.copy(isAcceptingTos = false) }
            }
        }
    }
}
