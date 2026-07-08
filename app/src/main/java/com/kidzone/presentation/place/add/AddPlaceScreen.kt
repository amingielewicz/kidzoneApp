package com.kidzone.presentation.place.add

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.core.content.ContextCompat
import java.security.MessageDigest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kidzone.R
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.presentation.common.createCameraImageUri
import com.kidzone.presentation.common.rememberHapticFeedback
import com.kidzone.presentation.common.style
import com.kidzone.utils.UiText
import com.kidzone.presentation.common.NetworkStatus
import com.kidzone.presentation.common.createCameraImageUri
import com.kidzone.presentation.common.rememberHapticFeedback
import com.kidzone.presentation.common.rememberNetworkStatus
import com.kidzone.presentation.common.style
import kotlinx.coroutines.launch

private val FORM_SECTION_GAP = 14.dp
private val FORM_VERTICAL_SPACING = 10.dp
private val SECTION_PADDING = 12.dp
private val PHOTO_THUMBNAIL_SIZE = 76.dp
private val PHOTO_REMOVE_BUTTON_SIZE = 20.dp
private val COUNTER_ROW_HEIGHT = 20.dp
private const val PLACE_NAME_UI_MAX_LENGTH = 50
private const val PLACE_NAME_WARNING_LENGTH = 40
private const val PLACE_DESCRIPTION_UI_MAX_LENGTH = 500
private const val PLACE_DESCRIPTION_COUNTER_THRESHOLD = 400
private const val PLACE_DESCRIPTION_WARNING_LENGTH = 480
private const val NEARBY_VISIBLE_LIMIT = 5
private const val LIMIT_REACHED_HINT = "Osiągnięto maksymalną liczbę znaków"
private const val LOCATION_PERMISSION_HELPER = "Aby pobrać lokalizację, zezwól na dostęp do GPS."
private const val LOCATION_GPS_HELPER = "Włącz GPS, aby pobrać lokalizację."
private const val LOCATION_READY_HELPER = "Kliknij przycisk powyżej, aby pobrać adres."
private const val SAVE_HINT_NAME_AND_LOCATION = "Wpisz nazwę i pobierz lokalizację, aby zapisać miejsce."
private const val SAVE_HINT_NAME = "Wpisz nazwę miejsca, aby odblokować zapis."
private const val SAVE_HINT_LOCATION = "Pobierz lokalizację miejsca, aby odblokować zapis."
private const val OFFLINE_SAVE_HINT = "Brak internetu. Dane zostają w formularzu — zapisz po odzyskaniu połączenia."
private const val OFFLINE_SAVE_SNACKBAR = "Brak internetu. Nie wyczyściliśmy formularza — spróbuj ponownie po odzyskaniu połączenia."
private const val OFFLINE_SAVE_ACTION = "Zapisz po odzyskaniu internetu"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaceScreen(
    onSaved: (newPlaceLat: Double?, newPlaceLng: Double?) -> Unit,
    onBack: () -> Unit,
    onOpenExistingPlace: (String) -> Unit,
    viewModel: AddPlaceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val networkStatus by rememberNetworkStatus()
    val isOffline = networkStatus != NetworkStatus.AVAILABLE
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val offlineSaveHint = stringResource(R.string.add_place_offline_save_hint)
    val offlineSaveAction = stringResource(R.string.add_place_offline_save_action)
    val offlineSaveSnackbar = stringResource(R.string.add_place_offline_save_snackbar)
    val haptic = rememberHapticFeedback()
    var locationPermissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }
    var locationServiceEnabled by remember { mutableStateOf(isLocationServiceEnabled(context)) }

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

    LaunchedEffect(isOffline) {
        if (isOffline) {
            snackbarHostState.showSnackbar(OFFLINE_SAVE_HINT)
        }
    }

    LaunchedEffect(state.photoDuplicateMessage) {
        val msg = state.photoDuplicateMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg.asString(context))
            viewModel.consumePhotoDuplicateMessage()
        }
    }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        locationPermissionGranted = hasLocationPermission(context)
        locationServiceEnabled = isLocationServiceEnabled(context)
        if (locationPermissionGranted && locationServiceEnabled) {
            coroutineScope.launch { fetchAndSetLocation(context, viewModel) }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationPermissionGranted = granted || hasLocationPermission(context)
        locationServiceEnabled = isLocationServiceEnabled(context)
        when {
            !locationPermissionGranted -> viewModel.onLocationError(
                UiText.StringResource(R.string.location_permission_denied)
            )
            !locationServiceEnabled -> locationSettingsLauncher.launch(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            )
            else -> coroutineScope.launch { fetchAndSetLocation(context, viewModel) }
        }
    }

    var photoHashSet by remember { mutableStateOf(setOf<String>()) }
    var placeHashesReady by remember { mutableStateOf(!state.isEditMode) }

    LaunchedEffect(state.existingPhotoUrls) {
        if (state.existingPhotoUrls.isNotEmpty() && photoHashSet.isEmpty()) {
            val hashes = mutableSetOf<String>()
            for (url in state.existingPhotoUrls) {
                val hash = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    computeRemotePlacePhotoHash(url)
                }
                if (hash != null) hashes.add(hash)
            }
            if (hashes.isNotEmpty()) photoHashSet = photoHashSet + hashes
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
            if (accepted.isNotEmpty()) viewModel.addPhotos(accepted)
            if (duplicatesFound > 0) {
                coroutineScope.launch { snackbarHostState.showSnackbar(duplicatePhotoError) }
            }
        }
    }

    var placeCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val placeCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = placeCameraUriString?.let(Uri::parse)
        if (success && uri != null) {
            val hash = computePlacePhotoHash(context, uri)
            if (hash == null || hash !in photoHashSet) {
                if (hash != null) photoHashSet = photoHashSet + hash
                viewModel.addPhotos(listOf(uri))
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar(duplicatePhotoError) }
            }
        }
        placeCameraUriString = null
    }

    val cameraUnavailable = stringResource(R.string.camera_unavailable)
    fun launchPlaceCamera() {
        val uri = createCameraImageUri(context, "place_camera_")
        if (uri == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar(cameraUnavailable) }
            return
        }
        placeCameraUriString = uri.toString()
        runCatching { placeCameraLauncher.launch(uri) }.onFailure {
            placeCameraUriString = null
            coroutineScope.launch { snackbarHostState.showSnackbar(cameraUnavailable) }
        }
    }

    val cameraAccessDenied = stringResource(R.string.camera_access_denied)
    val placeCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchPlaceCamera() else coroutineScope.launch {
            snackbarHostState.showSnackbar(cameraAccessDenied)
        }
    }

    fun handleLocationClick() {
        locationPermissionGranted = hasLocationPermission(context)
        locationServiceEnabled = isLocationServiceEnabled(context)
        when {
            !locationPermissionGranted -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            !locationServiceEnabled -> locationSettingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            else -> coroutineScope.launch { fetchAndSetLocation(context, viewModel) }
        }
    }

    fun handleSaveClick() {
        if (isOffline) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(offlineSaveSnackbar)
            }
            return
        }

        viewModel.save(isOffline = false)
    }

    val saveHint = when {
        state.isSaving || state.isLoadingPlace -> null
        !state.isFormValid && state.name.isBlank() &&
                (state.latitude == null || state.longitude == null) -> SAVE_HINT_NAME_AND_LOCATION
        !state.isFormValid && state.name.isBlank() -> SAVE_HINT_NAME
        !state.isFormValid && (state.latitude == null || state.longitude == null) -> SAVE_HINT_LOCATION
        isOffline -> offlineSaveHint
        else -> null
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
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = ::handleSaveClick,
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
                                text = when {
                                    isOffline -> offlineSaveAction
                                    state.isEditMode -> stringResource(R.string.update_place_action)
                                    else -> stringResource(R.string.save_place_action)
                                }
                            )
                        }
                    }
                    saveHint?.let { hint ->
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FORM_SECTION_GAP)
        ) {
            val nameHasError = state.hasTriedToSave && state.name.isBlank()
            val nameLength = state.name.length
            val showNameCounter = nameLength > 0
            val nameWarning = nameLength >= PLACE_NAME_WARNING_LENGTH
            val nameLimitReached = nameLength >= PLACE_NAME_UI_MAX_LENGTH
            val descriptionLength = state.description.length
            val showDescriptionCounter = descriptionLength >= PLACE_DESCRIPTION_COUNTER_THRESHOLD
            val descriptionWarning = descriptionLength >= PLACE_DESCRIPTION_WARNING_LENGTH
            val descriptionLimitReached = descriptionLength >= PLACE_DESCRIPTION_UI_MAX_LENGTH
            val hasCoordinates = state.latitude != null && state.longitude != null
            val addressHelper = when {
                !locationPermissionGranted -> LOCATION_PERMISSION_HELPER
                !locationServiceEnabled -> LOCATION_GPS_HELPER
                !hasCoordinates -> LOCATION_READY_HELPER
                else -> null
            }

            FormSection(title = "Podstawy") {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.onNameChange(it.take(PLACE_NAME_UI_MAX_LENGTH)) },
                    label = { RequiredFieldLabel(stringResource(R.string.place_name_label)) },
                    singleLine = true,
                    supportingText = if (nameHasError || showNameCounter) {
                        {
                            CharacterCounterRow(
                                count = nameLength,
                                max = PLACE_NAME_UI_MAX_LENGTH,
                                limitReached = nameLimitReached,
                                showLimitMessage = nameLimitReached,
                                requiredMessage = stringResource(R.string.field_required).takeIf { nameHasError }
                            )
                        }
                    } else null,
                    isError = nameHasError || nameWarning,
                    enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth().semantics {
                        if (nameHasError) error(context.getString(R.string.field_required))
                    }
                )

                OutlinedTextField(
                    value = state.description,
                    onValueChange = { viewModel.onDescriptionChange(it.take(PLACE_DESCRIPTION_UI_MAX_LENGTH)) },
                    label = { Text(stringResource(R.string.place_description_label)) },
                    minLines = 2,
                    maxLines = 5,
                    supportingText = if (showDescriptionCounter) {
                        {
                            CharacterCounterRow(
                                count = descriptionLength,
                                max = PLACE_DESCRIPTION_UI_MAX_LENGTH,
                                limitReached = descriptionLimitReached,
                                showLimitMessage = descriptionLimitReached
                            )
                        }
                    } else null,
                    isError = descriptionWarning,
                    enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )

                CategoryDropdown(
                    selected = state.category,
                    onSelected = viewModel::onCategoryChange,
                    enabled = !state.isSaving
                )
            }

            FormSection(title = "Lokalizacja") {
                LocationSection(
                    latitude = state.latitude,
                    longitude = state.longitude,
                    isFetching = state.isFetchingLocation,
                    hasLocationPermission = locationPermissionGranted,
                    isLocationEnabled = locationServiceEnabled,
                    onClickFetch = ::handleLocationClick,
                    enabled = !state.isSaving
                )

                if (state.nearbyPlaces.isNotEmpty()) {
                    NearbyPlacesList(
                        places = state.nearbyPlaces,
                        onOpenPlace = onOpenExistingPlace
                    )
                }

                AddressReadOnlyCard(address = state.address, helperText = addressHelper)
            }

            FormSection(title = "Szczegóły") {
                Text(
                    text = "Udogodnienia (${state.amenities.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                AmenitiesGrid(
                    selected = state.amenities,
                    category = state.category,
                    onToggle = viewModel::toggleAmenity,
                    enabled = !state.isSaving
                )

                state.errorMessage?.let { msg ->
                    Text(
                        text = msg.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }

                PhotosSection(
                    existingPhotoUrls = state.existingPhotoUrls,
                    photoUris = state.photoUris,
                    canAddMorePhotos = state.canAddMorePhotos,
                    isSaving = state.isSaving,
                    placeHashesReady = placeHashesReady,
                    onPickFromGallery = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onTakePhoto = {
                        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        if (hasPerm) launchPlaceCamera() else placeCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onRemoveExisting = { index ->
                        val removedUrl = state.existingPhotoUrls[index]
                        viewModel.removeExistingPhoto(index)
                        coroutineScope.launch {
                            val hash = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                computeRemotePlacePhotoHash(removedUrl)
                            }
                            if (hash != null) photoHashSet = photoHashSet - hash
                        }
                    },
                    onRemoveNew = { index ->
                        val removedUri = state.photoUris[index]
                        val hash = computePlacePhotoHash(context, removedUri)
                        viewModel.removeNewPhoto(index)
                        if (hash != null) photoHashSet = photoHashSet - hash
                    }
                )

                if (state.isUploadingPhotos) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(R.string.uploading_photos),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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

@Composable
private fun CharacterCounterRow(
    count: Int,
    max: Int,
    limitReached: Boolean,
    showLimitMessage: Boolean,
    requiredMessage: String? = null
) {
    val color = when {
        requiredMessage != null || limitReached -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier.fillMaxWidth().height(COUNTER_ROW_HEIGHT),
        contentAlignment = Alignment.CenterStart
    ) {
        when {
            requiredMessage != null -> {
                Text(
                    text = requiredMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            else -> {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$count/$max",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        maxLines = 1
                    )
                    if (showLimitMessage) {
                        Text(text = " | ", style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
                        Text(
                            text = LIMIT_REACHED_HINT,
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(SECTION_PADDING).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(FORM_VERTICAL_SPACING),
            content = {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                content()
            }
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
        val coords = kotlinx.coroutines.withTimeoutOrNull(12_000L) {
            fetchCurrentLocation(context)
        }

        if (coords == null) {
            viewModel.onLocationError(UiText.StringResource(R.string.error_location_timeout))
            return
        }

        val address = kotlinx.coroutines.withTimeoutOrNull(4_000L) {
            runCatching {
                reverseGeocode(context, coords.first, coords.second)
            }.getOrNull()
        }

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
                Icon(imageVector = selectedStyle.icon, contentDescription = null, tint = selectedStyle.color)
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PlaceCategory.entries.forEach { category ->
                val style = category.style
                DropdownMenuItem(
                    text = { Text(stringResource(category.labelRes)) },
                    leadingIcon = { Icon(imageVector = style.icon, contentDescription = null, tint = style.color) },
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
    hasLocationPermission: Boolean,
    isLocationEnabled: Boolean,
    onClickFetch: () -> Unit,
    enabled: Boolean
) {
    val isReady = hasLocationPermission && isLocationEnabled
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onClickFetch,
            enabled = enabled && !isFetching,
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            if (isFetching) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
                Text("Pobieranie lokalizacji...")
            } else {
                LocationButtonIcon(isReady = isReady)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = when {
                        !hasLocationPermission -> "Zezwól na lokalizację"
                        !isLocationEnabled -> "Włącz GPS"
                        latitude != null && longitude != null -> stringResource(R.string.update_location_action)
                        else -> stringResource(R.string.fetch_location_action)
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

@Composable
private fun LocationButtonIcon(isReady: Boolean) {
    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Filled.MyLocation,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!isReady) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(12.dp).align(Alignment.BottomEnd)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmenitiesGrid(
    selected: Set<Amenity>,
    category: PlaceCategory,
    onToggle: (Amenity) -> Unit,
    enabled: Boolean
) {
    val applicable = remember(category) { Amenity.forCategory(category) }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(FORM_VERTICAL_SPACING)
    ) {
        applicable.forEach { amenity ->
            AmenityChip(
                amenity = amenity,
                selected = amenity in selected,
                enabled = enabled,
                onClick = { onToggle(amenity) }
            )
        }
    }
}

@Composable
private fun AmenityChip(
    amenity: Amenity,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val border = BorderStroke(
        width = 1.dp,
        color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
    )

    Surface(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor,
        border = border
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = amenityIcon(amenity), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(amenity.labelRes),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun amenityIcon(amenity: Amenity): String = when (amenity) {
    Amenity.FENCING -> "🛡"
    Amenity.SOFT_SURFACE, Amenity.SOFT_PROTECTION -> "🧸"
    Amenity.TODDLER_ZONE, Amenity.AGE_ZONES -> "👶"
    Amenity.CAR_FREE_AREA, Amenity.SAFE_PATHS -> "🚸"
    Amenity.TOILET -> "🚻"
    Amenity.CHANGING_TABLE -> "🍼"
    Amenity.STROLLER_ACCESS, Amenity.STROLLER_RENTAL -> "♿"
    Amenity.SHADED_BENCHES, Amenity.REST_AREAS -> "🌳"
    Amenity.GOOD_LIGHTING -> "💡"
    Amenity.PARKING, Amenity.FAMILY_PARKING -> "🅿"
    Amenity.KIDS_MENU, Amenity.HIGH_CHAIR, Amenity.KIDS_TABLEWARE -> "🍽"
    Amenity.KIDS_ENTERTAINMENT, Amenity.SENSORY_TOYS -> "🎲"
    Amenity.KIDS_CORNER_VISIBLE -> "👀"
    Amenity.FAST_SERVICE, Amenity.FAMILY_FAST_TRACK -> "⚡"
    Amenity.QUIET_FEEDING, Amenity.BREASTFEEDING_AREA -> "🤱"
    Amenity.MICROWAVE -> "♨"
    Amenity.NO_LOUD_MUSIC, Amenity.QUIET_AREAS -> "🔇"
    Amenity.ANIMATOR -> "🎈"
    Amenity.MONITORING -> "📹"
    Amenity.TOY_SANITIZATION -> "🧼"
    Amenity.PARENT_ZONE, Amenity.PARENT_CHILD_ROOM -> "👨‍👩‍👧"
    Amenity.LOCKERS -> "🔒"
    Amenity.PICNIC_AREA -> "🧺"
    Amenity.DRINKING_WATER -> "💧"
    Amenity.LOST_CHILD_POINT -> "📍"
    Amenity.WIDE_DOORS -> "↔"
    Amenity.KID_FRIENDLY_SIGNS -> "ℹ"
    Amenity.WIFI -> "📶"
}

@Composable
private fun RequiredFieldLabel(text: String) {
    val errorColor = MaterialTheme.colorScheme.error
    Text(buildAnnotatedString {
        append(text)
        withStyle(SpanStyle(color = errorColor)) { append(" *") }
    })
}

@Composable
@Suppress("FunctionNaming")
private fun AddressReadOnlyCard(address: String, helperText: String? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.address_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when {
                        address.isNotBlank() -> address
                        helperText != null -> helperText
                        else -> stringResource(R.string.address_auto_placeholder)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PhotosSection(
    existingPhotoUrls: List<String>,
    photoUris: List<Uri>,
    canAddMorePhotos: Boolean,
    isSaving: Boolean,
    placeHashesReady: Boolean,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveExisting: (Int) -> Unit,
    onRemoveNew: (Int) -> Unit
) {
    Text(
        text = stringResource(R.string.photos_with_count, photoUris.size + existingPhotoUrls.size),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )

    if (existingPhotoUrls.isNotEmpty() || photoUris.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(existingPhotoUrls) { index, url ->
                PhotoThumbnail(model = url, onRemove = { onRemoveExisting(index) }, enabled = !isSaving)
            }
            itemsIndexed(photoUris) { index, uri ->
                PhotoThumbnail(model = uri, onRemove = { onRemoveNew(index) }, enabled = !isSaving)
            }
        }
    }

    if (canAddMorePhotos) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPickFromGallery,
                enabled = !isSaving && placeHashesReady,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.gallery))
            }
            OutlinedButton(
                onClick = onTakePhoto,
                enabled = !isSaving && placeHashesReady,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.camera))
            }
        }
    }
}

@Composable
private fun NearbyPlacesList(
    places: List<AddPlaceViewModel.NearbyPlace>,
    onOpenPlace: (String) -> Unit
) {
    val visiblePlaces = places.take(NEARBY_VISIBLE_LIMIT)
    val hiddenCount = places.size - visiblePlaces.size

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.nearby_places_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            visiblePlaces.forEach { place ->
                val style = place.category.style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPlace(place.id) }
                        .padding(vertical = 4.dp),
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${place.distanceMeters}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (hiddenCount > 0) {
                Text(
                    text = "+$hiddenCount więcej w pobliżu",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
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
        icon = { Icon(imageVector = style.icon, contentDescription = null, tint = style.color) },
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
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.duplicate_warning_confirm)) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Suppress("FunctionNaming")
@Composable
private fun PhotoThumbnail(
    model: Any,
    onRemove: () -> Unit,
    enabled: Boolean = true
) {
    Box(modifier = Modifier.size(PHOTO_THUMBNAIL_SIZE)) {
        AsyncImage(
            model = model,
            contentDescription = stringResource(R.string.photo_thumbnail_description),
            modifier = Modifier.size(PHOTO_THUMBNAIL_SIZE).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        if (enabled) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd).size(PHOTO_REMOVE_BUTTON_SIZE).background(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.82f),
                    shape = CircleShape
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.remove_photo_description),
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(12.dp)
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
        while (inputStream.read(buffer).also { bytesRead = it } != -1) md.update(buffer, 0, bytesRead)
        inputStream.close()
        md.digest().joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        null
    }
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
        while (inputStream.read(buffer).also { bytesRead = it } != -1) md.update(buffer, 0, bytesRead)
        inputStream.close()
        md.digest().joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        null
    }
}
