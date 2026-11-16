## Data model & POJO best practices

- **One class per file**: Every POJO, `data class`, `sealed class`, and `enum` gets its own file. Never create nested/inner Kotlin classes for these.
- **Immutable data classes**: Model API payloads and domain entities as `data class` with `val` properties. Prefer immutability; expose copies via `copy()` rather than mutating.
- **Sealed classes for results**: Represent the outcome of each use case step as its own sealed class (e.g. `PasskeyLoginOptionsResult`, `PasskeyLoginFlowResult`). One sealed class per step — do not overload a single result type across unrelated flows.
- **Clear naming**: Name models for what they represent in the domain (e.g. `PnrHolder`, `FlightSegment`), not after the screen or the HTTP shape.
- **Nullability is intentional**: Make a field nullable only when it can genuinely be absent. Resolve nullability at the mapping boundary so downstream code works with non-null values where possible.
- **Separate DTO from domain when it matters**: Keep API DTOs (in `data-source`) distinct from domain models when the API shape and the app's needs diverge; map between them explicitly.
- **No business logic in POJOs**: Keep data classes free of behavior beyond trivial computed properties. Business logic lives in use cases.
- **KDoc on non-obvious models**: Add KDoc explaining the purpose of a model and any field whose meaning isn't self-evident.
