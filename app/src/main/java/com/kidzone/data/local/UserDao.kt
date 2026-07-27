package com.kidzone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie lokalnym cache'em danych użytkowników.
 * - Udostępnianie danych do budowy rankingów oraz profilu w trybie offline.
 *
 * 🔌 Strategia Cache:
 * - Read-Cache zasilany przy każdym pobieraniu rankingu lub profilu z Firestore.
 * - [OnConflictStrategy.REPLACE] zapewnia aktualność statystyk i danych osobowych.
 */
@Dao
interface UserDao {
    /** Pobiera profil użytkownika z cache (one-shot). */
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getById(userId: String): UserEntity?

    /** Obserwuje profil użytkownika w cache (Live Flow). */
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeById(userId: String): Flow<UserEntity?>

    /** Top N użytkowników wg placesAddedCount (Ranking). */
    @Query("SELECT * FROM users ORDER BY placesAddedCount DESC, reviewsCount DESC LIMIT :limit")
    suspend fun getTopUsers(limit: Int): List<UserEntity>

    /** Wstawianie / nadpisywanie listy użytkowników (bulk upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(users: List<UserEntity>)

    /** Wstawianie / nadpisywanie pojedynczego użytkownika (np. bieżący profil). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    /** Czyszczenie całego cache użytkowników. */
    @Query("DELETE FROM users")
    suspend fun clearAll()
}
