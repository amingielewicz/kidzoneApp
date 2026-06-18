@file:Suppress("WildcardImport")

package com.kidzone.presentation.place.list

import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.PagedResult
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.service.LocationProvider
import com.kidzone.testutil.MainDispatcherRule
import com.kidzone.testutil.TestFixtures
import com.kidzone.utils.OpResult
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    private lateinit var locationProvider: LocationProvider
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
            latitude = 52.26, longitude = 21.04),
        TestFixtures.place(id = "p5", name = "Moja Restauracja", category = PlaceCategory.RESTAURANT,
            ownerUserId = "user-1", averageRating = 0.0, reviewsCount = 0, createdAtMillis = 3500L,
            latitude = 52.26, longitude = 21.04)
    )

    @BeforeEach
    fun setUp() {
        placeRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        locationProvider = mockk(relaxed = true)

        every { authRepository.currentUser } returns currentUserFlow
        every { placeRepository.observePlaces(any(), any()) } returns flowOf(samplePlaces)
        every { placeRepository.observePlacesByOwner(any()) } answers {
            val userId = firstArg<String>()
            flowOf(samplePlaces.filter { it.ownerUserId == userId })
        }
    }

    /**
     * Creates VM and starts a background collector to activate
     * the WhileSubscribed(5000) StateFlow. Without this, uiState
     * never emits beyond its initial value.
     */
    private fun kotlinx.coroutines.test.TestScope.createAndObserve(): PlaceListViewModel {
        val vm = PlaceListViewModel(placeRepository, authRepository, locationProvider)
        backgroundScope.launch { vm.uiState.collect {} }
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
            viewModel = createAndObserve()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.places.isNotEmpty())
        }

        @Test
        fun `default sort is NEAREST`() = runTest {
            viewModel = createAndObserve()
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
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.RECENTLY_ADDED)
            advanceUntilIdle()

            val places = viewModel.uiState.value.places
            assertTrue(places.size >= 2, "Expected at least 2 places but got ${places.size}")
            for (i in 0 until places.size - 1) {
                assertTrue(places[i].createdAtMillis >= places[i + 1].createdAtMillis,
                    "Places should be sorted by createdAtMillis desc")
            }
        }

        @Test
        fun `RECENTLY_ADDED keeps newest seeded test place first from unsorted input`() = runTest {
            val seededPlaces = listOf(
                TestFixtures.place(
                    id = "test-place-7",
                    name = "Testowe miejsce 7",
                    createdAtMillis = 1_700_000_000_007L
                ),
                TestFixtures.place(
                    id = "test-place-3",
                    name = "Testowe miejsce 3",
                    createdAtMillis = 1_700_000_000_003L
                ),
                TestFixtures.place(
                    id = "test-place-1",
                    name = "Testowe miejsce 1",
                    createdAtMillis = 1_700_000_000_010L
                )
            )
            every { placeRepository.observePlaces(any(), any()) } returns flowOf(seededPlaces)

            viewModel = createAndObserve()
            advanceUntilIdle()
            viewModel.onCategorySelected(PlaceCategory.PLAYGROUND)
            viewModel.onAmenitiesCleared()
            viewModel.onCategorySelected(null)
            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.RECENTLY_ADDED)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("test-place-1", state.places.first().id)
            assertTrue(state.selectedAmenities.isEmpty())
            assertNull(state.selectedCategory)
        }

        @Test
        fun `RECENTLY_ADDED has stable id tie breaker for identical timestamps`() = runTest {
            val seededPlaces = listOf(
                TestFixtures.place(id = "test-place-7", createdAtMillis = 1_700_000_000_000L),
                TestFixtures.place(id = "test-place-1", createdAtMillis = 1_700_000_000_000L),
                TestFixtures.place(id = "test-place-3", createdAtMillis = 1_700_000_000_000L)
            )
            every { placeRepository.observePlaces(any(), any()) } returns flowOf(seededPlaces)

            viewModel = createAndObserve()
            advanceUntilIdle()
            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.RECENTLY_ADDED)
            advanceUntilIdle()

            assertEquals(
                listOf("test-place-1", "test-place-3", "test-place-7"),
                viewModel.uiState.value.places.map { it.id }
            )
        }

        @Test
        fun `BEST_RATED sorts by averageRating desc`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.BEST_RATED)
            advanceUntilIdle()

            val places = viewModel.uiState.value.places
            val withRatings = places.filter { it.reviewsCount > 0 }
            assertTrue(withRatings.isNotEmpty(), "Expected places with ratings")
            for (i in 0 until withRatings.size - 1) {
                assertTrue(withRatings[i].averageRating >= withRatings[i + 1].averageRating,
                    "Places with reviews should be sorted by rating desc")
            }
        }

        @Test
        fun `ADDED_BY_ME shows only current user places`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.ADDED_BY_ME)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.places.all { it.ownerUserId == "user-1" },
                "Expected only user-1 places but got: ${state.places.map { it.id to it.ownerUserId }}")
        }

        @Test
        fun `ADDED_BY_ME with all categories uses owner source instead of global first page`() = runTest {
            val globalFirstPage = listOf(
                TestFixtures.place(id = "global-1", ownerUserId = "other-user"),
                TestFixtures.place(id = "global-2", ownerUserId = "other-user")
            )
            val myPlaces = listOf(
                TestFixtures.place(
                    id = "mine-playground",
                    category = PlaceCategory.PLAYGROUND,
                    ownerUserId = "user-1",
                    createdAtMillis = 2_000L
                ),
                TestFixtures.place(
                    id = "mine-restaurant",
                    category = PlaceCategory.RESTAURANT,
                    ownerUserId = "user-1",
                    createdAtMillis = 1_000L
                )
            )
            every { placeRepository.observePlaces(any(), any()) } returns flowOf(globalFirstPage)
            every { placeRepository.observePlacesByOwner("user-1") } returns flowOf(myPlaces)

            viewModel = createAndObserve()
            advanceUntilIdle()
            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.ADDED_BY_ME)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.selectedCategory)
            assertEquals(listOf("mine-playground", "mine-restaurant"), state.places.map { it.id })
            verify { placeRepository.observePlacesByOwner("user-1") }
        }

        @Test
        fun `ADDED_BY_ME still applies selected category locally`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.ADDED_BY_ME)
            viewModel.onCategorySelected(PlaceCategory.RESTAURANT)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(PlaceCategory.RESTAURANT, state.selectedCategory)
            assertTrue(
                state.places.isNotEmpty(),
                "Expected current user's restaurant places"
            )
            assertTrue(state.places.all { it.ownerUserId == "user-1" })
            assertTrue(state.places.all { it.category == PlaceCategory.RESTAURANT })
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
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.onCategorySelected(PlaceCategory.PLAYGROUND)
            advanceUntilIdle()

            verify { placeRepository.observePlaces(eq(PlaceCategory.PLAYGROUND), any()) }
        }

        @Test
        fun `clearing category calls repository with null category`() = runTest {
            viewModel = createAndObserve()
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
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.onAmenityToggled(Amenity.PARKING)
            advanceUntilIdle()

            assertTrue(Amenity.PARKING in viewModel.uiState.value.selectedAmenities)
        }

        @Test
        fun `toggling same amenity twice removes it`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.onAmenityToggled(Amenity.PARKING)
            viewModel.onAmenityToggled(Amenity.PARKING)
            advanceUntilIdle()

            assertFalse(Amenity.PARKING in viewModel.uiState.value.selectedAmenities)
        }

        @Test
        fun `onAmenitiesCleared removes all amenity filters`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.onAmenityToggled(Amenity.PARKING)
            viewModel.onAmenityToggled(Amenity.TOILET)
            advanceUntilIdle()

            viewModel.onAmenitiesCleared()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.selectedAmenities.isEmpty())
        }

        @Test
        fun `amenity filter applies AND logic`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.onAmenityToggled(Amenity.PARKING)
            viewModel.onAmenityToggled(Amenity.TOILET)
            advanceUntilIdle()

            val places = viewModel.uiState.value.places
            assertTrue(places.all { Amenity.PARKING in it.amenities && Amenity.TOILET in it.amenities },
                "All places should have both PARKING and TOILET")
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
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("test")
            advanceUntilIdle()

            assertEquals("test", viewModel.uiState.value.searchQuery)
        }

        @Test
        fun `search triggers repository call with query after debounce`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("restauracja")
            advanceTimeBy(450) // debounce is 400ms
            advanceUntilIdle()

            verify { placeRepository.observePlaces(any(), eq("restauracja")) }
        }

        @Test
        fun `typing quickly triggers only final repository query`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()
            clearMocks(placeRepository, answers = false, recordedCalls = true)

            viewModel.onSearchQueryChange("r")
            advanceTimeBy(100)
            viewModel.onSearchQueryChange("re")
            advanceTimeBy(100)
            viewModel.onSearchQueryChange("res")
            advanceTimeBy(450)
            advanceUntilIdle()

            verify(exactly = 1) { placeRepository.observePlaces(any(), eq("res")) }
            verify(exactly = 0) { placeRepository.observePlaces(any(), eq("r")) }
            verify(exactly = 0) { placeRepository.observePlaces(any(), eq("re")) }
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
            val manyPlaces = (1..30).map {
                TestFixtures.place(id = "p$it", createdAtMillis = it.toLong())
            }
            every { placeRepository.observePlaces(any(), any()) } returns flowOf(manyPlaces)

            viewModel = createAndObserve()
            advanceUntilIdle()

            val initialCount = viewModel.uiState.value.places.size
            viewModel.loadMore()
            advanceUntilIdle()

            val afterCount = viewModel.uiState.value.places.size
            assertTrue(
                afterCount > initialCount,
                "Expected more places after loadMore. Before: $initialCount, After: $afterCount"
            )
        }

        @Test
        fun `loadMore skips duplicated snapshot page and appends next server page`() = runTest {
            val firstPage = (1..20).map {
                TestFixtures.place(id = "p$it", createdAtMillis = it.toLong())
            }
            val secondPage = (21..40).map {
                TestFixtures.place(id = "p$it", createdAtMillis = it.toLong())
            }
            every { placeRepository.observePlaces(any(), any()) } returns flowOf(firstPage)
            coEvery {
                placeRepository.getPlacesPage(20, null, null, null)
            } returns OpResult.success(PagedResult(firstPage, "cursor-1"))
            coEvery {
                placeRepository.getPlacesPage(20, "cursor-1", null, null)
            } returns OpResult.success(PagedResult(secondPage, null))

            viewModel = createAndObserve()
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()

            assertEquals(40, viewModel.uiState.value.places.size)
            coVerify(exactly = 1) {
                placeRepository.getPlacesPage(20, null, null, null)
            }
            coVerify(exactly = 1) {
                placeRepository.getPlacesPage(20, "cursor-1", null, null)
            }
        }

        @Test
        fun `changing sort resets visible count after pagination`() = runTest {
            val manyPlaces = (1..30).map {
                TestFixtures.place(id = "p$it", createdAtMillis = it.toLong())
            }
            every { placeRepository.observePlaces(any(), any()) } returns flowOf(manyPlaces)

            viewModel = createAndObserve()
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()
            assertEquals(30, viewModel.uiState.value.places.size)

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.RECENTLY_ADDED)
            advanceUntilIdle()

            assertEquals(20, viewModel.uiState.value.places.size)
            assertEquals("p30", viewModel.uiState.value.places.first().id)
        }
    }
}
