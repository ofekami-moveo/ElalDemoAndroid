package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.AppLanguage
import com.elal.flightsdk.model.Carrier
import com.elal.flightsdk.model.Direction
import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Behaviour of the status-bar pill text per stage (Figma ELAL 6944:19137, 6944:19247). */
class ChipTextResolverTest {

    private val resolver = ChipTextResolver(FakeChipLabels())

    /** Gate stages show the bare gate, as in the Figma "E4" pill. */
    @Test
    fun gateStagesShowBareGate() {
        listOf(Stage.GATE_READY, Stage.BOARDING_PENDING, Stage.BOARDING, Stage.LAST_CALL).forEach { stage ->
            assertEquals("E4", resolver.resolve(state(stage, gate = "E4"), remainingFlightMs = null))
        }
    }

    /** A missing gate ("--" from the parser) or one too long for the pill leaves the pill icon-only. */
    @Test
    fun missingOrTooLongGateGivesNoText() {
        assertNull(resolver.resolve(state(Stage.BOARDING, gate = "--"), remainingFlightMs = null))
        assertNull(resolver.resolve(state(Stage.BOARDING, gate = "D123-456"), remainingFlightMs = null))
    }

    /** In flight the pill counts the time left as h:mm, then minutes in the last hour. */
    @Test
    fun departedShowsRemainingFlightTime() {
        val sixTwelve = (6 * 60 + 12) * 60_000L
        assertEquals("6:12h", resolver.resolve(state(Stage.DEPARTED), sixTwelve))
        assertEquals("45m", resolver.resolve(state(Stage.DEPARTED), 45 * 60_000L))
        assertEquals("1m", resolver.resolve(state(Stage.DEPARTED), 10_000L))
    }

    /** Without a known duration, or once it has run out, DEPARTED falls back to the arrival time. */
    @Test
    fun departedWithoutDurationShowsArrivalTime() {
        assertEquals("23:12", resolver.resolve(state(Stage.DEPARTED), remainingFlightMs = null))
        assertEquals("23:12", resolver.resolve(state(Stage.DEPARTED), remainingFlightMs = 0L))
    }

    /** Check-in shows the counters when they fit, else the departure time. */
    @Test
    fun checkInShowsCountersOrDepartureTime() {
        assertEquals("D78-99", resolver.resolve(state(Stage.CHECK_IN, counters = "D78-99"), null))
        assertEquals("17:00", resolver.resolve(state(Stage.CHECK_IN, counters = "--"), null))
        assertEquals("17:00", resolver.resolve(state(Stage.CHECK_IN, counters = "D78-D99, E1-E9"), null))
    }

    /** Baggage shows "Belt n", falls back to the bare belt, and LANDED falls back to the destination. */
    @Test
    fun baggageStagesShowBelt() {
        assertEquals("Belt 4", resolver.resolve(state(Stage.BAGGAGE, carousel = "4"), null))
        assertEquals("12A", resolver.resolve(state(Stage.BAGGAGE, carousel = "12A"), null)) // "Belt 12A" is 8
        assertEquals("JFK", resolver.resolve(state(Stage.LANDED, carousel = "--"), null))
        assertNull(resolver.resolve(state(Stage.BAGGAGE, carousel = "--"), null))
    }

    /** Every non-final stage produces text within the pill limit when its data is present. */
    @Test
    fun everyValueFitsThePill() {
        Stage.entries.filter { it != Stage.COMPLETED }.forEach { stage ->
            val text = resolver.resolve(state(stage, gate = "E4", counters = "D78-99", carousel = "4"), 6 * 3_600_000L)
            assertTrue("$stage → $text", text != null && ChipTextResolver.visibleLength(text) <= ChipTextResolver.MAX_CHIP_LENGTH)
        }
        assertNull(resolver.resolve(state(Stage.COMPLETED), null))
    }

    /** Invisible bidi isolates don't count against the 7-character limit. */
    @Test
    fun bidiControlsAreNotCounted() {
        assertEquals(7, ChipTextResolver.visibleLength("⁦Gate D3⁩"))
    }

    private fun state(
        stage: Stage,
        gate: String = "--",
        counters: String = "--",
        carousel: String = "--",
    ) = FlightContentState(
        flightNumber = "LY 317",
        direction = Direction.DEPARTURE,
        carrier = Carrier.EL_AL,
        origin = "TLV",
        destination = "JFK",
        departureTime = Instant.parse("2026-11-10T17:00:00Z"),
        arrivalTime = Instant.parse("2026-11-10T23:12:00Z"),
        gate = gate,
        checkInCounters = counters,
        baggageCarousel = carousel,
        stage = stage,
        language = AppLanguage.EN,
    )
}
