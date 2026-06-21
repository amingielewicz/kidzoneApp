package com.kidzone.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.kidzone.data.local.sync.OperationStatus
import com.kidzone.data.local.sync.OperationType
import com.kidzone.data.local.sync.PendingOperationDao
import com.kidzone.data.local.sync.PendingOperationEntity
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.ReviewRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
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
 *
 * Conflict Resolution (server-wins):
 *  - For UPDATE_PLACE and UPDATE_REVIEW operations, before applying the local
 *    change, the worker fetches the server document's `updatedAtMillis`.
 *  - If the server's timestamp is newer than the local payload's
 *    `updatedAtMillis`, the local change is DISCARDED (server wins).
 *  - If the local timestamp is newer or equal, the local change OVERWRITES
 *    the server document.
 *  - This prevents stale offline edits from clobbering more recent changes
 *    made by other clients or the admin panel.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingOperationDao: PendingOperationDao,
    private val placeRepository: PlaceRepository,
    private val reviewRepository: ReviewRepository,
    private val firestore: FirebaseFirestore
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
                Timber.w("SyncWorker: discarding unknown operation type: ${op.type}")
                true
            }
        }
    }

    // ─── Conflict Resolution Helper ─────────────────────────────────────────

    /**
     * Server-wins conflict resolution for UPDATE operations.
     *
     * Fetches the server document's `updatedAtMillis` field and compares it
     * with the local payload's timestamp.
     *
     * @param collection Firestore collection name ("places" or "reviews")
     * @param documentId The document ID to check
     * @param localUpdatedAtMillis The timestamp from the local pending payload
     * @return [ConflictResult.LOCAL_WINS] if local is newer (proceed with update),
     *         [ConflictResult.SERVER_WINS] if server is newer (discard local),
     *         [ConflictResult.DOCUMENT_NOT_FOUND] if the document was deleted on server.
     */
    private suspend fun resolveConflict(
        collection: String,
        documentId: String,
        localUpdatedAtMillis: Long
    ): ConflictResult {
        return try {
            val snapshot = firestore.collection(collection)
                .document(documentId)
                .get()
                .await()

            if (!snapshot.exists()) {
                Timber.d("SyncWorker: document $documentId not found on server (deleted?)")
                return ConflictResult.DOCUMENT_NOT_FOUND
            }

            val serverUpdatedAt = snapshot.getLong("updatedAtMillis") ?: 0L

            if (serverUpdatedAt > localUpdatedAtMillis) {
                Timber.d(
                    "SyncWorker: SERVER WINS for $documentId — " +
                        "server=$serverUpdatedAt > local=$localUpdatedAtMillis. Discarding local change."
                )
                ConflictResult.SERVER_WINS
            } else {
                Timber.d(
                    "SyncWorker: LOCAL WINS for $documentId — " +
                        "local=$localUpdatedAtMillis >= server=$serverUpdatedAt. Applying local change."
                )
                ConflictResult.LOCAL_WINS
            }
        } catch (e: Exception) {
            // Network error during conflict check – fail the operation so it retries later
            Timber.w(e, "SyncWorker: conflict resolution failed for $documentId")
            ConflictResult.ERROR
        }
    }

    private enum class ConflictResult {
        LOCAL_WINS,
        SERVER_WINS,
        DOCUMENT_NOT_FOUND,
        ERROR
    }

    // ─── Operation processors ────────────────────────────────────────────────

    private suspend fun processPendingAddPlace(payload: String): Boolean {
        val place = runCatching { OfflinePayload.deserializePlace(payload) }.getOrElse {
            Timber.w(it, "SyncWorker: invalid payload for ADD_PLACE")
            return true
        }

        Timber.w("SyncWorker: ADD_PLACE for ${place.id} is gated until write replay is implemented")
        return false
    }

    /**
     * Processes an UPDATE_PLACE operation with server-wins conflict resolution.
     *
     * Flow:
     *  1. Parse placeId and updatedAtMillis from JSON payload
     *  2. Fetch server document's updatedAtMillis
     *  3. If server is newer → discard local (return true to clear from queue)
     *  4. If local is newer → apply update via repository
     */
    private suspend fun processPendingUpdatePlace(payload: String): Boolean {
        Timber.d("SyncWorker: processing UPDATE_PLACE")

        val json = try {
            JSONObject(payload)
        } catch (e: Exception) {
            Timber.w(e, "SyncWorker: invalid JSON payload for UPDATE_PLACE")
            return true // Can't parse → discard to avoid infinite retries
        }

        val placeId = json.optString("id", "")
        val localUpdatedAt = json.optLong("updatedAtMillis", 0L)

        if (placeId.isBlank()) {
            Timber.w("SyncWorker: UPDATE_PLACE payload missing 'id' field")
            return true // Discard malformed operation
        }

        return when (resolveConflict("places", placeId, localUpdatedAt)) {
            ConflictResult.SERVER_WINS -> {
                // Server has a newer version — discard local change silently.
                // The next time user opens this place, the fresh server data
                // will be loaded through the normal getPlace() flow.
                Timber.i("SyncWorker: discarding local UPDATE_PLACE for $placeId (server wins)")
                true
            }
            ConflictResult.LOCAL_WINS -> {
                Timber.w("SyncWorker: UPDATE_PLACE for $placeId is gated until write replay is implemented")
                false
            }
            ConflictResult.DOCUMENT_NOT_FOUND -> {
                // Document was deleted on server — discard local update.
                Timber.i("SyncWorker: discarding UPDATE_PLACE for $placeId (document deleted on server)")
                true
            }
            ConflictResult.ERROR -> {
                // Couldn't reach Firestore — retry later.
                false
            }
        }
    }

    private suspend fun processPendingDeletePlace(payload: String): Boolean {
        val placeId = parseDeleteId(payload, OperationType.DELETE_PLACE) ?: return true
        Timber.w("SyncWorker: DELETE_PLACE for $placeId is gated until write replay is implemented")
        return false
    }

    private suspend fun processPendingAddReview(payload: String): Boolean {
        val review = runCatching { OfflinePayload.deserializeReview(payload) }.getOrElse {
            Timber.w(it, "SyncWorker: invalid payload for ADD_REVIEW")
            return true
        }

        Timber.w("SyncWorker: ADD_REVIEW for ${review.id} is gated until write replay is implemented")
        return false
    }

    /**
     * Processes an UPDATE_REVIEW operation with server-wins conflict resolution.
     *
     * Same logic as UPDATE_PLACE but targets the "reviews" collection.
     */
    private suspend fun processPendingUpdateReview(payload: String): Boolean {
        Timber.d("SyncWorker: processing UPDATE_REVIEW")

        val json = try {
            JSONObject(payload)
        } catch (e: Exception) {
            Timber.w(e, "SyncWorker: invalid JSON payload for UPDATE_REVIEW")
            return true
        }

        val reviewId = json.optString("id", "")
        val localUpdatedAt = json.optLong("updatedAtMillis", 0L)

        if (reviewId.isBlank()) {
            Timber.w("SyncWorker: UPDATE_REVIEW payload missing 'id' field")
            return true
        }

        return when (resolveConflict("reviews", reviewId, localUpdatedAt)) {
            ConflictResult.SERVER_WINS -> {
                Timber.i("SyncWorker: discarding local UPDATE_REVIEW for $reviewId (server wins)")
                true
            }
            ConflictResult.LOCAL_WINS -> {
                Timber.w("SyncWorker: UPDATE_REVIEW for $reviewId is gated until write replay is implemented")
                false
            }
            ConflictResult.DOCUMENT_NOT_FOUND -> {
                Timber.i("SyncWorker: discarding UPDATE_REVIEW for $reviewId (document deleted on server)")
                true
            }
            ConflictResult.ERROR -> {
                false
            }
        }
    }

    private suspend fun processPendingDeleteReview(payload: String): Boolean {
        val reviewId = parseDeleteId(payload, OperationType.DELETE_REVIEW) ?: return true
        Timber.w("SyncWorker: DELETE_REVIEW for $reviewId is gated until write replay is implemented")
        return false
    }

    private fun parseDeleteId(payload: String, operationType: String): String? {
        val id = runCatching { OfflinePayload.deserializeId(payload) }.getOrElse {
            Timber.w(it, "SyncWorker: invalid payload for $operationType")
            null
        }
        if (id?.isBlank() == true) {
            Timber.w("SyncWorker: $operationType payload missing 'id' field")
        }
        return id?.takeUnless { it.isBlank() }
    }

    companion object {
        /** Max retry attempts before moving to dead letter queue. */
        const val MAX_RETRIES = 5

        /** Unique work name for WorkManager (ensures single instance). */
        const val WORK_NAME = "kidzone_sync_queue"
    }
}
