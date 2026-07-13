package com.kidzone.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Minimalny czas wyświetlania ekranu startowego.
 *
 * Ogranicza krótkie mignięcie splasha przy ciepłym starcie i natychmiast dostępnej sesji Firebase.
 */
private const val MIN_DISPLAY_MS = 800L

/**
 * Maksymalny czas oczekiwania na pierwszy stan uwierzytelnienia.
 *
 * Po przekroczeniu limitu aplikacja przechodzi do stanu wylogowanego zamiast pozostawać na
 * nieskończonym ekranie ładowania.
 */
private const val AUTH_CHECK_TIMEOUT_MS = 5_000L

/**
 * Ustala docelowy graph nawigacji po uruchomieniu aplikacji.
 *
 * ViewModel czeka na pierwszy element z [AuthRepository.currentUser], respektuje minimalny czas
 * prezentacji splasha i stosuje timeout dla niedostępnego źródła sesji. Nie wykonuje nawigacji
 * bezpośrednio; warstwa UI obserwuje [state] i reaguje na zmianę.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Stan rozstrzygnięcia sesji na ekranie startowym.
     */
    enum class State {
        /** Trwa odczyt sesji lub minimalny czas prezentacji splasha. */
        Loading,

        /** Użytkownik posiada aktywną sesję. */
        SignedIn,

        /** Brak aktywnej sesji albo odczyt zakończył się timeoutem. */
        SignedOut
    }

    private val _state = MutableStateFlow(State.Loading)

    /**
     * Niezmienny strumień aktualnego stanu ekranu startowego.
     */
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val started = System.currentTimeMillis()
            val user = withTimeoutOrNull(AUTH_CHECK_TIMEOUT_MS) {
                authRepository.currentUser.first()
            }
            val elapsed = System.currentTimeMillis() - started
            val remaining = MIN_DISPLAY_MS - elapsed
            if (remaining > 0) delay(remaining)
            _state.value = if (user != null) State.SignedIn else State.SignedOut
        }
    }
}
