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

## Welcome mark and system splash

Welcome centers the mark in the space between the bottom of the top safe area
and the actual top edge of Sign In. Add Profile uses the bottom of its Material
top app bar as the upper boundary. A weighted logo region above the measured
button group owns this relationship; do not use a full-window center or a
fixed vertical offset.

Welcome has no text input. Use the union of system-bar and display-cutout
insets instead of `safeDrawing`, which also includes the IME. An outgoing
form's keyboard must not resize this logo region or lift Welcome's buttons
during a navigation transition.

The visible mark is 50% of the safe content width, capped at 260 dp (half the
520 dp form measure), with its original 598:460 aspect ratio. If vertical space
is tight, shrink it to fit with 16 dp of breathing room above and below. Actions
keep their native minimum 56 dp height, 8 dp gap, 16 dp horizontal margins and
bottom inset, and system-safe bottom clearance.

Android's system splash remains independently platform-sized: the visible
149.5 × 115 dp mark fits within the 192 dp safe circle of a centered 288 dp icon
canvas. Welcome no longer matches that size or position. This user-approved
layout supersedes the splash-matching requirement in WN-ANDROID-0019; do not
enlarge or reposition the system splash to imitate the Welcome composition.

## Web-image task sheet

- Close and Done use native compact app-bar controls, not 56 dp form actions.
  Done's outer trailing clearance is 16 dp, including the app bar's native
  4 dp action-edge inset. Its internal content padding remains Material-owned.
- Search/URL uses the Photo Picker's filled-tonal navigation-button pattern:
  theme medium shape (12 dp), equal widths, 16 dp screen margins, an 8 dp gap,
  and native compact button/48 dp minimum target metrics.
- Privacy and input use 16 dp peer spacing. Input-to-results and
  supporting-text-to-preview-heading use 24 dp section spacing; preview heading
  to image uses 8 dp. The dense, square three-column grid keeps 2 dp media
  gutters, a component-specific exception to the general 4/8 dp layout rhythm.
- The modal owns safe-area/IME insets once. Do not add status-bar padding to
  its nested app bar or another keyboard padding layer to its content.
  Scroll the privacy/input/results body instead of trapping fixed controls
  above a zero-height grid when space is constrained.

## Chats and shared scrolling headers

- Chats toolbar and row avatars have 40 dp and 52 dp visible diameters. Their
  leading artwork edges share the 16 dp content margin inside the same capped
  680 dp pane. The toolbar uses an 8 dp relationship inset in addition to its
  native 4 dp edge and 4 dp touch-target/40 dp avatar difference. Native Back
  targets and conversation-title avatars do not inherit this avatar inset.
- Chat rows use the native clickable `ListItem` overload, with its 12 dp
  avatar-to-content gap (25% tighter than the former 16 dp baseline). An 8 dp
  outer anchor inset plus 8 dp inner horizontal padding preserves the 16 dp
  artwork/trailing-content margin in every state. Names are semibold; text
  line height, vertical padding, ripple, and accessibility growth retain
  Material's metrics. Preview has up to two lines. Timestamp aligns to the
  name baseline; read/failure/invitation indicators align with preview content.
- The single-line name and its mute/timer/ended-membership symbols form one
  leading group, with 4 dp gaps between them. Reserve an 8 dp minimum gap to
  the independently measured timestamp at the trailing edge. Measure the
  timestamp and symbols first; the name uses its natural width up to the
  remaining constraint and native ellipsis, so short names keep adjacent
  icons and long names never displace them. No character-count or screen-width cutoff;
  the full name stays in semantics and the layout mirrors in RTL.
- A scoped scrolling-crash workaround keeps all five text/status slots in
  `ChatRowTextLayout`, inside the native `ListItem` content slot, rather than
  querying inherited baselines on a supporting `Row`/`Box`. Read baselines
  only from direct `Text` children and place children only in the placement
  pass. Preserve alpha25's 72/88 dp two-/three-line minima (minus the native
  outer padding inside this text region), `ListItemDefaults.verticalAlignment`
  and unconstrained growth for larger text. The avatar gap, outer geometry,
  native interaction/shape and menu remain unchanged. This is a compatibility
  exception, not a new app-wide list component or arbitrary height system.
- Custom pin badges are 20 dp with 14 dp artwork. Noninteractive inline state
  icons are 16 dp; gaps use the 4/8 dp grid. The exclusive trailing status uses
  the native content-badge minimum of 16 dp. Its shared circular footprint is
  the greater of that minimum, the measured `labelSmall` line height, and a
  single digit plus Material's 4 dp text padding on each side. Manual unread is
  a solid primary circle with that footprint, never the native 6 dp empty
  badge. Invitations use this same primary circle with the official Add
  symbol in `onPrimary`, centered as an overlay so icon/text padding cannot
  widen the circle. Multi-digit/99+ badges may expand horizontally. The unchanged error
  XML has an 800/960-unit circle; 1.2× artwork scale fits its visible circle to
  the same slot and uses `error`. Failure suppresses the unread display only.
- Long press uses `WhiteNoiseDropdownMenu`, the shared native Expressive menu
  described below. The highlighted row uses `surfaceContainerHigh` and the
  native list selected shape (large/16 dp corners); no padding changes on
  selection. Its popup anchor follows the inset row. Chats adds 8 dp of
  transparent vertical padding outside the menu group, separating its surface
  both below and above the row while preserving native fitting/fallback and
  start alignment in RTL. Other dropdowns keep their existing spacing. No
  horizontal chat gesture or custom swipe-action metrics remain.
- New Message uses the native 56 dp FAB, native large rounded-square shape,
  elevation, a 24 dp rounded pencil and primary/onPrimary. Scaffold owns the
  16 dp end/bottom inset, safe area and Snackbar clearance; a pane-only inset
  aligns it within the same capped 680 dp list on expanded windows.
- `WhiteNoiseScaffold` owns each destination's pinned Material scroll behavior.
  `WhiteNoiseTopBar` accepts it; actual lazy-list/grid/scroll state also updates
  the content offset after restoration/programmatic changes. Rest is `surface`;
  under-scroll is `surfaceContainer`. Empty/disposed scrollers reset the state.
  Camera, media, and contextual-selection headers keep specialized styling.
- Chats does not inset the list viewport above the bottom system area. Safe
  bottom clearance plus 88 dp (56 dp FAB + two 16 dp gaps) belongs to scrolling
  content padding, reduced to 16 dp when search hides the FAB.

## App-owned Material menus

- All six popup entry points use `WhiteNoiseDropdownMenu`: native
  `DropdownMenuPopup`, one `DropdownMenuGroup`, and the new command/selected
  item overloads, not the baseline menu overloads.
- Use `MenuDefaults.groupShapes` and index-aware `itemShape` with no local
  corner radius, width, row-height, typography or padding override. Official
  icon XML uses `MenuDefaults.LeadingIconSize` (20 dp in the pinned release),
  not the unrelated 24 dp toolbar metric. Material retains minimum targets
  and font-scaling behavior.
- Standard group color is `surfaceContainerLow`. Selected choices use native
  standard selection colors, shapes, radio semantics and a leading check.
  Destructive commands retain `error` text/icon roles; disabled colors remain
  native. Do not apply a global high-surface override to new menus.
- The popup owns position/focus/motion/RTL/Back/outside dismissal. A regular
  `verticalScroll` inside the group makes tall menus reachable while retaining
  the group's native clipping. No fixed menu dimensions or custom edge logic.
- WN-ANDROID-0040 pins Material 3 alpha25 for API 23 compatibility; see
  `docs/screens/app-menus.md` for the implementation and API-status audit.

## Ordinary Material sheets

- `WhiteNoiseModalBottomSheet` uses one continuous `surfaceContainerLow`:
  #F3F3F3 in light mode, #171717 in dark. Ordinary `ListItem` containers are
  transparent; intentional selected/tonal groups, fields and errors remain.
- Material owns width, top corners, handle, motion, drag and dismissal. Its
  native 32×4 dp handle and 22 dp vertical padding already form a 48 dp slot.
  Do not add another top spacer. The modal owns safeDrawing/IME insets once.
- `WhiteNoiseSheetHeader`: `titleLarge`, directional 24 dp text insets and
  an 8 dp gap to the body, no top padding. Titles wrap. Existing Close actions
  use trailing native IconButtons (48 dp targets), not a leading imitation of
  an iOS sheet control. Task sheets with Close/Done retain a native app bar,
  matching the sheet color, with zero repeated window insets.
- Chats and Chat Info share a native immediate-choice Mute alert dialog with
  scrollable duration rows and Cancel. Ordinary short choices no longer need
  a large separate mute sheet. Specialized camera/media and system UI are
  excluded from these surface/header rules.

## Governing Android sources

- [Grids and units](https://developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units)
- [Content composition and structure](https://developer.android.com/design/ui/mobile/guides/layout-and-content/content-structure)
- [Splash screens](https://developer.android.com/develop/ui/views/launch/splash-screen)
- [Material 3 inset handling](https://developer.android.com/develop/ui/compose/system/material-insets)
- [Compose text input](https://developer.android.com/develop/ui/compose/text/user-input)
- [Material TextField defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/TextFieldDefaults)
- [Material OutlinedTextField defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/OutlinedTextFieldDefaults)
- [Material text-field label positions](https://developer.android.com/reference/kotlin/androidx/compose/material3/TextFieldLabelPosition)
- [Material Button defaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/ButtonDefaults)
- [Material menu specifications](https://m3.material.io/components/menus/specs)
- [Photo Picker navigation buttons (AOSP)](https://android.googlesource.com/platform/packages/providers/MediaProvider/+/refs/heads/main/photopicker/src/com/android/photopicker/features/navigationbar/NavigationBar.kt)
