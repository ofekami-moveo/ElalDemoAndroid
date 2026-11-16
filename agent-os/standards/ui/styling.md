## Styling & theming best practices

Replaces web CSS guidance — this app styles via Android resources, XML styles/themes, and Compose Material3.

- **Resources, not hardcoded values**: Reference colors, dimensions, and strings from resource files (`colors.xml`, `dimens.xml`, `strings.xml`) — never hardcode hex colors, raw `dp`/`sp` values, or display strings in layouts or composables.
- **One design system, two renderers**: Keep XML themes/styles and the Compose Material3 theme aligned to the same color/typography/spacing tokens so View-based and Compose screens look consistent.
- **Material3 theming in Compose**: Drive Compose styling through the app's `MaterialTheme` (colorScheme, typography, shapes); read values from the theme rather than passing literal colors/sizes into composables.
- **Reuse styles**: Extract repeated XML attribute sets into `style`s and themes; extract repeated Compose styling into shared modifiers/components instead of copy-pasting.
- **Localization & RTL**: All user-facing text comes from `strings.xml` (the app ships Hebrew/RTL). Use `start`/`end` rather than `left`/`right` and verify layouts mirror correctly in RTL.
- **Dark/light awareness**: Resolve themed colors so screens behave correctly across configurations rather than assuming a single appearance.
- **No leftover/unused resources**: Remove unused styles, colors, and drawables rather than leaving clutter.
