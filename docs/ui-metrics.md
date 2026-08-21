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
| Settings section heading inset | 32 dp | `SettingsSectionInset` |
| Closely related controls or actions | 8 dp | `Related` |
| Peer fields in one form group | 16 dp | `FormField` |
| Separate content groups or an inline primary action after fields | 24 dp | `Section` |
| Pinned bottom task-action inset | 16 dp | `PinnedActionInset` |

Use `WhiteNoiseSpacing` from `ui/theme/WhiteNoiseSpacing.kt`; do not duplicate
these values as screen-local magic numbers. Constrained/adaptive panes may add
outer space at larger widths, but the compact margin remains 16 dp inside the
active pane.

## Forms and actions

- Use the shared Material-based tonal field for ordinary forms. It uses
  `surfaceContainerHigh`, no resting/disabled border, a 2 dp full-shape focus
  or error ring, and the shared extra-large shape so all four corners have the
  same 28 dp radius. A single-line 56 dp field is therefore a full capsule;
  multiline fields keep the fixed 28 dp radius rather than scaling it with
  their height. Disabled fields use `surfaceContainerLow` and Material disabled
  content colors.
- The field content inset is 16 dp. Above-field labels and supporting/error
  text align to that same directional start line; Material's 24 dp leading
  icon artwork also begins on it inside the standard 48 dp slot. Keep the
  native 4 dp label-to-container and supporting-text gaps.
- Separate peer fields by 16 dp. Use 24 dp between the form and a distinct
  section or an inline primary task action. A pinned action belongs to the
  `Scaffold` bottom slot and uses 16 dp outer insets plus navigation-bar and IME
  handling.
- Single-line fields use Material's 56 dp minimum container. Full-width,
  form-adjacent, and primary task buttons use Material's matching 56 dp medium
  button container with its 24 dp horizontal and 8 dp vertical content
  padding. Closely related stacked buttons use an 8 dp gap.
- Compact contextual actions, search bars, menus, and message composers keep
  their native component metrics instead of inheriting form spacing.

## Settings overview

- Main Settings destinations use Material's native one-line `ListItem`
  measurement rather than a custom fixed row height. In the current component
  this yields the standard 56 dp minimum while remaining free to grow for font
  and display scaling.
- Each destination uses one 24 dp leading Material Symbol and the standard
  trailing disclosure slot. The icon is a noninteractive signifier inside the
  row, not a separate touch target.
- A heading above a tonal settings group starts 32 dp from the screen edge:
  the 16 dp compact margin plus the list item's 16 dp internal content inset.
  This aligns the heading with leading icons on the overview and with row text
  on icon-free detail lists; it does not align to the outer container edge.
- Do not add supporting text when the headline already names the destination.
  Keep it only for an actionable unavailable reason. A short current value may
  sit in the trailing slot with an 8 dp gap before the disclosure chevron.
- Material continues to own list-item content padding, typography, state
  layer, minimum target size, and adaptation. Directional disclosure and exit
  symbols mirror in right-to-left layouts.

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
- [Material TextField defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/TextFieldDefaults)
- [Material OutlinedTextField defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/OutlinedTextFieldDefaults)
- [Material text-field label positions](https://developer.android.com/reference/kotlin/androidx/compose/material3/TextFieldLabelPosition)
- [Material Button defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/ButtonDefaults)
