package com.playground.domain.repository

import com.playground.domain.model.Place
import com.playground.domain.model.PlaceCategory
import com.playground.utils.OpResult
import kotlinx.coroutines.flow.Flow

/**
 * Operacje na miejscach: pobieranie listy, pojedynczego miejsca,
 * dodawanie/edytowanie/usuwanie oraz wyszukiwanie po lokalizacji / kategorii.
 *
 * Edytowanie i usuwanie powinno być w UI dostępne tylko dla właściciela
 * (`Place.ownerUserId == currentUserId`); reguły bezpieczeństwa po stronie
 * Firestore powinny tę regułę dodatkowo egzekwować.
 */
interface PlaceRepository {

    /** Wszystkie miejsca, opcjonalnie filtrowane po kategorii. */
    fun observePlaces(category: PlaceCategory? = null): Flow<List<Place>>

    suspend fun getPlace(placeId: String): OpResult<Place>

    /**
     * Miejsca w okolicy zadanej lokalizacji.
     *
     * @param radiusKm promień w kilometrach
     */
    suspend fun getPlacesNear(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): OpResult<List<Place>>

    /** Top miejsc wg [Place.averageRating]. */
    suspend fun getTopPlaces(limit: Int = 10): OpResult<List<Place>>

    suspend fun addPlace(place: Place): OpResult<Place>

    /**
     * Aktualizuje istniejące miejsce. Powinno być wywołane TYLKO wtedy gdy
     * zalogowany użytkownik jest właścicielem (`Place.ownerUserId`).
     * Zwraca uaktualnioną encję na sukcesie.
     */
    suspend fun updatePlace(place: Place): OpResult<Place>

    /** Usuwa miejsce. Patrz uwagi przy [updatePlace]. */
    suspend fun deletePlace(placeId: String): OpResult<Unit>
}
