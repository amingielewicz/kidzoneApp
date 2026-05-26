package com.playword.domain.repository

import com.playword.domain.model.Place
import com.playword.domain.model.PlaceCategory
import com.playword.utils.OpResult
import kotlinx.coroutines.flow.Flow

/**
 * Operacje na miejscach: pobieranie listy, pojedynczego miejsca,
 * dodawanie nowego oraz wyszukiwanie po lokalizacji / kategorii.
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
}
