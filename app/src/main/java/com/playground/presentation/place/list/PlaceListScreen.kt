package com.playground.presentation.place.list

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.playground.domain.model.Amenity
import com.playground.domain.model.Place
import com.playground.domain.model.PlaceCategory
import com.playground.presentation.common.style
import kotlinx.coroutines.launch

/**
 * 4 najczęściej szukane udogodnienia – pokazujemy je jako quick-chipy
 * bezpośrednio na ekranie (zawsze widoczne, multi-select). Pasują do każdej
 * kategorii i to po nich rodzice filtrują najczęściej.
 */
private val QUICK_AMENITIES = listOf(
    Amenity.CHANGING_TABLE,
    Amenity.TOILET,
    Amenity.STROLLER_ACCESS,
    Amenity.PARKING
)

/**
 * Lista miejsc – LazyColumn kart z filtrami po kategorii i udogodnieniach.
 *
 * Subskrybuje Firestore przez [PlaceListViewModel] – snapshot listener
 * w repo automatycznie aktualizuje listę po dodaniu nowego miejsca z
 * [com.playground.presentation.place.add.AddPlaceScreen], bez potrzeby
 * pull-to-refresh.
 *
 * Filtry:
 *  - kategoria (single-select chipy w pierwszym rzędzie)
 *  - 4 uniwersalne udogodnienia (multi-select chipy w drugim rzędzie)
 *  - pełna lista udogodnień (multi-select w bottom sheecie pod "Filtry · N")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceListScreen(
    onOpenPlaceDetails: (placeId: String) -> Unit,
    viewModel: PlaceListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Liczba aktywnych filtrów udogodnień siedzących w sheecie
    // (wszystko poza QUICK_AMENITIES – te są na ekranie głównym i mają osobny stan wizualny).
    val advancedAmenitiesCount = state.selectedAmenities.count { it !in QUICK_AMENITIES }

    Column(modifier = Modifier.fillMaxSize()) {
        CategoryFilterBar(
            selectedCategory = state.selectedCategory,
            onCategorySelected = viewModel::onCategorySelected
        )

        QuickAmenityBar(
            selectedAmenities = state.selectedAmenities,
            advancedFiltersCount = advancedAmenitiesCount,
            onAmenityToggled = viewModel::onAmenityToggled,
            onOpenFilterSheet = { showFilterSheet = true }
        )

        when {
            state.isLoading && state.places.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage != null && state.places.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            state.places.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Brak miejsc pasujących do filtrów",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = state.places, key = { it.id }) { place ->
                        PlaceCard(
                            place = place,
                            onClick = { onOpenPlaceDetails(place.id) }
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        AmenityFilterSheet(
            sheetState = sheetState,
            selectedCategory = state.selectedCategory,
            selectedAmenities = state.selectedAmenities,
            totalResultsCount = state.places.size,
            onAmenityToggled = viewModel::onAmenityToggled,
            onClearAll = viewModel::onAmenitiesCleared,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    if (!sheetState.isVisible) showFilterSheet = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterBar(
    selectedCategory: PlaceCategory?,
    onCategorySelected: (PlaceCategory?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text("Wszystkie") }
        )
        PlaceCategory.entries.forEach { category ->
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAmenityBar(
    selectedAmenities: Set<Amenity>,
    advancedFiltersCount: Int,
    onAmenityToggled: (Amenity) -> Unit,
    onOpenFilterSheet: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QUICK_AMENITIES.forEach { amenity ->
            FilterChip(
                selected = amenity in selectedAmenities,
                onClick = { onAmenityToggled(amenity) },
                label = { Text(stringResource(amenity.labelRes)) }
            )
        }

        AssistChip(
            onClick = onOpenFilterSheet,
            label = {
                Text(
                    text = if (advancedFiltersCount > 0) {
                        "Filtry · $advancedFiltersCount"
                    } else {
                        "Filtry"
                    },
                    fontWeight = if (advancedFiltersCount > 0) FontWeight.SemiBold else FontWeight.Normal
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null
                )
            },
            colors = if (advancedFiltersCount > 0) {
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                AssistChipDefaults.assistChipColors()
            }
        )
    }
}

@Composable
private fun PlaceCard(
    place: Place,
    onClick: () -> Unit
) {
    val categoryStyle = place.category.style

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = categoryStyle.icon,
                    contentDescription = null,
                    tint = categoryStyle.color,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
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
            if (place.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = place.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
