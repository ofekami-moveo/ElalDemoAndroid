# Implementation Report: Workflow bootstrap

**Spec:** `2026-11-10-live-update-parity`
**Date:** 2026-11-10
**Task group:** 0
**Branch:** `update-LUN-ui` (per-group branch waived by the user)
**Status:** Files written. Build and the smoke test have not been run yet (the user runs them).

## Files changed

- `CLAUDE.md` — **new.** Project rules for this repo: the main-app conventions that apply, the SDK exceptions,
  workflow rules (no `./gradlew`, no git state changes, no secrets), and the decided Live Update rules.
- `agent-os/commands/**`, `agent-os/standards/**`, `agent-os/config.yml` — copied unchanged from
  `/Users/ofek.amirav/Android/agent-os` (read-only source; contains no secrets).
- `agent-os/standards/SDK_EXCEPTIONS.md` — **new.** Overrides for DI, logging, Crashlytics, debug flags, storage,
  networking and testing inside `:elal-flight-sdk`.
- `agent-os/specs/2026-11-10-live-update-parity/` — **new.** `planning/initialization.md`,
  `planning/requirements.md` (findings F1–F10 incl. the Samsung S25+ finding, decisions, per-stage tables, clock
  design, server contract, regression matrix), `spec.md`, `tasks.md`, `planning/visuals/` (empty, for screenshots).
- `elal-flight-sdk/build.gradle.kts` — **build change (approved 2026-11-10):** one line,
  `testImplementation("junit:junit:4.13.2")`. Nothing else.
- `elal-flight-sdk/src/test/java/com/elal/flightsdk/utils/ToAbsoluteInstantSmokeTest.kt` — **new.** 3 tests.

## Tests

`ToAbsoluteInstantSmokeTest`: negative offset, positive offset, missing/invalid offset → null. Pure `java.time`.

## Not verified

- The Gradle sync and `:elal-flight-sdk:testDebugUnitTest` have not been run.
- No runtime change, so nothing to check in the demo app for this group.
