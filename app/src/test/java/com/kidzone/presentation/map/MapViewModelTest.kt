package com.kidzone.presentation.map

import com.kidzone.domain.model.GeoBounds
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.testutil.MainDispatcherRule
import com.kidzone.testutil.TestFixtures
import com.kidzone.utils.OpResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())
    }

    private lateinit var placeRepository: PlaceRepository
    private lateinit var authRepository: AuthRepository

    private val warsaw = GeoBounds(
        north = 52.35,
        east = 21.20,
        south = 52.10,
        west = 20.80
    )
    private val krakow = GeoBounds(
        north = 50.15,
        east = 20.10,
        south = 49.95,
        west = 19.75
    )

    @BeforeEach
    fun setUp() {
        placeRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        every { authRepository.currentUser } returns MutableStateFlow(null)
        coEvery {
            placeRepository.getPlacesInBounds(any(), any(), any())
        } returns OpResult.success(emptyList())
    }

    @Test
    fun `camera changes fetch only final viewport after debounce`() = runTest {
        val viewModel = createAndObserve()

        viewModel.onViewportChanged(warsaw)
        advanceTimeBy(200)
        viewModel.onViewportChanged(krakow)
        advanceTimeBy(500)
        advanceUntilIdle()

        coVerify(exactly = 0) {
            placeRepository.getPlacesInBounds(warsaw, null, 200)
        }
        coVerify(exactly = 1) {
            placeRepository.getPlacesInBounds(krakow, null, 200)
        }
    }

    @Test
    fun `returning to cached viewport avoids duplicate request`() = runTest {
        val viewModel = createAndObserve()

        viewModel.onViewportChanged(warsaw)
        advanceTimeBy(500)
        advanceUntilIdle()
        viewModel.onViewportChanged(krakow)
        advanceTimeBy(500)
        advanceUntilIdle()
        viewModel.onViewportChanged(warsaw)
        advanceTimeBy(500)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            placeRepository.getPlacesInBounds(warsaw, null, 200)
        }
    }

    @Test
    fun `category change refetches current viewport with server filter`() = runTest {
        val playground = TestFixtures.place(
            id = "playground",
            category = PlaceCategory.PLAYGROUND
        )
        coEvery {
            placeRepository.getPlacesInBounds(warsaw, PlaceCategory.PLAYGROUND, 200)
        } returns OpResult.success(listOf(playground))
        val viewModel = createAndObserve()

        viewModel.onViewportChanged(warsaw)
        advanceTimeBy(500)
        advanceUntilIdle()
        viewModel.onCategorySelected(PlaceCategory.PLAYGROUND)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            placeRepository.getPlacesInBounds(warsaw, PlaceCategory.PLAYGROUND, 200)
        }
        assertEquals(listOf(playground), viewModel.uiState.value.places)
    }

    private fun kotlinx.coroutines.test.TestScope.createAndObserve(): MapViewModel {
        val viewModel = MapViewModel(placeRepository, authRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }
}
