package com.elal.flightsdk.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * Smoke test for the SDK's JVM unit-test setup (task group 0).
 *
 * It exercises [toAbsoluteInstant], which is pure `java.time` code, so it proves the test task runs
 * without touching any `android.*` API.
 */
class ToAbsoluteInstantSmokeTest {

    /** A naive JFK-local 00:20 with offset -04 is 04:20 UTC on the real timeline. */
    @Test
    fun naiveOriginTimeWithNegativeOffsetResolvesToUtc() {
        val naive = Instant.parse("2026-11-10T00:20:00Z")

        val absolute = naive.toAbsoluteInstant("-04")

        assertEquals(Instant.parse("2026-11-10T04:20:00Z"), absolute)
    }

    /** A naive TLV-local 17:00 with offset +02:00 is 15:00 UTC. */
    @Test
    fun naiveOriginTimeWithPositiveOffsetResolvesToUtc() {
        val naive = Instant.parse("2026-11-10T17:00:00Z")

        val absolute = naive.toAbsoluteInstant("+02:00")

        assertEquals(Instant.parse("2026-11-10T15:00:00Z"), absolute)
    }

    /** Without a usable offset the time can't be placed on the timeline, so the result is null. */
    @Test
    fun missingOrInvalidOffsetReturnsNull() {
        val naive = Instant.parse("2026-11-10T17:00:00Z")

        assertNull(naive.toAbsoluteInstant(null))
        assertNull(naive.toAbsoluteInstant(""))
        assertNull(naive.toAbsoluteInstant("not-an-offset"))
    }
}
