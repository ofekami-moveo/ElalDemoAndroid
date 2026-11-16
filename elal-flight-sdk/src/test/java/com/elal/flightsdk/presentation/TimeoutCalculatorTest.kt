package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** `setTimeoutAfter` safety net (task 4.5): `dismissalDate`, otherwise arrival + 4 h. */
class TimeoutCalculatorTest {

    private val now = Instant.parse("2026-11-10T16:00:00Z").toEpochMilli()

    /** `dismissalDate` wins; without it, the calculated arrival (15:00Z + 180 min = 18:00Z) + 4 h = 22:00Z. */
    @Test
    fun endFromDismissalDateOrArrivalPlusFourHours() {
        val withDismissal = TestStates.state(Stage.DEPARTED).copy(dismissalDate = Instant.parse("2026-11-10T20:30:00Z"))
        assertEquals(
            Instant.parse("2026-11-10T20:30:00Z").toEpochMilli(),
            TimeoutCalculator.journeyEndServerMs(withDismissal, serverNowMs = now, wallNowMs = 0L)
        )
        val end = TimeoutCalculator.journeyEndServerMs(TestStates.state(Stage.DEPARTED), serverNowMs = now, wallNowMs = 0L)
        assertEquals(Instant.parse("2026-11-10T22:00:00Z").toEpochMilli(), end)
        assertEquals(6 * HOUR, TimeoutCalculator.timeoutAfterMs(end, now))
    }

    /** Once the end has passed there is nothing to time out: the restore check ends the journey instead. */
    @Test
    fun pastEndGivesNoTimeout() {
        val end = Instant.parse("2026-11-10T15:59:00Z").toEpochMilli()
        assertNull(TimeoutCalculator.timeoutAfterMs(end, now))
        assertNull(TimeoutCalculator.timeoutAfterMs(now, now))
    }

    private companion object {
        const val HOUR = 3_600_000L
    }
}
