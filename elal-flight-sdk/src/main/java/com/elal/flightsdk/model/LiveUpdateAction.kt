package com.elal.flightsdk.model

/**
 * A button on the Live Update. The host supplies where each one goes through
 * `LiveUpdateManager.actionIntentProvider`; an action the host returns `null` for is not shown.
 */
enum class LiveUpdateAction {
    /** Opens the boarding pass. (Directions was removed on 2026-11-16: not needed.) */
    BOARDING_PASS,
}
