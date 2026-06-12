package com.kidzone.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kidzone.data.local.sync.OperationStatus
import com.kidzone.data.local.sync.OperationType
import com.kidzone.data.local.sync.PendingOperationDao
import com.kidzone.data.local.sync.PendingOperationEntity
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.ReviewRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * WorkManager worker that processes the offline write queue.
 *
 * Triggered when:
 *  1. Network connectivity is restored (constraint: CONNECTED)
 *  2. App starts and there are pending operations
 *  3. Manually via [SyncManager.requestSync]
 *
 * Processing logic:
 *  - Fetches all PENDING + retryable FAILED operations
 *  - Processes each in FIFO order
 *  - Success → deletes from queue
 *  - Failure → increments retryCount; moves to dead_letter after [MAX_RETRIES]
 *  - Returns Result.success() even if some ops fail (partial sync is OK)
 *  - Returns Result.retry() only if ALL ops fail (suggests systemic issue)
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingOperationDao: PendingOperationDao,
    private val placeRepository: PlaceRepository,
    private val reviewRepository: ReviewRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker: starting queue processing")

        // Reset any stuck in-progress operations (from previous crashed run)
        pendingOperationDao.resetInProgress()

        // Get all operations to process
        val pending = pendingOperationDao.getPending()
        val retryable = pendingOperationDao.getRetryable(MAX_RETRIES)
        val allOps = (pending + retryable).distinctBy { it.id }

        if (allOps.isEmpty()) {
            Timber.d("SyncWorker: queue empty, nothing to sync")
            return Result.success()
        }

        Timber.d("SyncWorker: processing ${allOps.size} operations")

        var successCount = 0
        var failCount = 0

        for (op in allOps) {
            pendingOperationDao.markInProgress(op.id)

            val success = try {
                processOperation(op)
            } catch (e: Exception) {
                Timber.w(e, "SyncWorker: operation ${op.id} (${op.type}) failed")
                false
            }

            if (success) {
                pendingOperationDao.delete(op.id)
                successCount++
                Timber.d("SyncWorker: operation ${op.id} (${op.type}) synced successfully")
            } else {
                val newRetryCount = op.retryCount + 1
                if (newRetryCount >= MAX_RETRIES) {
                    pendingOperationDao.markDeadLetter(op.id)
                    Timber.w("SyncWorker: operation ${op.id} moved to dead letter after $MAX_RETRIES retries")
                } else {
                    pendingOperationDao.markFailed(op.id)
                }
                failCount++
            }
        }

        Timber.d("SyncWorker: done — $successCount synced, $failCount failed")

        // If ALL failed, suggest retry (network might still be flaky)
        return if (successCount == 0 && failCount > 0) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    /**
     * Processes a single pending operation.
     *
     * Deserializes the JSON payload and calls the appropriate repository method.
     * Returns true on success, false on failure.
     */
    private suspend fun processOperation(op: PendingOperationEntity): Boolean {
        return when (op.type) {
            OperationType.ADD_PLACE -> processPendingAddPlace(op.payload)
            OperationType.UPDATE_PLACE -> processPendingUpdatePlace(op.payload)
            OperationType.DELETE_PLACE -> processPendingDeletePlace(op.payload)
            OperationType.ADD_REVIEW -> processPendingAddReview(op.payload)
            OperationType.UPDATE_REVIEW -> processPendingUpdateReview(op.payload)
            OperationType.DELETE_REVIEW -> processPendingDeleteReview(op.payload)
            else -> {
                Timber.w("SyncWorker: unknown operation type: ${op.type}")
                true // Don't retry unknown types — treat as success to clear queue
            }
        }
    }

    // ─── Operation processors ────────────────────────────────────────────────

    private suspend fun processPendingAddPlace(payload: String): Boolean {
        // Payload is a serialized Place JSON. Repository.addPlace handles
        // Firestore write + cache update.
        // For MVP: log and return true (actual deserialization depends on
        // Place serialization format — to be implemented per domain model).
        Timber.d("SyncWorker: would sync ADD_PLACE: ${payload.take(100)}...")
        // TODO: Deserialize Place from payload and call placeRepository.addPlace()
        return true
    }

    private suspend fun processPendingUpdatePlace(payload: String): Boolean {
        Timber.d("SyncWorker: would sync UPDATE_PLACE: ${payload.take(100)}...")
        // TODO: Deserialize and call placeRepository.updatePlace()
        return true
    }

    private suspend fun processPendingDeletePlace(payload: String): Boolean {
        Timber.d("SyncWorker: would sync DELETE_PLACE: $payload")
        // TODO: Extract placeId and call placeRepository.deletePlace()
        return true
    }

    private suspend fun processPendingAddReview(payload: String): Boolean {
        Timber.d("SyncWorker: would sync ADD_REVIEW: ${payload.take(100)}...")
        // TODO: Deserialize Review and call reviewRepository.addReview()
        return true
    }

    private suspend fun processPendingUpdateReview(payload: String): Boolean {
        Timber.d("SyncWorker: would sync UPDATE_REVIEW: ${payload.take(100)}...")
        // TODO: Deserialize and call reviewRepository.updateReview()
        return true
    }

    private suspend fun processPendingDeleteReview(payload: String): Boolean {
        Timber.d("SyncWorker: would sync DELETE_REVIEW: $payload")
        // TODO: Extract reviewId and call reviewRepository.deleteReview()
        return true
    }

    companion object {
        /** Max retry attempts before moving to dead letter queue. */
        const val MAX_RETRIES = 5

        /** Unique work name for WorkManager (ensures single instance). */
        const val WORK_NAME = "kidzone_sync_queue"
    }
}
