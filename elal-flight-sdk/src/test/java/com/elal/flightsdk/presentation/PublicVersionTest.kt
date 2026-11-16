package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.Stage
import com.elal.flightsdk.model.TimeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Lock-screen public version (task 2.10): flight, stage, gate and times, never the PNR. */
class PublicVersionTest {

    private val builder = LiveUpdateContentBuilder(FakeStringProvider(), FakeBidiWrapper(), FakeChipLabels())
    private val snapshot = TimeSnapshot(flightProgress = 0.5, remainingFlightMs = 3_600_000L)

    /** Gate + departure before takeoff, arrival after; no field of any stage contains the PNR. */
    @Test
    fun publicTextHasGateAndTimesButNoPnr() {
        assertEquals(
            "Gate [D3] · Departs [17:00]",
            builder.build(TestStates.state(Stage.GATE_READY), snapshot, false).publicText
        )
        assertEquals("Arrives [23:12]", builder.build(TestStates.state(Stage.DEPARTED), snapshot, false).publicText)
        Stage.entries.forEach { stage ->
            val content = builder.build(TestStates.state(stage), snapshot, false)
            listOf(content.title, content.text, content.subText, content.publicText, content.shortCriticalText.orEmpty())
                .forEach { assertFalse("$stage leaks the PNR: $it", it.contains(TestStates.PNR)) }
        }
    }
}
