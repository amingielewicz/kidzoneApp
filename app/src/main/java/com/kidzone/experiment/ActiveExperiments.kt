package com.kidzone.experiment

/**
 * Registry of all currently active A/B test experiments.
 *
 * When an experiment concludes (winner decided), remove it from here
 * and hard-code the winning variant in the feature code.
 *
 * Each experiment maps 1:1 to a Firebase Remote Config parameter.
 * The parameter value is the variant name assigned by Firebase A/B Testing.
 */
object ActiveExperiments {

    /**
     * Experiment: Home screen layout.
     *
     * - control: current layout (nearby + top places horizontal rows)
     * - treatment: single vertical list with mixed content
     *
     * Remote Config key: `exp_home_layout`
     * Metric: session_duration, view_place rate
     */
    val HOME_LAYOUT = Experiment(
        key = "exp_home_layout",
        name = "Home Screen Layout",
        defaultVariant = Experiment.CONTROL
    )

    /**
     * Experiment: Review photos limit.
     *
     * - control: max 3 photos per review
     * - treatment: max 5 photos per review
     *
     * Remote Config key: `exp_review_photos_limit`
     * Metric: review completion rate, photos per review
     */
    val REVIEW_PHOTOS_LIMIT = Experiment(
        key = "exp_review_photos_limit",
        name = "Review Photos Limit",
        defaultVariant = "3"
    )

    /**
     * Experiment: Add Place CTA position.
     *
     * - control: FAB on map screen
     * - treatment: FAB on home screen + map screen
     *
     * Remote Config key: `exp_add_place_cta`
     * Metric: places added per user
     */
    val ADD_PLACE_CTA = Experiment(
        key = "exp_add_place_cta",
        name = "Add Place CTA Position",
        defaultVariant = Experiment.CONTROL
    )

    /** All active experiments — used for bulk operations (e.g., log all assignments). */
    val all: List<Experiment> = listOf(
        HOME_LAYOUT,
        REVIEW_PHOTOS_LIMIT,
        ADD_PLACE_CTA
    )
}
