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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap as GoogleMapSdk
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.view.ClusterRenderer
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.clustering.rememberClusterManager
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

private val DEFAULT_CAMERA_TARGET = LatLng(52.2297, 21.0122)
private const val DEFAULT_CAMERA_ZOOM = 11f
private const val NEAR_ME_ZOOM = 14f
private const val FOCUS_PLACE_ZOOM = 16f
private const val MIN_CLUSTER_SIZE = 2
private const val CLUSTER_FIT_BOUNDS_PADDING_PX = 96
private const val MARKER_ANCHOR_CENTER = 0.5f
private const val SPIDERFY_RADIUS_DEGREES = 0.00012
private const val SPIDERFY_RADIUS_STEP_DEGREES = 0.000015
private const val SPIDERFY_MAX_EXTRA = 8

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, MapsComposeExperimentalApi::class, ExperimentalSharedTransitionApi::class)
@Suppress("CyclomaticComplexMethod", "FunctionNaming", "LongMethod")
@Composable
fun MapScreen(
    onOpenPlaceDetails: (placeId: String) -> Unit,
    focusOn: LatLng? = null,
    locationPermissionGrantedSignal: Boolean = false,
    onFocusConsumed: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gpsEnabled = rememberLocationServiceEnabled()

    var locationPermissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_CAMERA_TARGET, DEFAULT_CAMERA_ZOOM)
    }
    var mapLoaded by remember { mutableStateOf(false) }
    var userTouchedMap by remember { mutableStateOf(false) }
    var showPlacesList by remember { mutableStateOf(false) }
    val markerIconCache = rememberMarkerIcons()
    val clusterItems = remember(state.places) {
        buildPlaceClusterItems(state.places)
    }

    ReportSettledViewport(
        cameraPositionState = cameraPositionState,
        mapLoaded = mapLoaded,
        onViewportChanged = viewModel::onViewportChanged
    )

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
            scope.launch {
                recenterOnUser(
                    context = context,
                    cameraPositionState = cameraPositionState,
                    shouldAnimate = { !userTouchedMap }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        if (locationPermissionGranted && focusOn == null) {
            recenterOnUser(
                context = context,
                cameraPositionState = cameraPositionState,
                shouldAnimate = { !userTouchedMap }
            )
        }
    }

    LaunchedEffect(locationPermissionGrantedSignal) {
        if (locationPermissionGrantedSignal && !locationPermissionGranted) {
            locationPermissionGranted = true
            if (focusOn == null) {
                recenterOnUser(
                    context = context,
                    cameraPositionState = cameraPositionState,
                    shouldAnimate = { !userTouchedMap }
                )
            }
        }
    }

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
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                compassEnabled = true
            ),
            // Logo Google nisko na krawędzi (6dp)
            contentPadding = PaddingValues(bottom = 6.dp),
            onMapLoaded = { mapLoaded = true },
            onMapClick = {
                userTouchedMap = true
                viewModel.selectPlace(null)
            }
        ) {
            val clusterManager = rememberClusterManager<PlaceClusterItem>()
            val clusterRenderer = rememberPlaceClusterRenderer(
                clusterManager = clusterManager,
                markerIconCache = markerIconCache
            )

            if (clusterManager != null && clusterRenderer != null) {
                LaunchedEffect(clusterManager, clusterRenderer) {
                    clusterManager.renderer = clusterRenderer
                    clusterManager.setOnClusterClickListener { cluster ->
                        userTouchedMap = true
                        viewModel.selectPlace(null)
                        scope.launch {
                            cameraPositionState.animate(cameraUpdateForCluster(cluster))
                        }
                        true
                    }
                    clusterManager.setOnClusterItemClickListener { item ->
                        userTouchedMap = true
                        viewModel.selectPlace(item.place.id)
                        true
                    }
                }

                androidx.compose.runtime.key(clusterItems) {
                    Clustering(
                        items = clusterItems,
                        clusterManager = clusterManager
                    )
                }
            }
        }

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
                showAddedByMeChip = true, // Simplified check
                onCategorySelected = viewModel::onCategorySelect,
                onToggleTopRated = viewModel::toggleTopRated,
                onToggleAddedByMe = viewModel::toggleAddedByMe,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.isLoading && state.places.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .semantics {
                        contentDescription = context.getString(R.string.map_loading_places)
                        liveRegion = LiveRegionMode.Polite
                    }
            )
        }

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
                .offset(y = 160.dp)
        )

        // Customowe przyciski zoom +/- na stałej, dobrej wysokości (180dp)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 180.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapIconButton(
                icon = Icons.Filled.Add,
                contentDescription = stringResource(R.string.map_zoom_in),
                onClick = {
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                    }
                }
            )
            MapIconButton(
                icon = Icons.Filled.Remove,
                contentDescription = stringResource(R.string.map_zoom_out),
                onClick = {
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                    }
                }
            )
        }

        Button(
            onClick = { showPlacesList = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 236.dp)
        ) {
            Text(stringResource(R.string.map_list_button, state.places.size))
        }

        state.errorMessage?.let { msg ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 88.dp, bottom = 96.dp)
                    .semantics {
                        liveRegion = LiveRegionMode.Assertive
                    },
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 4.dp
            ) {
                Text(
                    text = msg.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }

    if (selectedPlace != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectPlace(null) },
            sheetState = sheetState
        ) {
            PlacePreviewContent(
                place = selectedPlace,
                onOpenDetails = {
                    onOpenPlaceDetails(selectedPlace.id)
                    viewModel.selectPlace(null)
                }
            )
        }
    }

    if (showPlacesList) {
        ModalBottomSheet(
            onDismissRequest = { showPlacesList = false }
        ) {
            MapPlacesListSheet(
                places = state.places,
                onOpenDetails = { placeId ->
                    showPlacesList = false
                    viewModel.selectPlace(null)
                    onOpenPlaceDetails(placeId)
                }
            )
        }
    }
}

internal data class PlaceClusterItem(
    val place: Place,
    private val markerPosition: LatLng
) : com.google.maps.android.clustering.ClusterItem {
    override fun getPosition(): LatLng = markerPosition
    override fun getTitle(): String = place.name
    override fun getSnippet(): String? = place.address.takeIf { it.isNotBlank() }
    override fun getZIndex(): Float? = null
    override fun hashCode(): Int = place.id.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlaceClusterItem) return false
        return place.id == other.place.id
    }
}

internal fun buildPlaceClusterItems(places: List<Place>): List<PlaceClusterItem> =
    places
        .groupBy { place -> place.latitude to place.longitude }
        .flatMap { (_, group) ->
            if (group.size == 1) {
                group.map { place ->
                    PlaceClusterItem(place, LatLng(place.latitude, place.longitude))
                }
            } else {
                spiderfyPlaceItems(group)
            }
        }

private fun spiderfyPlaceItems(places: List<Place>): List<PlaceClusterItem> {
    val center = LatLng(places.first().latitude, places.first().longitude)
    val radius = SPIDERFY_RADIUS_DEGREES + places.size.coerceAtMost(SPIDERFY_MAX_EXTRA) *
        SPIDERFY_RADIUS_STEP_DEGREES
    return places.mapIndexed { index, place ->
        val angle = (2.0 * kotlin.math.PI * index) / places.size
        PlaceClusterItem(
            place = place,
            markerPosition = LatLng(
                center.latitude + kotlin.math.sin(angle) * radius,
                center.longitude + kotlin.math.cos(angle) * radius
            )
        )
    }
}

private fun cameraUpdateForCluster(cluster: Cluster<PlaceClusterItem>) =
    if (cluster.items.map { item -> item.place }.hasSameCoordinates()) {
        CameraUpdateFactory.newLatLngZoom(cluster.position, FOCUS_PLACE_ZOOM)
    } else {
        CameraUpdateFactory.newLatLngBounds(
            LatLngBounds.builder().apply {
                cluster.items.forEach { item ->
                    include(LatLng(item.place.latitude, item.place.longitude))
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
@GoogleMapComposable
@OptIn(MapsComposeExperimentalApi::class)
private fun rememberPlaceClusterRenderer(
    clusterManager: ClusterManager<PlaceClusterItem>?,
    markerIconCache: MarkerIconCache
): ClusterRenderer<PlaceClusterItem>? {
    val context = LocalContext.current
    val rendererState = remember {
        mutableStateOf<ClusterRenderer<PlaceClusterItem>?>(null)
    }

    clusterManager ?: return null
    MapEffect(clusterManager, markerIconCache) { map ->
        val renderer = PlaceClusterRenderer(
            context = context,
            googleMap = map,
            clusterManager = clusterManager,
            markerIconCache = markerIconCache
        ).apply {
            setMinClusterSize(MIN_CLUSTER_SIZE)
            setAnimation(false)
        }
        clusterManager.renderer = renderer
        rendererState.value = renderer
    }

    return rendererState.value
}

private class PlaceClusterRenderer(
    context: Context,
    private val googleMap: GoogleMapSdk,
    clusterManager: ClusterManager<PlaceClusterItem>,
    private val markerIconCache: MarkerIconCache
) : DefaultClusterRenderer<PlaceClusterItem>(context, googleMap, clusterManager) {

    override fun shouldRenderAsCluster(cluster: Cluster<PlaceClusterItem>): Boolean {
        val zoom = googleMap.cameraPosition.zoom
        return when {
            zoom > 15f -> cluster.size >= 10
            else -> cluster.size >= MIN_CLUSTER_SIZE
        }
    }

    override fun onBeforeClusterItemRendered(
        item: PlaceClusterItem,
        markerOptions: MarkerOptions
    ) {
        super.onBeforeClusterItemRendered(item, markerOptions)
        markerOptions
            .icon(markerIconCache.getCategoryIcon(item.place.category))
            .anchor(MARKER_ANCHOR_CENTER, MARKER_ANCHOR_CENTER)
    }

    override fun onClusterItemUpdated(item: PlaceClusterItem, marker: Marker) {
        super.onClusterItemUpdated(item, marker)
        marker.setIcon(markerIconCache.getCategoryIcon(item.place.category))
    }

    override fun onBeforeClusterRendered(
        cluster: Cluster<PlaceClusterItem>,
        markerOptions: MarkerOptions
    ) {
        markerOptions
            .icon(markerIconCache.getClusterIcon(cluster.size))
            .anchor(MARKER_ANCHOR_CENTER, MARKER_ANCHOR_CENTER)
    }

    override fun onClusterUpdated(cluster: Cluster<PlaceClusterItem>, marker: Marker) {
        marker.setIcon(markerIconCache.getClusterIcon(cluster.size))
    }
}

@OptIn(kotlinx.coroutines.FlowPreview::class)
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
            .distinctUntilChanged()
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

@Composable
private fun LocationPermissionBanner(
    onAllowClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Polite
        },
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
                    text = stringResource(R.string.map_location_banner_text),
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
                    Text(stringResource(R.string.settings))
                }
                Button(
                    onClick = onAllowClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.allow))
                }
            }
        }
    }
}

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
    val orderedCategories = PlaceCategory.entries

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
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
                    label = { Text(stringResource(R.string.category_all)) }
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
                            contentDescription = null,
                            tint = if (topRatedOnly) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = { Text(stringResource(R.string.filter_top_rated)) }
                )
                if (showAddedByMeChip) {
                    FilterChip(
                        selected = addedByMeOnly,
                        onClick = { onToggleAddedByMe() },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = if (addedByMeOnly) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = { Text(stringResource(R.string.filter_added_by_me)) }
                    )
                }
            }
        }
    }
}

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
        if (place.photoUrls.isNotEmpty()) {
            AsyncImage(
                model = place.photoUrls.first(),
                contentDescription = stringResource(R.string.photo_thumbnail_description),
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
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
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
            Text(stringResource(R.string.see_details))
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
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.view_on_google_maps))
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun MapPlacesListSheet(
    places: List<Place>,
    onOpenDetails: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.map_places_list_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.map_places_list_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (places.isEmpty()) {
            Text(
                text = stringResource(R.string.map_no_places_to_display),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = places,
                    key = { it.id }
                ) { place ->
                    MapPlaceListItem(
                        place = place,
                        onOpenDetails = { onOpenDetails(place.id) }
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming", "LongMethod")
private fun MapPlaceListItem(
    place: Place,
    onOpenDetails: () -> Unit
) {
    val categoryLabel = stringResource(place.category.labelRes)
    val ratingLabel = mapPlaceRatingLabel(place)
    val addressLabel = place.address.takeIf { it.isNotBlank() }
    val accessibilityLabel = buildString {
        append(place.name)
        append(", ")
        append(categoryLabel)
        append(", ")
        append(ratingLabel)
        addressLabel?.let {
            append(", ")
            append(it)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            }
            .clickable(
                onClickLabel = stringResource(R.string.map_open_place_details_label),
                role = Role.Button,
                onClick = onOpenDetails
            ),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = categoryLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = ratingLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            addressLabel?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun mapPlaceRatingLabel(place: Place): String =
    if (place.reviewsCount > 0) {
        stringResource(R.string.map_place_rating_count_label, place.averageRating, place.reviewsCount)
    } else {
        stringResource(R.string.map_no_reviews)
    }

@Composable
private fun MapIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        elevation = FloatingActionButtonDefaults.elevation(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MapMyLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
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
