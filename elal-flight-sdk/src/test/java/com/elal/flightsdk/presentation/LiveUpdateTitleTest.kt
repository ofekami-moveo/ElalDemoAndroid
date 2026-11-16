package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.Stage
import com.elal.flightsdk.model.TimeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Title, route and DEPARTED text (task 2.4). `[x]` marks a bidi-isolated value. */
class LiveUpdateTitleTest {

    private val builder = LiveUpdateContentBuilder(FakeStringProvider(), FakeBidiWrapper(), FakeChipLabels())
    private val snapshot = TimeSnapshot(flightProgress = 0.0, remainingFlightMs = null)

    /** "flight · stage title", the SDK label when the server sends none, and never a time. */
    @Test
    fun titleIsFlightAndStageWithoutTime() {
        assertEquals(
            "[LY 317] · Boarding now",
            builder.build(TestStates.state(Stage.BOARDING, stageTitle = "Boarding now"), snapshot, false).title
        )
        assertEquals("[LY 317] · Gate ready", builder.build(TestStates.state(Stage.GATE_READY), snapshot, false).title)
        assertEquals(
            "[LY 317] · Landing soon",
            builder.build(TestStates.state(Stage.LANDING_SOON, stageTitle = "--"), snapshot, false).title
        )
        assertEquals(
            "[LY 317] · Connection error",
            builder.build(TestStates.state(Stage.CONNECTION_ERROR, stageTitle = "Boarding"), snapshot, false).title
        )
        Stage.entries.forEach { stage ->
            val title = builder.build(TestStates.state(stage), snapshot, false).title
            assertFalse("$stage title has a time: $title", title.contains("17:00") || title.contains("23:12"))
        }
    }

    /** DEPARTED shows the absolute landing time; every stage has the route as sub-text. */
    @Test
    fun departedLandsAtAndRouteSubText() {
        assertEquals("Lands at [23:12]", builder.build(TestStates.state(Stage.DEPARTED), snapshot, false).text)
        Stage.entries.forEach { stage ->
            assertEquals("[TLV] → [JFK]", builder.build(TestStates.state(stage), snapshot, false).subText)
        }
    }
}
