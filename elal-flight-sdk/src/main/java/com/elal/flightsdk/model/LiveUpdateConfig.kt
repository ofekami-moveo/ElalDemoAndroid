package com.elal.flightsdk.model

/**
 * Host-tunable SDK settings, set once via `LiveUpdateManager.config`.
 *
 * @property progressRefreshIntervalMs longest gap between in-flight re-posts of the bar and the pill's time
 * left. Default 1 min so the pill's minutes stay right; must be positive, and is clamped to 1–15 min.
 * @property enableLegacyFallback on Android 15 and older, show a plain ongoing progress notification (no
 * promotion, no foreground service). Off by default: those devices show nothing.
 * @property scheduleChangeStyle how a changed time or gate is written. Default plain text ("17:55 (was 17:00)"):
 * the emulator (Android 17) doesn't draw `StrikethroughSpan` in a promoted Live Update (2026-11-16). SPAN is kept
 * as an option to check on physical devices.
 * @property showStatusBadge spike: show a coloured "ON TIME" / "DELAYED" badge as the large icon on the stages that
 * allow it. Off by default; the delay stays in the text ("(was …)") for TalkBack.
 */
data class LiveUpdateConfig(
    val progressRefreshIntervalMs: Long = DEFAULT_PROGRESS_REFRESH_INTERVAL_MS,
    val enableLegacyFallback: Boolean = false,
    val scheduleChangeStyle: ScheduleChangeStyle = ScheduleChangeStyle.PLAIN_TEXT,
    val showStatusBadge: Boolean = false,
) {
    init {
        require(progressRefreshIntervalMs > 0L) { "progressRefreshIntervalMs must be positive, was $progressRefreshIntervalMs" }
    }

    companion object {
        /** One minute: the pill shows minutes, so a longer gap would show a stale value. */
        const val DEFAULT_PROGRESS_REFRESH_INTERVAL_MS: Long = 60_000L
    }
}
