package com.kidzone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie lokalnym cache'em miejsc w bazie danych Room.
 * - Realizacja zapytań przestrzennych (bounding boxes) i wyszukiwania tekstowego na danych lokalnych.
 * - Obsługa TTL (Time To Live) dla wpisów w cache.
 *
 * 🔌 Strategia Cache:
 * - Room służy jako Read-Cache dla warstwy prezentacji.
 * - [OnConflictStrategy.REPLACE] zapewnia spójność przy dociąganiu nowszych wersji dokumentów z Firestore.
 *
 * ✅ Gwarancje:
 * - Flow automatycznie emituje nową listę przy każdej zmianie w tabeli `places`.
 * - Deterministyczne sortowanie (po dacie utworzenia lub ocenie).
 */
@Dao
interface PlaceDao {

    /** Ograniczona, najnowsza część cache'u jako Flow. */
    @Query("SELECT * FROM places ORDER BY createdAtMillis DESC LIMIT :limit")
    fun observeAll(limit: Int): Flow<List<PlaceEntity>>

    /** Miejsca filtrowane po kategorii. */
    @Query("SELECT * FROM places WHERE category = :category ORDER BY createdAtMillis DESC LIMIT :limit")
    fun observeByCategory(category: String, limit: Int): Flow<List<PlaceEntity>>

    /** Miejsca filtrowane po nazwie (wyszukiwarka). */
    @Query("SELECT * FROM places WHERE name LIKE '%' || :query || '%' ORDER BY createdAtMillis DESC LIMIT :limit")
    fun observeByName(query: String, limit: Int): Flow<List<PlaceEntity>>

    /** Miejsca filtrowane po kategorii i nazwie. */
    @Query(
        "SELECT * FROM places WHERE category = :category " +
            "AND name LIKE '%' || :query || '%' ORDER BY createdAtMillis DESC LIMIT :limit"
    )
    fun observeByCategoryAndName(category: String, query: String, limit: Int): Flow<List<PlaceEntity>>

    /** Miejsca dodane przez konkretnego usera. */
    @Query("SELECT * FROM places WHERE ownerUserId = :userId ORDER BY createdAtMillis DESC LIMIT :limit")
    fun observeByOwner(userId: String, limit: Int): Flow<List<PlaceEntity>>

    /** Pojedyncze miejsce po id (one-shot). */
    @Query("SELECT * FROM places WHERE id = :placeId LIMIT 1")
    suspend fun getById(placeId: String): PlaceEntity?

    /** Top N miejsc wg averageRating. */
    @Query("SELECT * FROM places ORDER BY averageRating DESC LIMIT :limit")
    suspend fun getTopPlaces(limit: Int): List<PlaceEntity>

    /** Ograniczony fallback dla zapytań geo, sortowany od najświeższych danych. */
    @Query("SELECT * FROM places ORDER BY cachedAtMillis DESC LIMIT :limit")
    suspend fun getRecentPlaces(limit: Int): List<PlaceEntity>

    /** Ograniczony fallback mapy dla zwykłego viewportu. */
    @Query(
        "SELECT * FROM places WHERE latitude BETWEEN :south AND :north " +
            "AND longitude BETWEEN :west AND :east " +
            "ORDER BY cachedAtMillis DESC LIMIT :limit"
    )
    suspend fun getPlacesInBounds(
        north: Double,
        east: Double,
        south: Double,
        west: Double,
        limit: Int
    ): List<PlaceEntity>

    /** Fallback mapy dla viewportu przecinającego południk 180°. */
    @Query(
        "SELECT * FROM places WHERE latitude BETWEEN :south AND :north " +
            "AND (longitude >= :west OR longitude <= :east) " +
            "ORDER BY cachedAtMillis DESC LIMIT :limit"
    )
    suspend fun getPlacesInWrappedBounds(
        north: Double,
        east: Double,
        south: Double,
        west: Double,
        limit: Int
    ): List<PlaceEntity>

    /** Strona cache'u zgodna z filtrami listy. */
    @Query(
        "SELECT * FROM places " +
            "WHERE (:category IS NULL OR category = :category) " +
            "AND (:query IS NULL OR name LIKE '%' || :query || '%') " +
            "ORDER BY createdAtMillis DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun getPlacesPage(
        category: String?,
        query: String?,
        limit: Int,
        offset: Int
    ): List<PlaceEntity>

    /** All places for widget (fetch all, distance calculated in memory). */
    @Query("SELECT * FROM places")
    suspend fun getAllPlaces(): List<PlaceEntity>

    /** Wstawianie / nadpisywanie listy miejsc (bulk upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(places: List<PlaceEntity>)

    /** Upsert pojedynczego miejsca (po dodaniu / edycji). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(place: PlaceEntity)

    /** Usunięcie miejsca z cache. */
    @Query("DELETE FROM places WHERE id = :placeId")
    suspend fun deleteById(placeId: String)

    /** Usunięcie wpisów starszych niż podany timestamp (TTL gc). */
    @Query("DELETE FROM places WHERE cachedAtMillis < :olderThan")
    suspend fun deleteStale(olderThan: Long)

    /** Czyszczenie całego cache (np. po wylogowaniu lub force-refresh). */
    @Query("DELETE FROM places")
    suspend fun clearAll()
}
