# Implementation Report: Schedule changes struck through (3b)

**Spec:** `2026-11-10-live-update-parity`
**Date:** 2026-11-16
**Task group:** 3b
**Branch:** `update-LUN-ui` (per-group branch waived by the user)
**Status:** Done on the emulator (plain-text default, 2 lines; `planning/device-check.md`). SDK builds;
`testDebugUnitTest` 52/52 pass (2026-11-16, JDK 21), also after the plain-text change. The first run failed
`gateChangeIsRecordedPerJourney`: a change back to the original gate was recorded as a new change; fixed by checking
the change-back first. App build and device checks pending.

## Decisions (2026-11-16)

- Group placement: after group 3 (it builds on `FlightDelayResolver.isDelayed(state)` and the content builder) and the
  only group that touches the data model, so it can be reviewed, reverted or left out of the port on its own.
- A later estimated departure counts as delayed, as well as `flightStatus` ≈ DELAYED (badge icon + amber bar).
- The in-flight clock (`DepartedClock`) and the timeout keep today's arrival; only the shown times change.

## Device result and follow-up decisions (2026-11-16)

- Emulator (Android 17): the promoted Live Update **does not draw `StrikethroughSpan`**; plain text renders. The
  expanded view shows **2 content lines**, so a third line was cut.
- Decided: **plain text by default** everywhere (`LiveUpdateConfig.scheduleChangeStyle = PLAIN_TEXT`); SPAN kept as an
  option and the demo toggle for the physical Pixel / S25+. **Change lines replace the stage line** (gate, departs or
  lands-at), max 2. Results in `planning/device-check.md`.

## What changed (visible, demo only until Phase B)

- With "Simulate delay" on, the content gains `Departs ~~17:00~~ 17:55` before takeoff; in DEPARTED `Lands at ~~23:12~~
  00:07` replaces "Lands at"; in LANDING_SOON it is added. The pill shows the estimated time.
- "Simulate gate change": `Gate ~~D3~~ E4` on gate stages; the pill shows `E4`.
- "Change style: text": `Departs 17:55 (was 17:00)` instead of the strike (to compare on the Pixel and the S25+).
- TalkBack reads the change as "17:55, was 17:00" (Hebrew: "17:55, במקום 17:00"), if the system honours `TtsSpan`.
- A real FCM payload has none of the new fields, so production output doesn't change.

## Files

SDK new: `model/ScheduleChangeStyle` (public), `model/SpanType`, `model/TextSpanSpec`, `model/GateRecord`,
`presentation/ScheduleChangeFormatter`, `presentation/StyledLine`, `presentation/ScheduleTimes`,
`presentation/GateChangeTracker`.
SDK changed: `model/JourneyData.kt` (4 optional fields), `model/LiveUpdateConfig` (+ `scheduleChangeStyle`),
`model/LiveUpdateContent` (+ `textSpans`), `presentation/LiveUpdateContentBuilder` (change lines, display times),
`presentation/ChipTextResolver` (display times), `presentation/FlightDelayResolver` (later estimate = delayed),
`presentation/StringKey`, `manager/AndroidStringProvider`, `manager/LiveUpdateNotificationMapper` (spans),
`manager/LiveUpdateManager` (gate record), `manager/JourneyPrefs` (gate record), strings `notif_change_*` (EN + HE).
Demo: `JourneyViewModel`, `JourneyDemoScreen`, strings `demo_gate_change`, `demo_strike_style_*` (EN + HE).
Docs: `planning/current-data-contract.md` (exception section).

## New public API (for the port report)

- `FlightContentState.scheduledDepartureTime`, `estimatedDepartureTime`, `scheduledArrivalTime`, `estimatedArrivalTime`
  (optional; demo-only until Phase B).
- `LiveUpdateConfig.scheduleChangeStyle`, enum `ScheduleChangeStyle`.

## Tests (3b.1, 6)

`ScheduleChangeTest` (6); helper `XmlStringProvider`; `FakeStringProvider` + 4 keys. Expected total: 52.

## Manual checklist

- Delay on at GATE_READY: `Departs ~~17:00~~ 17:55`, pill unchanged (gate); DEPARTURE_PENDING pill = estimated time;
  DEPARTED `Lands at ~~…~~ …`; HE order right-to-left.
- Gate change at BOARDING: `Gate ~~D3~~ E4`, pill `E4`; Next Stage (sample gate back) clears it.
- Style text: `Departs 17:55 (was 17:00)`.
- TalkBack on: what it reads for the change line; record per device.
- Delay off: no change lines; real FCM payload unchanged.

## Commit message (not committed)

```
Live Update: schedule and gate changes struck through (demo-only data)

Optional scheduled/estimated departure and arrival on FlightContentState
(demo-only until Phase B; parser untouched). Content shows "Departs
~~17:00~~ 17:55", "Lands at ~~23:12~~ 00:07" and "Gate ~~D2~~ D3"
(StrikethroughSpan + TtsSpan), or plain "17:55 (was 17:00)" via
LiveUpdateConfig.scheduleChangeStyle. Pill and public version use the
estimated times; a later estimate counts as delayed. 6 JVM tests.
```
