package com.elal.flightsdk.model

/** Which named colour a notification element uses; `LiveUpdateNotificationMapper` resolves it to a resource. */
internal enum class ColorRole {
    /** Progress segment, on time (`progress_bar_segment`). */
    SEGMENT_ON_TIME,

    /** Progress segment, delayed (`progress_bar_segment_delayed`). */
    SEGMENT_DELAYED,

    /** Progress segment on an error stage (`progress_bar_segment_error`). */
    SEGMENT_ERROR,

    /** Notification accent via `setColor`, on time (`live_update_chip_on_time`). */
    ACCENT_ON_TIME,

    /** Notification accent via `setColor`, delayed (`live_update_chip_delayed`). */
    ACCENT_DELAYED,
}
