package com.kidzone.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.SignInProvider
import com.kidzone.domain.service.BadgePreferences
import com.kidzone.domain.usecase.NotificationPrefsUseCase
import com.kidzone.i18n.AppLanguage
import com.kidzone.i18n.LanguagePreferences
import com.kidzone.presentation.common.BadgeContext
import com.kidzone.presentation.common.UserBadge
import com.kidzone.presentation.common.computeBadges
import com.kidzone.utils.OpResult
import com.kidzone.utils.UiText
import com.kidzone.utils.toUploadErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val RANKING_LIMIT = 100
private const val FLOW_SUBSCRIPTION_TIMEOUT_MS = 5000L

/**
 * ViewModel profilu użytkownika.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val placeRepository: PlaceRepository,
    private val badgePreferences: BadgePreferences,
    private val notificationPrefsUseCase: NotificationPrefsUseCase,
    private val languagePreferences: LanguagePreferences
) : ViewModel() {

    data class UiState(
        val isRefreshing: Boolean = false,
        val isEditOpen: Boolean = false,
        val isSaving: Boolean = false,
        val saveError: UiText? = null,
        val isNotificationPrefsOpen: Boolean = false,
        val notificationPrefs: NotificationPrefs = NotificationPrefs(),
        val isTermsOfServiceOpen: Boolean = false,
        val isPrivacyPolicyOpen: Boolean = false,
        val isContactOpen: Boolean = false,
        val isChangePasswordOpen: Boolean = false,
        val isChangeEmailOpen: Boolean = false,
        val isDeleteAccountOpen: Boolean = false,
        val isAccountActionInProgress: Boolean = false,
        val accountActionError: UiText? = null,
        val accountActionInfo: UiText? = null,
        val isBadgesInfoOpen: Boolean = false,
        val obtainedBadges: List<UserBadge> = emptyList(),
        val newlyEarnedBadges: List<UserBadge> = emptyList(),
        val userRank: Int? = null,
        val signInProvider: SignInProvider = SignInProvider.UNKNOWN,
        val selectedLanguage: AppLanguage = AppLanguage.SYSTEM,
        val isLanguageDialogOpen: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val richUser = authRepository.currentUser
        .flatMapLatest { current ->
            if (current == null) flowOf(null) else authRepository.observeUser(current.id)
        }
        .catch { emit(null) }

    private val userContext = richUser.map { user ->
        if (user == null) return@map null to BadgeContext()

        coroutineScope {
            val placesTask = async { placeRepository.getTopPlaces(RANKING_LIMIT) }
            val usersTask = async { authRepository.getTopUsers(RANKING_LIMIT) }

            val topPlaces = (placesTask.await() as? OpResult.Success)?.data.orEmpty()
            val topUsers = (usersTask.await() as? OpResult.Success)?.data.orEmpty()

            val myRank = topUsers.indexOfFirst { it.id == user.id }
                .takeIf { it != -1 }?.let { it + 1 }
            val myBestPlaceRank = topPlaces.indexOfFirst { it.ownerUserId == user.id }
                .takeIf { it != -1 }?.let { it + 1 }

            user to BadgeContext(userRank = myRank, bestPlaceRank = myBestPlaceRank)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val user: StateFlow<User?> = userContext.map { it?.first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_SUBSCRIPTION_TIMEOUT_MS), null)

    init {
        viewModelScope.launch {
            userContext.collect { context ->
                val (u, bCtx) = context ?: return@collect
                if (u == null) return@collect

                val allBadges = u.computeBadges(bCtx)
                val provider = authRepository.getCurrentSignInProvider()
                val lang = languagePreferences.getLanguage()

                val newlyEarned = detectNewBadges(u.id, allBadges)

                _uiState.update {
                    it.copy(
                        obtainedBadges = allBadges,
                        newlyEarnedBadges = newlyEarned,
                        userRank = bCtx.userRank,
                        signInProvider = provider,
                        selectedLanguage = lang
                    )
                }
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            authRepository.refreshUser()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onSignedOut()
        }
    }

    // --- Profile Editing ---

    fun openEditSheet() {
        _uiState.update { it.copy(isEditOpen = true, saveError = null) }
    }

    fun dismissEditSheet() {
        if (!_uiState.value.isSaving) {
            _uiState.update { it.copy(isEditOpen = false) }
        }
    }

    fun saveProfile(displayName: String, firstName: String, lastName: String, newAvatarUri: Uri?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }

            var finalAvatarUrl: String? = user.value?.avatarUrl
            if (newAvatarUri != null) {
                when (val uploadResult = authRepository.uploadAvatar(newAvatarUri)) {
                    is OpResult.Success -> finalAvatarUrl = uploadResult.data
                    is OpResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                saveError = uploadResult.error.toUploadErrorMessage()
                            )
                        }
                        return@launch
                    }
                }
            }

            val result = authRepository.updateUserProfile(displayName, firstName, lastName, finalAvatarUrl)
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(isSaving = false, isEditOpen = false)
                    is OpResult.Failure -> it.copy(
                        isSaving = false,
                        saveError = UiText.DynamicString(result.error.message ?: "Update failed")
                    )
                }
            }
        }
    }

    // --- Account Actions ---

    fun openChangePassword() {
        _uiState.update { it.copy(isChangePasswordOpen = true, accountActionError = null) }
    }

    fun dismissChangePassword() {
        _uiState.update { it.copy(isChangePasswordOpen = false) }
    }

    fun changePassword(current: String, new: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAccountActionInProgress = true, accountActionError = null) }
            val result = authRepository.changePassword(current, new)
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(
                        isAccountActionInProgress = false,
                        isChangePasswordOpen = false,
                        accountActionInfo = UiText.DynamicString("Password changed")
                    )
                    is OpResult.Failure -> it.copy(
                        isAccountActionInProgress = false,
                        accountActionError = UiText.DynamicString(result.error.message ?: "Action failed")
                    )
                }
            }
        }
    }

    fun openChangeEmail() {
        _uiState.update { it.copy(isChangeEmailOpen = true, accountActionError = null) }
    }

    fun dismissChangeEmail() {
        _uiState.update { it.copy(isChangeEmailOpen = false) }
    }

    fun changeEmail(password: String, newEmail: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAccountActionInProgress = true, accountActionError = null) }
            val result = authRepository.changeEmail(password, newEmail)
            _uiState.update {
                when (result) {
                    is OpResult.Success -> it.copy(
                        isAccountActionInProgress = false,
                        isChangeEmailOpen = false,
                        accountActionInfo = UiText.DynamicString("Verification link sent to $newEmail")
                    )
                    is OpResult.Failure -> it.copy(
                        isAccountActionInProgress = false,
                        accountActionError = UiText.DynamicString(result.error.message ?: "Action failed")
                    )
                }
            }
        }
    }

    fun openDeleteAccount() {
        _uiState.update {
            it.copy(isDeleteAccountOpen = true, accountActionError = null)
        }
    }

    fun dismissDeleteAccount() {
        if (!_uiState.value.isAccountActionInProgress) {
            _uiState.update { it.copy(isDeleteAccountOpen = false) }
        }
    }

    fun deleteAccount(password: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isAccountActionInProgress = true, accountActionError = null)
            }
            val result = authRepository.deleteAccount(password)
            if (result is OpResult.Success) {
                _uiState.update { it.copy(isAccountActionInProgress = false, isDeleteAccountOpen = false) }
                onDeleted()
            } else {
                _uiState.update {
                    val msg = (result as? OpResult.Failure)?.error?.message ?: "Delete failed"
                    it.copy(
                        isAccountActionInProgress = false,
                        accountActionError = UiText.DynamicString(msg)
                    )
                }
            }
        }
    }

    fun deleteAccountGoogle(idToken: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isAccountActionInProgress = true, accountActionError = null)
            }
            val result = authRepository.deleteAccountWithGoogle(idToken)
            if (result is OpResult.Success) {
                _uiState.update { it.copy(isAccountActionInProgress = false, isDeleteAccountOpen = false) }
                onDeleted()
            } else {
                _uiState.update {
                    val msg = (result as? OpResult.Failure)?.error?.message ?: "Delete failed"
                    it.copy(
                        isAccountActionInProgress = false,
                        accountActionError = UiText.DynamicString(msg)
                    )
                }
            }
        }
    }

    // --- Misc Dialogs ---

    fun openTermsOfService() { _uiState.update { it.copy(isTermsOfServiceOpen = true) } }
    fun dismissTermsOfService() { _uiState.update { it.copy(isTermsOfServiceOpen = false) } }

    fun openPrivacyPolicy() { _uiState.update { it.copy(isPrivacyPolicyOpen = true) } }
    fun dismissPrivacyPolicy() { _uiState.update { it.copy(isPrivacyPolicyOpen = false) } }

    fun openContact() { _uiState.update { it.copy(isContactOpen = true) } }
    fun dismissContact() { _uiState.update { it.copy(isContactOpen = false) } }

    fun openNotificationPrefs() {
        _uiState.update { it.copy(isNotificationPrefsOpen = true) }
        loadNotificationPrefs()
    }

    fun dismissNotificationPrefs() { _uiState.update { it.copy(isNotificationPrefsOpen = false) } }

    fun saveNotificationPrefs(prefs: NotificationPrefs) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAccountActionInProgress = true) }
            notificationPrefsUseCase.save(prefs)
            _uiState.update {
                it.copy(
                    isAccountActionInProgress = false,
                    isNotificationPrefsOpen = false,
                    notificationPrefs = prefs
                )
            }
        }
    }

    private fun loadNotificationPrefs() {
        viewModelScope.launch {
            val prefs = notificationPrefsUseCase.load()
            _uiState.update { it.copy(notificationPrefs = prefs) }
        }
    }

    fun openLanguageDialog() { _uiState.update { it.copy(isLanguageDialogOpen = true) } }
    fun dismissLanguageDialog() { _uiState.update { it.copy(isLanguageDialogOpen = false) } }

    fun saveLanguage(language: AppLanguage) {
        languagePreferences.setLanguage(language)
        _uiState.update { it.copy(selectedLanguage = language, isLanguageDialogOpen = false) }
    }

    // --- Badges ---

    fun openBadgesInfo() { _uiState.update { it.copy(isBadgesInfoOpen = true) } }
    fun dismissBadgesInfo() { _uiState.update { it.copy(isBadgesInfoOpen = false) } }

    fun consumeNewlyEarnedBadge() {
        _uiState.update { it.copy(newlyEarnedBadges = emptyList()) }
    }

    private fun detectNewBadges(userId: String, currentBadges: List<UserBadge>): List<UserBadge> {
        val seenNames = badgePreferences.getSeenBadges(userId)
        val currentNames = currentBadges.map { it.name }.toSet()
        val newlyEarned = currentBadges.filter { it.name !in seenNames }

        if (newlyEarned.isNotEmpty()) {
            badgePreferences.setSeenBadges(userId, seenNames + currentNames)
            // Persist also to Firestore so other devices know
            viewModelScope.launch {
                authRepository.recordBadgesEarned(newlyEarned.map { it.name })
            }
        }
        return newlyEarned
    }

    fun consumeAccountActionInfo() {
        _uiState.update { it.copy(accountActionInfo = null) }
    }
}
