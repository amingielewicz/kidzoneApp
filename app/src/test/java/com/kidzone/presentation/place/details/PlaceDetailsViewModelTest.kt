package com.kidzone.presentation.place.details

import androidx.lifecycle.SavedStateHandle
import com.kidzone.R
import com.kidzone.analytics.AnalyticsHelper
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.ReviewRepository
import com.kidzone.domain.service.ImageCompressorPort
import com.kidzone.domain.service.LocationProvider
import com.kidzone.navigation.Route
import com.kidzone.presentation.common.ScreenState
import com.kidzone.review.InAppReviewManager
import com.kidzone.testutil.MainDispatcherRule
import com.kidzone.testutil.TestFixtures
import com.kidzone.utils.OpResult
import com.kidzone.utils.PhotoHasher
import com.kidzone.utils.PhotoUploader
import com.kidzone.utils.UiText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * 🧪 Cel testu:
 * - Weryfikacja zarządzania stanem szczegółów miejsca w [PlaceDetailsViewModel].
 * - Sprawdzenie poprawności przejść między stanami [ScreenState] (Loading, Content, Error).
 * - Weryfikacja akcji użytkownika takich jak odświeżanie i usuwanie miejsca.
 *
 * 🛠️ Środowisko:
 * - Mockowanie repozytoriów, usług lokalizacji i analityki.
 * - [MainDispatcherRule] dla testów asynchronicznych operacji Flow i Coroutines.
 *
 * 🔍 Scenariusze:
 * - Stan początkowy to Loading.
 * - Pomyślne załadowanie danych miejsca zmienia stan na Content.
 * - Błąd pobierania danych zmienia stan na Error z odpowiednim komunikatem.
 * - Funkcja retry() poprawnie wymusza ponowne ładowanie po wystąpieniu błędu.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaceDetailsViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var placeRepository: PlaceRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var reviewRepository: ReviewRepository
    private lateinit var locationProvider: LocationProvider
    private lateinit var photoUploader: PhotoUploader
    private lateinit var imageCompressor: ImageCompressorPort
    private lateinit var photoHasher: PhotoHasher
    private lateinit var inAppReviewManager: InAppReviewManager
    private lateinit var analyticsHelper: AnalyticsHelper
    private lateinit var viewModel: PlaceDetailsViewModel

    private val placeId = "test-place-id"
    private val savedStateHandle = SavedStateHandle(mapOf(Route.PlaceDetails.ARG_PLACE_ID to placeId))

    @BeforeEach
    fun setUp() {
        placeRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        reviewRepository = mockk(relaxed = true)
        locationProvider = mockk(relaxed = true)
        photoUploader = mockk(relaxed = true)
        imageCompressor = mockk(relaxed = true)
        photoHasher = mockk(relaxed = true)
        inAppReviewManager = mockk(relaxed = true)
        analyticsHelper = mockk(relaxed = true)

        every { authRepository.currentUser } returns MutableStateFlow(null)
        every { reviewRepository.observeReviewsForPlace(any()) } returns flowOf(emptyList())
        coEvery { placeRepository.getTopPlaces(any()) } returns OpResult.success(emptyList())
    }

    private fun createViewModel() = PlaceDetailsViewModel(
        savedStateHandle,
        placeRepository,
        authRepository,
        reviewRepository,
        locationProvider,
        photoUploader,
        imageCompressor,
        photoHasher,
        inAppReviewManager,
        analyticsHelper
    )

    @Nested
    @DisplayName("Initial state & loading")
    inner class InitialState {

        @Test
        fun `initial state is Loading`() = runTest {
            // Note: VM triggers load in init, so we must mock success to be slow or just check immediately
            coEvery { placeRepository.getPlace(placeId) } coAnswers {
                kotlinx.coroutines.delay(1000)
                OpResult.success(TestFixtures.place(id = placeId))
            }

            viewModel = createViewModel()
            
            assertEquals(ScreenState.Loading, viewModel.uiState.value.screenState)
        }

        @Test
        fun `successful load transitions to Content state`() = runTest {
            val place = TestFixtures.place(id = placeId, name = "Fajny Plac")
            coEvery { placeRepository.getPlace(placeId) } returns OpResult.success(place)

            viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.screenState is ScreenState.Content)
            assertEquals(place, (state.screenState as ScreenState.Content).data)
        }

        @Test
        fun `failed load transitions to Error state`() = runTest {
            coEvery { placeRepository.getPlace(placeId) } returns OpResult.failure(Exception("Błąd sieci"))

            viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.screenState is ScreenState.Error)
            val error = state.screenState as ScreenState.Error
            assertTrue(error.message is UiText.StringResource)
            assertEquals(R.string.error_load_place, (error.message as UiText.StringResource).resId)
        }
    }

    @Nested
    @DisplayName("Retry mechanism")
    inner class Retry {

        @Test
        fun `retry clears error and starts loading again`() = runTest {
            coEvery { placeRepository.getPlace(placeId) } returns OpResult.failure(Exception("Błąd 1"))
            
            viewModel = createViewModel()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.screenState is ScreenState.Error)

            coEvery { placeRepository.getPlace(placeId) } returns OpResult.success(TestFixtures.place(id = placeId))
            
            viewModel.retry()
            // Should be Loading immediately after retry call
            assertEquals(ScreenState.Loading, viewModel.uiState.value.screenState)
            
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.screenState is ScreenState.Content)
        }
    }
}
