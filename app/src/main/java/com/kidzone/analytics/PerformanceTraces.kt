package com.kidzone.analytics

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized Firebase Performance custom traces for critical user paths.
 *
 * Custom traces measure latency of key operations that directly impact UX:
 * - Location fetch (GPS → coordinates)
 * - Image compression + upload
 * - Place loading (Firestore → UI)
 * - Review submission
 * - App cold start → first content
 *
 * ## Usage in ViewModel/Repository:
 *
 * ```kotlin
 * val trace = performanceTraces.startTrace(PerformanceTraces.LOCATION_FETCH)
 * val location = fetchCurrentLocation()
 * trace.putAttribute("provider", "fused")
 * performanceTraces.stopTrace(trace)
 * ```
 *
 * ## Usage with inline helper:
 *
 * ```kotlin
 * val result = performanceTraces.measure(PerformanceTraces.PLACE_LOAD) {
 *     placeRepository.getPlace(placeId)
 * }
 * ```
 *
 * Traces are visible in Firebase Console → Performance → Custom traces.
 */
@Singleton
class PerformanceTraces @Inject constructor() {

    private val perf: FirebasePerformance = FirebasePerformance.getInstance()

    /**
     * Starts a named custom trace.
     *
     * @param name trace name (use constants from companion object)
     * @return started [Trace] — caller must call [stopTrace] when done
     */
    fun startTrace(name: String): Trace {
        Timber.d("Perf trace started: $name")
        return perf.newTrace(name).also { it.start() }
    }

    /**
     * Stops a running trace. Safe to call multiple times (no-op after first stop).
     */
    fun stopTrace(trace: Trace) {
        trace.stop()
        Timber.d("Perf trace stopped: ${trace.name}")
    }

    /**
     * Inline helper — measures a suspend block and returns its result.
     *
     * Automatically starts/stops the trace and adds success/failure attribute.
     *
     * ```kotlin
     * val places = performanceTraces.measure(NEARBY_PLACES_LOAD) {
     *     placeRepository.getPlacesNear(lat, lng, radius)
     * }
     * ```
     */
    suspend fun <T> measure(traceName: String, block: suspend () -> T): T {
        val trace = startTrace(traceName)
        return try {
            val result = block()
            trace.putAttribute("status", "success")
            result
        } catch (e: Exception) {
            trace.putAttribute("status", "error")
            trace.putAttribute("error_type", e.javaClass.simpleName)
            throw e
        } finally {
            stopTrace(trace)
        }
    }

    /**
     * Non-suspend version of [measure] for synchronous operations.
     */
    fun <T> measureSync(traceName: String, block: () -> T): T {
        val trace = startTrace(traceName)
        return try {
            val result = block()
            trace.putAttribute("status", "success")
            result
        } catch (e: Exception) {
            trace.putAttribute("status", "error")
            trace.putAttribute("error_type", e.javaClass.simpleName)
            throw e
        } finally {
            stopTrace(trace)
        }
    }

    companion object {
        // ─── Trace names ─────────────────────────────────────────────────

        /** GPS/fused location acquisition (from request to coordinates). */
        const val LOCATION_FETCH = "location_fetch"

        /** Image compression pipeline (decode → rotate → resize → WebP). */
        const val IMAGE_COMPRESS = "image_compress"

        /** Photo upload to Firebase Storage (compressed bytes → download URL). */
        const val PHOTO_UPLOAD = "photo_upload"

        /** Single place load from Firestore (getPlace by ID). */
        const val PLACE_LOAD = "place_load"

        /** Nearby places query (geohash range query + haversine filter). */
        const val NEARBY_PLACES_LOAD = "nearby_places_load"

        /** Review submission (validation + Firestore write). */
        const val REVIEW_SUBMIT = "review_submit"

        /** Add place flow (form submit → Firestore write + photo uploads). */
        const val ADD_PLACE = "add_place"

        /** App cold start to first meaningful content on screen. */
        const val COLD_START = "cold_start"

        /** Remote Config fetch + activate. */
        const val REMOTE_CONFIG_FETCH = "remote_config_fetch"

        /** Ranking/badge context computation (top users + top places fetch). */
        const val BADGE_COMPUTATION = "badge_computation"
    }
}
