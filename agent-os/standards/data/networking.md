## Networking standards and conventions

- **Retrofit + OkHttp first**: New network calls use Retrofit 2.11.0 over OkHttp. Volley is legacy and being phased out — do not add new Volley calls.
- **Service interfaces in `data-source`**: Define Retrofit API interfaces and their request/response POJOs in the `data-source` module, not inside feature modules.
- **Suspend functions over callbacks**: Prefer `suspend` Retrofit functions consumed from coroutines (ViewModel/use case scope) rather than enqueued callbacks.
- **Use cases own the call**: Network requests are triggered from use case classes (single responsibility), not directly from Fragments or Views. ViewModels invoke use cases.
- **Typed results, not raw responses**: Map raw responses into sealed result classes (one sealed class per use case step) before returning to the ViewModel. Never leak Retrofit `Response`/`Call` types into the UI layer.
- **Environment-aware base URLs**: Resolve base URLs per build flavor (`dev`, `preProd`, `prod`) — never hardcode an environment-specific host in a call site.
- **Auth headers centrally**: Attach auth tokens/headers via OkHttp interceptors, not per-request. Read tokens from `EncryptedPreferencesManager`.
- **Handle failure explicitly**: Wrap calls so network/parse failures become a typed error result; record unexpected exceptions with `Firebase.crashlytics.recordException(e)` and log via `SnappLog.log()`.
- **No secrets in code**: API keys and secrets come from secure config/build config, never committed in source.
