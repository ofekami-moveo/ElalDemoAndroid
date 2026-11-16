package com.elal.flightsdk.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The badge fits whole inside the 16:9 large-icon frame (spike, after the device crop on 2026-11-16). */
class BadgeFitTest {

    /** A 52×17 badge in a 1600×900 frame: width-bound, centred, aspect kept, nothing outside the padding. */
    @Test
    fun wideBadgeIsCentredWholeInTheFrame() {
        val rect = BadgeFit.centered(frameWidth = 1600, frameHeight = 900, badgeWidth = 52, badgeHeight = 17, padding = 0.06f)
        assertEquals(FitRect(96, 220, 1504, 680), rect)
        assertEquals(52.0 / 17, (rect.right - rect.left).toDouble() / (rect.bottom - rect.top), 0.01)
        assertTrue(rect.left >= 96 && rect.right <= 1504 && rect.top >= 54 && rect.bottom <= 846)
    }
}
