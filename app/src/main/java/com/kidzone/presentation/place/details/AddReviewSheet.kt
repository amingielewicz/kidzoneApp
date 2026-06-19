package com.kidzone.presentation.place.details

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

/**
 * Maksymalna długość komentarza opinii.
 */
private const val COMMENT_MAX_LENGTH = 1000

/** Maksymalna liczba zdjęć na opinię. */
private const val MAX_REVIEW_PHOTOS = 3

/**
 * Oblicza MD5 hash zawartości URI. Czyta cały strumień (nie tylko 64KB),
 * żeby uniknąć false-negatives na zdjęciach z identycznym nagłówkiem.
 * Zwraca null gdy nie udało się odczytać contentu.
 */
private fun computeContentHash(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val md = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            md.update(buffer, 0, bytesRead)
        }
        inputStream.close()
        md.digest().joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        null
    }
}

/**
 * Pobiera zdjęcie z remote URL (Firebase Storage) i oblicza MD5 hash.
 * Używane do seedowania hashów istniejących zdjęć przy edycji opinii/miejsca,
 * żeby user nie mógł dodać duplikatu z galerii.
 * Wywołuj na Dispatchers.IO.
 */
private fun computeRemoteContentHash(url: String): String? {
    return try {
        val connection = java.net.URL(url).openConnection()
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        val inputStream = connection.getInputStream()
        val md = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            md.update(buffer, 0, bytesRead)
        }
        inputStream.close()
        md.digest().joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        null
    }
}

/**
 * Tworzy tymczasowy plik w cache i zwraca content URI przez FileProvider.
 * Plik jest tworzony na dysku (createNewFile), więc FileProvider nie rzuci.
 */
private fun createTempCameraUri(context: Context): Uri {
    val photoFile = File.createTempFile(
        "review_camera_",
        ".jpg",
        context.cacheDir
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
}

/**
 * Bottom sheet z formularzem dodawania LUB edycji opinii o miejscu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewSheet(
    placeName: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String, photoUris: List<Uri>, retainedPhotoUrls: List<String>) -> Unit,
    initialRating: Int = 0,
    initialComment: String = "",
    initialPhotoUrls: List<String> = emptyList(),
    isEditing: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var rating by rememberSaveable(initialRating) { mutableIntStateOf(initialRating) }
    var comment by rememberSaveable(initialComment) { mutableStateOf(initialComment) }
    var photoUris by rememberSaveable { mutableStateOf(listOf<Uri>()) }
    var existingPhotoUrls by rememberSaveable(initialPhotoUrls) {
        mutableStateOf(initialPhotoUrls)
    }
    // Hash set przechowywany jako List<String> żeby był Parcelable-friendly
    // (rememberSaveable wymaga serializowalności).
    var photoHashList by rememberSaveable { mutableStateOf(listOf<String>()) }
    var hashesReady by remember { mutableStateOf(initialPhotoUrls.isEmpty()) }

    // Seeduj hashe z istniejących remote URLs przy edycji, żeby nie dało się
    // dodać duplikatu (to samo zdjęcie z galerii co już jest w opinii).
    androidx.compose.runtime.LaunchedEffect(initialPhotoUrls) {
        if (initialPhotoUrls.isNotEmpty() && photoHashList.isEmpty()) {
            val hashes = mutableListOf<String>()
            for (url in initialPhotoUrls) {
                val hash = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    computeRemoteContentHash(url)
                }
                if (hash != null) hashes.add(hash)
            }
            if (hashes.isNotEmpty()) {
                photoHashList = photoHashList + hashes
            }
        }
        hashesReady = true
    }

    val totalPhotoCount = existingPhotoUrls.size + photoUris.size

    // --- Duplicate check helper ---
    fun isDuplicate(uri: Uri): Boolean {
        val hash = computeContentHash(context, uri) ?: return false
        return hash in photoHashList
    }

    fun addHashForUri(uri: Uri) {
        val hash = computeContentHash(context, uri)
        if (hash != null && hash !in photoHashList) {
            photoHashList = photoHashList + hash
        }
    }

    // --- Photo picker (galeria) ---
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_REVIEW_PHOTOS)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val available = MAX_REVIEW_PHOTOS - (existingPhotoUrls.size + photoUris.size)
            if (available <= 0) return@rememberLauncherForActivityResult

            val accepted = mutableListOf<Uri>()
            var duplicatesFound = 0
            for (uri in uris) {
                if (accepted.size >= available) break
                if (isDuplicate(uri)) {
                    duplicatesFound++
                    continue
                }
                addHashForUri(uri)
                accepted.add(uri)
            }
            if (accepted.isNotEmpty()) {
                photoUris = photoUris + accepted
            }
            if (duplicatesFound > 0) {
                scope.launch {
                    snackbarHostState.showSnackbar("To zdjęcie zostało już dodane. Nie można dodać duplikatu.")
                }
            }
        }
    }

    // --- Camera ---
    val cameraUri = remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri.value != null) {
            val uri = cameraUri.value!!
            val currentTotal = existingPhotoUrls.size + photoUris.size
            if (currentTotal < MAX_REVIEW_PHOTOS) {
                if (!isDuplicate(uri)) {
                    addHashForUri(uri)
                    photoUris = photoUris + uri
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("To zdjęcie zostało już dodane. Nie można dodać duplikatu.")
                    }
                }
            }
        }
    }

    // Funkcja uruchamiająca aparat (wyodrębniona, bo wołana z 2 miejsc)
    fun launchCamera() {
        val uri = createTempCameraUri(context)
        cameraUri.value = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Brak dostępu do aparatu")
            }
        }
    }

    val title = when {
        isEditing -> "Edytuj swoją opinię"
        placeName.isNotBlank() -> "Oceń \"$placeName\""
        else -> "Dodaj opinię"
    }
    val submitLabel = if (isEditing) "Zapisz zmiany" else "Opublikuj opinię"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Twoja ocena",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            StarRatingInput(
                rating = rating,
                onChange = { rating = it },
                enabled = !isSubmitting
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = { newValue ->
                    comment = newValue.take(COMMENT_MAX_LENGTH)
                },
                label = { Text("Komentarz (opcjonalnie)") },
                placeholder = { Text("Co sądzisz o tym miejscu?") },
                minLines = 3,
                maxLines = 6,
                enabled = !isSubmitting,
                supportingText = {
                    Text(
                        text = "${comment.length} / $COMMENT_MAX_LENGTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (comment.length >= COMMENT_MAX_LENGTH) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // --- Zdjęcia opinii ---
            Spacer(Modifier.height(12.dp))
            if (existingPhotoUrls.isNotEmpty() || photoUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(existingPhotoUrls) { index, url ->
                        PhotoThumbnail(
                            model = url,
                            onRemove = {
                                // Usuwamy hash remote URL żeby ponowne dodanie tego samego zdjęcia
                                // z galerii nie było blokowane jako duplikat.
                                val removedUrl = existingPhotoUrls[index]
                                existingPhotoUrls = existingPhotoUrls.toMutableList().apply { removeAt(index) }
                                scope.launch {
                                    val hash = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        computeRemoteContentHash(removedUrl)
                                    }
                                    if (hash != null) {
                                        photoHashList = photoHashList.filter { it != hash }
                                    }
                                }
                            }
                        )
                    }
                    itemsIndexed(photoUris) { index, uri ->
                        PhotoThumbnail(
                            model = uri,
                            onRemove = {
                                // Usuwamy hash lokalnego URI żeby ponowne dodanie tego samego
                                // zdjęcia nie pokazywało "już dodane".
                                val removedUri = photoUris[index]
                                val hash = computeContentHash(context, removedUri)
                                photoUris = photoUris.toMutableList().apply { removeAt(index) }
                                if (hash != null) {
                                    photoHashList = photoHashList.filter { it != hash }
                                }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Przyciski Galeria + Aparat – zawsze widoczne gdy jest wolne miejsce
            if (totalPhotoCount < MAX_REVIEW_PHOTOS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isSubmitting && hashesReady,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Galeria (${totalPhotoCount}/$MAX_REVIEW_PHOTOS)")
                    }
                    OutlinedButton(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                launchCamera()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        enabled = !isSubmitting && hashesReady,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Aparat")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onSubmit(rating, comment, photoUris, existingPhotoUrls) },
                enabled = rating in 1..5 && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(submitLabel)
                }
            }
            Spacer(Modifier.height(8.dp))
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PhotoThumbnail(
    model: Any,
    onRemove: () -> Unit
) {
    Box(modifier = Modifier.size(64.dp)) {
        AsyncImage(
            model = model,
            contentDescription = "Miniatura zdjęcia opinii",
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(18.dp)
                .background(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Usuń",
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun StarRatingInput(
    rating: Int,
    onChange: (Int) -> Unit,
    enabled: Boolean = true,
    starSize: Dp = 40.dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (1..5).forEach { star ->
            val isFilled = star <= rating
            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = "Oceń na $star",
                tint = if (isFilled) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .size(starSize)
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClick = { onChange(star) }
                    )
            )
        }
        Spacer(Modifier.width(8.dp))
        if (rating > 0) {
            Text(
                text = "$rating/5",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
