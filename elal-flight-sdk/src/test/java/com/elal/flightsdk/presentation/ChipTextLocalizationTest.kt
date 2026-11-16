package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.AppLanguage
import com.elal.flightsdk.model.Carrier
import com.elal.flightsdk.model.Direction
import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** The status-bar pill with the shipped EN and HE strings (task 2.1). */
class ChipTextLocalizationTest {

    private val locales = mapOf(
        "EN" to ChipTextResolver(XmlChipLabels(XmlStrings("values"))),
        "HE" to ChipTextResolver(XmlChipLabels(XmlStrings("values-iw"))),
    )

    /** Every stage that shows text stays within 7 visible characters in both languages, even on a long flight. */
    @Test
    fun everyStageFitsThePillInEnAndHe() {
        val longFlightMs = (11 * 60 + 52) * 60_000L
        locales.forEach { (locale, resolver) ->
            Stage.entries.filter { it != Stage.COMPLETED }.forEach { stage ->
                val text = resolver.resolve(state(stage, gate = "D3", counters = "D78-99", carousel = "12"), longFlightMs)
                assertTrue("$locale $stage → $text", text != null && ChipTextResolver.visibleLength(text) <= 7)
            }
        }
    }

    /** Gate stages show the bare gate (Figma), and nothing when the gate is missing or too long; belt falls back to the bare number. */
    @Test
    fun gateAndBeltFallbacks() {
        locales.forEach { (locale, resolver) ->
            assertEquals(locale, "D3", resolver.resolve(state(Stage.GATE_READY, gate = "D3"), null))
            assertNull(locale, resolver.resolve(state(Stage.GATE_READY, gate = "--"), null))
            assertNull(locale, resolver.resolve(state(Stage.GATE_READY, gate = "D123-456"), null))
            assertEquals(locale, "1234567", resolver.resolve(state(Stage.BAGGAGE, carousel = "1234567"), null))
        }
    }

    private fun state(stage: Stage, gate: String = "--", counters: String = "--", carousel: String = "--") =
        FlightContentState(
            flightNumber = "LY 317",
            direction = Direction.DEPARTURE,
            carrier = Carrier.EL_AL,
            origin = "TLV",
            destination = "JFK",
            departureTime = Instant.parse("2026-11-10T17:00:00Z"),
            arrivalTime = Instant.parse("2026-11-10T23:12:00Z"),
            gate = gate,
            checkInCounters = counters,
            baggageCarousel = carousel,
            stage = stage,
            language = AppLanguage.EN,
        )
}
