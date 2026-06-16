package com.kidzone.presentation.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.kidzone.R
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.model.GeoBounds
import com.kidzone.presentation.common.CategoryIcon
import com.kidzone.presentation.common.GpsDisabledBanner
import com.kidzone.presentation.common.rememberLocationServiceEnabled
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
private const val MAX_SPIDERFIED_CLUSTER_SIZE = 10
private const val SPIDERFY_MIN_ZOOM = 13f
private const val CLUSTER_FIT_BOUNDS_PADDING_PX = 96

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
@OptIn(ExperimentalMaterial3Api::class, MapsComposeExperimentalApi::class, ExperimentalSharedTransitionApi::class)
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
    val gpsEnabled = rememberLocationServiceEnabled()

    // Trzymamy lokalnie, bo musimy reagować na nadanie uprawnienia bez
    // restartu ekranu. Wartość początkowa = stan systemowy w chwili pierwszej
    // kompozycji (gdy user już raz zezwolił, native crosshair od razu jest
    // widoczny bez dodatkowego dialogu).
    var locationPermissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_CAMERA_TARGET, DEFAULT_CAMERA_ZOOM)
    }
    var mapLoaded by remember { mutableStateOf(false) }
    var expandedClusterKey by remember { mutableStateOf<String?>(null) }
    var expandedClusterPlaceIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var userTouchedMap by remember { mutableStateOf(false) }
    val currentZoom = cameraPositionState.position.zoom
    val markerItems = remember(state.places, expandedClusterKey, expandedClusterPlaceIds, currentZoom) {
        buildMapMarkerItems(
            places = state.places,
            expandedClusterKey = expandedClusterKey,
            expandedPlaceIds = expandedClusterPlaceIds,
            zoom = currentZoom
        )
    }

    ReportSettledViewport(
        cameraPositionState = cameraPositionState,
        mapLoaded = mapLoaded,
        onViewportChanged = viewModel::onViewportChanged
    )

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
            scope.launch {
                recenterOnUser(
                    context = context,
                    cameraPositionState = cameraPositionState,
                    shouldAnimate = { !userTouchedMap }
                )
            }
        }
    }

    // Auto-prośba o uprawnienie tylko jeśli go jeszcze nie mamy. Wchodząc
    // na zakładkę "Mapa" user wyraża jasną intencję chęci zobaczenia siebie
    // na mapie – timing dialogu jest naturalny. Jeśli wcześniej trwale
    // odmówił, system po cichu zwróci `granted=false` bez UI.
    //
    // Jeśli uprawnienie JUŻ jest – od razu centrujemy kamerę na bieżącej
    // lokalizacji, żeby user widział najbliższe miejsca bez ręcznego
    // klikania natywnego "Moja lokalizacja". Pomijamy to gdy nadszedł
    // sygnał `focusOn` (przyszliśmy tu z "właśnie dodałem miejsce") –
    // tam kamera ma jechać na nowy pin, nie na usera.
    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else if (focusOn == null) {
            recenterOnUser(
                context = context,
                cameraPositionState = cameraPositionState,
                shouldAnimate = { !userTouchedMap }
            )
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
                // Wyłączamy natywny przycisk lokalizacji – zamiast niego
                // wyświetlamy własny Compose FAB (`MapMyLocationButton`) w
                // prawym górnym rogu, co daje pełną kontrolę nad stylem i
                // zachowaniem (np. wyzwalanie permission launchera).
                myLocationButtonEnabled = false,
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
            // 120.dp ≈ 56 (FAB) + 16 (margin Scaffolda wokół FAB) + 24 (luz
            // wizualny) + 24 (dodatkowy buffer, żeby zoom buttons nie były
            // zbyt blisko FAB-a). Dobierane na oko, łatwo skorygować.
            contentPadding = PaddingValues(bottom = 120.dp),
            onMapLoaded = { mapLoaded = true },
            // Tap w pustą część mapy = zamykamy bottom sheet (jeśli otwarty).
            onMapClick = {
                userTouchedMap = true
                expandedClusterKey = null
                expandedClusterPlaceIds = emptySet()
                viewModel.onPlaceSelected(null)
            }
        ) {
            markerItems.forEach { marker ->
                val markerKeys: Array<Any> = arrayOf(
                    marker.key,
                    marker.place?.category?.name.orEmpty(),
                    marker.cluster?.places?.size ?: 0
                )
                MarkerComposable(
                    keys = markerKeys,
                    state = MarkerState(marker.position),
                    title = marker.place?.name ?: "${marker.cluster?.places?.size.orZero()} miejsc",
                    snippet = marker.place?.address?.takeIf { it.isNotBlank() },
                    // true = consume zdarzenie. Domyślny info-window ma
                    // uboższe info niż nasz sheet, więc nadpisujemy własnym.
                    onClick = {
                        userTouchedMap = true
                        marker.cluster?.let { cluster ->
                            val shouldZoomIntoCluster =
                                currentZoom < SPIDERFY_MIN_ZOOM ||
                                    cluster.places.size > MAX_SPIDERFIED_CLUSTER_SIZE
                            if (shouldZoomIntoCluster) {
                                expandedClusterKey = null
                                expandedClusterPlaceIds = if (
                                    cluster.places.size <= MAX_SPIDERFIED_CLUSTER_SIZE
                                ) {
                                    cluster.places.mapTo(mutableSetOf()) { place -> place.id }
                                } else {
                                    emptySet()
                                }
                                scope.launch {
                                    cameraPositionState.animate(
                                        cameraUpdateForCluster(cluster)
                                    )
                                }
                            } else {
                                expandedClusterPlaceIds = emptySet()
                                expandedClusterKey = cluster.key
                            }
                            viewModel.onPlaceSelected(null)
                        }
                        marker.place?.let { place ->
                            if (marker.isSpiderfied) {
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(place.latitude, place.longitude),
                                            FOCUS_PLACE_ZOOM
                                        )
                                    )
                                }
                            }
                            viewModel.onPlaceSelected(place.id)
                        }
                        true
                    }
                ) {
                    if (marker.cluster != null) {
                        ClusterMarkerIcon(count = marker.cluster.places.size)
                    } else {
                        CategoryMarkerIcon(category = marker.place!!.category)
                    }
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
            if (locationPermissionGranted && !gpsEnabled) {
                GpsDisabledBanner(modifier = Modifier.fillMaxWidth())
            }
            FiltersOverlay(
                selectedCategory = state.selectedCategory,
                topRatedOnly = state.topRatedOnly,
                addedByMeOnly = state.addedByMeOnly,
                showAddedByMeChip = state.currentUserId != null,
                onCategorySelected = viewModel::onCategorySelected,
                onToggleTopRated = viewModel::toggleTopRated,
                onToggleAddedByMe = viewModel::toggleAddedByMe,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // --- Stany pomocnicze: spinner przy pierwszym ładowaniu i błąd ---
        if (state.isLoading && state.places.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // --- Custom "Moja lokalizacja" FAB ---
        // Zastępuje natywny przycisk Maps SDK, żeby mieć pełną kontrolę
        // nad wyglądem, pozycjonowaniem i zachowaniem (np. automatyczne
        // wyzwalanie permission launchera gdy uprawnienie nie jest nadane).
        MapMyLocationButton(
            onClick = {
                if (locationPermissionGranted) {
                    userTouchedMap = false
                    scope.launch { recenterOnUser(context, cameraPositionState) }
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                // Offset w dół, żeby nie kolidować z filtrami overlay
                .offset(y = 160.dp)
        )
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

private fun Int?.orZero(): Int = this ?: 0

private fun cameraUpdateForCluster(cluster: MarkerCluster) =
    if (cluster.places.hasSameCoordinates()) {
        CameraUpdateFactory.newLatLngZoom(cluster.center, FOCUS_PLACE_ZOOM)
    } else {
        CameraUpdateFactory.newLatLngBounds(
            LatLngBounds.builder().apply {
                cluster.places.forEach { place ->
                    include(LatLng(place.latitude, place.longitude))
                }
            }.build(),
            CLUSTER_FIT_BOUNDS_PADDING_PX
        )
    }

private fun List<Place>.hasSameCoordinates(): Boolean {
    val first = firstOrNull() ?: return true
    return all { place ->
        place.latitude == first.latitude && place.longitude == first.longitude
    }
}

@Composable
private fun ReportSettledViewport(
    cameraPositionState: CameraPositionState,
    mapLoaded: Boolean,
    onViewportChanged: (GeoBounds) -> Unit
) {
    LaunchedEffect(cameraPositionState, mapLoaded) {
        if (!mapLoaded) return@LaunchedEffect
        snapshotFlow { cameraPositionState.isMoving to cameraPositionState.position }
            .filter { (isMoving, _) -> !isMoving }
            .mapNotNull {
                cameraPositionState.projection?.visibleRegion?.latLngBounds
            }
            .collect { bounds ->
                onViewportChanged(
                    GeoBounds(
                        north = bounds.northeast.latitude,
                        east = bounds.northeast.longitude,
                        south = bounds.southwest.latitude,
                        west = bounds.southwest.longitude
                    )
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
            contentDescription = "Kategoria miejsca",
            // Tinta na kolor surface (zwykle biały / prawie biały) – ikona
            // wyraźnie odcina się na kolorowym tle plakietki.
            tint = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .size(36.dp)
                .padding(6.dp)
        )
    }
}

@Composable
private fun ClusterMarkerIcon(count: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count > MAX_SPIDERFIED_CLUSTER_SIZE) {
                    "$MAX_SPIDERFIED_CLUSTER_SIZE+"
                } else {
                    count.toString()
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
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
                    contentDescription = "Lokalizacja",
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
 *
 * Drugi rząd zawiera dodatkowe filtry boolean:
 *  - "Najlepiej oceniane" – zawsze widoczny,
 *  - "Dodane przez Ciebie" – tylko gdy [showAddedByMeChip] = true (czyli
 *    user jest zalogowany; dla wylogowanego chip nie ma sensu).
 */
@Composable
private fun FiltersOverlay(
    selectedCategory: PlaceCategory?,
    topRatedOnly: Boolean,
    addedByMeOnly: Boolean,
    showAddedByMeChip: Boolean,
    onCategorySelected: (PlaceCategory?) -> Unit,
    onToggleTopRated: () -> Unit,
    onToggleAddedByMe: () -> Unit,
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
                                contentDescription = stringResource(category.labelRes),
                                tint = style.color
                            )
                        },
                        label = { Text(stringResource(category.labelRes)) }
                    )
                }
            }

            // Rząd 2: dodatkowe toggle. Scrollowany horyzontalnie, żeby
            // przy włączeniu obu chipów + węższym ekranie nic się nie chowało
            // za krawędź.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
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
                            contentDescription = "Najlepiej oceniane",
                            tint = if (topRatedOnly) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = { Text("Najlepiej oceniane") }
                )
                if (showAddedByMeChip) {
                    FilterChip(
                        selected = addedByMeOnly,
                        onClick = onToggleAddedByMe,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Dodane przez Ciebie",
                                tint = if (addedByMeOnly) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = { Text("Dodane przez Ciebie") }
                    )
                }
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
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PlacePreviewContent(
    place: Place,
    onOpenDetails: () -> Unit
) {
    val context = LocalContext.current

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
                contentDescription = "Zdjęcie miejsca ${place.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Spacer(Modifier.height(12.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(
                category = place.category,
                size = 28.dp,
                iconSize = 18.dp
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
                        contentDescription = "Ocena",
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
                    contentDescription = "Adres",
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
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val uri = Uri.parse(
                    "https://www.google.com/maps/search/?api=1" +
                        "&query=${place.latitude},${place.longitude}"
                )
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Pokaż na mapie Google",
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.view_on_google_maps))
        }
    }
}

/**
 * Custom "Moja lokalizacja" FAB – zamiennik natywnego przycisku Maps SDK.
 *
 * Zalety vs natywny:
 *  - Pełna kontrola nad wyglądem (Material 3, brand colors).
 *  - Możliwość wyzwolenia permission launchera z poziomu onClick (natywny
 *    button wymaga `isMyLocationEnabled = true`, więc nie działa bez
 *    uprawnienia).
 *  - Swobodne pozycjonowanie w layoutcie Compose (bez walki z
 *    `contentPadding` mapy).
 *
 * Wizualnie: mała okrągła powierzchnia z ikoną crosshair-a, lekko
 * podniesiona (shadow), żeby odcinać się od tła mapy.
 */
@Composable
private fun MapMyLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Icon(
            imageVector = Icons.Filled.MyLocation,
            contentDescription = stringResource(R.string.map_my_location),
            modifier = Modifier.size(22.dp)
        )
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
    cameraPositionState: CameraPositionState,
    shouldAnimate: () -> Boolean = { true }
) {
    val coords = runCatching { fetchCurrentLocation(context) }.getOrNull() ?: return
    if (!shouldAnimate()) return
    cameraPositionState.animate(
        CameraUpdateFactory.newLatLngZoom(
            LatLng(coords.first, coords.second),
            NEAR_ME_ZOOM
        )
    )
}
