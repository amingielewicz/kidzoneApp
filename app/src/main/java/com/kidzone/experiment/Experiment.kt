package com.kidzone.experiment

/**
 * Defines a single A/B test experiment powered by Firebase Remote Config.
 *
 * Each experiment corresponds to a Remote Config parameter whose value
 * determines which variant (arm) the user is assigned to. Firebase
 * A/B Testing handles assignment & statistical analysis; this class
 * provides type-safe access on the client side.
 *
 * ## How to add a new experiment
 *
 * 1. Add a new entry to [ActiveExperiments].
 * 2. Add a default value in [RemoteConfigService.init] defaults map.
 * 3. Create the Remote Config parameter + A/B Test in Firebase Console.
 * 4. Use [ExperimentManager.getVariant] in your feature code.
 * 5. Log exposure via [ExperimentManager.logExposure] when the user
 *    first **sees** the variant (not just when assigned).
 *
 * ## Naming convention
 *
 * Remote Config key: `exp_<feature_name>` (e.g., `exp_review_photos_limit`)
 * Analytics event: `experiment_exposure` with params `experiment_id` + `variant`
 */
data class Experiment(
    /** Remote Config parameter key (e.g., "exp_home_layout") */
    val key: String,

    /** Human-readable name for logging/debugging */
    val name: String,

    /** Default variant returned when Remote Config hasn't fetched yet */
    val defaultVariant: String = CONTROL
) {
    companion object {
        /** Standard control group name */
        const val CONTROL = "control"

        /** Standard treatment group name */
        const val TREATMENT = "treatment"
    }
}
