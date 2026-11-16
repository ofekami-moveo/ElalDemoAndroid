# Implementation Report: Legacy fallback and demo controls

**Spec:** `2026-11-10-live-update-parity`
**Date:** 2026-11-16
**Task group:** 5
**Branch:** `update-LUN-ui` (per-group branch waived by the user)
**Status:** SDK builds; `testDebugUnitTest` 46/46 pass (2026-11-16, JDK 21). The app build and the device checks are
pending (the fallback itself needs an Android 15 or older device).

## Decisions (2026-11-16)

- **5.5 approved:** debug-only manifest exporting `DebugNextStageReceiver`.
- **5.6 scope approved:** rewrite `PROJECT_OVERVIEW.md`; "outdated" banner on the two large docs (no rewrite).

## What changed

- **Fallback (5.2):** `LiveUpdateConfig.enableLegacyFallback` (default false). `LiveUpdateModeResolver`: Android 16+ →
  PROMOTED; older with the flag → FALLBACK (same content, `requestPromoted = false`, never the foreground service);
  older without it → OFF (all entry points no-op, as before). `LiveUpdateConfig` now rejects a non-positive
  refresh interval.
- **Permissions (5.3, F4):** `POST_NOTIFICATIONS` asked from Android 13; `POST_PROMOTED_NOTIFICATIONS` from 16.
- **Demo (5.4):** "Android 15 fallback: off / on" toggle; applies to the next start; no effect on 16+.
- **Debug receiver (5.5, F6):** `app/src/debug/AndroidManifest.xml` (`tools:replace="android:exported"`). Release
  keeps `exported="false"`.
- **Docs (5.6, F8):** `PROJECT_OVERVIEW.md` rewritten for today's code; banners on `Docs/CLIENT_TECHNICAL_SPEC.md`
  and `Docs/JOURNEY_STATES_DATA.md`.

## Files

SDK: `presentation/LiveUpdateMode` (new), `presentation/LiveUpdateModeResolver` (new), `model/LiveUpdateConfig`
(+ flag, validation), `presentation/LiveUpdateContentBuilder` (`promotionAllowed`), `manager/LiveUpdateManager`
(mode, no FGS in fallback, class KDoc).
Demo: `MainActivity`, `JourneyViewModel`, `JourneyDemoScreen`, strings `demo_fallback_*` (EN + HE),
**`app/src/debug/AndroidManifest.xml` (new)**.
Docs: `PROJECT_OVERVIEW.md`, banners in two `Docs/` files.

## New public API (for the port report)

- `LiveUpdateConfig.enableLegacyFallback`; `LiveUpdateConfig` throws on a non-positive `progressRefreshIntervalMs`.

## Tests (5.1, 4)

`LegacyFallbackTest` (4). Expected total: 46.

## Manual checklist

- Android 16+ (emulator): toggle on/off → no difference; still promoted.
- Android 15 or older device / emulator: toggle off → Start shows nothing; toggle on → plain ongoing progress
  notification with the same title / text / bar, no pill, no foreground service in DEPARTED
  (`dumpsys activity services com.elal.demoapp` empty).
- Debug build: `adb shell am broadcast -a com.elal.demoapp.debug.NEXT_STAGE -n com.elal.demoapp/.debug.DebugNextStageReceiver`
  advances an active journey.

## Commit message (not committed)

```
Live Update: opt-in Android 15 fallback, permissions, debug receiver, docs

LiveUpdateConfig.enableLegacyFallback (off by default) shows the same
content unpromoted and without the foreground service below Android 16.
POST_NOTIFICATIONS asked from Android 13 (F4). Debug-only manifest exports
DebugNextStageReceiver (F6). PROJECT_OVERVIEW rewritten; outdated banners
on the old state docs (F8). 4 JVM tests.
```
