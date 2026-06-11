package com.kidzone.presentation.place.add

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.navigation.Route
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
import org.junit.jupiter.api.Assertions.*
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
    private lateinit var appContext: Context

    private val currentUserFlow = MutableStateFlow<User?>(TestFixtures.user())

    @BeforeEach
    fun setUp() {
        savedStateHandle = SavedStateHandle()
        placeRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        photoUploader = mockk(relaxed = true)
        appContext = mockk(relaxed = true)

        every { authRepository.currentUser } returns currentUserFlow
        // observePlaces returns empty list for amenity frequency loading
        coEvery { placeRepository.observePlaces(category = null) } returns flowOf(emptyList())
    }

    private fun createViewModel(placeId: String? = null): AddPlaceViewModel {
        if (placeId != null) {
            savedStateHandle[Route.AddPlace.ARG_PLACE_ID] = placeId
        }
        return AddPlaceViewModel(savedStateHandle, placeRepository, authRepository, photoUploader, appContext)
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
            assertEquals("", state.address)
            assertNull(state.latitude)
            assertNull(state.longitude)
            assertTrue(state.amenities.isEmpty())
            assertFalse(state.isEditMode)
            assertNull(state.editingPlaceId)
            assertFalse(state.isSaving)
            assertFalse(state.isSaved)
            assertNull(state.errorMessage)
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

        @Test
        fun `form is invalid with only name (no location)`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNameChange("Test Place")
            assertFalse(viewModel.uiState.value.isFormValid)
        }

        @Test
        fun `form is invalid with only location (no name)`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onLocationFetched(52.0, 21.0)
            assertFalse(viewModel.uiState.value.isFormValid)
        }

        @Test
        fun `form is invalid when name is whitespace only`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNameChange("   ")
            viewModel.onLocationFetched(52.0, 21.0)
            assertFalse(viewModel.uiState.value.isFormValid)
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
                description = "Description",
                category = PlaceCategory.RESTAURANT,
                latitude = 52.5,
                longitude = 21.5,
                address = "ul. Istniejąca 5",
                amenities = setOf(Amenity.PARKING)
            )
            coEvery { placeRepository.getPlace("place-123") } returns OpResult.success(existingPlace)

            val viewModel = createViewModel(placeId = "place-123")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isEditMode)
            assertEquals("place-123", state.editingPlaceId)
            assertEquals("Existing Place", state.name)
            assertEquals("Description", state.description)
            assertEquals(PlaceCategory.RESTAURANT, state.category)
            assertEquals(52.5, state.latitude)
            assertEquals(21.5, state.longitude)
            assertEquals("ul. Istniejąca 5", state.address)
            assertTrue(Amenity.PARKING in state.amenities)
            assertFalse(state.isLoadingPlace)
        }

        @Test
        fun `shows error when place loading fails`() = runTest {
            coEvery { placeRepository.getPlace("bad-id") } returns
                OpResult.failure(NoSuchElementException("Not found"))

            val viewModel = createViewModel(placeId = "bad-id")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoadingPlace)
            assertNotNull(state.errorMessage)
        }
    }

    // =========================================================================
    // Field updates
    // =========================================================================

    @Nested
    @DisplayName("Field updates")
    inner class FieldUpdates {

        @Test
        fun `onNameChange updates name and clears error`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNameChange("New Name")
            assertEquals("New Name", viewModel.uiState.value.name)
            assertNull(viewModel.uiState.value.errorMessage)
        }

        @Test
        fun `onDescriptionChange updates description`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onDescriptionChange("A nice description")
            assertEquals("A nice description", viewModel.uiState.value.description)
        }

        @Test
        fun `onCategoryChange updates category and prunes incompatible amenities`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            // Set category and add an amenity
            viewModel.onCategoryChange(PlaceCategory.PLAYGROUND)
            // Toggle an amenity that's applicable to PLAYGROUND
            val playgroundAmenity = Amenity.entries.first { PlaceCategory.PLAYGROUND in it.applicableCategories }
            viewModel.toggleAmenity(playgroundAmenity)
            assertTrue(playgroundAmenity in viewModel.uiState.value.amenities)

            // Change to a category where that amenity may not apply
            viewModel.onCategoryChange(PlaceCategory.RESTAURANT)
            // Amenity should be pruned if not applicable to RESTAURANT
            val isApplicable = PlaceCategory.RESTAURANT in playgroundAmenity.applicableCategories
            if (!isApplicable) {
                assertFalse(playgroundAmenity in viewModel.uiState.value.amenities)
            }
        }

        @Test
        fun `onAddressChange updates address`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAddressChange("ul. Nowa 10")
            assertEquals("ul. Nowa 10", viewModel.uiState.value.address)
        }

        @Test
        fun `toggleAmenity adds and removes amenity`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val amenity = Amenity.entries.first { PlaceCategory.PLAYGROUND in it.applicableCategories }
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
        fun `onFetchingLocationStart sets flag`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onFetchingLocationStart()
            assertTrue(viewModel.uiState.value.isFetchingLocation)
        }

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
        fun `onLocationFetched preserves existing address when geocoded is null`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAddressChange("Manually typed address")
            viewModel.onLocationFetched(52.0, 21.0, null)

            assertEquals("Manually typed address", viewModel.uiState.value.address)
        }

        @Test
        fun `onLocationFetched preserves existing address when geocoded is blank`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAddressChange("Typed address")
            viewModel.onLocationFetched(52.0, 21.0, "")

            assertEquals("Typed address", viewModel.uiState.value.address)
        }

        @Test
        fun `onLocationError sets error message`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onFetchingLocationStart()
            viewModel.onLocationError("GPS niedostępny")

            val state = viewModel.uiState.value
            assertFalse(state.isFetchingLocation)
            assertEquals("GPS niedostępny", state.errorMessage)
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
        fun `shows error when user not logged in`() = runTest {
            // Override the mock to return a flow emitting null (no logged in user)
            every { authRepository.currentUser } returns MutableStateFlow<User?>(null)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNameChange("Test Place")
            viewModel.onLocationFetched(52.0, 21.0)
            viewModel.save()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isSaving)
            assertEquals("Musisz być zalogowany, by dodać miejsce", state.errorMessage)
        }

        @Test
        fun `successful save sets isSaved and coordinates`() = runTest {
            val savedPlace = TestFixtures.place(latitude = 52.5, longitude = 21.5)
            coEvery { placeRepository.addPlace(any()) } returns OpResult.success(savedPlace)
            // No nearby places for duplicate check
            coEvery { placeRepository.getPlacesNear(any(), any(), any()) } returns
                OpResult.success(emptyList())

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNameChange("New Place")
            viewModel.onLocationFetched(52.5, 21.5, "Test Address")
            viewModel.save()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isSaved)
            assertEquals(52.5, state.savedNewLatitude)
            assertEquals(21.5, state.savedNewLongitude)
            assertFalse(state.isSaving)
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

            val state = viewModel.uiState.value
            assertFalse(state.isSaved)
            assertFalse(state.isSaving)
            assertEquals("Network error", state.errorMessage)
        }
    }

    // =========================================================================
    // Save - Edit mode
    // =========================================================================

    @Nested
    @DisplayName("save() - Edit mode")
    inner class SaveEdit {

        @Test
        fun `successful edit updates existing place`() = runTest {
            val existingPlace = TestFixtures.place(id = "place-1")
            coEvery { placeRepository.getPlace("place-1") } returns OpResult.success(existingPlace)
            coEvery { placeRepository.updatePlace(any()) } returns OpResult.success(existingPlace)
            coEvery { placeRepository.getPlacesNear(any(), any(), any()) } returns
                OpResult.success(emptyList())

            val viewModel = createViewModel(placeId = "place-1")
            advanceUntilIdle()

            viewModel.onNameChange("Updated Name")
            viewModel.save()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isSaved)
            // In edit mode, saved coordinates should be null (no map focus)
            assertNull(state.savedNewLatitude)
            assertNull(state.savedNewLongitude)
            coVerify { placeRepository.updatePlace(any()) }
        }
    }

    // =========================================================================
    // Duplicate detection
    // =========================================================================

    @Nested
    @DisplayName("Duplicate detection")
    inner class DuplicateDetection {

        @Test
        fun `shows duplicate warning when same category place within 100m`() = runTest {
            val nearbyPlace = TestFixtures.place(
                id = "nearby-1",
                name = "Existing Playground",
                category = PlaceCategory.PLAYGROUND,
                latitude = 52.0001,
                longitude = 21.0001
            )
            coEvery { placeRepository.getPlacesNear(any(), any(), any()) } returns
                OpResult.success(listOf(nearbyPlace))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNameChange("My Playground")
            viewModel.onLocationFetched(52.0, 21.0)
            advanceUntilIdle() // let nearby places load

            viewModel.save()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.showDuplicateWarning)
            assertNotNull(state.duplicateCandidate)
            assertFalse(state.isSaved) // not saved yet
        }

        @Test
        fun `confirmSaveDespiteDuplicate proceeds with save`() = runTest {
            val nearbyPlace = TestFixtures.place(
                id = "nearby-1",
                category = PlaceCategory.PLAYGROUND,
                latitude = 52.0001,
                longitude = 21.0001
            )
            coEvery { placeRepository.getPlacesNear(any(), any(), any()) } returns
                OpResult.success(listOf(nearbyPlace))
            coEvery { placeRepository.addPlace(any()) } returns
                OpResult.success(TestFixtures.place())

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNameChange("My Playground")
            viewModel.onLocationFetched(52.0, 21.0)
            advanceUntilIdle()

            viewModel.save()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showDuplicateWarning)

            viewModel.confirmSaveDespiteDuplicate()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.showDuplicateWarning)
            assertTrue(state.isSaved)
        }

        @Test
        fun `dismissDuplicateWarning cancels save`() = runTest {
            val nearbyPlace = TestFixtures.place(
                id = "nearby-1",
                category = PlaceCategory.PLAYGROUND,
                latitude = 52.0001,
                longitude = 21.0001
            )
            coEvery { placeRepository.getPlacesNear(any(), any(), any()) } returns
                OpResult.success(listOf(nearbyPlace))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNameChange("My Playground")
            viewModel.onLocationFetched(52.0, 21.0)
            advanceUntilIdle()

            viewModel.save()
            advanceUntilIdle()

            viewModel.dismissDuplicateWarning()
            val state = viewModel.uiState.value
            assertFalse(state.showDuplicateWarning)
            assertNull(state.duplicateCandidate)
            assertFalse(state.isSaved)
        }

        @Test
        fun `no duplicate warning in edit mode`() = runTest {
            val existingPlace = TestFixtures.place(id = "place-1", category = PlaceCategory.PLAYGROUND)
            val nearbyPlace = TestFixtures.place(
                id = "nearby-1",
                category = PlaceCategory.PLAYGROUND,
                latitude = 52.0001,
                longitude = 21.0001
            )
            coEvery { placeRepository.getPlace("place-1") } returns OpResult.success(existingPlace)
            coEvery { placeRepository.updatePlace(any()) } returns OpResult.success(existingPlace)
            coEvery { placeRepository.getPlacesNear(any(), any(), any()) } returns
                OpResult.success(listOf(nearbyPlace))

            val viewModel = createViewModel(placeId = "place-1")
            advanceUntilIdle()

            viewModel.save()
            advanceUntilIdle()

            // In edit mode duplicate warning is skipped
            assertFalse(viewModel.uiState.value.showDuplicateWarning)
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

            // Should only add up to MAX_PLACE_PHOTOS (5)
            assertEquals(MAX_PLACE_PHOTOS, viewModel.uiState.value.photoUris.size)
        }

        @Test
        fun `removeNewPhoto removes photo at index`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val uri1 = mockk<android.net.Uri>()
            val uri2 = mockk<android.net.Uri>()
            viewModel.addPhotos(listOf(uri1, uri2))
            assertEquals(2, viewModel.uiState.value.photoUris.size)

            viewModel.removeNewPhoto(0)
            assertEquals(1, viewModel.uiState.value.photoUris.size)
            assertEquals(uri2, viewModel.uiState.value.photoUris[0])
        }

        @Test
        fun `consumePhotoDuplicateMessage clears message`() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            // We can't easily trigger the duplicate message without real compression,
            // but we can test the consume method
            viewModel.consumePhotoDuplicateMessage()
            assertNull(viewModel.uiState.value.photoDuplicateMessage)
        }
    }

    // =========================================================================
    // Amenity frequency
    // =========================================================================

    @Nested
    @DisplayName("Amenity frequency loading")
    inner class AmenityFrequency {

        @Test
        fun `loads amenity frequency from all places on init`() = runTest {
            val places = listOf(
                TestFixtures.place(amenities = setOf(Amenity.PARKING, Amenity.TOILET)),
                TestFixtures.place(id = "p2", amenities = setOf(Amenity.PARKING)),
                TestFixtures.place(id = "p3", amenities = setOf(Amenity.TOILET, Amenity.PARKING))
            )
            coEvery { placeRepository.observePlaces(category = null) } returns flowOf(places)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val freq = viewModel.uiState.value.amenityFrequency
            assertEquals(3, freq[Amenity.PARKING])
            assertEquals(2, freq[Amenity.TOILET])
        }

        @Test
        fun `amenity frequency is empty map on error`() = runTest {
            coEvery { placeRepository.observePlaces(category = null) } returns
                kotlinx.coroutines.flow.flow { throw RuntimeException("error") }

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.amenityFrequency.isEmpty())
        }
    }
}
