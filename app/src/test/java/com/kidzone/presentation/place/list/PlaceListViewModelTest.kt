package com.kidzone.presentation.place.list

import android.content.Context
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.testutil.MainDispatcherRule
import com.kidzone.testutil.TestFixtures
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceListViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var placeRepository: PlaceRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var appContext: Context
    private lateinit var viewModel: PlaceListViewModel

    private val currentUserFlow = MutableStateFlow(TestFixtures.user(id = "user-1"))

    private val samplePlaces = listOf(
        TestFixtures.place(id = "p1", name = "Plac Zabaw A", category = PlaceCategory.PLAYGROUND,
            averageRating = 4.5, reviewsCount = 10, createdAtMillis = 1000L,
            latitude = 52.23, longitude = 21.01, amenities = setOf(Amenity.PARKING)),
        TestFixtures.place(id = "p2", name = "Restauracja B", category = PlaceCategory.RESTAURANT,
            averageRating = 3.0, reviewsCount = 5, createdAtMillis = 2000L,
            latitude = 52.24, longitude = 21.02, amenities = setOf(Amenity.TOILET)),
        TestFixtures.place(id = "p3", name = "Park C", category = PlaceCategory.PARK,
            averageRating = 5.0, reviewsCount = 20, createdAtMillis = 3000L,
            latitude = 52.25, longitude = 21.03, amenities = setOf(Amenity.PARKING, Amenity.TOILET)),
        TestFixtures.place(id = "p4", name = "Moje Miejsce", category = PlaceCategory.PLAYGROUND,
            ownerUserId = "user-1", averageRating = 0.0, reviewsCount = 0, createdAtMillis = 4000L,
            latitude = 52.26, longitude = 21.04)
    )

    @BeforeEach
    fun setUp() {
        placeRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        appContext = mockk(relaxed = true)

        every { authRepository.currentUser } returns currentUserFlow
        every { placeRepository.observePlaces(any(), any()) } answers {
            val cat = firstArg<PlaceCategory?>()
            val query = secondArg<String?>()
            flowOf(samplePlaces.filter { 
                (cat == null || it.category == cat) &&
                (query.isNullOrBlank() || it.name.contains(query, ignoreCase = true))
            })
        }
    }

    private fun createViewModel(): PlaceListViewModel {
        return PlaceListViewModel(placeRepository, authRepository, appContext)
    }

    private fun TestScope.startCollectingUiState() {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    // =========================================================================
    // Initial state
    // =========================================================================

    @Nested
    @DisplayName("Initial state & loading")
    inner class InitialState {

        @Test
        fun `initial state shows correct totals`() = runTest {
            viewModel = createViewModel()
            startCollectingUiState()
            advanceTimeBy(1000)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(4, state.totalCount)
        }

        @Test
        fun `default sort is NEAREST but falls back to RECENTLY_ADDED without location`() = runTest {
            viewModel = createViewModel()
            startCollectingUiState()
            advanceTimeBy(1000)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(PlaceListViewModel.SortOrder.NEAREST, state.sortOrder)
            assertTrue(state.nearestUnavailable)
            // Without location, should sort by createdAtMillis desc (fallback)
            assertEquals("p4", state.places.first().id) // newest first
        }
    }

    // =========================================================================
    // Category filter
    // =========================================================================

    @Nested
    @DisplayName("Category filtering")
    inner class CategoryFilter {

        @Test
        fun `filtering by category shows only matching places`() = runTest {
            viewModel = createViewModel()
            startCollectingUiState()
            advanceTimeBy(1000)
            advanceUntilIdle()

            viewModel.onCategorySelected(PlaceCategory.PLAYGROUND)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(PlaceCategory.PLAYGROUND, state.selectedCategory)
            assertTrue(state.places.all { it.category == PlaceCategory.PLAYGROUND })
        }

        @Test
        fun `clearing category shows all places`() = runTest {
            viewModel = createViewModel()
            startCollectingUiState()
            advanceTimeBy(1000)
            advanceUntilIdle()

            viewModel.onCategorySelected(PlaceCategory.RESTAURANT)
            advanceUntilIdle()
            viewModel.onCategorySelected(null)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.selectedCategory)
            assertEquals(4, viewModel.uiState.value.totalCount)
        }
    }

    // =========================================================================
    // Amenity filter
    // =========================================================================

    @Nested
    @DisplayName("Amenity filtering")
    inner class AmenityFilter {

        @Test
        fun `filtering by amenity shows only places with that amenity`() = runTest {
            viewModel = createViewModel()
            startCollectingUiState()
            advanceTimeBy(1000)
            advanceUntilIdle()

            viewModel.onAmenityToggled(Amenity.PARKING)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(Amenity.PARKING in state.selectedAmenities)
            assertTrue(state.places.all { Amenity.PARKING in it.amenities })
            assertEquals(2, state.totalCount)
        }

        @Test
        fun `multiple amenities use AND logic`() = runTest {
            viewModel = createViewModel()
            startCollectingUiState()
            advanceTimeBy(1000)
            advanceUntilIdle()

            viewModel.onAmenityToggled(Amenity.PARKING)
            viewModel.onAmenityToggled(Amenity.TOILET)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.totalCount)
            assertEquals("p3", state.places.first().id)
        }
    }

    // =========================================================================
    // Sorting
    // =========================================================================

    @Nested
    @DisplayName("Sorting")
    inner class Sorting {

        @Test
        fun `RECENTLY_ADDED sorts by createdAtMillis desc`() = runTest {
            viewModel = createViewModel()
            startCollectingUiState()
            advanceTimeBy(1000)
            advanceUntilIdle()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.RECENTLY_ADDED)
            advanceUntilIdle()

            val places = viewModel.uiState.value.places
            assertEquals("p4", places[0].id) // 4000L
            assertEquals("p3", places[1].id) // 3000L
        }

        @Test
        fun `BEST_RATED sorts by averageRating desc`() = runTest {
            viewModel = createViewModel()
            startCollectingUiState()
            advanceTimeBy(1000)
            advanceUntilIdle()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.BEST_RATED)
            advanceUntilIdle()

            val places = viewModel.uiState.value.places
            assertEquals("p3", places[0].id) // 5.0
            assertEquals("p1", places[1].id) // 4.5
        }
    }

    // =========================================================================
    // Search
    // =========================================================================

    @Nested
    @DisplayName("Search")
    inner class Search {

        @Test
        fun `search filters by name case-insensitive`() = runTest {
            viewModel = createViewModel()
            startCollectingUiState()
            advanceTimeBy(1000)
            advanceUntilIdle()

            viewModel.onSearchQueryChange("restauracja")
            advanceTimeBy(500)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.totalCount)
            assertEquals("p2", state.places.first().id)
            assertEquals("restauracja", state.searchQuery)
        }

        @Test
        fun `empty search shows all places`() = runTest {
            viewModel = createViewModel()
            startCollectingUiState()
            advanceTimeBy(1000)
            advanceUntilIdle()

            viewModel.onSearchQueryChange("restauracja")
            advanceTimeBy(500)
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.totalCount)

            viewModel.onSearchQueryChange("")
            advanceUntilIdle()
            assertEquals(4, viewModel.uiState.value.totalCount)
        }
    }
}
