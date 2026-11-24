# El Al Demo Android — Live Update SDK

A demo host app plus the `:elal-flight-sdk` library that shows an El Al flight journey as an
**Android 16 Live Update**: a promoted, ongoing notification with a status-bar pill, a progress bar with
per-phase icons and action buttons. It is the Android counterpart of the iOS Live Activity and is updated
in place by server pushes (FCM), from check-in to baggage claim.

## Modules

| Module | What it is |
|---|---|
| `app/` | Demo host app: notification-permission flow, journey simulators and toggles, FCM parser, QA next-stage receiver |
| `elal-flight-sdk/` | The SDK: `LiveUpdateManager`, notification mapper, in-flight ticking service, persistence, pure-Kotlin presentation layer |

## Journey stages

```
CHECK_IN → GATE_READY → BOARDING_PENDING → BOARDING → LAST_CALL → DEPARTURE_PENDING
        → DEPARTED → LANDING_SOON → LANDED → BAGGAGE → COMPLETED
Exceptions: CONNECTION_ERROR, GENERAL_ERROR
```

- One notification (ID `7001`), updated in place. CHECK_IN is a regular ongoing notification; promotion
  starts at GATE_READY.
- In DEPARTED the progress bar and pill advance about once a minute.
- A user swipe stops all reposts for that journey; `endJourney` (or COMPLETED) clears it; a safety timeout
  ends it at `dismissalDate`, otherwise arrival + 4 h.

## How data flows

1. The server sends an FCM data message.
2. `FlightMessagingService` parses it into a `FlightContentState`.
3. `LiveUpdateManager` builds the content (`LiveUpdateContentBuilder`) and posts it
   (`LiveUpdateNotificationMapper`, `NotificationCompat.ProgressStyle`).
4. `JourneyPrefs` persists the state so a restarted process restores the journey.

## Host API

| API | Purpose |
|---|---|
| `LiveUpdateManager.startJourney / updateJourney / endJourney / restoreFromPrefs` | Journey lifecycle |
| `LiveUpdateManager.config` (`LiveUpdateConfig`) | Refresh interval; `enableLegacyFallback` for Android 15 and older |
| `LiveUpdateManager.actionIntentProvider` | Target of the Boarding pass button |
| `LiveUpdateManager.analyticsListener` | Activated, clicked, action clicked, dismissed |
| `LiveUpdateManager.promotionStatus`, `openPromotionSettings` | Why a Live Update may not be promoted |

## Requirements

- Android Studio with JDK 17+ (tests run on JDK 21)
- compileSdk 36, minSdk 26, AGP 8.9, Kotlin 2.0
- An **Android 16+** device or emulator to see the Live Update. On Android 15 and older nothing shows
  unless the demo's "Android 15 fallback" toggle is on.

## Getting started

```bash
git clone <this-repo>
cd ElalDemoAndroid
./gradlew :app:installDebug
```

Unit tests (pure presentation logic, JVM):

```bash
./gradlew :elal-flight-sdk:testDebugUnitTest
```

To send real pushes, use your own Firebase project's `google-services.json` and the Postman collection in
`Docs/handover/live-activity/`.

## Documentation

- `PROJECT_OVERVIEW.md` — architecture and folder map
- `Docs/` — integration guide, analytics/click handling, PNR mapping, handover notes
- `agent-os/specs/2026-11-10-live-update-parity/` — spec, tasks and decisions
- `CLAUDE.md` — coding conventions for this repo
