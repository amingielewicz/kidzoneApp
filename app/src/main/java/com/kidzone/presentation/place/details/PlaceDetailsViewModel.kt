package com.kidzone.presentation.place.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.R
import com.kidzone.data.repository.FirestoreReviewRepository
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.Review
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.ReviewRepository
import com.kidzone.domain.service.ImageCompressorPort
import com.kidzone.domain.service.LocationProvider
import com.kidzone.navigation.Route
import com.kidzone.presentation.place.add.PLACE_NAME_MAX_LENGTH
import com.kidzone.review.InAppReviewManager
import com.kidzone.utils.OpResult
import com.kidzone.utils.PhotoUploader
import com.kidzone.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeoutException
import javax.inject.Inject

private const val TOP_RANKING_POOL = 100
private const val MAX_PLACE_PHOTOS_ON_DETAILS = 5
private const val TOP_RANKING_BADGE_LIMIT = 10

/**
 * ViewModel ekranu szczegółów miejsca.
 */
@HiltViewModel
class PlaceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository,
    private val locationProvider: LocationProvider,
    private val photoUploader: PhotoUploader,
    private val imageCompressor: ImageCompressorPort,
    private val inAppReviewManager: InAppReviewManager
) : ViewModel() {

    data class UiState(
        val place: Place? = null,
        val author: User? = null,
        val reviews: List<Review> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: UiText? = null,
        val isDeleting: Boolean = false,
        val isDeleted: Boolean = false,
        val deleteErrorMessage: UiText? = null,
        val showAddReviewSheet: Boolean = false,
        val isAddingReview: Boolean = false,
        val addReviewError: UiText? = null,
        val editingReview: Review? = null,
        val sortOrder: ReviewSortOrder = ReviewSortOrder.NEWEST,
        val reviewActionEvent: ReviewActionEvent? = null,
        val topRank: Int? = null,
        val userLocation: Pair<Double, Double>? = null,
        val isUsingStaleLocation: Boolean = false,
        val staleLocationAgeMinutes: Int? = null,
        val isUploadingPlacePhoto: Boolean = false,
        val placePhotoDuplicateEvent: Boolean = false,
        val reviewPhotoDuplicateEvent: Boolean = false,
        val isPlaceReported: Boolean = false,
        val reportedPhotoUrls: Set<String> = emptySet(),
        val reportedReviewIds: Set<String> = emptySet(),
        val shouldRequestReview: Boolean = false
    )

    enum class ReviewSortOrder(val labelRes: Int, val comparator: Comparator<Review>) {
        NEWEST(
            labelRes = R.string.sort_recently_added,
            comparator = compareByDescending { it.createdAtMillis }
        ),
        OLDEST(
            labelRes = R.string.sort_oldest,
            comparator = compareBy { it.createdAtMillis }
        ),
        HIGHEST(
            labelRes = R.string.sort_best_rated,
            comparator = compareByDescending<Review> { it.rating }
                .thenByDescending { it.createdAtMillis }
        ),
        LOWEST(
            labelRes = R.string.sort_worst_rated,
            comparator = compareBy<Review> { it.rating }
                .thenByDescending { it.createdAtMillis }
        );

        @Composable
        fun getLabel(): String = stringResource(labelRes)
    }

    enum class ReviewActionEvent { ADDED, UPDATED }

    private val placeId: String =
        savedStateHandle.get<String>(Route.PlaceDetails.ARG_PLACE_ID).orEmpty()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        loadPlace()
        refreshLocation()
        viewModelScope.launch {
            reviewRepository.observeReviewsForPlace(placeId)
                .catch { e ->
                    _uiState.update {
                        if (it.place == null) {
                            it.copy(
                                isLoading = false,
                                errorMessage = UiText.StringResource(R.string.error_fetch_list)
                            )
                        } else {
                            it
                        }
                    }
                }
                .collect { reviews ->
                    _uiState.update {
                        it.copy(
                            reviews = reviews.sortedByDescending { r -> r.createdAtMillis }
                        )
                    }
                }
        }
    }

    fun retry() {
        loadPlace()
    }

    private fun loadPlace() {
        if (placeId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = UiText.StringResource(R.string.error_fetch_places)
                )
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = placeRepository.getPlace(placeId)) {
                is OpResult.Success -> {
                    _uiState.update {
                        it.copy(place = result.data, isLoading = false, errorMessage = null)
                    }
                    loadAuthor(result.data.ownerUserId)
                    loadTopRank()
                    seedPlacePhotoHashes(result.data.photoUrls)
                    loadUserReports()
                }

                is OpResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = UiText.StringResource(R.string.error_load_place)
                    )
                }
            }
        }
    }

    private fun loadTopRank() {
        val placeIdSnapshot = _uiState.value.place?.id ?: return
        viewModelScope.launch {
            when (val result = placeRepository.getTopPlaces(TOP_RANKING_POOL)) {
                is OpResult.Success -> {
                    val rank = result.data.indexOfFirst { it.id == placeIdSnapshot }
                    val rankOrNull = if (rank in 0 until TOP_RANKING_BADGE_LIMIT) {
                        rank + 1
                    } else null
                    _uiState.update { it.copy(topRank = rankOrNull) }
                }

                is OpResult.Failure -> {}
            }
        }
    }

    private fun loadAuthor(ownerUserId: String) {
        if (ownerUserId.isBlank()) return
        viewModelScope.launch {
            when (val result = authRepository.getUserById(ownerUserId)) {
                is OpResult.Success -> _uiState.update { it.copy(author = result.data) }
                is OpResult.Failure -> {}
            }
        }
    }

    fun delete() {
        val place = _uiState.value.place ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteErrorMessage = null) }

            val user = authRepository.currentUser.first()
            if (user == null || user.id != place.ownerUserId) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        deleteErrorMessage = UiText.StringResource(R.string.error_unauthorized)
                    )
                }
                return@launch
            }

            when (val result = placeRepository.deletePlace(place.id)) {
                is OpResult.Success -> _uiState.update {
                    it.copy(isDeleting = false, isDeleted = true)
                }

                is OpResult.Failure -> _uiState.update {
                    it.copy(
                        isDeleting = false,
                        deleteErrorMessage = UiText.StringResource(R.string.error_unknown)
                    )
                }
            }
        }
    }

    fun consumeDeleteError() {
        _uiState.update { it.copy(deleteErrorMessage = null) }
    }

    fun refresh() {
        loadPlace()
        refreshLocation()
    }

    private fun refreshLocation() {
        if (!locationProvider.hasPermission()) {
            useStaleLocationOrClear()
            return
        }

        viewModelScope.launch {
            applyCurrentOrStaleLocation(locationProvider.getCurrentLocation())
        }
    }

    private fun useStaleLocationOrClear() {
        val stale = locationProvider.getLastKnownLocation()

        if (stale == null) {
            _uiState.update {
                it.copy(
                    userLocation = null,
                    isUsingStaleLocation = false,
                    staleLocationAgeMinutes = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                userLocation = stale,
                isUsingStaleLocation = true,
                staleLocationAgeMinutes = locationProvider.getLastKnownLocationAgeMinutes()
            )
        }
    }

    private fun applyCurrentOrStaleLocation(location: Pair<Double, Double>?) {
        if (location != null) {
            _uiState.update {
                it.copy(
                    userLocation = location,
                    isUsingStaleLocation = false,
                    staleLocationAgeMinutes = null
                )
            }
            return
        }

        useStaleLocationOrClear()
    }

    fun deleteReview(reviewId: String) {
        val place = _uiState.value.place ?: return
        viewModelScope.launch {
            when (val result = reviewRepository.deleteReview(reviewId)) {
                is OpResult.Success -> {
                    val deleted = _uiState.value.reviews.firstOrNull { it.id == reviewId }
                    if (deleted != null) {
                        val oldCount = place.reviewsCount
                        val oldAvg = place.averageRating
                        val newCount = (oldCount - 1).coerceAtLeast(0)
                        val newAvg = if (newCount > 0) {
                            ((oldAvg * oldCount) - deleted.rating) / newCount
                        } else 0.0
                        _uiState.update {
                            it.copy(
                                place = place.copy(reviewsCount = newCount, averageRating = newAvg)
                            )
                        }
                    }
                }

                is OpResult.Failure -> {}
            }
        }
    }

    fun setSortOrder(order: ReviewSortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun consumeReviewActionEvent() {
        _uiState.update { it.copy(reviewActionEvent = null) }
    }

    fun consumeReviewRequest() {
        _uiState.update { it.copy(shouldRequestReview = false) }
    }

    fun openAddReviewSheet() {
        _uiState.update {
            it.copy(showAddReviewSheet = true, addReviewError = null, editingReview = null)
        }
    }

    fun openEditReviewSheet(review: Review) {
        _uiState.update {
            it.copy(showAddReviewSheet = true, addReviewError = null, editingReview = review)
        }
    }

    fun dismissAddReviewSheet() {
        if (_uiState.value.isAddingReview) return
        _uiState.update {
            it.copy(showAddReviewSheet = false, addReviewError = null, editingReview = null)
        }
    }

    fun submitReview(
        rating: Int,
        comment: String,
        photoUris: List<android.net.Uri> = emptyList(),
        retainedPhotoUrls: List<String> = emptyList()
    ) {
        val place = _uiState.value.place ?: return
        val user = currentUser.value
        if (user == null) {
            _uiState.update {
                it.copy(addReviewError = UiText.StringResource(R.string.login_no_account))
            }
            return
        }
        if (user.id == place.ownerUserId) {
            _uiState.update {
                it.copy(addReviewError = UiText.StringResource(R.string.error_owner_cannot_review))
            }
            return
        }
        if (rating !in 1..5) {
            _uiState.update {
                it.copy(addReviewError = UiText.StringResource(R.string.error_invalid_rating))
            }
            return
        }

        val editing = _uiState.value.editingReview
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingReview = true, addReviewError = null) }

            if (editing == null) {
                submitNewReview(place, user, rating, comment, photoUris)
            } else {
                submitEditedReview(place, editing, rating, comment, photoUris, retainedPhotoUrls)
            }
        }
    }

    private suspend fun submitNewReview(
        place: Place,
        user: User,
        rating: Int,
        comment: String,
        photoUris: List<android.net.Uri>
    ) {
        val uploadedPhotoUrls = mutableListOf<String>()
        val newHashes = mutableSetOf<String>()
        for (uri in photoUris) {
            val bytes = imageCompressor.compressToWebp(uri)
            if (bytes != null) {
                val hash = java.security.MessageDigest.getInstance("MD5")
                    .digest(bytes)
                    .joinToString("") { "%02x".format(it) }
                if (hash in newHashes) continue
                newHashes.add(hash)
                try {
                    val url = photoUploader.uploadReviewPhoto(
                        ownerUserId = user.id,
                        reviewId = "pending_${System.currentTimeMillis()}",
                        imageBytes = bytes
                    )
                    uploadedPhotoUrls.add(url)
                } catch (_: Exception) {
                }
            }
        }

        val review = Review(
            id = "",
            placeId = place.id,
            userId = user.id,
            authorName = user.name,
            rating = rating,
            comment = comment.trim(),
            photoUrls = uploadedPhotoUrls,
            createdAtMillis = System.currentTimeMillis()
        )

        when (val result = reviewRepository.addReview(review)) {
            is OpResult.Success -> {
                val oldCount = place.reviewsCount
                val oldAvg = place.averageRating
                val newCount = oldCount + 1
                val newAvg = (oldAvg * oldCount + rating) / newCount

                _uiState.update {
                    it.copy(
                        place = place.copy(
                            reviewsCount = newCount,
                            averageRating = newAvg
                        ),
                        isAddingReview = false,
                        showAddReviewSheet = false,
                        addReviewError = null,
                        editingReview = null,
                        reviewActionEvent = ReviewActionEvent.ADDED,
                        shouldRequestReview = inAppReviewManager.onReviewSubmitted()
                    )
                }
            }

            is OpResult.Failure -> _uiState.update {
                it.copy(
                    isAddingReview = false,
                    addReviewError = mapReviewError(result.error)
                )
            }
        }
    }

    private suspend fun submitEditedReview(
        place: Place,
        existing: Review,
        rating: Int,
        comment: String,
        photoUris: List<android.net.Uri>,
        retainedPhotoUrls: List<String>
    ) {
        val existingHashes = mutableSetOf<String>()
        for (url in retainedPhotoUrls) {
            try {
                val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val conn = java.net.URL(url).openConnection()
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 10_000
                    conn.getInputStream().readBytes()
                }
                val hash = java.security.MessageDigest.getInstance("MD5")
                    .digest(bytes)
                    .joinToString("") { "%02x".format(it) }
                existingHashes.add(hash)
            } catch (_: Exception) {
            }
        }

        val newUploadedUrls = mutableListOf<String>()
        var reviewDuplicatesSkipped = 0
        for (uri in photoUris) {
            val bytes = imageCompressor.compressToWebp(uri)
            if (bytes != null) {
                val hash = java.security.MessageDigest.getInstance("MD5")
                    .digest(bytes)
                    .joinToString("") { "%02x".format(it) }
                if (hash in existingHashes) {
                    reviewDuplicatesSkipped++
                    continue
                }
                existingHashes.add(hash)
                try {
                    val url = photoUploader.uploadReviewPhoto(
                        ownerUserId = existing.userId,
                        reviewId = existing.id,
                        imageBytes = bytes
                    )
                    newUploadedUrls.add(url)
                } catch (_: Exception) {
                }
            }
        }

        if (reviewDuplicatesSkipped > 0 && newUploadedUrls.isEmpty() && photoUris.isNotEmpty()
            && rating == existing.rating && comment.trim() == existing.comment
        ) {
            _uiState.update {
                it.copy(
                    isAddingReview = false,
                    showAddReviewSheet = false,
                    editingReview = null,
                    reviewPhotoDuplicateEvent = true
                )
            }
            return
        }

        val finalPhotoUrls = retainedPhotoUrls + newUploadedUrls

        val removedUrls = existing.photoUrls.filter { it !in retainedPhotoUrls }
        for (url in removedUrls) {
            try {
                photoUploader.deletePhoto(url)
            } catch (_: Exception) {
            }
        }

        val updated = existing.copy(
            rating = rating,
            comment = comment.trim(),
            photoUrls = finalPhotoUrls
        )

        when (val result = reviewRepository.updateReview(updated)) {
            is OpResult.Success -> {
                val count = place.reviewsCount
                val oldAvg = place.averageRating
                val oldRating = existing.rating
                val newAvg = if (count > 0) {
                    (oldAvg * count - oldRating + rating) / count
                } else {
                    rating.toDouble()
                }
                _uiState.update {
                    it.copy(
                        place = place.copy(averageRating = newAvg),
                        isAddingReview = false,
                        showAddReviewSheet = false,
                        addReviewError = null,
                        editingReview = null,
                        reviewActionEvent = ReviewActionEvent.UPDATED
                    )
                }
            }

            is OpResult.Failure -> _uiState.update {
                it.copy(
                    isAddingReview = false,
                    addReviewError = mapReviewError(result.error)
                )
            }
        }
    }

    private fun loadUserReports() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val hasReportedPlace = placeRepository.hasUserReportedPlace(placeId, user.id)
            val reportedPhotos = placeRepository.getReportedPhotos(user.id)
            val reportedReviews = reviewRepository.getReportedReviews(user.id)

            _uiState.update {
                it.copy(
                    isPlaceReported = hasReportedPlace,
                    reportedPhotoUrls = reportedPhotos,
                    reportedReviewIds = reportedReviews
                )
            }
        }
    }

    fun reportPlace(reason: String, comment: String = "") {
        val place = _uiState.value.place ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val result = placeRepository.reportPlace(
                placeId = place.id,
                reporterId = user.id,
                reason = reason,
                comment = comment
            )
            if (result is OpResult.Success) {
                _uiState.update { it.copy(isPlaceReported = true) }
            }
        }
    }

    fun reportReview(reviewId: String, reason: String, comment: String = "") {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val result = reviewRepository.reportReviewAsSpam(
                reviewId = reviewId,
                reporterId = user.id,
                reason = reason,
                comment = comment
            )
            if (result is OpResult.Success) {
                _uiState.update { it.copy(reportedReviewIds = it.reportedReviewIds + reviewId) }
            }
        }
    }

    fun reportPhoto(photoUrl: String, reason: String, comment: String = "") {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val result = placeRepository.reportPhoto(
                photoUrl = photoUrl,
                reporterId = user.id,
                reason = reason,
                comment = comment
            )
            if (result is OpResult.Success) {
                _uiState.update { it.copy(reportedPhotoUrls = it.reportedPhotoUrls + photoUrl) }
            }
        }
    }

    private val placePhotoHashes = mutableSetOf<String>()

    fun consumePlacePhotoDuplicateEvent() {
        _uiState.update { it.copy(placePhotoDuplicateEvent = false) }
    }

    fun consumeReviewPhotoDuplicateEvent() {
        _uiState.update { it.copy(reviewPhotoDuplicateEvent = false) }
    }

    fun addPhotoToPlace(photoUri: android.net.Uri) {
        val place = _uiState.value.place ?: return
        val user = currentUser.value ?: return
        if (place.photoUrls.size >= MAX_PLACE_PHOTOS_ON_DETAILS) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPlacePhoto = true) }

            val newBytes = imageCompressor.compressToWebp(photoUri)
            if (newBytes == null) {
                _uiState.update { it.copy(isUploadingPlacePhoto = false) }
                return@launch
            }

            val newHash = java.security.MessageDigest.getInstance("MD5")
                .digest(newBytes)
                .joinToString("") { "%02x".format(it) }

            if (newHash in placePhotoHashes) {
                _uiState.update {
                    it.copy(
                        isUploadingPlacePhoto = false,
                        placePhotoDuplicateEvent = true
                    )
                }
                return@launch
            }

            try {
                val url = photoUploader.uploadPlacePhoto(
                    ownerUserId = user.id,
                    placeId = place.id,
                    imageBytes = newBytes
                )
                when (placeRepository.addPhotoUrl(place.id, url, user.id)) {
                    is OpResult.Success -> {
                        placePhotoHashes.add(newHash)
                        _uiState.update {
                            it.copy(
                                place = place.copy(
                                    photoUrls = place.photoUrls + url,
                                    photoUploadedBy = place.photoUploadedBy + (url to user.id)
                                ),
                                isUploadingPlacePhoto = false
                            )
                        }
                    }

                    is OpResult.Failure -> {
                        _uiState.update { it.copy(isUploadingPlacePhoto = false) }
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isUploadingPlacePhoto = false) }
            }
        }
    }

    fun deletePhotoFromPlace(photoUrl: String) {
        val place = _uiState.value.place ?: return
        val user = currentUser.value ?: return

        val uploaderId = place.photoUploadedBy[photoUrl]
        if (uploaderId != user.id) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPlacePhoto = true) }

            when (placeRepository.removePhotoUrl(place.id, photoUrl)) {
                is OpResult.Success -> {
                    try {
                        photoUploader.deletePhoto(photoUrl)
                    } catch (_: Exception) {
                    }

                    _uiState.update {
                        it.copy(
                            place = place.copy(
                                photoUrls = place.photoUrls - photoUrl,
                                photoUploadedBy = place.photoUploadedBy - photoUrl
                            ),
                            isUploadingPlacePhoto = false
                        )
                    }
                    seedPlacePhotoHashes((_uiState.value.place?.photoUrls).orEmpty())
                }

                is OpResult.Failure -> {
                    _uiState.update { it.copy(isUploadingPlacePhoto = false) }
                }
            }
        }
    }

    private fun seedPlacePhotoHashes(photoUrls: List<String>) {
        placePhotoHashes.clear()
        if (photoUrls.isEmpty()) return
        viewModelScope.launch {
            for (url in photoUrls) {
                try {
                    val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val conn = java.net.URL(url).openConnection()
                        conn.connectTimeout = 10_000
                        conn.readTimeout = 10_000
                        conn.getInputStream().readBytes()
                    }
                    val hash = java.security.MessageDigest.getInstance("MD5")
                        .digest(bytes)
                        .joinToString("") { "%02x".format(it) }
                    placePhotoHashes.add(hash)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun submitSuggestedEdit(
        name: String,
        description: String,
        category: String,
        amenities: Set<String>
    ) {
        val place = _uiState.value.place ?: return
        val user = currentUser.value ?: return
        val changes = mutableMapOf<String, Any>()
        val trimmedName = name.trim().take(PLACE_NAME_MAX_LENGTH).trim()
        if (trimmedName != place.name) changes["name"] = trimmedName
        if (description.trim() != place.description) changes["description"] = description.trim()
        if (category != place.category.name) changes["category"] = category
        if (amenities != place.amenities.map { it.name }.toSet()) {
            changes["amenities"] = amenities.toList()
        }
        if (changes.isEmpty()) return
        viewModelScope.launch {
            placeRepository.submitChangeRequest(
                placeId = place.id,
                requesterId = user.id,
                changes = changes,
                type = "EDIT"
            )
        }
    }

    fun submitLocationCorrection(latitude: Double, longitude: Double, address: String?) {
        val place = _uiState.value.place ?: return
        val user = currentUser.value ?: return
        val changes = mutableMapOf<String, Any>(
            "latitude" to latitude,
            "longitude" to longitude
        )
        if (!address.isNullOrBlank()) changes["address"] = address
        viewModelScope.launch {
            placeRepository.submitChangeRequest(
                placeId = place.id,
                requesterId = user.id,
                changes = changes,
                type = "LOCATION"
            )
        }
    }

    private fun mapReviewError(e: Throwable): UiText = when (e) {
        is FirestoreReviewRepository.OfflineReviewSyncDisabledException ->
            UiText.StringResource(R.string.error_offline_sync)

        is FirestoreReviewRepository.AlreadyReportedException ->
            UiText.StringResource(R.string.error_already_reported)

        is TimeoutException ->
            UiText.StringResource(R.string.error_timeout)

        else -> UiText.StringResource(R.string.error_unknown)
    }

}