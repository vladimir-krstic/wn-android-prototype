# White Noise Android UI metrics

These are the default metrics for custom White Noise Android compositions.
Use them unless the latest explicit user direction or a standard Android or
Material component requires a different value. Material components continue
to own their internal padding, typography, shape, state layers, and motion.

## Layout rhythm

Android's current guidance uses an 8 dp layout grid, with a 4 dp sub-grid for
small details. White Noise maps that guidance to these shared tokens:

| Relationship | Default | Code token |
| --- | ---: | --- |
| Compact-screen horizontal margin | 16 dp | `CompactScreenMargin` |
| Closely related controls or actions | 8 dp | `Related` |
| Peer fields in one form group | 16 dp | `FormField` |
| Separate content groups or an inline primary action after fields | 24 dp | `Section` |
| Pinned bottom task-action inset | 16 dp | `PinnedActionInset` |

Use `WhiteNoiseSpacing` from `ui/theme/WhiteNoiseSpacing.kt`; do not duplicate
these values as screen-local magic numbers. Constrained/adaptive panes may add
outer space at larger widths, but the compact margin remains 16 dp inside the
active pane.

## Forms and actions

- Use Material 3 outlined fields for ordinary forms, with labels positioned by
  `TextFieldLabelPosition.Above` and `surfaceContainerLow` as the slight fill.
- Do not override a field's internal content padding or the label-to-container
  gap. Material owns those metrics, including supporting/error text.
- Separate peer fields by 16 dp. Use 24 dp between the form and a distinct
  section or an inline primary task action. A pinned action belongs to the
  `Scaffold` bottom slot and uses 16 dp outer insets plus navigation-bar and IME
  handling.
- Full-width task buttons remain at least 56 dp high with 24 dp horizontal
  content padding. Closely related stacked buttons use an 8 dp gap.
- Compact contextual actions, search bars, menus, and message composers keep
  their native component metrics instead of inheriting form spacing.

## Launch-to-Welcome mark handoff

Android fixes a splash icon without a background to a centered 288 dp canvas
whose content must fit within a 192 dp safe circle. The White Noise splash
vector places the visible mark at 149.5 × 115 dp, centered inside that canvas.
The initial Welcome screen must render the same mark at exactly 149.5 × 115 dp
at the center of the full launch window. Its actions remain separately pinned
to safe drawing insets, so their layout cannot shift the mark.

Do not resize or vertically offset either launch mark independently. If the
brand mark changes, update the splash vector, Welcome dimensions, relevant
tests, this document, and the onboarding brief in one batch.

## Governing Android sources

- [Grids and units](https://developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units)
- [Content composition and structure](https://developer.android.com/design/ui/mobile/guides/layout-and-content/content-structure)
- [Splash screens](https://developer.android.com/develop/ui/views/launch/splash-screen)
- [Compose text input](https://developer.android.com/develop/ui/compose/text/user-input)

