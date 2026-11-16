package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.PromotionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/** Promotion diagnostics (task 4.6): why a Live Update would not be promoted. */
class PromotionStatusResolverTest {

    /** Old OS first, then notifications off, then the user's Live Updates switch. */
    @Test
    fun statusInPriorityOrder() {
        assertEquals(PromotionStatus.UNSUPPORTED, PromotionStatusResolver.resolve(35, true, true))
        assertEquals(PromotionStatus.NOTIFICATIONS_DISABLED, PromotionStatusResolver.resolve(36, false, true))
        assertEquals(PromotionStatus.BLOCKED_BY_USER, PromotionStatusResolver.resolve(36, true, false))
        assertEquals(PromotionStatus.ALLOWED, PromotionStatusResolver.resolve(37, true, true))
    }
}
