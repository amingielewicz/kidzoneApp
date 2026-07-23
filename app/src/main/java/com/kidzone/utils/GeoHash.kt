package com.kidzone.utils

/**
 * 🎯 Odpowiedzialności:
 * - Kodowanie współrzędnych geograficznych (lat, lng) do formatu Geohash.
 * - Wyznaczanie optymalnej długości prefixu dla zapytań o promieniu.
 *
 * ⚙️ Techniczne:
 * - Wykorzystuje bazę BASE32 (standard Geohash).
 * - Precision 7 ≈ 150m (używane do zapisu w Firestore).
 * - Algorytm czysto obliczeniowy, brak zewnętrznych zależności.
 */
object GeoHash {

    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    /**
     * Koduje (latitude, longitude) w geohash o zadanej precyzji (domyślnie 7).
     */
    fun encode(latitude: Double, longitude: Double, precision: Int = 7): String {
        var latMin = -90.0
        var latMax = 90.0
        var lngMin = -180.0
        var lngMax = 180.0
        var isLng = true
        var bit = 0
        var charIndex = 0
        val hash = StringBuilder()

        while (hash.length < precision) {
            if (isLng) {
                val mid = (lngMin + lngMax) / 2
                if (longitude >= mid) {
                    charIndex = charIndex or (1 shl (4 - bit))
                    lngMin = mid
                } else {
                    lngMax = mid
                }
            } else {
                val mid = (latMin + latMax) / 2
                if (latitude >= mid) {
                    charIndex = charIndex or (1 shl (4 - bit))
                    latMin = mid
                } else {
                    latMax = mid
                }
            }
            isLng = !isLng
            bit++
            if (bit == 5) {
                hash.append(BASE32[charIndex])
                bit = 0
                charIndex = 0
            }
        }
        return hash.toString()
    }

    /**
     * Zwraca rekomendowaną długość prefixu geohash dla podanego promienia.
     *
     * Mapowanie jest przybliżone – celowo bierzemy krótszy prefix (większy
     * prostokąt) żeby nie wyciąć miejsc na granicy bucketu. Klient i tak
     * robi dokładne filtrowanie haversine.
     */
    fun prefixLengthForRadius(radiusKm: Double): Int = when {
        radiusKm <= 0.5 -> 6   // ~1.2 km bucket
        radiusKm <= 5.0 -> 5   // ~5 km bucket
        radiusKm <= 20.0 -> 4  // ~39 km bucket
        radiusKm <= 100.0 -> 3 // ~156 km bucket
        else -> 2              // ~600 km bucket
    }
}
