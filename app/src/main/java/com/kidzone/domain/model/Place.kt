package com.kidzone.domain.model

/**
 * Miejsce przyjazne dzieciom (plac zabaw, restauracja, park itp.).
 *
 * Odpowiada tabeli PLACES z dokumentu projektu.
 */
data class Place(
    val id: String,
    val ownerUserId: String,
    val name: String,
    val description: String,
    val category: PlaceCategory,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val averageRating: Double = 0.0,
    val reviewsCount: Int = 0,
    val amenities: Set<Amenity> = emptySet(),
    val photoUrls: List<String> = emptyList(),
    /** Mapowanie URL zdjęcia → userId autora. Do blokowania zgłoszenia własnych zdjęć. */
    val photoUploadedBy: Map<String, String> = emptyMap(),
    /** MD5 hashe skompresowanych zdjęć — do wykrywania duplikatów bez downloadu. */
    val photoHashes: List<String> = emptyList(),
    val createdAtMillis: Long = 0L
)
