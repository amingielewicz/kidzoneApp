package com.kidzone.analytics

import com.google.firebase.perf.metrics.Trace
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks app cold start from Application.onCreate to the first Compose content.
 *
 * The controller is intentionally idempotent because activity recreation,
 * warm starts, or test harnesses can call the lifecycle hooks more than once.
 */
@Singleton
class ColdStartTrace @Inject constructor(
    private val performanceTraces: PerformanceTraces
) {
    private var trace: Trace? = null
    private var completed = false

    fun start() {
        if (trace != null || completed) return
        trace = performanceTraces.startTrace(PerformanceTraces.COLD_START).also {
            it.putAttribute("start_phase", "application_on_create")
        }
    }

    fun stopAtFirstContent() {
        if (completed) return
        val currentTrace = trace ?: return
        completed = true
        trace = null
        currentTrace.putAttribute("end_phase", "first_compose_content")
        performanceTraces.stopTrace(currentTrace)
        Timber.d("Cold start trace completed")
    }
}
