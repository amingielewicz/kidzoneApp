package com.kidzone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie lokalnym cache'em opinii użytkowników.
 * - Udostępnianie reaktywnych strumieni opinii dla konkretnych miejsc i profilu użytkownika.
 *
 * 🔌 Strategia Cache:
 * - Read-Cache zasilany przez Snapshot Listeners z Firestore.
 * - Pełne wsparcie dla pracy w trybie offline.
 *
 * ✅ Gwarancje:
 * - Automatyczne odświeżanie UI po pobraniu nowych opinii w tle.
 */
@Dao
interface ReviewDao {

    /** Opinie dla danego miejsca (live Flow). */
    @Query("SELECT * FROM reviews WHERE placeId = :placeId ORDER BY createdAtMillis DESC")
    fun observeByPlace(placeId: String): Flow<List<ReviewEntity>>

    /** Opinie danego użytkownika (live Flow). */
    @Query("SELECT * FROM reviews WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeByUser(userId: String): Flow<List<ReviewEntity>>

    /** Wstawianie / nadpisywanie listy opinii (bulk upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reviews: List<ReviewEntity>)

    /** Upsert pojedynczej opinii. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: ReviewEntity)

    /** Usunięcie opinii z cache po id. */
    @Query("DELETE FROM reviews WHERE id = :reviewId")
    suspend fun deleteById(reviewId: String)

    /** Usunięcie wszystkich opinii dla danego miejsca. */
    @Query("DELETE FROM reviews WHERE placeId = :placeId")
    suspend fun deleteByPlace(placeId: String)

    /** Czyszczenie całego cache opinii. */
    @Query("DELETE FROM reviews")
    suspend fun clearAll()
}
