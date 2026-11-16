package com.elal.flightsdk.presentation

/** [ChipLabels] from the real `strings.xml` of one locale (see [XmlStrings]). */
internal class XmlChipLabels(private val strings: XmlStrings) : ChipLabels {

    /** `notif_chip_belt`. */
    override fun belt(carousel: String): String = strings.format("notif_chip_belt", carousel)

    /** `notif_chip_duration_hours`. */
    override fun durationHours(hours: Int, minutes: Int): String =
        strings.format("notif_chip_duration_hours", hours, minutes)

    /** `notif_chip_duration_minutes`. */
    override fun durationMinutes(minutes: Int): String = strings.format("notif_chip_duration_minutes", minutes)

    override val offline: String get() = strings.pattern("notif_chip_connection_error")

    override val error: String get() = strings.pattern("notif_chip_general_error")
}
