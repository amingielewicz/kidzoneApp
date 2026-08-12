package com.kidzone.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kidzone.data.local.sync.PendingOperationDao
import com.kidzone.data.local.sync.PendingOperationEntity

/**
 * 🎯 Odpowiedzialności:
 * - Główny magazyn danych lokalnych aplikacji kidZone (Room).
 * - Zarządzanie tabelami cache'u (miejsca, opinie) oraz kolejką operacji oczekujących.
 *
 * ⚙️ Techniczne:
 * - `version = 7`: Dodano `tosAcceptedAtMillis` do `UserEntity`.
 * - `exportSchema = false`: Schematy nie są eksportowane (używamy czyszczenia cache przy zmianach).
 *
 * ✅ Gwarancje:
 * - `fallbackToDestructiveMigration()`: Automatyczne odświeżenie bazy przy zmianie schematu (zapobiega crashom).
 */
@Database(
    entities = [PlaceEntity::class, ReviewEntity::class, PendingOperationEntity::class, UserEntity::class],
    version = 7,
    exportSchema = false
)
abstract class KidZoneDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun reviewDao(): ReviewDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun userDao(): UserDao
}
