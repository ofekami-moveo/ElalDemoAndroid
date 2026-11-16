package com.elal.flightsdk.manager

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.TtsSpan
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.ProgressStyle
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.elal.flightsdk.R
import com.elal.flightsdk.analytics.ClickSource
import com.elal.flightsdk.analytics.LiveActivityClickActivity
import com.elal.flightsdk.model.ColorRole
import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.IconRole
import com.elal.flightsdk.model.LiveUpdateAction
import com.elal.flightsdk.model.LiveUpdateContent
import com.elal.flightsdk.model.LiveUpdateProgress
import com.elal.flightsdk.model.SpanType
import com.elal.flightsdk.model.StatusBadge
import com.elal.flightsdk.model.TextSpanSpec
import com.elal.flightsdk.presentation.ActionFilter
import com.elal.flightsdk.presentation.BadgeFit
import kotlin.math.roundToInt

/**
 * Turns a [LiveUpdateContent] into the system `ProgressStyle` notification. The only place that knows
 * drawables, colours and `NotificationCompat`; all decisions are already made in the content.
 */
internal class LiveUpdateNotificationMapper(private val context: Context) {

    /**
     * Builds the promoted ongoing notification for [content], with a public version for the lock screen:
     * the main one stays `VISIBILITY_PRIVATE`, so a "hide content when locked" setting (the S25+ default)
     * shows [LiveUpdateContent.publicText] instead of "Contents hidden".
     *
     * The main version also carries the actions the host can open, the delete intent (a swipe stops reposts for
     * [state]'s journey) and the [timeoutAfterMs] safety net (`null` = none).
     */
    fun map(content: LiveUpdateContent, state: FlightContentState, timeoutAfterMs: Long?): Notification {
        val publicVersion = baseBuilder(content, content.publicText).build()
        val builder = baseBuilder(content, styledText(content.text, content.textSpans))
            .setPublicVersion(publicVersion)
            .setDeleteIntent(buildDismissPendingIntent(state.activityId))
        timeoutAfterMs?.let { builder.setTimeoutAfter(it) }
        val provider = LiveUpdateManager.actionIntentProvider
        if (provider != null) {
            ActionFilter.withTargets(content.actions) { provider(context, it, state) }
                .forEach { (action, _) -> builder.addAction(0, actionLabel(action), buildActionPendingIntent(action)) }
        }
        return builder.build()
    }

    /**
     * [text] with its spans: the old value of a schedule change struck through, and the change read by TalkBack as
     * "17:55, was 17:00" (best effort: the system reads notification text, checked per device). Plain when empty.
     */
    private fun styledText(text: String, spans: List<TextSpanSpec>): CharSequence {
        if (spans.isEmpty()) {
            return text
        }
        val styled = SpannableString(text)
        spans.forEach { span ->
            val what: Any = when (span.type) {
                SpanType.STRIKETHROUGH -> StrikethroughSpan()
                SpanType.SPOKEN -> TtsSpan.TextBuilder(span.spokenText.orEmpty()).build()
            }
            styled.setSpan(what, span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return styled
    }

    /**
     * The badge drawn whole and centred on a transparent canvas shaped like the system's large-icon frame (~16:9).
     * The system center-crops the large icon to that frame and scales it to fill, so a bare 3:1 badge lost its ends
     * and was upscaled (device check 2026-11-16). The canvas is [BADGE_SCALE]× the frame's size so the system only
     * scales down; the vector is drawn at that size, so it stays sharp. `drawable-iw/` variants resolve by locale.
     */
    private fun badgeBitmap(badge: StatusBadge): Bitmap {
        val res = when (badge) {
            StatusBadge.ON_TIME -> R.drawable.img_live_update_status_on_time
            StatusBadge.DELAYED -> R.drawable.img_live_update_status_delayed
        }
        val drawable = requireNotNull(ContextCompat.getDrawable(context, res)) { "missing badge drawable $badge" }
        val frameHeight = (BADGE_FRAME_HEIGHT_DP * context.resources.displayMetrics.density * BADGE_SCALE).roundToInt()
        val frameWidth = frameHeight * BadgeFit.FRAME_ASPECT_WIDTH / BadgeFit.FRAME_ASPECT_HEIGHT
        val bounds = BadgeFit.centered(
            frameWidth, frameHeight, drawable.intrinsicWidth, drawable.intrinsicHeight, BADGE_PADDING
        )
        val bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888)
        drawable.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    /** Localized button label for [action]. */
    private fun actionLabel(action: LiveUpdateAction): String = when (action) {
        LiveUpdateAction.BOARDING_PASS -> context.getString(R.string.notif_action_boarding_pass)
    }

    /**
     * An action goes through the click trampoline (analytics), which asks the host for the target again on tap.
     * One request code per action so the PendingIntents don't replace each other.
     */
    private fun buildActionPendingIntent(action: LiveUpdateAction): PendingIntent {
        val intent = Intent(context, LiveActivityClickActivity::class.java).apply {
            putExtra(LiveActivityClickActivity.EXTRA_CLICK_SOURCE, ClickSource.LOCK_SCREEN)
            putExtra(LiveActivityClickActivity.EXTRA_ACTION, action.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(
            context,
            LiveUpdateManager.NOTIFICATION_ID + 1 + action.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /** Delete intent to [LiveUpdateDismissReceiver], tagged with the journey's [activityId]. */
    private fun buildDismissPendingIntent(activityId: String): PendingIntent {
        val intent = Intent(context, LiveUpdateDismissReceiver::class.java)
            .putExtra(LiveUpdateDismissReceiver.EXTRA_ACTIVITY_ID, activityId)
        return PendingIntent.getBroadcast(
            context,
            LiveUpdateManager.NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /** Everything both versions share; only the content line differs. */
    private fun baseBuilder(content: LiveUpdateContent, text: CharSequence): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, LiveUpdateManager.CHANNEL_ID)
            .setSmallIcon(drawableFor(content.smallIcon))
            .setColor(colorFor(content.accentColor))
            .setContentTitle(content.title)
            .setContentText(text)
            .setSubText(content.subText)
            .setContentIntent(buildClickPendingIntent())
            .setStyle(buildProgressStyle(content.progress))
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(true)
            // CHECK_IN is a regular ongoing notification; promotion starts at GATE_READY (Google policy).
            .setRequestPromotedOngoing(content.requestPromoted)

        // The pill only appears when it has content, so every stage that has a value sets one.
        content.shortCriticalText?.let { builder.setShortCriticalText(it) }
        // Spike: coloured status badge on the right of the card (LiveUpdateConfig.showStatusBadge).
        content.statusBadge?.let { builder.setLargeIcon(badgeBitmap(it)) }

        return builder
            .setShowWhen(false)
            .setOnlyAlertOnce(content.onlyAlertOnce)
    }

    /**
     * Segments, fill, tracker and origin/destination icons of the bar; `setStyledByProgress` greys what isn't
     * reached yet.
     */
    private fun buildProgressStyle(progress: LiveUpdateProgress): ProgressStyle = ProgressStyle()
        .setProgressSegments(progress.segments.map { ProgressStyle.Segment(it.length).setColor(colorFor(it.color)) })
        .setProgress(progress.value)
        .setStyledByProgress(true)
        .setProgressTrackerIcon(iconFor(progress.tracker))
        .setProgressStartIcon(iconFor(progress.startIcon))
        .setProgressEndIcon(iconFor(progress.endIcon))

    /** An `IconCompat` for [role]. */
    private fun iconFor(role: IconRole): IconCompat = IconCompat.createWithResource(context, drawableFor(role))

    /** Tapping the notification goes through the analytics trampoline. */
    private fun buildClickPendingIntent(): PendingIntent {
        val intent = Intent(context, LiveActivityClickActivity::class.java).apply {
            putExtra(LiveActivityClickActivity.EXTRA_CLICK_SOURCE, ClickSource.LOCK_SCREEN)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(
            context,
            LiveUpdateManager.NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /** Drawable for each icon role. */
    @DrawableRes
    private fun drawableFor(role: IconRole): Int = when (role) {
        IconRole.CHIP_DEPARTURE -> R.drawable.ic_live_update_chip_departure
        IconRole.CHIP_DEPARTURE_DELAYED -> R.drawable.ic_live_update_chip_departure_delayed
        IconRole.CHIP_FLIGHT -> R.drawable.ic_live_update_chip_flight
        IconRole.CHIP_FLIGHT_DELAYED -> R.drawable.ic_live_update_chip_flight_delayed
        IconRole.TRACKER_NONE -> R.drawable.ic_airplane_placeholder_transparent
        IconRole.TRACKER_CHECK_IN -> R.drawable.ic_progress_tracker_check_in
        IconRole.TRACKER_GATE -> R.drawable.ic_progress_tracker_gate
        IconRole.TRACKER_TAKEOFF -> R.drawable.ic_airplane_taking_off_left_2
        IconRole.TRACKER_CRUISE -> R.drawable.ic_airplane_move_left_2
        IconRole.TRACKER_LANDING -> R.drawable.ic_airplane_land_left_2
        IconRole.TRACKER_BAGGAGE -> R.drawable.ic_progress_tracker_baggage
        IconRole.PROGRESS_START -> R.drawable.ic_progress_start
        IconRole.PROGRESS_END -> R.drawable.ic_progress_end
    }

    /** Resolved colour (day/night) for each colour role. */
    private fun colorFor(role: ColorRole): Int = ContextCompat.getColor(context, colorResFor(role))

    /** Named colour resource for each colour role. */
    @ColorRes
    private fun colorResFor(role: ColorRole): Int = when (role) {
        ColorRole.SEGMENT_ON_TIME -> R.color.progress_bar_segment
        ColorRole.SEGMENT_DELAYED -> R.color.progress_bar_segment_delayed
        ColorRole.SEGMENT_ERROR -> R.color.progress_bar_segment_error
        ColorRole.ACCENT_ON_TIME -> R.color.live_update_chip_on_time
        ColorRole.ACCENT_DELAYED -> R.color.live_update_chip_delayed
    }

    private companion object {
        /** Canvas resolution relative to the large-icon frame, so the system only scales down. */
        const val BADGE_SCALE = 2
        /** Height of the system's large-icon frame (approximate, 48 dp). */
        const val BADGE_FRAME_HEIGHT_DP = 48
        /** Free space on each side of the badge inside the frame, as a fraction of the frame. */
        const val BADGE_PADDING = 0.06f
    }
}
