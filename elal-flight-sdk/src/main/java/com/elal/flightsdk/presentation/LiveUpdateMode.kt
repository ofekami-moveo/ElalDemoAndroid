package com.elal.flightsdk.presentation

/** How the journey is shown on this device. */
internal enum class LiveUpdateMode(val usesForegroundService: Boolean) {
    /** Android 16+: promoted Live Update; the in-flight service keeps DEPARTED ticking. */
    PROMOTED(usesForegroundService = true),

    /** Android 15 and older with the opt-in flag: a plain ongoing progress notification, never a foreground service. */
    FALLBACK(usesForegroundService = false),

    /** Android 15 and older without the flag: the SDK posts nothing. */
    OFF(usesForegroundService = false),
}
