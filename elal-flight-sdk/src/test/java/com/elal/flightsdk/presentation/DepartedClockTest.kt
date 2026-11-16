package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** The in-flight clock behind the bar and the pill (task 2.5, fixes F1). */
class DepartedClockTest {

    /** 17:00 at +02 → 15:00Z; + 180 min → arrival 18:00Z. */
    private val t0 = Instant.parse("2026-11-10T15:00:00Z").toEpochMilli()

    /** A restore or a repeated DEPARTED push keeps the persisted start, so the plane doesn't jump back. */
    @Test
    fun progressContinuesAfterRestore() {
        val state = TestStates.state(Stage.DEPARTED)
        val first = DepartedClock.resolve(state, serverNowMs = t0, wallNowMs = 0L, persistedStartMs = null)
        assertEquals(t0, first.startServerMs)
        assertEquals(0.0, first.progressAt(t0), DELTA)

        // Process killed; restored one hour later with the persisted start.
        val restored = DepartedClock.resolve(state, serverNowMs = t0 + HOUR, wallNowMs = 0L, persistedStartMs = t0)
        assertEquals(1.0 / 3, restored.progressAt(t0 + HOUR), DELTA)
        assertEquals(2 * HOUR, restored.remainingAt(t0 + HOUR))

        // Another DEPARTED push two hours in.
        val repeated = DepartedClock.resolve(state, serverNowMs = t0 + 2 * HOUR, wallNowMs = 0L, persistedStartMs = t0)
        assertEquals(2.0 / 3, repeated.progressAt(t0 + 2 * HOUR), DELTA)
    }

    /** Arrival without an offset is start + duration; without a duration it is the server ETA; a passed arrival parks the plane. */
    @Test
    fun arrivalFallbacksAndPassedArrival() {
        val noOffset = TestStates.state(Stage.DEPARTED, offset = null)
        val clock = DepartedClock.resolve(noOffset, serverNowMs = t0 + HOUR, wallNowMs = 0L, persistedStartMs = t0)
        assertEquals(t0 + 3 * HOUR, clock.arrivalServerMs)

        val noDuration = TestStates.state(Stage.DEPARTED, durationMinutes = null)
        val eta = Instant.parse("2026-11-10T23:12:00Z").toEpochMilli()
        val etaClock = DepartedClock.resolve(noDuration, serverNowMs = t0, wallNowMs = eta - 2 * HOUR, persistedStartMs = null)
        assertEquals(t0 + 2 * HOUR, etaClock.arrivalServerMs)

        val passed = DepartedClock.resolve(TestStates.state(Stage.DEPARTED), t0 + 4 * HOUR, 0L, persistedStartMs = null)
        assertFalse(passed.hasDuration)
        assertEquals(1.0, passed.progressAt(t0 + 4 * HOUR), DELTA)
        assertNull(passed.remainingAt(t0 + 4 * HOUR))
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val DELTA = 0.0001
    }
}
