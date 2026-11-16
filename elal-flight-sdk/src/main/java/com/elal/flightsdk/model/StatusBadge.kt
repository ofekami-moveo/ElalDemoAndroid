package com.elal.flightsdk.model

/** The coloured status badge shown as the large icon (spike, `LiveUpdateConfig.showStatusBadge`). */
internal enum class StatusBadge {
    /** Green "ON TIME" (`img_live_update_status_on_time`). */
    ON_TIME,

    /** Orange "DELAYED" (`img_live_update_status_delayed`). */
    DELAYED,
}
