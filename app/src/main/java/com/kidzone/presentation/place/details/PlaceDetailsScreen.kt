package com.kidzone.presentation.place.details

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kidzone.R
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.Review
import com.kidzone.domain.model.User
import com.kidzone.presentation.common.style
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Szczegóły miejsca.
 *
 * Wszystko o samym miejscu zebrane w jedną kartę (header → opis →
 * lokalizacja → mały przycisk „Nawiguj" → autor + data); poniżej osobne
 * sekcje Udogodnienia i Opinie.
 *
 * Klik „Nawiguj" wystrzeliwuje intent z URL-em `https://www.google.com/maps/dir/`
 * – Android resolver otwiera Google Maps w trybie nawigacji turn-by-turn
 * (jeśli aplikacja jest zainstalowana, w przeciwnym razie spada na przeglądarkę).
 *
 * Dla **właściciela miejsca** w TopAppBar pojawia się overflow menu z akcjami
 * "Edytuj" i "Usuń" (z dialog potwierdzeniem).
 *
 * Dodawania opinii tu jeszcze nie ma – `addReview` w repo jest TODO.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailsScreen(
    onBack: () -> Unit,
    onEditPlace: (placeId: String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: PlaceDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val isOwner = remember(state.place, currentUser) {
        val place = state.place
        val user = currentUser
        place != null && user != null && place.ownerUserId == user.id
    }

    var showOverflow by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Po pomyślnym usunięciu – wracamy do listy.
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onDeleted()
    }

    // Błąd usuwania – pokazujemy w snackbarze i czyścimy w VM.
    LaunchedEffect(state.deleteErrorMessage) {
        val msg = state.deleteErrorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeDeleteError()
        }
    }

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
                },
                actions = {
                    if (isOwner && state.place != null) {
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Więcej akcji")
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edytuj") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = {
                                    showOverflow = false
                                    state.place?.let { onEditPlace(it.id) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Usuń") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showOverflow = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                        author = state.author,
                        reviews = state.reviews,
                        currentUserId = currentUser?.id,
                        onAddReview = viewModel::openAddReviewSheet
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            placeName = state.place?.name.orEmpty(),
            isDeleting = state.isDeleting,
            onConfirm = {
                viewModel.delete()
                showDeleteDialog = false
            },
            onDismiss = { if (!state.isDeleting) showDeleteDialog = false }
        )
    }

    // Sheet dodawania opinii – sterowany przez VM (state.showAddReviewSheet),
    // żeby błąd zapisu mógł go utrzymać otwartym (user widzi błąd, próbuje
    // ponownie). Po sukcesie VM ustawia flagę na false → sheet znika.
    if (state.showAddReviewSheet) {
        AddReviewSheet(
            placeName = state.place?.name.orEmpty(),
            isSubmitting = state.isAddingReview,
            errorMessage = state.addReviewError,
            onDismiss = viewModel::dismissAddReviewSheet,
            onSubmit = viewModel::submitReview
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceDetailsContent(
    place: Place,
    author: User?,
    reviews: List<Review>,
    currentUserId: String?,
    onAddReview: () -> Unit
) {
    // Jedna opinia per user per miejsce (MVP). Przycisk "Dodaj opinię" znika,
    // gdy zalogowany user już wystawił ocenę. Edycję dorobimy w osobnym PR-ze.
    val alreadyReviewed = currentUserId != null && reviews.any { it.userId == currentUserId }
    val canAddReview = currentUserId != null && !alreadyReviewed

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sekcja 1: cale miejsce w jednej karcie
        // (header + opis + lokalizacja + Nawiguj + autor)
        item {
            PlaceMainCard(
                place = place,
                author = author
            )
        }

        // Sekcja 2: Udogodnienia
        if (place.amenities.isNotEmpty()) {
            item {
                SectionCard(title = "Udogodnienia (${place.amenities.size})") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Stable order na podstawie kolejności w enumie Amenity
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

        // Sekcja 3: Opinie z przyciskiem "Dodaj opinię" w nagłówku.
        item {
            SectionCard(
                title = "Opinie (${reviews.size})",
                trailing = if (canAddReview) {
                    {
                        TextButton(onClick = onAddReview) {
                            Text("Dodaj opinię")
                        }
                    }
                } else null
            ) {
                if (reviews.isEmpty()) {
                    Text(
                        text = if (canAddReview) {
                            "Brak opinii. Bądź pierwszy!"
                        } else {
                            "Brak opinii."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Każda opinia jako osobny item, żeby LazyColumn dobrze recyklował przy długich listach
        items(items = reviews, key = { it.id }) { review ->
            ReviewCard(
                review = review,
                isMine = currentUserId != null && review.userId == currentUserId
            )
        }
    }
}

/**
 * Główna karta szczegółów miejsca – "jedno okno" w którym po kolei są:
 *  1. nazwa + ikona kategorii + ocena (header),
 *  2. opis (jeśli niepusty),
 *  3. adres + współrzędne,
 *  4. mały przycisk „Nawiguj" wyrzucający do Google Maps w trybie
 *     turn-by-turn navigation (intent z `maps/dir/?api=1`),
 *  5. autor + data dodania.
 *
 * Bez sub-headerów typu "Opis"/"Lokalizacja" – wizualnie jeden spójny
 * blok, a delikatne dividery rozdzielają poszczególne kawałki.
 */
@Composable
private fun PlaceMainCard(
    place: Place,
    author: User?
) {
    val style = place.category.style
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- 1. Header: nazwa + kategoria + ocena ---
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

            // --- 2. Opis ---
            if (place.description.isNotBlank()) {
                SoftDivider()
                Text(
                    text = place.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // --- 3. Lokalizacja: adres + współrzędne + Nawiguj ---
            SoftDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.address.ifBlank { "Adres niedostępny" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "%.5f, %.5f".format(place.latitude, place.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Mała ikona nawigacji – odpala Google Maps w trybie
                // turn-by-turn nawigacji (driving). URL `maps/dir/?api=1` jest
                // oficjalny Google'a i Android sam go resolwuje do aplikacji
                // Maps; jak Maps brak, otwiera się w przeglądarce.
                FilledTonalIconButton(
                    onClick = {
                        val uri = Uri.parse(
                            "https://www.google.com/maps/dir/?api=1" +
                                "&destination=${place.latitude},${place.longitude}" +
                                "&travelmode=driving"
                        )
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { context.startActivity(intent) }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = "Nawiguj"
                    )
                }
            }

            // --- 4. Dodano przez ---
            SoftDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                val authorName = author?.name?.takeIf { it.isNotBlank() }
                val datePart = place.createdAtMillis
                    .takeIf { it > 0L }
                    ?.let { " · " + formatDate(it) }
                    .orEmpty()
                Text(
                    text = if (authorName != null) {
                        "Dodano przez $authorName$datePart"
                    } else {
                        "Dodano przez nieznanego użytkownika$datePart"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Cienki divider z marginesem góra/dół, do separacji sekcji wewnątrz karty. */
@Composable
private fun SoftDivider() {
    Spacer(Modifier.height(12.dp))
    androidx.compose.material3.HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
    Spacer(Modifier.height(12.dp))
}

private fun formatDate(millis: Long): String {
    // dd.MM.yyyy zgodnie z polską normą.
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale("pl", "PL"))
    return formatter.format(Date(millis))
}

private fun plural(count: Int): String = when {
    count == 1 -> "opinia"
    count % 10 in 2..4 && (count % 100 !in 12..14) -> "opinie"
    else -> "opinii"
}

@Composable
private fun SectionCard(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                trailing?.invoke()
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ReviewCard(
    review: Review,
    isMine: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        // Subtelny outline na opinii zalogowanego usera, żeby ją łatwo
        // zlokalizował na dłuższej liście.
        border = if (isMine) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        } else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = review.authorName.ifBlank { "Anonim" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                if (isMine) {
                    Spacer(Modifier.width(6.dp))
                    MyReviewBadge()
                }
                Spacer(Modifier.weight(1f))
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
                    text = formatDate(review.createdAtMillis),
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

/** Mała plakietka pod nickiem, oznaczająca własną opinię na liście. */
@Composable
private fun MyReviewBadge() {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Text(
            text = "Twoja opinia",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    placeName: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Usunąć miejsce?") },
        text = {
            Text(
                text = if (placeName.isNotBlank()) {
                    "\"$placeName\" zostanie nieodwracalnie usunięte z bazy. Czy na pewno chcesz kontynuować?"
                } else {
                    "Miejsce zostanie nieodwracalnie usunięte z bazy. Czy na pewno chcesz kontynuować?"
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Usuń")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("Anuluj")
            }
        }
    )
}
