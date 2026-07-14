package com.kidzone.domain.model

/**
 * Domenowy model miejsca przyjaznego dzieciom.
 *
 * Model reprezentuje publiczne dane miejsca używane przez listę, mapę, ranking, szczegóły i cache.
 * Pola agregowane, takie jak [averageRating] i [reviewsCount], nie powinny być modyfikowane przez
 * niezaufanego klienta bez kontroli backendu.
 *
 * @property id identyfikator dokumentu miejsca.
 * @property ownerUserId identyfikator użytkownika, który dodał miejsce.
 * @property name publiczna nazwa miejsca.
 * @property description opis miejsca.
 * @property category kategoria używana do filtrowania i prezentacji.
 * @property latitude szerokość geograficzna miejsca.
 * @property longitude długość geograficzna miejsca.
 * @property address adres prezentowany użytkownikowi.
 * @property averageRating zmaterializowana średnia ocen.
 * @property reviewsCount zmaterializowana liczba opinii.
 * @property amenities udogodnienia zadeklarowane dla miejsca.
 * @property photoUrls URL-e zdjęć miejsca.
 * @property photoUploadedBy mapa URL zdjęcia do identyfikatora autora uploadu.
 * @property photoHashes hashe skompresowanych zdjęć używane do wykrywania duplikatów.
 * @property createdAtMillis czas utworzenia rekordu.
 * @property updatedAtMillis czas ostatniej zmiany używany między innymi przez rozwiązywanie konfliktów.
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
    val photoHashes: List<String> = emptyList(),
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)
