package com.playground.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.playground.data.remote.FirestoreCollections
import com.playground.data.remote.dto.PlaceDto
import com.playground.domain.model.Place
import com.playground.domain.model.PlaceCategory
import com.playground.domain.repository.PlaceRepository
import com.playground.utils.OpResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacja [PlaceRepository] oparta o Firestore.
 *
 * Implementacje metod są tu placeholderami – do uzupełnienia gdy podłączymy
 * prawdziwy projekt Firebase i zdefiniujemy reguły dostępu.
 */
@Singleton
class FirestorePlaceRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : PlaceRepository {

    override fun observePlaces(category: PlaceCategory?): Flow<List<Place>> {
        // TODO: podpiąć snapshotListener pod kolekcję places, opcjonalnie z whereEqualTo("category", ...)
        return flowOf(emptyList())
    }

    override suspend fun getPlace(placeId: String): OpResult<Place> {
        // TODO: placesCollection().document(placeId).get().await().toObject(PlaceDto::class.java)?.toDomain()
        return OpResult.failure(NotImplementedError("getPlace – do uzupełnienia"))
    }

    override suspend fun getPlacesNear(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): OpResult<List<Place>> {
        // TODO: filtrowanie po geohashu (np. biblioteka GeoFirestore) lub bounding box
        return OpResult.success(emptyList())
    }

    override suspend fun getTopPlaces(limit: Int): OpResult<List<Place>> {
        // TODO: orderBy("averageRating", DESCENDING).limit(limit)
        return OpResult.success(emptyList())
    }

    override suspend fun addPlace(place: Place): OpResult<Place> {
        // TODO: placesCollection().add(PlaceDto.fromDomain(place))
        return OpResult.failure(NotImplementedError("addPlace – do uzupełnienia"))
    }

    @Suppress("unused")
    private fun placesCollection() = firestore.collection(FirestoreCollections.PLACES)

    @Suppress("unused")
    private fun PlaceDto.dummyReference(): PlaceDto = this // marker, by import nie został wycięty
}
