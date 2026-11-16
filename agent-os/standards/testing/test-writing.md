## Test coverage best practices (Android)

- **Write Minimal Tests During Development**: Do NOT write tests for every change or intermediate step. Complete the feature first, then add strategic tests at logical completion points.
- **Test Only Core User Flows**: Write tests for critical paths and primary workflows (login/auth flows, flight/timeline resolution, key navigation). Skip non-critical utilities and secondary flows unless instructed.
- **Defer Edge Case Testing**: Don't test edge cases, error states, or validation unless they are business-critical; leave those for dedicated testing phases.
- **Test Behavior, Not Implementation**: Assert on what a use case / ViewModel produces (emitted state, sealed result), not on internal call sequences, to reduce brittleness.
- **Right test type for the layer**:
  - **Local JVM unit tests** (`src/test/`) for use cases, ViewModels, mappers, and pure logic — keep these fast and free of the Android framework.
  - **Instrumentation tests** (`src/androidTest/`, AndroidX `junit`/`runner`/UI Automator) for flows that genuinely need a device/emulator.
- **Coroutines & Flows**: Use a test dispatcher for coroutine-based use cases/ViewModels and assert on emitted `Flow`/`Channel` events.
- **Mock external dependencies**: Isolate units by faking/mocking network (Retrofit services), Realm, and the preference/managers layer so tests don't touch real I/O.
- **Clear Test Names**: Name tests for the scenario and expected outcome.
- **Fast Execution**: Keep unit tests in the millisecond range so they're run frequently.
