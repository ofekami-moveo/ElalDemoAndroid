## SDK exceptions (`:elal-flight-sdk`)

The standards in this folder were copied from the main EL AL Android app. Inside `:elal-flight-sdk`, the
rules below **override** them. Everything not listed here applies unchanged.

| Standard | Main-app rule | SDK rule |
|---|---|---|
| `global/conventions.md`, `global/tech-stack.md` | Hilt, `@Inject`, modules in `app/di/` | No DI framework. Plain constructors; `LiveUpdateManager` is the entry point. Dependencies (string provider, clock, config) are passed as constructor parameters so they can be faked in tests. |
| `global/conventions.md`, `global/error-handling.md` | `SnappLog.log("Class-> method-> msg")`, `Firebase.crashlytics.recordException` | Log via `com.elal.flightsdk.utils.Logger` with the same `"Class-> method-> msg"` shape. No Firebase in the SDK. Report failures the host must know about through `FlightAnalyticsListener` (new methods have default empty bodies). |
| `global/conventions.md` | Testing flags in `GeneralManager` / `DebugManager` | Neither exists. Debug behaviour comes from SDK configuration (`LiveUpdateManager.uiDebugMode`, `LiveUpdateConfig`). |
| `data/local-storage.md` | `EncryptedPreferencesManager` / `DataStoreManager`; never `PreferencesManager` | `SharedPreferences` via `JourneyPrefs` only. No secrets are stored. Writes that must survive process death use `commit()`. |
| `data/networking.md` | Retrofit/OkHttp | The SDK does no networking. State arrives from the host (FCM) as `FlightContentState`. |
| `global/tech-stack.md` | Gson | `kotlinx.serialization` for persisted state. New fields default to null so old persisted JSON still loads. |
| `testing/test-writing.md` | — | Local JVM tests in `src/test/`, JUnit 4 only, no Robolectric. Pure code must not touch `android.*`; `unitTests.isReturnDefaultValues` is deliberately **not** set so accidental Android calls fail. |

Rules that **do** apply in the SDK: braces always, `when` for more than 2 branches, KDoc on new functions,
one top-level class per file, no hardcoded colours/strings (EN + HE), RTL/bidi-safe text, and don't reformat
untouched legacy code.
