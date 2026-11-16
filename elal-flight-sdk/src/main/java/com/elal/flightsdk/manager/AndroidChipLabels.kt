package com.elal.flightsdk.manager

import android.content.Context
import com.elal.flightsdk.R
import com.elal.flightsdk.presentation.ChipLabels

/** [ChipLabels] backed by the SDK's EN/HE string resources. */
internal class AndroidChipLabels(private val context: Context) : ChipLabels {

    /** Formats the localized "Belt %s" label. */
    override fun belt(carousel: String): String = context.getString(R.string.notif_chip_belt, carousel)

    /** Formats the localized hours:minutes duration. */
    override fun durationHours(hours: Int, minutes: Int): String =
        context.getString(R.string.notif_chip_duration_hours, hours, minutes)

    /** Formats the localized minutes-only duration. */
    override fun durationMinutes(minutes: Int): String =
        context.getString(R.string.notif_chip_duration_minutes, minutes)

    override val offline: String get() = context.getString(R.string.notif_chip_connection_error)

    override val error: String get() = context.getString(R.string.notif_chip_general_error)
}
