@file:Suppress("MatchingDeclarationName")

package com.kidzone.presentation.common

import android.content.Context
import android.net.Uri
import java.security.MessageDigest

data class PhotoSelectionResult(
    val acceptedUris: List<Uri>,
    val acceptedHashes: Set<String>,
    val duplicatesFound: Int
)

fun selectUniquePhotoUris(
    context: Context,
    uris: List<Uri>,
    availableSlots: Int,
    knownHashes: Set<String> = emptySet()
): PhotoSelectionResult {
    if (uris.isEmpty() || availableSlots <= 0) {
        return PhotoSelectionResult(
            acceptedUris = emptyList(),
            acceptedHashes = emptySet(),
            duplicatesFound = 0
        )
    }

    val accepted = mutableListOf<Uri>()
    val hashes = knownHashes.toMutableSet()
    val acceptedHashes = mutableSetOf<String>()
    var duplicatesFound = 0

    for (uri in uris) {
        if (accepted.size >= availableSlots) break
        val hash = computePhotoContentHash(context, uri)
        if (hash != null && hash in hashes) {
            duplicatesFound++
        } else {
            if (hash != null) {
                hashes.add(hash)
                acceptedHashes.add(hash)
            }
            accepted.add(uri)
        }
    }

    return PhotoSelectionResult(
        acceptedUris = accepted,
        acceptedHashes = acceptedHashes,
        duplicatesFound = duplicatesFound
    )
}

fun computePhotoContentHash(context: Context, uri: Uri): String? = try {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    inputStream.use { stream ->
        val md = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(PHOTO_HASH_BUFFER_SIZE)
        var bytesRead: Int
        while (stream.read(buffer).also { bytesRead = it } != -1) {
            md.update(buffer, 0, bytesRead)
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }
} catch (_: Exception) {
    null
}

fun computeRemotePhotoContentHash(url: String): String? = try {
    val connection = java.net.URL(url).openConnection()
    connection.connectTimeout = REMOTE_PHOTO_HASH_TIMEOUT_MS
    connection.readTimeout = REMOTE_PHOTO_HASH_TIMEOUT_MS
    connection.getInputStream().use { stream ->
        val md = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(PHOTO_HASH_BUFFER_SIZE)
        var bytesRead: Int
        while (stream.read(buffer).also { bytesRead = it } != -1) {
            md.update(buffer, 0, bytesRead)
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }
} catch (_: Exception) {
    null
}

private const val PHOTO_HASH_BUFFER_SIZE = 8192
private const val REMOTE_PHOTO_HASH_TIMEOUT_MS = 10_000
