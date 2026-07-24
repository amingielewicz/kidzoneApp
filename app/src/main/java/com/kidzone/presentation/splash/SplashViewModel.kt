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
 * 🎯 Odpowiedzialności:
 * - Rozstrzyganie docelowego celu nawigacji po uruchomieniu aplikacji.
 * - Koordynacja czasu prezentacji ekranu powitalnego (Splash).
 * - Zarządzanie timeoutem przy braku odpowiedzi z systemu autoryzacji.
 *
 * 🚫 Poza zakresem:
 * - Brak decyzji o offline queue.
 * - Brak retry logiki dla autoryzacji.
 * - Brak bezpośredniej nawigacji (decyduje UI na podstawie stanu).
 *
 * 📥 Wejście:
 * - Strumień aktualnego użytkownika z [AuthRepository].
 *
 * 📤 Wyjście:
 * - Stan rozstrzygnięcia sesji ([State]).
 *
 * ✅ Gwarancje:
 * - Minimalny czas wyświetlania splasha ([MIN_DISPLAY_MS]), aby uniknąć mignięć UI.
 * - Przejście do stanu wylogowanego po przekroczeniu [AUTH_CHECK_TIMEOUT_MS].
 *
 * 🔌 Offline:
 * - Wspiera odczyt sesji z cache Firebase Auth.
 *
 * 🧵 Wątki:
 * - viewModelScope dla operacji asynchronicznych i opóźnień.
 * - Brak blokujących operacji na wątku Main.
 *
 * 🧪 Testowalność:
 * - Pełne DI.
 * - Deterministyczne rozstrzyganie celu nawigacji na podstawie mockowanych danych.
 *
 * 🧼 Lifecycle:
 * - Krótkotrwały cykl życia ograniczony do czasu startu aplikacji.
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
