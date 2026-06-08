package com.kidzone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kidzone.domain.model.Review

/**
 * Room entity reprezentujący opinię w lokalnym cache.
 *
 * Mapowanie 1:1 z [Review] z warstwy domain. Lista photoUrls przechowywana
 * jako pipe-separated string (analogicznie do PlaceEntity).
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
        photoUrls = photoUrls
            .split("|")
            .filter { it.isNotBlank() },
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
            photoUrls = review.photoUrls.joinToString("|"),
            createdAtMillis = review.createdAtMillis,
            updatedAtMillis = review.updatedAtMillis
        )
    }
}
