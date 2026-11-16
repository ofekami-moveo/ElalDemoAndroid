package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.utils.toFormattedTime
import java.time.Instant

/** The times to show: estimated ?: scheduled ?: the payload's departure / arrival (task 3b). */
internal object ScheduleTimes {

    /** Departure as originally planned. */
    fun scheduledDeparture(state: FlightContentState): Instant = state.scheduledDepartureTime ?: state.departureTime

    /** Departure to show now. */
    fun displayDeparture(state: FlightContentState): Instant = state.estimatedDepartureTime ?: scheduledDeparture(state)

    /** Arrival as originally planned. */
    fun scheduledArrival(state: FlightContentState): Instant = state.scheduledArrivalTime ?: state.arrivalTime

    /** Arrival to show now. */
    fun displayArrival(state: FlightContentState): Instant = state.estimatedArrivalTime ?: scheduledArrival(state)

    /** True when the shown departure differs from the scheduled one (compared as displayed, HH:mm). */
    fun departureChanged(state: FlightContentState): Boolean =
        state.estimatedDepartureTime != null &&
            displayDeparture(state).toFormattedTime() != scheduledDeparture(state).toFormattedTime()

    /** True when the shown arrival differs from the scheduled one (compared as displayed, HH:mm). */
    fun arrivalChanged(state: FlightContentState): Boolean =
        state.estimatedArrivalTime != null &&
            displayArrival(state).toFormattedTime() != scheduledArrival(state).toFormattedTime()
}
