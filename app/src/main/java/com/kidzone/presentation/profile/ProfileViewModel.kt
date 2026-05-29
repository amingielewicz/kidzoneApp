package com.kidzone.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.SignInProvider
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
 * Stan jest podzielony na trzy oś:
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
 * 3. **`signInProvider`** w [UiState] – decyduje, które akcje pokazać w
 *    sekcji "Konto i bezpieczeństwo". Inicjalizowany raz w `init`, bo
 *    Firebase Auth nie zmienia providera w trakcie sesji.
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
     * @property signInProvider sposób uwierzytelnienia (decyduje o dostępnych akcjach konta)
     * @property isChangePasswordOpen / [isChangeEmailOpen] / [isDeleteAccountOpen]
     *   widoczność każdego z dialogów zarządzania kontem
     * @property isAccountActionInProgress wspólny spinner dla 3 akcji
     *   (zmiana hasła / e-maila / usunięcie konta) – tylko jedna może
     *   być aktywna w danym momencie
     * @property accountActionError tekst błędu pokazywany w aktywnym dialogu
     * @property accountActionInfo informacja typu "Sprawdź skrzynkę..." po
     *   zmianie e-maila; pokazywana jako snack/toast po zamknięciu dialogu
     */
    data class UiState(
        val isEditOpen: Boolean = false,
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val isPrivacyPolicyOpen: Boolean = false,
        val signInProvider: SignInProvider = SignInProvider.UNKNOWN,
        val isChangePasswordOpen: Boolean = false,
        val isChangeEmailOpen: Boolean = false,
        val isDeleteAccountOpen: Boolean = false,
        val isAccountActionInProgress: Boolean = false,
        val accountActionError: String? = null,
        val accountActionInfo: String? = null
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
     * dostanie błąd (np. tymczasowy brak uprawnień podczas wylogowania albo
     * po `deleteAccount`, gdy doc usera już nie istnieje).
     */
    val user: StateFlow<User?> = authRepository.currentUser
        .flatMapLatest { current ->
            if (current == null) flowOf(null)
            else authRepository.observeUser(current.id)
        }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Provider raz w trakcie sesji – Firebase Auth go nie zmienia, dopóki
        // user się nie wyloguje i nie zaloguje innym sposobem (a wtedy VM
        // i tak jest tworzony na nowo, bo NavGraph wraca na Main → Profile).
        viewModelScope.launch {
            val provider = authRepository.getCurrentSignInProvider()
            _uiState.update { it.copy(signInProvider = provider) }
        }
    }

    // -------- Edycja profilu --------

    fun openEditSheet() {
        _uiState.update { it.copy(isEditOpen = true, saveError = null) }
    }

    fun dismissEditSheet() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isEditOpen = false, saveError = null) }
    }

    // -------- Polityka prywatności --------

    fun openPrivacyPolicy() {
        _uiState.update { it.copy(isPrivacyPolicyOpen = true) }
    }

    fun dismissPrivacyPolicy() {
        _uiState.update { it.copy(isPrivacyPolicyOpen = false) }
    }

    /**
     * Zapis zmian profilu (avatar + dane).
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

    // -------- Konto i bezpieczeństwo: dialogi --------

    fun openChangePassword() {
        _uiState.update {
            it.copy(isChangePasswordOpen = true, accountActionError = null)
        }
    }

    fun dismissChangePassword() {
        if (_uiState.value.isAccountActionInProgress) return
        _uiState.update {
            it.copy(isChangePasswordOpen = false, accountActionError = null)
        }
    }

    fun openChangeEmail() {
        _uiState.update {
            it.copy(isChangeEmailOpen = true, accountActionError = null)
        }
    }

    fun dismissChangeEmail() {
        if (_uiState.value.isAccountActionInProgress) return
        _uiState.update {
            it.copy(isChangeEmailOpen = false, accountActionError = null)
        }
    }

    fun openDeleteAccount() {
        _uiState.update {
            it.copy(isDeleteAccountOpen = true, accountActionError = null)
        }
    }

    fun dismissDeleteAccount() {
        if (_uiState.value.isAccountActionInProgress) return
        _uiState.update {
            it.copy(isDeleteAccountOpen = false, accountActionError = null)
        }
    }

    /** Czyści jednorazowy info-banner po pokazaniu (ack od UI). */
    fun consumeAccountActionInfo() {
        _uiState.update { it.copy(accountActionInfo = null) }
    }

    /**
     * Zmiana hasła. Walidacje (długość, match) są w UI, repo dodatkowo
     * waliduje przez Firebase (FirebaseAuthWeakPasswordException).
     */
    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isAccountActionInProgress = true, accountActionError = null)
            }
            when (val r = authRepository.changePassword(currentPassword, newPassword)) {
                is OpResult.Success -> _uiState.update {
                    it.copy(
                        isAccountActionInProgress = false,
                        isChangePasswordOpen = false,
                        accountActionInfo = "Hasło zostało zmienione"
                    )
                }
                is OpResult.Failure -> _uiState.update {
                    it.copy(
                        isAccountActionInProgress = false,
                        accountActionError = r.error.message ?: "Nie udało się zmienić hasła"
                    )
                }
            }
        }
    }

    /**
     * Zmiana e-maila przez verifyBeforeUpdateEmail – wysyła link weryfikacyjny
     * na nowy adres. Dialog się zamyka po sukcesie i pokazujemy snackowy info,
     * że user musi kliknąć w link.
     */
    fun changeEmail(currentPassword: String, newEmail: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isAccountActionInProgress = true, accountActionError = null)
            }
            when (val r = authRepository.changeEmail(currentPassword, newEmail)) {
                is OpResult.Success -> _uiState.update {
                    it.copy(
                        isAccountActionInProgress = false,
                        isChangeEmailOpen = false,
                        accountActionInfo = "Wysłaliśmy link weryfikacyjny na: $newEmail. " +
                            "Kliknij w niego, by potwierdzić zmianę adresu."
                    )
                }
                is OpResult.Failure -> _uiState.update {
                    it.copy(
                        isAccountActionInProgress = false,
                        accountActionError = r.error.message ?: "Nie udało się zmienić e-maila"
                    )
                }
            }
        }
    }

    /**
     * Trwałe usunięcie konta z reauth. Po sukcesie wywołuje [onDeleted],
     * żeby NavGraph przeszedł na ekran logowania – analogicznie jak [signOut].
     */
    fun deleteAccount(currentPassword: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isAccountActionInProgress = true, accountActionError = null)
            }
            when (val r = authRepository.deleteAccount(currentPassword)) {
                is OpResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAccountActionInProgress = false,
                            isDeleteAccountOpen = false
                        )
                    }
                    onDeleted()
                }
                is OpResult.Failure -> _uiState.update {
                    it.copy(
                        isAccountActionInProgress = false,
                        accountActionError = r.error.message ?: "Nie udało się usunąć konta"
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
