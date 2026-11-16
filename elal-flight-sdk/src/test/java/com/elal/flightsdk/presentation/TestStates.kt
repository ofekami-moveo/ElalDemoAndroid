package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.AppLanguage
import com.elal.flightsdk.model.Carrier
import com.elal.flightsdk.model.Direction
import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.Stage
import java.time.Instant

/** Shared sample state for the group 2 tests: LY 317 TLV → JFK, 17:00 → 23:12, gate D3, PNR ABC123. */
internal object TestStates {

    const val PNR = "ABC123"

    /** A full state for [stage]; [stageTitle] empty means "server sent no title". */
    fun state(
        stage: Stage,
        stageTitle: String = "",
        offset: String? = "+02",
        durationMinutes: Int? = 180,
    ) = FlightContentState(
        flightNumber = "LY 317",
        direction = Direction.DEPARTURE,
        carrier = Carrier.EL_AL,
        pnr = PNR,
        origin = "TLV",
        destination = "JFK",
        departureTime = Instant.parse("2026-11-10T17:00:00Z"),
        arrivalTime = Instant.parse("2026-11-10T23:12:00Z"),
        departureTimezoneOffset = offset,
        gate = "D3",
        checkInCounters = "D78-99",
        baggageCarousel = "4",
        stage = stage,
        stageTitle = stageTitle,
        flightDuration = durationMinutes,
        language = AppLanguage.EN,
    )
}
