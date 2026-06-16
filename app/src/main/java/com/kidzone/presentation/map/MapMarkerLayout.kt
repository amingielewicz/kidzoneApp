package com.kidzone.presentation.map

import com.google.android.gms.maps.model.LatLng
import com.kidzone.domain.model.Place
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

data class MapMarkerItem(
    val key: String,
    val position: LatLng,
    val place: Place?,
    val cluster: MarkerCluster?,
    val isSpiderfied: Boolean
)

data class MarkerCluster(
    val key: String,
    val center: LatLng,
    val places: List<Place>
)

fun clusterCountLabel(count: Int): String = when {
    count >= MAX_CLUSTER_LABEL_THRESHOLD -> "$MAX_CLUSTER_LABEL_THRESHOLD+"
    count >= MIN_ABBREVIATED_CLUSTER_COUNT -> "${count / CLUSTER_LABEL_STEP * CLUSTER_LABEL_STEP}+"
    else -> count.toString()
}

fun buildMapMarkerItems(
    places: List<Place>,
    expandedClusterKey: String?,
    expandedPlaceIds: Set<String> = emptySet(),
    zoom: Float = DEFAULT_CLUSTER_ZOOM
): List<MapMarkerItem> {
    return places
        .groupBy { place -> clusterKeyFor(place, zoom) }
        .flatMap { (key, groupedPlaces) ->
            val center = groupedPlaces.center()
            val isExpandedByPlaceIds = groupedPlaces.all { place -> place.id in expandedPlaceIds }
            if (groupedPlaces.size == 1) {
                listOf(groupedPlaces.first().toMarkerItem(center))
            } else if (isExpandedByPlaceIds && !groupedPlaces.hasSameCoordinates()) {
                groupedPlaces.map { place ->
                    place.toMarkerItem(LatLng(place.latitude, place.longitude))
                        .copy(key = "expanded_${place.id}")
                }
            } else if (isExpandedByPlaceIds) {
                spiderfy(groupedPlaces, center)
            } else if (key == expandedClusterKey) {
                spiderfy(groupedPlaces, center)
            } else {
                listOf(
                    MapMarkerItem(
                        key = "cluster_$key",
                        position = center,
                        place = null,
                        cluster = MarkerCluster(key, center, groupedPlaces),
                        isSpiderfied = false
                    )
                )
            }
        }
}

fun clusterKeyFor(place: Place): String = clusterKeyFor(place, DEFAULT_CLUSTER_ZOOM)

private fun clusterKeyFor(place: Place, zoom: Float): String {
    if (zoom >= INDIVIDUAL_MARKERS_ZOOM) {
        return "${place.latitude.roundToDecimals(CLUSTER_DECIMALS_EXACT)}:" +
            place.longitude.roundToDecimals(CLUSTER_DECIMALS_EXACT)
    }

    val scale = TILE_SIZE_PX * 2.0.pow(zoom.toDouble())
    val x = ((place.longitude + LONGITUDE_OFFSET) / FULL_LONGITUDE_DEGREES) * scale
    val latitudeRadians = Math.toRadians(place.latitude.coerceIn(MIN_MERCATOR_LAT, MAX_MERCATOR_LAT))
    val mercatorY = 0.5 - ln(tan(PI / 4.0 + latitudeRadians / 2.0)) / (2.0 * PI)
    val y = mercatorY * scale
    val gridSize = clusterGridSizeForZoom(zoom)
    return "${floor(x / gridSize).toInt()}:${floor(y / gridSize).toInt()}"
}

private fun clusterGridSizeForZoom(zoom: Float): Double = when {
    zoom < 7f -> COUNTRY_CLUSTER_GRID_PX
    zoom < 10f -> REGION_CLUSTER_GRID_PX
    else -> CITY_CLUSTER_GRID_PX
}

private fun List<Place>.center(): LatLng =
    LatLng(
        sumOf { it.latitude } / size,
        sumOf { it.longitude } / size
    )

private fun List<Place>.hasSameCoordinates(): Boolean {
    val first = firstOrNull() ?: return true
    return all { place ->
        place.latitude == first.latitude && place.longitude == first.longitude
    }
}

private fun Place.toMarkerItem(position: LatLng): MapMarkerItem =
    MapMarkerItem(
        key = "place_$id",
        position = position,
        place = this,
        cluster = null,
        isSpiderfied = false
    )

private fun spiderfy(places: List<Place>, center: LatLng): List<MapMarkerItem> {
    val radius = SPIDERFY_RADIUS_DEGREES + places.size.coerceAtMost(SPIDERFY_MAX_EXTRA) *
        SPIDERFY_RADIUS_STEP_DEGREES
    return places.mapIndexed { index, place ->
        val angle = (2.0 * PI * index) / places.size
        place.toMarkerItem(
            LatLng(
                center.latitude + sin(angle) * radius,
                center.longitude + cos(angle) * radius
            )
        ).copy(
            key = "spider_${place.id}",
            isSpiderfied = true
        )
    }
}

private fun Double.roundToDecimals(decimals: Int): Double {
    val factor = 10.0.pow(decimals)
    return (this * factor).roundToInt() / factor
}

private const val CLUSTER_DECIMALS_EXACT = 5
private const val DEFAULT_CLUSTER_ZOOM = 16f
private const val INDIVIDUAL_MARKERS_ZOOM = 13f
private const val TILE_SIZE_PX = 256.0
private const val COUNTRY_CLUSTER_GRID_PX = 64.0
private const val REGION_CLUSTER_GRID_PX = 72.0
private const val CITY_CLUSTER_GRID_PX = 80.0
private const val LONGITUDE_OFFSET = 180.0
private const val FULL_LONGITUDE_DEGREES = 360.0
private const val MAX_MERCATOR_LAT = 85.05112878
private const val MIN_MERCATOR_LAT = -85.05112878
private const val MIN_ABBREVIATED_CLUSTER_COUNT = 10
private const val CLUSTER_LABEL_STEP = 10
private const val MAX_CLUSTER_LABEL_THRESHOLD = 90
private const val SPIDERFY_RADIUS_DEGREES = 0.00012
private const val SPIDERFY_RADIUS_STEP_DEGREES = 0.000015
private const val SPIDERFY_MAX_EXTRA = 8
