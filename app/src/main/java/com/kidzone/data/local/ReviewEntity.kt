package com.kidzone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kidzone.data.local.LocalMapperUtils.fromPipeSeparated
import com.kidzone.data.local.LocalMapperUtils.toPhotoHashes
import com.kidzone.data.local.LocalMapperUtils.toPipeSeparated
import com.kidzone.data.local.LocalMapperUtils.toJson
import com.kidzone.domain.model.Review

/**
 * ⚙️ Techniczne:
 * Reprezentacja opinii w lokalnej bazie danych Room.
 *
 * @property photoUrls Adresy URL zdjęć połączone znakiem pipe (|).
 * @property photoHashes Mapa (JSON) URL -> Hash MD5 dla spójnej deduplikacji offline.
 */
@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val placeId: String,
    val userId: String,
    val authorName: String,
    val rating: Int,
    val comment: String,
    val photoUrls: String, // pipe-separated URLs
    val photoHashes: String = "{}", // JSON map: photo URL -> MD5 hash
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    /** Czas ostatniego zapisu do cache – do ewentualnej polityki TTL. */
    val cachedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain(): Review = Review(
        id = id,
        placeId = placeId,
        userId = userId,
        authorName = authorName,
        rating = rating,
        comment = comment,
        photoUrls = photoUrls.fromPipeSeparated(),
        photoHashes = photoHashes.toPhotoHashes(),
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )

    companion object {
        fun fromDomain(review: Review): ReviewEntity = ReviewEntity(
            id = review.id,
            placeId = review.placeId,
            userId = review.userId,
            authorName = review.authorName,
            rating = review.rating,
            comment = review.comment,
            photoUrls = review.photoUrls.toPipeSeparated(),
            photoHashes = review.photoHashes.toJson(),
            createdAtMillis = review.createdAtMillis,
            updatedAtMillis = review.updatedAtMillis
        )
    }
}
