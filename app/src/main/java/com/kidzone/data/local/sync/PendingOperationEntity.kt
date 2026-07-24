package com.kidzone.data.local.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ⚙️ Techniczne:
 * Rekord operacji oczekującej na przetworzenie przez kolejkę synchronizacji.
 * Encja przechowuje kontrakt pomiędzy Repository, [com.kidzone.sync.SyncManager] i [com.kidzone.sync.SyncWorker].
 *
 * @property id lokalny identyfikator nadawany przez Room.
 * @property type typ operacji z [OperationType].
 * @property payload zserializowane dane (JSON). Może zawierać PII.
 * @property retryCount liczba nieudanych prób przetworzenia.
 * @property status bieżący status z [OperationStatus].
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
 * Nazwy typów operacji obsługiwanych przez lokalną kolejkę.
 *
 * Dodanie stałej nie oznacza automatycznie gotowego replay. Każdy typ wymaga kompletnego processora
 * w `SyncWorker`, idempotencji, obsługi ownership i testów z Firebase Emulator.
 */
object OperationType {
    /** Dodanie miejsca. */
    const val ADD_PLACE = "add_place"

    /** Aktualizacja istniejącego miejsca. */
    const val UPDATE_PLACE = "update_place"

    /** Usunięcie miejsca. */
    const val DELETE_PLACE = "delete_place"

    /** Dodanie opinii. */
    const val ADD_REVIEW = "add_review"

    /** Aktualizacja opinii. */
    const val UPDATE_REVIEW = "update_review"

    /** Usunięcie opinii. */
    const val DELETE_REVIEW = "delete_review"

    /** Aktualizacja profilu użytkownika. */
    const val UPDATE_PROFILE = "update_profile"

    /** Zgłoszenie miejsca. */
    const val REPORT_PLACE = "report_place"

    /** Zgłoszenie opinii. */
    const val REPORT_REVIEW = "report_review"

    /** Zgłoszenie zdjęcia. */
    const val REPORT_PHOTO = "report_photo"
}

/**
 * Status cyklu życia operacji w kolejce synchronizacji.
 */
object OperationStatus {
    /** Operacja oczekuje na pierwszą albo kolejną próbę. */
    const val PENDING = "pending"

    /** Operacja została pobrana przez aktualnie działającego workera. */
    const val IN_PROGRESS = "in_progress"

    /** Próba nie powiodła się, ale operacja może zostać ponowiona. */
    const val FAILED = "failed"

    /** Operacja przekroczyła limit prób i wymaga ręcznej analizy lub jawnej obsługi. */
    const val DEAD_LETTER = "dead_letter"
}
