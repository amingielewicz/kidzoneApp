package com.kidzone.presentation.map

import com.kidzone.domain.model.GeoBounds
import com.kidzone.data.remote.PerformanceConfig
import com.kidzone.data.remote.PerformanceConfigProvider
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

/**
 * 🧪 Cel testu:
 * - Weryfikacja reaktywnego ładowania miejsc na mapie przez [MapViewModel].
 * - Sprawdzenie poprawności filtrowania i optymalizacji zapytań (Viewport cache).
 *
 * 🛠️ Środowisko:
 * - Mockowanie warstwy danych ([PlaceRepository], [AuthRepository]).
 * - [MainDispatcherRule] do kontroli wirtualnego czasu (debounce).
 *
 * 🔍 Scenariusze:
 * - Ładowanie miejsc dla danych granic geograficznych.
 * - Efektywność cache'owania granic (brak powtórnych zapytań dla podobnych obszarów).
 * - Działanie filtrów kategorii i "tylko moje miejsca".
 * - Obsługa błędów pobierania z Firestore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())
        private const val LIMIT = 500 // Current default in MapViewModel
    }

    private lateinit var placeRepository: PlaceRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var performanceConfigProvider: PerformanceConfigProvider

    private val warsaw = GeoBounds(north = 52.35, east = 21.20, south = 52.10, west = 20.80)
    private val krakow = GeoBounds(north = 50.15, east = 20.10, south = 49.95, west = 19.75)

    @BeforeEach
    fun setUp() {
        placeRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        performanceConfigProvider = mockk(relaxed = true)
        every { authRepository.currentUser } returns MutableStateFlow(null)
        every { performanceConfigProvider.performanceConfig } returns
            PerformanceConfig(mapMarkersLimit = LIMIT)
        coEvery {
            placeRepository.getPlacesInBounds(any(), any(), any())
        } returns OpResult.success(emptyList())
    }

    @Test
    fun `first viewport fetches immediately`() = runTest {
        val viewModel = createAndObserve()
        viewModel.onViewportChanged(warsaw)
        advanceUntilIdle()
        coVerify(exactly = 1) { placeRepository.getPlacesInBounds(warsaw, null, LIMIT) }
    }

    @Test
    fun `subsequent camera changes fetch only final viewport after debounce`() = runTest {
        val viewModel = createAndObserve()
        viewModel.onViewportChanged(warsaw)
        advanceUntilIdle()

        viewModel.onViewportChanged(krakow)
        advanceTimeBy(100)
        viewModel.onViewportChanged(warsaw)
        advanceTimeBy(100)
        viewModel.onViewportChanged(krakow)
        advanceTimeBy(350) // More than debounce (300)
        advanceUntilIdle()

        coVerify(exactly = 1) { placeRepository.getPlacesInBounds(warsaw, null, LIMIT) }
        coVerify(exactly = 1) { placeRepository.getPlacesInBounds(krakow, null, LIMIT) }
    }

    @Test
    fun `category change refetches current viewport with server filter`() = runTest {
        val playground = TestFixtures.place(id = "playground", category = PlaceCategory.PLAYGROUND)
        coEvery {
            placeRepository.getPlacesInBounds(warsaw, PlaceCategory.PLAYGROUND, LIMIT)
        } returns OpResult.success(listOf(playground))

        val viewModel = createAndObserve()
        viewModel.onViewportChanged(warsaw)
        advanceUntilIdle()
        viewModel.onCategorySelect(PlaceCategory.PLAYGROUND)
        advanceUntilIdle()

        coVerify(exactly = 1) { placeRepository.getPlacesInBounds(warsaw, PlaceCategory.PLAYGROUND, LIMIT) }
        assertEquals(listOf(playground), viewModel.uiState.value.places)
    }

    @Test
    fun `retry refetches same viewport after failure`() = runTest {
        val place = TestFixtures.place(id = "retry-place")
        coEvery {
            placeRepository.getPlacesInBounds(warsaw, null, LIMIT)
        } returnsMany listOf(
            OpResult.failure(RuntimeException("network")),
            OpResult.success(listOf(place))
        )

        val viewModel = createAndObserve()
        viewModel.onViewportChanged(warsaw)
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        coVerify(exactly = 2) { placeRepository.getPlacesInBounds(warsaw, null, LIMIT) }
        assertEquals(listOf(place), viewModel.uiState.value.places)
    }

    private fun kotlinx.coroutines.test.TestScope.createAndObserve(): MapViewModel {
        val viewModel = MapViewModel(placeRepository, authRepository, performanceConfigProvider)
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }
}
