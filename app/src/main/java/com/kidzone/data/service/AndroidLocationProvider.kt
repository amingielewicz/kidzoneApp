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

    override fun hasPermission(): Boolean = hasLocationPermission(context)

    override fun isServiceEnabled(): Boolean = isLocationServiceEnabled(context)

    override suspend fun getCurrentLocation(): Pair<Double, Double>? {
        val trace = performanceTraces.startTrace(PerformanceTraces.LOCATION_FETCH)
        val result = fetchCurrentLocation(context)
        trace.putAttribute("has_fix", (result != null).toString())
        performanceTraces.stopTrace(trace)
        return result
    }
}
