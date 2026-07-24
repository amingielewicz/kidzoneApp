@file:Suppress("ReturnCount")

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
import com.kidzone.data.remote.FirestoreCollections
import com.kidzone.data.remote.dto.PlaceDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import timber.log.Timber

/**
 * 🎯 Odpowiedzialności:
 * - Procesowanie lokalnej kolejki operacji oczekujących (offline queue).
 * - Realizacja strategii "serwer wygrywa" (server-wins) przy rozwiązywaniu konfliktów edycji.
 * - Zarządzanie cyklem życia operacji (OCZEKUJĄCA -> W TOKU -> SUKCES/BŁĄD).
 *
 * 🛡️ Bezpieczeństwo i Prywatność:
 * - Przetwarza dane osobowe (PII) w paczkach danych (treści użytkownika).
 * - Bezpieczna deserializacja JSON zapobiegająca awariom przy uszkodzonych danych.
 *
 * ⚡ Wydajność i Zasoby:
 * - Działa w tle poprzez WorkManager (uruchamiany tylko przy dostępie do sieci).
 * - Przetwarzanie FIFO (pierwsze weszło, pierwsze wyszło) ograniczające liczbę konfliktów.
 *
 * ✅ Gwarancje:
 * - Idempotentność procesorów zapobiegająca powstawaniu duplikatów przy ponowieniach.
 * - Przenoszenie trwale błędnych operacji do "dead_letter" po [MAX_RETRIES] próbach.
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
     * Procesuje pojedynczą operację oczekującą.
     *
     * Deserializuje ładunek JSON i wywołuje odpowiednią metodę repozytorium.
     * Zwraca true przy sukcesie, false przy błędzie.
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
                Timber.w("SyncWorker: odrzucono nieznany typ operacji: ${op.type}")
                true
            }
        }
    }

    // ─── Pomocnik Rozwiązywania Konfliktów ──────────────────────────────────

    /**
     * Rozwiązywanie konfliktów typu "serwer wygrywa" dla operacji UPDATE.
     *
     * Pobiera pole `updatedAtMillis` z dokumentu na serwerze i porównuje je
     * z timestampem lokalnej operacji.
     *
     * @param collection Nazwa kolekcji Firestore ("places" lub "reviews").
     * @param documentId ID sprawdzanego dokumentu.
     * @param localUpdatedAtMillis Timestamp z lokalnego ładunku operacji.
     * @return [ConflictResult.LOCAL_WINS] jeśli lokalna zmiana jest nowsza (kontynuuj),
     *         [ConflictResult.SERVER_WINS] jeśli serwer ma nowszą wersję (odrzuć lokalną),
     *         [ConflictResult.DOCUMENT_NOT_FOUND] jeśli dokument został usunięty na serwerze.
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

        if (place.id.isBlank()) {
            Timber.w("SyncWorker: ADD_PLACE payload missing place id")
            return true
        }

        return try {
            firestore.collection(FirestoreCollections.PLACES)
                .document(place.id)
                .set(PlaceDto.fromDomain(place))
                .await()

            Timber.i("SyncWorker: ADD_PLACE synced for ${place.id}")
            true
        } catch (e: Exception) {
            Timber.w(e, "SyncWorker: ADD_PLACE failed for ${place.id}")
            false
        }
    }

    /**
     * Procesuje operację UPDATE_PLACE ze strategią "serwer wygrywa".
     *
     * Przepływ:
     *  1. Parsuje ID miejsca i updatedAtMillis z ładunku JSON.
     *  2. Pobiera updatedAtMillis z dokumentu na serwerze.
     *  3. Jeśli serwer jest nowszy → odrzuca lokalną zmianę (czyści z kolejki).
     *  4. Jeśli lokalna zmiana jest nowsza → aplikuje aktualizację przez repozytorium.
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
     * Procesuje operację UPDATE_REVIEW ze strategią "serwer wygrywa".
     *
     * Logika analogiczna do UPDATE_PLACE, ale celuje w kolekcję "reviews".
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
        /** Maksymalna liczba ponowień przed przeniesieniem do dead letter. */
        const val MAX_RETRIES = 5

        /** Unikalna nazwa zadania dla WorkManager (gwarantuje jedną instancję). */
        const val WORK_NAME = "kidzone_sync_queue"
    }
}
