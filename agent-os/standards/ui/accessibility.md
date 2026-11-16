## UI accessibility best practices (Android)

- **Content descriptions**: Provide `contentDescription` for all meaningful `ImageView`s/icons and interactive elements; mark purely decorative elements as `null`/non-important for accessibility. In Compose, use `contentDescription` and `semantics {}`.
- **TalkBack testing**: Verify screens are usable with TalkBack — logical focus order, every actionable element announced, no traps.
- **Touch target size**: Ensure tap targets are at least 48dp × 48dp.
- **Color contrast**: Maintain sufficient text contrast (≈4.5:1 for normal text) and never rely on color alone to convey state or meaning.
- **Scalable text**: Use `sp` for text and verify layouts remain usable at large system font scales without clipping or overlap.
- **Labels for inputs**: Associate every input/field with a visible label or `hint`/`labelFor`; don't rely on placeholder text alone.
- **Focus management**: Manage focus deliberately in dialogs, bottom sheets, and dynamic content so keyboard/switch/TalkBack users land where expected.
- **Respect system settings**: Honor system font size, display size, and reduced-motion preferences rather than forcing fixed values.
- **RTL correctness**: Confirm screens read correctly right-to-left for Hebrew.
