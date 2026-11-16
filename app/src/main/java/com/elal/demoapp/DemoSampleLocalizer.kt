package com.elal.demoapp

import android.content.Context
import com.elal.flightsdk.model.AppLanguage
import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.Stage
import com.elal.flightsdk.utils.toFormattedTime

/**
 * Gives the SDK's sample journeys server-style labels in the demo app's language (task 6.5). The real server sends
 * them in the user's language; the SDK sample data has them in Hebrew only, which mixed languages in the
 * notification. Only the fields the samples set are replaced; the label texts live in the demo's `strings.xml`.
 */
object DemoSampleLocalizer {

    private const val HEBREW = "iw"
    private const val HEBREW_ALT = "he"

    /** True when the app currently runs in Hebrew. */
    fun isHebrew(context: Context): Boolean {
        val language = context.resources.configuration.locales[0].language
        return language == HEBREW || language == HEBREW_ALT
    }

    /** [state] with its sample labels and `language` in the app's current language. */
    fun localize(context: Context, state: FlightContentState): FlightContentState {
        val labelled = when (state.stage) {
            // The notification reads the check-in label from `title` (as the production server sends it).
            Stage.CHECK_IN -> context.getString(R.string.sample_desk_title).let { label ->
                state.copy(deskTitle = label, title = label)
            }
            Stage.GATE_READY -> state.copy(gateTitle = context.getString(R.string.sample_gate_title))
            Stage.BOARDING_PENDING -> state.copy(
                message = context.getString(R.string.sample_boarding_soon),
                gateTitle = context.getString(R.string.sample_gate_title),
            )
            Stage.BOARDING -> state.copy(title = context.getString(R.string.sample_zone_title))
            Stage.LAST_CALL -> state.copy(title = context.getString(R.string.sample_last_call_title))
            Stage.DEPARTURE_PENDING -> state.copy(
                stageTitle = context.getString(R.string.sample_departing_soon),
                message = context.getString(R.string.sample_estimated_flight_time),
            )
            Stage.DEPARTED -> state.copy(
                title = context.getString(R.string.sample_landing_at, state.arrivalTime.toFormattedTime())
            )
            Stage.LANDING_SOON -> state.copy(message = context.getString(R.string.sample_landing_soon))
            Stage.LANDED -> if (hasCarousel(state)) {
                state.copy(title = context.getString(R.string.sample_baggage_carousel))
            } else {
                state.copy(title = context.getString(R.string.sample_welcome_to, state.destination))
            }
            Stage.BAGGAGE -> state.copy(title = context.getString(R.string.sample_baggage_carousel))
            Stage.COMPLETED, Stage.CONNECTION_ERROR, Stage.GENERAL_ERROR -> state
        }
        return labelled.copy(language = if (isHebrew(context)) AppLanguage.HE else AppLanguage.EN)
    }

    /** The parser's placeholder for "no carousel yet" is `--`. */
    private fun hasCarousel(state: FlightContentState): Boolean =
        !state.baggageCarousel.isNullOrBlank() && state.baggageCarousel != "--"
}
