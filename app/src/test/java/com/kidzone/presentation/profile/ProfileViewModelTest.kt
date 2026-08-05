@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kidzone.presentation.profile

import com.kidzone.R
import com.kidzone.testutil.TestFixtures
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.SignInProvider
import com.kidzone.domain.service.BadgePreferences
import com.kidzone.domain.usecase.NotificationPrefsUseCase
import com.kidzone.i18n.AppLanguage
import com.kidzone.i18n.LanguagePreferences
import com.kidzone.presentation.common.ScreenState
import com.kidzone.presentation.common.UserBadge
import com.kidzone.utils.OpResult
import com.kidzone.testutil.MainDispatcherRule
import com.google.firebase.functions.FirebaseFunctions
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlinx.coroutines.awaitCancellation

class ProfileViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var viewModel: ProfileViewModel
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val placeRepository = mockk<PlaceRepository>(relaxed = true)
    private val badgePreferences = mockk<BadgePreferences>(relaxed = true)
    private val notificationPrefsUseCase = mockk<NotificationPrefsUseCase>(relaxed = true)
    private val languagePreferences = mockk<LanguagePreferences>(relaxed = true)
    private val functions = mockk<FirebaseFunctions>(relaxed = true)

    private val currentUserFlow = MutableStateFlow<User?>(null)
    private val observeUserFlow = MutableStateFlow<User?>(null)

    @BeforeEach
    fun setUp() {
        every { authRepository.currentUser } returns currentUserFlow
        every { authRepository.observeUser(any()) } returns observeUserFlow
        coEvery { authRepository.getCurrentSignInProvider() } returns SignInProvider.EMAIL_PASSWORD
        every { languagePreferences.getLanguage() } returns AppLanguage.POLISH
        
        coEvery { placeRepository.getTopPlaces(any()) } returns OpResult.success(emptyList())
        coEvery { authRepository.getTopUsers(any()) } returns OpResult.success(emptyList())
    }

    private fun createAndObserve(): ProfileViewModel {
        return ProfileViewModel(
            authRepository,
            placeRepository,
            badgePreferences,
            notificationPrefsUseCase,
            languagePreferences,
            functions
        )
    }

    @Nested
    @DisplayName("Initialization & Loading")
    inner class Initialization {

        @Test
        fun `initial state is Loading`() = runTest {
            viewModel = createAndObserve()
            assertTrue(viewModel.profileState.value is ScreenState.Loading)
        }

        @Test
        fun `loads profile successfully from repository`() = runTest {
            val testUser = TestFixtures.user(id = "uid-1", name = "Jan")

            viewModel = createAndObserve()
            advanceUntilIdle()

            currentUserFlow.value = testUser
            observeUserFlow.value = testUser
            advanceUntilIdle()

            assertEquals("uid-1", viewModel.user.value?.id)
            assertTrue(viewModel.profileState.value is ScreenState.Content)
        }

        @Test
        fun `missing profile is exposed as error`() = runTest {
            val testUser = TestFixtures.user(id = "uid-missing")
            currentUserFlow.value = testUser
            observeUserFlow.value = null
            
            viewModel = createAndObserve()
            advanceUntilIdle()

            assertTrue(viewModel.profileState.value is ScreenState.Error)
        }

        @Test
        fun `retry transitions profile from error to content`() = runTest {
            val testUser = TestFixtures.user(id = "uid-retry")
            currentUserFlow.value = testUser
            
            every { authRepository.observeUser(testUser.id) } returns flow { throw IllegalStateException() }
            
            viewModel = createAndObserve()
            advanceUntilIdle()
            assertTrue(viewModel.profileState.value is ScreenState.Error)

            every { authRepository.observeUser(testUser.id) } returns flowOf(testUser)
            viewModel.retryProfile()
            
            // Przejście przez Loading
            advanceTimeBy(1)
            assertTrue(viewModel.profileState.value is ScreenState.Loading)
            
            advanceUntilIdle()
            assertTrue(viewModel.profileState.value is ScreenState.Content)
        }
    }

    @Nested
    @DisplayName("Badges Logic")
    inner class Badges {

        @Test
        fun `detects and displays newly earned badges`() = runTest {
            val testUser = TestFixtures.user(id = "uid-1", placesAddedCount = 1)
            currentUserFlow.value = testUser
            observeUserFlow.value = testUser
            every { badgePreferences.getSeenBadges("uid-1") } returns emptySet()

            viewModel = createAndObserve()
            // INITIAL_BADGE_COLLECTION_DELAY_MS is 1500ms
            advanceTimeBy(1600)
            
            assertEquals(listOf(UserBadge.FIRST_PLACE), viewModel.uiState.value.newlyEarnedBadges)
        }
        
        @Test
        fun `shows summary of historical badges on first load`() = runTest {
            val testUser = TestFixtures.user(
                id = "uid-1", 
                placesAddedCount = 1
            ).copy(badgeEarnedAt = mapOf("FIRST_PLACE" to 123456L))
            
            currentUserFlow.value = testUser
            observeUserFlow.value = testUser
            every { badgePreferences.getSeenBadges("uid-1") } returns emptySet()

            viewModel = createAndObserve()
            advanceTimeBy(1600)

            // Should show summary (FIRST_PLACE was historical)
            assertEquals(listOf(UserBadge.FIRST_PLACE), viewModel.uiState.value.newlyEarnedBadges)
            // No auto-save to preferences before user clicks Super
            verify(exactly = 0) { badgePreferences.setSeenBadges("uid-1", any()) }
        }
    }

    @Nested
    @DisplayName("Dialogs & Actions")
    inner class Dialogs {

        @Test
        fun `consumeNewlyEarnedBadge saves to preferences and clears state`() = runTest {
            val testUser = TestFixtures.user(id = "uid-1", placesAddedCount = 1)
            currentUserFlow.value = testUser
            observeUserFlow.value = testUser
            every { badgePreferences.getSeenBadges("uid-1") } returns emptySet()

            viewModel = createAndObserve()
            advanceTimeBy(1600)

            assertEquals(listOf(UserBadge.FIRST_PLACE), viewModel.uiState.value.newlyEarnedBadges)
            
            viewModel.consumeNewlyEarnedBadge()

            assertTrue(viewModel.uiState.value.newlyEarnedBadges.isEmpty())
            verify { badgePreferences.setSeenBadges("uid-1", setOf("FIRST_PLACE")) }
        }

        @Test
        fun `revokes badges that are no longer earned`() = runTest {
            val testUser = TestFixtures.user(id = "uid-1", placesAddedCount = 0)
            currentUserFlow.value = testUser
            observeUserFlow.value = testUser
            every { badgePreferences.getSeenBadges("uid-1") } returns setOf("FIRST_PLACE")

            viewModel = createAndObserve()
            advanceUntilIdle()

            coVerify { authRepository.revokeBadges(listOf("FIRST_PLACE")) }
            verify { badgePreferences.setSeenBadges("uid-1", emptySet()) }
        }
    }

    @Nested
    @DisplayName("refreshProfile()")
    inner class Refresh {

        @Test
        fun `refreshProfile sets isRefreshing`() = runTest {
            val testUser = TestFixtures.user(id = "uid-1")
            currentUserFlow.value = testUser
            observeUserFlow.value = testUser

            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.refreshProfile()
            assertTrue(viewModel.uiState.value.isRefreshing)
            
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isRefreshing)
            coVerify { authRepository.refreshUser() }
        }

        @Test
        fun `retry shows loading while waiting after profile error`() = runTest {
            val testUser = TestFixtures.user(id = "uid-retry")
            currentUserFlow.value = testUser
            every { authRepository.observeUser(testUser.id) } returnsMany listOf(
                flow { throw IllegalStateException("offline") },
                flow { awaitCancellation() }
            )

            viewModel = createAndObserve()
            advanceUntilIdle()
            assertTrue(viewModel.profileState.value is ScreenState.Error)

            viewModel.retryProfile()
            
            advanceTimeBy(1)
            assertTrue(viewModel.profileState.value is ScreenState.Loading)
        }
    }
}
