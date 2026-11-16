# Requirements: Live Update parity (LY 317)

## 0. Scope: UI only (decided 2026-11-10)

This spec changes **how the notification looks**, not the data. The FCM payload, the demo parser
(`FlightMessagingService.toFlightContentState`) and `FlightContentState` stay as frozen in
`planning/current-data-contract.md`. Consequences:
- No new fields (`gateOpenTime`, `version`, `delayMinutes`, `gateCloseTime`, absolute times). The server-contract
  items in §5 are recorded for a later data spec, not built here.
- Chronometer targets come only from existing fields: departure (`departureTime` + offset) and arrival
  (departure + offset + `flightDuration`). Gate-open and gate-close countdowns wait for the data spec.
- Delay is read from the existing `flightStatus` only.
- UI design for the pills and texts: Figma (link to be added to `planning/visuals/`).

## 1. Findings in the current code

| # | Finding | Where | Fixed in |
|---|---|---|---|
| F1 | After a process kill in DEPARTED the plane jumps back to the start: `restoreFromPrefs` → `setupRealtimeClocks` resets the start to *now* and the duration to the time remaining. A repeated DEPARTED push does the same. | `LiveUpdateManager.setupRealtimeClocks` | Group 2 |
| F2 | The bar goes backwards: LAST_CALL renders 88, DEPARTURE_PENDING (later) renders 38. | `LiveUpdateManager.buildProgressStyle` | Group 3 |
| F3 | The server already sends `gateOpenTime` (GATE_READY, BOARDING, DEPARTED) and `flightStatus: "ON-TIME"`; the parser drops `gateOpenTime`. | Postman collection, `FlightMessagingService` | Group 2 |
| F4 | `MainActivity` asks for `POST_NOTIFICATIONS` only on API ≥36. | `MainActivity` | Group 5 |
| F5 | The SDK manifest doesn't declare `POST_PROMOTED_NOTIFICATIONS`; only the demo host does. | SDK `AndroidManifest.xml` | Group 3 |
| F6 | `DebugNextStageReceiver` is `exported="false"`, so `adb shell am broadcast` can't reach it. | app manifest | Group 5 |
| F7 | Unused RemoteViews leftovers (`layout/notification_progress_bar.xml`, `notification_dot_*`, `notification_seg_*`) and unused strings. | SDK `res/` | Group 3 |
| F8 | `CLIENT_TECHNICAL_SPEC`, `JOURNEY_STATES_DATA`, `PROJECT_OVERVIEW` describe older models. | `Docs/` | Group 5 |
| F9 | The doc names `Settings.ACTION_MANAGE_APP_PROMOTED_NOTIFICATIONS`; the compileSdk-36 jar only has `ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS`. `Manifest.permission.POST_PROMOTED_NOTIFICATIONS` exists only in android-37. | platform jars | Use the API-36 constant; string literal in the manifest. |
| **F10** | **Samsung Galaxy S25+ (Android 17):** with the demo simulation in the background, `dumpsys notification --noredact` shows `flags=ONGOING_EVENT\|PROMOTED_ONGOING semFlags=0x2008`, so the notification **is promoted**. But the status bar shows only the El Al icon, with no pill and no heads-up; it just sits in the shade. Likely cause: no short critical text and `setShowWhen(false)`, so the pill has nothing to show. **Spike result (`spike-chip-results.md`):** confirmed on the Pixel (chip text → pill). On the S25+ no pill and no Now Bar even with chip text, chronometer, or both; One UI withholds them from this app (open Samsung risk). | device, 2026-11-10 | **Group 2, top priority** |

## 2. Decisions (user answers, 2026-11-10)

1. **Chip per phase:** the §3a table below.
   - Gate: localized label + gate (`Gate D3` / `שער D3`), from strings.xml (EN + HE), bidi-wrapped, only if the
     result is ≤7 characters. Otherwise the bare gate (`D3`).
   - CHECK_IN without counters: departure time (`17:00`).
   - DEPARTED: no chip text, so the chronometer shows.
   - Which one wins when both are set: test on **both** the Pixel and the Samsung S25+ (Android 17).
2. **Delay:** `flightStatus` compared case-insensitively with `"DELAYED"`, in **one** function so the field can
   change later. Colour: amber `#FEC630` (matches iOS), a named resource in the SDK `colors.xml` with a
   `values-night` variant. No hex in code. Exact values and `delayMinutes`/`isDelayed` are a server item (S2);
   not blocking group 3.
3. **Actions:** Boarding pass on the §3b stages (**Directions removed 2026-11-16:** not needed). Nothing on LANDED/BAGGAGE (TODO: baggage
   info). Deep links come from the host via `actionIntentProvider`; server-driven targets (S5) later. The demo host
   provides placeholder intents.
4. **Timeout:** `dismissalDate`, otherwise arrival + 4 h.
5. **≤15 fallback:** opt-in, off by default for the first release.
6. **version:** optional. A missing version is always accepted; a lower version than the saved one is dropped.
   Not blocking.
7. **InFlightService:** ~~retire it in favour of an inexact alarm, as its own sub-task.~~ **2026-11-16: kept** (the
   pill's time-left text needs ~1-min re-posts; revisit in Phase B). Manual tests: process kill
   mid-flight, reboot mid-flight, screen off 30+ min, Samsung battery optimisation. If it turns out risky, keep the
   service and revisit.
8. **CHECK_IN:** not promoted; shown as a regular ongoing notification. Promoted from GATE_READY onward.
9. **Build line:** `testImplementation("junit:junit:4.13.2")` in `elal-flight-sdk` only. Nothing else in build
   files.

## 3. Per-stage rendering (target)

Common: title `"{flightNumber} · {stageTitle}"` (SDK `status_*` fallback when blank), no time in the title.
`subText = "{origin} → {destination}"` (arrow from a string resource, mirrored for HE).

### 3a. Text and time

| Stage | Content text | Chip EN / HE | Chronometer target |
|---|---|---|---|
| CHECK_IN | title + check-in counters | counters if ≤7, else departure `HH:mm` | none |
| GATE_READY | message + gate | `Gate D3`/`שער D3` if ≤7, else `D3` | none (`gateOpenTime` deferred to the data spec) |
| BOARDING_PENDING | message | gate | none (deferred to the data spec) |
| BOARDING | today's | gate | `gateCloseTime` once sent |
| LAST_CALL | today's | gate | `gateCloseTime` once sent |
| DEPARTURE_PENDING | message + flight time | departure `HH:mm` | departure if ≥2 min ahead |
| DEPARTED | `Lands at 23:12` / `נוחתים ב-23:12` | **none** (chronometer) | arrival |
| LANDING_SOON | message | arrival `HH:mm` | arrival if still ahead |
| LANDED | title + carousel | `Belt 4`/`מסוע 4`, else destination code | none |
| BAGGAGE | today's | `Belt 12`/`מסוע 12` | none |
| COMPLETED | ends the journey | — | — |
| CONNECTION_ERROR | today's | `Offline` / `מנותק` | none |
| GENERAL_ERROR | today's | `Error` / `שגיאה` | none |

Chip rule: formatted label if ≤7 characters (bidi controls not counted), else the raw value if ≤7, else
`null` (icon only). The doc says full text only if "less than 7", so 7-character values are checked on device.

**Figma overrides (2026-11-10, `planning/visuals/figma-pills.md`):**
- Gate stages show the **bare gate** (`E4`), not `Gate E4`.
- DEPARTED shows the **time left as text** (`6:12h`, under an hour `45m`), refreshed on every self-update tick,
  instead of a system chronometer. Falls back to the arrival time when the duration is unknown.
- Delayed amber is **`#FFB936`** (replaces `#FEC630`), on-time blue `#1B358F`, applied via `setColor`.
  **Device result:** the pill background does not change; the system draws it. Delay must be shown another way
  (pill text, icon, progress-bar segment colour, content text). **Decision (2026-11-10): delayed icon + amber
  bar.** The pill keeps its normal text; its icon switches to a variant with an alert badge
  (`ic_live_update_chip_departure_delayed` / `ic_live_update_chip_flight_delayed`), and every progress-bar segment
  uses `progress_bar_segment_delayed` (`#FFB936`). Design to be told: the pill background and font can't be set on
  Android, so the Figma pill defines its text and icon only.
- Pill icon: departure arrow before takeoff, plane while airborne (DEPARTED, LANDING_SOON).
- HE duration has no unit (`11:52`) because `11:52 ש׳` is 8 characters; to review with design.

**Pill rule (spike 2026-11-10):** a promoted notification gets a pill only if it has content: `shortCriticalText`
or a shown `when`/chronometer. Every stage must therefore set one of them; a stage with neither shows the icon only.
The pill text comes from push fields (`gate`, `checkInCounters`, `departureTime`, `arrivalTime`,
`baggageCarousel`, `destination`) plus SDK labels (`notif_chip_gate`, `notif_chip_belt`, `notif_chip_offline`,
`notif_chip_error`); `baggageCarousel == "--"` counts as missing. The pill colour is system-drawn: `setColorized`
breaks promotion, so delay is shown in the pill text (e.g. the new time) and in the segment colour, not in the pill
colour.

### 3b. Visuals and behaviour (progress max 240, segments 38 / 48 / 154)

| Stage | Progress | Segment colour | Tracker | Actions | Promoted | Alert on entry |
|---|---|---|---|---|---|---|
| CHECK_IN | 0 | brand / delayed | check-in | — | **no** | yes |
| GATE_READY | 38 | brand / delayed | gate | Boarding pass | yes | yes |
| BOARDING_PENDING | 38 | brand / delayed | gate | Boarding pass | yes | no |
| BOARDING | 38 | brand / delayed | gate | Boarding pass | yes | yes |
| LAST_CALL | 88 | brand / delayed | takeoff | Boarding pass | yes | yes |
| DEPARTURE_PENDING | 88 | brand / delayed | takeoff | Boarding pass | yes | no |
| DEPARTED | 88 + fill | brand / delayed | cruise | — | yes | no |
| LANDING_SOON | 240 | brand | landing | — | yes | no |
| LANDED | 240 | brand | landing | — (TODO baggage info) | yes | yes |
| BAGGAGE | 240 | brand | baggage | — (TODO baggage info) | yes | no |
| CONNECTION_ERROR / GENERAL_ERROR | 0 | error grey | none | — | yes | no |

Start icon (origin) and end icon (destination) on every stage. Alerts also fire when the delay flag flips.

## 4. Clock design

- `whenDeviceMs = targetServerMs − (ServerClock.nowMs() − System.currentTimeMillis())`.
- Chronometer only if `targetServerMs − serverNow ≥ 2 min`; otherwise `setShowWhen(false)`.
- Recompute on every push, restore, progress tick and time/timezone change.
- Bar and countdown come from one `FlightTimeline` + one `serverNowMs` per build. Progress is
  `(serverNow − departedStartServerMs) / (arrivalServerMs − departedStartServerMs)` with `departedStartServerMs`
  persisted per `activityId` (fixes F1).
- ~~Progress-only tick, default 15 min, clamped 10–15 min.~~ **Changed 2026-11-16:** the pill shows the time left as
  text (Figma), not a chronometer, so the in-flight tick is 1% of the flight capped by
  `LiveUpdateConfig.progressRefreshIntervalMs`, **default 1 min, clamped 1–15 min** (today's cadence).

## 5. Server contract

| # | Field | Status | Blocks |
|---|---|---|---|
| S1 | `version` (Long, per `activityId`) | waiting for backend | nothing |
| S2 | `flightStatus` exact values; `delayMinutes` / `isDelayed` | waiting for backend | nothing (client uses `flightStatus` ≈ `DELAYED`) |
| S3 | `gateOpenTime` (sent, document it), `boardingTime`, `gateCloseTime` | partial | BOARDING/LAST_CALL chronometer |
| S4 | Absolute times (ISO with offset) + `arrivalTimezoneOffset` | requested | nothing |
| S5 | Server-driven deep links | later | nothing |
| S6 | `dismissalDate` always ISO with `Z` | requested | nothing (falls back to arrival + 4 h) |

## 6. Regression matrix (every group)

Devices: **Pixel (Android 16)** and **Samsung Galaxy S25+ (Android 17)**; plus one ≤15 device for group 5.

After each group, on both devices:
```
adb shell dumpsys notification --noredact | grep -A60 "id=7001"
```
Check `flags` (`PROMOTED_ONGOING` / `0x40000`), Samsung `semFlags`, `android.requestPromotedOngoing=true`,
`android.shortCriticalText`, `when=`, `android.chronometerCountDown`. Then look at: status-bar pill, Samsung Now Bar
on the lock screen, shade, lock screen, AOD; EN and HE; light and dark.
