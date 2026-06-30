@file:Suppress("MagicNumber")

package com.kidzone.domain.model

import com.kidzone.utils.GeoHash
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

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

    /**
     * Sprawdza czy prostokąt jest podobny (do de-bounce ruchów mapy).
     */
    fun isSimilarTo(other: GeoBounds, threshold: Double = 0.001): Boolean {
        return abs(north - other.north) < threshold &&
            abs(east - other.east) < threshold &&
            abs(south - other.south) < threshold &&
            abs(west - other.west) < threshold
    }

    /**
     * Generuje prefiks geohasha dla centrum z precyzją zależną od rozmiaru.
     */
    fun geohashPrefix(): String {
        val latSpan = north - south
        val lngSpan = if (west <= east) east - west else 360.0 - west + east
        val radiusKm = max(latSpan * 111.0, lngSpan * 111.0 * cos(Math.toRadians(centerLatitude))) / 2.0
        val precision = when {
            radiusKm < 10 -> 5
            radiusKm < 50 -> 4
            else -> 3
        }
        return GeoHash.encode(centerLatitude, centerLongitude, precision)
    }
}
