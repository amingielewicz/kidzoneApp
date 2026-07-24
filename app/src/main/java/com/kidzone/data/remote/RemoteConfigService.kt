package com.kidzone.data.remote

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.kidzone.analytics.PerformanceTraces
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🎯 Odpowiedzialności:
 * - Centralny dostęp do konfiguracji zdalnej Firebase Remote Config.
 * - Zarządzanie flagami funkcji (Feature Flags) i trybem konserwacji (Maintenance Mode).
 * - Dostarczanie parametrów wydajnościowych (PerformanceConfig) dla ekranów UI.
 *
 * 🛡️ Bezpieczeństwo i Prywatność:
 * - Nie pobiera ani nie wysyła danych PII.
 * - Używa anonimowych parametrów do segmentacji konfiguracji.
 *
 * ⚡ Wydajność i Zasoby:
 * - Inteligentne interwały odświeżania (0s w Debug, 1h w Release).
 * - Śledzenie wydajności pobierania poprzez [PerformanceTraces].
 *
 * ✅ Gwarancje:
 * - Bezpieczne wartości domyślne (fallback) przy braku połączenia sieciowego.
 * - Walidacja zakresów wartości dla parametrów technicznych.
 */
@Singleton
class RemoteConfigService @Inject constructor(
    private val performanceTraces: PerformanceTraces
) : PerformanceConfigProvider {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (com.kidzone.BuildConfig.DEBUG) 0 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(
            mapOf(
                "maintenance_mode" to false,
                "maintenance_message" to "",
                "max_photos_per_place" to 10L,
                "max_review_length" to 500L,
                "enable_subscriptions" to false,
                "min_app_version" to "0.1.0",
                PerformanceConfig.KEY_HOME_NEARBY_LIMIT to
                    PerformanceConfig.HOME_NEARBY_LIMIT_DEFAULT.toLong(),
                PerformanceConfig.KEY_HOME_TOP_PLACES_LIMIT to
                    PerformanceConfig.HOME_TOP_PLACES_LIMIT_DEFAULT.toLong(),
                PerformanceConfig.KEY_HOME_RECENTLY_ADDED_LIMIT to
                    PerformanceConfig.HOME_RECENTLY_ADDED_LIMIT_DEFAULT.toLong(),
                PerformanceConfig.KEY_HOME_TOP_PLACES_RADIUS_KM to
                    PerformanceConfig.HOME_TOP_PLACES_RADIUS_KM_DEFAULT,
                PerformanceConfig.KEY_HOME_FETCH_RADIUS_KM to
                    PerformanceConfig.HOME_FETCH_RADIUS_KM_DEFAULT,
                PerformanceConfig.KEY_MAP_MARKERS_LIMIT to
                    PerformanceConfig.MAP_MARKERS_LIMIT_DEFAULT.toLong(),
                PerformanceConfig.KEY_RANKING_TOP_LIMIT to
                    PerformanceConfig.RANKING_TOP_LIMIT_DEFAULT.toLong(),
                PerformanceConfig.KEY_RANKING_FETCH_POOL to
                    PerformanceConfig.RANKING_FETCH_POOL_DEFAULT.toLong(),
                // A/B Testing experiment defaults
                "exp_home_layout" to "control",
                "exp_review_photos_limit" to "3",
                "exp_add_place_cta" to "control"
            )
        )
    }

    suspend fun fetchAndActivate() {
        val trace = performanceTraces.startTrace(PerformanceTraces.REMOTE_CONFIG_FETCH)
        try {
            remoteConfig.fetchAndActivate().await()
            trace.putAttribute("status", "success")
            Timber.d("Remote Config fetched and activated")
        } catch (e: Exception) {
            trace.putAttribute("status", "failed")
            Timber.w(e, "Remote Config fetch failed, using cached/default values")
        } finally {
            performanceTraces.stopTrace(trace)
        }
    }

    val isMaintenanceMode: Boolean
        get() = remoteConfig.getBoolean("maintenance_mode")

    val maintenanceMessage: String
        get() = remoteConfig.getString("maintenance_message")

    val maxPhotosPerPlace: Int
        get() = remoteConfig.getLong("max_photos_per_place").toInt()

    val maxReviewLength: Int
        get() = remoteConfig.getLong("max_review_length").toInt()

    val enableSubscriptions: Boolean
        get() = remoteConfig.getBoolean("enable_subscriptions")

    val minAppVersion: String
        get() = remoteConfig.getString("min_app_version")

    override val performanceConfig: PerformanceConfig
        get() = PerformanceConfig(
            homeNearbyLimit = getInt(
                key = PerformanceConfig.KEY_HOME_NEARBY_LIMIT,
                default = PerformanceConfig.HOME_NEARBY_LIMIT_DEFAULT,
                range = 5..40
            ),
            homeTopPlacesLimit = getInt(
                key = PerformanceConfig.KEY_HOME_TOP_PLACES_LIMIT,
                default = PerformanceConfig.HOME_TOP_PLACES_LIMIT_DEFAULT,
                range = 5..40
            ),
            homeRecentlyAddedLimit = getInt(
                key = PerformanceConfig.KEY_HOME_RECENTLY_ADDED_LIMIT,
                default = PerformanceConfig.HOME_RECENTLY_ADDED_LIMIT_DEFAULT,
                range = 3..30
            ),
            homeTopPlacesRadiusKm = getDouble(
                key = PerformanceConfig.KEY_HOME_TOP_PLACES_RADIUS_KM,
                default = PerformanceConfig.HOME_TOP_PLACES_RADIUS_KM_DEFAULT,
                min = 1.0,
                max = 50.0
            ),
            homeFetchRadiusKm = getDouble(
                key = PerformanceConfig.KEY_HOME_FETCH_RADIUS_KM,
                default = PerformanceConfig.HOME_FETCH_RADIUS_KM_DEFAULT,
                min = 5.0,
                max = 100.0
            ),
            mapMarkersLimit = getInt(
                key = PerformanceConfig.KEY_MAP_MARKERS_LIMIT,
                default = PerformanceConfig.MAP_MARKERS_LIMIT_DEFAULT,
                range = 100..2_000
            ),
            rankingTopLimit = getInt(
                key = PerformanceConfig.KEY_RANKING_TOP_LIMIT,
                default = PerformanceConfig.RANKING_TOP_LIMIT_DEFAULT,
                range = 10..200
            ),
            rankingFetchPool = getInt(
                key = PerformanceConfig.KEY_RANKING_FETCH_POOL,
                default = PerformanceConfig.RANKING_FETCH_POOL_DEFAULT,
                range = 20..500
            )
        ).let { config ->
            if (config.rankingFetchPool >= config.rankingTopLimit) {
                config
            } else {
                config.copy(rankingFetchPool = config.rankingTopLimit)
            }
        }

    private fun getInt(key: String, default: Int, range: IntRange): Int =
        remoteConfig.getLong(key)
            .takeIf { it in range.first.toLong()..range.last.toLong() }
            ?.toInt()
            ?: default

    private fun getDouble(key: String, default: Double, min: Double, max: Double): Double =
        remoteConfig.getDouble(key)
            .takeIf { it in min..max }
            ?: default
}
