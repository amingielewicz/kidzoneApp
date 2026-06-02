package com.kidzone.domain.repository

import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.utils.OpResult
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

    /**
     * Strumień miejsc dodanych przez konkretnego usera (snapshot listener).
     *
     * Używane przez ekran "Moje miejsca" w profilu. Sortowanie po stronie
     * klienta (po `createdAtMillis` malejąco) – Firestore wymagałby wtedy
     * composite indexu (ownerUserId + createdAtMillis), do uniknięcia.
     *
     * Emituje pustą listę gdy [ownerUserId] jest pusty.
     */
    fun observePlacesByOwner(ownerUserId: String): Flow<List<Place>>

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

    /**
     * Zgłasza miejsce jako spam/naruszenie.
     */
    suspend fun reportPlace(
        placeId: String,
        reporterId: String,
        reason: String,
        comment: String = ""
    ): OpResult<Unit>

    /**
     * Wysyła propozycję zmiany danych miejsca (przez nie-właściciela).
     */
    suspend fun submitChangeRequest(
        placeId: String,
        requesterId: String,
        changes: Map<String, Any>,
        type: String = "EDIT"
    ): OpResult<Unit>
}
