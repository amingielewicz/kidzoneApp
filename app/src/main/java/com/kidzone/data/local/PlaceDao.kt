package com.kidzone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO dla tabeli `places` – operacje cache'owania miejsc.
 *
 * Strategia: Firestore jest source-of-truth, Room to read-cache.
 * Przy każdym uaktualnieniu z Firestore nadpisujemy (REPLACE) lokalne
 * encje i usuwamy te, których już nie ma w zdalnym zbiorze.
 */
@Dao
interface PlaceDao {

    /** Wszystkie miejsca z cache'u, jako Flow (Room LiveData-like). */
    @Query("SELECT * FROM places ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<PlaceEntity>>

    /** Miejsca filtrowane po kategorii. */
    @Query("SELECT * FROM places WHERE category = :category ORDER BY createdAtMillis DESC")
    fun observeByCategory(category: String): Flow<List<PlaceEntity>>

    /** Miejsca dodane przez konkretnego usera. */
    @Query("SELECT * FROM places WHERE ownerUserId = :userId ORDER BY createdAtMillis DESC")
    fun observeByOwner(userId: String): Flow<List<PlaceEntity>>

    /** Pojedyncze miejsce po id (one-shot). */
    @Query("SELECT * FROM places WHERE id = :placeId LIMIT 1")
    suspend fun getById(placeId: String): PlaceEntity?

    /** Top N miejsc wg averageRating. */
    @Query("SELECT * FROM places ORDER BY averageRating DESC LIMIT :limit")
    suspend fun getTopPlaces(limit: Int): List<PlaceEntity>

    /** Wstawianie / nadpisywanie listy miejsc (bulk upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(places: List<PlaceEntity>)

    /** Upsert pojedynczego miejsca (po dodaniu / edycji). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(place: PlaceEntity)

    /** Usunięcie miejsca z cache. */
    @Query("DELETE FROM places WHERE id = :placeId")
    suspend fun deleteById(placeId: String)

    /** Czyszczenie całego cache (np. po wylogowaniu lub force-refresh). */
    @Query("DELETE FROM places")
    suspend fun clearAll()
}
