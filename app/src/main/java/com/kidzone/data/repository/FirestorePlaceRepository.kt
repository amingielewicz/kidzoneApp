package com.kidzone.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.kidzone.data.remote.FirestoreCollections
import com.kidzone.data.remote.dto.PlaceDto
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.utils.OpResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maks. czas na zapis do Firestore (w ms).
 *
 * Bez timeoutu Firebase retryuje w nieskończoność, gdy emulator ma popsute
 * Google Play Services (znany glitch z `SecurityException: Unknown calling
 * package name 'com.google.android.gms'`). Po tym czasie zwracamy
 * [java.util.concurrent.TimeoutException], żeby UI mogło pokazać użytkownikowi
 * sensowny komunikat zamiast wieczystego spinnera.
 */
private const val WRITE_TIMEOUT_MS = 30_000L

/**
 * Implementacja [PlaceRepository] oparta o Firestore.
 */
@Singleton
class FirestorePlaceRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : PlaceRepository {

    override fun observePlaces(category: PlaceCategory?): Flow<List<Place>> = callbackFlow {
        // Bazowe zapytanie - opcjonalnie filtrowane po kategorii.
        val query = if (category != null) {
            placesCollection().whereEqualTo("category", category.name)
        } else {
            placesCollection()
        }

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val places = snapshot?.documents
                ?.mapNotNull { it.toObject<PlaceDto>()?.toDomain() }
                .orEmpty()
            trySend(places)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getPlace(placeId: String): OpResult<Place> = try {
        val snapshot = placesCollection().document(placeId).get().await()
        val dto = snapshot.toObject<PlaceDto>()
        if (dto != null) {
            OpResult.success(dto.toDomain())
        } else {
            OpResult.failure(NoSuchElementException("Brak miejsca o id=$placeId"))
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    override suspend fun getPlacesNear(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): OpResult<List<Place>> {
        // TODO: filtrowanie po geohashu (np. biblioteka GeoFirestore) lub bounding box.
        // Na razie zwracamy wszystkie - klient wyfiltruje, MVP bez geo-zapytania.
        return try {
            val snapshot = placesCollection().get().await()
            val places = snapshot.documents.mapNotNull { it.toObject<PlaceDto>()?.toDomain() }
            OpResult.success(places)
        } catch (e: Exception) {
            OpResult.failure(e)
        }
    }

    override suspend fun getTopPlaces(limit: Int): OpResult<List<Place>> = try {
        val snapshot = placesCollection()
            .orderBy("averageRating", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        val places = snapshot.documents.mapNotNull { it.toObject<PlaceDto>()?.toDomain() }
        OpResult.success(places)
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    override suspend fun addPlace(place: Place): OpResult<Place> = try {
        // Tworzymy referencje (auto-generowany id), id wkladamy do dokumentu zeby
        // pozniej moc czytac id z samego DTO bez polegania na nazwie dokumentu.
        val docRef = placesCollection().document()
        val placeWithId = place.copy(id = docRef.id)
        val ownerUserId = placeWithId.ownerUserId

        // Transakcja: zapis miejsca + atomowa inkrementacja licznika
        // `placesAddedCount` na dokumencie autora. Dzieki transakcji albo oba
        // zapisy sie powioda, albo zaden - nie zostawimy "osierodonego" miejsca
        // bez zliczenia w rankingu, ani odwrotnie.
        //
        // Jezeli `ownerUserId` jest puste (nie powinno sie zdarzyc, ale lepiej
        // sie zabezpieczyc), pomijamy update usera - zapis miejsca i tak
        // sie odbedzie.
        val completed = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            firestore.runTransaction<Unit> { tx ->
                tx.set(docRef, PlaceDto.fromDomain(placeWithId))
                if (ownerUserId.isNotBlank()) {
                    val userRef = firestore
                        .collection(FirestoreCollections.USERS)
                        .document(ownerUserId)
                    tx.update(userRef, "placesAddedCount", FieldValue.increment(1))
                }
            }.await()
            true
        }
        if (completed == null) {
            OpResult.failure(
                java.util.concurrent.TimeoutException(
                    "Zapis trwa zbyt długo. Sprawdź połączenie z Internetem, " +
                        "a jeśli używasz emulatora – wykonaj Cold Boot."
                )
            )
        } else {
            OpResult.success(placeWithId)
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    override suspend fun updatePlace(place: Place): OpResult<Place> = try {
        require(place.id.isNotBlank()) { "Place.id musi być ustawione przy edycji" }

        // .set() bez merge nadpisuje cały dokument - jest to świadome:
        // przy edycji UI zawsze wysyła kompletny obiekt (z zachowanymi
        // ownerUserId, createdAtMillis, averageRating, reviewsCount itd.),
        // a nadpisanie zapewnia że Firestore nie zostawi nieużywanych pól
        // gdyby user np. usunął wszystkie udogodnienia.
        val completed = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            placesCollection().document(place.id)
                .set(PlaceDto.fromDomain(place))
                .await()
            true
        }
        if (completed == null) {
            OpResult.failure(
                java.util.concurrent.TimeoutException(
                    "Zapis trwa zbyt długo. Sprawdź połączenie z Internetem."
                )
            )
        } else {
            OpResult.success(place)
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    override suspend fun deletePlace(placeId: String): OpResult<Unit> = try {
        require(placeId.isNotBlank()) { "placeId nie może być puste" }
        val completed = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            placesCollection().document(placeId).delete().await()
            true
        }
        if (completed == null) {
            OpResult.failure(
                java.util.concurrent.TimeoutException(
                    "Usunięcie trwa zbyt długo. Sprawdź połączenie z Internetem."
                )
            )
        } else {
            OpResult.success(Unit)
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    private fun placesCollection() = firestore.collection(FirestoreCollections.PLACES)
}
