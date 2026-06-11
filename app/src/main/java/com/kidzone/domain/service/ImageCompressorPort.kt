package com.kidzone.domain.service

import android.net.Uri

/**
 * Abstraction over image compression pipeline.
 *
 * Decouples ViewModels from Android ContentResolver and Bitmap APIs.
 * Production implementation delegates to [com.kidzone.utils.ImageCompressor].
 */
interface ImageCompressorPort {
    /**
     * Compresses image at [uri] to WebP format.
     * @return ByteArray of compressed image, or null if decode failed.
     */
    fun compressToWebp(uri: Uri): ByteArray?
}
