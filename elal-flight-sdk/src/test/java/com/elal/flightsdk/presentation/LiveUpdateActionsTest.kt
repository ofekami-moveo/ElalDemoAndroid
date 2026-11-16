package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.LiveUpdateAction
import com.elal.flightsdk.model.Stage
import com.elal.flightsdk.model.TimeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

/** Notification actions (task 4.2; Directions removed 2026-11-16): only those the host can open. */
class LiveUpdateActionsTest {

    private val builder = LiveUpdateContentBuilder(FakeStringProvider(), FakeBidiWrapper(), FakeChipLabels())
    private val snapshot = TimeSnapshot(flightProgress = 0.0, remainingFlightMs = null)

    /** Boarding pass from GATE_READY until takeoff; no button at check-in or after takeoff. */
    @Test
    fun actionsPerStage() {
        Stage.entries.forEach { stage ->
            val expected = when (stage) {
                Stage.GATE_READY, Stage.BOARDING_PENDING, Stage.BOARDING, Stage.LAST_CALL, Stage.DEPARTURE_PENDING ->
                    listOf(LiveUpdateAction.BOARDING_PASS)
                else -> emptyList()
            }
            val actions = builder.build(TestStates.state(stage), snapshot, false).actions
            assertEquals("$stage", expected, actions)
        }
    }

    /** An action whose host target is `null` is left out. */
    @Test
    fun nullTargetHidesTheAction() {
        val actions = listOf(LiveUpdateAction.BOARDING_PASS)
        assertEquals(listOf(LiveUpdateAction.BOARDING_PASS to "pass"), ActionFilter.withTargets(actions) { "pass" })
        assertEquals(emptyList<Pair<LiveUpdateAction, String>>(), ActionFilter.withTargets(actions) { null })
    }
}
