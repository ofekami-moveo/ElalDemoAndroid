# Implementation Report: Progress visuals

**Spec:** `2026-11-10-live-update-parity`
**Date:** 2026-11-16
**Task group:** 3
**Branch:** `update-LUN-ui` (per-group branch waived by the user)
**Status:** SDK builds; `testDebugUnitTest` 34/34 pass (2026-11-16, JDK 21). The demo build and the device checks
(incl. 3.7) are pending.

## What changed (visible)

- **Bar icons:** origin ring at the start and destination pin at the end of every stage; a tracker per phase:
  ticket (check-in), gate door (gate stages), plane takeoff / cruise / landing, suitcase (baggage).
- **F2 fixed:** DEPARTURE_PENDING sits at 88 like LAST_CALL, so the bar never goes back.
- **Colours:** amber segments when delayed, only until landing; grey segments on error stages.
- **Promotion:** CHECK_IN is a regular ongoing notification (no pill, no Live Update chip); promoted from GATE_READY.
- **Alerts:** sound / heads-up only on entering CHECK_IN, GATE_READY, BOARDING, LAST_CALL, LANDED, and when the
  delay flips; other stages, re-posts and ticks update silently. Before this, every stage change alerted.

## Files

New: `model/PostedState`, `presentation/AlertPolicy`, `presentation/SegmentStyle`; drawables `ic_progress_start`,
`ic_progress_end`, `ic_progress_tracker_check_in`, `ic_progress_tracker_gate`, `ic_progress_tracker_baggage`.
Changed:
- `presentation/StageVisualCatalog` + `StageVisuals`: §3b table (position, tracker, segment style, airborne,
  alert on entry, promoted).
- `presentation/LiveUpdateContentBuilder`: segment colour, start/end icons, `requestPromoted`, alert decision,
  `postedState`; delay limited to delay-aware stages.
- `presentation/FlightDelayResolver`: `isDelayed(state)` entry point.
- `model/IconRole`, `ColorRole`, `LiveUpdateProgress`, `LiveUpdateContent`.
- `manager/LiveUpdateNotificationMapper`: `setRequestPromotedOngoing(content.requestPromoted)`, start/end icons, new
  drawables and the error colour.
- `manager/LiveUpdateManager`: keeps `lastPosted` (reset on start and end).
- Colours: `progress_bar_segment_error` (`#9AA0A6` / night `#5F6368`), `live_update_icon_background` (`#1B358F`,
  both themes, like the plane trackers), `live_update_icon_foreground` (`#FFFFFF`).
- **Manifest (approved 2026-11-16):** `elal-flight-sdk/src/main/AndroidManifest.xml` +
  `<uses-permission android:name="android.permission.POST_PROMOTED_NOTIFICATIONS" />`.
- **Deleted (F7, approved):** `layout/notification_progress_bar.xml`, `drawable/notification_dot_active|inactive`,
  `drawable/notification_seg_active|inactive`, and 22 unreferenced strings in `values/` and `values-iw/`
  (`notif_content_check_in|gate_ready|boarding|last_call|departed|departed_minutes|landing_soon|arrival_baggage`,
  `notif_chip_check_in|boarding|departed|arrival`, all `notif_statusbar_*`). `status_*` kept (the demo app uses
  the same names). `ic_elal_chip` is now unused too; left for now.

## Open points

- Icons are Material placeholders (Apache 2.0); design to supply the El Al set.
- After a restore the first post counts as entering the stage, so it can alert once if that stage alerts on entry.
- 3.7 (`SEG0_COMPENSATION` with start icons) needs a device look.

## Tests (3.1, 7)

`StageVisualsTest` (7). Updated: `LiveUpdateContentBuilderTest` (DEPARTURE_PENDING 88, error segments, tracker check
moved to the new test). Expected total: 34.

## Manual checklist

- Each stage, light and dark, EN and HE: start/end icons, tracker per phase, gate door mirrored in HE.
- Delay on: amber until DEPARTED, blue from LANDING_SOON; errors grey.
- CHECK_IN: no pill / not promoted in dumpsys; GATE_READY: `PROMOTED_ONGOING`.
- Sound / heads-up only on the alerting stages and on the delay toggle.
- 3.7: with the start icon, is the first segment's gap still right? Record the result.

## Commit message (not committed)

```
Live Update: per-phase icons, delay/error colours, promotion and alerts

Origin/destination icons and a tracker per phase; DEPARTURE_PENDING at 88
(F2). Amber segments while delayed before landing, grey on errors.
Promotion from GATE_READY (CHECK_IN regular ongoing). Alerts only on the
§3b stages and delay flips. SDK manifest declares
POST_PROMOTED_NOTIFICATIONS; F7 RemoteViews leftovers removed. 7 JVM tests.
```
