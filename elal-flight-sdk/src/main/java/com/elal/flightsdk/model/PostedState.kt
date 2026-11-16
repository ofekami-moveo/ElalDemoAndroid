package com.elal.flightsdk.model

/**
 * What was last posted, kept by `LiveUpdateManager` so the next post knows whether it enters a new stage or
 * flips the delay (both alert) or only refreshes (silent).
 */
internal data class PostedState(val stage: Stage, val isDelayed: Boolean)
