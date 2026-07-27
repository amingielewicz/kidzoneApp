package com.kidzone.domain.service

import com.kidzone.di.ApplicationScope
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.ReviewRepository
import com.kidzone.data.remote.PerformanceConfigProvider
import com.kidzone.utils.OpResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🎯 Odpowiedzialności:
 * - Proaktywne pobieranie danych z serwera i wypełnianie lokalnego cache Room.
 * - Zapewnienie dostępności danych offline natychmiast po uruchomieniu aplikacji.
 * - Koordynacja pobierania danych globalnych oraz prywatnych danych użytkownika.
 *
 * ✅ Gwarancje:
 * - Działa w tle, nie blokując głównego wątku ani nawigacji (np. Onboarding).
 * - Automatycznie synchronizuje metadane miejsc dla opinii użytkownika.
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

    /**
     * Uruchamia proces wstępnego pobierania danych.
     * Metoda jest bezpieczna do wielokrotnego wywołania (idempotentna w ramach jednej sesji).
     */
    fun startPrefetch() {
        if (isPrefetching) return
        isPrefetching = true

        externalScope.launch {
            try {
                Timber.d("Starting global data prefetch...")
                prefetchGlobalData()
                
                authRepository.currentUser.first()?.let { user ->
                    Timber.d("Starting user data prefetch for uid=${user.id}...")
                    prefetchUserData(user.id)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Data prefetch failed partially")
            } finally {
                isPrefetching = false
            }
        }
    }

    private suspend fun prefetchGlobalData() {
        val pool = performanceConfigProvider.performanceConfig.rankingFetchPool
        
        // 1. Rankingi (Miejsca i Użytkownicy)
        placeRepository.getTopPlaces(limit = pool)
        authRepository.getTopUsers(limit = pool)
        
        // 2. Najnowsze miejsca (pierwsza strona)
        placeRepository.getPlacesPage(pageSize = 20, cursor = null, category = null, query = null)
        
        // 3. Domyślna lokalizacja dla Mapy (Warszawa) jako fallback
        @Suppress("MagicNumber")
        placeRepository.getPlacesNear(52.2297, 21.0122, radiusKm = 10.0)
    }

    private suspend fun prefetchUserData(userId: String) {
        // 1. Synchronizuj pełny profil użytkownika (wypełnia UserDao cache)
        authRepository.getUserById(userId)

        // 2. Synchronizuj własne miejsca użytkownika (One-shot sync do Room)
        placeRepository.syncPlacesByOwner(userId)
        
        // 3. Synchronizuj własne opinie użytkownika (One-shot sync do Room)
        val reviewsResult = reviewRepository.syncReviewsByUser(userId)
        
        // 4. Metadane miejsc dla tych opinii (nazwa, kategoria)
        if (reviewsResult is OpResult.Success) {
            reviewsResult.data.forEach { review ->
                placeRepository.getPlace(review.placeId)
            }
        }
    }
}
