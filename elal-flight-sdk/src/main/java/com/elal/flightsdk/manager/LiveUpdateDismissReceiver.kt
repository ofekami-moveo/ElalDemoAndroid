package com.elal.flightsdk.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives the Live Update's delete intent (the user swiped it away or cleared all). Declared in the SDK manifest,
 * not exported, so it also works after the process died.
 */
class LiveUpdateDismissReceiver : BroadcastReceiver() {

    /** Records the dismissal for the journey the notification belonged to. */
    override fun onReceive(context: Context, intent: Intent) {
        val activityId = intent.getStringExtra(EXTRA_ACTIVITY_ID) ?: return
        LiveUpdateManager.onUserDismissed(context, activityId)
    }

    companion object {
        /** `activityId` of the dismissed journey. */
        const val EXTRA_ACTIVITY_ID = "com.elal.flightsdk.extra.ACTIVITY_ID"
    }
}
