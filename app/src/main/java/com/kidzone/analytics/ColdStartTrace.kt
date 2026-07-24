package com.kidzone.analytics

import com.google.firebase.perf.metrics.Trace
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🎯 Odpowiedzialności:
 * - Pomiar czasu pełnego uruchomienia aplikacji (Cold Start).
 * - Śledzenie przejścia od `Application.onCreate` do pierwszego wyrenderowania treści w Compose.
 *
 * ✅ Gwarancje:
 * - Idempotentność: pomiar wykonywany jest tylko raz na proces (odporność na Activity recreation).
 * - Minimalny narzut na czas startu głównego wątku.
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
