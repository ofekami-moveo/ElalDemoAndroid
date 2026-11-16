package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.LiveUpdateConfig
import com.elal.flightsdk.model.Stage
import com.elal.flightsdk.model.TimeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plain progress notification on Android 15 and older, behind a flag (task 5.1). */
class LegacyFallbackTest {

    private val builder = LiveUpdateContentBuilder(FakeStringProvider(), FakeBidiWrapper(), FakeChipLabels())
    private val snapshot = TimeSnapshot(flightProgress = 0.5, remainingFlightMs = 3_600_000L)

    /** The fallback shows exactly the same content; only the promotion request is off. */
    @Test
    fun fallbackContentEqualsPromotedApartFromPromotion() {
        Stage.entries.forEach { stage ->
            val state = TestStates.state(stage)
            val promoted = builder.build(state, snapshot, false, promotionAllowed = true)
            val fallback = builder.build(state, snapshot, false, promotionAllowed = false)
            assertFalse("$stage", fallback.requestPromoted)
            assertEquals("$stage", promoted.copy(requestPromoted = false), fallback)
        }
    }

    /** Off by default: nothing posts on Android 15 and older unless the host opts in. */
    @Test
    fun fallbackIsOffByDefault() {
        assertFalse(LiveUpdateConfig().enableLegacyFallback)
        assertEquals(LiveUpdateMode.OFF, LiveUpdateModeResolver.resolve(sdkInt = 35, enableLegacyFallback = false))
        assertEquals(LiveUpdateMode.OFF, LiveUpdateModeResolver.resolve(sdkInt = 26, enableLegacyFallback = false))
    }

    /** Android 16+ is always promoted; the fallback below it never asks for the foreground service. */
    @Test
    fun fallbackNeverUsesTheForegroundService() {
        assertEquals(LiveUpdateMode.FALLBACK, LiveUpdateModeResolver.resolve(sdkInt = 35, enableLegacyFallback = true))
        assertEquals(LiveUpdateMode.PROMOTED, LiveUpdateModeResolver.resolve(sdkInt = 36, enableLegacyFallback = false))
        assertEquals(LiveUpdateMode.PROMOTED, LiveUpdateModeResolver.resolve(sdkInt = 37, enableLegacyFallback = true))
        assertFalse(LiveUpdateMode.FALLBACK.usesForegroundService)
        assertFalse(LiveUpdateMode.OFF.usesForegroundService)
        assertTrue(LiveUpdateMode.PROMOTED.usesForegroundService)
    }

    /** A non-positive refresh interval is a host bug and is rejected; values out of 1–15 min are clamped later. */
    @Test(expected = IllegalArgumentException::class)
    fun configRejectsNonPositiveRefreshInterval() {
        LiveUpdateConfig(progressRefreshIntervalMs = 0L)
    }
}
