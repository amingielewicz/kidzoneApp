package com.playground.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playground.domain.model.Place
import com.playground.domain.model.PlaceCategory
import com.playground.domain.repository.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel ekranu mapy.
 *
 * Subskrybuje [PlaceRepository.observePlaces] – snapshot listener Firestore
 * automatycznie pushuje nowe miejsca (np. dodane przez `AddPlaceScreen`),
 * dzięki czemu pinezki pojawiają się na żywo bez ręcznego odświeżania.
 *
 * Filtrowanie:
 *  - **kategoria** – po stronie Firestore (`whereEqualTo("category", ...)`).
 *    Zmiana kategorii rebinduje strumień przez `flatMapLatest`, stary
 *    listener jest unsubscribowany.
 *  - **najlepiej oceniane** – po stronie klienta, próg [TOP_RATED_THRESHOLD].
 *    Trzymamy lokalnie, bo Firestore w jednym query nie umie
 *    `whereEqualTo(category) AND whereGreaterThan(averageRating)` bez
 *    composite indexa, a dla MVP nie chcemy go zmuszać tworzyć.
 *
 * Filter "darmowe" jest wzmiankowany w docs ekranu, ale `Place` nie ma na
 * dziś pola ceny – świadomie pomijamy do czasu rozszerzenia modelu.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val placeRepository: PlaceRepository
) : ViewModel() {

    /**
     * @property places aktualnie pokazywane miejsca (po filtrach)
     * @property selectedCategory filtr kategorii; null = wszystkie
     * @property topRatedOnly true = pokaż tylko `averageRating >= 4.0`
     * @property selectedPlaceId id pinezki, na której pokazujemy bottom sheet
     * @property isLoading true do pierwszego emita ze strumienia
     * @property errorMessage komunikat błędu z snapshot listenera
     */
    data class UiState(
        val places: List<Place> = emptyList(),
        val selectedCategory: PlaceCategory? = null,
        val topRatedOnly: Boolean = false,
        val selectedPlaceId: String? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    private val selectedCategory = MutableStateFlow<PlaceCategory?>(null)
    private val topRatedOnly = MutableStateFlow(false)
    private val selectedPlaceId = MutableStateFlow<String?>(null)

    /** Wewnętrzny model wyniku ze strumienia Firestore. */
    private sealed interface PlacesLoad {
        data object Loading : PlacesLoad
        data class Success(val list: List<Place>) : PlacesLoad
        data class Error(val message: String) : PlacesLoad
    }

    private val placesLoad: Flow<PlacesLoad> = selectedCategory
        .flatMapLatest { category ->
            placeRepository.observePlaces(category)
                .map<List<Place>, PlacesLoad> { PlacesLoad.Success(it) }
                .onStart { emit(PlacesLoad.Loading) }
                .catch { e ->
                    emit(PlacesLoad.Error(e.message ?: "Nie udało się wczytać miejsc"))
                }
        }

    val uiState: StateFlow<UiState> = combine(
        placesLoad,
        selectedCategory,
        topRatedOnly,
        selectedPlaceId
    ) { load, category, topOnly, sel ->
        when (load) {
            PlacesLoad.Loading -> UiState(
                selectedCategory = category,
                topRatedOnly = topOnly,
                selectedPlaceId = sel,
                isLoading = true
            )
            is PlacesLoad.Success -> {
                val filtered = if (topOnly) {
                    load.list.filter { it.averageRating >= TOP_RATED_THRESHOLD }
                } else {
                    load.list
                }
                UiState(
                    places = filtered,
                    selectedCategory = category,
                    topRatedOnly = topOnly,
                    // Jeśli wybrane miejsce wypadło z listy po zmianie filtra –
                    // kasujemy zaznaczenie, żeby sheet się zamknął sam.
                    selectedPlaceId = sel?.takeIf { id -> filtered.any { it.id == id } },
                    isLoading = false,
                    errorMessage = null
                )
            }
            is PlacesLoad.Error -> UiState(
                selectedCategory = category,
                topRatedOnly = topOnly,
                selectedPlaceId = sel,
                isLoading = false,
                errorMessage = load.message
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState()
    )

    fun onCategorySelected(category: PlaceCategory?) {
        selectedCategory.value = category
    }

    fun toggleTopRated() {
        topRatedOnly.value = !topRatedOnly.value
    }

    /** Ustaw `null`, żeby zamknąć bottom sheet. */
    fun onPlaceSelected(id: String?) {
        selectedPlaceId.value = id
    }

    private companion object {
        /**
         * Próg "najlepiej oceniane" – świadomie wybrany jako 4.0 (a nie 4.5),
         * żeby przy małej liczbie opinii sekcja nie była pusta. Łatwo zmienić
         * gdy bazka miejsc urośnie.
         */
        const val TOP_RATED_THRESHOLD = 4.0
    }
}
