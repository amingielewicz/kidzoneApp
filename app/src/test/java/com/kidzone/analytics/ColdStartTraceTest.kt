package com.kidzone.analytics

import com.google.firebase.perf.metrics.Trace
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test

class ColdStartTraceTest {

    private val performanceTraces = mockk<PerformanceTraces>()
    private val trace = mockk<Trace>(relaxed = true)

    @Test
    fun `starts trace once`() {
        every { performanceTraces.startTrace(PerformanceTraces.COLD_START) } returns trace
        every { performanceTraces.stopTrace(trace) } just runs

        val coldStartTrace = ColdStartTrace(performanceTraces)

        coldStartTrace.start()
        coldStartTrace.start()

        verify(exactly = 1) { performanceTraces.startTrace(PerformanceTraces.COLD_START) }
        verify(exactly = 1) { trace.putAttribute("start_phase", "application_on_create") }
    }

    @Test
    fun `stops trace once at first content`() {
        every { performanceTraces.startTrace(PerformanceTraces.COLD_START) } returns trace
        every { performanceTraces.stopTrace(trace) } just runs

        val coldStartTrace = ColdStartTrace(performanceTraces)

        coldStartTrace.start()
        coldStartTrace.stopAtFirstContent()
        coldStartTrace.stopAtFirstContent()

        verify(exactly = 1) { trace.putAttribute("end_phase", "first_compose_content") }
        verify(exactly = 1) { performanceTraces.stopTrace(trace) }
    }

    @Test
    fun `ignores stop before start`() {
        val coldStartTrace = ColdStartTrace(performanceTraces)

        coldStartTrace.stopAtFirstContent()

        verify(exactly = 0) { performanceTraces.stopTrace(any()) }
    }
}
