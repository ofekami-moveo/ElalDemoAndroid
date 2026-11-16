# Initialization: Live Update parity (LY 317)

**Date:** 2026-11-10
**Repo:** `/Users/ofek.amirav/ElalDemoAndroid` (`:app`, `:elal-flight-sdk`)
**Working branch:** `update-LUN-ui` (the user waived the per-group branches; the `**Branch:**` lines in
`tasks.md` are kept as the intended names).

## Brief

Bring the Android 16 Live Update to parity with the iOS Live Activity for LY 317, using the system notification
template (ProgressStyle), no custom UI.

Problems to fix:
1. Status-bar chip text is too long. It fits about 7 characters, so each phase needs one short value.
2. The static "Landing in" text goes stale. Replace it with a chronometer countdown.
3. Progress-point labels don't render. Use start/end icons and a tracker icon per phase.
4. Collapsed and expanded views show different times. Use one title.
5. Add actions (boarding pass, directions), a segment colour for delay, and `setSubText("TLV → JFK")`.

Implementation direction: server-sent state with absolute times and a version; one notification ID updated in
place via FCM; ProgressStyle with a fallback for Android ≤15; log promotion status; never repost after a user
dismiss; refresh progress in flight every 10–15 min; end at baggage with `setTimeoutAfter` as a safety net.

## Working rules

- Never run `./gradlew`; the user builds and pastes errors back.
- Never read or print `keystore.properties`, `local.properties` or secrets.
- No branches, commits, pushes, merges, or build/signing changes without explicit approval; call out every
  such line.
- `/Users/ofek.amirav/Android` is read-only.
- Every task group also ships the **demo-app changes** needed to check it on a device (added 2026-11-10 at the
  user's request).
- End of each group: summary, files touched, tests added, manual checklist, commit message / PR text.
