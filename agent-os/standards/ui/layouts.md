## Layout & multi-device best practices

Replaces web responsive guidance — this is the Android equivalent for adapting to screen sizes, densities, and configuration changes.

- **Density-independent units**: Use `dp` for sizing/spacing and `sp` for text. Never use raw pixels.
- **Flexible layouts**: Prefer `ConstraintLayout` and flexible widths (`match_constraint` / `0dp`, weights) over fixed dimensions so screens adapt to varying widths. In Compose, use `fillMaxWidth`, weights, and constraints rather than fixed sizes.
- **Support all target screens**: The app runs on phones from minSdk 26 up to targetSdk 35 — verify layouts on small and large screen sizes and densities, not just one device.
- **Handle configuration changes**: Survive rotation and process death by holding UI state in the ViewModel (`StateFlow`/saved state), not in the View.
- **Touch targets**: Keep interactive elements at least 48dp × 48dp.
- **Edge-to-edge & insets**: Respect system bars / display cutouts by applying window insets rather than hardcoding padding.
- **RTL-aware layouts**: Use `start`/`end` constraints and margins (not `left`/`right`) so layouts mirror correctly for Hebrew/RTL.
- **Avoid deep view hierarchies**: Keep XML hierarchies shallow for performance; lazy-load lists with `RecyclerView`/`LazyColumn` instead of inflating long static lists.
- **Test across sizes**: Verify changes render and remain usable across phone sizes and orientations before considering UI work done.
