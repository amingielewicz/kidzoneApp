@file:Suppress("MagicNumber")

package com.kidzone.domain.model

import com.kidzone.utils.GeoHash
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

/**
 * Prostokątny obszar geograficzny używany przez mapę i zapytania viewportu.
 *
 * Długości geograficzne mogą przecinać południk 180°, dlatego zakres z [west] większym od [east]
 * jest interpretowany jako obszar zawijający się przez linię zmiany daty.
 *
 * @property north północna granica w zakresie od -90 do 90 stopni.
 * @property east wschodnia granica w zakresie od -180 do 180 stopni.
 * @property south południowa granica w zakresie od -90 do 90 stopni.
 * @property west zachodnia granica w zakresie od -180 do 180 stopni.
 * @throws IllegalArgumentException gdy granice są poza zakresem albo [north] jest mniejsze od [south].
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

    /** Środkowa szerokość geograficzna viewportu. */
    val centerLatitude: Double
        get() = (north + south) / 2.0

    /**
     * Środkowa długość geograficzna viewportu z obsługą obszaru przecinającego południk 180°.
     */
    val centerLongitude: Double
        get() {
            if (west <= east) return (west + east) / 2.0
            val wrappedEast = east + 360.0
            val center = (west + wrappedEast) / 2.0
            return if (center > 180.0) center - 360.0 else center
        }

    /**
     * Sprawdza, czy współrzędne znajdują się w granicach viewportu.
     *
     * @param latitude szerokość geograficzna punktu.
     * @param longitude długość geograficzna punktu.
     * @return `true`, gdy punkt należy do obszaru, również przy zawinięciu przez południk 180°.
     */
    fun contains(latitude: Double, longitude: Double): Boolean {
        if (latitude !in south..north) return false
        return if (west <= east) {
            longitude in west..east
        } else {
            longitude >= west || longitude <= east
        }
    }

    /**
     * Sprawdza, czy dwa viewporty są wystarczająco podobne, aby pominąć kolejne zapytanie.
     *
     * @param other porównywany obszar.
     * @param threshold maksymalna różnica każdej granicy w stopniach.
     */
    fun isSimilarTo(other: GeoBounds, threshold: Double = 0.001): Boolean {
        return abs(north - other.north) < threshold &&
            abs(east - other.east) < threshold &&
            abs(south - other.south) < threshold &&
            abs(west - other.west) < threshold
    }

    /**
     * Generuje prefiks geohasha dla centrum viewportu.
     *
     * Precyzja maleje wraz z rozmiarem obszaru, co pozwala grupować podobne viewporty w cache i
     * ograniczać liczbę zapytań podczas ruchu mapy.
     *
     * @return geohash centrum o precyzji dopasowanej do przybliżonego promienia viewportu.
     */
    fun geohashPrefix(): String {
        val latSpan = north - south
        val lngSpan = if (west <= east) east - west else 360.0 - west + east
        val radiusKm = max(
            latSpan * 111.0,
            lngSpan * 111.0 * cos(Math.toRadians(centerLatitude))
        ) / 2.0
        val precision = when {
            radiusKm < 10 -> 5
            radiusKm < 50 -> 4
            else -> 3
        }
        return GeoHash.encode(centerLatitude, centerLongitude, precision)
    }
}
