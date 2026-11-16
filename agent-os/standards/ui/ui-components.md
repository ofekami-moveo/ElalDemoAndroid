## UI component & screen best practices

The app uses **MVVM** with two UI systems: XML layouts + ViewBinding (Fragments) and Jetpack Compose. These conventions apply to both.

### Architecture

- **MVVM, no logic in the View**: Fragments/Composables render state and forward user intent. All business logic lives in use cases, invoked through the ViewModel.
- **One-shot events via `Channel`/`SharedFlow`**: Newer ViewModels expose one-shot events through a `Channel` consumed with `receiveAsFlow()` (or `SharedFlow`). Use `SingleLiveEvent` only when extending older ViewModels that already use it. Do not use `LiveData` for one-shot events.
- **State as observable streams**: Expose screen state as `StateFlow`/`Flow` the View collects; avoid pushing imperative UI mutations from the ViewModel.
- **Dependency injection with Hilt**: Inject ViewModels and dependencies via Hilt (`@Inject constructor`; `@HiltViewModel`). Manual bindings go in `AppModule`. All DI modules live in `app/src/main/java/di/` — never inside feature modules.
- **Use cases for behavior**: Each unit of business logic is a single-responsibility use case class. Never pass an `Activity` into a use case — split into request/complete steps if a prompt (biometric/passkey) is needed.

### Fragments (View system)

- **Extend `BaseFragment<Binding>`**: Use the lifecycle hooks — `beforeInit()`, `getBundle()`, `setDefaultViewsBehavior()`, `afterInit()`, `setViewsClickListeners()`, `subscribeToViewModel()` — instead of cramming everything into `onViewCreated`.
- **ViewBinding only**: Access views through the generated binding; never `findViewById`. Null out the binding in the appropriate lifecycle hook to avoid leaks.
- **Reuse fragments deliberately**: When a fragment serves multiple tabs (e.g. `TimelineFragment` for both Home and Trips), distinguish behavior with explicit bundle flags (e.g. `isHomeTabTimeline`) rather than duplicating the fragment.

### Compose

- **Stateless, hoisted composables**: Keep composables stateless where possible; hoist state up and pass data down with explicit parameters and event lambdas.
- **Single responsibility**: Each composable does one thing; compose complex screens from small, reusable composables rather than one monolith.
- **Collect state lifecycle-aware**: Use `collectAsStateWithLifecycle()` to observe ViewModel flows from Compose.
- **Preview support**: Provide `@Preview` for reusable composables to ease iteration.
