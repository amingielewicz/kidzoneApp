package com.kidzone.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kidzone.data.local.sync.PendingOperationDao
import com.kidzone.data.local.sync.PendingOperationEntity

/**
 * Główna baza danych Room aplikacji kidZone.
 *
 * Zawiera tabele:
 *  - `places` – offline cache miejsc,
 *  - `reviews` – offline cache opinii,
 *  - `pending_operations` – offline write queue (sync when online).
 *
 * exportSchema = false – nie generujemy JSON-ów schematów,
 * bo nie potrzebujemy migracji (cache można wyczyścić i załadować ponownie).
 *
 * version = 3 – dodano tabelę `pending_operations` (offline write queue).
 * fallbackToDestructiveMigration() w DatabaseModule zapewnia, że stara baza
 * zostanie usunięta i odtworzona.
 */
@Database(
    entities = [PlaceEntity::class, ReviewEntity::class, PendingOperationEntity::class],
    version = 5,
    exportSchema = false
)
abstract class KidZoneDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun reviewDao(): ReviewDao
    abstract fun pendingOperationDao(): PendingOperationDao
}
