package com.kidzone.presentation.place.list

import android.content.Context
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.testutil.MainDispatcherRule
import com.kidzone.testutil.TestFixtures
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
        val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())
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
        // observePlaces now takes (category, query) - mock both params
        every { placeRepository.observePlaces(any(), any()) } returns flowOf(samplePlaces)
    }

    private fun createViewModel(): PlaceListViewModel {
        val vm = PlaceListViewModel(placeRepository, authRepository, appContext)
        return vm
    }

    // =========================================================================
    // Initial state
    // =========================================================================

    @Nested
    @DisplayName("Initial state & loading")
    inner class InitialState {

        @Test
        fun `loads all places after initialization`() = runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.places.isNotEmpty())
        }

        @Test
        fun `default sort is NEAREST`() = runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(PlaceListViewModel.SortOrder.NEAREST, viewModel.uiState.value.sortOrder)
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
            advanceUntilIdle()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.RECENTLY_ADDED)
            advanceUntilIdle()

            val places = viewModel.uiState.value.places
            assertTrue(places.size >= 2)
            // Verify descending order
            for (i in 0 until places.size - 1) {
                assertTrue(places[i].createdAtMillis >= places[i + 1].createdAtMillis,
                    "Places should be sorted by createdAtMillis desc")
            }
        }

        @Test
        fun `BEST_RATED sorts by averageRating desc`() = runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.BEST_RATED)
            advanceUntilIdle()

            val places = viewModel.uiState.value.places
            val withRatings = places.filter { it.reviewsCount > 0 }
            for (i in 0 until withRatings.size - 1) {
                assertTrue(withRatings[i].averageRating >= withRatings[i + 1].averageRating,
                    "Places with reviews should be sorted by rating desc")
            }
        }

        @Test
        fun `ADDED_BY_ME shows only current user places`() = runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.ADDED_BY_ME)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.places.all { it.ownerUserId == "user-1" })
        }
    }

    // =========================================================================
    // Category filter
    // =========================================================================

    @Nested
    @DisplayName("Category filtering")
    inner class CategoryFilter {

        @Test
        fun `filtering by category calls repository with category`() = runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onCategorySelected(PlaceCategory.PLAYGROUND)
            advanceUntilIdle()

            verify { placeRepository.observePlaces(PlaceCategory.PLAYGROUND, any()) }
        }

        @Test
        fun `clearing category calls repository with null`() = runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onCategorySelected(PlaceCategory.RESTAURANT)
            advanceUntilIdle()
            viewModel.onCategorySelected(null)
            advanceUntilIdle()

            verify { placeRepository.observePlaces(isNull(), any()) }
        }
    }

    // =========================================================================
    // Amenity filter
    // =========================================================================

    @Nested
    @DisplayName("Amenity filtering")
    inner class AmenityFilter {

        @Test
        fun `toggling amenity adds it to selectedAmenities`() = runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmenityToggled(Amenity.PARKING)
            advanceUntilIdle()

            assertTrue(Amenity.PARKING in viewModel.uiState.value.selectedAmenities)
        }

        @Test
        fun `toggling same amenity twice removes it`() = runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmenityToggled(Amenity.PARKING)
            viewModel.onAmenityToggled(Amenity.PARKING)
            advanceUntilIdle()

            assertFalse(Amenity.PARKING in viewModel.uiState.value.selectedAmenities)
        }

        @Test
        fun `onAmenitiesCleared removes all amenity filters`() = runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmenityToggled(Amenity.PARKING)
            viewModel.onAmenityToggled(Amenity.TOILET)
            advanceUntilIdle()

            viewModel.onAmenitiesCleared()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.selectedAmenities.isEmpty())
        }

        @Test
        fun `amenity filter applies AND logic on places`() = runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmenityToggled(Amenity.PARKING)
            viewModel.onAmenityToggled(Amenity.TOILET)
            advanceUntilIdle()

            val places = viewModel.uiState.value.places
            // Only p3 has both PARKING and TOILET
            assertTrue(places.all { Amenity.PARKING in it.amenities && Amenity.TOILET in it.amenities })
        }
    }

    // =========================================================================
    // Search
    // =========================================================================

    @Nested
    @DisplayName("Search")
    inner class Search {

        @Test
        fun `onSearchQueryChange updates searchQuery in state`() = runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("test")
            advanceUntilIdle()

            assertEquals("test", viewModel.uiState.value.searchQuery)
        }

        @Test
        fun `search triggers repository call with query after debounce`() = runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("restauracja")
            advanceTimeBy(350) // debounce is 300ms
            advanceUntilIdle()

            verify { placeRepository.observePlaces(any(), eq("restauracja")) }
        }
    }

    // =========================================================================
    // Pagination
    // =========================================================================

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        @Test
        fun `loadMore increases visible items`() = runTest {
            // Create 30 places to exceed PAGE_SIZE (20)
            val manyPlaces = (1..30).map {
                TestFixtures.place(id = "p$it", createdAtMillis = it.toLong())
            }
            every { placeRepository.observePlaces(any(), any()) } returns flowOf(manyPlaces)

            viewModel = createViewModel()
            advanceUntilIdle()

            val initialCount = viewModel.uiState.value.places.size
            viewModel.loadMore()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.places.size > initialCount)
        }
    }
}
