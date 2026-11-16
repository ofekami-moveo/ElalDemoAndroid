package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.FlightContentState

/**
 * The in-flight interval on the server timeline: from the first DEPARTED push ([startServerMs], persisted
 * per `activityId`) to the arrival ([arrivalServerMs], recomputed on every push). The bar and the pill's time
 * left both read it, so they always agree, and a restore or a repeated push no longer restarts the bar (F1).
 */
internal data class DepartedClock(val startServerMs: Long, val arrivalServerMs: Long) {

    /** Length of the interval; zero or less when the arrival is not after the start. */
    val totalMs: Long get() = arrivalServerMs - startServerMs

    /** True when there is a positive interval to animate over. */
    val hasDuration: Boolean get() = totalMs > 0L

    /** Fraction of the flight done at [nowMs], 0..1; 1.0 when there is nothing to animate. */
    fun progressAt(nowMs: Long): Double {
        if (!hasDuration) {
            return 1.0
        }
        return ((nowMs - startServerMs).toDouble() / totalMs).coerceIn(0.0, 1.0)
    }

    /** Time left at [nowMs], never negative, or `null` when there is no interval. */
    fun remainingAt(nowMs: Long): Long? = if (hasDuration) (arrivalServerMs - nowMs).coerceAtLeast(0L) else null

    companion object {
        private const val MS_PER_MINUTE = 60_000L

        /**
         * Builds the clock for a DEPARTED [state] at [serverNowMs]. The start is [persistedStartMs] when this
         * journey already has one, otherwise now. The arrival, in order of preference: the iOS-style calculated
         * arrival (departure + offset + duration); start + duration when the offset is missing; the server ETA
         * (`arrivalTime`, naive local) measured against [wallNowMs] when there is no duration.
         */
        fun resolve(state: FlightContentState, serverNowMs: Long, wallNowMs: Long, persistedStartMs: Long?): DepartedClock {
            val startMs = persistedStartMs ?: serverNowMs
            val timeline = FlightTimeline.from(state)
            val calculatedArrivalMs = timeline.calculatedArrivalUtcMs
            val durationMinutes = timeline.flightDurationMinutes
            val arrivalMs = when {
                calculatedArrivalMs != null -> calculatedArrivalMs
                durationMinutes != null -> startMs + durationMinutes * MS_PER_MINUTE
                else -> serverNowMs + (state.arrivalTime.toEpochMilli() - wallNowMs)
            }
            return DepartedClock(startMs, arrivalMs)
        }
    }
}
