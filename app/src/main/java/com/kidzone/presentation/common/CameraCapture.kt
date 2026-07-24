package com.kidzone.presentation.common

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

private const val CAMERA_CACHE_DIR = "review_photos"

/**
 * 🎯 Odpowiedzialności:
 * - Tworzenie bezpiecznych kontenerów (plików tymczasowych) dla zdjęć z aparatu.
 * - Generowanie URI kompatybilnych z [FileProvider].
 *
 * ⚙️ Techniczne:
 * - Przechowuje pliki w katalogu cache aplikacji (`/cache/review_photos`).
 * - Wykorzystuje [FileProvider] do bezpiecznego udostępniania plików zewnętrznym aplikacjom aparatu.
 */
fun createCameraImageUri(context: Context, filePrefix: String): Uri? = runCatching {
    val photoDir = File(context.cacheDir, CAMERA_CACHE_DIR).apply {
        if (!exists()) mkdirs()
    }
    if (!photoDir.exists() || !photoDir.canWrite()) return@runCatching null
    val photoFile = File.createTempFile(filePrefix, ".jpg", photoDir)
    FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
}.getOrNull()
