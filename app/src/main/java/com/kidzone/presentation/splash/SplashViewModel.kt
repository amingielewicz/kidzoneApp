package com.kidzone.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decyduje na podstawie [AuthRepository.currentUser] gdzie wysłać
 * użytkownika po splash screenie.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    enum class State { Loading, SignedIn, SignedOut }

    private val _state = MutableStateFlow(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.currentUser.first()
            _state.value = if (user != null) State.SignedIn else State.SignedOut
        }
    }
}
