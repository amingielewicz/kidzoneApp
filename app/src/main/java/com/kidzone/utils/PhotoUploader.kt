package com.kidzone.utils

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.kidzone.analytics.PerformanceTraces
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Upload skompresowanych zdjęć WebP do Firebase Storage.
 *
 * Ścieżki:
 * - miejsca: places/{ownerUserId}/{placeId}/photos/{uuid}.webp
 * - opinie: reviews/{ownerUserId}/{reviewId}/photos/{uuid}.webp
 *
 * Każdy upload jest objęty pomiarem Firebase Performance.
 */
@Singleton
class PhotoUploader @Inject constructor(
    private val storage: FirebaseStorage,
    private val performanceTraces: PerformanceTraces
) {

    private val webpMetadata = StorageMetadata.Builder()
        .setContentType("image/webp")
        .build()

    suspend fun uploadPlacePhoto(
        ownerUserId: String,
        placeId: String,
        imageBytes: ByteArray
    ): String {
        require(ownerUserId.isNotBlank()) {
            "ownerUserId must not be blank"
        }
        require(placeId.isNotBlank()) {
            "placeId must not be blank"
        }

        val trace = performanceTraces.startTrace(
            PerformanceTraces.PHOTO_UPLOAD
        )
        trace.putAttribute("type", "place")
        trace.putMetric("size_bytes", imageBytes.size.toLong())

        return try {
            val fileName = "${UUID.randomUUID()}.webp"
            val ref = storage.reference.child(
                "places/$ownerUserId/$placeId/photos/$fileName"
            )

            ref.putBytes(imageBytes, webpMetadata).await()

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

    suspend fun uploadReviewPhoto(
        ownerUserId: String,
        reviewId: String,
        imageBytes: ByteArray
    ): String {
        require(ownerUserId.isNotBlank()) {
            "ownerUserId must not be blank"
        }
        require(reviewId.isNotBlank()) {
            "reviewId must not be blank"
        }

        val trace = performanceTraces.startTrace(
            PerformanceTraces.PHOTO_UPLOAD
        )
        trace.putAttribute("type", "review")
        trace.putMetric("size_bytes", imageBytes.size.toLong())

        return try {
            val fileName = "${UUID.randomUUID()}.webp"
            val ref = storage.reference.child(
                "reviews/$ownerUserId/$reviewId/photos/$fileName"
            )

            ref.putBytes(imageBytes, webpMetadata).await()

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

    suspend fun deletePhoto(downloadUrl: String) {
        val ref = getStorageReference(downloadUrl)
        ref.delete().await()
    }

    private fun getStorageReference(
        downloadUrl: String
    ): StorageReference {
        return runCatching {
            storage.getReferenceFromUrl(downloadUrl)
        }.getOrElse {
            val uri = Uri.parse(downloadUrl)

            val encodedObjectPath = uri.encodedPath
                ?.substringAfter(
                    delimiter = "/o/",
                    missingDelimiterValue = ""
                )
                .orEmpty()

            require(encodedObjectPath.isNotBlank()) {
                "Nie można odczytać ścieżki pliku Storage z URL-a"
            }

            storage.reference.child(
                Uri.decode(encodedObjectPath)
            )
        }
    }
}
