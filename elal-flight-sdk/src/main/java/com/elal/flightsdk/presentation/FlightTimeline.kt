package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.utils.toAbsoluteInstant
import java.time.Instant

/**
 * The flight's departure and calculated arrival on the GMT timeline, as the iOS Live Activity derives
 * them: arrival = naive departure placed with `departureTimezoneOffset` + `flightDuration`. The server
 * never sends the arrival's own offset, so `arrivalTime` can't be used for this.
 *
 * @property departureUtc the real departure instant, or `null` when the offset is missing or unparseable.
 * @property flightDurationMinutes server flight time in minutes, or `null` when missing or not positive.
 * @property calculatedArrivalUtcMs the arrival in epoch millis, or `null` when either input is missing.
 */
internal class FlightTimeline private constructor(
    val departureUtc: Instant?,
    val flightDurationMinutes: Int?,
) {

    val calculatedArrivalUtcMs: Long? = flightDurationMinutes?.let { minutes ->
        departureUtc?.let { it.toEpochMilli() + minutes * MS_PER_MINUTE }
    }

    companion object {
        private const val MS_PER_MINUTE = 60_000L

        /** Builds the timeline from the fields of [state]; reads no clock and no device timezone. */
        fun from(state: FlightContentState): FlightTimeline = FlightTimeline(
            departureUtc = state.departureTime.toAbsoluteInstant(state.departureTimezoneOffset),
            flightDurationMinutes = state.flightDuration?.takeIf { it > 0 },
        )
    }
}
