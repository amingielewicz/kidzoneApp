package com.kidzone.domain.service

/**
 * 🎯 Odpowiedzialności:
 * - Przechowywanie ostatnio wykrytej przybliżonej lokalizacji (IP).
 * - Zarządzanie czasem ważności cache'u lokalizacji.
 */
interface LocationPreferences {
    fun saveIpLocation(lat: Double, lng: Double)
    fun getIpLocation(): Pair<Double, Double>?
    fun isIpLocationValid(): Boolean
    fun clearIpLocation()
}
