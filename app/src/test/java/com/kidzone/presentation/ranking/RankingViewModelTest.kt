package com.kidzone.presentation.ranking

import com.kidzone.R
import com.kidzone.data.remote.PerformanceConfig
import com.kidzone.data.remote.PerformanceConfigProvider
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.presentation.common.UserBadge
import com.kidzone.testutil.MainDispatcherRule
import com.kidzone.testutil.TestFixtures
import com.kidzone.utils.OpResult
import com.kidzone.utils.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * 🧪 Cel testu:
 * - Weryfikacja pobierania i prezentacji rankingów użytkowników oraz miejsc w [RankingViewModel].
 * - Sprawdzenie poprawności obliczania odznak w kontekście rankingowym.
 *
 * 🛠️ Środowisko:
 * - Mockowanie warstwy danych i konfiguracji wydajnościowej.
 * - [MainDispatcherRule] do synchronizacji operacji asynchronicznych.
 *
 * 🔍 Scenariusze:
 * - Sukces pobrania obu list (miejsca i użytkownicy).
 * - Obsługa błędów częściowych (np. błąd pobierania rankingu miejsc).
 * - Weryfikacja limitów wyników pobieranych z repozytoriów.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RankingViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var placeRepository: PlaceRepository
    private lateinit var authRepository: AuthRepository

    @BeforeEach
    fun setUp() {
        placeRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
    }

    @Test
    fun `loads active places and users using configured fetch pool and visible limit`() = runTest {
        coEvery { placeRepository.getTopPlaces(limit = 4) } returns OpResult.success(
            listOf(
                TestFixtures.place(id = "place-1", ownerUserId = "user-1", averageRating = 4.9, reviewsCount = 8),
                TestFixtures.place(id = "inactive-place", averageRating = 0.0, reviewsCount = 0),
                TestFixtures.place(id = "place-2", ownerUserId = "user-2", averageRating = 4.7, reviewsCount = 3),
                TestFixtures.place(id = "place-3", ownerUserId = "user-3", averageRating = 4.5, reviewsCount = 2)
            )
        )
        coEvery { authRepository.getTopUsers(limit = 4) } returns OpResult.success(
            listOf(
                TestFixtures.user(id = "user-1", placesAddedCount = 8, reviewsCount = 4),
                TestFixtures.user(id = "inactive-user", placesAddedCount = 0, reviewsCount = 0),
                TestFixtures.user(id = "user-2", placesAddedCount = 2, reviewsCount = 1),
                TestFixtures.user(id = "user-3", placesAddedCount = 1, reviewsCount = 1)
            )
        )

        val viewModel = createViewModel(
            PerformanceConfig(
                rankingTopLimit = 2,
                rankingFetchPool = 4
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { placeRepository.getTopPlaces(limit = 4) }
        coVerify(exactly = 1) { authRepository.getTopUsers(limit = 4) }
        assertEquals(listOf("place-1", "place-2"), viewModel.uiState.value.topPlaces.map { it.id })
        assertEquals(listOf("user-1", "user-2"), viewModel.uiState.value.topUsers.map { it.id })
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `sets error on failure and clears loading`() = runTest {
        coEvery { placeRepository.getTopPlaces(limit = 200) } returns
            OpResult.failure(RuntimeException("places failed"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(emptyList<Any>(), viewModel.uiState.value.topPlaces)
        assertTrue(viewModel.uiState.value.errorMessage is UiText.StringResource)
        assertEquals(R.string.error_fetch_list, (viewModel.uiState.value.errorMessage as UiText.StringResource).resId)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `computes ranking badges for top user and owner of top place`() = runTest {
        coEvery { placeRepository.getTopPlaces(limit = 200) } returns OpResult.success(
            listOf(
                TestFixtures.place(id = "place-1", ownerUserId = "user-1", averageRating = 5.0, reviewsCount = 10)
            )
        )
        coEvery { authRepository.getTopUsers(limit = 200) } returns OpResult.success(
            listOf(
                TestFixtures.user(id = "user-1", placesAddedCount = 1, reviewsCount = 1)
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val badges = viewModel.uiState.value.userBadges.getValue("user-1")
        assertTrue(UserBadge.LEADER_GOLD in badges)
        assertTrue(UserBadge.PLACE_TOP3 in badges)
        assertTrue(UserBadge.PLACE_TOP1 in badges)
    }

    private fun createViewModel(
        performanceConfig: PerformanceConfig = PerformanceConfig()
    ): RankingViewModel {
        val performanceConfigProvider = object : PerformanceConfigProvider {
            override val performanceConfig: PerformanceConfig = performanceConfig
        }
        return RankingViewModel(
            placeRepository = placeRepository,
            authRepository = authRepository,
            performanceConfigProvider = performanceConfigProvider
        )
    }
}
