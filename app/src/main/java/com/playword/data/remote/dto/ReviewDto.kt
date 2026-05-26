package com.playword.data.remote.dto

import com.playword.domain.model.Review

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
    val createdAtMillis: Long = 0L,
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
        createdAtMillis = createdAtMillis
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
            createdAtMillis = review.createdAtMillis
        )
    }
}
