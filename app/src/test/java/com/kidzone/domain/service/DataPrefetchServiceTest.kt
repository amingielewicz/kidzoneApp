package com.kidzone.domain.service

import com.kidzone.domain.model.PagedResult
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.ReviewRepository
import com.kidzone.data.remote.PerformanceConfigProvider
import com.kidzone.testutil.TestFixtures
import com.kidzone.utils.OpResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DataPrefetchServiceTest {

    private lateinit var placeRepository: PlaceRepository
    private lateinit var reviewRepository: ReviewRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var performanceConfigProvider: PerformanceConfigProvider
    private val testScope = TestScope(UnconfinedTestDispatcher())
    
    private lateinit var service: DataPrefetchService

    private val currentUserFlow = MutableStateFlow<User?>(null)

    @BeforeEach
    fun setUp() {
        placeRepository = mockk(relaxed = true)
        reviewRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        performanceConfigProvider = mockk(relaxed = true)

        every { authRepository.currentUser } returns currentUserFlow
        every { authRepository.observeUser(any()) } returns flowOf(TestFixtures.user())
        every { placeRepository.observePlacesByOwner(any()) } returns flowOf(emptyList())
        every { reviewRepository.observeReviewsByUser(any()) } returns flowOf(emptyList())

        coEvery { placeRepository.getTopPlaces(any()) } returns OpResult.success(emptyList())
        coEvery { placeRepository.getPlacesPage(any(), any(), any(), any()) } returns 
            OpResult.success(PagedResult(emptyList(), null))
        coEvery { placeRepository.getPlace(any()) } returns OpResult.success(TestFixtures.place())

        service = DataPrefetchService(
            placeRepository,
            reviewRepository,
            authRepository,
            performanceConfigProvider,
            testScope
        )
    }

    @Test
    fun `startPrefetch triggers global data loading`() = runTest {
        service.startPrefetch()
        
        coVerify { placeRepository.getTopPlaces(any()) }
        coVerify { placeRepository.getPlacesPage(any(), any(), any(), any()) }
    }

    @Test
    fun `startPrefetch triggers user data loading when user is logged in`() = runTest {
        val user = User(id = "user-1", name = "Test", email = "")
        currentUserFlow.value = user
        
        service.startPrefetch()
        advanceUntilIdle()
        
        coVerify { authRepository.getUserById("user-1") }
        coVerify { placeRepository.syncPlacesByOwner("user-1") }
        coVerify { reviewRepository.syncReviewsByUser("user-1") }
    }
}
