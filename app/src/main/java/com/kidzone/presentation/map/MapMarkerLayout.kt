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
    expandedClusterKey: String?
): List<MapMarkerItem> {
    return places
        .groupBy(::clusterKeyFor)
        .flatMap { (key, groupedPlaces) ->
            val center = groupedPlaces.first().let { LatLng(it.latitude, it.longitude) }
            if (groupedPlaces.size == 1) {
                listOf(groupedPlaces.first().toMarkerItem(center))
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

fun clusterKeyFor(place: Place): String =
    "${place.latitude.roundToDecimals(CLUSTER_DECIMALS)}:" +
        place.longitude.roundToDecimals(CLUSTER_DECIMALS)

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

private const val CLUSTER_DECIMALS = 5
private const val SPIDERFY_RADIUS_DEGREES = 0.00012
private const val SPIDERFY_RADIUS_STEP_DEGREES = 0.000015
private const val SPIDERFY_MAX_EXTRA = 8
