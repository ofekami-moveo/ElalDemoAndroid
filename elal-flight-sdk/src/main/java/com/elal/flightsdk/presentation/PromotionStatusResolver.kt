package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.PromotionStatus

/** Maps the system's answers to one [PromotionStatus]; pure so the order of checks is tested. */
internal object PromotionStatusResolver {

    private const val MIN_API = 36

    /** The first reason that blocks promotion, in the order a user would have to fix them, else ALLOWED. */
    fun resolve(sdkInt: Int, notificationsEnabled: Boolean, canPostPromoted: Boolean): PromotionStatus = when {
        sdkInt < MIN_API -> PromotionStatus.UNSUPPORTED
        !notificationsEnabled -> PromotionStatus.NOTIFICATIONS_DISABLED
        !canPostPromoted -> PromotionStatus.BLOCKED_BY_USER
        else -> PromotionStatus.ALLOWED
    }
}
