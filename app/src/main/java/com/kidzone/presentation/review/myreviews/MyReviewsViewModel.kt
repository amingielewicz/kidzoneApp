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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Łączy opinię użytkownika z minimalnymi danymi miejsca potrzebnymi do prezentacji.
 *
 * @property review opinia należąca do aktualnego użytkownika.
 * @property placeName aktualna nazwa miejsca albo `null`, gdy miejsce zostało usunięte lub jest
 * niedostępne.
 * @property placeCategory aktualna kategoria miejsca albo `null`, gdy danych nie można pobrać.
 */
data class MyReviewItem(
    val review: Review,
    val placeName: String?,
    val placeCategory: PlaceCategory? = null
)

/**
 * 🎯 Odpowiedzialności:
 * - Udostępnianie listy opinii należących do aktualnie zalogowanego użytkownika.
 * - Łączenie danych opinii z podstawowymi informacjami o miejscach (nazwa, kategoria).
 * - Obsługa usuwania opinii i odświeżania stanu powiązanego.
 *
 * 🚫 Poza zakresem:
 * - Brak decyzji o offline queue.
 * - Brak retry logiki dla operacji sieciowych.
 * - Brak bezpośredniego zarządzania sesją (delegowane do [AuthRepository]).
 *
 * 📥 Wejście:
 * - Strumień aktualnego użytkownika z [AuthRepository].
 *
 * 📤 Wyjście:
 * - Stan ekranu "Moje opinie" ([UiState]) zawierający listę elementów [MyReviewItem].
 *
 * ✅ Gwarancje:
 * - Automatyczne czyszczenie danych prywatnych po zakończeniu sesji.
 * - Deterministyczne łączenie opinii z danymi miejsc.
 *
 * 🔌 Offline:
 * - Wspiera odczyt własnych opinii z cache lokalnego Room.
 *
 * 🧵 Wątki:
 * - viewModelScope dla reaktywnych strumieni danych i usuwania opinii.
 * - Wykorzystanie [async]/[awaitAll] do wydajnego dociągania danych miejsc.
 *
 * 🧪 Testowalność:
 * - Pełne DI.
 * - Deterministyczne mapowanie stanów bazy na listę UI.
 *
 * 🧼 Lifecycle:
 * - Zarządzanie równoległym dociąganiem danych przy zmianie sesji.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyReviewsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository,
    private val placeRepository: PlaceRepository
) : ViewModel() {

    /** Stan ekranu „Moje opinie”. */
    sealed interface UiState {
        /** Trwa pierwsze ładowanie danych. */
        data object Loading : UiState

        /**
         * Lista została przygotowana.
         *
         * @property items opinie wraz z nazwami i kategoriami miejsc.
         */
        data class Ready(val items: List<MyReviewItem>) : UiState

        /**
         * Nie udało się odczytać listy.
         *
         * @property message komunikat przeznaczony dla UI.
         */
        data class Error(val message: String) : UiState
    }

    /**
     * Stan dialogu potwierdzającego usunięcie opinii.
     *
     * @property pendingDeleteReviewId identyfikator opinii oczekującej na potwierdzenie.
     * @property isDeleting czy trwa usuwanie i dialog nie powinien zostać zamknięty.
     * @property deleteError komunikat ostatniego błędu usuwania.
     */
    data class DialogState(
        val pendingDeleteReviewId: String? = null,
        val isDeleting: Boolean = false,
        val deleteError: String? = null
    )

    private val _dialogState = MutableStateFlow(DialogState())

    /** Stan dialogu obserwowany przez ekran Compose. */
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    /**
     * Stan listy opinii obserwowany przez ekran Compose.
     *
     * Przy zmianie listy zachowuje poprzednią treść do czasu pobrania danych miejsc, aby ograniczyć
     * miganie interfejsu. Brak sesji jest reprezentowany przez `Ready(emptyList())`.
     */
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
                                        when (val result = placeRepository.getPlace(id)) {
                                            is OpResult.Success -> id to (result.data.name to result.data.category)
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
                    .catch { error ->
                        emit(UiState.Error(error.message ?: "Could not load your reviews"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    /**
     * Otwiera dialog usuwania dla wskazanej opinii.
     *
     * @param reviewId identyfikator opinii należącej do aktualnego użytkownika.
     */
    fun openDeleteDialog(reviewId: String) {
        _dialogState.update {
            it.copy(pendingDeleteReviewId = reviewId, deleteError = null)
        }
    }

    /** Zamyka dialog, o ile operacja usuwania nie jest aktualnie wykonywana. */
    fun dismissDeleteDialog() {
        if (_dialogState.value.isDeleting) return
        _dialogState.update {
            it.copy(pendingDeleteReviewId = null, deleteError = null)
        }
    }

    /**
     * Usuwa opinię wskazaną w [DialogState.pendingDeleteReviewId].
     *
     * Repository odpowiada również za aktualizację agregatów miejsca i użytkownika. Dialog jest
     * zamykany dopiero po potwierdzonym sukcesie backendu.
     */
    fun confirmDelete() {
        val id = _dialogState.value.pendingDeleteReviewId ?: return
        viewModelScope.launch {
            _dialogState.update { it.copy(isDeleting = true, deleteError = null) }
            when (val result = reviewRepository.deleteReview(id)) {
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
                        deleteError = result.error.message ?: "Could not delete the review"
                    )
                }
            }
        }
    }
}
