package com.kidzone.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Główna baza danych Room aplikacji kidZone.
 *
 * Zawiera tabele:
 *  - `places` – offline cache miejsc,
 *  - `reviews` – offline cache opinii.
 *
 * exportSchema = false – nie generujemy JSON-ów schematów,
 * bo nie potrzebujemy migracji (cache można wyczyścić i załadować ponownie).
 *
 * version = 2 – dodano tabelę `reviews`. fallbackToDestructiveMigration()
 * w DatabaseModule zapewnia, że stara baza zostanie usunięta i odtworzona.
 */
@Database(
    entities = [PlaceEntity::class, ReviewEntity::class],
    version = 2,
    exportSchema = false
)
abstract class KidZoneDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun reviewDao(): ReviewDao
}
