package com.kidzone.data.remote

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.kidzone.analytics.PerformanceTraces
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralny dostęp do Firebase Remote Config.
 * Feature flags i maintenance mode.
 */
@Singleton
class RemoteConfigService @Inject constructor(
    private val performanceTraces: PerformanceTraces
) {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (com.kidzone.BuildConfig.DEBUG) 0 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(mapOf(
            "maintenance_mode" to false,
            "maintenance_message" to "Aplikacja jest chwilowo niedostępna. Spróbuj ponownie później.",
            "max_photos_per_place" to 10L,
            "max_review_length" to 500L,
            "enable_subscriptions" to false,
            "min_app_version" to "0.1.0",
            // A/B Testing experiment defaults
            "exp_home_layout" to "control",
            "exp_review_photos_limit" to "3",
            "exp_add_place_cta" to "control"
        ))
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
}
