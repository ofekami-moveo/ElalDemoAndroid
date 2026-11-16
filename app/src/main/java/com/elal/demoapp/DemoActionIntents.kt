package com.elal.demoapp

import android.content.Context
import android.content.Intent
import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.LiveUpdateAction

/**
 * Demo target for the Live Update's Boarding pass button. The real host opens its boarding-pass screen; here it
 * reopens the demo with a placeholder message.
 */
object DemoActionIntents {

    /** Extra on [MainActivity]'s intent naming the placeholder to show. */
    const val EXTRA_PLACEHOLDER = "com.elal.demoapp.extra.PLACEHOLDER"

    /** The target for [action] in [state]; never null in the demo, so the button always shows. */
    @Suppress("UNUSED_PARAMETER")
    fun intentFor(context: Context, action: LiveUpdateAction, state: FlightContentState): Intent = when (action) {
        LiveUpdateAction.BOARDING_PASS -> Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_PLACEHOLDER, action.name)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
