package com.kidzone.domain.repository

import com.kidzone.domain.model.PagedResult
import com.kidzone.domain.model.GeoBounds
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

    /**
     * Wszystkie miejsca, opcjonalnie filtrowane po kategorii i nazwie (prefix).
     *
     * @param category kategoria miejsca
     * @param query fraza wyszukiwania (prefix po stronie Firestore, contains po stronie Room)
     */
    fun observePlaces(category: PlaceCategory? = null, query: String? = null): Flow<List<Place>>

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

    /**
     * Ograniczona lista miejsc widocznych w aktualnym viewportcie mapy.
     */
    suspend fun getPlacesInBounds(
        bounds: GeoBounds,
        category: PlaceCategory? = null,
        limit: Int = 200
    ): OpResult<List<Place>>

    /** Top miejsc wg [Place.averageRating]. */
    suspend fun getTopPlaces(limit: Int = 10): OpResult<List<Place>>

    /**
     * Paginated place fetch with server-side cursor.
     *
     * Returns [pageSize] places ordered by [createdAtMillis] descending,
     * optionally filtered by [category] and/or name prefix [query].
     *
     * @param pageSize number of items per page (default 20)
     * @param cursor opaque cursor from a previous [PagedResult.nextCursor].
     *   Pass null for the first page.
     * @param category optional category filter
     * @param query optional name prefix filter
     * @return [PagedResult] with items and cursor for next page (null if last)
     */
    suspend fun getPlacesPage(
        pageSize: Int = 20,
        cursor: String? = null,
        category: PlaceCategory? = null,
        query: String? = null
    ): OpResult<PagedResult<Place>>

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

    /**
     * Zgłasza zdjęcie jako nieodpowiednie.
     *
     * Zapis do kolekcji `photo_reports` z danymi zgłaszającego,
     * URL-em zdjęcia, powodem i komentarzem.
     */
    suspend fun reportPhoto(
        photoUrl: String,
        reporterId: String,
        reason: String,
        comment: String = ""
    ): OpResult<Unit>

    /**
     * Dodaje URL zdjęcia do listy `photoUrls` na dokumencie miejsca.
     * Zapisuje też kto dodał zdjęcie w `photoUploadedBy`.
     */
    suspend fun addPhotoUrl(placeId: String, photoUrl: String, uploadedByUserId: String): OpResult<Unit>

    /**
     * Usuwa URL zdjęcia z listy `photoUrls` na dokumencie miejsca.
     * Usuwa też wpis z `photoUploadedBy`.
     *
     * Autoryzacja po stronie klienta: wywołujący powinien upewnić się,
     * że `photoUploadedBy[photoUrl] == currentUserId` przed wywołaniem.
     * Reguły Firestore pozwalają na update `photoUrls` + `photoUploadedBy`
     * przez każdego zalogowanego usera.
     */
    suspend fun removePhotoUrl(placeId: String, photoUrl: String): OpResult<Unit>

    /**
     * Sprawdza czy użytkownik już zgłosił dane miejsce.
     */
    suspend fun hasUserReportedPlace(placeId: String, userId: String): Boolean

    /**
     * Pobiera listę URL-i zdjęć zgłoszonych przez danego użytkownika.
     */
    suspend fun getReportedPhotos(userId: String): Set<String>
}
