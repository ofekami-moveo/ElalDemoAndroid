# Implementation Report: Content builder (pure refactor)

**Spec:** `2026-11-10-live-update-parity`
**Date:** 2026-11-16
**Task group:** 1 (also documents the Figma pill work done ahead of it on 2026-11-10)
**Branch:** `update-LUN-ui` (per-group branch waived by the user)
**Status:** Code written; module builds; `testDebugUnitTest` 19/19 pass (2026-11-16, JDK 21): smoke 3, chip 8,
delay 2, content builder 5, timeline 1. Before/after device screenshots not taken yet.

## What changed

All notification content is now built by a pure `LiveUpdateContentBuilder` (no `android.*`), and a
`LiveUpdateNotificationMapper` is the only code that touches `NotificationCompat`, drawables and colours:

```
buildNotification(state) = mapper.map(builder.build(state, currentTimeSnapshot(), isSelfUpdate))
```

Output is meant to be identical to before. Values were moved, not changed: F2 (DEPARTURE_PENDING at 38) stays
until group 3.

## Files

New, `model/` (internal):
- `LiveUpdateContent`, `LiveUpdateProgress`, `SegmentSpec`, `IconRole`, `ColorRole`, `TimeSnapshot`

New, `presentation/` (internal, pure Kotlin):
- `LiveUpdateContentBuilder`: title, content text, DEPARTED countdown, flight time, pill text/icon, accent,
  progress, `onlyAlertOnce`. Replaces `buildStatusBarText`, `buildContentText`, `buildDepartedCountdown`,
  `buildEstimatedFlightTime`, `chipIconFor`, `chipColorFor`, `buildProgressStyle`.
- `StageVisualCatalog` + `StageVisuals` + `ProgressPosition`: progress position, tracker, airborne, error
  no-alert, per stage (today's values).
- `ProgressCalculator`: segment layout (38 / 48 / 152, max 240, `SEG0_COMPENSATION`) and fill value.
- `FlightTimeline`: departure on GMT + calculated arrival; used by `setupRealtimeClocks`.
- `StringProvider`, `StringKey`, `BidiWrapper`.

New, `manager/` (Android):
- `LiveUpdateNotificationMapper` (also owns the click `PendingIntent`), `AndroidStringProvider`,
  `AndroidBidiWrapper`.

Changed:
- `manager/LiveUpdateManager.kt`: `buildNotification` delegates; new `currentTimeSnapshot()` reads the clock
  pair once per build (replaces `remainingFlightMsOrNull` / `departedCountdownRemainingMs`);
  `setupRealtimeClocks` uses `FlightTimeline`; removed the replaced private builders and `PROGRESS_MAX` /
  `SEG0_COMPENSATION`. Tick timing, service, watchdog and restore logic untouched.

Not removed: `utils/combineLabelAndValue` is now unused in the SDK but is a public top-level function, so it is
left for hosts. The builder has a private copy that goes through `BidiWrapper`.

## Tests (1.1, 6)

- `LiveUpdateContentBuilderTest`: title/content per stage; DEPARTED countdown, minimum 1 min, "Landing soon",
  wall-clock fallback, pill time; progress per stage incl. F2 and the 239 cap, delayed segments; tracker and
  pill icon per stage incl. delayed variants; error stages and `onlyAlertOnce`.
- `FlightTimelineTest`: arrival = departure + offset + duration; missing offset; no duration.
- Fakes: `FakeStringProvider` (EN strings), `FakeBidiWrapper` (`[x]` marks a wrapped value).

Run: `elal-flight-sdk` → `testDebugUnitTest` (expected 19 = 13 existing + 6 new).

## Manual checklist

- Build the demo app; run "Simulate Journey from Tel Aviv" (it runs the JFK→TLV data until group 6) with delay off
  and on, and "Start Activity" + "Next Stage".
- Per stage, EN and HE: title, content text, bar fill and tracker, pill text and icon look exactly as before.
- DEPARTED: countdown text and pill tick down together; `uiDebugMode` logs `[AIRPLANE] progressBar=…`.
- Error buttons: error title/text, empty bar, no re-alert.
- `adb shell dumpsys notification --noredact | grep -A60 "id=7001"`: same extras as before.

## Commit message (not committed)

```
Extract Live Update content into a pure, tested builder

LiveUpdateContentBuilder builds title, text, pill, icons, colours and
progress from FlightContentState + TimeSnapshot with no android.*;
LiveUpdateNotificationMapper maps it to the ProgressStyle notification.
StageVisualCatalog/ProgressCalculator/FlightTimeline hold today's values.
No visible change. 6 JVM tests.
```
