@file:Suppress("CyclomaticComplexMethod", "FunctionNaming", "LongMethod", "LongParameterList")

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.kidzone.analytics.PlaceReportReason
import com.kidzone.analytics.ReviewReportReason
import com.kidzone.analytics.PhotoReportReason
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.Review
import com.kidzone.domain.model.User
import com.kidzone.presentation.common.CategoryIcon
import com.kidzone.presentation.common.formatDistance
import com.kidzone.presentation.common.FullscreenPhotoAction
import com.kidzone.presentation.common.KidZoneActionDialog
import com.kidzone.presentation.common.KidZoneDropdownMenuItem
import com.kidzone.presentation.common.KidZoneReportDialog
import com.kidzone.presentation.common.KidZoneSortMenu
import com.kidzone.presentation.common.NewPlaceBadge
import com.kidzone.presentation.common.PlaceRatingStatus
import com.kidzone.presentation.common.NetworkStatus
import com.kidzone.presentation.common.OfflineAwareSubmitButton
import com.kidzone.presentation.common.RankBadge
import com.kidzone.presentation.common.ScreenState
import com.kidzone.presentation.common.ScreenStateContent
import com.kidzone.presentation.common.SortMenuIcon
import com.kidzone.presentation.common.SortMenuOption
import com.kidzone.presentation.common.createCameraImageUri
import com.kidzone.presentation.common.isNewWithoutReviews
import com.kidzone.presentation.common.rememberNetworkStatus
import com.kidzone.presentation.common.shimmerEffect
import com.kidzone.presentation.common.RatingIcon
import com.kidzone.presentation.common.selectUniquePhotoUris
import com.kidzone.presentation.common.requestCameraPermissionOrOpenSettings
import com.kidzone.utils.DateUtils
import com.kidzone.utils.GeoUtils
import com.kidzone.utils.NumberUtils
import java.util.Locale

private val PLACE_DETAILS_SECTION_SPACING = 12.dp
private val PLACE_DETAILS_CONTENT_PADDING = 16.dp
private val PLACE_DETAILS_CARD_PADDING = 14.dp
private val PLACE_DETAILS_SMALL_SPACING = 6.dp
private val PLACE_DETAILS_TINY_SPACING = 2.dp

private val PLACE_DETAILS_CARD_ELEVATION = 1.dp
private val PLACE_DETAILS_MAIN_CARD_ELEVATION = 2.dp
private val PLACE_DETAILS_PROGRESS_SIZE = 24.dp

private val PLACE_DETAILS_ICON_SIZE = 18.dp
private val PLACE_DETAILS_AVERAGE_RATING_ICON_SIZE = 20.dp
private val PLACE_DETAILS_CATEGORY_ICON_SIZE = 36.dp
private val PLACE_DETAILS_CATEGORY_INNER_ICON_SIZE = 22.dp
private val PLACE_DETAILS_BADGE_HORIZONTAL_PADDING = 4.dp
private val PLACE_DETAILS_CHIP_HORIZONTAL_PADDING = 8.dp
private val PLACE_DETAILS_CHIP_CONTENT_SPACING = 4.dp
private val PLACE_DETAILS_REVIEW_ACTION_BUTTON_SIZE = 30.dp
private val PLACE_DETAILS_REVIEW_ACTION_ICON_SIZE = 16.dp
private val PLACE_DETAILS_REVIEW_STAR_SIZE = 15.dp
private val PLACE_DETAILS_REVIEW_OWN_ICON_SIZE = 14.dp
private const val PLACE_DETAILS_REVIEW_OWN_ALPHA = 0.7f
private const val PLACE_DETAILS_REVIEW_ACTION_ALPHA = 0.55f
private val PLACE_DETAILS_REVIEW_CARD_ELEVATION = 0.dp

private const val PLACE_DETAILS_MY_REVIEW_BACKGROUND_ALPHA = 0.06f
private const val PLACE_DETAILS_DISABLED_STAR_ALPHA = 0.3f
private val PLACE_DETAILS_DISTRIBUTION_STAR_COLUMN_WIDTH = 12.dp
private val PLACE_DETAILS_DISTRIBUTION_COUNT_COLUMN_WIDTH = 28.dp
private val PLACE_DETAILS_DISTRIBUTION_BAR_HEIGHT = 6.dp
private val PLACE_DETAILS_DISTRIBUTION_BAR_RADIUS = 3.dp
private val PLACE_DETAILS_DISTRIBUTION_ICON_SIZE = 12.dp

private val PLACE_DETAILS_DIALOG_PROGRESS_STROKE_WIDTH = 2.dp

private val PLACE_DETAILS_PLACE_PHOTO_SIZE = 120.dp
private val PLACE_DETAILS_REVIEW_PHOTO_SIZE = 64.dp
private val PLACE_DETAILS_PHOTO_CORNER_RADIUS = 8.dp

private val PLACE_DETAILS_SKELETON_LARGE_WIDTH = 200.dp
private val PLACE_DETAILS_SKELETON_MEDIUM_WIDTH = 120.dp
private val PLACE_DETAILS_SKELETON_SMALL_WIDTH = 100.dp
private val PLACE_DETAILS_SKELETON_TINY_WIDTH = 80.dp
private val PLACE_DETAILS_SKELETON_CHIP_WIDTH = 70.dp
private val PLACE_DETAILS_SKELETON_TITLE_HEIGHT = 24.dp
private val PLACE_DETAILS_SKELETON_TEXT_HEIGHT = 16.dp
private val PLACE_DETAILS_SKELETON_RATING_HEIGHT = 20.dp
private val PLACE_DETAILS_SKELETON_DESCRIPTION_HEIGHT = 80.dp
private val PLACE_DETAILS_SKELETON_CHIP_HEIGHT = 32.dp
private val PLACE_DETAILS_SKELETON_PHOTO_SIZE = 100.dp
private val PLACE_DETAILS_SKELETON_CHIP_RADIUS = 16.dp

private val PLACE_DETAILS_DIVIDER_THICKNESS = 1.dp
private const val PLACE_DETAILS_DIVIDER_ALPHA = 0.5f

private const val PLACE_DETAILS_MAX_PHOTOS = 5

/**
 * 🎯 Odpowiedzialności:
 * - Prezentacja szczegółowych informacji o wybranym miejscu.
 * - Wyświetlanie galerii zdjęć, udogodnień oraz listy opinii.
 * - Obsługa interakcji: dodawanie opinii, zgłaszanie błędów, udostępnianie.
 *
 * 📥 Wejście:
 * - [onBack] powrót do poprzedniego ekranu.
 * - [onEditPlace] przejście do edycji miejsca.
 * - [onDeleted] powrót po usunięciu miejsca.
 * - [placeId] identyfikator wyświetlanego miejsca.
 *
 * 📤 Wyjście:
 * - Zdarzenia nawigacji oraz operacje zapisu opinii/zgłoszeń w Firestore.
 */
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod", "FunctionNaming")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlaceDetailsScreen(
    onBack: () -> Unit,
    onEditPlace: (placeId: String) -> Unit,
    onDeleted: () -> Unit,
    onSignOut: () -> Unit,
    placeId: String = "",
    viewModel: PlaceDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    animationSource: String? = null
) {
    val state by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val currentPlace = (state.screenState as? ScreenState.Content)?.data
    val isOwner = remember(currentPlace, currentUser) {
        currentPlace != null && currentUser != null && currentPlace.ownerUserId == currentUser?.id
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
    var fullscreenPhotoUploadedBy by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var fullscreenReview by remember { mutableStateOf<Review?>(null) }
    var showReportPhotoDialog by remember { mutableStateOf(false) }
    var photoUrlToReport by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val networkStatus by rememberNetworkStatus()
    val isOffline = networkStatus == NetworkStatus.UNAVAILABLE

    val availablePlacePhotoSlots = (
        PLACE_DETAILS_MAX_PHOTOS - ((state.screenState as? ScreenState.Content)?.data?.photoUrls?.size ?: 0)
    ).coerceAtLeast(0)
    val duplicatePhotoError = stringResource(R.string.duplicate_photo_error)

    fun addPickedPlacePhotos(uris: List<Uri>) {
        val currentPlace = (state.screenState as? ScreenState.Content)?.data ?: return
        val availableSlots = PLACE_DETAILS_MAX_PHOTOS - currentPlace.photoUrls.size
        if (uris.isEmpty() || availableSlots <= 0) return

        val selection = selectUniquePhotoUris(
            context = context,
            uris = uris,
            availableSlots = availableSlots
        )

        if (selection.acceptedUris.isNotEmpty()) {
            viewModel.addPhotosToPlace(selection.acceptedUris)
        }
        if (selection.duplicatesFound > 0) {
            scope.launch {
                snackbarHostState.showSnackbar(duplicatePhotoError)
            }
        }
    }

    val placePhotoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(
            availablePlacePhotoSlots.coerceAtLeast(2)
        )
    ) { uris ->
        addPickedPlacePhotos(uris)
    }
    val singlePlacePhotoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) addPickedPlacePhotos(listOf(uri))
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
            val currentPlace = (state.screenState as? ScreenState.Content)?.data
            TopAppBar(
                title = {
                    Text(
                        text = currentPlace?.name ?: stringResource(R.string.place_details),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (currentPlace != null) {
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_actions))
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false }
                        ) {
                            if (isOwner) {
                                KidZoneDropdownMenuItem(
                                    text = stringResource(R.string.edit),
                                    leadingIcon = Icons.Filled.Edit,
                                    onClick = {
                                        showOverflow = false
                                        onEditPlace(currentPlace.id)
                                    }
                                )
                                KidZoneDropdownMenuItem(
                                    text = stringResource(R.string.delete),
                                    leadingIcon = Icons.Filled.Delete,
                                    iconTint = MaterialTheme.colorScheme.error,
                                    onClick = {
                                        showOverflow = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                            KidZoneDropdownMenuItem(
                                text = stringResource(R.string.share),
                                leadingIcon = Icons.Filled.Share,
                                onClick = {
                                    showOverflow = false
                                    val shareText = buildString {
                                        append(currentPlace.name)
                                        if (currentPlace.address.isNotBlank()) {
                                            append("\n")
                                            append(currentPlace.address)
                                        }
                                        append("\n\nhttps://playground-705e7162.web.app/place/${currentPlace.id}")
                                        append("?lat=${currentPlace.latitude}&lng=${currentPlace.longitude}")
                                    }
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, currentPlace.name)
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(intent, context.getString(R.string.share_place_title))
                                    )
                                }
                            )
                            if (!isOwner) {
                                KidZoneDropdownMenuItem(
                                    text = stringResource(R.string.suggest_edit),
                                    leadingIcon = Icons.Filled.Edit,
                                    onClick = {
                                        showOverflow = false
                                        showSuggestEditSheet = true
                                    }
                                )
                                KidZoneDropdownMenuItem(
                                    text = stringResource(R.string.correct_location),
                                    leadingIcon = Icons.Filled.LocationOn,
                                    onClick = {
                                        showOverflow = false
                                        showLocationCorrectionDialog = true
                                    }
                                )
                                if (!state.isPlaceReported) {
                                    KidZoneDropdownMenuItem(
                                        text = stringResource(R.string.report),
                                        leadingIcon = Icons.Filled.Flag,
                                        iconTint = MaterialTheme.colorScheme.error,
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
            ScreenStateContent(
                state = state.screenState,
                onRetry = viewModel::retry,
                loading = {
                    PlaceDetailsSkeleton(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope,
                        animationSource = animationSource,
                        placeId = placeId
                    )
                },
                fallbackActionLabel = stringResource(R.string.sign_out),
                onFallbackAction = { viewModel.signOut(onSignOut) },
                content = { place ->
                    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                        isRefreshing = state.screenState is ScreenState.Loading,
                        onRefresh = viewModel::refresh
                    ) {
                        PlaceDetailsContent(
                            place = place,
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
                            onOpenPhotoViewer = { photos, index, review ->
                                fullscreenPhotos = photos
                                fullscreenPhotoIndex = index
                                fullscreenReview = review
                                fullscreenPhotoUploadedBy = place.photoUploadedBy
                            },
                            onAddPlacePhoto = if (currentUser != null) {
                                {
                                    val request = androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                    when {
                                        availablePlacePhotoSlots <= 0 -> Unit
                                        availablePlacePhotoSlots == 1 -> singlePlacePhotoPickerLauncher.launch(
                                            request
                                        )
                                        else -> placePhotoPickerLauncher.launch(
                                            request
                                        )
                                    }
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
                                        requestCameraPermissionOrOpenSettings(
                                            context = context,
                                            requestPermission = {
                                                placeCameraPermissionLauncher.launch(
                                                    android.Manifest.permission.CAMERA
                                                )
                                            }
                                        )
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
            )
        }
    }

    if (showDeleteDialog) {
        val currentPlace = (state.screenState as? ScreenState.Content)?.data
        DeleteConfirmationDialog(
            placeName = currentPlace?.name.orEmpty(),
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
            isOffline = isOffline,
            onSubmit = { reason, comment ->
                val typedReason = runCatching {
                    PlaceReportReason.valueOf(reason)
                }.getOrElse {
                    PlaceReportReason.OTHER
                }

                viewModel.reportPlace(
                    reason = typedReason,
                    comment = comment
                )

                showReportDialog = false

                scope.launch {
                    snackbarHostState.showSnackbar(thankYouReport)
                }
            },
            onDismiss = { showReportDialog = false }
        )
    }

    if (showSuggestEditSheet) {
        val currentPlace = (state.screenState as? ScreenState.Content)?.data
        if (currentPlace != null) {
            val thankYouSuggestEdit = stringResource(R.string.thank_you_suggest_edit)
            SuggestEditSheet(
                place = currentPlace,
                onSubmit = { name, description, category, amenities, comment ->
                    viewModel.submitSuggestedEdit(name, description, category, amenities, comment)
                    showSuggestEditSheet = false
                    scope.launch {
                        snackbarHostState.showSnackbar(thankYouSuggestEdit)
                    }
                },
                onDismiss = { showSuggestEditSheet = false }
            )
        }
    }

    if (showLocationCorrectionDialog) {
        val currentPlace = (state.screenState as? ScreenState.Content)?.data
        if (currentPlace != null) {
            val thankYouLocationCorrection = stringResource(R.string.thank_you_location_correction)
            LocationCorrectionDialog(
                isOffline = isOffline,
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
    }

    if (state.showAddReviewSheet) {
        val currentPlace = (state.screenState as? ScreenState.Content)?.data
        val editing = state.editingReview
        AddReviewSheet(
            placeName = currentPlace?.name.orEmpty(),
            isSubmitting = state.isAddingReview,
            errorMessage = state.addReviewError?.asString(),
            onDismiss = viewModel::dismissAddReviewSheet,
            onSubmit = { rating, comment, photoUris, retainedUrls ->
                viewModel.submitReview(rating, comment, photoUris, retainedUrls)
            },
            isOffline = isOffline,
            initialRating = editing?.rating ?: 0,
            initialComment = editing?.comment.orEmpty(),
            initialPhotoUrls = editing?.photoUrls.orEmpty(),
            initialPhotoHashes = editing?.photoHashes.orEmpty(),
            isEditing = editing != null
        )
    }

    if (showReportReviewDialog && reviewToReport != null) {
        val thankYouReportReview = stringResource(R.string.thank_you_report_review)
        ReportReviewDialog(
            authorName = reviewToReport!!.authorName,
            isOffline = isOffline,
            onSubmit = { reason, comment ->
                val typedReason = runCatching {
                    ReviewReportReason.valueOf(reason)
                }.getOrElse {
                    ReviewReportReason.OTHER
                }

                viewModel.reportReview(
                    reviewId = reviewToReport!!.id,
                    reason = typedReason,
                    comment = comment
                )
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
            onDismiss = {
                fullscreenPhotos = emptyList()
                fullscreenReview = null
            },
            onReportPhoto = { url ->
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
                val review = fullscreenReview
                if (review != null) {
                    viewModel.deletePhotoFromReview(review, url)
                } else {
                    viewModel.deletePhotoFromPlace(url)
                }
            },
            canDeletePhoto = { url ->
                val review = fullscreenReview
                if (review != null) {
                    myUserId != null && review.userId == myUserId && url in review.photoUrls
                } else {
                    val uploaderId = fullscreenPhotoUploadedBy[url]
                    myUserId != null && uploaderId == myUserId
                }
            },
            photoAction = { url ->
                val review = fullscreenReview
                val canDelete = if (review != null) {
                    myUserId != null && review.userId == myUserId && url in review.photoUrls
                } else {
                    val uploaderId = fullscreenPhotoUploadedBy[url]
                    myUserId != null && uploaderId == myUserId
                }
                when {
                    canDelete -> FullscreenPhotoAction.DELETE
                    url !in state.reportedPhotoUrls -> FullscreenPhotoAction.REPORT
                    else -> FullscreenPhotoAction.NONE
                }
            }
        )
    }

    if (showReportPhotoDialog && photoUrlToReport != null) {
        val thankYouReportPhoto = stringResource(R.string.thank_you_report_photo)

        ReportPhotoDialog(
            isOffline = isOffline,
            onSubmit = { reason, comment ->
                val typedReason = runCatching {
                    PhotoReportReason.valueOf(reason)
                }.getOrElse {
                    PhotoReportReason.OTHER
                }

                viewModel.reportPhoto(
                    photoUrl = photoUrlToReport!!,
                    reason = typedReason,
                    comment = comment
                )

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
    onOpenPhotoViewer: (
        photos: List<String>,
        startIndex: Int,
        review: Review?
    ) -> Unit = { _, _, _ -> },
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
        contentPadding = PaddingValues(
            horizontal = PLACE_DETAILS_CONTENT_PADDING,
            vertical = PLACE_DETAILS_SECTION_SPACING
        ),
        verticalArrangement = Arrangement.spacedBy(PLACE_DETAILS_SECTION_SPACING)
    ) {
        item {
            PlaceMainCard(
                place = place,
                author = author,
                currentUserId = currentUserId,
                topRank = topRank,
                distanceKm = userLocation?.let { (lat, lng) ->
                    GeoUtils.haversineKm(lat, lng, place.latitude, place.longitude)
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
                        onOpenPhotoViewer(place.photoUrls, index, null)
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
                        CircularProgressIndicator(modifier = Modifier.size(PLACE_DETAILS_PROGRESS_SIZE))
                        Spacer(Modifier.width(PLACE_DETAILS_CHIP_HORIZONTAL_PADDING))
                        Text(
                            text = stringResource(R.string.uploading_photo),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PLACE_DETAILS_CHIP_HORIZONTAL_PADDING)
                    ) {
                        OutlinedButton(
                            onClick = onAddPlacePhoto,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(PLACE_DETAILS_ICON_SIZE)
                            )
                            Spacer(Modifier.width(PLACE_DETAILS_SMALL_SPACING))
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
                                    modifier = Modifier.size(PLACE_DETAILS_ICON_SIZE)
                                )
                                Spacer(Modifier.width(PLACE_DETAILS_SMALL_SPACING))
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
                    com.kidzone.presentation.common.AmenitiesFlowGrid(
                        amenities = place.amenities.toList()
                    )
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
                        Spacer(Modifier.height(PLACE_DETAILS_SECTION_SPACING))
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
                        onOpenPhotoViewer(
                            review.photoUrls,
                            index,
                            review
                        )
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
        elevation = CardDefaults.cardElevation(defaultElevation = PLACE_DETAILS_REVIEW_CARD_ELEVATION)
    ) {
        Column(modifier = Modifier.padding(PLACE_DETAILS_CARD_PADDING)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIcon(
                    category = place.category,
                    animationKey = "${keyPrefix}place_icon_${place.id}",
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                    size = PLACE_DETAILS_CATEGORY_ICON_SIZE,
                    iconSize = PLACE_DETAILS_CATEGORY_INNER_ICON_SIZE
                )
                Spacer(Modifier.width(PLACE_DETAILS_SECTION_SPACING))
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
                    Spacer(Modifier.width(PLACE_DETAILS_CHIP_HORIZONTAL_PADDING))
                    RankBadge(
                        rank = topRank,
                        label = stringResource(R.string.top_100_label),
                        modifier = Modifier.padding(horizontal = PLACE_DETAILS_BADGE_HORIZONTAL_PADDING)
                    )
                }
            }
            Spacer(Modifier.height(PLACE_DETAILS_SECTION_SPACING))

            PlaceRatingStatus(
                place = place,
                iconSize = PLACE_DETAILS_ICON_SIZE
            )

            if (place.description.isNotBlank()) {
                MainCardDivider()
                Text(
                    text = place.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            MainCardDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = stringResource(R.string.map_location_banner_text),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(PLACE_DETAILS_ICON_SIZE)
                )
                Spacer(Modifier.width(PLACE_DETAILS_SMALL_SPACING))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.address.ifBlank { stringResource(R.string.address_unavailable) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = GeoUtils.formatCoordinates(place.latitude, place.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(PLACE_DETAILS_CHIP_HORIZONTAL_PADDING))

                Column(horizontalAlignment = Alignment.End) {
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

                    distanceKm?.let { distance ->
                        Spacer(Modifier.height(PLACE_DETAILS_TINY_SPACING))
                        Text(
                            text = formatDistance(distance, staleLocationAgeMinutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.End,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(PLACE_DETAILS_SMALL_SPACING))
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
                    modifier = Modifier.size(PLACE_DETAILS_ICON_SIZE)
                )
                Spacer(Modifier.width(PLACE_DETAILS_SMALL_SPACING))
                Text(stringResource(R.string.view_on_google_maps))
            }

            Spacer(Modifier.height(PLACE_DETAILS_SMALL_SPACING))
            androidx.compose.material3.HorizontalDivider(
                thickness = PLACE_DETAILS_DIVIDER_THICKNESS,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = PLACE_DETAILS_DIVIDER_ALPHA)
            )
            Spacer(Modifier.height(PLACE_DETAILS_SMALL_SPACING))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(PLACE_DETAILS_ICON_SIZE)
                )
                Spacer(Modifier.width(PLACE_DETAILS_SMALL_SPACING))
                val datePart = place.createdAtMillis
                    .takeIf { it > 0L }
                    ?.let { DateUtils.formatDate(it) }
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
private fun MainCardDivider() {
    Spacer(Modifier.height(PLACE_DETAILS_SMALL_SPACING))
    androidx.compose.material3.HorizontalDivider(
        thickness = PLACE_DETAILS_DIVIDER_THICKNESS,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = PLACE_DETAILS_DIVIDER_ALPHA)
    )
    Spacer(Modifier.height(PLACE_DETAILS_SMALL_SPACING))
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
            .padding(PLACE_DETAILS_CONTENT_PADDING),
        verticalArrangement = Arrangement.spacedBy(PLACE_DETAILS_CONTENT_PADDING)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = PLACE_DETAILS_MAIN_CARD_ELEVATION)
        ) {
            Column(modifier = Modifier.padding(PLACE_DETAILS_CARD_PADDING)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(
                        category = com.kidzone.domain.model.PlaceCategory.OTHER,
                        animationKey = if (placeId.isNotBlank()) "${keyPrefix}place_icon_$placeId" else null,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope,
                        size = PLACE_DETAILS_CATEGORY_ICON_SIZE,
                        iconSize = PLACE_DETAILS_CATEGORY_INNER_ICON_SIZE
                    )
                    Spacer(Modifier.width(PLACE_DETAILS_SECTION_SPACING))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .width(PLACE_DETAILS_SKELETON_LARGE_WIDTH)
                                .height(PLACE_DETAILS_SKELETON_TITLE_HEIGHT)
                                .clip(MaterialTheme.shapes.small)
                                .shimmerEffect()
                        )
                        Spacer(Modifier.height(PLACE_DETAILS_CHIP_HORIZONTAL_PADDING))
                        Box(
                            modifier = Modifier
                                .width(PLACE_DETAILS_SKELETON_MEDIUM_WIDTH)
                                .height(PLACE_DETAILS_SKELETON_TEXT_HEIGHT)
                                .clip(MaterialTheme.shapes.small)
                                .shimmerEffect()
                        )
                    }
                }
                Spacer(Modifier.height(PLACE_DETAILS_SKELETON_TEXT_HEIGHT))
                Box(
                    modifier = Modifier
                        .width(PLACE_DETAILS_SKELETON_SMALL_WIDTH)
                        .height(PLACE_DETAILS_SKELETON_RATING_HEIGHT)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect()
                )
                Spacer(Modifier.height(PLACE_DETAILS_SKELETON_TEXT_HEIGHT))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PLACE_DETAILS_SKELETON_DESCRIPTION_HEIGHT)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect()
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = PLACE_DETAILS_CARD_ELEVATION)
        ) {
            Column(modifier = Modifier.padding(PLACE_DETAILS_CARD_PADDING)) {
                Box(
                    modifier = Modifier
                        .width(PLACE_DETAILS_SKELETON_TINY_WIDTH)
                        .height(PLACE_DETAILS_SKELETON_TEXT_HEIGHT)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect()
                )
                Spacer(Modifier.height(PLACE_DETAILS_SECTION_SPACING))
                Row(horizontalArrangement = Arrangement.spacedBy(PLACE_DETAILS_CHIP_HORIZONTAL_PADDING)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(PLACE_DETAILS_SKELETON_PHOTO_SIZE)
                                .clip(RoundedCornerShape(PLACE_DETAILS_PHOTO_CORNER_RADIUS))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = PLACE_DETAILS_CARD_ELEVATION)
        ) {
            Column(modifier = Modifier.padding(PLACE_DETAILS_CARD_PADDING)) {
                Box(
                    modifier = Modifier
                        .width(PLACE_DETAILS_SKELETON_MEDIUM_WIDTH)
                        .height(PLACE_DETAILS_SKELETON_TEXT_HEIGHT)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect()
                )
                Spacer(Modifier.height(PLACE_DETAILS_SECTION_SPACING))
                Row(horizontalArrangement = Arrangement.spacedBy(PLACE_DETAILS_CHIP_HORIZONTAL_PADDING)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(PLACE_DETAILS_SKELETON_CHIP_WIDTH)
                                .height(PLACE_DETAILS_SKELETON_CHIP_HEIGHT)
                                .clip(RoundedCornerShape(PLACE_DETAILS_SKELETON_CHIP_RADIUS))
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
        elevation = CardDefaults.cardElevation(defaultElevation = PLACE_DETAILS_CARD_ELEVATION)
    ) {
        Column(modifier = Modifier.padding(PLACE_DETAILS_CARD_PADDING)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                trailing?.invoke()
            }
            Spacer(Modifier.height(PLACE_DETAILS_SECTION_SPACING))
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
        elevation = CardDefaults.cardElevation(
            defaultElevation = PLACE_DETAILS_REVIEW_CARD_ELEVATION
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isMine) {
                MaterialTheme.colorScheme.primary.copy(
                    alpha = PLACE_DETAILS_MY_REVIEW_BACKGROUND_ALPHA
                )
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(PLACE_DETAILS_CARD_PADDING)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = review.authorName.ifBlank {
                            stringResource(R.string.anonymous)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isMine) {
                        Spacer(Modifier.width(PLACE_DETAILS_SMALL_SPACING))

                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = stringResource(R.string.your_review),
                            tint = MaterialTheme.colorScheme.primary.copy(
                                alpha = PLACE_DETAILS_REVIEW_OWN_ALPHA
                            ),
                            modifier = Modifier.size(PLACE_DETAILS_REVIEW_OWN_ICON_SIZE)
                        )

                        Spacer(Modifier.width(PLACE_DETAILS_CHIP_CONTENT_SPACING))

                        Text(
                            text = stringResource(R.string.your_review),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = PLACE_DETAILS_DISABLED_STAR_ALPHA
                                )
                            },
                            modifier = Modifier.size(PLACE_DETAILS_REVIEW_STAR_SIZE)
                        )
                    }
                }
            }

            ReviewTimestampRow(
                createdAtMillis = review.createdAtMillis,
                updatedAtMillis = review.updatedAtMillis
            )

            if (review.comment.isNotBlank()) {
                Spacer(Modifier.height(PLACE_DETAILS_CHIP_HORIZONTAL_PADDING))

                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (review.photoUrls.isNotEmpty()) {
                Spacer(Modifier.height(PLACE_DETAILS_CHIP_HORIZONTAL_PADDING))

                ReviewPhotoRow(
                    photoUrls = review.photoUrls,
                    onPhotoClick = { index -> onPhotoClick?.invoke(index) }
                )
            }

            if (onEdit != null || onDelete != null || onReport != null) {
                Spacer(Modifier.height(PLACE_DETAILS_SMALL_SPACING))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(
                                PLACE_DETAILS_REVIEW_ACTION_BUTTON_SIZE
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(
                                    R.string.edit_your_review
                                ),
                                tint = MaterialTheme.colorScheme.primary.copy(
                                    alpha = PLACE_DETAILS_REVIEW_ACTION_ALPHA
                                ),
                                modifier = Modifier.size(
                                    PLACE_DETAILS_REVIEW_ACTION_ICON_SIZE
                                )
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(
                                PLACE_DETAILS_REVIEW_ACTION_BUTTON_SIZE
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(
                                    R.string.delete_your_review
                                ),
                                tint = MaterialTheme.colorScheme.error.copy(
                                    alpha = PLACE_DETAILS_REVIEW_ACTION_ALPHA
                                ),
                                modifier = Modifier.size(
                                    PLACE_DETAILS_REVIEW_ACTION_ICON_SIZE
                                )
                            )
                        }
                    }

                    if (onReport != null) {
                        IconButton(
                            onClick = onReport,
                            modifier = Modifier.size(
                                PLACE_DETAILS_REVIEW_ACTION_BUTTON_SIZE
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Flag,
                                contentDescription = stringResource(
                                    R.string.report_review
                                ),
                                tint = MaterialTheme.colorScheme.error.copy(
                                    alpha = PLACE_DETAILS_REVIEW_ACTION_ALPHA
                                ),
                                modifier = Modifier.size(
                                    PLACE_DETAILS_REVIEW_ACTION_ICON_SIZE
                                )
                            )
                        }
                    }
                }
            }
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
                text = DateUtils.formatDate(createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (updatedAtMillis > createdAtMillis && updatedAtMillis > 0L) {
            Spacer(Modifier.width(PLACE_DETAILS_SMALL_SPACING))
            Text(
                text = stringResource(R.string.edited_with_date, DateUtils.formatDate(updatedAtMillis)),
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
                text = NumberUtils.formatRating(avg),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(PLACE_DETAILS_CHIP_HORIZONTAL_PADDING))
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = stringResource(R.string.average_rating),
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(PLACE_DETAILS_AVERAGE_RATING_ICON_SIZE)
            )
            Spacer(Modifier.weight(1f))
            val reviewsCountText = pluralStringResource(R.plurals.reviews_count, total, total)
            Text(
                text = reviewsCountText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(PLACE_DETAILS_CHIP_HORIZONTAL_PADDING))
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
            .padding(vertical = PLACE_DETAILS_TINY_SPACING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$star",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(PLACE_DETAILS_DISTRIBUTION_STAR_COLUMN_WIDTH),
            textAlign = TextAlign.End
        )
        Spacer(Modifier.width(PLACE_DETAILS_TINY_SPACING))
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = stringResource(R.string.star_count_label, star),
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(PLACE_DETAILS_DISTRIBUTION_ICON_SIZE)
        )
        Spacer(Modifier.width(PLACE_DETAILS_CHIP_HORIZONTAL_PADDING))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .weight(1f)
                .height(PLACE_DETAILS_DISTRIBUTION_BAR_HEIGHT)
                .clip(RoundedCornerShape(PLACE_DETAILS_DISTRIBUTION_BAR_RADIUS)),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.width(PLACE_DETAILS_CHIP_HORIZONTAL_PADDING))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(PLACE_DETAILS_DISTRIBUTION_COUNT_COLUMN_WIDTH),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ReviewSortDropdown(
    current: PlaceDetailsViewModel.ReviewSortOrder,
    onChange: (PlaceDetailsViewModel.ReviewSortOrder) -> Unit
) {
    KidZoneSortMenu(
        current = current,
        currentLabel = current.getLabel(),
        options = PlaceDetailsViewModel.ReviewSortOrder.entries.map { order ->
            SortMenuOption(
                value = order,
                label = order.getLabel(),
                icon = order.sortMenuIcon
            )
        },
        onChange = onChange
    )
}

private val PlaceDetailsViewModel.ReviewSortOrder.sortMenuIcon: SortMenuIcon
    get() = when (this) {
        PlaceDetailsViewModel.ReviewSortOrder.NEWEST -> SortMenuIcon.RECENT
        PlaceDetailsViewModel.ReviewSortOrder.OLDEST -> SortMenuIcon.RECENT
        PlaceDetailsViewModel.ReviewSortOrder.HIGHEST -> SortMenuIcon.BEST_RATED
        PlaceDetailsViewModel.ReviewSortOrder.LOWEST -> SortMenuIcon.WORST_RATED
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
                        modifier = Modifier.size(PLACE_DETAILS_REVIEW_ACTION_ICON_SIZE),
                        strokeWidth = PLACE_DETAILS_DIALOG_PROGRESS_STROKE_WIDTH,
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
    onDismiss: () -> Unit,
    isOffline: Boolean
) {
    val reasons = listOf(
        "NOT_EXISTS" to stringResource(R.string.report_reason_not_exists),
        "INAPPROPRIATE" to stringResource(R.string.report_reason_inappropriate),
        "DUPLICATE" to stringResource(R.string.report_reason_duplicate),
        "FALSE_DATA" to stringResource(R.string.report_reason_false_data),
        "OTHER" to stringResource(R.string.report_reason_other)
    )
    KidZoneReportDialog(
        title = stringResource(R.string.report),
        reasons = reasons,
        onDismiss = onDismiss,
        onSubmit = onSubmit,
        isOffline = isOffline
    )
}

@Composable
private fun ReportReviewDialog(
    authorName: String,
    onSubmit: (reason: String, comment: String) -> Unit,
    onDismiss: () -> Unit,
    isOffline: Boolean
) {
    val reasons = listOf(
        "SPAM" to stringResource(R.string.report_reason_spam),
        "OFFENSIVE" to stringResource(R.string.report_reason_offensive),
        "FALSE_INFO" to stringResource(R.string.report_reason_false_info),
        "NOT_RELEVANT" to stringResource(R.string.report_reason_not_relevant),
        "OTHER" to stringResource(R.string.report_reason_other)
    )
    KidZoneReportDialog(
        title = stringResource(R.string.report_review),
        reasons = reasons,
        onDismiss = onDismiss,
        onSubmit = onSubmit,
        isOffline = isOffline,
        description = stringResource(
            R.string.report_review_context,
            authorName.ifBlank { stringResource(R.string.anonymous) }
        )
    )
}

@Composable
private fun PlacePhotoGallery(
    photoUrls: List<String>,
    onPhotoClick: (index: Int) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = PLACE_DETAILS_CARD_ELEVATION)
    ) {
        Column(modifier = Modifier.padding(PLACE_DETAILS_CARD_PADDING)) {
            Text(
                text = stringResource(R.string.photos_with_count, photoUrls.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(PLACE_DETAILS_SECTION_SPACING))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(PLACE_DETAILS_CHIP_HORIZONTAL_PADDING)
            ) {
                items(photoUrls.size) { index ->
                    val contentDesc = stringResource(R.string.photo_index, index + 1)
                    coil.compose.AsyncImage(
                        model = photoUrls[index],
                        contentDescription = contentDesc,
                        modifier = Modifier
                            .size(PLACE_DETAILS_PLACE_PHOTO_SIZE)
                            .clip(
                                androidx.compose.foundation.shape.RoundedCornerShape(
                                    PLACE_DETAILS_PHOTO_CORNER_RADIUS
                                )
                            )
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
        horizontalArrangement = Arrangement.spacedBy(PLACE_DETAILS_SMALL_SPACING)
    ) {
        items(photoUrls.size) { index ->
            val contentDesc = stringResource(R.string.review_photo_index, index + 1)
            coil.compose.AsyncImage(
                model = photoUrls[index],
                contentDescription = contentDesc,
                modifier = Modifier
                    .size(PLACE_DETAILS_REVIEW_PHOTO_SIZE)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(PLACE_DETAILS_PHOTO_CORNER_RADIUS))
                    .clickable { onPhotoClick(index) },
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
    }
}

@Composable
private fun ReportPhotoDialog(
    onSubmit: (reason: String, comment: String) -> Unit,
    onDismiss: () -> Unit,
    isOffline: Boolean
) {
    val reasons = listOf(
        "INAPPROPRIATE" to stringResource(R.string.report_reason_inappropriate),
        "NOT_RELEVANT" to stringResource(R.string.report_reason_not_related),
        "COPYRIGHT" to stringResource(R.string.report_reason_copyright),
        "OFFENSIVE" to stringResource(R.string.report_reason_offensive_vulgar),
        "OTHER" to stringResource(R.string.report_reason_other)
    )
    KidZoneReportDialog(
        title = stringResource(R.string.report_photo),
        reasons = reasons,
        onDismiss = onDismiss,
        onSubmit = onSubmit,
        isOffline = isOffline
    )
}
