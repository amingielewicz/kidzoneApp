package com.kidzone.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kidzone.data.local.sync.OperationType
import com.kidzone.data.local.sync.PendingOperationDao
import com.kidzone.data.local.sync.PendingOperationEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the offline write queue and sync scheduling.
 *
 * Offline write replay is currently gated for user-facing writes until
 * [SyncWorker] has production processors for every queued operation. Do not
 * enqueue new place/review writes unless their processor performs the Firestore
 * write instead of only acknowledging the payload.
 *
 * ## How it works:
 *
 * 1. **Enqueue**: When a write operation fails due to no network (or is
 *    explicitly requested for offline-first mode), call [enqueue] with
 *    the operation type and serialized payload.
 *
 * 2. **Schedule**: After enqueue, [requestSync] schedules a [SyncWorker]
 *    with WorkManager constraint: `NetworkType.CONNECTED`. WorkManager
 *    guarantees execution when network is available, even if app is killed.
 *
 * 3. **Process**: [SyncWorker] picks up pending operations FIFO, syncs
 *    each to Firestore, and removes successful ones from the queue.
 *
 * 4. **Retry**: Failed operations are retried with exponential backoff
 *    (10s, 20s, 40s, 80s, 160s). After 5 failures → dead letter.
 *
 * ## Usage in Repository:
 *
 * ```kotlin
 * suspend fun addPlace(place: Place): OpResult<Place> {
 *     return try {
 *         // Try online write first
 *         firestoreWrite(place)
 *     } catch (e: Exception) {
 *         OpResult.failure(e)
 *     }
 * }
 * ```
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingOperationDao: PendingOperationDao
) {

    /**
     * Enqueue a write operation for later sync.
     *
     * @param type Operation type (see [OperationType])
     * @param payload JSON-serialized data for the operation
     * @return ID of the enqueued operation
     */
    suspend fun enqueue(type: String, payload: String): Long {
        val entity = PendingOperationEntity(
            type = type,
            payload = payload,
            createdAtMillis = System.currentTimeMillis()
        )
        val id = pendingOperationDao.enqueue(entity)
        Timber.d("SyncManager: enqueued $type (id=$id)")

        // Schedule sync when network is available
        requestSync()

        return id
    }

    /**
     * Schedule a sync attempt. WorkManager handles:
     * - Waiting for network connectivity
     * - Surviving app/process death
     * - Exponential backoff on failure
     * - Battery-efficient scheduling
     */
    fun requestSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SyncWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )

        Timber.d("SyncManager: sync scheduled (network constraint)")
    }

    /**
     * Observe count of pending operations.
     * UI can show a badge/indicator when > 0.
     */
    fun observePendingCount(): Flow<Int> = pendingOperationDao.observePendingCount()

    /**
     * Get current pending count (one-shot).
     */
    suspend fun getPendingCount(): Int = pendingOperationDao.getPendingCount()

    /**
     * Check if there are pending operations.
     */
    suspend fun hasPendingOperations(): Boolean = getPendingCount() > 0
}
