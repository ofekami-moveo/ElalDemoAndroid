package com.elal.demoapp.ui.journey

import android.app.Application
import android.app.LocaleManager
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.LocaleList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elal.demoapp.DemoSampleLocalizer
import com.elal.demoapp.R
import com.elal.demoapp.debug.DebugNextStageReceiver // TODO: DEBUG - Remove before release
import com.elal.demoapp.ui.util.UiText
import com.elal.flightsdk.manager.LiveUpdateError
import com.elal.flightsdk.manager.LiveUpdateManager
import com.elal.flightsdk.manager.onFailure
import com.elal.flightsdk.manager.onSuccess
import com.elal.flightsdk.model.FlightContentState
import com.elal.flightsdk.model.LiveUpdateConfig
import com.elal.flightsdk.model.PromotionStatus
import com.elal.flightsdk.model.ScheduleChangeStyle
import com.elal.flightsdk.model.Stage
import com.elal.flightsdk.sample.SampleFlightData
import com.elal.flightsdk.utils.orDash
import com.elal.flightsdk.utils.toFormattedTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit

class JourneyViewModel(application: Application) : AndroidViewModel(application) {

    private val _statusText = MutableStateFlow<UiText>(UiText.StringResource(R.string.no_active_activity))
    val statusText: StateFlow<UiText> = _statusText.asStateFlow()

    private val _detailText = MutableStateFlow<UiText?>(null)
    val detailText: StateFlow<UiText?> = _detailText.asStateFlow()

    private val _isActivityActive = MutableStateFlow(false)
    val isActivityActive: StateFlow<Boolean> = _isActivityActive.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _errorMessage = MutableStateFlow<UiText?>(null)
    val errorMessage: StateFlow<UiText?> = _errorMessage.asStateFlow()

    val watchdogFlow: StateFlow<LiveUpdateManager.WatchdogState?> = LiveUpdateManager.watchdogFlow

    /** The status-bar pill text the SDK last posted, shown on the demo screen to compare with the device. */
    val pillTextFlow: StateFlow<String?> = LiveUpdateManager.pillTextFlow

    /** Whether the system will promote the Live Update (shown on the demo's status card). */
    val promotionStatus: StateFlow<PromotionStatus> = LiveUpdateManager.promotionStatus

    /** Re-reads the promotion status, e.g. after returning from the settings screen. */
    fun refreshPromotionStatus() {
        LiveUpdateManager.refreshPromotionStatus(getApplication())
    }

    /** Opens the system setting for Live Updates. */
    fun openPromotionSettings() {
        LiveUpdateManager.openPromotionSettings(getApplication())
    }

    private val _isLegacyFallback = MutableStateFlow(LiveUpdateManager.config.enableLegacyFallback)

    /** Demo toggle for the Android 15 fallback; no effect on Android 16+, which is always promoted. */
    val isLegacyFallback: StateFlow<Boolean> = _isLegacyFallback.asStateFlow()

    /** Flips [LiveUpdateConfig.enableLegacyFallback] (applies to the next start). */
    fun toggleLegacyFallback() {
        val enabled = !_isLegacyFallback.value
        LiveUpdateManager.config = LiveUpdateManager.config.copy(enableLegacyFallback = enabled)
        _isLegacyFallback.value = enabled
    }

    private val _isStatusBadge = MutableStateFlow(LiveUpdateManager.config.showStatusBadge)

    /** Demo toggle for the status-badge spike (large icon "ON TIME" / "DELAYED"). */
    val isStatusBadge: StateFlow<Boolean> = _isStatusBadge.asStateFlow()

    /** Flips [LiveUpdateConfig.showStatusBadge] and re-posts the showing stage, to compare on each device. */
    fun toggleStatusBadge() {
        val enabled = !_isStatusBadge.value
        LiveUpdateManager.config = LiveUpdateManager.config.copy(showStatusBadge = enabled)
        _isStatusBadge.value = enabled
        val current = LiveUpdateManager.stateFlow.value ?: return
        LiveUpdateManager.updateJourney(getApplication(), current)
            .onFailure { _errorMessage.value = it.toDisplayMessage() }
    }

    /** Simulates the user swiping the Live Update away: the SDK must not post this journey again. */
    fun dismissLiveUpdate() {
        LiveUpdateManager.simulateUserDismiss(getApplication())
    }

    private val _isDelayed = MutableStateFlow(false)
    /** Demo toggle: when true, every posted sample state carries `flightStatus = "DELAYED"`. */
    val isDelayed: StateFlow<Boolean> = _isDelayed.asStateFlow()

    private var currentStateIndex = 0
    private var activeSequence: List<FlightContentState> = SampleFlightData.journeySimulationSequence
    private var simulationJob: Job? = null

    init {
        viewModelScope.launch {
            LiveUpdateManager.stateFlow.collect { state ->
                if (state == null) {
                    _isActivityActive.value = false
                    _isSimulating.value = false
                    _statusText.value = UiText.StringResource(R.string.no_active_activity)
                    _detailText.value = null
                } else {
                    _isActivityActive.value = true
                    updateUI(state)
                }
            }
        }
    }

    fun enableDebugWatchdog() {
        LiveUpdateManager.setWatchdogDuration(LiveUpdateManager.DEBUG_WATCHDOG_MS)
    }

    // TODO: DEBUG - Remove before release (along with all DebugNextStageReceiver usages)
    private val isDebuggable: Boolean
        get() = (getApplication<Application>().applicationInfo.flags and
                ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /** "Start Activity": the JFK → TLV journey, stepped by hand with "Next Stage". */
    fun startActivity() {
        startSequence(SampleFlightData.arrivalSimulationSequence)
    }

    fun updateToNextState() {
        val sequence = activeSequence
        currentStateIndex = (currentStateIndex + 1).coerceAtMost(sequence.size - 1)
        val state = sequence[currentStateIndex]
        if (isDebuggable) DebugNextStageReceiver.syncIndex(currentStateIndex) // TODO: DEBUG - Remove before release
        LiveUpdateManager.updateJourney(getApplication(), state.prepared())
            .onSuccess {
                if (state.stage == Stage.COMPLETED) {
                    _isActivityActive.value = false
                }
                _errorMessage.value = null
            }
            .onFailure { _errorMessage.value = it.toDisplayMessage() }
    }

    fun endActivity() {
        if (isDebuggable) DebugNextStageReceiver.teardown() // TODO: DEBUG - Remove before release
        LiveUpdateManager.endJourney(getApplication())
        _isActivityActive.value = false
        _isSimulating.value = false
        simulationJob?.cancel()
        simulationJob = null
        _statusText.value = UiText.StringResource(R.string.no_active_activity)
        _detailText.value = null
        _errorMessage.value = null
    }

    /** "Simulate Journey from Tel Aviv": TLV → JFK, one stage every 5 s. */
    fun simulateJourney() {
        runSimulation(SampleFlightData.journeySimulationSequence)
    }

    /** "Simulate Journey from New York": JFK → TLV, one stage every 5 s. */
    fun simulateArrivalJourney() {
        runSimulation(SampleFlightData.arrivalSimulationSequence)
    }

    /**
     * Starts [sequence] at its first stage. A journey that is already showing (or a running simulation) is ended
     * first, as the production FcmService does for a `start` push, instead of failing with ActivityAlreadyRunning.
     * Also fixes the old bug where "from Tel Aviv" ran the New York data: the sequence is passed in, not read
     * back from a field that `startActivity` used to overwrite.
     */
    private fun startSequence(sequence: List<FlightContentState>): Boolean {
        simulationJob?.cancel()
        simulationJob = null
        _isSimulating.value = false
        if (LiveUpdateManager.isActive) {
            LiveUpdateManager.endJourney(getApplication())
        }
        activeSequence = sequence
        currentStateIndex = 0
        if (isDebuggable) DebugNextStageReceiver.setup(getApplication()) // TODO: DEBUG - Remove before release
        var started = false
        LiveUpdateManager.startJourney(getApplication(), sequence[0].prepared())
            .onSuccess {
                started = true
                _isActivityActive.value = true
                _errorMessage.value = null
            }
            .onFailure { _errorMessage.value = it.toDisplayMessage() }
        return started
    }

    /** Starts [sequence] and posts its next stage every 5 s until COMPLETED; tapping again restarts it. */
    private fun runSimulation(sequence: List<FlightContentState>) {
        if (!startSequence(sequence)) {
            return
        }
        _isSimulating.value = true
        simulationJob = viewModelScope.launch {
            for (i in 1 until sequence.size) {
                delay(5_000L)
                currentStateIndex = i
                val state = sequence[i]
                LiveUpdateManager.updateJourney(getApplication(), state.prepared())
                    .onFailure { _errorMessage.value = it.toDisplayMessage() }
                if (state.stage == Stage.COMPLETED) break
            }

            endActivity()
        }
    }

    fun setConnectionError() {
        val errorState = SampleFlightData.journeySimulationSequence[0].copy(stage = Stage.CONNECTION_ERROR)
        if (!_isActivityActive.value) {
            LiveUpdateManager.startJourney(getApplication(), errorState)
                .onSuccess {
                    _isActivityActive.value = true
                    _errorMessage.value = null
                }
                .onFailure { _errorMessage.value = it.toDisplayMessage() }
        } else {
            LiveUpdateManager.updateJourney(getApplication(), errorState)
                .onSuccess { _errorMessage.value = null }
                .onFailure { _errorMessage.value = it.toDisplayMessage() }
        }
    }

    fun setGeneralError() {
        val errorState = SampleFlightData.journeySimulationSequence[0].copy(stage = Stage.GENERAL_ERROR)
        if (!_isActivityActive.value) {
            LiveUpdateManager.startJourney(getApplication(), errorState)
                .onSuccess {
                    _isActivityActive.value = true
                    _errorMessage.value = null
                }
                .onFailure { _errorMessage.value = it.toDisplayMessage() }
        } else {
            LiveUpdateManager.updateJourney(getApplication(), errorState)
                .onSuccess { _errorMessage.value = null }
                .onFailure { _errorMessage.value = it.toDisplayMessage() }
        }
    }

    /**
     * Flips the demo delay flag and, if a journey is showing, re-posts the current stage so the pill
     * colour (amber vs blue) can be checked on the device straight away.
     */
    fun toggleDelay() {
        _isDelayed.value = !_isDelayed.value
        val current = LiveUpdateManager.stateFlow.value ?: return
        val onTimeStatus = activeSequence.getOrNull(currentStateIndex)?.flightStatus ?: current.flightStatus
        val updated = if (_isDelayed.value) {
            current.withDelay(true)
        } else {
            current.withDelay(false).copy(flightStatus = onTimeStatus)
        }
        LiveUpdateManager.updateJourney(getApplication(), updated)
            .onFailure { _errorMessage.value = it.toDisplayMessage() }
    }

    /**
     * A sample state as the demo posts it: server-style labels in the app's language (task 6.5) and the demo delay
     * flag applied.
     */
    private fun FlightContentState.prepared(): FlightContentState =
        DemoSampleLocalizer.localize(getApplication(), this).withDelay(_isDelayed.value)

    /**
     * With [delayed]: `flightStatus = DELAYED` and the schedule moved by [DEMO_DELAY_MINUTES] (task 3b: scheduled =
     * the sample time, estimated = +55 min, departure and arrival). Without: the schedule fields cleared.
     */
    private fun FlightContentState.withDelay(delayed: Boolean): FlightContentState = if (delayed) {
        val scheduledDeparture = scheduledDepartureTime ?: departureTime
        val scheduledArrival = scheduledArrivalTime ?: arrivalTime
        copy(
            flightStatus = DELAYED_STATUS,
            scheduledDepartureTime = scheduledDeparture,
            estimatedDepartureTime = scheduledDeparture.plus(DEMO_DELAY_MINUTES, ChronoUnit.MINUTES),
            scheduledArrivalTime = scheduledArrival,
            estimatedArrivalTime = scheduledArrival.plus(DEMO_DELAY_MINUTES, ChronoUnit.MINUTES),
        )
    } else {
        copy(
            scheduledDepartureTime = null,
            estimatedDepartureTime = null,
            scheduledArrivalTime = null,
            estimatedArrivalTime = null,
        )
    }

    /**
     * Demo gate change (task 3b.8): re-posts the showing stage with another gate, so the SDK shows "Gate ~~D3~~ E4".
     * Tapping again switches back to the sample gate.
     */
    fun simulateGateChange() {
        val current = LiveUpdateManager.stateFlow.value ?: return
        val sampleGate = activeSequence.getOrNull(currentStateIndex)?.gate ?: current.gate
        val nextGate = if (current.gate == DEMO_CHANGED_GATE) sampleGate else DEMO_CHANGED_GATE
        LiveUpdateManager.updateJourney(getApplication(), current.copy(gate = nextGate))
            .onFailure { _errorMessage.value = it.toDisplayMessage() }
    }

    private val _isPlainScheduleChange = MutableStateFlow(
        LiveUpdateManager.config.scheduleChangeStyle == ScheduleChangeStyle.PLAIN_TEXT
    )

    /** Demo toggle: struck-through change ("~~17:00~~ 17:55") or plain text ("17:55 (was 17:00)"). */
    val isPlainScheduleChange: StateFlow<Boolean> = _isPlainScheduleChange.asStateFlow()

    /** Flips [LiveUpdateConfig.scheduleChangeStyle] and re-posts the showing stage, to compare on each device. */
    fun toggleScheduleChangeStyle() {
        val plain = !_isPlainScheduleChange.value
        val style = if (plain) ScheduleChangeStyle.PLAIN_TEXT else ScheduleChangeStyle.SPAN
        LiveUpdateManager.config = LiveUpdateManager.config.copy(scheduleChangeStyle = style)
        _isPlainScheduleChange.value = plain
        val current = LiveUpdateManager.stateFlow.value ?: return
        LiveUpdateManager.updateJourney(getApplication(), current)
            .onFailure { _errorMessage.value = it.toDisplayMessage() }
    }

    /**
     * Demo language toggle (task 6.5): switches the app between English and Hebrew with the per-app language API,
     * so the SDK's own strings follow it as they will in the real app. The activity is recreated; the screen then
     * calls [onLanguageApplied] to re-post the current stage in the new language.
     */
    fun toggleLanguage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val localeManager = getApplication<Application>().getSystemService(LocaleManager::class.java)
        val next = if (DemoSampleLocalizer.isHebrew(getApplication())) LANGUAGE_TAG_EN else LANGUAGE_TAG_HE
        localeManager.applicationLocales = LocaleList.forLanguageTags(next)
    }

    // Language the screen last ran in (null until the first composition), so only a real switch re-posts.
    private var appliedHebrew: Boolean? = null

    /** Re-posts the showing stage with its labels in the current language after a switch (no alert: same stage). */
    fun onLanguageApplied() {
        val isHebrew = DemoSampleLocalizer.isHebrew(getApplication())
        val previous = appliedHebrew
        appliedHebrew = isHebrew
        if (previous == null || previous == isHebrew) {
            return
        }
        val current = LiveUpdateManager.stateFlow.value ?: return
        LiveUpdateManager.updateJourney(getApplication(), DemoSampleLocalizer.localize(getApplication(), current))
            .onFailure { _errorMessage.value = it.toDisplayMessage() }
    }

    private fun LiveUpdateError.toDisplayMessage(): UiText = when (this) {
        is LiveUpdateError.ActivityAlreadyRunning  -> UiText.StringResource(R.string.error_already_running)
        is LiveUpdateError.NoActiveActivity        -> UiText.StringResource(R.string.error_no_active)
        is LiveUpdateError.NotificationsNotEnabled -> UiText.StringResource(R.string.error_notifications_disabled)
        is LiveUpdateError.FailedToStart           -> UiText.StringResource(R.string.error_failed_to_start, cause.message ?: "")
    }

    private fun updateUI(state: FlightContentState) {
        _statusText.value = when (state.stage) {
            Stage.CHECK_IN         -> UiText.StringResource(R.string.status_check_in)
            Stage.GATE_READY       -> UiText.StringResource(R.string.status_gate_ready)
            Stage.BOARDING_PENDING -> UiText.StringResource(R.string.status_boarding_pending)
            Stage.BOARDING         -> UiText.StringResource(R.string.status_boarding)
            Stage.LAST_CALL        -> UiText.StringResource(R.string.status_last_call)
            Stage.DEPARTURE_PENDING -> UiText.StringResource(R.string.status_departure_pending)
            Stage.DEPARTED         -> UiText.StringResource(R.string.status_departed)
            Stage.LANDING_SOON     -> UiText.StringResource(R.string.status_landing_soon)
            Stage.LANDED           -> UiText.StringResource(R.string.status_landed)
            Stage.BAGGAGE          -> UiText.StringResource(R.string.status_baggage)
            Stage.COMPLETED        -> UiText.StringResource(R.string.status_completed)
            Stage.CONNECTION_ERROR -> UiText.StringResource(R.string.status_connection_error)
            Stage.GENERAL_ERROR    -> UiText.StringResource(R.string.status_general_error)
        }
        _detailText.value = when (state.stage) {
            Stage.CHECK_IN    -> UiText.StringResource(R.string.detail_check_in, state.checkInCounters.orDash())
            Stage.GATE_READY  -> UiText.StringResource(R.string.detail_gate_ready, state.gate.orDash())
            Stage.BOARDING_PENDING -> UiText.StringResource(R.string.detail_boarding_pending, state.gate.orDash())
            Stage.BOARDING    -> UiText.StringResource(R.string.detail_boarding, state.gate.orDash())
            Stage.LAST_CALL   -> UiText.StringResource(R.string.detail_last_call, state.gate.orDash())
            Stage.DEPARTURE_PENDING -> UiText.StringResource(R.string.detail_departure_pending, state.departureTime.toFormattedTime())
            Stage.DEPARTED    -> UiText.StringResource(R.string.detail_departed, state.arrivalTime.toFormattedTime())
            Stage.LANDING_SOON -> UiText.StringResource(R.string.detail_landing_soon, state.destination)
            Stage.LANDED      -> UiText.StringResource(R.string.detail_landed, state.destination)
            Stage.BAGGAGE     -> UiText.StringResource(R.string.detail_baggage, state.baggageCarousel.orDash())
            Stage.COMPLETED   -> null
            Stage.CONNECTION_ERROR -> UiText.StringResource(R.string.detail_connection_error)
            Stage.GENERAL_ERROR    -> UiText.StringResource(R.string.detail_general_error)
        }
    }

    private companion object {
        /** The value the SDK's FlightDelayResolver treats as delayed. */
        const val DELAYED_STATUS = "DELAYED"
        const val LANGUAGE_TAG_EN = "en"
        const val LANGUAGE_TAG_HE = "iw"
        /** Demo delay for the schedule-change fields (task 3b). */
        const val DEMO_DELAY_MINUTES = 55L
        /** The other gate "Simulate gate change" switches to. */
        const val DEMO_CHANGED_GATE = "E4"
    }
}
