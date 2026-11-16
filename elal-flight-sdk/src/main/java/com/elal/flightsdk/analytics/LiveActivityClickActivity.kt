package com.elal.flightsdk.analytics

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.elal.flightsdk.manager.LiveUpdateManager
import com.elal.flightsdk.model.LiveUpdateAction
import com.elal.flightsdk.utils.Logger

/**
 * Invisible trampoline that runs when the user taps the Live Activity.
 *
 * Notification trampolines must be Activities on Android 12+ (broadcast/service
 * trampolines are blocked), so this Activity:
 *  1. Fires the `live_activity_clicked` analytics event via the host's listener.
 *  2. Launches the host app — either a customer-provided intent ([LiveUpdateManager.clickIntentProvider])
 *     or the package's default launcher activity.
 *  3. Finishes itself with no visible transition.
 *
 * The host app does not need to declare or reference this Activity; it is registered
 * in the SDK manifest and wired automatically by [LiveUpdateManager].
 */
class LiveActivityClickActivity : Activity() {

    private val log = Logger("LiveActivityClick")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)

        val source = intent?.getStringExtra(EXTRA_CLICK_SOURCE) ?: ClickSource.LOCK_SCREEN
        val action = intent?.getStringExtra(EXTRA_ACTION)
            ?.let { name -> LiveUpdateAction.entries.firstOrNull { it.name == name } }

        val state = LiveUpdateManager.stateFlow.value
        if (state != null) {
            runCatching {
                val listener = LiveUpdateManager.analyticsListener
                if (action != null) {
                    listener?.onLiveActivityActionClicked(AnalyticsPayloadBuilder.actionClicked(state, action))
                } else {
                    listener?.onLiveActivityClicked(AnalyticsPayloadBuilder.clicked(state, source))
                }
            }.onFailure { log.e("analytics listener threw on click: ${it.message}") }
        } else {
            log.i("click received with no active state — skipping analytics")
        }

        // An action opens the host's target for it (asked again now, as the state may have changed); a tap on
        // the notification, or an action whose target is gone, opens the host's click intent or launcher.
        val actionTarget = if (action != null && state != null) {
            LiveUpdateManager.actionIntentProvider?.invoke(this, action, state)
        } else {
            null
        }
        val launch = actionTarget
            ?: LiveUpdateManager.clickIntentProvider?.invoke(this)
            ?: packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            runCatching { startActivity(launch) }
                .onFailure { log.e("failed to launch host activity: ${it.message}") }
        } else {
            log.e("no launchable activity found for package=$packageName")
        }

        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_CLICK_SOURCE = "com.elal.flightsdk.extra.CLICK_SOURCE"

        /** Name of the [LiveUpdateAction] tapped; absent for a tap on the notification itself. */
        const val EXTRA_ACTION = "com.elal.flightsdk.extra.ACTION"
    }
}
