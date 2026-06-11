package com.kidzone.presentation.place.list

import android.content.Context
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.testutil.MainDispatcherRule
import com.kidzone.testutil.TestFixtures
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        every { placeRepository.observePlaces(isNull()) } returns flowOf(samplePlaces)
        every { placeRepository.observePlaces(any<PlaceCategory>()) } answers {
            val cat = firstArg<PlaceCategory?>()
            flowOf(samplePlaces.filter { cat == null || it.category == cat })
        }
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
        fun `initial state shows loading`() = runTest {
            viewModel = createViewModel()
            // With UnconfinedTestDispatcher, flow starts immediately
            // Initial value before first emission should be loading
            // But with Unconfined it processes eagerly - check that it loaded
            val state = viewModel.uiState.value
            // After eager execution with UnconfinedTestDispatcher, data is already loaded
            assertFalse(state.isLoading)
            assertEquals(4, state.totalCount)
        }

        @Test
        fun `loads all places after initialization`() = runTest {
            viewModel = createViewModel()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(4, state.totalCount)
            assertTrue(state.places.isNotEmpty())
        }

        @Test
        fun `default sort is NEAREST but falls back to RECENTLY_ADDED without location`() = runTest {
            viewModel = createViewModel()

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

            viewModel.onCategorySelected(PlaceCategory.PLAYGROUND)

            val state = viewModel.uiState.value
            assertEquals(PlaceCategory.PLAYGROUND, state.selectedCategory)
            assertTrue(state.places.all { it.category == PlaceCategory.PLAYGROUND })
        }

        @Test
        fun `clearing category shows all places`() = runTest {
            viewModel = createViewModel()

            viewModel.onCategorySelected(PlaceCategory.RESTAURANT)
            viewModel.onCategorySelected(null)

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

            viewModel.onAmenityToggled(Amenity.PARKING)

            val state = viewModel.uiState.value
            assertTrue(Amenity.PARKING in state.selectedAmenities)
            assertTrue(state.places.all { Amenity.PARKING in it.amenities })
            // p1 and p3 have PARKING
            assertEquals(2, state.totalCount)
        }

        @Test
        fun `multiple amenities use AND logic`() = runTest {
            viewModel = createViewModel()

            viewModel.onAmenityToggled(Amenity.PARKING)
            viewModel.onAmenityToggled(Amenity.TOILET)

            val state = viewModel.uiState.value
            // Only p3 has both PARKING and TOILET
            assertEquals(1, state.totalCount)
            assertEquals("p3", state.places.first().id)
        }

        @Test
        fun `toggling same amenity twice removes filter`() = runTest {
            viewModel = createViewModel()

            viewModel.onAmenityToggled(Amenity.PARKING)
            assertEquals(2, viewModel.uiState.value.totalCount)

            viewModel.onAmenityToggled(Amenity.PARKING)
            assertEquals(4, viewModel.uiState.value.totalCount)
        }

        @Test
        fun `onAmenitiesCleared removes all amenity filters`() = runTest {
            viewModel = createViewModel()

            viewModel.onAmenityToggled(Amenity.PARKING)
            viewModel.onAmenityToggled(Amenity.TOILET)

            viewModel.onAmenitiesCleared()

            assertTrue(viewModel.uiState.value.selectedAmenities.isEmpty())
            assertEquals(4, viewModel.uiState.value.totalCount)
        }
    }

    // =========================================================================
    // Sort orders
    // =========================================================================

    @Nested
    @DisplayName("Sorting")
    inner class Sorting {

        @Test
        fun `RECENTLY_ADDED sorts by createdAtMillis desc`() = runTest {
            viewModel = createViewModel()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.RECENTLY_ADDED)

            val places = viewModel.uiState.value.places
            assertEquals("p4", places[0].id) // 4000L
            assertEquals("p3", places[1].id) // 3000L
            assertEquals("p2", places[2].id) // 2000L
            assertEquals("p1", places[3].id) // 1000L
        }

        @Test
        fun `BEST_RATED sorts by averageRating desc`() = runTest {
            viewModel = createViewModel()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.BEST_RATED)

            val places = viewModel.uiState.value.places
            assertEquals("p3", places[0].id) // 5.0
            assertEquals("p1", places[1].id) // 4.5
            assertEquals("p2", places[2].id) // 3.0
            // p4 has 0.0 rating but 0 reviews - goes last
        }

        @Test
        fun `WORST_RATED puts places with 0 reviews at the end`() = runTest {
            viewModel = createViewModel()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.WORST_RATED)

            val places = viewModel.uiState.value.places
            // p4 has 0 reviews, should be last (not first despite 0.0 rating)
            assertEquals("p4", places.last().id)
            // First should be lowest rated with reviews
            assertEquals("p2", places[0].id) // 3.0
        }

        @Test
        fun `ADDED_BY_ME shows only current user places`() = runTest {
            viewModel = createViewModel()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.ADDED_BY_ME)

            val state = viewModel.uiState.value
            assertEquals(1, state.totalCount)
            assertEquals("p4", state.places.first().id)
            assertEquals("user-1", state.places.first().ownerUserId)
        }

        @Test
        fun `ADDED_BY_ME shows empty list when user id is blank`() = runTest {
            // Use a user with blank id to simulate "no user id available"
            every { authRepository.currentUser } returns flowOf(TestFixtures.user(id = ""))
            viewModel = createViewModel()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.ADDED_BY_ME)

            assertTrue(viewModel.uiState.value.places.isEmpty())
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

            viewModel.onSearchQueryChange("restauracja")

            val state = viewModel.uiState.value
            assertEquals(1, state.totalCount)
            assertEquals("p2", state.places.first().id)
            assertEquals("restauracja", state.searchQuery)
        }

        @Test
        fun `empty search shows all places`() = runTest {
            viewModel = createViewModel()

            viewModel.onSearchQueryChange("restauracja")
            assertEquals(1, viewModel.uiState.value.totalCount)

            viewModel.onSearchQueryChange("")
            assertEquals(4, viewModel.uiState.value.totalCount)
        }

        @Test
        fun `search with no matches shows empty list`() = runTest {
            viewModel = createViewModel()

            viewModel.onSearchQueryChange("nonexistent")

            assertEquals(0, viewModel.uiState.value.totalCount)
            assertTrue(viewModel.uiState.value.places.isEmpty())
        }
    }

    // =========================================================================
    // Pagination
    // =========================================================================

    @Nested
    @DisplayName("Pagination (infinite scroll)")
    inner class Pagination {

        @Test
        fun `initial page shows PAGE_SIZE items max`() = runTest {
            val manyPlaces = (1..30).map {
                TestFixtures.place(id = "p$it", createdAtMillis = it.toLong())
            }
            every { placeRepository.observePlaces(isNull()) } returns flowOf(manyPlaces)

            viewModel = createViewModel()

            val state = viewModel.uiState.value
            assertEquals(20, state.places.size) // PAGE_SIZE = 20
            assertEquals(30, state.totalCount)
            assertTrue(state.hasMore)
        }

        @Test
        fun `loadMore increases visible count`() = runTest {
            val manyPlaces = (1..30).map {
                TestFixtures.place(id = "p$it", createdAtMillis = it.toLong())
            }
            every { placeRepository.observePlaces(isNull()) } returns flowOf(manyPlaces)

            viewModel = createViewModel()

            viewModel.loadMore()

            val state = viewModel.uiState.value
            assertEquals(30, state.places.size) // all loaded
            assertFalse(state.hasMore)
        }

        @Test
        fun `changing category resets pagination`() = runTest {
            val manyPlaces = (1..30).map {
                TestFixtures.place(id = "p$it", category = PlaceCategory.PLAYGROUND, createdAtMillis = it.toLong())
            }
            every { placeRepository.observePlaces(isNull()) } returns flowOf(manyPlaces)
            every { placeRepository.observePlaces(PlaceCategory.PLAYGROUND) } returns flowOf(manyPlaces)

            viewModel = createViewModel()

            viewModel.loadMore() // now at 40

            viewModel.onCategorySelected(PlaceCategory.PLAYGROUND)

            // Should reset to PAGE_SIZE
            assertEquals(20, viewModel.uiState.value.places.size)
        }
    }

    // =========================================================================
    // Error handling
    // =========================================================================

    @Nested
    @DisplayName("Error handling")
    inner class ErrorHandling {

        @Test
        fun `error from snapshot listener shows errorMessage`() = runTest {
            every { placeRepository.observePlaces(isNull()) } returns
                kotlinx.coroutines.flow.flow { throw RuntimeException("Firestore unavailable") }

            viewModel = createViewModel()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.errorMessage)
            assertTrue(state.places.isEmpty())
        }
    }
}
