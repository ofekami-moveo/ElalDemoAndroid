# Implementation Report: Chip and countdown

**Spec:** `2026-11-10-live-update-parity`
**Date:** 2026-11-16
**Task group:** 2
**Branch:** `update-LUN-ui` (per-group branch waived by the user)
**Status:** SDK builds; `testDebugUnitTest` 27/27 pass (2026-11-16, JDK 21). The demo app (`:app`) hasn't been
compiled yet, and the device checks haven't been run.

## Decisions taken in this group

- **2.3 chronometer replaced** by the Figma time-left text in the pill (already decided 2026-11-10); `setShowWhen(false)` stays.
- **Tick (2.6), decided 2026-11-16:** 1% of the flight, capped by `LiveUpdateConfig.progressRefreshIntervalMs`,
  default 1 min, clamped 1–15 min, so the pill's minutes stay right. Requirements §4 updated.
- **Gate pill:** bare gate (Figma), so no `notif_chip_gate` string.

## What changed (visible)

- Title: `LY 317 · Boarding` (no time) in both views. Server `stageTitle` wins; when blank or `--`, the SDK label
  (`notif_stage_*`, EN + HE); errors always use the SDK label. This removes the `--` seen in the title.
- Sub-text: route `TLV → JFK` (HE `TLV ← JFK`), codes bidi-isolated.
- DEPARTED content: `Lands at 23:12` / `נוחתים ב-23:12` (absolute, never stale). The pill keeps the time left.
- Lock screen: a public version (title, route, pill, bar; content = gate + departure / arrival time; never the PNR).
- DEPARTED after a process kill or a repeated push: the bar and the pill continue from the first DEPARTED push (F1).

## Files

New, `presentation/`: `DepartedClock` (start/arrival on the server timeline, progress, time left; arrival
fallbacks), `TickIntervalCalculator`.
New, `model/`: `LiveUpdateConfig` (**public**).
Changed:
- `presentation/LiveUpdateContentBuilder.kt`: title, sub-text, `Lands at`, public text; removed the "Landing in"
  countdown text.
- `presentation/StringKey.kt`, `manager/AndroidStringProvider.kt`: new keys; unused countdown / status-bar keys removed.
- `model/LiveUpdateContent.kt` (+ `subText`, `publicText`), `model/TimeSnapshot.kt` (− `wallNowMs`).
- `manager/LiveUpdateNotificationMapper.kt`: `setSubText`, `setPublicVersion` (shared `baseBuilder`).
- `manager/LiveUpdateManager.kt`: `departedClock` replaces the elapsedRealtime pair; `setupRealtimeClocks(context, state)`
  loads/saves the start; tick via `TickIntervalCalculator`; new public `config` and `pillTextFlow`.
- `manager/JourneyPrefs.kt`: `saveDepartedStart` / `loadDepartedStart` (per `activityId`, cleared on end).
- SDK strings EN + HE: `notif_title`, `notif_subtext_route`, `notif_content_lands_at`, `notif_public_*`, `notif_stage_*`.
- Demo: `JourneyViewModel.pillTextFlow`, pill line on the status card, strings `demo_pill_text`,
  `demo_pill_icon_only` (EN + HE).

No build, manifest or permission change.

## New public API (for the port report)

- `LiveUpdateManager.config: LiveUpdateConfig` (optional; defaults keep behaviour).
- `LiveUpdateManager.pillTextFlow: StateFlow<String?>` (diagnostics).

## Tests (2.1, 8)

`ChipTextLocalizationTest` (2, real EN/HE `strings.xml` via `XmlStrings`), `LiveUpdateTitleTest` (2),
`DepartedClockTest` (2), `TickIntervalCalculatorTest` (1), `PublicVersionTest` (1). Group 1's
`LiveUpdateContentBuilderTest` updated for the new title and DEPARTED text (still 5).
Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :elal-flight-sdk:testDebugUnitTest` → expected 27.

## Manual checklist

- Every stage, EN and HE: title `LY 316 · <stage>` (no `--`, no time), route sub-text, content as before except
  DEPARTED `Lands at …`; demo card shows the same pill text as the status bar.
- DEPARTED: pill time left drops about once a minute (every few seconds on the 5-min demo flight).
- Process kill in DEPARTED (`adb shell am kill com.elal.demoapp` with the app in the background, then trigger a tick
  or a push): the bar continues, doesn't jump back.
- S25+ with "hide content when locked": lock screen shows the public version, not "Contents hidden".
- `dumpsys notification --noredact | grep -A60 "id=7001"`: `PROMOTED_ONGOING`, `android.shortCriticalText`,
  `android.subText`, a `publicVersion`.

## Commit message (not committed)

```
Live Update: one title, route, lock-screen version, F1 restore fix

Title is "flight · stage" with SDK fallbacks and no time; route sub-text;
DEPARTED reads "Lands at HH:mm" while the pill keeps the time left.
Public version for the lock screen (gate and times, no PNR).
DepartedClock persists the first DEPARTED start per activityId, so a
restore or repeated push no longer restarts the bar (F1). In-flight tick
capped by LiveUpdateConfig.progressRefreshIntervalMs (default 1 min).
8 JVM tests.
```
