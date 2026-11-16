package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.AppLanguage
import com.elal.flightsdk.model.Carrier
import com.elal.flightsdk.model.Direction
import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** The arrival the DEPARTED bar and countdown run to, as `setupRealtimeClocks` calculated it before group 1. */
class FlightTimelineTest {

    /** Arrival = naive departure placed on GMT with its offset + flightDuration; no offset or duration → no arrival. */
    @Test
    fun arrivalIsDeparturePlusOffsetPlusDuration() {
        val timeline = FlightTimeline.from(state(offset = "+02", durationMinutes = 210))
        assertEquals(Instant.parse("2026-11-10T15:00:00Z"), timeline.departureUtc)
        assertEquals(Instant.parse("2026-11-10T18:30:00Z").toEpochMilli(), timeline.calculatedArrivalUtcMs)

        val noOffset = FlightTimeline.from(state(offset = null, durationMinutes = 210))
        assertNull(noOffset.departureUtc)
        assertNull(noOffset.calculatedArrivalUtcMs)
        assertEquals(210, noOffset.flightDurationMinutes)

        val noDuration = FlightTimeline.from(state(offset = "-04", durationMinutes = 0))
        assertEquals(Instant.parse("2026-11-10T21:00:00Z"), noDuration.departureUtc)
        assertNull(noDuration.flightDurationMinutes)
        assertNull(noDuration.calculatedArrivalUtcMs)
    }

    private fun state(offset: String?, durationMinutes: Int?) = FlightContentState(
        direction = Direction.DEPARTURE,
        carrier = Carrier.EL_AL,
        origin = "TLV",
        destination = "JFK",
        departureTime = Instant.parse("2026-11-10T17:00:00Z"),
        arrivalTime = Instant.parse("2026-11-10T23:12:00Z"),
        departureTimezoneOffset = offset,
        stage = Stage.DEPARTED,
        flightDuration = durationMinutes,
        language = AppLanguage.EN,
    )
}
