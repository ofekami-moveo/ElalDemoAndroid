# Task Breakdown: Live Update parity (LY 317)

## Overview

Total: 8 groups + feature verification. Dependencies 0 → 1 → 2 → 3 → 3b → 4 → 5; group 6 (demo restart) needs
only 1.

**Build note:** never run `./gradlew`; the user builds and pastes errors back.
**Branch note:** the user waived per-group branches; work happens on `update-LUN-ui`. The `**Branch:**` lines are
the intended names if branches are cut later.
**Demo note:** every group ships the demo-app changes needed to check it on a device.
**Device note:** after every group run the regression matrix (requirements §6) on the Pixel and the Samsung S25+.

**Scope note:** UI only. The data contract is frozen in `planning/current-data-contract.md`; tasks that change the
payload, parser or `FlightContentState` are deferred. **One exception (2026-11-16):** group 3b adds four optional
schedule fields to `FlightContentState`, filled only by the demo simulator, "demo-only until Phase B". The FCM parser
is not touched.

**Reference:** `spec.md`, `planning/requirements.md`, `planning/current-data-contract.md`, Figma (`planning/visuals/`).

## Task List

#### Task Group 0: Workflow bootstrap

**Branch:** `moveo/feature/live-update-parity/workflow-bootstrap` · **Dependencies:** none

- [x] 0.0 Set up the workflow
  - [x] 0.1 `CLAUDE.md` adapted for the SDK
  - [x] 0.2 Copy `agent-os/{commands,standards}` from the main app; add `standards/SDK_EXCEPTIONS.md`
  - [x] 0.3 Create this spec folder (`planning/initialization.md`, `planning/requirements.md`, `spec.md`, `tasks.md`)
  - [x] 0.4 Add `testImplementation("junit:junit:4.13.2")` to `elal-flight-sdk/build.gradle.kts` (approved 2026-11-10)
  - [x] 0.5 One smoke test, `ToAbsoluteInstantSmokeTest`
  - [x] 0.6 Run ONLY the smoke test (user): `elal-flight-sdk` → `testDebugUnitTest` (covered by the 13/13 run
    2026-11-10, which included the 3 smoke tests)

**Acceptance Criteria:** the module builds, the test task runs 1 test class and passes, app behaviour unchanged.

#### Figma pill (done ahead of group 1, 2026-11-10)

- [x] Icons `ic_live_update_chip_departure` / `ic_live_update_chip_flight`, colours `live_update_chip_on_time` /
  `live_update_chip_delayed` (day + night)
- [x] `presentation/ChipTextResolver`, `ChipLabels`, `FlightDelayResolver`; `manager/AndroidChipLabels`
- [x] `LiveUpdateManager.buildNotification`: small icon per phase, `setColor` by delay, `setShortCriticalText`
- [x] Strings `notif_chip_belt`, `notif_chip_duration_hours`, `notif_chip_duration_minutes` (EN + HE); HE offline → `מנותק`
- [x] Demo app: "Simulate delay" toggle
- [x] Pill background can't be coloured (device): delay shown by badge icons `ic_live_update_chip_*_delayed` and an
  amber bar (`progress_bar_segment_delayed`, day + night)
- [x] Tests: `ChipTextResolverTest` (8), `FlightDelayResolverTest` (2)
- [x] Tests run 2026-11-10 with JDK 21: 13/13 pass (smoke 3, chip 8, delay 2)
- [ ] Device check on Pixel and S25+

Group 1 moves these pure classes into `LiveUpdateContentBuilder`; 2.2 is covered, 2.3 (chronometer) is replaced by the
Figma duration text, 3.3 colour is covered.

#### Task Group 1: Content builder (pure refactor)

**Branch:** `moveo/feature/live-update-parity/content-builder` · **Dependencies:** 0

- [ ] 1.0 Extract the content into a pure builder (code and tests done 2026-11-16; device screenshots pending)
  - [x] 1.1 Write 6 tests: title/content per stage equal today's (fake strings); DEPARTED countdown text and
    "Landing soon"; progress per stage equals today's (incl. F2 value); tracker role per stage; error title and
    progress; `FlightTimeline` arrival = departure + offset + duration, missing-offset fallback
  - [x] 1.2 `model/*` and `presentation/*` with **today's** values in `StageVisualCatalog` (only the types group 1
    needs; `LiveUpdateAction`, `PromotionStatus`, `LiveUpdateConfig` come with their groups)
  - [x] 1.3 `LiveUpdateNotificationMapper`, `AndroidStringProvider`, `AndroidBidiWrapper`; `buildNotification` =
    `mapper.map(builder.build(state, snapshot))`
  - [x] 1.4 Tick passes progress and remaining time through `TimeSnapshot`; timing unchanged
  - [x] 1.5 Remove the replaced private builders; leave untouched code unformatted
  - [x] 1.6 Run ONLY the 6 tests from 1.1 (2026-11-16, JDK 21: 19/19 pass in `testDebugUnitTest`, incl. the 6 new)

**Acceptance Criteria:** screenshots before and after identical for every stage, EN and HE.

#### Task Group 2: Chip and countdown (top priority, F10)

**Branch:** `moveo/feature/live-update-parity/chip-and-countdown` · **Dependencies:** 1 · **Server:** S3 optional

- [ ] 2.0 Chip, title, restore fix, lock screen (code and tests done 2026-11-16; demo app build and device check pending)
  - [x] 2.1 Write 8 tests (chronometer tests replaced, see 2.3): pill ≤7 for every stage in EN and HE using the real
    strings (`XmlStrings`); bare gate → `null` and belt fallbacks (Figma); title is "flight · stage" with SDK fallback
    and no time; DEPARTED "Lands at" + route sub-text; progress continues after a simulated restore; arrival
    fallbacks and passed arrival; tick 1% capped by the config, clamped 1–15 min; public version has gate and
    times and never the PNR
  - [x] 2.2 `ChipTextResolver` with `setShortCriticalText` (done with the Figma pill). Gate stages show the bare gate
    (Figma), so `notif_chip_gate` isn't added; offline / error use `notif_chip_connection_error` /
    `notif_chip_general_error`
  - ~~2.3 `ChronometerCalculator`, `setWhen` / `setUsesChronometer`~~ **Replaced** by the Figma time-left text in the
    pill (decided 2026-11-10); `setShowWhen(false)` stays
  - [x] 2.4 Title `flight · stageTitle` with SDK fallbacks (`notif_stage_*`, EN + HE); DEPARTED content
    `notif_content_lands_at`; `setSubText` route (`notif_subtext_route`, arrow mirrored in HE)
  - [x] 2.5 Persist `departedStartServerMs` per `activityId` (`JourneyPrefs`, `DepartedClock`); fixes F1
  - [x] 2.6 In-flight tick: 1% of the flight, capped by `LiveUpdateConfig.progressRefreshIntervalMs` (default
    **1 min**, clamped 1–15 min; decided 2026-11-16 because the pill shows minutes); `InFlightService` unchanged
  - ~~2.7 Parse `gateOpenTime`~~ **Deferred:** UI-only scope, data contract frozen (`planning/current-data-contract.md`)
  - [x] 2.8 Demo app: the posted pill text on the demo screen (`LiveUpdateManager.pillTextFlow`)
  - [x] 2.10 Lock screen: `setPublicVersion()` built by the mapper from the same content (flight, stage title, gate,
    times; **no PNR**); the main notification stays `VISIBILITY_PRIVATE`. Fixes "Contents hidden" on the S25+
  - [x] 2.9 Run ONLY the 8 tests from 2.1 (2026-11-16, JDK 21: 27/27 pass in `testDebugUnitTest`, incl. the 8 new)

**Acceptance Criteria:** on the Pixel the pill appears for every stage with data (text; DEPARTED shows the time left
and moves about once a minute); every stage sets chip text or shows the icon only when the data is missing; the bar
and the pill continue after a process kill in DEPARTED instead of restarting; collapsed and expanded show the same
title with no time. On the Samsung S25+ dumpsys shows `PROMOTED_ONGOING` and `android.shortCriticalText`. The pill
and Now Bar on Samsung are **not** an acceptance gate: the spike showed One UI withholds them (open Samsung risk,
`planning/spike-chip-results.md`). With "hide content when locked", the S25+ lock screen shows the public version
instead of "Contents hidden".
The spike code (`SpikeChipVariant`, `// SPIKE` lines) is removed before group 2 starts (checked 2026-11-16: none left).

#### Task Group 3: Progress visuals

**Branch:** `moveo/feature/live-update-parity/progress-visuals` · **Dependencies:** 2 · **Server:** S2 optional

- [ ] 3.0 Icons, delay colour, promotion per stage (done 2026-11-16 on the emulator; dark / RTL / HE look and the S25+ pending)
  - [x] 3.1 Write 7 tests (`StageVisualsTest`): progress never decreases along both sample sequences; segment colour
    role delayed / on time / error; delay parsing (`DELAYED`, `delayed`, padded); tracker per phase; start/end icons
    always present; promotion per stage (CHECK_IN not promoted); alert-on-entry matrix
  - [x] 3.2 Drawables: `ic_progress_start` / `ic_progress_end`, trackers `ic_progress_tracker_check_in`, `_gate`
    (autoMirrored), `_baggage`; same 39×25 frame as the plane trackers. Glyphs are Material placeholders until design
    sends the El Al icons. RTL look to check on device
  - [x] 3.3 `FlightDelayResolver.isDelayed(state)` (one entry point); `progress_bar_segment_delayed` (`#FFB936`, Figma)
    and `progress_bar_segment_error` in `values/` + `values-night/`. Delay shows only until landing (§3b)
  - [x] 3.4 DEPARTURE_PENDING = 88 (fixes F2)
  - [x] 3.5 Promotion from GATE_READY; `onlyAlertOnce` from the catalog + previous post (`AlertPolicy`, `PostedState`):
    alert on entering the §3b stages and on a delay flip, never on a re-post or a tick
  - [x] 3.6 SDK manifest declares `POST_PROMOTED_NOTIFICATIONS` (approved 2026-11-16); F7 leftovers deleted
    (`layout/notification_progress_bar.xml`, `notification_dot_*`, `notification_seg_*`, 22 unused strings EN + HE)
  - [x] 3.7 Re-check `SEG0_COMPENSATION` with start icons (emulator 2026-11-16: seg0 and seg1 render equal next to the
    start icon; compensation kept, `planning/device-check.md`)
  - [x] 3.8 Demo app: "Simulate delay" toggle (done with the Figma pill, 2026-11-10)
  - [x] 3.9 Run ONLY the 7 tests from 3.1 (2026-11-16, JDK 21: 34/34 pass in `testDebugUnitTest`, incl. the 7 new)

**Acceptance Criteria:** icons visible on every stage in EN and HE; delayed colour on toggle; CHECK_IN not promoted
in dumpsys, GATE_READY promoted.

#### Task Group 3b: Schedule changes struck through (delay and gate change)

**Branch:** `moveo/feature/live-update-parity/schedule-changes` · **Dependencies:** 3 · **Server:** S2, S4 (Phase B)

- [x] 3b.0 Old value struck through, new value next to it (iOS style) (done 2026-11-16; emulator checked: plain text default, 2 lines;
  physical Pixel / S25+ pending)
  - [x] 3b.1 Write 6 tests (`ScheduleChangeTest`): span ranges for departure, arrival and gate in EN and HE (real HE
    strings via `XmlStringProvider`); plain-text fallback EN + HE; no new fields → today's text; same time → no strike
    (and a later estimate counts as delayed); pill shows the estimated time; gate change recorded per journey
  - [x] 3b.2 `FlightContentState`: optional `scheduledDepartureTime`, `estimatedDepartureTime`, `scheduledArrivalTime`,
    `estimatedArrivalTime`; recorded in `planning/current-data-contract.md` as demo-only until Phase B. Parser untouched
  - [x] 3b.3 Pure `ScheduleChangeFormatter` (+ `StyledLine`, `TextSpanSpec`, `SpanType`); bidi-isolated values; strings
    `notif_change_departs|values|was|spoken` (EN + HE); arrival reuses `notif_content_lands_at`, gate `notif_public_gate`
  - [x] 3b.4 Mapper: `StrikethroughSpan`, or plain text via `LiveUpdateConfig.scheduleChangeStyle`. **Default
    `PLAIN_TEXT`** (decided 2026-11-16: the Android 17 emulator doesn't draw the strike in a promoted notification);
    `SPAN` kept as an option. Change lines replace the stage line, max 2 (the expanded view shows 2 lines)
  - [x] 3b.5 TalkBack: `TtsSpan` over the change, "17:55, was 17:00" (best effort; check per device)
  - [x] 3b.6 `ScheduleTimes`: pill, "Lands at" and the public version use estimated ?: scheduled ?: payload time.
    Decided 2026-11-16: a later estimated departure also counts as delayed (`FlightDelayResolver.isDelayed(state)`);
    the in-flight clock and the timeout keep today's arrival
  - [x] 3b.7 `GateChangeTracker` + `GateRecord`, persisted in `JourneyPrefs` per `activityId`; cleared on a new journey
    or when the gate changes back. The pill shows the new gate only
  - [x] 3b.8 Demo: "Simulate delay" also sets scheduled / estimated (+55 min) departure and arrival; "Simulate gate
    change" (→ `E4` and back); "Change style: strike / text" toggle
  - [x] 3b.9 Run ONLY the 6 tests from 3b.1 (re-run after the plain-text default: 52/52) (2026-11-16, JDK 21: 52/52 pass; the first run caught a gate change-back
    bug in `GateChangeTracker`, fixed)

**Acceptance Criteria:** with delay on, the content shows `Departs 17:55 (was 17:00)` (default) or, with the SPAN
style, `Departs ~~17:00~~ 17:55`, and the pill `17:55`; gate change shows `Gate ~~D2~~ D3`; HE renders the times and gate in the right
order; TalkBack result recorded per device; with no new fields the notification equals group 3's output. Both styles
compared on the Pixel and the S25+, result recorded.

#### Task Group 4: Actions and lifecycle

**Branch:** `moveo/feature/live-update-parity/actions-and-lifecycle` · **Dependencies:** 3 · **Server:** S1, S5, S6 optional

- [ ] 4.0 Actions, dismiss, timeout, diagnostics (done 2026-11-16 on the emulator, `planning/device-check.md`; timeout firing and the S25+ pending)
  - [x] 4.1 Write 8 tests (version tests dropped with 4.4; promotion-status test added): dismissed activity never
    reposted (push or tick share one check); new `activityId` posts after dismiss; `end` clears the dismissal;
    timeout from `dismissalDate` vs arrival + 4 h; past end gives no timeout; actions per stage; `null` target hides
    the action; promotion status order
  - [x] 4.2 `LiveUpdateManager.actionIntentProvider`, action strings EN + HE, trampoline `EXTRA_ACTION` (asks the
    host again on tap), listener default method `onLiveActivityActionClicked`
  - [x] 4.3 `DismissPolicy` + persisted dismissal (`JourneyPrefs`), `setDeleteIntent`, `LiveUpdateDismissReceiver`
    (SDK manifest, not exported; approved 2026-11-16), `onUserDismissed`, listener default `onLiveActivityDismissed`.
    `InFlightService` stops instead of reposting a dismissed journey
  - ~~4.4 `VersionGuard`, `version` field~~ **Deferred:** UI-only scope, data contract frozen
  - [x] 4.5 `TimeoutCalculator`, `setTimeoutAfter`, persisted absolute end, restore check (past end → `end`);
    watchdog kept
  - [x] 4.6 `PromotionDiagnostics` + `PromotionStatusResolver`, `promotionStatus` (logged on start / stage change),
    `refreshPromotionStatus`, `openPromotionSettings`
  - ~~4.7 Retire `InFlightService`~~ **Deferred (decided 2026-11-16):** the DEPARTED pill shows the time left as text
    and needs ~1-min re-posts; an inexact alarm (10–15 min, longer in Doze) would leave it stale. Keep the service
    (requirements §2.7 fallback); revisit in Phase B with Play's `specialUse` review
  - [x] 4.8 Demo app: Boarding pass target (placeholder toast), "Dismiss (simulate swipe)" button
    (`LiveUpdateManager.simulateUserDismiss`), promotion status card with "Open settings". **Directions removed
    2026-11-16** (enum value, strings, demo target): Boarding pass only, from GATE_READY until takeoff
  - [x] 4.9 Run ONLY the 8 tests from 4.1 (2026-11-16, JDK 21: 42/42 pass in `testDebugUnitTest`, incl. the 8 new)

**Acceptance Criteria:** swipe → no repost on the next push or tick, and none after a process kill; `end` still
clears; a new journey posts; actions open the demo placeholders; dumpsys shows the actions, the delete intent and a
`timeoutAfter`; the demo's promotion card shows the right status and opens settings. (4.7 manual tests not needed:
the service stays.)

#### Task Group 5: Legacy fallback and demo controls

**Branch:** `moveo/feature/live-update-parity/legacy-fallback` · **Dependencies:** 4

- [ ] 5.0 Fallback for ≤15 (done 2026-11-16; emulator API 37 checked, `planning/device-check.md`; fallback on an API ≤35 device pending)
  - [x] 5.1 Write 4 tests (`LegacyFallbackTest`): promoted vs fallback content equal apart from `requestPromoted`;
    flag off by default; fallback never asks for the FGS; config validation (non-positive interval rejected)
  - [x] 5.2 `LiveUpdateConfig.enableLegacyFallback` (default false); `LiveUpdateModeResolver` (PROMOTED / FALLBACK /
    OFF); `isSupported` reads it; the fallback never starts `InFlightService`
  - [x] 5.3 `MainActivity` requests `POST_NOTIFICATIONS` on 33+ (F4); `POST_PROMOTED_NOTIFICATIONS` on 36+
  - [x] 5.4 Demo app: "Android 15 fallback" toggle (applies to the next start)
  - [x] 5.5 `app/src/debug/AndroidManifest.xml` exports `DebugNextStageReceiver` in debug only (F6, approved 2026-11-16)
  - [x] 5.6 Docs (F8, scope approved 2026-11-16): `PROJECT_OVERVIEW.md` rewritten; "outdated" banner on
    `Docs/CLIENT_TECHNICAL_SPEC.md` and `Docs/JOURNEY_STATES_DATA.md`
  - [x] 5.7 Run ONLY the 4 tests from 5.1 (2026-11-16, JDK 21: 46/46 pass in `testDebugUnitTest`, incl. the 4 new)

**Acceptance Criteria:** on a ≤15 device the fallback renders a plain ongoing progress notification (same title, text and
bar, no promotion, no foreground service) when enabled; nothing posts there when disabled. (No chronometer: replaced by
the pill text in group 2.) On Android 16+ the toggle changes nothing. Debug build: `adb shell am broadcast -a
com.elal.demoapp.debug.NEXT_STAGE -n com.elal.demoapp/.debug.DebugNextStageReceiver` advances the stage.

#### Task Group 6: Demo restart (A6)

**Branch:** `moveo/feature/live-update-parity/demo-restart` · **Dependencies:** 1 (demo app only; can run any time)

- [x] 6.0 A demo `start` while a journey is active ends the old one first (done 2026-11-16 on the emulator; FCM `start`
  via Postman pending)
  - [x] 6.1 `JourneyViewModel.startSequence`: Start / Simulate end the active journey (and a running simulation)
    first instead of showing `ActivityAlreadyRunning`; tapping Simulate again restarts it (SDK contract unchanged)
  - [x] 6.2 `FlightMessagingService`: `event=start` with an active journey ends it first, matching the production
    `FcmService.handleLiveUpdatePush` (read-only check 2026-11-16)
  - [x] 6.3 Fix: "Simulate Journey from Tel Aviv" ran the arrival sequence; the sequence is now passed in
  - [x] 6.4 Manual checks on the emulator (`planning/device-check.md`); found and fixed: Start / Simulate were disabled
    or off-screen (always enabled now, screen scrolls). The demo app has no unit-test setup
  - [x] 6.5 Demo language toggle (added 2026-11-16): `LocaleManager.applicationLocales` (existing `localeConfig`, no
    manifest/build/SDK change); `DemoSampleLocalizer` gives the samples' server-style labels and `language` in the app
    language (texts in the demo's `strings.xml`); the showing stage is re-posted after a switch

**Acceptance Criteria:** tapping Simulate twice restarts cleanly with no error; the Tel Aviv button shows TLV → JFK
(`D78-99`, `D3`); a second FCM `start` replaces the journey. With the toggle on EN or HE, every notification text
(title, content, pill) is in one language on both devices.

#### Spike: status badge as the large icon (2026-11-16)

- [x] Vector drawables from the user's SVGs: `img_live_update_status_on_time`, `img_live_update_status_delayed` (EN;
  `drawable-iw/` variants when provided); colours `live_update_status_*` (day + night)
- [x] `StageVisuals.showStatusBadge`: GATE_READY → DEPARTURE_PENDING, DEPARTED, LANDING_SOON
- [x] Mapper `setLargeIcon` (vector → bitmap) by `isDelayed(state)`, behind `LiveUpdateConfig.showStatusBadge` (default
  off); demo toggle "Status badge"; delay text unchanged
- [x] Tests `StatusBadgeTest` (2): badge per stage and delay state; flag off → no large icon
- [x] Run the tests: 54/54 pass (2026-11-16, JDK 21)
- [x] First device check (Pixel, S25+): shown in colour but center-cropped and soft → badge now drawn whole on a 16:9
  canvas at 2× (`BadgeFit` + `BadgeFitTest`, expected 55 tests)
- [x] Re-check on the Pixel and the S25+: looks good (user, 2026-11-16); 55/55 tests
- [ ] PM approval (optional feature, off by default); lock screen + dumpsys `PROMOTED_ONGOING` with the badge on

#### Fix: bar contrast on dark cards (2026-11-16)

- [x] `progress_bar_segment` and `live_update_icon_background` = `#4A86F5` in day and night (≥3:1 on light, dark and
  the Samsung lock screen); plane trackers use the colour resources. Colour-only change, no logic
- [ ] Re-check shade and lock screen, light and dark, Pixel and S25+

#### Feature verification (no branch)

- [ ] Full regression matrix on Pixel, Samsung S25+ and a ≤15 device (emulator API 37 done for groups 1–6,
  `planning/device-check.md`); delete intent on swipe vs timeout; Hebrew chip
  with a Latin gate on both A16+ devices.
- [x] "Ready to port" report (A7): `verification/ready-to-port.md` (2026-11-16): SDK diff vs `Android/elal-flight-sdk`
  (production added check-in queue load: merge into the builder), host API, manifest/permission changes, open risks.
  Final verification: `verification/final-verification.md` (⚠️ passed with issues: 3b not started, devices pending).
