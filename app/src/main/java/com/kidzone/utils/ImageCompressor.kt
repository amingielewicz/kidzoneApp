package com.kidzone.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/**
 * Utility do kompresji zdjęć przed uploadem do Firebase Storage.
 *
 * Pipeline: URI → decode → auto-rotate (EXIF) → resize (max [MAX_DIMENSION]) →
 * compress to WebP (quality [WEBP_QUALITY]) → ByteArray.
 *
 * Typowy wynik: zdjęcie 4000x3000 (12MP, ~4MB JPEG) → 1024x768, WebP ~80-150KB.
 */
object ImageCompressor {

    /** Maksymalna szerokość/wysokość po resize. */
    private const val MAX_DIMENSION = 1024

    /** Jakość WebP (0-100). 75 to dobry balans rozmiar/jakość. */
    private const val WEBP_QUALITY = 75

    /**
     * Kompresuje zdjęcie z podanego URI do WebP ByteArray.
     *
     * @param context do otwarcia ContentResolver
     * @param uri URI zdjęcia (z photo pickera lub kamery)
     * @return ByteArray skompresowanego WebP, lub null jeśli decode się nie powiódł
     */
    fun compressToWebp(context: Context, uri: Uri): ByteArray? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        // Oblicz inSampleSize (downsampling przy decode – oszczędza RAM)
        val (origWidth, origHeight) = options.outWidth to options.outHeight
        options.inSampleSize = calculateInSampleSize(origWidth, origHeight, MAX_DIMENSION)
        options.inJustDecodeBounds = false

        val stream2 = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(stream2, null, options) ?: run {
            stream2.close()
            return null
        }
        stream2.close()

        // Auto-rotate na podstawie EXIF (zdjęcia z kamery bywają obrócone)
        val rotated = autoRotate(context, uri, bitmap)

        // Resize do max dimension (inSampleSize może dać trochę większy wynik)
        val resized = resizeIfNeeded(rotated, MAX_DIMENSION)

        // Compress do WebP (lossy)
        val output = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.WEBP_LOSSY, WEBP_QUALITY, output)

        // Cleanup
        if (resized !== rotated) resized.recycle()
        if (rotated !== bitmap) rotated.recycle()
        bitmap.recycle()

        return output.toByteArray()
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        val larger = maxOf(width, height)
        while (larger / inSampleSize > maxDim * 2) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun resizeIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap

        val scale = maxDim.toFloat() / maxOf(w, h)
        val newW = (w * scale).toInt()
        val newH = (h * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun autoRotate(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val rotation = try {
            val input = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(input)
            input.close()
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (_: Exception) {
            0f
        }
        if (rotation == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
