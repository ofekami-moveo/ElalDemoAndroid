package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.ColorRole
import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.IconRole
import com.elal.flightsdk.model.PostedState
import com.elal.flightsdk.model.Stage
import com.elal.flightsdk.model.TimeSnapshot
import com.elal.flightsdk.sample.SampleFlightData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Progress visuals, promotion and alerting per stage (task 3.1, requirements §3b). */
class StageVisualsTest {

    private val builder = LiveUpdateContentBuilder(FakeStringProvider(), FakeBidiWrapper(), FakeChipLabels())
    private val midFlight = TimeSnapshot(flightProgress = 0.5, remainingFlightMs = 3_600_000L)

    /** Along both demo journeys the bar never goes backwards (F2: DEPARTURE_PENDING stays at 88). */
    @Test
    fun progressNeverDecreasesAlongBothSequences() {
        listOf(SampleFlightData.journeySimulationSequence, SampleFlightData.arrivalSimulationSequence).forEach { sequence ->
            val values = sequence.map { builder.build(it, midFlight, false).progress.value }
            values.zipWithNext().forEach { (before, after) -> assertTrue("$values", after >= before) }
        }
        assertEquals(88, builder.build(TestStates.state(Stage.DEPARTURE_PENDING), midFlight, false).progress.value)
    }

    /** Segments: amber when delayed before landing, brand blue from landing on, grey on errors. */
    @Test
    fun segmentColourRolePerStage() {
        Stage.entries.forEach { stage ->
            val onTime = segmentColor(TestStates.state(stage))
            val delayed = segmentColor(delayed(TestStates.state(stage)))
            when (stage) {
                Stage.CONNECTION_ERROR, Stage.GENERAL_ERROR -> {
                    assertEquals("$stage", ColorRole.SEGMENT_ERROR, onTime)
                    assertEquals("$stage", ColorRole.SEGMENT_ERROR, delayed)
                }
                Stage.LANDING_SOON, Stage.LANDED, Stage.BAGGAGE, Stage.COMPLETED -> {
                    assertEquals("$stage", ColorRole.SEGMENT_ON_TIME, onTime)
                    assertEquals("$stage", ColorRole.SEGMENT_ON_TIME, delayed)
                }
                else -> {
                    assertEquals("$stage", ColorRole.SEGMENT_ON_TIME, onTime)
                    assertEquals("$stage", ColorRole.SEGMENT_DELAYED, delayed)
                }
            }
        }
    }

    /** `flightStatus` ≈ "DELAYED" in any case, padded; anything else is on time. */
    @Test
    fun delayParsingFromState() {
        listOf("DELAYED", "delayed", "  Delayed ").forEach {
            assertTrue(it, FlightDelayResolver.isDelayed(TestStates.state(Stage.BOARDING).copy(flightStatus = it)))
        }
        listOf("ON-TIME", "", null, "DELAY").forEach {
            assertFalse("$it", FlightDelayResolver.isDelayed(TestStates.state(Stage.BOARDING).copy(flightStatus = it)))
        }
    }

    /** One tracker per phase: check-in, gate, takeoff, cruise, landing, baggage; none on errors. */
    @Test
    fun trackerPerPhase() {
        val expected = mapOf(
            Stage.CHECK_IN to IconRole.TRACKER_CHECK_IN,
            Stage.GATE_READY to IconRole.TRACKER_GATE,
            Stage.BOARDING_PENDING to IconRole.TRACKER_GATE,
            Stage.BOARDING to IconRole.TRACKER_GATE,
            Stage.LAST_CALL to IconRole.TRACKER_TAKEOFF,
            Stage.DEPARTURE_PENDING to IconRole.TRACKER_TAKEOFF,
            Stage.DEPARTED to IconRole.TRACKER_CRUISE,
            Stage.LANDING_SOON to IconRole.TRACKER_LANDING,
            Stage.LANDED to IconRole.TRACKER_LANDING,
            Stage.BAGGAGE to IconRole.TRACKER_BAGGAGE,
            Stage.COMPLETED to IconRole.TRACKER_LANDING,
            Stage.CONNECTION_ERROR to IconRole.TRACKER_NONE,
            Stage.GENERAL_ERROR to IconRole.TRACKER_NONE,
        )
        assertEquals(Stage.entries.toSet(), expected.keys)
        expected.forEach { (stage, tracker) ->
            assertEquals("$stage", tracker, builder.build(TestStates.state(stage), midFlight, false).progress.tracker)
        }
    }

    /** Origin and destination icons are on the bar in every stage. */
    @Test
    fun startAndEndIconsAlwaysPresent() {
        Stage.entries.forEach { stage ->
            val progress = builder.build(TestStates.state(stage), midFlight, false).progress
            assertEquals("$stage", IconRole.PROGRESS_START, progress.startIcon)
            assertEquals("$stage", IconRole.PROGRESS_END, progress.endIcon)
        }
    }

    /** Promotion from GATE_READY on (Google policy); CHECK_IN is a regular ongoing notification. */
    @Test
    fun promotionPerStage() {
        Stage.entries.forEach { stage ->
            val promoted = builder.build(TestStates.state(stage), midFlight, false).requestPromoted
            assertEquals("$stage", stage != Stage.CHECK_IN, promoted)
        }
    }

    /** Alerts on entering the §3b stages and when the delay flips; never on a re-post, a tick or an error. */
    @Test
    fun alertOnEntryMatrix() {
        val alertsOnEntry = setOf(Stage.CHECK_IN, Stage.GATE_READY, Stage.BOARDING, Stage.LAST_CALL, Stage.LANDED)
        Stage.entries.forEach { stage ->
            val state = TestStates.state(stage)
            val entered = builder.build(state, midFlight, false, previous = PostedState(Stage.COMPLETED, false))
            assertEquals("enter $stage", stage in alertsOnEntry, !entered.onlyAlertOnce)
            val reposted = builder.build(state, midFlight, false, previous = entered.postedState)
            assertTrue("repost $stage", reposted.onlyAlertOnce)
            assertTrue("tick $stage", builder.build(state, midFlight, isSelfUpdate = true, previous = null).onlyAlertOnce)
        }
        val boarding = TestStates.state(Stage.BOARDING)
        val onTime = builder.build(boarding, midFlight, false, previous = PostedState(Stage.BOARDING, false))
        val nowDelayed = builder.build(delayed(boarding), midFlight, false, previous = onTime.postedState)
        assertFalse("delay flip alerts", nowDelayed.onlyAlertOnce)
    }

    private fun segmentColor(state: FlightContentState): ColorRole =
        builder.build(state, midFlight, false).progress.segments.map { it.color }.distinct().single()

    private fun delayed(state: FlightContentState) = state.copy(flightStatus = "DELAYED")
}
