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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
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
import kotlinx.coroutines.launch

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

    private fun createViewModel(): ProfileViewModel {
        val vm = ProfileViewModel(
            authRepository,
            placeRepository,
            badgePreferences,
            notificationPrefsUseCase,
            languagePreferences,
            functions
        )
        // Background collection to keep StateFlow active
        mainDispatcherRule.testDispatcher.scheduler.run {
            // No-op collection
        }
        return vm
    }

    @Nested
    @DisplayName("Initialization & Loading")
    inner class Initialization {

        @Test
        fun `initial state is Loading`() = runTest {
            viewModel = createViewModel()
            assertEquals(ScreenState.Loading, viewModel.profileState.value)
        }

        @Test
        fun `loads profile successfully from repository`() = runTest {
            val testUser = TestFixtures.user(id = "uid-1", name = "Jan")
            currentUserFlow.value = testUser
            observeUserFlow.value = testUser

            viewModel = createViewModel()
            
            // Ensure flows are processed
            backgroundScope.launch { viewModel.profileState.collect {} }
            runCurrent()

            assertTrue(viewModel.profileState.value is ScreenState.Content)
            assertEquals("uid-1", (viewModel.profileState.value as ScreenState.Content).data.id)
        }

        @Test
        fun `missing profile is exposed as error`() = runTest {
            val testUser = TestFixtures.user(id = "uid-missing")
            currentUserFlow.value = testUser
            every { authRepository.observeUser(testUser.id) } returns flowOf(null)
            
            viewModel = createViewModel()
            backgroundScope.launch { viewModel.profileState.collect {} }
            runCurrent()

            assertTrue(viewModel.profileState.value is ScreenState.Error)
        }

        @Test
        fun `retry transitions profile from error to content`() = runTest {
            val testUser = TestFixtures.user(id = "uid-retry")
            currentUserFlow.value = testUser
            
            // 1. Setup failure
            every { authRepository.observeUser(testUser.id) } returns flow { throw IllegalStateException("fail") }
            
            viewModel = createViewModel()
            backgroundScope.launch { viewModel.profileState.collect {} }
            runCurrent()
            
            assertTrue(viewModel.profileState.value is ScreenState.Error)

            // 2. Setup success
            every { authRepository.observeUser(testUser.id) } returns flowOf(testUser)
            
            viewModel.retryProfile()
            runCurrent()
            
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

            viewModel = createViewModel()
            backgroundScope.launch { viewModel.profileState.collect {} }
            backgroundScope.launch { viewModel.uiState.collect {} }
            
            // Wait for INITIAL_BADGE_COLLECTION_DELAY_MS (1500ms)
            advanceTimeBy(1600)
            
            assertEquals(listOf(UserBadge.FIRST_PLACE), viewModel.uiState.value.newlyEarnedBadges)
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

            viewModel = createViewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            viewModel.refreshProfile()
            assertTrue(viewModel.uiState.value.isRefreshing)
            
            // Advance time for REFRESH_DELAY_MS (300ms)
            advanceTimeBy(301)
            assertFalse(viewModel.uiState.value.isRefreshing)
            coVerify { authRepository.refreshUser() }
        }
    }
}
