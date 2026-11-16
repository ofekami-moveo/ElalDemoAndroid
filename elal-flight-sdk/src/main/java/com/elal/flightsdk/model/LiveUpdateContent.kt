package com.elal.flightsdk.model

/**
 * Everything the Live Update notification shows, as plain values. Built by `LiveUpdateContentBuilder`
 * (pure, JVM-tested) and turned into a `Notification` by `LiveUpdateNotificationMapper`.
 *
 * @property textSpans styled ranges of [text] (struck-through old values, TalkBack wording); empty when nothing changed.
 * @property subText the route, e.g. "TLV → JFK".
 * @property shortCriticalText status-bar pill text, already bidi-wrapped, or `null` for icon only.
 * @property publicText content line of the lock-screen public version: gate and times, never the PNR.
 * @property requestPromoted ask for a promoted Live Update (from GATE_READY on); CHECK_IN is a regular ongoing one.
 * @property postedState what this post shows, for the next post's alert decision.
 * @property actions buttons offered in this stage; the mapper drops those the host has no target for.
 * @property statusBadge the large-icon badge, or `null` (flag off, or a stage without it).
 */
internal data class LiveUpdateContent(
    val title: String,
    val text: String,
    val textSpans: List<TextSpanSpec>,
    val subText: String,
    val shortCriticalText: String?,
    val publicText: String,
    val smallIcon: IconRole,
    val accentColor: ColorRole,
    val progress: LiveUpdateProgress,
    val onlyAlertOnce: Boolean,
    val requestPromoted: Boolean,
    val postedState: PostedState,
    val actions: List<LiveUpdateAction>,
    val statusBadge: StatusBadge?,
)
