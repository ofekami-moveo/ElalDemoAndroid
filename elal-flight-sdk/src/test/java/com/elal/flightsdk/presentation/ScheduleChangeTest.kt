package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.GateRecord
import com.elal.flightsdk.model.LiveUpdateConfig
import com.elal.flightsdk.model.ScheduleChangeStyle
import com.elal.flightsdk.model.SpanType
import com.elal.flightsdk.model.Stage
import com.elal.flightsdk.model.TextSpanSpec
import com.elal.flightsdk.model.TimeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Old value struck through, new value next to it (task 3b.1). `[x]` marks a bidi-isolated value. */
class ScheduleChangeTest {

    private val en = LiveUpdateContentBuilder(FakeStringProvider(), FakeBidiWrapper(), FakeChipLabels())
    private val he = LiveUpdateContentBuilder(
        XmlStringProvider(XmlStrings("values-iw")), FakeBidiWrapper(), XmlChipLabels(XmlStrings("values-iw"))
    )
    private val snapshot = TimeSnapshot(flightProgress = 0.5, remainingFlightMs = null)

    /** Departure, arrival and gate changes: the old value is struck, the whole change is read as "new, was old". */
    @Test
    fun spanRangesForDepartureArrivalAndGate() {
        val boarding = en.build(delayed(TestStates.state(Stage.BOARDING)), snapshot, false, previousGate = "D2", scheduleChangeStyle = SPAN)
        // The change lines replace the stage's line (2 lines max in the expanded view).
        assertEquals("Gate [D2] [D3]\nDeparts [17:00] [17:55]", boarding.text)
        assertStruck(boarding.text, boarding.textSpans, listOf("[D2]", "[17:00]"))
        assertSpoken(boarding.textSpans, listOf("D3, was D2", "17:55, was 17:00"))

        val departed = en.build(delayed(TestStates.state(Stage.DEPARTED)), snapshot, false, scheduleChangeStyle = SPAN)
        assertEquals("Lands at [23:12] [00:07]", departed.text)
        assertStruck(departed.text, departed.textSpans, listOf("[23:12]"))

        val heBoarding = he.build(delayed(TestStates.state(Stage.BOARDING)), snapshot, false, previousGate = "D2", scheduleChangeStyle = SPAN)
        assertTrue(heBoarding.text, heBoarding.text.endsWith("המראה [17:00] [17:55]"))
        assertStruck(heBoarding.text, heBoarding.textSpans, listOf("[D2]", "[17:00]"))
        assertSpoken(heBoarding.textSpans, listOf("D3, במקום D2", "17:55, במקום 17:00"))
    }

    /** Plain text is the default (2026-11-16: the emulator doesn't draw the strike): written out, without spans. */
    @Test
    fun plainTextIsTheDefault() {
        assertEquals(ScheduleChangeStyle.PLAIN_TEXT, LiveUpdateConfig().scheduleChangeStyle)
        val enText = en.build(delayed(TestStates.state(Stage.GATE_READY)), snapshot, false)
        assertEquals("Departs [17:55] (was [17:00])", enText.text)
        assertTrue(enText.textSpans.isEmpty())
        val heText = he.build(delayed(TestStates.state(Stage.GATE_READY)), snapshot, false)
        assertEquals("המראה [17:55] (במקום [17:00])", heText.text)
    }

    /** Without the new fields (the real FCM payload today) the content is exactly as before. */
    @Test
    fun noScheduleFieldsGiveTodaysText() {
        Stage.entries.forEach { stage ->
            val content = en.build(TestStates.state(stage), snapshot, false)
            assertTrue("$stage", content.textSpans.isEmpty())
            assertFalse("$stage: ${content.text}", content.text.contains("Departs"))
        }
        assertEquals("Lands at [23:12]", en.build(TestStates.state(Stage.DEPARTED), snapshot, false).text)
    }

    /** Estimated equal to scheduled: nothing struck and not delayed; a later estimate counts as delayed. */
    @Test
    fun sameTimeIsNoChangeAndLaterIsDelayed() {
        val departure = Instant.parse("2026-11-10T17:00:00Z")
        val onTime = TestStates.state(Stage.GATE_READY).copy(
            scheduledDepartureTime = departure, estimatedDepartureTime = departure
        )
        val content = en.build(onTime, snapshot, false, scheduleChangeStyle = SPAN)
        assertTrue(content.textSpans.isEmpty())
        assertEquals("[[D3]]", content.text)
        assertFalse(FlightDelayResolver.isDelayed(onTime))
        assertTrue(FlightDelayResolver.isDelayed(delayed(TestStates.state(Stage.GATE_READY))))
    }

    /** The pill shows the estimated time wherever it shows a time. */
    @Test
    fun pillShowsEstimatedTime() {
        val noCounters = delayed(TestStates.state(Stage.CHECK_IN)).copy(checkInCounters = "--")
        assertEquals("[17:55]", en.build(noCounters, snapshot, false).shortCriticalText)
        assertEquals("[17:55]", en.build(delayed(TestStates.state(Stage.DEPARTURE_PENDING)), snapshot, false).shortCriticalText)
        assertEquals("[00:07]", en.build(delayed(TestStates.state(Stage.DEPARTED)), snapshot, false).shortCriticalText)
        assertEquals("[00:07]", en.build(delayed(TestStates.state(Stage.LANDING_SOON)), snapshot, false).shortCriticalText)
    }

    /** A gate change records the old gate; a new activityId or the gate changing back clears it. */
    @Test
    fun gateChangeIsRecordedPerJourney() {
        val changed = GateChangeTracker.update(null, "J1", oldGate = "D2", newGate = "D3")
        assertEquals(GateRecord("J1", "D2"), changed)
        assertEquals(changed, GateChangeTracker.update(changed, "J1", oldGate = "D3", newGate = "D3"))
        assertNull(GateChangeTracker.update(changed, "J2", oldGate = "D3", newGate = "D3"))
        assertNull(GateChangeTracker.update(changed, "J1", oldGate = "D3", newGate = "D2"))
        assertNull(GateChangeTracker.update(null, "J1", oldGate = "--", newGate = "D3"))
    }

    /** Scheduled 17:00 → estimated 17:55 departure, 23:12 → 00:07 arrival (+55 min, like the demo's toggle). */
    private fun delayed(state: FlightContentState): FlightContentState = state.copy(
        scheduledDepartureTime = state.departureTime,
        estimatedDepartureTime = state.departureTime.plusSeconds(DELAY_S),
        scheduledArrivalTime = state.arrivalTime,
        estimatedArrivalTime = state.arrivalTime.plusSeconds(DELAY_S),
    )

    private fun assertStruck(text: String, spans: List<TextSpanSpec>, expected: List<String>) {
        val struck = spans.filter { it.type == SpanType.STRIKETHROUGH }.map { text.substring(it.start, it.end) }
        assertEquals(expected, struck)
    }

    private fun assertSpoken(spans: List<TextSpanSpec>, expected: List<String>) {
        assertEquals(expected, spans.filter { it.type == SpanType.SPOKEN }.map { it.spokenText })
    }

    private companion object {
        const val DELAY_S = 55 * 60L
        val SPAN = ScheduleChangeStyle.SPAN
    }
}
