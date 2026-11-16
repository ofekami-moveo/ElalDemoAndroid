package com.elal.flightsdk.model

/** Whether the system will show the journey as a promoted Live Update, and if not, why. */
enum class PromotionStatus {
    /** Promotion is allowed. */
    ALLOWED,

    /** The user turned off Live Updates for this app (`canPostPromotedNotifications() == false`). */
    BLOCKED_BY_USER,

    /** Notifications are off for this app, so nothing is shown. */
    NOTIFICATIONS_DISABLED,

    /** Android 15 or older: no Live Updates. */
    UNSUPPORTED,
}
