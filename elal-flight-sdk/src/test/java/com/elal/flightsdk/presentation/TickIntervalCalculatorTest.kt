package com.elal.flightsdk.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

/** In-flight re-post cadence (task 2.6, decided 2026-11-16: about 1 min, configurable 1–15 min). */
class TickIntervalCalculatorTest {

    /** 1% of the flight, at least 1 s, at most the configured interval, which is clamped to 1–15 min. */
    @Test
    fun tickIsOnePercentCappedByClampedConfig() {
        val minute = 60_000L
        assertEquals(minute, TickIntervalCalculator.intervalMs(totalFlightMs = 210 * minute, refreshIntervalMs = minute))
        assertEquals(3_000L, TickIntervalCalculator.intervalMs(totalFlightMs = 5 * minute, refreshIntervalMs = minute))
        assertEquals(1_000L, TickIntervalCalculator.intervalMs(totalFlightMs = 0L, refreshIntervalMs = minute))

        assertEquals(minute, TickIntervalCalculator.clampRefreshInterval(5_000L))
        assertEquals(15 * minute, TickIntervalCalculator.clampRefreshInterval(60 * minute))
        assertEquals(minute, TickIntervalCalculator.intervalMs(totalFlightMs = 600 * minute, refreshIntervalMs = 5_000L))
        assertEquals(15 * minute, TickIntervalCalculator.intervalMs(totalFlightMs = 1_800 * minute, refreshIntervalMs = 60 * minute))
    }
}
