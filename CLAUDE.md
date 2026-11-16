# CLAUDE.md - EL AL Demo Android (Live Update SDK)

## Project Overview
Demo host app plus the `:elal-flight-sdk` library that renders the El Al flight journey as an Android 16
Live Update (promoted ongoing notification), the Android counterpart of the iOS Live Activity.
The SDK is synced into the production app (`/Users/ofek.amirav/Android`) by hand; see
`Docs/handover/live-activity/01-live-activity.html`. That repo is **read-only** from here.

## Modules
- **app** — demo host: `MainActivity`, `JourneyViewModel` simulators, `FlightMessagingService` (FCM parser),
  `DebugNextStageReceiver`.
- **elal-flight-sdk** — `LiveUpdateManager` (singleton `object`), `InFlightService`, `JourneyPrefs`,
  `ServerClock`, models in `model/`, analytics in `analytics/`.

## Build & SDK
- compileSdk 36, minSdk 26, Java 17, `androidx.core:core-ktx:1.18.0` (NotificationCompat.ProgressStyle).
- **Never run `./gradlew`.** The user runs builds and pastes errors back.
- **Never** create branches, commit, push or merge, and never change build/signing config without explicit
  approval. Call out every such line.
- **Never read or print** `keystore.properties`, `local.properties` or any secret.

## Workflow
- Agent OS: `agent-os/commands/`, `agent-os/standards/`, specs in `agent-os/specs/<date>-<name>/`.
- Before each task group, check the current branch matches the group's `**Branch:**` line
  (the user may waive this explicitly).
- At the end of each group: summary, files touched, tests added, manual checklist, commit message / PR text,
  and `implementation/<n>-<group>-implementation.md`.

## Code Conventions (apply in both modules)
- **Always use curly braces** for `if`/`else`/`when` bodies, never braceless single-line statements.
- **Use `when` instead of `if/else`** when there are more than 2 conditions.
- **KDoc on every new function**: what it does and why.
- **One top-level class per file**: no nested/inner classes for new code.
- **No hardcoded colours or strings**: named `@color/` resources (with a `values-night` variant where it
  matters) and string resources in **both** `values/` (EN) and `values-iw/` (HE).
- **RTL / bidi-safe text**: wrap user-visible values (gates, codes, times) with a bidi isolate when they are
  composed into a sentence; never build direction-dependent text by string concatenation alone.
- **Don't reformat untouched legacy code.** Keep diffs to the lines the task needs.
- Pure presentation logic has no `android.*` imports, so it runs in JVM unit tests (`src/test/`, JUnit 4).

## SDK exceptions (see `agent-os/standards/SDK_EXCEPTIONS.md`)
The SDK is a standalone library shipped into other hosts, so these main-app rules do **not** apply inside
`:elal-flight-sdk`:
- No Hilt / DI framework. Plain constructors and the `LiveUpdateManager` object.
- No `SnappLog`: log through `com.elal.flightsdk.utils.Logger`.
- No Crashlytics: report problems to the host through `FlightAnalyticsListener` (new methods get a default
  empty body so existing hosts still compile).
- No `DebugManager` / `GeneralManager`: debug behaviour is driven by SDK config (`uiDebugMode`, config objects).
- `SharedPreferences` (`JourneyPrefs`) is the SDK's storage; `EncryptedPreferencesManager` / `DataStoreManager`
  don't exist here. Nothing secret is stored.

## Live Update rules (decided, don't re-litigate)
- System template only: `NotificationCompat.ProgressStyle`, no RemoteViews, no `setColorized`.
- Promotion requires ongoing + title + `setRequestPromotedOngoing(true)` + `POST_PROMOTED_NOTIFICATIONS`.
- Promote only from GATE_READY onward (Google policy: airport/boarding). CHECK_IN is a regular ongoing notification.
- One notification ID (7001), updated in place. Never repost after the user dismisses it.
