package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.LiveUpdateConfig
import com.elal.flightsdk.model.Stage
import com.elal.flightsdk.model.StatusBadge
import com.elal.flightsdk.model.TimeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Spike: coloured status badge as the large icon. */
class StatusBadgeTest {

    private val builder = LiveUpdateContentBuilder(FakeStringProvider(), FakeBidiWrapper(), FakeChipLabels())
    private val snapshot = TimeSnapshot(flightProgress = 0.5, remainingFlightMs = null)
    private val badgeStages = setOf(
        Stage.GATE_READY, Stage.BOARDING_PENDING, Stage.BOARDING, Stage.LAST_CALL, Stage.DEPARTURE_PENDING,
        Stage.DEPARTED, Stage.LANDING_SOON,
    )

    /** ON TIME / DELAYED on GATE_READY → LANDING_SOON, by `isDelayed(state)`; none on the other stages. */
    @Test
    fun badgePerStageAndDelay() {
        Stage.entries.forEach { stage ->
            val onTime = builder.build(TestStates.state(stage), snapshot, false, showStatusBadge = true)
            val delayed = builder.build(
                TestStates.state(stage).copy(flightStatus = "DELAYED"), snapshot, false, showStatusBadge = true
            )
            if (stage in badgeStages) {
                assertEquals("$stage", StatusBadge.ON_TIME, onTime.statusBadge)
                assertEquals("$stage", StatusBadge.DELAYED, delayed.statusBadge)
            } else {
                assertNull("$stage", onTime.statusBadge)
                assertNull("$stage", delayed.statusBadge)
            }
        }
    }

    /** Flag off (the default): no badge on any stage, and the content is otherwise the same. */
    @Test
    fun flagOffGivesNoLargeIcon() {
        assertFalse(LiveUpdateConfig().showStatusBadge)
        Stage.entries.forEach { stage ->
            val off = builder.build(TestStates.state(stage), snapshot, false)
            val on = builder.build(TestStates.state(stage), snapshot, false, showStatusBadge = true)
            assertNull("$stage", off.statusBadge)
            assertEquals("$stage", on.copy(statusBadge = null), off)
        }
    }
}
