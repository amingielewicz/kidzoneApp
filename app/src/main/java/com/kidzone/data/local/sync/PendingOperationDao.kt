package com.kidzone.data.local.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie cyklem życia operacji w kolejce synchronizacji (Sync Queue).
 * - Obsługa stanów przejścia operacji: PENDING -> IN_PROGRESS -> FAILED/RESOLVED.
 * - Utrzymywanie porządku FIFO (First-In-First-Out).
 *
 * ✅ Gwarancje:
 * - Atomowość zmian stanu w obrębie tabeli `pending_operations`.
 * - Automatyczny powrót operacji "utkniętych" (in_progress) do stanu pending po restarcie.
 */
@Dao
interface PendingOperationDao {

    /** Dodaje operację do kolejki. */
    @Insert
    suspend fun enqueue(operation: PendingOperationEntity): Long

    /** Pobiera wszystkie oczekujące operacje w porządku FIFO. */
    @Query("SELECT * FROM pending_operations WHERE status = 'pending' ORDER BY createdAtMillis ASC")
    suspend fun getPending(): List<PendingOperationEntity>

    /** Obserwuje liczbę oczekujących operacji (dla licznika w UI). */
    @Query("SELECT COUNT(*) FROM pending_operations WHERE status IN ('pending', 'failed')")
    fun observePendingCount(): Flow<Int>

    /** Pobiera liczbę oczekujących operacji (jednorazowo). */
    @Query("SELECT COUNT(*) FROM pending_operations WHERE status IN ('pending', 'failed')")
    suspend fun getPendingCount(): Int

    /** Oznacza operację jako "w toku". */
    @Query("UPDATE pending_operations SET status = 'in_progress' WHERE id = :id")
    suspend fun markInProgress(id: Long)

    /** Oznacza operację jako błąd i zwiększa licznik ponowień. */
    @Query("UPDATE pending_operations SET status = 'failed', retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markFailed(id: Long)

    /** Przenosi do dead letter (trwały błąd). */
    @Query("UPDATE pending_operations SET status = 'dead_letter' WHERE id = :id")
    suspend fun markDeadLetter(id: Long)

    /** Usuwa pomyślnie zsynchronizowaną operację. */
    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun delete(id: Long)

    /** Pobiera operacje do ponowienia (licznik < limit). */
    @Query("SELECT * FROM pending_operations WHERE status = 'failed' AND retryCount < :maxRetries ORDER BY createdAtMillis ASC")
    suspend fun getRetryable(maxRetries: Int): List<PendingOperationEntity>

    /** Usuwa wszystkie operacje z dead-letter (akcja administratora). */
    @Query("DELETE FROM pending_operations WHERE status = 'dead_letter'")
    suspend fun clearDeadLetters()

    /** Resetuje operacje "utknięte" z powrotem do stanu oczekiwania (odzyskiwanie po awarii). */
    @Query("UPDATE pending_operations SET status = 'pending' WHERE status = 'in_progress'")
    suspend fun resetInProgress()

    /** Pobiera wszystkie operacje (cele diagnostyczne). */
    @Query("SELECT * FROM pending_operations ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<PendingOperationEntity>
}
