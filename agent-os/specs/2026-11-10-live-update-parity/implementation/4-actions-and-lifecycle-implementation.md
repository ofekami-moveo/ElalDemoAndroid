# Implementation Report: Actions and lifecycle

**Spec:** `2026-11-10-live-update-parity`
**Date:** 2026-11-16
**Task group:** 4
**Branch:** `update-LUN-ui` (per-group branch waived by the user)
**Status:** SDK builds; `testDebugUnitTest` 42/42 pass (2026-11-16, JDK 21). The compiler's two
`overridePendingTransition` deprecation warnings in `LiveActivityClickActivity` are pre-existing lines (left as is).
The demo build and the device checks are pending.

## Decisions taken in this group (2026-11-16)

- **4.7 deferred: InFlightService stays.** The pill's time-left text needs ~1-min re-posts; an inexact alarm would
  leave it 10+ min stale. Requirements §2.7 updated.
- **4.4 stays deferred** (data contract frozen), so the version tests were dropped; a promotion-status test was added.
- **Manifest (approved):** `LiveUpdateDismissReceiver`, `exported="false"`.

## Later change (2026-11-16)

- **Directions removed** at the user's request: `LiveUpdateAction.DIRECTIONS`, `notif_action_directions` (EN + HE),
  the demo's maps target and `demo_directions_query` are gone. CHECK_IN has no button; GATE_READY → DEPARTURE_PENDING
  show Boarding pass. `LiveUpdateActionsTest` updated (still 2 tests).

## What changed (visible)

- **Actions:** Directions at CHECK_IN, Boarding pass + Directions at GATE_READY, Boarding pass from BOARDING_PENDING
  to DEPARTURE_PENDING, none after takeoff. Shown only when the host's `actionIntentProvider` returns a target.
  Taps go through the click trampoline (analytics `onLiveActivityActionClicked`, `action` param).
- **Swipe:** the delete intent records the dismissal per `activityId` (persisted). No push, tick, restore or
  service start reposts it; `end` still clears; a new `activityId` posts. Analytics `onLiveActivityDismissed`.
- **Timeout:** `setTimeoutAfter` until `dismissalDate`, else arrival + 4 h. The end is persisted; a restore after it
  ends the journey instead of bringing it back. Watchdog unchanged.
- **Promotion diagnostics:** `promotionStatus` (ALLOWED / BLOCKED_BY_USER / NOTIFICATIONS_DISABLED / UNSUPPORTED),
  logged on start and stage change; `openPromotionSettings` opens `ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS`.

## Files

New (SDK): `model/LiveUpdateAction` (public), `model/PromotionStatus` (public), `presentation/DismissPolicy`,
`presentation/TimeoutCalculator`, `presentation/ActionFilter`, `presentation/PromotionStatusResolver`,
`manager/LiveUpdateDismissReceiver`, `manager/PromotionDiagnostics`.
Changed (SDK): `StageVisuals` / `StageVisualCatalog` (actions), `LiveUpdateContent` (+ `actions`),
`LiveUpdateContentBuilder`, `LiveUpdateNotificationMapper` (actions, delete intent, timeout), `LiveUpdateManager`
(provider, dismissal, timeout, restore check, promotion status), `JourneyPrefs` (dismissed id, journey end),
`InFlightService` (stops for a dismissed journey), `LiveActivityClickActivity` (`EXTRA_ACTION`),
`FlightAnalyticsListener` (2 default methods), `AnalyticsPayloadBuilder` (`actionClicked`, `dismissed`),
strings `notif_action_*` (EN + HE), **`AndroidManifest.xml` (receiver)**.
Demo: `DemoActionIntents` (new), `ElAlDemoApplication` (provider, listener logs), `MainActivity` (placeholder toast),
`JourneyViewModel` / `JourneyDemoScreen` (Dismiss button, promotion card), strings `demo_*` (EN + HE).

## New public API (for the port report)

- `LiveUpdateManager.actionIntentProvider: ((Context, LiveUpdateAction, FlightContentState) -> Intent?)?`
- `LiveUpdateManager.promotionStatus`, `refreshPromotionStatus(context)`, `openPromotionSettings(context)`
- `LiveUpdateManager.onUserDismissed(context, activityId)` (called by the receiver), `simulateUserDismiss(context)` (demo / QA)
- `FlightAnalyticsListener.onLiveActivityActionClicked`, `onLiveActivityDismissed` (default bodies)
- Enums `LiveUpdateAction`, `PromotionStatus`
- SDK manifest: `LiveUpdateDismissReceiver`

## Tests (4.1, 8)

`DismissPolicyTest` (3), `TimeoutCalculatorTest` (2), `LiveUpdateActionsTest` (2), `PromotionStatusResolverTest` (1).
Expected total: 42.

## Manual checklist

- Each stage: the right buttons; Boarding pass → demo toast; Directions → maps search.
- Swipe the Live Update (or "Dismiss"): next Next Stage / push / tick doesn't bring it back; after a process kill
  neither; End clears; Start posts a new one.
- dumpsys: `actions=`, `deleteIntent`, `timeout=`.
- Promotion card: "Allowed"; turn Live Updates off for the app in settings → "Blocked by the user" after return.

## Commit message (not committed)

```
Live Update: actions, swipe-to-dismiss, timeout, promotion diagnostics

Boarding pass / Directions actions via the host's actionIntentProvider,
through the analytics trampoline. A swipe is recorded per activityId and
nothing reposts it (push, tick, restore, service); end still clears.
setTimeoutAfter(dismissalDate ?: arrival + 4 h) with a restore check.
promotionStatus + openPromotionSettings. InFlightService kept (4.7
deferred). SDK manifest: LiveUpdateDismissReceiver. 8 JVM tests.
```
