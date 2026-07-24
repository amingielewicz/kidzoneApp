package com.kidzone.utils

import android.content.Context
import com.kidzone.R

/**
 * 🎯 Odpowiedzialności:
 * - Centralizacja logiki formatowania dystansów dla komponentów nie-UI (np. Widgety).
 */
object DistanceUtils {

    private const val VERY_CLOSE_DISTANCE_METERS = 50
    private const val METER_THRESHOLD = 1000
    private const val DISTANCE_ROUNDING_OFFSET_METERS = 25
    private const val DISTANCE_ROUNDING_STEP_METERS = 50
    private const val METERS_IN_KM = 1000.0
    private const val KM_INTEGER_THRESHOLD = 100.0

    /**
     * Formatuje dystans w metrach na czytelny tekst.
     */
    fun formatDistanceMeters(context: Context, meters: Int): String {
        return when {
            meters <= VERY_CLOSE_DISTANCE_METERS -> context.getString(R.string.very_close_distance)
            meters < METER_THRESHOLD -> {
                val rounded = (
                    (meters + DISTANCE_ROUNDING_OFFSET_METERS) /
                        DISTANCE_ROUNDING_STEP_METERS
                    ) * DISTANCE_ROUNDING_STEP_METERS
                if (rounded == 0) context.getString(R.string.very_close_distance)
                else context.getString(R.string.distance_m, rounded)
            }
            else -> {
                val km = meters / METERS_IN_KM
                if (km < KM_INTEGER_THRESHOLD) {
                    context.getString(R.string.distance_km, km)
                } else {
                    context.getString(R.string.distance_km_integer, km.toInt())
                }
            }
        }
    }

    /**
     * Formatuje wiek lokalizacji.
     */
    fun formatStaleAge(context: Context, ageMinutes: Int): String {
        return if (ageMinutes <= 1) {
            context.getString(R.string.stale_age_one_minute)
        } else {
            context.getString(R.string.stale_age_minutes, ageMinutes)
        }
    }
}
