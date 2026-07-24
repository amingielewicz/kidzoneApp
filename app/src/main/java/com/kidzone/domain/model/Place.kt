package com.kidzone.domain.model

/**
 * 📌 Przeznaczenie:
 * Domenowy model miejsca przyjaznego dzieciom (plac zabaw, restauracja, park itp.).
 * Reprezentuje publiczne dane używane przez listę, mapę, ranking i cache.
 *
 * @property id identyfikator dokumentu miejsca.
 * @property ownerUserId identyfikator użytkownika, który dodał miejsce.
 * @property name publiczna nazwa miejsca (znormalizowana).
 * @property description opis miejsca.
 * @property category kategoria używana do filtrowania i prezentacji.
 * @property latitude szerokość geograficzna miejsca.
 * @property longitude długość geograficzna miejsca.
 * @property address adres prezentowany użytkownikowi.
 * @property averageRating zmaterializowana średnia ocen (liczona po stronie backendu).
 * @property reviewsCount zmaterializowana liczba opinii.
 * @property amenities udogodnienia zadeklarowane dla miejsca.
 * @property photoUrls URL-e zdjęć miejsca przechowywane w Firebase Storage.
 * @property photoUploadedBy mapa URL zdjęcia do identyfikatora autora uploadu.
 * @property photoHashes mapa URL zdjęcia do jego hashu MD5 (używana do wykrywania duplikatów).
 * @property createdAtMillis czas utworzenia rekordu.
 * @property updatedAtMillis czas ostatniej zmiany (używany do rozwiązywania konfliktów przy synchronizacji).
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
    val photoUploadedBy: Map<String, String> = emptyMap(),
    val photoHashes: Map<String, String> = emptyMap(),
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)
