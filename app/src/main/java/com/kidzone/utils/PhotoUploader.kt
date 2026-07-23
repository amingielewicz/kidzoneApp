package com.kidzone.utils

import com.google.firebase.storage.FirebaseStorage
import com.kidzone.analytics.PerformanceTraces
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🎯 Odpowiedzialności:
 * - Wysyłanie (upload) skompresowanych bajtów zdjęć do Firebase Storage.
 * - Zarządzanie hierarchią folderów w Storage (places, reviews, avatars).
 * - Monitorowanie wydajności uploadu poprzez [PerformanceTraces].
 *
 * 🛡️ Bezpieczeństwo i Prywatność:
 * - Nie loguje treści zdjęć.
 * - Ścieżki zapisu są izolowane per użytkownik (ownerUserId).
 *
 * ⚡ Wydajność i Zasoby:
 * - Zwraca download URL natychmiast po udanym zapisie.
 * - Wykorzystuje asynchroniczne wywołania Task API Firebase.
 *
 * ✅ Gwarancje:
 * - Unikalność nazw plików dzięki zastosowaniu UUID.
 * - Operacje usuwania działają w trybie "best-effort" (nie blokują UI przy błędach).
 */
@Singleton
class PhotoUploader @Inject constructor(
    private val storage: FirebaseStorage,
    private val performanceTraces: PerformanceTraces
) {
    /**
     * Uploaduje zdjęcie miejsca.
     *
     * @param ownerUserId ID użytkownika, który dodaje zdjęcie
     * @param placeId ID miejsca
     * @param imageBytes skompresowany WebP
     * @return download URL
     */
    suspend fun uploadPlacePhoto(ownerUserId: String, placeId: String, imageBytes: ByteArray): String {
        val trace = performanceTraces.startTrace(PerformanceTraces.PHOTO_UPLOAD)
        trace.putAttribute("type", "place")
        trace.putMetric("size_bytes", imageBytes.size.toLong())
        return try {
            require(ownerUserId.isNotBlank()) { "ownerUserId must not be blank" }
            require(placeId.isNotBlank()) { "placeId must not be blank" }

            val fileName = "${UUID.randomUUID()}.webp"
            val ref = storage.reference.child("places/$ownerUserId/$placeId/photos/$fileName")
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
    suspend fun uploadReviewPhoto(ownerUserId: String, reviewId: String, imageBytes: ByteArray): String {
        val trace = performanceTraces.startTrace(PerformanceTraces.PHOTO_UPLOAD)
        trace.putAttribute("type", "review")
        trace.putMetric("size_bytes", imageBytes.size.toLong())
        return try {
            require(ownerUserId.isNotBlank()) { "ownerUserId must not be blank" }
            require(reviewId.isNotBlank()) { "reviewId must not be blank" }

            val fileName = "${UUID.randomUUID()}.webp"
            val ref = storage.reference.child("reviews/$ownerUserId/$reviewId/photos/$fileName")
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
