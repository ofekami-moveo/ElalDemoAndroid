# Specification: Live Update parity (LY 317)

## Goal

Make the Android 16 Live Update for an El Al journey show the same information as the iOS Live Activity, using
only the system `ProgressStyle` template: a short status-bar chip, a live countdown that never goes stale,
per-phase progress icons, one consistent title, actions, and a delay colour, with a safe lifecycle (no repost
after dismiss, timeout, version guard) and promotion diagnostics.

## User stories

- As a traveller at the airport, I want my gate or departure time in the status-bar pill, so I don't have to open
  the shade.
- As a traveller in flight, I want a "time to landing" countdown that is right even if the app was killed.
- As a traveller, I want one tap to my boarding pass from the notification.
- As a traveller who swiped the notification away, I don't want it to come back for the same flight.

## Specific requirements

1. **Chip (top priority, F10).** `setShortCriticalText` with one value per phase, ≤7 characters in EN and HE
   (requirements §3a). DEPARTED sets no chip text so the chronometer fills the chip. Acceptance on the Samsung S25+:
   the pill appears in the status bar and in the Now Bar on the lock screen, for pre-flight stages (text) and
   DEPARTED (chronometer).
2. **Countdown.** `setWhen` + `setUsesChronometer(true)` + `setChronometerCountDown(true)` against a server-time
   target converted to device time (requirements §4). Content text holds absolute times only.
3. **Title.** `"{flightNumber} · {stageTitle}"`, identical in collapsed and expanded views; `setSubText` with the
   route.
4. **Progress visuals.** Start/end icons on every stage, tracker icon per phase, monotonic progress (F2 fixed),
   delayed segment colour (`flightStatus` ≈ `DELAYED`, case-insensitive, one function), error grey.
5. **Promotion.** Requested from GATE_READY onward; CHECK_IN is a regular ongoing notification. The SDK manifest
   declares `POST_PROMOTED_NOTIFICATIONS`. `promotionStatus: StateFlow<PromotionStatus>`, logged on start/stage
   change, plus `openPromotionSettings(context)`.
6. **Actions.** Boarding pass (Directions removed 2026-11-16) via host `actionIntentProvider`, through the click trampoline
   for analytics. None on LANDED/BAGGAGE yet.
7. **Lifecycle.** `setDeleteIntent` → never repost for that `activityId` (`end` still honoured); optional
   `version` guard; `setTimeoutAfter(dismissalDate ?: arrival + 4 h)` recomputed per post; watchdog kept.
8. **In-flight refresh.** Progress-only tick every 10–15 min. `InFlightService` retired in favour of an inexact
   alarm (own sub-task, revert if risky).
9. **≤15 fallback.** Behind `LiveUpdateConfig.enableLegacyFallback`, off by default; no FGS below 36.
10. **Demo app.** Each group ships the demo-app changes needed to check it on a device.

## Architecture

- `model/`: `LiveUpdateContent`, `LiveUpdateProgress`, `SegmentSpec`, `IconRole`, `ColorRole`,
  `LiveUpdateAction`, `TimeSnapshot`, `PromotionStatus`, `LiveUpdateConfig`.
- `presentation/` (pure Kotlin, JVM-tested): `StringProvider`, `StringKey`, `BidiWrapper`, `StageVisuals`,
  `StageVisualCatalog`, `FlightTimeline`, `ChipTextResolver`, `ChronometerCalculator`, `ProgressCalculator`,
  `TimeoutCalculator`, `LiveUpdateContentBuilder`.
- `manager/` (Android): `AndroidStringProvider`, `AndroidBidiWrapper`, `LiveUpdateNotificationMapper`,
  `VersionGuard`, `DismissRegistry`, `PromotionDiagnostics`, `LiveUpdateDismissReceiver`, `ProgressRefreshReceiver`.

## Out of scope

- Server-driven deep links (S5), baggage-info action, Samsung-specific APIs, changes to the production repo.

## Reference

`planning/requirements.md` (findings, decisions, per-stage tables, clock design, server contract, regression matrix).
