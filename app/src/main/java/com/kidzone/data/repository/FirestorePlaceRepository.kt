package com.kidzone.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kidzone.data.local.PlaceDao
import com.kidzone.data.local.PlaceEntity
import com.kidzone.data.remote.FirestoreCollections
import com.kidzone.data.remote.dto.PlaceDto
import com.kidzone.di.ApplicationScope
import com.kidzone.domain.model.PagedResult
import com.kidzone.domain.model.GeoBounds
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.data.local.sync.OperationType
import com.kidzone.sync.NetworkUtils
import com.kidzone.sync.OfflinePayload
import com.kidzone.sync.SyncManager
import com.kidzone.utils.AppConfig
import com.kidzone.utils.GeoHash
import com.kidzone.utils.OpResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacja [PlaceRepository] oparta o Firestore.
 */
@Singleton
class FirestorePlaceRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val placeDao: PlaceDao,
    private val syncManager: SyncManager,
    @ApplicationScope private val applicationScope: CoroutineScope
) : PlaceRepository {

    companion object {
        /**
         * Number of places fetched via snapshot listener (initial/real-time page).
         * Additional pages are fetched on demand via [getPlacesPage] cursor pagination.
         */
        private const val PAGE_SIZE_SNAPSHOT = 20
        private const val OWNER_PLACES_LIMIT = 100
        private const val GEO_QUERY_LIMIT = 200
        private const val MAP_GEOHASH_PREFIX_LIMIT = 9
        private const val KM_PER_DEGREE = 111.0
        private const val MIN_LONGITUDE_COSINE = 0.1
        private const val ROOM_CURSOR_PREFIX = "room:"
    }

    private data class PlacesQueryKey(
        val category: PlaceCategory?,
        val query: String?
    )

    private val sharedPlaceFlows = ConcurrentHashMap<PlacesQueryKey, Flow<List<Place>>>()

    override fun observePlaces(category: PlaceCategory?, query: String?): Flow<List<Place>> {
        val normalizedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        if (normalizedQuery != null) {
            // Frazy są krótkotrwałe i praktycznie nie współdzielą się między
            // ekranami. Nie trzymamy ich w mapie przez cały proces aplikacji.
            return createPlacesFlow(category, normalizedQuery)
        }
        val key = PlacesQueryKey(category, normalizedQuery)
        return sharedPlaceFlows.computeIfAbsent(key) {
            createPlacesFlow(category, normalizedQuery)
                .shareIn(
                    scope = applicationScope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                    replay = 1
                )
        }
    }

    private fun createPlacesFlow(
        category: PlaceCategory?,
        query: String?
    ): Flow<List<Place>> = channelFlow {
        // 1. Room jako local source – emitujemy z niego do kanału.
        // Jeśli jest query, Room filtruje po 'contains', Firestore po prefixie.
        val localFlow = when {
            category != null && !query.isNullOrBlank() ->
                placeDao.observeByCategoryAndName(category.name, query, PAGE_SIZE_SNAPSHOT)
            category != null -> placeDao.observeByCategory(category.name, PAGE_SIZE_SNAPSHOT)
            !query.isNullOrBlank() -> placeDao.observeByName(query, PAGE_SIZE_SNAPSHOT)
            else -> placeDao.observeAll(PAGE_SIZE_SNAPSHOT)
        }

        // 2. Firestore snapshot listener – aktualizuje Room w tle.
        launch {
            val firestoreFlow = callbackFlow {
                var firestoreQuery = placesCollection().limit(PAGE_SIZE_SNAPSHOT.toLong()) // First page via snapshot; rest via cursor pagination

                if (category != null) {
                    firestoreQuery = firestoreQuery.whereEqualTo("category", category.name)
                }

                if (!query.isNullOrBlank()) {
                    // Uwaga: Firestore prefix search wymaga orderBy("name").
                    // Jeśli mamy też whereEqualTo("category"), Firestore może wymagać
                    // indeksu złożonego (category ASC, name ASC).
                    firestoreQuery = firestoreQuery.orderBy("name")
                        .startAt(query)
                        .endAt(query + "\uf8ff")
                }

                val registration = firestoreQuery.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Jeśli brakuje indeksu, Firestore rzuci błędem z linkiem do konsoli.
                        close(error)
                        return@addSnapshotListener
                    }
                    val places = snapshot?.documents
                        ?.mapNotNull { it.toObject(PlaceDto::class.java)?.toDomain() }
                        .orEmpty()
                    trySend(places)
                }
                awaitClose { registration.remove() }
            }
            firestoreFlow
                .catch {
                    Timber.w(
                        it,
                        "observePlaces sync failed (category=$category, query=$query)"
                    )
                }
                .collect { places ->
                    placeDao.upsertAll(places.map(PlaceEntity::fromDomain))
                }
        }

        // 3. Emituj dane z Room (re-emituje automatycznie po upsert z synca).
        localFlow.collectLatest { entities ->
            trySend(entities.map { it.toDomain() })
        }
    }

    override fun observePlacesByOwner(ownerUserId: String): Flow<List<Place>> = channelFlow {
        if (ownerUserId.isBlank()) {
            trySend(emptyList())
            return@channelFlow
        }

        val localFlow = placeDao.observeByOwner(ownerUserId, OWNER_PLACES_LIMIT)

        val syncJob = launch {
            val firestoreFlow = callbackFlow {
                val registration = placesCollection()
                    .whereEqualTo("ownerUserId", ownerUserId)
                    .limit(OWNER_PLACES_LIMIT.toLong())
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val places = snapshot?.documents
                            ?.mapNotNull { it.toObject(PlaceDto::class.java)?.toDomain() }
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
    }

    override suspend fun getPlace(placeId: String): OpResult<Place> = try {
        val snapshot = placesCollection().document(placeId).get().await()
        val dto = snapshot.toObject(PlaceDto::class.java)
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
                .limit(GEO_QUERY_LIMIT.toLong())
                .get()
                .await()
            val places = snapshot.documents.mapNotNull { it.toObject(PlaceDto::class.java)?.toDomain() }

            // Persystuj do cache (nawet jeśli 0 wyników — to ważna informacja).
            if (places.isNotEmpty()) {
                placeDao.upsertAll(places.map(PlaceEntity::fromDomain))
            }
            OpResult.success(places)
        } catch (e: Exception) {
            // Offline fallback: zwróć z Room cache, klient filtruje haversinem.
            val cached = placeDao.getRecentPlaces(GEO_QUERY_LIMIT)
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
        val places = snapshot.documents.mapNotNull { it.toObject(PlaceDto::class.java)?.toDomain() }
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

    override suspend fun getPlacesInBounds(
        bounds: GeoBounds,
        category: PlaceCategory?,
        limit: Int
    ): OpResult<List<Place>> {
        require(limit > 0) { "limit musi być dodatni" }

        return try {
            val places = fetchRemotePlacesInBounds(bounds, category, limit)

            if (places.isNotEmpty()) {
                placeDao.upsertAll(places.map(PlaceEntity::fromDomain))
            }
            OpResult.success(places)
        } catch (e: Exception) {
            val cachedPlaces = getCachedPlacesInBounds(bounds, category, limit)
            if (cachedPlaces.isNotEmpty()) {
                OpResult.success(cachedPlaces)
            } else {
                OpResult.failure(e)
            }
        }
    }

    private suspend fun fetchRemotePlacesInBounds(
        bounds: GeoBounds,
        category: PlaceCategory?,
        limit: Int
    ): List<Place> {
        val prefixes = viewportPrefixes(bounds)
        val perPrefixLimit = max(1, ceil(limit.toDouble() / prefixes.size).toInt())

        return coroutineScope {
            prefixes.map { prefix ->
                async { fetchPlacesForPrefix(prefix, category, perPrefixLimit) }
            }.awaitAll()
        }
            .flatten()
            .distinctBy { it.id }
            .filter { bounds.contains(it.latitude, it.longitude) }
            .take(limit)
    }

    private suspend fun fetchPlacesForPrefix(
        prefix: String,
        category: PlaceCategory?,
        limit: Int
    ): List<Place> {
        var query: com.google.firebase.firestore.Query = placesCollection()
        if (category != null) {
            query = query.whereEqualTo("category", category.name)
        }
        return query
            .whereGreaterThanOrEqualTo("geohash", prefix)
            .whereLessThanOrEqualTo("geohash", prefix + "\uf8ff")
            .limit(limit.toLong())
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(PlaceDto::class.java)?.toDomain() }
    }

    private suspend fun getCachedPlacesInBounds(
        bounds: GeoBounds,
        category: PlaceCategory?,
        limit: Int
    ): List<Place> {
        val cached = if (bounds.west <= bounds.east) {
            placeDao.getPlacesInBounds(
                north = bounds.north,
                east = bounds.east,
                south = bounds.south,
                west = bounds.west,
                limit = limit
            )
        } else {
            placeDao.getPlacesInWrappedBounds(
                north = bounds.north,
                east = bounds.east,
                south = bounds.south,
                west = bounds.west,
                limit = limit
            )
        }
        return cached
            .map { it.toDomain() }
            .filter { category == null || it.category == category }
            .take(limit)
    }

    private fun viewportPrefixes(bounds: GeoBounds): List<String> {
        val latitudeSpanKm = (bounds.north - bounds.south) * KM_PER_DEGREE
        val longitudeSpan = if (bounds.west <= bounds.east) {
            bounds.east - bounds.west
        } else {
            (180.0 - bounds.west) + (bounds.east + 180.0)
        }
        val longitudeSpanKm = longitudeSpan * KM_PER_DEGREE *
            cos(Math.toRadians(bounds.centerLatitude)).coerceAtLeast(MIN_LONGITUDE_COSINE)
        val radiusKm = max(latitudeSpanKm, longitudeSpanKm) / 2.0
        val precision = GeoHash.prefixLengthForRadius(radiusKm)

        val latitudes = listOf(bounds.south, bounds.centerLatitude, bounds.north)
        val longitudes = if (bounds.west <= bounds.east) {
            listOf(bounds.west, bounds.centerLongitude, bounds.east)
        } else {
            listOf(bounds.west, bounds.centerLongitude, bounds.east)
        }

        return latitudes
            .flatMap { latitude ->
                longitudes.map { longitude ->
                    GeoHash.encode(latitude, longitude, precision)
                }
            }
            .distinct()
            .take(MAP_GEOHASH_PREFIX_LIMIT)
    }

    override suspend fun getPlacesPage(
        pageSize: Int,
        cursor: String?,
        category: PlaceCategory?,
        query: String?
    ): OpResult<PagedResult<Place>> = try {
        var firestoreQuery: com.google.firebase.firestore.Query = placesCollection()
        if (category != null) {
            firestoreQuery = firestoreQuery.whereEqualTo("category", category.name)
        }

        firestoreQuery = if (!query.isNullOrBlank()) {
            firestoreQuery.orderBy("name")
        } else {
            firestoreQuery.orderBy(
                "createdAtMillis",
                com.google.firebase.firestore.Query.Direction.DESCENDING
            )
        }

        // Apply cursor: fetch the document snapshot for startAfter.
        if (!cursor.isNullOrBlank() && !cursor.startsWith(ROOM_CURSOR_PREFIX)) {
            val cursorSnapshot = placesCollection().document(cursor).get().await()
            if (cursorSnapshot.exists()) {
                firestoreQuery = firestoreQuery.startAfter(cursorSnapshot)
            }
        } else if (!query.isNullOrBlank()) {
            firestoreQuery = firestoreQuery.startAt(query)
        }

        if (!query.isNullOrBlank()) {
            firestoreQuery = firestoreQuery.endAt(query + "\uf8ff")
        }

        // Fetch pageSize + 1 to know if there's a next page.
        val fetchLimit = pageSize + 1
        val snapshot = firestoreQuery.limit(fetchLimit.toLong()).get().await()

        val allDocs = snapshot.documents
        val hasMore = allDocs.size > pageSize
        val pageDocs = if (hasMore) allDocs.take(pageSize) else allDocs

        val places = pageDocs.mapNotNull { it.toObject(PlaceDto::class.java)?.toDomain() }

        // Persist to Room cache.
        if (places.isNotEmpty()) {
            placeDao.upsertAll(places.map(PlaceEntity::fromDomain))
        }

        // Next cursor = document ID of the last item on this page.
        val nextCursor = if (hasMore) pageDocs.lastOrNull()?.id else null

        OpResult.success(PagedResult(items = places, nextCursor = nextCursor))
    } catch (e: Exception) {
        // Offline fallback: paginate from Room cache.
        val offset = cursor
            ?.removePrefix(ROOM_CURSOR_PREFIX)
            ?.toIntOrNull()
            ?: 0
        val cached = placeDao.getPlacesPage(
            category = category?.name,
            query = query,
            limit = pageSize + 1,
            offset = offset
        )
        if (cached.isNotEmpty()) {
            val hasMore = cached.size > pageSize
            val page = if (hasMore) cached.take(pageSize) else cached
            OpResult.success(
                PagedResult(
                    items = page.map { it.toDomain() },
                    nextCursor = if (hasMore) "$ROOM_CURSOR_PREFIX${offset + pageSize}" else null
                )
            )
        } else {
            OpResult.failure(e)
        }
    }

    override suspend fun addPlace(place: Place): OpResult<Place> = try {
        // Tworzymy referencje (auto-generowany id), id wkladamy do dokumentu zeby
        // pozniej moc czytac id z samego DTO bez polegania na nazwie dokumentu.
        val docRef = placesCollection().document()
        val placeWithId = place.copy(id = docRef.id)

        val completed = withTimeoutOrNull(AppConfig.WRITE_TIMEOUT_MS) {
            // Cloud Function (updateUserStatsOnPlaceCreate) zajmie się licznikiem
            // placesAddedCount na dokumencie autora.
            placesCollection().document(placeWithId.id)
                .set(PlaceDto.fromDomain(placeWithId))
                .await()
            true
        }
        if (completed == null) {
            // Timeout — queue offline for retry
            Timber.w("addPlace: timeout, queuing offline")
            placeDao.upsert(PlaceEntity.fromDomain(placeWithId))
            syncManager.enqueue(OperationType.ADD_PLACE, OfflinePayload.serializePlace(placeWithId))
            OpResult.success(placeWithId) // Optimistic success — UI shows place
        } else {
            // Persystuj do lokalnego cache.
            placeDao.upsert(PlaceEntity.fromDomain(placeWithId))
            OpResult.success(placeWithId)
        }
    } catch (e: Exception) {
        if (NetworkUtils.isNetworkError(e)) {
            // Offline: queue for later sync, persist to cache optimistically
            val docId = placesCollection().document().id
            val placeWithId = place.copy(id = docId)
            placeDao.upsert(PlaceEntity.fromDomain(placeWithId))
            syncManager.enqueue(OperationType.ADD_PLACE, OfflinePayload.serializePlace(placeWithId))
            Timber.d("addPlace: offline, queued for sync (id=$docId)")
            OpResult.success(placeWithId)
        } else {
            OpResult.failure(e)
        }
    }

    override suspend fun updatePlace(place: Place): OpResult<Place> = try {
        require(place.id.isNotBlank()) { "Place.id musi być ustawione przy edycji" }

        val completed = withTimeoutOrNull(AppConfig.WRITE_TIMEOUT_MS) {
            placesCollection().document(place.id)
                .set(PlaceDto.fromDomain(place))
                .await()
            true
        }
        if (completed == null) {
            // Timeout — queue offline
            Timber.w("updatePlace: timeout, queuing offline")
            placeDao.upsert(PlaceEntity.fromDomain(place))
            syncManager.enqueue(OperationType.UPDATE_PLACE, OfflinePayload.serializePlace(place))
            OpResult.success(place)
        } else {
            // Zaktualizuj cache.
            placeDao.upsert(PlaceEntity.fromDomain(place))
            OpResult.success(place)
        }
    } catch (e: Exception) {
        if (NetworkUtils.isNetworkError(e)) {
            placeDao.upsert(PlaceEntity.fromDomain(place))
            syncManager.enqueue(OperationType.UPDATE_PLACE, OfflinePayload.serializePlace(place))
            Timber.d("updatePlace: offline, queued for sync (id=${place.id})")
            OpResult.success(place)
        } else {
            OpResult.failure(e)
        }
    }

    override suspend fun deletePlace(placeId: String): OpResult<Unit> = try {
        require(placeId.isNotBlank()) { "placeId nie moze byc puste" }

        val completed = withTimeoutOrNull(AppConfig.WRITE_TIMEOUT_MS) {
            // Cloud Function (updateUserStatsOnPlaceDelete) zajmie się licznikiem
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
        // Sprawdź czy użytkownik już zgłosił to miejsce (1 zgłoszenie na użytkownika na cel)
        val existing = firestore.collection(FirestoreCollections.PLACE_REPORTS)
            .whereEqualTo("reporterId", reporterId)
            .whereEqualTo("placeId", placeId)
            .get()
            .await()
        if (existing.documents.isNotEmpty()) {
            throw IllegalStateException("Już zgłosiłeś to miejsce")
        }

        val reportData = mapOf(
            "placeId" to placeId,
            "reporterId" to reporterId,
            "reason" to reason,
            "comment" to comment,
            "createdAtMillis" to System.currentTimeMillis(),
            "status" to "pending"
        )
        val completed = withTimeoutOrNull(AppConfig.WRITE_TIMEOUT_MS) {
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
        val completed = withTimeoutOrNull(AppConfig.WRITE_TIMEOUT_MS) {
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

        // Sprawdź czy użytkownik już zgłosił to zdjęcie (1 zgłoszenie na użytkownika na cel)
        val existing = firestore.collection(FirestoreCollections.PHOTO_REPORTS)
            .whereEqualTo("reporterId", reporterId)
            .whereEqualTo("photoUrl", photoUrl)
            .get()
            .await()
        if (existing.documents.isNotEmpty()) {
            throw IllegalStateException("Już zgłosiłeś to zdjęcie")
        }

        val reportData = mapOf(
            "photoUrl" to photoUrl,
            "reporterId" to reporterId,
            "reason" to reason,
            "comment" to comment,
            "createdAtMillis" to System.currentTimeMillis(),
            "status" to "pending"
        )
        val completed = withTimeoutOrNull(AppConfig.WRITE_TIMEOUT_MS) {
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

    override suspend fun addPhotoUrl(placeId: String, photoUrl: String, uploadedByUserId: String): OpResult<Unit> = try {
        require(placeId.isNotBlank()) { "placeId nie może być puste" }
        require(photoUrl.isNotBlank()) { "photoUrl nie może być puste" }

        val completed = withTimeoutOrNull(AppConfig.WRITE_TIMEOUT_MS) {
            // Transakcja eliminuje race condition: dwa równoległe uploady
            // nie nadpiszą sobie nawzajem wpisu w photoUploadedBy.
            val docRef = placesCollection().document(placeId)

            firestore.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                @Suppress("UNCHECKED_CAST")
                val currentMap = (snap.get("photoUploadedBy") as? Map<String, String>).orEmpty()
                val updatedMap = currentMap + (photoUrl to uploadedByUserId)

                transaction.update(docRef, mapOf(
                    "photoUrls" to FieldValue.arrayUnion(photoUrl),
                    "photoUploadedBy" to updatedMap
                ))
            }.await()
            true
        }
        if (completed == null) {
            OpResult.failure(
                java.util.concurrent.TimeoutException(
                    "Dodawanie zdjęcia trwa zbyt długo. Spróbuj ponownie."
                )
            )
        } else {
            OpResult.success(Unit)
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    override suspend fun removePhotoUrl(placeId: String, photoUrl: String): OpResult<Unit> = try {
        require(placeId.isNotBlank()) { "placeId nie może być puste" }
        require(photoUrl.isNotBlank()) { "photoUrl nie może być puste" }

        val completed = withTimeoutOrNull(AppConfig.WRITE_TIMEOUT_MS) {
            val docRef = placesCollection().document(placeId)

            // Pobierz aktualną mapę i usuń wpis
            val snap = docRef.get().await()
            @Suppress("UNCHECKED_CAST")
            val currentMap = (snap.get("photoUploadedBy") as? Map<String, String>).orEmpty()
            val updatedMap = currentMap - photoUrl

            docRef.update(
                mapOf(
                    "photoUrls" to FieldValue.arrayRemove(photoUrl),
                    "photoUploadedBy" to updatedMap
                )
            ).await()
            true
        }
        if (completed == null) {
            OpResult.failure(
                java.util.concurrent.TimeoutException(
                    "Usuwanie zdjęcia trwa zbyt długo. Spróbuj ponownie."
                )
            )
        } else {
            OpResult.success(Unit)
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    override suspend fun hasUserReportedPlace(placeId: String, userId: String): Boolean = try {
        val snapshot = firestore.collection(FirestoreCollections.PLACE_REPORTS)
            .whereEqualTo("reporterId", userId)
            .whereEqualTo("placeId", placeId)
            .limit(1)
            .get()
            .await()
        !snapshot.isEmpty
    } catch (_: Exception) {
        false
    }

    override suspend fun getReportedPhotos(userId: String): Set<String> = try {
        val snapshot = firestore.collection(FirestoreCollections.PHOTO_REPORTS)
            .whereEqualTo("reporterId", userId)
            .get()
            .await()
        snapshot.documents.mapNotNull { it.getString("photoUrl") }.toSet()
    } catch (_: Exception) {
        emptySet()
    }
}
