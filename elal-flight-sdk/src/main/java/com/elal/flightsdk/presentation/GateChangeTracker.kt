package com.elal.flightsdk.presentation

import com.elal.flightsdk.model.GateRecord

/** Keeps the gate a journey had before its last gate change (task 3b.7). */
internal object GateChangeTracker {

    private const val MISSING = "--"

    /**
     * The record after an update from [oldGate] to [newGate] for [activityId]: a real change records the old gate;
     * a new journey starts clean; a change back to the recorded gate clears it; otherwise the record is kept.
     */
    fun update(record: GateRecord?, activityId: String, oldGate: String?, newGate: String?): GateRecord? {
        val current = record?.takeIf { it.activityId == activityId }
        val old = oldGate.presentOrNull()
        val new = newGate.presentOrNull()
        return when {
            // Back to the original gate: nothing to strike any more (checked first, it is also a change).
            current != null && new == current.previousGate -> null
            old != null && new != null && old != new -> GateRecord(activityId, old)
            else -> current
        }
    }

    /** Null for blank or the parser's "--". */
    private fun String?.presentOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() && it != MISSING }
}
