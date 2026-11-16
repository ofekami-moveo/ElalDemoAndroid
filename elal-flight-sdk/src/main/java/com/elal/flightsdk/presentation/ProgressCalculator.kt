package com.elal.flightsdk.presentation

/**
 * Segment layout and fill value of the `ProgressStyle` bar. Always exactly 3 segments:
 * seg0 (short) + seg1 (short) + seg2 (long / flight path).
 *
 * - CHECK_IN: 0, all grey, no tracker
 * - GATE_READY, BOARDING: end of seg0
 * - LAST_CALL, DEPARTURE_PENDING: start of the flight path, plane at the start (takeoff icon)
 * - DEPARTED: grows along the flight path
 * - LANDING_SOON, LANDED, BAGGAGE: full bar, landing icon
 */
internal object ProgressCalculator {

    const val PROGRESS_MAX = 240

    // Android's ProgressStyle gap bug fixed
    private const val SEG0_COMPENSATION = PROGRESS_MAX / 24              // 10
    private const val SHORT_SEGMENT = PROGRESS_MAX / 5                   // 48
    const val SEG0_LENGTH = SHORT_SEGMENT - SEG0_COMPENSATION            // 38
    const val SEG1_LENGTH = SHORT_SEGMENT                                // 48
    const val FLIGHT_START = SEG0_LENGTH + SEG1_LENGTH + 2               // 88
    const val FLIGHT_SEGMENT_LENGTH = PROGRESS_MAX - FLIGHT_START        // 152

    /** The three segment lengths, in bar order. */
    val segmentLengths: List<Int> = listOf(SEG0_LENGTH, SEG1_LENGTH, FLIGHT_SEGMENT_LENGTH)

    /**
     * The fill value for [position]. In flight, [flightProgress] (0..1) fills the flight segment, stopping
     * one unit short of its end, as before.
     */
    fun valueFor(position: ProgressPosition, flightProgress: Double): Int = when (position) {
        ProgressPosition.START -> 0
        ProgressPosition.GATE -> SEG0_LENGTH
        ProgressPosition.FLIGHT_START -> FLIGHT_START
        ProgressPosition.IN_FLIGHT -> {
            val filled = (flightProgress * FLIGHT_SEGMENT_LENGTH).toInt().coerceIn(0, FLIGHT_SEGMENT_LENGTH - 1)
            FLIGHT_START + filled
        }
        ProgressPosition.END -> PROGRESS_MAX
    }
}
