package com.elal.flightsdk.presentation

/** English labels matching `res/values/strings.xml`, without Android. */
class FakeChipLabels : ChipLabels {

    /** Mirrors `notif_chip_belt`. */
    override fun belt(carousel: String): String = "Belt $carousel"

    /** Mirrors `notif_chip_duration_hours`. */
    override fun durationHours(hours: Int, minutes: Int): String = "%d:%02dh".format(hours, minutes)

    /** Mirrors `notif_chip_duration_minutes`. */
    override fun durationMinutes(minutes: Int): String = "${minutes}m"

    override val offline: String = "Offline"

    override val error: String = "Error"
}
