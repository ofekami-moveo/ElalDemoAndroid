package com.elal.flightsdk.presentation

/**
 * Localized labels the status-bar pill needs. An interface so [ChipTextResolver] stays free of
 * `android.*` and can run in JVM tests with a fake.
 */
interface ChipLabels {

    /** "Belt 4" / "מסוע 4" for [carousel]. */
    fun belt(carousel: String): String

    /** Remaining flight time of an hour or more, e.g. "6:12h". */
    fun durationHours(hours: Int, minutes: Int): String

    /** Remaining flight time under an hour, e.g. "45m". */
    fun durationMinutes(minutes: Int): String

    /** Pill text while offline. */
    val offline: String

    /** Pill text on a general error. */
    val error: String
}
