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
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
            ownerUserId = "user-1",
            averageRating = 4.5, reviewsCount = 10, createdAtMillis = 1000L,
            latitude = 52.23, longitude = 21.01, amenities = setOf(Amenity.PARKING)),
        TestFixtures.place(id = "p2", name = "Restauracja B", category = PlaceCategory.RESTAURANT,
            ownerUserId = "user-2",
            averageRating = 3.0, reviewsCount = 5, createdAtMillis = 2000L,
            latitude = 52.24, longitude = 21.02, amenities = setOf(Amenity.TOILET)),
        TestFixtures.place(id = "p3", name = "Park C", category = PlaceCategory.PARK,
            ownerUserId = "user-1",
            averageRating = 5.0, reviewsCount = 20, createdAtMillis = 3000L,
            latitude = 52.25, longitude = 21.03, amenities = setOf(Amenity.PARKING, Amenity.TOILET)),
        TestFixtures.place(id = "p4", name = "Nowe bez ocen", category = PlaceCategory.PARK,
            ownerUserId = "user-3",
            averageRating = 0.0, reviewsCount = 0, createdAtMillis = 4000L,
            latitude = 52.26, longitude = 21.04),
    )

    @BeforeEach
    fun setUp() {
        placeRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        locationProvider = mockk(relaxed = true)

        every { authRepository.currentUser } returns currentUserFlow
        coEvery {
            placeRepository.getPlacesPage(any(), any(), any(), any())
        } returns OpResult.success(PagedResult(samplePlaces, null))
        coEvery { locationProvider.getCurrentLocation() } returns null
    }

    private fun kotlinx.coroutines.test.TestScope.createAndObserve(): PlaceListViewModel {
        val vm = PlaceListViewModel(placeRepository, authRepository, locationProvider)
        backgroundScope.launch { vm.uiState.collect {} }
        return vm
    }

    @Nested
    @DisplayName("Initial state & loading")
    inner class InitialState {

        @Test
        fun `loads all places after initialization`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(4, state.places.size)
        }

        @Test
        fun `default sort is NEAREST`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            assertEquals(PlaceListViewModel.SortOrder.NEAREST, viewModel.uiState.value.sortOrder)
        }
    }

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
            assertEquals("p4", places[0].id)
            assertEquals("p3", places[1].id)
            assertEquals("p2", places[2].id)
            assertEquals("p1", places[3].id)
        }

        @Test
        fun `ADDED_BY_ME filters out places owned by other users`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.ADDED_BY_ME)
            advanceUntilIdle()

            assertEquals(listOf("p3", "p1"), viewModel.uiState.value.places.map { it.id })
        }

        @Test
        fun `WORST_RATED puts places without reviews last`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()

            viewModel.onSortOrderChange(PlaceListViewModel.SortOrder.WORST_RATED)
            advanceUntilIdle()

            assertEquals(listOf("p2", "p1", "p3", "p4"), viewModel.uiState.value.places.map { it.id })
        }
    }

    @Nested
    @DisplayName("Filtering")
    inner class Filtering {

        @Test
        fun `onCategorySelect triggers refresh from repository`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()
            clearMocks(placeRepository, answers = false)

            viewModel.onCategorySelect(PlaceCategory.PLAYGROUND)
            advanceUntilIdle()

            coVerify {
                placeRepository.getPlacesPage(any(), null, PlaceCategory.PLAYGROUND, any())
            }
        }

        @Test
        fun `onSearchQueryChange triggers refresh from repository`() = runTest {
            viewModel = createAndObserve()
            advanceUntilIdle()
            clearMocks(placeRepository, answers = false)

            viewModel.onSearchQueryChange("test")
            advanceUntilIdle()

            coVerify {
                placeRepository.getPlacesPage(any(), null, any(), "test")
            }
        }
    }

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        @Test
        fun `loadMore appends items from next page`() = runTest {
            val firstPage = samplePlaces
            val secondPage = listOf(TestFixtures.place(id = "p5"))

            coEvery {
                placeRepository.getPlacesPage(any(), null, any(), any())
            } returns OpResult.success(PagedResult(firstPage, "cursor-1"))

            coEvery {
                placeRepository.getPlacesPage(any(), "cursor-1", any(), any())
            } returns OpResult.success(PagedResult(secondPage, null))

            viewModel = createAndObserve()
            advanceUntilIdle()

            assertEquals(4, viewModel.uiState.value.places.size)
            assertTrue(viewModel.uiState.value.hasMore)

            viewModel.loadMore()
            advanceUntilIdle()

            assertEquals(5, viewModel.uiState.value.places.size)
            assertFalse(viewModel.uiState.value.hasMore)
        }
    }
}
