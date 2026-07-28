package com.kidzone.domain.service

import com.kidzone.di.ApplicationScope
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.ReviewRepository
import com.kidzone.data.remote.PerformanceConfigProvider
import com.kidzone.utils.OpResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay

/**
 * 🎯 Odpowiedzialności:
 * - Proaktywne pobieranie danych z serwera i wypełnianie lokalnego cache Room.
 * - Zapewnienie dostępności danych offline natychmiast po uruchomieniu aplikacji.
 * - Koordynacja pobierania danych globalnych oraz prywatnych danych użytkownika.
 * - Monitorowanie stanu sieci w celu dokończenia przerwanego pobierania.
 *
 * ✅ Gwarancje:
 * - Działa w tle, nie blokując głównego wątku ani nawigacji (np. Onboarding).
 * - Automatycznie synchronizuje metadane miejsc dla opinii użytkownika.
 * - Idempotentność: nie powtarza udanego pobierania w ramach tej samej sesji.
 *
 * 🧵 Wątki:
 * - Wykorzystuje [ApplicationScope], dzięki czemu proces nie jest przerywany przy zmianie ekranów.
 */
@Singleton
@Suppress("TooGenericExceptionCaught")
class DataPrefetchService @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository,
    private val performanceConfigProvider: PerformanceConfigProvider,
    @ApplicationScope private val externalScope: CoroutineScope
) {
    private var isPrefetching = false
    private var globalDataPrefetched = false
    private var userDataPrefetched = false

    /**
     * Uruchamia proces wstępnego pobierania danych.
     * Reaguje na zmiany stanu zalogowania.
     */
    fun startPrefetch() {
        externalScope.launch {
            authRepository.currentUser.collectLatest { user ->
                if (user != null) {
                    performPrefetch(user.id)
                } else {
                    // Public prefetch only
                    performPublicPrefetch()
                }
            }
        }
    }

    private suspend fun performPublicPrefetch() {
        if (globalDataPrefetched || isPrefetching) return
        try {
            isPrefetching = true
            Timber.d("Starting global data prefetch...")
            prefetchGlobalData()
            globalDataPrefetched = true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Global data prefetch failed")
        } finally {
            isPrefetching = false
        }
    }

    private suspend fun performPrefetch(userId: String) {
        if (userDataPrefetched && globalDataPrefetched) return
        
        try {
            isPrefetching = true
            
            if (!globalDataPrefetched) {
                Timber.d("Starting global data prefetch...")
                prefetchGlobalData()
                globalDataPrefetched = true
            }

            Timber.d("Starting user data prefetch for uid=$userId...")
            prefetchUserData(userId)
            userDataPrefetched = true
            
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Prefetch failed partially - will retry on next check")
        } finally {
            isPrefetching = false
        }
    }

    private suspend fun prefetchGlobalData() {
        val pool = performanceConfigProvider.performanceConfig.rankingFetchPool

        coroutineScope {
            // 1. Globalne rankingi
            launch { placeRepository.getTopPlaces(limit = pool) }
            launch { authRepository.getTopUsers(limit = pool) }

            // 2. Najnowsze miejsca (globalnie)
            launch { placeRepository.getPlacesPage(pageSize = 20, cursor = null, category = null, query = null) }

            // 3. Domyślna lokalizacja dla Mapy (Warszawa) - zwiekszamy promien do 30km
            launch {
                @Suppress("MagicNumber")
                placeRepository.getPlacesNear(52.2297, 21.0122, radiusKm = 30.0)
            }
        }
    }

    private suspend fun prefetchUserData(userId: String) {
        coroutineScope {
            // 1. Synchronizuj pełny profil użytkownika
            launch { authRepository.getUserById(userId) }

            // 2. Synchronizuj WSZYSTKIE własne miejsca użytkownika
            launch { placeRepository.syncPlacesByOwner(userId) }

            // 3. Synchronizuj WSZYSTKIE własne opinie użytkownika
            launch {
                val reviewsResult = reviewRepository.syncReviewsByUser(userId)
                if (reviewsResult is OpResult.Success) {
                    reviewsResult.data.forEach { review ->
                        // Pobieramy metadane miejsc w tle
                        launch { placeRepository.getPlace(review.placeId) }
                    }
                }
            }
        }
    }
}
