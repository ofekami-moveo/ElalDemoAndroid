package com.elal.flightsdk.presentation

/** [StringProvider] over the real `strings.xml` of one locale, for EN / HE checks without Android. */
internal class XmlStringProvider(private val strings: XmlStrings) : StringProvider {

    /** Formats the resource that [AndroidStringProvider] maps [key] to. */
    override fun get(key: StringKey, vararg args: Any): String = strings.format(resourceName(key), *args)

    /** Same key → resource mapping as `AndroidStringProvider`. */
    private fun resourceName(key: StringKey): String = when (key) {
        StringKey.TITLE -> "notif_title"
        StringKey.SUBTEXT_ROUTE -> "notif_subtext_route"
        StringKey.CONTENT_LANDS_AT -> "notif_content_lands_at"
        StringKey.CONTENT_CONNECTION_ERROR -> "notif_content_connection_error"
        StringKey.CONTENT_GENERAL_ERROR -> "notif_content_general_error"
        StringKey.CONTENT_ARRIVAL_WELCOME -> "notif_content_arrival_welcome"
        StringKey.FLIGHT_TIME -> "notif_flight_time"
        StringKey.PUBLIC_GATE -> "notif_public_gate"
        StringKey.PUBLIC_DEPARTS -> "notif_public_departs"
        StringKey.PUBLIC_ARRIVES -> "notif_public_arrives"
        StringKey.CHANGE_DEPARTS -> "notif_change_departs"
        StringKey.CHANGE_VALUES -> "notif_change_values"
        StringKey.CHANGE_WAS -> "notif_change_was"
        StringKey.CHANGE_SPOKEN -> "notif_change_spoken"
        StringKey.STAGE_CHECK_IN -> "notif_stage_check_in"
        StringKey.STAGE_GATE_READY -> "notif_stage_gate_ready"
        StringKey.STAGE_BOARDING_PENDING -> "notif_stage_boarding_pending"
        StringKey.STAGE_BOARDING -> "notif_stage_boarding"
        StringKey.STAGE_LAST_CALL -> "notif_stage_last_call"
        StringKey.STAGE_DEPARTURE_PENDING -> "notif_stage_departure_pending"
        StringKey.STAGE_DEPARTED -> "notif_stage_departed"
        StringKey.STAGE_LANDING_SOON -> "notif_stage_landing_soon"
        StringKey.STAGE_LANDED -> "notif_stage_landed"
        StringKey.STAGE_BAGGAGE -> "notif_stage_baggage"
        StringKey.STAGE_COMPLETED -> "notif_stage_completed"
        StringKey.STAGE_CONNECTION_ERROR -> "notif_stage_connection_error"
        StringKey.STAGE_GENERAL_ERROR -> "notif_stage_general_error"
    }
}
