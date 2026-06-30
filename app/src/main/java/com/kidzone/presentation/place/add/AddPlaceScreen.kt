package com.kidzone.presentation.place.add

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kidzone.R
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.presentation.common.style
import com.kidzone.presentation.common.rememberHapticFeedback
import com.kidzone.utils.UiText
import kotlinx.coroutines.launch

/**
 * Ekran dodawania nowego miejsca – formularz zapisywany do Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaceScreen(
    onSaved: (newPlaceLat: Double?, newPlaceLng: Double?) -> Unit,
    onBack: () -> Unit,
    viewModel: AddPlaceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = rememberHapticFeedback()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            haptic.success()
            if (state.shouldRequestReview) {
                val activity = context as? android.app.Activity
                if (activity != null) {
                    com.kidzone.review.InAppReviewManager(context).launchReviewFlow(activity)
                }
                viewModel.consumeReviewRequest()
            }
            onSaved(state.savedNewLatitude, state.savedNewLongitude)
        }
    }

    LaunchedEffect(state.photoDuplicateMessage) {
        val msg = state.photoDuplicateMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg.asString(context))
            viewModel.consumePhotoDuplicateMessage()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            coroutineScope.launch { fetchAndSetLocation(context, viewModel) }
        } else {
            viewModel.onLocationError(UiText.StringResource(R.string.location_permission_denied))
        }
    }

    var photoHashSet by remember { mutableStateOf(setOf<String>()) }
    var placeHashesReady by remember { mutableStateOf(!state.isEditMode) }

    androidx.compose.runtime.LaunchedEffect(state.existingPhotoUrls) {
        if (state.existingPhotoUrls.isNotEmpty() && photoHashSet.isEmpty()) {
            val hashes = mutableSetOf<String>()
            for (url in state.existingPhotoUrls) {
                val hash = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    computeRemotePlacePhotoHash(url)
                }
                if (hash != null) hashes.add(hash)
            }
            if (hashes.isNotEmpty()) {
                photoHashSet = photoHashSet + hashes
            }
        }
        placeHashesReady = true
    }

    val duplicatePhotoError = stringResource(R.string.duplicate_photo_error)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_PLACE_PHOTOS)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val accepted = mutableListOf<Uri>()
            val hashes = photoHashSet.toMutableSet()
            var duplicatesFound = 0
            for (uri in uris) {
                val hash = computePlacePhotoHash(context, uri)
                if (hash != null && hash in hashes) {
                    duplicatesFound++
                    continue
                }
                if (hash != null) hashes.add(hash)
                accepted.add(uri)
            }
            photoHashSet = hashes
            if (accepted.isNotEmpty()) {
                viewModel.addPhotos(accepted)
            }
            if (duplicatesFound > 0) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(duplicatePhotoError)
                }
            }
        }
    }

    val placeCameraUri = remember { mutableStateOf<Uri?>(null) }
    val placeCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && placeCameraUri.value != null) {
            val uri = placeCameraUri.value!!
            val hash = computePlacePhotoHash(context, uri)
            if (hash == null || hash !in photoHashSet) {
                if (hash != null) photoHashSet = photoHashSet + hash
                viewModel.addPhotos(listOf(uri))
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(duplicatePhotoError)
                }
            }
        }
    }

    fun launchPlaceCamera() {
        val uri = createPlaceCameraUri(context)
        placeCameraUri.value = uri
        placeCameraLauncher.launch(uri)
    }

    val cameraAccessDenied = stringResource(R.string.camera_access_denied)
    val placeCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchPlaceCamera()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(cameraAccessDenied)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) stringResource(R.string.edit_place_title)
                        else stringResource(R.string.add_place)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            val nameHasError = state.hasTriedToSave && state.name.isBlank()
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { RequiredFieldLabel(stringResource(R.string.place_name_label)) },
                singleLine = true,
                supportingText = {
                    val requiredText = if (state.name.isBlank()) {
                        stringResource(R.string.field_required) + ". "
                    } else {
                        ""
                    }
                    Text("$requiredText${state.name.length}/$PLACE_NAME_MAX_LENGTH")
                },
                isError = nameHasError,
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        if (nameHasError) {
                            error(context.getString(R.string.field_required))
                        }
                    }
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.place_description_label)) },
                minLines = 2,
                maxLines = 5,
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            CategoryDropdown(
                selected = state.category,
                onSelected = viewModel::onCategoryChange,
                enabled = !state.isSaving
            )

            Spacer(Modifier.height(8.dp))

            Spacer(Modifier.height(12.dp))

            LocationSection(
                latitude = state.latitude,
                longitude = state.longitude,
                isFetching = state.isFetchingLocation,
                onClickFetch = {
                    if (hasLocationPermission(context)) {
                        coroutineScope.launch { fetchAndSetLocation(context, viewModel) }
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                enabled = !state.isSaving
            )

            Spacer(Modifier.height(6.dp))
            Text(
                text = if (state.latitude == null || state.longitude == null) {
                    stringResource(R.string.location_required_hint)
                } else {
                    stringResource(R.string.location_fetched_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (state.latitude == null || state.longitude == null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                    }
            )

            if (state.nearbyPlaces.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                NearbyPlacesList(places = state.nearbyPlaces)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.address,
                onValueChange = { /* read-only */ },
                label = { Text(stringResource(R.string.address_label)) },
                supportingText = {
                    Text(stringResource(R.string.address_auto_hint))
                },
                singleLine = true,
                readOnly = true,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.amenities_label),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            AmenitiesGrid(
                selected = state.amenities,
                category = state.category,
                amenityFrequency = state.amenityFrequency,
                onToggle = viewModel::toggleAmenity,
                enabled = !state.isSaving
            )

            state.errorMessage?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = msg.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Assertive
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.photos_with_count, state.photoUris.size + state.existingPhotoUrls.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            if (state.existingPhotoUrls.isNotEmpty() || state.photoUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(state.existingPhotoUrls) { index, url ->
                        PhotoThumbnail(
                            model = url,
                            onRemove = {
                                val removedUrl = state.existingPhotoUrls[index]
                                viewModel.removeExistingPhoto(index)
                                coroutineScope.launch {
                                    val hash = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        computeRemotePlacePhotoHash(removedUrl)
                                    }
                                    if (hash != null) {
                                        photoHashSet = photoHashSet - hash
                                    }
                                }
                            },
                            enabled = !state.isSaving
                        )
                    }
                    itemsIndexed(state.photoUris) { index, uri ->
                        PhotoThumbnail(
                            model = uri,
                            onRemove = {
                                val removedUri = state.photoUris[index]
                                val hash = computePlacePhotoHash(context, removedUri)
                                viewModel.removeNewPhoto(index)
                                if (hash != null) {
                                    photoHashSet = photoHashSet - hash
                                }
                            },
                            enabled = !state.isSaving
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (state.canAddMorePhotos) {
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
                        enabled = !state.isSaving && placeHashesReady,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.gallery))
                    }
                    OutlinedButton(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                launchPlaceCamera()
                            } else {
                                placeCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                        enabled = !state.isSaving && placeHashesReady,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.camera))
                    }
                }
            }

            if (state.isUploadingPhotos) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.uploading_photos),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving && !state.isLoadingPlace && state.isFormValid,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (state.isEditMode) stringResource(R.string.update_place_action)
                        else stringResource(R.string.save_place_action)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    val duplicateCandidate = state.duplicateCandidate
    if (state.showDuplicateWarning && duplicateCandidate != null) {
        DuplicateWarningDialog(
            candidate = duplicateCandidate,
            onConfirm = viewModel::confirmSaveDespiteDuplicate,
            onDismiss = viewModel::dismissDuplicateWarning
        )
    }
}

private suspend fun fetchAndSetLocation(
    context: android.content.Context,
    viewModel: AddPlaceViewModel
) {
    viewModel.onFetchingLocationStart()

    if (!isLocationServiceEnabled(context)) {
        viewModel.onLocationError(UiText.StringResource(R.string.error_location_service_disabled))
        return
    }

    try {
        val coords = fetchCurrentLocation(context)
        if (coords == null) {
            viewModel.onLocationError(UiText.StringResource(R.string.error_location_timeout))
            return
        }
        val address = runCatching { reverseGeocode(context, coords.first, coords.second) }
            .getOrNull()
        viewModel.onLocationFetched(coords.first, coords.second, address)
    } catch (e: Exception) {
        viewModel.onLocationError(UiText.StringResource(R.string.error_location_timeout))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: PlaceCategory,
    onSelected: (PlaceCategory) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedStyle = selected.style

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = stringResource(selected.labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.category_label)) },
            leadingIcon = {
                Icon(
                    imageVector = selectedStyle.icon,
                    contentDescription = null,
                    tint = selectedStyle.color
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PlaceCategory.entries.forEach { category ->
                val style = category.style
                DropdownMenuItem(
                    text = { Text(stringResource(category.labelRes)) },
                    leadingIcon = {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            tint = style.color
                        )
                    },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LocationSection(
    latitude: Double?,
    longitude: Double?,
    isFetching: Boolean,
    onClickFetch: () -> Unit,
    enabled: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onClickFetch,
            enabled = enabled && !isFetching,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (isFetching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Filled.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (latitude != null && longitude != null) {
                        stringResource(R.string.update_location_action)
                    } else {
                        stringResource(R.string.fetch_location_action)
                    }
                )
            }
        }
        if (latitude != null && longitude != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "GPS: %.5f, %.5f".format(latitude, longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmenitiesGrid(
    selected: Set<Amenity>,
    category: PlaceCategory,
    amenityFrequency: Map<Amenity, Int>,
    onToggle: (Amenity) -> Unit,
    enabled: Boolean
) {
    val applicable = remember(category, amenityFrequency) {
        Amenity.forCategory(category)
            .sortedWith(
                compareByDescending<Amenity> { amenityFrequency[it] ?: 0 }
                    .thenBy { it.ordinal }
            )
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        applicable.forEach { amenity ->
            FilterChip(
                selected = amenity in selected,
                onClick = { onToggle(amenity) },
                enabled = enabled,
                label = { Text(stringResource(amenity.labelRes)) }
            )
        }
    }
}

@Composable
private fun RequiredFieldLabel(text: String) {
    val errorColor = MaterialTheme.colorScheme.error
    Text(
        buildAnnotatedString {
            append(text)
            withStyle(SpanStyle(color = errorColor)) {
                append(" *")
            }
        }
    )
}

@Composable
private fun NearbyPlacesList(places: List<AddPlaceViewModel.NearbyPlace>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.nearby_places_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            places.take(5).forEach { place ->
                val style = place.category.style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = stringResource(R.string.category_label),
                        tint = style.color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        text = "${place.distanceMeters}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DuplicateWarningDialog(
    candidate: AddPlaceViewModel.NearbyPlace,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val style = candidate.category.style
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.color
            )
        },
        title = { Text(stringResource(R.string.duplicate_warning_title)) },
        text = {
            Text(
                text = stringResource(
                    R.string.duplicate_warning_message,
                    candidate.distanceMeters,
                    candidate.name,
                    stringResource(candidate.category.labelRes)
                )
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.duplicate_warning_confirm))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Suppress("FunctionNaming")
@Composable
private fun PhotoThumbnail(
    model: Any,
    onRemove: () -> Unit,
    enabled: Boolean = true
) {
    Box(modifier = Modifier.size(80.dp)) {
        AsyncImage(
            model = model,
            contentDescription = stringResource(R.string.photo_thumbnail_description),
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        if (enabled) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.remove_photo_description),
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun computePlacePhotoHash(context: android.content.Context, uri: Uri): String? {
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

private fun createPlaceCameraUri(context: android.content.Context): Uri {
    val photoFile = File.createTempFile(
        "place_camera_",
        ".jpg",
        context.cacheDir
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
}

private fun computeRemotePlacePhotoHash(url: String): String? {
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
