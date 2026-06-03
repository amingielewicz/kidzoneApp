package com.kidzone.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.kidzone.data.local.PlaceDao
import com.kidzone.data.local.PlaceEntity
import com.kidzone.data.remote.FirestoreCollections
import com.kidzone.data.remote.dto.PlaceDto
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.utils.GeoHash
import com.kidzone.utils.OpResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    private val firestore: FirebaseFirestore,
    private val placeDao: PlaceDao
) : PlaceRepository {

    override fun observePlaces(category: PlaceCategory?): Flow<List<Place>> = channelFlow {
        // 1. Room jako local source – emitujemy z niego do kanału.
        val localFlow = if (category != null) {
            placeDao.observeByCategory(category.name)
        } else {
            placeDao.observeAll()
        }

        // 2. Firestore snapshot listener – aktualizuje Room w tle.
        val syncJob = launch {
            val firestoreFlow = callbackFlow {
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
            firestoreFlow.collect { places ->
                placeDao.upsertAll(places.map(PlaceEntity::fromDomain))
            }
        }

        // 3. Emituj dane z Room (re-emituje automatycznie po upsert z synca).
        localFlow.collectLatest { entities ->
            trySend(entities.map { it.toDomain() })
        }

        syncJob.cancel()
    }

    override fun observePlacesByOwner(ownerUserId: String): Flow<List<Place>> = channelFlow {
        if (ownerUserId.isBlank()) {
            trySend(emptyList())
            return@channelFlow
        }

        val localFlow = placeDao.observeByOwner(ownerUserId)

        val syncJob = launch {
            val firestoreFlow = callbackFlow {
                val registration = placesCollection()
                    .whereEqualTo("ownerUserId", ownerUserId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val places = snapshot?.documents
                            ?.mapNotNull { it.toObject<PlaceDto>()?.toDomain() }
                            ?.sortedByDescending { it.createdAtMillis }
                            .orEmpty()
                        trySend(places)
                    }
                awaitClose { registration.remove() }
            }
            firestoreFlow.collect { places ->
                placeDao.upsertAll(places.map(PlaceEntity::fromDomain))
            }
        }

        localFlow.collectLatest { entities ->
            trySend(entities.map { it.toDomain() })
        }

        syncJob.cancel()
    }

    override suspend fun getPlace(placeId: String): OpResult<Place> = try {
        val snapshot = placesCollection().document(placeId).get().await()
        val dto = snapshot.toObject<PlaceDto>()
        if (dto != null) {
            val place = dto.toDomain()
            // Zaktualizuj cache po udanym pobraniu z sieci.
            placeDao.upsert(PlaceEntity.fromDomain(place))
            OpResult.success(place)
        } else {
            OpResult.failure(NoSuchElementException("Brak miejsca o id=$placeId"))
        }
    } catch (e: Exception) {
        // Fallback do cache offline przy błędzie sieci.
        val cached = placeDao.getById(placeId)
        if (cached != null) {
            OpResult.success(cached.toDomain())
        } else {
            OpResult.failure(e)
        }
    }

    override suspend fun getPlacesNear(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): OpResult<List<Place>> {
        return try {
            val prefixLen = GeoHash.prefixLengthForRadius(radiusKm)
            val centerHash = GeoHash.encode(latitude, longitude, prefixLen)
            // Firestore range query na prefixie geohash: >= prefix, < prefix~
            // (~ = ostatni znak inkrementowany). Zwraca wszystkie miejsca w
            // „buckecie" geohash, klient doprecyzowuje haversinem.
            val hashEnd = centerHash.substring(0, centerHash.length - 1) +
                (centerHash.last() + 1)

            val snapshot = placesCollection()
                .whereGreaterThanOrEqualTo("geohash", centerHash)
                .whereLessThan("geohash", hashEnd)
                .get()
                .await()
            val places = snapshot.documents.mapNotNull { it.toObject<PlaceDto>()?.toDomain() }

            // Fallback: jeśli geohash query zwrócił 0 wyników (np. stare
            // miejsca bez geohash), pobierz wszystko (legacy behavior).
            val result = if (places.isEmpty()) {
                val allSnapshot = placesCollection().get().await()
                allSnapshot.documents.mapNotNull { it.toObject<PlaceDto>()?.toDomain() }
            } else {
                places
            }

            // Persystuj do cache.
            placeDao.upsertAll(result.map(PlaceEntity::fromDomain))
            OpResult.success(result)
        } catch (e: Exception) {
            // Fallback: zwróć wszystko z cache (klient filtruje po odległości).
            val cached = placeDao.getTopPlaces(Int.MAX_VALUE)
            if (cached.isNotEmpty()) {
                OpResult.success(cached.map { it.toDomain() })
            } else {
                OpResult.failure(e)
            }
        }
    }

    override suspend fun getTopPlaces(limit: Int): OpResult<List<Place>> = try {
        val snapshot = placesCollection()
            .orderBy("averageRating", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        val places = snapshot.documents.mapNotNull { it.toObject<PlaceDto>()?.toDomain() }
        // Persystuj do cache.
        placeDao.upsertAll(places.map(PlaceEntity::fromDomain))
        OpResult.success(places)
    } catch (e: Exception) {
        // Fallback: top z cache.
        val cached = placeDao.getTopPlaces(limit)
        if (cached.isNotEmpty()) {
            OpResult.success(cached.map { it.toDomain() })
        } else {
            OpResult.failure(e)
        }
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
                    // set + merge zamiast update: jeśli doc istnieje, tylko
                    // inkrementuje pole `placesAddedCount`; jeśli go brak
                    // (legacy user / nieudana rejestracja), tworzy minimalny
                    // doc z samym licznikiem, żeby user zaczął się pojawiać
                    // w rankingu. Brakujące pola (name, email, avatar) zostaną
                    // dopełnione przy najbliższym logowaniu przez ensureUserDoc().
                    tx.set(
                        userRef,
                        mapOf("placesAddedCount" to FieldValue.increment(1)),
                        SetOptions.merge()
                    )
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
            // Persystuj do lokalnego cache.
            placeDao.upsert(PlaceEntity.fromDomain(placeWithId))
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
            // Zaktualizuj cache.
            placeDao.upsert(PlaceEntity.fromDomain(place))
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
            // Usuń z cache.
            placeDao.deleteById(placeId)
            OpResult.success(Unit)
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    override suspend fun reportPlace(
        placeId: String,
        reporterId: String,
        reason: String,
        comment: String
    ): OpResult<Unit> = try {
        val reportData = mapOf(
            "placeId" to placeId,
            "reporterId" to reporterId,
            "reason" to reason,
            "comment" to comment,
            "createdAtMillis" to System.currentTimeMillis(),
            "status" to "pending"
        )
        val completed = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            firestore.collection(FirestoreCollections.PLACE_REPORTS)
                .add(reportData)
                .await()
            true
        }
        if (completed == null) {
            OpResult.failure(
                java.util.concurrent.TimeoutException(
                    "Wys\u0142anie zg\u0142oszenia trwa zbyt d\u0142ugo. Spr\u00F3buj ponownie."
                )
            )
        } else {
            OpResult.success(Unit)
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    override suspend fun submitChangeRequest(
        placeId: String,
        requesterId: String,
        changes: Map<String, Any>,
        type: String
    ): OpResult<Unit> = try {
        val requestData = mapOf(
            "placeId" to placeId,
            "requesterId" to requesterId,
            "changes" to changes,
            "type" to type,
            "createdAtMillis" to System.currentTimeMillis(),
            "status" to "pending"
        )
        val completed = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            firestore.collection(FirestoreCollections.PLACE_CHANGE_REQUESTS)
                .add(requestData)
                .await()
            true
        }
        if (completed == null) {
            OpResult.failure(
                java.util.concurrent.TimeoutException(
                    "Wys\u0142anie propozycji trwa zbyt d\u0142ugo. Spr\u00F3buj ponownie."
                )
            )
        } else {
            OpResult.success(Unit)
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    private fun placesCollection() = firestore.collection(FirestoreCollections.PLACES)

    override suspend fun reportPhoto(
        photoUrl: String,
        reporterId: String,
        reason: String,
        comment: String
    ): OpResult<Unit> = try {
        require(photoUrl.isNotBlank()) { "photoUrl nie może być puste" }
        require(reporterId.isNotBlank()) { "reporterId nie może być puste" }

        val reportData = mapOf(
            "photoUrl" to photoUrl,
            "reporterId" to reporterId,
            "reason" to reason,
            "comment" to comment,
            "createdAtMillis" to System.currentTimeMillis(),
            "status" to "pending"
        )
        val completed = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            firestore.collection(FirestoreCollections.PHOTO_REPORTS)
                .add(reportData)
                .await()
            true
        }
        if (completed == null) {
            OpResult.failure(
                java.util.concurrent.TimeoutException(
                    "Wysłanie zgłoszenia trwa zbyt długo. Spróbuj ponownie."
                )
            )
        } else {
            OpResult.success(Unit)
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }
}
