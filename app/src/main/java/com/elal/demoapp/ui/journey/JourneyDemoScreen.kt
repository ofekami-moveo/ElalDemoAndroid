package com.elal.demoapp.ui.journey

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elal.demoapp.R
import com.elal.demoapp.ui.theme.ElAlBlue
import com.elal.demoapp.ui.theme.ElAlDemoTheme
import com.elal.demoapp.ui.theme.Green
import com.elal.demoapp.ui.theme.Red
import com.elal.demoapp.ui.theme.Yellow
import com.elal.demoapp.ui.util.UiText
import com.elal.flightsdk.manager.LiveUpdateManager
import com.elal.flightsdk.model.PromotionStatus
import kotlinx.coroutines.delay

@Composable
fun JourneyDemoScreen(viewModel: JourneyViewModel = viewModel()) {
    val statusText by viewModel.statusText.collectAsState()
    val detailText by viewModel.detailText.collectAsState()
    val isActivityActive by viewModel.isActivityActive.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val watchdog by viewModel.watchdogFlow.collectAsState()
    val isDelayed by viewModel.isDelayed.collectAsState()
    val pillText by viewModel.pillTextFlow.collectAsState()
    val promotionStatus by viewModel.promotionStatus.collectAsState()
    val isLegacyFallback by viewModel.isLegacyFallback.collectAsState()
    val isPlainScheduleChange by viewModel.isPlainScheduleChange.collectAsState()
    val isStatusBadge by viewModel.isStatusBadge.collectAsState()
    // Read once when the screen opens; the SDK refreshes it again on every start and stage change.
    LaunchedEffect(Unit) { viewModel.refreshPromotionStatus() }
    // After a language switch the activity is recreated with the new locale: re-post the stage in that language.
    val locales = LocalConfiguration.current.locales
    LaunchedEffect(locales) { viewModel.onLanguageApplied() }

    JourneyDemoContent(
        statusText = statusText,
        detailText = detailText,
        isActivityActive = isActivityActive,
        isSimulating = isSimulating,
        errorMessage = errorMessage,
        watchdog = watchdog,
        onStartActivity = { viewModel.startActivity() },
        onNextStage = { viewModel.updateToNextState() },
        onEndActivity = { viewModel.endActivity() },
        onConnectionError = { viewModel.setConnectionError() },
        onGeneralError = { viewModel.setGeneralError() },
        onSimulateDeparture = { viewModel.simulateJourney() },
        onSimulateArrival = { viewModel.simulateArrivalJourney() },
        onDebugWatchdog = { viewModel.enableDebugWatchdog() },
        isDelayed = isDelayed,
        onToggleDelay = { viewModel.toggleDelay() },
        pillText = pillText,
        promotionStatus = promotionStatus,
        onDismiss = { viewModel.dismissLiveUpdate() },
        onOpenPromotionSettings = { viewModel.openPromotionSettings() },
        isLegacyFallback = isLegacyFallback,
        onToggleLegacyFallback = { viewModel.toggleLegacyFallback() },
        onToggleLanguage = { viewModel.toggleLanguage() },
        onSimulateGateChange = { viewModel.simulateGateChange() },
        isPlainScheduleChange = isPlainScheduleChange,
        onToggleScheduleChangeStyle = { viewModel.toggleScheduleChangeStyle() },
        isStatusBadge = isStatusBadge,
        onToggleStatusBadge = { viewModel.toggleStatusBadge() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyDemoContent(
    statusText: UiText,
    detailText: UiText?,
    isActivityActive: Boolean,
    isSimulating: Boolean,
    errorMessage: UiText?,
    watchdog: LiveUpdateManager.WatchdogState?,
    onStartActivity: () -> Unit,
    onNextStage: () -> Unit,
    onEndActivity: () -> Unit,
    onConnectionError: () -> Unit,
    onGeneralError: () -> Unit,
    onSimulateDeparture: () -> Unit,
    onSimulateArrival: () -> Unit,
    onDebugWatchdog: () -> Unit,
    isDelayed: Boolean = false,
    onToggleDelay: () -> Unit = {},
    pillText: String? = null,
    promotionStatus: PromotionStatus? = null,
    onDismiss: () -> Unit = {},
    onOpenPromotionSettings: () -> Unit = {},
    isLegacyFallback: Boolean = false,
    onToggleLegacyFallback: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    onSimulateGateChange: () -> Unit = {},
    isPlainScheduleChange: Boolean = false,
    onToggleScheduleChangeStyle: () -> Unit = {},
    isStatusBadge: Boolean = false,
    onToggleStatusBadge: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journey_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ElAlBlue,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // The group 4–6 controls made the screen taller than a phone; scroll so every button is reachable.
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Status Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElAlBlue)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.current_stage),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = statusText.asString(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                if (detailText != null) {
                    Text(
                        text = detailText.asString(),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
                // What the SDK put in the status-bar pill, to compare with the device
                if (isActivityActive) {
                    Text(
                        text = if (pillText != null) {
                            stringResource(R.string.demo_pill_text, pillText)
                        } else {
                            stringResource(R.string.demo_pill_icon_only)
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            // Control Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    // Always enabled (task 6.1): starting while a journey is active replaces it.
                    onClick = onStartActivity,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Text(stringResource(R.string.start_activity))
                }

                Button(
                    onClick = onNextStage,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isActivityActive,
                    colors = ButtonDefaults.buttonColors(containerColor = ElAlBlue)
                ) {
                    Text(stringResource(R.string.next_stage))
                }

                Button(
                    onClick = onEndActivity,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isActivityActive,
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    Text(stringResource(R.string.end_activity))
                }
            }

            // Error State Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onConnectionError,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    Text(stringResource(R.string.conn_error_button))
                }

                Button(
                    onClick = onGeneralError,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    Text(stringResource(R.string.error_button))
                }
            }

            // Delay toggle: switches the pill between on-time blue and delayed amber
            Button(
                onClick = onToggleDelay,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDelayed) Yellow else ElAlBlue,
                    contentColor = if (isDelayed) Color.Black else Color.White
                )
            ) {
                Text(stringResource(if (isDelayed) R.string.delay_toggle_on else R.string.delay_toggle_off))
            }

            // Schedule changes (task 3b): gate change and the strike style
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onSimulateGateChange, modifier = Modifier.weight(1f), enabled = isActivityActive) {
                    Text(stringResource(R.string.demo_gate_change))
                }
                OutlinedButton(onClick = onToggleScheduleChangeStyle, modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            if (isPlainScheduleChange) R.string.demo_strike_style_text else R.string.demo_strike_style_span
                        )
                    )
                }
            }

            // Spike: coloured status badge as the large icon
            OutlinedButton(onClick = onToggleStatusBadge, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (isStatusBadge) R.string.demo_status_badge_on else R.string.demo_status_badge_off))
            }

            // Simulates the user swiping the Live Update away (group 4: no repost afterwards)
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                enabled = isActivityActive,
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) {
                Text(stringResource(R.string.demo_dismiss))
            }

            if (promotionStatus != null) {
                PromotionCard(status = promotionStatus, onOpenSettings = onOpenPromotionSettings)
            }

            // Demo language (task 6.5): the SDK strings and the sample labels follow it
            OutlinedButton(onClick = onToggleLanguage, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.demo_language_switch))
            }

            // Android 15 fallback (group 5): applies to the next start; no effect on Android 16+
            OutlinedButton(onClick = onToggleLegacyFallback, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (isLegacyFallback) R.string.demo_fallback_on else R.string.demo_fallback_off))
            }

            // Watchdog countdown + debug button
            WatchdogCard(
                watchdog = watchdog,
                onDebugWatchdog = onDebugWatchdog
            )

            HorizontalDivider()

            // Simulate Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    // Always enabled (task 6.1): tapping again restarts the simulation.
                    onClick = onSimulateDeparture,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElAlBlue)
                ) {
                    Text(
                        text = stringResource(R.string.simulate_journey_departure),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    // Always enabled (task 6.1): tapping again restarts the simulation.
                    onClick = onSimulateArrival,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElAlBlue)
                ) {
                    Text(
                        text = stringResource(R.string.simulate_journey_arrival),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // CircularProgressIndicator — shown while simulation is running
            if (isSimulating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = ElAlBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.simulating), fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (errorMessage != null) {
                Text(
                    text = errorMessage.asString(),
                    color = Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

/** Shows whether the system will promote the Live Update, with a shortcut to the setting that controls it. */
@Composable
private fun PromotionCard(status: PromotionStatus, onOpenSettings: () -> Unit) {
    val label = when (status) {
        PromotionStatus.ALLOWED -> R.string.demo_promotion_allowed
        PromotionStatus.BLOCKED_BY_USER -> R.string.demo_promotion_blocked
        PromotionStatus.NOTIFICATIONS_DISABLED -> R.string.demo_promotion_notifications_disabled
        PromotionStatus.UNSUPPORTED -> R.string.demo_promotion_unsupported
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.demo_promotion_title), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                text = stringResource(label),
                fontSize = 14.sp,
                color = if (status == PromotionStatus.ALLOWED) Green else Red
            )
        }
        Button(onClick = onOpenSettings, colors = ButtonDefaults.buttonColors(containerColor = ElAlBlue)) {
            Text(stringResource(R.string.demo_open_settings))
        }
    }
}

@Composable
private fun WatchdogCard(
    watchdog: LiveUpdateManager.WatchdogState?,
    onDebugWatchdog: () -> Unit
) {
    if (watchdog == null) return

    var nowMs by remember(watchdog) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(watchdog) {
        while (true) {
            nowMs = SystemClock.elapsedRealtime()
            delay(1_000L)
        }
    }
    val remainingMs = (watchdog.startElapsedRealtimeMs + watchdog.durationMs - nowMs).coerceAtLeast(0L)
    val totalSec = remainingMs / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    val countdown = "%02d:%02d:%02d".format(hours, minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F4F9))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.watchdog_label),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )
        Text(
            text = countdown,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = ElAlBlue
        )
        Button(
            onClick = onDebugWatchdog,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Red)
        ) {
            Text(stringResource(R.string.watchdog_debug_button))
        }
    }
}

@Preview(showBackground = true, locale = "iw")
@Composable
fun JourneyDemoContentIdlePreview() {
    ElAlDemoTheme {
        JourneyDemoContent(
            statusText = UiText.DynamicString("Idle"),
            detailText = null,
            isActivityActive = false,
            isSimulating = false,
            errorMessage = null,
            watchdog = null,
            onStartActivity = {},
            onNextStage = {},
            onEndActivity = {},
            onConnectionError = {},
            onGeneralError = {},
            onSimulateDeparture = {},
            onSimulateArrival = {},
            onDebugWatchdog = {}
        )
    }
}

@Preview(showBackground = true, locale = "iw")
@Composable
fun JourneyDemoContentBoardingPreview() {
    ElAlDemoTheme {
        JourneyDemoContent(
            statusText = UiText.DynamicString("Boarding"),
            detailText = UiText.DynamicString("Group 3 · Gate B14"),
            isActivityActive = true,
            isSimulating = false,
            errorMessage = null,
            watchdog = LiveUpdateManager.WatchdogState(SystemClock.elapsedRealtime(), LiveUpdateManager.DEFAULT_WATCHDOG_MS),
            onStartActivity = {},
            onNextStage = {},
            onEndActivity = {},
            onConnectionError = {},
            onGeneralError = {},
            onSimulateDeparture = {},
            onSimulateArrival = {},
            onDebugWatchdog = {}
        )
    }
}

@Preview(showBackground = true, locale = "iw")
@Composable
fun JourneyDemoContentSimulatingPreview() {
    ElAlDemoTheme {
        JourneyDemoContent(
            statusText = UiText.DynamicString("On The Way"),
            detailText = UiText.DynamicString("ETA 14:35"),
            isActivityActive = true,
            isSimulating = true,
            errorMessage = null,
            watchdog = LiveUpdateManager.WatchdogState(SystemClock.elapsedRealtime(), LiveUpdateManager.DEFAULT_WATCHDOG_MS),
            onStartActivity = {},
            onNextStage = {},
            onEndActivity = {},
            onConnectionError = {},
            onGeneralError = {},
            onSimulateDeparture = {},
            onSimulateArrival = {},
            onDebugWatchdog = {}
        )
    }
}

@Preview(showBackground = true, locale = "iw")
@Composable
fun JourneyDemoContentErrorPreview() {
    ElAlDemoTheme {
        JourneyDemoContent(
            statusText = UiText.DynamicString("Connection Error"),
            detailText = null,
            isActivityActive = true,
            isSimulating = false,
            errorMessage = UiText.DynamicString("You seem to be offline. Check your connection."),
            watchdog = null,
            onStartActivity = {},
            onNextStage = {},
            onEndActivity = {},
            onConnectionError = {},
            onGeneralError = {},
            onSimulateDeparture = {},
            onSimulateArrival = {},
            onDebugWatchdog = {}
        )
    }
}
