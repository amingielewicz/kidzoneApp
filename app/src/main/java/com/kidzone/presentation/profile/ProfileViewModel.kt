package com.kidzone.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.SignInProvider
import com.kidzone.presentation.common.BadgeContext
import com.kidzone.presentation.common.UserBadge
import com.kidzone.presentation.common.computeBadges
import com.kidzone.utils.OpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
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
    private val authRepository: AuthRepository,
    private val placeRepository: PlaceRepository,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    @ApplicationContext private val appContext: Context
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
     * @property isBadgesInfoOpen true gdy user otworzył info-dialog odznak
     *   (kliknął "?" obok sekcji "Odznaki" w profilu).
     * @property obtainedBadges aktualnie zdobyte odznaki (po uwzględnieniu
     *   kontekstu rankingowego). Wyliczane przez VM, żeby UI nie musiał
     *   znać szczegółów [BadgeContext].
     * @property userRank 1-based pozycja w rankingu TOP 100 użytkowników
     *   (w obrębie filtrów aktywności tożsamych z RankingViewModel - tylko
     *   userzy z >=1 miejscem lub opinią). Null = poza TOP 100. Używane
     *   przez UI do plakietki "TOP" w prawym górnym rogu nagłówka profilu.
     * @property newlyEarnedBadges nowo zdobyte odznaki do pokazania w zbiorczym
     *   dialogu gratulacyjnym. Konsumujemy całą listę naraz przez
     *   [consumeNewlyEarnedBadge] po zamknięciu dialogu.
     */
    data class UiState(
        val isEditOpen: Boolean = false,
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val isPrivacyPolicyOpen: Boolean = false,
        val isTermsOfServiceOpen: Boolean = false,
        val signInProvider: SignInProvider = SignInProvider.UNKNOWN,
        val isChangePasswordOpen: Boolean = false,
        val isChangeEmailOpen: Boolean = false,
        val isDeleteAccountOpen: Boolean = false,
        val isAccountActionInProgress: Boolean = false,
        val accountActionError: String? = null,
        val accountActionInfo: String? = null,
        val isBadgesInfoOpen: Boolean = false,
        val obtainedBadges: List<UserBadge> = emptyList(),
        val userRank: Int? = null,
        val newlyEarnedBadges: List<UserBadge> = emptyList(),
        val isRefreshing: Boolean = false,
        val isNotificationPrefsOpen: Boolean = false,
        val notificationPrefs: NotificationPrefs = NotificationPrefs()
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

        // Detekcja nowych odznak. Subskrybujemy strumień bogatego usera i
        // przy każdej emisji liczymy odznaki (z udziałem aktualnego rankingu),
        // porównując do persisted "ostatnio widzianych" (SharedPreferences
        // per uid). Diff trafia do UiState - UI pokazuje jeden dialog
        // gratulacyjny na odznakę, kolejne czekają w `pendingNewBadges`.
        viewModelScope.launch {
            user.filterNotNull().collect { u ->
                val context = computeBadgeContext(u)
                val obtained = u.computeBadges(context)
                _uiState.update {
                    it.copy(
                        obtainedBadges = obtained,
                        // BadgeContext.userRank trzymane juz 1..100 z
                        // computeBadgeContext (BADGE_RANK_POOL = 100); UI
                        // pokazuje plakietke gdy != null.
                        userRank = context.userRank
                    )
                }
                checkForNewBadges(uid = u.id, current = obtained.toSet())
            }
        }
    }

    /**
     * Jednorazowy fetch rankingów potrzebnych do wyliczenia odznak
     * "rankingowych" ([UserBadge.LEADER_GOLD] / [UserBadge.PLACE_TOP1] itp.).
     *
     * Reguły są zgodne z tym, co [com.kidzone.presentation.ranking.RankingViewModel]
     * pokazuje w UI:
     *  - **userów** filtrujemy do tych, którzy mają choć 1 dodane miejsce
     *    LUB 1 opinię (czyli "aktywnych"), i bierzemy ich pozycję 1-based
     *    do [BadgeContext.userRank];
     *  - **miejsca** filtrujemy do tych z >0 opiniami i >0.0 średnią ocen,
     *    bierzemy najlepszą pozycję jakiegokolwiek miejsca tego usera
     *    do [BadgeContext.bestPlaceRank].
     *
     * Spójność z RankingScreen jest celowa - jeśli user widzi siebie na
     * #1 w rankingu, dostaje odznakę "Złoty lider". Inaczej "wiszące"
     * pozycje (np. user #1 w surowym fetchu, ale poza filtrem aktywności)
     * dostawałyby odznakę bez bycia widocznym w rankingu - mylące.
     *
     * Best-effort: przy błędzie fetcha zwracamy pusty kontekst, czyli
     * po prostu nie przyznajemy odznak rankingowych. Pozostałe (count-based)
     * lecą normalnie.
     */
    private suspend fun computeBadgeContext(user: User): BadgeContext {
        return try {
            val users = (authRepository.getTopUsers(BADGE_RANK_POOL) as? OpResult.Success)
                ?.data
                .orEmpty()
                .filter { it.placesAddedCount > 0 || it.reviewsCount > 0 }
            val userRank = users.indexOfFirst { it.id == user.id }
                .takeIf { it >= 0 }
                ?.plus(1)

            val topPlaces = (placeRepository.getTopPlaces(BADGE_RANK_POOL) as? OpResult.Success)
                ?.data
                .orEmpty()
                .filter { it.reviewsCount > 0 && it.averageRating > 0.0 }
            val bestPlaceRank = topPlaces
                .mapIndexedNotNull { idx, place ->
                    if (place.ownerUserId == user.id) idx + 1 else null
                }
                .minOrNull()

            BadgeContext(userRank = userRank, bestPlaceRank = bestPlaceRank)
        } catch (_: Exception) {
            BadgeContext()
        }
    }

    // -------- Edycja profilu --------

    /** Pull-to-refresh: re-compute badge context (ranks may have changed). */
    fun refreshProfile() {
        val u = user.value ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val context = computeBadgeContext(u)
            val obtained = u.computeBadges(context)
            _uiState.update {
                it.copy(
                    obtainedBadges = obtained,
                    userRank = context.userRank,
                    isRefreshing = false
                )
            }
        }
    }

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

    // -------- Regulamin --------

    fun openTermsOfService() {
        _uiState.update { it.copy(isTermsOfServiceOpen = true) }
    }

    fun dismissTermsOfService() {
        _uiState.update { it.copy(isTermsOfServiceOpen = false) }
    }

    // -------- Preferencje powiadomień --------

    fun openNotificationPrefs() {
        _uiState.update { it.copy(isNotificationPrefsOpen = true) }
        loadNotificationPrefs()
    }

    fun dismissNotificationPrefs() {
        _uiState.update { it.copy(isNotificationPrefsOpen = false) }
    }

    fun saveNotificationPrefs(prefs: NotificationPrefs) {
        viewModelScope.launch {
            val uid = authRepository.currentUser.first()?.id ?: return@launch
            val data = mapOf(
                "notificationPreferences" to mapOf(
                    "newReviewOnMyPlace" to prefs.newReviewOnMyPlace,
                    "newBadgeEarned" to prefs.newBadgeEarned,
                    "newPhotoOnMyPlace" to prefs.newPhotoOnMyPlace,
                    "rankings" to prefs.rankings
                ),
                "emailNotificationsEnabled" to prefs.emailNotificationsEnabled
            )
            try {
                firestore
                    .collection("users").document(uid)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .await()
            } catch (_: Exception) { }
            _uiState.update { it.copy(isNotificationPrefsOpen = false, notificationPrefs = prefs) }
        }
    }

    private fun loadNotificationPrefs() {
        viewModelScope.launch {
            val uid = authRepository.currentUser.first()?.id ?: return@launch
            try {
                val snap = firestore
                    .collection("users").document(uid).get().await()
                @Suppress("UNCHECKED_CAST")
                val prefsMap = snap.get("notificationPreferences") as? Map<String, Boolean>
                val emailEnabled = snap.getBoolean("emailNotificationsEnabled") ?: true
                if (prefsMap != null) {
                    _uiState.update {
                        it.copy(notificationPrefs = NotificationPrefs(
                            newReviewOnMyPlace = prefsMap["newReviewOnMyPlace"] ?: true,
                            newBadgeEarned = prefsMap["newBadgeEarned"] ?: true,
                            newPhotoOnMyPlace = prefsMap["newPhotoOnMyPlace"] ?: true,
                            rankings = prefsMap["rankings"] ?: true,
                            emailNotificationsEnabled = emailEnabled
                        ))
                    }
                } else {
                    _uiState.update {
                        it.copy(notificationPrefs = NotificationPrefs(
                            emailNotificationsEnabled = emailEnabled
                        ))
                    }
                }
            } catch (_: Exception) { }
        }
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

    /**
     * Trwałe usunięcie konta Google (reauth przez Google idToken).
     * Wywoływane po pomyślnym Google Sign-In w UI.
     */
    fun deleteAccountGoogle(idToken: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isAccountActionInProgress = true, accountActionError = null)
            }
            when (val r = authRepository.deleteAccountWithGoogle(idToken)) {
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

    // -------- Odznaki --------

    fun openBadgesInfo() {
        _uiState.update { it.copy(isBadgesInfoOpen = true) }
    }

    fun dismissBadgesInfo() {
        _uiState.update { it.copy(isBadgesInfoOpen = false) }
    }

    /**
     * Konsumuje aktualnie pokazywaną odznakę (gratulacyjny dialog został
     * zamknięty przez usera). Jeśli w buforze [UiState.pendingNewBadges]
     * są kolejne, przesuwamy następną do [UiState.newlyEarnedBadge] -
     * UI od razu pokaże kolejny dialog.
     */
    /**
     * Zamyka dialog gratulacyjny (użytkownik go obejrzał / zamknął).
     * Czyści całą listę na raz — wszystkie nowe odznaki były widoczne
     * w jednym zbiorczym dialogu.
     */
    fun consumeNewlyEarnedBadge() {
        _uiState.update { current ->
            current.copy(newlyEarnedBadges = emptyList())
        }
    }

    /**
     * Porównuje aktualnie zdobyte odznaki z tymi, o których powiadomiliśmy
     * usera już wcześniej (zapisane w SharedPreferences per uid). Jeśli
     * pojawiły się nowe - wpycha je do UiState (pierwszą do
     * [UiState.newlyEarnedBadge], resztę do [UiState.pendingNewBadges]),
     * persistuje do SharedPrefs i zapisuje timestampy zdobycia w Firestore.
     *
     * Persist robimy ZA każdym razem, gdy detekcja zachodzi - jeśli user
     * straci odznakę (np. usunął miejsca), nie chcemy mu jej znów pokazywać
     * w przyszłości jako "nowo zdobyta" przy ponownym wbiciu progu.
     *
     * Dwa źródła prawdy:
     *  - **SharedPreferences** (`seen_badges_<uid>`): "czy już pokazaliśmy
     *    dialog gratulacyjny na TYM urządzeniu?". Per-device, bo dialog ma
     *    sens raz na user-device, niezależnie od synchronizacji ze servera.
     *  - **Firestore** (`users/{uid}.badgeEarnedAt`): "kiedy ta odznaka
     *    została zdobyta?". Globalne, do chronologicznego sortu w ranking
     *    cards na dowolnym kliencie. First-write-wins (zob.
     *    [com.kidzone.domain.repository.AuthRepository.recordBadgesEarned]).
     *
     * SharedPreferences zamiast DataStore - prostsze API, ten store jest
     * mikroskopijny (kilka stringów per user), więc nie potrzebujemy
     * korutyn DataStore'owych. Klucz `seen_badges_$uid` izoluje stany
     * różnych userów na tym samym urządzeniu (dwóch rodziców logujących
     * się z jednego telefonu).
     */
    private fun checkForNewBadges(uid: String, current: Set<UserBadge>) {
        val key = "$BADGE_PREFS_KEY_PREFIX$uid"
        val seenNames = prefs.getStringSet(key, emptySet()).orEmpty()
        val seen = seenNames.mapNotNull { runCatching { UserBadge.valueOf(it) }.getOrNull() }
            .toSet()

        val newlyEarned = (current - seen)
            .sortedBy { it.ordinal }

        val revoked = (seen - current)

        if (newlyEarned.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(
                    newlyEarnedBadges = state.newlyEarnedBadges + newlyEarned
                )
            }
            viewModelScope.launch {
                authRepository.recordBadgesEarned(newlyEarned.map { it.name })
            }
        }

        if (revoked.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(
                    newlyEarnedBadges = state.newlyEarnedBadges.filter { it !in revoked }
                )
            }
            viewModelScope.launch {
                authRepository.revokeBadges(revoked.map { it.name })
            }
        }

        if (seen != current) {
            prefs.edit()
                .putStringSet(key, current.map { it.name }.toSet())
                .apply()
        }
    }

    /** Lazy-initialized SharedPreferences dla detekcji odznak. */
    private val prefs by lazy {
        appContext.getSharedPreferences(BADGE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private companion object {
        const val BADGE_PREFS_NAME = "badge_notifications"
        const val BADGE_PREFS_KEY_PREFIX = "seen_badges_"

        /**
         * Pula rankingu używana do wyliczenia [BadgeContext]. 100 jest
         * zgodne z [com.kidzone.presentation.ranking.RankingViewModel.TOP_LIMIT] -
         * to gwarantuje, że "twoje miejsce w TOP 3" / "ty w TOP 3
         * userów" odzwierciedla dokładnie to, co user widzi na zakładce
         * Ranking. Większa pula nic by nie dała (i tak interesują nas
         * tylko pozycje 1-3).
         */
        const val BADGE_RANK_POOL = 100
    }
}
