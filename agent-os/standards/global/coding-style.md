## Coding style best practices (Kotlin / Android)

- **Follow Kotlin conventions**: `PascalCase` for classes/objects, `camelCase` for functions/properties, `UPPER_SNAKE_CASE` for constants. File names match the primary class.
- **One class per file**: Each class, `data class`, `sealed class`, and `enum` lives in its own file. Never create nested/inner Kotlin classes for these.
- **Always use curly braces**: Wrap every `if`/`else`/`when` body in `{}` — never write braceless single-line statements like `if (x) return y`.
- **Prefer `when` over chained `if/else`**: Use `when` when there are more than two conditions.
- **Meaningful Names**: Choose descriptive names that reveal intent; avoid abbreviations and single-letter variables except in narrow contexts.
- **Small, Focused Functions**: Keep functions small and focused on a single task. Business logic belongs in single-responsibility use case classes.
- **Prefer immutability**: Favor `val` over `var` and immutable `data class`es; use Kotlin idioms (scope functions, `?.`, `?:`, destructuring) where they improve clarity, not to be clever.
- **Consistent formatting**: Keep indentation and line breaks consistent with surrounding code; match the file's existing style.
- **Remove Dead Code**: Delete unused code, commented-out blocks, and imports rather than leaving them as clutter.
- **Backward compatibility only when required**: Unless specifically instructed otherwise, assume you do not need to write extra logic for backward compatibility.
- **DRY Principle**: Extract common logic into shared functions, extensions, or `core`/`util` modules rather than duplicating it.
- **Testing flags belong in `GeneralManager`**: Never create local testing flags in use cases or other classes.
