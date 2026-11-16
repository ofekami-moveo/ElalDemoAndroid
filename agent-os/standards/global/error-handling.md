## Error handling best practices

- **Model outcomes with sealed classes**: Represent success/failure of each use case step as a dedicated sealed class result, so callers handle every case explicitly via `when`. Don't surface raw exceptions or framework types (Retrofit `Response`, Realm errors) to the UI.
- **User-Friendly Messages**: Show clear, localized, actionable messages (from `strings.xml`) without exposing technical or security details.
- **Fail Fast and Explicitly**: Validate input and preconditions early; return a clear typed error rather than allowing invalid state to propagate.
- **Specific over generic**: Catch and throw specific exception types where it enables targeted handling; avoid swallowing `Exception` silently.
- **Handle at boundaries**: Convert exceptions to results at the use case / data layer boundary rather than scattering try/catch through the UI.
- **Record and log**: Record unexpected exceptions with `Firebase.crashlytics.recordException(e)` and log context via `SnappLog.log("ClassName-> methodName-> message")`. Never log secrets, tokens, or PII.
- **Graceful Degradation**: When a non-critical service fails (e.g. an optional widget feed), degrade the screen gracefully instead of breaking the whole flow.
- **Retry Strategies**: Use exponential backoff for transient network failures; don't retry non-idempotent operations blindly.
- **Clean Up Resources**: Release resources (Realm instances, streams, listeners, ViewBinding references) in the appropriate lifecycle hook or `finally` block; cancel coroutines with their scope.
