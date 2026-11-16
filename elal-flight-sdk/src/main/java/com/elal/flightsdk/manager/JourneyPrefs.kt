package com.elal.flightsdk.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.GateRecord
import com.elal.flightsdk.utils.Logger
import kotlinx.serialization.json.Json

internal object JourneyPrefs {

    private const val PREFS_NAME = "journey_prefs"
    private const val KEY_STATE = "state"
    private const val KEY_CLOCK_SERVER_MS = "clock_server_ms"
    private const val KEY_CLOCK_REALTIME_MS = "clock_realtime_ms"
    private const val KEY_DEPARTED_ACTIVITY_ID = "departed_activity_id"
    private const val KEY_DEPARTED_START_SERVER_MS = "departed_start_server_ms"
    private const val KEY_DISMISSED_ACTIVITY_ID = "dismissed_activity_id"
    private const val KEY_JOURNEY_END_SERVER_MS = "journey_end_server_ms"
    private const val KEY_GATE_ACTIVITY_ID = "gate_activity_id"
    private const val KEY_PREVIOUS_GATE = "previous_gate"

    private val log = Logger("JourneyPrefs")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun saveState(context: Context, state: FlightContentState) {
        // commit() (synchronous) rather than apply(): an FCM update can be immediately followed by
        // the process being killed, and an async apply() write may not have flushed by then. The
        // critical FCM path runs off the main thread; the in-app debug path is on the main thread but
        // this is a single small value, so the blocking write is negligible.
        context.prefs().edit(commit = true) { putString(KEY_STATE, json.encodeToString(FlightContentState.serializer(), state)) }
    }

    fun loadState(context: Context): FlightContentState? {
        val stateJson = context.prefs().getString(KEY_STATE, null) ?: return null
        return try {
            json.decodeFromString<FlightContentState>(stateJson)
        } catch (e: Exception) {
            log.e("loadState: corrupt data, clearing — ${e.message}", e)
            context.prefs().edit { clear() }
            null
        }
    }

    /**
     * Persist [ServerClock]'s anchor pair. Written with commit() for the same reason saveState is:
     * the FCM push that produced it can be immediately followed by a process kill.
     */
    fun saveClockAnchor(context: Context, serverMs: Long, realtimeMs: Long) {
        context.prefs().edit(commit = true) {
            putLong(KEY_CLOCK_SERVER_MS, serverMs)
            putLong(KEY_CLOCK_REALTIME_MS, realtimeMs)
        }
    }

    /** The persisted (serverMs, elapsedRealtimeMs) pair, or null when no push has been anchored. */
    fun loadClockAnchor(context: Context): Pair<Long, Long>? {
        val prefs = context.prefs()
        val serverMs = prefs.getLong(KEY_CLOCK_SERVER_MS, 0L)
        if (serverMs <= 0L) return null
        return serverMs to prefs.getLong(KEY_CLOCK_REALTIME_MS, 0L)
    }

    /**
     * Persist the server time of the first DEPARTED push for [activityId], so a restore or a repeated
     * DEPARTED push continues the bar instead of restarting it (F1). commit() for the same reason as saveState.
     */
    fun saveDepartedStart(context: Context, activityId: String, startServerMs: Long) {
        context.prefs().edit(commit = true) {
            putString(KEY_DEPARTED_ACTIVITY_ID, activityId)
            putLong(KEY_DEPARTED_START_SERVER_MS, startServerMs)
        }
    }

    /** The persisted DEPARTED start for [activityId], or null when none was saved for this journey. */
    fun loadDepartedStart(context: Context, activityId: String): Long? {
        val prefs = context.prefs()
        if (prefs.getString(KEY_DEPARTED_ACTIVITY_ID, null) != activityId) {
            return null
        }
        return prefs.getLong(KEY_DEPARTED_START_SERVER_MS, 0L).takeIf { it > 0L }
    }

    /** Persist that the user swiped away [activityId], so no push, tick or restore reposts it. */
    fun saveDismissed(context: Context, activityId: String) {
        context.prefs().edit(commit = true) { putString(KEY_DISMISSED_ACTIVITY_ID, activityId) }
    }

    /** The dismissed journey's activityId, or null. Cleared with everything else on `end`. */
    fun loadDismissed(context: Context): String? = context.prefs().getString(KEY_DISMISSED_ACTIVITY_ID, null)

    /** Persist the journey's absolute end (server time), checked on restore. */
    fun saveJourneyEnd(context: Context, endServerMs: Long) {
        context.prefs().edit { putLong(KEY_JOURNEY_END_SERVER_MS, endServerMs) }
    }

    /** The persisted journey end, or null when none was saved. */
    fun loadJourneyEnd(context: Context): Long? =
        context.prefs().getLong(KEY_JOURNEY_END_SERVER_MS, 0L).takeIf { it > 0L }

    /** Persist the gate before the last gate change (task 3b.7), or remove it when [record] is null. */
    fun saveGateRecord(context: Context, record: GateRecord?) {
        context.prefs().edit {
            if (record == null) {
                remove(KEY_GATE_ACTIVITY_ID)
                remove(KEY_PREVIOUS_GATE)
            } else {
                putString(KEY_GATE_ACTIVITY_ID, record.activityId)
                putString(KEY_PREVIOUS_GATE, record.previousGate)
            }
        }
    }

    /** The persisted gate record, or null. */
    fun loadGateRecord(context: Context): GateRecord? {
        val prefs = context.prefs()
        val activityId = prefs.getString(KEY_GATE_ACTIVITY_ID, null) ?: return null
        val gate = prefs.getString(KEY_PREVIOUS_GATE, null) ?: return null
        return GateRecord(activityId, gate)
    }

    fun clear(context: Context) {
        context.prefs().edit { clear() }
    }

    private fun Context.prefs(): SharedPreferences =
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
