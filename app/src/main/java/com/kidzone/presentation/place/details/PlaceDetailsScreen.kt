package com.kidzone.presentation.place.details

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kidzone.R
import com.kidzone.domain.model.Amenity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.Review
import com.kidzone.domain.model.User
import com.kidzone.presentation.common.CategoryIcon
import com.kidzone.presentation.common.NewPlaceBadge
import com.kidzone.presentation.common.RankBadge
import com.kidzone.presentation.common.createCameraImageUri
import com.kidzone.presentation.common.isNewWithoutReviews
import com.kidzone.presentation.common.shimmerEffect
import com.kidzone.presentation.common.style
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val VERY_CLOSE_DISTANCE_LABEL = "Tuż obok"

/**
 * Szczegóły miejsca.
 */
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod", "FunctionNaming")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlaceDetailsScreen(
    onBack: () -> Unit,
    onEditPlace: (placeId: String) -> Unit,
    onDeleted: () -> Unit,
    placeId: String = "",
    viewModel: PlaceDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    animationSource: String? = null
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

    var fullscreenPhotos by remember { mutableStateOf<List<String>>(emptyList()) }
    var fullscreenPhotoIndex by remember { mutableStateOf(0) }
    var fullscreenPhotosAreMine by remember { mutableStateOf(false) }
    var fullscreenPhotoUploadedBy by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showReportPhotoDialog by remember { mutableStateOf(false) }
    var photoUrlToReport by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val placePhotoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.addPhotoToPlace(uri)
        }
    }

    var placeCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val placeCameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = placeCameraUriString?.let(Uri::parse)
        if (success && uri != null) {
            viewModel.addPhotoToPlace(uri)
        }
        placeCameraUriString = null
    }
    val cameraUnavailable = stringResource(R.string.camera_unavailable)
    fun launchPlaceCamera() {
        val uri = createCameraImageUri(context, "place_camera_")
        if (uri == null) {
            scope.launch {
                snackbarHostState.showSnackbar(cameraUnavailable)
            }
            return
        }
        placeCameraUriString = uri.toString()
        runCatching {
            placeCameraLauncher.launch(uri)
        }.onFailure {
            placeCameraUriString = null
            scope.launch {
                snackbarHostState.showSnackbar(cameraUnavailable)
            }
        }
    }
    val cameraAccessDenied = stringResource(R.string.camera_access_denied)
    val placeCameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchPlaceCamera()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(cameraAccessDenied)
            }
        }
    }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onDeleted()
    }

    LaunchedEffect(state.deleteErrorMessage) {
        val msg = state.deleteErrorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg.asString(context))
            viewModel.consumeDeleteError()
        }
    }

    val thankYouReview = stringResource(R.string.thank_you_review)
    val reviewUpdated = stringResource(R.string.review_updated)
    LaunchedEffect(state.reviewActionEvent) {
        val event = state.reviewActionEvent ?: return@LaunchedEffect
        val message = when (event) {
            PlaceDetailsViewModel.ReviewActionEvent.ADDED -> thankYouReview
            PlaceDetailsViewModel.ReviewActionEvent.UPDATED -> reviewUpdated
        }
        snackbarHostState.showSnackbar(message)
        viewModel.consumeReviewActionEvent()
    }

    val duplicatePhotoError = stringResource(R.string.duplicate_photo_error)
    LaunchedEffect(state.placePhotoDuplicateEvent) {
        if (state.placePhotoDuplicateEvent) {
            snackbarHostState.showSnackbar(duplicatePhotoError)
            viewModel.consumePlacePhotoDuplicateEvent()
        }
    }

    LaunchedEffect(state.reviewPhotoDuplicateEvent) {
        if (state.reviewPhotoDuplicateEvent) {
            snackbarHostState.showSnackbar(duplicatePhotoError)
            viewModel.consumeReviewPhotoDuplicateEvent()
        }
    }

    LaunchedEffect(state.shouldRequestReview) {
        if (state.shouldRequestReview) {
            val activity = context as? android.app.Activity
            if (activity != null) {
                com.kidzone.review.InAppReviewManager(context).launchReviewFlow(activity)
            }
            viewModel.consumeReviewRequest()
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state.place != null) {
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_actions))
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false }
                        ) {
                            if (isOwner) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit)) },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = {
                                        showOverflow = false
                                        state.place?.let { onEditPlace(it.id) }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete)) },
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
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share)) },
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
                                            append("\n\nhttps://playground-705e7162.web.app/place/${place.id}")
                                            append("?lat=${place.latitude}&lng=${place.longitude}")
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, place.name)
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(intent, context.getString(R.string.share_place_title))
                                        )
                                    }
                                }
                            )
                            if (!isOwner) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.suggest_edit)) },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = {
                                        showOverflow = false
                                        showSuggestEditSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.correct_location)) },
                                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                                    onClick = {
                                        showOverflow = false
                                        showLocationCorrectionDialog = true
                                    }
                                )
                                if (!state.isPlaceReported) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.report)) },
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
                    var showSkeleton by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(100)
                        showSkeleton = true
                    }
                    if (showSkeleton) {
                        PlaceDetailsSkeleton(
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            animationSource = animationSource,
                            placeId = placeId
                        )
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
                            text = state.errorMessage?.asString() ?: stringResource(R.string.error_load_place),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = viewModel::retry) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                else -> {
                    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                        isRefreshing = state.isLoading,
                        onRefresh = viewModel::refresh
                    ) {
                        PlaceDetailsContent(
                            place = state.place!!,
                            author = state.author,
                            reviews = state.reviews,
                            currentUserId = currentUser?.id,
                            topRank = state.topRank,
                            userLocation = state.userLocation,
                            staleLocationAgeMinutes = state.staleLocationAgeMinutes.takeIf {
                                state.isUsingStaleLocation
                            },
                            sortOrder = state.sortOrder,
                            reportedReviewIds = state.reportedReviewIds,
                            onSortOrderChange = viewModel::setSortOrder,
                            onAddReview = viewModel::openAddReviewSheet,
                            onEditReview = viewModel::openEditReviewSheet,
                            onDeleteReview = { review -> viewModel.deleteReview(review.id) },
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
                            isUploadingPlacePhoto = state.isUploadingPlacePhoto,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            animationSource = animationSource
                        )
                    }
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

    val thankYouReport = stringResource(R.string.thank_you_report)
    if (showReportDialog) {
        ReportPlaceDialog(
            onSubmit = { reason, comment ->
                viewModel.reportPlace(reason, comment)
                showReportDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(thankYouReport)
                }
            },
            onDismiss = { showReportDialog = false }
        )
    }

    if (showSuggestEditSheet && state.place != null) {
        val thankYouSuggestEdit = stringResource(R.string.thank_you_suggest_edit)
        SuggestEditSheet(
            place = state.place!!,
            onSubmit = { name, description, category, amenities ->
                viewModel.submitSuggestedEdit(name, description, category, amenities)
                showSuggestEditSheet = false
                scope.launch {
                    snackbarHostState.showSnackbar(thankYouSuggestEdit)
                }
            },
            onDismiss = { showSuggestEditSheet = false }
        )
    }

    if (showLocationCorrectionDialog && state.place != null) {
        val thankYouLocationCorrection = stringResource(R.string.thank_you_location_correction)
        LocationCorrectionDialog(
            onSubmit = { lat, lng, address ->
                viewModel.submitLocationCorrection(lat, lng, address)
                showLocationCorrectionDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(thankYouLocationCorrection)
                }
            },
            onDismiss = { showLocationCorrectionDialog = false }
        )
    }

    if (state.showAddReviewSheet) {
        val editing = state.editingReview
        AddReviewSheet(
            placeName = state.place?.name.orEmpty(),
            isSubmitting = state.isAddingReview,
            errorMessage = state.addReviewError?.asString(),
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

    if (showReportReviewDialog && reviewToReport != null) {
        val thankYouReportReview = stringResource(R.string.thank_you_report_review)
        ReportReviewDialog(
            authorName = reviewToReport!!.authorName,
            onSubmit = { reason, comment ->
                viewModel.reportReview(reviewToReport!!.id, reason, comment)
                showReportReviewDialog = false
                reviewToReport = null
                scope.launch {
                    snackbarHostState.showSnackbar(thankYouReportReview)
                }
            },
            onDismiss = {
                showReportReviewDialog = false
                reviewToReport = null
            }
        )
    }

    if (fullscreenPhotos.isNotEmpty()) {
        val myUserId = currentUser?.id
        com.kidzone.presentation.common.FullscreenPhotoViewer(
            photoUrls = fullscreenPhotos,
            initialIndex = fullscreenPhotoIndex,
            onDismiss = { fullscreenPhotos = emptyList() },
            onReportPhoto = if (fullscreenPhotosAreMine) null else { url ->
                if (!state.reportedPhotoUrls.contains(url)) {
                    photoUrlToReport = url
                    showReportPhotoDialog = true
                }
            },
            canReportPhoto = { url ->
                val uploaderId = fullscreenPhotoUploadedBy[url]
                val notMine = uploaderId == null || uploaderId != myUserId
                val notReported = !state.reportedPhotoUrls.contains(url)
                notMine && notReported
            },
            onDeletePhoto = { url ->
                viewModel.deletePhotoFromPlace(url)
            },
            canDeletePhoto = { url ->
                val uploaderId = fullscreenPhotoUploadedBy[url]
                myUserId != null && uploaderId == myUserId
            }
        )
    }

    if (showReportPhotoDialog && photoUrlToReport != null) {
        val thankYouReportPhoto = stringResource(R.string.thank_you_report_photo)
        ReportPhotoDialog(
            onSubmit = { reason, comment ->
                viewModel.reportPhoto(photoUrlToReport!!, reason, comment)
                showReportPhotoDialog = false
                photoUrlToReport = null
                scope.launch {
                    snackbarHostState.showSnackbar(thankYouReportPhoto)
                }
            },
            onDismiss = {
                showReportPhotoDialog = false
                photoUrlToReport = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun PlaceDetailsContent(
    place: Place,
    author: User?,
    reviews: List<Review>,
    currentUserId: String?,
    topRank: Int?,
    userLocation: Pair<Double, Double>?,
    staleLocationAgeMinutes: Int?,
    sortOrder: PlaceDetailsViewModel.ReviewSortOrder,
    reportedReviewIds: Set<String> = emptySet(),
    onSortOrderChange: (PlaceDetailsViewModel.ReviewSortOrder) -> Unit,
    onAddReview: () -> Unit,
    onEditReview: (Review) -> Unit,
    onDeleteReview: (Review) -> Unit,
    onReportReview: (Review) -> Unit,
    onOpenPhotoViewer: (photos: List<String>, startIndex: Int, areMine: Boolean) -> Unit = { _, _, _ -> },
    onAddPlacePhoto: (() -> Unit)? = null,
    onAddPlaceCamera: (() -> Unit)? = null,
    isUploadingPlacePhoto: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    animationSource: String? = null
) {
    val isOwner = currentUserId != null && place.ownerUserId == currentUserId
    val alreadyReviewed = currentUserId != null && reviews.any { it.userId == currentUserId }
    val canAddReview = currentUserId != null && !isOwner && !alreadyReviewed

    val sortedReviews = remember(reviews, sortOrder) {
        reviews.sortedWith(sortOrder.comparator)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PlaceMainCard(
                place = place,
                author = author,
                currentUserId = currentUserId,
                topRank = topRank,
                distanceKm = userLocation?.let { (lat, lng) ->
                    haversineKm(lat, lng, place.latitude, place.longitude)
                },
                staleLocationAgeMinutes = staleLocationAgeMinutes,
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
                animationSource = animationSource
            )
        }

        if (place.photoUrls.isNotEmpty()) {
            item {
                PlacePhotoGallery(
                    photoUrls = place.photoUrls,
                    onPhotoClick = { index ->
                        onOpenPhotoViewer(place.photoUrls, index, false)
                    }
                )
            }
        }

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
                            text = stringResource(R.string.uploading_photo),
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
                            Spacer(Modifier.width(6.6.dp))
                            Text(stringResource(R.string.gallery_with_count, place.photoUrls.size))
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
                                Spacer(Modifier.width(6.6.dp))
                                Text(stringResource(R.string.camera))
                            }
                        }
                    }
                }
            }
        }

        if (place.amenities.isNotEmpty()) {
            item {
                SectionCard(title = stringResource(R.string.amenities_with_count, place.amenities.size)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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

        item {
            SectionCard(
                title = stringResource(R.string.reviews_with_count, reviews.size),
                trailing = if (canAddReview) {
                    {
                        TextButton(onClick = onAddReview) {
                            Text(stringResource(R.string.add_review))
                        }
                    }
                } else null
            ) {
                if (reviews.isEmpty()) {
                    Text(
                        text = if (canAddReview) {
                            stringResource(R.string.no_reviews_be_first)
                        } else {
                            stringResource(R.string.no_reviews)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
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

        items(items = sortedReviews, key = { it.id }) { review ->
            val isMine = currentUserId != null && review.userId == currentUserId
            ReviewCard(
                review = review,
                isMine = isMine,
                onEdit = if (isMine) {
                    { onEditReview(review) }
                } else null,
                onDelete = if (isMine) {
                    { onDeleteReview(review) }
                } else null,
                onReport = if (!isMine && currentUserId != null && !reportedReviewIds.contains(review.id)) {
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PlaceMainCard(
    place: Place,
    author: User?,
    currentUserId: String?,
    topRank: Int?,
    distanceKm: Double?,
    staleLocationAgeMinutes: Int?,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    animationSource: String? = null
) {
    val context = LocalContext.current
    val isOwnerLine = currentUserId != null && currentUserId == place.ownerUserId
    val keyPrefix = animationSource?.let { "${it}_" } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(
                    category = place.category,
                    animationKey = "${keyPrefix}place_icon_${place.id}",
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                    size = 36.dp,
                    iconSize = 22.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(place.category.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (topRank != null) {
                    Spacer(Modifier.width(8.dp))
                    RankBadge(
                        rank = topRank,
                        label = stringResource(R.string.top_100_label),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            PlaceDetailsRatingStatus(place = place)

            if (place.description.isNotBlank()) {
                MainCardDivider()
                Text(
                    text = place.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            MainCardDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = stringResource(R.string.map_location_banner_text),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.address.ifBlank { stringResource(R.string.address_unavailable) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.5f, %.5f".format(place.latitude, place.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            contentDescription = stringResource(R.string.navigate)
                        )
                    }

                    if (distanceKm != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = formatDistance(distanceKm, staleLocationAgeMinutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
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
                Spacer(Modifier.width(6.6.dp))
                Text(stringResource(R.string.view_on_google_maps))
            }

            MainCardDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.6.dp))
                val datePart = place.createdAtMillis
                    .takeIf { it > 0L }
                    ?.let { formatDate(it) }
                    .orEmpty()
                val authorName = author?.name?.takeIf { it.isNotBlank() }
                Text(
                    text = when {
                        isOwnerLine -> stringResource(
                            R.string.added_by_you_with_date,
                            " · $datePart"
                        )
                        authorName != null -> stringResource(
                            R.string.added_by_author_with_date,
                            authorName,
                            " · $datePart"
                        )
                        else -> stringResource(
                            R.string.added_by_unknown_with_date,
                            " · $datePart"
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlaceDetailsRatingStatus(place: Place) {
    when {
        place.reviewsCount > 0 -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.rating),
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "%.1f".format(place.averageRating),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                val reviewsCountText = pluralStringResource(
                    R.plurals.reviews_count,
                    place.reviewsCount,
                    place.reviewsCount
                )
                Text(
                    text = stringResource(R.string.reviews_count_short, reviewsCountText),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        place.isNewWithoutReviews() -> {
            NewPlaceBadge()
        }

        else -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.rating),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.map_no_reviews),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun formatDistance(
    km: Double,
    staleLocationAgeMinutes: Int? = null
): String {
    val distance = when {
        km < 0.05 -> VERY_CLOSE_DISTANCE_LABEL
        km < 1.0 -> {
            val meters = (km * 1000).toInt()
            val rounded = ((meters + 25) / 50) * 50
            if (rounded == 0) VERY_CLOSE_DISTANCE_LABEL else stringResource(R.string.distance_m, rounded)
        }
        km < 100.0 -> stringResource(R.string.distance_km, km)
        else -> stringResource(R.string.distance_km_integer, km.toInt())
    }

    return staleLocationAgeMinutes?.let { "$distance (${staleAgeLabel(it)})" } ?: distance
}

private fun staleAgeLabel(ageMinutes: Int): String = when {
    ageMinutes <= 1 -> "1 min temu"
    else -> "$ageMinutes min temu"
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2).let { it * it }
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return r * c
}

@Composable
private fun SoftDivider() {
    Spacer(Modifier.height(12.dp))
    androidx.compose.material3.HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun MainCardDivider() {
    Spacer(Modifier.height(8.dp))
    androidx.compose.material3.HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
    Spacer(Modifier.height(8.dp))
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PlaceDetailsSkeleton(
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    animationSource: String? = null,
    placeId: String = ""
) {
    val keyPrefix = animationSource?.let { "${it}_" } ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(
                        category = com.kidzone.domain.model.PlaceCategory.OTHER,
                        animationKey = if (placeId.isNotBlank()) "${keyPrefix}place_icon_$placeId" else null,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope,
                        size = 36.dp,
                        iconSize = 22.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .width(200.dp)
                                .height(24.dp)
                                .clip(MaterialTheme.shapes.small)
                                .shimmerEffect()
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(16.dp)
                                .clip(MaterialTheme.shapes.small)
                                .shimmerEffect()
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(20.dp)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect()
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect()
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(16.dp)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
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
    onDelete: (() -> Unit)? = null,
    onReport: (() -> Unit)? = null,
    onPhotoClick: ((index: Int) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                    text = review.authorName.ifBlank { stringResource(R.string.anonymous) },
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
                            contentDescription = if (index < review.rating) {
                                stringResource(R.string.star_selected, index + 1)
                            } else {
                                stringResource(R.string.star_not_selected, index + 1)
                            },
                            tint = if (index < review.rating) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (onEdit != null) {
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit_your_review),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete_your_review),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (onReport != null) {
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onReport,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = stringResource(R.string.report_review),
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
        if (review.photoUrls.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            ReviewPhotoRow(
                photoUrls = review.photoUrls,
                onPhotoClick = { index -> onPhotoClick?.invoke(index) }
            )
        }
    }
}

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
            Spacer(Modifier.width(6.6.dp))
            Text(
                text = stringResource(R.string.edited_with_date, formatDate(updatedAtMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
private fun ReviewDistributionChart(reviews: List<Review>) {
    val total = reviews.size
    if (total <= 0) return
    val counts = (1..5).associateWith { star -> reviews.count { it.rating == star } }
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
                contentDescription = stringResource(R.string.average_rating),
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.weight(1f))
            val reviewsCountText = pluralStringResource(R.plurals.reviews_count, total, total)
            Text(
                text = reviewsCountText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
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
            contentDescription = stringResource(R.string.star_count_label, star),
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.tertiary,
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
            Text(text = current.getLabel())
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
                    text = { Text(order.getLabel()) },
                    onClick = {
                        onChange(order)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MyReviewBadge() {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Text(
            text = stringResource(R.string.your_review),
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
        title = { Text(stringResource(R.string.delete_place_title)) },
        text = {
            Text(
                text = if (placeName.isNotBlank()) {
                    stringResource(R.string.delete_place_confirmation, placeName)
                } else {
                    stringResource(R.string.delete_place_confirmation_generic)
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
                    Text(stringResource(R.string.delete))
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text(stringResource(R.string.cancel))
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
        "NOT_EXISTS" to stringResource(R.string.report_reason_not_exists),
        "INAPPROPRIATE" to stringResource(R.string.report_reason_inappropriate),
        "DUPLICATE" to stringResource(R.string.report_reason_duplicate),
        "FALSE_DATA" to stringResource(R.string.report_reason_false_data),
        "OTHER" to stringResource(R.string.report_reason_other)
    )
    var selectedReason by remember { mutableStateOf(reasons.first().first) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Flag,
                contentDescription = stringResource(R.string.report),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.report)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.report_choose_reason),
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
                    label = { Text(stringResource(R.string.report_comment_label)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason, comment.trim()) }) {
                Text(stringResource(R.string.report_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ReportReviewDialog(
    authorName: String,
    onSubmit: (reason: String, comment: String) -> Unit,
    onDismiss: () -> Unit
) {
    val reasons = listOf(
        "SPAM" to stringResource(R.string.report_reason_spam),
        "OFFENSIVE" to stringResource(R.string.report_reason_offensive),
        "FALSE_INFO" to stringResource(R.string.report_reason_false_info),
        "NOT_RELEVANT" to stringResource(R.string.report_reason_not_relevant),
        "OTHER" to stringResource(R.string.report_reason_other)
    )
    var selectedReason by remember { mutableStateOf(reasons.first().first) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Flag,
                contentDescription = stringResource(R.string.report_review),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.report_review)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val reviewContext = stringResource(
                    R.string.report_review_context,
                    authorName.ifBlank { stringResource(R.string.anonymous) }
                )
                Text(
                    text = reviewContext,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.report_choose_reason),
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
                    label = { Text(stringResource(R.string.report_comment_label)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason, comment.trim()) }) {
                Text(stringResource(R.string.report_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

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
                text = stringResource(R.string.photos_with_count, photoUrls.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photoUrls.size) { index ->
                    val contentDesc = stringResource(R.string.photo_index, index + 1)
                    coil.compose.AsyncImage(
                        model = photoUrls[index],
                        contentDescription = contentDesc,
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

@Composable
private fun ReviewPhotoRow(
    photoUrls: List<String>,
    onPhotoClick: (index: Int) -> Unit = {}
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(photoUrls.size) { index ->
            val contentDesc = stringResource(R.string.review_photo_index, index + 1)
            coil.compose.AsyncImage(
                model = photoUrls[index],
                contentDescription = contentDesc,
                modifier = Modifier
                    .size(72.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                    .clickable { onPhotoClick(index) },
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
    }
}

@Composable
private fun ReportPhotoDialog(
    onSubmit: (reason: String, comment: String) -> Unit,
    onDismiss: () -> Unit
) {
    val reasons = listOf(
        "INAPPROPRIATE" to stringResource(R.string.report_reason_inappropriate),
        "NOT_RELEVANT" to stringResource(R.string.report_reason_not_related),
        "COPYRIGHT" to stringResource(R.string.report_reason_copyright),
        "OFFENSIVE" to stringResource(R.string.report_reason_offensive_vulgar),
        "OTHER" to stringResource(R.string.report_reason_other)
    )
    var selectedReason by remember { mutableStateOf(reasons.first().first) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Flag,
                contentDescription = stringResource(R.string.report_photo),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.report_photo)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.report_choose_reason),
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
                    label = { Text(stringResource(R.string.report_comment_label)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason, comment.trim()) }) {
                Text(stringResource(R.string.report_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
