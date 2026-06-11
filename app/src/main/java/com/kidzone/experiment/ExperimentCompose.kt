package com.kidzone.experiment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * Compose helper for rendering different UI based on experiment variant.
 *
 * Automatically logs exposure when the composable enters the composition.
 *
 * Usage:
 * ```kotlin
 * ExperimentSwitch(
 *     experimentManager = experimentManager,
 *     experiment = ActiveExperiments.HOME_LAYOUT,
 *     control = { HomeLayoutA() },
 *     treatment = { HomeLayoutB() }
 * )
 * ```
 */
@Composable
fun ExperimentSwitch(
    experimentManager: ExperimentManager,
    experiment: Experiment,
    control: @Composable () -> Unit,
    treatment: @Composable () -> Unit
) {
    val variant = remember(experiment.key) { experimentManager.getVariant(experiment) }

    LaunchedEffect(experiment.key) {
        experimentManager.logExposure(experiment)
    }

    when (variant) {
        Experiment.TREATMENT -> treatment()
        else -> control()
    }
}

/**
 * Compose helper for multi-variant experiments (more than 2 arms).
 *
 * Usage:
 * ```kotlin
 * ExperimentVariant(
 *     experimentManager = experimentManager,
 *     experiment = ActiveExperiments.HOME_LAYOUT,
 * ) { variant ->
 *     when (variant) {
 *         "control" -> LayoutA()
 *         "variant_b" -> LayoutB()
 *         "variant_c" -> LayoutC()
 *         else -> LayoutA() // fallback
 *     }
 * }
 * ```
 */
@Composable
fun ExperimentVariant(
    experimentManager: ExperimentManager,
    experiment: Experiment,
    content: @Composable (variant: String) -> Unit
) {
    val variant = remember(experiment.key) { experimentManager.getVariant(experiment) }

    LaunchedEffect(experiment.key) {
        experimentManager.logExposure(experiment)
    }

    content(variant)
}
