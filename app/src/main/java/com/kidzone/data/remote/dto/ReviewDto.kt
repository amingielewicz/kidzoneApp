package com.kidzone.data.remote.dto

import com.kidzone.domain.model.Review

/**
 * Reprezentacja [Review] w kolekcji `reviews` w Firestore.
 */
data class ReviewDto(
    val id: String = "",
    val placeId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val photoUrls: List<String> = emptyList(),
    /**
     * `Any?` celowo: stare dokumenty zawierają listę, nowe mapę URL -> hash.
     * [toDomain] akceptuje wyłącznie nowy, jednoznaczny format.
     */
    val photoHashes: Any? = emptyMap<String, String>(),
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val reportedAsSpam: Boolean = false
) {
    fun toDomain(): Review = Review(
        id = id,
        placeId = placeId,
        userId = userId,
        authorName = authorName,
        rating = rating,
        comment = comment,
        photoUrls = photoUrls,
        photoHashes = photoHashes.toPhotoHashMap(),
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )

    companion object {
        fun fromDomain(review: Review): ReviewDto = ReviewDto(
            id = review.id,
            placeId = review.placeId,
            userId = review.userId,
            authorName = review.authorName,
            rating = review.rating,
            comment = review.comment,
            photoUrls = review.photoUrls,
            photoHashes = review.photoHashes,
            createdAtMillis = review.createdAtMillis,
            updatedAtMillis = review.updatedAtMillis
        )
    }
}

private fun Any?.toPhotoHashMap(): Map<String, String> =
    (this as? Map<*, *>)
        ?.mapNotNull { (url, hash) ->
            if (url is String && hash is String) url to hash else null
        }
        ?.toMap()
        .orEmpty()
