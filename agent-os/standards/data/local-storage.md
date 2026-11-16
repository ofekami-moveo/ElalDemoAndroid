## Local storage best practices

Pick the right store for the data — the app has three, with strict rules:

- **`EncryptedPreferencesManager`** — login credentials, tokens, passkey/WebAuthn data, and any sensitive/secure value. Always use this for secrets.
- **`DataStoreManager`** — regular stored params, settings, and non-sensitive preferences.
- **`Realm`** (10.15.1) — structured local database for larger/relational app data.
- **Never use `PreferencesManager`** — it is legacy. Do not read from or write to it for anything.

### General

- **Single source of truth**: Access a given piece of stored state through one manager, not scattered direct reads. Route through the appropriate manager rather than touching storage APIs directly from Fragments.
- **Off the main thread**: Perform disk/DB reads and writes off the UI thread (coroutines / Realm transactions), never blocking the main thread.
- **Clear on logout**: Ensure sensitive stored data (tokens, credentials, passkey data) is cleared on logout/session end.
- **Don't store what you can derive**: Avoid persisting values that can be recomputed cheaply; persist only what must survive process death.

### Realm

- **Transactions for writes**: Wrap all Realm writes in transactions; keep them small and focused.
- **Close/scope realms correctly**: Respect Realm thread-confinement; don't pass managed objects across threads — copy out (`copyFromRealm`) when leaving the realm's scope.
- **Schema migrations**: When changing a Realm schema, bump the schema version and provide an explicit migration block. Never ship a schema change without a migration path, and test that existing installs upgrade cleanly.
