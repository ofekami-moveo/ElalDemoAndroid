# Implementation Report: Demo restart and language (A6)

**Spec:** `2026-11-10-live-update-parity`
**Date:** 2026-11-16
**Task group:** 6
**Branch:** `update-LUN-ui` (per-group branch waived by the user)
**Status:** Done and checked on the emulator API 37 (`planning/device-check.md`). FCM `start` via Postman pending.
No SDK change, so no new unit tests (the demo app has no test setup; adding one is a build change).

## What changed

- **6.1 Restart:** `JourneyViewModel.startSequence(sequence)` cancels a running simulation, ends an active journey,
  then starts the new one. "Start Activity", both "Simulate" buttons and a second tap on the same button all go
  through it. `LiveUpdateManager.startJourney` still returns `ActivityAlreadyRunning` for other callers.
- **6.2 FCM start:** `FlightMessagingService` ends the active journey before an `event=start`, as the production
  `FcmService.handleLiveUpdatePush` does (read from `/Users/ofek.amirav/Android/app/src/main/java/services/FcmService.kt`).
- **6.3 Tel Aviv bug:** `simulateJourney()` set the TLV sequence and then called `startActivity()`, which overwrote it
  with the JFK one. The sequence is now a parameter (`runSimulation(sequence)`).
- **6.5 Language:** "Language: English (switch to עברית)" button → `LocaleManager.applicationLocales` (`en` / `iw`;
  Android 13+). `DemoSampleLocalizer` replaces exactly the labels the SDK samples set (desk / gate / zone /
  last-call titles, messages, departing-soon stage title, landing / welcome / carousel titles) with the demo's
  strings, and sets `language`. After the activity is recreated the screen re-posts the showing stage (only on a real
  switch; same stage, so no alert).

- **Found on the device (fixed, needs a rebuild):** the Start button was disabled while a journey was active and the
  Simulate buttons while simulating, so the new restart couldn't be reached; and the extra group 4–6 controls pushed
  the Simulate buttons off-screen. Start / Simulate are now always enabled and the screen scrolls
  (`verticalScroll`).

## Files

Demo only: `ui/journey/JourneyViewModel.kt`, `ui/journey/JourneyDemoScreen.kt`, `firebase/FlightMessagingService.kt`,
`DemoSampleLocalizer.kt` (new), `JourneyDemoScreen.kt` (always-enabled Start / Simulate, `verticalScroll`), strings `demo_language_switch`, `sample_*` (EN + HE).

## Manual checklist (6.4)

- Simulate twice / Start while active: restarts, no "already running" error.
- "Simulate Journey from Tel Aviv": TLV → JFK, check-in `D78-99`, gate `D3`.
- Language EN: title, content and pill all English; HE: all Hebrew (incl. SDK strings); switching mid-journey re-posts
  without alerting.
- FCM `start` while active (Postman): replaces the journey.

## Commit message (not committed)

```
Demo: restart on start, Tel Aviv simulator fix, EN/HE language toggle

Start / Simulate end the active journey first (like the production
FcmService), and FCM start does the same. "Simulate from Tel Aviv" now
runs the TLV sequence. Language toggle via LocaleManager; sample labels
come from the demo's strings in the app language.
```
