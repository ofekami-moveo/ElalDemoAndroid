# Spike: why the status-bar pill doesn't show

**Date:** 2026-11-10 · **Branch:** `update-LUN-ui` (throwaway; spike lines are marked `// SPIKE - do not merge`)

## Question

The notification is promoted on both the Pixel and the Samsung S25+ (`PROMOTED_ONGOING` in dumpsys), but there's
no pill, only the small icon. Hypothesis: the pill has nothing to show, because there's no short critical text and
`setShowWhen(false)` hides the time.

## How to switch variants

`LiveUpdateManager.kt`, top of the object: `SPIKE_CHIP_VARIANT = SpikeChipVariant.A | B | C | OFF`, then rebuild
from Android Studio.

| Variant | Change |
|---|---|
| OFF | Current behaviour (baseline) |
| A | `setShortCriticalText("B4")` |
| B | `setShowWhen(true)`, `setWhen(now + 1 h)`, `setUsesChronometer(true)`, `setChronometerCountDown(true)`; no chip text |
| C | A + B |

The 1 h target is fixed once per process, so DEPARTED ticks don't move it. Each build logs
`SPIKE chip variant=…` under the `LiveUpdateManager` tag.

## Steps (per variant, per device)

1. In the demo app, end the active journey.
2. Start the simulation, then press Home.
3. Look at the status bar, the top of the shade, and the lock screen / Now Bar (Samsung).
4. Run:
   ```
   adb shell dumpsys notification --noredact | grep -A60 "id=7001"
   ```
   Note `flags` (`PROMOTED_ONGOING`), Samsung `semFlags`, `android.shortCriticalText`, `when=`,
   `android.showChronometer`, `android.chronometerCountDown`.
5. Optional: `adb logcat -s LiveUpdateManager | grep SPIKE` to confirm the variant.

## Results

| Variant | Device | Promoted? | Pill shown? | Pill content | Top of shade | Lock screen / Now Bar | Notes |
|---|---|---|---|---|---|---|---|
| OFF | Pixel (A16) | yes | no | — | | | baseline (reported) |
| OFF | S25+ (A17) | yes (`semFlags=0x2008`) | no | — | | | baseline (reported) |
| A | Pixel | yes | **yes** | `B4` | | | reported by user |
| A | S25+ (A17, SDK 37, One UI 9.0) | yes (`semFlags=0x2008`) | **no** | — | | | `android.shortCriticalText=B4` present; app not listed in Samsung's Live notifications settings (see below) |
| B | Pixel | | | | | | |
| B | S25+ | | | | | | |
| C | Pixel | | | | | | which wins: text or timer? |
| C | S25+ | yes | **no** | — | | no Now Bar | dumpsys: `showChronometer=true`, `chronometerCountDown=true`, `when` ≈ now + 59 min, `text=true` (14:28:45) |

## Samsung S25+ evidence (read via adb, 2026-11-10 14:20)

- `POST_PROMOTED_NOTIFICATIONS: granted=true`; notification `flags=ONGOING_EVENT|PROMOTED_ONGOING`, `semFlags=0x2008`,
  `importance=4`, `android.requestPromotedOngoing=true`, **`android.shortCriticalText=B4`**. Log: `variant=A text=true`.
  So everything Android asks for is in place.
- App ops: `POST_PROMOTED_NOTIFICATIONS: default; rejectTime=+4m43s ago`, so something checked the op and was
  refused, even though the posted notification carries `PROMOTED_ONGOING`. Cause unknown.
- Samsung keeps per-package Now Bar keys in `settings system`: `key_now_bar_com_google_android_googlequicksearchbox=1`,
  `key_now_bar_com_nhn_android_search=1`, `key_now_bar_servicebox_music=1`, `support_nowbar_naver_sports=1`. There's
  no key for `com.elal.demoapp`, and the app isn't listed in Samsung's Live notifications settings.
- `APP_NOTIFICATION_PROMOTION_SETTINGS` on the S25+ opens Samsung's general app page
  (`Settings$AppNotificationSettingsActivity`): there is **no per-app Live Updates toggle** on One UI 9. That page
  shows notifications allowed, lock screen / badge / pop-up ticked, pop-up style "brief", and lock-screen content
  "hide when locked" (default). Our notification is `vis=PRIVATE`, so the lock screen / Now Bar may also be hiding
  the content: candidate for group 2 is `setVisibility(VISIBILITY_PUBLIC)` or a public version.
- Diagnostic: `settings put system key_now_bar_com_elal_demoapp 1`, journey reposted at 14:26:32 (variant A).
  **Still no pill** in the status bar and **no Now Bar** on the lock screen. That key alone does not enable it
  (not tested after a reboot). The key was deleted again afterwards.
- Variants B and C (chronometer) have **not** been tried on the S25+ yet.
- Working theory (not proven): One UI gates the status-bar pill and the Now Bar per app, separately from Android's
  promotion, so a promoted notification from an app that isn't enabled stays in the shade.

## Conclusion

- **Root cause of the missing pill (Pixel):** the notification had no chip content (`shortCriticalText` commented out,
  `setShowWhen(false)`). Promotion alone doesn't create a pill. Fixed by variant A.
- **Samsung S25+ (One UI 9, Android 17):** no pill and no Now Bar with chip text (A), with chip text + chronometer
  (C), or with the Now Bar key set. Our notification carries every documented extra. One UI withholds the pill and
  the Now Bar from this app; there's no per-app setting to enable it. **Open risk, outside our code.** Follow-up:
  Samsung developer docs / partner programme for the Now Bar and live notifications.
- **Still open (Pixel):** with both text and chronometer set (C), which one the pill shows. Variant B on the
  Pixel (chronometer only) confirms DEPARTED. Check both in group 2.

**Spec updates:** requirements F10 and §3a, tasks group 2 acceptance (2026-11-10).
