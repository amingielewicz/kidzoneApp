package com.kidzone.domain.repository

import com.kidzone.utils.OpResult

/**
 * 🎯 Odpowiedzialności:
 * - Pobieranie przybliżonej lokalizacji użytkownika na podstawie adresu IP.
 * - Wykorzystywane jako fallback, gdy brak uprawnień GPS.
 */
interface IpLocationRepository {
    /**
     * Zwraca parę współrzędnych (szerokość, długość) lub błąd.
     */
    suspend fun getApproximateLocation(): OpResult<Pair<Double, Double>>
}
