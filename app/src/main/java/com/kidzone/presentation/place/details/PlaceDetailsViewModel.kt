package com.kidzone.presentation.place.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.Review
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.ReviewRepository
import com.kidzone.domain.service.ImageCompressorPort
import com.kidzone.navigation.Route
import com.kidzone.presentation.place.add.PLACE_NAME_MAX_LENGTH
import com.kidzone.review.InAppReviewManager
import com.kidzone.utils.OpResult
import com.kidzone.utils.PhotoUploader
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
import javax.inject.Inject

/**
 * Rozmiar puli "ranking TOP 100" - tyle najlepiej ocenianych miejsc bierzemy
 * pod uwagę przy wyliczaniu pozycji w plakietce na ekranie szczegółów.
 *
 * Spójne z labelem "TOP 100" pokazywanym nad gwiazdką - user ma świadomość,
 * że plakietka odnosi się do rankingu setki najlepszych miejsc, mimo że
 * sama wyświetla się tylko dla pierwszej dziesiątki ([TOP_RANKING_BADGE_LIMIT]).
 */
private const val TOP_RANKING_POOL = 100

/** Maksymalna liczba zdjęć na miejsce (widoczna z poziomu ekranu szczegółów). */
private const val MAX_PLACE_PHOTOS_ON_DETAILS = 5

/**
 * Górna granica pozycji, dla której pokazujemy plakietkę "TOP 100" z numerem.
 *
 * Świadoma decyzja: tylko miejsca w pierwszej 10 dostają wizualne wyróżnienie -
 * w przeciwnym wypadku plakietka byłaby na każdej karcie i straciłaby
 * znaczenie. "TOP 100" w treści labelu odnosi się do puli rankingowej
 * (zob. [TOP_RANKING_POOL]), nie do liczby plakietek.
 */
private const val TOP_RANKING_BADGE_LIMIT = 10

/**
 * ViewModel ekranu szczegółów miejsca.
 *
 * Ładuje:
 *  - pojedyncze [Place] przez [PlaceRepository.getPlace] (one-shot),
 *  - dane autora przez [AuthRepository.getUserById] (one-shot, po załadowaniu
 *    place'a),
 *  - listę opinii przez [ReviewRepository.observeReviewsForPlace] (live).
 *
 * Eksponuje też strumień [currentUser] do wyliczenia czy aktualny użytkownik
 * jest właścicielem miejsca (i tym samym czy może edytować/usuwać).
 *
 * Akcja [delete] usuwa miejsce po potwierdzeniu w UI – zwraca przez
 * `isDeleted=true` w state, na który ekran nawiguje.
 */
@HiltViewModel
class PlaceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placeRepository: PlaceRepository,
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository,
    private val photoUploader: PhotoUploader,
    private val imageCompressor: ImageCompressorPort,
    private val inAppReviewManager: InAppReviewManager
) : ViewModel() {

    /**
     * @property place załadowane miejsce; null gdy jeszcze nie wczytane lub nie istnieje
     * @property author dane autora; null gdy jeszcze nie pobrane lub nie znaleziono
     * @property reviews opinie z snapshot listenera (już posortowane po dacie desc)
     * @property isLoading true do pierwszego wyniku `getPlace`
     * @property errorMessage komunikat błędu z `getPlace` lub strumienia opinii
     * @property isDeleting true podczas wywoływania `deletePlace`
     * @property isDeleted true po pomyślnym usunięciu – sygnał dla UI do nawigacji
     * @property deleteErrorMessage komunikat błędu z `deletePlace`
     * @property showAddReviewSheet true gdy user otworzył formularz dodawania opinii
     * @property isAddingReview true podczas wywołania `addReview` (spinner w sheet)
     * @property addReviewError komunikat błędu z `addReview` (do pokazania w sheet)
     * @property editingReview gdy != null, sheet jest w trybie edycji tej opinii
     *   (pre-fill ratingu i komentarza). Decyduje też, którą metodę repo
     *   zawoła [submitReview] – `updateReview` zamiast `addReview`.
     * @property sortOrder aktualne sortowanie listy opinii (dla UI – sort robi
     *   się klient-side, lista w `reviews` jest "surowa").
     * @property reviewActionEvent jednorazowy event "opinia zapisana" do
     *   wyświetlenia przez UI Snackbara. Ekran konsumuje go przez
     *   [consumeReviewActionEvent], dzięki czemu rotacja / re-kompozycja nie
     *   pokażą snackbara dwa razy.
     * @property topRank pozycja miejsca w rankingu TOP 100 (1-based) jeżeli
     *   miejsce mieści się w pierwszej dziesiątce; null w pozostałych
     *   przypadkach (poza top 10, brak ocen, błąd fetcha rankingu). UI
     *   pokazuje plakietkę z numerem tylko gdy != null.
     */
    data class UiState(
        val place: Place? = null,
        val author: User? = null,
        val reviews: List<Review> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val isDeleting: Boolean = false,
        val isDeleted: Boolean = false,
        val deleteErrorMessage: String? = null,
        val showAddReviewSheet: Boolean = false,
        val isAddingReview: Boolean = false,
        val addReviewError: String? = null,
        val editingReview: Review? = null,
        val sortOrder: ReviewSortOrder = ReviewSortOrder.NEWEST,
        val reviewActionEvent: ReviewActionEvent? = null,
        val topRank: Int? = null,
        val isUploadingPlacePhoto: Boolean = false,
        val placePhotoDuplicateEvent: Boolean = false,
        val reviewPhotoDuplicateEvent: Boolean = false,
        val isPlaceReported: Boolean = false,
        val reportedPhotoUrls: Set<String> = emptySet(),
        val reportedReviewIds: Set<String> = emptySet(),
        val shouldRequestReview: Boolean = false
    )

    /**
     * Sposoby sortowania listy opinii. Etykiety po polsku, bo idą wprost
     * do `DropdownMenuItem`'ów w UI. Selektor [comparator] dostarcza
     * [java.util.Comparator] gotowy do `sortedWith`.
     */
    enum class ReviewSortOrder(val label: String, val comparator: Comparator<Review>) {
        NEWEST(
            label = "Najnowsze",
            comparator = compareByDescending { it.createdAtMillis }
        ),
        OLDEST(
            label = "Najstarsze",
            comparator = compareBy { it.createdAtMillis }
        ),
        HIGHEST(
            label = "Najwyżej oceniane",
            comparator = compareByDescending<Review> { it.rating }
                .thenByDescending { it.createdAtMillis }
        ),
        LOWEST(
            label = "Najniżej oceniane",
            comparator = compareBy<Review> { it.rating }
                .thenByDescending { it.createdAtMillis }
        )
    }

    /** Rodzaj zakończonej akcji – decyduje o treści Snackbara w UI. */
    enum class ReviewActionEvent { ADDED, UPDATED }

    private val placeId: String =
        savedStateHandle.get<String>(Route.PlaceDetails.ARG_PLACE_ID).orEmpty()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Aktualnie zalogowany użytkownik – używamy go w UI do sprawdzenia
     * czy pokazać akcje edytuj/usuń.
     */
    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        loadPlace()

        // Subskrypcja opinii – live, snapshot listener z Firestore.
        viewModelScope.launch {
            reviewRepository.observeReviewsForPlace(placeId)
                .catch { e ->
                    // Błąd opinii nie powinien blokować widoku samego miejsca –
                    // pokazujemy go tylko gdy żaden place też się nie załadował.
                    _uiState.update {
                        if (it.place == null) {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.message ?: "Nie udało się wczytać opinii"
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
                it.copy(isLoading = false, errorMessage = "Brak identyfikatora miejsca")
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
                        errorMessage = result.error.message ?: "Nie udało się wczytać miejsca"
                    )
                }
            }
        }
    }

    /**
     * Pobiera ranking top 100 miejsc i wylicza pozycję bieżącego miejsca.
     *
     * Reguła wyświetlania plakietki TOP 100 (zob. [UiState.topRank]):
     *  - bierzemy [TOP_RANKING_POOL] (100) najlepiej ocenianych miejsc,
     *  - sprawdzamy pozycję (1-based) bieżącego miejsca,
     *  - jeżeli mieści się w [TOP_RANKING_BADGE_LIMIT] (10) - ustawiamy
     *    `topRank = pozycja` i UI pokazuje plakietkę,
     *  - w przeciwnym wypadku zostawiamy `topRank = null`.
     *
     * Best-effort: błąd fetcha (np. brak sieci) tylko zostawia `topRank = null`,
     * ekran szczegółów się przez to nie psuje.
     *
     * Świadomie one-shot fetch zamiast snapshot listenera - ranking nie musi
     * być real-time na ekranie szczegółów, a unikamy dodatkowego listenera
     * na całej kolekcji `places`.
     */
    private fun loadTopRank() {
        val placeIdSnapshot = _uiState.value.place?.id ?: return
        viewModelScope.launch {
            when (val result = placeRepository.getTopPlaces(TOP_RANKING_POOL)) {
                is OpResult.Success -> {
                    val rank = result.data.indexOfFirst { it.id == placeIdSnapshot }
                    val rankOrNull = if (rank in 0 until TOP_RANKING_BADGE_LIMIT) {
                        rank + 1 // 1-based
                    } else null
                    _uiState.update { it.copy(topRank = rankOrNull) }
                }
                is OpResult.Failure -> {
                    // brak rankingu = brak plakietki, ale ekran nadal działa
                }
            }
        }
    }

    private fun loadAuthor(ownerUserId: String) {
        if (ownerUserId.isBlank()) return
        viewModelScope.launch {
            // Best-effort – brak autora nie blokuje wyświetlenia szczegółów,
            // pokazujemy wtedy tylko datę bez nicka.
            when (val result = authRepository.getUserById(ownerUserId)) {
                is OpResult.Success -> _uiState.update { it.copy(author = result.data) }
                is OpResult.Failure -> { /* zostawiamy author = null */ }
            }
        }
    }

    /**
     * Usuwa aktualnie wyświetlane miejsce. Powinno być wywołane TYLKO gdy
     * [isCurrentUserOwner] = true (UI tak filtruje), ale dodatkowo tu robimy
     * sanity-check zalogowanego użytkownika.
     */
    fun delete() {
        val place = _uiState.value.place ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteErrorMessage = null) }

            val user = authRepository.currentUser.first()
            if (user == null || user.id != place.ownerUserId) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        deleteErrorMessage = "Nie masz uprawnień, by usunąć to miejsce"
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
                        deleteErrorMessage = result.error.message ?: "Nie udało się usunąć miejsca"
                    )
                }
            }
        }
    }

    fun consumeDeleteError() {
        _uiState.update { it.copy(deleteErrorMessage = null) }
    }

    /**
     * Pull-to-refresh – ponownie ładuje miejsce, autora i ranking.
     * Opinie odświeżają się same (snapshot listener), ale re-fetch place
     * zaktualizuje np. zmieniony opis, zdjęcia dodane przez innych userów itp.
     */
    fun refresh() {
        loadPlace()
    }

    /**
     * Usuwa opinię bieżącego usera z ekranu szczegółów.
     * Dostępne tylko dla autora opinii (UI to filtruje).
     */
    fun deleteReview(reviewId: String) {
        val place = _uiState.value.place ?: return
        viewModelScope.launch {
            when (val result = reviewRepository.deleteReview(reviewId)) {
                is OpResult.Success -> {
                    // Optymistycznie przelicz agregaty (tak jak repo robi w transakcji)
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
                is OpResult.Failure -> {
                    // Snapshot listener sam odświeży listę – brak specjalnego handling
                }
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

    // --- Dodawanie / edycja opinii ---

    fun openAddReviewSheet() {
        _uiState.update {
            it.copy(showAddReviewSheet = true, addReviewError = null, editingReview = null)
        }
    }

    /** Otwiera ten sam sheet w trybie edycji – pre-fill ratingu i komentarza. */
    fun openEditReviewSheet(review: Review) {
        _uiState.update {
            it.copy(showAddReviewSheet = true, addReviewError = null, editingReview = review)
        }
    }

    fun dismissAddReviewSheet() {
        // W trakcie zapisu nie pozwalamy zamknąć (uniknij gubienia spinnera /
        // nieoczekiwanego dismissa po dwukliku), użytkownik może wrócić.
        if (_uiState.value.isAddingReview) return
        _uiState.update {
            it.copy(showAddReviewSheet = false, addReviewError = null, editingReview = null)
        }
    }

    /**
     * Wysyła nową opinię LUB aktualizuje istniejącą – zależy od
     * `state.editingReview`. Dwa flow celowo dzielą jeden submit, żeby
     * sheet UI był jednym miejscem prawdy o formularzu.
     *
     * Optymistyczne odświeżenie agregatów miejsca tak, jak by zrobiło to
     * repo w transakcji – dzięki temu karta główna pokazuje aktualne dane
     * od razu, bez re-fetcha.
     *
     * @param rating 1..5
     * @param comment treść opinii (opcjonalna; trim-ujemy whitespace).
     */
    fun submitReview(rating: Int, comment: String, photoUris: List<android.net.Uri> = emptyList(), retainedPhotoUrls: List<String> = emptyList()) {
        val place = _uiState.value.place ?: return
        val user = currentUser.value
        if (user == null) {
            _uiState.update {
                it.copy(addReviewError = "Musisz być zalogowany, by dodać opinię")
            }
            return
        }
        if (user.id == place.ownerUserId) {
            _uiState.update {
                it.copy(addReviewError = "Nie możesz oceniać miejsca dodanego przez Ciebie")
            }
            return
        }
        if (rating !in 1..5) {
            _uiState.update {
                it.copy(addReviewError = "Wybierz ocenę 1–5 gwiazdek")
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
        // Upload zdjęć opinii (jeśli są) – z dedup wśród nowych
        val uploadedPhotoUrls = mutableListOf<String>()
        val newHashes = mutableSetOf<String>()
        for (uri in photoUris) {
            val bytes = imageCompressor.compressToWebp(uri)
            if (bytes != null) {
                val hash = java.security.MessageDigest.getInstance("MD5")
                    .digest(bytes)
                    .joinToString("") { "%02x".format(it) }
                if (hash in newHashes) continue // duplikat wśród nowych
                newHashes.add(hash)
                try {
                    val url = photoUploader.uploadReviewPhoto(
                        ownerUserId = user.id,
                        reviewId = "pending_${System.currentTimeMillis()}",
                        imageBytes = bytes
                    )
                    uploadedPhotoUrls.add(url)
                } catch (_: Exception) { /* best-effort – pomijamy nieudane */ }
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
                // Średnia krocząca (taki sam wzór jak w transakcji repo);
                // przy współbieżnych zapisach z innych klientów stan może
                // chwilowo się rozjechać o 1 – akceptowalne dla MVP, naprawi
                // się przy najbliższym (re-)wejściu na ekran.
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
                    addReviewError = result.error.message
                        ?: "Nie udało się dodać opinii"
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
        // Seed hashów z istniejących retained zdjęć (do dedup)
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
            } catch (_: Exception) { /* best-effort */ }
        }

        // Upload new photos (if any) – z dedup check
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
                } catch (_: Exception) { /* best-effort */ }
            }
        }

        // Jeśli WSZYSTKIE nowe zdjęcia to duplikaty – pokaż błąd i nie zapisuj
        if (reviewDuplicatesSkipped > 0 && newUploadedUrls.isEmpty() && photoUris.isNotEmpty()
            && rating == existing.rating && comment.trim() == existing.comment) {
            // Nic się nie zmieniło (ani tekst, ani zdjęcia) – pokaż snackbar
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

        // Final photo list = retained existing URLs + newly uploaded ones
        val finalPhotoUrls = retainedPhotoUrls + newUploadedUrls

        // Delete removed photos from Storage (best-effort)
        val removedUrls = existing.photoUrls.filter { it !in retainedPhotoUrls }
        for (url in removedUrls) {
            try { photoUploader.deletePhoto(url) } catch (_: Exception) { }
        }

        // Update – zachowujemy id / userId / authorName / placeId / createdAt
        // z istniejącej opinii, nadpisując tylko user-edytowalne pola.
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
                // Delta-form: count się nie zmienia, więc (avg*count - old + new)/count.
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
                    addReviewError = result.error.message
                        ?: "Nie udało się zaktualizować opinii"
                )
            }
        }
    }

    // --- Zgłaszanie spamu ---

    /** Ładuje informacje o tym, co bieżący użytkownik już zgłosił w obrębie tego miejsca. */
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

    // --- Dodawanie zdjęcia do miejsca (przez dowolnego zalogowanego usera) ---

    /** Zbiór MD5 hashów skompresowanych zdjęć miejsca (seedowany przy ładowaniu). */
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

            // Duplicate check: hash skompresowanego zdjęcia
            val newHash = java.security.MessageDigest.getInstance("MD5")
                .digest(newBytes)
                .joinToString("") { "%02x".format(it) }

            if (newHash in placePhotoHashes) {
                _uiState.update { it.copy(isUploadingPlacePhoto = false, placePhotoDuplicateEvent = true) }
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

    /**
     * Usuwa zdjęcie z galerii miejsca. Tylko zdjęcia dodane przez bieżącego
     * usera mogą być usunięte (sprawdzane przez `photoUploadedBy`).
     */
    fun deletePhotoFromPlace(photoUrl: String) {
        val place = _uiState.value.place ?: return
        val user = currentUser.value ?: return

        // Autoryzacja klient-side: tylko własne zdjęcia
        val uploaderId = place.photoUploadedBy[photoUrl]
        if (uploaderId != user.id) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPlacePhoto = true) }

            when (placeRepository.removePhotoUrl(place.id, photoUrl)) {
                is OpResult.Success -> {
                    // Usuń z Storage (best-effort)
                    try { photoUploader.deletePhoto(photoUrl) } catch (_: Exception) {}

                    // Aktualizuj stan optymistycznie
                    _uiState.update {
                        it.copy(
                            place = place.copy(
                                photoUrls = place.photoUrls - photoUrl,
                                photoUploadedBy = place.photoUploadedBy - photoUrl
                            ),
                            isUploadingPlacePhoto = false
                        )
                    }
                    // Re-seed hashów po usunięciu
                    seedPlacePhotoHashes((_uiState.value.place?.photoUrls).orEmpty())
                }
                is OpResult.Failure -> {
                    _uiState.update { it.copy(isUploadingPlacePhoto = false) }
                }
            }
        }
    }

    /** Seeduje hash set istniejących zdjęć miejsca (wołane z init po załadowaniu place). */
    private fun seedPlacePhotoHashes(photoUrls: List<String>) {
        // Reset the set to match current state – this fixes the bug where
        // a deleted photo's hash would remain and block re-adding it.
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
                } catch (_: Exception) { /* best-effort */ }
            }
        }
    }

    // --- Propozycja zmiany / korekta lokalizacji ---

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
}
