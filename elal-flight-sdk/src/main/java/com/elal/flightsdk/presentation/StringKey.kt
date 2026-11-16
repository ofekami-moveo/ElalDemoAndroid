package com.elal.flightsdk.presentation

/** The SDK strings the content builder needs; `AndroidStringProvider` maps each to its `R.string`. */
internal enum class StringKey {
    /** `notif_title`: "flight · stage". */
    TITLE,

    /** `notif_subtext_route`: "TLV → JFK" (arrow mirrored in HE). */
    SUBTEXT_ROUTE,

    /** `notif_content_lands_at` (time). */
    CONTENT_LANDS_AT,

    /** `notif_content_connection_error`. */
    CONTENT_CONNECTION_ERROR,

    /** `notif_content_general_error`. */
    CONTENT_GENERAL_ERROR,

    /** `notif_content_arrival_welcome` (destination). */
    CONTENT_ARRIVAL_WELCOME,

    /** `notif_flight_time` (hours, minutes). */
    FLIGHT_TIME,

    /** `notif_public_gate` (gate), lock-screen public version. */
    PUBLIC_GATE,

    /** `notif_public_departs` (time), lock-screen public version. */
    PUBLIC_DEPARTS,

    /** `notif_public_arrives` (time), lock-screen public version. */
    PUBLIC_ARRIVES,

    /** `notif_change_departs` (change): "Departs %s". */
    CHANGE_DEPARTS,

    /** `notif_change_values` (old, new): the struck old value, then the new one. */
    CHANGE_VALUES,

    /** `notif_change_was` (new, old): plain-text change, "17:55 (was 17:00)". */
    CHANGE_WAS,

    /** `notif_change_spoken` (new, old): what TalkBack reads, "17:55, was 17:00". */
    CHANGE_SPOKEN,

    /** `notif_stage_check_in`: stage title when the server sends none. */
    STAGE_CHECK_IN,

    /** `notif_stage_gate_ready`. */
    STAGE_GATE_READY,

    /** `notif_stage_boarding_pending`. */
    STAGE_BOARDING_PENDING,

    /** `notif_stage_boarding`. */
    STAGE_BOARDING,

    /** `notif_stage_last_call`. */
    STAGE_LAST_CALL,

    /** `notif_stage_departure_pending`. */
    STAGE_DEPARTURE_PENDING,

    /** `notif_stage_departed`. */
    STAGE_DEPARTED,

    /** `notif_stage_landing_soon`. */
    STAGE_LANDING_SOON,

    /** `notif_stage_landed`. */
    STAGE_LANDED,

    /** `notif_stage_baggage`. */
    STAGE_BAGGAGE,

    /** `notif_stage_completed`. */
    STAGE_COMPLETED,

    /** `notif_stage_connection_error`. */
    STAGE_CONNECTION_ERROR,

    /** `notif_stage_general_error`. */
    STAGE_GENERAL_ERROR,
}
