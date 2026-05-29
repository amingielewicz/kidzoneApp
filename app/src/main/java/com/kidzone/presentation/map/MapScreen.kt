package com.kidzone.presentation.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.presentation.common.style
import com.kidzone.presentation.place.add.fetchCurrentLocation
import com.kidzone.presentation.place.add.hasLocationPermission

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
 * Zoom kamery po dodaniu nowego miejsca – bliżej niż "Blisko mnie", żeby
 * świeży pin był wyraźnie widoczny pośrodku ekranu z otoczeniem ulicznym.
 */
private const val FOCUS_PLACE_ZOOM = 16f

/**
 * Ekran mapy z pinezkami miejsc.
 *
 *  - Pinezki to [MarkerComposable] z maps-compose – każdy marker renderuje
 *    okrągłą plakietkę w kolorze kategorii z ikonką tej kategorii w środku
 *    (spójne z [com.kidzone.presentation.common.CategoryStyle], czyli
 *    tym co user widzi na chipach / kartach miejsc).
 *  - Filtry na overlayu nad mapą: kategoria + przełącznik "Najlepiej oceniane".
 *  - Natywne kontrolki Maps SDK: przycisk "Moja lokalizacja" (top-right) i
 *    zoom +/- (bottom-right) – żeby mapa wyglądała "po Google'owemu".
 *    Kontrolki są przesunięte przez `contentPadding`, żeby nie wpadały pod
 *    globalny `+` FAB z [com.kidzone.presentation.main.MainScreen].
 *  - Permission ACCESS_FINE_LOCATION jest proszona automatycznie przy
 *    pierwszym wejściu na ekran ([LaunchedEffect]) – natywny crosshair
 *    pokaże się dopiero gdy `isMyLocationEnabled == true`.
 *  - Klik pinezki otwiera [ModalBottomSheet] z miniaturą + nazwą + adresem
 *    + przyciskiem "Zobacz szczegóły".
 *
 * Adnotacja [SuppressLint] – Lint nie potrafi prześledzić, że
 * `MapProperties.isMyLocationEnabled = locationPermissionGranted` jest
 * ustawiane tylko gdy uprawnienie faktycznie zostało nadane (sprawdzamy
 * w runtime). [MapsComposeExperimentalApi] – wymagane przez
 * `MarkerComposable`, oficjalna ścieżka renderowania custom contentu jako
 * pinezki.
 */
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, MapsComposeExperimentalApi::class)
@Composable
fun MapScreen(
    onOpenPlaceDetails: (placeId: String) -> Unit,
    focusOn: LatLng? = null,
    onFocusConsumed: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Trzymamy lokalnie, bo musimy reagować na nadanie uprawnienia bez
    // restartu ekranu. Wartość początkowa = stan systemowy w chwili pierwszej
    // kompozycji (gdy user już raz zezwolił, native crosshair od razu jest
    // widoczny bez dodatkowego dialogu).
    var locationPermissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_CAMERA_TARGET, DEFAULT_CAMERA_ZOOM)
    }

    // Po pomyślnym `addPlace` parent przekazuje współrzędne nowego miejsca
    // przez [focusOn] – animujemy kamerę na ten punkt na poziomie
    // [FOCUS_PLACE_ZOOM] (bliżej niż domyślny widok miasta, żeby nowy pin
    // był wyraźnie widoczny). Po skończonej animacji konsumujemy sygnał,
    // żeby przy zmianie konfiguracji / rekompozycji nie nawigować ponownie.
    LaunchedEffect(focusOn) {
        focusOn?.let { target ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(target, FOCUS_PLACE_ZOOM)
            )
            onFocusConsumed()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationPermissionGranted = granted
        if (granted) {
            // Nadanie uprawnienia = jasny sygnał "chcę się znaleźć", więc
            // sami centrujemy kamerę. Native crosshair user może później
            // używać do "wróć do mnie" po przewinięciu mapy.
            scope.launch { recenterOnUser(context, cameraPositionState) }
        }
    }

    // Auto-prośba o uprawnienie tylko jeśli go jeszcze nie mamy. Wchodząc
    // na zakładkę "Mapa" user wyraża jasną intencję chęci zobaczenia siebie
    // na mapie – timing dialogu jest naturalny. Jeśli wcześniej trwale
    // odmówił, system po cichu zwróci `granted=false` bez UI.
    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Gdy user wraca z systemowych ustawień appki (gdzie ręcznie nadał lub
    // odebrał uprawnienie), Activity wraca w stan RESUMED. Wtedy odświeżamy
    // `locationPermissionGranted` z systemu, żeby banner automatycznie
    // zniknął bez potrzeby restartu zakładki Mapa.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                locationPermissionGranted = hasLocationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                // Natywny przycisk lokalizacji w prawym górnym rogu mapy –
                // pojawia się dopiero gdy isMyLocationEnabled == true (czyli
                // gdy permission jest granted, patrz LaunchedEffect powyżej).
                myLocationButtonEnabled = true,
                // Natywne +/- w prawym dolnym rogu mapy. contentPadding
                // poniżej przesuwa je tak, by nie kolidowały z `+` FAB-em
                // z MainScreena (też BottomEnd).
                zoomControlsEnabled = true,
                mapToolbarEnabled = false,
                compassEnabled = true
            ),
            // Native zoom controls + atrybucja Google'a domyślnie siedzą w
            // prawym dolnym rogu canvasu mapy, czyli pod globalnym FAB-em
            // "+" z MainScreena. Przesuwamy je o ok. wysokość FAB-a +
            // bottom navigation, żeby były dostępne palcem.
            //
            // 96.dp ≈ 56 (FAB) + 16 (margin Scaffolda wokół FAB) + 24 (luz
            // wizualny + bottom nav). Dobierane na oko, łatwo skorygować.
            contentPadding = PaddingValues(bottom = 96.dp),
            // Tap w pustą część mapy = zamykamy bottom sheet (jeśli otwarty).
            onMapClick = { viewModel.onPlaceSelected(null) }
        ) {
            state.places.forEach { place ->
                MarkerComposable(
                    keys = arrayOf(place.id, place.category),
                    state = MarkerState(LatLng(place.latitude, place.longitude)),
                    title = place.name,
                    snippet = place.address.takeIf { it.isNotBlank() },
                    // true = consume zdarzenie. Domyślny info-window ma
                    // uboższe info niż nasz sheet, więc nadpisujemy własnym.
                    onClick = {
                        viewModel.onPlaceSelected(place.id)
                        true
                    }
                ) {
                    CategoryMarkerIcon(category = place.category)
                }
            }
        }

        // --- Overlay z filtrami + (opcjonalnie) banner permission u góry ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FiltersOverlay(
                selectedCategory = state.selectedCategory,
                topRatedOnly = state.topRatedOnly,
                onCategorySelected = viewModel::onCategorySelected,
                onToggleTopRated = viewModel::toggleTopRated,
                modifier = Modifier.fillMaxWidth()
            )
            if (!locationPermissionGranted) {
                LocationPermissionBanner(
                    onAllowClick = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    onOpenSettingsClick = {
                        // Fallback dla "permanently denied" – w tym stanie launcher.launch()
                        // nic nie zrobi (callback wraca z false bez UI). Przerzucamy usera
                        // do systemowych Ustawień appki, gdzie zawsze może włączyć Location.
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
                    // Bottom padding > FAB + bottom nav, żeby błąd nie
                    // chował się pod kontrolkami.
                    .padding(start = 16.dp, end = 88.dp, bottom = 96.dp),
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
 * Pojedyncza pinezka renderowana jako [MarkerComposable] – kółko w kolorze
 * kategorii z białą ikoną tej kategorii w środku, plus białe obramowanie
 * dla kontrastu na różnych tłach mapy.
 *
 * MarkerComposable zamienia ten Composable na bitmapę przy starcie i
 * dalej traktuje ją tak jak zwykłą pinezkę Maps SDK, więc nie ma kosztu
 * recompose przy każdym przesunięciu kamery.
 */
@Composable
private fun CategoryMarkerIcon(category: PlaceCategory) {
    val style = category.style
    Surface(
        shape = CircleShape,
        color = style.color,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
        shadowElevation = 4.dp
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            // Tinta na kolor surface (zwykle biały / prawie biały) – ikona
            // wyraźnie odcina się na kolorowym tle plakietki.
            tint = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .size(36.dp)
                .padding(6.dp)
        )
    }
}

/**
 * Banner widoczny gdy user nie ma jeszcze nadanego uprawnienia
 * [Manifest.permission.ACCESS_FINE_LOCATION] – tłumaczy dlaczego niebieska
 * kropka "gdzie jestem" się nie pojawia, i daje dwie ścieżki naprawy:
 *
 *  - **"Pozwól"** – ponawia systemowy dialog uprawnień. Działa, gdy user
 *    odmówił raz (Don't allow). Jeśli wybrał "Don't ask again" /
 *    "permanently denied", dialog się nie pokaże – wtedy zostaje przycisk
 *    "Ustawienia".
 *  - **"Ustawienia"** – otwiera stronę ustawień appki w systemie, gdzie
 *    user zawsze może ręcznie włączyć Location. Po powrocie banner
 *    znika automatycznie dzięki `DisposableEffect` na ON_RESUME w callsite.
 */
@Composable
private fun LocationPermissionBanner(
    onAllowClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Włącz lokalizację, żeby zobaczyć siebie na mapie",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onOpenSettingsClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ustawienia")
                }
                Button(
                    onClick = onAllowClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pozwól")
                }
            }
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
    // Kolejność jak w enum PlaceCategory (świadomie nie alfabetycznie –
    // logiczne grupowanie: place zabaw → sale → kawiarnia/restauracja →
    // park → atrakcje → inne). Spójnie z PlaceListScreen.
    val orderedCategories = PlaceCategory.entries

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
