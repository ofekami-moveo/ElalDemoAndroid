package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.ColorRole
import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.IconRole
import com.elal.flightsdk.model.LiveUpdateContent
import com.elal.flightsdk.model.LiveUpdateProgress
import com.elal.flightsdk.model.PostedState
import com.elal.flightsdk.model.ScheduleChangeStyle
import com.elal.flightsdk.model.SegmentSpec
import com.elal.flightsdk.model.Stage
import com.elal.flightsdk.model.StatusBadge
import com.elal.flightsdk.model.TextSpanSpec
import com.elal.flightsdk.model.TimeSnapshot
import com.elal.flightsdk.utils.orDash
import com.elal.flightsdk.utils.toFormattedTime

/**
 * Builds everything the Live Update shows from a [FlightContentState] and one [TimeSnapshot], with no
 * `android.*` access, so the whole notification content is covered by JVM tests.
 * `LiveUpdateNotificationMapper` turns the result into a `Notification`.
 */
internal class LiveUpdateContentBuilder(
    private val strings: StringProvider,
    private val bidi: BidiWrapper,
    chipLabels: ChipLabels,
) {

    private val chipTextResolver = ChipTextResolver(chipLabels)

    /**
     * Returns the content for [state] at [snapshot]. [isSelfUpdate] marks an in-flight progress tick, which
     * never alerts; [previous] is what was posted before (`null` on the first post), for the alert decision.
     * [promotionAllowed] is false in the Android 15 fallback, which shows the same content unpromoted.
     * [previousGate] is the gate before the last gate change (shown struck through), in [scheduleChangeStyle].
     */
    fun build(
        state: FlightContentState,
        snapshot: TimeSnapshot,
        isSelfUpdate: Boolean,
        previous: PostedState? = null,
        promotionAllowed: Boolean = true,
        previousGate: String? = null,
        scheduleChangeStyle: ScheduleChangeStyle = ScheduleChangeStyle.PLAIN_TEXT,
        showStatusBadge: Boolean = false,
    ): LiveUpdateContent {
        val visuals = StageVisualCatalog.visualsFor(state.stage)
        // A delay is shown only until landing; after that it no longer matters to the traveller.
        val isDelayed = visuals.segmentStyle == SegmentStyle.DELAY_AWARE && FlightDelayResolver.isDelayed(state)
        val shouldAlert = AlertPolicy.shouldAlert(state.stage, visuals.alertOnEntry, isDelayed, previous, isSelfUpdate)
        val content = buildStyledText(state, previousGate, ScheduleChangeFormatter(strings, bidi, scheduleChangeStyle))
        return LiveUpdateContent(
            title = buildTitle(state),
            text = content.text,
            textSpans = content.spans,
            subText = strings.get(StringKey.SUBTEXT_ROUTE, bidi.wrap(state.origin), bidi.wrap(state.destination)),
            shortCriticalText = chipTextResolver.resolve(state, snapshot.remainingFlightMs)?.let { bidi.wrap(it) },
            publicText = buildPublicText(state),
            smallIcon = pillIcon(visuals.isAirborne, isDelayed),
            accentColor = if (isDelayed) ColorRole.ACCENT_DELAYED else ColorRole.ACCENT_ON_TIME,
            progress = buildProgress(visuals, isDelayed, snapshot),
            onlyAlertOnce = !shouldAlert,
            requestPromoted = promotionAllowed && visuals.isPromoted,
            postedState = PostedState(state.stage, isDelayed),
            actions = visuals.actions,
            statusBadge = statusBadge(state, visuals, showStatusBadge),
        )
    }

    /**
     * "LY 317 · Boarding", the same in the collapsed and expanded views. No time: the content and the pill
     * carry it. The server's stage title wins; the SDK label is used when it sends none, and always on errors.
     */
    private fun buildTitle(state: FlightContentState): String {
        val serverTitle = state.stageTitle.takeIf { it.isNotBlank() && it != MISSING }
        val stageTitle = when (state.stage) {
            Stage.CONNECTION_ERROR, Stage.GENERAL_ERROR -> strings.get(stageTitleKey(state.stage))
            else -> serverTitle ?: strings.get(stageTitleKey(state.stage))
        }
        return strings.get(StringKey.TITLE, bidi.wrap(state.flightNumber.orDash()), stageTitle)
    }

    /** The SDK's own stage label, used when the server sends no stage title. */
    private fun stageTitleKey(stage: Stage): StringKey = when (stage) {
        Stage.CHECK_IN -> StringKey.STAGE_CHECK_IN
        Stage.GATE_READY -> StringKey.STAGE_GATE_READY
        Stage.BOARDING_PENDING -> StringKey.STAGE_BOARDING_PENDING
        Stage.BOARDING -> StringKey.STAGE_BOARDING
        Stage.LAST_CALL -> StringKey.STAGE_LAST_CALL
        Stage.DEPARTURE_PENDING -> StringKey.STAGE_DEPARTURE_PENDING
        Stage.DEPARTED -> StringKey.STAGE_DEPARTED
        Stage.LANDING_SOON -> StringKey.STAGE_LANDING_SOON
        Stage.LANDED -> StringKey.STAGE_LANDED
        Stage.BAGGAGE -> StringKey.STAGE_BAGGAGE
        Stage.COMPLETED -> StringKey.STAGE_COMPLETED
        Stage.CONNECTION_ERROR -> StringKey.STAGE_CONNECTION_ERROR
        Stage.GENERAL_ERROR -> StringKey.STAGE_GENERAL_ERROR
    }

    /**
     * The content text (task 3b). Without a change it is exactly [buildText]. With one, the change lines replace the
     * stage's line, because the expanded view shows only 2 lines (decided 2026-11-16): "Gate ~~D2~~ D3" on gate stages
     * after a gate change, "Departs ~~17:00~~ 17:55" before takeoff when the departure moved, and "Lands at ~~23:12~~
     * 00:07" in DEPARTED / LANDING_SOON when the arrival moved; at most [MAX_CHANGE_LINES].
     */
    private fun buildStyledText(state: FlightContentState, previousGate: String?, changes: ScheduleChangeFormatter): StyledLine {
        val lines = mutableListOf<StyledLine>()
        val gate = state.gate?.takeIf { it.isNotBlank() && it != MISSING }
        if (state.stage in GATE_STAGES && previousGate != null && gate != null && previousGate != gate) {
            lines += changes.line(StringKey.PUBLIC_GATE, previousGate, gate)
        }
        if ((state.stage == Stage.CHECK_IN || state.stage in GATE_STAGES) && ScheduleTimes.departureChanged(state)) {
            lines += changes.line(
                StringKey.CHANGE_DEPARTS,
                ScheduleTimes.scheduledDeparture(state).toFormattedTime(),
                ScheduleTimes.displayDeparture(state).toFormattedTime(),
            )
        }
        if ((state.stage == Stage.DEPARTED || state.stage == Stage.LANDING_SOON) && ScheduleTimes.arrivalChanged(state)) {
            lines += changes.line(
                StringKey.CONTENT_LANDS_AT,
                ScheduleTimes.scheduledArrival(state).toFormattedTime(),
                ScheduleTimes.displayArrival(state).toFormattedTime(),
            )
        }
        if (lines.isEmpty()) {
            return StyledLine(buildText(state), emptyList())
        }
        return joinLines(lines.take(MAX_CHANGE_LINES))
    }

    /** Joins [lines] with line breaks, shifting each line's spans to its offset in the result. */
    private fun joinLines(lines: List<StyledLine>): StyledLine {
        val text = StringBuilder()
        val spans = mutableListOf<TextSpanSpec>()
        lines.forEachIndexed { index, line ->
            if (index > 0) {
                text.append(LINE_BREAK)
            }
            val offset = text.length
            spans += line.spans.map { it.copy(start = it.start + offset, end = it.end + offset) }
            text.append(line.text)
        }
        return StyledLine(text.toString(), spans)
    }

    /** The content line per stage, from the server's labels plus the bidi-isolated values. */
    private fun buildText(state: FlightContentState): String = when (state.stage) {
        Stage.CHECK_IN -> labelAndValue(state.title, state.checkInCounters.orDash())
        Stage.GATE_READY -> labelAndValue(state.message, labelAndValue(state.gateTitle, state.gate.orDash()))
        Stage.BOARDING_PENDING -> state.message.orDash()
        Stage.BOARDING -> state.gate
            ?.takeIf { it.isNotBlank() && it != MISSING }
            ?.let { labelAndValue(state.message, labelAndValue(state.gateTitle, it)) }
            ?: state.message.orDash()
        Stage.LAST_CALL -> labelAndValue(state.title, labelAndValue(state.gateTitle, state.gate.orDash()))
        Stage.DEPARTURE_PENDING -> buildEstimatedFlightTime(state)
        Stage.DEPARTED -> strings.get(StringKey.CONTENT_LANDS_AT, bidi.wrap(ScheduleTimes.displayArrival(state).toFormattedTime()))
        Stage.LANDING_SOON -> state.message.orDash()
        Stage.LANDED -> if (state.baggageCarousel.isNullOrBlank() || state.baggageCarousel == MISSING) {
            state.title
        } else {
            val carousel = labelAndValue(labelAndValue(state.message + '-', state.carouselTitle), state.baggageCarousel)
            "${state.title}\n$carousel"
        }
        Stage.BAGGAGE -> labelAndValue(state.title, state.baggageCarousel.orDash())
        Stage.COMPLETED -> strings.get(StringKey.CONTENT_ARRIVAL_WELCOME, bidi.wrap(state.destination))
        Stage.CONNECTION_ERROR -> strings.get(StringKey.CONTENT_CONNECTION_ERROR)
        Stage.GENERAL_ERROR -> strings.get(StringKey.CONTENT_GENERAL_ERROR)
    }

    /**
     * Lock-screen line (public version): the gate before takeoff, then the departure or arrival time.
     * Built only from these fields, so it can never contain the PNR or other booking data.
     */
    private fun buildPublicText(state: FlightContentState): String {
        val gate = state.gate?.takeIf { it.isNotBlank() && it != MISSING }
        val parts = when (state.stage) {
            Stage.CHECK_IN -> listOf(departs(state))
            Stage.GATE_READY, Stage.BOARDING_PENDING, Stage.BOARDING, Stage.LAST_CALL, Stage.DEPARTURE_PENDING ->
                listOfNotNull(gate?.let { strings.get(StringKey.PUBLIC_GATE, bidi.wrap(it)) }, departs(state))
            Stage.DEPARTED, Stage.LANDING_SOON, Stage.LANDED, Stage.BAGGAGE, Stage.COMPLETED ->
                listOf(strings.get(StringKey.PUBLIC_ARRIVES, bidi.wrap(ScheduleTimes.displayArrival(state).toFormattedTime())))
            Stage.CONNECTION_ERROR -> listOf(strings.get(StringKey.CONTENT_CONNECTION_ERROR))
            Stage.GENERAL_ERROR -> listOf(strings.get(StringKey.CONTENT_GENERAL_ERROR))
        }
        return parts.joinToString(PUBLIC_SEPARATOR)
    }

    /** "Departs 17:00" with the time bidi-isolated. */
    private fun departs(state: FlightContentState): String =
        strings.get(StringKey.PUBLIC_DEPARTS, bidi.wrap(ScheduleTimes.displayDeparture(state).toFormattedTime()))

    /**
     * DEPARTURE_PENDING line: the server message (e.g. "Estimated flight time") + the flight time from
     * `flightDuration` minutes ("3h:30m"); the message alone when there is no usable duration.
     */
    private fun buildEstimatedFlightTime(state: FlightContentState): String {
        val minutes = state.flightDuration?.takeIf { it > 0 } ?: return state.message.orDash()
        val flightTime = strings.get(StringKey.FLIGHT_TIME, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
        return labelAndValue(state.message, flightTime)
    }

    /**
     * The large-icon badge (spike): none when [enabled] is off or the stage has none; DELAYED when
     * [FlightDelayResolver.isDelayed] says so, else ON TIME.
     */
    private fun statusBadge(state: FlightContentState, visuals: StageVisuals, enabled: Boolean): StatusBadge? = when {
        !enabled || !visuals.showStatusBadge -> null
        FlightDelayResolver.isDelayed(state) -> StatusBadge.DELAYED
        else -> StatusBadge.ON_TIME
    }

    /** Bar segments, fill value, tracker and origin/destination icons; amber when delayed, grey on errors. */
    private fun buildProgress(visuals: StageVisuals, isDelayed: Boolean, snapshot: TimeSnapshot): LiveUpdateProgress {
        val segmentColor = when {
            visuals.segmentStyle == SegmentStyle.ERROR -> ColorRole.SEGMENT_ERROR
            isDelayed -> ColorRole.SEGMENT_DELAYED
            else -> ColorRole.SEGMENT_ON_TIME
        }
        return LiveUpdateProgress(
            segments = ProgressCalculator.segmentLengths.map { SegmentSpec(it, segmentColor) },
            value = ProgressCalculator.valueFor(visuals.progressPosition, snapshot.flightProgress),
            tracker = visuals.tracker,
            startIcon = IconRole.PROGRESS_START,
            endIcon = IconRole.PROGRESS_END,
        )
    }

    /**
     * Pill icon (Figma ELAL): the plane while airborne, the departure arrow otherwise; the badge variant
     * when delayed, because the system doesn't let us colour the pill background.
     */
    private fun pillIcon(isAirborne: Boolean, isDelayed: Boolean): IconRole = when {
        isAirborne && isDelayed -> IconRole.CHIP_FLIGHT_DELAYED
        isAirborne -> IconRole.CHIP_FLIGHT
        isDelayed -> IconRole.CHIP_DEPARTURE_DELAYED
        else -> IconRole.CHIP_DEPARTURE
    }

    /**
     * "label value" with the value bidi-isolated; a blank label or value is left out. Same output as
     * `combineLabelAndValue` in `utils`, but through [bidi] so it stays free of `android.*`.
     */
    private fun labelAndValue(label: String, value: String?): String = listOfNotNull(
        label.takeIf { it.isNotBlank() },
        value?.takeIf { it.isNotBlank() }?.let { bidi.wrap(it) },
    ).joinToString(" ")

    private companion object {
        const val MISSING = "--"
        const val MINUTES_PER_HOUR = 60
        const val PUBLIC_SEPARATOR = " · "
        const val LINE_BREAK = '\n'
        /** The expanded Live Update shows 2 content lines. */
        const val MAX_CHANGE_LINES = 2

        /** Stages that show the gate: a gate change is shown here. */
        val GATE_STAGES = setOf(
            Stage.GATE_READY, Stage.BOARDING_PENDING, Stage.BOARDING, Stage.LAST_CALL, Stage.DEPARTURE_PENDING
        )
    }
}
