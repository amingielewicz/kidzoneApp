package com.kidzone.data.local.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a pending write operation queued while offline.
 *
 * When the user performs a write (add place, add review, edit profile, etc.)
 * without network connectivity, the operation is serialized to this entity
 * and stored in Room. When connectivity returns, [com.kidzone.sync.SyncWorker]
 * processes the queue in FIFO order.
 *
 * @property id Auto-generated primary key
 * @property type Operation type (see [OperationType])
 * @property payload JSON-serialized operation data (varies by type)
 * @property createdAtMillis Timestamp when operation was enqueued
 * @property retryCount Number of failed sync attempts (exponential backoff)
 * @property status Current status of the operation
 */
@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val payload: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = OperationStatus.PENDING
)

/**
 * Types of operations that can be queued offline.
 */
object OperationType {
    const val ADD_PLACE = "add_place"
    const val UPDATE_PLACE = "update_place"
    const val DELETE_PLACE = "delete_place"
    const val ADD_REVIEW = "add_review"
    const val UPDATE_REVIEW = "update_review"
    const val DELETE_REVIEW = "delete_review"
    const val UPDATE_PROFILE = "update_profile"
    const val REPORT_PLACE = "report_place"
    const val REPORT_REVIEW = "report_review"
    const val REPORT_PHOTO = "report_photo"
}

/**
 * Status of a pending operation in the sync queue.
 */
object OperationStatus {
    /** Waiting to be synced. */
    const val PENDING = "pending"
    /** Currently being synced (in-flight). */
    const val IN_PROGRESS = "in_progress"
    /** Sync failed, will retry (up to max retries). */
    const val FAILED = "failed"
    /** Permanently failed (max retries exceeded). */
    const val DEAD_LETTER = "dead_letter"
}
