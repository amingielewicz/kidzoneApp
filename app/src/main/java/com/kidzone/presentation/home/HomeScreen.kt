package com.kidzone.presentation.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kidzone.R
import com.kidzone.domain.model.Place
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
 *  3. "Blisko Ciebie" – LazyRow z miejscami w okolicy; jeżeli brak permission,
 *     pokazujemy rationale + przycisk requesta.
 *  4. "Top miejsca" – LazyRow z najwyżej ocenianymi.
 */
@Composable
fun HomeScreen(
    onOpenPlaceDetails: (placeId: String) -> Unit,
    onOpenMap: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

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

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // Wystarczy zgoda na coarse, żeby pokazać miejsca w pobliżu –
        // dokładność z grubsza jest tu OK (radius 10km).
        if (result.values.any { it }) {
            viewModel.onLocationPermissionGranted()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
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
        item {
            if (!state.locationGranted) {
                EnableLocationCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            } else {
                HorizontalPlacesRow(
                    places = state.nearbyPlaces,
                    isLoading = state.isNearbyLoading,
                    emptyMessage = stringResource(R.string.home_no_nearby_places),
                    onPlaceClick = onOpenPlaceDetails
                )
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.home_top_places),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        item {
            HorizontalPlacesRow(
                places = state.topPlaces,
                isLoading = state.isTopLoading,
                emptyMessage = stringResource(R.string.home_no_top_places),
                onPlaceClick = onOpenPlaceDetails
            )
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
                contentDescription = null,
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
                contentDescription = null
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

/**
 * Horyzontalna lista [PlaceCard]ów. Obsługuje 3 stany:
 *  - loading – spinner pośrodku rzędu,
 *  - puste – komunikat [emptyMessage],
 *  - dane – LazyRow z kartami.
 *
 * Wysokość rzędu jest stała ([ROW_HEIGHT]) niezależnie od stanu, żeby
 * zawartość listy nie skakała przy odświeżeniu.
 */
@Composable
private fun HorizontalPlacesRow(
    places: List<Place>,
    isLoading: Boolean,
    emptyMessage: String,
    onPlaceClick: (placeId: String) -> Unit
) {
    val rowHeight = PLACE_ROW_HEIGHT
    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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
                items(places, key = { it.id }) { place ->
                    PlaceCard(place = place, onClick = { onPlaceClick(place.id) })
                }
            }
        }
    }
}

/**
 * Karta pojedynczego miejsca w sekcji – kafelek z kolorowym headerem
 * (kolor i ikona z [com.kidzone.presentation.common.style] dla danej
 * kategorii), nazwą, kategorią i oceną.
 *
 * Świadomie nie pokazujemy zdjęcia (nawet jeśli `photoUrls` jest niepuste),
 * żeby strona Start ładowała się szybko bez sieciowego refetcha. Zdjęcia
 * są w PlaceDetails.
 */
@Composable
private fun PlaceCard(
    place: Place,
    onClick: () -> Unit
) {
    val style = place.category.style
    Card(
        modifier = Modifier
            .width(PLACE_CARD_WIDTH)
            .height(PLACE_ROW_HEIGHT)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header z kolorem i ikoną kategorii – działa jak "okładka" karty.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PLACE_CARD_HEADER_HEIGHT)
                    .background(style.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(PLACE_CARD_ICON_SIZE)
                )
            }
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
                        contentDescription = null,
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
 * Karta z prośbą o włączenie lokalizacji. Pokazywana w sekcji "Blisko Ciebie"
 * gdy user nie nadał uprawnienia – tłumaczy po co nam to + button do requesta.
 */
@Composable
private fun EnableLocationCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_location_rationale),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClick) {
                Text(stringResource(R.string.home_enable_location))
            }
        }
    }
}

