package com.elal.flightsdk.model

/**
 * The clock values one notification build reads, taken once by `LiveUpdateManager` so the bar, the
 * countdown text and the pill all agree.
 *
 * @property flightProgress DEPARTED fill fraction 0..1 (1.0 when there is no duration to run over).
 * @property remainingFlightMs time left on the in-flight timer, or `null` when no timer is running.
 */
internal data class TimeSnapshot(
    val flightProgress: Double,
    val remainingFlightMs: Long?,
)
