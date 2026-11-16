package com.elal.flightsdk.presentation

/**
 * How often the in-flight bar and pill are re-posted: 1% of the flight, at least 1 s, at most the
 * configured interval. Short (demo) flights still move smoothly; real flights tick about once a minute.
 */
internal object TickIntervalCalculator {

    private const val MIN_TICK_MS = 1_000L
    private const val MIN_REFRESH_MS = 60_000L
    private const val MAX_REFRESH_MS = 15 * 60_000L
    private const val TICKS_PER_FLIGHT = 100

    /** Clamps a host-configured refresh interval to 1–15 min. */
    fun clampRefreshInterval(refreshIntervalMs: Long): Long = refreshIntervalMs.coerceIn(MIN_REFRESH_MS, MAX_REFRESH_MS)

    /** The delay before the next tick for a flight of [totalFlightMs]; 1 s when there is no duration. */
    fun intervalMs(totalFlightMs: Long, refreshIntervalMs: Long): Long {
        if (totalFlightMs <= 0L) {
            return MIN_TICK_MS
        }
        return (totalFlightMs / TICKS_PER_FLIGHT).coerceIn(MIN_TICK_MS, clampRefreshInterval(refreshIntervalMs))
    }
}
