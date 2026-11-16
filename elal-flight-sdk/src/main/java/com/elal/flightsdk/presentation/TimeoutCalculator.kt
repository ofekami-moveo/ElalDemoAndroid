package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.FlightContentState

/**
 * The journey's absolute end, for `setTimeoutAfter` and the restore check: the server's `dismissalDate`, otherwise
 * the arrival + 4 h. A safety net only; the server's `end` normally clears the Live Update first.
 */
internal object TimeoutCalculator {

    private const val AFTER_ARRIVAL_MS = 4 * 60 * 60 * 1000L

    /**
     * The end on the server timeline. The arrival is the calculated one (departure + offset + duration) when
     * possible, else the naive server ETA measured against the device's [wallNowMs].
     */
    fun journeyEndServerMs(state: FlightContentState, serverNowMs: Long, wallNowMs: Long): Long {
        state.dismissalDate?.let { return it.toEpochMilli() }
        val arrivalMs = FlightTimeline.from(state).calculatedArrivalUtcMs
            ?: (serverNowMs + (state.arrivalTime.toEpochMilli() - wallNowMs))
        return arrivalMs + AFTER_ARRIVAL_MS
    }

    /** Milliseconds until [endServerMs], or `null` when it has already passed. */
    fun timeoutAfterMs(endServerMs: Long, serverNowMs: Long): Long? = (endServerMs - serverNowMs).takeIf { it > 0L }
}
