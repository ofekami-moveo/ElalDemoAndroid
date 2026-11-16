package com.elal.flightsdk.model

/** How a changed time or gate is written in the notification (`LiveUpdateConfig.scheduleChangeStyle`). */
enum class ScheduleChangeStyle {
    /** Old value struck through, new value next to it: "Departs ~~17:00~~ 17:55" (iOS style). Not drawn in promoted
     * Live Updates on the Android 17 emulator; opt-in for device checks. */
    SPAN,

    /** Plain text: "Departs 17:55 (was 17:00)". The default (decided 2026-11-16). */
    PLAIN_TEXT,
}
