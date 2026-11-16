package com.elal.flightsdk.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.elal.flightsdk.R
import com.elal.flightsdk.analytics.AnalyticsPayloadBuilder
import com.elal.flightsdk.analytics.FlightAnalyticsListener
import com.elal.flightsdk.analytics.TriggerSource
import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.GateRecord
import com.elal.flightsdk.model.LiveUpdateAction
import com.elal.flightsdk.model.LiveUpdateConfig
import com.elal.flightsdk.model.PostedState
import com.elal.flightsdk.model.PromotionStatus
import com.elal.flightsdk.model.Stage
import com.elal.flightsdk.model.TimeSnapshot
import com.elal.flightsdk.presentation.DepartedClock
import com.elal.flightsdk.presentation.DismissPolicy
import com.elal.flightsdk.presentation.GateChangeTracker
import com.elal.flightsdk.presentation.FlightTimeline
import com.elal.flightsdk.presentation.LiveUpdateContentBuilder
import com.elal.flightsdk.presentation.LiveUpdateMode
import com.elal.flightsdk.presentation.LiveUpdateModeResolver
import com.elal.flightsdk.presentation.ProgressCalculator
import com.elal.flightsdk.presentation.TickIntervalCalculator
import com.elal.flightsdk.presentation.TimeoutCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.elal.flightsdk.utils.Logger
import com.elal.flightsdk.utils.orDash
import com.elal.flightsdk.utils.toHhMmSs
import com.elal.flightsdk.utils.toMsReadable
import com.elal.flightsdk.utils.wallClockNow
import java.time.Instant

/**
 * Manages the lifecycle of Live Update notifications.
 * Promoted Live Update on Android 16 (API 36)+. On older devices all public entry points are no-ops, unless the
 * host sets [LiveUpdateConfig.enableLegacyFallback]: then a plain ongoing progress notification (no promotion, no
 * foreground service) is shown.
 */
object LiveUpdateManager {

    internal const val NOTIFICATION_ID = 7001
    internal const val CHANNEL_ID = "elal_journey"
    private const val JOURNEY_END_SAVE_TOLERANCE_MS = 60_000L

    private val mode: LiveUpdateMode get() = LiveUpdateModeResolver.resolve(Build.VERSION.SDK_INT, config.enableLegacyFallback)
    private val isSupported: Boolean get() = mode != LiveUpdateMode.OFF
    private val log = Logger("LiveUpdateManager")

    // Safety net: if no server event arrives within this window, end the journey.
    // Protects against network outages that would otherwise leave the flow stuck.
    const val DEFAULT_WATCHDOG_MS: Long = 23L * 60L * 60L * 1000L
    const val DEBUG_WATCHDOG_MS: Long = 60L * 1000L

    var uiDebugMode: Boolean = false

    /**
     * Host-app sink for `live_activity_activated` and `live_activity_clicked` events.
     * Set once at SDK initialization (e.g. in Application.onCreate).
     */
    var analyticsListener: FlightAnalyticsListener? = null

    /**
     * Optional override for the Intent launched when the user taps the Live Activity.
     * Return null (or leave unset) to fall back to the host's launcher activity.
     */
    var clickIntentProvider: ((Context) -> Intent?)? = null

    /**
     * Where each Live Update action goes (boarding pass), asked when posting and again on tap.
     * Return null to hide that action. Unset = no actions.
     */
    var actionIntentProvider: ((Context, LiveUpdateAction, FlightContentState) -> Intent?)? = null

    private val _promotionStatus = MutableStateFlow(PromotionStatus.UNSUPPORTED)

    /** Whether the system will show the journey as a promoted Live Update; refreshed on start and stage change. */
    val promotionStatus: StateFlow<PromotionStatus> = _promotionStatus.asStateFlow()

    /** Host-tunable settings (in-flight refresh interval). Set once at SDK initialization. */
    @Volatile var config: LiveUpdateConfig = LiveUpdateConfig()

    private val _pillTextFlow = MutableStateFlow<String?>(null)

    /** The status-bar pill text last posted (`null` = icon only or no journey). For host diagnostics and the demo. */
    val pillTextFlow: StateFlow<String?> = _pillTextFlow.asStateFlow()

    private val _stateFlow = MutableStateFlow<FlightContentState?>(null)
    val stateFlow: StateFlow<FlightContentState?> = _stateFlow.asStateFlow()

    data class WatchdogState(val startElapsedRealtimeMs: Long, val durationMs: Long)

    private val _watchdogFlow = MutableStateFlow<WatchdogState?>(null)
    val watchdogFlow: StateFlow<WatchdogState?> = _watchdogFlow.asStateFlow()

    @Volatile private var watchdogDurationMs: Long = DEFAULT_WATCHDOG_MS

    // Shared mutable state is touched from both the FCM-delivery thread (start/update/end) and the
    // main-thread handler callbacks (self-update tick, watchdog). @Volatile guarantees visibility;
    // @Synchronized on the entry points + the tick runnable serializes mutation.
    @Volatile private var currentState: FlightContentState? = null
    val isActive: Boolean get() = currentState != null
    // In-flight interval on the server timeline; null outside DEPARTED. Its start is persisted per activityId.
    @Volatile private var departedClock: DepartedClock? = null
    // Stage and delay of the last post, so the next one alerts only on a new stage or a delay flip.
    @Volatile private var lastPosted: PostedState? = null
    // activityId the user swiped away (persisted); null when none. Cleared on end.
    @Volatile private var dismissedActivityId: String? = null
    // Journey end last written to prefs, to avoid a write on every tick.
    @Volatile private var savedJourneyEndMs: Long = 0L
    // Gate before the last gate change of the current journey (persisted), for "Gate ~~D2~~ D3".
    @Volatile private var gateRecord: GateRecord? = null

    @Volatile private var appContext: Context? = null
    private val handler = Handler(Looper.getMainLooper())

    private val watchdogRunnable = Runnable {
        log.i("⏰ WATCHDOG FIRED: ${watchdogDurationMs / 1000}s elapsed without server events — ending journey")
        appContext?.let { endJourney(it) }
    }

    @Volatile private var selfUpdateState: FlightContentState? = null
    private val selfUpdateRunnable: Runnable = object : Runnable {
        // Same monitor as the @Synchronized entry points, so departedClock can't be read mid-update
        // while updateJourney rewrites it.
        override fun run() = synchronized(LiveUpdateManager) {
            val ctx = appContext ?: return@synchronized
            val state = selfUpdateState ?: return@synchronized
            if (!DismissPolicy.shouldPost(state.activityId, dismissedActivityId)) {
                stopSelfUpdate()
                return@synchronized
            }
            val nowMs = ServerClock.nowMs()
            val progress = departedClock?.progressAt(nowMs) ?: 1.0
            val remainingMs = departedClock?.remainingAt(nowMs) ?: 0L
            val nextTickMs = calculateNextTickInterval()
            log.d("✈️ FLIGHT PROGRESS: ${(progress * 100).toInt()}% Complete | Remaining: ${remainingMs.toMsReadable()} | Next update in ${nextTickMs / 1000}s")
            postNotification(ctx, state, isSelfUpdate = true)
            // Bar and countdown share one clock, so 100% progress means the text is at zero too.
            if (progress >= 1.0) {
                log.i("🏁 FLIGHT COMPLETE: Progress reached 100% — stopping updates")
                stopSelfUpdate()
                return@synchronized
            }
            handler.postDelayed(this, nextTickMs)
            Unit
        }
    }

    // TODO: for DEBUG only
    //var debugNextStageIntent: PendingIntent? = null

    fun ensureChannel(context: Context) {
        if (!isSupported) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_description)
            }
            nm.createNotificationChannel(channel)
        }
    }

    @Synchronized
    fun startJourney(
        context: Context,
        state: FlightContentState,
        triggerSource: String = TriggerSource.PUSH,
        serverNowMs: Long = 0L
    ): EmptyResult<LiveUpdateError> {
        if (!isSupported) return Result.Success(Unit)
        if (!androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled())
            return Result.Failure(LiveUpdateError.NotificationsNotEnabled())
        if (isActive) return Result.Failure(LiveUpdateError.ActivityAlreadyRunning())
        log.d("startJourney: flight=${state.flightNumber} stage=${state.stage} trigger=$triggerSource")
        ServerClock.anchor(context, serverNowMs)
        appContext = context.applicationContext
        ensureChannel(context)
        currentState = state
        _stateFlow.value = state
        // Persist on start (not just on update) so a process kill before the first update still
        // leaves a state for restoreFromPrefs to rebuild — otherwise the next incremental FCM has
        // no fallback and fails to parse its missing required fields.
        JourneyPrefs.saveState(context, state)
        departedClock = null
        lastPosted = null
        gateRecord = null
        dismissedActivityId = JourneyPrefs.loadDismissed(context)
        resetWatchdog()
        logPromotionStatus(context, state)
        postNotification(context, state)
        fireActivated(state, triggerSource)
        return Result.Success(Unit)
    }

    private fun fireActivated(state: FlightContentState, triggerSource: String) {
        val listener = analyticsListener ?: return
        runCatching { listener.onLiveActivityActivated(AnalyticsPayloadBuilder.activated(state, triggerSource)) }
            .onFailure { log.e("analytics listener threw on activated: ${it.message}") }
    }

    @Synchronized
    fun updateJourney(
        context: Context,
        state: FlightContentState,
        serverNowMs: Long = 0L
    ): EmptyResult<LiveUpdateError> {
        if (!isSupported) return Result.Success(Unit)
        if (currentState == null) return Result.Failure(LiveUpdateError.NoActiveActivity())
        log.d("updateJourney: stage=${state.stage}")
        val previousStage = currentState?.stage
        val updatedGateRecord = GateChangeTracker.update(gateRecord, state.activityId, currentState?.gate, state.gate)
        if (updatedGateRecord != gateRecord) {
            gateRecord = updatedGateRecord
            JourneyPrefs.saveGateRecord(context, updatedGateRecord)
        }

        // Before setupRealtimeClocks below reads it — every DEPARTED push re-anchors "now" to the
        // backend's stamp, so the countdown never inherits a wrong handset clock.
        ServerClock.anchor(context, serverNowMs)
        currentState = state
        JourneyPrefs.saveState(context, state)
        resetWatchdog()

        val serviceIntent = android.content.Intent(context, InFlightService::class.java)

        if (state.stage == Stage.COMPLETED) {
            endJourney(context)  // emits null to stateFlow
            return Result.Success(Unit)
        }

        _stateFlow.value = state

        // The user swiped this journey away: track it (so `end` still clears it) but never post or tick again.
        if (!DismissPolicy.shouldPost(state.activityId, dismissedActivityId)) {
            log.i("updateJourney: activityId=${state.activityId} was dismissed by the user — not reposting")
            resetFlightTimers()
            context.stopService(serviceIntent)
            return Result.Success(Unit)
        }
        if (previousStage != state.stage) {
            logPromotionStatus(context, state)
        }

        when (state.stage) {
            Stage.DEPARTED -> {
                if (selfUpdateState == null) {
                    val hasDurationToAnimate = setupRealtimeClocks(context, state)
                    if (hasDurationToAnimate) startSelfUpdate(state) else selfUpdateState = state
                    ensureInFlightService(context)
                } else {
                    selfUpdateState = state
                    setupRealtimeClocks(context, state)
                }
            }
            Stage.LANDING_SOON, Stage.LANDED, Stage.BAGGAGE -> {
                resetFlightTimers()
            }
            else -> {
                resetFlightTimers()
                context.stopService(serviceIntent)
            }
        }

        postNotification(context, state)
        return Result.Success(Unit)
    }

    @Synchronized
    fun endJourney(context: Context) {
        if (!isSupported) return
        log.i("endJourney")
        resetFlightTimers()
        stopWatchdog()
        context.stopService(android.content.Intent(context, InFlightService::class.java))
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        JourneyPrefs.clear(context)
        ServerClock.reset()
        currentState = null
        _stateFlow.value = null
        _pillTextFlow.value = null
        lastPosted = null
        dismissedActivityId = null
        savedJourneyEndMs = 0L
        gateRecord = null
        appContext = null
    }

    /**
     * Restart the watchdog. Called on every server event so the journey is killed only
     * after [watchdogDurationMs] of silence — protecting against stuck flows when network
     * outages prevent the server's final "end" event from reaching the device.
     */
    private fun resetWatchdog() {
        handler.removeCallbacks(watchdogRunnable)
        val start = android.os.SystemClock.elapsedRealtime()
        handler.postDelayed(watchdogRunnable, watchdogDurationMs)
        _watchdogFlow.value = WatchdogState(start, watchdogDurationMs)
        log.i("⏰ WATCHDOG RESET: will fire in ${watchdogDurationMs / 1000}s")
    }

    private fun stopWatchdog() {
        handler.removeCallbacks(watchdogRunnable)
        _watchdogFlow.value = null
    }

    /** Change the watchdog duration. If a watchdog is currently active, restart it with the new value. */
    @Synchronized
    fun setWatchdogDuration(durationMs: Long) {
        watchdogDurationMs = durationMs
        if (_watchdogFlow.value != null) resetWatchdog()
    }

    private fun startSelfUpdate(state: FlightContentState) {
        selfUpdateState = state
        handler.removeCallbacks(selfUpdateRunnable)
        val tickMs = calculateNextTickInterval()
        log.i(" SELF-UPDATE STARTED: Airplane will move every ${tickMs / 1000}s")
        handler.postDelayed(selfUpdateRunnable, tickMs)
    }

    private fun stopSelfUpdate() {
        handler.removeCallbacks(selfUpdateRunnable)
        selfUpdateState = null
        log.i("🛑 SELF-UPDATE NOT ACTIVE")
    }

    fun pauseTicks() {
        handler.removeCallbacks(selfUpdateRunnable)
    }

    fun resumeTicks() {
        handler.removeCallbacks(selfUpdateRunnable)
        handler.post(selfUpdateRunnable)
    }

    private fun buildNotification( // android's native notification object
        context: Context,
        state: FlightContentState,
        isSelfUpdate: Boolean = false
    ): android.app.Notification {
        val content = LiveUpdateContentBuilder(AndroidStringProvider(context), AndroidBidiWrapper(), AndroidChipLabels(context))
            .build(
                state,
                currentTimeSnapshot(),
                isSelfUpdate,
                lastPosted,
                promotionAllowed = mode == LiveUpdateMode.PROMOTED,
                previousGate = gateRecord?.takeIf { it.activityId == state.activityId }?.previousGate,
                scheduleChangeStyle = config.scheduleChangeStyle,
                showStatusBadge = config.showStatusBadge,
            )
        lastPosted = content.postedState
        if (uiDebugMode && state.stage == Stage.DEPARTED) {
            log.d("[AIRPLANE] progressBar=${content.progress.value} / ${ProgressCalculator.PROGRESS_MAX}")
        }
        _pillTextFlow.value = content.shortCriticalText
        // TODO: for DEBUG only
        //debugNextStageIntent?.let { builder.addAction(0, "Next Stage", it) }
        return LiveUpdateNotificationMapper(context).map(content, state, timeoutAfterMs(context, state))
    }

    /**
     * `setTimeoutAfter` for this post: until `dismissalDate`, else arrival + 4 h (safety net if `end` never
     * arrives). The absolute end is persisted for the restore check. `null` once the end has passed.
     */
    private fun timeoutAfterMs(context: Context, state: FlightContentState): Long? {
        val nowMs = ServerClock.nowMs()
        val endMs = TimeoutCalculator.journeyEndServerMs(state, nowMs, wallClockNow().toEpochMilli())
        if (kotlin.math.abs(endMs - savedJourneyEndMs) > JOURNEY_END_SAVE_TOLERANCE_MS) {
            JourneyPrefs.saveJourneyEnd(context, endMs)
            savedJourneyEndMs = endMs
        }
        return TimeoutCalculator.timeoutAfterMs(endMs, nowMs)
    }

    /** Refreshes [promotionStatus] and logs it, so a missing pill can be explained from logcat. */
    private fun logPromotionStatus(context: Context, state: FlightContentState) {
        val status = PromotionDiagnostics.read(context)
        _promotionStatus.value = status
        log.i("promotionStatus=$status (stage=${state.stage})")
    }

    /** Re-reads [promotionStatus], e.g. when the host screen resumes after the user visited settings. */
    fun refreshPromotionStatus(context: Context): PromotionStatus {
        val status = PromotionDiagnostics.read(context)
        _promotionStatus.value = status
        return status
    }

    /** Opens the system setting that allows Live Updates for this app (falls back to its notification settings). */
    fun openPromotionSettings(context: Context) {
        runCatching { context.startActivity(PromotionDiagnostics.settingsIntent(context)) }
            .onFailure { log.e("openPromotionSettings failed: ${it.message}") }
    }

    /**
     * Called by the SDK's delete-intent receiver when the user swipes the Live Update away. The journey stays
     * tracked (updates and `end` are still handled) but is never posted or ticked again; a new activityId posts.
     */
    @Synchronized
    fun onUserDismissed(context: Context, activityId: String) {
        if (!isSupported) return
        if (!isActive) {
            restoreFromPrefs(context, startServiceIfActive = false)
        }
        val state = currentState
        if (state == null || state.activityId != activityId) {
            log.i("onUserDismissed: activityId=$activityId is not the current journey — ignored")
            return
        }
        log.i("onUserDismissed: activityId=$activityId — no more reposts for this journey")
        JourneyPrefs.saveDismissed(context, activityId)
        dismissedActivityId = activityId
        resetFlightTimers()
        context.stopService(android.content.Intent(context, InFlightService::class.java))
        _pillTextFlow.value = null
        val listener = analyticsListener ?: return
        runCatching { listener.onLiveActivityDismissed(AnalyticsPayloadBuilder.dismissed(state)) }
            .onFailure { log.e("analytics listener threw on dismissed: ${it.message}") }
    }

    /**
     * Demo / QA helper: removes the Live Update and runs the same path as a user swipe, for devices or test
     * runs where swiping isn't practical.
     */
    fun simulateUserDismiss(context: Context) {
        val activityId = currentState?.activityId ?: return
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        onUserDismissed(context, activityId)
    }

    /** True when the current journey was swiped away; [InFlightService] uses it to avoid reposting. */
    internal fun isCurrentJourneyDismissed(context: Context): Boolean {
        val activityId = currentState?.activityId ?: return false
        val dismissed = dismissedActivityId ?: JourneyPrefs.loadDismissed(context)?.also { dismissedActivityId = it }
        return !DismissPolicy.shouldPost(activityId, dismissed)
    }

    /**
     * Reads the in-flight clock once per build, so the bar and the pill share one value.
     * `remainingFlightMs` is null when no DEPARTED clock is running; progress is 1.0 then, as before.
     */
    private fun currentTimeSnapshot(): TimeSnapshot {
        val clock = departedClock
        val nowMs = ServerClock.nowMs()
        return TimeSnapshot(
            flightProgress = clock?.progressAt(nowMs) ?: 1.0,
            remainingFlightMs = clock?.remainingAt(nowMs),
        )
    }

    private fun postNotification(
        context: Context,
        state: FlightContentState,
        isSelfUpdate: Boolean = false
    ) {
        if (!DismissPolicy.shouldPost(state.activityId, dismissedActivityId)) {
            log.i("postNotification: activityId=${state.activityId} was dismissed — skipped")
            return
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(context, state, isSelfUpdate))
    }

    fun buildCurrentNotification(context: Context): android.app.Notification? {
        val state = selfUpdateState ?: currentState ?: return null
        return buildNotification(context, state, isSelfUpdate = selfUpdateState != null)
    }

    internal fun buildGeneralErrorNotification(context: Context): android.app.Notification {
        val errorState = currentState?.copy(stage = Stage.GENERAL_ERROR)
        if (errorState != null) {
            log.e("buildGeneralErrorNotification: active journey — transitioning to GENERAL_ERROR (flight=${errorState.flightNumber})")
            _stateFlow.value = errorState
            return buildNotification(context, errorState)
        }
        // No active state — bare notification to satisfy the Android foreground service contract
        log.e("buildGeneralErrorNotification: no active state, showing bare error notification")
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chip_error)
            .setContentText(context.getString(R.string.notif_content_general_error))
            .build()
    }

    @Synchronized
    fun restoreFromPrefs(context: Context, startServiceIfActive: Boolean = true) {
        if (!isSupported) return
        val state = JourneyPrefs.loadState(context) ?: return
        // elapsedRealtime is measured from boot, so the persisted anchor is still projectable after a
        // process kill — the countdown resumes on server time rather than dropping to the handset's.
        ServerClock.restore(context)
        // Past the journey's end (dismissalDate, else arrival + 4 h): the safety net would have removed it.
        val endMs = JourneyPrefs.loadJourneyEnd(context)
        if (endMs != null && ServerClock.nowMs() > endMs) {
            log.i("restoreFromPrefs: journey ended at $endMs — clearing instead of restoring")
            currentState = state
            endJourney(context)
            return
        }
        currentState = state
        _stateFlow.value = state
        appContext = context.applicationContext
        dismissedActivityId = JourneyPrefs.loadDismissed(context)
        savedJourneyEndMs = endMs ?: 0L
        gateRecord = JourneyPrefs.loadGateRecord(context)
        resetWatchdog()
        if (!DismissPolicy.shouldPost(state.activityId, dismissedActivityId)) {
            log.i("restoreFromPrefs: activityId=${state.activityId} was dismissed — restored without ticks")
            return
        }
        when (state.stage) {
            Stage.DEPARTED -> {
                if (setupRealtimeClocks(context, state)) startSelfUpdate(state) else selfUpdateState = state
                if (startServiceIfActive) ensureInFlightService(context)
            }
            else -> Unit
        }
    }

    /**
     * Start [InFlightService] as a foreground service, tolerating the Android 12+ background-start
     * restriction. When the app is in the background without an active exemption (e.g. a
     * normal-priority FCM), startForegroundService() throws ForegroundServiceStartNotAllowedException.
     * Left unguarded this crashed the FCM handler, and crashed Application.onCreate during
     * restoreFromPrefs — so a persisted DEPARTED state bricked FCM delivery in a crash loop
     * ("crashed too many times, killing"). The Live Update notification is posted separately via
     * NotificationManager and still updates; we only forgo the process-keep-alive service until the
     * app is next in the foreground.
     */
    private fun ensureInFlightService(context: Context) {
        // The Android 15 fallback never runs a foreground service: ticks continue only while the process lives.
        if (!mode.usesForegroundService) {
            return
        }
        val intent = android.content.Intent(context, InFlightService::class.java)
        try {
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            log.e("startForegroundService denied (app likely backgrounded) — notification still updates, skipping FGS: ${e.message}")
        }
    }

    /**
     * Never compares the arrival time against now to decide whether the flight is over — the stage
     * comes from the server and the SDK renders what it was told.
     *
     * @return true when there is a positive interval for the bar to fill over, so callers can tick.
     * False only means there is nothing to animate (the arrival is not after the start); the airplane
     * then sits at full progress, since [DepartedClock.progressAt] returns 1.0.
     */
    private fun setupRealtimeClocks(context: Context, state: FlightContentState): Boolean {
        // Mirrors the iOS live activity: the arrival is recomputed from scratch on every DEPARTED push as
        //     departureTime + departureTimezoneOffset + flightDuration
        // resolved on the GMT timeline (see DepartedClock.resolve for the fallbacks). The start is the
        // first DEPARTED push of this activityId, persisted, so a restore or a repeated push continues
        // the bar instead of restarting it (F1).
        //
        // "Now" comes from [ServerClock], i.e. the backend's own stamp on the push projected forward
        // monotonically, so a handset with auto-time off and a hand-set hour still counts down right.
        val nowMs = ServerClock.nowMs()
        val timeline = FlightTimeline.from(state)
        val persistedStartMs = JourneyPrefs.loadDepartedStart(context, state.activityId)
        val clock = DepartedClock.resolve(state, nowMs, wallClockNow().toEpochMilli(), persistedStartMs)
        if (persistedStartMs == null) {
            JourneyPrefs.saveDepartedStart(context, state.activityId, clock.startServerMs)
        }
        departedClock = clock

        val source = when {
            timeline.calculatedArrivalUtcMs != null -> "calculated arrival (flightDuration=${timeline.flightDurationMinutes}min)"
            timeline.flightDurationMinutes != null -> "start + flightDuration — NO departureTimezoneOffset"
            else -> "server ETA − device now"
        }
        if (timeline.calculatedArrivalUtcMs == null && timeline.flightDurationMinutes != null) {
            log.e("departureTimezoneOffset missing/unparseable ('${state.departureTimezoneOffset}') — arrival = start + ${timeline.flightDurationMinutes}min, which can read later than iOS")
        }
        log.i(
            "\n📋 JOURNEY TIMELINE" +
                    "\n   Departure           : ${state.departureTime.toHhMmSs()} (${state.origin} local, offset ${state.departureTimezoneOffset.orDash()})" +
                    (timeline.departureUtc?.let { "\n   Departure (GMT)     : ${it.toHhMmSs()}" } ?: "") +
                    "\n   Arrival (used)      : ${Instant.ofEpochMilli(clock.arrivalServerMs).toHhMmSs()} GMT  ($source)" +
                    "\n   Arrival (server ETA): ${state.arrivalTime.toHhMmSs()}  (display only)" +
                    "\n   Start               : ${Instant.ofEpochMilli(clock.startServerMs).toHhMmSs()} GMT  (${if (persistedStartMs != null) "persisted" else "this push"})" +
                    "\n   Now (GMT)           : ${Instant.ofEpochMilli(nowMs).toHhMmSs()}  (${if (ServerClock.isAnchored) "server clock" else "device clock"})" +
                    "\n   ─────────────────────────────────────" +
                    "\n   Interval            : ${clock.totalMs.coerceAtLeast(0L).toMsReadable()}  ← bar and pill both run over this" +
                    "\n   ─────────────────────────────────────" +
                    "\n   Tick Interval       : ${calculateNextTickInterval() / 1000}s"
        )
        return clock.hasDuration
    }

    /** 1% of the flight, capped by [LiveUpdateConfig.progressRefreshIntervalMs] (clamped 1–15 min). */
    private fun calculateNextTickInterval(): Long =
        TickIntervalCalculator.intervalMs(departedClock?.totalMs ?: 0L, config.progressRefreshIntervalMs)

    private fun resetFlightTimers() {
        stopSelfUpdate()
        departedClock = null
    }
}
