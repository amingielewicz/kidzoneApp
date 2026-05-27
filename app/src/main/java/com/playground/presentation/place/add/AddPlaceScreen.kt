package com.playground.presentation.place.add

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.playground.R
import com.playground.domain.model.Amenity
import com.playground.domain.model.PlaceCategory
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
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddPlaceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Po pomyślnym zapisie – wracamy poziom wyżej.
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
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
                title = { Text(stringResource(R.string.add_place)) },
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
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { RequiredFieldLabel("Nazwa miejsca") },
                singleLine = true,
                isError = state.name.isNotEmpty() && state.name.isBlank(),
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // --- Opis ---
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Opis") },
                minLines = 2,
                maxLines = 5,
                enabled = !state.isSaving,
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

            // --- Adres (wymagany) ---
            OutlinedTextField(
                value = state.address,
                onValueChange = viewModel::onAddressChange,
                label = { RequiredFieldLabel("Adres") },
                singleLine = true,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // --- Lokalizacja GPS (wymagana) ---
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

            Spacer(Modifier.height(16.dp))

            // --- Udogodnienia (FilterChips) ---
            Text(
                text = "Udogodnienia",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            AmenitiesGrid(
                selected = state.amenities,
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
                enabled = !state.isSaving && state.isFormValid,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Zapisz miejsce")
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
            viewModel.onLocationError("Brak fixu GPS – sprawdź, czy lokalizacja jest włączona")
            return
        }
        // Reverse geocoding jest best-effort – jego błędy nie blokują flow.
        val address = runCatching { reverseGeocode(context, coords.first, coords.second) }
            .getOrNull()
        viewModel.onLocationFetched(coords.first, coords.second, address)
    } catch (e: Exception) {
        viewModel.onLocationError(e.message ?: "Błąd pobierania lokalizacji")
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

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = stringResource(selected.labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text("Kategoria") },
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
                DropdownMenuItem(
                    text = { Text(stringResource(category.labelRes)) },
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

/** Siatka FilterChip-ów do multi-select udogodnień. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmenitiesGrid(
    selected: Set<Amenity>,
    onToggle: (Amenity) -> Unit,
    enabled: Boolean
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Amenity.entries.forEach { amenity ->
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
