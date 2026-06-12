package com.kidzone.utils

import com.google.firebase.storage.FirebaseStorage
import com.kidzone.analytics.PerformanceTraces
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
 * Each upload is wrapped with a Firebase Performance trace.
 */
@Singleton
class PhotoUploader @Inject constructor(
    private val storage: FirebaseStorage,
    private val performanceTraces: PerformanceTraces
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
        val trace = performanceTraces.startTrace(PerformanceTraces.PHOTO_UPLOAD)
        trace.putAttribute("type", "place")
        trace.putMetric("size_bytes", imageBytes.size.toLong())
        return try {
            val fileName = "${UUID.randomUUID()}.webp"
            val ref = storage.reference.child("places/$placeId/photos/$fileName")
            ref.putBytes(imageBytes).await()
            val url = ref.downloadUrl.await().toString()
            trace.putAttribute("status", "success")
            url
        } catch (e: Exception) {
            trace.putAttribute("status", "error")
            throw e
        } finally {
            performanceTraces.stopTrace(trace)
        }
    }

    /**
     * Uploaduje zdjęcie opinii.
     */
    suspend fun uploadReviewPhoto(reviewId: String, imageBytes: ByteArray): String {
        val trace = performanceTraces.startTrace(PerformanceTraces.PHOTO_UPLOAD)
        trace.putAttribute("type", "review")
        trace.putMetric("size_bytes", imageBytes.size.toLong())
        return try {
            val fileName = "${UUID.randomUUID()}.webp"
            val ref = storage.reference.child("reviews/$reviewId/photos/$fileName")
            ref.putBytes(imageBytes).await()
            val url = ref.downloadUrl.await().toString()
            trace.putAttribute("status", "success")
            url
        } catch (e: Exception) {
            trace.putAttribute("status", "error")
            throw e
        } finally {
            performanceTraces.stopTrace(trace)
        }
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
