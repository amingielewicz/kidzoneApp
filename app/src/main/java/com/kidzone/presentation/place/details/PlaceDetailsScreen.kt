package com.kidzone.presentation.place.details

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kidzone.R
import com.kidzone.domain.model.Amenity
import kotlinx.coroutines.launch
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.Review
import com.kidzone.domain.model.User
import com.kidzone.presentation.common.RankBadge
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
    var showReportDialog by remember { mutableStateOf(false) }
    var showReportReviewDialog by remember { mutableStateOf(false) }
    var reviewToReport by remember { mutableStateOf<Review?>(null) }
    var showSuggestEditSheet by remember { mutableStateOf(false) }
    var showLocationCorrectionDialog by remember { mutableStateOf(false) }
    // Fullscreen photo viewer state
    var fullscreenPhotos by remember { mutableStateOf<List<String>>(emptyList()) }
    var fullscreenPhotoIndex by remember { mutableStateOf(0) }
    var fullscreenPhotosAreMine by remember { mutableStateOf(false) }
    var fullscreenPhotoUploadedBy by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showReportPhotoDialog by remember { mutableStateOf(false) }
    var photoUrlToReport by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Photo picker for adding photos to place (any logged-in user)
    val placePhotoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.addPhotoToPlace(uri)
        }
    }

    // Camera for adding photos to place
    val placeCameraUri = remember { androidx.compose.runtime.mutableStateOf<android.net.Uri?>(null) }
    val placeCameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && placeCameraUri.value != null) {
            viewModel.addPhotoToPlace(placeCameraUri.value!!)
        }
    }
    fun launchPlaceCamera() {
        val photoFile = java.io.File.createTempFile(
            "place_camera_",
            ".jpg",
            context.cacheDir
        )
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        placeCameraUri.value = uri
        placeCameraLauncher.launch(uri)
    }
    val placeCameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchPlaceCamera()
        }
    }

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

    // Snackbar po pomyślnym dodaniu / aktualizacji opinii. Event z VM
    // (jednorazowy) – po pokazaniu konsumujemy, żeby rotacja / re-kompozycja
    // nie pokazały go drugi raz.
    LaunchedEffect(state.reviewActionEvent) {
        val event = state.reviewActionEvent ?: return@LaunchedEffect
        val message = when (event) {
            PlaceDetailsViewModel.ReviewActionEvent.ADDED -> "Dziękujemy za opinię!"
            PlaceDetailsViewModel.ReviewActionEvent.UPDATED -> "Opinia zaktualizowana"
        }
        snackbarHostState.showSnackbar(message)
        viewModel.consumeReviewActionEvent()
    }

    // Snackbar: duplikat zdjęcia na ekranie szczegółów miejsca
    LaunchedEffect(state.placePhotoDuplicateEvent) {
        if (state.placePhotoDuplicateEvent) {
            snackbarHostState.showSnackbar("To zdjęcie zostało już dodane. Nie można dodać duplikatu.")
            viewModel.consumePlacePhotoDuplicateEvent()
        }
    }

    // Snackbar: duplikat zdjęcia w edycji opinii
    LaunchedEffect(state.reviewPhotoDuplicateEvent) {
        if (state.reviewPhotoDuplicateEvent) {
            snackbarHostState.showSnackbar("To zdjęcie zostało już dodane. Nie można dodać duplikatu.")
            viewModel.consumeReviewPhotoDuplicateEvent()
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
                    if (state.place != null) {
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Wi\u0119cej akcji")
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false }
                        ) {
                            // --- Opcje właściciela ---
                            if (isOwner) {
                                DropdownMenuItem(
                                    text = { Text("Edytuj") },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = {
                                        showOverflow = false
                                        state.place?.let { onEditPlace(it.id) }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Usu\u0144") },
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
                            // --- Udostępnij (dla wszystkich) ---
                            DropdownMenuItem(
                                text = { Text("Udost\u0119pnij") },
                                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                onClick = {
                                    showOverflow = false
                                    state.place?.let { place ->
                                        val shareText = buildString {
                                            append(place.name)
                                            if (place.address.isNotBlank()) {
                                                append("\n")
                                                append(place.address)
                                            }
                                            append("\n\nhttps://www.google.com/maps/search/?api=1")
                                            append("&query=${place.latitude},${place.longitude}")
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, place.name)
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(intent, "Udost\u0119pnij miejsce")
                                        )
                                    }
                                }
                            )
                            // --- Opcje nie-w\u0142a\u015Bciciela ---
                            if (!isOwner) {
                                DropdownMenuItem(
                                    text = { Text("Zaproponuj zmian\u0119") },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = {
                                        showOverflow = false
                                        showSuggestEditSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Koryguj lokalizacj\u0119") },
                                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                                    onClick = {
                                        showOverflow = false
                                        showLocationCorrectionDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Zg\u0142o\u015B") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Flag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showOverflow = false
                                        showReportDialog = true
                                    }
                                )
                            }
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
                        topRank = state.topRank,
                        sortOrder = state.sortOrder,
                        onSortOrderChange = viewModel::setSortOrder,
                        onAddReview = viewModel::openAddReviewSheet,
                        onEditReview = viewModel::openEditReviewSheet,
                        onReportReview = { review ->
                            reviewToReport = review
                            showReportReviewDialog = true
                        },
                        onOpenPhotoViewer = { photos, index, areMine ->
                            fullscreenPhotos = photos
                            fullscreenPhotoIndex = index
                            fullscreenPhotosAreMine = areMine
                            fullscreenPhotoUploadedBy = state.place?.photoUploadedBy.orEmpty()
                        },
                        onAddPlacePhoto = if (currentUser != null) {
                            {
                                placePhotoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        } else null,
                        onAddPlaceCamera = if (currentUser != null) {
                            {
                                val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    launchPlaceCamera()
                                } else {
                                    placeCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            }
                        } else null,
                        isUploadingPlacePhoto = state.isUploadingPlacePhoto
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

    if (showReportDialog) {
        ReportPlaceDialog(
            onSubmit = { reason, comment ->
                viewModel.reportPlace(reason, comment)
                showReportDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Dzi\u0119kujemy za zg\u0142oszenie!")
                }
            },
            onDismiss = { showReportDialog = false }
        )
    }

    // --- Zaproponuj zmianę ---
    if (showSuggestEditSheet && state.place != null) {
        SuggestEditSheet(
            place = state.place!!,
            onSubmit = { name, description, category, amenities ->
                viewModel.submitSuggestedEdit(name, description, category, amenities)
                showSuggestEditSheet = false
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Dzi\u0119kujemy! Propozycja zmiany zosta\u0142a wys\u0142ana do weryfikacji."
                    )
                }
            },
            onDismiss = { showSuggestEditSheet = false }
        )
    }

    // --- Koryguj lokalizację ---
    if (showLocationCorrectionDialog && state.place != null) {
        LocationCorrectionDialog(
            onSubmit = { lat, lng, address ->
                viewModel.submitLocationCorrection(lat, lng, address)
                showLocationCorrectionDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Dzi\u0119kujemy! Korekta lokalizacji zosta\u0142a wys\u0142ana do weryfikacji."
                    )
                }
            },
            onDismiss = { showLocationCorrectionDialog = false }
        )
    }

    // Sheet dodawania opinii – sterowany przez VM (state.showAddReviewSheet),
    // żeby błąd zapisu mógł go utrzymać otwartym (user widzi błąd, próbuje
    // ponownie). Po sukcesie VM ustawia flagę na false → sheet znika.
    if (state.showAddReviewSheet) {
        val editing = state.editingReview
        AddReviewSheet(
            placeName = state.place?.name.orEmpty(),
            isSubmitting = state.isAddingReview,
            errorMessage = state.addReviewError,
            onDismiss = viewModel::dismissAddReviewSheet,
            onSubmit = { rating, comment, photoUris, retainedUrls ->
                viewModel.submitReview(rating, comment, photoUris, retainedUrls)
            },
            initialRating = editing?.rating ?: 0,
            initialComment = editing?.comment.orEmpty(),
            initialPhotoUrls = editing?.photoUrls.orEmpty(),
            isEditing = editing != null
        )
    }

    // Dialog zgłaszania opinii jako spam – analogiczny do ReportPlaceDialog.
    if (showReportReviewDialog && reviewToReport != null) {
        ReportReviewDialog(
            authorName = reviewToReport!!.authorName,
            onSubmit = { reason, comment ->
                viewModel.reportReview(reviewToReport!!.id, reason, comment)
                showReportReviewDialog = false
                reviewToReport = null
                scope.launch {
                    snackbarHostState.showSnackbar("Dziękujemy za zgłoszenie opinii!")
                }
            },
            onDismiss = {
                showReportReviewDialog = false
                reviewToReport = null
            }
        )
    }

    // Fullscreen photo viewer
    if (fullscreenPhotos.isNotEmpty()) {
        val myUserId = currentUser?.id
        com.kidzone.presentation.common.FullscreenPhotoViewer(
            photoUrls = fullscreenPhotos,
            initialIndex = fullscreenPhotoIndex,
            onDismiss = { fullscreenPhotos = emptyList() },
            onReportPhoto = if (fullscreenPhotosAreMine) null else { url ->
                photoUrlToReport = url
                showReportPhotoDialog = true
            },
            canReportPhoto = { url ->
                // Ukryj flagę na zdjęciach dodanych przez bieżącego usera
                val uploaderId = fullscreenPhotoUploadedBy[url]
                uploaderId == null || uploaderId != myUserId
            },
            onDeletePhoto = { url ->
                viewModel.deletePhotoFromPlace(url)
                // Viewer dismisses itself after deletion (onDismiss called inside)
            },
            canDeletePhoto = { url ->
                // Pokaż kosz tylko na zdjęciach dodanych przez bieżącego usera
                val uploaderId = fullscreenPhotoUploadedBy[url]
                myUserId != null && uploaderId == myUserId
            }
        )
    }

    // Report photo dialog
    if (showReportPhotoDialog && photoUrlToReport != null) {
        ReportPhotoDialog(
            onSubmit = { reason, comment ->
                viewModel.reportPhoto(photoUrlToReport!!, reason, comment)
                showReportPhotoDialog = false
                photoUrlToReport = null
                scope.launch {
                    snackbarHostState.showSnackbar("Dziękujemy za zgłoszenie zdjęcia!")
                }
            },
            onDismiss = {
                showReportPhotoDialog = false
                photoUrlToReport = null
            }
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
    topRank: Int?,
    sortOrder: PlaceDetailsViewModel.ReviewSortOrder,
    onSortOrderChange: (PlaceDetailsViewModel.ReviewSortOrder) -> Unit,
    onAddReview: () -> Unit,
    onEditReview: (Review) -> Unit,
    onReportReview: (Review) -> Unit,
    onOpenPhotoViewer: (photos: List<String>, startIndex: Int, areMine: Boolean) -> Unit = { _, _, _ -> },
    onAddPlacePhoto: (() -> Unit)? = null,
    onAddPlaceCamera: (() -> Unit)? = null,
    isUploadingPlacePhoto: Boolean = false
) {
    // Przycisk "Dodaj opinię" widoczny tylko gdy:
    //  - user jest zalogowany,
    //  - NIE jest właścicielem miejsca (nie oceniamy swoich miejsc),
    //  - jeszcze nie wystawił opinii (1 opinia per user per miejsce).
    val isOwner = currentUserId != null && place.ownerUserId == currentUserId
    val alreadyReviewed = currentUserId != null && reviews.any { it.userId == currentUserId }
    val canAddReview = currentUserId != null && !isOwner && !alreadyReviewed

    // Klient-side sort. `remember` z kluczami chroni przed niepotrzebnym
    // re-sortowaniem – wykonuje się tylko gdy zmieni się lista albo sortOrder.
    val sortedReviews = remember(reviews, sortOrder) {
        reviews.sortedWith(sortOrder.comparator)
    }

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
                author = author,
                currentUserId = currentUserId,
                topRank = topRank
            )
        }

        // Sekcja 1b: Zdjęcia miejsca
        if (place.photoUrls.isNotEmpty()) {
            item {
                PlacePhotoGallery(
                    photoUrls = place.photoUrls,
                    onPhotoClick = { index ->
                        // Zdjęcia miejsca – właściciel MOŻE zgłaszać (bo inni usery
                        // mogą dodawać zdjęcia do jego miejsca). Nie-właściciel też może.
                        // Jedyny case "areMine" to zdjęcia opinii autora.
                        onOpenPhotoViewer(place.photoUrls, index, false)
                    }
                )
            }
        }

        // Przycisk "Dodaj zdjęcie" – widoczny gdy < 5 zdjęć i user zalogowany
        if (onAddPlacePhoto != null && place.photoUrls.size < 5) {
            item {
                if (isUploadingPlacePhoto) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Przesyłanie zdjęcia...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAddPlacePhoto,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Galeria (${place.photoUrls.size}/5)")
                        }
                        if (onAddPlaceCamera != null) {
                            OutlinedButton(
                                onClick = onAddPlaceCamera,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Aparat")
                            }
                        }
                    }
                }
            }
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
                } else {
                    // Wykres rozkładu ocen pokazujemy od 3 opinii w górę –
                    // przy 1-2 wygląda jak prawie pusty placeholder i nie
                    // niesie żadnej informacji.
                    if (reviews.size >= 3) {
                        ReviewDistributionChart(reviews = reviews)
                        Spacer(Modifier.height(12.dp))
                    }
                    if (reviews.size >= 2) {
                        ReviewSortDropdown(
                            current = sortOrder,
                            onChange = onSortOrderChange
                        )
                    }
                }
            }
        }

        // Każda opinia jako osobny item, żeby LazyColumn dobrze recyklował przy długich listach
        items(items = sortedReviews, key = { it.id }) { review ->
            val isMine = currentUserId != null && review.userId == currentUserId
            ReviewCard(
                review = review,
                isMine = isMine,
                onEdit = if (isMine) {
                    { onEditReview(review) }
                } else null,
                onReport = if (!isMine && currentUserId != null) {
                    { onReportReview(review) }
                } else null,
                onPhotoClick = if (review.photoUrls.isNotEmpty()) {
                    { index ->
                        val isMyReview = currentUserId != null && review.userId == currentUserId
                        onOpenPhotoViewer(review.photoUrls, index, isMyReview)
                    }
                } else null
            )
        }
    }
}

/**
 * Główna karta szczegółów miejsca – "jedno okno" w którym po kolei są:
 *  1. nazwa + ikona kategorii + ocena (header) + (opc.) plakietka TOP 100,
 *  2. opis (jeśli niepusty),
 *  3. adres + współrzędne,
 *  4. mały przycisk „Nawiguj" wyrzucający do Google Maps w trybie
 *     turn-by-turn navigation (intent z `maps/dir/?api=1`),
 *  5. autor + data dodania (lub "Dodano przez Ciebie", gdy zalogowany user
 *     jest właścicielem - patrz [authorLine]).
 *
 * Bez sub-headerów typu "Opis"/"Lokalizacja" – wizualnie jeden spójny
 * blok, a delikatne dividery rozdzielają poszczególne kawałki.
 *
 * @param topRank pozycja w rankingu TOP 100 (1-based), tylko gdy <= 10.
 *   Null = nie pokazujemy plakietki.
 */
@Composable
private fun PlaceMainCard(
    place: Place,
    author: User?,
    currentUserId: String?,
    topRank: Int?
) {
    val style = place.category.style
    val context = LocalContext.current
    val isOwnerLine = currentUserId != null && currentUserId == place.ownerUserId

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- 1. Header: nazwa + kategoria + ocena + (opc.) plakietka TOP 100 ---
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
                // Plakietka rankingu - obok nazwy, jak gwiazdka jakości.
                // Pokazujemy tylko dla pierwszej dziesiątki TOP 100;
                // dla pozostałych miejsc nic nie renderujemy (brak Box-a).
                if (topRank != null) {
                    Spacer(Modifier.width(8.dp))
                    RankBadge(
                        rank = topRank,
                        label = "TOP 100",
                        modifier = Modifier.padding(horizontal = 4.dp)
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

            // Przycisk "Zobacz na Google Maps" – otwiera miejsce w Google Maps
            // w trybie search (query=lat,lng), dzięki czemu user widzi oceny
            // Google, godziny otwarcia, zdjęcia i inne szczegóły z ekosystemu
            // Google Maps, których nie mamy w kidZone.
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
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.view_on_google_maps))
            }

            // --- 4. Dodano przez ---
            // Dla zalogowanego usera-właściciela pokazujemy "Dodano przez Ciebie"
            // (z datą), zamiast jego własnego nicka - taka konwencja jest
            // czytelniejsza, bo użytkownik nie musi rozpoznawać samego siebie
            // w nagłówku miejsca.
            SoftDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                val datePart = place.createdAtMillis
                    .takeIf { it > 0L }
                    ?.let { " · " + formatDate(it) }
                    .orEmpty()
                val authorName = author?.name?.takeIf { it.isNotBlank() }
                Text(
                    text = when {
                        isOwnerLine -> "Dodano przez Ciebie$datePart"
                        authorName != null -> "Dodano przez $authorName$datePart"
                        else -> "Dodano przez nieznanego użytkownika$datePart"
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
    isMine: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onReport: (() -> Unit)? = null,
    onPhotoClick: ((index: Int) -> Unit)? = null
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
                    // Ołówek edycji – tylko dla własnej opinii. Klik otwiera
                    // ten sam sheet co "Dodaj opinię", ale w trybie edit
                    // (pre-filled aktualnymi wartościami).
                    if (onEdit != null) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edytuj swoją opinię",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    // Flaga zgłoszenia – tylko dla cudzych opinii.
                    if (onReport != null) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = onReport,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Flag,
                                contentDescription = "Zgłoś opinię",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            ReviewTimestampRow(
                createdAtMillis = review.createdAtMillis,
                updatedAtMillis = review.updatedAtMillis
            )
            if (review.comment.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // Zdjęcia opinii
            if (review.photoUrls.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ReviewPhotoRow(
                    photoUrls = review.photoUrls,
                    onPhotoClick = { index -> onPhotoClick?.invoke(index) }
                )
            }
        }
    }
}

/**
 * Wiersz z datą utworzenia opinii i opcjonalnym znacznikiem "edytowana".
 * Pokazujemy obie informacje, żeby nikt nie podmienił 1★ -> 5★ po cichu –
 * data edycji jest widoczna i niezatajalna.
 */
@Composable
private fun ReviewTimestampRow(
    createdAtMillis: Long,
    updatedAtMillis: Long
) {
    if (createdAtMillis <= 0L && updatedAtMillis <= 0L) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (createdAtMillis > 0L) {
            Text(
                text = formatDate(createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (updatedAtMillis > createdAtMillis && updatedAtMillis > 0L) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = "· edytowana ${formatDate(updatedAtMillis)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

/**
 * Wykres rozkładu opinii (Google Maps style): średnia + 5 wierszy
 * 5★/4★/3★/2★/1★ z poziomym paskiem proporcjonalnym do najwyższej liczby
 * w grupie. Renderujemy gdy reviews.size >= 3 (próg czytelności).
 */
@Composable
private fun ReviewDistributionChart(reviews: List<Review>) {
    val total = reviews.size
    if (total <= 0) return
    // Mapa rating(1..5) -> count. Nawet jeśli jakaś gwiazdka ma 0 wystąpień,
    // chcemy ją pokazać w wierszu (czytelność).
    val counts = (1..5).associateWith { star -> reviews.count { it.rating == star } }
    // Skalowanie pasków do najwyższej grupy (a nie totalu) – wizualnie
    // lepiej porównuje proporcje, jak robi to Google Maps.
    val maxCount = counts.values.max().coerceAtLeast(1)
    val avg = reviews.map { it.rating }.average()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "%.1f".format(avg),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "$total ${pluralOpinii(total)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        // Wiersze 5★..1★ – top-down (najwyższa ocena na górze).
        (5 downTo 1).forEach { star ->
            val count = counts.getValue(star)
            DistributionRow(
                star = star,
                count = count,
                fraction = count.toFloat() / maxCount
            )
        }
    }
}

@Composable
private fun DistributionRow(
    star: Int,
    count: Int,
    fraction: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$star",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(12.dp),
            textAlign = TextAlign.End
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
    }
}

/** Polska odmiana rzeczownika "opinia" w zależności od liczby. */
private fun pluralOpinii(n: Int): String = when {
    n == 1 -> "opinia"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "opinie"
    else -> "opinii"
}

/**
 * Mały selektor sortowania nad listą opinii (Najnowsze / Najstarsze /
 * Najwyżej / Najniżej oceniane). Renderowany jako TextButton z ikoną
 * sortowania + label aktualnego trybu + ArrowDropDown.
 */
@Composable
private fun ReviewSortDropdown(
    current: PlaceDetailsViewModel.ReviewSortOrder,
    onChange: (PlaceDetailsViewModel.ReviewSortOrder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(text = current.label)
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PlaceDetailsViewModel.ReviewSortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label) },
                    onClick = {
                        onChange(order)
                        expanded = false
                    }
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



@Composable
private fun ReportPlaceDialog(
    onSubmit: (reason: String, comment: String) -> Unit,
    onDismiss: () -> Unit
) {
    val reasons = listOf(
        "NOT_EXISTS" to "Miejsce nie istnieje / zamkni\u0119te",
        "INAPPROPRIATE" to "Nieodpowiednia tre\u015B\u0107",
        "DUPLICATE" to "Duplikat innego miejsca",
        "FALSE_DATA" to "Fa\u0142szywe dane (adres, udogodnienia)",
        "OTHER" to "Inne"
    )
    var selectedReason by remember { mutableStateOf(reasons.first().first) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Zg\u0142o\u015B miejsce") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Wybierz pow\u00F3d zg\u0142oszenia:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                reasons.forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = code }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedReason == code,
                            onClick = { selectedReason = code }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Komentarz (opcjonalny)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason, comment.trim()) }) {
                Text("Wy\u015Blij zg\u0142oszenie")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}



/**
 * Dialog zgłaszania opinii – analogiczny do [ReportPlaceDialog], ale
 * z powodami dostosowanymi do opinii (spam, obraźliwa treść itp.).
 */
@Composable
private fun ReportReviewDialog(
    authorName: String,
    onSubmit: (reason: String, comment: String) -> Unit,
    onDismiss: () -> Unit
) {
    val reasons = listOf(
        "SPAM" to "Spam / reklama",
        "OFFENSIVE" to "Obraźliwa treść",
        "FALSE_INFO" to "Fałszywe informacje",
        "NOT_RELEVANT" to "Nie dotyczy tego miejsca",
        "OTHER" to "Inne"
    )
    var selectedReason by remember { mutableStateOf(reasons.first().first) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Zgłoś opinię") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Zgłaszasz opinię użytkownika ${authorName.ifBlank { "Anonim" }}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Wybierz powód zgłoszenia:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                reasons.forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = code }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedReason == code,
                            onClick = { selectedReason = code }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Komentarz (opcjonalny)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason, comment.trim()) }) {
                Text("Wyślij zgłoszenie")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}


/**
 * Galeria zdjęć miejsca – pełnoszerokościowy LazyRow z miniaturami.
 * Klik na miniaturę otwiera powiększony podgląd fullscreen.
 */
@Composable
private fun PlacePhotoGallery(
    photoUrls: List<String>,
    onPhotoClick: (index: Int) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Zdjęcia (${photoUrls.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photoUrls.size) { index ->
                    coil.compose.AsyncImage(
                        model = photoUrls[index],
                        contentDescription = "Zdjęcie ${index + 1}",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .clickable { onPhotoClick(index) },
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }
        }
    }
}

/**
 * Wiersz miniaturek zdjęć w opinii – mniejsze niż w galerii miejsca.
 */
@Composable
private fun ReviewPhotoRow(
    photoUrls: List<String>,
    onPhotoClick: (index: Int) -> Unit = {}
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(photoUrls.size) { index ->
            coil.compose.AsyncImage(
                model = photoUrls[index],
                contentDescription = "Zdjęcie opinii ${index + 1}",
                modifier = Modifier
                    .size(72.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                    .clickable { onPhotoClick(index) },
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
    }
}


/**
 * Dialog zgłaszania zdjęcia – powody dostosowane do zdjęć
 * (nieodpowiednia treść, niezwiązane z miejscem, narusza prawa autorskie itp.).
 */
@Composable
private fun ReportPhotoDialog(
    onSubmit: (reason: String, comment: String) -> Unit,
    onDismiss: () -> Unit
) {
    val reasons = listOf(
        "INAPPROPRIATE" to "Nieodpowiednia treść",
        "NOT_RELEVANT" to "Niezwiązane z miejscem",
        "COPYRIGHT" to "Narusza prawa autorskie",
        "OFFENSIVE" to "Obraźliwe / wulgarne",
        "OTHER" to "Inne"
    )
    var selectedReason by remember { mutableStateOf(reasons.first().first) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Zgłoś zdjęcie") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Wybierz powód zgłoszenia:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                reasons.forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = code }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedReason == code,
                            onClick = { selectedReason = code }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Komentarz (opcjonalny)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason, comment.trim()) }) {
                Text("Wyślij zgłoszenie")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
