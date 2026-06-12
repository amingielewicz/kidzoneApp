package com.kidzone.sync

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.model.Review

/**
 * Serialization helpers for offline queue payloads.
 *
 * Uses Gson (already in project via Retrofit) for JSON serialization.
 * Each operation type has a specific payload structure.
 */
object OfflinePayload {

    private val gson = Gson()

    // ─── Place payloads ──────────────────────────────────────────────────

    data class PlacePayload(
        val id: String,
        val ownerUserId: String,
        val name: String,
        val description: String,
        val category: String,
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val amenities: List<String>,
        val photoUrls: List<String>
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
            amenities = place.amenities.map { it.name },
            photoUrls = place.photoUrls
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
            amenities = payload.amenities.map { Amenity.valueOf(it) }.toSet(),
            photoUrls = payload.photoUrls
        )
    }

    // ─── Review payloads ─────────────────────────────────────────────────

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

    // ─── Simple ID payloads (delete operations) ──────────────────────────

    fun serializeId(id: String): String = gson.toJson(mapOf("id" to id))

    fun deserializeId(json: String): String {
        val map: Map<String, String> = gson.fromJson(
            json,
            object : TypeToken<Map<String, String>>() {}.type
        )
        return map["id"].orEmpty()
    }
}
