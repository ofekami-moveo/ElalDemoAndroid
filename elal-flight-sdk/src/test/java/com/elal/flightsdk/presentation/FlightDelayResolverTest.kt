package com.elal.flightsdk.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The delay check that drives the amber pill (Figma ELAL 6944:19267). */
class FlightDelayResolverTest {

    /** "DELAYED" in any case, with surrounding spaces, counts as delayed. */
    @Test
    fun delayedInAnyCaseIsDelayed() {
        assertTrue(FlightDelayResolver.isDelayed("DELAYED"))
        assertTrue(FlightDelayResolver.isDelayed("delayed"))
        assertTrue(FlightDelayResolver.isDelayed("  Delayed "))
    }

    /** On time, the parser's "--" placeholder, blank and null are not delayed. */
    @Test
    fun otherStatusesAreNotDelayed() {
        assertFalse(FlightDelayResolver.isDelayed("ON-TIME"))
        assertFalse(FlightDelayResolver.isDelayed("--"))
        assertFalse(FlightDelayResolver.isDelayed(""))
        assertFalse(FlightDelayResolver.isDelayed(null))
    }
}
