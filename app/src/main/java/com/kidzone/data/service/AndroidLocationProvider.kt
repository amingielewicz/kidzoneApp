package com.kidzone.data.service

import android.content.Context
import com.kidzone.analytics.PerformanceTraces
import com.kidzone.domain.service.LocationProvider
import com.kidzone.presentation.place.add.fetchCurrentLocation
import com.kidzone.presentation.place.add.hasLocationPermission
import com.kidzone.presentation.place.add.isLocationServiceEnabled
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val ONE_MINUTE_MILLIS = 60_000L

/**
 * Android implementation of [LocationProvider].
 *
 * Delegates to existing [hasLocationPermission], [isLocationServiceEnabled],
 * and [fetchCurrentLocation] helpers from LocationHelper.kt.
 * Wraps getCurrentLocation with Firebase Performance trace.
 */
@Singleton
class AndroidLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performanceTraces: PerformanceTraces
) : LocationProvider {

    private var lastKnownLocation: Pair<Double, Double>? = null
    private var lastKnownLocationTimestampMillis: Long? = null

    override fun hasPermission(): Boolean = hasLocationPermission(context)

    override fun isServiceEnabled(): Boolean = isLocationServiceEnabled(context)

    override suspend fun getCurrentLocation(): Pair<Double, Double>? {
        val trace = performanceTraces.startTrace(PerformanceTraces.LOCATION_FETCH)
        val result = fetchCurrentLocation(context)

        if (result != null) {
            lastKnownLocation = result
            lastKnownLocationTimestampMillis = System.currentTimeMillis()
        }

        trace.putAttribute("has_fix", (result != null).toString())
        performanceTraces.stopTrace(trace)

        return result
    }

    override fun getLastKnownLocation(): Pair<Double, Double>? = lastKnownLocation

    override fun getLastKnownLocationAgeMinutes(): Int? {
        val timestamp = lastKnownLocationTimestampMillis ?: return null

        return ((System.currentTimeMillis() - timestamp) / ONE_MINUTE_MILLIS)
            .toInt()
            .coerceAtLeast(1)
    }
}