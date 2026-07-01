package com.kidzone.presentation.common

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

private const val CAMERA_CACHE_DIR = "review_photos"

fun createCameraImageUri(context: Context, filePrefix: String): Uri? = runCatching {
    val photoDir = File(context.cacheDir, CAMERA_CACHE_DIR).apply { mkdirs() }
    val photoFile = File.createTempFile(filePrefix, ".jpg", photoDir)
    FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
}.getOrNull()
