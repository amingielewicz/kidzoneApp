package com.kidzone.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kidzone.analytics.PerformanceTraces
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
import com.kidzone.sync.SyncManager
import com.kidzone.utils.AppConfig
import com.kidzone.utils.GeoHash
import com.kidzone.utils.OpResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class FirestorePlaceRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val placeDao: PlaceDao,
    private val syncManager: SyncManager,
    private val performanceTraces: PerformanceTraces
) : PlaceRepository {

    companion object {
        private const val PAGE_SIZE_SNAPSHOT = 20
        private const val OWNER_PLACES_LIMIT = 100
        private const val GEO_QUERY_LIMIT = 1500
        private const val MAP_GEOHASH_PREFIX_LIMIT = 9 // 3x3 grid (much faster loading)
        private const val PER_PREFIX_FETCH_LIMIT = 200
        private const val KM_PER_DEGREE = 111.0
        private const val MIN_LONGITUDE_COSINE = 0.1
        private const val ROOM_CURSOR_PREFIX = "room:"
    }

    private data class PlacesQueryKey(val category: PlaceCategory?, val query: String?)
    private val sharedPlaceFlows = ConcurrentHashMap<PlacesQueryKey, Flow<List<Place>>>()

    override fun observePlaces(category: PlaceCategory?, query: String?): Flow<List<Place>> {
        val normalizedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        val key = PlacesQueryKey(category, normalizedQuery)
        return sharedPlaceFlows.getOrPut(key) {
            placeDao.observeAll(GEO_QUERY_LIMIT)
                .map { list -> 
                    list.map { it.toDomain() }
                        .filter { category == null || it.category == category }
                }
        }
    }

    override fun observePlacesByOwner(ownerUserId: String): Flow<List<Place>> = 
        placeDao.observeByOwner(ownerUserId, OWNER_PLACES_LIMIT).map { list -> list.map { it.toDomain() } }

    override suspend fun getPlace(placeId: String): OpResult<Place> =
        performanceTraces.measureResult(PerformanceTraces.PLACE_LOAD) {
            try {
                val doc = firestore.collection(FirestoreCollections.PLACES).document(placeId).get().await()
                val place = doc.toObject(PlaceDto::class.java)?.toDomain()
                if (place != null) OpResult.success(place) else OpResult.failure(Exception("Not found"))
            } catch (e: Exception) {
                val cached = placeDao.getById(placeId)
                if (cached != null) OpResult.success(cached.toDomain()) else OpResult.failure(e)
            }
    }

    override suspend fun getPlacesNear(latitude: Double, longitude: Double, radiusKm: Double): OpResult<List<Place>> =
        performanceTraces.measureResult(PerformanceTraces.NEARBY_PLACES_LOAD) {
            try {
                val precision = GeoHash.prefixLengthForRadius(radiusKm)
                val hash = GeoHash.encode(latitude, longitude, precision)
                val hashEnd = hash.substring(0, hash.length - 1) + (hash.last() + 1)
                val snapshot = firestore.collection(FirestoreCollections.PLACES)
                    .whereGreaterThanOrEqualTo("geohash", hash)
                    .whereLessThan("geohash", hashEnd)
                    .limit(GEO_QUERY_LIMIT.toLong()).get().await()
                val places = snapshot.documents.mapNotNull { it.toObject(PlaceDto::class.java)?.toDomain() }
                if (places.isNotEmpty()) placeDao.upsertAll(places.map(PlaceEntity::fromDomain))
                OpResult.success(places)
            } catch (e: Exception) {
                val cached = placeDao.getRecentPlaces(GEO_QUERY_LIMIT)
                if (cached.isNotEmpty()) OpResult.success(cached.map { it.toDomain() }) else OpResult.failure(e)
            }
        }

    override suspend fun getTopPlaces(limit: Int): OpResult<List<Place>> =
        performanceTraces.measureResult(PerformanceTraces.TOP_PLACES_LOAD) {
            try {
                val snapshot = firestore.collection(FirestoreCollections.PLACES)
                    .orderBy("averageRating", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(limit.toLong()).get().await()
                val places = snapshot.documents.mapNotNull { it.toObject(PlaceDto::class.java)?.toDomain() }
                placeDao.upsertAll(places.map(PlaceEntity::fromDomain))
                OpResult.success(places)
            } catch (e: Exception) {
                val cached = placeDao.getTopPlaces(limit)
                if (cached.isNotEmpty()) OpResult.success(cached.map { it.toDomain() }) else OpResult.failure(e)
            }
        }

    override suspend fun getPlacesInBounds(bounds: GeoBounds, category: PlaceCategory?, limit: Int): OpResult<List<Place>> {
        return performanceTraces.measureResult(PerformanceTraces.MAP_PLACES_LOAD) {
            val cached = getCachedPlacesInBounds(bounds, category, limit)
            if (cached.isNotEmpty()) {
                // Rezygnujemy z agresywnego odświeżania w tle przy każdym ruchu,
                // żeby nie dławić łącza. Dane z cache są wystarczające dla płynności.
                OpResult.success(cached)
            } else {
                try {
                    val remote = fetchRemotePlacesInBounds(bounds, category, limit)
                    if (remote.isNotEmpty()) placeDao.upsertAll(remote.map(PlaceEntity::fromDomain))
                    OpResult.success(remote)
                } catch (e: Exception) { OpResult.failure(e) }
            }
        }
    }

    private suspend fun fetchRemotePlacesInBounds(bounds: GeoBounds, category: PlaceCategory?, limit: Int): List<Place> {
        val prefixes = viewportPrefixes(bounds)
        return coroutineScope {
            prefixes.map { prefix ->
                async { fetchPlacesForPrefix(prefix, PER_PREFIX_FETCH_LIMIT) }
            }.awaitAll()
        }.flatten().distinctBy { it.id }
            .filter { bounds.contains(it.latitude, it.longitude) }
            .filter { category == null || it.category == category }
            .sortedByDescending { it.averageRating }
            .take(limit)
    }

    private suspend fun fetchPlacesForPrefix(prefix: String, limit: Int): List<Place> = try {
        firestore.collection(FirestoreCollections.PLACES)
            .whereGreaterThanOrEqualTo("geohash", prefix)
            .whereLessThanOrEqualTo("geohash", prefix + "\uf8ff")
            .orderBy("geohash")
            .orderBy("averageRating", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong()).get().await().documents.mapNotNull { it.toObject(PlaceDto::class.java)?.toDomain() }
    } catch (e: Exception) {
        firestore.collection(FirestoreCollections.PLACES)
            .whereGreaterThanOrEqualTo("geohash", prefix)
            .whereLessThanOrEqualTo("geohash", prefix + "\uf8ff")
            .limit(limit.toLong()).get().await().documents.mapNotNull { it.toObject(PlaceDto::class.java)?.toDomain() }
    }

    private suspend fun getCachedPlacesInBounds(bounds: GeoBounds, category: PlaceCategory?, limit: Int): List<Place> {
        val cached = if (bounds.west <= bounds.east) {
            placeDao.getPlacesInBounds(bounds.north, bounds.east, bounds.south, bounds.west, limit)
        } else {
            placeDao.getPlacesInWrappedBounds(bounds.north, bounds.east, bounds.south, bounds.west, limit)
        }
        return cached.map { it.toDomain() }.filter { category == null || it.category == category }.take(limit)
    }

    private fun viewportPrefixes(bounds: GeoBounds): List<String> {
        val latSpan = bounds.north - bounds.south
        val lngSpan = if (bounds.west <= bounds.east) bounds.east - bounds.west else 360.0 - bounds.west + bounds.east
        val radiusKm = max(latSpan * KM_PER_DEGREE, lngSpan * KM_PER_DEGREE * cos(Math.toRadians(bounds.centerLatitude)).coerceAtLeast(MIN_LONGITUDE_COSINE)) / 2.0
        val precision = when {
            radiusKm < 10 -> 5
            radiusKm < 50 -> 4
            else -> 3
        }
        val steps = 3 // 3x3 grid (9 points) covers the screen effectively and is much faster
        val lats = (0 until steps).map { bounds.south + latSpan * it / (steps - 1) }
        val lngs = (0 until steps).map { 
            var lng = bounds.west + lngSpan * it / (steps - 1)
            if (lng > 180.0) lng -= 360.0
            if (lng < -180.0) lng += 360.0
            lng
        }
        return lats.flatMap { la -> lngs.map { lo -> GeoHash.encode(la, lo, precision) } }.distinct().take(MAP_GEOHASH_PREFIX_LIMIT)
    }

    override suspend fun getPlacesPage(pageSize: Int, cursor: String?, category: PlaceCategory?, query: String?): OpResult<PagedResult<Place>> {
        val traceName = if (query.isNullOrBlank()) {
            PerformanceTraces.PLACES_PAGE_LOAD
        } else {
            PerformanceTraces.PLACE_SEARCH_LOAD
        }
        return performanceTraces.measureResult(traceName) {
            try {
                var q = placesCollection() as com.google.firebase.firestore.Query
                if (category != null) q = q.whereEqualTo("category", category.name)
                q = if (!query.isNullOrBlank()) q.orderBy("name") else q.orderBy("createdAtMillis", com.google.firebase.firestore.Query.Direction.DESCENDING)
                if (!cursor.isNullOrBlank() && !cursor.startsWith(ROOM_CURSOR_PREFIX)) {
                    val cs = placesCollection().document(cursor).get().await()
                    if (cs.exists()) q = q.startAfter(cs)
                } else if (!query.isNullOrBlank()) q = q.startAt(query)
                if (!query.isNullOrBlank()) q = q.endAt(query + "\uf8ff")
                val snap = q.limit((pageSize + 1).toLong()).get().await()
                val docs = snap.documents
                val hasMore = docs.size > pageSize
                val page = if (hasMore) docs.take(pageSize) else docs
                val places = page.mapNotNull { it.toObject(PlaceDto::class.java)?.toDomain() }
                if (places.isNotEmpty()) placeDao.upsertAll(places.map(PlaceEntity::fromDomain))
                OpResult.success(PagedResult(places, if (hasMore) page.lastOrNull()?.id else null))
            } catch (e: Exception) { OpResult.failure(e) }
        }
    }

    override suspend fun addPlace(place: Place): OpResult<Place> =
        performanceTraces.measureResult(PerformanceTraces.ADD_PLACE) {
            try {
                val doc = placesCollection().document()
                val p = place.copy(id = doc.id)
                placesCollection().document(p.id).set(PlaceDto.fromDomain(p)).await()
                placeDao.upsert(PlaceEntity.fromDomain(p))
                OpResult.success(p)
            } catch (e: Exception) { OpResult.failure(e) }
        }

    override suspend fun updatePlace(place: Place): OpResult<Place> = try {
        placesCollection().document(place.id).set(PlaceDto.fromDomain(place)).await()
        placeDao.upsert(PlaceEntity.fromDomain(place))
        OpResult.success(place)
    } catch (e: Exception) { OpResult.failure(e) }

    override suspend fun deletePlace(id: String): OpResult<Unit> = try {
        placesCollection().document(id).delete().await()
        placeDao.deleteById(id)
        OpResult.success(Unit)
    } catch (e: Exception) { OpResult.failure(e) }

    override suspend fun reportPlace(p: String, r: String, re: String, c: String): OpResult<Unit> = OpResult.success(Unit)
    override suspend fun submitChangeRequest(p: String, r: String, ch: Map<String, Any>, t: String): OpResult<Unit> = OpResult.success(Unit)
    override suspend fun reportPhoto(p: String, r: String, re: String, c: String): OpResult<Unit> = OpResult.success(Unit)
    override suspend fun addPhotoUrl(p: String, ph: String, u: String): OpResult<Unit> = OpResult.success(Unit)
    override suspend fun removePhotoUrl(p: String, ph: String): OpResult<Unit> = OpResult.success(Unit)
    override suspend fun hasUserReportedPlace(p: String, u: String): Boolean = false
    override suspend fun getReportedPhotos(u: String): Set<String> = emptySet()
    private fun placesCollection() = firestore.collection(FirestoreCollections.PLACES)
}
