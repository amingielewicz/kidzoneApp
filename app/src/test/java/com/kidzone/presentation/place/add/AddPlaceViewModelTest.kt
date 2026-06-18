@file:Suppress("WildcardImport")

package com.kidzone.presentation.place.add

import androidx.lifecycle.SavedStateHandle
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.service.ImageCompressorPort
import com.kidzone.navigation.Route
import com.kidzone.review.InAppReviewManager
import com.kidzone.testutil.MainDispatcherRule
import com.kidzone.testutil.TestFixtures
import com.kidzone.utils.OpResult
import com.kidzone.utils.PhotoUploader
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class AddPlaceViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var placeRepository: PlaceRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var photoUploader: PhotoUploader
    private lateinit var imageCompressor: ImageCompressorPort
    private lateinit var inAppReviewManager: InAppReviewManager

    private val currentUserFlow = MutableStateFlow(TestFixtures.user())

    @BeforeEach
    fun setUp() {
        savedStateHandle = SavedStateHandle()
        placeRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        photoUploader = mockk(relaxed = true)
        imageCompressor = mockk(relaxed = true)
        inAppReviewManager = mockk(relaxed = true)

        every { authRepository.currentUser } returns currentUserFlow
        // observePlaces(category, query) - 2 params
        every { placeRepository.observePlaces(any(), any()) } returns flowOf(emptyList())
    }

    private fun createViewModel(placeId: String? = null): AddPlaceViewModel {
        if (placeId != null) {
            savedStateHandle[Route.AddPlace.ARG_PLACE_ID] = placeId
        }
        return AddPlaceViewModel(
            savedStateHandle,
            placeRepository,
            authRepository,
            photoUploader,
            imageCompressor,
            inAppReviewManager
        )
    }

    // =========================================================================
    // Initial state - Create mode
    // =========================================================================

    @Nested
    @DisplayName("Create mode - initial state")
    inner class CreateMode {

        @Test
        fun `initial state is empty form in create mode`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("", state.name)
            assertEquals("", state.description)
            assertEquals(PlaceCategory.PLAYGROUND, state.category)
            assertNull(state.latitude)
            assertNull(state.longitude)
            assertTrue(state.amenities.isEmpty())
            assertFalse(state.isEditMode)
            assertFalse(state.isSaving)
            assertFalse(state.isSaved)
        }

        @Test
        fun `form is invalid without name and location`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isFormValid)
        }

        @Test
        fun `form is valid with name and location`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNameChange("Test Place")
            viewModel.onLocationFetched(52.0, 21.0, "ul. Testowa 1")
            assertTrue(viewModel.uiState.value.isFormValid)
        }
    }

    // =========================================================================
    // Edit mode
    // =========================================================================

    @Nested
    @DisplayName("Edit mode")
    inner class EditMode {

        @Test
        fun `loads existing place data for editing`() = runTest {
            val existingPlace = TestFixtures.place(
                id = "place-123",
                name = "Existing Place",
                category = PlaceCategory.RESTAURANT,
                latitude = 52.5,
                longitude = 21.5,
            )
            coEvery { placeRepository.getPlace("place-123") } returns OpResult.success(existingPlace)

            val viewModel = createViewModel(placeId = "place-123")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isEditMode)
            assertEquals("Existing Place", state.name)
            assertEquals(PlaceCategory.RESTAURANT, state.category)
        }

        @Test
        fun `shows error when place loading fails`() = runTest {
            coEvery { placeRepository.getPlace("bad-id") } returns
                OpResult.failure(NoSuchElementException("Not found"))

            val viewModel = createViewModel(placeId = "bad-id")
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.errorMessage)
        }
    }

    // =========================================================================
    // Field updates
    // =========================================================================

    @Nested
    @DisplayName("Field updates")
    inner class FieldUpdates {

        @Test
        fun `onNameChange updates name`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNameChange("New Name")
            assertEquals("New Name", viewModel.uiState.value.name)
        }

        @Test
        fun `onNameChange limits name to max length`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNameChange("A".repeat(PLACE_NAME_MAX_LENGTH + 1))

            assertEquals(PLACE_NAME_MAX_LENGTH, viewModel.uiState.value.name.length)
        }

        @Test
        fun `onDescriptionChange updates description`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onDescriptionChange("A nice description")
            assertEquals("A nice description", viewModel.uiState.value.description)
        }

        @Test
        fun `onCategoryChange updates category`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onCategoryChange(PlaceCategory.RESTAURANT)
            assertEquals(PlaceCategory.RESTAURANT, viewModel.uiState.value.category)
        }

        @Test
        fun `toggleAmenity adds and removes amenity`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val amenity = Amenity.PARKING
            viewModel.onCategoryChange(PlaceCategory.RESTAURANT) // PARKING applies to RESTAURANT
            viewModel.toggleAmenity(amenity)
            assertTrue(amenity in viewModel.uiState.value.amenities)

            viewModel.toggleAmenity(amenity)
            assertFalse(amenity in viewModel.uiState.value.amenities)
        }
    }

    // =========================================================================
    // Location
    // =========================================================================

    @Nested
    @DisplayName("Location handling")
    inner class Location {

        @Test
        fun `onLocationFetched saves coordinates and address`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onLocationFetched(52.2297, 21.0122, "Warszawa, Centrum")

            val state = viewModel.uiState.value
            assertEquals(52.2297, state.latitude)
            assertEquals(21.0122, state.longitude)
            assertEquals("Warszawa, Centrum", state.address)
            assertFalse(state.isFetchingLocation)
        }

        @Test
        fun `onLocationError sets error message`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onFetchingLocationStart()
            viewModel.onLocationError("GPS niedostępny")

            assertFalse(viewModel.uiState.value.isFetchingLocation)
            assertEquals("GPS niedostępny", viewModel.uiState.value.errorMessage)
        }
    }

    // =========================================================================
    // Save - Create mode
    // =========================================================================

    @Nested
    @DisplayName("save() - Create mode")
    inner class SaveCreate {

        @Test
        fun `shows error when form is invalid`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.save()
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isSaved)
        }

        @Test
        fun `successful save sets isSaved`() = runTest {
            val savedPlace = TestFixtures.place(latitude = 52.5, longitude = 21.5)
            coEvery { placeRepository.addPlace(any()) } returns OpResult.success(savedPlace)
            coEvery { placeRepository.getPlacesNear(any(), any(), any()) } returns
                OpResult.success(emptyList())

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNameChange("New Place")
            viewModel.onLocationFetched(52.5, 21.5, "Test Address")
            viewModel.save()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isSaved)
        }

        @Test
        fun `failed save shows error message`() = runTest {
            coEvery { placeRepository.addPlace(any()) } returns
                OpResult.failure(RuntimeException("Network error"))
            coEvery { placeRepository.getPlacesNear(any(), any(), any()) } returns
                OpResult.success(emptyList())

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNameChange("New Place")
            viewModel.onLocationFetched(52.0, 21.0)
            viewModel.save()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSaved)
            assertNotNull(viewModel.uiState.value.errorMessage)
        }
    }

    // =========================================================================
    // Photos
    // =========================================================================

    @Nested
    @DisplayName("Photo management")
    inner class Photos {

        @Test
        fun `canAddMorePhotos is true when under limit`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.canAddMorePhotos)
        }

        @Test
        fun `addPhotos respects MAX_PLACE_PHOTOS limit`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val uris = (1..10).map { mockk<android.net.Uri>() }
            viewModel.addPhotos(uris)

            assertEquals(MAX_PLACE_PHOTOS, viewModel.uiState.value.photoUris.size)
        }

        @Test
        fun `removeNewPhoto removes photo at index`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val uri1 = mockk<android.net.Uri>()
            val uri2 = mockk<android.net.Uri>()
            viewModel.addPhotos(listOf(uri1, uri2))

            viewModel.removeNewPhoto(0)
            assertEquals(1, viewModel.uiState.value.photoUris.size)
            assertEquals(uri2, viewModel.uiState.value.photoUris[0])
        }
    }

    // =========================================================================
    // Duplicate detection
    // =========================================================================

    @Nested
    @DisplayName("Duplicate detection")
    inner class DuplicateDetection {

        @Test
        fun `dismissDuplicateWarning clears state`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.dismissDuplicateWarning()
            assertFalse(viewModel.uiState.value.showDuplicateWarning)
            assertNull(viewModel.uiState.value.duplicateCandidate)
        }
    }
}
