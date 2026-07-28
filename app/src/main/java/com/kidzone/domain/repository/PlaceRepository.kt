package com.kidzone.domain.repository

import com.kidzone.domain.model.PagedResult
import com.kidzone.domain.model.GeoBounds
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.utils.OpResult
import kotlinx.coroutines.flow.Flow

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie cyklem życia i widocznością miejsc przyjaznych dzieciom.
 * - Koordynacja synchronizacji między Firestore a lokalnym cache Room.
 * - Obsługa geo-zapytań (bliskość, viewport mapy).
 *
 * 🔌 Strategia Cache:
 * - Single Source of Truth: Wszystkie obserwowane dane (Flow) pochodzą z Room.
 * - Room jest aktualizowany reaktywnie przez listenery Firestore w tle.
 *
 * 🛡️ Autoryzacja i Bezpieczeństwo:
 * - Odczyt publiczny dostępny dla wszystkich.
 * - Akcje modyfikujące (add/update/delete) wymagają autoryzacji [AuthRepository].
 *
 * ✅ Gwarancje spójności:
 * - Zmiany liczników ocen wykonywane atomowo na backendzie.
 * - Synchronizacja zdjęć (URL + Autor + Hash) jako jedna operacja.
 *
 * 📤 Mapowanie błędów:
 * - Techniczne błędy Firestore mapowane na [OpResult] z czytelnym kontekstem.
 *
 * 🧵 Threading:
 * - Wszystkie implementacje muszą być bezpieczne do wywołania z dowolnego wątku (Dispatcher.IO).
 */
interface PlaceRepository {

    /**
     * Obserwuje miejsca, opcjonalnie filtrowane po kategorii i nazwie.
     *
     * @param category opcjonalna kategoria miejsca.
     * @param query opcjonalna fraza wyszukiwania.
     * @return strumień aktualnej listy miejsc, który może być zasilany z cache i backendu.
     */
    fun observePlaces(category: PlaceCategory? = null, query: String? = null): Flow<List<Place>>

    /**
     * Obserwuje miejsca dodane przez wskazanego użytkownika.
     *
     * Emituje pustą listę, gdy [ownerUserId] jest pusty.
     *
     * @param ownerUserId identyfikator właściciela miejsc.
     * @return strumień miejsc użytkownika posortowanych przez implementację lub warstwę wyższą.
     */
    fun observePlacesByOwner(ownerUserId: String): Flow<List<Place>>

    /**
     * Wykonuje pełną synchronizację miejsc przypisanych do użytkownika.
     * Pobiera dane z serwera i aktualizuje lokalny cache.
     *
     * @param ownerUserId identyfikator właściciela miejsc.
     * @return lista zsynchronizowanych miejsc.
     */
    suspend fun syncPlacesByOwner(ownerUserId: String): OpResult<List<Place>>

    /**
     * Pobiera pojedyncze miejsce.
     *
     * Implementacja może użyć cache jako fallbacku, ale nie powinna zwracać danych prywatnych ani
     * ukrytych przez moderację.
     *
     * @param placeId identyfikator miejsca.
     * @return miejsce albo zmapowany błąd, np. brak zasobu lub brak uprawnień.
     */
    suspend fun getPlace(placeId: String): OpResult<Place>

    /**
     * Pobiera miejsca w promieniu od zadanej lokalizacji.
     *
     * @param latitude szerokość geograficzna środka wyszukiwania.
     * @param longitude długość geograficzna środka wyszukiwania.
     * @param radiusKm promień w kilometrach.
     * @return ograniczona lista miejsc albo zmapowany błąd.
     */
    suspend fun getPlacesNear(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): OpResult<List<Place>>

    /**
     * Pobiera miejsca z lokalnego cache'u w zadanym promieniu.
     * Metoda nie wykonuje zapytań sieciowych.
     */
    suspend fun getCachedPlacesNear(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<Place>

    /**
     * Pobiera ograniczoną listę miejsc widocznych w aktualnym viewportcie mapy.
     *
     * @param bounds granice geograficzne viewportu.
     * @param category opcjonalna kategoria.
     * @param limit maksymalna liczba zwracanych miejsc.
     */
    suspend fun getPlacesInBounds(
        bounds: GeoBounds,
        category: PlaceCategory? = null,
        limit: Int = 200
    ): OpResult<List<Place>>

    /**
     * Pobiera najlepiej oceniane miejsca.
     *
     * @param limit maksymalna liczba wyników.
     * @return lista miejsc uporządkowana według aktualnej reguły rankingowej.
     */
    suspend fun getTopPlaces(limit: Int = 10): OpResult<List<Place>>

    /**
     * Pobiera stronę miejsc z kursorem serwerowym.
     *
     * @param pageSize liczba elementów na stronie.
     * @param cursor nieprzezroczysty kursor z poprzedniego [PagedResult.nextCursor].
     * @param category opcjonalna kategoria.
     * @param query opcjonalny prefiks nazwy.
     * @return strona danych i kursor następnej strony albo zmapowany błąd.
     */
    suspend fun getPlacesPage(
        pageSize: Int = 20,
        cursor: String? = null,
        category: PlaceCategory? = null,
        query: String? = null
    ): OpResult<PagedResult<Place>>

    /**
     * Dodaje nowe miejsce.
     *
     * Sukces oznacza potwierdzony zapis po stronie backendu. Operacja powinna być odporna na
     * wielokrotne wywołanie i nie tworzyć duplikatu po timeoutcie.
     *
     * @param place miejsce do zapisania.
     * @return zapisane miejsce z identyfikatorem i polami serwerowymi albo błąd.
     */
    suspend fun addPlace(place: Place): OpResult<Place>

    /**
     * Aktualizuje istniejące miejsce.
     *
     * Wywołujący powinien posiadać uprawnienie właściciela, a backend musi je ponownie zweryfikować.
     *
     * @param place miejsce zawierające zaktualizowane dane.
     * @return zaktualizowane miejsce albo błąd autoryzacji, walidacji lub sieci.
     */
    suspend fun updatePlace(place: Place): OpResult<Place>

    /**
     * Usuwa miejsce należące do aktualnego użytkownika.
     *
     * @param placeId identyfikator miejsca.
     * @return sukces po zakończeniu usuwania albo zmapowany błąd.
     */
    suspend fun deletePlace(placeId: String): OpResult<Unit>

    /**
     * Zgłasza miejsce jako spam lub naruszenie.
     *
     * @param placeId identyfikator zgłaszanego miejsca.
     * @param reporterId identyfikator zgłaszającego.
     * @param reason powód zgłoszenia.
     * @param comment opcjonalny komentarz.
     */
    suspend fun reportPlace(
        placeId: String,
        reporterId: String,
        reason: String,
        comment: String = ""
    ): OpResult<Unit>

    /**
     * Wysyła propozycję zmiany danych miejsca przez osobę niebędącą właścicielem.
     *
     * @param placeId identyfikator miejsca.
     * @param requesterId identyfikator autora propozycji.
     * @param changes mapa dozwolonych pól i nowych wartości.
     * @param type typ propozycji.
     * @param comment opcjonalne uzasadnienie.
     */
    suspend fun submitChangeRequest(
        placeId: String,
        requesterId: String,
        changes: Map<String, Any>,
        type: String = "EDIT",
        comment: String = ""
    ): OpResult<Unit>

    /**
     * Zgłasza zdjęcie jako nieodpowiednie.
     *
     * @param photoUrl adres zgłaszanego zdjęcia.
     * @param reporterId identyfikator zgłaszającego.
     * @param reason powód zgłoszenia.
     * @param comment opcjonalny komentarz.
     */
    suspend fun reportPhoto(
        photoUrl: String,
        reporterId: String,
        reason: String,
        comment: String = ""
    ): OpResult<Unit>

    /**
     * Dodaje URL zdjęcia do miejsca wraz z hashem MD5 dla deduplikacji.
     *
     * @param placeId identyfikator miejsca.
     * @param photoUrl URL pliku po udanym uploadzie.
     * @param uploadedByUserId identyfikator autora zdjęcia.
     * @param photoHash hash MD5 zawartości zdjęcia.
     */
    suspend fun addPhotoUrl(
        placeId: String,
        photoUrl: String,
        uploadedByUserId: String,
        photoHash: String
    ): OpResult<Unit>

    /**
     * Usuwa URL zdjęcia z miejsca.
     *
     * Uprawnienie autora lub moderatora musi zostać zweryfikowane również po stronie backendu.
     *
     * @param placeId identyfikator miejsca.
     * @param photoUrl URL usuwanego zdjęcia.
     */
    suspend fun removePhotoUrl(placeId: String, photoUrl: String): OpResult<Unit>

    /**
     * Sprawdza, czy użytkownik zgłosił już dane miejsce.
     *
     * @param placeId identyfikator miejsca.
     * @param userId identyfikator zgłaszającego.
     */
    suspend fun hasUserReportedPlace(placeId: String, userId: String): Boolean

    /**
     * Pobiera URL-e zdjęć zgłoszonych przez użytkownika.
     *
     * @param userId identyfikator zgłaszającego.
     * @return zbiór URL-i; pusty zbiór, gdy brak zgłoszeń.
     */
    suspend fun getReportedPhotos(userId: String): Set<String>
}
