package com.kidzone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kidzone.data.local.LocalMapperUtils.fromPipeSeparated
import com.kidzone.data.local.LocalMapperUtils.toPhotoHashes
import com.kidzone.data.local.LocalMapperUtils.toPipeSeparated
import com.kidzone.data.local.LocalMapperUtils.toJson
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory

/**
 * ⚙️ Techniczne:
 * Reprezentacja miejsca w lokalnej bazie danych Room.
 *
 * @property amenities Nazwy udogodnień połączone znakiem pipe (|).
 * @property photoUrls Adresy URL zdjęć połączone znakiem pipe (|).
 * @property photoHashes Mapa (JSON) URL -> Hash MD5 dla spójnej deduplikacji offline.
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
    val photoHashes: String = "{}", // JSON map: photo URL -> MD5 hash
    val createdAtMillis: Long,
    val updatedAtMillis: Long = 0L,
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
            .fromPipeSeparated()
            .mapNotNull(Amenity.Companion::fromKey)
            .toSet(),
        photoUrls = photoUrls.fromPipeSeparated(),
        photoHashes = photoHashes.toPhotoHashes(),
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
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
            amenities = place.amenities.map { it.name }.toPipeSeparated(),
            photoUrls = place.photoUrls.toPipeSeparated(),
            photoHashes = place.photoHashes.toJson(),
            createdAtMillis = place.createdAtMillis,
            updatedAtMillis = place.updatedAtMillis
        )
    }
}
