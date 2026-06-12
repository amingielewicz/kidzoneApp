package com.kidzone.data.service

import android.content.Context
import android.net.Uri
import com.kidzone.analytics.PerformanceTraces
import com.kidzone.domain.service.ImageCompressorPort
import com.kidzone.utils.ImageCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of [ImageCompressorPort].
 *
 * Delegates to the existing [ImageCompressor] utility which handles
 * decode → EXIF rotate → resize → WebP compress pipeline.
 * Wraps with Firebase Performance trace.
 */
@Singleton
class AndroidImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performanceTraces: PerformanceTraces
) : ImageCompressorPort {

    override fun compressToWebp(uri: Uri): ByteArray? {
        return performanceTraces.measureSync(PerformanceTraces.IMAGE_COMPRESS) {
            ImageCompressor.compressToWebp(context, uri)
        }
    }
}
