package com.kidzone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory

/**
 * Room entity reprezentujący miejsce w lokalnym cache.
 *
 * Mapowanie 1:1 z [Place] z warstwy domain. Kolekcje (amenities, photoUrls)
 * przechowywane jako CSV / pipe-separated stringi (Room nie wspiera list
 * natywnie bez TypeConvertera, ale proste joiny dają zero zależności i są
 * wystarczające dla MVP).
 */
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val name: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val averageRating: Double,
    val reviewsCount: Int,
    val amenities: String, // pipe-separated amenity names
    val photoUrls: String, // pipe-separated URLs
    val photoHashes: String = "", // pipe-separated MD5 hashes
    val createdAtMillis: Long,
    /** Czas ostatniego zapisu do cache – do ewentualnej polityki TTL. */
    val cachedAtMillis: Long = System.currentTimeMillis()
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
        amenities = amenities
            .split("|")
            .filter { it.isNotBlank() }
            .mapNotNull(Amenity.Companion::fromKey)
            .toSet(),
        photoUrls = photoUrls
            .split("|")
            .filter { it.isNotBlank() },
        photoHashes = photoHashes
            .split("|")
            .filter { it.isNotBlank() },
        createdAtMillis = createdAtMillis
    )

    companion object {
        fun fromDomain(place: Place): PlaceEntity = PlaceEntity(
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
            amenities = place.amenities.joinToString("|") { it.name },
            photoUrls = place.photoUrls.joinToString("|"),
            photoHashes = place.photoHashes.joinToString("|"),
            createdAtMillis = place.createdAtMillis
        )
    }
}
