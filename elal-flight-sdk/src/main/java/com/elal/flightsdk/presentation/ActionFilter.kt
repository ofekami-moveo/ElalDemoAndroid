package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.LiveUpdateAction

/** Keeps only the actions the host can open. Generic over the target so it runs without Android in tests. */
internal object ActionFilter {

    /** Pairs each action with its [target]; actions whose target is `null` are dropped, order kept. */
    fun <T : Any> withTargets(actions: List<LiveUpdateAction>, target: (LiveUpdateAction) -> T?): List<Pair<LiveUpdateAction, T>> =
        actions.mapNotNull { action -> target(action)?.let { action to it } }
}
