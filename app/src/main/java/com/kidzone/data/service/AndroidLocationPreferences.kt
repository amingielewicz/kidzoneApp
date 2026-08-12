package com.kidzone.data.service

import android.content.Context
import android.content.SharedPreferences
import com.kidzone.domain.service.LocationPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "location_prefs"
private const val KEY_IP_LAT = "ip_lat"
private const val KEY_IP_LNG = "ip_lng"
private const val KEY_IP_TIMESTAMP = "ip_timestamp"
private const val CACHE_VALIDITY_MS = 24 * 60 * 60 * 1000L // 24 hours

/**
 * Androidowa implementacja [LocationPreferences] oparta na SharedPreferences.
 */
@Singleton
class AndroidLocationPreferences @Inject constructor(
    @ApplicationContext context: Context
) : LocationPreferences {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun saveIpLocation(lat: Double, lng: Double) {
        prefs.edit()
            .putFloat(KEY_IP_LAT, lat.toFloat())
            .putFloat(KEY_IP_LNG, lng.toFloat())
            .putLong(KEY_IP_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    override fun getIpLocation(): Pair<Double, Double>? {
        if (!isIpLocationValid()) return null
        
        val lat = prefs.getFloat(KEY_IP_LAT, Float.NaN)
        val lng = prefs.getFloat(KEY_IP_LNG, Float.NaN)
        
        return if (!lat.isNaN() && !lng.isNaN()) {
            lat.toDouble() to lng.toDouble()
        } else {
            null
        }
    }

    override fun isIpLocationValid(): Boolean {
        val timestamp = prefs.getLong(KEY_IP_TIMESTAMP, 0L)
        return (System.currentTimeMillis() - timestamp) < CACHE_VALIDITY_MS
    }

    override fun clearIpLocation() {
        prefs.edit()
            .remove(KEY_IP_LAT)
            .remove(KEY_IP_LNG)
            .remove(KEY_IP_TIMESTAMP)
            .apply()
    }
}
