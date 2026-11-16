# Device check: Live Update (Phase A)

Covers A0 (Figma pill) and groups 1–2, from 2026-11-16. The **Samsung S25+ (Android 17) is tested at the end of
Phase A** (user decision, 2026-11-16). The runs below are on the **emulator** (`sdk_gphone64_arm64`, API 37 /
Android 17), the only device connected; the physical Pixel (Android 16) is still to do.

## Emulator API 37, run 1: 2026-11-16 13:32–13:40 (user, logcat)

Build: groups 1 + 2 on `update-LUN-ui` (uncommitted). `adb devices` later confirmed it was the emulator.

| Check | Result | Evidence |
|---|---|---|
| Restore after process restart (journey in CHECK_IN) | ✅ restored, no crash; next stages post normally | process 7910 created 13:32:37, watchdog reset on start, GATE_READY update 13:32:41 |
| Stages GATE_READY → LANDING_SOON post without errors | ✅ | `updateJourney` per stage, no exceptions |
| DEPARTED timeline (TLV → JFK sample) | ✅ interval 209 m 37 s, arrival via "server ETA − device now" (sample has no offset/duration in DEPARTED) | 13:33:01 timeline |
| In-flight tick cadence (decision: 1 min) | ✅ configured: `Tick Interval 60s`, self-update started | 13:33:01; not observed firing (LANDING_SOON came 1 s later) |
| DEPARTED after the arrival time has passed (JFK → TLV sample, 5-min flight, DEPARTED at +6.5 min) | ✅ expected behaviour: interval 0, no ticking, plane parked at the end, pill falls back to the arrival time | 13:40:00 timeline |
| F1: bar continues after a process kill **in DEPARTED** | ✅ see run 2 | — |
| Pill text / icon per stage, title, route, `Lands at`, delay on/off | ⏳ no visual results yet | — |
| Lock screen (public version) | ⏳ | — |
| dumpsys (`PROMOTED_ONGOING`, `shortCriticalText`, `subText`, public version) | ✅ see run 2 | — |

## Emulator API 37, run 2: 2026-11-16 13:45–13:47 (Claude via adb, F1 check)

Setup: the emulator had restored an old snapshot (app installed 2026-11-10, original timeline code), so the
13:32 Android Studio build (`app-debug.apk`, newer than the group 2 sources) was installed with `adb install -r -t`.
Then End → Start Activity (JFK → TLV sample, lands 5 min after start) → Next Stage ×6 to DEPARTED → Home.

| Check | Result | Evidence |
|---|---|---|
| Promoted | ✅ `flags=ONGOING_EVENT\|ONLY_ALERT_ONCE\|NO_CLEAR\|FOREGROUND_SERVICE\|PROMOTED_ONGOING`, `requestPromotedOngoing=true` | dumpsys 13:46 |
| Title / route / content (2.4) | ✅ `LY 316 · On the way`, `JFK → TLV`, `Lands at 13:50` | dumpsys |
| Pill text (DEPARTED time left) | ✅ `5m` with 4 m 23 s left (rounds up), `4m` after the restart; demo card showed the same | dumpsys, demo card |
| Public version (2.10) | ✅ present, `vis=PRIVATE` main; public text `Arrives 13:50`, no PNR | dumpsys |
| Tick | ✅ every ~2.8 s (1% of a 4 m 40 s flight, under the 1-min cap) | `FLIGHT PROGRESS` lines |
| **F1: process kill in DEPARTED** | ✅ **fixed**: `am kill` didn't kill (foreground service), so the process was killed with `run-as … kill -9` at 13:46:35; it restarted at 13:46:36 with `Start : 11:46:03 GMT (persisted)`, same 4 m 40 s interval, progress continued at 12% (was 11% before the kill), bar 97 → 112 | logcat pid 6000 |

Not covered by run 2: visual check of the shade / lock screen, delay on, HE, and the 1-min tick on a real-length flight.
Observation from the first attempt (13:42): the app showed a restored LANDED journey with no notification 7001
posted. Not reproduced in run 2 (the notification survived the kill); likely the snapshot / force-stop state.
Watch for it in group 4 (restore and lifecycle).

### Notes from the log (not group 1–2 regressions)

- The timeline prints `Tick Interval : 1s` when there is nothing to animate; no tick is scheduled then. Cosmetic.
- The demo's "Start Activity" reports `trigger=push` in analytics (default `TriggerSource.PUSH`). Demo-only.
- The demo host logs the FCM token (`MainActivity`) and the analytics payload incl. the `pnr` field
  (`ElAlDemoApplication`). Demo-only; note for the port report that a host listener must not log the PNR.

## Emulator API 37, group 3: 2026-11-16 13:56–13:59 (Claude via adb)

Build: Android Studio build installed 13:56:06 (after the group 3 sources). Start Activity (JFK → TLV) → Next Stage
through every stage; dumpsys per stage; shade screenshots in `verification/screenshots/g3-*.png`.

| Stage | Promoted | Alerts (no `ONLY_ALERT_ONCE`) | Progress | Pill | Expected (§3b) |
|---|---|---|---|---|---|
| CHECK_IN | **no** (`requestPromotedOngoing=false`) | yes | 0 | (`B12-18` set, not shown: not promoted) | ✅ |
| GATE_READY | yes | yes | 38 | `B7` | ✅ |
| BOARDING_PENDING | yes | no | 38 | `B7` | ✅ |
| BOARDING | yes | yes | 38 | `B7` | ✅ |
| LAST_CALL | yes | yes | 88 | `B7` | ✅ |
| DEPARTURE_PENDING | yes | no | **88** (F2 fixed) | `13:57` | ✅ |
| DEPARTED | yes | no | 88 → | `5m` | ✅ |
| LANDING_SOON | yes | no | 240 | `14:02` | ✅ |
| LANDED | yes | yes | 240 | `Belt 3` | ✅ |
| BAGGAGE | yes | no | 240 | `Belt 12` | ✅ |

Screenshots (light, system EN):
- `g3-gate-ready.png`: origin ring, gate-door tracker at the end of seg0, destination pin.
- `g3-gate-ready-delayed.png`: segments amber (rendered as a darker gold by the system), accent `0xffffb936`;
  no alert-once flag on the toggle (delay flip alerts) ✅.
- `g3-departed.png`: plane tracker at the start of the flight segment; `Lands at 14:03`.
- `g3-baggage.png`: suitcase tracker at the end.
- `g3-connection-error.png`: grey segments, no tracker, origin/destination icons still shown ✅.
- `g3-check-in.png`: collapsed regular notification (not promoted), no bar in the collapsed view.

**3.7 `SEG0_COMPENSATION` with the start icon:** seg0 (38) and seg1 (48) render at the same visible width next to
the start ring, and the gate tracker sits at the end of seg0. The compensation still does its job; keep it.

Not covered (need system settings the user changes): dark theme, system RTL (gate icon mirroring), HE locale.
The Hebrew text in the shade comes from the demo's Hebrew sample labels (task 6.5).

## Emulator API 37, group 4: 2026-11-16 14:14–14:17 (Claude via adb)

Build: Android Studio build installed 14:14:13 (after the group 4 sources). Start Activity (JFK → TLV) → Next Stage.

| Check | Result | Evidence |
|---|---|---|
| Actions per stage | ✅ CHECK_IN `Directions`; GATE_READY `Boarding pass, Directions`; BOARDING_PENDING → DEPARTURE_PENDING `Boarding pass`; DEPARTED none | dumpsys `actions=` |
| Delete intent + timeout on every post | ✅ `deleteIntent` set; `timeout≈PT4H5M` (arrival in 5 min + 4 h) | dumpsys |
| Boarding pass tap | ✅ trampoline → `live_activity_action_clicked … action=boarding_pass` → demo resumed with the placeholder toast | logcat, top activity |
| Directions tap | ✅ `action=directions` → Google Maps opened (**button removed later on 2026-11-16**) | logcat, top activity |
| **Real swipe in the shade** | ✅ the promoted Live Update is dismissible; delete intent → `onUserDismissed`, `live_activity_dismissed` | logcat 14:16:14 |
| No repost after the swipe: 5 updates (to DEPARTED) | ✅ no notification; `updateJourney: … dismissed — not reposting` | dumpsys, logcat |
| … in-flight ticks | ✅ none started; no notification | logcat |
| … process kill + reopen | ✅ `restoreFromPrefs: … dismissed — restored without ticks`; no notification | logcat |
| End → Start | ✅ new notification posts (CHECK_IN, `Directions`) | dumpsys |
| Promotion card | ✅ "Allowed"; "Open settings" opens the app's notification page with the **Live updates** toggle | UI dump |

Not covered: timeout actually firing (needs 4 h, or a short `dismissalDate` via FCM); restore after the end
(same); a real FCM push after a swipe (the demo buttons call the same `updateJourney`).

## Emulator API 37, group 5: 2026-11-16 14:24–14:25 (Claude via adb)

Build: Android Studio build installed 14:24:14 (after the group 5 sources).

| Check | Result | Evidence |
|---|---|---|
| Permissions | ✅ `POST_NOTIFICATIONS` and `POST_PROMOTED_NOTIFICATIONS` granted | `dumpsys package` |
| Fallback toggle on Android 17 | ✅ no effect: with "Android 15 fallback: on", GATE_READY is still `PROMOTED_ONGOING`, `requestPromotedOngoing=true`, pill `B7`, 2 actions | dumpsys |
| Debug receiver exported (debug build) | ✅ `adb shell am broadcast -a com.elal.demoapp.debug.NEXT_STAGE -n com.elal.demoapp/.debug.DebugNextStageReceiver` → `result=0`, GATE_READY → BOARDING_PENDING | logcat, dumpsys |

Toggle set back to off afterwards. **Not covered:** the fallback itself (needs an Android 15 or older device or
emulator) and a release build keeping the receiver unexported.

## Emulator API 37, group 6: 2026-11-16 14:34–14:37 (Claude via adb), build 14:34

| Check | Result | Evidence |
|---|---|---|
| Language EN → HE mid-journey (GATE_READY) | ✅ everything Hebrew: title `LY 317 · השער מוכן` (SDK stage label), content `שער: D3`, route `TLV ← JFK`, buttons `כרטיס עלייה` / `הוראות הגעה`; re-post has `ONLY_ALERT_ONCE` (no alert) | dumpsys, `verification/screenshots/g6-hebrew.png` |
| HE through to DEPARTED | ✅ `LY 317 · בדרך`, `נוחתים ב-18:04`, pill `3:28` (no unit, as decided) | dumpsys |
| Back to EN mid-flight | ✅ `LY 317 · On the way`, `Lands at 18:04`, pill `3:28h` | dumpsys |
| Start / Simulate reachable | **found and fixed:** Start was disabled while a journey was active, Simulate while simulating, and the new controls pushed Simulate off-screen. Now always enabled; the screen scrolls | UI dump |
| "Simulate Journey from Tel Aviv" data (6.3) | ✅ `LY 317 · Check-in`, `D78-99`, `TLV → JFK` (was the JFK sequence) | dumpsys 14:37:54 (after the fix) |
| Simulate twice (6.1) | ✅ second tap: `endJourney` → `startJourney LY 317`, no error | logcat 14:37:58 |
| New York while simulating (6.1) | ✅ `endJourney` → `startJourney LY 316`, `JFK → TLV`, `B12-18` | logcat 14:38:03 |
| Start while active (6.1) | ✅ `endJourney` → `startJourney LY 316`; no "already running" on screen | logcat 14:38:09, UI dump |
| FCM `start` while active (6.2) | ⏳ needs a real push (Postman) | — |

The shade is drawn LTR because the emulator's system language is English; the Hebrew lines read correctly
right-to-left. A Hebrew system locale (RTL shade) is still to check on a phone.

## Emulator API 37, group 3b: 2026-11-16 14:55–14:58 (Claude via adb), build 14:55

Start Activity (JFK → TLV), "Simulate delay" on (+55 min).

| Check | Result | Evidence |
|---|---|---|
| Change lines in the posted text | ✅ `SpannableString`: CHECK_IN `B12-18 / Departs 14:56 15:51`; GATE_READY `Gate: B7 / Departs …`; DEPARTURE_PENDING `… / Departs …`; DEPARTED `Lands at 15:01 15:56` | dumpsys |
| Pill uses the estimated time | ✅ DEPARTURE_PENDING `15:51` (estimated) | dumpsys |
| Public version | ✅ `Gate B7 · Departs 15:51`, `Arrives 15:56` | dumpsys |
| Gate change | ✅ `Gate B7 E4` line, pill `E4`; tapping again (back to B7) clears it | dumpsys |
| Plain-text style | ✅ `Gate E4 (was B7)`, `Departs 15:51 (was 14:56)`, plain `String` | dumpsys, `g3b-boarding-text-style.png` |
| **Strikethrough drawn** | ❌ **not rendered** on the promoted Live Update: `Lands at 15:01 15:56` with no line (`g3b-departed-strike.png`) | screenshots |
| **Three lines** (stage line + gate change + departs) | ❌ the expanded view shows 2 lines: `E4 / Gate B7 E4…`, the departs line is cut | `g3b-boarding-gate-change-strike.png` |
| Pill vs content in DEPARTED | ⚠️ pill `5m` runs to the original arrival (clock kept, decided), content says `Lands at … 15:56` | dumpsys |
| Unpromoted CHECK_IN with a strike | ⏳ couldn't expand it via adb | — |
| Hebrew, TalkBack | ⏳ (HE strings covered by tests) | — |

**Result: Android 17 (emulator) drops `StrikethroughSpan` in promoted notifications**, so the SPAN style shows two
times side by side.

**Decided 2026-11-16:** plain text everywhere by default (`LiveUpdateConfig.scheduleChangeStyle = PLAIN_TEXT`:
"Departs 15:51 (was 14:56)" on every stage and Android version); SPAN stays as a config option and the demo toggle
("Change style") for the physical Pixel and S25+. When a change exists, the change lines replace the stage line (max 2).

**Re-check after the change (emulator, 15:03–15:05):** BOARDING with delay + gate change shows two full lines
`Gate E4 (was B7)` / `Departs 15:58 (was 15:03)` (`g3b-plain-boarding.png`); DEPARTED `Lands at 16:03 (was 15:08)`;
public `Gate E4 · Departs 15:58`; the "Change style" toggle still switches to the SPAN version. ✅

| Physical device | SPAN drawn? | Plain text OK? | TalkBack reads | Notes |
|---|---|---|---|---|
| Pixel (Android 16) | | | | (user to add) |
| Samsung S25+ (Android 17) | ❌ no: old and new time side by side, no line (user, 2026-11-16 15:06–15:12) | not checked yet | not checked | Build predates the Directions removal (`actions=2` at GATE_READY in the log); the log has no notification text, so the active style is from the user's report |

## Spike: coloured status badge as the large icon (2026-11-16)

Behind `LiveUpdateConfig.showStatusBadge` (default off) and the demo toggle "Status badge". `setLargeIcon` with the
user's SVGs (`img_live_update_status_on_time` green "ON TIME", `img_live_update_status_delayed` orange "DELAYED";
EN only, `drawable-iw/` when Hebrew ones arrive), rendered at 2× their 52×17 dp. Shown on GATE_READY →
DEPARTURE_PENDING, DEPARTED, LANDING_SOON; DELAYED when `isDelayed(state)`. The delay also stays in the text.

**Checklist (each phone):** End → toggle "Status badge: on" → Start Activity → Next Stage to GATE_READY, then
BOARDING with "Simulate delay" off and on, then DEPARTED. At each, look at the shade (collapsed and expanded) and the
lock screen, then run:

```
adb shell dumpsys notification --noredact | grep -A80 "id=7001" | grep -E "flags=|android.largeIcon|android.title="
```

| Check | Pixel (Android 16) | Samsung S25+ (Android 17) |
|---|---|---|
| Badge shown? Where: shade collapsed / expanded / lock screen | ✅ shade (card with bar), right side; lock screen not reported | ✅ shade, left side (Hebrew RTL); lock screen not reported |
| In colour (green / orange), or tinted / greyed by the system? | ✅ in colour (green) | ✅ in colour (orange) |
| Shape: whole pill, or cropped into a circle / square? | ❌ **center-cropped** into a rounded ~16:9 frame: "ON TIM", pill ends cut | ❌ same: "DELAYE" |
| Readable at its size ("ON TIME" / "DELAYED")? | ❌ cut and soft (upscaled) | ❌ cut and soft (upscaled) |
| Switches ON TIME ↔ DELAYED with the delay toggle | | |
| None on CHECK_IN, LANDED, BAGGAGE | | |
| dumpsys still `PROMOTED_ONGOING` with the badge on | | |
| Pill still shown (status bar) with the badge on | | |
| Notes | screenshot 2026-11-16 (user) | screenshot 2026-11-16 (user); `(במקום 16:04)` plain text renders ✅ |

**Cause:** the system draws the large icon in a fixed frame (about 16:9, rounded corners, scaled to fill: center-crop). The badge is ~3:1 (52×17), so its ends are cut, and the 17-dp-tall bitmap is scaled up to the frame's height, so it looks soft.

**Fix (2026-11-16):** the badge is now drawn whole and centred (6 % padding) on a transparent 16:9 canvas at 2× the
frame's size (48 dp tall), so the system neither crops nor upscales it (`BadgeFit`, `BadgeFitTest`). The pill is
about 28 dp tall inside the frame. Re-check on both phones: whole "ON TIME" / "DELAYED", sharp, rounded ends visible.

**Re-check (user, 2026-11-16):** ✅ looks good on the Pixel and the S25+ after the fix. Lock screen and dumpsys
`PROMOTED_ONGOING` with the badge on still to confirm. Next: PM approval as an optional, configurable feature.

## Dark card contrast (S25+ screenshot, user, 2026-11-16)

The flight path was barely visible on a dark card (Samsung lock screen): the bar, start/end icons and tracker circles
used the day navy `#1B358F` (~1.1–1.3:1 on dark). Colours are resolved when the notification is posted, and the lock
screen draws a dark card even in light mode, so the night variant isn't always used.

**Fix (decided 2026-11-16):** `#4A86F5` in both themes for `progress_bar_segment` and `live_update_icon_background`;
the plane trackers (`ic_airplane_*_left_2`) now use the colour resources instead of a hard-coded `#1B358F` / `#ffffff`.
Contrast: light card 3.1:1, dark card 4.1:1, Samsung lock screen 3.3:1. Amber (delay) and grey (error) unchanged.
To re-check: shade and lock screen, light and dark, on both phones.

## Samsung S25+

Planned for the end of Phase A (feature verification).

## A0 results table (to fill)

| Check | Pixel (on time) | Pixel (delayed) | S25+ (on time) | S25+ (delayed) |
|---|---|---|---|---|
| Promoted (dumpsys) | | | end of Phase A | end of Phase A |
| `shortCriticalText` present | | | | |
| Pill shown | | | | |
| Pill text matches the table | | | | |
| Icon arrow → plane | | | | |
| Badge icon when delayed | n/a | | n/a | |
| Amber bar in shade | n/a | | n/a | |
| Shade looks right (EN / HE) | | | | |
| Lock screen | | | | |
