package com.kidzone.presentation.map

import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.testutil.MainDispatcherRule
import com.kidzone.testutil.TestFixtures
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var placeRepository: PlaceRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: MapViewModel

    private val samplePlaces = listOf(
        TestFixtures.place(id = "p1", name = "Plac A", category = PlaceCategory.PLAYGROUND, averageRating = 4.5),
        TestFixtures.place(id = "p2", name = "Kawiarnia B", category = PlaceCategory.RESTAURANT, averageRating = 3.5),
        TestFixtures.place(id = "p3", name = "Park C", category = PlaceCategory.PARK, averageRating = 5.0)
    )

    @BeforeEach
    fun setUp() {
        placeRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)

        every { authRepository.currentUser } returns MutableStateFlow(TestFixtures.user(id = "user-1"))
        every { placeRepository.observePlaces(any(), any()) } answers {
            val cat = firstArg<PlaceCategory?>()
            val query = secondArg<String?>()
            flowOf(samplePlaces.filter { 
                (cat == null || it.category == cat) &&
                (query.isNullOrBlank() || it.name.contains(query, ignoreCase = true))
            })
        }
        viewModel = MapViewModel(placeRepository, authRepository)
    }

    private fun TestScope.startCollectingUiState() {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    @Test
    fun `initial state loads all places`() = runTest {
        startCollectingUiState()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.places.size)
    }

    @Test
    fun `searching by name filters places with debounce`() = runTest {
        startCollectingUiState()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Plac")
        
        // Before debounce time
        advanceTimeBy(100)
        assertEquals(3, viewModel.uiState.value.places.size)

        // After debounce time
        advanceTimeBy(300)
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(1, state.places.size)
        assertEquals("p1", state.places.first().id)
    }

    @Test
    fun `filtering by category works together with search`() = runTest {
        startCollectingUiState()
        advanceUntilIdle()

        viewModel.onCategorySelected(PlaceCategory.PLAYGROUND)
        viewModel.onSearchQueryChange("Plac")
        advanceTimeBy(400)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.places.size)
        assertEquals("p1", state.places.first().id)

        // Change category to something that doesn't match the search
        viewModel.onCategorySelected(PlaceCategory.PARK)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.places.isEmpty())
    }

    @Test
    fun `top rated filter works on the client side`() = runTest {
        startCollectingUiState()
        advanceUntilIdle()

        viewModel.toggleTopRated() // Threshold is 4.0
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.places.size) // p1 (4.5) and p3 (5.0)
        assertTrue(state.places.all { it.averageRating >= 4.0 })
    }
}
