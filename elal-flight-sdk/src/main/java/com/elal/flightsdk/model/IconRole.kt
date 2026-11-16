package com.elal.flightsdk.model

/**
 * Which icon a notification slot shows, without naming a drawable, so the content can be built and
 * tested without Android. `LiveUpdateNotificationMapper` turns each role into a resource.
 */
internal enum class IconRole {
    /** Pill: departure arrow (Figma ELAL 6944:19137). */
    CHIP_DEPARTURE,

    /** Pill: departure arrow with the delay badge. */
    CHIP_DEPARTURE_DELAYED,

    /** Pill: plane while airborne (Figma ELAL 6944:19247). */
    CHIP_FLIGHT,

    /** Pill: plane with the delay badge. */
    CHIP_FLIGHT_DELAYED,

    /** Progress tracker: transparent placeholder (no visible tracker, errors). */
    TRACKER_NONE,

    /** Progress tracker: check-in (ticket). */
    TRACKER_CHECK_IN,

    /** Progress tracker: at the gate. */
    TRACKER_GATE,

    /** Progress tracker: plane taking off. */
    TRACKER_TAKEOFF,

    /** Progress tracker: plane in cruise. */
    TRACKER_CRUISE,

    /** Progress tracker: plane landing. */
    TRACKER_LANDING,

    /** Progress tracker: baggage claim. */
    TRACKER_BAGGAGE,

    /** Start of the bar: origin. */
    PROGRESS_START,

    /** End of the bar: destination. */
    PROGRESS_END,
}
