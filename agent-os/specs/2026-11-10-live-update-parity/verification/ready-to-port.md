# Ready to port: Live Update parity, Phase A → production

**Date:** 2026-11-16 · **Source:** `ElalDemoAndroid` on `update-LUN-ui` (uncommitted, on top of `8655dc1`)
**Target:** `/Users/ofek.amirav/Android` (`:elal-flight-sdk` + host `app`), read-only until Phase A sign-off.
Compared read-only against the production checkout on `moveo/feature/debug-tools-logger/dot` (last SDK commit
`6d5f84c23`, 2026-11-11).

## Verdict

**Ready to port with one manual merge and two decisions.** The SDK builds, 52/52 JVM tests pass, and groups 1–6 are
checked on the API 37 emulator. Before the port:

1. **Merge production's check-in waiting-times change** into the new content builder (section 2). It is the only
   conflict.
2. **Group 3b** (delay / gate change struck through) was implemented after this report's first draft
   (`implementation/3b-schedule-changes-implementation.md`). Its four schedule fields are **demo-only until Phase B**:
   port them (harmless, null in real payloads) or leave the group out.
3. Run the **Samsung S25+** and **physical Pixel (Android 16)** checks (section 6) before sign-off.

---

## 1. SDK diff: demo vs production

### Production changed since the demo's base (`8655dc1`)

Production has one SDK feature the demo doesn't have: **check-in queue load** (Day of Travel v2, PR 1368).

| File | Production change | Port action |
|---|---|---|
| `model/QueueLoad.kt` | New enum `LOW / MEDIUM / HIGH` + `fromString` | Copy as is |
| `model/JourneyData.kt` | `FlightContentState` + 6 optional fields: `checkInCounterNumbers`, `checkInZone`, `checkInQueueLoad`, `checkInQueueLoadText`, `selfServiceQueueLoad`, `selfServiceQueueLoadText` | Keep them (additive, default null) |
| `manager/LiveUpdateManager.kt` | `buildCheckInContent` + `joinWithDot` (CHECK_IN content: "desks · traffic" and a second "self-service · traffic" line) | **Move into `LiveUpdateContentBuilder.buildText(CHECK_IN)`**, using `BidiWrapper` instead of `BidiFormatter`; the separator `·` as a constant. Add a JVM test. `LiveUpdateManager` no longer builds content |
| `sample/SampleFlightData.kt` | CHECK_IN samples carry the queue fields | Keep production's samples |

The CHECK_IN pill still uses `checkInCounters` (`D78-99`), so the queue fields don't affect it.

### Demo changes to bring over (everything else)

- **New, pure (`presentation/`, JVM-tested):** `LiveUpdateContentBuilder`, `StageVisualCatalog` / `StageVisuals` /
  `SegmentStyle` / `ProgressPosition`, `ProgressCalculator`, `FlightTimeline`, `DepartedClock`,
  `TickIntervalCalculator`, `TimeoutCalculator`, `AlertPolicy`, `DismissPolicy`, `ActionFilter`,
  `PromotionStatusResolver`, `LiveUpdateModeResolver` / `LiveUpdateMode`, `ChipTextResolver` / `ChipLabels`,
  `FlightDelayResolver`, `StringProvider` / `StringKey`, `BidiWrapper`.
- **New models:** `LiveUpdateContent`, `LiveUpdateProgress`, `SegmentSpec`, `IconRole`, `ColorRole`, `TimeSnapshot`,
  `PostedState` (internal); `LiveUpdateConfig`, `LiveUpdateAction`, `PromotionStatus` (public).
- **New Android:** `LiveUpdateNotificationMapper`, `LiveUpdateDismissReceiver`, `PromotionDiagnostics`,
  `AndroidStringProvider`, `AndroidBidiWrapper`, `AndroidChipLabels`.
- **Changed:** `LiveUpdateManager` (content moved out; departed clock persisted; dismissal; timeout; promotion status;
  fallback mode), `JourneyPrefs` (departed start, dismissed id, journey end), `InFlightService` (stops for a
  dismissed journey), `LiveActivityClickActivity` (`EXTRA_ACTION`), `FlightAnalyticsListener`,
  `AnalyticsPayloadBuilder`.
- **Resources:** 9 new drawables (`ic_live_update_chip_*` ×4, `ic_progress_start|end`,
  `ic_progress_tracker_check_in|gate|baggage`); colours `progress_bar_segment_delayed|error`,
  `live_update_chip_on_time|delayed`, `live_update_icon_background|foreground` (day + night); strings
  `notif_title`, `notif_subtext_route`, `notif_content_lands_at`, `notif_public_*`, `notif_stage_*`,
  `notif_chip_belt|duration_*`, `notif_action_*` (EN + HE).
- **Deleted (F7):** `layout/notification_progress_bar.xml`, `drawable/notification_dot_*`,
  `drawable/notification_seg_*`, 22 unused strings (`notif_content_check_in|gate_ready|boarding|last_call|departed|
  departed_minutes|landing_soon|arrival_baggage`, `notif_chip_check_in|boarding|departed|arrival`, all
  `notif_statusbar_*`). **The production host references none of them** (checked).
- **Tests:** `src/test/` (17 test classes, 52 tests) + `testImplementation("junit:junit:4.13.2")` in
  `elal-flight-sdk/build.gradle.kts` (the only build-file change).

---

## 2. Public API: what the host must / may call

**Nothing breaks.** The production host uses `startJourney`, `updateJourney`, `endJourney`, `restoreFromPrefs`,
`isActive`, `stateFlow`, `analyticsListener` and `clickIntentProvider`, all unchanged. New listener methods have
default bodies.

| API | Required? | What the host does |
|---|---|---|
| `LiveUpdateManager.actionIntentProvider: ((Context, LiveUpdateAction, FlightContentState) -> Intent?)?` | **Needed for the button** | Return the boarding-pass screen intent; `null` hides it. Unset = no button |
| `FlightAnalyticsListener.onLiveActivityActionClicked`, `onLiveActivityDismissed` | Optional | Forward to analytics (`action` param on clicks) |
| `LiveUpdateManager.config = LiveUpdateConfig(...)` | Optional | `progressRefreshIntervalMs` (default 1 min, clamped 1–15 min, must be > 0); `enableLegacyFallback` (default false) |
| `LiveUpdateManager.promotionStatus` / `refreshPromotionStatus(ctx)` / `openPromotionSettings(ctx)` | Optional | Settings UI: "Live Updates off → open settings" |
| `LiveUpdateManager.pillTextFlow` | Optional | Diagnostics only |
| `LiveUpdateManager.onUserDismissed(ctx, activityId)` | No | Called by the SDK's receiver |
| `LiveUpdateManager.simulateUserDismiss(ctx)` | No | Demo / QA helper |

Host behaviour already in production: `FcmService` ends the active journey before an `event=start` (the demo now does
the same); the host already declares `POST_NOTIFICATIONS` and `POST_PROMOTED_NOTIFICATIONS`.

**To check in the host:** that `POST_NOTIFICATIONS` is requested at runtime from Android 13 (F4 fixed this in the demo).

---

## 3. Manifest, permissions, build

| Where | Change | Approved |
|---|---|---|
| SDK `AndroidManifest.xml` | `+ <uses-permission android:name="android.permission.POST_PROMOTED_NOTIFICATIONS" />` (merges with the host's existing line) | 2026-11-16 |
| SDK `AndroidManifest.xml` | `+ <receiver android:name=".manager.LiveUpdateDismissReceiver" android:exported="false" />` | 2026-11-16 |
| SDK `build.gradle.kts` | `+ testImplementation("junit:junit:4.13.2")` | 2026-11-10 |
| SDK | `InFlightService` + `FOREGROUND_SERVICE_SPECIAL_USE` **kept** (4.7 deferred) | 2026-11-16 |
| Demo only | `app/src/debug/AndroidManifest.xml` exports `DebugNextStageReceiver` in debug | 2026-11-16 (don't port) |

---

## 4. Behaviour changes users will see

- Title `LY 317 · <stage>` (no time), route sub-text, DEPARTED content `Lands at 23:12`.
- Status-bar pill per stage (gate, time, belt, time left in flight; ≤ 7 characters). One UI may withhold it (risk 1).
- Bar: origin / destination icons, a tracker per phase, never goes back (F2), amber while delayed (until landing),
  grey on errors.
- **CHECK_IN is no longer promoted** (Google policy): no pill at check-in; promotion from GATE_READY.
- **Fewer alerts:** only entering CHECK_IN, GATE_READY, BOARDING, LAST_CALL, LANDED, and on a delay flip.
- Lock screen: public version (gate + times, no PNR) instead of "Contents hidden".
- Button: Boarding pass from GATE_READY until takeoff (Directions removed 2026-11-16); none at check-in or after takeoff.
- A swipe is final for that journey; safety timeout at `dismissalDate`, else arrival + 4 h.
- A process kill in flight no longer restarts the bar (F1).

---

## 5. Open risks

| # | Risk | Impact | Mitigation / owner |
|---|---|---|---|
| 1 | **Samsung One UI withholds the pill and Now Bar** (spike, F10) | No pill on Samsung even though promoted | S25+ run pending; Samsung partner docs / programme |
| 2 | **Production divergence** (check-in queue load) | Merge conflict in `LiveUpdateManager` | Section 1; add a builder test for the two-line CHECK_IN text |
| 3 | Group 3b data is demo-only (schedule fields not in the FCM payload); Android 17 emulator doesn't draw `StrikethroughSpan` in promoted notifications | No change lines in production until the backend sends the fields; plain text ("17:55 (was 17:00)") is the default | Data spec (Phase B, S4); SPAN / TalkBack to check on the Pixel and S25+ |
| 4 | `specialUse` foreground service kept for DEPARTED | Play policy review; battery | Revisit in Phase B (4.7) |
| 5 | Icons are Material placeholders | Off-brand bar icons | Design to supply the El Al set (`ic_progress_*`) |
| 6 | **PNR in analytics payloads**, logged by the host (`HostFlightAnalytics` via `SnappLog`; demo listener too) | PNR in logcat | Host: don't log params in release, or strip `pnr` |
| 7 | HE pill duration has no unit (`11:52`) | Ambiguous to some users | Design review |
| 8 | `DepartedClock` falls back to the server ETA when the payload has no `departureTimezoneOffset` / `flightDuration` | Pill/bar can differ from iOS | Server contract S3/S4 (Phase B) |
| 9 | First post after a restore counts as "entering" a stage | One extra alert in rare cases | Accept, or persist `PostedState` |
| 10 | Deferred by scope: 2.7 `gateOpenTime`, 4.4 `version` guard | — | Data spec (Phase B) |

---

## 6. Verification status

| Area | Emulator API 37 | Pixel (Android 16) | Samsung S25+ (Android 17) | ≤ Android 15 |
|---|---|---|---|---|
| Groups 1–2 (content, pill, title, F1, public version) | ✅ | ⏳ | ⏳ | — |
| Group 3 (icons, colours, promotion, alerts, F2) | ✅ | ⏳ | ⏳ | — |
| Group 4 (actions, swipe, timeout set, promotion status) | ✅ | ⏳ | ⏳ | — |
| Group 5 (permissions, fallback toggle, debug receiver) | ✅ | ⏳ | ⏳ | ⏳ fallback itself |
| Group 6 (restart, Tel Aviv, EN / HE) | ✅ | ⏳ | ⏳ | — |
| Not yet: timeout firing, Hebrew system locale (RTL shade), dark theme, FCM pushes via Postman, release build | ⏳ | | | |

Details: `planning/device-check.md`. Screenshots: `verification/screenshots/`.

## 7. Port order (suggested)

1. Copy the SDK `src/main` changes and `src/test`; add the junit line; delete the F7 files.
2. Re-apply production's queue-load content in `LiveUpdateContentBuilder` (+ test); keep production's `JourneyData`,
   `QueueLoad`, samples.
3. Host: set `actionIntentProvider`; add the two listener methods; optionally a promotion-status entry in settings.
4. Run `:elal-flight-sdk:testDebugUnitTest`, then the regression matrix (requirements §6) on the Pixel and the S25+.
