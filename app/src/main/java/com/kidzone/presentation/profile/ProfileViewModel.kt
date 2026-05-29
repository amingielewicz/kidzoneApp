package com.kidzone.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.utils.OpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel ekranu profilu użytkownika.
 *
 * Ten VM ma dwa rodzaje stanu:
 *
 * 1. **`user`** – aktualnie zalogowany user, "bogata" wersja z Firestore
 *    (z licznikami i firstName/lastName). Czytamy go przez snapshot listener
 *    [AuthRepository.observeUser], dzięki czemu po zapisaniu edycji albo
 *    po dodaniu nowego miejsca / opinii (które zwiększają liczniki w
 *    Firestore) UI od razu zauważa zmianę – bez ręcznego refreshu.
 *
 * 2. **`uiState`** – stan ekranu (otwarte dialogi, spinner "Zapisuję", błędy).
 *    Trzymane oddzielnie od usera, bo niezależne od źródła danych.
 *
 * `signOut` jest jedynym mutującym call-em który NIE zmienia danych w
 * Firestore – zamiast tego wywołuje listener auth state'u, który w
 * SplashScreen / NavGraph wykrywa wylogowanie i przerzuca user'a na ekran
 * logowania.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * @property isEditOpen true gdy user otworzył sheet edycji profilu
     * @property isSaving true podczas uploadAvatar / updateUserProfile
     * @property saveError komunikat błędu zapisu (do pokazania w sheecie)
     * @property isPrivacyPolicyOpen true gdy user otworzył dialog polityki prywatności
     */
    data class UiState(
        val isEditOpen: Boolean = false,
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val isPrivacyPolicyOpen: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Bogaty profil zalogowanego użytkownika z Firestore. Łańcuch:
     *  1. `currentUser` (Auth) emituje uid – lub null gdy wylogowany.
     *  2. `flatMapLatest` przepina się na `observeUser(uid)` (snapshot z Firestore).
     *  3. Po wylogowaniu emitujemy `null`, żeby ekran nie pokazywał stale danych.
     *
     * `catch { emit(null) }` chroni UI przed crashem, gdy snapshot listener
     * dostanie błąd (np. tymczasowy brak uprawnień podczas wylogowania).
     */
    val user: StateFlow<User?> = authRepository.currentUser
        .flatMapLatest { current ->
            if (current == null) flowOf(null)
            else authRepository.observeUser(current.id)
        }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun openEditSheet() {
        _uiState.update { it.copy(isEditOpen = true, saveError = null) }
    }

    fun dismissEditSheet() {
        // W trakcie zapisu nie pozwalamy zamknąć sheet'a (sheet i tak ignoruje
        // dismiss request, ale tu jest druga linia obrony).
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isEditOpen = false, saveError = null) }
    }

    fun openPrivacyPolicy() {
        _uiState.update { it.copy(isPrivacyPolicyOpen = true) }
    }

    fun dismissPrivacyPolicy() {
        _uiState.update { it.copy(isPrivacyPolicyOpen = false) }
    }

    /**
     * Zapis zmian profilu.
     *
     * Kolejność operacji:
     *  1. Jeśli wybrano nowy avatar – upload do Firebase Storage,
     *     zwracane downloadUrl trafia do `finalAvatarUrl`.
     *  2. Update profilu w Firestore + FirebaseAuth.
     *
     * Świadomie wybieramy "upload first, save second" – jeśli upload się
     * wywali (np. brak Internetu w trakcie), nie zostawiamy w Firestore
     * referencji do zdjęcia, którego nie ma. Jeśli upload się powiedzie,
     * a save padnie – plik wisi sam w Storage, ale przy najbliższym
     * "Zapisz" zostanie nadpisany (ścieżka jest stała: avatars/{uid}/avatar.jpg).
     *
     * @param newAvatarUri lokalny URI z PhotoPickera. Null oznacza "nie zmieniaj
     *   avatara" – wtedy zachowujemy [currentAvatarUrl] z aktualnego dokumentu.
     */
    fun saveProfile(
        displayName: String,
        firstName: String,
        lastName: String,
        newAvatarUri: Uri?
    ) {
        val currentAvatarUrl = user.value?.avatarUrl
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }

            // Krok 1: upload (opcjonalny).
            val finalAvatarUrl: String? = if (newAvatarUri != null) {
                when (val uploadResult = authRepository.uploadAvatar(newAvatarUri)) {
                    is OpResult.Success -> uploadResult.data
                    is OpResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                saveError = uploadResult.error.message
                                    ?: "Nie udało się wgrać zdjęcia"
                            )
                        }
                        return@launch
                    }
                }
            } else {
                currentAvatarUrl
            }

            // Krok 2: update danych profilu.
            when (
                val updateResult = authRepository.updateUserProfile(
                    displayName = displayName,
                    firstName = firstName,
                    lastName = lastName,
                    avatarUrl = finalAvatarUrl
                )
            ) {
                is OpResult.Success -> _uiState.update {
                    it.copy(isSaving = false, isEditOpen = false, saveError = null)
                }
                is OpResult.Failure -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = updateResult.error.message
                            ?: "Nie udało się zapisać profilu"
                    )
                }
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onComplete()
        }
    }
}
