package com.kidzone.presentation.place.add

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kidzone.R
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.presentation.common.style
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

    // Po pomyślnym zapisie – wracamy poziom wyżej. W trybie create
    // dodatkowo przekazujemy współrzędne nowego pinu, żeby Main mógł
    // wycentrować na nim mapę.
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved(state.savedNewLatitude, state.savedNewLongitude)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
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
                isError = state.name.isNotEmpty() && state.name.isBlank(),
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
                modifier = Modifier.fillMaxWidth()
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

            Spacer(Modifier.height(12.dp))

            // --- Adres (wymagany) – po GPS, żeby reverse geocoding mógł go
            // wypełnić, ale nadal w pełni edytowalny przez użytkownika.
            // Words = duża litera na początku każdego słowa ("Aleje
            // Ujazdowskie 4, Warszawa") - zgodnie z polską konwencją
            // adresową. ---
            OutlinedTextField(
                value = state.address,
                onValueChange = viewModel::onAddressChange,
                label = { RequiredFieldLabel("Adres") },
                singleLine = true,
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
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
                onToggle = viewModel::toggleAmenity,
                enabled = !state.isSaving
            )

            // --- Komunikat błędu ---
            state.errorMessage?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
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
    try {
        val coords = fetchCurrentLocation(context)
        if (coords == null) {
            // null = brak fixu albo timeout. Spójny komunikat dla obu
            // przypadków (zob. LOCATION_TIMEOUT_USER_MESSAGE) - user nie
            // potrzebuje wiedzieć, czy GPS się spóźnił, czy w ogóle nie
            // ma sygnału, w obu sytuacjach robi się to samo (próbuje
            // później albo wpisuje adres ręcznie).
            viewModel.onLocationError(LOCATION_TIMEOUT_USER_MESSAGE)
            return
        }
        // Reverse geocoding jest best-effort – jego błędy nie blokują flow.
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
                .menuAnchor()
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

/** Siatka FilterChip-ów do multi-select udogodnień, filtrowana po [category]. */
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
