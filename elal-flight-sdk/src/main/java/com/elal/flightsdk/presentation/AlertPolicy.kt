package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.PostedState
import com.elal.flightsdk.model.Stage

/** Decides whether a post alerts (sound / heads-up) or updates silently (`setOnlyAlertOnce(true)`). */
internal object AlertPolicy {

    /**
     * True when this post should alert: entering a stage marked `alertOnEntry`, or a flip of the delay flag.
     * Never on an in-flight tick. [previous] is `null` for the first post of a journey (or after a restore),
     * which counts as entering the stage.
     */
    fun shouldAlert(
        stage: Stage,
        alertOnEntry: Boolean,
        isDelayed: Boolean,
        previous: PostedState?,
        isSelfUpdate: Boolean,
    ): Boolean {
        if (isSelfUpdate) {
            return false
        }
        val enteredStage = previous == null || previous.stage != stage
        val delayFlipped = previous != null && previous.isDelayed != isDelayed
        return (enteredStage && alertOnEntry) || delayFlipped
    }
}
