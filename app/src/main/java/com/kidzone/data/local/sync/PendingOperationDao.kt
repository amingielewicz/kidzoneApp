package com.kidzone.data.local.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the pending operations sync queue.
 *
 * Operations are processed FIFO (ordered by createdAtMillis).
 * Failed operations are retried with exponential backoff up to [MAX_RETRIES].
 */
@Dao
interface PendingOperationDao {

    /** Enqueue a new operation. */
    @Insert
    suspend fun enqueue(operation: PendingOperationEntity): Long

    /** Get all pending operations ordered by creation time (FIFO). */
    @Query("SELECT * FROM pending_operations WHERE status = 'pending' ORDER BY createdAtMillis ASC")
    suspend fun getPending(): List<PendingOperationEntity>

    /** Get count of pending operations (for UI badge/indicator). */
    @Query("SELECT COUNT(*) FROM pending_operations WHERE status IN ('pending', 'failed')")
    fun observePendingCount(): Flow<Int>

    /** Get pending count (one-shot). */
    @Query("SELECT COUNT(*) FROM pending_operations WHERE status IN ('pending', 'failed')")
    suspend fun getPendingCount(): Int

    /** Mark operation as in-progress. */
    @Query("UPDATE pending_operations SET status = 'in_progress' WHERE id = :id")
    suspend fun markInProgress(id: Long)

    /** Mark operation as failed and increment retry count. */
    @Query("UPDATE pending_operations SET status = 'failed', retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markFailed(id: Long)

    /** Move to dead letter (permanently failed). */
    @Query("UPDATE pending_operations SET status = 'dead_letter' WHERE id = :id")
    suspend fun markDeadLetter(id: Long)

    /** Remove successfully synced operation. */
    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun delete(id: Long)

    /** Get failed operations eligible for retry (retryCount < maxRetries). */
    @Query("SELECT * FROM pending_operations WHERE status = 'failed' AND retryCount < :maxRetries ORDER BY createdAtMillis ASC")
    suspend fun getRetryable(maxRetries: Int): List<PendingOperationEntity>

    /** Clear all dead-letter operations (admin/debug action). */
    @Query("DELETE FROM pending_operations WHERE status = 'dead_letter'")
    suspend fun clearDeadLetters()

    /** Reset in-progress operations back to pending (app restart recovery). */
    @Query("UPDATE pending_operations SET status = 'pending' WHERE status = 'in_progress'")
    suspend fun resetInProgress()

    /** Get all operations (for debug/admin). */
    @Query("SELECT * FROM pending_operations ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<PendingOperationEntity>
}
