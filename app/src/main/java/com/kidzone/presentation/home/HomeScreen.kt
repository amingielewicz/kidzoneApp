package com.kidzone.presentation.home

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import com.kidzone.R
import com.kidzone.domain.model.Place
import com.kidzone.presentation.common.CategoryIcon
import com.kidzone.presentation.common.GpsAcquiringBanner
import com.kidzone.presentation.common.GpsDisabledBanner
import com.kidzone.presentation.common.rememberLocationServiceEnabled
import com.kidzone.presentation.common.shimmerEffect
import com.kidzone.presentation.common.style

private val PLACE_ROW_HEIGHT = 148.dp
private val PLACE_CARD_WIDTH = 164.dp
private val PLACE_CARD_HEADER_HEIGHT = 56.dp
private val PLACE_CARD_ICON_SIZE = 28.dp
private val PLACE_CARD_CONTENT_PADDING = 10.dp

/**
 * Ekran "Start" – pierwsza zakładka po zalogowaniu.
 *
 * Sekcje (w kolejności):
 *  1. Hero – kolorowe powitanie z taglinem.
 *  2. CTA do mapy – pełnoszerokościowa karta zachęcająca do otwarcia mapy.
 *  3. Systemowy dialog Androida o lokalizację, jeśli permission nie jest jeszcze nadany.
 *  4. "Blisko Ciebie" – LazyRow z miejscami w okolicy.
 *  5. "Top miejsca" – LazyRow z najwyżej ocenianymi miejscami w pobliżu.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    onOpenPlaceDetails: (placeId: String, source: String?) -> Unit,
    onOpenMap: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val state by viewModel.uiState.collectAsState()
    val gpsEnabled = rememberLocationServiceEnabled()
    val networkStatus by com.kidzone.presentation.common.rememberNetworkStatus()

    // Auto-refresh po przywróceniu internetu
    var previousNetworkStatus by remember { mutableStateOf(networkStatus) }
    LaunchedEffect(networkStatus) {
        if (previousNetworkStatus == com.kidzone.presentation.common.NetworkStatus.UNAVAILABLE
            && networkStatus == com.kidzone.presentation.common.NetworkStatus.AVAILABLE
        ) {
            viewModel.refresh()
        }
        previousNetworkStatus = networkStatus
    }

    // Refresh permission flag gdy ekran wraca na pierwszy plan – user mógł
    // pójść do Settings i włączyć/wyłączyć lokalizację, a my chcemy mieć
    // aktualny stan w UI bez restartu.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLocationGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    var hasAskedForLocationPermission by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // Wystarczy zgoda na coarse, żeby pokazać miejsca w pobliżu –
        // dokładność z grubsza jest tu OK (radius 10km).
        if (result.values.any { it }) {
            viewModel.onLocationPermissionGranted()
        }
    }

    // --- SettingsClient: systemowy dialog "Włącz GPS" bez wychodzenia z apki ---
    val context = LocalContext.current
    val gpsSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User włączył GPS w systemowym dialogu – odśwież dane
            viewModel.refresh()
        }
    }

    // Automatycznie wyświetl dialog SettingsClient gdy GPS jest wyłączony
    // a permission jest nadany. Używamy LaunchedEffect z kluczem gpsEnabled,
    // żeby dialog pokazał się raz (nie w kółko).
    var hasRequestedGpsDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.locationGranted, gpsEnabled) {
        if (state.locationGranted && !gpsEnabled && !hasRequestedGpsDialog) {
            hasRequestedGpsDialog = true
            try {
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    10_000L
                ).build()
                val settingsRequest = LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest)
                    .setAlwaysShow(true) // force show dialog even if previously dismissed
                    .build()
                val settingsClient = LocationServices.getSettingsClient(context)
                settingsClient.checkLocationSettings(settingsRequest).await()
                // GPS jest już włączony (edge case – zmieniono w tle)
            } catch (e: Exception) {
                if (e is ResolvableApiException) {
                    // Pokazuje systemowy dialog "Włącz lokalizację"
                    val intentSender = e.resolution.intentSender
                    gpsSettingsLauncher.launch(
                        IntentSenderRequest.Builder(intentSender).build()
                    )
                }
            }
        }
        // Reset flagi gdy GPS zostanie włączony (żeby następne wyłączenie
        // znów wyzwoliło dialog)
        if (gpsEnabled) {
            hasRequestedGpsDialog = false
        }
    }

    LaunchedEffect(state.locationGranted) {
        if (!state.locationGranted && !hasAskedForLocationPermission) {
            hasAskedForLocationPermission = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // GPS banners - fixed at the top of the content
            if (state.locationGranted && !gpsEnabled) {
                GpsDisabledBanner()
            }
            if (state.isAcquiringLocation) {
                GpsAcquiringBanner()
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { HeroSection() }

                item {
                    OpenMapCta(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = onOpenMap
                    )
                }

                item {
                    SectionHeader(
                        title = stringResource(R.string.home_nearby_places),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                if (state.locationGranted) {
                    item {
                        HorizontalPlacesRow(
                            places = state.nearbyPlaces,
                            isLoading = state.isNearbyLoading,
                            emptyMessage = stringResource(R.string.home_no_nearby_places),
                            onPlaceClick = { onOpenPlaceDetails(it, "nearby") },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            keyPrefix = "nearby"
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = stringResource(R.string.home_top_places),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                if (state.locationGranted) {
                    item {
                        HorizontalPlacesRow(
                            places = state.topPlaces,
                            isLoading = state.isTopLoading,
                            emptyMessage = stringResource(R.string.home_no_top_places),
                            onPlaceClick = { onOpenPlaceDetails(it, "top") },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            keyPrefix = "top"
                        )
                    }
                }

                state.errorMessage?.let { msg ->
                    item {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hero ekranu Start - powitanie + tagline na tle gradientu w brand-blue.
 *
 * Wizualnie celowo "uniesiony": nie pełnoszerokościowy bar przyklejony do
 * krawędzi ekranu, tylko karta z marginesami po bokach i zaokrąglonymi
 * rogami. Daje to "troszkę węższy" niebieski blok z tekstem, który
 * lepiej dialoguje z kartami "Top miejsca" / "Blisko Ciebie" pod spodem
 * (one też mają boczne paddingi 16 dp). Tekst wewnątrz dodatkowo nie
 * rozciąga się na 100% szerokości karty - ograniczamy go do ~88%, żeby
 * długie taglines nie dotykały prawej krawędzi gradientu.
 */
@Composable
private fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .height(112.dp)
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            // ~88% szerokości karty - tekst zostaje czytelny, a niebieski blok
            // wygląda "troszkę węższy" niż gdyby napis biegł od krawędzi do krawędzi.
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Text(
                text = stringResource(R.string.home_welcome),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * CTA do mapy – pełnoszerokościowa karta z ikoną Map i strzałką w prawo.
 * Kliknięcie woła [onClick] (= przełączenie taba na Map w MainScreen).
 */
@Composable
private fun OpenMapCta(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Map,
                contentDescription = "Otwórz mapę",
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_open_map),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.home_open_map_subtitle),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Przejdź do mapy"
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun HorizontalPlacesRow(
    places: List<Place>,
    isLoading: Boolean,
    emptyMessage: String,
    onPlaceClick: (placeId: String) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    keyPrefix: String = ""
) {
    val rowHeight = PLACE_ROW_HEIGHT
    when {
        isLoading -> {
            LazyRow(
                modifier = Modifier.height(rowHeight),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = false
            ) {
                items(5) {
                    PlaceCardSkeleton()
                }
            }
        }
        places.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            LazyRow(
                modifier = Modifier.height(rowHeight),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(places, key = { "${keyPrefix}_${it.id}" }) { place ->
                    PlaceCard(
                        place = place,
                        onClick = { onPlaceClick(place.id) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope,
                        keyPrefix = keyPrefix
                    )
                }
            }
        }
    }
}

/**
 * Karta pojedynczego miejsca w sekcji – kafelek z kolorowym headerem
 * (kolor i ikona z [com.kidzone.presentation.common.style] dla danej
 * kategorii), nazwą, kategorią i oceną.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PlaceCard(
    place: Place,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    keyPrefix: String = ""
) {
    // Prefix keys to avoid duplicates on the same screen (e.g. Nearby vs Top)
    val animationKey = if (keyPrefix.isBlank()) "" else "${keyPrefix}_"

    Card(
        modifier = Modifier
            .width(PLACE_CARD_WIDTH)
            .height(PLACE_ROW_HEIGHT)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CategoryIcon(
                category = place.category,
                animationKey = "${animationKey}place_icon_${place.id}",
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
                size = PLACE_CARD_HEADER_HEIGHT,
                iconSize = PLACE_CARD_ICON_SIZE,
                modifier = Modifier.fillMaxWidth()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PLACE_CARD_CONTENT_PADDING),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(place.category.labelRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Ocena",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (place.reviewsCount == 0) {
                            "—"
                        } else {
                            "%.1f (%d)".format(place.averageRating, place.reviewsCount)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Skeleton loader for PlaceCard.
 */
@Composable
private fun PlaceCardSkeleton() {
    Card(
        modifier = Modifier
            .width(PLACE_CARD_WIDTH)
            .height(PLACE_ROW_HEIGHT),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PLACE_CARD_HEADER_HEIGHT)
                    .shimmerEffect()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PLACE_CARD_CONTENT_PADDING),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(16.dp)
                            .clip(MaterialTheme.shapes.small)
                            .shimmerEffect()
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(12.dp)
                            .clip(MaterialTheme.shapes.small)
                            .shimmerEffect()
                    )
                }
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(12.dp)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect()
                )
            }
        }
    }
}
