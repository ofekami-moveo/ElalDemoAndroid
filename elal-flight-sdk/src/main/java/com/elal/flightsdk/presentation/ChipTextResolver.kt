package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.Stage
import com.elal.flightsdk.utils.toFormattedTime

/**
 * Picks the one short value shown in the status-bar pill (`setShortCriticalText`) for each stage.
 *
 * A promoted notification only gets a pill when it has content, so every rendered stage returns a value
 * when its data allows. The system shows the text in full only when it is short, so anything over
 * [MAX_CHIP_LENGTH] visible characters falls back to a shorter value or to `null` (icon only).
 * Design: Figma ELAL 6944:19137 (gate "E4"), 6944:19247 (in flight "6:12h").
 */
class ChipTextResolver(private val labels: ChipLabels) {

    /**
     * Returns the pill text for [state], or `null` when there is nothing short enough to show.
     *
     * [remainingFlightMs] is the time left until arrival, used only in DEPARTED; pass `null` when the
     * flight duration is unknown, and the arrival time is shown instead.
     */
    fun resolve(state: FlightContentState, remainingFlightMs: Long?): String? = when (state.stage) {
        Stage.CHECK_IN -> state.checkInCounters.presentOrNull()?.fitOrNull()
            ?: ScheduleTimes.displayDeparture(state).toFormattedTime()
        Stage.GATE_READY,
        Stage.BOARDING_PENDING,
        Stage.BOARDING,
        Stage.LAST_CALL -> state.gate.presentOrNull()?.fitOrNull()
        // Times are the estimated ones when the schedule changed (task 3b).
        Stage.DEPARTURE_PENDING -> ScheduleTimes.displayDeparture(state).toFormattedTime()
        Stage.DEPARTED -> remainingFlightMs?.takeIf { it > 0 }?.let { formatDuration(it) }?.fitOrNull()
            ?: ScheduleTimes.displayArrival(state).toFormattedTime()
        Stage.LANDING_SOON -> ScheduleTimes.displayArrival(state).toFormattedTime()
        Stage.LANDED -> beltOrNull(state.baggageCarousel) ?: state.destination.presentOrNull()?.fitOrNull()
        Stage.BAGGAGE -> beltOrNull(state.baggageCarousel)
        Stage.COMPLETED -> null
        Stage.CONNECTION_ERROR -> labels.offline.fitOrNull()
        Stage.GENERAL_ERROR -> labels.error.fitOrNull()
    }

    /** "Belt 4" when it fits, else the bare carousel, else `null`. */
    private fun beltOrNull(carousel: String?): String? {
        val value = carousel.presentOrNull() ?: return null
        return labels.belt(value).fitOrNull() ?: value.fitOrNull()
    }

    /** Formats [remainingMs] as hours:minutes, or minutes under an hour; rounds up so it never shows 0 early. */
    private fun formatDuration(remainingMs: Long): String {
        val totalMinutes = ((remainingMs + MS_PER_MINUTE - 1) / MS_PER_MINUTE).toInt()
        return if (totalMinutes >= MINUTES_PER_HOUR) {
            labels.durationHours(totalMinutes / MINUTES_PER_HOUR, totalMinutes % MINUTES_PER_HOUR)
        } else {
            labels.durationMinutes(totalMinutes)
        }
    }

    /** Returns this string when it has at most [MAX_CHIP_LENGTH] visible characters, else `null`. */
    private fun String.fitOrNull(): String? = takeIf { visibleLength(it) <= MAX_CHIP_LENGTH }

    /** Treats null, blank and the parser's `"--"` placeholder as missing. */
    private fun String?.presentOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() && it != MISSING }

    companion object {
        /** The pill shows text in full only when it is about this short (Android Live Updates docs). */
        const val MAX_CHIP_LENGTH = 7
        private const val MISSING = "--"
        private const val MS_PER_MINUTE = 60_000L
        private const val MINUTES_PER_HOUR = 60
        private val BIDI_CONTROLS = Regex("[‎‏‪-‮⁦-⁩]")

        /** Counts code points the user sees: invisible bidi controls added for RTL safety don't count. */
        fun visibleLength(text: String): Int {
            val visible = text.replace(BIDI_CONTROLS, "")
            return visible.codePointCount(0, visible.length)
        }
    }
}
