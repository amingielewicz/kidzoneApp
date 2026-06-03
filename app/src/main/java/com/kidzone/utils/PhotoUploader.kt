package com.kidzone.utils

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Upload skompresowanych zdjęć (WebP ByteArray) do Firebase Storage.
 *
 * Ścieżki w Storage:
 *  - Miejsca: `places/{placeId}/photos/{uuid}.webp`
 *  - Opinie:  `reviews/{reviewId}/photos/{uuid}.webp`
 *  - Avatary: `avatars/{userId}/avatar.jpg` (istniejące, nie ruszamy)
 *
 * Zwraca download URL po pomyślnym uploadzie.
 */
@Singleton
class PhotoUploader @Inject constructor(
    private val storage: FirebaseStorage
) {
    /**
     * Uploaduje zdjęcie miejsca.
     *
     * @param placeId ID miejsca (może być tymczasowe "" – wtedy caller
     *   musi uploadować PO uzyskaniu ID z Firestore)
     * @param imageBytes skompresowany WebP
     * @return download URL
     */
    suspend fun uploadPlacePhoto(placeId: String, imageBytes: ByteArray): String {
        val fileName = "${UUID.randomUUID()}.webp"
        val ref = storage.reference.child("places/$placeId/photos/$fileName")
        ref.putBytes(imageBytes).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Uploaduje zdjęcie opinii.
     */
    suspend fun uploadReviewPhoto(reviewId: String, imageBytes: ByteArray): String {
        val fileName = "${UUID.randomUUID()}.webp"
        val ref = storage.reference.child("reviews/$reviewId/photos/$fileName")
        ref.putBytes(imageBytes).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Usuwa zdjęcie z podanego URL-a Storage.
     * Best-effort – jeśli się nie uda (np. URL nieprawidłowy), ignorujemy.
     */
    suspend fun deletePhoto(downloadUrl: String) {
        try {
            val ref = storage.getReferenceFromUrl(downloadUrl)
            ref.delete().await()
        } catch (_: Exception) {
            // Best-effort – nie blokujemy operacji nadrzędnej
        }
    }
}
