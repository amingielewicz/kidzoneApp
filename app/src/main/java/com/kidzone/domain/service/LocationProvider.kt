package com.kidzone.domain.service

/**
 * Abstraction over device location services.
 *
 * Decouples ViewModels from Android Context, LocationManager, and
 * FusedLocationProviderClient. Enables unit testing without Robolectric.
 */
interface LocationProvider {
    /** True if the app has ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION. */
    fun hasPermission(): Boolean

    /** True if GPS or Network location provider is enabled in system settings. */
    fun isServiceEnabled(): Boolean

    /**
     * Fetches current device location.
     * @return (latitude, longitude) pair, or null if unavailable/timed out.
     */
    suspend fun getCurrentLocation(): Pair<Double, Double>?

    /** Last location successfully fetched by the app. */
    fun getLastKnownLocation(): Pair<Double, Double>?

    /** Age of last location in minutes. */
    fun getLastKnownLocationAgeMinutes(): Int?
}
