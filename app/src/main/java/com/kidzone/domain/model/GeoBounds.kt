@file:Suppress("MagicNumber")

package com.kidzone.domain.model

/**
 * Prostokątny obszar mapy. Długości geograficzne mogą przecinać południk 180°.
 */
data class GeoBounds(
    val north: Double,
    val east: Double,
    val south: Double,
    val west: Double
) {
    init {
        require(north in -90.0..90.0)
        require(south in -90.0..90.0)
        require(east in -180.0..180.0)
        require(west in -180.0..180.0)
        require(north >= south)
    }

    val centerLatitude: Double
        get() = (north + south) / 2.0

    val centerLongitude: Double
        get() {
            if (west <= east) return (west + east) / 2.0
            val wrappedEast = east + 360.0
            val center = (west + wrappedEast) / 2.0
            return if (center > 180.0) center - 360.0 else center
        }

    fun contains(latitude: Double, longitude: Double): Boolean {
        if (latitude !in south..north) return false
        return if (west <= east) {
            longitude in west..east
        } else {
            longitude >= west || longitude <= east
        }
    }
}
