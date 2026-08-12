@file:Suppress("CyclomaticComplexMethod", "LongMethod")

package com.kidzone.presentation.profile

import android.net.Uri
import com.google.firebase.functions.FirebaseFunctions
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.R
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.SignInProvider
import com.kidzone.domain.service.BadgePreferences
import com.kidzone.domain.usecase.NotificationPrefsUseCase
import com.kidzone.i18n.AppLanguage
import com.kidzone.i18n.LanguagePreferences
import com.kidzone.presentation.common.BadgeContext
import com.kidzone.presentation.common.ScreenState
import com.kidzone.presentation.common.UserBadge
import com.kidzone.presentation.common.computeBadges
import com.kidzone.utils.AppConfig
import com.kidzone.utils.OpResult
import com.kidzone.utils.UiText
import com.kidzone.utils.toAuthErrorMessage
import com.kidzone.utils.toUploadErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val RANKING_LIMIT = 100
private const val CONTACT_SUBJECT_MIN_LENGTH = 3
private const val CONTACT_MESSAGE_MIN_LENGTH = 10
private const val CONTACT_MESSAGE_FUNCTION = "submitContactMessage"
private const val REFRESH_DELAY_MS = 300L
private const val INITIAL_BADGE_COLLECTION_DELAY_MS = 1500L

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie stanem profilu użytkownika (dane osobowe, statystyki, odznaki).
 * - Obsługa preferencji (język, powiadomienia) i ustawień konta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val placeRepository: PlaceRepository,
    private val badgePreferences: BadgePreferences,
    private val notificationPrefsUseCase: NotificationPrefsUseCase,
    private val languagePreferences: LanguagePreferences,
    private val functions: FirebaseFunctions
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
        val isInitialCheckComplete: Boolean = false,
        val userRank: Int? = null,
        val signInProvider: SignInProvider = SignInProvider.UNKNOWN,
        val selectedLanguage: AppLanguage = AppLanguage.SYSTEM,
        val isLanguageDialogOpen: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private var pendingSeenBadgesUserId: String? = null
    private var pendingSeenBadgeNames: Set<String> = emptySet()
    private val profileReload = MutableStateFlow(0)
    
    private var isInitialCollectionPhase = true
    private val badgeBuffer = mutableSetOf<UserBadge>()
    private var detectedBadgeNames: Set<String> = emptySet()

    val profileState: StateFlow<ScreenState<User>> = combine(
        profileReload,
        authRepository.currentUser.distinctUntilChanged { old, new -> old?.id == new?.id }
    ) { _, current ->
        current
    }.flatMapLatest { current ->
        if (current == null) {
            flowOf(ScreenState.Error(UiText.StringResource(R.string.error_unauthorized)))
        } else {
            authRepository.observeUser(current.id)
                .map { user ->
                    if (user == null) {
                        ScreenState.Error(UiText.StringResource(R.string.profile_load_error))
                    } else {
                        ScreenState.Content(user)
                    }
                }
                .onStart { emit(ScreenState.Loading) }
                .catch { emit(ScreenState.Error(UiText.StringResource(R.string.profile_load_error))) }
        }
    }
    .stateIn(viewModelScope, SharingStarted.Eagerly, ScreenState.Loading)

    private val rankings: StateFlow<BadgeContext> = profileState
        .mapNotNull { (it as? ScreenState.Content<User>)?.data }
        .distinctUntilChanged { old, new -> old.id == new.id }
        .flatMapLatest { user ->
            flow {
                val context = coroutineScope {
                    val placesTask = async { placeRepository.getTopPlaces(RANKING_LIMIT) }
                    val usersTask = async { authRepository.getTopUsers(RANKING_LIMIT) }
                    val topPlaces = (placesTask.await() as? OpResult.Success)?.data.orEmpty()
                    val topUsers = (usersTask.await() as? OpResult.Success)?.data.orEmpty()
                    val myRank = topUsers.indexOfFirst { it.id == user.id }.takeIf { it != -1 }?.let { it + 1 }
                    val myBestPlaceRank = topPlaces.indexOfFirst { 
                        it.ownerUserId == user.id 
                    }.takeIf { it != -1 }?.let { it + 1 }
                    BadgeContext(userRank = myRank, bestPlaceRank = myBestPlaceRank)
                }
                emit(context)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, BadgeContext())

    private val userContext: StateFlow<Pair<User?, BadgeContext>?> = profileState
        .mapNotNull { (it as? ScreenState.Content<User>)?.data }
        .combine(rankings) { user, rankCtx -> user to rankCtx }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val user: StateFlow<User?> = profileState.map { (it as? ScreenState.Content)?.data }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConfig.FLOW_SUBSCRIPTION_TIMEOUT_MS), null)

    init {
        // Zawsze odświeżamy dane z Firebase Auth przy wejściu na profil,
        // aby wykryć zmiany np. po kliknięciu linku weryfikacyjnego e-mail.
        viewModelScope.launch {
            authRepository.refreshUser()
        }

        viewModelScope.launch {
            user.mapNotNull { it }
                .distinctUntilChanged { old, new -> 
                    old.id == new.id && old.avatarUrl == new.avatarUrl && old.name == new.name && old.email == new.email
                }
                .collect {
                    val lang = languagePreferences.getLanguage()
                    val provider = authRepository.getCurrentSignInProvider()
                    _uiState.update { state -> state.copy(signInProvider = provider, selectedLanguage = lang) }
                }
        }

        viewModelScope.launch {
            userContext.collect { context ->
                val (u, bCtx) = context ?: return@collect
                if (u == null) return@collect

                val allBadges = u.computeBadges(bCtx)
                val newlyEarned = detectNewBadges(u.id, allBadges)

                _uiState.update { state ->
                    if (state.obtainedBadges == allBadges && 
                        state.userRank == bCtx.userRank && 
                        newlyEarned.isEmpty()
                    ) return@update state
                    
                    state.copy(
                        obtainedBadges = allBadges,
                        newlyEarnedBadges = if (isInitialCollectionPhase) {
                            state.newlyEarnedBadges
                        } else {
                            (state.newlyEarnedBadges + newlyEarned).distinct()
                        },
                        userRank = bCtx.userRank
                    )
                }
            }
        }

        viewModelScope.launch {
            delay(INITIAL_BADGE_COLLECTION_DELAY_MS)
            isInitialCollectionPhase = false
            _uiState.update { state ->
                state.copy(newlyEarnedBadges = badgeBuffer.toList().distinct(), isInitialCheckComplete = true) 
            }
        }
    }

    fun refreshProfile() {
        _uiState.update { it.copy(isRefreshing = true) }
        profileReload.update { it + 1 }
        viewModelScope.launch {
            authRepository.refreshUser()
            delay(REFRESH_DELAY_MS)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun retryProfile() {
        profileReload.update { it + 1 }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch { authRepository.signOut(); onSignedOut() }
    }

    fun openEditSheet() { _uiState.update { it.copy(isEditOpen = true, saveError = null) } }
    fun dismissEditSheet() { if (!_uiState.value.isSaving) _uiState.update { it.copy(isEditOpen = false) } }

    fun saveProfile(displayName: String, firstName: String, lastName: String, newAvatarUri: Uri?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            var finalAvatarUrl: String? = user.value?.avatarUrl
            if (newAvatarUri != null) {
                when (val uploadResult = authRepository.uploadAvatar(newAvatarUri)) {
                    is OpResult.Success -> finalAvatarUrl = uploadResult.data
                    is OpResult.Failure -> {
                        _uiState.update { state ->
                            state.copy(
                                isSaving = false, 
                                saveError = uploadResult.error.toUploadErrorMessage()
                            ) 
                        }
                        return@launch
                    }
                }
            }
            val result = authRepository.updateUserProfile(displayName, firstName, lastName, finalAvatarUrl)
            _uiState.update { state ->
                when (result) {
                    is OpResult.Success -> state.copy(isSaving = false, isEditOpen = false)
                    is OpResult.Failure -> state.copy(
                        isSaving = false,
                        saveError = result.error.toAuthErrorMessage(R.string.profile_update_failed)
                    )
                }
            }
        }
    }

    fun openChangePassword() { _uiState.update { it.copy(isChangePasswordOpen = true, accountActionError = null) } }
    fun dismissChangePassword() { 
        if (!_uiState.value.isAccountActionInProgress) {
            _uiState.update { it.copy(isChangePasswordOpen = false, accountActionError = null) } 
        }
    }
    fun changePassword(current: String, new: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAccountActionInProgress = true, accountActionError = null) }
            val result = authRepository.changePassword(current, new)
            _uiState.update { state ->
                when (result) {
                    is OpResult.Success -> state.copy(
                        isAccountActionInProgress = false,
                        isChangePasswordOpen = false,
                        accountActionInfo = UiText.StringResource(R.string.password_changed)
                    )
                    is OpResult.Failure -> state.copy(
                        isAccountActionInProgress = false,
                        accountActionError = result.error.toAuthErrorMessage(R.string.account_action_failed)
                    )
                }
            }
        }
    }

    fun openChangeEmail() { _uiState.update { it.copy(isChangeEmailOpen = true, accountActionError = null) } }
    fun dismissChangeEmail() { 
        if (!_uiState.value.isAccountActionInProgress) {
            _uiState.update { it.copy(isChangeEmailOpen = false, accountActionError = null) } 
        }
    }
    fun changeEmail(password: String, newEmail: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAccountActionInProgress = true, accountActionError = null) }
            val result = authRepository.changeEmail(password, newEmail)
            _uiState.update { state ->
                when (result) {
                    is OpResult.Success -> state.copy(
                        isAccountActionInProgress = false,
                        isChangeEmailOpen = false,
                        accountActionInfo = UiText.StringResource(
                            R.string.change_email_verification_sent,
                            newEmail
                        )
                    )
                    is OpResult.Failure -> state.copy(
                        isAccountActionInProgress = false,
                        accountActionError = result.error.toAuthErrorMessage(
                            R.string.account_action_failed
                        )
                    )
                }
            }
        }
    }

    fun openDeleteAccount() { _uiState.update { it.copy(isDeleteAccountOpen = true, accountActionError = null) } }
    fun dismissDeleteAccount() { 
        if (!_uiState.value.isAccountActionInProgress) {
            _uiState.update { it.copy(isDeleteAccountOpen = false) } 
        }
    }
    fun deleteAccount(password: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAccountActionInProgress = true, accountActionError = null) }
            val result = authRepository.deleteAccount(password)
            if (result is OpResult.Success) { 
                _uiState.update { it.copy(isAccountActionInProgress = false, isDeleteAccountOpen = false) }
                onDeleted() 
            } else { 
                _uiState.update { state ->
                    state.copy(
                        isAccountActionInProgress = false, 
                        accountActionError = (result as? OpResult.Failure)?.error
                            ?.toAuthErrorMessage(R.string.delete_account_failed) 
                            ?: UiText.StringResource(R.string.delete_account_failed)
                    ) 
                } 
            }
        }
    }

    fun deleteAccountGoogle(idToken: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAccountActionInProgress = true, accountActionError = null) }
            val result = authRepository.deleteAccountWithGoogle(idToken)
            if (result is OpResult.Success) { 
                _uiState.update { it.copy(isAccountActionInProgress = false, isDeleteAccountOpen = false) }
                onDeleted() 
            } else { 
                _uiState.update { state ->
                    state.copy(
                        isAccountActionInProgress = false, 
                        accountActionError = (result as? OpResult.Failure)?.error
                            ?.toAuthErrorMessage(R.string.delete_account_failed) 
                            ?: UiText.StringResource(R.string.delete_account_failed)
                    ) 
                } 
            }
        }
    }

    fun openTermsOfService() { _uiState.update { it.copy(isTermsOfServiceOpen = true) } }
    fun dismissTermsOfService() { _uiState.update { it.copy(isTermsOfServiceOpen = false) } }
    fun openPrivacyPolicy() { _uiState.update { it.copy(isPrivacyPolicyOpen = true) } }
    fun dismissPrivacyPolicy() { _uiState.update { it.copy(isPrivacyPolicyOpen = false) } }
    fun openContact() { _uiState.update { it.copy(isContactOpen = true) } }
    fun dismissContact() { _uiState.update { it.copy(isContactOpen = false) } }

    fun submitContactMessage(subject: String, message: String) {
        val cleanSubject = subject.trim()
        val cleanMessage = message.trim()
        if (cleanSubject.length < CONTACT_SUBJECT_MIN_LENGTH || 
            cleanMessage.length < CONTACT_MESSAGE_MIN_LENGTH
        ) {
            _uiState.update { 
                it.copy(
                    accountActionError = UiText.StringResource(
                        R.string.contact_support_validation_error
                    )
                ) 
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAccountActionInProgress = true, accountActionError = null) }
            runCatching { 
                functions.getHttpsCallable(CONTACT_MESSAGE_FUNCTION).call(
                    mapOf("subject" to cleanSubject, "message" to cleanMessage)
                ).await() 
            }
                .onSuccess { 
                    _uiState.update { state ->
                        state.copy(
                            isAccountActionInProgress = false, 
                            isContactOpen = false, 
                            accountActionInfo = UiText.StringResource(R.string.contact_support_sent)
                        ) 
                    } 
                }
                .onFailure { 
                    _uiState.update { state ->
                        state.copy(
                            isAccountActionInProgress = false, 
                            accountActionError = UiText.StringResource(R.string.contact_support_send_failed)
                        ) 
                    } 
                }
        }
    }

    fun openNotificationPrefs() { _uiState.update { it.copy(isNotificationPrefsOpen = true) }; loadNotificationPrefs() }
    fun dismissNotificationPrefs() { _uiState.update { it.copy(isNotificationPrefsOpen = false) } }
    fun saveNotificationPrefs(prefs: NotificationPrefs) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAccountActionInProgress = true) }
            notificationPrefsUseCase.save(prefs)
            _uiState.update { state ->
                state.copy(
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
        _uiState.update { state -> 
            state.copy(selectedLanguage = language, isLanguageDialogOpen = false) 
        } 
    }
    fun openBadgesInfo() { _uiState.update { it.copy(isBadgesInfoOpen = true) } }
    fun dismissBadgesInfo() { _uiState.update { it.copy(isBadgesInfoOpen = false) } }

    fun consumeNewlyEarnedBadge() {
        val userId = pendingSeenBadgesUserId
        if (userId != null && pendingSeenBadgeNames.isNotEmpty()) {
            badgePreferences.setSeenBadges(userId, pendingSeenBadgeNames)
        }
        pendingSeenBadgesUserId = null
        pendingSeenBadgeNames = emptySet()
        // detectedBadgeNames remains to prevent re-triggering in the same session
        badgeBuffer.clear()
        isInitialCollectionPhase = false
        _uiState.update { it.copy(newlyEarnedBadges = emptyList()) }
    }

    private fun detectNewBadges(userId: String, currentBadges: List<UserBadge>): List<UserBadge> {
        val seenNamesInPrefs = badgePreferences.getSeenBadges(userId)
        
        // Baseline: co już użytkownik widział na tym urządzeniu + co wykryliśmy w tej sesji.
        val allKnownBadges = seenNamesInPrefs + detectedBadgeNames
        val currentNames = currentBadges.map { it.name }.toSet()
        
        // Filtrujemy tylko te, których nie znamy lokalnie.
        var newlyEarned = currentBadges.filter { it.name !in allKnownBadges }.sortedBy { it.ordinal }

        // Dodatkowe zabezpieczenie: jeśli badge jest już w profilu (Firestore), 
        // to nie jest "nowy" dla tego użytkownika, a jedynie dla tego urządzenia.
        // Wyświetlamy go tylko podczas pierwszej synchronizacji (initial phase).
        if (!isInitialCollectionPhase) {
            val firestoreBadges = user.value?.badgeEarnedAt?.keys ?: emptySet()
            newlyEarned = newlyEarned.filter { it.name !in firestoreBadges }
        }
        
        // Obsługa odznak odebranych (np. spadek w rankingu).
        // Tylko jeśli faktycznie coś ubyło względem tego, co zapisaliśmy jako widoczne.
        val revoked = seenNamesInPrefs.filter { name -> currentBadges.none { it.name == name } }
        if (revoked.isNotEmpty()) {
            viewModelScope.launch { authRepository.revokeBadges(revoked) }
            // Nie czyścimy całego stanu seen, bo to spowoduje ponowne gratulacje dla reszty.
            // Zamiast tego usuwamy tylko te odebrane z lokalnego zapisu.
            badgePreferences.setSeenBadges(userId, currentNames)
        }

        if (newlyEarned.isNotEmpty()) {
            // Rejestrujemy wykryte odznaki, aby nie wykrywać ich ponownie przed zatwierdzeniem.
            detectedBadgeNames = detectedBadgeNames + newlyEarned.map { it.name }
            pendingSeenBadgesUserId = userId
            // Przygotowujemy KOMPLETNY stan odznak do zapisu w Prefs po kliknięciu "Super!".
            pendingSeenBadgeNames = currentNames
            
            if (isInitialCollectionPhase) {
                badgeBuffer.addAll(newlyEarned)
            } else {
                viewModelScope.launch {
                    authRepository.recordBadgesEarned(newlyEarned.map { it.name })
                }
            }
            return newlyEarned
        }
        return emptyList()
    }

    fun consumeAccountActionInfo() { _uiState.update { it.copy(accountActionInfo = null) } }
}
