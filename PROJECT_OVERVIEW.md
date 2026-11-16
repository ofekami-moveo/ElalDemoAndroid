# El Al Demo Android Project — Overview

_Updated 2026-11-16 (Live Update parity, Phase A, groups 1–5)._

## What does this app do?

It shows an El Al journey as an **Android 16 Live Update**: a promoted, ongoing notification with a status-bar
pill, a progress bar with per-phase icons, and buttons. It follows the flight from check-in to baggage claim and
is updated in place by server pushes (FCM). It is the Android counterpart of the iOS Live Activity.

---

## The two modules

```
ElalDemoAndroid/
├── app/                 ← Demo host app (what you install): simulators, FCM parser, demo controls
└── elal-flight-sdk/     ← The SDK (synced by hand into the production app)
```

---

## How the folders are organized

```
app/src/main/java/com/elal/demoapp/
├── MainActivity.kt              ← Asks for notification permissions; shows the boarding-pass placeholder
├── ElAlDemoApplication.kt       ← SDK setup: channel, analytics listener, action targets, restore
├── DemoActionIntents.kt         ← Demo target for the Boarding pass button
├── firebase/FlightMessagingService.kt  ← FCM → FlightContentState (data contract frozen for Phase A)
├── ui/journey/                  ← JourneyDemoScreen + JourneyViewModel (simulators and toggles)
└── debug/DebugNextStageReceiver ← Next-stage broadcast for QA (exported in debug builds only)

elal-flight-sdk/src/main/java/com/elal/flightsdk/
├── manager/        ← Android side
│   ├── LiveUpdateManager             ← Entry point (object): start / update / end, ticks, restore, dismiss
│   ├── LiveUpdateNotificationMapper  ← LiveUpdateContent → NotificationCompat (ProgressStyle, public version)
│   ├── LiveUpdateDismissReceiver     ← Delete intent: the user swiped the Live Update away
│   ├── PromotionDiagnostics          ← Whether the system will promote it; opens the setting
│   ├── InFlightService               ← Foreground service that keeps DEPARTED ticking (kept; see tasks 4.7)
│   ├── JourneyPrefs / ServerClock    ← Persistence and server-anchored "now"
│   └── Android*Provider / AndroidBidiWrapper / AndroidChipLabels
├── presentation/   ← Pure Kotlin, JVM-tested (no android.*)
│   ├── LiveUpdateContentBuilder      ← Everything the notification shows, from state + time snapshot
│   ├── StageVisualCatalog            ← Per-stage table: bar position, tracker, colours, alerts, promotion, actions
│   ├── ChipTextResolver              ← Status-bar pill text (≤ 7 characters)
│   ├── DepartedClock / FlightTimeline / TickIntervalCalculator / TimeoutCalculator
│   └── AlertPolicy / DismissPolicy / FlightDelayResolver / LiveUpdateModeResolver / …
├── model/          ← FlightContentState (JourneyData.kt), LiveUpdateConfig, LiveUpdateAction, PromotionStatus, …
├── analytics/      ← FlightAnalyticsListener, click trampoline (LiveActivityClickActivity)
└── sample/         ← SampleFlightData (TLV → JFK and JFK → TLV journeys)
```

---

## The journey

```
CHECK_IN → GATE_READY → BOARDING_PENDING → BOARDING → LAST_CALL → DEPARTURE_PENDING
        → DEPARTED → LANDING_SOON → LANDED → BAGGAGE → COMPLETED
Exceptions: CONNECTION_ERROR, GENERAL_ERROR
```

- One notification (ID 7001), updated in place. CHECK_IN is a regular ongoing notification; promotion starts at
  GATE_READY.
- Title `LY 317 · <stage>`, route sub-text, pill text per stage (gate, time, belt, time left in flight).
- In DEPARTED the bar and the pill move about once a minute (InFlightService keeps the process alive).
- A swipe stops all reposts for that journey; `end` (or COMPLETED) clears it; a safety timeout ends it at
  `dismissalDate`, else arrival + 4 h.

---

## How data flows

1. The server sends an FCM data message.
2. `FlightMessagingService` parses it into a `FlightContentState` (see `planning/current-data-contract.md`).
3. `LiveUpdateManager` builds the content (`LiveUpdateContentBuilder`) and posts it (`LiveUpdateNotificationMapper`).
4. In DEPARTED, a tick re-posts the bar and the pill; the start of the flight is persisted, so a process kill
   doesn't restart the bar.
5. `JourneyPrefs` keeps the state, so a restarted process restores the journey.

---

## Host API (what an app integrating the SDK sets)

| API | Purpose |
|---|---|
| `LiveUpdateManager.startJourney / updateJourney / endJourney / restoreFromPrefs` | Journey lifecycle |
| `LiveUpdateManager.config` (`LiveUpdateConfig`) | Refresh interval; `enableLegacyFallback` for Android 15 and older |
| `LiveUpdateManager.actionIntentProvider` | Target of the Boarding pass button |
| `LiveUpdateManager.analyticsListener` | Activated, clicked, action clicked, dismissed |
| `LiveUpdateManager.promotionStatus`, `openPromotionSettings` | Why a Live Update may not be promoted |

---

## Working on it

- Spec, tasks and decisions: `agent-os/specs/2026-11-10-live-update-parity/` (`spec.md`, `tasks.md`,
  `planning/requirements.md`, `planning/device-check.md`). Project rules: `CLAUDE.md`.
- Unit tests: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :elal-flight-sdk:testDebugUnitTest`.
- Run on an **Android 16+** device or emulator for the Live Update. On Android 15 and older nothing shows unless
  the demo's "Android 15 fallback" toggle is on.
- The demo's sample labels are Hebrew while the SDK strings follow the app language (task 6.5 adds a language
  toggle).
- `Docs/CLIENT_TECHNICAL_SPEC.md` and `Docs/JOURNEY_STATES_DATA.md` describe an older state model; see their banners.
