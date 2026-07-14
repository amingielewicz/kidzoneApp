package com.kidzone.sync

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.model.Review

object OfflinePayload {

    private val gson = Gson()

    data class PlacePayload(
        val id: String,
        val ownerUserId: String,
        val name: String,
        val description: String,
        val category: String,
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val averageRating: Double,
        val reviewsCount: Int,
        val amenities: List<String>,
        val photoUrls: List<String>,
        val photoUploadedBy: Map<String, String>,
        val photoHashes: Any?,
        val createdAtMillis: Long,
        val updatedAtMillis: Long
    )

    fun serializePlace(place: Place): String {
        val payload = PlacePayload(
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
            updatedAtMillis = place.updatedAtMillis
        )
        return gson.toJson(payload)
    }

    fun deserializePlace(json: String): Place {
        val payload = gson.fromJson(json, PlacePayload::class.java)
        return Place(
            id = payload.id,
            ownerUserId = payload.ownerUserId,
            name = payload.name,
            description = payload.description,
            category = PlaceCategory.valueOf(payload.category),
            latitude = payload.latitude,
            longitude = payload.longitude,
            address = payload.address,
            averageRating = payload.averageRating,
            reviewsCount = payload.reviewsCount,
            amenities = payload.amenities.map { Amenity.valueOf(it) }.toSet(),
            photoUrls = payload.photoUrls,
            photoUploadedBy = payload.photoUploadedBy,
            photoHashes = payload.photoHashes.toPhotoHashMap(),
            createdAtMillis = payload.createdAtMillis,
            updatedAtMillis = payload.updatedAtMillis
        )
    }

    data class ReviewPayload(
        val id: String,
        val placeId: String,
        val userId: String,
        val authorName: String,
        val rating: Int,
        val comment: String,
        val photoUrls: List<String>,
        val createdAtMillis: Long
    )

    fun serializeReview(review: Review): String {
        val payload = ReviewPayload(
            id = review.id,
            placeId = review.placeId,
            userId = review.userId,
            authorName = review.authorName,
            rating = review.rating,
            comment = review.comment,
            photoUrls = review.photoUrls,
            createdAtMillis = review.createdAtMillis
        )
        return gson.toJson(payload)
    }

    fun deserializeReview(json: String): Review {
        val payload = gson.fromJson(json, ReviewPayload::class.java)
        return Review(
            id = payload.id,
            placeId = payload.placeId,
            userId = payload.userId,
            authorName = payload.authorName,
            rating = payload.rating,
            comment = payload.comment,
            photoUrls = payload.photoUrls,
            createdAtMillis = payload.createdAtMillis
        )
    }

    fun serializeId(id: String): String = gson.toJson(mapOf("id" to id))

    fun deserializeId(json: String): String {
        val map: Map<String, String> = gson.fromJson(
            json,
            object : TypeToken<Map<String, String>>() {}.type
        )
        return map["id"].orEmpty()
    }

    private fun Any?.toPhotoHashMap(): Map<String, String> =
        (this as? Map<*, *>)
            ?.mapNotNull { (url, hash) ->
                if (url is String && hash is String) url to hash else null
            }
            ?.toMap()
            .orEmpty()
}
