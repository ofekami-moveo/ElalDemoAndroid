## Tech stack

This is the EL AL Israel Airlines Android app — a hybrid **native Android (Kotlin)** + **React Native** application. App ID `il.co.elal.mobile`, namespace `il.co.ewave.elal`.

### Language & Build

- **Language:** Kotlin 2.0.0 (Java 17 toolchain)
- **Build system:** Gradle 8.9, Android Gradle Plugin 8.7.3
- **SDK:** minSdk 26, targetSdk 35, compileSdk 35
- **Environments / flavors:** `dev`, `preProd`, `prod`
- **Obfuscation:** DexGuard

### Modules

- **app** — main application module (UI, DI, managers, use cases)
- **auth** — authentication (login, biometric, passkey/WebAuthn)
- **core** — shared core (user management, network, extensions, base classes)
- **data-source** — data layer (API services, POJOs, Retrofit interfaces)
- **hotels, trips, timeline, youth-world** — feature modules
- **util** — utilities and logging
- **ucrop** — image cropping library
- **elal-native** — React Native side (RN 0.82.1)

### UI

- **View system:** XML layouts + ViewBinding; fragments extend `BaseFragment<Binding>`
- **Jetpack Compose:** Material3, navigation-compose, coil-compose, lottie-compose (used alongside the View system)
- **Pattern:** MVVM — ViewModels emit one-shot events via `Channel`/`SharedFlow` (not LiveData)
- **Animations:** Lottie 6.0.0
- **Image loading:** Glide 4.16.0 (Views), Coil (Compose)

### Dependency Injection

- **Hilt** 2.51.1 — `@Inject constructor` for automatic injection; `@Module`/`@Provides` in `AppModule` for manual bindings. All DI modules live in `app/src/main/java/di/`.

### Data & Storage

- **Networking:** Retrofit 2.11.0 + OkHttp 5.0.0-alpha.2 (primary); Volley (legacy, being phased out)
- **Local database:** Realm 10.15.1
- **Secure storage:** `EncryptedPreferencesManager` (credentials, tokens, passkey data)
- **Regular storage:** `DataStoreManager` (settings, non-sensitive prefs)
- **Never use** the legacy `PreferencesManager`

### Authentication

- **Credential Manager** 1.3.0 — Passkey/WebAuthn
- Flows: biometric, RSA remember-me, OTP passkey

### Monitoring & Logging

- **Firebase** (BoM 33.4.0) — Crashlytics, Analytics, Messaging, Performance
- **Logging:** `SnappLog.log()` with `"ClassName-> methodName-> message"` format
- **Error recording:** `Firebase.crashlytics.recordException(e)`

### Testing & Build scripts

- **Instrumentation:** AndroidX test (`androidx.test.ext:junit`, `androidx.test:runner`, UI Automator)
- **Build scripts:** `./assembleDevReleaseApk.sh`, `./assemblePreProdDebugApk.sh`, `./assembleProdReleaseBundle.sh`; Firebase distribution: `./assembleDevReleaseApk-firebase.sh`, `./assemblePreProdReleaseApk-firebase.sh`, `./assembleProdReleaseApk-firebase.sh`
