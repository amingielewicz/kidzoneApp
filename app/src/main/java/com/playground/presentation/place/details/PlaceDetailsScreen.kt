package com.playground.presentation.place.details

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.playground.R
import com.playground.domain.model.Amenity
import com.playground.domain.model.Place
import com.playground.domain.model.Review
import com.playground.presentation.common.style
import java.text.DateFormat
import java.util.Date

/**
 * Szczegóły miejsca.
 *
 * Sekcje:
 *  1. Header z ikoną i nazwą kategorii + ocena + liczba opinii
 *  2. Opis (jeśli niepusty)
 *  3. Adres + przycisk „Otwórz w Mapach" (intent geo:)
 *  4. Udogodnienia (chipy)
 *  5. Opinie (lista lub empty state)
 *
 * Dodawania opinii tu jeszcze nie ma – `addReview` w repo jest TODO,
 * przyjedzie w osobnym PR-ze razem z transakcyjną aktualizacją
 * averageRating.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailsScreen(
    onBack: () -> Unit,
    viewModel: PlaceDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.place?.name ?: stringResource(R.string.place_details),
                        maxLines = 1
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.place == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.place == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.errorMessage ?: "Nie udało się wczytać miejsca",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = viewModel::retry) {
                            Text("Spróbuj ponownie")
                        }
                    }
                }
                else -> {
                    PlaceDetailsContent(
                        place = state.place!!,
                        reviews = state.reviews
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceDetailsContent(
    place: Place,
    reviews: List<Review>
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header
        item {
            HeaderCard(place = place)
        }

        // 2. Opis
        if (place.description.isNotBlank()) {
            item {
                SectionCard(title = "Opis") {
                    Text(
                        text = place.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // 3. Adres + intent
        item {
            SectionCard(title = "Lokalizacja") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = place.address.ifBlank { "Adres niedostępny" },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "%.5f, %.5f".format(place.latitude, place.longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val uri = Uri.parse(
                            "geo:${place.latitude},${place.longitude}" +
                                "?q=${place.latitude},${place.longitude}" +
                                "(${Uri.encode(place.name)})"
                        )
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        // Bez tej flagi system woła nową activity z procesu, który nie ma
                        // task-a – w niektórych wersjach Androida by to crashnęło.
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Otwórz w Mapach")
                }
            }
        }

        // 4. Udogodnienia
        if (place.amenities.isNotEmpty()) {
            item {
                SectionCard(title = "Udogodnienia (${place.amenities.size})") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Stable order for amenities, na podstawie kolejności enum
                        // (czyli pogrupowanie z Amenity.kt: TL;DR -> plac -> jedzenie -> ...)
                        Amenity.entries
                            .filter { it in place.amenities }
                            .forEach { amenity ->
                                AssistChip(
                                    onClick = { /* read-only */ },
                                    enabled = false,
                                    label = { Text(stringResource(amenity.labelRes)) }
                                )
                            }
                    }
                }
            }
        }

        // 5. Opinie
        item {
            SectionCard(title = "Opinie (${reviews.size})") {
                if (reviews.isEmpty()) {
                    Text(
                        text = "Brak opinii. Bądź pierwszy!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Każda opinia jako osobny item, żeby LazyColumn dobrze recyklował przy długich listach
        items(items = reviews, key = { it.id }) { review ->
            ReviewCard(review = review)
        }
    }
}

@Composable
private fun HeaderCard(place: Place) {
    val style = place.category.style

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.color,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(place.category.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                if (place.reviewsCount > 0) {
                    Text(
                        text = "%.1f".format(place.averageRating),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "(${place.reviewsCount} ${plural(place.reviewsCount)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Brak ocen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun plural(count: Int): String = when {
    count == 1 -> "opinia"
    count % 10 in 2..4 && (count % 100 !in 12..14) -> "opinie"
    else -> "opinii"
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ReviewCard(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = review.authorName.ifBlank { "Anonim" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < review.rating) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            if (review.createdAtMillis > 0L) {
                Text(
                    text = DateFormat
                        .getDateInstance(DateFormat.MEDIUM)
                        .format(Date(review.createdAtMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (review.comment.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
