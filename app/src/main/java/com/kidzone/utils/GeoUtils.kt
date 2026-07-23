package com.kidzone.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 🎯 Odpowiedzialności:
 * - Obliczanie odległości między punktami geograficznymi.
 * - Udostępnianie stałych matematycznych dla geolokalizacji.
 */
object GeoUtils {

    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Oblicza odległość w kilometrach między dwoma punktami (formuła Haversine).
     */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Oblicza odległość w metrach.
     */
    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int =
        (haversineKm(lat1, lon1, lat2, lon2) * METERS_IN_KM).toInt()

    private const val METERS_IN_KM = 1000

    /**
     * Formatuje współrzędne GPS do czytelnego ciągu znaków.
     */
    fun formatCoordinates(latitude: Double, longitude: Double): String =
        "%.5f, %.5f".format(latitude, longitude)
}
