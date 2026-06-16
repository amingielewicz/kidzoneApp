package com.kidzone.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    @ApplicationScope private val applicationScope: CoroutineScope
) : PlaceRepository {

    companion object {
        private const val GEO_QUERY_LIMIT = 1500
        private const val MAP_GEOHASH_PREFIX_LIMIT = 25 // 5x5 siatka dla stabilności
        private const val PER_PREFIX_FETCH_LIMIT = 150 // Top 150 z każdego sektora
        private const val KM_PER_DEGREE = 111.0
        private const val MIN_LONGITUDE_COSINE = 0.1
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
        placeDao.observeByOwner(ownerUserId, GEO_QUERY_LIMIT).map { list -> list.map { it.toDomain() } }

    override suspend fun getPlace(placeId: String): OpResult<Place> = try {
        val doc = firestore.collection(FirestoreCollections.PLACES).document(placeId).get().await()
        val place = doc.toObject(PlaceDto::class.java)?.toDomain()
        if (place != null) OpResult.success(place) else OpResult.failure(Exception("Not found"))
    } catch (e: Exception) { OpResult.failure(e) }

    override suspend fun getPlacesNear(latitude: Double, longitude: Double, radiusKm: Double): OpResult<List<Place>> = try {
        val precision = GeoHash.prefixLengthForRadius(radiusKm)
        val latStep = radiusKm / KM_PER_DEGREE
        val lngStep = radiusKm / (KM_PER_DEGREE * cos(Math.toRadians(latitude)).coerceAtLeast(MIN_LONGITUDE_COSINE))
        
        val latitudes = listOf(latitude - latStep, latitude, latitude + latStep)
        val longitudes = listOf(longitude - lngStep, longitude, longitude + lngStep)
        
        val prefixes = latitudes.flatMap { lat ->
            longitudes.map { lng -> GeoHash.encode(lat, lng, precision) }
        }.distinct()
        
        val places = coroutineScope {
            prefixes.map { prefix ->
                async { fetchPlacesForPrefix(prefix, PER_PREFIX_FETCH_LIMIT) }
            }.awaitAll()
        }.flatten().distinctBy { it.id }

        OpResult.success(places)
    } catch (e: Exception) { OpResult.failure(e) }

    override suspend fun getPlacesInBounds(bounds: GeoBounds, category: PlaceCategory?, limit: Int): OpResult<List<Place>> {
        return try {
            val places = fetchRemotePlacesInBounds(bounds, category, limit)
            if (places.isNotEmpty()) placeDao.upsertAll(places.map(PlaceEntity::fromDomain))
            OpResult.success(places)
        } catch (e: Exception) { OpResult.failure(e) }
    }

    override suspend fun getTopPlaces(limit: Int): OpResult<List<Place>> = try {
        val snapshot = firestore.collection(FirestoreCollections.PLACES)
            .orderBy("averageRating", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get().await()
        OpResult.success(snapshot.documents.mapNotNull { it.toObject(PlaceDto::class.java)?.toDomain() })
    } catch (e: Exception) { OpResult.failure(e) }

    private suspend fun fetchRemotePlacesInBounds(bounds: GeoBounds, category: PlaceCategory?, limit: Int): List<Place> {
        val prefixes = viewportPrefixes(bounds)
        
        val allPlaces = coroutineScope {
            prefixes.map { prefix ->
                async { fetchPlacesForPrefix(prefix, PER_PREFIX_FETCH_LIMIT) }
            }.awaitAll()
        }.flatten()
            .distinctBy { it.id }
            .filter { bounds.contains(it.latitude, it.longitude) }
            .filter { category == null || it.category == category }
        
        return allPlaces
            .sortedByDescending { it.averageRating }
            .take(limit)
    }

    private suspend fun fetchPlacesForPrefix(prefix: String, limit: Int): List<Place> = try {
        firestore.collection(FirestoreCollections.PLACES)
            .whereGreaterThanOrEqualTo("geohash", prefix)
            .whereLessThanOrEqualTo("geohash", prefix + "\uf8ff")
            .limit(limit.toLong())
            .get().await().documents.mapNotNull { it.toObject(PlaceDto::class.java)?.toDomain() }
    } catch (_: Exception) { emptyList() }

    private fun viewportPrefixes(bounds: GeoBounds): List<String> {
        val latSpan = bounds.north - bounds.south
        val lngSpan = if (bounds.west <= bounds.east) bounds.east - bounds.west else (360.0 - bounds.west + bounds.east)
        val radiusKm = max(latSpan, lngSpan) * KM_PER_DEGREE / 2.0
        
        // Stabilizacja precyzji: min 3 dla kraju, min 4 dla miasta. 
        // Zapobiega "pustym zoomom" przez zbyt wielkie buckety.
        val precision = when {
            radiusKm < 10 -> 5
            radiusKm < 50 -> 4
            else -> 3
        }
        
        val steps = 5
        val lats = (0 until steps).map { bounds.south + latSpan * it / (steps - 1) }
        val lngs = (0 until steps).map { 
            var l = bounds.west + lngSpan * it / (steps - 1)
            if (l > 180.0) l -= 360.0
            if (l < -180.0) l += 360.0
            l
        }
        return lats.flatMap { la -> lngs.map { lo -> GeoHash.encode(la, lo, precision) } }.distinct().take(MAP_GEOHASH_PREFIX_LIMIT)
    }

    override suspend fun getPlacesPage(pageSize: Int, cursor: String?, category: PlaceCategory?, query: String?): OpResult<PagedResult<Place>> = OpResult.failure(Exception("Not implemented"))
    override suspend fun addPlace(place: Place): OpResult<Place> = OpResult.failure(Exception("Not implemented"))
    override suspend fun updatePlace(place: Place): OpResult<Place> = OpResult.failure(Exception("Not implemented"))
    override suspend fun deletePlace(placeId: String): OpResult<Unit> = OpResult.failure(Exception("Not implemented"))
    override suspend fun reportPlace(p: String, r: String, re: String, c: String): OpResult<Unit> = OpResult.failure(Exception("Not implemented"))
    override suspend fun submitChangeRequest(p: String, r: String, ch: Map<String, Any>, t: String): OpResult<Unit> = OpResult.failure(Exception("Not implemented"))
    override suspend fun reportPhoto(p: String, r: String, re: String, c: String): OpResult<Unit> = OpResult.failure(Exception("Not implemented"))
    override suspend fun addPhotoUrl(p: String, ph: String, u: String): OpResult<Unit> = OpResult.failure(Exception("Not implemented"))
    override suspend fun removePhotoUrl(p: String, ph: String): OpResult<Unit> = OpResult.failure(Exception("Not implemented"))
    override suspend fun hasUserReportedPlace(p: String, u: String): Boolean = false
    override suspend fun getReportedPhotos(u: String): Set<String> = emptySet()
}
