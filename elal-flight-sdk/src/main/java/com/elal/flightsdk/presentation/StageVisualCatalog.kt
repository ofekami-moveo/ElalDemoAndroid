package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.IconRole
import com.elal.flightsdk.model.LiveUpdateAction
import com.elal.flightsdk.model.Stage

/**
 * The one table of per-stage visuals (requirements §3b): bar position, tracker icon, segment colour, pill icon,
 * alert on entry, promotion, actions and the status badge (spike: GATE_READY → LANDING_SOON). Change a stage's look
 * here, in one place.
 */
internal object StageVisualCatalog {

    /** Returns the visuals for [stage]. */
    fun visualsFor(stage: Stage): StageVisuals = when (stage) {
        Stage.CHECK_IN -> visuals(ProgressPosition.START, IconRole.TRACKER_CHECK_IN, alertOnEntry = true, isPromoted = false)
        Stage.GATE_READY -> visuals(
            ProgressPosition.GATE, IconRole.TRACKER_GATE, alertOnEntry = true, actions = BOARDING_PASS_ONLY,
            showStatusBadge = true,
        )
        Stage.BOARDING_PENDING -> visuals(
            ProgressPosition.GATE, IconRole.TRACKER_GATE, actions = BOARDING_PASS_ONLY, showStatusBadge = true
        )
        Stage.BOARDING -> visuals(
            ProgressPosition.GATE, IconRole.TRACKER_GATE, alertOnEntry = true, actions = BOARDING_PASS_ONLY,
            showStatusBadge = true,
        )
        Stage.LAST_CALL -> visuals(
            ProgressPosition.FLIGHT_START, IconRole.TRACKER_TAKEOFF, alertOnEntry = true, actions = BOARDING_PASS_ONLY,
            showStatusBadge = true,
        )
        Stage.DEPARTURE_PENDING -> visuals(
            ProgressPosition.FLIGHT_START, IconRole.TRACKER_TAKEOFF, actions = BOARDING_PASS_ONLY, showStatusBadge = true
        )
        Stage.DEPARTED -> visuals(
            ProgressPosition.IN_FLIGHT, IconRole.TRACKER_CRUISE, isAirborne = true, showStatusBadge = true
        )
        Stage.LANDING_SOON -> visuals(
            ProgressPosition.END, IconRole.TRACKER_LANDING, SegmentStyle.ON_TIME_ONLY, isAirborne = true,
            showStatusBadge = true,
        )
        Stage.LANDED -> visuals(ProgressPosition.END, IconRole.TRACKER_LANDING, SegmentStyle.ON_TIME_ONLY, alertOnEntry = true)
        Stage.BAGGAGE -> visuals(ProgressPosition.END, IconRole.TRACKER_BAGGAGE, SegmentStyle.ON_TIME_ONLY)
        Stage.COMPLETED -> visuals(ProgressPosition.END, IconRole.TRACKER_LANDING, SegmentStyle.ON_TIME_ONLY)
        Stage.CONNECTION_ERROR,
        Stage.GENERAL_ERROR -> visuals(ProgressPosition.START, IconRole.TRACKER_NONE, SegmentStyle.ERROR)
    }

    /** Boarding pass: from GATE_READY until takeoff (no baggage-info action yet, TODO). */
    private val BOARDING_PASS_ONLY = listOf(LiveUpdateAction.BOARDING_PASS)

    /** Shorthand for a [StageVisuals] with the common defaults (delay-aware, silent, promoted, no actions). */
    private fun visuals(
        position: ProgressPosition,
        tracker: IconRole,
        segmentStyle: SegmentStyle = SegmentStyle.DELAY_AWARE,
        isAirborne: Boolean = false,
        alertOnEntry: Boolean = false,
        isPromoted: Boolean = true,
        actions: List<LiveUpdateAction> = emptyList(),
        showStatusBadge: Boolean = false,
    ) = StageVisuals(position, tracker, segmentStyle, isAirborne, alertOnEntry, isPromoted, actions, showStatusBadge)
}
