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
import kotlinx.coroutines.launch

/**
 * Ekran dodawania nowego miejsca – formularz zapisywany do Firestore.
 *
 *  - nazwa, opis, kategoria, adres – pola tekstowe
 *  - GPS przez przycisk "Pobierz moją lokalizację" (uses FusedLocationClient,
 *    z permission requestem przy pierwszym użyciu)
 *  - udogodnienia jako FilterChips (multi-select)
 *  - przycisk "Zapisz miejsce" enabled tylko gdy formularz ważny
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

    // Po pomyślnym zapisie – wracamy poziom wyżej. W trybie create
    // dodatkowo przekazujemy współrzędne nowego pinu, żeby Main mógł
    // wycentrować na nim mapę.
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            haptic.success()
            // Trigger in-app review if threshold reached (3rd place added)
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

    // Komunikat o duplikatach zdjęć
    LaunchedEffect(state.photoDuplicateMessage) {
        val msg = state.photoDuplicateMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumePhotoDuplicateMessage()
        }
    }

    // Launcher prośby o uprawnienie lokalizacji.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            coroutineScope.launch { fetchAndSetLocation(context, viewModel) }
        } else {
            viewModel.onLocationError("Brak uprawnienia do lokalizacji")
        }
    }

    // Photo picker – max 5 zdjęć jednocześnie.
    var photoHashSet by remember { mutableStateOf(setOf<String>()) }
    var placeHashesReady by remember { mutableStateOf(!state.isEditMode) }

    // Seeduj hashe z istniejących remote URLs przy edycji miejsca
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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_PLACE_PHOTOS)
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Deduplikacja na bazie content hash
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
                    snackbarHostState.showSnackbar("To zdjęcie zostało już dodane. Nie można dodać duplikatu.")
                }
            }
        }
    }

    // Camera launcher
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
                    snackbarHostState.showSnackbar("To zdjęcie zostało już dodane. Nie można dodać duplikatu.")
                }
            }
        }
    }

    fun launchPlaceCamera() {
        val uri = createPlaceCameraUri(context)
        placeCameraUri.value = uri
        placeCameraLauncher.launch(uri)
    }

    val placeCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchPlaceCamera()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Brak dostępu do aparatu")
            }
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) "Edytuj miejsce"
                        else stringResource(R.string.add_place)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
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
            // --- Nazwa (wymagana) ---
            val nameHasError = state.hasTriedToSave && state.name.isBlank()
            // Title Case (KeyboardCapitalization.Words) - "Plac Zabaw Kasztanowa"
            // wygląda lepiej niż "plac zabaw kasztanowa". Klawiatura sama
            // zacznie każde słowo dużą literą; ostateczna normalizacja
            // (np. gdy user wpisze małą po autocorrect) zachodzi w VM
            // przy zapisie - patrz AddPlaceViewModel.save().
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { RequiredFieldLabel("Nazwa miejsca") },
                singleLine = true,
                supportingText = {
                    val requiredText = if (state.name.isBlank()) {
                        "Pole wymagane. "
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
                            error("Pole wymagane")
                        }
                    }
            )

            Spacer(Modifier.height(8.dp))

            // --- Opis ---
            // Sentences - duża litera tylko po kropce, jak w naturalnym
            // tekście opisowym ("Fajny park z kacikiem dla maluchow.").
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Opis") },
                minLines = 2,
                maxLines = 5,
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // --- Kategoria (dropdown) ---
            CategoryDropdown(
                selected = state.category,
                onSelected = viewModel::onCategoryChange,
                enabled = !state.isSaving
            )

            Spacer(Modifier.height(8.dp))

            Spacer(Modifier.height(12.dp))

            // --- Lokalizacja GPS (wymagana) – nad adresem, by po pobraniu
            // GPS adres mógł zostać wypełniony przez reverse geocoding ---
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

            // Info pod przyciskiem GPS
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (state.latitude == null || state.longitude == null) {
                    "Lokalizacja jest wymagana. Pobierz GPS w miejscu, które dodajesz."
                } else {
                    "Lokalizacja pobrana. Możesz zapisać miejsce."
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

            // --- Miejsca w pobliżu (ochrona przed duplikatami) ---
            if (state.nearbyPlaces.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                NearbyPlacesList(places = state.nearbyPlaces)
            }

            Spacer(Modifier.height(12.dp))

            // --- Adres (read-only, wypełniany przez reverse geocoding po
            // pobraniu lokalizacji GPS). Użytkownik nie może edytować ręcznie. ---
            OutlinedTextField(
                value = state.address,
                onValueChange = { /* read-only */ },
                label = { Text("Adres") },
                supportingText = {
                    Text("Uzupełnia się automatycznie po pobraniu lokalizacji")
                },
                singleLine = true,
                readOnly = true,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // --- Udogodnienia (FilterChips) – filtrowane po wybranej kategorii ---
            Text(
                text = "Udogodnienia",
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

            // --- Komunikat błędu ---
            state.errorMessage?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Assertive
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // --- Zdjęcia ---
            Text(
                text = "Zdjęcia (${state.photoUris.size + state.existingPhotoUrls.size}/$MAX_PLACE_PHOTOS)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            // Miniaturki istniejących zdjęć (edycja)
            if (state.existingPhotoUrls.isNotEmpty() || state.photoUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Istniejące (już uploadowane)
                    itemsIndexed(state.existingPhotoUrls) { index, url ->
                        PhotoThumbnail(
                            model = url,
                            onRemove = {
                                // Usuwamy hash żeby ponowne dodanie tego samego zdjęcia nie było blokowane
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
                    // Nowe (lokalne URI)
                    itemsIndexed(state.photoUris) { index, uri ->
                        PhotoThumbnail(
                            model = uri,
                            onRemove = {
                                // Usuwamy hash żeby ponowne dodanie tego samego zdjęcia nie było blokowane
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

            // Przyciski Galeria + Aparat
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
                        Text("Galeria")
                    }
                    OutlinedButton(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                launchPlaceCamera()
                            } else {
                                placeCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
                        Text("Aparat")
                    }
                }
            }

            if (state.isUploadingPhotos) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Przesyłanie zdjęć...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            // --- Przycisk zapisu ---
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
                        text = if (state.isEditMode) "Zaktualizuj miejsce"
                        else "Zapisz miejsce"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // --- Dialog ostrzeżenia o potencjalnym duplikacie ---
    val duplicateCandidate = state.duplicateCandidate
    if (state.showDuplicateWarning && duplicateCandidate != null) {
        DuplicateWarningDialog(
            candidate = duplicateCandidate,
            onConfirm = viewModel::confirmSaveDespiteDuplicate,
            onDismiss = viewModel::dismissDuplicateWarning
        )
    }
}

/**
 * Pobiera GPS przez [fetchCurrentLocation], a następnie best-effort
 * reverse-geocoduje współrzędne na adres przez [reverseGeocode].
 * Wszystko propaguje jednym wołaniem do ViewModelu, dzięki czemu spinner
 * znika jednorazowo (a nie miga między fazami).
 */
private suspend fun fetchAndSetLocation(
    context: android.content.Context,
    viewModel: AddPlaceViewModel
) {
    viewModel.onFetchingLocationStart()

    // Sprawdź najpierw czy usługa lokalizacji jest w ogóle włączona
    if (!isLocationServiceEnabled(context)) {
        viewModel.onLocationError(LOCATION_SERVICE_DISABLED_MESSAGE)
        return
    }

    try {
        val coords = fetchCurrentLocation(context)
        if (coords == null) {
            viewModel.onLocationError(LOCATION_TIMEOUT_USER_MESSAGE)
            return
        }
        val address = runCatching { reverseGeocode(context, coords.first, coords.second) }
            .getOrNull()
        viewModel.onLocationFetched(coords.first, coords.second, address)
    } catch (e: Exception) {
        viewModel.onLocationError(LOCATION_TIMEOUT_USER_MESSAGE)
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
            label = { Text("Kategoria") },
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
                        "Aktualizuj lokalizację"
                    } else {
                        "Pobierz moją lokalizację *"
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

/**
 * Siatka FilterChip-ów do multi-select udogodnień, filtrowana po [category].
 *
 * Sortowanie chipów:
 *  1. najczęściej używane na początku (wg [amenityFrequency] - mapa
 *     liczby miejsc, w których dane udogodnienie jest zaznaczone),
 *  2. tiebreak: kolejność z enuma (tj. logiczne grupowanie z [Amenity]).
 *
 * Gdy mapa jest pusta (świeży start, brak miejsc w bazie, błąd fetcha) -
 * spadamy na kolejność z enuma. Dzięki temu ekran nie czeka na asynchroniczny
 * count, tylko płynnie przechodzi z "logicznej" kolejności do
 * "od najczęstszego" gdy frequency dotrze.
 */
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

/** Tekst etykiety + czerwona gwiazdka, do wymaganych pól. */
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


/**
 * Mini-lista istniejących miejsc w pobliżu, wyświetlana po pobraniu GPS.
 * Pomaga użytkownikowi zauważyć, że podobne miejsce już istnieje.
 */
@Composable
private fun NearbyPlacesList(places: List<AddPlaceViewModel.NearbyPlace>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Miejsca w pobli\u017Cu:",
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
                        contentDescription = "Kategoria",
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

/**
 * Dialog ostrzegawczy wyświetlany gdy w promieniu 100m od pobranej lokalizacji
 * istnieje już miejsce tej samej kategorii (potencjalny duplikat).
 */
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
                contentDescription = "Ikona kategorii",
                tint = style.color
            )
        },
        title = { Text("Potencjalny duplikat") },
        text = {
            Text(
                text = "W pobli\u017Cu (~${candidate.distanceMeters}m) istnieje ju\u017C miejsce " +
                    "\u201E${candidate.name}\u201D (${stringResource(candidate.category.labelRes)}). " +
                    "Czy na pewno chcesz doda\u0107 nowe?"
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Dodaj mimo to")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}


/**
 * Miniaturka zdjęcia z przyciskiem "X" do usunięcia.
 * Akceptuje zarówno [android.net.Uri] (nowe) jak i [String] URL (istniejące).
 */
@Composable
private fun PhotoThumbnail(
    model: Any, // Uri lub String URL
    onRemove: () -> Unit,
    enabled: Boolean = true
) {
    Box(modifier = Modifier.size(80.dp)) {
        AsyncImage(
            model = model,
            contentDescription = "Miniatura zdjęcia",
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
                    contentDescription = "Usuń zdjęcie",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}


/**
 * Oblicza MD5 hash zawartości URI do detekcji duplikatów zdjęć.
 */
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

/**
 * Tworzy tymczasowy plik dla zdjęcia z aparatu i zwraca content URI.
 */
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


/**
 * Pobiera zdjęcie z remote URL i oblicza MD5 hash.
 * Używane do seedowania hashów istniejących zdjęć przy edycji miejsca.
 * Wywołuj na Dispatchers.IO.
 */
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
