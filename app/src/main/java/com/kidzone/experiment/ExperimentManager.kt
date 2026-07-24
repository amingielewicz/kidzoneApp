package com.kidzone.experiment

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie przypisaniem użytkowników do wariantów testów A/B.
 * - Logowanie ekspozycji (Exposure) do Firebase Analytics dla celów metrycznych.
 * - Synchronizacja wariantów z właściwościami użytkownika (User Properties).
 *
 * 🛡️ Bezpieczeństwo i Prywatność:
 * - Nie gromadzi danych PII.
 * - Używa anonimowych kluczy eksperymentów.
 *
 * ⚡ Wydajność i Zasoby:
 * - Deduplikacja zdarzeń ekspozycji w obrębie sesji (minimalizacja ruchu sieciowego).
 * - Lekkie operacje odczytu z pamięci podręcznej Remote Config.
 *
 * ✅ Gwarancje:
 * - Rozróżnienie między Przypisaniem (serwer) a Ekspozycją (moment zobaczenia przez użytkownika).
 * - Idempotentność metody [logExposure].
 */
@Singleton
class ExperimentManager @Inject constructor(
    private val analytics: FirebaseAnalytics
) {
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    /** Set of experiment keys that have already been exposed this session. */
    private val exposedThisSession = mutableSetOf<String>()

    /**
     * Returns the variant assigned to this user for the given experiment.
     *
     * @return variant string from Remote Config, or [Experiment.defaultVariant]
     *         if not fetched yet / parameter doesn't exist.
     */
    fun getVariant(experiment: Experiment): String {
        val value = remoteConfig.getString(experiment.key)
        return if (value.isBlank()) experiment.defaultVariant else value
    }

    /**
     * Returns the variant as a Long (useful for numeric experiments like limits).
     */
    fun getVariantLong(experiment: Experiment): Long {
        val value = remoteConfig.getLong(experiment.key)
        return if (value == 0L) {
            experiment.defaultVariant.toLongOrNull() ?: 0L
        } else {
            value
        }
    }

    /**
     * Returns the variant as a Boolean (useful for feature flag experiments).
     */
    fun getVariantBoolean(experiment: Experiment): Boolean {
        return remoteConfig.getBoolean(experiment.key)
    }

    /**
     * Checks if this experiment's variant matches the treatment group.
     */
    fun isTreatment(experiment: Experiment): Boolean {
        return getVariant(experiment) == Experiment.TREATMENT
    }

    /**
     * Log that the user was EXPOSED to an experiment variant.
     *
     * Call this when the user actually sees the UI that differs between
     * control and treatment. Deduplicated per session — safe to call
     * multiple times (e.g., on every recomposition).
     *
     * Firebase A/B Testing uses this event to calculate metrics only for
     * users who were actually exposed, not just assigned.
     */
    fun logExposure(experiment: Experiment) {
        if (experiment.key in exposedThisSession) return
        exposedThisSession.add(experiment.key)

        val variant = getVariant(experiment)
        Timber.d("Experiment exposure: ${experiment.name} → $variant")

        analytics.logEvent("experiment_exposure") {
            param("experiment_id", experiment.key)
            param("experiment_name", experiment.name)
            param("variant", variant)
        }

        // Also set as user property for segmentation in Analytics dashboards.
        // Firebase Analytics user property names are limited to 24 chars.
        analytics.setUserProperty(experiment.analyticsUserPropertyName(), variant)
    }

    /**
     * Logs all active experiment assignments as user properties.
     *
     * Call once after Remote Config fetchAndActivate (e.g., at app start).
     * This ensures Firebase Analytics dashboards can segment by experiment
     * group even before explicit exposure.
     */
    fun syncAssignments() {
        ActiveExperiments.all.forEach { experiment ->
            val variant = getVariant(experiment)
            analytics.setUserProperty(experiment.analyticsUserPropertyName(), variant)
            Timber.d("Experiment assignment synced: ${experiment.name} → $variant")
        }
    }

    /** Reset exposure tracking (e.g., on sign-out). */
    fun resetSession() {
        exposedThisSession.clear()
    }

    private fun Experiment.analyticsUserPropertyName(): String {
        val normalizedKey = if (key.startsWith(EXPERIMENT_KEY_PREFIX)) {
            key
        } else {
            "$EXPERIMENT_KEY_PREFIX$key"
        }
        return normalizedKey.take(MAX_USER_PROPERTY_NAME_LENGTH)
    }

    private companion object {
        const val EXPERIMENT_KEY_PREFIX = "exp_"
        const val MAX_USER_PROPERTY_NAME_LENGTH = 24
    }
}
