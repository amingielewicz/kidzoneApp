package com.kidzone.presentation.profile

import android.net.Uri
import com.google.firebase.functions.FirebaseFunctions
import com.kidzone.R
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.SignInProvider
import com.kidzone.domain.service.BadgePreferences
import com.kidzone.domain.usecase.NotificationPrefsUseCase
import com.kidzone.i18n.AppLanguage
import com.kidzone.i18n.LanguagePreferences
import com.kidzone.presentation.common.UserBadge
import com.kidzone.testutil.MainDispatcherRule
import com.kidzone.testutil.TestFixtures
import com.kidzone.utils.AuthException
import com.kidzone.utils.OpResult
import com.kidzone.utils.UiText
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var authRepository: AuthRepository
    private lateinit var placeRepository: PlaceRepository
    private lateinit var badgePreferences: BadgePreferences
    private lateinit var notificationPrefsUseCase: NotificationPrefsUseCase
    private lateinit var languagePreferences: LanguagePreferences
    private lateinit var functions: FirebaseFunctions
    private lateinit var viewModel: ProfileViewModel

    private val currentUserFlow = MutableStateFlow<User?>(null)

    @BeforeEach
    fun setUp() {
        authRepository = mockk(relaxed = true)
        placeRepository = mockk(relaxed = true)
        badgePreferences = mockk(relaxed = true)
        notificationPrefsUseCase = mockk(relaxed = true)
        languagePreferences = mockk(relaxed = true)
        functions = mockk(relaxed = true)

        every { badgePreferences.getSeenBadges(any()) } returns emptySet()
        every { languagePreferences.getLanguage() } returns AppLanguage.SYSTEM
        every { languagePreferences.setLanguage(any()) } just Runs

        every { authRepository.currentUser } returns currentUserFlow
        coEvery { authRepository.getCurrentSignInProvider() } returns SignInProvider.EMAIL_PASSWORD
        every { authRepository.observeUser(any()) } returns currentUserFlow
        coEvery { placeRepository.getTopPlaces(any()) } returns OpResult.success(emptyList())
        coEvery { authRepository.getTopUsers(any()) } returns OpResult.success(emptyList())
        coEvery { notificationPrefsUseCase.load() } returns NotificationPrefs()
        coEvery { notificationPrefsUseCase.save(any()) } returns true
    }

    private fun kotlinx.coroutines.test.TestScope.createAndObserve(): ProfileViewModel {
        val vm = ProfileViewModel(
            authRepository,
            placeRepository,
            badgePreferences,
            notificationPrefsUseCase,
            languagePreferences,
            functions
        )
        // Activate flows
        backgroundScope.launch { vm.uiState.collect {} }
        backgroundScope.launch { vm.user.collect {} }
        return vm
    }

    // =========================================================================
    // Initialization
    // =========================================================================

    @Nested
    @DisplayName("Initialization")
    inner class Initialization {

        @Test
        fun `initial UiState has correct defaults`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isEditOpen)
            assertFalse(state.isSaving)
            assertNull(state.saveError)
            assertFalse(state.isPrivacyPolicyOpen)
            assertFalse(state.isChangePasswordOpen)
            assertFalse(state.isChangeEmailOpen)
            assertFalse(state.isDeleteAccountOpen)
            assertFalse(state.isAccountActionInProgress)
            assertNull(state.accountActionError)
            assertNull(state.accountActionInfo)
        }

        @Test
        fun `fetches sign in provider on init`() = runTest {
            currentUserFlow.value = TestFixtures.user()
            coEvery { authRepository.getCurrentSignInProvider() } returns SignInProvider.GOOGLE

            viewModel = createAndObserve()
            advanceUntilIdle()

            assertEquals(SignInProvider.GOOGLE, viewModel.uiState.value.signInProvider)
        }

        @Test
        fun `user flow emits null when no user logged in`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            assertNull(viewModel.user.value)
        }

        @Test
        fun `user flow emits user data when logged in`() = runTest {
            val testUser = TestFixtures.user(id = "uid-1", name = "Jan")

            viewModel = createAndObserve()
            advanceUntilIdle()

            currentUserFlow.value = testUser
            advanceUntilIdle()

            assertEquals("uid-1", viewModel.user.value?.id)
            assertEquals("Jan", viewModel.user.value?.name)
        }
    }

    // =========================================================================
    // Edit profile sheet
    // =========================================================================

    @Nested
    @DisplayName("Edit profile")
    inner class EditProfile {

        @Test
        fun `openEditSheet sets isEditOpen to true`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openEditSheet()
            assertTrue(viewModel.uiState.value.isEditOpen)
            assertNull(viewModel.uiState.value.saveError)
        }

        @Test
        fun `dismissEditSheet sets isEditOpen to false`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openEditSheet()
            viewModel.dismissEditSheet()
            assertFalse(viewModel.uiState.value.isEditOpen)
        }

        @Test
        fun `saveProfile uploads avatar when new URI provided`() = runTest {
            val testUser = TestFixtures.user(id = "uid-1")
            currentUserFlow.value = testUser
            coEvery { authRepository.uploadAvatar(any()) } returns
                OpResult.success("http://new-avatar.url")
            coEvery { authRepository.updateUserProfile(any(), any(), any(), any()) } returns
                OpResult.success(testUser)

            viewModel = createAndObserve()
            advanceUntilIdle()

            val mockUri = mockk<Uri>()
            viewModel.saveProfile("Jan", "Jan", "Kowalski", mockUri)
            advanceUntilIdle()

            coVerify { authRepository.uploadAvatar(mockUri) }
            coVerify {
                authRepository.updateUserProfile("Jan", "Jan", "Kowalski", "http://new-avatar.url")
            }
        }

        @Test
        fun `saveProfile skips upload when no new avatar`() = runTest {
            val testUser = TestFixtures.user(id = "uid-1", avatarUrl = "http://existing.url")
            currentUserFlow.value = testUser
            coEvery { authRepository.updateUserProfile(any(), any(), any(), any()) } returns
                OpResult.success(testUser)

            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.saveProfile("Jan", "Jan", "Kowalski", null)
            advanceUntilIdle()

            coVerify(exactly = 0) { authRepository.uploadAvatar(any()) }
            coVerify {
                authRepository.updateUserProfile("Jan", "Jan", "Kowalski", "http://existing.url")
            }
        }

        @Test
        fun `saveProfile shows error on avatar upload failure`() = runTest {
            val testUser = TestFixtures.user(id = "uid-1")
            currentUserFlow.value = testUser
            coEvery { authRepository.uploadAvatar(any()) } returns
                OpResult.failure(RuntimeException("Storage full"))

            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.saveProfile("Jan", "Jan", "Kowalski", mockk())
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isSaving)
            assertTrue(state.saveError is UiText.StringResource)
            assertEquals(R.string.error_upload_failed, (state.saveError as UiText.StringResource).resId)
        }

        @Test
        fun `saveProfile shows error on profile update failure`() = runTest {
            val testUser = TestFixtures.user(id = "uid-1")
            currentUserFlow.value = testUser
            coEvery { authRepository.updateUserProfile(any(), any(), any(), any()) } returns
                OpResult.failure(AuthException.UsernameAlreadyTaken)

            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openEditSheet()
            viewModel.saveProfile("ExistingUser", "Jan", "Kowalski", null)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isSaving)
            assertNotNull(state.saveError)
            assertTrue(state.saveError is UiText.StringResource)
            assertEquals(R.string.error_username_taken, (state.saveError as UiText.StringResource).resId)
        }

        @Test
        fun `saveProfile closes sheet on success`() = runTest {
            val testUser = TestFixtures.user(id = "uid-1")
            currentUserFlow.value = testUser
            coEvery { authRepository.updateUserProfile(any(), any(), any(), any()) } returns
                OpResult.success(testUser)

            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openEditSheet()
            viewModel.saveProfile("Jan", "Jan", "Kowalski", null)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isEditOpen)
            assertFalse(state.isSaving)
            assertNull(state.saveError)
        }
    }

    // =========================================================================
    // Change password
    // =========================================================================

    @Nested
    @DisplayName("changePassword()")
    inner class ChangePassword {

        @Test
        fun `openChangePassword opens dialog`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openChangePassword()
            assertTrue(viewModel.uiState.value.isChangePasswordOpen)
            assertNull(viewModel.uiState.value.accountActionError)
        }

        @Test
        fun `dismissChangePassword closes dialog`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openChangePassword()
            viewModel.dismissChangePassword()
            assertFalse(viewModel.uiState.value.isChangePasswordOpen)
        }

        @Test
        fun `successful password change closes dialog and shows info`() = runTest {
            coEvery { authRepository.changePassword(any(), any()) } returns OpResult.success(Unit)

            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openChangePassword()
            viewModel.changePassword("oldPass", "NewP@ss123")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isChangePasswordOpen)
            assertFalse(state.isAccountActionInProgress)
            assertTrue(state.accountActionInfo is UiText.StringResource)
            assertEquals(R.string.password_changed, (state.accountActionInfo as UiText.StringResource).resId)
        }

        @Test
        fun `failed password change shows error in dialog`() = runTest {
            coEvery { authRepository.changePassword(any(), any()) } returns
                OpResult.failure(AuthException.InvalidCredentials)

            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openChangePassword()
            viewModel.changePassword("wrongOldPass", "NewP@ss123")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isChangePasswordOpen) // dialog stays open
            assertFalse(state.isAccountActionInProgress)
            assertNotNull(state.accountActionError)
        }
    }

    // =========================================================================
    // Change email
    // =========================================================================

    @Nested
    @DisplayName("changeEmail()")
    inner class ChangeEmail {

        @Test
        fun `successful email change closes dialog and shows verification info`() = runTest {
            coEvery { authRepository.changeEmail(any(), any()) } returns OpResult.success(Unit)

            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openChangeEmail()
            viewModel.changeEmail("currentPass", "new@email.com")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isChangeEmailOpen)
            assertTrue(state.accountActionInfo is UiText.StringResource)
            assertEquals(
                R.string.change_email_verification_sent,
                (state.accountActionInfo as UiText.StringResource).resId
            )
        }

        @Test
        fun `failed email change keeps dialog open with error`() = runTest {
            coEvery { authRepository.changeEmail(any(), any()) } returns
                OpResult.failure(AuthException.EmailAlreadyInUse)

            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openChangeEmail()
            viewModel.changeEmail("currentPass", "existing@email.com")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isChangeEmailOpen)
            assertNotNull(state.accountActionError)
        }
    }

    // =========================================================================
    // Delete account
    // =========================================================================

    @Nested
    @DisplayName("deleteAccount()")
    inner class DeleteAccount {

        @Test
        fun `successful deletion calls onDeleted callback`() = runTest {
            coEvery { authRepository.deleteAccount(any()) } returns OpResult.success(Unit)

            viewModel = createAndObserve()
            advanceUntilIdle()

            var deletedCalled = false
            viewModel.openDeleteAccount()
            viewModel.deleteAccount("currentPass") { deletedCalled = true }
            advanceUntilIdle()

            assertTrue(deletedCalled)
            assertFalse(viewModel.uiState.value.isDeleteAccountOpen)
        }

        @Test
        fun `failed deletion shows error and does not call callback`() = runTest {
            coEvery { authRepository.deleteAccount(any()) } returns
                OpResult.failure(AuthException.InvalidCredentials)

            viewModel = createAndObserve()
            advanceUntilIdle()

            var deletedCalled = false
            viewModel.openDeleteAccount()
            viewModel.deleteAccount("wrongPass") { deletedCalled = true }
            advanceUntilIdle()

            assertFalse(deletedCalled)
            assertTrue(viewModel.uiState.value.isDeleteAccountOpen)
            assertNotNull(viewModel.uiState.value.accountActionError)
        }

        @Test
        fun `deleteAccountGoogle calls repository with token`() = runTest {
            coEvery { authRepository.deleteAccountWithGoogle(any()) } returns OpResult.success(Unit)

            viewModel = createAndObserve()
            advanceUntilIdle()

            var deletedCalled = false
            viewModel.deleteAccountGoogle("google-id-token") { deletedCalled = true }
            advanceUntilIdle()

            assertTrue(deletedCalled)
            coVerify { authRepository.deleteAccountWithGoogle("google-id-token") }
        }
    }

    // =========================================================================
    // Sign out
    // =========================================================================

    @Nested
    @DisplayName("signOut()")
    inner class SignOut {

        @Test
        fun `signOut calls repository and invokes callback`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            var signedOut = false
            viewModel.signOut { signedOut = true }
            advanceUntilIdle()

            assertTrue(signedOut)
            coVerify { authRepository.signOut() }
        }
    }

    // =========================================================================
    // Dialogs
    // =========================================================================

    @Nested
    @DisplayName("Dialog state management")
    inner class Dialogs {

        @Test
        fun `privacy policy dialog opens and closes`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openPrivacyPolicy()
            assertTrue(viewModel.uiState.value.isPrivacyPolicyOpen)

            viewModel.dismissPrivacyPolicy()
            assertFalse(viewModel.uiState.value.isPrivacyPolicyOpen)
        }

        @Test
        fun `terms of service dialog opens and closes`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openTermsOfService()
            assertTrue(viewModel.uiState.value.isTermsOfServiceOpen)

            viewModel.dismissTermsOfService()
            assertFalse(viewModel.uiState.value.isTermsOfServiceOpen)
        }

        @Test
        fun `badges info dialog opens and closes`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openBadgesInfo()
            assertTrue(viewModel.uiState.value.isBadgesInfoOpen)

            viewModel.dismissBadgesInfo()
            assertFalse(viewModel.uiState.value.isBadgesInfoOpen)
        }

        @Test
        fun `new badge dialog stays visible until user dismisses it`() = runTest {
            val userWithBadge = TestFixtures.user(id = "uid-1", placesAddedCount = 1)
            every { badgePreferences.getSeenBadges("uid-1") } returns emptySet()

            viewModel = createAndObserve()
            advanceUntilIdle()

            currentUserFlow.value = userWithBadge
            advanceUntilIdle()

            assertEquals(
                listOf(UserBadge.FIRST_PLACE),
                viewModel.uiState.value.newlyEarnedBadges
            )
            verify(exactly = 0) { badgePreferences.setSeenBadges("uid-1", any()) }

            currentUserFlow.value = userWithBadge.copy(name = "Jan po odświeżeniu")
            advanceUntilIdle()

            assertEquals(
                listOf(UserBadge.FIRST_PLACE),
                viewModel.uiState.value.newlyEarnedBadges
            )
            verify(exactly = 0) { badgePreferences.setSeenBadges("uid-1", any()) }
            coVerify(exactly = 1) { authRepository.recordBadgesEarned(listOf("FIRST_PLACE")) }

            viewModel.consumeNewlyEarnedBadge()

            assertTrue(viewModel.uiState.value.newlyEarnedBadges.isEmpty())
            verify { badgePreferences.setSeenBadges("uid-1", setOf("FIRST_PLACE")) }
        }

        @Test
        fun `revokes badges that are no longer earned`() = runTest {
            every { badgePreferences.getSeenBadges("uid-1") } returns setOf("FIRST_PLACE")

            viewModel = createAndObserve()
            advanceUntilIdle()

            currentUserFlow.value = TestFixtures.user(id = "uid-1", placesAddedCount = 0, reviewsCount = 0)
            advanceUntilIdle()

            coVerify { authRepository.revokeBadges(listOf("FIRST_PLACE")) }
            verify { badgePreferences.setSeenBadges("uid-1", emptySet()) }
        }

        @Test
        fun `language dialog saves selected language`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openLanguageDialog()
            assertTrue(viewModel.uiState.value.isLanguageDialogOpen)

            viewModel.saveLanguage(AppLanguage.ENGLISH)

            assertFalse(viewModel.uiState.value.isLanguageDialogOpen)
            assertEquals(AppLanguage.ENGLISH, viewModel.uiState.value.selectedLanguage)
            verify { languagePreferences.setLanguage(AppLanguage.ENGLISH) }
        }

        @Test
        fun `openNotificationPrefs loads persisted preferences`() = runTest {
            val prefs = NotificationPrefs(
                newReviewOnMyPlace = false,
                newBadgeEarned = true,
                newPhotoOnMyPlace = false,
                rankings = true,
                emailNotificationsEnabled = false
            )
            coEvery { notificationPrefsUseCase.load() } returns prefs

            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openNotificationPrefs()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isNotificationPrefsOpen)
            assertEquals(prefs, viewModel.uiState.value.notificationPrefs)
            coVerify { notificationPrefsUseCase.load() }
        }

        @Test
        fun `saveNotificationPrefs persists preferences and closes dialog`() = runTest {
            val prefs = NotificationPrefs(
                newReviewOnMyPlace = false,
                newBadgeEarned = false,
                newPhotoOnMyPlace = true,
                rankings = false,
                emailNotificationsEnabled = false
            )

            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.openNotificationPrefs()
            viewModel.saveNotificationPrefs(prefs)
            advanceUntilIdle()

            coVerify { notificationPrefsUseCase.save(prefs) }
            assertFalse(viewModel.uiState.value.isNotificationPrefsOpen)
            assertFalse(viewModel.uiState.value.isAccountActionInProgress)
            assertEquals(prefs, viewModel.uiState.value.notificationPrefs)
        }

        @Test
        fun `consumeAccountActionInfo clears info`() = runTest {
            coEvery { authRepository.changePassword(any(), any()) } returns OpResult.success(Unit)

            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.changePassword("old", "NewP@ss1!")
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.accountActionInfo)

            viewModel.consumeAccountActionInfo()
            assertNull(viewModel.uiState.value.accountActionInfo)
        }
    }

    // =========================================================================
    // Refresh
    // =========================================================================

    @Nested
    @DisplayName("refreshProfile()")
    inner class Refresh {

        @Test
        fun `refreshProfile does nothing when user is null`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            // user is null by default
            viewModel.refreshProfile()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRefreshing)
        }

        @Test
        fun `refreshProfile sets isRefreshing`() = runTest {
            val testUser = TestFixtures.user(id = "uid-1", placesAddedCount = 5)
            currentUserFlow.value = testUser

            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.refreshProfile()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRefreshing)
            coVerify { authRepository.refreshUser() }
        }
    }
}
