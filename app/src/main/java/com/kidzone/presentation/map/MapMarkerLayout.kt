package com.kidzone.presentation.map

import com.google.android.gms.maps.model.LatLng
import com.kidzone.domain.model.Place
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

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

fun buildMapMarkerItems(
    places: List<Place>,
    expandedClusterKey: String?,
    expandedPlaceIds: Set<String> = emptySet(),
    zoom: Float = DEFAULT_CLUSTER_ZOOM
): List<MapMarkerItem> {
    val clusterDecimals = clusterDecimalsForZoom(zoom)
    return places
        .groupBy { place -> clusterKeyFor(place, clusterDecimals) }
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

fun clusterKeyFor(place: Place): String = clusterKeyFor(place, CLUSTER_DECIMALS_EXACT)

private fun clusterKeyFor(place: Place, decimals: Int): String =
    "${place.latitude.roundToDecimals(decimals)}:" +
        place.longitude.roundToDecimals(decimals)

private fun clusterDecimalsForZoom(zoom: Float): Int = when {
    zoom < 7f -> 1
    zoom < 10f -> 2
    zoom < 13f -> 3
    else -> CLUSTER_DECIMALS_EXACT
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
private const val SPIDERFY_RADIUS_DEGREES = 0.00012
private const val SPIDERFY_RADIUS_STEP_DEGREES = 0.000015
private const val SPIDERFY_MAX_EXTRA = 8
