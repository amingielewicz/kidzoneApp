package com.playground.presentation.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.playground.domain.model.Place
import com.playground.domain.model.PlaceCategory
import com.playground.presentation.common.style
import com.playground.presentation.place.add.fetchCurrentLocation
import com.playground.presentation.place.add.hasLocationPermission

/**
 * Domyślny target kamery – Warszawa, Pl. Defilad. Zoom 11 daje pełny widok
 * miasta, więc gdy mamy parę miejsc dodanych w okolicy stolicy, user widzi
 * je od razu bez ręcznego odsuwania.
 *
 * W przyszłości można fitować kamerę do bounding boxu wszystkich pinezek
 * (`LatLngBounds.Builder` + `CameraUpdateFactory.newLatLngBounds(...)`),
 * ale tylko gdy `places` jest niepuste i nie zacieni to UX dla pierwszego
 * uruchomienia, gdy lista jest pusta.
 */
private val DEFAULT_CAMERA_TARGET = LatLng(52.2297, 21.0122)
private const val DEFAULT_CAMERA_ZOOM = 11f

/** Zoom kamery po wciśnięciu "Blisko mnie" – poziom dzielnicy. */
private const val NEAR_ME_ZOOM = 14f

/**
 * Ekran mapy z pinezkami miejsc.
 *
 *  - Markery generowane z [com.playground.domain.model.Place], kolor pinezki
 *    zgodny z [com.playground.presentation.common.CategoryStyle] (poprzez
 *    konwersję RGB → HSV hue dla `BitmapDescriptorFactory.defaultMarker`).
 *  - Filtry na overlayu nad mapą: kategoria + przełącznik "Najlepiej oceniane".
 *  - FAB "Blisko mnie" – pyta o uprawnienie lokalizacji przy pierwszym użyciu,
 *    a po jego nadaniu animuje kamerę do bieżącej pozycji.
 *  - Klik pinezki otwiera [ModalBottomSheet] z miniaturą + nazwą + adresem
 *    + przyciskiem "Zobacz szczegóły".
 *
 * Adnotacja [SuppressLint] – Lint nie potrafi prześledzić, że
 * `MapProperties.isMyLocationEnabled = locationPermissionGranted` jest
 * ustawiane tylko gdy uprawnienie faktycznie zostało nadane (sprawdzamy
 * w runtime).
 */
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onOpenPlaceDetails: (placeId: String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Trzymamy lokalnie, bo musimy reagować na nadanie uprawnienia bez
    // restartu ekranu. Wartość początkowa = stan systemowy w chwili pierwszej
    // kompozycji (gdy user już raz zezwolił, FAB od razu działa bez dialogu).
    var locationPermissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_CAMERA_TARGET, DEFAULT_CAMERA_ZOOM)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationPermissionGranted = granted
        if (granted) {
            // User właśnie nadał uprawnienie z intencją "pokaż mnie", więc
            // od razu centrujemy kamerę – inaczej musiałby kliknąć FAB drugi raz.
            scope.launch { recenterOnUser(context, cameraPositionState) }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedPlace = state.places.firstOrNull { it.id == state.selectedPlaceId }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = locationPermissionGranted
            ),
            uiSettings = MapUiSettings(
                // Wbudowany przycisk "my location" SDK byłby duplikatem naszego
                // FAB – wyłączamy go, żeby nie nakładał się na overlay filtrów.
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                compassEnabled = true
            ),
            // Tap w pustą część mapy = zamykamy bottom sheet (jeśli otwarty).
            onMapClick = { viewModel.onPlaceSelected(null) }
        ) {
            state.places.forEach { place ->
                Marker(
                    state = MarkerState(LatLng(place.latitude, place.longitude)),
                    title = place.name,
                    snippet = place.address.takeIf { it.isNotBlank() },
                    icon = BitmapDescriptorFactory.defaultMarker(place.category.toMarkerHue()),
                    // true = consume zdarzenie. Domyślny info-window ma uboższe
                    // info niż nasz sheet, więc nadpisujemy zachowanie własnym.
                    onClick = {
                        viewModel.onPlaceSelected(place.id)
                        true
                    }
                )
            }
        }

        // --- Overlay z filtrami u góry ---
        FiltersOverlay(
            selectedCategory = state.selectedCategory,
            topRatedOnly = state.topRatedOnly,
            onCategorySelected = viewModel::onCategorySelected,
            onToggleTopRated = viewModel::toggleTopRated,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(8.dp)
        )

        // --- FAB "Blisko mnie" w prawym dolnym rogu ---
        FloatingActionButton(
            onClick = {
                if (locationPermissionGranted) {
                    scope.launch { recenterOnUser(context, cameraPositionState) }
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Blisko mnie")
        }

        // --- Stany pomocnicze: spinner przy pierwszym ładowaniu i błąd ---
        if (state.isLoading && state.places.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
        state.errorMessage?.let { msg ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Nie chcemy zasłaniać FAB-a, więc bottom padding > FAB.
                    .padding(start = 16.dp, end = 88.dp, bottom = 16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 4.dp
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }

    // --- Bottom sheet z podglądem klikniętej pinezki ---
    if (selectedPlace != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onPlaceSelected(null) },
            sheetState = sheetState
        ) {
            PlacePreviewContent(
                place = selectedPlace,
                onOpenDetails = {
                    onOpenPlaceDetails(selectedPlace.id)
                    // Zamykamy zaznaczenie tu, żeby po powrocie z details
                    // sheet nie wyświetlił się ponownie (bo selectedPlaceId
                    // jest trzymany w VM i przeżyje config change).
                    viewModel.onPlaceSelected(null)
                }
            )
        }
    }
}

/**
 * Pasek z chipami filtrów nad mapą – wystylowany jako lekko podniesiona
 * powierzchnia, żeby był czytelny zarówno na jasnym, jak i ciemnym tle mapy.
 */
@Composable
private fun FiltersOverlay(
    selectedCategory: PlaceCategory?,
    topRatedOnly: Boolean,
    onCategorySelected: (PlaceCategory?) -> Unit,
    onToggleTopRated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Kategorie alfabetycznie po polskim labelu (Context-bound, więc remember
    // z kluczem context). Spójnie z PlaceListScreen.
    val orderedCategories = remember(context) {
        PlaceCategory.entries.sortedBy { context.getString(it.labelRes).lowercase() }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            // Rząd 1: kategoria (single-select, "Wszystkie" na początku jako reset).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("Wszystkie") }
                )
                orderedCategories.forEach { category ->
                    val style = category.style
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelected(category) },
                        leadingIcon = {
                            Icon(
                                imageVector = style.icon,
                                contentDescription = null,
                                tint = style.color
                            )
                        },
                        label = { Text(stringResource(category.labelRes)) }
                    )
                }
            }

            // Rząd 2: dodatkowe toggle (na razie tylko "najlepiej oceniane").
            // "Darmowe" wymagałoby pola w `Place` – patrz docs MapViewModel.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = topRatedOnly,
                    onClick = onToggleTopRated,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (topRatedOnly) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = { Text("Najlepiej oceniane") }
                )
            }
        }
    }
}

/**
 * Treść `ModalBottomSheet` – wystarczająco bogata, żeby user mógł zdecydować
 * "klikam dalej czy nie", ale na tyle zwięzła, żeby nie konkurować z pełnym
 * ekranem szczegółów.
 *
 * Zawiera:
 *  - miniaturkę (pierwsze zdjęcie z `photoUrls`, przez Coil) – jeżeli jest,
 *  - nazwę + chip kategorii + ocenę,
 *  - adres,
 *  - CTA "Zobacz szczegóły".
 */
@Composable
private fun PlacePreviewContent(
    place: Place,
    onOpenDetails: () -> Unit
) {
    val style = place.category.style

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        // Miniaturka pojawia się tylko wtedy, gdy faktycznie jest – inaczej
        // sheet się "kurczy" do samego tekstu i nie ma pustego prostokąta.
        if (place.photoUrls.isNotEmpty()) {
            AsyncImage(
                model = place.photoUrls.first(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Spacer(Modifier.height(12.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(place.category.labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (place.reviewsCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "%.1f".format(place.averageRating),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (place.address.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = place.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onOpenDetails,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zobacz szczegóły")
        }
    }
}

/**
 * Konwertuje kolor kategorii (Compose `Color`, RGB) na hue HSV (0..360),
 * którego oczekuje [BitmapDescriptorFactory.defaultMarker]. Dzięki temu
 * pinezki na mapie mają spójną tożsamość wizualną z chipami / kartami,
 * bez konieczności rysowania własnych assetów PNG.
 */
private fun PlaceCategory.toMarkerHue(): Float {
    val color: Color = this.style.color
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
        hsv
    )
    return hsv[0]
}

/**
 * Centruje kamerę na bieżącej lokalizacji użytkownika z [NEAR_ME_ZOOM].
 *
 * Best-effort – jeżeli urządzenie nie ma fixu (np. emulator bez ustawionej
 * lokalizacji), nie robimy nic. Świadomie nie pokazujemy w tym miejscu
 * Toastu, żeby nie blokować threadu UI; user widzi po prostu, że nic się
 * nie zmieniło – w kolejnej iteracji można dorzucić Snackbar.
 *
 * Wymaga [Manifest.permission.ACCESS_FINE_LOCATION] – callsite musi to
 * zweryfikować przez [hasLocationPermission].
 */
private suspend fun recenterOnUser(
    context: Context,
    cameraPositionState: CameraPositionState
) {
    val coords = runCatching { fetchCurrentLocation(context) }.getOrNull() ?: return
    cameraPositionState.animate(
        CameraUpdateFactory.newLatLngZoom(
            LatLng(coords.first, coords.second),
            NEAR_ME_ZOOM
        )
    )
}
