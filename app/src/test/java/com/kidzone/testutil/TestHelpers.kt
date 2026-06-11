package com.kidzone.testutil

import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.model.Review
import com.kidzone.domain.model.User

/**
 * Factory functions for creating test fixtures with sensible defaults.
 */
object TestFixtures {

    fun user(
        id: String = "user-1",
        name: String = "Test User",
        email: String = "test@example.com",
        firstName: String = "",
        lastName: String = "",
        avatarUrl: String? = null,
        placesAddedCount: Int = 0,
        reviewsCount: Int = 0,
        createdAtMillis: Long = 1_700_000_000_000L,
        nameLowercase: String = name.lowercase(),
        bannedUntilMillis: Long = 0L,
        banReason: String = ""
    ) = User(
        id = id,
        name = name,
        email = email,
        firstName = firstName,
        lastName = lastName,
        avatarUrl = avatarUrl,
        placesAddedCount = placesAddedCount,
        reviewsCount = reviewsCount,
        createdAtMillis = createdAtMillis,
        nameLowercase = nameLowercase,
        bannedUntilMillis = bannedUntilMillis,
        banReason = banReason
    )

    fun place(
        id: String = "place-1",
        ownerUserId: String = "user-1",
        name: String = "Plac Zabaw Kasztanowa",
        description: String = "Fajny plac zabaw",
        category: PlaceCategory = PlaceCategory.PLAYGROUND,
        latitude: Double = 52.2297,
        longitude: Double = 21.0122,
        address: String = "ul. Kasztanowa 1",
        averageRating: Double = 4.5,
        reviewsCount: Int = 10,
        amenities: Set<Amenity> = emptySet(),
        photoUrls: List<String> = emptyList(),
        createdAtMillis: Long = 1_700_000_000_000L
    ) = Place(
        id = id,
        ownerUserId = ownerUserId,
        name = name,
        description = description,
        category = category,
        latitude = latitude,
        longitude = longitude,
        address = address,
        averageRating = averageRating,
        reviewsCount = reviewsCount,
        amenities = amenities,
        photoUrls = photoUrls,
        createdAtMillis = createdAtMillis
    )

    fun review(
        id: String = "review-1",
        placeId: String = "place-1",
        userId: String = "user-1",
        authorName: String = "Test User",
        rating: Int = 4,
        comment: String = "Świetne miejsce!",
        createdAtMillis: Long = 1_700_000_000_000L
    ) = Review(
        id = id,
        placeId = placeId,
        userId = userId,
        authorName = authorName,
        rating = rating,
        comment = comment,
        createdAtMillis = createdAtMillis
    )
}
