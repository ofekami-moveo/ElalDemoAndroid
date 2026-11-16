package com.elal.flightsdk.presentation

/** English strings matching `res/values/strings.xml`, without Android. */
internal class FakeStringProvider : StringProvider {

    /** Formats the English pattern for [key] with [args], like `Context.getString(res, *args)`. */
    override fun get(key: StringKey, vararg args: Any): String = when (key) {
        StringKey.TITLE -> "%1\$s · %2\$s"
        StringKey.SUBTEXT_ROUTE -> "%1\$s → %2\$s"
        StringKey.CONTENT_LANDS_AT -> "Lands at %s"
        StringKey.CONTENT_CONNECTION_ERROR -> "You seem to be offline. Check your connection."
        StringKey.CONTENT_GENERAL_ERROR -> "Error has occurred"
        StringKey.CONTENT_ARRIVAL_WELCOME -> "Welcome to %s"
        StringKey.FLIGHT_TIME -> "%1\$dh:%2\$02dm"
        StringKey.PUBLIC_GATE -> "Gate %s"
        StringKey.PUBLIC_DEPARTS -> "Departs %s"
        StringKey.PUBLIC_ARRIVES -> "Arrives %s"
        StringKey.CHANGE_DEPARTS -> "Departs %s"
        StringKey.CHANGE_VALUES -> "%1\$s %2\$s"
        StringKey.CHANGE_WAS -> "%1\$s (was %2\$s)"
        StringKey.CHANGE_SPOKEN -> "%1\$s, was %2\$s"
        StringKey.STAGE_CHECK_IN -> "Check-in"
        StringKey.STAGE_GATE_READY -> "Gate ready"
        StringKey.STAGE_BOARDING_PENDING -> "Boarding soon"
        StringKey.STAGE_BOARDING -> "Boarding"
        StringKey.STAGE_LAST_CALL -> "Last call"
        StringKey.STAGE_DEPARTURE_PENDING -> "Departing soon"
        StringKey.STAGE_DEPARTED -> "On the way"
        StringKey.STAGE_LANDING_SOON -> "Landing soon"
        StringKey.STAGE_LANDED -> "Landed"
        StringKey.STAGE_BAGGAGE -> "Baggage claim"
        StringKey.STAGE_COMPLETED -> "Completed"
        StringKey.STAGE_CONNECTION_ERROR -> "Connection error"
        StringKey.STAGE_GENERAL_ERROR -> "Error"
    }.format(*args)
}
