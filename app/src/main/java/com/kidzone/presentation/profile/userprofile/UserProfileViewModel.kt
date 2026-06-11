package com.kidzone.presentation.profile.userprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.navigation.Route
import com.kidzone.presentation.common.BadgeContext
import com.kidzone.presentation.common.UserBadge
import com.kidzone.presentation.common.computeBadges
import com.kidzone.utils.OpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel ekranu publicznego profilu użytkownika.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val placeRepository: PlaceRepository
) : ViewModel() {

    data class UiState(
        val user: User? = null,
        val obtainedBadges: List<UserBadge> = emptyList(),
        val userRank: Int? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val userId: String = checkNotNull(savedStateHandle[Route.UserProfile.ARG_USER_ID])

    init {
        observeUser()
    }

    private fun observeUser() {
        viewModelScope.launch {
            authRepository.observeUser(userId)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Błąd ładowania profilu") }
                }
                .collectLatest { user ->
                    if (user == null) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Użytkownik nie istnieje") }
                    } else {
                        val context = computeBadgeContext(user)
                        _uiState.update {
                            it.copy(
                                user = user,
                                obtainedBadges = user.computeBadges(context),
                                userRank = context.userRank,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
                }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        observeUser()
    }

    private suspend fun computeBadgeContext(user: User): BadgeContext {
        return try {
            val users = (authRepository.getTopUsers(BADGE_RANK_POOL) as? OpResult.Success)
                ?.data
                .orEmpty()
                .filter { it.placesAddedCount > 0 || it.reviewsCount > 0 }
            val userRank = users.indexOfFirst { it.id == user.id }
                .takeIf { it >= 0 }
                ?.plus(1)

            val topPlaces = (placeRepository.getTopPlaces(BADGE_RANK_POOL) as? OpResult.Success)
                ?.data
                .orEmpty()
                .filter { it.reviewsCount > 0 && it.averageRating > 0.0 }
            val bestPlaceRank = topPlaces
                .mapIndexedNotNull { idx, place ->
                    if (place.ownerUserId == user.id) idx + 1 else null
                }
                .minOrNull()

            BadgeContext(userRank = userRank, bestPlaceRank = bestPlaceRank)
        } catch (_: Exception) {
            BadgeContext()
        }
    }

    private companion object {
        const val BADGE_RANK_POOL = 100
    }
}
