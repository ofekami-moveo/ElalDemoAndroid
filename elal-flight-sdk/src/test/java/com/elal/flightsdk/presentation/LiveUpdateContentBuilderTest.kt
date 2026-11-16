package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.AppLanguage
import com.elal.flightsdk.model.Carrier
import com.elal.flightsdk.model.ColorRole
import com.elal.flightsdk.model.Direction
import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.IconRole
import com.elal.flightsdk.model.Stage
import com.elal.flightsdk.model.TimeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Content per stage with English strings; `[x]` marks a bidi-isolated value. Group 1 pinned today's output;
 * group 2 changed the title and the DEPARTED text on purpose.
 */
class LiveUpdateContentBuilderTest {

    private val builder = LiveUpdateContentBuilder(FakeStringProvider(), FakeBidiWrapper(), FakeChipLabels())

    /** Title and content text per stage (group 2: "flight · stage" title, "Lands at" in DEPARTED). */
    @Test
    fun titleAndContentPerStage() {
        val title = "[LY 317] · Boarding"
        val expected = mapOf(
            Stage.CHECK_IN to (title to "T [D78-99]"),
            Stage.GATE_READY to (title to "M [Gate: [D3]]"),
            Stage.BOARDING_PENDING to (title to "M"),
            Stage.BOARDING to (title to "M [Gate: [D3]]"),
            Stage.LAST_CALL to (title to "T [Gate: [D3]]"),
            Stage.DEPARTURE_PENDING to (title to "M [3h:30m]"),
            Stage.DEPARTED to (title to "Lands at [23:12]"),
            Stage.LANDING_SOON to (title to "M"),
            Stage.LANDED to (title to "T\nM- [Belt] [4]"),
            Stage.BAGGAGE to (title to "T [4]"),
            Stage.COMPLETED to (title to "Welcome to [JFK]"),
            Stage.CONNECTION_ERROR to ("[LY 317] · Connection error" to "You seem to be offline. Check your connection."),
            Stage.GENERAL_ERROR to ("[LY 317] · Error" to "Error has occurred"),
        )
        assertEquals(Stage.entries.toSet(), expected.keys)
        expected.forEach { (stage, titleAndText) ->
            val content = builder.build(state(stage), snapshot(remainingMs = THREE_H_FIVE_MIN), isSelfUpdate = false)
            assertEquals("title $stage", titleAndText.first, content.title)
            assertEquals("text $stage", titleAndText.second, content.text)
        }
        // BOARDING without a gate falls back to the message alone.
        assertEquals("M", builder.build(state(Stage.BOARDING, gate = "--"), snapshot(), false).text)
        // LANDED without a carousel shows only the title.
        assertEquals("T", builder.build(state(Stage.LANDED, carousel = "--"), snapshot(), false).text)
    }

    /** In flight the pill counts down from the timer, and shows the arrival time when no timer runs. */
    @Test
    fun departedPillCountsDown() {
        val departed = state(Stage.DEPARTED)
        assertEquals("[3:05h]", builder.build(departed, snapshot(THREE_H_FIVE_MIN), false).shortCriticalText)
        assertEquals("[45m]", builder.build(departed, snapshot(45 * MINUTE), false).shortCriticalText)
        assertEquals("[23:12]", builder.build(departed, snapshot(null), false).shortCriticalText)
    }

    /** Progress per stage (group 3: DEPARTURE_PENDING at 88, F2 fixed). */
    @Test
    fun progressPerStageMatchesToday() {
        val expected = mapOf(
            Stage.CHECK_IN to 0,
            Stage.GATE_READY to 38,
            Stage.BOARDING_PENDING to 38,
            Stage.BOARDING to 38,
            Stage.LAST_CALL to 88,
            Stage.DEPARTURE_PENDING to 88,
            Stage.DEPARTED to 164,
            Stage.LANDING_SOON to 240,
            Stage.LANDED to 240,
            Stage.BAGGAGE to 240,
            Stage.COMPLETED to 240,
            Stage.CONNECTION_ERROR to 0,
            Stage.GENERAL_ERROR to 0,
        )
        expected.forEach { (stage, value) ->
            val progress = builder.build(state(stage), snapshot(flightProgress = 0.5), false).progress
            assertEquals("progress $stage", value, progress.value)
            assertEquals(listOf(38, 48, 152), progress.segments.map { it.length })
        }
        // Full (or unknown) duration parks the plane one step before the end of the long segment.
        assertEquals(239, builder.build(state(Stage.DEPARTED), snapshot(flightProgress = 1.0), false).progress.value)

        val delayed = builder.build(state(Stage.BOARDING, status = "DELAYED"), snapshot(), false)
        assertTrue(delayed.progress.segments.all { it.color == ColorRole.SEGMENT_DELAYED })
        assertEquals(ColorRole.ACCENT_DELAYED, delayed.accentColor)
    }

    /** Pill icon per stage, with the delayed variants (trackers: `StageVisualsTest`). */
    @Test
    fun pillIconPerStage() {
        Stage.entries.forEach { stage ->
            val content = builder.build(state(stage), snapshot(), false)
            val airborne = stage == Stage.DEPARTED || stage == Stage.LANDING_SOON
            val pillIcon = if (airborne) IconRole.CHIP_FLIGHT else IconRole.CHIP_DEPARTURE
            assertEquals("pill icon $stage", pillIcon, content.smallIcon)
            assertEquals(ColorRole.ACCENT_ON_TIME, content.accentColor)
        }
        assertEquals(
            IconRole.CHIP_FLIGHT_DELAYED,
            builder.build(state(Stage.DEPARTED, status = "delayed"), snapshot(), false).smallIcon
        )
        assertEquals(
            IconRole.CHIP_DEPARTURE_DELAYED,
            builder.build(state(Stage.GATE_READY, status = "delayed"), snapshot(), false).smallIcon
        )
    }

    /** Error stages: error title, empty bar, no tracker, and never re-alert; other stages alert unless self-updating. */
    @Test
    fun errorStagesAndAlerting() {
        listOf(Stage.CONNECTION_ERROR, Stage.GENERAL_ERROR).forEach { stage ->
            val content = builder.build(state(stage), snapshot(), isSelfUpdate = false)
            assertEquals(0, content.progress.value)
            assertEquals(IconRole.TRACKER_NONE, content.progress.tracker)
            assertTrue(content.progress.segments.all { it.color == ColorRole.SEGMENT_ERROR })
            assertTrue("onlyAlertOnce $stage", content.onlyAlertOnce)
        }
        assertEquals("[Offline]", builder.build(state(Stage.CONNECTION_ERROR), snapshot(), false).shortCriticalText)
        assertFalse(builder.build(state(Stage.BOARDING), snapshot(), isSelfUpdate = false).onlyAlertOnce)
        assertTrue(builder.build(state(Stage.DEPARTED), snapshot(), isSelfUpdate = true).onlyAlertOnce)
    }

    private fun snapshot(
        remainingMs: Long? = null,
        flightProgress: Double = 1.0,
    ) = TimeSnapshot(flightProgress = flightProgress, remainingFlightMs = remainingMs)

    private fun state(
        stage: Stage,
        gate: String = "D3",
        carousel: String = "4",
        status: String = "ON-TIME",
    ) = FlightContentState(
        flightNumber = "LY 317",
        direction = Direction.DEPARTURE,
        carrier = Carrier.EL_AL,
        origin = "TLV",
        destination = "JFK",
        departureTime = Instant.parse("2026-11-10T17:00:00Z"),
        arrivalTime = ARRIVAL,
        gate = gate,
        checkInCounters = "D78-99",
        baggageCarousel = carousel,
        stage = stage,
        flightStatus = status,
        title = "T",
        stageTitle = "Boarding",
        message = "M",
        gateTitle = "Gate:",
        carouselTitle = "Belt",
        flightDuration = 210,
        language = AppLanguage.EN,
    )

    private companion object {
        val ARRIVAL: Instant = Instant.parse("2026-11-10T23:12:00Z")
        const val MINUTE = 60_000L
        const val THREE_H_FIVE_MIN = (3 * 60 + 5) * MINUTE
    }
}
