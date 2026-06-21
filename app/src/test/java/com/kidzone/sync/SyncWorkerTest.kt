package com.kidzone.sync

import android.content.Context
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.kidzone.data.local.sync.OperationStatus
import com.kidzone.data.local.sync.OperationType
import com.kidzone.data.local.sync.PendingOperationDao
import com.kidzone.data.local.sync.PendingOperationEntity
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.ReviewRepository
import com.kidzone.testutil.TestFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SyncWorkerTest {

    private lateinit var pendingOperationDao: PendingOperationDao
    private lateinit var worker: SyncWorker

    @BeforeEach
    fun setUp() {
        pendingOperationDao = mockk(relaxed = true)
        worker = SyncWorker(
            appContext = mockk<Context>(relaxed = true),
            workerParams = mockk<WorkerParameters>(relaxed = true),
            pendingOperationDao = pendingOperationDao,
            placeRepository = mockk<PlaceRepository>(relaxed = true),
            reviewRepository = mockk<ReviewRepository>(relaxed = true),
            firestore = mockk<FirebaseFirestore>(relaxed = true)
        )
    }

    @Test
    fun `malformed review payload is deleted to avoid infinite retry`() = runTest {
        val operation = pendingOperation(payload = "not-json")
        coEvery { pendingOperationDao.getPending() } returns listOf(operation)
        coEvery { pendingOperationDao.getRetryable(any()) } returns emptyList()

        worker.doWork()

        coVerify { pendingOperationDao.markInProgress(operation.id) }
        coVerify { pendingOperationDao.delete(operation.id) }
        coVerify(exactly = 0) { pendingOperationDao.markFailed(operation.id) }
        coVerify(exactly = 0) { pendingOperationDao.markDeadLetter(operation.id) }
    }

    @Test
    fun `valid gated review payload is marked failed for retry`() = runTest {
        val review = TestFixtures.review(id = "review-1")
        val operation = pendingOperation(payload = OfflinePayload.serializeReview(review))
        coEvery { pendingOperationDao.getPending() } returns emptyList()
        coEvery { pendingOperationDao.getRetryable(any()) } returns listOf(operation)

        worker.doWork()

        coVerify { pendingOperationDao.markInProgress(operation.id) }
        coVerify { pendingOperationDao.markFailed(operation.id) }
        coVerify(exactly = 0) { pendingOperationDao.delete(operation.id) }
        coVerify(exactly = 0) { pendingOperationDao.markDeadLetter(operation.id) }
    }

    @Test
    fun `valid gated review payload moves to dead letter after max retries`() = runTest {
        val review = TestFixtures.review(id = "review-1")
        val operation = pendingOperation(
            payload = OfflinePayload.serializeReview(review),
            retryCount = SyncWorker.MAX_RETRIES - 1,
            status = OperationStatus.FAILED
        )
        coEvery { pendingOperationDao.getPending() } returns listOf(operation)
        coEvery { pendingOperationDao.getRetryable(any()) } returns emptyList()

        worker.doWork()

        coVerify { pendingOperationDao.markInProgress(operation.id) }
        coVerify { pendingOperationDao.markDeadLetter(operation.id) }
        coVerify(exactly = 0) { pendingOperationDao.delete(operation.id) }
        coVerify(exactly = 0) { pendingOperationDao.markFailed(operation.id) }
    }

    private fun pendingOperation(
        payload: String,
        retryCount: Int = 0,
        status: String = OperationStatus.PENDING
    ) = PendingOperationEntity(
        id = 42,
        type = OperationType.ADD_REVIEW,
        payload = payload,
        retryCount = retryCount,
        status = status
    )
}
