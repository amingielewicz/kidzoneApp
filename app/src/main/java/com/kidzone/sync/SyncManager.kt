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
 * 🎯 Odpowiedzialności:
 * - Zarządzanie lokalną kolejką operacji oczekujących (zapis z opóźnieniem).
 * - Harmonogramowanie zadań [SyncWorker] poprzez WorkManager.
 *
 * 🛡️ Bezpieczeństwo i Prywatność:
 * - Zawartość operacji może zawierać dane osobowe (PII); zabrania się logowania pełnej treści.
 * - Operacje są powiązane z sesją użytkownika na poziomie bazy danych.
 *
 * ⚡ Wydajność i Zasoby:
 * - Wykorzystuje WorkManager z wykładniczym czasem ponowień (Backoff).
 * - Ogranicza aktywność do momentu uzyskania stabilnego połączenia sieciowego.
 *
 * ✅ Gwarancje:
 * - Trwałość danych (Persistence): operacje przeżywają restart aplikacji i urządzenia.
 * - Kolejkowanie FIFO (pierwsze weszło, pierwsze wyszło) na poziomie bazy danych.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingOperationDao: PendingOperationDao
) {

    /**
     * Dodaje operację do kolejki i planuje próbę synchronizacji.
     *
     * Wywołujący musi przekazać typ obsługiwany przez [SyncWorker]. Metoda nie waliduje semantycznie
     * payloadu i nie powinna być używana jako potwierdzenie zapisu dla UI.
     *
     * @param type typ operacji z [OperationType].
     * @param payload zserializowane dane operacji; nie mogą być zapisywane w logach.
     * @return identyfikator rekordu Room.
     */
    suspend fun enqueue(type: String, payload: String): Long {
        val entity = PendingOperationEntity(
            type = type,
            payload = payload,
            createdAtMillis = System.currentTimeMillis()
        )
        val id = pendingOperationDao.enqueue(entity)
        Timber.d("SyncManager: enqueued $type (id=$id)")
        requestSync()
        return id
    }

    /**
     * Planuje unikalną próbę przetworzenia kolejki po uzyskaniu połączenia z siecią.
     *
     * [ExistingWorkPolicy.REPLACE] zastępuje poprzednie oczekujące zlecenie o tej samej nazwie.
     * WorkManager odpowiada za przetrwanie restartu procesu i wykładniczy backoff.
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
     * Obserwuje liczbę operacji, które nadal oczekują na zakończenie.
     *
     * @return strumień liczby rekordów kwalifikowanych przez DAO jako oczekujące.
     */
    fun observePendingCount(): Flow<Int> = pendingOperationDao.observePendingCount()

    /**
     * Pobiera aktualną liczbę oczekujących operacji.
     */
    suspend fun getPendingCount(): Int = pendingOperationDao.getPendingCount()

    /**
     * Sprawdza, czy kolejka zawiera operacje oczekujące.
     */
    suspend fun hasPendingOperations(): Boolean = getPendingCount() > 0
}
