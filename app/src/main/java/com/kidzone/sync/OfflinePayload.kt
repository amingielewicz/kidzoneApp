package com.kidzone.sync

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.model.Review

/**
 * Serializuje dane operacji oczekujących do formatu przechowywanego w kolejce offline.
 *
 * Payload jest kontraktem pomiędzy [SyncManager], bazą Room i [SyncWorker]. Zmiana pól wymaga
 * zachowania kompatybilności z operacjami zapisanymi przez starsze wersje aplikacji albo jawnej
 * migracji kolejki.
 *
 * Surowy JSON może zawierać treści użytkownika i nie powinien być zapisywany w logach ani
 * telemetryce.
 */
object OfflinePayload {

    private val gson = Gson()

    /**
     * Stabilny model danych miejsca zapisywany w kolejce offline.
     *
     * @property id identyfikator dokumentu miejsca.
     * @property ownerUserId identyfikator właściciela używany do kontroli ownership.
     * @property name nazwa miejsca.
     * @property description opis miejsca.
     * @property category nazwa wartości [PlaceCategory].
     * @property latitude szerokość geograficzna miejsca.
     * @property longitude długość geograficzna miejsca.
     * @property address adres prezentowany użytkownikowi.
     * @property averageRating zmaterializowana średnia ocen.
     * @property reviewsCount liczba opinii używana przez ranking.
     * @property amenities nazwy wartości [Amenity].
     * @property photoUrls URL-e zdjęć miejsca.
     * @property photoUploadedBy mapa URL do identyfikatora autora uploadu.
     * @property photoHashes hashe używane do wykrywania duplikatów.
     * @property createdAtMillis czas utworzenia rekordu.
     * @property updatedAtMillis czas ostatniej zmiany używany przy rozwiązywaniu konfliktów.
     */
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
        val photoHashes: List<String>,
        val createdAtMillis: Long,
        val updatedAtMillis: Long
    )

    /**
     * Serializuje miejsce do wersjonowanego kontraktu kolejki.
     *
     * @param place model domenowy do zapisania.
     * @return JSON przeznaczony wyłącznie do lokalnej kolejki operacji.
     */
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

    /**
     * Odtwarza model miejsca z payloadu kolejki.
     *
     * @param json JSON utworzony przez [serializePlace].
     * @return odtworzony model domenowy.
     * @throws IllegalArgumentException gdy kategoria lub udogodnienie nie jest rozpoznawane.
     */
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
            photoHashes = payload.photoHashes,
            createdAtMillis = payload.createdAtMillis,
            updatedAtMillis = payload.updatedAtMillis
        )
    }

    /**
     * Stabilny model opinii zapisywany w kolejce offline.
     *
     * @property id identyfikator opinii.
     * @property placeId identyfikator ocenianego miejsca.
     * @property userId identyfikator autora.
     * @property authorName publiczna nazwa autora utrwalona przy zapisie.
     * @property rating ocena liczbowa.
     * @property comment treść opinii.
     * @property photoUrls zdjęcia dołączone do opinii.
     * @property createdAtMillis czas utworzenia.
     */
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

    /**
     * Serializuje opinię do lokalnej kolejki.
     *
     * @param review model opinii.
     * @return JSON payloadu kolejki.
     */
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

    /**
     * Odtwarza opinię z payloadu kolejki.
     *
     * @param json JSON utworzony przez [serializeReview].
     * @return odtworzony model domenowy.
     */
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

    /**
     * Serializuje identyfikator operacji usuwania.
     *
     * @param id identyfikator dokumentu.
     */
    fun serializeId(id: String): String = gson.toJson(mapOf("id" to id))

    /**
     * Odczytuje identyfikator z payloadu operacji usuwania.
     *
     * @param json JSON utworzony przez [serializeId].
     * @return identyfikator albo pusty tekst, gdy pole nie istnieje.
     */
    fun deserializeId(json: String): String {
        val map: Map<String, String> = gson.fromJson(
            json,
            object : TypeToken<Map<String, String>>() {}.type
        )
        return map["id"].orEmpty()
    }
}
