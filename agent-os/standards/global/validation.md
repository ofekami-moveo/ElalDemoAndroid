## Validation best practices

- **Client validation is for UX only**: On-device validation gives immediate feedback (form fields, dates, passenger details) but never replaces server-side validation — the backend remains the source of truth for security and data integrity.
- **Validate at the ViewModel/use case boundary**: Check input in the ViewModel or use case before triggering a network call; keep validation logic out of Fragments/composables.
- **Fail Early**: Reject invalid input before processing and surface a typed result the UI can render.
- **Field-specific messages**: Provide clear, localized, field-level error messages (from `strings.xml`) that tell the user how to fix the input.
- **Type, format & range checks**: Systematically validate types, formats (dates, emails, PNR codes, phone numbers), ranges, and required fields. Use Kotlin's type system and nullability to make invalid states unrepresentable where possible.
- **Allowlists over blocklists**: Define what is allowed rather than enumerating everything that isn't.
- **Trust boundaries**: Treat all data crossing into the app (network responses, deep links, RN bridge payloads, intent extras) as untrusted — validate before use.
- **Don't store invalid data**: Validate before persisting to Realm/DataStore/EncryptedPreferences so local state stays consistent.
- **Consistent validation**: Apply the same rules across every entry point that accepts the input.
