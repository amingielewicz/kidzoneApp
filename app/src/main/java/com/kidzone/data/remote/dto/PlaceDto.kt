package com.kidzone.data.remote.dto

import com.kidzone.data.remote.dto.DtoMapperUtils.toPhotoHashMap
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.utils.GeoHash

/**
 * ⚙️ Techniczne:
 * Reprezentacja [Place] w kolekcji `places` w Firestore.
 *
 * @property geohash Obliczane z (lat, lng) przy zapisie. Precision 7 ≈ 150m. Używane do geo-zapytań.
 * @property photoHashes `Any?` celowo: stare dokumenty zawierają listę, nowe mapę URL -> hash.
 * [toDomain] akceptuje wyłącznie nowy, jednoznaczny format.
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
    val photoUploadedBy: Map<String, String> = emptyMap(),
    /**
     * `Any?` celowo: stare dokumenty zawierają listę, nowe mapę URL -> hash.
     * [toDomain] akceptuje wyłącznie nowy, jednoznaczny format.
     */
    val photoHashes: Any? = emptyMap<String, String>(),
    val createdAtMillis: Long = 0L,
    val geohash: String = ""
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
        photoUploadedBy = photoUploadedBy,
        photoHashes = photoHashes.toPhotoHashMap(),
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
            photoUploadedBy = place.photoUploadedBy,
            photoHashes = place.photoHashes,
            createdAtMillis = place.createdAtMillis,
            geohash = GeoHash.encode(place.latitude, place.longitude)
        )
    }
}
