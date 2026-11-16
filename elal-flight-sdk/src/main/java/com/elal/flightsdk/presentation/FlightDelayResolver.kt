package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.FlightContentState

/**
 * The single place that decides whether a flight is delayed, so the source field can change later
 * (e.g. a dedicated `isDelayed` / `delayMinutes` from the server) without touching the UI code.
 */
object FlightDelayResolver {

    private const val DELAYED = "DELAYED"

    /**
     * True when [state] is delayed: `flightStatus` ≈ "DELAYED", or the estimated departure is later than the
     * scheduled one (task 3b, decided 2026-11-16). The one entry point the UI uses.
     */
    fun isDelayed(state: FlightContentState): Boolean =
        isDelayed(state.flightStatus) || departsLater(state)

    /** True when an estimated departure is set and later than the scheduled (else payload) departure. */
    private fun departsLater(state: FlightContentState): Boolean {
        val estimated = state.estimatedDepartureTime ?: return false
        return estimated.isAfter(state.scheduledDepartureTime ?: state.departureTime)
    }

    /** True when the server's `flightStatus` is "DELAYED", ignoring case and surrounding spaces. */
    fun isDelayed(flightStatus: String?): Boolean =
        flightStatus?.trim()?.equals(DELAYED, ignoreCase = true) == true
}
