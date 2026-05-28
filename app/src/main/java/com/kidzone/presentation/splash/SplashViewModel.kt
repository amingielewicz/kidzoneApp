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
 * Minimalny czas wyświetlania splasha (w ms).
 *
 * Bez tego progu, na ciepłym starcie / z aktywną sesją Firebase, splash
 * "miga" dosłownie na 1 klatkę i nawigacja od razu skacze do Main – co
 * wygląda jak glitch. 800 ms to kompromis: na tyle krótko, że nie irytuje,
 * a na tyle długo, że logo jest widoczne.
 */
private const val MIN_DISPLAY_MS = 800L

/**
 * Maksymalny czas oczekiwania na pierwszy emit z [AuthRepository.currentUser].
 *
 * W praktyce Firebase Auth emituje natychmiast (ma cached state), ale gdyby
 * z jakiegoś powodu strumień się zawiesił (np. źle zainicjalizowany Firebase,
 * brak Google Play Services na emulatorze), nie chcemy zawieszać użytkownika
 * na splash-screenie – traktujemy to jako "niezalogowany" i wysyłamy do
 * loginu, gdzie zobaczy realny komunikat błędu.
 */
private const val AUTH_CHECK_TIMEOUT_MS = 5_000L

/**
 * Decyduje na podstawie [AuthRepository.currentUser] gdzie wysłać
 * użytkownika po splash screenie.
 *
 * Dba też o:
 *  - [MIN_DISPLAY_MS] – splash ma być widoczny wystarczająco długo,
 *  - [AUTH_CHECK_TIMEOUT_MS] – fallback gdyby Firebase się zawiesił.
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
            val started = System.currentTimeMillis()
            // withTimeoutOrNull zwróci null jeżeli timeout strzeli przed
            // pierwszym emitem; null traktujemy jak "niezalogowany".
            val user = withTimeoutOrNull(AUTH_CHECK_TIMEOUT_MS) {
                authRepository.currentUser.first()
            }
            // Doczekaj do MIN_DISPLAY_MS, żeby splash nie mignął.
            val elapsed = System.currentTimeMillis() - started
            val remaining = MIN_DISPLAY_MS - elapsed
            if (remaining > 0) delay(remaining)
            _state.value = if (user != null) State.SignedIn else State.SignedOut
        }
    }
}
