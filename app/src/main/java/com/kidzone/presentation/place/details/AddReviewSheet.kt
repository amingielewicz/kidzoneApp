package com.kidzone.presentation.place.details

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Maksymalna długość komentarza opinii. Świadomy kompromis między swobodą
 * wypowiedzi a UX listy (długie opinie psują skanowanie karty miejsca)
 * oraz kosztem Firestore (1MB hard limit per dokument).
 *
 * Walidacja jest egzekwowana w dwóch miejscach (defense-in-depth):
 *  - tu w UI – cap w `onValueChange` + licznik + kolor erroru,
 *  - w `FirestoreReviewRepository.addReview` – `require(...)`.
 */
private const val COMMENT_MAX_LENGTH = 1000

/** Maksymalna liczba zdjęć na opinię. */
private const val MAX_REVIEW_PHOTOS = 3

/**
 * Bottom sheet z formularzem dodawania LUB edycji opinii o miejscu.
 *
 * - Stan formularza (rating + comment) trzymany lokalnie przez `rememberSaveable`,
 *   żeby przeżył rotację ekranu i tymczasowe schowanie sheetu. Pre-fill z
 *   `initialRating` / `initialComment` – w trybie edycji pochodzą z istniejącej
 *   opinii.
 * - Stan wysyłki (`isSubmitting`, `errorMessage`) przychodzi z parent-VM przez
 *   parametry – sheet jest "głupi", VM steruje cyklem życia operacji.
 * - Po pomyślnym zapisie parent ustawia w VM `showAddReviewSheet = false`,
 *   wtedy sheet znika z drzewa kompozycji – stan formularza się czyści.
 *
 * @param isEditing wpływa tylko na teksty (tytuł / button) – cała logika
 *   "add vs update" jest po stronie ViewModelu.
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

    // Klucze (initialRating / initialComment) gwarantują że jak parent zmieni
    // tryb (np. user otworzy edycję innej opinii) – formularz się zresetuje
    // do nowych wartości startowych.
    var rating by rememberSaveable(initialRating) { mutableIntStateOf(initialRating) }
    var comment by rememberSaveable(initialComment) { mutableStateOf(initialComment) }
    var photoUris by rememberSaveable { mutableStateOf(listOf<Uri>()) }
    // Existing photo URLs from a previously saved review (edit mode)
    var existingPhotoUrls by rememberSaveable(initialPhotoUrls) {
        mutableStateOf(initialPhotoUrls)
    }

    // Total photos = existing URLs + new URIs; constrained to MAX_REVIEW_PHOTOS
    val totalPhotoCount = existingPhotoUrls.size + photoUris.size

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_REVIEW_PHOTOS)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val currentTotal = existingPhotoUrls.size + photoUris.size
            val available = MAX_REVIEW_PHOTOS - currentTotal
            // Deduplikacja: odrzucamy URI już obecne w liście (blokada duplikatów)
            val existingSet = photoUris.map { it.toString() }.toSet()
            val newUris = uris.filter { it.toString() !in existingSet }
            photoUris = photoUris + newUris.take(available)
        }
    }

    val cameraUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri.value?.let { uri ->
                val currentTotal = existingPhotoUrls.size + photoUris.size
                val existingSet = photoUris.map { it.toString() }.toSet()
                if (uri.toString() !in existingSet && currentTotal < MAX_REVIEW_PHOTOS) {
                    photoUris = photoUris + uri
                }
            }
        }
    }

    val context = LocalContext.current

    val title = when {
        isEditing -> "Edytuj swoją opinię"
        placeName.isNotBlank() -> "Oceń \"$placeName\""
        else -> "Dodaj opinię"
    }
    val submitLabel = if (isEditing) "Zapisz zmiany" else "Opublikuj opinię"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // imePadding domyślnie nie obsłuży klawiatury wewnątrz sheetu w
        // wszystkich wersjach Material3, dlatego dodajemy ręcznie poniżej.
        contentWindowInsets = { WindowInsets(0) }
    ) {
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
                    // Hard-cap długości komentarza po stronie UI: tnij do
                    // limitu zamiast odrzucać cały input. Dzięki temu wklejenie
                    // tekstu dłuższego niż 1000 znaków daje pierwsze 1000
                    // (intuicyjne), zamiast po cichu znikać. Repo dodatkowo
                    // waliduje to samo (defense-in-depth).
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
            // Existing photo URLs (from edit mode) + new local URIs
            if (existingPhotoUrls.isNotEmpty() || photoUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Existing remote URLs
                    itemsIndexed(existingPhotoUrls) { index, url ->
                        Box(modifier = Modifier.size(64.dp)) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    existingPhotoUrls = existingPhotoUrls.toMutableList().apply { removeAt(index) }
                                },
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
                    // New local URIs
                    itemsIndexed(photoUris) { index, uri ->
                        Box(modifier = Modifier.size(64.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    photoUris = photoUris.toMutableList().apply { removeAt(index) }
                                },
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
                }
                Spacer(Modifier.height(8.dp))
            }
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
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Galeria (${totalPhotoCount}/$MAX_REVIEW_PHOTOS)")
                    }
                    OutlinedButton(
                        onClick = {
                            val photoFile = File(
                                context.cacheDir,
                                "review_photo_${System.currentTimeMillis()}.jpg"
                            )
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            cameraUri.value = uri
                            cameraLauncher.launch(uri)
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
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
    }
}

/**
 * Interaktywny picker oceny 1..5. Klik w gwiazdkę o numerze N ustawia
 * rating=N (i tym samym podświetla wszystkie gwiazdki ≤ N). Klik w już
 * wybraną gwiazdkę nie zmienia stanu (świadomie, by uniknąć przypadkowego
 * "wyzerowania" oceny).
 */
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
