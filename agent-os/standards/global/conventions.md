## General development conventions

- **Package structure**: Organize code by responsibility — `di/`, `managers/`, `useCases/`, `sealedClasses/`, `helpers/`, `extensions/`, `ui/`. Place shared code in `core`/`util`, data/API code in `data-source`, and feature code in the matching feature module.
- **Module boundaries**: All DI modules live in `app/src/main/java/di/` — never create DI modules inside feature modules. Keep feature modules depending on `core`/`data-source`, not on each other unless intended.
- **MVVM + use cases**: Business logic lives in single-responsibility use case classes; ViewModels orchestrate them and expose state/events to the View. Never pass an `Activity` into a use case — split into request/complete steps when a prompt is needed.
- **DI with Hilt**: Prefer `@Inject constructor` for automatic injection; use `@Module`/`@Provides` in `AppModule` only for manual bindings.
- **Version Control**: Use clear commit messages and feature branches off `master`; open PRs with meaningful descriptions.
- **Environment Configuration**: Drive environment differences through build flavors (`dev`, `preProd`, `prod`); never commit secrets or API keys.
- **Dependency Management**: Keep dependencies minimal and reasonably current; justify major additions. Phase out legacy dependencies (e.g. Volley, `PreferencesManager`) rather than extending them.
- **Logging & errors**: Log via `SnappLog.log()` using `"ClassName-> methodName-> message"`; record unexpected exceptions with `Firebase.crashlytics.recordException(e)`.
- **Testing flags**: Keep all testing/feature toggles in `GeneralManager`, never as ad-hoc local flags.
- **Code Review Process**: Review for adherence to these standards and the rules in CLAUDE.md before merging.
