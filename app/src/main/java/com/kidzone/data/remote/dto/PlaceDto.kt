package com.kidzone.data.remote.dto

import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory

/**
 * Reprezentacja [Place] w kolekcji `places` w Firestore.
 */
data class PlaceDto(
    val id: String = "",
    val ownerUserId: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = PlaceCategory.OTHER.name,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val averageRating: Double = 0.0,
    val reviewsCount: Int = 0,
    val amenities: List<String> = emptyList(),
    val photoUrls: List<String> = emptyList(),
    val createdAtMillis: Long = 0L
) {
    fun toDomain(): Place = Place(
        id = id,
        ownerUserId = ownerUserId,
        name = name,
        description = description,
        category = PlaceCategory.fromKey(category),
        latitude = latitude,
        longitude = longitude,
        address = address,
        averageRating = averageRating,
        reviewsCount = reviewsCount,
        amenities = amenities.mapNotNull(Amenity.Companion::fromKey).toSet(),
        photoUrls = photoUrls,
        createdAtMillis = createdAtMillis
    )

    companion object {
        fun fromDomain(place: Place): PlaceDto = PlaceDto(
            id = place.id,
            ownerUserId = place.ownerUserId,
            name = place.name,
            description = place.description,
            category = place.category.name,
            latitude = place.latitude,
            longitude = place.longitude,
            address = place.address,
            averageRating = place.averageRating,
            reviewsCount = place.reviewsCount,
            amenities = place.amenities.map { it.name },
            photoUrls = place.photoUrls,
            createdAtMillis = place.createdAtMillis
        )
    }
}
