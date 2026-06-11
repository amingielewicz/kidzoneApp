package com.kidzone.presentation.review.myreviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.domain.model.Review
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.ReviewRepository
import com.kidzone.utils.OpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Para review + dane miejsca potrzebne do UI (nazwa + kategoria dla animacji).
 */
data class MyReviewItem(
    val review: Review,
    val placeName: String?,
    val placeCategory: PlaceCategory?
)

/**
 * ViewModel ekranu "Moje opinie".
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyReviewsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository,
    private val placeRepository: PlaceRepository
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Ready(val items: List<MyReviewItem>) : UiState
        data class Error(val message: String) : UiState
    }

    data class DialogState(
        val pendingDeleteReviewId: String? = null,
        val isDeleting: Boolean = false,
        val deleteError: String? = null
    )

    private val _dialogState = MutableStateFlow(DialogState())
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    val uiState: StateFlow<UiState> = authRepository.currentUser
        .flatMapLatest { current ->
            if (current == null) {
                flowOf<UiState>(UiState.Ready(emptyList()))
            } else {
                reviewRepository.observeReviewsByUser(current.id)
                    .transformLatest<List<Review>, UiState> { reviews ->
                        val currentItems = (uiState.value as? UiState.Ready)?.items ?: emptyList()
                        if (currentItems.isEmpty()) {
                            emit(UiState.Loading)
                        }
                        
                        val placeIds = reviews.map { it.placeId }.distinct().filter { it.isNotBlank() }
                        val placeData = if (placeIds.isEmpty()) {
                            emptyMap<String, Pair<String, PlaceCategory>>()
                        } else {
                            coroutineScope {
                                placeIds.map { id ->
                                    async {
                                        when (val r = placeRepository.getPlace(id)) {
                                            is OpResult.Success -> id to (r.data.name to r.data.category)
                                            is OpResult.Failure -> id to null
                                        }
                                    }
                                }.awaitAll()
                            }
                                .toMap()
                                .filterValues { it != null }
                                .mapValues { it.value!! }
                        }
                        val items = reviews.map { review ->
                            val data = placeData[review.placeId]
                            MyReviewItem(
                                review = review,
                                placeName = data?.first,
                                placeCategory = data?.second
                            )
                        }
                        emit(UiState.Ready(items))
                    }
                    .onStart { emit(UiState.Loading) }
                    .catch { e ->
                        emit(UiState.Error(e.message ?: "Nie uda\u0142o si\u0119 wczyta\u0107 Twoich opinii"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    fun openDeleteDialog(reviewId: String) {
        _dialogState.update {
            it.copy(pendingDeleteReviewId = reviewId, deleteError = null)
        }
    }

    fun dismissDeleteDialog() {
        if (_dialogState.value.isDeleting) return
        _dialogState.update {
            it.copy(pendingDeleteReviewId = null, deleteError = null)
        }
    }

    fun confirmDelete() {
        val id = _dialogState.value.pendingDeleteReviewId ?: return
        viewModelScope.launch {
            _dialogState.update { it.copy(isDeleting = true, deleteError = null) }
            when (val r = reviewRepository.deleteReview(id)) {
                is OpResult.Success -> _dialogState.update {
                    it.copy(
                        pendingDeleteReviewId = null,
                        isDeleting = false,
                        deleteError = null
                    )
                }
                is OpResult.Failure -> _dialogState.update {
                    it.copy(
                        isDeleting = false,
                        deleteError = r.error.message ?: "Nie uda\u0142o si\u0119 usun\u0105\u0107 opinii"
                    )
                }
            }
        }
    }
}
