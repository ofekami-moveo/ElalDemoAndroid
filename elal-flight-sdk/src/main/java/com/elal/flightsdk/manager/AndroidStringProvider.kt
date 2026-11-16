package com.elal.flightsdk.manager

import android.content.Context
import androidx.annotation.StringRes
import com.elal.flightsdk.R
import com.elal.flightsdk.presentation.StringKey
import com.elal.flightsdk.presentation.StringProvider

/** [StringProvider] backed by the SDK's EN/HE string resources. */
internal class AndroidStringProvider(private val context: Context) : StringProvider {

    /** Formats the string resource for [key] with [args]. */
    override fun get(key: StringKey, vararg args: Any): String = context.getString(resourceFor(key), *args)

    /** Maps each key to its `R.string`. */
    @StringRes
    private fun resourceFor(key: StringKey): Int = when (key) {
        StringKey.TITLE -> R.string.notif_title
        StringKey.SUBTEXT_ROUTE -> R.string.notif_subtext_route
        StringKey.CONTENT_LANDS_AT -> R.string.notif_content_lands_at
        StringKey.CONTENT_CONNECTION_ERROR -> R.string.notif_content_connection_error
        StringKey.CONTENT_GENERAL_ERROR -> R.string.notif_content_general_error
        StringKey.CONTENT_ARRIVAL_WELCOME -> R.string.notif_content_arrival_welcome
        StringKey.FLIGHT_TIME -> R.string.notif_flight_time
        StringKey.PUBLIC_GATE -> R.string.notif_public_gate
        StringKey.PUBLIC_DEPARTS -> R.string.notif_public_departs
        StringKey.PUBLIC_ARRIVES -> R.string.notif_public_arrives
        StringKey.CHANGE_DEPARTS -> R.string.notif_change_departs
        StringKey.CHANGE_VALUES -> R.string.notif_change_values
        StringKey.CHANGE_WAS -> R.string.notif_change_was
        StringKey.CHANGE_SPOKEN -> R.string.notif_change_spoken
        StringKey.STAGE_CHECK_IN -> R.string.notif_stage_check_in
        StringKey.STAGE_GATE_READY -> R.string.notif_stage_gate_ready
        StringKey.STAGE_BOARDING_PENDING -> R.string.notif_stage_boarding_pending
        StringKey.STAGE_BOARDING -> R.string.notif_stage_boarding
        StringKey.STAGE_LAST_CALL -> R.string.notif_stage_last_call
        StringKey.STAGE_DEPARTURE_PENDING -> R.string.notif_stage_departure_pending
        StringKey.STAGE_DEPARTED -> R.string.notif_stage_departed
        StringKey.STAGE_LANDING_SOON -> R.string.notif_stage_landing_soon
        StringKey.STAGE_LANDED -> R.string.notif_stage_landed
        StringKey.STAGE_BAGGAGE -> R.string.notif_stage_baggage
        StringKey.STAGE_COMPLETED -> R.string.notif_stage_completed
        StringKey.STAGE_CONNECTION_ERROR -> R.string.notif_stage_connection_error
        StringKey.STAGE_GENERAL_ERROR -> R.string.notif_stage_general_error
    }
}
