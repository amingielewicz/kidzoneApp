package com.kidzone.data.remote

data class PerformanceConfig(
    val homeNearbyLimit: Int = HOME_NEARBY_LIMIT_DEFAULT,
    val homeTopPlacesLimit: Int = HOME_TOP_PLACES_LIMIT_DEFAULT,
    val homeRecentlyAddedLimit: Int = HOME_RECENTLY_ADDED_LIMIT_DEFAULT,
    val homeTopPlacesRadiusKm: Double = HOME_TOP_PLACES_RADIUS_KM_DEFAULT,
    val homeFetchRadiusKm: Double = HOME_FETCH_RADIUS_KM_DEFAULT,
    val mapMarkersLimit: Int = MAP_MARKERS_LIMIT_DEFAULT,
    val rankingTopLimit: Int = RANKING_TOP_LIMIT_DEFAULT,
    val rankingFetchPool: Int = RANKING_FETCH_POOL_DEFAULT
) {
    companion object {
        const val HOME_NEARBY_LIMIT_DEFAULT = 20
        const val HOME_TOP_PLACES_LIMIT_DEFAULT = 20
        const val HOME_RECENTLY_ADDED_LIMIT_DEFAULT = 10
        const val HOME_TOP_PLACES_RADIUS_KM_DEFAULT = 10.0
        const val HOME_FETCH_RADIUS_KM_DEFAULT = 50.0
        const val MAP_MARKERS_LIMIT_DEFAULT = 1000
        const val RANKING_TOP_LIMIT_DEFAULT = 100
        const val RANKING_FETCH_POOL_DEFAULT = 200

        const val KEY_HOME_NEARBY_LIMIT = "perf_home_nearby_limit"
        const val KEY_HOME_TOP_PLACES_LIMIT = "perf_home_top_places_limit"
        const val KEY_HOME_RECENTLY_ADDED_LIMIT = "perf_home_recently_added_limit"
        const val KEY_HOME_TOP_PLACES_RADIUS_KM = "perf_home_top_places_radius_km"
        const val KEY_HOME_FETCH_RADIUS_KM = "perf_home_fetch_radius_km"
        const val KEY_MAP_MARKERS_LIMIT = "perf_map_markers_limit"
        const val KEY_RANKING_TOP_LIMIT = "perf_ranking_top_limit"
        const val KEY_RANKING_FETCH_POOL = "perf_ranking_fetch_pool"
    }
}

interface PerformanceConfigProvider {
    val performanceConfig: PerformanceConfig
}
