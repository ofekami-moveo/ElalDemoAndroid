package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.IconRole
import com.elal.flightsdk.model.LiveUpdateAction

/**
 * Per-stage visual settings, looked up in [StageVisualCatalog] (requirements §3b).
 *
 * @property isAirborne pill shows the plane instead of the departure arrow.
 * @property alertOnEntry sound/heads-up when the journey enters this stage.
 * @property isPromoted request promotion to a Live Update (not on CHECK_IN, Google policy).
 * @property actions buttons offered in this stage (max 2), shown only when the host has a target.
 * @property showStatusBadge the "ON TIME" / "DELAYED" badge may show as the large icon (spike, behind a config flag).
 */
internal data class StageVisuals(
    val progressPosition: ProgressPosition,
    val tracker: IconRole,
    val segmentStyle: SegmentStyle,
    val isAirborne: Boolean,
    val alertOnEntry: Boolean,
    val isPromoted: Boolean,
    val actions: List<LiveUpdateAction>,
    val showStatusBadge: Boolean,
)
