package com.kidzone.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Główna baza danych Room aplikacji kidZone.
 *
 * Na razie zawiera jedną tabelę `places` (offline cache miejsc).
 * W przyszłości może dojść `reviews`, `users` itp.
 *
 * exportSchema = false – nie generujemy JSON-ów schematów,
 * bo nie potrzebujemy migracji (cache można wyczyścić i załadować ponownie).
 */
@Database(
    entities = [PlaceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class KidZoneDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
}
