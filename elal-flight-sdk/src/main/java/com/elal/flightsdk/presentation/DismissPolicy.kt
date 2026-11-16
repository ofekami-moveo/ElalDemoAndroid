package com.elal.flightsdk.presentation

/** Decides whether a journey may be posted after the user swiped a Live Update away. */
internal object DismissPolicy {

    /**
     * False when [activityId] is the journey the user dismissed: neither a push nor an in-flight tick may bring
     * it back. A different journey, or none dismissed (`end` clears it), posts normally.
     */
    fun shouldPost(activityId: String, dismissedActivityId: String?): Boolean =
        dismissedActivityId == null || activityId != dismissedActivityId
}
