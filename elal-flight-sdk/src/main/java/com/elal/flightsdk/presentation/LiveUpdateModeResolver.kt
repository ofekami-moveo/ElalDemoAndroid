package com.elal.flightsdk.presentation

/** Picks the [LiveUpdateMode] for an OS version and the host's opt-in flag. */
internal object LiveUpdateModeResolver {

    private const val MIN_PROMOTED_API = 36

    /** PROMOTED on Android 16+, FALLBACK below it only when [enableLegacyFallback] is set, else OFF. */
    fun resolve(sdkInt: Int, enableLegacyFallback: Boolean): LiveUpdateMode = when {
        sdkInt >= MIN_PROMOTED_API -> LiveUpdateMode.PROMOTED
        enableLegacyFallback -> LiveUpdateMode.FALLBACK
        else -> LiveUpdateMode.OFF
    }
}
